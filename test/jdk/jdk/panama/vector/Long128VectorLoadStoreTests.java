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

/*
 * @test
 * @modules jdk.panama.vector
 * @run testng Long128VectorLoadStoreTests
 *
 */

import jdk.panama.vector.Vector.Shape;
import jdk.panama.vector.Vector;

import jdk.panama.vector.LongVector;

import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.nio.ByteBuffer;
import java.nio.LongBuffer;
import java.nio.ByteOrder;
import java.util.List;
import java.util.function.IntFunction;

@Test
public class Long128VectorLoadStoreTests extends AbstractVectorTest {
    static final LongVector.LongSpecies SPECIES =
                LongVector.species(Shape.S_128_BIT);

    static final int INVOC_COUNT = Integer.getInteger("jdk.panama.vector.test.loop-iterations", 10);

    static void assertArraysEquals(long[] a, long[] r, boolean[] mask) {
        int i = 0;
        try {
            for (; i < a.length; i++) {
                Assert.assertEquals(mask[i % SPECIES.length()] ? a[i] : (long) 0, r[i]);
            }
        } catch (AssertionError e) {
            Assert.assertEquals(mask[i % SPECIES.length()] ? a[i] : (long) 0, r[i], "at index #" + i);
        }
    }

    static void assertArraysEquals(long[] a, long[] r, int[] im) {
        int i = 0;
        try {
            for (; i < a.length; i++) {
                Assert.assertEquals(a[im[i]], r[i]);
            }
        } catch (AssertionError e) {
            Assert.assertEquals(a[im[i]], r[i], "at index #" + i);
        }
    }

    static void assertArraysEquals(long[] a, long[] r, int[] im, boolean[] mask) {
        int i = 0;
        try {
            for (; i < a.length; i++) {
                Assert.assertEquals(mask[i % SPECIES.length()] ? a[im[i]] : (long) 0, r[i]);
            }
        } catch (AssertionError e) {
            Assert.assertEquals(mask[i % SPECIES.length()] ? a[im[i]] : (long) 0, r[i], "at index #" + i);
        }
    }

    static final List<IntFunction<long[]>> LONG_GENERATORS = List.of(
            withToString("long[i * 5]", (int s) -> {
                return fill(s * 1000,
                            i -> (long)(i * 5));
            }),
            withToString("long[i + 1]", (int s) -> {
                return fill(s * 1000,
                            i -> (((long)(i + 1) == 0) ? 1 : (long)(i + 1)));
            })
    );

    @DataProvider
    public Object[][] longProvider() {
        return LONG_GENERATORS.stream().
                map(f -> new Object[]{f}).
                toArray(Object[][]::new);
    }

    @DataProvider
    public Object[][] longMaskProvider() {
        return BOOLEAN_MASK_GENERATORS.stream().
                flatMap(fm -> LONG_GENERATORS.stream().map(fa -> {
                    return new Object[] {fa, fm};
                })).
                toArray(Object[][]::new);
    }

    @DataProvider
    public Object[][] longIndexMapProvider() {
        return INDEX_GENERATORS.stream().
                flatMap(fim -> LONG_GENERATORS.stream().map(fa -> {
                    return new Object[] {fa, fim};
                })).
                toArray(Object[][]::new);
    }

    @DataProvider
    public Object[][] longIndexMapMaskProvider() {
        return BOOLEAN_MASK_GENERATORS.stream().
                flatMap(fm -> INDEX_GENERATORS.stream().
                    flatMap(fim -> LONG_GENERATORS.stream().map(fa -> {
                        return new Object[] {fa, fim, fm};
                }))).
                toArray(Object[][]::new);
    }

    @DataProvider
    public Object[][] longByteBufferProvider() {
        return LONG_GENERATORS.stream().
                flatMap(fa -> BYTE_BUFFER_GENERATORS.stream().map(fb -> {
                    return new Object[]{fa, fb};
                })).
                toArray(Object[][]::new);
    }

    @DataProvider
    public Object[][] longByteBufferMaskProvider() {
        return BOOLEAN_MASK_GENERATORS.stream().
                flatMap(fm -> LONG_GENERATORS.stream().
                        flatMap(fa -> BYTE_BUFFER_GENERATORS.stream().map(fb -> {
                            return new Object[]{fa, fb, fm};
                        }))).
                toArray(Object[][]::new);
    }

    static ByteBuffer toBuffer(long[] a, IntFunction<ByteBuffer> fb) {
        ByteBuffer bb = fb.apply(a.length * SPECIES.elementSize() / 8);
        for (long v : a) {
            bb.putLong(v);
        }
        return bb.clear();
    }

    static long[] bufferToArray(ByteBuffer bb) {
        LongBuffer db = bb.asLongBuffer();
        long[] d = new long[db.capacity()];
        db.get(d);
        return d;
    }

    interface ToLongF {
        long apply(int i);
    }

    static long[] fill(int s , ToLongF f) {
        return fill(new long[s], f);
    }

    static long[] fill(long[] a, ToLongF f) {
        for (int i = 0; i < a.length; i++) {
            a[i] = f.apply(i);
        }
        return a;
    }

