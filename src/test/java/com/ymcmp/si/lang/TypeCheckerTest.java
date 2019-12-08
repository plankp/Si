/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package com.ymcmp.si.lang;

import static com.ymcmp.si.lang.type.TypeUtils.equiv;
import static com.ymcmp.si.lang.type.TypeUtils.free;
import static com.ymcmp.si.lang.type.TypeUtils.func;
import static com.ymcmp.si.lang.type.TypeUtils.group;
import static com.ymcmp.si.lang.type.TypeUtils.infer;
import static com.ymcmp.si.lang.type.TypeUtils.name;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import com.ymcmp.midform.tac.*;
import com.ymcmp.midform.tac.type.*;
import com.ymcmp.midform.tac.value.*;

import com.ymcmp.si.lang.grammar.SiLexer;
import com.ymcmp.si.lang.grammar.SiParser;
import com.ymcmp.si.lang.type.FreeType;
import com.ymcmp.si.lang.type.ParametricType;
import com.ymcmp.si.lang.type.TypeMismatchException;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.junit.Assert;
import org.junit.Test;

public class TypeCheckerTest {

    private static HashMap<String, TypeBank<Type>> createTypeTestingMap() {
        // Just in case we need to change the default type map
        return new HashMap<>();
    }

    private void testTypeCheckResultHelper(TypeChecker checker, Optional<Map<String, TypeBank<Type>>> types, Optional<Map<String, TypeBank<FunctionType>>> funcs) {
        types.ifPresent(t -> Assert.assertEquals(t, checker.getUserDefinedTypes().unrollAccessible()));
        funcs.ifPresent(f -> Assert.assertEquals(f, checker.getUserDefinedFunctions()));
    }

    @Test
    public void testTypesSi() {
        TypeChecker visitor = new TypeChecker();
        visitor.loadSource("spec/types.si");
        visitor.processLoadedModules();

        final HashMap<String, TypeBank<Type>> map = createTypeTestingMap();

        map.put("\\str", TypeBank.withSimpleType(name("string")));
        map.put("\\unit", TypeBank.withSimpleType(UnitType.INSTANCE));

        map.put("\\triple", TypeBank.withSimpleType(group(name("int"), name("int"), name("int"))));

        map.put("\\int_double", TypeBank.withSimpleType(group(name("int"), name("double"))));

        final FreeType freeTypeT = free("T");
        final FreeType freeTypeS = free("S");
        map.put("\\JavaPredicate", TypeBank
                .withParametricType(new ParametricType<>(func(freeTypeT, name("bool")), Arrays.asList(freeTypeT))));
        map.put("\\PhantomType",
                TypeBank.withParametricType(new ParametricType<>(name("string"), Arrays.asList(freeTypeS))));

        map.put("\\int_double_pred", TypeBank.withSimpleType(func(group(name("int"), name("double")), name("bool"))));
        map.put("\\int_pred", TypeBank.withSimpleType(func(name("int"), name("bool"))));

        map.put("\\hof_1", TypeBank.withSimpleType(func(name("int"), func(name("int"), name("int")))));
        map.put("\\hof_2", TypeBank.withSimpleType(func(func(name("int"), name("int")), name("int"))));

        map.put("\\str_pred", TypeBank.withSimpleType(func(name("string"), name("bool"))));

        map.put("\\int_int_pred", TypeBank.withSimpleType(func(group(name("int"), name("int")), name("bool"))));

        map.put("\\lost_type", TypeBank.withSimpleType(func(name("string"), name("bool"))));

        final FreeType equiv = equiv("T", name("int"));
        map.put("\\idiotic_string",
                TypeBank.withParametricType(new ParametricType<>(name("string"), Arrays.asList(equiv))));
        map.put("\\valid_expansion", TypeBank.withSimpleType(name("string")));

        this.testTypeCheckResultHelper(visitor, Optional.of(map), Optional.empty());
    }

