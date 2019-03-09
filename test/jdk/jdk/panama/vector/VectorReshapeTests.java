import jdk.panama.vector.*;
import jdk.internal.vm.annotation.ForceInline;
import org.testng.Assert;
import org.testng.annotations.Test;
import org.testng.annotations.DataProvider;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.Arrays;
import java.util.List;
import java.util.function.IntFunction;
import jdk.panama.vector.Vector.Shape;

/**
 * @test
 * @modules jdk.panama.vector
 * @modules java.base/jdk.internal.vm.annotation
 * @run testng/othervm --add-opens jdk.panama.vector/jdk.panama.vector=ALL-UNNAMED
 *      VectorReshapeTests
 */

@Test
public class VectorReshapeTests {
    static final int INVOC_COUNT = Integer.getInteger("jdk.panama.vector.test.loop-iterations", 100);
    static final int NUM_ITER = 200 * INVOC_COUNT;

    static final Shape S_Max_BIT = getMaxBit();

    static final IntVector.IntSpecies ispec64 = IntVector.species(Shape.S_64_BIT);
    static final FloatVector.FloatSpecies fspec64 = FloatVector.species(Shape.S_64_BIT);
    static final LongVector.LongSpecies lspec64 = LongVector.species(Shape.S_64_BIT);
    static final DoubleVector.DoubleSpecies dspec64 = DoubleVector.species(Shape.S_64_BIT);
    static final ByteVector.ByteSpecies bspec64 = ByteVector.species(Shape.S_64_BIT);
    static final ShortVector.ShortSpecies sspec64 = ShortVector.species(Shape.S_64_BIT);

    static final IntVector.IntSpecies ispec128 = IntVector.species(Shape.S_128_BIT);
    static final FloatVector.FloatSpecies fspec128 = FloatVector.species(Shape.S_128_BIT);
    static final LongVector.LongSpecies lspec128 = LongVector.species(Shape.S_128_BIT);
    static final DoubleVector.DoubleSpecies dspec128 = DoubleVector.species(Shape.S_128_BIT);
    static final ByteVector.ByteSpecies bspec128 = ByteVector.species(Shape.S_128_BIT);
    static final ShortVector.ShortSpecies sspec128 = ShortVector.species(Shape.S_128_BIT);

    static final IntVector.IntSpecies ispec256 = IntVector.species(Shape.S_256_BIT);
    static final FloatVector.FloatSpecies fspec256 = FloatVector.species(Shape.S_256_BIT);
    static final LongVector.LongSpecies lspec256 = LongVector.species(Shape.S_256_BIT);
    static final DoubleVector.DoubleSpecies dspec256 = DoubleVector.species(Shape.S_256_BIT);
    static final ByteVector.ByteSpecies bspec256 = ByteVector.species(Shape.S_256_BIT);
    static final ShortVector.ShortSpecies sspec256 = ShortVector.species(Shape.S_256_BIT);

    static final IntVector.IntSpecies ispec512 = IntVector.species(Shape.S_512_BIT);
    static final FloatVector.FloatSpecies fspec512 = FloatVector.species(Shape.S_512_BIT);
    static final LongVector.LongSpecies lspec512 = LongVector.species(Shape.S_512_BIT);
    static final DoubleVector.DoubleSpecies dspec512 = DoubleVector.species(Shape.S_512_BIT);
    static final ByteVector.ByteSpecies bspec512 = ByteVector.species(Shape.S_512_BIT);
    static final ShortVector.ShortSpecies sspec512 = ShortVector.species(Shape.S_512_BIT);

    static final IntVector.IntSpecies ispecMax = IntVector.species(S_Max_BIT);
    static final FloatVector.FloatSpecies fspecMax = FloatVector.species(S_Max_BIT);
    static final LongVector.LongSpecies lspecMax = LongVector.species(S_Max_BIT);
    static final DoubleVector.DoubleSpecies dspecMax = DoubleVector.species(S_Max_BIT);
    static final ByteVector.ByteSpecies bspecMax = ByteVector.species(S_Max_BIT);
    static final ShortVector.ShortSpecies sspecMax = ShortVector.species(S_Max_BIT);

    static Shape getMaxBit() {
        return Shape.S_Max_BIT;
    }

    static <T> IntFunction<T> withToString(String s, IntFunction<T> f) {
        return new IntFunction<T>() {
            @Override
            public T apply(int v) {
                return f.apply(v);
            }

            @Override
            public String toString() {
                return s;
            }
        };
    }

    interface ToByteF {
        byte apply(int i);
    }

    static byte[] fill_byte(int s , ToByteF f) {
        return fill_byte(new byte[s], f);
    }

    static byte[] fill_byte(byte[] a, ToByteF f) {
        for (int i = 0; i < a.length; i++) {
            a[i] = f.apply(i);
        }
        return a;
    }

    interface ToBoolF {
        boolean apply(int i);
    }

    static boolean[] fill_bool(int s , ToBoolF f) {
        return fill_bool(new boolean[s], f);
    }

    static boolean[] fill_bool(boolean[] a, ToBoolF f) {
        for (int i = 0; i < a.length; i++) {
            a[i] = f.apply(i);
        }
        return a;
    }

    interface ToShortF {
        short apply(int i);
    }

    static short[] fill_short(int s , ToShortF f) {
        return fill_short(new short[s], f);
    }

    static short[] fill_short(short[] a, ToShortF f) {
        for (int i = 0; i < a.length; i++) {
            a[i] = f.apply(i);
        }
        return a;
    }

    interface ToIntF {
        int apply(int i);
    }

    static int[] fill_int(int s , ToIntF f) {
        return fill_int(new int[s], f);
    }

    static int[] fill_int(int[] a, ToIntF f) {
        for (int i = 0; i < a.length; i++) {
            a[i] = f.apply(i);
        }
        return a;
    }

    interface ToLongF {
        long apply(int i);
    }

    static long[] fill_long(int s , ToLongF f) {
        return fill_long(new long[s], f);
    }

    static long[] fill_long(long[] a, ToLongF f) {
        for (int i = 0; i < a.length; i++) {
            a[i] = f.apply(i);
        }
        return a;
    }

    interface ToFloatF {
        float apply(int i);
    }

    static float[] fill_float(int s , ToFloatF f) {
        return fill_float(new float[s], f);
    }

    static float[] fill_float(float[] a, ToFloatF f) {
        for (int i = 0; i < a.length; i++) {
            a[i] = f.apply(i);
        }
        return a;
    }

    interface ToDoubleF {
        double apply(int i);
    }

    static double[] fill_double(int s , ToDoubleF f) {
        return fill_double(new double[s], f);
    }

    static double[] fill_double(double[] a, ToDoubleF f) {
        for (int i = 0; i < a.length; i++) {
            a[i] = f.apply(i);
        }
        return a;
    }

    static final List<IntFunction<byte[]>> BYTE_GENERATORS = List.of(
            withToString("byte(i)", (int s) -> {
                return fill_byte(s, i -> (byte)i);
            })
    );

    @DataProvider
    public Object[][] byteUnaryOpProvider() {
        return BYTE_GENERATORS.stream().
                map(f -> new Object[]{f}).
                toArray(Object[][]::new);
    }

    static final List<IntFunction<boolean[]>> BOOL_GENERATORS = List.of(
        withToString("boolean(i%3)", (int s) -> {
            return fill_bool(s, i -> i % 3 == 0);
        })
    );

    @DataProvider
    public Object[][] booleanUnaryOpProvider() {
        return BOOL_GENERATORS.stream().
                map(f -> new Object[]{f}).
                toArray(Object[][]::new);
    }

    static final List<IntFunction<short[]>> SHORT_GENERATORS = List.of(
            withToString("short(i)", (int s) -> {
                return fill_short(s, i -> (short)i);
            })
    );

    @DataProvider
    public Object[][] shortUnaryOpProvider() {
        return SHORT_GENERATORS.stream().
                map(f -> new Object[]{f}).
                toArray(Object[][]::new);
    }

    static final List<IntFunction<int[]>> INT_GENERATORS = List.of(
            withToString("int(i)", (int s) -> {
                return fill_int(s, i -> (int)i);
            })
    );

    @DataProvider
    public Object[][] intUnaryOpProvider() {
        return INT_GENERATORS.stream().
                map(f -> new Object[]{f}).
                toArray(Object[][]::new);
    }

    static final List<IntFunction<long[]>> LONG_GENERATORS = List.of(
            withToString("long(i)", (int s) -> {
                return fill_long(s, i -> (long)i);
            })
    );

    @DataProvider
    public Object[][] longUnaryOpProvider() {
        return LONG_GENERATORS.stream().
                map(f -> new Object[]{f}).
                toArray(Object[][]::new);
    }

    static final List<IntFunction<float[]>> FLOAT_GENERATORS = List.of(
            withToString("float(i)", (int s) -> {
                return fill_float(s, i -> (float)i);
            })
    );

    @DataProvider
    public Object[][] floatUnaryOpProvider() {
        return FLOAT_GENERATORS.stream().
                map(f -> new Object[]{f}).
                toArray(Object[][]::new);
    }

    static final List<IntFunction<double[]>> DOUBLE_GENERATORS = List.of(
            withToString("double(i)", (int s) -> {
                return fill_double(s, i -> (double)i);
            })
    );

    @DataProvider
    public Object[][] doubleUnaryOpProvider() {
        return DOUBLE_GENERATORS.stream().
                map(f -> new Object[]{f}).
                toArray(Object[][]::new);
    }

    @ForceInline
    static <E>
    void testVectorResize(Vector.Species<E> a, Vector.Species<E> b, byte[] input, byte[] output) {
        Vector<E> av = a.fromByteArray(input, 0);
        Vector<E> bv = b.resize(av);
        bv.intoByteArray(output, 0);

        byte[] expected = Arrays.copyOf(input, output.length);


        Assert.assertEquals(expected, output);
    }

    @Test(dataProvider = "byteUnaryOpProvider")
    static void testResizeByte(IntFunction<byte[]> fa) {
        byte[] bin64 = fa.apply(64/Byte.SIZE);
        byte[] bin128 = fa.apply(128/Byte.SIZE);
        byte[] bin256 = fa.apply(256/Byte.SIZE);
        byte[] bin512 = fa.apply(512/Byte.SIZE);
        byte[] binMax = fa.apply(S_Max_BIT.bitSize()/Byte.SIZE);
        byte[] bout64 = new byte[bin64.length];
        byte[] bout128 = new byte[bin128.length];
        byte[] bout256 = new byte[bin256.length];
        byte[] bout512 = new byte[bin512.length];
        byte[] boutMax = new byte[binMax.length];

        for (int i = 0; i < NUM_ITER; i++) {
            testVectorResize(bspec64, bspec64, bin64, bout64);
            testVectorResize(bspec64, bspec128, bin64, bout128);
            testVectorResize(bspec64, bspec256, bin64, bout256);
            testVectorResize(bspec64, bspec512, bin64, bout512);
            testVectorResize(bspec64, bspecMax, bin64, boutMax);

            testVectorResize(bspec128, bspec64, bin128, bout64);
            testVectorResize(bspec128, bspec128, bin128, bout128);
            testVectorResize(bspec128, bspec256, bin128, bout256);
            testVectorResize(bspec128, bspec512, bin128, bout512);
            testVectorResize(bspec128, bspecMax, bin128, boutMax);

            testVectorResize(bspec256, bspec64, bin256, bout64);
            testVectorResize(bspec256, bspec128, bin256, bout128);
            testVectorResize(bspec256, bspec256, bin256, bout256);
            testVectorResize(bspec256, bspec512, bin256, bout512);
            testVectorResize(bspec256, bspecMax, bin256, boutMax);

            testVectorResize(bspec512, bspec64, bin512, bout64);
            testVectorResize(bspec512, bspec128, bin512, bout128);
            testVectorResize(bspec512, bspec256, bin512, bout256);
            testVectorResize(bspec512, bspec512, bin512, bout512);
            testVectorResize(bspec512, bspecMax, bin512, boutMax);

            testVectorResize(bspecMax, bspec64, binMax, bout64);
            testVectorResize(bspecMax, bspec128, binMax, bout128);
            testVectorResize(bspecMax, bspec256, binMax, bout256);
            testVectorResize(bspecMax, bspec512, binMax, bout512);
            testVectorResize(bspecMax, bspecMax, binMax, boutMax);
        }
    }

    @Test(dataProvider = "byteUnaryOpProvider")
    static void testResizeShort(IntFunction<byte[]> fa) {
        byte[] bin64 = fa.apply(64/Byte.SIZE);
        byte[] bin128 = fa.apply(128/Byte.SIZE);
        byte[] bin256 = fa.apply(256/Byte.SIZE);
        byte[] bin512 = fa.apply(512/Byte.SIZE);
        byte[] binMax = fa.apply(S_Max_BIT.bitSize()/Byte.SIZE);
        byte[] bout64 = new byte[bin64.length];
        byte[] bout128 = new byte[bin128.length];
        byte[] bout256 = new byte[bin256.length];
        byte[] bout512 = new byte[bin512.length];
        byte[] boutMax = new byte[binMax.length];

        for (int i = 0; i < NUM_ITER; i++) {
            testVectorResize(sspec64, sspec64, bin64, bout64);
            testVectorResize(sspec64, sspec128, bin64, bout128);
            testVectorResize(sspec64, sspec256, bin64, bout256);
            testVectorResize(sspec64, sspec512, bin64, bout512);
            testVectorResize(sspec64, sspecMax, bin64, boutMax);

            testVectorResize(sspec128, sspec64, bin128, bout64);
            testVectorResize(sspec128, sspec128, bin128, bout128);
            testVectorResize(sspec128, sspec256, bin128, bout256);
            testVectorResize(sspec128, sspec512, bin128, bout512);
            testVectorResize(sspec128, sspecMax, bin128, boutMax);

            testVectorResize(sspec256, sspec64, bin256, bout64);
            testVectorResize(sspec256, sspec128, bin256, bout128);
            testVectorResize(sspec256, sspec256, bin256, bout256);
            testVectorResize(sspec256, sspec512, bin256, bout512);
            testVectorResize(sspec256, sspecMax, bin256, boutMax);

            testVectorResize(sspec512, sspec64, bin512, bout64);
            testVectorResize(sspec512, sspec128, bin512, bout128);
            testVectorResize(sspec512, sspec256, bin512, bout256);
            testVectorResize(sspec512, sspec512, bin512, bout512);
            testVectorResize(sspec512, sspecMax, bin512, boutMax);

            testVectorResize(sspecMax, sspec64, binMax, bout64);
            testVectorResize(sspecMax, sspec128, binMax, bout128);
            testVectorResize(sspecMax, sspec256, binMax, bout256);
            testVectorResize(sspecMax, sspec512, binMax, bout512);
            testVectorResize(sspecMax, sspecMax, binMax, boutMax);
        }
    }

    @Test(dataProvider = "byteUnaryOpProvider")
    static void testResizeInt(IntFunction<byte[]> fa) {
        byte[] bin64 = fa.apply(64/Byte.SIZE);
        byte[] bin128 = fa.apply(128/Byte.SIZE);
        byte[] bin256 = fa.apply(256/Byte.SIZE);
        byte[] bin512 = fa.apply(512/Byte.SIZE);
        byte[] binMax = fa.apply(S_Max_BIT.bitSize()/Byte.SIZE);
        byte[] bout64 = new byte[bin64.length];
        byte[] bout128 = new byte[bin128.length];
        byte[] bout256 = new byte[bin256.length];
        byte[] bout512 = new byte[bin512.length];
        byte[] boutMax = new byte[binMax.length];

        for (int i = 0; i < NUM_ITER; i++) {
            testVectorResize(ispec64, ispec64, bin64, bout64);
            testVectorResize(ispec64, ispec128, bin64, bout128);
            testVectorResize(ispec64, ispec256, bin64, bout256);
            testVectorResize(ispec64, ispec512, bin64, bout512);
            testVectorResize(ispec64, ispecMax, bin64, boutMax);

            testVectorResize(ispec128, ispec64, bin128, bout64);
            testVectorResize(ispec128, ispec128, bin128, bout128);
            testVectorResize(ispec128, ispec256, bin128, bout256);
            testVectorResize(ispec128, ispec512, bin128, bout512);
            testVectorResize(ispec128, ispecMax, bin128, boutMax);

            testVectorResize(ispec256, ispec64, bin256, bout64);
            testVectorResize(ispec256, ispec128, bin256, bout128);
            testVectorResize(ispec256, ispec256, bin256, bout256);
            testVectorResize(ispec256, ispec512, bin256, bout512);
            testVectorResize(ispec256, ispecMax, bin256, boutMax);

            testVectorResize(ispec512, ispec64, bin512, bout64);
            testVectorResize(ispec512, ispec128, bin512, bout128);
            testVectorResize(ispec512, ispec256, bin512, bout256);
            testVectorResize(ispec512, ispec512, bin512, bout512);
            testVectorResize(ispec512, ispecMax, bin512, boutMax);

            testVectorResize(ispecMax, ispec64, binMax, bout64);
            testVectorResize(ispecMax, ispec128, binMax, bout128);
            testVectorResize(ispecMax, ispec256, binMax, bout256);
            testVectorResize(ispecMax, ispec512, binMax, bout512);
            testVectorResize(ispecMax, ispecMax, binMax, boutMax);
        }
    }

    @Test(dataProvider = "byteUnaryOpProvider")
    static void testResizeLong(IntFunction<byte[]> fa) {
        byte[] bin64 = fa.apply(64/Byte.SIZE);
        byte[] bin128 = fa.apply(128/Byte.SIZE);
        byte[] bin256 = fa.apply(256/Byte.SIZE);
        byte[] bin512 = fa.apply(512/Byte.SIZE);
        byte[] binMax = fa.apply(S_Max_BIT.bitSize()/Byte.SIZE);
        byte[] bout64 = new byte[bin64.length];
        byte[] bout128 = new byte[bin128.length];
        byte[] bout256 = new byte[bin256.length];
        byte[] bout512 = new byte[bin512.length];
        byte[] boutMax = new byte[binMax.length];

        for (int i = 0; i < NUM_ITER; i++) {
            testVectorResize(lspec64, lspec64, bin64, bout64);
            testVectorResize(lspec64, lspec128, bin64, bout128);
            testVectorResize(lspec64, lspec256, bin64, bout256);
            testVectorResize(lspec64, lspec512, bin64, bout512);
            testVectorResize(lspec64, lspecMax, bin64, boutMax);

            testVectorResize(lspec128, lspec64, bin128, bout64);
            testVectorResize(lspec128, lspec128, bin128, bout128);
            testVectorResize(lspec128, lspec256, bin128, bout256);
            testVectorResize(lspec128, lspec512, bin128, bout512);
            testVectorResize(lspec128, lspecMax, bin128, boutMax);

            testVectorResize(lspec256, lspec64, bin256, bout64);
            testVectorResize(lspec256, lspec128, bin256, bout128);
            testVectorResize(lspec256, lspec256, bin256, bout256);
            testVectorResize(lspec256, lspec512, bin256, bout512);
            testVectorResize(lspec256, lspecMax, bin256, boutMax);

            testVectorResize(lspec512, lspec64, bin512, bout64);
            testVectorResize(lspec512, lspec128, bin512, bout128);
            testVectorResize(lspec512, lspec256, bin512, bout256);
            testVectorResize(lspec512, lspec512, bin512, bout512);
            testVectorResize(lspec512, lspecMax, bin512, boutMax);

            testVectorResize(lspecMax, lspec64, binMax, bout64);
            testVectorResize(lspecMax, lspec128, binMax, bout128);
            testVectorResize(lspecMax, lspec256, binMax, bout256);
            testVectorResize(lspecMax, lspec512, binMax, bout512);
            testVectorResize(lspecMax, lspecMax, binMax, boutMax);
        }
    }

    @Test(dataProvider = "byteUnaryOpProvider")
    static void testResizeFloat(IntFunction<byte[]> fa) {
        byte[] bin64 = fa.apply(64/Byte.SIZE);
        byte[] bin128 = fa.apply(128/Byte.SIZE);
        byte[] bin256 = fa.apply(256/Byte.SIZE);
        byte[] bin512 = fa.apply(512/Byte.SIZE);
        byte[] binMax = fa.apply(S_Max_BIT.bitSize()/Byte.SIZE);
        byte[] bout64 = new byte[bin64.length];
        byte[] bout128 = new byte[bin128.length];
        byte[] bout256 = new byte[bin256.length];
        byte[] bout512 = new byte[bin512.length];
        byte[] boutMax = new byte[binMax.length];

        for (int i = 0; i < NUM_ITER; i++) {
            testVectorResize(fspec64, fspec64, bin64, bout64);
            testVectorResize(fspec64, fspec128, bin64, bout128);
            testVectorResize(fspec64, fspec256, bin64, bout256);
            testVectorResize(fspec64, fspec512, bin64, bout512);
            testVectorResize(fspec64, fspecMax, bin64, boutMax);

            testVectorResize(fspec128, fspec64, bin128, bout64);
            testVectorResize(fspec128, fspec128, bin128, bout128);
            testVectorResize(fspec128, fspec256, bin128, bout256);
            testVectorResize(fspec128, fspec512, bin128, bout512);
            testVectorResize(fspec128, fspecMax, bin128, boutMax);

            testVectorResize(fspec256, fspec64, bin256, bout64);
            testVectorResize(fspec256, fspec128, bin256, bout128);
            testVectorResize(fspec256, fspec256, bin256, bout256);
            testVectorResize(fspec256, fspec512, bin256, bout512);
            testVectorResize(fspec256, fspecMax, bin256, boutMax);

            testVectorResize(fspec512, fspec64, bin512, bout64);
            testVectorResize(fspec512, fspec128, bin512, bout128);
            testVectorResize(fspec512, fspec256, bin512, bout256);
            testVectorResize(fspec512, fspec512, bin512, bout512);
            testVectorResize(fspec512, fspecMax, bin512, boutMax);

            testVectorResize(fspecMax, fspec64, binMax, bout64);
            testVectorResize(fspecMax, fspec128, binMax, bout128);
            testVectorResize(fspecMax, fspec256, binMax, bout256);
            testVectorResize(fspecMax, fspec512, binMax, bout512);
            testVectorResize(fspecMax, fspecMax, binMax, boutMax);
        }
    }

