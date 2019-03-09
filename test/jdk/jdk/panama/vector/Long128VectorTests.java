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
 * @run testng Long128VectorTests
 *
 */

import jdk.panama.vector.Vector.Shape;
import jdk.panama.vector.Vector;

import jdk.panama.vector.LongVector;

import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.lang.Integer;
import java.util.List;
import java.util.Arrays;
import java.util.function.BiFunction;
import java.util.function.IntFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Test
public class Long128VectorTests extends AbstractVectorTest {

    static final LongVector.LongSpecies SPECIES =
                LongVector.species(Shape.S_128_BIT);

    static final int INVOC_COUNT = Integer.getInteger("jdk.panama.vector.test.loop-iterations", 100);

    interface FUnOp {
        long apply(long a);
    }

    static void assertArraysEquals(long[] a, long[] r, FUnOp f) {
        int i = 0;
        try {
            for (; i < a.length; i++) {
                Assert.assertEquals(r[i], f.apply(a[i]));
            }
        } catch (AssertionError e) {
            Assert.assertEquals(r[i], f.apply(a[i]), "at index #" + i + ", input = " + a[i]);
        }
    }

    static void assertArraysEquals(long[] a, long[] r, boolean[] mask, FUnOp f) {
        int i = 0;
        try {
            for (; i < a.length; i++) {
                Assert.assertEquals(r[i], mask[i % SPECIES.length()] ? f.apply(a[i]) : a[i]);
            }
        } catch (AssertionError e) {
            Assert.assertEquals(r[i], mask[i % SPECIES.length()] ? f.apply(a[i]) : a[i], "at index #" + i + ", input = " + a[i] + ", mask = " + mask[i % SPECIES.length()]);
        }
    }

    interface FReductionOp {
        long apply(long[] a, int idx);
    }

    static void assertReductionArraysEquals(long[] a, long[] b, FReductionOp f) {
        int i = 0;
        try {
            for (; i < a.length; i += SPECIES.length()) {
                Assert.assertEquals(b[i], f.apply(a, i));
            }
        } catch (AssertionError e) {
            Assert.assertEquals(b[i], f.apply(a, i), "at index #" + i);
        }
    }

    interface FBoolReductionOp {
        boolean apply(boolean[] a, int idx);
    }

    static void assertReductionBoolArraysEquals(boolean[] a, boolean[] b, FBoolReductionOp f) {
        int i = 0;
        try {
            for (; i < a.length; i += SPECIES.length()) {
                Assert.assertEquals(f.apply(a, i), b[i]);
            }
        } catch (AssertionError e) {
            Assert.assertEquals(f.apply(a, i), b[i], "at index #" + i);
        }
    }

    static void assertInsertArraysEquals(long[] a, long[] b, long element, int index) {
        int i = 0;
        try {
            for (; i < a.length; i += 1) {
                if(i%SPECIES.length() == index) {
                    Assert.assertEquals(b[i], element);
                } else {
                    Assert.assertEquals(b[i], a[i]);
                }
            }
        } catch (AssertionError e) {
            if (i%SPECIES.length() == index) {
                Assert.assertEquals(b[i], element, "at index #" + i);
            } else {
                Assert.assertEquals(b[i], a[i], "at index #" + i);
            }
        }
    }

    static void assertRearrangeArraysEquals(long[] a, long[] r, int[] order, int vector_len) {
        int i = 0, j = 0;
        try {
            for (; i < a.length; i += vector_len) {
                for (j = 0; j < vector_len; j++) {
                    Assert.assertEquals(r[i+j], a[i+order[i+j]]);
                }
            }
        } catch (AssertionError e) {
            int idx = i + j;
            Assert.assertEquals(r[i+j], a[i+order[i+j]], "at index #" + idx + ", input = " + a[i+order[i+j]]);
        }
    }

    interface FBinOp {
        long apply(long a, long b);
    }

    interface FBinMaskOp {
        long apply(long a, long b, boolean m);

        static FBinMaskOp lift(FBinOp f) {
            return (a, b, m) -> m ? f.apply(a, b) : a;
        }
    }

    static void assertArraysEquals(long[] a, long[] b, long[] r, FBinOp f) {
        int i = 0;
        try {
            for (; i < a.length; i++) {
                Assert.assertEquals(r[i], f.apply(a[i], b[i]));
            }
        } catch (AssertionError e) {
            Assert.assertEquals(f.apply(a[i], b[i]), r[i], "(" + a[i] + ", " + b[i] + ") at index #" + i);
        }
    }

    static void assertArraysEquals(long[] a, long[] b, long[] r, boolean[] mask, FBinOp f) {
        assertArraysEquals(a, b, r, mask, FBinMaskOp.lift(f));
    }

    static void assertArraysEquals(long[] a, long[] b, long[] r, boolean[] mask, FBinMaskOp f) {
        int i = 0;
        try {
            for (; i < a.length; i++) {
                Assert.assertEquals(r[i], f.apply(a[i], b[i], mask[i % SPECIES.length()]));
            }
        } catch (AssertionError err) {
            Assert.assertEquals(r[i], f.apply(a[i], b[i], mask[i % SPECIES.length()]), "at index #" + i + ", input1 = " + a[i] + ", input2 = " + b[i] + ", mask = " + mask[i % SPECIES.length()]);
        }
    }

    static void assertShiftArraysEquals(long[] a, long[] b, long[] r, FBinOp f) {
        int i = 0;
        int j = 0;
        try {
            for (; j < a.length; j += SPECIES.length()) {
              for (i = 0; i < SPECIES.length(); i++) {
                Assert.assertEquals(f.apply(a[i+j], b[j]), r[i+j]);
              }
            }
        } catch (AssertionError e) {
            Assert.assertEquals(f.apply(a[i+j], b[j]), r[i+j], "at index #" + i + ", " + j);
        }
    }

    static void assertShiftArraysEquals(long[] a, long[] b, long[] r, boolean[] mask, FBinOp f) {
        assertShiftArraysEquals(a, b, r, mask, FBinMaskOp.lift(f));
    }

    static void assertShiftArraysEquals(long[] a, long[] b, long[] r, boolean[] mask, FBinMaskOp f) {
        int i = 0;
        int j = 0;
        try {
            for (; j < a.length; j += SPECIES.length()) {
              for (i = 0; i < SPECIES.length(); i++) {
                Assert.assertEquals(r[i+j], f.apply(a[i+j], b[j], mask[i]));
              }
            }
        } catch (AssertionError err) {
            Assert.assertEquals(r[i+j], f.apply(a[i+j], b[j], mask[i]), "at index #" + i + ", input1 = " + a[i+j] + ", input2 = " + b[j] + ", mask = " + mask[i]);
        }
    }


    interface FBinArrayOp {
        long apply(long[] a, int b);
    }

