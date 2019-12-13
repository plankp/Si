/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package com.ymcmp.midform.tac.codegen;

import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import com.ymcmp.midform.tac.Block;
import com.ymcmp.midform.tac.Subroutine;
import com.ymcmp.midform.tac.statement.*;
import com.ymcmp.midform.tac.value.*;
import com.ymcmp.midform.tac.type.*;

public final class C99Generator {

    private final HashMap<ImmString, String> strLiterals = new HashMap<>();
    private final HashMap<TupleType, String> tupleTypes = new HashMap<>();
    private final HashMap<FunctionType, String> funcTypes = new HashMap<>();
    private final HashSet<FuncRef.Native> nativeFuncs = new HashSet<>();

    private final LinkedList<Block> pending = new LinkedList<>();
    private final HashSet<Block> visited = new HashSet<>();
    private final HashSet<Binding> locals = new HashSet<>();

    private final StringBuilder head = new StringBuilder();
    private final StringBuilder body = new StringBuilder();
    private int insertionPoint;

    private boolean inclMath;
    private boolean inclString;

    private boolean genStr;
    private boolean genCmp;

    public C99Generator() {
        this.reset();
    }

    public void reset() {
        this.strLiterals.clear();
        this.tupleTypes.clear();
        this.funcTypes.clear();
        this.nativeFuncs.clear();

        this.pending.clear();
        this.visited.clear();
        this.locals.clear();

        this.head.setLength(0);
        this.body.setLength(0);
        this.insertionPoint = 0;

        this.inclMath = false;
        this.inclString = false;

        this.genStr = false;
        this.genCmp = false;
    }

    public String getGenerated() {
        final StringBuilder sb = new StringBuilder()
                .append("#include <stddef.h>")
                .append(System.lineSeparator());

        if (this.inclMath) sb
                .append("#include <math.h>")
                .append(System.lineSeparator());

        if (this.inclString || this.genStr) sb
                .append("typedef struct").append(System.lineSeparator())
                .append("{").append(System.lineSeparator())
                .append("  size_t sz;").append(System.lineSeparator())
                .append("  short unsigned const buf[];").append(System.lineSeparator())
                .append("} string;")
                .append(System.lineSeparator());

        if (this.inclString) sb
                .append("long int utf16cmp(string const *a, string const *b) {").append(System.lineSeparator())
                .append("  size_t const limit = a->sz < b->sz ? a->sz : b->sz;").append(System.lineSeparator())
                .append("  size_t i;").append(System.lineSeparator())
                .append("  for (i = 0; i < limit; ++i)").append(System.lineSeparator())
                .append("  {").append(System.lineSeparator())
                .append("    long int const diff = a->buf[i] - b->buf[i];").append(System.lineSeparator())
                .append("    if (diff != 0) return diff;").append(System.lineSeparator())
                .append("  }").append(System.lineSeparator())
                .append("  if (a->sz > b->sz) return a->buf[i];").append(System.lineSeparator())
                .append("  if (a->sz < b->sz) return b->buf[i];").append(System.lineSeparator())
                .append("  return 0;").append(System.lineSeparator())
                .append("}")
                .append(System.lineSeparator());

        if (this.genCmp) sb
                .append("#define CMP(a,b) ((a<b)?-1:((a>b)?1:0))")
                .append(System.lineSeparator());

        sb.append(head).append(body);

        return sb.toString();
    }