    @Test(dataProvider = "byteUnaryOpProvider")
    static void testResizeDouble(IntFunction<byte[]> fa) {
        byte[] bin64 = fa.apply(64/Byte.SIZE);
        byte[] bin128 = fa.apply(128/Byte.SIZE);
        byte[] bin256 = fa.apply(256/Byte.SIZE);
        byte[] bin512 = fa.apply(512/Byte.SIZE);
        byte[] binMax = fa.apply(S_Max_BIT.bitSize()/Byte.SIZE);
        byte[] bout64 = new byte[bin64.length];
        byte[] bout128 = new byte[bin128.length];
        byte[] bout256 = new byte[bin256.length];
        byte[] bout512 = new byte[bin512.length];
        byte[] boutMax = new byte[binMax.length];

        for (int i = 0; i < NUM_ITER; i++) {
            testVectorResize(dspec64, dspec64, bin64, bout64);
            testVectorResize(dspec64, dspec128, bin64, bout128);
            testVectorResize(dspec64, dspec256, bin64, bout256);
            testVectorResize(dspec64, dspec512, bin64, bout512);
            testVectorResize(dspec64, dspecMax, bin64, boutMax);

            testVectorResize(dspec128, dspec64, bin128, bout64);
            testVectorResize(dspec128, dspec128, bin128, bout128);
            testVectorResize(dspec128, dspec256, bin128, bout256);
            testVectorResize(dspec128, dspec512, bin128, bout512);
            testVectorResize(dspec128, dspecMax, bin128, boutMax);

            testVectorResize(dspec256, dspec64, bin256, bout64);
            testVectorResize(dspec256, dspec128, bin256, bout128);
            testVectorResize(dspec256, dspec256, bin256, bout256);
            testVectorResize(dspec256, dspec512, bin256, bout512);
            testVectorResize(dspec256, dspecMax, bin256, boutMax);

            testVectorResize(dspec512, dspec64, bin512, bout64);
            testVectorResize(dspec512, dspec128, bin512, bout128);
            testVectorResize(dspec512, dspec256, bin512, bout256);
            testVectorResize(dspec512, dspec512, bin512, bout512);
            testVectorResize(dspec512, dspecMax, bin512, boutMax);

            testVectorResize(dspecMax, dspec64, binMax, bout64);
            testVectorResize(dspecMax, dspec128, binMax, bout128);
            testVectorResize(dspecMax, dspec256, binMax, bout256);
            testVectorResize(dspecMax, dspec512, binMax, bout512);
            testVectorResize(dspecMax, dspecMax, binMax, boutMax);
        }
    }

    @ForceInline
    static <E,F>
    void testVectorRebracket(Vector.Species<E> a, Vector.Species<F> b, byte[] input, byte[] output) {
       assert(input.length == output.length);

        Vector<E> av = a.fromByteArray(input, 0);
        Vector<F> bv = b.rebracket(av);
        bv.intoByteArray(output, 0);

        Assert.assertEquals(input, output);
    }

    @Test(dataProvider = "byteUnaryOpProvider")
    static void testRebracket64(IntFunction<byte[]> fa) {
        byte[] barr = fa.apply(64/Byte.SIZE);
        byte[] bout = new byte[barr.length];
        for (int i = 0; i < NUM_ITER; i++) {
            testVectorRebracket(bspec64, bspec64, barr, bout);
            testVectorRebracket(bspec64, sspec64, barr, bout);
            testVectorRebracket(bspec64, ispec64, barr, bout);
            testVectorRebracket(bspec64, lspec64, barr, bout);
            testVectorRebracket(bspec64, fspec64, barr, bout);
            testVectorRebracket(bspec64, dspec64, barr, bout);

            testVectorRebracket(sspec64, bspec64, barr, bout);
            testVectorRebracket(sspec64, sspec64, barr, bout);
            testVectorRebracket(sspec64, ispec64, barr, bout);
            testVectorRebracket(sspec64, lspec64, barr, bout);
            testVectorRebracket(sspec64, fspec64, barr, bout);
            testVectorRebracket(sspec64, dspec64, barr, bout);

            testVectorRebracket(ispec64, bspec64, barr, bout);
            testVectorRebracket(ispec64, sspec64, barr, bout);
            testVectorRebracket(ispec64, ispec64, barr, bout);
            testVectorRebracket(ispec64, lspec64, barr, bout);
            testVectorRebracket(ispec64, fspec64, barr, bout);
            testVectorRebracket(ispec64, dspec64, barr, bout);

            testVectorRebracket(lspec64, bspec64, barr, bout);
            testVectorRebracket(lspec64, sspec64, barr, bout);
            testVectorRebracket(lspec64, ispec64, barr, bout);
            testVectorRebracket(lspec64, lspec64, barr, bout);
            testVectorRebracket(lspec64, fspec64, barr, bout);
            testVectorRebracket(lspec64, dspec64, barr, bout);

            testVectorRebracket(fspec64, bspec64, barr, bout);
            testVectorRebracket(fspec64, sspec64, barr, bout);
            testVectorRebracket(fspec64, ispec64, barr, bout);
            testVectorRebracket(fspec64, lspec64, barr, bout);
            testVectorRebracket(fspec64, fspec64, barr, bout);
            testVectorRebracket(fspec64, dspec64, barr, bout);

            testVectorRebracket(dspec64, bspec64, barr, bout);
            testVectorRebracket(dspec64, sspec64, barr, bout);
            testVectorRebracket(dspec64, ispec64, barr, bout);
            testVectorRebracket(dspec64, lspec64, barr, bout);
            testVectorRebracket(dspec64, fspec64, barr, bout);
            testVectorRebracket(dspec64, dspec64, barr, bout);
        }
    }

    @Test(dataProvider = "byteUnaryOpProvider")
    static void testRebracket128(IntFunction<byte[]> fa) {
        byte[] barr = fa.apply(128/Byte.SIZE);
        byte[] bout = new byte[barr.length];
        for (int i = 0; i < NUM_ITER; i++) {
            testVectorRebracket(bspec128, bspec128, barr, bout);
            testVectorRebracket(bspec128, sspec128, barr, bout);
            testVectorRebracket(bspec128, ispec128, barr, bout);
            testVectorRebracket(bspec128, lspec128, barr, bout);
            testVectorRebracket(bspec128, fspec128, barr, bout);
            testVectorRebracket(bspec128, dspec128, barr, bout);

            testVectorRebracket(sspec128, bspec128, barr, bout);
            testVectorRebracket(sspec128, sspec128, barr, bout);
            testVectorRebracket(sspec128, ispec128, barr, bout);
            testVectorRebracket(sspec128, lspec128, barr, bout);
            testVectorRebracket(sspec128, fspec128, barr, bout);
            testVectorRebracket(sspec128, dspec128, barr, bout);

            testVectorRebracket(ispec128, bspec128, barr, bout);
            testVectorRebracket(ispec128, sspec128, barr, bout);
            testVectorRebracket(ispec128, ispec128, barr, bout);
            testVectorRebracket(ispec128, lspec128, barr, bout);
            testVectorRebracket(ispec128, fspec128, barr, bout);
            testVectorRebracket(ispec128, dspec128, barr, bout);

            testVectorRebracket(lspec128, bspec128, barr, bout);
            testVectorRebracket(lspec128, sspec128, barr, bout);
            testVectorRebracket(lspec128, ispec128, barr, bout);
            testVectorRebracket(lspec128, lspec128, barr, bout);
            testVectorRebracket(lspec128, fspec128, barr, bout);
            testVectorRebracket(lspec128, dspec128, barr, bout);

            testVectorRebracket(fspec128, bspec128, barr, bout);
            testVectorRebracket(fspec128, sspec128, barr, bout);
            testVectorRebracket(fspec128, ispec128, barr, bout);
            testVectorRebracket(fspec128, lspec128, barr, bout);
            testVectorRebracket(fspec128, fspec128, barr, bout);
            testVectorRebracket(fspec128, dspec128, barr, bout);

            testVectorRebracket(dspec128, bspec128, barr, bout);
            testVectorRebracket(dspec128, sspec128, barr, bout);
            testVectorRebracket(dspec128, ispec128, barr, bout);
            testVectorRebracket(dspec128, lspec128, barr, bout);
            testVectorRebracket(dspec128, fspec128, barr, bout);
            testVectorRebracket(dspec128, dspec128, barr, bout);
        }
    }

    @Test(dataProvider = "byteUnaryOpProvider")
    static void testRebracket256(IntFunction<byte[]> fa) {
        byte[] barr = fa.apply(256/Byte.SIZE);
        byte[] bout = new byte[barr.length];
        for (int i = 0; i < NUM_ITER; i++) {
            testVectorRebracket(bspec256, bspec256, barr, bout);
            testVectorRebracket(bspec256, sspec256, barr, bout);
            testVectorRebracket(bspec256, ispec256, barr, bout);
            testVectorRebracket(bspec256, lspec256, barr, bout);
            testVectorRebracket(bspec256, fspec256, barr, bout);
            testVectorRebracket(bspec256, dspec256, barr, bout);

            testVectorRebracket(sspec256, bspec256, barr, bout);
            testVectorRebracket(sspec256, sspec256, barr, bout);
            testVectorRebracket(sspec256, ispec256, barr, bout);
            testVectorRebracket(sspec256, lspec256, barr, bout);
            testVectorRebracket(sspec256, fspec256, barr, bout);
            testVectorRebracket(sspec256, dspec256, barr, bout);

            testVectorRebracket(ispec256, bspec256, barr, bout);
            testVectorRebracket(ispec256, sspec256, barr, bout);
            testVectorRebracket(ispec256, ispec256, barr, bout);
            testVectorRebracket(ispec256, lspec256, barr, bout);
            testVectorRebracket(ispec256, fspec256, barr, bout);
            testVectorRebracket(ispec256, dspec256, barr, bout);

            testVectorRebracket(lspec256, bspec256, barr, bout);
            testVectorRebracket(lspec256, sspec256, barr, bout);
            testVectorRebracket(lspec256, ispec256, barr, bout);
            testVectorRebracket(lspec256, lspec256, barr, bout);
            testVectorRebracket(lspec256, fspec256, barr, bout);
            testVectorRebracket(lspec256, dspec256, barr, bout);

            testVectorRebracket(fspec256, bspec256, barr, bout);
            testVectorRebracket(fspec256, sspec256, barr, bout);
            testVectorRebracket(fspec256, ispec256, barr, bout);
            testVectorRebracket(fspec256, lspec256, barr, bout);
            testVectorRebracket(fspec256, fspec256, barr, bout);
            testVectorRebracket(fspec256, dspec256, barr, bout);

            testVectorRebracket(dspec256, bspec256, barr, bout);
            testVectorRebracket(dspec256, sspec256, barr, bout);
            testVectorRebracket(dspec256, ispec256, barr, bout);
            testVectorRebracket(dspec256, lspec256, barr, bout);
            testVectorRebracket(dspec256, fspec256, barr, bout);
            testVectorRebracket(dspec256, dspec256, barr, bout);
        }
    }

    @Test(dataProvider = "byteUnaryOpProvider")
    static void testRebracket512(IntFunction<byte[]> fa) {
        byte[] barr = fa.apply(512/Byte.SIZE);
        byte[] bout = new byte[barr.length];
        for (int i = 0; i < NUM_ITER; i++) {
            testVectorRebracket(bspec512, bspec512, barr, bout);
            testVectorRebracket(bspec512, sspec512, barr, bout);
            testVectorRebracket(bspec512, ispec512, barr, bout);
            testVectorRebracket(bspec512, lspec512, barr, bout);
            testVectorRebracket(bspec512, fspec512, barr, bout);
            testVectorRebracket(bspec512, dspec512, barr, bout);

            testVectorRebracket(sspec512, bspec512, barr, bout);
            testVectorRebracket(sspec512, sspec512, barr, bout);
            testVectorRebracket(sspec512, ispec512, barr, bout);
            testVectorRebracket(sspec512, lspec512, barr, bout);
            testVectorRebracket(sspec512, fspec512, barr, bout);
            testVectorRebracket(sspec512, dspec512, barr, bout);

            testVectorRebracket(ispec512, bspec512, barr, bout);
            testVectorRebracket(ispec512, sspec512, barr, bout);
            testVectorRebracket(ispec512, ispec512, barr, bout);
            testVectorRebracket(ispec512, lspec512, barr, bout);
            testVectorRebracket(ispec512, fspec512, barr, bout);
            testVectorRebracket(ispec512, dspec512, barr, bout);

            testVectorRebracket(lspec512, bspec512, barr, bout);
            testVectorRebracket(lspec512, sspec512, barr, bout);
            testVectorRebracket(lspec512, ispec512, barr, bout);
            testVectorRebracket(lspec512, lspec512, barr, bout);
            testVectorRebracket(lspec512, fspec512, barr, bout);
            testVectorRebracket(lspec512, dspec512, barr, bout);

            testVectorRebracket(fspec512, bspec512, barr, bout);
            testVectorRebracket(fspec512, sspec512, barr, bout);
            testVectorRebracket(fspec512, ispec512, barr, bout);
            testVectorRebracket(fspec512, lspec512, barr, bout);
            testVectorRebracket(fspec512, fspec512, barr, bout);
            testVectorRebracket(fspec512, dspec512, barr, bout);

            testVectorRebracket(dspec512, bspec512, barr, bout);
            testVectorRebracket(dspec512, sspec512, barr, bout);
            testVectorRebracket(dspec512, ispec512, barr, bout);
            testVectorRebracket(dspec512, lspec512, barr, bout);
            testVectorRebracket(dspec512, fspec512, barr, bout);
            testVectorRebracket(dspec512, dspec512, barr, bout);
        }
    }

    @Test(dataProvider = "byteUnaryOpProvider")
    static void testRebracketMax(IntFunction<byte[]> fa) {
        byte[] barr = fa.apply(S_Max_BIT.bitSize()/Byte.SIZE);
        byte[] bout = new byte[barr.length];
        for (int i = 0; i < NUM_ITER; i++) {
            testVectorRebracket(bspecMax, bspecMax, barr, bout);
            testVectorRebracket(bspecMax, sspecMax, barr, bout);
            testVectorRebracket(bspecMax, ispecMax, barr, bout);
            testVectorRebracket(bspecMax, lspecMax, barr, bout);
            testVectorRebracket(bspecMax, fspecMax, barr, bout);
            testVectorRebracket(bspecMax, dspecMax, barr, bout);

            testVectorRebracket(sspecMax, bspecMax, barr, bout);
            testVectorRebracket(sspecMax, sspecMax, barr, bout);
            testVectorRebracket(sspecMax, ispecMax, barr, bout);
            testVectorRebracket(sspecMax, lspecMax, barr, bout);
            testVectorRebracket(sspecMax, fspecMax, barr, bout);
            testVectorRebracket(sspecMax, dspecMax, barr, bout);

            testVectorRebracket(ispecMax, bspecMax, barr, bout);
            testVectorRebracket(ispecMax, sspecMax, barr, bout);
            testVectorRebracket(ispecMax, ispecMax, barr, bout);
            testVectorRebracket(ispecMax, lspecMax, barr, bout);
            testVectorRebracket(ispecMax, fspecMax, barr, bout);
            testVectorRebracket(ispecMax, dspecMax, barr, bout);

            testVectorRebracket(lspecMax, bspecMax, barr, bout);
            testVectorRebracket(lspecMax, sspecMax, barr, bout);
            testVectorRebracket(lspecMax, ispecMax, barr, bout);
            testVectorRebracket(lspecMax, lspecMax, barr, bout);
            testVectorRebracket(lspecMax, fspecMax, barr, bout);
            testVectorRebracket(lspecMax, dspecMax, barr, bout);

            testVectorRebracket(fspecMax, bspecMax, barr, bout);
            testVectorRebracket(fspecMax, sspecMax, barr, bout);
            testVectorRebracket(fspecMax, ispecMax, barr, bout);
            testVectorRebracket(fspecMax, lspecMax, barr, bout);
            testVectorRebracket(fspecMax, fspecMax, barr, bout);
            testVectorRebracket(fspecMax, dspecMax, barr, bout);

            testVectorRebracket(dspecMax, bspecMax, barr, bout);
            testVectorRebracket(dspecMax, sspecMax, barr, bout);
            testVectorRebracket(dspecMax, ispecMax, barr, bout);
            testVectorRebracket(dspecMax, lspecMax, barr, bout);
            testVectorRebracket(dspecMax, fspecMax, barr, bout);
            testVectorRebracket(dspecMax, dspecMax, barr, bout);
        }
    }

    @ForceInline
    static 
    void testVectorCastByteToFloat(ByteVector.ByteSpecies a, FloatVector.FloatSpecies b, byte[] input, float[] output) {
        assert(input.length == a.length());
        assert(output.length == b.length());

        ByteVector av = a.fromArray(input, 0);
        FloatVector bv = b.cast(av);
        bv.intoArray(output, 0);

        for (int i = 0; i < Math.min(input.length, output.length); i++) {
            Assert.assertEquals(output[i], (float)input[i]);
        }
        for(int i = input.length; i < output.length; i++) {
            Assert.assertEquals(output[i], (float)0);
        }
    }

    @ForceInline
    static 
    void testVectorCastByteToFloatFail(ByteVector.ByteSpecies a, FloatVector.FloatSpecies b, byte[] input) {
        assert(input.length == a.length());

        ByteVector av = a.fromArray(input, 0);
        try {
            b.cast(av);
            Assert.fail(String.format(
                    "Cast failed to throw IllegalArgumentException for differing species lengths for %s and %s",
                    a, b));
        } catch (IllegalArgumentException e) {
        }
    }

    @ForceInline
    static 
    void testVectorCastShortToFloat(ShortVector.ShortSpecies a, FloatVector.FloatSpecies b, short[] input, float[] output) {
        assert(input.length == a.length());
        assert(output.length == b.length());

        ShortVector av = a.fromArray(input, 0);
        FloatVector bv = b.cast(av);
        bv.intoArray(output, 0);

        for (int i = 0; i < Math.min(input.length, output.length); i++) {
            Assert.assertEquals(output[i], (float)input[i]);
        }
        for(int i = input.length; i < output.length; i++) {
            Assert.assertEquals(output[i], (float)0);
        }
    }

    @ForceInline
    static 
    void testVectorCastShortToFloatFail(ShortVector.ShortSpecies a, FloatVector.FloatSpecies b, short[] input) {
        assert(input.length == a.length());

        ShortVector av = a.fromArray(input, 0);
        try {
            b.cast(av);
            Assert.fail(String.format(
                    "Cast failed to throw IllegalArgumentException for differing species lengths for %s and %s",
                    a, b));
        } catch (IllegalArgumentException e) {
        }
    }

    @ForceInline
    static 
    void testVectorCastIntToFloat(IntVector.IntSpecies a, FloatVector.FloatSpecies b, int[] input, float[] output) {
        assert(input.length == a.length());
        assert(output.length == b.length());

        IntVector av = a.fromArray(input, 0);
        FloatVector bv = b.cast(av);
        bv.intoArray(output, 0);

        for (int i = 0; i < Math.min(input.length, output.length); i++) {
            Assert.assertEquals(output[i], (float)input[i]);
        }
        for(int i = input.length; i < output.length; i++) {
            Assert.assertEquals(output[i], (float)0);
        }
    }

    @ForceInline
    static 
    void testVectorCastIntToFloatFail(IntVector.IntSpecies a, FloatVector.FloatSpecies b, int[] input) {
        assert(input.length == a.length());

        IntVector av = a.fromArray(input, 0);
        try {
            b.cast(av);
            Assert.fail(String.format(
                    "Cast failed to throw IllegalArgumentException for differing species lengths for %s and %s",
                    a, b));
        } catch (IllegalArgumentException e) {
        }
    }

    @ForceInline
    static 
    void testVectorCastLongToFloat(LongVector.LongSpecies a, FloatVector.FloatSpecies b, long[] input, float[] output) {
        assert(input.length == a.length());
        assert(output.length == b.length());

        LongVector av = a.fromArray(input, 0);
        FloatVector bv = b.cast(av);
        bv.intoArray(output, 0);

        for (int i = 0; i < Math.min(input.length, output.length); i++) {
            Assert.assertEquals(output[i], (float)input[i]);
        }
        for(int i = input.length; i < output.length; i++) {
            Assert.assertEquals(output[i], (float)0);
        }
    }

    @ForceInline
    static 
    void testVectorCastLongToFloatFail(LongVector.LongSpecies a, FloatVector.FloatSpecies b, long[] input) {
        assert(input.length == a.length());

        LongVector av = a.fromArray(input, 0);
        try {
            b.cast(av);
            Assert.fail(String.format(
                    "Cast failed to throw IllegalArgumentException for differing species lengths for %s and %s",
                    a, b));
        } catch (IllegalArgumentException e) {
        }
    }