    static void assertArraysEquals(long[] a, long[] r, FBinArrayOp f) {
        int i = 0;
        try {
            for (; i < a.length; i++) {
                Assert.assertEquals(f.apply(a, i), r[i]);
            }
        } catch (AssertionError e) {
            Assert.assertEquals(f.apply(a,i), r[i], "at index #" + i);
        }
    }
    interface FGatherScatterOp {
        long[] apply(long[] a, int ix, int[] b, int iy);
    }

    static void assertArraysEquals(long[] a, int[] b, long[] r, FGatherScatterOp f) {
        int i = 0;
        try {
            for (; i < a.length; i += SPECIES.length()) {
                Assert.assertEquals(Arrays.copyOfRange(r, i, i+SPECIES.length()),
                  f.apply(a, i, b, i));
            }
        } catch (AssertionError e) {
            long[] ref = f.apply(a, i, b, i);
            long[] res = Arrays.copyOfRange(r, i, i+SPECIES.length());
            Assert.assertEquals(ref, res,
              "(ref: " + Arrays.toString(ref) + ", res: " + Arrays.toString(res) + ", a: "
              + Arrays.toString(Arrays.copyOfRange(a, i, i+SPECIES.length()))
              + ", b: "
              + Arrays.toString(Arrays.copyOfRange(b, i, i+SPECIES.length()))
              + " at index #" + i);
        }
    }


    static final List<IntFunction<long[]>> LONG_GENERATORS = List.of(
            withToString("long[-i * 5]", (int s) -> {
                return fill(s * 1000,
                            i -> (long)(-i * 5));
            }),
            withToString("long[i * 5]", (int s) -> {
                return fill(s * 1000,
                            i -> (long)(i * 5));
            }),
            withToString("long[i + 1]", (int s) -> {
                return fill(s * 1000,
                            i -> (((long)(i + 1) == 0) ? 1 : (long)(i + 1)));
            }),
            withToString("long[cornerCaseValue(i)]", (int s) -> {
                return fill(s * 1000,
                            i -> cornerCaseValue(i));
            })
    );

    // Create combinations of pairs
    // @@@ Might be sensitive to order e.g. div by 0
    static final List<List<IntFunction<long[]>>> LONG_GENERATOR_PAIRS =
        Stream.of(LONG_GENERATORS.get(0)).
                flatMap(fa -> LONG_GENERATORS.stream().skip(1).map(fb -> List.of(fa, fb))).
                collect(Collectors.toList());

    @DataProvider
    public Object[][] boolUnaryOpProvider() {
        return BOOL_ARRAY_GENERATORS.stream().
                map(f -> new Object[]{f}).
                toArray(Object[][]::new);
    }


    @DataProvider
    public Object[][] longBinaryOpProvider() {
        return LONG_GENERATOR_PAIRS.stream().map(List::toArray).
                toArray(Object[][]::new);
    }

    @DataProvider
    public Object[][] longIndexedOpProvider() {
        return LONG_GENERATOR_PAIRS.stream().map(List::toArray).
                toArray(Object[][]::new);
    }

    @DataProvider
    public Object[][] longBinaryOpMaskProvider() {
        return BOOLEAN_MASK_GENERATORS.stream().
                flatMap(fm -> LONG_GENERATOR_PAIRS.stream().map(lfa -> {
                    return Stream.concat(lfa.stream(), Stream.of(fm)).toArray();
                })).
                toArray(Object[][]::new);
    }


    @DataProvider
    public Object[][] longUnaryOpProvider() {
        return LONG_GENERATORS.stream().
                map(f -> new Object[]{f}).
                toArray(Object[][]::new);
    }

    @DataProvider
    public Object[][] longUnaryOpMaskProvider() {
        return BOOLEAN_MASK_GENERATORS.stream().
                flatMap(fm -> LONG_GENERATORS.stream().map(fa -> {
                    return new Object[] {fa, fm};
                })).
                toArray(Object[][]::new);
    }

    @DataProvider
    public Object[][] longUnaryOpShuffleProvider() {
        return INT_SHUFFLE_GENERATORS.stream().
                flatMap(fs -> LONG_GENERATORS.stream().map(fa -> {
                    return new Object[] {fa, fs};
                })).
                toArray(Object[][]::new);
    }

    @DataProvider
    public Object[][] longUnaryOpIndexProvider() {
        return INT_INDEX_GENERATORS.stream().
                flatMap(fs -> LONG_GENERATORS.stream().map(fa -> {
                    return new Object[] {fa, fs};
                })).
                toArray(Object[][]::new);
    }


    static final List<IntFunction<long[]>> LONG_COMPARE_GENERATORS = List.of(
            withToString("long[i]", (int s) -> {
                return fill(s * 1000,
                            i -> (long)i);
            }),
            withToString("long[i + 1]", (int s) -> {
                return fill(s * 1000,
                            i -> (long)(i + 1));
            }),
            withToString("long[i - 2]", (int s) -> {
                return fill(s * 1000,
                            i -> (long)(i - 2));
            }),
            withToString("long[zigZag(i)]", (int s) -> {
                return fill(s * 1000,
                            i -> i%3 == 0 ? (long)i : (i%3 == 1 ? (long)(i + 1) : (long)(i - 2)));
            }),
            withToString("long[cornerCaseValue(i)]", (int s) -> {
                return fill(s * 1000,
                            i -> cornerCaseValue(i));
            })
    );

    static final List<List<IntFunction<long[]>>> LONG_COMPARE_GENERATOR_PAIRS =
        LONG_COMPARE_GENERATORS.stream().
                flatMap(fa -> LONG_COMPARE_GENERATORS.stream().map(fb -> List.of(fa, fb))).
                collect(Collectors.toList());

    @DataProvider
    public Object[][] longCompareOpProvider() {
        return LONG_COMPARE_GENERATOR_PAIRS.stream().map(List::toArray).
                toArray(Object[][]::new);
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

    static long cornerCaseValue(int i) {
        switch(i % 5) {
            case 0:
                return Long.MAX_VALUE;
            case 1:
                return Long.MIN_VALUE;
            case 2:
                return Long.MIN_VALUE;
            case 3:
                return Long.MAX_VALUE;
            default:
                return (long)0;
        }
    }
   static long get(long[] a, int i) {
       return (long) a[i];
   }

   static final IntFunction<long[]> fr = (vl) -> {
        int length = 1000 * vl;
        return new long[length];
    };

    static final IntFunction<boolean[]> fmr = (vl) -> {
        int length = 1000 * vl;
        return new boolean[length];
    };

    static long add(long a, long b) {
        return (long)(a + b);
    }

    @Test(dataProvider = "longBinaryOpProvider")
    static void addLong128VectorTests(IntFunction<long[]> fa, IntFunction<long[]> fb) {
        long[] a = fa.apply(SPECIES.length());
        long[] b = fb.apply(SPECIES.length());
        long[] r = fr.apply(SPECIES.length());

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                LongVector av = SPECIES.fromArray(a, i);
                LongVector bv = SPECIES.fromArray(b, i);
                av.add(bv).intoArray(r, i);
            }
        }