    @Test
    public void testTypeDispatchSi() {
        TypeChecker visitor = new TypeChecker();
        visitor.loadSource("spec/type_dispatch.si");
        visitor.processLoadedModules();

        final HashMap<String, TypeBank<Type>> typeMap = createTypeTestingMap();
        final HashMap<String, TypeBank<FunctionType>> funcMap = new HashMap<>();

        {
            final FreeType tInt = equiv("T", name("int"));
            final FreeType tDouble = equiv("T", name("double"));
            final FreeType tBool = equiv("T", name("bool"));
            final FreeType tChar = equiv("T", name("char"));
            final FreeType tString = equiv("T", name("string"));
            typeMap.put("\\permut",
                    TypeBank.withParametricTypes(
                            Arrays.asList(new ParametricType<>(name("double"), Arrays.asList(tInt)),
                                    new ParametricType<>(name("bool"), Arrays.asList(tDouble)),
                                    new ParametricType<>(name("char"), Arrays.asList(tBool)),
                                    new ParametricType<>(name("string"), Arrays.asList(tChar)),
                                    new ParametricType<>(name("int"), Arrays.asList(tString)))));
        }

        typeMap.put("\\in_int", TypeBank.withSimpleType(name("double")));
        typeMap.put("\\in_double", TypeBank.withSimpleType(name("bool")));
        typeMap.put("\\in_bool", TypeBank.withSimpleType(name("char")));
        typeMap.put("\\in_char", TypeBank.withSimpleType(name("string")));
        typeMap.put("\\in_string", TypeBank.withSimpleType(name("int")));

        {
            final FreeType tInt = equiv("T", name("int"));
            final FreeType tAny = free("T");
            funcMap.put("\\extreme",
                    TypeBank.withParametricTypes(Arrays.asList(
                            new ParametricType<>(func(UnitType.INSTANCE, name("int")), Arrays.asList(tInt)),
                            new ParametricType<>(func(UnitType.INSTANCE, name("bool")), Arrays.asList(tAny)))));
        }

        funcMap.put("\\int_extreme", TypeBank.withSimpleType(func(UnitType.INSTANCE, name("int"))));
        funcMap.put("\\bool_extreme", TypeBank.withSimpleType(func(UnitType.INSTANCE, name("bool"))));

        this.testTypeCheckResultHelper(visitor, Optional.of(typeMap), Optional.of(funcMap));
    }

    @Test
    public void testOperatorsSi() {
        TypeChecker visitor = new TypeChecker();
        visitor.loadSource("spec/operators.si");
        visitor.processLoadedModules();

        // We don't actually check which types or functions are defined
        // we just want to know if the type checker is accepting (or
        // rejecting) programs correctly.
    }

    @Test
    public void testCastingSi() {
        TypeChecker visitor = new TypeChecker();
        visitor.loadSource("spec/casting.si");
        visitor.processLoadedModules();

        // We don't actually check which types or functions are defined
        // we just want to know if the type checker is accepting (or
        // rejecting) programs correctly.
    }

    @Test
    public void testPropagatingBoundsSi() {
        TypeChecker visitor = new TypeChecker();
        visitor.loadSource("spec/propagating_bounds.si");
        visitor.processLoadedModules();

        final HashMap<String, TypeBank<Type>> map = createTypeTestingMap();

        {
            final FreeType T = equiv("T", name("int"));
            map.put("\\guard_type",
                    TypeBank.withParametricType(new ParametricType<>(UnitType.INSTANCE, Arrays.asList(T))));
        }

        {
            final FreeType T = equiv("T", name("int"));
            map.put("\\guard_type_2",
                    TypeBank.withParametricType(new ParametricType<>(UnitType.INSTANCE, Arrays.asList(T))));
        }

        {
            final FreeType T = equiv("T", name("int"));
            final FreeType U = free("U");
            map.put("\\two",
                    TypeBank.withParametricType(new ParametricType<>(UnitType.INSTANCE, Arrays.asList(T, U))));
        }

        {
            final FreeType S = free("S");
            map.put("\\three",
                    TypeBank.withParametricType(new ParametricType<>(UnitType.INSTANCE, Arrays.asList(S))));
        }

        this.testTypeCheckResultHelper(visitor, Optional.of(map), Optional.empty());
    }

    @Test(expected = CompileTimeException.class)
    public void testIllegalParametrizationSi() {
        TypeChecker visitor = new TypeChecker();
        visitor.loadSource("spec/illegal_parametrization.si");
        visitor.processLoadedModules();
    }

    @Test(expected = CompileTimeException.class)
    public void testIllegalPropagationSi() {
        TypeChecker visitor = new TypeChecker();
        visitor.loadSource("spec/illegal_propagation.si");
        visitor.processLoadedModules();
    }

    @Test(expected = CompileTimeException.class)
    public void testIllegalDuplicateParametrizationSi() {
        TypeChecker visitor = new TypeChecker();
        visitor.loadSource("spec/duplicate_parametrization.si");
        visitor.processLoadedModules();
    }

