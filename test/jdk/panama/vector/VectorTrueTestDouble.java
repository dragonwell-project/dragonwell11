import jdk.panama.vector.*;
import java.util.Arrays;
import java.util.Random;
import java.lang.reflect.Field;
import java.io.IOException;
import jdk.panama.vector.Vector.Mask;
import jdk.panama.vector.Vector.Shape;
/*
 *  @test
 *  @modules jdk.panama.vector
 *   @run testng/othervm  -XX:+UseVectorAPI -Djdk.panama.vector.VECTOR_ACCESS_OOB_CHECK=0 --add-opens jdk.panama.vector/jdk.panama.vector=ALL-UNNAMED VectorTrueTestDouble
 *
 */
public class VectorTrueTestDouble
{

    static Random random = new Random();
    static final DoubleVector.DoubleSpecies Species = DoubleVector.species(Vector.Shape.S_512_BIT);
    public static int size = 1024 * 8;
    public static int length = Species.length();
    public static int resultSize = size / length;
    static boolean[] anyResultV = new boolean[resultSize];
    static boolean[] allResultV = new boolean[resultSize];
    static boolean[] anyResult = new boolean[resultSize];
    static boolean[] allResult = new boolean[resultSize];
    static boolean[] anyInput = new boolean[size];
    static boolean[] allInput = new boolean[size];
    public static void main(String[] args) throws  NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException, InstantiationException {
        long start0 = System.currentTimeMillis();
        long startv = System.currentTimeMillis();
        long normalTime = 0;
        long vecTime = 0;
        int i = 0;
        for (i = 0; i < resultSize; i++) {
            anyResultV[i] = false;
            allResultV[i] = true;
        }
        for (i = 0; i < size; i++) {
            anyInput[i] = random.nextInt(20) > 15 ? true : false;
            allInput[i] = random.nextInt(20) < 3 ? false : true;
        }
        for (i = 0; i < resultSize; i++) {
            anyResultV[i] = false;
            allResultV[i] = false;
        }
        for (i = 0; i < 20000; i++) {
            normalTest();
            vecTestSpeed(Species);
            vecTestCorrect(Species);
        }

        start0 = System.currentTimeMillis();
        for (i = 0; i < 20000; i++) {
            normalTest();
        }
        long  norTime = System.currentTimeMillis() - start0;
        System.out.println("normal    time used:" + norTime);
        startv = System.currentTimeMillis();
        for (i = 0; i < 20000; i++) {
            vecTestSpeed(Species);
        }
        vecTime = System.currentTimeMillis() - startv;
        System.out.println("vector 512 time used:" + vecTime);
        vecTestCorrect(Species);
        for (i = 0; i < (resultSize - 1); i++) {
            if (anyResultV[i] != anyResult[i]) {
                System.out.println("Wrong anyTrue result. normal " + anyResult[i] + " vec " + anyResultV[i]);
                for (int j = 0; j < Species.length(); j++) {
                    System.out.println(anyInput[(i * Species.length()) + j]);
                }
                throw new RuntimeException("Wrong result!");
            }
        }
        for (i = 0; i < (resultSize - 1); i++) {
            if (allResultV[i] != allResult[i]) {
                System.out.println("Wrong allTrue result. index " + i + " normal " + allResult[i] + " vec " + allResultV[i]);
                for (int j = 0; j < Species.length(); j++) {
                    System.out.println(allInput[(i * Species.length()) + j]);
                }
                throw new RuntimeException("Wrong result!");
            }
        }
    }
    static void vecTestSpeed(DoubleVector.DoubleSpecies Speciesint) {
        DoubleVector v0;
        int i = 0;
        int j = 0;
        Mask maskAny = Speciesint.maskFromArray(anyInput, i);
        Mask maskAll = Speciesint.maskFromArray(allInput, i);
        for (i = 0; i + (Speciesint.length()) <= size; i += Speciesint.length()) {
            allResultV[j] = maskAll.allTrue();
            anyResultV[j] = maskAny.anyTrue();
            j++;
        }
        return;
    }
    static void vecTestCorrect(DoubleVector.DoubleSpecies Speciesint) {
        DoubleVector v0;
        int i = 0;
        int j = 0;
        Mask maskAny = Speciesint.maskFromArray(anyInput, i);
        Mask maskAll = Speciesint.maskFromArray(allInput, i);
        for (i = 0; i + (Speciesint.length()) <= size; i += Speciesint.length()) {
            maskAny = Speciesint.maskFromArray(anyInput, i);
            maskAll = Speciesint.maskFromArray(allInput, i);
            allResultV[j] = maskAll.allTrue();
            anyResultV[j] = maskAny.anyTrue();
            j++;
        }
        return;
    }
    static void normalTest() {
        int i = 0;
        int j = 0;
        for (i = 0; i + (Species.length()) <= size; i += Species.length()) {
            anyResult[j] = false;
            allResult[j] = true;
            for (int k = 0; k < Species.length(); k++) {
                if (anyInput[j*Species.length() + k] == true) {
                    anyResult[j] = true;
                }
                if (allInput[j*Species.length() + k] == false) {
                    allResult[j] = false;
                }
            }
            j++;
        }
        return;
    }
}
