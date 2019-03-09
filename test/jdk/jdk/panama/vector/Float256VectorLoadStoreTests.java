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
 * @run testng Float256VectorLoadStoreTests
 *
 */

import jdk.panama.vector.Vector.Shape;
import jdk.panama.vector.Vector;

import jdk.panama.vector.FloatVector;

import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.ByteOrder;
import java.util.List;
import java.util.function.IntFunction;

@Test
public class Float256VectorLoadStoreTests extends AbstractVectorTest {
    static final FloatVector.FloatSpecies SPECIES =
                FloatVector.species(Shape.S_256_BIT);

    static final int INVOC_COUNT = Integer.getInteger("jdk.panama.vector.test.loop-iterations", 10);

    static void assertArraysEquals(float[] a, float[] r, boolean[] mask) {
        int i = 0;
        try {
            for (; i < a.length; i++) {
                Assert.assertEquals(mask[i % SPECIES.length()] ? a[i] : (float) 0, r[i]);
            }
        } catch (AssertionError e) {
            Assert.assertEquals(mask[i % SPECIES.length()] ? a[i] : (float) 0, r[i], "at index #" + i);
        }
    }

    static void assertArraysEquals(float[] a, float[] r, int[] im) {
        int i = 0;
        try {
            for (; i < a.length; i++) {
                Assert.assertEquals(a[im[i]], r[i]);
            }
        } catch (AssertionError e) {
            Assert.assertEquals(a[im[i]], r[i], "at index #" + i);
        }
    }

    static void assertArraysEquals(float[] a, float[] r, int[] im, boolean[] mask) {
        int i = 0;
        try {
            for (; i < a.length; i++) {
                Assert.assertEquals(mask[i % SPECIES.length()] ? a[im[i]] : (float) 0, r[i]);
            }
        } catch (AssertionError e) {
            Assert.assertEquals(mask[i % SPECIES.length()] ? a[im[i]] : (float) 0, r[i], "at index #" + i);
        }
    }

    static final List<IntFunction<float[]>> FLOAT_GENERATORS = List.of(
            withToString("float[i * 5]", (int s) -> {
                return fill(s * 1000,
                            i -> (float)(i * 5));
            }),
            withToString("float[i + 1]", (int s) -> {
                return fill(s * 1000,
                            i -> (((float)(i + 1) == 0) ? 1 : (float)(i + 1)));
            })
    );

    @DataProvider
    public Object[][] floatProvider() {
        return FLOAT_GENERATORS.stream().
                map(f -> new Object[]{f}).
                toArray(Object[][]::new);
    }

    @DataProvider
    public Object[][] floatMaskProvider() {
        return BOOLEAN_MASK_GENERATORS.stream().
                flatMap(fm -> FLOAT_GENERATORS.stream().map(fa -> {
                    return new Object[] {fa, fm};
                })).
                toArray(Object[][]::new);
    }

    @DataProvider
    public Object[][] floatIndexMapProvider() {
        return INDEX_GENERATORS.stream().
                flatMap(fim -> FLOAT_GENERATORS.stream().map(fa -> {
                    return new Object[] {fa, fim};
                })).
                toArray(Object[][]::new);
    }

    @DataProvider
    public Object[][] floatIndexMapMaskProvider() {
        return BOOLEAN_MASK_GENERATORS.stream().
                flatMap(fm -> INDEX_GENERATORS.stream().
                    flatMap(fim -> FLOAT_GENERATORS.stream().map(fa -> {
                        return new Object[] {fa, fim, fm};
                }))).
                toArray(Object[][]::new);
    }

    @DataProvider
    public Object[][] floatByteBufferProvider() {
        return FLOAT_GENERATORS.stream().
                flatMap(fa -> BYTE_BUFFER_GENERATORS.stream().map(fb -> {
                    return new Object[]{fa, fb};
                })).
                toArray(Object[][]::new);
    }

    @DataProvider
    public Object[][] floatByteBufferMaskProvider() {
        return BOOLEAN_MASK_GENERATORS.stream().
                flatMap(fm -> FLOAT_GENERATORS.stream().
                        flatMap(fa -> BYTE_BUFFER_GENERATORS.stream().map(fb -> {
                            return new Object[]{fa, fb, fm};
                        }))).
                toArray(Object[][]::new);
    }

    static ByteBuffer toBuffer(float[] a, IntFunction<ByteBuffer> fb) {
        ByteBuffer bb = fb.apply(a.length * SPECIES.elementSize() / 8);
        for (float v : a) {
            bb.putFloat(v);
        }
        return bb.clear();
    }

    static float[] bufferToArray(ByteBuffer bb) {
        FloatBuffer db = bb.asFloatBuffer();
        float[] d = new float[db.capacity()];
        db.get(d);
        return d;
    }

    interface ToFloatF {
        float apply(int i);
    }

    static float[] fill(int s , ToFloatF f) {
        return fill(new float[s], f);
    }

    static float[] fill(float[] a, ToFloatF f) {
        for (int i = 0; i < a.length; i++) {
            a[i] = f.apply(i);
        }
        return a;
    }

