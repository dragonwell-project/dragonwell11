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
 * @run testng Float64VectorTests
 *
 */

import jdk.panama.vector.Vector.Shape;
import jdk.panama.vector.Vector;

import jdk.panama.vector.FloatVector;

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
public class Float64VectorTests extends AbstractVectorTest {

    static final FloatVector.FloatSpecies SPECIES =
                FloatVector.species(Shape.S_64_BIT);

    static final int INVOC_COUNT = Integer.getInteger("jdk.panama.vector.test.loop-iterations", 100);

    interface FUnOp {
        float apply(float a);
    }

    static void assertArraysEquals(float[] a, float[] r, FUnOp f) {
        int i = 0;
        try {
            for (; i < a.length; i++) {
                Assert.assertEquals(r[i], f.apply(a[i]));
            }
        } catch (AssertionError e) {
            Assert.assertEquals(r[i], f.apply(a[i]), "at index #" + i + ", input = " + a[i]);
        }
    }

    static void assertArraysEquals(float[] a, float[] r, boolean[] mask, FUnOp f) {
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
        float apply(float[] a, int idx);
    }

    static void assertReductionArraysEquals(float[] a, float[] b, FReductionOp f) {
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

    static void assertInsertArraysEquals(float[] a, float[] b, float element, int index) {
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

    static void assertRearrangeArraysEquals(float[] a, float[] r, int[] order, int vector_len) {
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
        float apply(float a, float b);
    }

    interface FBinMaskOp {
        float apply(float a, float b, boolean m);

        static FBinMaskOp lift(FBinOp f) {
            return (a, b, m) -> m ? f.apply(a, b) : a;
        }
    }

    static void assertArraysEquals(float[] a, float[] b, float[] r, FBinOp f) {
        int i = 0;
        try {
            for (; i < a.length; i++) {
                Assert.assertEquals(r[i], f.apply(a[i], b[i]));
            }
        } catch (AssertionError e) {
            Assert.assertEquals(f.apply(a[i], b[i]), r[i], "(" + a[i] + ", " + b[i] + ") at index #" + i);
        }
    }

    static void assertArraysEquals(float[] a, float[] b, float[] r, boolean[] mask, FBinOp f) {
        assertArraysEquals(a, b, r, mask, FBinMaskOp.lift(f));
    }

    static void assertArraysEquals(float[] a, float[] b, float[] r, boolean[] mask, FBinMaskOp f) {
        int i = 0;
        try {
            for (; i < a.length; i++) {
                Assert.assertEquals(r[i], f.apply(a[i], b[i], mask[i % SPECIES.length()]));
            }
        } catch (AssertionError err) {
            Assert.assertEquals(r[i], f.apply(a[i], b[i], mask[i % SPECIES.length()]), "at index #" + i + ", input1 = " + a[i] + ", input2 = " + b[i] + ", mask = " + mask[i % SPECIES.length()]);
        }
    }

    static void assertShiftArraysEquals(float[] a, float[] b, float[] r, FBinOp f) {
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

    static void assertShiftArraysEquals(float[] a, float[] b, float[] r, boolean[] mask, FBinOp f) {
        assertShiftArraysEquals(a, b, r, mask, FBinMaskOp.lift(f));
    }

    static void assertShiftArraysEquals(float[] a, float[] b, float[] r, boolean[] mask, FBinMaskOp f) {
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

    interface FTernOp {
        float apply(float a, float b, float c);
    }

    interface FTernMaskOp {
        float apply(float a, float b, float c, boolean m);

        static FTernMaskOp lift(FTernOp f) {
            return (a, b, c, m) -> m ? f.apply(a, b, c) : a;
        }
    }

    static void assertArraysEquals(float[] a, float[] b, float[] c, float[] r, FTernOp f) {
        int i = 0;
        try {
            for (; i < a.length; i++) {
                Assert.assertEquals(r[i], f.apply(a[i], b[i], c[i]));
            }
        } catch (AssertionError e) {
            Assert.assertEquals(r[i], f.apply(a[i], b[i], c[i]), "at index #" + i + ", input1 = " + a[i] + ", input2 = " + b[i] + ", input3 = " + c[i]);
        }
    }

    static void assertArraysEquals(float[] a, float[] b, float[] c, float[] r, boolean[] mask, FTernOp f) {
        assertArraysEquals(a, b, c, r, mask, FTernMaskOp.lift(f));
    }

    static void assertArraysEquals(float[] a, float[] b, float[] c, float[] r, boolean[] mask, FTernMaskOp f) {
        int i = 0;
        try {
            for (; i < a.length; i++) {
                Assert.assertEquals(r[i], f.apply(a[i], b[i], c[i], mask[i % SPECIES.length()]));
            }
        } catch (AssertionError err) {
            Assert.assertEquals(r[i], f.apply(a[i], b[i], c[i], mask[i % SPECIES.length()]), "at index #" + i + ", input1 = " + a[i] + ", input2 = "
              + b[i] + ", input3 = " + c[i] + ", mask = " + mask[i % SPECIES.length()]);
        }
    }

    static boolean isWithin1Ulp(float actual, float expected) {
        if (Float.isNaN(expected) && !Float.isNaN(actual)) {
            return false;
        } else if (!Float.isNaN(expected) && Float.isNaN(actual)) {
            return false;
        }

        float low = Math.nextDown(expected);
        float high = Math.nextUp(expected);

        if (Float.compare(low, expected) > 0) {
            return false;
        }

        if (Float.compare(high, expected) < 0) {
            return false;
        }

        return true;
    }

    static void assertArraysEqualsWithinOneUlp(float[] a, float[] r, FUnOp mathf, FUnOp strictmathf) {
        int i = 0;
        try {
            // Check that result is within 1 ulp of strict math or equivalent to math implementation.
            for (; i < a.length; i++) {
                Assert.assertTrue(Float.compare(r[i], mathf.apply(a[i])) == 0 ||
                                    isWithin1Ulp(r[i], strictmathf.apply(a[i])));
            }
        } catch (AssertionError e) {
            Assert.assertTrue(Float.compare(r[i], mathf.apply(a[i])) == 0, "at index #" + i + ", input = " + a[i] + ", actual = " + r[i] + ", expected = " + mathf.apply(a[i]));
            Assert.assertTrue(isWithin1Ulp(r[i], strictmathf.apply(a[i])), "at index #" + i + ", input = " + a[i] + ", actual = " + r[i] + ", expected (within 1 ulp) = " + strictmathf.apply(a[i]));
        }
    }

    static void assertArraysEqualsWithinOneUlp(float[] a, float[] b, float[] r, FBinOp mathf, FBinOp strictmathf) {
        int i = 0;
        try {
            // Check that result is within 1 ulp of strict math or equivalent to math implementation.
            for (; i < a.length; i++) {
                Assert.assertTrue(Float.compare(r[i], mathf.apply(a[i], b[i])) == 0 ||
                                    isWithin1Ulp(r[i], strictmathf.apply(a[i], b[i])));
            }
        } catch (AssertionError e) {
            Assert.assertTrue(Float.compare(r[i], mathf.apply(a[i], b[i])) == 0, "at index #" + i + ", input1 = " + a[i] + ", input2 = " + b[i] + ", actual = " + r[i] + ", expected = " + mathf.apply(a[i], b[i]));
            Assert.assertTrue(isWithin1Ulp(r[i], strictmathf.apply(a[i], b[i])), "at index #" + i + ", input1 = " + a[i] + ", input2 = " + b[i] + ", actual = " + r[i] + ", expected (within 1 ulp) = " + strictmathf.apply(a[i], b[i]));
        }
    }

    interface FBinArrayOp {
        float apply(float[] a, int b);
    }

    static void assertArraysEquals(float[] a, float[] r, FBinArrayOp f) {
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
        float[] apply(float[] a, int ix, int[] b, int iy);
    }

    static void assertArraysEquals(float[] a, int[] b, float[] r, FGatherScatterOp f) {
        int i = 0;
        try {
            for (; i < a.length; i += SPECIES.length()) {
                Assert.assertEquals(Arrays.copyOfRange(r, i, i+SPECIES.length()),
                  f.apply(a, i, b, i));
            }
        } catch (AssertionError e) {
            float[] ref = f.apply(a, i, b, i);
            float[] res = Arrays.copyOfRange(r, i, i+SPECIES.length());
            Assert.assertEquals(ref, res,
              "(ref: " + Arrays.toString(ref) + ", res: " + Arrays.toString(res) + ", a: "
              + Arrays.toString(Arrays.copyOfRange(a, i, i+SPECIES.length()))
              + ", b: "
              + Arrays.toString(Arrays.copyOfRange(b, i, i+SPECIES.length()))
              + " at index #" + i);
        }
    }


    static final List<IntFunction<float[]>> FLOAT_GENERATORS = List.of(
            withToString("float[-i * 5]", (int s) -> {
                return fill(s * 1000,
                            i -> (float)(-i * 5));
            }),
            withToString("float[i * 5]", (int s) -> {
                return fill(s * 1000,
                            i -> (float)(i * 5));
            }),
            withToString("float[i + 1]", (int s) -> {
                return fill(s * 1000,
                            i -> (((float)(i + 1) == 0) ? 1 : (float)(i + 1)));
            }),
            withToString("float[cornerCaseValue(i)]", (int s) -> {
                return fill(s * 1000,
                            i -> cornerCaseValue(i));
            })
    );

    // Create combinations of pairs
    // @@@ Might be sensitive to order e.g. div by 0
    static final List<List<IntFunction<float[]>>> FLOAT_GENERATOR_PAIRS =
        Stream.of(FLOAT_GENERATORS.get(0)).
                flatMap(fa -> FLOAT_GENERATORS.stream().skip(1).map(fb -> List.of(fa, fb))).
                collect(Collectors.toList());

    @DataProvider
    public Object[][] boolUnaryOpProvider() {
        return BOOL_ARRAY_GENERATORS.stream().
                map(f -> new Object[]{f}).
                toArray(Object[][]::new);
    }

    static final List<List<IntFunction<float[]>>> FLOAT_GENERATOR_TRIPLES =
        FLOAT_GENERATOR_PAIRS.stream().
                flatMap(pair -> FLOAT_GENERATORS.stream().map(f -> List.of(pair.get(0), pair.get(1), f))).
                collect(Collectors.toList());

    @DataProvider
    public Object[][] floatBinaryOpProvider() {
        return FLOAT_GENERATOR_PAIRS.stream().map(List::toArray).
                toArray(Object[][]::new);
    }

    @DataProvider
    public Object[][] floatIndexedOpProvider() {
        return FLOAT_GENERATOR_PAIRS.stream().map(List::toArray).
                toArray(Object[][]::new);
    }

    @DataProvider
    public Object[][] floatBinaryOpMaskProvider() {
        return BOOLEAN_MASK_GENERATORS.stream().
                flatMap(fm -> FLOAT_GENERATOR_PAIRS.stream().map(lfa -> {
                    return Stream.concat(lfa.stream(), Stream.of(fm)).toArray();
                })).
                toArray(Object[][]::new);
    }

    @DataProvider
    public Object[][] floatTernaryOpProvider() {
        return FLOAT_GENERATOR_TRIPLES.stream().map(List::toArray).
                toArray(Object[][]::new);
    }

    @DataProvider
    public Object[][] floatTernaryOpMaskProvider() {
        return BOOLEAN_MASK_GENERATORS.stream().
                flatMap(fm -> FLOAT_GENERATOR_TRIPLES.stream().map(lfa -> {
                    return Stream.concat(lfa.stream(), Stream.of(fm)).toArray();
                })).
                toArray(Object[][]::new);
    }

    @DataProvider
    public Object[][] floatUnaryOpProvider() {
        return FLOAT_GENERATORS.stream().
                map(f -> new Object[]{f}).
                toArray(Object[][]::new);
    }

    @DataProvider
    public Object[][] floatUnaryOpMaskProvider() {
        return BOOLEAN_MASK_GENERATORS.stream().
                flatMap(fm -> FLOAT_GENERATORS.stream().map(fa -> {
                    return new Object[] {fa, fm};
                })).
                toArray(Object[][]::new);
    }

    @DataProvider
    public Object[][] floatUnaryOpShuffleProvider() {
        return INT_SHUFFLE_GENERATORS.stream().
                flatMap(fs -> FLOAT_GENERATORS.stream().map(fa -> {
                    return new Object[] {fa, fs};
                })).
                toArray(Object[][]::new);
    }

    @DataProvider
    public Object[][] floatUnaryOpIndexProvider() {
        return INT_INDEX_GENERATORS.stream().
                flatMap(fs -> FLOAT_GENERATORS.stream().map(fa -> {
                    return new Object[] {fa, fs};
                })).
                toArray(Object[][]::new);
    }


    static final List<IntFunction<float[]>> FLOAT_COMPARE_GENERATORS = List.of(
            withToString("float[i]", (int s) -> {
                return fill(s * 1000,
                            i -> (float)i);
            }),
            withToString("float[i + 1]", (int s) -> {
                return fill(s * 1000,
                            i -> (float)(i + 1));
            }),
            withToString("float[i - 2]", (int s) -> {
                return fill(s * 1000,
                            i -> (float)(i - 2));
            }),
            withToString("float[zigZag(i)]", (int s) -> {
                return fill(s * 1000,
                            i -> i%3 == 0 ? (float)i : (i%3 == 1 ? (float)(i + 1) : (float)(i - 2)));
            }),
            withToString("float[cornerCaseValue(i)]", (int s) -> {
                return fill(s * 1000,
                            i -> cornerCaseValue(i));
            })
    );

    static final List<List<IntFunction<float[]>>> FLOAT_COMPARE_GENERATOR_PAIRS =
        FLOAT_COMPARE_GENERATORS.stream().
                flatMap(fa -> FLOAT_COMPARE_GENERATORS.stream().map(fb -> List.of(fa, fb))).
                collect(Collectors.toList());

    @DataProvider
    public Object[][] floatCompareOpProvider() {
        return FLOAT_COMPARE_GENERATOR_PAIRS.stream().map(List::toArray).
                toArray(Object[][]::new);
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

    static float cornerCaseValue(int i) {
        switch(i % 7) {
            case 0:
                return Float.MAX_VALUE;
            case 1:
                return Float.MIN_VALUE;
            case 2:
                return Float.NEGATIVE_INFINITY;
            case 3:
                return Float.POSITIVE_INFINITY;
            case 4:
                return Float.NaN;
            case 5:
                return (float)0.0;
            default:
                return (float)-0.0;
        }
    }
   static float get(float[] a, int i) {
       return (float) a[i];
   }

   static final IntFunction<float[]> fr = (vl) -> {
        int length = 1000 * vl;
        return new float[length];
    };

    static final IntFunction<boolean[]> fmr = (vl) -> {
        int length = 1000 * vl;
        return new boolean[length];
    };

    static float add(float a, float b) {
        return (float)(a + b);
    }

    @Test(dataProvider = "floatBinaryOpProvider")
    static void addFloat64VectorTests(IntFunction<float[]> fa, IntFunction<float[]> fb) {
        float[] a = fa.apply(SPECIES.length());
        float[] b = fb.apply(SPECIES.length());
        float[] r = fr.apply(SPECIES.length());

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                FloatVector av = SPECIES.fromArray(a, i);
                FloatVector bv = SPECIES.fromArray(b, i);
                av.add(bv).intoArray(r, i);
            }
        }

        assertArraysEquals(a, b, r, Float64VectorTests::add);
    }

    @Test(dataProvider = "floatBinaryOpMaskProvider")
    static void addFloat64VectorTests(IntFunction<float[]> fa, IntFunction<float[]> fb,
                                          IntFunction<boolean[]> fm) {
        float[] a = fa.apply(SPECIES.length());
        float[] b = fb.apply(SPECIES.length());
        float[] r = fr.apply(SPECIES.length());
        boolean[] mask = fm.apply(SPECIES.length());
        Vector.Mask<Float> vmask = SPECIES.maskFromValues(mask);

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                FloatVector av = SPECIES.fromArray(a, i);
                FloatVector bv = SPECIES.fromArray(b, i);
                av.add(bv, vmask).intoArray(r, i);
            }
        }

        assertArraysEquals(a, b, r, mask, Float64VectorTests::add);
    }
    static float sub(float a, float b) {
        return (float)(a - b);
    }

    @Test(dataProvider = "floatBinaryOpProvider")
    static void subFloat64VectorTests(IntFunction<float[]> fa, IntFunction<float[]> fb) {
        float[] a = fa.apply(SPECIES.length());
        float[] b = fb.apply(SPECIES.length());
        float[] r = fr.apply(SPECIES.length());

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                FloatVector av = SPECIES.fromArray(a, i);
                FloatVector bv = SPECIES.fromArray(b, i);
                av.sub(bv).intoArray(r, i);
            }
        }