    public void visitSubroutine(Subroutine sub) {
        this.visited.clear();
        this.locals.clear();

        final String ret = typeToStr(sub.type.getOutput());
        final String params = sub.getParameters().stream()
                .map(this::generateVar)
                .filter(e -> !e.isEmpty())
                .collect(Collectors.joining(","));
        final String mangled = new StringBuilder()
                .append(ret.isEmpty() ? "void" : ret)
                .append(' ')
                .append(mangleSubroutineName(sub))
                .append('(')
                .append(params.isEmpty() ? "void" : params)
                .append(')')
                .toString();

        this.head.append(mangled).append(';').append(System.lineSeparator());

        this.pending.addLast(sub.getInitialBlock());

        this.body.append(System.lineSeparator())
                .append(System.lineSeparator())
                .append(mangled)
                .append(System.lineSeparator())
                .append('{')
                .append(System.lineSeparator());
        this.insertionPoint = this.body.length();

        Block b;
        while ((b = this.pending.pollFirst()) != null) {
            this.visitBlock(b);
        }

        this.body.append('}');
    }

    public void visitBlock(Block block) {
        if (!this.visited.contains(block)) {
            this.visited.add(block);
            this.body.append(mangleBlockName(block)).append(':').append(System.lineSeparator());
            for (final Statement stmt : block.getStatements()) {
                this.visitStatement(stmt);
            }
        }
    }

    public void visitStatement(Statement stmt) {
        final Class<? extends Statement> clazz = stmt.getClass();
        final String name = "visit" + clazz.getSimpleName();
        try {
            final Method method = this.getClass().getMethod(name, clazz);
            method.invoke(this, stmt);
        } catch (NoSuchMethodException | IllegalAccessException ex) {
            throw new RuntimeException("DAMN!: " + stmt + " :: " + name + '(' + clazz.getSimpleName() + " stmt)");
        } catch (InvocationTargetException ex) {
            throw new RuntimeException("DAMN!: " + ex.getTargetException());
        }
    }

    public void visitMakeRefStatement(MakeRefStatement stmt) {
        this.generateLocal(stmt.dst);

        this.body.append("  ")
                .append(valToStr(stmt.dst))
                .append(" = &")
                .append(valToStr(stmt.src))
                .append(';')
                .append(System.lineSeparator());
    }

    public void visitLoadRefStatement(LoadRefStatement stmt) {
        this.generateLocal(stmt.dst);

        final String dst = valToStr(stmt.dst);
        if (!dst.isEmpty()) {
            this.body.append("  ")
                    .append(dst)
                    .append(" = *")
                    .append(valToStr(stmt.ref))
                    .append(';')
                    .append(System.lineSeparator());
        }
    }

    public void visitStoreRefStatement(StoreRefStatement stmt) {
        final String src = valToStr(stmt.src);
        if (!src.isEmpty()) {
            this.body.append("  ")
                    .append('*').append(valToStr(stmt.ref))
                    .append(" = ")
                    .append(src)
                    .append(';')
                    .append(System.lineSeparator());
        }
    }

    public void visitMoveStatement(MoveStatement stmt) {
        this.generateLocal(stmt.dst);

        final String dst = valToStr(stmt.dst);
        if (!dst.isEmpty()) {
            this.body.append("  ")
                    .append(dst)
                    .append(" = ")
                    .append(valToStr(stmt.src))
                    .append(';')
                    .append(System.lineSeparator());
        }
    }

    public void visitUnaryStatement(UnaryStatement stmt) {
        this.generateLocal(stmt.dst);

        final String src = valToStr(stmt.src);

        this.body.append("  ")
                .append(valToStr(stmt.dst))
                .append(" = ");

        switch (stmt.operator) {
            case NOT_I:
                this.body.append('~').append(src);
                break;
            case NEG_D:
            case NEG_I:
                this.body.append('-').append(src);
                break;
            case POS_D:
            case POS_I:
                this.body.append('+').append(src);
                break;
            case NOT_Z:
                this.body.append('!').append(src);
                break;
            case I2D:
                this.body.append("(double)").append(src);
                break;
            case D2I:
                this.body.append("(long int)").append(src);
                break;
            case I2Z:
                this.body.append("(_Bool)").append(src);
                break;
            case Z2I:
                this.body.append("(long int)").append(src);
                break;
            default:
                throw new AssertionError("Unhandled unary operator: " + stmt.operator);
        }

        this.body.append(';')
                .append(System.lineSeparator());
    }