    @Test
    public void testNamespacesSi() {
        TypeChecker visitor = new TypeChecker();
        visitor.loadSource("spec/namespaces.si");
        visitor.processLoadedModules();

        // namespaces.si is included by import.si
        // so we'll do all the testing in import.si
    }

    @Test
    public void testImportSi() {
        TypeChecker visitor = new TypeChecker();
        visitor.loadSource("spec/import.si");
        visitor.processLoadedModules();

        final HashMap<String, TypeBank<Type>> typeMap = createTypeTestingMap();
        final HashMap<String, TypeBank<FunctionType>> funcMap = new HashMap<>();

        // namespaces.si

        typeMap.put("\\spec\\foo\\str_1", TypeBank.withSimpleType(name("string")));
        typeMap.put("\\spec\\foo\\str_rel", TypeBank.withSimpleType(name("string")));
        typeMap.put("\\spec\\foo\\str_abs", TypeBank.withSimpleType(name("string")));

        funcMap.put("\\spec\\foo\\f", TypeBank.withSimpleType(func(UnitType.INSTANCE, infer(UnitType.INSTANCE))));
        funcMap.put("\\spec\\foo\\g", TypeBank.withSimpleType(func(UnitType.INSTANCE, infer(UnitType.INSTANCE))));
        funcMap.put("\\spec\\foo\\h", TypeBank.withSimpleType(func(UnitType.INSTANCE, infer(UnitType.INSTANCE))));

        // import.si

        funcMap.put("\\spec\\bar\\ret_str_1", TypeBank.withSimpleType(func(UnitType.INSTANCE, name("string"))));

        this.testTypeCheckResultHelper(visitor, Optional.of(typeMap), Optional.of(funcMap));
    }

    @Test
    public void testBindingsSi() {
        TypeChecker visitor = new TypeChecker();
        visitor.loadSource("spec/bindings.si");
        visitor.processLoadedModules();

        final HashMap<String, TypeBank<FunctionType>> funcMap = new HashMap<>();

        funcMap.put("\\nested_bindings", TypeBank.withSimpleType(func(UnitType.INSTANCE, infer(name("int")))));
        funcMap.put("\\mixed_lookup", TypeBank.withSimpleType(func(UnitType.INSTANCE, infer(name("int")))));

        this.testTypeCheckResultHelper(visitor, Optional.empty(), Optional.of(funcMap));

        // and check if the program runs correctly!
        final Map<String, Subroutine> ifuncs = visitor.getAllInstantiatedFunctions();
        final Emulator emu = new Emulator();

        Assert.assertEquals(new ImmInteger(15), emu.callSubroutine(ifuncs.get("\\nested_bindings"), ImmUnit.INSTANCE));
        Assert.assertEquals(new ImmInteger(3), emu.callSubroutine(ifuncs.get("\\mixed_lookup"), ImmUnit.INSTANCE));
    }