    @ForceInline
    static 
    void testVectorCastFloatToFloat(FloatVector.FloatSpecies a, FloatVector.FloatSpecies b, float[] input, float[] output) {
        assert(input.length == a.length());
        assert(output.length == b.length());

        FloatVector av = a.fromArray(input, 0);
        FloatVector bv = b.cast(av);
        bv.intoArray(output, 0);

        for (int i = 0; i < Math.min(input.length, output.length); i++) {
            Assert.assertEquals(output[i], (float)input[i]);
        }
        for(int i = input.length; i < output.length; i++) {
            Assert.assertEquals(output[i], (float)0);
        }
    }

    @ForceInline
    static 
    void testVectorCastFloatToFloatFail(FloatVector.FloatSpecies a, FloatVector.FloatSpecies b, float[] input) {
        assert(input.length == a.length());

        FloatVector av = a.fromArray(input, 0);
        try {
            b.cast(av);
            Assert.fail(String.format(
                    "Cast failed to throw IllegalArgumentException for differing species lengths for %s and %s",
                    a, b));
        } catch (IllegalArgumentException e) {
        }
    }

    @ForceInline
    static 
    void testVectorCastDoubleToFloat(DoubleVector.DoubleSpecies a, FloatVector.FloatSpecies b, double[] input, float[] output) {
        assert(input.length == a.length());
        assert(output.length == b.length());

        DoubleVector av = a.fromArray(input, 0);
        FloatVector bv = b.cast(av);
        bv.intoArray(output, 0);

        for (int i = 0; i < Math.min(input.length, output.length); i++) {
            Assert.assertEquals(output[i], (float)input[i]);
        }
        for(int i = input.length; i < output.length; i++) {
            Assert.assertEquals(output[i], (float)0);
        }
    }

    @ForceInline
    static 
    void testVectorCastDoubleToFloatFail(DoubleVector.DoubleSpecies a, FloatVector.FloatSpecies b, double[] input) {
        assert(input.length == a.length());

        DoubleVector av = a.fromArray(input, 0);
        try {
            b.cast(av);
            Assert.fail(String.format(
                    "Cast failed to throw IllegalArgumentException for differing species lengths for %s and %s",
                    a, b));
        } catch (IllegalArgumentException e) {
        }
    }

    @ForceInline
    static 
    void testVectorCastByteToByte(ByteVector.ByteSpecies a, ByteVector.ByteSpecies b, byte[] input, byte[] output) {
        assert(input.length == a.length());
        assert(output.length == b.length());

        ByteVector av = a.fromArray(input, 0);
        ByteVector bv = b.cast(av);
        bv.intoArray(output, 0);

        for (int i = 0; i < Math.min(input.length, output.length); i++) {
            Assert.assertEquals(output[i], (byte)input[i]);
        }
        for(int i = input.length; i < output.length; i++) {
            Assert.assertEquals(output[i], (byte)0);
        }
    }

    @ForceInline
    static 
    void testVectorCastByteToByteFail(ByteVector.ByteSpecies a, ByteVector.ByteSpecies b, byte[] input) {
        assert(input.length == a.length());

        ByteVector av = a.fromArray(input, 0);
        try {
            b.cast(av);
            Assert.fail(String.format(
                    "Cast failed to throw IllegalArgumentException for differing species lengths for %s and %s",
                    a, b));
        } catch (IllegalArgumentException e) {
        }
    }

    @ForceInline
    static 
    void testVectorCastShortToByte(ShortVector.ShortSpecies a, ByteVector.ByteSpecies b, short[] input, byte[] output) {
        assert(input.length == a.length());
        assert(output.length == b.length());

        ShortVector av = a.fromArray(input, 0);
        ByteVector bv = b.cast(av);
        bv.intoArray(output, 0);

        for (int i = 0; i < Math.min(input.length, output.length); i++) {
            Assert.assertEquals(output[i], (byte)input[i]);
        }
        for(int i = input.length; i < output.length; i++) {
            Assert.assertEquals(output[i], (byte)0);
        }
    }

    @ForceInline
    static 
    void testVectorCastShortToByteFail(ShortVector.ShortSpecies a, ByteVector.ByteSpecies b, short[] input) {
        assert(input.length == a.length());

        ShortVector av = a.fromArray(input, 0);
        try {
            b.cast(av);
            Assert.fail(String.format(
                    "Cast failed to throw IllegalArgumentException for differing species lengths for %s and %s",
                    a, b));
        } catch (IllegalArgumentException e) {
        }
    }

    @ForceInline
    static 
    void testVectorCastIntToByte(IntVector.IntSpecies a, ByteVector.ByteSpecies b, int[] input, byte[] output) {
        assert(input.length == a.length());
        assert(output.length == b.length());

        IntVector av = a.fromArray(input, 0);
        ByteVector bv = b.cast(av);
        bv.intoArray(output, 0);

        for (int i = 0; i < Math.min(input.length, output.length); i++) {
            Assert.assertEquals(output[i], (byte)input[i]);
        }
        for(int i = input.length; i < output.length; i++) {
            Assert.assertEquals(output[i], (byte)0);
        }
    }

    @ForceInline
    static 
    void testVectorCastIntToByteFail(IntVector.IntSpecies a, ByteVector.ByteSpecies b, int[] input) {
        assert(input.length == a.length());

        IntVector av = a.fromArray(input, 0);
        try {
            b.cast(av);
            Assert.fail(String.format(
                    "Cast failed to throw IllegalArgumentException for differing species lengths for %s and %s",
                    a, b));
        } catch (IllegalArgumentException e) {
        }
    }

    @ForceInline
    static 
    void testVectorCastLongToByte(LongVector.LongSpecies a, ByteVector.ByteSpecies b, long[] input, byte[] output) {
        assert(input.length == a.length());
        assert(output.length == b.length());

        LongVector av = a.fromArray(input, 0);
        ByteVector bv = b.cast(av);
        bv.intoArray(output, 0);

        for (int i = 0; i < Math.min(input.length, output.length); i++) {
            Assert.assertEquals(output[i], (byte)input[i]);
        }
        for(int i = input.length; i < output.length; i++) {
            Assert.assertEquals(output[i], (byte)0);
        }
    }

    @ForceInline
    static 
    void testVectorCastLongToByteFail(LongVector.LongSpecies a, ByteVector.ByteSpecies b, long[] input) {
        assert(input.length == a.length());

        LongVector av = a.fromArray(input, 0);
        try {
            b.cast(av);
            Assert.fail(String.format(
                    "Cast failed to throw IllegalArgumentException for differing species lengths for %s and %s",
                    a, b));
        } catch (IllegalArgumentException e) {
        }
    }

    @ForceInline
    static 
    void testVectorCastFloatToByte(FloatVector.FloatSpecies a, ByteVector.ByteSpecies b, float[] input, byte[] output) {
        assert(input.length == a.length());
        assert(output.length == b.length());

        FloatVector av = a.fromArray(input, 0);
        ByteVector bv = b.cast(av);
        bv.intoArray(output, 0);

        for (int i = 0; i < Math.min(input.length, output.length); i++) {
            Assert.assertEquals(output[i], (byte)input[i]);
        }
        for(int i = input.length; i < output.length; i++) {
            Assert.assertEquals(output[i], (byte)0);
        }
    }

    @ForceInline
    static 
    void testVectorCastFloatToByteFail(FloatVector.FloatSpecies a, ByteVector.ByteSpecies b, float[] input) {
        assert(input.length == a.length());

        FloatVector av = a.fromArray(input, 0);
        try {
            b.cast(av);
            Assert.fail(String.format(
                    "Cast failed to throw IllegalArgumentException for differing species lengths for %s and %s",
                    a, b));
        } catch (IllegalArgumentException e) {
        }
    }

    @ForceInline
    static 
    void testVectorCastDoubleToByte(DoubleVector.DoubleSpecies a, ByteVector.ByteSpecies b, double[] input, byte[] output) {
        assert(input.length == a.length());
        assert(output.length == b.length());

        DoubleVector av = a.fromArray(input, 0);
        ByteVector bv = b.cast(av);
        bv.intoArray(output, 0);

        for (int i = 0; i < Math.min(input.length, output.length); i++) {
            Assert.assertEquals(output[i], (byte)input[i]);
        }
        for(int i = input.length; i < output.length; i++) {
            Assert.assertEquals(output[i], (byte)0);
        }
    }

    @ForceInline
    static 
    void testVectorCastDoubleToByteFail(DoubleVector.DoubleSpecies a, ByteVector.ByteSpecies b, double[] input) {
        assert(input.length == a.length());

        DoubleVector av = a.fromArray(input, 0);
        try {
            b.cast(av);
            Assert.fail(String.format(
                    "Cast failed to throw IllegalArgumentException for differing species lengths for %s and %s",
                    a, b));
        } catch (IllegalArgumentException e) {
        }
    }

    @ForceInline
    static 
    void testVectorCastByteToShort(ByteVector.ByteSpecies a, ShortVector.ShortSpecies b, byte[] input, short[] output) {
        assert(input.length == a.length());
        assert(output.length == b.length());

        ByteVector av = a.fromArray(input, 0);
        ShortVector bv = b.cast(av);
        bv.intoArray(output, 0);

        for (int i = 0; i < Math.min(input.length, output.length); i++) {
            Assert.assertEquals(output[i], (short)input[i]);
        }
        for(int i = input.length; i < output.length; i++) {
            Assert.assertEquals(output[i], (short)0);
        }
    }

    @ForceInline
    static 
    void testVectorCastByteToShortFail(ByteVector.ByteSpecies a, ShortVector.ShortSpecies b, byte[] input) {
        assert(input.length == a.length());

        ByteVector av = a.fromArray(input, 0);
        try {
            b.cast(av);
            Assert.fail(String.format(
                    "Cast failed to throw IllegalArgumentException for differing species lengths for %s and %s",
                    a, b));
        } catch (IllegalArgumentException e) {
        }
    }

    @ForceInline
    static 
    void testVectorCastShortToShort(ShortVector.ShortSpecies a, ShortVector.ShortSpecies b, short[] input, short[] output) {
        assert(input.length == a.length());
        assert(output.length == b.length());

        ShortVector av = a.fromArray(input, 0);
        ShortVector bv = b.cast(av);
        bv.intoArray(output, 0);

        for (int i = 0; i < Math.min(input.length, output.length); i++) {
            Assert.assertEquals(output[i], (short)input[i]);
        }
        for(int i = input.length; i < output.length; i++) {
            Assert.assertEquals(output[i], (short)0);
        }
    }

    @ForceInline
    static 
    void testVectorCastShortToShortFail(ShortVector.ShortSpecies a, ShortVector.ShortSpecies b, short[] input) {
        assert(input.length == a.length());

        ShortVector av = a.fromArray(input, 0);
        try {
            b.cast(av);
            Assert.fail(String.format(
                    "Cast failed to throw IllegalArgumentException for differing species lengths for %s and %s",
                    a, b));
        } catch (IllegalArgumentException e) {
        }
    }

    @ForceInline
    static 
    void testVectorCastIntToShort(IntVector.IntSpecies a, ShortVector.ShortSpecies b, int[] input, short[] output) {
        assert(input.length == a.length());
        assert(output.length == b.length());

        IntVector av = a.fromArray(input, 0);
        ShortVector bv = b.cast(av);
        bv.intoArray(output, 0);

        for (int i = 0; i < Math.min(input.length, output.length); i++) {
            Assert.assertEquals(output[i], (short)input[i]);
        }
        for(int i = input.length; i < output.length; i++) {
            Assert.assertEquals(output[i], (short)0);
        }
    }

    @ForceInline
    static 
    void testVectorCastIntToShortFail(IntVector.IntSpecies a, ShortVector.ShortSpecies b, int[] input) {
        assert(input.length == a.length());

        IntVector av = a.fromArray(input, 0);
        try {
            b.cast(av);
            Assert.fail(String.format(
                    "Cast failed to throw IllegalArgumentException for differing species lengths for %s and %s",
                    a, b));
        } catch (IllegalArgumentException e) {
        }
    }

    @ForceInline
    static 
    void testVectorCastLongToShort(LongVector.LongSpecies a, ShortVector.ShortSpecies b, long[] input, short[] output) {
        assert(input.length == a.length());
        assert(output.length == b.length());

        LongVector av = a.fromArray(input, 0);
        ShortVector bv = b.cast(av);
        bv.intoArray(output, 0);

        for (int i = 0; i < Math.min(input.length, output.length); i++) {
            Assert.assertEquals(output[i], (short)input[i]);
        }
        for(int i = input.length; i < output.length; i++) {
            Assert.assertEquals(output[i], (short)0);
        }
    }

    @ForceInline
    static 
    void testVectorCastLongToShortFail(LongVector.LongSpecies a, ShortVector.ShortSpecies b, long[] input) {
        assert(input.length == a.length());

        LongVector av = a.fromArray(input, 0);
        try {
            b.cast(av);
            Assert.fail(String.format(
                    "Cast failed to throw IllegalArgumentException for differing species lengths for %s and %s",
                    a, b));
        } catch (IllegalArgumentException e) {
        }
    }

    @ForceInline
    static 
    void testVectorCastFloatToShort(FloatVector.FloatSpecies a, ShortVector.ShortSpecies b, float[] input, short[] output) {
        assert(input.length == a.length());
        assert(output.length == b.length());

        FloatVector av = a.fromArray(input, 0);
        ShortVector bv = b.cast(av);
        bv.intoArray(output, 0);

        for (int i = 0; i < Math.min(input.length, output.length); i++) {
            Assert.assertEquals(output[i], (short)input[i]);
        }
        for(int i = input.length; i < output.length; i++) {
            Assert.assertEquals(output[i], (short)0);
        }
    }

    @ForceInline
    static 
    void testVectorCastFloatToShortFail(FloatVector.FloatSpecies a, ShortVector.ShortSpecies b, float[] input) {
        assert(input.length == a.length());

        FloatVector av = a.fromArray(input, 0);
        try {
            b.cast(av);
            Assert.fail(String.format(
                    "Cast failed to throw IllegalArgumentException for differing species lengths for %s and %s",
                    a, b));
        } catch (IllegalArgumentException e) {
        }
    }

    @ForceInline
    static 
    void testVectorCastDoubleToShort(DoubleVector.DoubleSpecies a, ShortVector.ShortSpecies b, double[] input, short[] output) {
        assert(input.length == a.length());
        assert(output.length == b.length());

        DoubleVector av = a.fromArray(input, 0);
        ShortVector bv = b.cast(av);
        bv.intoArray(output, 0);

        for (int i = 0; i < Math.min(input.length, output.length); i++) {
            Assert.assertEquals(output[i], (short)input[i]);
        }
        for(int i = input.length; i < output.length; i++) {
            Assert.assertEquals(output[i], (short)0);
        }
    }

    @ForceInline
    static 
    void testVectorCastDoubleToShortFail(DoubleVector.DoubleSpecies a, ShortVector.ShortSpecies b, double[] input) {
        assert(input.length == a.length());

        DoubleVector av = a.fromArray(input, 0);
        try {
            b.cast(av);
            Assert.fail(String.format(
                    "Cast failed to throw IllegalArgumentException for differing species lengths for %s and %s",
                    a, b));
        } catch (IllegalArgumentException e) {
        }
    }

    @ForceInline
    static 
    void testVectorCastByteToInt(ByteVector.ByteSpecies a, IntVector.IntSpecies b, byte[] input, int[] output) {
        assert(input.length == a.length());
        assert(output.length == b.length());

        ByteVector av = a.fromArray(input, 0);
        IntVector bv = b.cast(av);
        bv.intoArray(output, 0);

        for (int i = 0; i < Math.min(input.length, output.length); i++) {
            Assert.assertEquals(output[i], (int)input[i]);
        }
        for(int i = input.length; i < output.length; i++) {
            Assert.assertEquals(output[i], (int)0);
        }
    }

    @ForceInline
    static 
    void testVectorCastByteToIntFail(ByteVector.ByteSpecies a, IntVector.IntSpecies b, byte[] input) {
        assert(input.length == a.length());

        ByteVector av = a.fromArray(input, 0);
        try {
            b.cast(av);
            Assert.fail(String.format(
                    "Cast failed to throw IllegalArgumentException for differing species lengths for %s and %s",
                    a, b));
        } catch (IllegalArgumentException e) {
        }
    }

    @ForceInline
    static 
    void testVectorCastShortToInt(ShortVector.ShortSpecies a, IntVector.IntSpecies b, short[] input, int[] output) {
        assert(input.length == a.length());
        assert(output.length == b.length());

        ShortVector av = a.fromArray(input, 0);
        IntVector bv = b.cast(av);
        bv.intoArray(output, 0);

        for (int i = 0; i < Math.min(input.length, output.length); i++) {
            Assert.assertEquals(output[i], (int)input[i]);
        }
        for(int i = input.length; i < output.length; i++) {
            Assert.assertEquals(output[i], (int)0);
        }
    }

    @ForceInline
    static 
    void testVectorCastShortToIntFail(ShortVector.ShortSpecies a, IntVector.IntSpecies b, short[] input) {
        assert(input.length == a.length());

        ShortVector av = a.fromArray(input, 0);
        try {
            b.cast(av);
            Assert.fail(String.format(
                    "Cast failed to throw IllegalArgumentException for differing species lengths for %s and %s",
                    a, b));
        } catch (IllegalArgumentException e) {
        }
    }

    @ForceInline
    static 
    void testVectorCastIntToInt(IntVector.IntSpecies a, IntVector.IntSpecies b, int[] input, int[] output) {
        assert(input.length == a.length());
        assert(output.length == b.length());

        IntVector av = a.fromArray(input, 0);
        IntVector bv = b.cast(av);
        bv.intoArray(output, 0);

        for (int i = 0; i < Math.min(input.length, output.length); i++) {
            Assert.assertEquals(output[i], (int)input[i]);
        }
        for(int i = input.length; i < output.length; i++) {
            Assert.assertEquals(output[i], (int)0);
        }
    }

    @ForceInline
    static 
    void testVectorCastIntToIntFail(IntVector.IntSpecies a, IntVector.IntSpecies b, int[] input) {
        assert(input.length == a.length());

        IntVector av = a.fromArray(input, 0);
        try {
            b.cast(av);
            Assert.fail(String.format(
                    "Cast failed to throw IllegalArgumentException for differing species lengths for %s and %s",
                    a, b));
        } catch (IllegalArgumentException e) {
        }
    }

    @ForceInline
    static 
    void testVectorCastLongToInt(LongVector.LongSpecies a, IntVector.IntSpecies b, long[] input, int[] output) {
        assert(input.length == a.length());
        assert(output.length == b.length());

        LongVector av = a.fromArray(input, 0);
        IntVector bv = b.cast(av);
        bv.intoArray(output, 0);

        for (int i = 0; i < Math.min(input.length, output.length); i++) {
            Assert.assertEquals(output[i], (int)input[i]);
        }
        for(int i = input.length; i < output.length; i++) {
            Assert.assertEquals(output[i], (int)0);
        }
    }

    @ForceInline
    static 
    void testVectorCastLongToIntFail(LongVector.LongSpecies a, IntVector.IntSpecies b, long[] input) {
        assert(input.length == a.length());

        LongVector av = a.fromArray(input, 0);
        try {
            b.cast(av);
            Assert.fail(String.format(
                    "Cast failed to throw IllegalArgumentException for differing species lengths for %s and %s",
                    a, b));
        } catch (IllegalArgumentException e) {
        }
    }

    @ForceInline
    static 
    void testVectorCastFloatToInt(FloatVector.FloatSpecies a, IntVector.IntSpecies b, float[] input, int[] output) {
        assert(input.length == a.length());
        assert(output.length == b.length());

        FloatVector av = a.fromArray(input, 0);
        IntVector bv = b.cast(av);
        bv.intoArray(output, 0);

        for (int i = 0; i < Math.min(input.length, output.length); i++) {
            Assert.assertEquals(output[i], (int)input[i]);
        }
        for(int i = input.length; i < output.length; i++) {
            Assert.assertEquals(output[i], (int)0);
        }
    }

    @ForceInline
    static 
    void testVectorCastFloatToIntFail(FloatVector.FloatSpecies a, IntVector.IntSpecies b, float[] input) {
        assert(input.length == a.length());

        FloatVector av = a.fromArray(input, 0);
        try {
            b.cast(av);
            Assert.fail(String.format(
                    "Cast failed to throw IllegalArgumentException for differing species lengths for %s and %s",
                    a, b));
        } catch (IllegalArgumentException e) {
        }
    }

    @ForceInline
    static 
    void testVectorCastDoubleToInt(DoubleVector.DoubleSpecies a, IntVector.IntSpecies b, double[] input, int[] output) {
        assert(input.length == a.length());
        assert(output.length == b.length());

        DoubleVector av = a.fromArray(input, 0);
        IntVector bv = b.cast(av);
        bv.intoArray(output, 0);

        for (int i = 0; i < Math.min(input.length, output.length); i++) {
            Assert.assertEquals(output[i], (int)input[i]);
        }
        for(int i = input.length; i < output.length; i++) {
            Assert.assertEquals(output[i], (int)0);
        }
    }

    @ForceInline
    static 
    void testVectorCastDoubleToIntFail(DoubleVector.DoubleSpecies a, IntVector.IntSpecies b, double[] input) {
        assert(input.length == a.length());

        DoubleVector av = a.fromArray(input, 0);
        try {
            b.cast(av);
            Assert.fail(String.format(
                    "Cast failed to throw IllegalArgumentException for differing species lengths for %s and %s",
                    a, b));
        } catch (IllegalArgumentException e) {
        }
    }