    public void visitBinaryStatement(BinaryStatement stmt) {
        this.generateLocal(stmt.dst);

        final String lhs = valToStr(stmt.lhs);
        final String rhs = valToStr(stmt.rhs);

        this.body.append("  ")
                .append(valToStr(stmt.dst))
                .append(" = ");

        switch (stmt.operator) {
            case AND_II:
                this.body.append(lhs).append('&').append(rhs);
                break;
            case OR_II:
                this.body.append(lhs).append('|').append(rhs);
                break;
            case XOR_II:
                this.body.append(lhs).append('^').append(rhs);
                break;
            case ADD_DD:
            case ADD_II:
                this.body.append(lhs).append('+').append(rhs);
                break;
            case SUB_DD:
            case SUB_II:
                this.body.append(lhs).append('-').append(rhs);
                break;
            case MUL_DD:
            case MUL_II:
                this.body.append(lhs).append('*').append(rhs);
                break;
            case DIV_DD:
            case DIV_II:
                this.body.append(lhs).append('/').append(rhs);
                break;
            case MOD_DD:
                this.inclMath = true;
                this.body.append("fmod(").append(lhs).append(',').append(rhs).append(')');
                break;
            case MOD_II:
                this.body.append(lhs).append('%').append(rhs);
                break;
            case CMP_II:
            case CMP_DD:
            case CMP_CC:
                this.genCmp = true;
                this.body.append("CMP(").append(lhs).append(',').append(rhs).append(')');
                break;
            case CMP_SS:
                this.inclString = true;
                this.body.append("utf16cmp(").append(lhs).append(',').append(rhs).append(')');
                break;
            default:
                throw new AssertionError("Unhandled binary operator: " + stmt.operator);
        }

        this.body.append(';')
                .append(System.lineSeparator());
    }

    public void visitConditionalJumpStatement(ConditionalJumpStatement stmt) {
        final String lhs = valToStr(stmt.lhs);
        final String rhs = valToStr(stmt.rhs);

        this.body.append("  ")
                .append("if (");

        switch (stmt.operator) {
            case EQ_ZZ:
            case EQ_CC:
            case EQ_DD:
            case EQ_II:
                this.body.append(lhs).append("==").append(rhs);
                break;
            case NE_ZZ:
            case NE_CC:
            case NE_DD:
            case NE_II:
                this.body.append(lhs).append("!=").append(rhs);
                break;
            case LT_CC:
            case LT_DD:
            case LT_II:
                this.body.append(lhs).append("<").append(rhs);
                break;
            case LE_CC:
            case LE_DD:
            case LE_II:
                this.body.append(lhs).append("<=").append(rhs);
                break;
            case GE_CC:
            case GE_DD:
            case GE_II:
                this.body.append(lhs).append(">=").append(rhs);
                break;
            case GT_CC:
            case GT_DD:
            case GT_II:
                this.body.append(lhs).append(">").append(rhs);
                break;
            case EQ_SS:
                this.inclString = true;
                this.body.append("utf16cmp(").append(lhs).append(',').append(rhs).append(") == 0L");
                break;
            case NE_SS:
                this.inclString = true;
                this.body.append("utf16cmp(").append(lhs).append(',').append(rhs).append(") != 0L");
                break;
            case LT_SS:
                this.inclString = true;
                this.body.append("utf16cmp(").append(lhs).append(',').append(rhs).append(") < 0L");
                break;
            case LE_SS:
                this.inclString = true;
                this.body.append("utf16cmp(").append(lhs).append(',').append(rhs).append(") <= 0L");
                break;
            case GE_SS:
                this.inclString = true;
                this.body.append("utf16cmp(").append(lhs).append(',').append(rhs).append(") >= 0L");
                break;
            case GT_SS:
                this.inclString = true;
                this.body.append("utf16cmp(").append(lhs).append(',').append(rhs).append(") > 0L");
                break;
            default:
                throw new AssertionError("Unhandled conditional jump operator: " + stmt.operator);
        }

        this.body.append(") goto ")
                .append(mangleBlockName(stmt.ifTrue))
                .append(';')
                .append(System.lineSeparator())
                .append("  ")
                .append("else goto ")
                .append(mangleBlockName(stmt.ifFalse))
                .append(';')
                .append(System.lineSeparator());

        this.pending.addLast(stmt.ifTrue);
        this.pending.addLast(stmt.ifFalse);
    }

