/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package com.ymcmp.si.lang;

import static com.ymcmp.si.lang.LiteralUtils.*;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.ymcmp.midform.tac.value.*;
import com.ymcmp.midform.tac.type.*;

import com.ymcmp.si.lang.grammar.SiBaseVisitor;
import com.ymcmp.si.lang.grammar.SiLexer;
import com.ymcmp.si.lang.grammar.SiParser;
import com.ymcmp.si.lang.type.*;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ParseTree;

public final class TypeChecker extends SiBaseVisitor<Object> {

    private final Map<Path, SiParser.FileContext> importMap = new LinkedHashMap<>();

    private final Scope<String, TypeBank<Type, Boolean>> definedTypes = new Scope<>();

    private final LinkedList<InstantiatedFunction.Local> ifuncQueue = new LinkedList<>();

    private String namespacePrefix;
    private Path currentFile;
    private boolean isExported;

    public TypeChecker() {
        this.reset();
    }

    public void reset() {
        this.importMap.clear();

        this.definedTypes.clear();

        this.ifuncQueue.clear();

        this.namespacePrefix = "";
        this.currentFile = null;
        this.isExported = false;
    }

    public boolean loadSource(final String raw) {
        return this.loadSource(Paths.get(raw));
    }

    public boolean loadSource(final Path rawPath) {
        final Path path = rawPath.normalize().toAbsolutePath();
        if (this.importMap.containsKey(path)) {
            return false;
        }

        final Path saved = this.currentFile;

        try {
            final SiLexer lexer = new SiLexer(CharStreams.fromPath(path));
            final CommonTokenStream tokens = new CommonTokenStream(lexer);
            final SiParser parser = new SiParser(tokens);

            final SiParser.FileContext ctx = parser.file();

            this.importMap.put(path, ctx);
            this.currentFile = path;
            this.visitFile(ctx);

            this.importMap.remove(path);
            this.importMap.put(path, ctx);
            return true;
        } catch (IOException ex) {
            throw new IllegalImportException("Cannot import " + rawPath, ex);
        } finally {
            this.currentFile = saved;
        }
    }

    public void processLoadedModules() {
        final LinkedList<Pair<String, SiParser.TopLevelDeclContext>> queue = new LinkedList<>();
        for (final SiParser.FileContext ctx : this.importMap.values()) {
            this.processModule(queue, ctx);
        }

        // sort the queued declarations by their process priority
        queue.sort((a, b) -> Integer.compare(getProcessPriority(a.b), getProcessPriority(b.b)));

        // setup global variables
        this.definedTypes.enter();

        // then process the declarations
        Pair<String, SiParser.TopLevelDeclContext> item;
        while ((item = queue.pollFirst()) != null) {
            this.namespacePrefix = item.a;
            this.visitTopLevelDecl(item.b);
        }

        InstantiatedFunction.Local ifunc;
        while ((ifunc = this.ifuncQueue.pollFirst()) != null) {
            this.processFunction(ifunc);
        }
    }

    private void processModule(List<Pair<String, SiParser.TopLevelDeclContext>> queue, SiParser.FileContext ctx) {
        // create the namespace
        this.namespacePrefix = "";
        if (ctx.ns != null) this.visitNamespaceDecl(ctx.ns);

        // then queue the declaration
        for (final SiParser.TopLevelDeclContext decl : ctx.decls) {
            queue.add(new Pair<>(this.namespacePrefix, decl));
        }
    }

    private static int getProcessPriority(SiParser.TopLevelDeclContext ctx) {
        // modules can contain three types of declarations:
        // - (local) types:    these have to be processed first
        // - native functions: these go after
        // - local functions:  these go last

        final ParseTree tree = getDeclarationContext(ctx);
        if (tree instanceof SiParser.DeclTypeContext)       return 0;
        if (tree instanceof SiParser.DeclNativeFuncContext) return 1;
        if (tree instanceof SiParser.DeclFuncContext)       return 2;

        // why not? (just in case)
        return Integer.MAX_VALUE;
    }

    private static ParseTree getDeclarationContext(SiParser.TopLevelDeclContext ctx) {
        return ctx.getChild(ctx.getChildCount() - 2);
    }

    private void processFunction(InstantiatedFunction.Local func) {
        //
    }

    @Override
    public String visitNamespacePath(SiParser.NamespacePathContext ctx) {
        final String prefix = (ctx.root == null ? this.namespacePrefix : "") + '\\';
        return ctx.parts.stream().map(Token::getText).collect(Collectors.joining("\\", prefix, ""));
    }

