/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have
 * questions.
 */

package benchmark.jdk.panama.vector;

import jdk.panama.vector.ByteVector;
import jdk.panama.vector.ByteVector.ByteSpecies;
import jdk.panama.vector.IntVector;
import jdk.panama.vector.IntVector.IntSpecies;
import jdk.panama.vector.ShortVector;
import jdk.panama.vector.ShortVector.ShortSpecies;
import jdk.panama.vector.LongVector;
import jdk.panama.vector.LongVector.LongSpecies;
import jdk.panama.vector.Vector;
import jdk.panama.vector.Vector.Shape;
import jdk.panama.vector.Vector.Species;

import java.util.Random;
import java.util.function.IntFunction;

public class AbstractVectorBenchmark {
    static final Random RANDOM = new Random(Integer.getInteger("jdk.panama.vector.random-seed", 1337));

    static final ByteSpecies B64  = ByteVector.species(Shape.S_64_BIT);
    static final ByteSpecies B128 = ByteVector.species(Shape.S_128_BIT);
    static final ByteSpecies B256 = ByteVector.species(Shape.S_256_BIT);
    static final ByteSpecies B512 = ByteVector.species(Shape.S_512_BIT);

    static final ShortSpecies S64  = ShortVector.species(Shape.S_64_BIT);
    static final ShortSpecies S128 = ShortVector.species(Shape.S_128_BIT);
    static final ShortSpecies S256 = ShortVector.species(Shape.S_256_BIT);
    static final ShortSpecies S512 = ShortVector.species(Shape.S_512_BIT);

    static final IntSpecies I64  = IntVector.species(Vector.Shape.S_64_BIT);
    static final IntSpecies I128 = IntVector.species(Vector.Shape.S_128_BIT);
    static final IntSpecies I256 = IntVector.species(Vector.Shape.S_256_BIT);
    static final IntSpecies I512 = IntVector.species(Vector.Shape.S_512_BIT);

    static final LongSpecies L64  = LongVector.species(Vector.Shape.S_64_BIT);
    static final LongSpecies L128 = LongVector.species(Vector.Shape.S_128_BIT);
    static final LongSpecies L256 = LongVector.species(Vector.Shape.S_256_BIT);
    static final LongSpecies L512 = LongVector.species(Vector.Shape.S_512_BIT);

    static Shape widen(Shape s) {
        switch (s) {
            case S_64_BIT:  return Shape.S_128_BIT;
            case S_128_BIT: return Shape.S_256_BIT;
            case S_256_BIT: return Shape.S_512_BIT;
            default: throw new IllegalArgumentException("" + s);
        }
    }

    static Shape narrow(Shape s) {
        switch (s) {
            case S_512_BIT: return Shape.S_256_BIT;
            case S_256_BIT: return Shape.S_128_BIT;
            case S_128_BIT: return Shape.S_64_BIT;
            default: throw new IllegalArgumentException("" + s);
        }
    }

    static <E> Species<E> widen(Species<E> s) {
        return Vector.species(s.elementType(), widen(s.shape()));
    }

    static <E> Species<E> narrow(Species<E> s) {
        return Vector.species(s.elementType(), narrow(s.shape()));
    }

    static IntVector join(IntVector.IntSpecies from, IntVector.IntSpecies to, IntVector lo, IntVector hi) {
        assert 2 * from.length() == to.length();

        int vlen = from.length();
        var lo_mask = mask(from, to, 0);

        var v1 = lo.resize(to);
        var v2 = hi.resize(to).shiftER(vlen);
        var r = v2.blend(v1, lo_mask);
        return r;
    }

    static Vector.Mask<Integer> mask(IntVector.IntSpecies from, IntVector.IntSpecies to, int i) {
        int vlen = from.length();
        var v1 = from.broadcast(1);    //                         [1 1 ... 1]
        var v2 = v1.resize(to);        // [0 0 ... 0 |   ...     | 1 1 ... 1]
        var v3 = v2.shiftER(i * vlen); // [0 0 ... 0 | 1 1 ... 1 | 0 0 ... 0]
        return v3.notEqual(0);         // [F F ... F | T T ... T | F F ... F]
    }

    static <E> IntVector sum(ByteVector va) {
        IntSpecies species = IntVector.species(va.shape());
        var acc = species.zero();
        int limit = va.length() / species.length();
        for (int k = 0; k < limit; k++) {
            var vb = species.cast(va.shiftEL(k * B64.length()).resize(B64)).and(0xFF);
            acc = acc.add(vb);
        }
        return acc;
    }

    /* ============================================================================================================== */

    boolean[] fillMask(int size, IntFunction<Boolean> f) {
        boolean[] array = new boolean[size];
        for (int i = 0; i < array.length; i++) {
            array[i] = f.apply(i);
        }
        return array;
    }

    byte[] fillByte(int size, IntFunction<Byte> f) {
        byte[] array = new byte[size];
        for (int i = 0; i < size; i++) {
            array[i] = f.apply(i);
        }
        return array;
    }

    int[] fillInt(int size, IntFunction<Integer> f) {
        int[] array = new int[size];
        for (int i = 0; i < array.length; i++) {
            array[i] = f.apply(i);
        }
        return array;
    }

    long[] fillLong(int size, IntFunction<Long> f) {
        long[] array = new long[size];
        for (int i = 0; i < array.length; i++) {
            array[i] = f.apply(i);
        }
        return array;
    }
}