    public void visitGotoStatement(GotoStatement stmt) {
        this.body.append("  ")
                .append("goto ")
                .append(mangleBlockName(stmt.next))
                .append(';')
                .append(System.lineSeparator());

        this.pending.addLast(stmt.next);
    }

    public void visitReturnStatement(ReturnStatement stmt) {
        this.body.append("  ")
                .append("return ")
                .append(valToStr(stmt.value))
                .append(';')
                .append(System.lineSeparator());
    }

    public void visitTailCallStatement(TailCallStatement stmt) {
        this.body.append("  ")
                .append("return ")
                .append(generateFunctionCall(stmt.sub, stmt.arg))
                .append(';')
                .append(System.lineSeparator());
    }

    public void visitCallStatement(CallStatement stmt) {
        this.generateLocal(stmt.dst);

        this.body.append("  ");

        if (!Types.equivalent(UnitType.INSTANCE, stmt.dst.getType())) {
            this.body.append(valToStr(stmt.dst))
                    .append(" = ");
        }
        this.body.append(generateFunctionCall(stmt.sub, stmt.arg))
                .append(';')
                .append(System.lineSeparator());
    }

    private String generateFunctionCall(Value func, Value arg) {
        final StringBuilder sb = new StringBuilder()
                .append(valToStr(func))
                .append('(');

        // for reasons, we actually do not pass a tuple
        // (implemented as a struct) to functions.
        if (arg instanceof Tuple) {
            final Tuple tuple = (Tuple) arg;
            for (int i = 0; i < tuple.values.size(); ++i) {
                final String v = valToStr(tuple.values.get(i));
                if (!v.isEmpty()) {
                    sb.append(v).append(',');
                }
            }
            sb.deleteCharAt(sb.length() - 1);
        } else {
            sb.append(valToStr(arg));
        }

        return sb.append(')').toString();
    }

    private void generateLocal(Binding binding) {
        if (!this.locals.contains(binding)) {
            this.locals.add(binding);
            final String decl = generateVar(binding);
            if (!decl.isEmpty()) {
                this.body.insert(this.insertionPoint, "  " + decl + ';' + System.lineSeparator());
            }
        }
    }

    private String generateVar(Binding binding) {
        final String type = typeToStr(binding.getType());
        if (!type.isEmpty()) {
            return type + ' ' + valToStr(binding);
        }
        return "";
    }

    private String typeToStr(Type type) {
        type = type.expandBound();

        if (Types.equivalent(UnitType.INSTANCE, type)) {
            return "";
        }
        if (Types.equivalent(ImmBoolean.TYPE, type)) {
            // come on... we can afford to use C99 right?
            return "_Bool";
        }
        if (Types.equivalent(ImmByte.TYPE, type)) {
            // our bytes are signed 8 bit
            return "char signed";
        }
        if (Types.equivalent(ImmInteger.TYPE, type)) {
            // our ints are 32 bit (so a long int in C)
            return "long int";
        }
        if (Types.equivalent(ImmCharacter.TYPE, type)) {
            // each char is a utf16 codepoint
            return "short unsigned";
        }
        if (Types.equivalent(ImmString.TYPE, type)) {
            this.genStr = true;
            return "string const *";
        }
        if (type instanceof ReferenceType) {
            final ReferenceType ref = (ReferenceType) type;
            String s = typeToStr(ref.getReferentType());
            if (s.isEmpty()) s = "void";
            return s + (ref.isReferentImmutable() ? " const" : "") + " *";
        }
        if (type instanceof TupleType) {
            return this.generateTupleType((TupleType) type);
        }
        if (type instanceof FunctionType) {
            return this.generateFunctionType((FunctionType) type);
        }

        return type.toString();
    }