    @ForceInline
    static 
    void testVectorCastByteToLong(ByteVector.ByteSpecies a, LongVector.LongSpecies b, byte[] input, long[] output) {
        assert(input.length == a.length());
        assert(output.length == b.length());

        ByteVector av = a.fromArray(input, 0);
        LongVector bv = b.cast(av);
        bv.intoArray(output, 0);

        for (int i = 0; i < Math.min(input.length, output.length); i++) {
            Assert.assertEquals(output[i], (long)input[i]);
        }
        for(int i = input.length; i < output.length; i++) {
            Assert.assertEquals(output[i], (long)0);
        }
    }

    @ForceInline
    static 
    void testVectorCastByteToLongFail(ByteVector.ByteSpecies a, LongVector.LongSpecies b, byte[] input) {
        assert(input.length == a.length());

        ByteVector av = a.fromArray(input, 0);
        try {
            b.cast(av);
            Assert.fail(String.format(
                    "Cast failed to throw IllegalArgumentException for differing species lengths for %s and %s",
                    a, b));
        } catch (IllegalArgumentException e) {
        }
    }

    @ForceInline
    static 
    void testVectorCastShortToLong(ShortVector.ShortSpecies a, LongVector.LongSpecies b, short[] input, long[] output) {
        assert(input.length == a.length());
        assert(output.length == b.length());

        ShortVector av = a.fromArray(input, 0);
        LongVector bv = b.cast(av);
        bv.intoArray(output, 0);

        for (int i = 0; i < Math.min(input.length, output.length); i++) {
            Assert.assertEquals(output[i], (long)input[i]);
        }
        for(int i = input.length; i < output.length; i++) {
            Assert.assertEquals(output[i], (long)0);
        }
    }

    @ForceInline
    static 
    void testVectorCastShortToLongFail(ShortVector.ShortSpecies a, LongVector.LongSpecies b, short[] input) {
        assert(input.length == a.length());

        ShortVector av = a.fromArray(input, 0);
        try {
            b.cast(av);
            Assert.fail(String.format(
                    "Cast failed to throw IllegalArgumentException for differing species lengths for %s and %s",
                    a, b));
        } catch (IllegalArgumentException e) {
        }
    }

    @ForceInline
    static 
    void testVectorCastIntToLong(IntVector.IntSpecies a, LongVector.LongSpecies b, int[] input, long[] output) {
        assert(input.length == a.length());
        assert(output.length == b.length());

        IntVector av = a.fromArray(input, 0);
        LongVector bv = b.cast(av);
        bv.intoArray(output, 0);

        for (int i = 0; i < Math.min(input.length, output.length); i++) {
            Assert.assertEquals(output[i], (long)input[i]);
        }
        for(int i = input.length; i < output.length; i++) {
            Assert.assertEquals(output[i], (long)0);
        }
    }

    @ForceInline
    static 
    void testVectorCastIntToLongFail(IntVector.IntSpecies a, LongVector.LongSpecies b, int[] input) {
        assert(input.length == a.length());

        IntVector av = a.fromArray(input, 0);
        try {
            b.cast(av);
            Assert.fail(String.format(
                    "Cast failed to throw IllegalArgumentException for differing species lengths for %s and %s",
                    a, b));
        } catch (IllegalArgumentException e) {
        }
    }

    @ForceInline
    static 
    void testVectorCastLongToLong(LongVector.LongSpecies a, LongVector.LongSpecies b, long[] input, long[] output) {
        assert(input.length == a.length());
        assert(output.length == b.length());

        LongVector av = a.fromArray(input, 0);
        LongVector bv = b.cast(av);
        bv.intoArray(output, 0);

        for (int i = 0; i < Math.min(input.length, output.length); i++) {
            Assert.assertEquals(output[i], (long)input[i]);
        }
        for(int i = input.length; i < output.length; i++) {
            Assert.assertEquals(output[i], (long)0);
        }
    }

    @ForceInline
    static 
    void testVectorCastLongToLongFail(LongVector.LongSpecies a, LongVector.LongSpecies b, long[] input) {
        assert(input.length == a.length());

        LongVector av = a.fromArray(input, 0);
        try {
            b.cast(av);
            Assert.fail(String.format(
                    "Cast failed to throw IllegalArgumentException for differing species lengths for %s and %s",
                    a, b));
        } catch (IllegalArgumentException e) {
        }
    }

    @ForceInline
    static 
    void testVectorCastFloatToLong(FloatVector.FloatSpecies a, LongVector.LongSpecies b, float[] input, long[] output) {
        assert(input.length == a.length());
        assert(output.length == b.length());

        FloatVector av = a.fromArray(input, 0);
        LongVector bv = b.cast(av);
        bv.intoArray(output, 0);

        for (int i = 0; i < Math.min(input.length, output.length); i++) {
            Assert.assertEquals(output[i], (long)input[i]);
        }
        for(int i = input.length; i < output.length; i++) {
            Assert.assertEquals(output[i], (long)0);
        }
    }

    @ForceInline
    static 
    void testVectorCastFloatToLongFail(FloatVector.FloatSpecies a, LongVector.LongSpecies b, float[] input) {
        assert(input.length == a.length());

        FloatVector av = a.fromArray(input, 0);
        try {
            b.cast(av);
            Assert.fail(String.format(
                    "Cast failed to throw IllegalArgumentException for differing species lengths for %s and %s",
                    a, b));
        } catch (IllegalArgumentException e) {
        }
    }

    @ForceInline
    static 
    void testVectorCastDoubleToLong(DoubleVector.DoubleSpecies a, LongVector.LongSpecies b, double[] input, long[] output) {
        assert(input.length == a.length());
        assert(output.length == b.length());

        DoubleVector av = a.fromArray(input, 0);
        LongVector bv = b.cast(av);
        bv.intoArray(output, 0);

        for (int i = 0; i < Math.min(input.length, output.length); i++) {
            Assert.assertEquals(output[i], (long)input[i]);
        }
        for(int i = input.length; i < output.length; i++) {
            Assert.assertEquals(output[i], (long)0);
        }
    }

    @ForceInline
    static 
    void testVectorCastDoubleToLongFail(DoubleVector.DoubleSpecies a, LongVector.LongSpecies b, double[] input) {
        assert(input.length == a.length());

        DoubleVector av = a.fromArray(input, 0);
        try {
            b.cast(av);
            Assert.fail(String.format(
                    "Cast failed to throw IllegalArgumentException for differing species lengths for %s and %s",
                    a, b));
        } catch (IllegalArgumentException e) {
        }
    }

    @ForceInline
    static 
    void testVectorCastByteToDouble(ByteVector.ByteSpecies a, DoubleVector.DoubleSpecies b, byte[] input, double[] output) {
        assert(input.length == a.length());
        assert(output.length == b.length());

        ByteVector av = a.fromArray(input, 0);
        DoubleVector bv = b.cast(av);
        bv.intoArray(output, 0);

        for (int i = 0; i < Math.min(input.length, output.length); i++) {
            Assert.assertEquals(output[i], (double)input[i]);
        }
        for(int i = input.length; i < output.length; i++) {
            Assert.assertEquals(output[i], (double)0);
        }
    }

    @ForceInline
    static 
    void testVectorCastByteToDoubleFail(ByteVector.ByteSpecies a, DoubleVector.DoubleSpecies b, byte[] input) {
        assert(input.length == a.length());

        ByteVector av = a.fromArray(input, 0);
        try {
            b.cast(av);
            Assert.fail(String.format(
                    "Cast failed to throw IllegalArgumentException for differing species lengths for %s and %s",
                    a, b));
        } catch (IllegalArgumentException e) {
        }
    }

    @ForceInline
    static 
    void testVectorCastShortToDouble(ShortVector.ShortSpecies a, DoubleVector.DoubleSpecies b, short[] input, double[] output) {
        assert(input.length == a.length());
        assert(output.length == b.length());

        ShortVector av = a.fromArray(input, 0);
        DoubleVector bv = b.cast(av);
        bv.intoArray(output, 0);

        for (int i = 0; i < Math.min(input.length, output.length); i++) {
            Assert.assertEquals(output[i], (double)input[i]);
        }
        for(int i = input.length; i < output.length; i++) {
            Assert.assertEquals(output[i], (double)0);
        }
    }

    @ForceInline
    static 
    void testVectorCastShortToDoubleFail(ShortVector.ShortSpecies a, DoubleVector.DoubleSpecies b, short[] input) {
        assert(input.length == a.length());

        ShortVector av = a.fromArray(input, 0);
        try {
            b.cast(av);
            Assert.fail(String.format(
                    "Cast failed to throw IllegalArgumentException for differing species lengths for %s and %s",
                    a, b));
        } catch (IllegalArgumentException e) {
        }
    }

    @ForceInline
    static 
    void testVectorCastIntToDouble(IntVector.IntSpecies a, DoubleVector.DoubleSpecies b, int[] input, double[] output) {
        assert(input.length == a.length());
        assert(output.length == b.length());

        IntVector av = a.fromArray(input, 0);
        DoubleVector bv = b.cast(av);
        bv.intoArray(output, 0);

        for (int i = 0; i < Math.min(input.length, output.length); i++) {
            Assert.assertEquals(output[i], (double)input[i]);
        }
        for(int i = input.length; i < output.length; i++) {
            Assert.assertEquals(output[i], (double)0);
        }
    }

    @ForceInline
    static 
    void testVectorCastIntToDoubleFail(IntVector.IntSpecies a, DoubleVector.DoubleSpecies b, int[] input) {
        assert(input.length == a.length());

        IntVector av = a.fromArray(input, 0);
        try {
            b.cast(av);
            Assert.fail(String.format(
                    "Cast failed to throw IllegalArgumentException for differing species lengths for %s and %s",
                    a, b));
        } catch (IllegalArgumentException e) {
        }
    }

    @ForceInline
    static 
    void testVectorCastLongToDouble(LongVector.LongSpecies a, DoubleVector.DoubleSpecies b, long[] input, double[] output) {
        assert(input.length == a.length());
        assert(output.length == b.length());

        LongVector av = a.fromArray(input, 0);
        DoubleVector bv = b.cast(av);
        bv.intoArray(output, 0);

        for (int i = 0; i < Math.min(input.length, output.length); i++) {
            Assert.assertEquals(output[i], (double)input[i]);
        }
        for(int i = input.length; i < output.length; i++) {
            Assert.assertEquals(output[i], (double)0);
        }
    }

    @ForceInline
    static 
    void testVectorCastLongToDoubleFail(LongVector.LongSpecies a, DoubleVector.DoubleSpecies b, long[] input) {
        assert(input.length == a.length());

        LongVector av = a.fromArray(input, 0);
        try {
            b.cast(av);
            Assert.fail(String.format(
                    "Cast failed to throw IllegalArgumentException for differing species lengths for %s and %s",
                    a, b));
        } catch (IllegalArgumentException e) {
        }
    }

    @ForceInline
    static 
    void testVectorCastFloatToDouble(FloatVector.FloatSpecies a, DoubleVector.DoubleSpecies b, float[] input, double[] output) {
        assert(input.length == a.length());
        assert(output.length == b.length());

        FloatVector av = a.fromArray(input, 0);
        DoubleVector bv = b.cast(av);
        bv.intoArray(output, 0);

        for (int i = 0; i < Math.min(input.length, output.length); i++) {
            Assert.assertEquals(output[i], (double)input[i]);
        }
        for(int i = input.length; i < output.length; i++) {
            Assert.assertEquals(output[i], (double)0);
        }
    }

    @ForceInline
    static 
    void testVectorCastFloatToDoubleFail(FloatVector.FloatSpecies a, DoubleVector.DoubleSpecies b, float[] input) {
        assert(input.length == a.length());

        FloatVector av = a.fromArray(input, 0);
        try {
            b.cast(av);
            Assert.fail(String.format(
                    "Cast failed to throw IllegalArgumentException for differing species lengths for %s and %s",
                    a, b));
        } catch (IllegalArgumentException e) {
        }
    }

    @ForceInline
    static 
    void testVectorCastDoubleToDouble(DoubleVector.DoubleSpecies a, DoubleVector.DoubleSpecies b, double[] input, double[] output) {
        assert(input.length == a.length());
        assert(output.length == b.length());

        DoubleVector av = a.fromArray(input, 0);
        DoubleVector bv = b.cast(av);
        bv.intoArray(output, 0);

        for (int i = 0; i < Math.min(input.length, output.length); i++) {
            Assert.assertEquals(output[i], (double)input[i]);
        }
        for(int i = input.length; i < output.length; i++) {
            Assert.assertEquals(output[i], (double)0);
        }
    }

    @ForceInline
    static 
    void testVectorCastDoubleToDoubleFail(DoubleVector.DoubleSpecies a, DoubleVector.DoubleSpecies b, double[] input) {
        assert(input.length == a.length());

        DoubleVector av = a.fromArray(input, 0);
        try {
            b.cast(av);
            Assert.fail(String.format(
                    "Cast failed to throw IllegalArgumentException for differing species lengths for %s and %s",
                    a, b));
        } catch (IllegalArgumentException e) {
        }
    }

    @Test(dataProvider = "byteUnaryOpProvider")
    static void testCastFromByte(IntFunction<byte[]> fa) {
        byte[] bin64 = fa.apply(bspec64.length());
        byte[] bin128 = fa.apply(bspec128.length());
        byte[] bin256 = fa.apply(bspec256.length());
        byte[] bin512 = fa.apply(bspec512.length());

        byte[] bout64 = new byte[bspec64.length()];
        byte[] bout128 = new byte[bspec128.length()];
        byte[] bout256 = new byte[bspec256.length()];
        byte[] bout512 = new byte[bspec512.length()];

        short[] sout128 = new short[sspec128.length()];
        short[] sout256 = new short[sspec256.length()];
        short[] sout512 = new short[sspec512.length()];

        int[] iout256 = new int[ispec256.length()];
        int[] iout512 = new int[ispec512.length()];

        long[] lout512 = new long[lspec512.length()];

        float[] fout256 = new float[fspec256.length()];
        float[] fout512 = new float[fspec512.length()];

        double[] dout512 = new double[dspec512.length()];

        for (int i = 0; i < NUM_ITER; i++) {
            testVectorCastByteToByte(bspec64, bspec64, bin64, bout64);
            testVectorCastByteToByte(bspec128, bspec128, bin128, bout128);
            testVectorCastByteToByte(bspec256, bspec256, bin256, bout256);
            testVectorCastByteToByte(bspec512, bspec512, bin512, bout512);

            testVectorCastByteToShort(bspec64, sspec128, bin64, sout128);
            testVectorCastByteToShort(bspec128, sspec256, bin128, sout256);
            testVectorCastByteToShort(bspec256, sspec512, bin256, sout512);

            testVectorCastByteToInt(bspec64, ispec256, bin64, iout256);
            testVectorCastByteToInt(bspec128, ispec512, bin128, iout512);

            testVectorCastByteToLong(bspec64, lspec512, bin64, lout512);

            testVectorCastByteToFloat(bspec64, fspec256, bin64, fout256);
            testVectorCastByteToFloat(bspec128, fspec512, bin128, fout512);

            testVectorCastByteToDouble(bspec64, dspec512, bin64, dout512);
        }
    }

    @Test
    static void testCastFromByteFail() {
        byte[] bin64 = new byte[bspec64.length()];
        byte[] bin128 = new byte[bspec128.length()];
        byte[] bin256 = new byte[bspec256.length()];
        byte[] bin512 = new byte[bspec512.length()];

        for (int i = 0; i < INVOC_COUNT; i++) {
            testVectorCastByteToByteFail(bspec64, bspec128, bin64);
            testVectorCastByteToByteFail(bspec64, bspec256, bin64);
            testVectorCastByteToByteFail(bspec64, bspec512, bin64);

            testVectorCastByteToByteFail(bspec128, bspec64, bin128);
            testVectorCastByteToByteFail(bspec128, bspec256, bin128);
            testVectorCastByteToByteFail(bspec128, bspec512, bin128);

            testVectorCastByteToByteFail(bspec256, bspec64, bin256);
            testVectorCastByteToByteFail(bspec256, bspec128, bin256);
            testVectorCastByteToByteFail(bspec256, bspec512, bin256);

            testVectorCastByteToByteFail(bspec512, bspec64, bin512);
            testVectorCastByteToByteFail(bspec512, bspec128, bin512);
            testVectorCastByteToByteFail(bspec512, bspec256, bin512);

            testVectorCastByteToShortFail(bspec64, sspec64, bin64);
            testVectorCastByteToShortFail(bspec64, sspec256, bin64);
            testVectorCastByteToShortFail(bspec64, sspec512, bin64);

            testVectorCastByteToShortFail(bspec128, sspec64, bin128);
            testVectorCastByteToShortFail(bspec128, sspec128, bin128);
            testVectorCastByteToShortFail(bspec128, sspec512, bin128);

            testVectorCastByteToShortFail(bspec256, sspec64, bin256);
            testVectorCastByteToShortFail(bspec256, sspec128, bin256);
            testVectorCastByteToShortFail(bspec256, sspec256, bin256);

            testVectorCastByteToShortFail(bspec512, sspec64, bin512);
            testVectorCastByteToShortFail(bspec512, sspec128, bin512);
            testVectorCastByteToShortFail(bspec512, sspec256, bin512);
            testVectorCastByteToShortFail(bspec512, sspec512, bin512);

            testVectorCastByteToIntFail(bspec64, ispec64, bin64);
            testVectorCastByteToIntFail(bspec64, ispec128, bin64);
            testVectorCastByteToIntFail(bspec64, ispec512, bin64);

            testVectorCastByteToIntFail(bspec128, ispec64, bin128);
            testVectorCastByteToIntFail(bspec128, ispec128, bin128);
            testVectorCastByteToIntFail(bspec128, ispec256, bin128);

            testVectorCastByteToIntFail(bspec256, ispec64, bin256);
            testVectorCastByteToIntFail(bspec256, ispec128, bin256);
            testVectorCastByteToIntFail(bspec256, ispec256, bin256);
            testVectorCastByteToIntFail(bspec256, ispec512, bin256);

            testVectorCastByteToIntFail(bspec512, ispec64, bin512);
            testVectorCastByteToIntFail(bspec512, ispec128, bin512);
            testVectorCastByteToIntFail(bspec512, ispec256, bin512);
            testVectorCastByteToIntFail(bspec512, ispec512, bin512);

            testVectorCastByteToLongFail(bspec64, lspec64, bin64);
            testVectorCastByteToLongFail(bspec64, lspec128, bin64);
            testVectorCastByteToLongFail(bspec64, lspec256, bin64);

            testVectorCastByteToLongFail(bspec128, lspec64, bin128);
            testVectorCastByteToLongFail(bspec128, lspec128, bin128);
            testVectorCastByteToLongFail(bspec128, lspec256, bin128);
            testVectorCastByteToLongFail(bspec128, lspec512, bin128);

            testVectorCastByteToLongFail(bspec256, lspec64, bin256);
            testVectorCastByteToLongFail(bspec256, lspec128, bin256);
            testVectorCastByteToLongFail(bspec256, lspec256, bin256);
            testVectorCastByteToLongFail(bspec256, lspec512, bin256);

            testVectorCastByteToLongFail(bspec512, lspec64, bin512);
            testVectorCastByteToLongFail(bspec512, lspec128, bin512);
            testVectorCastByteToLongFail(bspec512, lspec256, bin512);
            testVectorCastByteToLongFail(bspec512, lspec512, bin512);

            testVectorCastByteToFloatFail(bspec64, fspec64, bin64);
            testVectorCastByteToFloatFail(bspec64, fspec128, bin64);
            testVectorCastByteToFloatFail(bspec64, fspec512, bin64);

            testVectorCastByteToFloatFail(bspec128, fspec64, bin128);
            testVectorCastByteToFloatFail(bspec128, fspec128, bin128);
            testVectorCastByteToFloatFail(bspec128, fspec256, bin128);

            testVectorCastByteToFloatFail(bspec256, fspec64, bin256);
            testVectorCastByteToFloatFail(bspec256, fspec128, bin256);
            testVectorCastByteToFloatFail(bspec256, fspec256, bin256);
            testVectorCastByteToFloatFail(bspec256, fspec512, bin256);

            testVectorCastByteToFloatFail(bspec512, fspec64, bin512);
            testVectorCastByteToFloatFail(bspec512, fspec128, bin512);
            testVectorCastByteToFloatFail(bspec512, fspec256, bin512);
            testVectorCastByteToFloatFail(bspec512, fspec512, bin512);

            testVectorCastByteToDoubleFail(bspec64, dspec64, bin64);
            testVectorCastByteToDoubleFail(bspec64, dspec128, bin64);
            testVectorCastByteToDoubleFail(bspec64, dspec256, bin64);

            testVectorCastByteToDoubleFail(bspec128, dspec64, bin128);
            testVectorCastByteToDoubleFail(bspec128, dspec128, bin128);
            testVectorCastByteToDoubleFail(bspec128, dspec256, bin128);
            testVectorCastByteToDoubleFail(bspec128, dspec512, bin128);

            testVectorCastByteToDoubleFail(bspec256, dspec64, bin256);
            testVectorCastByteToDoubleFail(bspec256, dspec128, bin256);
            testVectorCastByteToDoubleFail(bspec256, dspec256, bin256);
            testVectorCastByteToDoubleFail(bspec256, dspec512, bin256);

            testVectorCastByteToDoubleFail(bspec512, dspec64, bin512);
            testVectorCastByteToDoubleFail(bspec512, dspec128, bin512);
            testVectorCastByteToDoubleFail(bspec512, dspec256, bin512);
            testVectorCastByteToDoubleFail(bspec512, dspec512, bin512);
        }
    }

