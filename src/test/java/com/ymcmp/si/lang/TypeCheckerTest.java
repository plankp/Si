/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package com.ymcmp.si.lang;

import static com.ymcmp.si.lang.type.TypeUtils.convFrom;
import static com.ymcmp.si.lang.type.TypeUtils.convTo;
import static com.ymcmp.si.lang.type.TypeUtils.equiv;
import static com.ymcmp.si.lang.type.TypeUtils.group;
import static com.ymcmp.si.lang.type.TypeUtils.free;
import static com.ymcmp.si.lang.type.TypeUtils.func;
import static com.ymcmp.si.lang.type.TypeUtils.name;
import static com.ymcmp.si.lang.type.TypeUtils.or;

import java.util.Arrays;
import java.util.HashMap;

import org.antlr.v4.runtime.CharStreams;
import com.ymcmp.si.lang.grammar.SiLexer;
import com.ymcmp.si.lang.grammar.SiParser;
import com.ymcmp.si.lang.type.ParametricType;
import com.ymcmp.si.lang.type.TupleType;
import com.ymcmp.si.lang.type.Type;
import com.ymcmp.si.lang.type.TypeMismatchException;
import com.ymcmp.si.lang.type.UnitType;
import com.ymcmp.si.lang.type.restriction.TypeRestriction;

import org.antlr.v4.runtime.CommonTokenStream;
import org.junit.Assert;
import org.junit.Test;

public class TypeCheckerTest {

    private static HashMap<String, TypeBank> createTypeTestingMap() {
        // Just in case we need to change the default type map
        return new HashMap<>();
    }

    @Test
    public void testTypesSi() {
        try {
            SiLexer lexer = new SiLexer(CharStreams.fromStream(this.getClass().getResourceAsStream("/types.si")));
            CommonTokenStream tokens = new CommonTokenStream(lexer);
            SiParser parser = new SiParser(tokens);

            TypeChecker visitor = new TypeChecker();
            visitor.visit(parser.file());

            final HashMap<String, TypeBank> map = createTypeTestingMap();

            map.put("str", TypeBank.withSimpleType(name("string")));
            map.put("unit", TypeBank.withSimpleType(UnitType.INSTANCE));

            map.put("triple", TypeBank.withSimpleType(group(name("int"), name("int"), name("int"))));

            map.put("int_double", TypeBank.withSimpleType(group(name("int"), name("double"))));

            final TypeRestriction freeTypeT = free("T");
            final TypeRestriction freeTypeS = free("S");
            map.put("JavaPredicate", TypeBank.withParametricType(
                    new ParametricType(func(freeTypeT.getAssociatedType(), name("bool")), Arrays.asList(freeTypeT))));
            map.put("PhantomType",
                    TypeBank.withParametricType(new ParametricType(name("string"), Arrays.asList(freeTypeS))));

            map.put("int_double_pred", TypeBank.withSimpleType(func(group(name("int"), name("double")), name("bool"))));
            map.put("int_pred", TypeBank.withSimpleType(func(name("int"), name("bool"))));

            map.put("hof_1", TypeBank.withSimpleType(func(name("int"), func(name("int"), name("int")))));
            map.put("hof_2", TypeBank.withSimpleType(func(func(name("int"), name("int")), name("int"))));

            map.put("str_pred", TypeBank.withSimpleType(func(name("string"), name("bool"))));

            map.put("int_int_pred", TypeBank.withSimpleType(func(group(name("int"), name("int")), name("bool"))));

            map.put("lost_type", TypeBank.withSimpleType(func(name("string"), name("bool"))));

            final TypeRestriction equiv = equiv("T", name("int"));
            map.put("idiotic_string",
                    TypeBank.withParametricType(new ParametricType(name("string"), Arrays.asList(equiv))));
            map.put("valid_expansion", TypeBank.withSimpleType(name("string")));

            final TypeRestriction bound = convTo("T", or(name("int"), name("string")));
            map.put("int_string_variant",
                    TypeBank.withParametricType(new ParametricType(bound.getAssociatedType(), Arrays.asList(bound))));
            map.put("wtf_int", TypeBank.withSimpleType(name("int")));
            map.put("wtf_str", TypeBank.withSimpleType(name("string")));

            visitor.getUserDefinedTypes().forEachAccessible((k, v) -> {
                if (map.containsKey(k)) {
                    Assert.assertEquals("For typename " + k, map.get(k), v);
                } else {
                    System.out.println(k + " as " + v + " ignored!");
                }
            });
        } catch (java.io.IOException ex) {
            Assert.fail("Wut!? IOException should not happen: " + ex.getMessage());
        }
    }