    private String valToStr(Value value) {
        if (Types.equivalent(UnitType.INSTANCE, value.getType())) {
            return "";
        }

        if (value instanceof ImmBoolean) {
            return ((ImmBoolean) value).content ? "1" : "0";
        }

        if (value instanceof ImmInteger) {
            return ((ImmInteger) value).content + "L";
        }

        if (value instanceof ImmCharacter) {
            return String.format("0x%04x", (int) (((ImmCharacter) value).content));
        }

        if (value instanceof ImmString) {
            final ImmString utf16str = (ImmString) value;
            final String id = this.strLiterals.get(utf16str);
            if (id != null) {
                return id;
            }

            final String name = "_S" + this.strLiterals.size();
            final String constructed = "&" + name;
            this.strLiterals.put(utf16str, constructed);

            this.genStr = true;

            // make the literal immutable!
            this.head.append("string const ")
                    .append(name)
                    .append(" = { .sz=")
                    .append(utf16str.content.length())
                    .append(", .buf= {")
                    .append(utf16str.content.chars().mapToObj(c -> String.format("0x%04x", c)).collect(Collectors.joining(", ")))
                    .append(" } };")
                    .append(System.lineSeparator());

            return constructed;
        }

        if (value instanceof Binding) {
            return splitAndJoin(value.toString(), "%", "_L");
        }

        if (value instanceof FuncRef.Local) {
            return mangleSubroutineName(((FuncRef.Local) value).sub);
        }

        if (value instanceof FuncRef.Native) {
            final FuncRef.Native nat = (FuncRef.Native) value;
            final String sig = this.generateFunctionType(nat.type);

            if (!this.nativeFuncs.contains(nat)) {
                this.nativeFuncs.add(nat);
                this.head.append("extern ")
                        .append(sig)
                        .append(' ')
                        .append(nat.name)
                        .append(';')
                        .append(System.lineSeparator());
            }
            return nat.name;
        }

        if (value instanceof Tuple) {
            final Tuple tuple = (Tuple) value;
            final String name = this.generateTupleType(tuple.type);
            final StringBuilder sb = new StringBuilder()
                    .append('(')
                    .append(name)
                    .append(") {");

            for (int i = 0; i < tuple.values.size(); ++i) {
                final String v = valToStr(tuple.values.get(i));
                if (!v.isEmpty()) {
                    sb.append(" .t").append(i)
                            .append('=')
                            .append(v)
                            .append(',');
                }
            }
            sb.deleteCharAt(sb.length() - 1);

            sb.append(" }");
            return sb.toString();
        }

        return value.toString();
    }

    private String generateTupleType(TupleType tuple) {
        final String str = this.tupleTypes.get(tuple);
        if (str != null) {
            return str;
        }

        // construct a struct that will act like the tuple
        final String name = "struct _T" + this.tupleTypes.size();
        this.tupleTypes.put(tuple, name);

        final StringBuilder sb = new StringBuilder()
                .append(name).append(" /* ").append(tuple).append(" */")
                .append(System.lineSeparator())
                .append('{')
                .append(System.lineSeparator());
        for (int i = 0; i < tuple.elements.size(); ++i) {
            final String f = typeToStr(tuple.elements.get(i));
            if (!f.isEmpty()) {
                sb.append("  ")
                        .append(f)
                        .append(" t").append(i)
                        .append(';')
                        .append(System.lineSeparator());
            }
        }
        sb.append("};").append(System.lineSeparator());

        this.head.append(sb);

        return name;
    }