    @Test(dataProvider = "longProvider")
    static void loadStoreArray(IntFunction<long[]> fa) {
        long[] a = fa.apply(SPECIES.length());
        long[] r = new long[a.length];

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                LongVector av = SPECIES.fromArray(a, i);
                av.intoArray(r, i);
            }
        }
        Assert.assertEquals(a, r);
    }

    @Test(dataProvider = "longMaskProvider")
    static void loadStoreMaskArray(IntFunction<long[]> fa,
                                   IntFunction<boolean[]> fm) {
        long[] a = fa.apply(SPECIES.length());
        long[] r = new long[a.length];
        boolean[] mask = fm.apply(SPECIES.length());
        Vector.Mask<Long> vmask = SPECIES.maskFromValues(mask);

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                LongVector av = SPECIES.fromArray(a, i, vmask);
                av.intoArray(r, i);
            }
        }
        assertArraysEquals(a, r, mask);

        r = new long[a.length];
        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                LongVector av = SPECIES.fromArray(a, i);
                av.intoArray(r, i, vmask);
            }
        }

        assertArraysEquals(a, r, mask);
    }


    @Test(dataProvider = "longByteBufferProvider")
    static void loadStoreByteBuffer(IntFunction<long[]> fa,
                                    IntFunction<ByteBuffer> fb) {
        ByteBuffer a = toBuffer(fa.apply(SPECIES.length()), fb);
        ByteBuffer r = fb.apply(a.limit());

        int l = a.limit();
        int s = SPECIES.length() * SPECIES.elementSize() / 8;

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < l; i += s) {
                LongVector av = SPECIES.fromByteBuffer(a, i);
                av.intoByteBuffer(r, i);
            }
        }
        Assert.assertEquals(a.position(), 0, "Input buffer position changed");
        Assert.assertEquals(a.limit(), l, "Input buffer limit changed");
        Assert.assertEquals(r.position(), 0, "Result buffer position changed");
        Assert.assertEquals(r.limit(), l, "Result buffer limit changed");
        Assert.assertEquals(a, r, "Buffers not equal");
    }

    @Test(dataProvider = "longByteBufferProvider")
    static void loadReadOnlyStoreByteBuffer(IntFunction<long[]> fa,
                                            IntFunction<ByteBuffer> fb) {
        ByteBuffer a = toBuffer(fa.apply(SPECIES.length()), fb);
        a = a.asReadOnlyBuffer().order(a.order());
        ByteBuffer r = fb.apply(a.limit());

        int l = a.limit();
        int s = SPECIES.length() * SPECIES.elementSize() / 8;

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < l; i += s) {
                LongVector av = SPECIES.fromByteBuffer(a, i);
                av.intoByteBuffer(r, i);
            }
        }
        Assert.assertEquals(a.position(), 0, "Input buffer position changed");
        Assert.assertEquals(a.limit(), l, "Input buffer limit changed");
        Assert.assertEquals(r.position(), 0, "Result buffer position changed");
        Assert.assertEquals(r.limit(), l, "Result buffer limit changed");
        Assert.assertEquals(a, r, "Buffers not equal");
    }

    @Test(dataProvider = "longByteBufferMaskProvider")
    static void loadStoreByteBufferMask(IntFunction<long[]> fa,
                                        IntFunction<ByteBuffer> fb,
                                        IntFunction<boolean[]> fm) {
        ByteBuffer a = toBuffer(fa.apply(SPECIES.length()), fb);
        ByteBuffer r = fb.apply(a.limit());
        boolean[] mask = fm.apply(SPECIES.length());
        Vector.Mask<Long> vmask = SPECIES.maskFromValues(mask);

        int l = a.limit();
        int s = SPECIES.length() * SPECIES.elementSize() / 8;

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < l; i += s) {
                LongVector av = SPECIES.fromByteBuffer(a, i, vmask);
                av.intoByteBuffer(r, i);
            }
        }
        Assert.assertEquals(a.position(), 0, "Input buffer position changed");
        Assert.assertEquals(a.limit(), l, "Input buffer limit changed");
        Assert.assertEquals(r.position(), 0, "Result buffer position changed");
        Assert.assertEquals(r.limit(), l, "Result buffer limit changed");
        assertArraysEquals(bufferToArray(a), bufferToArray(r), mask);

        a = toBuffer(fa.apply(SPECIES.length()), fb);
        r = fb.apply(a.limit());
        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < l; i += s) {
                LongVector av = SPECIES.fromByteBuffer(a, i);
                av.intoByteBuffer(r, i, vmask);
            }
        }
        Assert.assertEquals(a.position(), 0, "Input buffer position changed");
        Assert.assertEquals(a.limit(), l, "Input buffer limit changed");
        Assert.assertEquals(r.position(), 0, "Result buffer position changed");
        Assert.assertEquals(r.limit(), l, "Result buffer limit changed");
        assertArraysEquals(bufferToArray(a), bufferToArray(r), mask);
    }

    @Test(dataProvider = "longByteBufferMaskProvider")
    static void loadReadOnlyStoreByteBufferMask(IntFunction<long[]> fa,
                                                IntFunction<ByteBuffer> fb,
                                                IntFunction<boolean[]> fm) {
        ByteBuffer a = toBuffer(fa.apply(SPECIES.length()), fb);
        a = a.asReadOnlyBuffer().order(a.order());
        ByteBuffer r = fb.apply(a.limit());
        boolean[] mask = fm.apply(SPECIES.length());
        Vector.Mask<Long> vmask = SPECIES.maskFromValues(mask);

        int l = a.limit();
        int s = SPECIES.length() * SPECIES.elementSize() / 8;

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < l; i += s) {
                LongVector av = SPECIES.fromByteBuffer(a, i, vmask);
                av.intoByteBuffer(r, i);
            }
        }
        Assert.assertEquals(a.position(), 0, "Input buffer position changed");
        Assert.assertEquals(a.limit(), l, "Input buffer limit changed");
        Assert.assertEquals(r.position(), 0, "Result buffer position changed");
        Assert.assertEquals(r.limit(), l, "Result buffer limit changed");
        assertArraysEquals(bufferToArray(a), bufferToArray(r), mask);
    }
}