        assertArraysEquals(a, b, r, Long128VectorTests::add);
    }

    @Test(dataProvider = "longBinaryOpMaskProvider")
    static void addLong128VectorTests(IntFunction<long[]> fa, IntFunction<long[]> fb,
                                          IntFunction<boolean[]> fm) {
        long[] a = fa.apply(SPECIES.length());
        long[] b = fb.apply(SPECIES.length());
        long[] r = fr.apply(SPECIES.length());
        boolean[] mask = fm.apply(SPECIES.length());
        Vector.Mask<Long> vmask = SPECIES.maskFromValues(mask);

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                LongVector av = SPECIES.fromArray(a, i);
                LongVector bv = SPECIES.fromArray(b, i);
                av.add(bv, vmask).intoArray(r, i);
            }
        }

        assertArraysEquals(a, b, r, mask, Long128VectorTests::add);
    }
    static long sub(long a, long b) {
        return (long)(a - b);
    }

    @Test(dataProvider = "longBinaryOpProvider")
    static void subLong128VectorTests(IntFunction<long[]> fa, IntFunction<long[]> fb) {
        long[] a = fa.apply(SPECIES.length());
        long[] b = fb.apply(SPECIES.length());
        long[] r = fr.apply(SPECIES.length());

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                LongVector av = SPECIES.fromArray(a, i);
                LongVector bv = SPECIES.fromArray(b, i);
                av.sub(bv).intoArray(r, i);
            }
        }

        assertArraysEquals(a, b, r, Long128VectorTests::sub);
    }

    @Test(dataProvider = "longBinaryOpMaskProvider")
    static void subLong128VectorTests(IntFunction<long[]> fa, IntFunction<long[]> fb,
                                          IntFunction<boolean[]> fm) {
        long[] a = fa.apply(SPECIES.length());
        long[] b = fb.apply(SPECIES.length());
        long[] r = fr.apply(SPECIES.length());
        boolean[] mask = fm.apply(SPECIES.length());
        Vector.Mask<Long> vmask = SPECIES.maskFromValues(mask);

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                LongVector av = SPECIES.fromArray(a, i);
                LongVector bv = SPECIES.fromArray(b, i);
                av.sub(bv, vmask).intoArray(r, i);
            }
        }

        assertArraysEquals(a, b, r, mask, Long128VectorTests::sub);
    }


    static long mul(long a, long b) {
        return (long)(a * b);
    }

    @Test(dataProvider = "longBinaryOpProvider")
    static void mulLong128VectorTests(IntFunction<long[]> fa, IntFunction<long[]> fb) {
        long[] a = fa.apply(SPECIES.length());
        long[] b = fb.apply(SPECIES.length());
        long[] r = fr.apply(SPECIES.length());

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                LongVector av = SPECIES.fromArray(a, i);
                LongVector bv = SPECIES.fromArray(b, i);
                av.mul(bv).intoArray(r, i);
            }
        }

        assertArraysEquals(a, b, r, Long128VectorTests::mul);
    }

    @Test(dataProvider = "longBinaryOpMaskProvider")
    static void mulLong128VectorTests(IntFunction<long[]> fa, IntFunction<long[]> fb,
                                          IntFunction<boolean[]> fm) {
        long[] a = fa.apply(SPECIES.length());
        long[] b = fb.apply(SPECIES.length());
        long[] r = fr.apply(SPECIES.length());
        boolean[] mask = fm.apply(SPECIES.length());
        Vector.Mask<Long> vmask = SPECIES.maskFromValues(mask);

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                LongVector av = SPECIES.fromArray(a, i);
                LongVector bv = SPECIES.fromArray(b, i);
                av.mul(bv, vmask).intoArray(r, i);
            }
        }

        assertArraysEquals(a, b, r, mask, Long128VectorTests::mul);
    }

    static long and(long a, long b) {
        return (long)(a & b);
    }

    @Test(dataProvider = "longBinaryOpProvider")
    static void andLong128VectorTests(IntFunction<long[]> fa, IntFunction<long[]> fb) {
        long[] a = fa.apply(SPECIES.length());
        long[] b = fb.apply(SPECIES.length());
        long[] r = fr.apply(SPECIES.length());

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                LongVector av = SPECIES.fromArray(a, i);
                LongVector bv = SPECIES.fromArray(b, i);
                av.and(bv).intoArray(r, i);
            }
        }

        assertArraysEquals(a, b, r, Long128VectorTests::and);
    }



    @Test(dataProvider = "longBinaryOpMaskProvider")
    static void andLong128VectorTests(IntFunction<long[]> fa, IntFunction<long[]> fb,
                                          IntFunction<boolean[]> fm) {
        long[] a = fa.apply(SPECIES.length());
        long[] b = fb.apply(SPECIES.length());
        long[] r = fr.apply(SPECIES.length());
        boolean[] mask = fm.apply(SPECIES.length());
        Vector.Mask<Long> vmask = SPECIES.maskFromValues(mask);

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                LongVector av = SPECIES.fromArray(a, i);
                LongVector bv = SPECIES.fromArray(b, i);
                av.and(bv, vmask).intoArray(r, i);
            }
        }

        assertArraysEquals(a, b, r, mask, Long128VectorTests::and);
    }


    static long or(long a, long b) {
        return (long)(a | b);
    }

    @Test(dataProvider = "longBinaryOpProvider")
    static void orLong128VectorTests(IntFunction<long[]> fa, IntFunction<long[]> fb) {
        long[] a = fa.apply(SPECIES.length());
        long[] b = fb.apply(SPECIES.length());
        long[] r = fr.apply(SPECIES.length());

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                LongVector av = SPECIES.fromArray(a, i);
                LongVector bv = SPECIES.fromArray(b, i);
                av.or(bv).intoArray(r, i);
            }
        }

        assertArraysEquals(a, b, r, Long128VectorTests::or);
    }



    @Test(dataProvider = "longBinaryOpMaskProvider")
    static void orLong128VectorTests(IntFunction<long[]> fa, IntFunction<long[]> fb,
                                          IntFunction<boolean[]> fm) {
        long[] a = fa.apply(SPECIES.length());
        long[] b = fb.apply(SPECIES.length());
        long[] r = fr.apply(SPECIES.length());
        boolean[] mask = fm.apply(SPECIES.length());
        Vector.Mask<Long> vmask = SPECIES.maskFromValues(mask);

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                LongVector av = SPECIES.fromArray(a, i);
                LongVector bv = SPECIES.fromArray(b, i);
                av.or(bv, vmask).intoArray(r, i);
            }
        }

        assertArraysEquals(a, b, r, mask, Long128VectorTests::or);
    }


    static long xor(long a, long b) {
        return (long)(a ^ b);
    }

    @Test(dataProvider = "longBinaryOpProvider")
    static void xorLong128VectorTests(IntFunction<long[]> fa, IntFunction<long[]> fb) {
        long[] a = fa.apply(SPECIES.length());
        long[] b = fb.apply(SPECIES.length());
        long[] r = fr.apply(SPECIES.length());

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                LongVector av = SPECIES.fromArray(a, i);
                LongVector bv = SPECIES.fromArray(b, i);
                av.xor(bv).intoArray(r, i);
            }
        }

        assertArraysEquals(a, b, r, Long128VectorTests::xor);
    }



    @Test(dataProvider = "longBinaryOpMaskProvider")
    static void xorLong128VectorTests(IntFunction<long[]> fa, IntFunction<long[]> fb,
                                          IntFunction<boolean[]> fm) {
        long[] a = fa.apply(SPECIES.length());
        long[] b = fb.apply(SPECIES.length());
        long[] r = fr.apply(SPECIES.length());
        boolean[] mask = fm.apply(SPECIES.length());
        Vector.Mask<Long> vmask = SPECIES.maskFromValues(mask);

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                LongVector av = SPECIES.fromArray(a, i);
                LongVector bv = SPECIES.fromArray(b, i);
                av.xor(bv, vmask).intoArray(r, i);
            }
        }

        assertArraysEquals(a, b, r, mask, Long128VectorTests::xor);
    }


    static long shiftR(long a, long b) {
        return (long)((a >>> b));
    }

    @Test(dataProvider = "longBinaryOpProvider")
    static void shiftRLong128VectorTests(IntFunction<long[]> fa, IntFunction<long[]> fb) {
        long[] a = fa.apply(SPECIES.length());
        long[] b = fb.apply(SPECIES.length());
        long[] r = fr.apply(SPECIES.length());

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                LongVector av = SPECIES.fromArray(a, i);
                LongVector bv = SPECIES.fromArray(b, i);
                av.shiftR(bv).intoArray(r, i);
            }
        }

        assertArraysEquals(a, b, r, Long128VectorTests::shiftR);
    }



    @Test(dataProvider = "longBinaryOpMaskProvider")
    static void shiftRLong128VectorTests(IntFunction<long[]> fa, IntFunction<long[]> fb,
                                          IntFunction<boolean[]> fm) {
        long[] a = fa.apply(SPECIES.length());
        long[] b = fb.apply(SPECIES.length());
        long[] r = fr.apply(SPECIES.length());
        boolean[] mask = fm.apply(SPECIES.length());
        Vector.Mask<Long> vmask = SPECIES.maskFromValues(mask);

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                LongVector av = SPECIES.fromArray(a, i);
                LongVector bv = SPECIES.fromArray(b, i);
                av.shiftR(bv, vmask).intoArray(r, i);
            }
        }

        assertArraysEquals(a, b, r, mask, Long128VectorTests::shiftR);
    }


    static long shiftL(long a, long b) {
        return (long)((a << b));
    }

    @Test(dataProvider = "longBinaryOpProvider")
    static void shiftLLong128VectorTests(IntFunction<long[]> fa, IntFunction<long[]> fb) {
        long[] a = fa.apply(SPECIES.length());
        long[] b = fb.apply(SPECIES.length());
        long[] r = fr.apply(SPECIES.length());

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                LongVector av = SPECIES.fromArray(a, i);
                LongVector bv = SPECIES.fromArray(b, i);
                av.shiftL(bv).intoArray(r, i);
            }
        }

        assertArraysEquals(a, b, r, Long128VectorTests::shiftL);
    }



    @Test(dataProvider = "longBinaryOpMaskProvider")
    static void shiftLLong128VectorTests(IntFunction<long[]> fa, IntFunction<long[]> fb,
                                          IntFunction<boolean[]> fm) {
        long[] a = fa.apply(SPECIES.length());
        long[] b = fb.apply(SPECIES.length());
        long[] r = fr.apply(SPECIES.length());
        boolean[] mask = fm.apply(SPECIES.length());
        Vector.Mask<Long> vmask = SPECIES.maskFromValues(mask);

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                LongVector av = SPECIES.fromArray(a, i);
                LongVector bv = SPECIES.fromArray(b, i);
                av.shiftL(bv, vmask).intoArray(r, i);
            }
        }

        assertArraysEquals(a, b, r, mask, Long128VectorTests::shiftL);
    }


    static long aShiftR(long a, long b) {
        return (long)((a >> b));
    }

    @Test(dataProvider = "longBinaryOpProvider")
    static void aShiftRLong128VectorTests(IntFunction<long[]> fa, IntFunction<long[]> fb) {
        long[] a = fa.apply(SPECIES.length());
        long[] b = fb.apply(SPECIES.length());
        long[] r = fr.apply(SPECIES.length());

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                LongVector av = SPECIES.fromArray(a, i);
                LongVector bv = SPECIES.fromArray(b, i);
                av.aShiftR(bv).intoArray(r, i);
            }
        }

        assertArraysEquals(a, b, r, Long128VectorTests::aShiftR);
    }



    @Test(dataProvider = "longBinaryOpMaskProvider")
    static void aShiftRLong128VectorTests(IntFunction<long[]> fa, IntFunction<long[]> fb,
                                          IntFunction<boolean[]> fm) {
        long[] a = fa.apply(SPECIES.length());
        long[] b = fb.apply(SPECIES.length());
        long[] r = fr.apply(SPECIES.length());
        boolean[] mask = fm.apply(SPECIES.length());
        Vector.Mask<Long> vmask = SPECIES.maskFromValues(mask);

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                LongVector av = SPECIES.fromArray(a, i);
                LongVector bv = SPECIES.fromArray(b, i);
                av.aShiftR(bv, vmask).intoArray(r, i);
            }
        }

        assertArraysEquals(a, b, r, mask, Long128VectorTests::aShiftR);
    }


    static long aShiftR_unary(long a, long b) {
        return (long)((a >> b));
    }

    @Test(dataProvider = "longBinaryOpProvider")
    static void aShiftRLong128VectorTestsShift(IntFunction<long[]> fa, IntFunction<long[]> fb) {
        long[] a = fa.apply(SPECIES.length());
        long[] b = fb.apply(SPECIES.length());
        long[] r = fr.apply(SPECIES.length());

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                LongVector av = SPECIES.fromArray(a, i);
                av.aShiftR((int)b[i]).intoArray(r, i);
            }
        }

        assertShiftArraysEquals(a, b, r, Long128VectorTests::aShiftR_unary);
    }



    @Test(dataProvider = "longBinaryOpMaskProvider")
    static void aShiftRLong128VectorTestsShift(IntFunction<long[]> fa, IntFunction<long[]> fb,
                                          IntFunction<boolean[]> fm) {
        long[] a = fa.apply(SPECIES.length());
        long[] b = fb.apply(SPECIES.length());
        long[] r = fr.apply(SPECIES.length());
        boolean[] mask = fm.apply(SPECIES.length());
        Vector.Mask<Long> vmask = SPECIES.maskFromValues(mask);

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                LongVector av = SPECIES.fromArray(a, i);
                av.aShiftR((int)b[i], vmask).intoArray(r, i);
            }
        }

        assertShiftArraysEquals(a, b, r, mask, Long128VectorTests::aShiftR_unary);
    }


    static long shiftR_unary(long a, long b) {
        return (long)((a >>> b));
    }

    @Test(dataProvider = "longBinaryOpProvider")
    static void shiftRLong128VectorTestsShift(IntFunction<long[]> fa, IntFunction<long[]> fb) {
        long[] a = fa.apply(SPECIES.length());
        long[] b = fb.apply(SPECIES.length());
        long[] r = fr.apply(SPECIES.length());

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                LongVector av = SPECIES.fromArray(a, i);
                av.shiftR((int)b[i]).intoArray(r, i);
            }
        }

        assertShiftArraysEquals(a, b, r, Long128VectorTests::shiftR_unary);
    }



    @Test(dataProvider = "longBinaryOpMaskProvider")
    static void shiftRLong128VectorTestsShift(IntFunction<long[]> fa, IntFunction<long[]> fb,
                                          IntFunction<boolean[]> fm) {
        long[] a = fa.apply(SPECIES.length());
        long[] b = fb.apply(SPECIES.length());
        long[] r = fr.apply(SPECIES.length());
        boolean[] mask = fm.apply(SPECIES.length());
        Vector.Mask<Long> vmask = SPECIES.maskFromValues(mask);

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                LongVector av = SPECIES.fromArray(a, i);
                av.shiftR((int)b[i], vmask).intoArray(r, i);
            }
        }

        assertShiftArraysEquals(a, b, r, mask, Long128VectorTests::shiftR_unary);
    }


    static long shiftL_unary(long a, long b) {
        return (long)((a << b));
    }

    @Test(dataProvider = "longBinaryOpProvider")
    static void shiftLLong128VectorTestsShift(IntFunction<long[]> fa, IntFunction<long[]> fb) {
        long[] a = fa.apply(SPECIES.length());
        long[] b = fb.apply(SPECIES.length());
        long[] r = fr.apply(SPECIES.length());

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                LongVector av = SPECIES.fromArray(a, i);
                av.shiftL((int)b[i]).intoArray(r, i);
            }
        }

        assertShiftArraysEquals(a, b, r, Long128VectorTests::shiftL_unary);
    }



    @Test(dataProvider = "longBinaryOpMaskProvider")
    static void shiftLLong128VectorTestsShift(IntFunction<long[]> fa, IntFunction<long[]> fb,
                                          IntFunction<boolean[]> fm) {
        long[] a = fa.apply(SPECIES.length());
        long[] b = fb.apply(SPECIES.length());
        long[] r = fr.apply(SPECIES.length());
        boolean[] mask = fm.apply(SPECIES.length());
        Vector.Mask<Long> vmask = SPECIES.maskFromValues(mask);

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                LongVector av = SPECIES.fromArray(a, i);
                av.shiftL((int)b[i], vmask).intoArray(r, i);
            }
        }

        assertShiftArraysEquals(a, b, r, mask, Long128VectorTests::shiftL_unary);
    }

    static long max(long a, long b) {
        return (long)((a > b) ? a : b);
    }

    @Test(dataProvider = "longBinaryOpProvider")
    static void maxLong128VectorTests(IntFunction<long[]> fa, IntFunction<long[]> fb) {
        long[] a = fa.apply(SPECIES.length());
        long[] b = fb.apply(SPECIES.length());
        long[] r = fr.apply(SPECIES.length());

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                LongVector av = SPECIES.fromArray(a, i);
                LongVector bv = SPECIES.fromArray(b, i);
                av.max(bv).intoArray(r, i);
            }
        }

        assertArraysEquals(a, b, r, Long128VectorTests::max);
    }
    static long min(long a, long b) {
        return (long)((a < b) ? a : b);
    }

    @Test(dataProvider = "longBinaryOpProvider")
    static void minLong128VectorTests(IntFunction<long[]> fa, IntFunction<long[]> fb) {
        long[] a = fa.apply(SPECIES.length());
        long[] b = fb.apply(SPECIES.length());
        long[] r = fr.apply(SPECIES.length());

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                LongVector av = SPECIES.fromArray(a, i);
                LongVector bv = SPECIES.fromArray(b, i);
                av.min(bv).intoArray(r, i);
            }
        }

        assertArraysEquals(a, b, r, Long128VectorTests::min);
    }

    static long andAll(long[] a, int idx) {
        long res = -1;
        for (int i = idx; i < (idx + SPECIES.length()); i++) {
          res &= a[i];
        }

        return res;
    }


    @Test(dataProvider = "longUnaryOpProvider")
    static void andAllLong128VectorTests(IntFunction<long[]> fa) {
        long[] a = fa.apply(SPECIES.length());
        long[] r = fr.apply(SPECIES.length());

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
              LongVector av = SPECIES.fromArray(a, i);
              r[i] = av.andAll();
            }
        }

        assertReductionArraysEquals(a, r, Long128VectorTests::andAll);
    }


    static long orAll(long[] a, int idx) {
        long res = 0;
        for (int i = idx; i < (idx + SPECIES.length()); i++) {
          res |= a[i];
        }

        return res;
    }


    @Test(dataProvider = "longUnaryOpProvider")
    static void orAllLong128VectorTests(IntFunction<long[]> fa) {
        long[] a = fa.apply(SPECIES.length());
        long[] r = fr.apply(SPECIES.length());

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
              LongVector av = SPECIES.fromArray(a, i);
              r[i] = av.orAll();
            }
        }

        assertReductionArraysEquals(a, r, Long128VectorTests::orAll);
    }


    static long xorAll(long[] a, int idx) {
        long res = 0;
        for (int i = idx; i < (idx + SPECIES.length()); i++) {
          res ^= a[i];
        }

        return res;
    }


    @Test(dataProvider = "longUnaryOpProvider")
    static void xorAllLong128VectorTests(IntFunction<long[]> fa) {
        long[] a = fa.apply(SPECIES.length());
        long[] r = fr.apply(SPECIES.length());

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
              LongVector av = SPECIES.fromArray(a, i);
              r[i] = av.xorAll();
            }
        }

        assertReductionArraysEquals(a, r, Long128VectorTests::xorAll);
    }

    static long addAll(long[] a, int idx) {
        long res = 0;
        for (int i = idx; i < (idx + SPECIES.length()); i++) {
          res += a[i];
        }

        return res;
    }
    @Test(dataProvider = "longUnaryOpProvider")
    static void addAllLong128VectorTests(IntFunction<long[]> fa) {
        long[] a = fa.apply(SPECIES.length());
        long[] r = fr.apply(SPECIES.length());

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
              LongVector av = SPECIES.fromArray(a, i);
              r[i] = av.addAll();
            }
        }

        assertReductionArraysEquals(a, r, Long128VectorTests::addAll);
    }
    static long subAll(long[] a, int idx) {
        long res = 0;
        for (int i = idx; i < (idx + SPECIES.length()); i++) {
          res -= a[i];
        }

        return res;
    }
    @Test(dataProvider = "longUnaryOpProvider")
    static void subAllLong128VectorTests(IntFunction<long[]> fa) {
        long[] a = fa.apply(SPECIES.length());
        long[] r = fr.apply(SPECIES.length());

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
              LongVector av = SPECIES.fromArray(a, i);
              r[i] = av.subAll();
            }
        }

        assertReductionArraysEquals(a, r, Long128VectorTests::subAll);
    }
    static long mulAll(long[] a, int idx) {
        long res = 1;
        for (int i = idx; i < (idx + SPECIES.length()); i++) {
          res *= a[i];
        }

        return res;
    }
    @Test(dataProvider = "longUnaryOpProvider")
    static void mulAllLong128VectorTests(IntFunction<long[]> fa) {
        long[] a = fa.apply(SPECIES.length());
        long[] r = fr.apply(SPECIES.length());

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
              LongVector av = SPECIES.fromArray(a, i);
              r[i] = av.mulAll();
            }
        }

        assertReductionArraysEquals(a, r, Long128VectorTests::mulAll);
    }
    static long minAll(long[] a, int idx) {
        long res = Long.MAX_VALUE;
        for (int i = idx; i < (idx + SPECIES.length()); i++) {
            res = (res < a[i])?res:a[i];
        }

        return res;
    }
    @Test(dataProvider = "longUnaryOpProvider")
    static void minAllLong128VectorTests(IntFunction<long[]> fa) {
        long[] a = fa.apply(SPECIES.length());
        long[] r = fr.apply(SPECIES.length());

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
              LongVector av = SPECIES.fromArray(a, i);
              r[i] = av.minAll();
            }
        }

        assertReductionArraysEquals(a, r, Long128VectorTests::minAll);
    }
    static long maxAll(long[] a, int idx) {
        long res = Long.MIN_VALUE;
        for (int i = idx; i < (idx + SPECIES.length()); i++) {
          res = (res > a[i])?res:a[i];
        }

        return res;
    }
    @Test(dataProvider = "longUnaryOpProvider")
    static void maxAllLong128VectorTests(IntFunction<long[]> fa) {
        long[] a = fa.apply(SPECIES.length());
        long[] r = fr.apply(SPECIES.length());

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
              LongVector av = SPECIES.fromArray(a, i);
              r[i] = av.maxAll();
            }
        }

        assertReductionArraysEquals(a, r, Long128VectorTests::maxAll);
    }

    static boolean anyTrue(boolean[] a, int idx) {
        boolean res = false;
        for (int i = idx; i < (idx + SPECIES.length()); i++) {
          res |= a[i];
        }

        return res;
    }


    @Test(dataProvider = "boolUnaryOpProvider")
    static void anyTrueLong128VectorTests(IntFunction<boolean[]> fm) {
        boolean[] mask = fm.apply(SPECIES.length());
        boolean[] r = fmr.apply(SPECIES.length());

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < mask.length; i += SPECIES.length()) {
              Vector.Mask<Long> vmask = SPECIES.maskFromArray(mask, i);
              r[i] = vmask.anyTrue();
            }
        }

        assertReductionBoolArraysEquals(mask, r, Long128VectorTests::anyTrue);
    }


    static boolean allTrue(boolean[] a, int idx) {
        boolean res = true;
        for (int i = idx; i < (idx + SPECIES.length()); i++) {
          res &= a[i];
        }

        return res;
    }


    @Test(dataProvider = "boolUnaryOpProvider")
    static void allTrueLong128VectorTests(IntFunction<boolean[]> fm) {
        boolean[] mask = fm.apply(SPECIES.length());
        boolean[] r = fmr.apply(SPECIES.length());

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < mask.length; i += SPECIES.length()) {
              Vector.Mask<Long> vmask = SPECIES.maskFromArray(mask, i);
              r[i] = vmask.allTrue();
            }
        }

        assertReductionBoolArraysEquals(mask, r, Long128VectorTests::allTrue);
    }


    @Test(dataProvider = "longUnaryOpProvider")
    static void withLong128VectorTests(IntFunction<long []> fa) {
        long[] a = fa.apply(SPECIES.length());
        long[] r = fr.apply(SPECIES.length());

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
              LongVector av = SPECIES.fromArray(a, i);
              av.with(0, (long)4).intoArray(r, i);
            }
        }

        assertInsertArraysEquals(a, r, (long)4, 0);
    }

    @Test(dataProvider = "longCompareOpProvider")
    static void lessThanLong128VectorTests(IntFunction<long[]> fa, IntFunction<long[]> fb) {
        long[] a = fa.apply(SPECIES.length());
        long[] b = fb.apply(SPECIES.length());

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                LongVector av = SPECIES.fromArray(a, i);
                LongVector bv = SPECIES.fromArray(b, i);
                Vector.Mask<Long> mv = av.lessThan(bv);

                // Check results as part of computation.
                for (int j = 0; j < SPECIES.length(); j++) {
                    Assert.assertEquals(mv.getElement(j), a[i + j] < b[i + j]);
                }
            }
        }
    }


    @Test(dataProvider = "longCompareOpProvider")
    static void greaterThanLong128VectorTests(IntFunction<long[]> fa, IntFunction<long[]> fb) {
        long[] a = fa.apply(SPECIES.length());
        long[] b = fb.apply(SPECIES.length());

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                LongVector av = SPECIES.fromArray(a, i);
                LongVector bv = SPECIES.fromArray(b, i);
                Vector.Mask<Long> mv = av.greaterThan(bv);

                // Check results as part of computation.
                for (int j = 0; j < SPECIES.length(); j++) {
                    Assert.assertEquals(mv.getElement(j), a[i + j] > b[i + j]);
                }
            }
        }
    }


    @Test(dataProvider = "longCompareOpProvider")
    static void equalLong128VectorTests(IntFunction<long[]> fa, IntFunction<long[]> fb) {
        long[] a = fa.apply(SPECIES.length());
        long[] b = fb.apply(SPECIES.length());

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                LongVector av = SPECIES.fromArray(a, i);
                LongVector bv = SPECIES.fromArray(b, i);
                Vector.Mask<Long> mv = av.equal(bv);

                // Check results as part of computation.
                for (int j = 0; j < SPECIES.length(); j++) {
                    Assert.assertEquals(mv.getElement(j), a[i + j] == b[i + j]);
                }
            }
        }
    }


    @Test(dataProvider = "longCompareOpProvider")
    static void notEqualLong128VectorTests(IntFunction<long[]> fa, IntFunction<long[]> fb) {
        long[] a = fa.apply(SPECIES.length());
        long[] b = fb.apply(SPECIES.length());

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                LongVector av = SPECIES.fromArray(a, i);
                LongVector bv = SPECIES.fromArray(b, i);
                Vector.Mask<Long> mv = av.notEqual(bv);

                // Check results as part of computation.
                for (int j = 0; j < SPECIES.length(); j++) {
                    Assert.assertEquals(mv.getElement(j), a[i + j] != b[i + j]);
                }
            }
        }
    }


    @Test(dataProvider = "longCompareOpProvider")
    static void lessThanEqLong128VectorTests(IntFunction<long[]> fa, IntFunction<long[]> fb) {
        long[] a = fa.apply(SPECIES.length());
        long[] b = fb.apply(SPECIES.length());

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                LongVector av = SPECIES.fromArray(a, i);
                LongVector bv = SPECIES.fromArray(b, i);
                Vector.Mask<Long> mv = av.lessThanEq(bv);

                // Check results as part of computation.
                for (int j = 0; j < SPECIES.length(); j++) {
                    Assert.assertEquals(mv.getElement(j), a[i + j] <= b[i + j]);
                }
            }
        }
    }


    @Test(dataProvider = "longCompareOpProvider")
    static void greaterThanEqLong128VectorTests(IntFunction<long[]> fa, IntFunction<long[]> fb) {
        long[] a = fa.apply(SPECIES.length());
        long[] b = fb.apply(SPECIES.length());

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                LongVector av = SPECIES.fromArray(a, i);
                LongVector bv = SPECIES.fromArray(b, i);
                Vector.Mask<Long> mv = av.greaterThanEq(bv);

                // Check results as part of computation.
                for (int j = 0; j < SPECIES.length(); j++) {
                    Assert.assertEquals(mv.getElement(j), a[i + j] >= b[i + j]);
                }
            }
        }
    }


    static long blend(long a, long b, boolean mask) {
        return mask ? b : a;
    }

    @Test(dataProvider = "longBinaryOpMaskProvider")
    static void blendLong128VectorTests(IntFunction<long[]> fa, IntFunction<long[]> fb,
                                          IntFunction<boolean[]> fm) {
        long[] a = fa.apply(SPECIES.length());
        long[] b = fb.apply(SPECIES.length());
        long[] r = fr.apply(SPECIES.length());
        boolean[] mask = fm.apply(SPECIES.length());
        Vector.Mask<Long> vmask = SPECIES.maskFromValues(mask);

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                LongVector av = SPECIES.fromArray(a, i);
                LongVector bv = SPECIES.fromArray(b, i);
                av.blend(bv, vmask).intoArray(r, i);
            }
        }

        assertArraysEquals(a, b, r, mask, Long128VectorTests::blend);
    }

    @Test(dataProvider = "longUnaryOpShuffleProvider")
    static void RearrangeLong128VectorTests(IntFunction<long[]> fa,
                                           BiFunction<Integer,Integer,int[]> fs) {
        long[] a = fa.apply(SPECIES.length());
        int[] order = fs.apply(a.length, SPECIES.length());
        long[] r = fr.apply(SPECIES.length());

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                LongVector av = SPECIES.fromArray(a, i);
                av.rearrange(SPECIES.shuffleFromArray(order, i)).intoArray(r, i);
            }
        }

        assertRearrangeArraysEquals(a, r, order, SPECIES.length());
    }




    @Test(dataProvider = "longUnaryOpProvider")
    static void getLong128VectorTests(IntFunction<long[]> fa) {
        long[] a = fa.apply(SPECIES.length());
        long[] r = fr.apply(SPECIES.length());

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                LongVector av = SPECIES.fromArray(a, i);
                int num_lanes = SPECIES.length();
                // Manually unroll because full unroll happens after intrinsification.
                // Unroll is needed because get intrinsic requires for index to be a known constant.
                if (num_lanes == 1) {
                    r[i]=av.get(0);
                } else if (num_lanes == 2) {
                    r[i]=av.get(0);
                    r[i+1]=av.get(1);
                } else if (num_lanes == 4) {
                    r[i]=av.get(0);
                    r[i+1]=av.get(1);
                    r[i+2]=av.get(2);
                    r[i+3]=av.get(3);
                } else if (num_lanes == 8) {
                    r[i]=av.get(0);
                    r[i+1]=av.get(1);
                    r[i+2]=av.get(2);
                    r[i+3]=av.get(3);
                    r[i+4]=av.get(4);
                    r[i+5]=av.get(5);
                    r[i+6]=av.get(6);
                    r[i+7]=av.get(7);
                } else if (num_lanes == 16) {
                    r[i]=av.get(0);
                    r[i+1]=av.get(1);
                    r[i+2]=av.get(2);
                    r[i+3]=av.get(3);
                    r[i+4]=av.get(4);
                    r[i+5]=av.get(5);
                    r[i+6]=av.get(6);
                    r[i+7]=av.get(7);
                    r[i+8]=av.get(8);
                    r[i+9]=av.get(9);
                    r[i+10]=av.get(10);
                    r[i+11]=av.get(11);
                    r[i+12]=av.get(12);
                    r[i+13]=av.get(13);
                    r[i+14]=av.get(14);
                    r[i+15]=av.get(15);
                } else if (num_lanes == 32) {
                    r[i]=av.get(0);
                    r[i+1]=av.get(1);
                    r[i+2]=av.get(2);
                    r[i+3]=av.get(3);
                    r[i+4]=av.get(4);
                    r[i+5]=av.get(5);
                    r[i+6]=av.get(6);
                    r[i+7]=av.get(7);
                    r[i+8]=av.get(8);
                    r[i+9]=av.get(9);
                    r[i+10]=av.get(10);
                    r[i+11]=av.get(11);
                    r[i+12]=av.get(12);
                    r[i+13]=av.get(13);
                    r[i+14]=av.get(14);
                    r[i+15]=av.get(15);
                    r[i+16]=av.get(16);
                    r[i+17]=av.get(17);
                    r[i+18]=av.get(18);
                    r[i+19]=av.get(19);
                    r[i+20]=av.get(20);
                    r[i+21]=av.get(21);
                    r[i+22]=av.get(22);
                    r[i+23]=av.get(23);
                    r[i+24]=av.get(24);
                    r[i+25]=av.get(25);
                    r[i+26]=av.get(26);
                    r[i+27]=av.get(27);
                    r[i+28]=av.get(28);
                    r[i+29]=av.get(29);
                    r[i+30]=av.get(30);
                    r[i+31]=av.get(31);
                } else if (num_lanes == 64) {
                    r[i]=av.get(0);
                    r[i+1]=av.get(1);
                    r[i+2]=av.get(2);
                    r[i+3]=av.get(3);
                    r[i+4]=av.get(4);
                    r[i+5]=av.get(5);
                    r[i+6]=av.get(6);
                    r[i+7]=av.get(7);
                    r[i+8]=av.get(8);
                    r[i+9]=av.get(9);
                    r[i+10]=av.get(10);
                    r[i+11]=av.get(11);
                    r[i+12]=av.get(12);
                    r[i+13]=av.get(13);
                    r[i+14]=av.get(14);
                    r[i+15]=av.get(15);
                    r[i+16]=av.get(16);
                    r[i+17]=av.get(17);
                    r[i+18]=av.get(18);
                    r[i+19]=av.get(19);
                    r[i+20]=av.get(20);
                    r[i+21]=av.get(21);
                    r[i+22]=av.get(22);
                    r[i+23]=av.get(23);
                    r[i+24]=av.get(24);
                    r[i+25]=av.get(25);
                    r[i+26]=av.get(26);
                    r[i+27]=av.get(27);
                    r[i+28]=av.get(28);
                    r[i+29]=av.get(29);
                    r[i+30]=av.get(30);
                    r[i+31]=av.get(31);
                    r[i+32]=av.get(32);
                    r[i+33]=av.get(33);
                    r[i+34]=av.get(34);
                    r[i+35]=av.get(35);
                    r[i+36]=av.get(36);
                    r[i+37]=av.get(37);
                    r[i+38]=av.get(38);
                    r[i+39]=av.get(39);
                    r[i+40]=av.get(40);
                    r[i+41]=av.get(41);
                    r[i+42]=av.get(42);
                    r[i+43]=av.get(43);
                    r[i+44]=av.get(44);
                    r[i+45]=av.get(45);
                    r[i+46]=av.get(46);
                    r[i+47]=av.get(47);
                    r[i+48]=av.get(48);
                    r[i+49]=av.get(49);
                    r[i+50]=av.get(50);
                    r[i+51]=av.get(51);
                    r[i+52]=av.get(52);
                    r[i+53]=av.get(53);
                    r[i+54]=av.get(54);
                    r[i+55]=av.get(55);
                    r[i+56]=av.get(56);
                    r[i+57]=av.get(57);
                    r[i+58]=av.get(58);
                    r[i+59]=av.get(59);
                    r[i+60]=av.get(60);
                    r[i+61]=av.get(61);
                    r[i+62]=av.get(62);
                    r[i+63]=av.get(63);
                } else {
                    for (int j = 0; j < SPECIES.length(); j++) {
                        r[i+j]=av.get(j);
                    }
                }
            }
        }

        assertArraysEquals(a, r, Long128VectorTests::get);
    }






















    static long neg(long a) {
        return (long)(-((long)a));
    }

    @Test(dataProvider = "longUnaryOpProvider")
    static void negLong128VectorTests(IntFunction<long[]> fa) {
        long[] a = fa.apply(SPECIES.length());
        long[] r = fr.apply(SPECIES.length());

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                LongVector av = SPECIES.fromArray(a, i);
                av.neg().intoArray(r, i);
            }
        }

        assertArraysEquals(a, r, Long128VectorTests::neg);
    }

    @Test(dataProvider = "longUnaryOpMaskProvider")
    static void negMaskedLong128VectorTests(IntFunction<long[]> fa,
                                                IntFunction<boolean[]> fm) {
        long[] a = fa.apply(SPECIES.length());
        long[] r = fr.apply(SPECIES.length());
        boolean[] mask = fm.apply(SPECIES.length());
        Vector.Mask<Long> vmask = SPECIES.maskFromValues(mask);

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                LongVector av = SPECIES.fromArray(a, i);
                av.neg(vmask).intoArray(r, i);
            }
        }

        assertArraysEquals(a, r, mask, Long128VectorTests::neg);
    }





    static long abs(long a) {
        return (long)(Math.abs((long)a));
    }

    @Test(dataProvider = "longUnaryOpProvider")
    static void absLong128VectorTests(IntFunction<long[]> fa) {
        long[] a = fa.apply(SPECIES.length());
        long[] r = fr.apply(SPECIES.length());

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                LongVector av = SPECIES.fromArray(a, i);
                av.abs().intoArray(r, i);
            }
        }

        assertArraysEquals(a, r, Long128VectorTests::abs);
    }

    @Test(dataProvider = "longUnaryOpMaskProvider")
    static void absMaskedLong128VectorTests(IntFunction<long[]> fa,
                                                IntFunction<boolean[]> fm) {
        long[] a = fa.apply(SPECIES.length());
        long[] r = fr.apply(SPECIES.length());
        boolean[] mask = fm.apply(SPECIES.length());
        Vector.Mask<Long> vmask = SPECIES.maskFromValues(mask);

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                LongVector av = SPECIES.fromArray(a, i);
                av.abs(vmask).intoArray(r, i);
            }
        }

        assertArraysEquals(a, r, mask, Long128VectorTests::abs);
    }






    static long not(long a) {
        return (long)(~((long)a));
    }



    @Test(dataProvider = "longUnaryOpProvider")
    static void notLong128VectorTests(IntFunction<long[]> fa) {
        long[] a = fa.apply(SPECIES.length());
        long[] r = fr.apply(SPECIES.length());

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                LongVector av = SPECIES.fromArray(a, i);
                av.not().intoArray(r, i);
            }
        }

        assertArraysEquals(a, r, Long128VectorTests::not);
    }



    @Test(dataProvider = "longUnaryOpMaskProvider")
    static void notMaskedLong128VectorTests(IntFunction<long[]> fa,
                                                IntFunction<boolean[]> fm) {
        long[] a = fa.apply(SPECIES.length());
        long[] r = fr.apply(SPECIES.length());
        boolean[] mask = fm.apply(SPECIES.length());
        Vector.Mask<Long> vmask = SPECIES.maskFromValues(mask);

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                LongVector av = SPECIES.fromArray(a, i);
                av.not(vmask).intoArray(r, i);
            }
        }

        assertArraysEquals(a, r, mask, Long128VectorTests::not);
    }









    static long[] gather(long a[], int ix, int[] b, int iy) {
      long[] res = new long[SPECIES.length()];
      for (int i = 0; i < SPECIES.length(); i++) {
        int bi = iy + i;
        res[i] = a[b[bi] + ix];
      }
      return res;
    }

    @Test(dataProvider = "longUnaryOpIndexProvider")
    static void gatherLong128VectorTests(IntFunction<long[]> fa, BiFunction<Integer,Integer,int[]> fs) {
        long[] a = fa.apply(SPECIES.length());
        int[] b    = fs.apply(a.length, SPECIES.length());
        long[] r = new long[a.length];

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                LongVector av = SPECIES.fromArray(a, i, b, i);
                av.intoArray(r, i);
            }
        }

        assertArraysEquals(a, b, r, Long128VectorTests::gather);
    }


    static long[] scatter(long a[], int ix, int[] b, int iy) {
      long[] res = new long[SPECIES.length()];
      for (int i = 0; i < SPECIES.length(); i++) {
        int bi = iy + i;
        res[b[bi]] = a[i + ix];
      }
      return res;
    }

    @Test(dataProvider = "longUnaryOpIndexProvider")
    static void scatterLong128VectorTests(IntFunction<long[]> fa, BiFunction<Integer,Integer,int[]> fs) {
        long[] a = fa.apply(SPECIES.length());
        int[] b = fs.apply(a.length, SPECIES.length());
        long[] r = new long[a.length];

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                LongVector av = SPECIES.fromArray(a, i);
                av.intoArray(r, i, b, i);
            }
        }

        assertArraysEquals(a, b, r, Long128VectorTests::scatter);
    }

}