    private String generateFunctionType(FunctionType funcType) {
        final String str = this.funcTypes.get(funcType);
        if (str != null) {
            return str;
        }

        // use typedefs (damn function pointers are ugly to work with)
        final String name = "_F" + this.funcTypes.size();
        this.funcTypes.put(funcType, name);

        final String out = typeToStr(funcType.getOutput());
        final StringBuilder sb = new StringBuilder()
                .append("typedef ")
                .append(out.isEmpty() ? "void" : out)
                .append(" (")
                .append(name)
                .append(")(");

        final int limit = funcType.numberOfSplattedInputs();
        if (limit == 0) {
            sb.append("void");
        } else {
            for (int i = 0; i < limit; ++i) {
                final String f = typeToStr(funcType.getSplattedInput(i));
                if (!f.isEmpty()) {
                    sb.append(f).append(',');
                }
            }

            sb.deleteCharAt(sb.length() - 1);
        }
        sb.append(");").append(System.lineSeparator());

        this.head.append(sb);

        return name;
    }

    private static String mangleBlockName(Block block) {
        return splitAndJoin(block.name, "%", "_B");
    }

    public static String mangleSubroutineName(Subroutine sub) {
        final StringBuilder name = new StringBuilder()
                .append(splitAndJoin(sub.getSimpleName(), "\\\\", "_Z"));

        final List<Type> params = sub.getTypeParameters();
        if (!params.isEmpty()) {
            name.append('Y');
            for (final Type t : params) {
                name.append(mangleTypeName(t)).append('_');
            }
            name.deleteCharAt(name.length() - 1);
        }
        return name.toString();
    }

    private static String splitAndJoin(String str, String pat, String prefix) {
        final String[] chunks = str.split(pat);
        final StringBuilder sb = new StringBuilder(prefix);

        // if first one is empty (example: \spec\foo becomes "", "spec", "foo")
        // then we start skip it!
        int start = 0;
        if (chunks[0].isEmpty()) ++start;

        // if there is more than one processed chunk, we start with 'N'
        if (chunks.length - start > 1) {
            sb.append('N');
        }

        for (int i = start; i < chunks.length; ++i) {
            final String chunk = chunks[i];
            sb.append(chunk.length()).append(chunk);
        }

        // if there is more than one processed chunk, we end with 'E'
        if (chunks.length - start > 1) {
            sb.append('E');
        }

        return sb.toString();
    }

    private static String mangleTypeName(Type type) {
        type = type.expandBound();

        if (Types.equivalent(UnitType.INSTANCE, type)) {
            return "U";
        }
        if (Types.equivalent(ImmBoolean.TYPE, type)) {
            return "Z";
        }
        if (Types.equivalent(ImmByte.TYPE, type)) {
            return "B";
        }
        if (Types.equivalent(ImmInteger.TYPE, type)) {
            return "I";
        }
        if (Types.equivalent(ImmCharacter.TYPE, type)) {
            return "C";
        }
        if (Types.equivalent(ImmString.TYPE, type)) {
            return "S";
        }
        if (type instanceof ReferenceType) {
            final ReferenceType ref = (ReferenceType) type;
            return "P" + (ref.isReferentImmutable() ? "K" : "M") + mangleTypeName(ref.getReferentType());
        }
        if (type instanceof TupleType) {
            final TupleType tuple = (TupleType) type;
            final StringBuilder sb = new StringBuilder()
                    .append('T').append(tuple.numberOfElements());
            for (final Type el : tuple.getElements()) {
                sb.append(mangleTypeName(el));
            }
            return sb.toString();
        }
        if (type instanceof FunctionType) {
            final FunctionType func = (FunctionType) type;
            return 'F' + mangleTypeName(func.getInput()) + mangleTypeName(func.getOutput());
        }

        final String frag = type.toString();
        return frag.length() + frag;
    }
}