    @Test
    public void testPropagatingBoundsSi() {
        try {
            SiLexer lexer = new SiLexer(
                    CharStreams.fromStream(this.getClass().getResourceAsStream("/propagating_bounds.si")));
            CommonTokenStream tokens = new CommonTokenStream(lexer);
            SiParser parser = new SiParser(tokens);

            TypeChecker visitor = new TypeChecker();
            visitor.visit(parser.file());

            final HashMap<String, TypeBank> map = createTypeTestingMap();

            {
                final TypeRestriction T = equiv("T", name("int"));
                map.put("guard_type", TypeBank.withParametricType(new ParametricType(UnitType.INSTANCE, Arrays.asList(T))));
            }

            {
                final TypeRestriction T = equiv("T", name("int"));
                map.put("guard_type_2", TypeBank.withParametricType(new ParametricType(UnitType.INSTANCE, Arrays.asList(T))));
            }

            {
                final TypeRestriction T = convTo("T", or(name("int"), name("string"), name("char")));
                map.put("triple_variant", TypeBank.withParametricType(new ParametricType(T.getAssociatedType(), Arrays.asList(T))));
            }

            {
                final TypeRestriction T = convTo("T", or(name("int"), name("char")));
                map.put("double_of_triple", TypeBank.withParametricType(new ParametricType(T.getAssociatedType(), Arrays.asList(T))));
            }

            {
                map.put("my_int", TypeBank.withSimpleType(name("int")));
            }

            {
                final TypeRestriction T = convFrom("T", or(name("int"), name("char")));
                map.put("double_variant", TypeBank.withParametricType(new ParametricType(T.getAssociatedType(), Arrays.asList(T))));
            }

            {
                final TypeRestriction T = convFrom("T", or(name("int"), name("string"), name("char")));
                map.put("triple_of_double", TypeBank.withParametricType(new ParametricType(T.getAssociatedType(), Arrays.asList(T))));
            }

            {
                map.put("my_quad", TypeBank.withSimpleType(or(name("string"), name("int"), name("char"), UnitType.INSTANCE)));
            }

            visitor.getUserDefinedTypes().forEachAccessible((k, v) -> {
                if (map.containsKey(k)) {
                    Assert.assertEquals("For typename " + k, map.get(k), v);
                } else {
                    System.out.println(k + " as " + v + " ignored!");
                }
            });
        } catch (java.io.IOException ex) {
            Assert.fail("Wut!? IOException should not happen: " + ex.getMessage());
        }
    }

    @Test(expected = TypeMismatchException.class)
    public void testIllegalTypeParametrizationSi() {
        try {
            SiLexer lexer = new SiLexer(
                    CharStreams.fromStream(this.getClass().getResourceAsStream("/illegal_parametrization.si")));
            CommonTokenStream tokens = new CommonTokenStream(lexer);
            SiParser parser = new SiParser(tokens);

            TypeChecker visitor = new TypeChecker();
            visitor.visit(parser.file());
        } catch (java.io.IOException ex) {
            Assert.fail("Wut!? IOException should not happen: " + ex.getMessage());
        }
    }

    @Test(expected = DuplicateDefinitionException.class)
    public void testIllegalDuplicateParametrizationSi() {
        try {
            SiLexer lexer = new SiLexer(
                    CharStreams.fromStream(this.getClass().getResourceAsStream("/duplicate_parametrization.si")));
            CommonTokenStream tokens = new CommonTokenStream(lexer);
            SiParser parser = new SiParser(tokens);

            TypeChecker visitor = new TypeChecker();
            visitor.visit(parser.file());
        } catch (java.io.IOException ex) {
            Assert.fail("Wut!? IOException should not happen: " + ex.getMessage());
        }
    }

    @Test
    public void testFuncsSi() {
        try {
            SiLexer lexer = new SiLexer(CharStreams.fromStream(this.getClass().getResourceAsStream("/funcs.si")));
            CommonTokenStream tokens = new CommonTokenStream(lexer);
            SiParser parser = new SiParser(tokens);

            TypeChecker visitor = new TypeChecker();
            visitor.visit(parser.file());

            final HashMap<String, TypeBank> map = new HashMap<>();

            {
                final TypeBank bank = new TypeBank();
                map.put("nilary", bank);
                bank.setSimpleType(func(UnitType.INSTANCE, UnitType.INSTANCE));
            }
            {
                final TypeBank bank = new TypeBank();
                map.put("unary", bank);
                bank.setSimpleType(func(name("int"), name("int")));
            }
            {
                final TypeBank bank = new TypeBank();
                map.put("binary", bank);
                bank.setSimpleType(func(group(name("int"), name("int")), name("int")));
            }

            {
                final TypeBank bank = new TypeBank();
                map.put("identity", bank);

                final TypeRestriction freeType = free("T");
                bank.addParametricType(new ParametricType(
                        func(freeType.getAssociatedType(), freeType.getAssociatedType()), Arrays.asList(freeType)));
            }

            visitor.getUserDefinedFunctions().forEachAccessible((k, v) -> {
                if (map.containsKey(k)) {
                    Assert.assertEquals("For function name " + k, map.get(k), v);
                } else {
                    System.out.println(k + " as " + v + " ignored!");
                }
            });
        } catch (java.io.IOException ex) {
            Assert.fail("Wut!? IOException should not happen: " + ex.getMessage());
        }
    }
}