    @Override
    public Object visitNamespaceDecl(SiParser.NamespaceDeclContext ctx) {
        this.namespacePrefix = this.visitNamespacePath(ctx.ns);
        return null;
    }

    @Override
    public Object visitImportDecl(SiParser.ImportDeclContext ctx) {
        final String filePath = convertStringLiteral(ctx.path.getText());

        // Want to resolve it against the directory of the current file
        // that's why use resolveSibling instead of resolve
        this.loadSource(this.currentFile.resolveSibling(filePath));
        return null;
    }

    @Override
    public Object visitFile(SiParser.FileContext ctx) {
        // Only process the imports. For actual processing of declarations,
        // see methods processLoadedModules and processModule.
        for (final SiParser.ImportDeclContext importDecls : ctx.imports) {
            this.visitImportDecl(importDecls);
        }
        return null;
    }

    @Override
    public Object visitTopLevelDecl(SiParser.TopLevelDeclContext ctx) {
        this.isExported = ctx.vis != null;
        return this.visit(getDeclarationContext(ctx));
    }

    @Override
    public Object visitDeclTypeAlias(SiParser.DeclTypeAliasContext ctx) {
        final String name = this.namespacePrefix + '\\' + ctx.name.getText();
        final TypeBank<Type, Boolean> bank = definedTypes.getCurrentOrInit(name, TypeBank::new);

        final Type aliased;
        if (ctx.generic != null) {
            this.definedTypes.enter();
            final List<FreeType> boundary = this.visitDeclGeneric(ctx.generic);
            final ParametricType<Type> pt = new ParametricType<>(this.visitCoreTypes(ctx.type), boundary);
            aliased = pt;
            bank.addParametricType(pt, this.isExported);
            this.definedTypes.exit();
        } else {
            final Type t = this.visitCoreTypes(ctx.type); 
            aliased = t;
            bank.setSimpleType(t, this.isExported);
        }

        System.out.println("scope  =   " + this.namespacePrefix);
        System.out.println("alias  :   " + ctx.name.getText());
        System.out.println(" type  :   " + aliased);
        return null;
    }

    @Override
    public FreeType visitParamFreeType(SiParser.ParamFreeTypeContext ctx) {
        return new FreeType(ctx.name.getText());
    }

    @Override
    public FreeType visitParamEquivType(SiParser.ParamEquivTypeContext ctx) {
        return new FreeType(ctx.name.getText(), (Type) this.visit(ctx.bound));
    }

    @Override
    public List<FreeType> visitDeclGeneric(SiParser.DeclGenericContext ctx) {
        final ArrayList<FreeType> list = new ArrayList<>(ctx.args.size());
        for (final SiParser.GenericParamContext t : ctx.args) {
            final FreeType bound = (FreeType) this.visit(t);
            list.add(bound);

            // need to add it to the current type scope
            final String name = bound.getName();
            final TypeBank<Type, Boolean> bank = this.definedTypes.getCurrentOrInit(name, TypeBank::new);

            try {
                bank.setSimpleType(bound.expandBound(), true);
            } catch (DuplicateDefinitionException ex) {
                throw new DuplicateDefinitionException("Duplicate type parameter: " + name, ex);
            }
        }

        return list;
    }

    @Override
    public List<Type> visitTypeParams(SiParser.TypeParamsContext ctx) {
        final ArrayList<Type> list = new ArrayList<>(ctx.types.size());
        for (final SiParser.BaseLevelContext t : ctx.types) {
            list.add((Type) this.visit(t));
        }

        return list;
    }

    @Override
    public Type visitCoreNomialType(SiParser.CoreNomialTypeContext ctx) {
        switch (ctx.getText()) {
            case "int":     return IntegerType.INT32;
            case "double":  return ImmDouble.TYPE;
            case "bool":    return ImmBoolean.TYPE;
            case "byte":    return IntegerType.INT8;
            case "char":    return ImmCharacter.TYPE;
            case "string":  return ImmString.TYPE;
            default:        throw new UnboundDefinitionException("Unknown primitive type: " + ctx.getText());
        }
    }