    @Test(dataProvider = "shortUnaryOpProvider")
    static void testCastFromShort(IntFunction<short[]> fa) {
        short[] sin64 = fa.apply(sspec64.length());
        short[] sin128 = fa.apply(sspec128.length());
        short[] sin256 = fa.apply(sspec256.length());
        short[] sin512 = fa.apply(sspec512.length());

        byte[] bout64 = new byte[bspec64.length()];
        byte[] bout128 = new byte[bspec128.length()];
        byte[] bout256 = new byte[bspec256.length()];

        short[] sout64 = new short[sspec64.length()];
        short[] sout128 = new short[sspec128.length()];
        short[] sout256 = new short[sspec256.length()];
        short[] sout512 = new short[sspec512.length()];

        int[] iout128 = new int[ispec128.length()];
        int[] iout256 = new int[ispec256.length()];
        int[] iout512 = new int[ispec512.length()];

        long[] lout256 = new long[lspec256.length()];
        long[] lout512 = new long[lspec512.length()];

        float[] fout128 = new float[fspec128.length()];
        float[] fout256 = new float[fspec256.length()];
        float[] fout512 = new float[fspec512.length()];

        double[] dout256 = new double[dspec256.length()];
        double[] dout512 = new double[dspec512.length()];

        for (int i = 0; i < NUM_ITER; i++) {
            testVectorCastShortToByte(sspec128, bspec64, sin128, bout64);
            testVectorCastShortToByte(sspec256, bspec128, sin256, bout128);
            testVectorCastShortToByte(sspec512, bspec256, sin512, bout256);

            testVectorCastShortToShort(sspec64, sspec64, sin64, sout64);
            testVectorCastShortToShort(sspec128, sspec128, sin128, sout128);
            testVectorCastShortToShort(sspec256, sspec256, sin256, sout256);
            testVectorCastShortToShort(sspec512, sspec512, sin512, sout512);

            testVectorCastShortToInt(sspec64, ispec128, sin64, iout128);
            testVectorCastShortToInt(sspec128, ispec256, sin128, iout256);
            testVectorCastShortToInt(sspec256, ispec512, sin256, iout512);

            testVectorCastShortToLong(sspec64, lspec256, sin64, lout256);
            testVectorCastShortToLong(sspec128, lspec512, sin128, lout512);

            testVectorCastShortToFloat(sspec64, fspec128, sin64, fout128);
            testVectorCastShortToFloat(sspec128, fspec256, sin128, fout256);
            testVectorCastShortToFloat(sspec256, fspec512, sin256, fout512);

            testVectorCastShortToDouble(sspec64, dspec256, sin64, dout256);
            testVectorCastShortToDouble(sspec128, dspec512, sin128, dout512);
        }
    }

    @Test()
    static void testCastFromShortFail() {
        short[] sin64 = new short[sspec64.length()];
        short[] sin128 = new short[sspec128.length()];
        short[] sin256 = new short[sspec256.length()];
        short[] sin512 = new short[sspec512.length()];

        for (int i = 0; i < INVOC_COUNT; i++) {
            testVectorCastShortToByteFail(sspec64, bspec64, sin64);
            testVectorCastShortToByteFail(sspec64, bspec128, sin64);
            testVectorCastShortToByteFail(sspec64, bspec256, sin64);
            testVectorCastShortToByteFail(sspec64, bspec512, sin64);

            testVectorCastShortToByteFail(sspec128, bspec128, sin128);
            testVectorCastShortToByteFail(sspec128, bspec256, sin128);
            testVectorCastShortToByteFail(sspec128, bspec512, sin128);

            testVectorCastShortToByteFail(sspec256, bspec64, sin256);
            testVectorCastShortToByteFail(sspec256, bspec256, sin256);
            testVectorCastShortToByteFail(sspec256, bspec512, sin256);

            testVectorCastShortToByteFail(sspec512, bspec64, sin512);
            testVectorCastShortToByteFail(sspec512, bspec128, sin512);
            testVectorCastShortToByteFail(sspec512, bspec512, sin512);

            testVectorCastShortToShortFail(sspec64, sspec128, sin64);
            testVectorCastShortToShortFail(sspec64, sspec256, sin64);
            testVectorCastShortToShortFail(sspec64, sspec512, sin64);

            testVectorCastShortToShortFail(sspec128, sspec64, sin128);
            testVectorCastShortToShortFail(sspec128, sspec256, sin128);
            testVectorCastShortToShortFail(sspec128, sspec512, sin128);

            testVectorCastShortToShortFail(sspec256, sspec64, sin256);
            testVectorCastShortToShortFail(sspec256, sspec128, sin256);
            testVectorCastShortToShortFail(sspec256, sspec512, sin256);

            testVectorCastShortToShortFail(sspec512, sspec64, sin512);
            testVectorCastShortToShortFail(sspec512, sspec128, sin512);
            testVectorCastShortToShortFail(sspec512, sspec256, sin512);

            testVectorCastShortToIntFail(sspec64, ispec64, sin64);
            testVectorCastShortToIntFail(sspec64, ispec256, sin64);
            testVectorCastShortToIntFail(sspec64, ispec512, sin64);

            testVectorCastShortToIntFail(sspec128, ispec64, sin128);
            testVectorCastShortToIntFail(sspec128, ispec128, sin128);
            testVectorCastShortToIntFail(sspec128, ispec512, sin128);

            testVectorCastShortToIntFail(sspec256, ispec64, sin256);
            testVectorCastShortToIntFail(sspec256, ispec128, sin256);
            testVectorCastShortToIntFail(sspec256, ispec256, sin256);

            testVectorCastShortToIntFail(sspec512, ispec64, sin512);
            testVectorCastShortToIntFail(sspec512, ispec128, sin512);
            testVectorCastShortToIntFail(sspec512, ispec256, sin512);
            testVectorCastShortToIntFail(sspec512, ispec512, sin512);

            testVectorCastShortToLongFail(sspec64, lspec64, sin64);
            testVectorCastShortToLongFail(sspec64, lspec128, sin64);
            testVectorCastShortToLongFail(sspec64, lspec512, sin64);

            testVectorCastShortToLongFail(sspec128, lspec64, sin128);
            testVectorCastShortToLongFail(sspec128, lspec128, sin128);
            testVectorCastShortToLongFail(sspec128, lspec256, sin128);

            testVectorCastShortToLongFail(sspec256, lspec64, sin256);
            testVectorCastShortToLongFail(sspec256, lspec128, sin256);
            testVectorCastShortToLongFail(sspec256, lspec256, sin256);
            testVectorCastShortToLongFail(sspec256, lspec512, sin256);

            testVectorCastShortToLongFail(sspec512, lspec64, sin512);
            testVectorCastShortToLongFail(sspec512, lspec128, sin512);
            testVectorCastShortToLongFail(sspec512, lspec256, sin512);
            testVectorCastShortToLongFail(sspec512, lspec512, sin512);

            testVectorCastShortToFloatFail(sspec64, fspec64, sin64);
            testVectorCastShortToFloatFail(sspec64, fspec256, sin64);
            testVectorCastShortToFloatFail(sspec64, fspec512, sin64);

            testVectorCastShortToFloatFail(sspec128, fspec64, sin128);
            testVectorCastShortToFloatFail(sspec128, fspec128, sin128);
            testVectorCastShortToFloatFail(sspec128, fspec512, sin128);

            testVectorCastShortToFloatFail(sspec256, fspec64, sin256);
            testVectorCastShortToFloatFail(sspec256, fspec128, sin256);
            testVectorCastShortToFloatFail(sspec256, fspec256, sin256);

            testVectorCastShortToFloatFail(sspec512, fspec64, sin512);
            testVectorCastShortToFloatFail(sspec512, fspec128, sin512);
            testVectorCastShortToFloatFail(sspec512, fspec256, sin512);
            testVectorCastShortToFloatFail(sspec512, fspec512, sin512);

            testVectorCastShortToDoubleFail(sspec64, dspec64, sin64);
            testVectorCastShortToDoubleFail(sspec64, dspec128, sin64);
            testVectorCastShortToDoubleFail(sspec64, dspec512, sin64);

            testVectorCastShortToDoubleFail(sspec128, dspec64, sin128);
            testVectorCastShortToDoubleFail(sspec128, dspec128, sin128);
            testVectorCastShortToDoubleFail(sspec128, dspec256, sin128);

            testVectorCastShortToDoubleFail(sspec256, dspec64, sin256);
            testVectorCastShortToDoubleFail(sspec256, dspec128, sin256);
            testVectorCastShortToDoubleFail(sspec256, dspec256, sin256);
            testVectorCastShortToDoubleFail(sspec256, dspec512, sin256);

            testVectorCastShortToDoubleFail(sspec512, dspec64, sin512);
            testVectorCastShortToDoubleFail(sspec512, dspec128, sin512);
            testVectorCastShortToDoubleFail(sspec512, dspec256, sin512);
            testVectorCastShortToDoubleFail(sspec512, dspec512, sin512);
        }
    }

    @Test(dataProvider = "intUnaryOpProvider")
    static void testCastFromInt(IntFunction<int[]> fa) {
        int[] iin64 = fa.apply(ispec64.length());
        int[] iin128 = fa.apply(ispec128.length());
        int[] iin256 = fa.apply(ispec256.length());
        int[] iin512 = fa.apply(ispec512.length());

        byte[] bout64 = new byte[bspec64.length()];
        byte[] bout128 = new byte[bspec128.length()];

        short[] sout64 = new short[sspec64.length()];
        short[] sout128 = new short[sspec128.length()];
        short[] sout256 = new short[sspec256.length()];

        int[] iout64 = new int[ispec64.length()];
        int[] iout128 = new int[ispec128.length()];
        int[] iout256 = new int[ispec256.length()];
        int[] iout512 = new int[ispec512.length()];

        long[] lout128 = new long[lspec128.length()];
        long[] lout256 = new long[lspec256.length()];
        long[] lout512 = new long[lspec512.length()];

        float[] fout64 = new float[fspec64.length()];
        float[] fout128 = new float[fspec128.length()];
        float[] fout256 = new float[fspec256.length()];
        float[] fout512 = new float[fspec512.length()];

        double[] dout128 = new double[dspec128.length()];
        double[] dout256 = new double[dspec256.length()];
        double[] dout512 = new double[dspec512.length()];

        for (int i = 0; i < NUM_ITER; i++) {
            testVectorCastIntToByte(ispec256, bspec64, iin256, bout64);
            testVectorCastIntToByte(ispec512, bspec128, iin512, bout128);

            testVectorCastIntToShort(ispec128, sspec64, iin128, sout64);
            testVectorCastIntToShort(ispec256, sspec128, iin256, sout128);
            testVectorCastIntToShort(ispec512, sspec256, iin512, sout256);

            testVectorCastIntToInt(ispec64, ispec64, iin64, iout64);
            testVectorCastIntToInt(ispec128, ispec128, iin128, iout128);
            testVectorCastIntToInt(ispec256, ispec256, iin256, iout256);
            testVectorCastIntToInt(ispec512, ispec512, iin512, iout512);

            testVectorCastIntToLong(ispec64, lspec128, iin64, lout128);
            testVectorCastIntToLong(ispec128, lspec256, iin128, lout256);
            testVectorCastIntToLong(ispec256, lspec512, iin256, lout512);

            testVectorCastIntToFloat(ispec64, fspec64, iin64, fout64);
            testVectorCastIntToFloat(ispec128, fspec128, iin128, fout128);
            testVectorCastIntToFloat(ispec256, fspec256, iin256, fout256);
            testVectorCastIntToFloat(ispec512, fspec512, iin512, fout512);

            testVectorCastIntToDouble(ispec64, dspec128, iin64, dout128);
            testVectorCastIntToDouble(ispec128, dspec256, iin128, dout256);
            testVectorCastIntToDouble(ispec256, dspec512, iin256, dout512);
        }
    }

    @Test
    static void testCastFromIntFail() {
        int[] iin64 = new int[ispec64.length()];
        int[] iin128 = new int[ispec128.length()];
        int[] iin256 = new int[ispec256.length()];
        int[] iin512 = new int[ispec512.length()];

        for (int i = 0; i < INVOC_COUNT; i++) {
            testVectorCastIntToByteFail(ispec64, bspec64, iin64);
            testVectorCastIntToByteFail(ispec64, bspec128, iin64);
            testVectorCastIntToByteFail(ispec64, bspec256, iin64);
            testVectorCastIntToByteFail(ispec64, bspec512, iin64);

            testVectorCastIntToByteFail(ispec128, bspec64, iin128);
            testVectorCastIntToByteFail(ispec128, bspec128, iin128);
            testVectorCastIntToByteFail(ispec128, bspec256, iin128);
            testVectorCastIntToByteFail(ispec128, bspec512, iin128);

            testVectorCastIntToByteFail(ispec256, bspec128, iin256);
            testVectorCastIntToByteFail(ispec256, bspec256, iin256);
            testVectorCastIntToByteFail(ispec256, bspec512, iin256);

            testVectorCastIntToByteFail(ispec512, bspec64, iin512);
            testVectorCastIntToByteFail(ispec512, bspec256, iin512);
            testVectorCastIntToByteFail(ispec512, bspec512, iin512);

            testVectorCastIntToShortFail(ispec64, sspec64, iin64);
            testVectorCastIntToShortFail(ispec64, sspec128, iin64);
            testVectorCastIntToShortFail(ispec64, sspec256, iin64);
            testVectorCastIntToShortFail(ispec64, sspec512, iin64);

            testVectorCastIntToShortFail(ispec128, sspec128, iin128);
            testVectorCastIntToShortFail(ispec128, sspec256, iin128);
            testVectorCastIntToShortFail(ispec128, sspec512, iin128);

            testVectorCastIntToShortFail(ispec256, sspec64, iin256);
            testVectorCastIntToShortFail(ispec256, sspec256, iin256);
            testVectorCastIntToShortFail(ispec256, sspec512, iin256);

            testVectorCastIntToShortFail(ispec512, sspec64, iin512);
            testVectorCastIntToShortFail(ispec512, sspec128, iin512);
            testVectorCastIntToShortFail(ispec512, sspec512, iin512);

            testVectorCastIntToIntFail(ispec64, ispec128, iin64);
            testVectorCastIntToIntFail(ispec64, ispec256, iin64);
            testVectorCastIntToIntFail(ispec64, ispec512, iin64);

            testVectorCastIntToIntFail(ispec128, ispec64, iin128);
            testVectorCastIntToIntFail(ispec128, ispec256, iin128);
            testVectorCastIntToIntFail(ispec128, ispec512, iin128);

            testVectorCastIntToIntFail(ispec256, ispec64, iin256);
            testVectorCastIntToIntFail(ispec256, ispec128, iin256);
            testVectorCastIntToIntFail(ispec256, ispec512, iin256);

            testVectorCastIntToIntFail(ispec512, ispec64, iin512);
            testVectorCastIntToIntFail(ispec512, ispec128, iin512);
            testVectorCastIntToIntFail(ispec512, ispec256, iin512);

            testVectorCastIntToLongFail(ispec64, lspec64, iin64);
            testVectorCastIntToLongFail(ispec64, lspec256, iin64);
            testVectorCastIntToLongFail(ispec64, lspec512, iin64);

            testVectorCastIntToLongFail(ispec128, lspec64, iin128);
            testVectorCastIntToLongFail(ispec128, lspec128, iin128);
            testVectorCastIntToLongFail(ispec128, lspec512, iin128);

            testVectorCastIntToLongFail(ispec256, lspec64, iin256);
            testVectorCastIntToLongFail(ispec256, lspec128, iin256);
            testVectorCastIntToLongFail(ispec256, lspec256, iin256);

            testVectorCastIntToLongFail(ispec512, lspec64, iin512);
            testVectorCastIntToLongFail(ispec512, lspec128, iin512);
            testVectorCastIntToLongFail(ispec512, lspec256, iin512);
            testVectorCastIntToLongFail(ispec512, lspec512, iin512);

            testVectorCastIntToFloatFail(ispec64, fspec128, iin64);
            testVectorCastIntToFloatFail(ispec64, fspec256, iin64);
            testVectorCastIntToFloatFail(ispec64, fspec512, iin64);

            testVectorCastIntToFloatFail(ispec128, fspec64, iin128);
            testVectorCastIntToFloatFail(ispec128, fspec256, iin128);
            testVectorCastIntToFloatFail(ispec128, fspec512, iin128);

            testVectorCastIntToFloatFail(ispec256, fspec64, iin256);
            testVectorCastIntToFloatFail(ispec256, fspec128, iin256);
            testVectorCastIntToFloatFail(ispec256, fspec512, iin256);

            testVectorCastIntToFloatFail(ispec512, fspec64, iin512);
            testVectorCastIntToFloatFail(ispec512, fspec128, iin512);
            testVectorCastIntToFloatFail(ispec512, fspec256, iin512);

            testVectorCastIntToDoubleFail(ispec64, dspec64, iin64);
            testVectorCastIntToDoubleFail(ispec64, dspec256, iin64);
            testVectorCastIntToDoubleFail(ispec64, dspec512, iin64);

            testVectorCastIntToDoubleFail(ispec128, dspec64, iin128);
            testVectorCastIntToDoubleFail(ispec128, dspec128, iin128);
            testVectorCastIntToDoubleFail(ispec128, dspec512, iin128);

            testVectorCastIntToDoubleFail(ispec256, dspec64, iin256);
            testVectorCastIntToDoubleFail(ispec256, dspec128, iin256);
            testVectorCastIntToDoubleFail(ispec256, dspec256, iin256);

            testVectorCastIntToDoubleFail(ispec512, dspec64, iin512);
            testVectorCastIntToDoubleFail(ispec512, dspec128, iin512);
            testVectorCastIntToDoubleFail(ispec512, dspec256, iin512);
            testVectorCastIntToDoubleFail(ispec512, dspec512, iin512);
        }
    }

    @Test(dataProvider = "longUnaryOpProvider")
    static void testCastFromLong(IntFunction<long[]> fa) {
        long[] lin64 = fa.apply(lspec64.length());
        long[] lin128 = fa.apply(lspec128.length());
        long[] lin256 = fa.apply(lspec256.length());
        long[] lin512 = fa.apply(lspec512.length());

        byte[] bout64 = new byte[bspec64.length()];

        short[] sout64 = new short[sspec64.length()];
        short[] sout128 = new short[sspec128.length()];

        int[] iout64 = new int[ispec64.length()];
        int[] iout128 = new int[ispec128.length()];
        int[] iout256 = new int[ispec256.length()];

        long[] lout64 = new long[lspec64.length()];
        long[] lout128 = new long[lspec128.length()];
        long[] lout256 = new long[lspec256.length()];
        long[] lout512 = new long[lspec512.length()];

        float[] fout64 = new float[fspec64.length()];
        float[] fout128 = new float[fspec128.length()];
        float[] fout256 = new float[fspec256.length()];

        double[] dout64 = new double[dspec64.length()];
        double[] dout128 = new double[dspec128.length()];
        double[] dout256 = new double[dspec256.length()];
        double[] dout512 = new double[dspec512.length()];

        for (int i = 0; i < NUM_ITER; i++) {
            testVectorCastLongToByte(lspec512, bspec64, lin512, bout64);

            testVectorCastLongToShort(lspec256, sspec64, lin256, sout64);
            testVectorCastLongToShort(lspec512, sspec128, lin512, sout128);

            testVectorCastLongToInt(lspec128, ispec64, lin128, iout64);
            testVectorCastLongToInt(lspec256, ispec128, lin256, iout128);
            testVectorCastLongToInt(lspec512, ispec256, lin512, iout256);

            testVectorCastLongToLong(lspec64, lspec64, lin64, lout64);
            testVectorCastLongToLong(lspec128, lspec128, lin128, lout128);
            testVectorCastLongToLong(lspec256, lspec256, lin256, lout256);
            testVectorCastLongToLong(lspec512, lspec512, lin512, lout512);

            testVectorCastLongToFloat(lspec128, fspec64, lin128, fout64);
            testVectorCastLongToFloat(lspec256, fspec128, lin256, fout128);
            testVectorCastLongToFloat(lspec512, fspec256, lin512, fout256);

            testVectorCastLongToDouble(lspec64, dspec64, lin64, dout64);
            testVectorCastLongToDouble(lspec128, dspec128, lin128, dout128);
            testVectorCastLongToDouble(lspec256, dspec256, lin256, dout256);
            testVectorCastLongToDouble(lspec512, dspec512, lin512, dout512);
        }
    }

