/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package com.ymcmp.si.lang.type;

import java.util.Arrays;

import com.ymcmp.midform.tac.type.*;

public final class TypeUtils {

    private TypeUtils() {
    }

    public static NomialType name(String name) {
        return new NomialType(name);
    }

    public static IntegerType integer(int width) {
        switch (width) {
            case 8:     return IntegerType.INT8;
            case 16:    return IntegerType.INT16;
            case 32:    return IntegerType.INT32;
            case 64:    return IntegerType.INT64;
            default:    throw new UnsupportedOperationException("Unsupport integer with bit width: " + width);
        }
    }

    public static TupleType group(Type... col) {
        return new TupleType(Arrays.asList(col));
    }

    public static FunctionType func(Type in, Type out) {
        return new FunctionType(in, out);
    }

    public static VariantType or(Type... types) {
        return new VariantType(Arrays.asList(types));
    }

    public static InferredType infer(Type t) {
        final InferredType ret = new InferredType();
        ret.assignableFrom(t); // this sets the type
        return ret;
    }

    public static FreeType free(String name) {
        return new FreeType(name);
    }

    public static FreeType equiv(String name, Type bound) {
        return new FreeType(name, bound);
    }
}