    @Override
    public Type visitUserDefType(SiParser.UserDefTypeContext ctx) {
        final String rawName = ctx.base.getText();
        String selectedName = null;
        TypeBank<Type, Boolean> bank = null;

        if (!rawName.contains("\\")) {
            // It might be a type parameter (from generic types)
            selectedName = rawName;
            bank = this.definedTypes.get(rawName);
        }

        if (bank == null) {
            // It might be a file-level type
            selectedName = this.visitNamespacePath(ctx.base);
            bank = this.definedTypes.get(selectedName);
        }

        if (bank == null) {
            throw new UnboundDefinitionException("Attempt to use undefined type: " + rawName);
        }

        // This has to be a simple type (based on grammar)
        if (!bank.hasSimpleType()) {
            if (bank.hasParametricType()) {
                throw new TypeMismatchException("Missing type paramters for type: " + selectedName);
            }
            throw new UnboundDefinitionException("Unbound definition for type: " + selectedName);
        }

        if (!bank.getSimpleMapping()) {
            // If hidden, check if namespace allows us to do so
            if (!isAccessible(selectedName, this.namespacePrefix)) {
                throw new UnboundDefinitionException("Using hidden type: " + selectedName + " from: " + this.namespacePrefix);
            }
        }
        return bank.getSimpleType();
    }

    @Override
    public Type visitParametrizeGeneric(SiParser.ParametrizeGenericContext ctx) {
        final String name = this.visitNamespacePath(ctx.base);
        final TypeBank<Type, Boolean> bank = this.definedTypes.get(name);
        if (bank == null) {
            throw new UnboundDefinitionException("Attempt to use undefined type: " + name);
        }

        final List<Type> args = this.visitTypeParams(ctx.args);
        try {
            final ParametricType<Type> pt = bank.selectParametrization(args);
            if (!bank.getParametricMapping(pt)) {
                // If hidden, check if namespace allows us to do so
                if (!isAccessible(name, this.namespacePrefix)) {
                    throw new UnboundDefinitionException("Using hidden type: " + name + " from: " + this.namespacePrefix);
                }
            }
            return pt.parametrize(args);
        } catch (TypeMismatchException ex) {
            throw new TypeMismatchException("Cannot parametrize type: " + name, ex);
        }
    }

    @Override
    public Type visitTypeParenthesis(SiParser.TypeParenthesisContext ctx) {
        final Type input = ctx.e == null ? UnitType.INSTANCE : this.visitCoreTypes(ctx.e);
        if (ctx.out == null) {
            // the parenthesis was just to group types
            return input;
        }
        return new FunctionType(input, (Type) this.visit(ctx.out));
    }

    @Override
    public Type visitTupleLevel(SiParser.TupleLevelContext ctx) {
        final ArrayList<Type> list = new ArrayList<>(ctx.t.size());
        for (final SiParser.BaseLevelContext t : ctx.t) {
            list.add((Type) this.visit(t));
        }

        if (list.size() == 1) {
            return list.get(0);
        }
        return new TupleType(list);
    }

    @Override
    public Type visitExtensionLevel(SiParser.ExtensionLevelContext ctx) {
        final ArrayList<Type> list = new ArrayList<>(ctx.t.size());
        for (final SiParser.TupleLevelContext t : ctx.t) {
            list.add(this.visitTupleLevel(t));
        }

        if (list.size() != 1) {
            throw new UnsupportedOperationException("Extension type is not supported (yet)");
        }
        return list.get(0);
    }

    @Override
    public Type visitCoreTypes(SiParser.CoreTypesContext ctx) {
        final ArrayList<Type> list = new ArrayList<>(ctx.t.size());
        for (final SiParser.ExtensionLevelContext t : ctx.t) {
            list.add(this.visitExtensionLevel(t));
        }

        if (list.size() != 1) {
            throw new UnsupportedOperationException("Variant type is not supported (yet)");
        }
        return list.get(0);
    }

    @Override
    public Object visitDeclNativeFunc(SiParser.DeclNativeFuncContext ctx) {
        System.out.println("scope  =   " + this.namespacePrefix);
        System.out.println("native : " + ctx.name.getText());
        return null;
    }

    @Override
    public Object visitDeclFunc(SiParser.DeclFuncContext ctx) {
        System.out.println("scope  =   " + this.namespacePrefix);
        System.out.println(" func  :   " + ctx.name.getText());
        return null;
    }

    private static boolean isAccessible(String id, String accScope) {
        // id = \com\ymcmp\si\lang\id
        // is accessible if accScope starts with \com\ymcmp\si\lang\
        return (accScope + '\\').startsWith(id.substring(0, id.lastIndexOf('\\') + 1));
    }
}