    @Test
    static void testCastFromLongFail() {
        long[] lin64 = new long[lspec64.length()];
        long[] lin128 = new long[lspec128.length()];
        long[] lin256 = new long[lspec256.length()];
        long[] lin512 = new long[lspec512.length()];

        for (int i = 0; i < INVOC_COUNT; i++) {
            testVectorCastLongToByteFail(lspec64, bspec64, lin64);
            testVectorCastLongToByteFail(lspec64, bspec128, lin64);
            testVectorCastLongToByteFail(lspec64, bspec256, lin64);
            testVectorCastLongToByteFail(lspec64, bspec512, lin64);

            testVectorCastLongToByteFail(lspec128, bspec64, lin128);
            testVectorCastLongToByteFail(lspec128, bspec128, lin128);
            testVectorCastLongToByteFail(lspec128, bspec256, lin128);
            testVectorCastLongToByteFail(lspec128, bspec512, lin128);

            testVectorCastLongToByteFail(lspec256, bspec64, lin256);
            testVectorCastLongToByteFail(lspec256, bspec128, lin256);
            testVectorCastLongToByteFail(lspec256, bspec256, lin256);
            testVectorCastLongToByteFail(lspec256, bspec512, lin256);

            testVectorCastLongToByteFail(lspec512, bspec128, lin512);
            testVectorCastLongToByteFail(lspec512, bspec256, lin512);
            testVectorCastLongToByteFail(lspec512, bspec512, lin512);

            testVectorCastLongToShortFail(lspec64, sspec64, lin64);
            testVectorCastLongToShortFail(lspec64, sspec128, lin64);
            testVectorCastLongToShortFail(lspec64, sspec256, lin64);
            testVectorCastLongToShortFail(lspec64, sspec512, lin64);

            testVectorCastLongToShortFail(lspec128, sspec64, lin128);
            testVectorCastLongToShortFail(lspec128, sspec128, lin128);
            testVectorCastLongToShortFail(lspec128, sspec256, lin128);
            testVectorCastLongToShortFail(lspec128, sspec512, lin128);

            testVectorCastLongToShortFail(lspec256, sspec128, lin256);
            testVectorCastLongToShortFail(lspec256, sspec256, lin256);
            testVectorCastLongToShortFail(lspec256, sspec512, lin256);

            testVectorCastLongToShortFail(lspec512, sspec64, lin512);
            testVectorCastLongToShortFail(lspec512, sspec256, lin512);
            testVectorCastLongToShortFail(lspec512, sspec512, lin512);

            testVectorCastLongToIntFail(lspec64, ispec64, lin64);
            testVectorCastLongToIntFail(lspec64, ispec128, lin64);
            testVectorCastLongToIntFail(lspec64, ispec256, lin64);
            testVectorCastLongToIntFail(lspec64, ispec512, lin64);

            testVectorCastLongToIntFail(lspec128, ispec128, lin128);
            testVectorCastLongToIntFail(lspec128, ispec256, lin128);
            testVectorCastLongToIntFail(lspec128, ispec512, lin128);

            testVectorCastLongToIntFail(lspec256, ispec64, lin256);
            testVectorCastLongToIntFail(lspec256, ispec256, lin256);
            testVectorCastLongToIntFail(lspec256, ispec512, lin256);

            testVectorCastLongToIntFail(lspec512, ispec64, lin512);
            testVectorCastLongToIntFail(lspec512, ispec128, lin512);
            testVectorCastLongToIntFail(lspec512, ispec512, lin512);

            testVectorCastLongToLongFail(lspec64, lspec128, lin64);
            testVectorCastLongToLongFail(lspec64, lspec256, lin64);
            testVectorCastLongToLongFail(lspec64, lspec512, lin64);

            testVectorCastLongToLongFail(lspec128, lspec64, lin128);
            testVectorCastLongToLongFail(lspec128, lspec256, lin128);
            testVectorCastLongToLongFail(lspec128, lspec512, lin128);

            testVectorCastLongToLongFail(lspec256, lspec64, lin256);
            testVectorCastLongToLongFail(lspec256, lspec128, lin256);
            testVectorCastLongToLongFail(lspec256, lspec512, lin256);

            testVectorCastLongToLongFail(lspec512, lspec64, lin512);
            testVectorCastLongToLongFail(lspec512, lspec128, lin512);
            testVectorCastLongToLongFail(lspec512, lspec256, lin512);

            testVectorCastLongToFloatFail(lspec64, fspec64, lin64);
            testVectorCastLongToFloatFail(lspec64, fspec128, lin64);
            testVectorCastLongToFloatFail(lspec64, fspec256, lin64);
            testVectorCastLongToFloatFail(lspec64, fspec512, lin64);

            testVectorCastLongToFloatFail(lspec128, fspec128, lin128);
            testVectorCastLongToFloatFail(lspec128, fspec256, lin128);
            testVectorCastLongToFloatFail(lspec128, fspec512, lin128);

            testVectorCastLongToFloatFail(lspec256, fspec64, lin256);
            testVectorCastLongToFloatFail(lspec256, fspec256, lin256);
            testVectorCastLongToFloatFail(lspec256, fspec512, lin256);

            testVectorCastLongToFloatFail(lspec512, fspec64, lin512);
            testVectorCastLongToFloatFail(lspec512, fspec128, lin512);
            testVectorCastLongToFloatFail(lspec512, fspec512, lin512);

            testVectorCastLongToDoubleFail(lspec64, dspec128, lin64);
            testVectorCastLongToDoubleFail(lspec64, dspec256, lin64);
            testVectorCastLongToDoubleFail(lspec64, dspec512, lin64);

            testVectorCastLongToDoubleFail(lspec128, dspec64, lin128);
            testVectorCastLongToDoubleFail(lspec128, dspec256, lin128);
            testVectorCastLongToDoubleFail(lspec128, dspec512, lin128);

            testVectorCastLongToDoubleFail(lspec256, dspec64, lin256);
            testVectorCastLongToDoubleFail(lspec256, dspec128, lin256);
            testVectorCastLongToDoubleFail(lspec256, dspec512, lin256);

            testVectorCastLongToDoubleFail(lspec512, dspec64, lin512);
            testVectorCastLongToDoubleFail(lspec512, dspec128, lin512);
            testVectorCastLongToDoubleFail(lspec512, dspec256, lin512);
        }
    }

    @Test(dataProvider = "floatUnaryOpProvider")
    static void testCastFromFloat(IntFunction<float[]> fa) {
        float[] fin64 = fa.apply(fspec64.length());
        float[] fin128 = fa.apply(fspec128.length());
        float[] fin256 = fa.apply(fspec256.length());
        float[] fin512 = fa.apply(fspec512.length());

        byte[] bout64 = new byte[bspec64.length()];
        byte[] bout128 = new byte[bspec128.length()];

        short[] sout64 = new short[sspec64.length()];
        short[] sout128 = new short[sspec128.length()];
        short[] sout256 = new short[sspec256.length()];

        int[] iout64 = new int[ispec64.length()];
        int[] iout128 = new int[ispec128.length()];
        int[] iout256 = new int[ispec256.length()];
        int[] iout512 = new int[ispec512.length()];

        long[] lout128 = new long[lspec128.length()];
        long[] lout256 = new long[lspec256.length()];
        long[] lout512 = new long[lspec512.length()];

        float[] fout64 = new float[fspec64.length()];
        float[] fout128 = new float[fspec128.length()];
        float[] fout256 = new float[fspec256.length()];
        float[] fout512 = new float[fspec512.length()];

        double[] dout128 = new double[dspec128.length()];
        double[] dout256 = new double[dspec256.length()];
        double[] dout512 = new double[dspec512.length()];

        for (int i = 0; i < NUM_ITER; i++) {
            testVectorCastFloatToByte(fspec256, bspec64, fin256, bout64);
            testVectorCastFloatToByte(fspec512, bspec128, fin512, bout128);

            testVectorCastFloatToShort(fspec128, sspec64, fin128, sout64);
            testVectorCastFloatToShort(fspec256, sspec128, fin256, sout128);
            testVectorCastFloatToShort(fspec512, sspec256, fin512, sout256);

            testVectorCastFloatToInt(fspec64, ispec64, fin64, iout64);
            testVectorCastFloatToInt(fspec128, ispec128, fin128, iout128);
            testVectorCastFloatToInt(fspec256, ispec256, fin256, iout256);
            testVectorCastFloatToInt(fspec512, ispec512, fin512, iout512);

            testVectorCastFloatToLong(fspec64, lspec128, fin64, lout128);
            testVectorCastFloatToLong(fspec128, lspec256, fin128, lout256);
            testVectorCastFloatToLong(fspec256, lspec512, fin256, lout512);

            testVectorCastFloatToFloat(fspec64, fspec64, fin64, fout64);
            testVectorCastFloatToFloat(fspec128, fspec128, fin128, fout128);
            testVectorCastFloatToFloat(fspec256, fspec256, fin256, fout256);
            testVectorCastFloatToFloat(fspec512, fspec512, fin512, fout512);

            testVectorCastFloatToDouble(fspec64, dspec128, fin64, dout128);
            testVectorCastFloatToDouble(fspec128, dspec256, fin128, dout256);
            testVectorCastFloatToDouble(fspec256, dspec512, fin256, dout512);
        }
    }

    @Test
    static void testCastFromFloatFail() {
        float[] fin64 = new float[fspec64.length()];
        float[] fin128 = new float[fspec128.length()];
        float[] fin256 = new float[fspec256.length()];
        float[] fin512 = new float[fspec512.length()];

        for (int i = 0; i < INVOC_COUNT; i++) {
            testVectorCastFloatToByteFail(fspec64, bspec64, fin64);
            testVectorCastFloatToByteFail(fspec64, bspec128, fin64);
            testVectorCastFloatToByteFail(fspec64, bspec256, fin64);
            testVectorCastFloatToByteFail(fspec64, bspec512, fin64);

            testVectorCastFloatToByteFail(fspec128, bspec64, fin128);
            testVectorCastFloatToByteFail(fspec128, bspec128, fin128);
            testVectorCastFloatToByteFail(fspec128, bspec256, fin128);
            testVectorCastFloatToByteFail(fspec128, bspec512, fin128);

            testVectorCastFloatToByteFail(fspec256, bspec128, fin256);
            testVectorCastFloatToByteFail(fspec256, bspec256, fin256);
            testVectorCastFloatToByteFail(fspec256, bspec512, fin256);

            testVectorCastFloatToByteFail(fspec512, bspec64, fin512);
            testVectorCastFloatToByteFail(fspec512, bspec256, fin512);
            testVectorCastFloatToByteFail(fspec512, bspec512, fin512);

            testVectorCastFloatToShortFail(fspec64, sspec64, fin64);
            testVectorCastFloatToShortFail(fspec64, sspec128, fin64);
            testVectorCastFloatToShortFail(fspec64, sspec256, fin64);
            testVectorCastFloatToShortFail(fspec64, sspec512, fin64);

            testVectorCastFloatToShortFail(fspec128, sspec128, fin128);
            testVectorCastFloatToShortFail(fspec128, sspec256, fin128);
            testVectorCastFloatToShortFail(fspec128, sspec512, fin128);

            testVectorCastFloatToShortFail(fspec256, sspec64, fin256);
            testVectorCastFloatToShortFail(fspec256, sspec256, fin256);
            testVectorCastFloatToShortFail(fspec256, sspec512, fin256);

            testVectorCastFloatToShortFail(fspec512, sspec64, fin512);
            testVectorCastFloatToShortFail(fspec512, sspec128, fin512);
            testVectorCastFloatToShortFail(fspec512, sspec512, fin512);

            testVectorCastFloatToIntFail(fspec64, ispec128, fin64);
            testVectorCastFloatToIntFail(fspec64, ispec256, fin64);
            testVectorCastFloatToIntFail(fspec64, ispec512, fin64);

            testVectorCastFloatToIntFail(fspec128, ispec64, fin128);
            testVectorCastFloatToIntFail(fspec128, ispec256, fin128);
            testVectorCastFloatToIntFail(fspec128, ispec512, fin128);

            testVectorCastFloatToIntFail(fspec256, ispec64, fin256);
            testVectorCastFloatToIntFail(fspec256, ispec128, fin256);
            testVectorCastFloatToIntFail(fspec256, ispec512, fin256);

            testVectorCastFloatToIntFail(fspec512, ispec64, fin512);
            testVectorCastFloatToIntFail(fspec512, ispec128, fin512);
            testVectorCastFloatToIntFail(fspec512, ispec256, fin512);

            testVectorCastFloatToLongFail(fspec64, lspec64, fin64);
            testVectorCastFloatToLongFail(fspec64, lspec256, fin64);
            testVectorCastFloatToLongFail(fspec64, lspec512, fin64);

            testVectorCastFloatToLongFail(fspec128, lspec64, fin128);
            testVectorCastFloatToLongFail(fspec128, lspec128, fin128);
            testVectorCastFloatToLongFail(fspec128, lspec512, fin128);

            testVectorCastFloatToLongFail(fspec256, lspec64, fin256);
            testVectorCastFloatToLongFail(fspec256, lspec128, fin256);
            testVectorCastFloatToLongFail(fspec256, lspec256, fin256);

            testVectorCastFloatToLongFail(fspec512, lspec64, fin512);
            testVectorCastFloatToLongFail(fspec512, lspec128, fin512);
            testVectorCastFloatToLongFail(fspec512, lspec256, fin512);
            testVectorCastFloatToLongFail(fspec512, lspec512, fin512);

            testVectorCastFloatToFloatFail(fspec64, fspec128, fin64);
            testVectorCastFloatToFloatFail(fspec64, fspec256, fin64);
            testVectorCastFloatToFloatFail(fspec64, fspec512, fin64);

            testVectorCastFloatToFloatFail(fspec128, fspec64, fin128);
            testVectorCastFloatToFloatFail(fspec128, fspec256, fin128);
            testVectorCastFloatToFloatFail(fspec128, fspec512, fin128);

            testVectorCastFloatToFloatFail(fspec256, fspec64, fin256);
            testVectorCastFloatToFloatFail(fspec256, fspec128, fin256);
            testVectorCastFloatToFloatFail(fspec256, fspec512, fin256);

            testVectorCastFloatToFloatFail(fspec512, fspec64, fin512);
            testVectorCastFloatToFloatFail(fspec512, fspec128, fin512);
            testVectorCastFloatToFloatFail(fspec512, fspec256, fin512);

            testVectorCastFloatToDoubleFail(fspec64, dspec64, fin64);
            testVectorCastFloatToDoubleFail(fspec64, dspec256, fin64);
            testVectorCastFloatToDoubleFail(fspec64, dspec512, fin64);

            testVectorCastFloatToDoubleFail(fspec128, dspec64, fin128);
            testVectorCastFloatToDoubleFail(fspec128, dspec128, fin128);
            testVectorCastFloatToDoubleFail(fspec128, dspec512, fin128);

            testVectorCastFloatToDoubleFail(fspec256, dspec64, fin256);
            testVectorCastFloatToDoubleFail(fspec256, dspec128, fin256);
            testVectorCastFloatToDoubleFail(fspec256, dspec256, fin256);

            testVectorCastFloatToDoubleFail(fspec512, dspec64, fin512);
            testVectorCastFloatToDoubleFail(fspec512, dspec128, fin512);
            testVectorCastFloatToDoubleFail(fspec512, dspec256, fin512);
            testVectorCastFloatToDoubleFail(fspec512, dspec512, fin512);
        }
    }

    @Test(dataProvider = "doubleUnaryOpProvider")
    static void testCastFromDouble(IntFunction<double[]> fa) {
        double[] din64 = fa.apply(dspec64.length());
        double[] din128 = fa.apply(dspec128.length());
        double[] din256 = fa.apply(dspec256.length());
        double[] din512 = fa.apply(dspec512.length());

        byte[] bout64 = new byte[bspec64.length()];

        short[] sout64 = new short[sspec64.length()];
        short[] sout128 = new short[sspec128.length()];

        int[] iout64 = new int[ispec64.length()];
        int[] iout128 = new int[ispec128.length()];
        int[] iout256 = new int[ispec256.length()];

        long[] lout64 = new long[lspec64.length()];
        long[] lout128 = new long[lspec128.length()];
        long[] lout256 = new long[lspec256.length()];
        long[] lout512 = new long[lspec512.length()];

        float[] fout64 = new float[fspec64.length()];
        float[] fout128 = new float[fspec128.length()];
        float[] fout256 = new float[fspec256.length()];

        double[] dout64 = new double[dspec64.length()];
        double[] dout128 = new double[dspec128.length()];
        double[] dout256 = new double[dspec256.length()];
        double[] dout512 = new double[dspec512.length()];

        for (int i = 0; i < NUM_ITER; i++) {
            testVectorCastDoubleToByte(dspec512, bspec64, din512, bout64);

            testVectorCastDoubleToShort(dspec256, sspec64, din256, sout64);
            testVectorCastDoubleToShort(dspec512, sspec128, din512, sout128);

            testVectorCastDoubleToInt(dspec128, ispec64, din128, iout64);
            testVectorCastDoubleToInt(dspec256, ispec128, din256, iout128);
            testVectorCastDoubleToInt(dspec512, ispec256, din512, iout256);

            testVectorCastDoubleToLong(dspec64, lspec64, din64, lout64);
            testVectorCastDoubleToLong(dspec128, lspec128, din128, lout128);
            testVectorCastDoubleToLong(dspec256, lspec256, din256, lout256);
            testVectorCastDoubleToLong(dspec512, lspec512, din512, lout512);

            testVectorCastDoubleToFloat(dspec128, fspec64, din128, fout64);
            testVectorCastDoubleToFloat(dspec256, fspec128, din256, fout128);
            testVectorCastDoubleToFloat(dspec512, fspec256, din512, fout256);

            testVectorCastDoubleToDouble(dspec64, dspec64, din64, dout64);
            testVectorCastDoubleToDouble(dspec128, dspec128, din128, dout128);
            testVectorCastDoubleToDouble(dspec256, dspec256, din256, dout256);
            testVectorCastDoubleToDouble(dspec512, dspec512, din512, dout512);
        }
    }

    @Test
    static void testCastFromDoubleFail() {
        double[] din64 = new double[dspec64.length()];
        double[] din128 = new double[dspec128.length()];
        double[] din256 = new double[dspec256.length()];
        double[] din512 = new double[dspec512.length()];

        for (int i = 0; i < INVOC_COUNT; i++) {
            testVectorCastDoubleToByteFail(dspec64, bspec64, din64);
            testVectorCastDoubleToByteFail(dspec64, bspec128, din64);
            testVectorCastDoubleToByteFail(dspec64, bspec256, din64);
            testVectorCastDoubleToByteFail(dspec64, bspec512, din64);

            testVectorCastDoubleToByteFail(dspec128, bspec64, din128);
            testVectorCastDoubleToByteFail(dspec128, bspec128, din128);
            testVectorCastDoubleToByteFail(dspec128, bspec256, din128);
            testVectorCastDoubleToByteFail(dspec128, bspec512, din128);

            testVectorCastDoubleToByteFail(dspec256, bspec64, din256);
            testVectorCastDoubleToByteFail(dspec256, bspec128, din256);
            testVectorCastDoubleToByteFail(dspec256, bspec256, din256);
            testVectorCastDoubleToByteFail(dspec256, bspec512, din256);

            testVectorCastDoubleToByteFail(dspec512, bspec128, din512);
            testVectorCastDoubleToByteFail(dspec512, bspec256, din512);
            testVectorCastDoubleToByteFail(dspec512, bspec512, din512);

            testVectorCastDoubleToShortFail(dspec64, sspec64, din64);
            testVectorCastDoubleToShortFail(dspec64, sspec128, din64);
            testVectorCastDoubleToShortFail(dspec64, sspec256, din64);
            testVectorCastDoubleToShortFail(dspec64, sspec512, din64);

            testVectorCastDoubleToShortFail(dspec128, sspec64, din128);
            testVectorCastDoubleToShortFail(dspec128, sspec128, din128);
            testVectorCastDoubleToShortFail(dspec128, sspec256, din128);
            testVectorCastDoubleToShortFail(dspec128, sspec512, din128);

            testVectorCastDoubleToShortFail(dspec256, sspec128, din256);
            testVectorCastDoubleToShortFail(dspec256, sspec256, din256);
            testVectorCastDoubleToShortFail(dspec256, sspec512, din256);

            testVectorCastDoubleToShortFail(dspec512, sspec64, din512);
            testVectorCastDoubleToShortFail(dspec512, sspec256, din512);
            testVectorCastDoubleToShortFail(dspec512, sspec512, din512);

            testVectorCastDoubleToIntFail(dspec64, ispec64, din64);
            testVectorCastDoubleToIntFail(dspec64, ispec128, din64);
            testVectorCastDoubleToIntFail(dspec64, ispec256, din64);
            testVectorCastDoubleToIntFail(dspec64, ispec512, din64);

            testVectorCastDoubleToIntFail(dspec128, ispec128, din128);
            testVectorCastDoubleToIntFail(dspec128, ispec256, din128);
            testVectorCastDoubleToIntFail(dspec128, ispec512, din128);

            testVectorCastDoubleToIntFail(dspec256, ispec64, din256);
            testVectorCastDoubleToIntFail(dspec256, ispec256, din256);
            testVectorCastDoubleToIntFail(dspec256, ispec512, din256);

            testVectorCastDoubleToIntFail(dspec512, ispec64, din512);
            testVectorCastDoubleToIntFail(dspec512, ispec128, din512);
            testVectorCastDoubleToIntFail(dspec512, ispec512, din512);

            testVectorCastDoubleToLongFail(dspec64, lspec128, din64);
            testVectorCastDoubleToLongFail(dspec64, lspec256, din64);
            testVectorCastDoubleToLongFail(dspec64, lspec512, din64);

            testVectorCastDoubleToLongFail(dspec128, lspec64, din128);
            testVectorCastDoubleToLongFail(dspec128, lspec256, din128);
            testVectorCastDoubleToLongFail(dspec128, lspec512, din128);

            testVectorCastDoubleToLongFail(dspec256, lspec64, din256);
            testVectorCastDoubleToLongFail(dspec256, lspec128, din256);
            testVectorCastDoubleToLongFail(dspec256, lspec512, din256);

            testVectorCastDoubleToLongFail(dspec512, lspec64, din512);
            testVectorCastDoubleToLongFail(dspec512, lspec128, din512);
            testVectorCastDoubleToLongFail(dspec512, lspec256, din512);

            testVectorCastDoubleToFloatFail(dspec64, fspec64, din64);
            testVectorCastDoubleToFloatFail(dspec64, fspec128, din64);
            testVectorCastDoubleToFloatFail(dspec64, fspec256, din64);
            testVectorCastDoubleToFloatFail(dspec64, fspec512, din64);

            testVectorCastDoubleToFloatFail(dspec128, fspec128, din128);
            testVectorCastDoubleToFloatFail(dspec128, fspec256, din128);
            testVectorCastDoubleToFloatFail(dspec128, fspec512, din128);

            testVectorCastDoubleToFloatFail(dspec256, fspec64, din256);
            testVectorCastDoubleToFloatFail(dspec256, fspec256, din256);
            testVectorCastDoubleToFloatFail(dspec256, fspec512, din256);

            testVectorCastDoubleToFloatFail(dspec512, fspec64, din512);
            testVectorCastDoubleToFloatFail(dspec512, fspec128, din512);
            testVectorCastDoubleToFloatFail(dspec512, fspec512, din512);

            testVectorCastDoubleToDoubleFail(dspec64, dspec128, din64);
            testVectorCastDoubleToDoubleFail(dspec64, dspec256, din64);
            testVectorCastDoubleToDoubleFail(dspec64, dspec512, din64);

            testVectorCastDoubleToDoubleFail(dspec128, dspec64, din128);
            testVectorCastDoubleToDoubleFail(dspec128, dspec256, din128);
            testVectorCastDoubleToDoubleFail(dspec128, dspec512, din128);

            testVectorCastDoubleToDoubleFail(dspec256, dspec64, din256);
            testVectorCastDoubleToDoubleFail(dspec256, dspec128, din256);
            testVectorCastDoubleToDoubleFail(dspec256, dspec512, din256);

            testVectorCastDoubleToDoubleFail(dspec512, dspec64, din512);
            testVectorCastDoubleToDoubleFail(dspec512, dspec128, din512);
            testVectorCastDoubleToDoubleFail(dspec512, dspec256, din512);
        }
    }

