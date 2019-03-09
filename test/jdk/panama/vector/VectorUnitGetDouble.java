import jdk.incubator.vector.*;
import java.util.Arrays;
import java.util.Random;
import sun.misc.Unsafe;
import java.io.File;
import java.lang.reflect.Field;
import java.io.IOException;


public class VectorUnitGetDouble
{
    public static final int shift = 3;
    public static final int bytesPerDouble = 1 << shift;
    public static int rows = 1024 * 16;
    public static int cols = 32;
    static Random random = new Random();
    static final DoubleVector.DoubleSpecies species512 = DoubleVector.species(Vector.Shape.S_512_BIT);
    static DoubleVector l512;
    static boolean[] AisNull = new boolean[rows];
    public static void main(String[] args) throws  NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException, InstantiationException {
        Field f = Unsafe.class.getDeclaredField("theUnsafe"); //Doubleernal reference
        f.setAccessible(true);
        Unsafe unsafe = (Unsafe) f.get(null);
        byte[][] matrix = genMatrix();
        byte[] array = new byte[rows];
        random.nextBytes(array);
        int i = 0;
        double[] result;
        l512 = species512.fromByteArray(array, 0);
        for (i = 0; i < 20000; i++) {
            result = normalMatrixDot(matrix, array, cols, unsafe);
        }
        for (i = 0; i < 20000; i++) {
            result = matrixDot512(matrix, array, cols, species512, unsafe);
        }
        System.out.println("begin test");
        long start = System.currentTimeMillis();
        for (i = 0; i < 10000; i++) {
            result = normalMatrixDot(matrix, array, cols, unsafe);
        }
        System.out.println("normal  time used:" + (System.currentTimeMillis() - start));
        start  = System.currentTimeMillis();
        for (i = 0; i < 10000; i++) {
            result = matrixDot512(matrix, array, cols, species512, unsafe);
        }
        System.out.println("vec 512 time used:" + (System.currentTimeMillis() - start));
    }
    public static byte[][] genMatrix() {
        byte[][] res = new byte[cols][rows];
        for (int i = 0; i < cols; i++) {
            random.nextBytes(res[i]);
        }
        return res;
    }

    public static double[] matrixDot512(byte[][] source, byte[] target, int resLen, DoubleVector.DoubleSpecies species512, Unsafe unsafe) {
        double[] res = new double[resLen];
        for (int i = 0; i < resLen; i++) {
            res[i] = vectorDot512(source[i], target, species512, unsafe);
        }
        return res;
    }
    public static double[] normalMatrixDot(byte[][] source, byte[] target, int resLen, Unsafe unsafe) {
        double[] res = new double[resLen];
        for (int i = 0; i < resLen; i++) {
            res[i]  = normalVectorDot(source[i], target, unsafe);
        }
        return res;
    }
    public static double normalVectorDot(byte[] a, byte[] b, Unsafe unsafe) {
        double sum  = 0;
        double tmp = 0;
        double aDouble = 0;
        double bDouble = 0;
        double gg = 0;
        for (int i = 0; (i + bytesPerDouble) < a.length; i = i + bytesPerDouble) {
            if ((i % 4) == 3) {
                aDouble = unsafe.getDouble(a, (long) Unsafe.ARRAY_BYTE_BASE_OFFSET + 0);
                sum = aDouble;
            }
        }
        return sum;
    }
    public static double vectorDot512(byte[] a, byte[] b, DoubleVector.DoubleSpecies species512, Unsafe unsafe) {
        int i = 0;
        double sum = 0;
        for (i = 0; (i + (species512.length() << shift)) < a.length; i+=(species512.length() << shift)) {
            sum = l512.get(1);
        }
        return sum;
    }
}