    @Test
    public void testBranchingSi() {
        TypeChecker visitor = new TypeChecker();
        visitor.loadSource("spec/branching.si");
        visitor.processLoadedModules();

        final HashMap<String, TypeBank<FunctionType>> funcMap = new HashMap<>();

        funcMap.put("\\single_if", TypeBank.withSimpleType(func(infer(name("char")), infer(name("string")))));
        funcMap.put("\\double_if", TypeBank.withSimpleType(func(infer(name("char")), infer(name("string")))));
        funcMap.put("\\triple_if", TypeBank.withSimpleType(func(group(infer(name("char")), infer(name("char"))), infer(name("string")))));
        funcMap.put("\\short_circuiting", TypeBank.withSimpleType(func(group(infer(name("char")), infer(name("char"))), infer(name("int")))));

        this.testTypeCheckResultHelper(visitor, Optional.empty(), Optional.of(funcMap));

        // and check if the program runs correctly!
        final Map<String, Subroutine> ifuncs = visitor.getAllInstantiatedFunctions();
        final Emulator emu = new Emulator();

        final ImmCharacter charA = new ImmCharacter('A');
        final ImmCharacter charB = new ImmCharacter('B');
        final ImmCharacter charC = new ImmCharacter('C');

        Assert.assertEquals(new ImmString("yes!"), emu.callSubroutine(ifuncs.get("\\single_if"), charA));
        Assert.assertEquals(new ImmString("no!"), emu.callSubroutine(ifuncs.get("\\single_if"), charB));
        Assert.assertEquals(new ImmString("no!"), emu.callSubroutine(ifuncs.get("\\single_if"), charC));

        Assert.assertEquals(new ImmString("yes!"), emu.callSubroutine(ifuncs.get("\\double_if"), charA));
        Assert.assertEquals(new ImmString("umm"), emu.callSubroutine(ifuncs.get("\\double_if"), charB));
        Assert.assertEquals(new ImmString("no!"), emu.callSubroutine(ifuncs.get("\\double_if"), charC));

        Assert.assertEquals(new ImmString("AA"), emu.callSubroutine(ifuncs.get("\\triple_if"), Tuple.from(charA, charA)));
        Assert.assertEquals(new ImmString("A?"), emu.callSubroutine(ifuncs.get("\\triple_if"), Tuple.from(charA, charB)));
        Assert.assertEquals(new ImmString("BA"), emu.callSubroutine(ifuncs.get("\\triple_if"), Tuple.from(charB, charA)));
        Assert.assertEquals(new ImmString("B?"), emu.callSubroutine(ifuncs.get("\\triple_if"), Tuple.from(charB, charB)));
        Assert.assertEquals(new ImmString("??"), emu.callSubroutine(ifuncs.get("\\triple_if"), Tuple.from(charC, charA)));

        Assert.assertEquals(new ImmInteger(2), emu.callSubroutine(ifuncs.get("\\short_circuiting"), Tuple.from(charA, charA)));
        Assert.assertEquals(new ImmInteger(1), emu.callSubroutine(ifuncs.get("\\short_circuiting"), Tuple.from(charA, charB)));
        Assert.assertEquals(new ImmInteger(1), emu.callSubroutine(ifuncs.get("\\short_circuiting"), Tuple.from(charB, charA)));
        Assert.assertEquals(new ImmInteger(0), emu.callSubroutine(ifuncs.get("\\short_circuiting"), Tuple.from(charB, charB)));
        Assert.assertEquals(new ImmInteger(1), emu.callSubroutine(ifuncs.get("\\short_circuiting"), Tuple.from(charC, charA)));
    }

    @Test
    public void testFuncsSi() {
        TypeChecker visitor = new TypeChecker();
        visitor.loadSource("spec/funcs.si");
        visitor.processLoadedModules();

        final HashMap<String, TypeBank<FunctionType>> map = new HashMap<>();

        {
            final TypeBank<FunctionType> bank = new TypeBank<>();
            map.put("\\nilary", bank);
            bank.setSimpleType(func(UnitType.INSTANCE, UnitType.INSTANCE));
        }
        {
            final TypeBank<FunctionType> bank = new TypeBank<>();
            map.put("\\unary", bank);
            bank.setSimpleType(func(name("int"), name("int")));
        }
        {
            final TypeBank<FunctionType> bank = new TypeBank<>();
            map.put("\\binary", bank);
            bank.setSimpleType(func(group(name("int"), name("int")), infer(name("int"))));
        }

        {
            final TypeBank<FunctionType> bank = new TypeBank<>();
            map.put("\\identity", bank);

            final FreeType freeType = free("T");
            bank.addParametricType(new ParametricType<>(func(freeType, freeType), Arrays.asList(freeType)));
        }

        map.put("\\call_binary", TypeBank.withSimpleType(func(UnitType.INSTANCE, name("int"))));

        {
            final FreeType rBool = equiv("T", name("bool"));
            final FreeType rInt = equiv("T", name("int"));
            final FreeType rUnit = equiv("T", UnitType.INSTANCE);
            map.put("\\to_int",
                    TypeBank.withParametricTypes(Arrays.asList(
                            new ParametricType<>(func(name("bool"), name("int")), Arrays.asList(rBool)),
                            new ParametricType<>(func(name("int"), name("int")), Arrays.asList(rInt)),
                            new ParametricType<>(func(UnitType.INSTANCE, name("int")), Arrays.asList(rUnit)))));
        }

        {
            final FreeType freeType = free("T");
            map.put("\\calls_to_int", TypeBank.withParametricType(
                    new ParametricType<>(func(freeType, name("int")), Arrays.asList(freeType))));
        }

        map.put("\\returns_1", TypeBank.withSimpleType(func(UnitType.INSTANCE, name("int"))));

        map.put("\\is_zero", TypeBank.withSimpleType(func(infer(name("int")), infer(name("bool")))));
        map.put("\\is_space", TypeBank.withSimpleType(func(infer(name("char")), infer(name("bool")))));

        this.testTypeCheckResultHelper(visitor, Optional.empty(), Optional.of(map));
    }
}
