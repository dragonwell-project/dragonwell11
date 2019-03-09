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
 *   @run testng/othervm --add-opens jdk.panama.vector/jdk.panama.vector=ALL-UNNAMED VectorTrueTest
 *
 */
public class VectorTrueTest
{

    static Random random = new Random();
    static final IntVector.IntSpecies Species = IntVector.species(Vector.Shape.S_256_BIT);
    public static int size = 1024;
    public static int length = Species.length();
    public static int resultSize = size / length;
    static boolean[] anyResultV = new boolean[resultSize];
    static boolean[] allResultV = new boolean[resultSize];
    static boolean[] anyInput = new boolean[size];
    static boolean[] allInput = new boolean[size];
    static int t = 10;
    static int p = 10;
    public static void main(String[] args) throws  NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException, InstantiationException {
        long start0 = System.currentTimeMillis();
        long startv = System.currentTimeMillis();
        long normalTime = 0;
        long vecTime = 0;
        int i = 0;
        for (i = 0; i < resultSize; i++) {
            anyResultV[i] = true;
            allResultV[i] = true;
        }
        for (i = 0; i < size; i++) {
            anyInput[i] = false;
            allInput[i] = false;
        }
        for (i = 0; i < 20000; i++) {
            vecTest(Species);
        }
        for (i = 0; i < resultSize; i++) {
            anyResultV[i] = true;
            allResultV[i] = true;
        }
        vecTest(Species);
        for (i = 0; i < (resultSize - 1); i++) {
            if (anyResultV[i] != false) throw new RuntimeException("Wrong anyTrue result! Should be all false, index " + i);
            if (allResultV[i] != false) throw new RuntimeException("Wrong allTrue result! Should be all false, index " + i);
        }
    }
    static void vecTest(IntVector.IntSpecies Speciesint ) {
        IntVector v0;
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
}