    @Test(dataProvider = "floatProvider")
    static void loadStoreArray(IntFunction<float[]> fa) {
        float[] a = fa.apply(SPECIES.length());
        float[] r = new float[a.length];

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                FloatVector av = SPECIES.fromArray(a, i);
                av.intoArray(r, i);
            }
        }
        Assert.assertEquals(a, r);
    }

    @Test(dataProvider = "floatMaskProvider")
    static void loadStoreMaskArray(IntFunction<float[]> fa,
                                   IntFunction<boolean[]> fm) {
        float[] a = fa.apply(SPECIES.length());
        float[] r = new float[a.length];
        boolean[] mask = fm.apply(SPECIES.length());
        Vector.Mask<Float> vmask = SPECIES.maskFromValues(mask);

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                FloatVector av = SPECIES.fromArray(a, i, vmask);
                av.intoArray(r, i);
            }
        }
        assertArraysEquals(a, r, mask);

        r = new float[a.length];
        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                FloatVector av = SPECIES.fromArray(a, i);
                av.intoArray(r, i, vmask);
            }
        }

        assertArraysEquals(a, r, mask);
    }


    @Test(dataProvider = "floatByteBufferProvider")
    static void loadStoreByteBuffer(IntFunction<float[]> fa,
                                    IntFunction<ByteBuffer> fb) {
        ByteBuffer a = toBuffer(fa.apply(SPECIES.length()), fb);
        ByteBuffer r = fb.apply(a.limit());

        int l = a.limit();
        int s = SPECIES.length() * SPECIES.elementSize() / 8;

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < l; i += s) {
                FloatVector av = SPECIES.fromByteBuffer(a, i);
                av.intoByteBuffer(r, i);
            }
        }
        Assert.assertEquals(a.position(), 0, "Input buffer position changed");
        Assert.assertEquals(a.limit(), l, "Input buffer limit changed");
        Assert.assertEquals(r.position(), 0, "Result buffer position changed");
        Assert.assertEquals(r.limit(), l, "Result buffer limit changed");
        Assert.assertEquals(a, r, "Buffers not equal");
    }

    @Test(dataProvider = "floatByteBufferProvider")
    static void loadReadOnlyStoreByteBuffer(IntFunction<float[]> fa,
                                            IntFunction<ByteBuffer> fb) {
        ByteBuffer a = toBuffer(fa.apply(SPECIES.length()), fb);
        a = a.asReadOnlyBuffer().order(a.order());
        ByteBuffer r = fb.apply(a.limit());

        int l = a.limit();
        int s = SPECIES.length() * SPECIES.elementSize() / 8;

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < l; i += s) {
                FloatVector av = SPECIES.fromByteBuffer(a, i);
                av.intoByteBuffer(r, i);
            }
        }
        Assert.assertEquals(a.position(), 0, "Input buffer position changed");
        Assert.assertEquals(a.limit(), l, "Input buffer limit changed");
        Assert.assertEquals(r.position(), 0, "Result buffer position changed");
        Assert.assertEquals(r.limit(), l, "Result buffer limit changed");
        Assert.assertEquals(a, r, "Buffers not equal");
    }

    @Test(dataProvider = "floatByteBufferMaskProvider")
    static void loadStoreByteBufferMask(IntFunction<float[]> fa,
                                        IntFunction<ByteBuffer> fb,
                                        IntFunction<boolean[]> fm) {
        ByteBuffer a = toBuffer(fa.apply(SPECIES.length()), fb);
        ByteBuffer r = fb.apply(a.limit());
        boolean[] mask = fm.apply(SPECIES.length());
        Vector.Mask<Float> vmask = SPECIES.maskFromValues(mask);

        int l = a.limit();
        int s = SPECIES.length() * SPECIES.elementSize() / 8;

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < l; i += s) {
                FloatVector av = SPECIES.fromByteBuffer(a, i, vmask);
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
                FloatVector av = SPECIES.fromByteBuffer(a, i);
                av.intoByteBuffer(r, i, vmask);
            }
        }
        Assert.assertEquals(a.position(), 0, "Input buffer position changed");
        Assert.assertEquals(a.limit(), l, "Input buffer limit changed");
        Assert.assertEquals(r.position(), 0, "Result buffer position changed");
        Assert.assertEquals(r.limit(), l, "Result buffer limit changed");
        assertArraysEquals(bufferToArray(a), bufferToArray(r), mask);
    }

    @Test(dataProvider = "floatByteBufferMaskProvider")
    static void loadReadOnlyStoreByteBufferMask(IntFunction<float[]> fa,
                                                IntFunction<ByteBuffer> fb,
                                                IntFunction<boolean[]> fm) {
        ByteBuffer a = toBuffer(fa.apply(SPECIES.length()), fb);
        a = a.asReadOnlyBuffer().order(a.order());
        ByteBuffer r = fb.apply(a.limit());
        boolean[] mask = fm.apply(SPECIES.length());
        Vector.Mask<Float> vmask = SPECIES.maskFromValues(mask);

        int l = a.limit();
        int s = SPECIES.length() * SPECIES.elementSize() / 8;

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < l; i += s) {
                FloatVector av = SPECIES.fromByteBuffer(a, i, vmask);
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