    static 
    void testVectorCastByteMaxToByte(ByteVector.ByteSpecies a, ByteVector.ByteSpecies b,
                                          byte[] input, byte[] output) {
        if (S_Max_BIT.bitSize() == b.length() * Byte.SIZE) {
            testVectorCastByteToByte(a, b, input, output);
        } else {
            testVectorCastByteToByteFail(a, b, input);
        }
    }

    static 
    void testVectorCastByteMaxToShort(ByteVector.ByteSpecies a, ShortVector.ShortSpecies b,
                                           byte[] input, short[] output) {
        if (S_Max_BIT.bitSize() == b.length() * Byte.SIZE) {
            testVectorCastByteToShort(a, b, input, output);
        } else {
            testVectorCastByteToShortFail(a, b, input);
        }
    }

    static 
    void testVectorCastByteMaxToInt(ByteVector.ByteSpecies a, IntVector.IntSpecies b,
                                         byte[] input, int[] output) {
        if (S_Max_BIT.bitSize() == b.length() * Byte.SIZE) {
            testVectorCastByteToInt(a, b, input, output);
        } else {
            testVectorCastByteToIntFail(a, b, input);
        }
    }

    static 
    void testVectorCastByteMaxToLong(ByteVector.ByteSpecies a, LongVector.LongSpecies b,
                                          byte[] input, long[] output) {
        if (S_Max_BIT.bitSize() == b.length() * Byte.SIZE) {
            testVectorCastByteToLong(a, b, input, output);
        } else {
            testVectorCastByteToLongFail(a, b, input);
        }
    }

    static 
    void testVectorCastByteMaxToFloat(ByteVector.ByteSpecies a, FloatVector.FloatSpecies b,
                                           byte[] input, float[] output) {
        if (S_Max_BIT.bitSize() == b.length() * Byte.SIZE) {
            testVectorCastByteToFloat(a, b, input, output);
        } else {
            testVectorCastByteToFloatFail(a, b, input);
        }
    }

    static 
    void testVectorCastByteMaxToDouble(ByteVector.ByteSpecies a, DoubleVector.DoubleSpecies b,
                                            byte[] input, double[] output) {
        if (S_Max_BIT.bitSize() == b.length() * Byte.SIZE) {
            testVectorCastByteToDouble(a, b, input, output);
        } else {
            testVectorCastByteToDoubleFail(a, b, input);
        }
    }

    static 
    void testVectorCastShortMaxToByte(ShortVector.ShortSpecies a, ByteVector.ByteSpecies b,
                                           short[] input, byte[] output) {
        if (S_Max_BIT.bitSize() == b.length() * Short.SIZE) {
            testVectorCastShortToByte(a, b, input, output);
        } else {
            testVectorCastShortToByteFail(a, b, input);
        }
    }

    static 
    void testVectorCastShortMaxToShort(ShortVector.ShortSpecies a, ShortVector.ShortSpecies b,
                                            short[] input, short[] output) {
        if (S_Max_BIT.bitSize() == b.length() * Short.SIZE) {
            testVectorCastShortToShort(a, b, input, output);
        } else {
            testVectorCastShortToShortFail(a, b, input);
        }
    }

    static 
    void testVectorCastShortMaxToInt(ShortVector.ShortSpecies a, IntVector.IntSpecies b,
                                          short[] input, int[] output) {
        if (S_Max_BIT.bitSize() == b.length() * Short.SIZE) {
            testVectorCastShortToInt(a, b, input, output);
        } else {
            testVectorCastShortToIntFail(a, b, input);
        }
    }

    static 
    void testVectorCastShortMaxToLong(ShortVector.ShortSpecies a, LongVector.LongSpecies b,
                                           short[] input, long[] output) {
        if (S_Max_BIT.bitSize() == b.length() * Short.SIZE) {
            testVectorCastShortToLong(a, b, input, output);
        } else {
            testVectorCastShortToLongFail(a, b, input);
        }
    }

    static 
    void testVectorCastShortMaxToFloat(ShortVector.ShortSpecies a, FloatVector.FloatSpecies b,
                                            short[] input, float[] output) {
        if (S_Max_BIT.bitSize() == b.length() * Short.SIZE) {
            testVectorCastShortToFloat(a, b, input, output);
        } else {
            testVectorCastShortToFloatFail(a, b, input);
        }
    }

    static 
    void testVectorCastShortMaxToDouble(ShortVector.ShortSpecies a, DoubleVector.DoubleSpecies b,
                                             short[] input, double[] output) {
        if (S_Max_BIT.bitSize() == b.length() * Short.SIZE) {
            testVectorCastShortToDouble(a, b, input, output);
        } else {
            testVectorCastShortToDoubleFail(a, b, input);
        }
    }

    static 
    void testVectorCastIntMaxToByte(IntVector.IntSpecies a, ByteVector.ByteSpecies b,
                                         int[] input, byte[] output) {
        if (S_Max_BIT.bitSize() == b.length() * Integer.SIZE) {
            testVectorCastIntToByte(a, b, input, output);
        } else {
            testVectorCastIntToByteFail(a, b, input);
        }
    }

    static 
    void testVectorCastIntMaxToShort(IntVector.IntSpecies a, ShortVector.ShortSpecies b,
                                          int[] input, short[] output) {
        if (S_Max_BIT.bitSize() == b.length() * Integer.SIZE) {
            testVectorCastIntToShort(a, b, input, output);
        } else {
            testVectorCastIntToShortFail(a, b, input);
        }
    }

    static 
    void testVectorCastIntMaxToInt(IntVector.IntSpecies a, IntVector.IntSpecies b,
                                        int[] input, int[] output) {
        if (S_Max_BIT.bitSize() == b.length() * Integer.SIZE) {
            testVectorCastIntToInt(a, b, input, output);
        } else {
            testVectorCastIntToIntFail(a, b, input);
        }
    }

    static 
    void testVectorCastIntMaxToLong(IntVector.IntSpecies a, LongVector.LongSpecies b,
                                         int[] input, long[] output) {
        if (S_Max_BIT.bitSize() == b.length() * Integer.SIZE) {
            testVectorCastIntToLong(a, b, input, output);
        } else {
            testVectorCastIntToLongFail(a, b, input);
        }
    }

    static 
    void testVectorCastIntMaxToFloat(IntVector.IntSpecies a, FloatVector.FloatSpecies b,
                                          int[] input, float[] output) {
        if (S_Max_BIT.bitSize() == b.length() * Integer.SIZE) {
            testVectorCastIntToFloat(a, b, input, output);
        } else {
            testVectorCastIntToFloatFail(a, b, input);
        }
    }

    static 
    void testVectorCastIntMaxToDouble(IntVector.IntSpecies a, DoubleVector.DoubleSpecies b,
                                           int[] input, double[] output) {
        if (S_Max_BIT.bitSize() == b.length() * Integer.SIZE) {
            testVectorCastIntToDouble(a, b, input, output);
        } else {
            testVectorCastIntToDoubleFail(a, b, input);
        }
    }

    static 
    void testVectorCastLongMaxToByte(LongVector.LongSpecies a, ByteVector.ByteSpecies b,
                                          long[] input, byte[] output) {
        if (S_Max_BIT.bitSize() == b.length() * Long.SIZE) {
            testVectorCastLongToByte(a, b, input, output);
        } else {
            testVectorCastLongToByteFail(a, b, input);
        }
    }

    static 
    void testVectorCastLongMaxToShort(LongVector.LongSpecies a, ShortVector.ShortSpecies b,
                                           long[] input, short[] output) {
        if (S_Max_BIT.bitSize() == b.length() * Long.SIZE) {
            testVectorCastLongToShort(a, b, input, output);
        } else {
            testVectorCastLongToShortFail(a, b, input);
        }
    }

    static 
    void testVectorCastLongMaxToInt(LongVector.LongSpecies a, IntVector.IntSpecies b,
                                         long[] input, int[] output) {
        if (S_Max_BIT.bitSize() == b.length() * Long.SIZE) {
            testVectorCastLongToInt(a, b, input, output);
        } else {
            testVectorCastLongToIntFail(a, b, input);
        }
    }

    static 
    void testVectorCastLongMaxToLong(LongVector.LongSpecies a, LongVector.LongSpecies b,
                                          long[] input, long[] output) {
        if (S_Max_BIT.bitSize() == b.length() * Long.SIZE) {
            testVectorCastLongToLong(a, b, input, output);
        } else {
            testVectorCastLongToLongFail(a, b, input);
        }
    }

    static 
    void testVectorCastLongMaxToFloat(LongVector.LongSpecies a, FloatVector.FloatSpecies b,
                                           long[] input, float[] output) {
        if (S_Max_BIT.bitSize() == b.length() * Long.SIZE) {
            testVectorCastLongToFloat(a, b, input, output);
        } else {
            testVectorCastLongToFloatFail(a, b, input);
        }
    }

    static 
    void testVectorCastLongMaxToDouble(LongVector.LongSpecies a, DoubleVector.DoubleSpecies b,
                                            long[] input, double[] output) {
        if (S_Max_BIT.bitSize() == b.length() * Long.SIZE) {
            testVectorCastLongToDouble(a, b, input, output);
        } else {
            testVectorCastLongToDoubleFail(a, b, input);
        }
    }

    static 
    void testVectorCastFloatMaxToByte(FloatVector.FloatSpecies a, ByteVector.ByteSpecies b,
                                           float[] input, byte[] output) {
        if (S_Max_BIT.bitSize() == b.length() * Float.SIZE) {
            testVectorCastFloatToByte(a, b, input, output);
        } else {
            testVectorCastFloatToByteFail(a, b, input);
        }
    }

    static 
    void testVectorCastFloatMaxToShort(FloatVector.FloatSpecies a, ShortVector.ShortSpecies b,
                                            float[] input, short[] output) {
        if (S_Max_BIT.bitSize() == b.length() * Float.SIZE) {
            testVectorCastFloatToShort(a, b, input, output);
        } else {
            testVectorCastFloatToShortFail(a, b, input);
        }
    }

    static 
    void testVectorCastFloatMaxToInt(FloatVector.FloatSpecies a, IntVector.IntSpecies b,
                                          float[] input, int[] output) {
        if (S_Max_BIT.bitSize() == b.length() * Float.SIZE) {
            testVectorCastFloatToInt(a, b, input, output);
        } else {
            testVectorCastFloatToIntFail(a, b, input);
        }
    }

    static 
    void testVectorCastFloatMaxToLong(FloatVector.FloatSpecies a, LongVector.LongSpecies b,
                                           float[] input, long[] output) {
        if (S_Max_BIT.bitSize() == b.length() * Float.SIZE) {
            testVectorCastFloatToLong(a, b, input, output);
        } else {
            testVectorCastFloatToLongFail(a, b, input);
        }
    }

    static 
    void testVectorCastFloatMaxToFloat(FloatVector.FloatSpecies a, FloatVector.FloatSpecies b,
                                            float[] input, float[] output) {
        if (S_Max_BIT.bitSize() == b.length() * Float.SIZE) {
            testVectorCastFloatToFloat(a, b, input, output);
        } else {
            testVectorCastFloatToFloatFail(a, b, input);
        }
    }

    static 
    void testVectorCastFloatMaxToDouble(FloatVector.FloatSpecies a, DoubleVector.DoubleSpecies b,
                                             float[] input, double[] output) {
        if (S_Max_BIT.bitSize() == b.length() * Float.SIZE) {
            testVectorCastFloatToDouble(a, b, input, output);
        } else {
            testVectorCastFloatToDoubleFail(a, b, input);
        }
    }

    static 
    void testVectorCastDoubleMaxToByte(DoubleVector.DoubleSpecies a, ByteVector.ByteSpecies b,
                                            double[] input, byte[] output) {
        if (S_Max_BIT.bitSize() == b.length() * Double.SIZE) {
            testVectorCastDoubleToByte(a, b, input, output);
        } else {
            testVectorCastDoubleToByteFail(a, b, input);
        }
    }

    static 
    void testVectorCastDoubleMaxToShort(DoubleVector.DoubleSpecies a, ShortVector.ShortSpecies b,
                                             double[] input, short[] output) {
        if (S_Max_BIT.bitSize() == b.length() * Double.SIZE) {
            testVectorCastDoubleToShort(a, b, input, output);
        } else {
            testVectorCastDoubleToShortFail(a, b, input);
        }
    }

    static 
    void testVectorCastDoubleMaxToInt(DoubleVector.DoubleSpecies a, IntVector.IntSpecies b,
                                           double[] input, int[] output) {
        if (S_Max_BIT.bitSize() == b.length() * Double.SIZE) {
            testVectorCastDoubleToInt(a, b, input, output);
        } else {
            testVectorCastDoubleToIntFail(a, b, input);
        }
    }

    static 
    void testVectorCastDoubleMaxToLong(DoubleVector.DoubleSpecies a, LongVector.LongSpecies b,
                                            double[] input, long[] output) {
        if (S_Max_BIT.bitSize() == b.length() * Double.SIZE) {
            testVectorCastDoubleToLong(a, b, input, output);
        } else {
            testVectorCastDoubleToLongFail(a, b, input);
        }
    }

    static 
    void testVectorCastDoubleMaxToFloat(DoubleVector.DoubleSpecies a, FloatVector.FloatSpecies b,
                                             double[] input, float[] output) {
        if (S_Max_BIT.bitSize() == b.length() * Double.SIZE) {
            testVectorCastDoubleToFloat(a, b, input, output);
        } else {
            testVectorCastDoubleToFloatFail(a, b, input);
        }
    }

    static 
    void testVectorCastDoubleMaxToDouble(DoubleVector.DoubleSpecies a, DoubleVector.DoubleSpecies b,
                                              double[] input, double[] output) {
        if (S_Max_BIT.bitSize() == b.length() * Double.SIZE) {
            testVectorCastDoubleToDouble(a, b, input, output);
        } else {
            testVectorCastDoubleToDoubleFail(a, b, input);
        }
    }

    @Test(dataProvider = "byteUnaryOpProvider")
    static void testCastFromByteMax(IntFunction<byte[]> fa) {
        byte[] binMax = fa.apply(bspecMax.length());

        byte[] bout64 = new byte[bspec64.length()];
        byte[] bout128 = new byte[bspec128.length()];
        byte[] bout256 = new byte[bspec256.length()];
        byte[] bout512 = new byte[bspec512.length()];
        byte[] boutMax = new byte[bspecMax.length()];

        short[] sout64 = new short[sspec64.length()];
        short[] sout128 = new short[sspec128.length()];
        short[] sout256 = new short[sspec256.length()];
        short[] sout512 = new short[sspec512.length()];
        short[] soutMax = new short[sspecMax.length()];

        int[] iout64 = new int[ispec64.length()];
        int[] iout128 = new int[ispec128.length()];
        int[] iout256 = new int[ispec256.length()];
        int[] iout512 = new int[ispec512.length()];
        int[] ioutMax = new int[ispecMax.length()];

        long[] lout64 = new long[lspec64.length()];
        long[] lout128 = new long[lspec128.length()];
        long[] lout256 = new long[lspec256.length()];
        long[] lout512 = new long[lspec512.length()];
        long[] loutMax = new long[lspecMax.length()];

        float[] fout64 = new float[fspec64.length()];
        float[] fout128 = new float[fspec128.length()];
        float[] fout256 = new float[fspec256.length()];
        float[] fout512 = new float[fspec512.length()];
        float[] foutMax = new float[fspecMax.length()];

        double[] dout64 = new double[dspec64.length()];
        double[] dout128 = new double[dspec128.length()];
        double[] dout256 = new double[dspec256.length()];
        double[] dout512 = new double[dspec512.length()];
        double[] doutMax = new double[dspecMax.length()];

        for (int i = 0; i < NUM_ITER; i++) {
            testVectorCastByteMaxToByte(bspecMax, bspec64, binMax, bout64);
            testVectorCastByteMaxToByte(bspecMax, bspec128, binMax, bout128);
            testVectorCastByteMaxToByte(bspecMax, bspec256, binMax, bout256);
            testVectorCastByteMaxToByte(bspecMax, bspec512, binMax, bout512);
            testVectorCastByteMaxToByte(bspecMax, bspecMax, binMax, boutMax);

            testVectorCastByteMaxToShort(bspecMax, sspec64, binMax, sout64);
            testVectorCastByteMaxToShort(bspecMax, sspec128, binMax, sout128);
            testVectorCastByteMaxToShort(bspecMax, sspec256, binMax, sout256);
            testVectorCastByteMaxToShort(bspecMax, sspec512, binMax, sout512);
            testVectorCastByteMaxToShort(bspecMax, sspecMax, binMax, soutMax);

            testVectorCastByteMaxToInt(bspecMax, ispec64, binMax, iout64);
            testVectorCastByteMaxToInt(bspecMax, ispec128, binMax, iout128);
            testVectorCastByteMaxToInt(bspecMax, ispec256, binMax, iout256);
            testVectorCastByteMaxToInt(bspecMax, ispec512, binMax, iout512);
            testVectorCastByteMaxToInt(bspecMax, ispecMax, binMax, ioutMax);

            testVectorCastByteMaxToLong(bspecMax, lspec64, binMax, lout64);
            testVectorCastByteMaxToLong(bspecMax, lspec128, binMax, lout128);
            testVectorCastByteMaxToLong(bspecMax, lspec256, binMax, lout256);
            testVectorCastByteMaxToLong(bspecMax, lspec512, binMax, lout512);
            testVectorCastByteMaxToLong(bspecMax, lspecMax, binMax, loutMax);

            testVectorCastByteMaxToFloat(bspecMax, fspec64, binMax, fout64);
            testVectorCastByteMaxToFloat(bspecMax, fspec128, binMax, fout128);
            testVectorCastByteMaxToFloat(bspecMax, fspec256, binMax, fout256);
            testVectorCastByteMaxToFloat(bspecMax, fspec512, binMax, fout512);
            testVectorCastByteMaxToFloat(bspecMax, fspecMax, binMax, foutMax);

            testVectorCastByteMaxToDouble(bspecMax, dspec64, binMax, dout64);
            testVectorCastByteMaxToDouble(bspecMax, dspec128, binMax, dout128);
            testVectorCastByteMaxToDouble(bspecMax, dspec256, binMax, dout256);
            testVectorCastByteMaxToDouble(bspecMax, dspec512, binMax, dout512);
            testVectorCastByteMaxToDouble(bspecMax, dspecMax, binMax, doutMax);
        }
    }

