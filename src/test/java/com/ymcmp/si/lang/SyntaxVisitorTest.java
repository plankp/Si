/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package com.ymcmp.si.lang;

import static com.ymcmp.si.lang.type.TypeUtils.group;
import static com.ymcmp.si.lang.type.TypeUtils.func;
import static com.ymcmp.si.lang.type.TypeUtils.name;

import java.util.Arrays;
import java.util.HashMap;

import org.antlr.v4.runtime.CharStreams;
import com.ymcmp.si.lang.grammar.SiLexer;
import com.ymcmp.si.lang.grammar.SiParser;
import com.ymcmp.si.lang.type.TupleType;
import com.ymcmp.si.lang.type.Type;
import com.ymcmp.si.lang.type.TypeDelegate;
import com.ymcmp.si.lang.type.UnitType;

import org.antlr.v4.runtime.CommonTokenStream;
import org.junit.Assert;
import org.junit.Test;

public class SyntaxVisitorTest {

    @Test
    public void testTypesSi() {
        try {
            SiLexer lexer = new SiLexer(CharStreams.fromStream(this.getClass().getResourceAsStream("/types.si")));
            CommonTokenStream tokens = new CommonTokenStream(lexer);
            SiParser parser = new SiParser(tokens);

            SyntaxVisitor visitor = new SyntaxVisitor();
            visitor.visitFileHelper(parser.file(), false);

            final HashMap<String, Type> map = new HashMap<>();

            map.put("str", name("string"));
            map.put("unit", UnitType.INSTANCE);

            map.put("triple", group(name("int"), name("int"), name("int")));

            map.put("int_double", group(name("int"), name("double")));
            map.put("comp206_ssv", new TypeDelegate("comp206_ssv", map.get("int_double")));

            map.put("int_double_pred", func(group(name("int"), name("double")), name("bool")));
            map.put("int_pred", func(name("int"), name("bool")));

            map.put("hof_1", func(name("int"), func(name("int"), name("int"))));
            map.put("hof_2", func(func(name("int"), name("int")), name("int")));

            map.put("str_pred", func(name("string"), name("bool")));
            map.put("lost_type", func(name("string"), name("bool")));

            visitor.getUserDefinedTypes().forEachAccessible((k, v) -> {
                if (map.containsKey(k)) {
                    Assert.assertEquals("For " + k, map.get(k), v);
                } else {
                    System.out.println(k + " as " + v + " ignored!");
                }
            });
        } catch (java.io.IOException ex) {
            Assert.fail("Wut!? IOException should not happen: " + ex.getMessage());
        }
    }
}