        assertArraysEquals(a, b, r, Float64VectorTests::sub);
    }

    @Test(dataProvider = "floatBinaryOpMaskProvider")
    static void subFloat64VectorTests(IntFunction<float[]> fa, IntFunction<float[]> fb,
                                          IntFunction<boolean[]> fm) {
        float[] a = fa.apply(SPECIES.length());
        float[] b = fb.apply(SPECIES.length());
        float[] r = fr.apply(SPECIES.length());
        boolean[] mask = fm.apply(SPECIES.length());
        Vector.Mask<Float> vmask = SPECIES.maskFromValues(mask);

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                FloatVector av = SPECIES.fromArray(a, i);
                FloatVector bv = SPECIES.fromArray(b, i);
                av.sub(bv, vmask).intoArray(r, i);
            }
        }

        assertArraysEquals(a, b, r, mask, Float64VectorTests::sub);
    }

    static float div(float a, float b) {
        return (float)(a / b);
    }

    @Test(dataProvider = "floatBinaryOpProvider")
    static void divFloat64VectorTests(IntFunction<float[]> fa, IntFunction<float[]> fb) {
        float[] a = fa.apply(SPECIES.length());
        float[] b = fb.apply(SPECIES.length());
        float[] r = fr.apply(SPECIES.length());

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                FloatVector av = SPECIES.fromArray(a, i);
                FloatVector bv = SPECIES.fromArray(b, i);
                av.div(bv).intoArray(r, i);
            }
        }

        assertArraysEquals(a, b, r, Float64VectorTests::div);
    }



    @Test(dataProvider = "floatBinaryOpMaskProvider")
    static void divFloat64VectorTests(IntFunction<float[]> fa, IntFunction<float[]> fb,
                                          IntFunction<boolean[]> fm) {
        float[] a = fa.apply(SPECIES.length());
        float[] b = fb.apply(SPECIES.length());
        float[] r = fr.apply(SPECIES.length());
        boolean[] mask = fm.apply(SPECIES.length());
        Vector.Mask<Float> vmask = SPECIES.maskFromValues(mask);

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                FloatVector av = SPECIES.fromArray(a, i);
                FloatVector bv = SPECIES.fromArray(b, i);
                av.div(bv, vmask).intoArray(r, i);
            }
        }

        assertArraysEquals(a, b, r, mask, Float64VectorTests::div);
    }

    static float mul(float a, float b) {
        return (float)(a * b);
    }

    @Test(dataProvider = "floatBinaryOpProvider")
    static void mulFloat64VectorTests(IntFunction<float[]> fa, IntFunction<float[]> fb) {
        float[] a = fa.apply(SPECIES.length());
        float[] b = fb.apply(SPECIES.length());
        float[] r = fr.apply(SPECIES.length());

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                FloatVector av = SPECIES.fromArray(a, i);
                FloatVector bv = SPECIES.fromArray(b, i);
                av.mul(bv).intoArray(r, i);
            }
        }

        assertArraysEquals(a, b, r, Float64VectorTests::mul);
    }

    @Test(dataProvider = "floatBinaryOpMaskProvider")
    static void mulFloat64VectorTests(IntFunction<float[]> fa, IntFunction<float[]> fb,
                                          IntFunction<boolean[]> fm) {
        float[] a = fa.apply(SPECIES.length());
        float[] b = fb.apply(SPECIES.length());
        float[] r = fr.apply(SPECIES.length());
        boolean[] mask = fm.apply(SPECIES.length());
        Vector.Mask<Float> vmask = SPECIES.maskFromValues(mask);

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                FloatVector av = SPECIES.fromArray(a, i);
                FloatVector bv = SPECIES.fromArray(b, i);
                av.mul(bv, vmask).intoArray(r, i);
            }
        }

        assertArraysEquals(a, b, r, mask, Float64VectorTests::mul);
    }


















    static float max(float a, float b) {
        return (float)((a > b) ? a : b);
    }

    @Test(dataProvider = "floatBinaryOpProvider")
    static void maxFloat64VectorTests(IntFunction<float[]> fa, IntFunction<float[]> fb) {
        float[] a = fa.apply(SPECIES.length());
        float[] b = fb.apply(SPECIES.length());
        float[] r = fr.apply(SPECIES.length());

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                FloatVector av = SPECIES.fromArray(a, i);
                FloatVector bv = SPECIES.fromArray(b, i);
                av.max(bv).intoArray(r, i);
            }
        }

        assertArraysEquals(a, b, r, Float64VectorTests::max);
    }
    static float min(float a, float b) {
        return (float)((a < b) ? a : b);
    }

    @Test(dataProvider = "floatBinaryOpProvider")
    static void minFloat64VectorTests(IntFunction<float[]> fa, IntFunction<float[]> fb) {
        float[] a = fa.apply(SPECIES.length());
        float[] b = fb.apply(SPECIES.length());
        float[] r = fr.apply(SPECIES.length());

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                FloatVector av = SPECIES.fromArray(a, i);
                FloatVector bv = SPECIES.fromArray(b, i);
                av.min(bv).intoArray(r, i);
            }
        }

        assertArraysEquals(a, b, r, Float64VectorTests::min);
    }






    static float addAll(float[] a, int idx) {
        float res = 0;
        for (int i = idx; i < (idx + SPECIES.length()); i++) {
          res += a[i];
        }

        return res;
    }
    @Test(dataProvider = "floatUnaryOpProvider")
    static void addAllFloat64VectorTests(IntFunction<float[]> fa) {
        float[] a = fa.apply(SPECIES.length());
        float[] r = fr.apply(SPECIES.length());

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
              FloatVector av = SPECIES.fromArray(a, i);
              r[i] = av.addAll();
            }
        }

        assertReductionArraysEquals(a, r, Float64VectorTests::addAll);
    }
    static float subAll(float[] a, int idx) {
        float res = 0;
        for (int i = idx; i < (idx + SPECIES.length()); i++) {
          res -= a[i];
        }

        return res;
    }
    @Test(dataProvider = "floatUnaryOpProvider")
    static void subAllFloat64VectorTests(IntFunction<float[]> fa) {
        float[] a = fa.apply(SPECIES.length());
        float[] r = fr.apply(SPECIES.length());

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
              FloatVector av = SPECIES.fromArray(a, i);
              r[i] = av.subAll();
            }
        }

        assertReductionArraysEquals(a, r, Float64VectorTests::subAll);
    }
    static float mulAll(float[] a, int idx) {
        float res = 1;
        for (int i = idx; i < (idx + SPECIES.length()); i++) {
          res *= a[i];
        }

        return res;
    }
    @Test(dataProvider = "floatUnaryOpProvider")
    static void mulAllFloat64VectorTests(IntFunction<float[]> fa) {
        float[] a = fa.apply(SPECIES.length());
        float[] r = fr.apply(SPECIES.length());

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
              FloatVector av = SPECIES.fromArray(a, i);
              r[i] = av.mulAll();
            }
        }

        assertReductionArraysEquals(a, r, Float64VectorTests::mulAll);
    }
    static float minAll(float[] a, int idx) {
        float res = Float.MAX_VALUE;
        for (int i = idx; i < (idx + SPECIES.length()); i++) {
            res = (res < a[i])?res:a[i];
        }

        return res;
    }
    @Test(dataProvider = "floatUnaryOpProvider")
    static void minAllFloat64VectorTests(IntFunction<float[]> fa) {
        float[] a = fa.apply(SPECIES.length());
        float[] r = fr.apply(SPECIES.length());

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
              FloatVector av = SPECIES.fromArray(a, i);
              r[i] = av.minAll();
            }
        }

        assertReductionArraysEquals(a, r, Float64VectorTests::minAll);
    }
    static float maxAll(float[] a, int idx) {
        float res = Float.MIN_VALUE;
        for (int i = idx; i < (idx + SPECIES.length()); i++) {
          res = (res > a[i])?res:a[i];
        }

        return res;
    }
    @Test(dataProvider = "floatUnaryOpProvider")
    static void maxAllFloat64VectorTests(IntFunction<float[]> fa) {
        float[] a = fa.apply(SPECIES.length());
        float[] r = fr.apply(SPECIES.length());

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
              FloatVector av = SPECIES.fromArray(a, i);
              r[i] = av.maxAll();
            }
        }

        assertReductionArraysEquals(a, r, Float64VectorTests::maxAll);
    }





    @Test(dataProvider = "floatUnaryOpProvider")
    static void withFloat64VectorTests(IntFunction<float []> fa) {
        float[] a = fa.apply(SPECIES.length());
        float[] r = fr.apply(SPECIES.length());

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
              FloatVector av = SPECIES.fromArray(a, i);
              av.with(0, (float)4).intoArray(r, i);
            }
        }

        assertInsertArraysEquals(a, r, (float)4, 0);
    }

    @Test(dataProvider = "floatCompareOpProvider")
    static void lessThanFloat64VectorTests(IntFunction<float[]> fa, IntFunction<float[]> fb) {
        float[] a = fa.apply(SPECIES.length());
        float[] b = fb.apply(SPECIES.length());

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                FloatVector av = SPECIES.fromArray(a, i);
                FloatVector bv = SPECIES.fromArray(b, i);
                Vector.Mask<Float> mv = av.lessThan(bv);

                // Check results as part of computation.
                for (int j = 0; j < SPECIES.length(); j++) {
                    Assert.assertEquals(mv.getElement(j), a[i + j] < b[i + j]);
                }
            }
        }
    }


    @Test(dataProvider = "floatCompareOpProvider")
    static void greaterThanFloat64VectorTests(IntFunction<float[]> fa, IntFunction<float[]> fb) {
        float[] a = fa.apply(SPECIES.length());
        float[] b = fb.apply(SPECIES.length());

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                FloatVector av = SPECIES.fromArray(a, i);
                FloatVector bv = SPECIES.fromArray(b, i);
                Vector.Mask<Float> mv = av.greaterThan(bv);

                // Check results as part of computation.
                for (int j = 0; j < SPECIES.length(); j++) {
                    Assert.assertEquals(mv.getElement(j), a[i + j] > b[i + j]);
                }
            }
        }
    }


    @Test(dataProvider = "floatCompareOpProvider")
    static void equalFloat64VectorTests(IntFunction<float[]> fa, IntFunction<float[]> fb) {
        float[] a = fa.apply(SPECIES.length());
        float[] b = fb.apply(SPECIES.length());

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                FloatVector av = SPECIES.fromArray(a, i);
                FloatVector bv = SPECIES.fromArray(b, i);
                Vector.Mask<Float> mv = av.equal(bv);

                // Check results as part of computation.
                for (int j = 0; j < SPECIES.length(); j++) {
                    Assert.assertEquals(mv.getElement(j), a[i + j] == b[i + j]);
                }
            }
        }
    }


    @Test(dataProvider = "floatCompareOpProvider")
    static void notEqualFloat64VectorTests(IntFunction<float[]> fa, IntFunction<float[]> fb) {
        float[] a = fa.apply(SPECIES.length());
        float[] b = fb.apply(SPECIES.length());

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                FloatVector av = SPECIES.fromArray(a, i);
                FloatVector bv = SPECIES.fromArray(b, i);
                Vector.Mask<Float> mv = av.notEqual(bv);

                // Check results as part of computation.
                for (int j = 0; j < SPECIES.length(); j++) {
                    Assert.assertEquals(mv.getElement(j), a[i + j] != b[i + j]);
                }
            }
        }
    }


    @Test(dataProvider = "floatCompareOpProvider")
    static void lessThanEqFloat64VectorTests(IntFunction<float[]> fa, IntFunction<float[]> fb) {
        float[] a = fa.apply(SPECIES.length());
        float[] b = fb.apply(SPECIES.length());

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                FloatVector av = SPECIES.fromArray(a, i);
                FloatVector bv = SPECIES.fromArray(b, i);
                Vector.Mask<Float> mv = av.lessThanEq(bv);

                // Check results as part of computation.
                for (int j = 0; j < SPECIES.length(); j++) {
                    Assert.assertEquals(mv.getElement(j), a[i + j] <= b[i + j]);
                }
            }
        }
    }


    @Test(dataProvider = "floatCompareOpProvider")
    static void greaterThanEqFloat64VectorTests(IntFunction<float[]> fa, IntFunction<float[]> fb) {
        float[] a = fa.apply(SPECIES.length());
        float[] b = fb.apply(SPECIES.length());

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                FloatVector av = SPECIES.fromArray(a, i);
                FloatVector bv = SPECIES.fromArray(b, i);
                Vector.Mask<Float> mv = av.greaterThanEq(bv);

                // Check results as part of computation.
                for (int j = 0; j < SPECIES.length(); j++) {
                    Assert.assertEquals(mv.getElement(j), a[i + j] >= b[i + j]);
                }
            }
        }
    }


    static float blend(float a, float b, boolean mask) {
        return mask ? b : a;
    }

    @Test(dataProvider = "floatBinaryOpMaskProvider")
    static void blendFloat64VectorTests(IntFunction<float[]> fa, IntFunction<float[]> fb,
                                          IntFunction<boolean[]> fm) {
        float[] a = fa.apply(SPECIES.length());
        float[] b = fb.apply(SPECIES.length());
        float[] r = fr.apply(SPECIES.length());
        boolean[] mask = fm.apply(SPECIES.length());
        Vector.Mask<Float> vmask = SPECIES.maskFromValues(mask);

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                FloatVector av = SPECIES.fromArray(a, i);
                FloatVector bv = SPECIES.fromArray(b, i);
                av.blend(bv, vmask).intoArray(r, i);
            }
        }

        assertArraysEquals(a, b, r, mask, Float64VectorTests::blend);
    }

    @Test(dataProvider = "floatUnaryOpShuffleProvider")
    static void RearrangeFloat64VectorTests(IntFunction<float[]> fa,
                                           BiFunction<Integer,Integer,int[]> fs) {
        float[] a = fa.apply(SPECIES.length());
        int[] order = fs.apply(a.length, SPECIES.length());
        float[] r = fr.apply(SPECIES.length());

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                FloatVector av = SPECIES.fromArray(a, i);
                av.rearrange(SPECIES.shuffleFromArray(order, i)).intoArray(r, i);
            }
        }

        assertRearrangeArraysEquals(a, r, order, SPECIES.length());
    }




    @Test(dataProvider = "floatUnaryOpProvider")
    static void getFloat64VectorTests(IntFunction<float[]> fa) {
        float[] a = fa.apply(SPECIES.length());
        float[] r = fr.apply(SPECIES.length());

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                FloatVector av = SPECIES.fromArray(a, i);
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

        assertArraysEquals(a, r, Float64VectorTests::get);
    }

    static float sin(float a) {
        return (float)(Math.sin((double)a));
    }

    static float strictsin(float a) {
        return (float)(StrictMath.sin((double)a));
    }

    @Test(dataProvider = "floatUnaryOpProvider")
    static void sinFloat64VectorTests(IntFunction<float[]> fa) {
        float[] a = fa.apply(SPECIES.length());
        float[] r = fr.apply(SPECIES.length());

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                FloatVector av = SPECIES.fromArray(a, i);
                av.sin().intoArray(r, i);
            }
        }

        assertArraysEqualsWithinOneUlp(a, r, Float64VectorTests::sin, Float64VectorTests::strictsin);
    }


    static float exp(float a) {
        return (float)(Math.exp((double)a));
    }

    static float strictexp(float a) {
        return (float)(StrictMath.exp((double)a));
    }

    @Test(dataProvider = "floatUnaryOpProvider")
    static void expFloat64VectorTests(IntFunction<float[]> fa) {
        float[] a = fa.apply(SPECIES.length());
        float[] r = fr.apply(SPECIES.length());

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                FloatVector av = SPECIES.fromArray(a, i);
                av.exp().intoArray(r, i);
            }
        }

        assertArraysEqualsWithinOneUlp(a, r, Float64VectorTests::exp, Float64VectorTests::strictexp);
    }


    static float log1p(float a) {
        return (float)(Math.log1p((double)a));
    }

    static float strictlog1p(float a) {
        return (float)(StrictMath.log1p((double)a));
    }

    @Test(dataProvider = "floatUnaryOpProvider")
    static void log1pFloat64VectorTests(IntFunction<float[]> fa) {
        float[] a = fa.apply(SPECIES.length());
        float[] r = fr.apply(SPECIES.length());

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                FloatVector av = SPECIES.fromArray(a, i);
                av.log1p().intoArray(r, i);
            }
        }

        assertArraysEqualsWithinOneUlp(a, r, Float64VectorTests::log1p, Float64VectorTests::strictlog1p);
    }


    static float log(float a) {
        return (float)(Math.log((double)a));
    }

    static float strictlog(float a) {
        return (float)(StrictMath.log((double)a));
    }

    @Test(dataProvider = "floatUnaryOpProvider")
    static void logFloat64VectorTests(IntFunction<float[]> fa) {
        float[] a = fa.apply(SPECIES.length());
        float[] r = fr.apply(SPECIES.length());

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                FloatVector av = SPECIES.fromArray(a, i);
                av.log().intoArray(r, i);
            }
        }

        assertArraysEqualsWithinOneUlp(a, r, Float64VectorTests::log, Float64VectorTests::strictlog);
    }


    static float log10(float a) {
        return (float)(Math.log10((double)a));
    }

    static float strictlog10(float a) {
        return (float)(StrictMath.log10((double)a));
    }

    @Test(dataProvider = "floatUnaryOpProvider")
    static void log10Float64VectorTests(IntFunction<float[]> fa) {
        float[] a = fa.apply(SPECIES.length());
        float[] r = fr.apply(SPECIES.length());

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                FloatVector av = SPECIES.fromArray(a, i);
                av.log10().intoArray(r, i);
            }
        }

        assertArraysEqualsWithinOneUlp(a, r, Float64VectorTests::log10, Float64VectorTests::strictlog10);
    }


    static float expm1(float a) {
        return (float)(Math.expm1((double)a));
    }

    static float strictexpm1(float a) {
        return (float)(StrictMath.expm1((double)a));
    }

    @Test(dataProvider = "floatUnaryOpProvider")
    static void expm1Float64VectorTests(IntFunction<float[]> fa) {
        float[] a = fa.apply(SPECIES.length());
        float[] r = fr.apply(SPECIES.length());

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                FloatVector av = SPECIES.fromArray(a, i);
                av.expm1().intoArray(r, i);
            }
        }

        assertArraysEqualsWithinOneUlp(a, r, Float64VectorTests::expm1, Float64VectorTests::strictexpm1);
    }


    static float cos(float a) {
        return (float)(Math.cos((double)a));
    }

    static float strictcos(float a) {
        return (float)(StrictMath.cos((double)a));
    }

    @Test(dataProvider = "floatUnaryOpProvider")
    static void cosFloat64VectorTests(IntFunction<float[]> fa) {
        float[] a = fa.apply(SPECIES.length());
        float[] r = fr.apply(SPECIES.length());

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                FloatVector av = SPECIES.fromArray(a, i);
                av.cos().intoArray(r, i);
            }
        }

        assertArraysEqualsWithinOneUlp(a, r, Float64VectorTests::cos, Float64VectorTests::strictcos);
    }


    static float tan(float a) {
        return (float)(Math.tan((double)a));
    }

    static float stricttan(float a) {
        return (float)(StrictMath.tan((double)a));
    }

    @Test(dataProvider = "floatUnaryOpProvider")
    static void tanFloat64VectorTests(IntFunction<float[]> fa) {
        float[] a = fa.apply(SPECIES.length());
        float[] r = fr.apply(SPECIES.length());

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                FloatVector av = SPECIES.fromArray(a, i);
                av.tan().intoArray(r, i);
            }
        }

        assertArraysEqualsWithinOneUlp(a, r, Float64VectorTests::tan, Float64VectorTests::stricttan);
    }


    static float sinh(float a) {
        return (float)(Math.sinh((double)a));
    }

    static float strictsinh(float a) {
        return (float)(StrictMath.sinh((double)a));
    }

    @Test(dataProvider = "floatUnaryOpProvider")
    static void sinhFloat64VectorTests(IntFunction<float[]> fa) {
        float[] a = fa.apply(SPECIES.length());
        float[] r = fr.apply(SPECIES.length());

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                FloatVector av = SPECIES.fromArray(a, i);
                av.sinh().intoArray(r, i);
            }
        }

        assertArraysEqualsWithinOneUlp(a, r, Float64VectorTests::sinh, Float64VectorTests::strictsinh);
    }


    static float cosh(float a) {
        return (float)(Math.cosh((double)a));
    }

    static float strictcosh(float a) {
        return (float)(StrictMath.cosh((double)a));
    }

    @Test(dataProvider = "floatUnaryOpProvider")
    static void coshFloat64VectorTests(IntFunction<float[]> fa) {
        float[] a = fa.apply(SPECIES.length());
        float[] r = fr.apply(SPECIES.length());

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                FloatVector av = SPECIES.fromArray(a, i);
                av.cosh().intoArray(r, i);
            }
        }

        assertArraysEqualsWithinOneUlp(a, r, Float64VectorTests::cosh, Float64VectorTests::strictcosh);
    }


    static float tanh(float a) {
        return (float)(Math.tanh((double)a));
    }

    static float stricttanh(float a) {
        return (float)(StrictMath.tanh((double)a));
    }

    @Test(dataProvider = "floatUnaryOpProvider")
    static void tanhFloat64VectorTests(IntFunction<float[]> fa) {
        float[] a = fa.apply(SPECIES.length());
        float[] r = fr.apply(SPECIES.length());

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                FloatVector av = SPECIES.fromArray(a, i);
                av.tanh().intoArray(r, i);
            }
        }

        assertArraysEqualsWithinOneUlp(a, r, Float64VectorTests::tanh, Float64VectorTests::stricttanh);
    }


    static float asin(float a) {
        return (float)(Math.asin((double)a));
    }

    static float strictasin(float a) {
        return (float)(StrictMath.asin((double)a));
    }

    @Test(dataProvider = "floatUnaryOpProvider")
    static void asinFloat64VectorTests(IntFunction<float[]> fa) {
        float[] a = fa.apply(SPECIES.length());
        float[] r = fr.apply(SPECIES.length());

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                FloatVector av = SPECIES.fromArray(a, i);
                av.asin().intoArray(r, i);
            }
        }

        assertArraysEqualsWithinOneUlp(a, r, Float64VectorTests::asin, Float64VectorTests::strictasin);
    }


    static float acos(float a) {
        return (float)(Math.acos((double)a));
    }

    static float strictacos(float a) {
        return (float)(StrictMath.acos((double)a));
    }

    @Test(dataProvider = "floatUnaryOpProvider")
    static void acosFloat64VectorTests(IntFunction<float[]> fa) {
        float[] a = fa.apply(SPECIES.length());
        float[] r = fr.apply(SPECIES.length());

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                FloatVector av = SPECIES.fromArray(a, i);
                av.acos().intoArray(r, i);
            }
        }

        assertArraysEqualsWithinOneUlp(a, r, Float64VectorTests::acos, Float64VectorTests::strictacos);
    }


    static float atan(float a) {
        return (float)(Math.atan((double)a));
    }

    static float strictatan(float a) {
        return (float)(StrictMath.atan((double)a));
    }

    @Test(dataProvider = "floatUnaryOpProvider")
    static void atanFloat64VectorTests(IntFunction<float[]> fa) {
        float[] a = fa.apply(SPECIES.length());
        float[] r = fr.apply(SPECIES.length());

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                FloatVector av = SPECIES.fromArray(a, i);
                av.atan().intoArray(r, i);
            }
        }

        assertArraysEqualsWithinOneUlp(a, r, Float64VectorTests::atan, Float64VectorTests::strictatan);
    }


    static float cbrt(float a) {
        return (float)(Math.cbrt((double)a));
    }

    static float strictcbrt(float a) {
        return (float)(StrictMath.cbrt((double)a));
    }

    @Test(dataProvider = "floatUnaryOpProvider")
    static void cbrtFloat64VectorTests(IntFunction<float[]> fa) {
        float[] a = fa.apply(SPECIES.length());
        float[] r = fr.apply(SPECIES.length());

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                FloatVector av = SPECIES.fromArray(a, i);
                av.cbrt().intoArray(r, i);
            }
        }

        assertArraysEqualsWithinOneUlp(a, r, Float64VectorTests::cbrt, Float64VectorTests::strictcbrt);
    }


    static float hypot(float a, float b) {
        return (float)(Math.hypot((double)a, (double)b));
    }

    static float stricthypot(float a, float b) {
        return (float)(StrictMath.hypot((double)a, (double)b));
    }

    @Test(dataProvider = "floatBinaryOpProvider")
    static void hypotFloat64VectorTests(IntFunction<float[]> fa, IntFunction<float[]> fb) {
        float[] a = fa.apply(SPECIES.length());
        float[] b = fb.apply(SPECIES.length());
        float[] r = fr.apply(SPECIES.length());

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                FloatVector av = SPECIES.fromArray(a, i);
                FloatVector bv = SPECIES.fromArray(b, i);
                av.hypot(bv).intoArray(r, i);
            }
        }

        assertArraysEqualsWithinOneUlp(a, b, r, Float64VectorTests::hypot, Float64VectorTests::stricthypot);
    }



    static float pow(float a, float b) {
        return (float)(Math.pow((double)a, (double)b));
    }

    static float strictpow(float a, float b) {
        return (float)(StrictMath.pow((double)a, (double)b));
    }

    @Test(dataProvider = "floatBinaryOpProvider")
    static void powFloat64VectorTests(IntFunction<float[]> fa, IntFunction<float[]> fb) {
        float[] a = fa.apply(SPECIES.length());
        float[] b = fb.apply(SPECIES.length());
        float[] r = fr.apply(SPECIES.length());

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                FloatVector av = SPECIES.fromArray(a, i);
                FloatVector bv = SPECIES.fromArray(b, i);
                av.pow(bv).intoArray(r, i);
            }
        }

        assertArraysEqualsWithinOneUlp(a, b, r, Float64VectorTests::pow, Float64VectorTests::strictpow);
    }



    static float atan2(float a, float b) {
        return (float)(Math.atan2((double)a, (double)b));
    }

    static float strictatan2(float a, float b) {
        return (float)(StrictMath.atan2((double)a, (double)b));
    }

    @Test(dataProvider = "floatBinaryOpProvider")
    static void atan2Float64VectorTests(IntFunction<float[]> fa, IntFunction<float[]> fb) {
        float[] a = fa.apply(SPECIES.length());
        float[] b = fb.apply(SPECIES.length());
        float[] r = fr.apply(SPECIES.length());

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                FloatVector av = SPECIES.fromArray(a, i);
                FloatVector bv = SPECIES.fromArray(b, i);
                av.atan2(bv).intoArray(r, i);
            }
        }

        assertArraysEqualsWithinOneUlp(a, b, r, Float64VectorTests::atan2, Float64VectorTests::strictatan2);
    }



    static float fma(float a, float b, float c) {
        return (float)(Math.fma(a, b, c));
    }


    @Test(dataProvider = "floatTernaryOpProvider")
    static void fmaFloat64VectorTests(IntFunction<float[]> fa, IntFunction<float[]> fb, IntFunction<float[]> fc) {
        float[] a = fa.apply(SPECIES.length());
        float[] b = fb.apply(SPECIES.length());
        float[] c = fc.apply(SPECIES.length());
        float[] r = fr.apply(SPECIES.length());

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                FloatVector av = SPECIES.fromArray(a, i);
                FloatVector bv = SPECIES.fromArray(b, i);
                FloatVector cv = SPECIES.fromArray(c, i);
                av.fma(bv, cv).intoArray(r, i);
            }
        }

        assertArraysEquals(a, b, c, r, Float64VectorTests::fma);
    }


    @Test(dataProvider = "floatTernaryOpMaskProvider")
    static void fmaFloat64VectorTests(IntFunction<float[]> fa, IntFunction<float[]> fb,
                                          IntFunction<float[]> fc, IntFunction<boolean[]> fm) {
        float[] a = fa.apply(SPECIES.length());
        float[] b = fb.apply(SPECIES.length());
        float[] c = fc.apply(SPECIES.length());
        float[] r = fr.apply(SPECIES.length());
        boolean[] mask = fm.apply(SPECIES.length());
        Vector.Mask<Float> vmask = SPECIES.maskFromValues(mask);

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                FloatVector av = SPECIES.fromArray(a, i);
                FloatVector bv = SPECIES.fromArray(b, i);
                FloatVector cv = SPECIES.fromArray(c, i);
                av.fma(bv, cv, vmask).intoArray(r, i);
            }
        }

        assertArraysEquals(a, b, c, r, mask, Float64VectorTests::fma);
    }


    static float neg(float a) {
        return (float)(-((float)a));
    }

    @Test(dataProvider = "floatUnaryOpProvider")
    static void negFloat64VectorTests(IntFunction<float[]> fa) {
        float[] a = fa.apply(SPECIES.length());
        float[] r = fr.apply(SPECIES.length());

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                FloatVector av = SPECIES.fromArray(a, i);
                av.neg().intoArray(r, i);
            }
        }

        assertArraysEquals(a, r, Float64VectorTests::neg);
    }

    @Test(dataProvider = "floatUnaryOpMaskProvider")
    static void negMaskedFloat64VectorTests(IntFunction<float[]> fa,
                                                IntFunction<boolean[]> fm) {
        float[] a = fa.apply(SPECIES.length());
        float[] r = fr.apply(SPECIES.length());
        boolean[] mask = fm.apply(SPECIES.length());
        Vector.Mask<Float> vmask = SPECIES.maskFromValues(mask);

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                FloatVector av = SPECIES.fromArray(a, i);
                av.neg(vmask).intoArray(r, i);
            }
        }

        assertArraysEquals(a, r, mask, Float64VectorTests::neg);
    }





    static float abs(float a) {
        return (float)(Math.abs((float)a));
    }

    @Test(dataProvider = "floatUnaryOpProvider")
    static void absFloat64VectorTests(IntFunction<float[]> fa) {
        float[] a = fa.apply(SPECIES.length());
        float[] r = fr.apply(SPECIES.length());

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                FloatVector av = SPECIES.fromArray(a, i);
                av.abs().intoArray(r, i);
            }
        }

        assertArraysEquals(a, r, Float64VectorTests::abs);
    }

    @Test(dataProvider = "floatUnaryOpMaskProvider")
    static void absMaskedFloat64VectorTests(IntFunction<float[]> fa,
                                                IntFunction<boolean[]> fm) {
        float[] a = fa.apply(SPECIES.length());
        float[] r = fr.apply(SPECIES.length());
        boolean[] mask = fm.apply(SPECIES.length());
        Vector.Mask<Float> vmask = SPECIES.maskFromValues(mask);

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                FloatVector av = SPECIES.fromArray(a, i);
                av.abs(vmask).intoArray(r, i);
            }
        }

        assertArraysEquals(a, r, mask, Float64VectorTests::abs);
    }









    static float sqrt(float a) {
        return (float)(Math.sqrt((double)a));
    }



    @Test(dataProvider = "floatUnaryOpProvider")
    static void sqrtFloat64VectorTests(IntFunction<float[]> fa) {
        float[] a = fa.apply(SPECIES.length());
        float[] r = fr.apply(SPECIES.length());

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                FloatVector av = SPECIES.fromArray(a, i);
                av.sqrt().intoArray(r, i);
            }
        }

        assertArraysEquals(a, r, Float64VectorTests::sqrt);
    }



    @Test(dataProvider = "floatUnaryOpMaskProvider")
    static void sqrtMaskedFloat64VectorTests(IntFunction<float[]> fa,
                                                IntFunction<boolean[]> fm) {
        float[] a = fa.apply(SPECIES.length());
        float[] r = fr.apply(SPECIES.length());
        boolean[] mask = fm.apply(SPECIES.length());
        Vector.Mask<Float> vmask = SPECIES.maskFromValues(mask);

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                FloatVector av = SPECIES.fromArray(a, i);
                av.sqrt(vmask).intoArray(r, i);
            }
        }

        assertArraysEquals(a, r, mask, Float64VectorTests::sqrt);
    }






    static float[] gather(float a[], int ix, int[] b, int iy) {
      float[] res = new float[SPECIES.length()];
      for (int i = 0; i < SPECIES.length(); i++) {
        int bi = iy + i;
        res[i] = a[b[bi] + ix];
      }
      return res;
    }

    @Test(dataProvider = "floatUnaryOpIndexProvider")
    static void gatherFloat64VectorTests(IntFunction<float[]> fa, BiFunction<Integer,Integer,int[]> fs) {
        float[] a = fa.apply(SPECIES.length());
        int[] b    = fs.apply(a.length, SPECIES.length());
        float[] r = new float[a.length];

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                FloatVector av = SPECIES.fromArray(a, i, b, i);
                av.intoArray(r, i);
            }
        }

        assertArraysEquals(a, b, r, Float64VectorTests::gather);
    }


    static float[] scatter(float a[], int ix, int[] b, int iy) {
      float[] res = new float[SPECIES.length()];
      for (int i = 0; i < SPECIES.length(); i++) {
        int bi = iy + i;
        res[b[bi]] = a[i + ix];
      }
      return res;
    }

    @Test(dataProvider = "floatUnaryOpIndexProvider")
    static void scatterFloat64VectorTests(IntFunction<float[]> fa, BiFunction<Integer,Integer,int[]> fs) {
        float[] a = fa.apply(SPECIES.length());
        int[] b = fs.apply(a.length, SPECIES.length());
        float[] r = new float[a.length];

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                FloatVector av = SPECIES.fromArray(a, i);
                av.intoArray(r, i, b, i);
            }
        }

        assertArraysEquals(a, b, r, Float64VectorTests::scatter);
    }

}