    @Test(dataProvider = "shortUnaryOpProvider")
    static void testCastFromShortMax(IntFunction<short[]> fa) {
        short[] sinMax = fa.apply(sspecMax.length());

        byte[] bout64 = new byte[bspec64.length()];
        byte[] bout128 = new byte[bspec128.length()];
        byte[] bout256 = new byte[bspec256.length()];
        byte[] bout512 = new byte[bspec512.length()];
        byte[] boutMax = new byte[bspecMax.length()];

        short[] sout64 = new short[sspec64.length()];
        short[] sout128 = new short[sspec128.length()];
        short[] sout256 = new short[sspec256.length()];
        short[] sout512 = new short[sspec512.length()];
        short[] soutMax = new short[sspecMax.length()];

        int[] iout64 = new int[ispec64.length()];
        int[] iout128 = new int[ispec128.length()];
        int[] iout256 = new int[ispec256.length()];
        int[] iout512 = new int[ispec512.length()];
        int[] ioutMax = new int[ispecMax.length()];

        long[] lout64 = new long[lspec64.length()];
        long[] lout128 = new long[lspec128.length()];
        long[] lout256 = new long[lspec256.length()];
        long[] lout512 = new long[lspec512.length()];
        long[] loutMax = new long[lspecMax.length()];

        float[] fout64 = new float[fspec64.length()];
        float[] fout128 = new float[fspec128.length()];
        float[] fout256 = new float[fspec256.length()];
        float[] fout512 = new float[fspec512.length()];
        float[] foutMax = new float[fspecMax.length()];

        double[] dout64 = new double[dspec64.length()];
        double[] dout128 = new double[dspec128.length()];
        double[] dout256 = new double[dspec256.length()];
        double[] dout512 = new double[dspec512.length()];
        double[] doutMax = new double[dspecMax.length()];

        for (int i = 0; i < NUM_ITER; i++) {
            testVectorCastShortMaxToByte(sspecMax, bspec64, sinMax, bout64);
            testVectorCastShortMaxToByte(sspecMax, bspec128, sinMax, bout128);
            testVectorCastShortMaxToByte(sspecMax, bspec256, sinMax, bout256);
            testVectorCastShortMaxToByte(sspecMax, bspec512, sinMax, bout512);
            testVectorCastShortMaxToByte(sspecMax, bspecMax, sinMax, boutMax);

            testVectorCastShortMaxToShort(sspecMax, sspec64, sinMax, sout64);
            testVectorCastShortMaxToShort(sspecMax, sspec128, sinMax, sout128);
            testVectorCastShortMaxToShort(sspecMax, sspec256, sinMax, sout256);
            testVectorCastShortMaxToShort(sspecMax, sspec512, sinMax, sout512);
            testVectorCastShortMaxToShort(sspecMax, sspecMax, sinMax, soutMax);

            testVectorCastShortMaxToInt(sspecMax, ispec64, sinMax, iout64);
            testVectorCastShortMaxToInt(sspecMax, ispec128, sinMax, iout128);
            testVectorCastShortMaxToInt(sspecMax, ispec256, sinMax, iout256);
            testVectorCastShortMaxToInt(sspecMax, ispec512, sinMax, iout512);
            testVectorCastShortMaxToInt(sspecMax, ispecMax, sinMax, ioutMax);

            testVectorCastShortMaxToLong(sspecMax, lspec64, sinMax, lout64);
            testVectorCastShortMaxToLong(sspecMax, lspec128, sinMax, lout128);
            testVectorCastShortMaxToLong(sspecMax, lspec256, sinMax, lout256);
            testVectorCastShortMaxToLong(sspecMax, lspec512, sinMax, lout512);
            testVectorCastShortMaxToLong(sspecMax, lspecMax, sinMax, loutMax);

            testVectorCastShortMaxToFloat(sspecMax, fspec64, sinMax, fout64);
            testVectorCastShortMaxToFloat(sspecMax, fspec128, sinMax, fout128);
            testVectorCastShortMaxToFloat(sspecMax, fspec256, sinMax, fout256);
            testVectorCastShortMaxToFloat(sspecMax, fspec512, sinMax, fout512);
            testVectorCastShortMaxToFloat(sspecMax, fspecMax, sinMax, foutMax);

            testVectorCastShortMaxToDouble(sspecMax, dspec64, sinMax, dout64);
            testVectorCastShortMaxToDouble(sspecMax, dspec128, sinMax, dout128);
            testVectorCastShortMaxToDouble(sspecMax, dspec256, sinMax, dout256);
            testVectorCastShortMaxToDouble(sspecMax, dspec512, sinMax, dout512);
            testVectorCastShortMaxToDouble(sspecMax, dspecMax, sinMax, doutMax);
        }
    }

    @Test(dataProvider = "intUnaryOpProvider")
    static void testCastFromIntMax(IntFunction<int[]> fa) {
        int[] iinMax = fa.apply(ispecMax.length());

        byte[] bout64 = new byte[bspec64.length()];
        byte[] bout128 = new byte[bspec128.length()];
        byte[] bout256 = new byte[bspec256.length()];
        byte[] bout512 = new byte[bspec512.length()];
        byte[] boutMax = new byte[bspecMax.length()];

        short[] sout64 = new short[sspec64.length()];
        short[] sout128 = new short[sspec128.length()];
        short[] sout256 = new short[sspec256.length()];
        short[] sout512 = new short[sspec512.length()];
        short[] soutMax = new short[sspecMax.length()];

        int[] iout64 = new int[ispec64.length()];
        int[] iout128 = new int[ispec128.length()];
        int[] iout256 = new int[ispec256.length()];
        int[] iout512 = new int[ispec512.length()];
        int[] ioutMax = new int[ispecMax.length()];

        long[] lout64 = new long[lspec64.length()];
        long[] lout128 = new long[lspec128.length()];
        long[] lout256 = new long[lspec256.length()];
        long[] lout512 = new long[lspec512.length()];
        long[] loutMax = new long[lspecMax.length()];

        float[] fout64 = new float[fspec64.length()];
        float[] fout128 = new float[fspec128.length()];
        float[] fout256 = new float[fspec256.length()];
        float[] fout512 = new float[fspec512.length()];
        float[] foutMax = new float[fspecMax.length()];

        double[] dout64 = new double[dspec64.length()];
        double[] dout128 = new double[dspec128.length()];
        double[] dout256 = new double[dspec256.length()];
        double[] dout512 = new double[dspec512.length()];
        double[] doutMax = new double[dspecMax.length()];

        for (int i = 0; i < NUM_ITER; i++) {
            testVectorCastIntMaxToByte(ispecMax, bspec64, iinMax, bout64);
            testVectorCastIntMaxToByte(ispecMax, bspec128, iinMax, bout128);
            testVectorCastIntMaxToByte(ispecMax, bspec256, iinMax, bout256);
            testVectorCastIntMaxToByte(ispecMax, bspec512, iinMax, bout512);
            testVectorCastIntMaxToByte(ispecMax, bspecMax, iinMax, boutMax);

            testVectorCastIntMaxToShort(ispecMax, sspec64, iinMax, sout64);
            testVectorCastIntMaxToShort(ispecMax, sspec128, iinMax, sout128);
            testVectorCastIntMaxToShort(ispecMax, sspec256, iinMax, sout256);
            testVectorCastIntMaxToShort(ispecMax, sspec512, iinMax, sout512);
            testVectorCastIntMaxToShort(ispecMax, sspecMax, iinMax, soutMax);

            testVectorCastIntMaxToInt(ispecMax, ispec64, iinMax, iout64);
            testVectorCastIntMaxToInt(ispecMax, ispec128, iinMax, iout128);
            testVectorCastIntMaxToInt(ispecMax, ispec256, iinMax, iout256);
            testVectorCastIntMaxToInt(ispecMax, ispec512, iinMax, iout512);
            testVectorCastIntMaxToInt(ispecMax, ispecMax, iinMax, ioutMax);

            testVectorCastIntMaxToLong(ispecMax, lspec64, iinMax, lout64);
            testVectorCastIntMaxToLong(ispecMax, lspec128, iinMax, lout128);
            testVectorCastIntMaxToLong(ispecMax, lspec256, iinMax, lout256);
            testVectorCastIntMaxToLong(ispecMax, lspec512, iinMax, lout512);
            testVectorCastIntMaxToLong(ispecMax, lspecMax, iinMax, loutMax);

            testVectorCastIntMaxToFloat(ispecMax, fspec64, iinMax, fout64);
            testVectorCastIntMaxToFloat(ispecMax, fspec128, iinMax, fout128);
            testVectorCastIntMaxToFloat(ispecMax, fspec256, iinMax, fout256);
            testVectorCastIntMaxToFloat(ispecMax, fspec512, iinMax, fout512);
            testVectorCastIntMaxToFloat(ispecMax, fspecMax, iinMax, foutMax);

            testVectorCastIntMaxToDouble(ispecMax, dspec64, iinMax, dout64);
            testVectorCastIntMaxToDouble(ispecMax, dspec128, iinMax, dout128);
            testVectorCastIntMaxToDouble(ispecMax, dspec256, iinMax, dout256);
            testVectorCastIntMaxToDouble(ispecMax, dspec512, iinMax, dout512);
            testVectorCastIntMaxToDouble(ispecMax, dspecMax, iinMax, doutMax);
        }
    }

    @Test(dataProvider = "longUnaryOpProvider")
    static void testCastFromLongMax(IntFunction<long[]> fa) {
        long[] linMax = fa.apply(lspecMax.length());

        byte[] bout64 = new byte[bspec64.length()];
        byte[] bout128 = new byte[bspec128.length()];
        byte[] bout256 = new byte[bspec256.length()];
        byte[] bout512 = new byte[bspec512.length()];
        byte[] boutMax = new byte[bspecMax.length()];

        short[] sout64 = new short[sspec64.length()];
        short[] sout128 = new short[sspec128.length()];
        short[] sout256 = new short[sspec256.length()];
        short[] sout512 = new short[sspec512.length()];
        short[] soutMax = new short[sspecMax.length()];

        int[] iout64 = new int[ispec64.length()];
        int[] iout128 = new int[ispec128.length()];
        int[] iout256 = new int[ispec256.length()];
        int[] iout512 = new int[ispec512.length()];
        int[] ioutMax = new int[ispecMax.length()];

        long[] lout64 = new long[lspec64.length()];
        long[] lout128 = new long[lspec128.length()];
        long[] lout256 = new long[lspec256.length()];
        long[] lout512 = new long[lspec512.length()];
        long[] loutMax = new long[lspecMax.length()];

        float[] fout64 = new float[fspec64.length()];
        float[] fout128 = new float[fspec128.length()];
        float[] fout256 = new float[fspec256.length()];
        float[] fout512 = new float[fspec512.length()];
        float[] foutMax = new float[fspecMax.length()];

        double[] dout64 = new double[dspec64.length()];
        double[] dout128 = new double[dspec128.length()];
        double[] dout256 = new double[dspec256.length()];
        double[] dout512 = new double[dspec512.length()];
        double[] doutMax = new double[dspecMax.length()];

        for (int i = 0; i < NUM_ITER; i++) {
            testVectorCastLongMaxToByte(lspecMax, bspec64, linMax, bout64);
            testVectorCastLongMaxToByte(lspecMax, bspec128, linMax, bout128);
            testVectorCastLongMaxToByte(lspecMax, bspec256, linMax, bout256);
            testVectorCastLongMaxToByte(lspecMax, bspec512, linMax, bout512);
            testVectorCastLongMaxToByte(lspecMax, bspecMax, linMax, boutMax);

            testVectorCastLongMaxToShort(lspecMax, sspec64, linMax, sout64);
            testVectorCastLongMaxToShort(lspecMax, sspec128, linMax, sout128);
            testVectorCastLongMaxToShort(lspecMax, sspec256, linMax, sout256);
            testVectorCastLongMaxToShort(lspecMax, sspec512, linMax, sout512);
            testVectorCastLongMaxToShort(lspecMax, sspecMax, linMax, soutMax);

            testVectorCastLongMaxToInt(lspecMax, ispec64, linMax, iout64);
            testVectorCastLongMaxToInt(lspecMax, ispec128, linMax, iout128);
            testVectorCastLongMaxToInt(lspecMax, ispec256, linMax, iout256);
            testVectorCastLongMaxToInt(lspecMax, ispec512, linMax, iout512);
            testVectorCastLongMaxToInt(lspecMax, ispecMax, linMax, ioutMax);

            testVectorCastLongMaxToLong(lspecMax, lspec64, linMax, lout64);
            testVectorCastLongMaxToLong(lspecMax, lspec128, linMax, lout128);
            testVectorCastLongMaxToLong(lspecMax, lspec256, linMax, lout256);
            testVectorCastLongMaxToLong(lspecMax, lspec512, linMax, lout512);
            testVectorCastLongMaxToLong(lspecMax, lspecMax, linMax, loutMax);

            testVectorCastLongMaxToFloat(lspecMax, fspec64, linMax, fout64);
            testVectorCastLongMaxToFloat(lspecMax, fspec128, linMax, fout128);
            testVectorCastLongMaxToFloat(lspecMax, fspec256, linMax, fout256);
            testVectorCastLongMaxToFloat(lspecMax, fspec512, linMax, fout512);
            testVectorCastLongMaxToFloat(lspecMax, fspecMax, linMax, foutMax);

            testVectorCastLongMaxToDouble(lspecMax, dspec64, linMax, dout64);
            testVectorCastLongMaxToDouble(lspecMax, dspec128, linMax, dout128);
            testVectorCastLongMaxToDouble(lspecMax, dspec256, linMax, dout256);
            testVectorCastLongMaxToDouble(lspecMax, dspec512, linMax, dout512);
            testVectorCastLongMaxToDouble(lspecMax, dspecMax, linMax, doutMax);
        }
    }

    @Test(dataProvider = "floatUnaryOpProvider")
    static void testCastFromFloatMax(IntFunction<float[]> fa) {
        float[] finMax = fa.apply(fspecMax.length());

        byte[] bout64 = new byte[bspec64.length()];
        byte[] bout128 = new byte[bspec128.length()];
        byte[] bout256 = new byte[bspec256.length()];
        byte[] bout512 = new byte[bspec512.length()];
        byte[] boutMax = new byte[bspecMax.length()];

        short[] sout64 = new short[sspec64.length()];
        short[] sout128 = new short[sspec128.length()];
        short[] sout256 = new short[sspec256.length()];
        short[] sout512 = new short[sspec512.length()];
        short[] soutMax = new short[sspecMax.length()];

        int[] iout64 = new int[ispec64.length()];
        int[] iout128 = new int[ispec128.length()];
        int[] iout256 = new int[ispec256.length()];
        int[] iout512 = new int[ispec512.length()];
        int[] ioutMax = new int[ispecMax.length()];

        long[] lout64 = new long[lspec64.length()];
        long[] lout128 = new long[lspec128.length()];
        long[] lout256 = new long[lspec256.length()];
        long[] lout512 = new long[lspec512.length()];
        long[] loutMax = new long[lspecMax.length()];

        float[] fout64 = new float[fspec64.length()];
        float[] fout128 = new float[fspec128.length()];
        float[] fout256 = new float[fspec256.length()];
        float[] fout512 = new float[fspec512.length()];
        float[] foutMax = new float[fspecMax.length()];

        double[] dout64 = new double[dspec64.length()];
        double[] dout128 = new double[dspec128.length()];
        double[] dout256 = new double[dspec256.length()];
        double[] dout512 = new double[dspec512.length()];
        double[] doutMax = new double[dspecMax.length()];

        for (int i = 0; i < NUM_ITER; i++) {
            testVectorCastFloatMaxToByte(fspecMax, bspec64, finMax, bout64);
            testVectorCastFloatMaxToByte(fspecMax, bspec128, finMax, bout128);
            testVectorCastFloatMaxToByte(fspecMax, bspec256, finMax, bout256);
            testVectorCastFloatMaxToByte(fspecMax, bspec512, finMax, bout512);
            testVectorCastFloatMaxToByte(fspecMax, bspecMax, finMax, boutMax);

            testVectorCastFloatMaxToShort(fspecMax, sspec64, finMax, sout64);
            testVectorCastFloatMaxToShort(fspecMax, sspec128, finMax, sout128);
            testVectorCastFloatMaxToShort(fspecMax, sspec256, finMax, sout256);
            testVectorCastFloatMaxToShort(fspecMax, sspec512, finMax, sout512);
            testVectorCastFloatMaxToShort(fspecMax, sspecMax, finMax, soutMax);

            testVectorCastFloatMaxToInt(fspecMax, ispec64, finMax, iout64);
            testVectorCastFloatMaxToInt(fspecMax, ispec128, finMax, iout128);
            testVectorCastFloatMaxToInt(fspecMax, ispec256, finMax, iout256);
            testVectorCastFloatMaxToInt(fspecMax, ispec512, finMax, iout512);
            testVectorCastFloatMaxToInt(fspecMax, ispecMax, finMax, ioutMax);

            testVectorCastFloatMaxToLong(fspecMax, lspec64, finMax, lout64);
            testVectorCastFloatMaxToLong(fspecMax, lspec128, finMax, lout128);
            testVectorCastFloatMaxToLong(fspecMax, lspec256, finMax, lout256);
            testVectorCastFloatMaxToLong(fspecMax, lspec512, finMax, lout512);
            testVectorCastFloatMaxToLong(fspecMax, lspecMax, finMax, loutMax);

            testVectorCastFloatMaxToFloat(fspecMax, fspec64, finMax, fout64);
            testVectorCastFloatMaxToFloat(fspecMax, fspec128, finMax, fout128);
            testVectorCastFloatMaxToFloat(fspecMax, fspec256, finMax, fout256);
            testVectorCastFloatMaxToFloat(fspecMax, fspec512, finMax, fout512);
            testVectorCastFloatMaxToFloat(fspecMax, fspecMax, finMax, foutMax);

            testVectorCastFloatMaxToDouble(fspecMax, dspec64, finMax, dout64);
            testVectorCastFloatMaxToDouble(fspecMax, dspec128, finMax, dout128);
            testVectorCastFloatMaxToDouble(fspecMax, dspec256, finMax, dout256);
            testVectorCastFloatMaxToDouble(fspecMax, dspec512, finMax, dout512);
            testVectorCastFloatMaxToDouble(fspecMax, dspecMax, finMax, doutMax);
        }
    }

    @Test(dataProvider = "doubleUnaryOpProvider")
    static void testCastFromDoubleMax(IntFunction<double[]> fa) {
        double[] dinMax = fa.apply(dspecMax.length());

        byte[] bout64 = new byte[bspec64.length()];
        byte[] bout128 = new byte[bspec128.length()];
        byte[] bout256 = new byte[bspec256.length()];
        byte[] bout512 = new byte[bspec512.length()];
        byte[] boutMax = new byte[bspecMax.length()];

        short[] sout64 = new short[sspec64.length()];
        short[] sout128 = new short[sspec128.length()];
        short[] sout256 = new short[sspec256.length()];
        short[] sout512 = new short[sspec512.length()];
        short[] soutMax = new short[sspecMax.length()];

        int[] iout64 = new int[ispec64.length()];
        int[] iout128 = new int[ispec128.length()];
        int[] iout256 = new int[ispec256.length()];
        int[] iout512 = new int[ispec512.length()];
        int[] ioutMax = new int[ispecMax.length()];

        long[] lout64 = new long[lspec64.length()];
        long[] lout128 = new long[lspec128.length()];
        long[] lout256 = new long[lspec256.length()];
        long[] lout512 = new long[lspec512.length()];
        long[] loutMax = new long[lspecMax.length()];

        float[] fout64 = new float[fspec64.length()];
        float[] fout128 = new float[fspec128.length()];
        float[] fout256 = new float[fspec256.length()];
        float[] fout512 = new float[fspec512.length()];
        float[] foutMax = new float[fspecMax.length()];

        double[] dout64 = new double[dspec64.length()];
        double[] dout128 = new double[dspec128.length()];
        double[] dout256 = new double[dspec256.length()];
        double[] dout512 = new double[dspec512.length()];
        double[] doutMax = new double[dspecMax.length()];

        for (int i = 0; i < NUM_ITER; i++) {
            testVectorCastDoubleMaxToByte(dspecMax, bspec64, dinMax, bout64);
            testVectorCastDoubleMaxToByte(dspecMax, bspec128, dinMax, bout128);
            testVectorCastDoubleMaxToByte(dspecMax, bspec256, dinMax, bout256);
            testVectorCastDoubleMaxToByte(dspecMax, bspec512, dinMax, bout512);
            testVectorCastDoubleMaxToByte(dspecMax, bspecMax, dinMax, boutMax);

            testVectorCastDoubleMaxToShort(dspecMax, sspec64, dinMax, sout64);
            testVectorCastDoubleMaxToShort(dspecMax, sspec128, dinMax, sout128);
            testVectorCastDoubleMaxToShort(dspecMax, sspec256, dinMax, sout256);
            testVectorCastDoubleMaxToShort(dspecMax, sspec512, dinMax, sout512);
            testVectorCastDoubleMaxToShort(dspecMax, sspecMax, dinMax, soutMax);

            testVectorCastDoubleMaxToInt(dspecMax, ispec64, dinMax, iout64);
            testVectorCastDoubleMaxToInt(dspecMax, ispec128, dinMax, iout128);
            testVectorCastDoubleMaxToInt(dspecMax, ispec256, dinMax, iout256);
            testVectorCastDoubleMaxToInt(dspecMax, ispec512, dinMax, iout512);
            testVectorCastDoubleMaxToInt(dspecMax, ispecMax, dinMax, ioutMax);

            testVectorCastDoubleMaxToLong(dspecMax, lspec64, dinMax, lout64);
            testVectorCastDoubleMaxToLong(dspecMax, lspec128, dinMax, lout128);
            testVectorCastDoubleMaxToLong(dspecMax, lspec256, dinMax, lout256);
            testVectorCastDoubleMaxToLong(dspecMax, lspec512, dinMax, lout512);
            testVectorCastDoubleMaxToLong(dspecMax, lspecMax, dinMax, loutMax);

            testVectorCastDoubleMaxToFloat(dspecMax, fspec64, dinMax, fout64);
            testVectorCastDoubleMaxToFloat(dspecMax, fspec128, dinMax, fout128);
            testVectorCastDoubleMaxToFloat(dspecMax, fspec256, dinMax, fout256);
            testVectorCastDoubleMaxToFloat(dspecMax, fspec512, dinMax, fout512);
            testVectorCastDoubleMaxToFloat(dspecMax, fspecMax, dinMax, foutMax);

            testVectorCastDoubleMaxToDouble(dspecMax, dspec64, dinMax, dout64);
            testVectorCastDoubleMaxToDouble(dspecMax, dspec128, dinMax, dout128);
            testVectorCastDoubleMaxToDouble(dspecMax, dspec256, dinMax, dout256);
            testVectorCastDoubleMaxToDouble(dspecMax, dspec512, dinMax, dout512);
            testVectorCastDoubleMaxToDouble(dspecMax, dspecMax, dinMax, doutMax);
        }
    }
}
