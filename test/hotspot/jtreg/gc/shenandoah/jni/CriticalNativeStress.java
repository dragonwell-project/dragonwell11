/*
 * Copyright (c) 2018, 2022, Red Hat, Inc. and/or its affiliates.
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
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 *
 */

package gc.shenandoah.jni;

import java.util.Random;
import jdk.test.lib.Utils;

/* @test
 * @key randomness
 * @library /
 *          /test/lib
 *
 * @requires (os.arch =="x86_64" | os.arch == "amd64" | os.arch=="x86" | os.arch=="i386") & !vm.graal.enabled & vm.gc.Shenandoah
 *
 * @run main/othervm/native -XX:+UnlockDiagnosticVMOptions -XX:+UnlockExperimentalVMOptions -XX:+UseShenandoahGC -XX:ShenandoahGCMode=passive    -XX:+ShenandoahDegeneratedGC -Xcomp -Xmx512M -XX:+CriticalJNINatives gc.shenandoah.jni.CriticalNativeStress
 * @run main/othervm/native -XX:+UnlockDiagnosticVMOptions -XX:+UnlockExperimentalVMOptions -XX:+UseShenandoahGC -XX:ShenandoahGCMode=passive    -XX:-ShenandoahDegeneratedGC -Xcomp -Xmx512M -XX:+CriticalJNINatives gc.shenandoah.jni.CriticalNativeStress
 *
 * @run main/othervm/native -XX:+UnlockDiagnosticVMOptions -XX:+UnlockExperimentalVMOptions -XX:+UseShenandoahGC -XX:ShenandoahGCHeuristics=aggressive -Xcomp -Xmx512M -XX:+CriticalJNINatives gc.shenandoah.jni.CriticalNativeStress
 *
 * @run main/othervm/native -XX:+UnlockDiagnosticVMOptions -XX:+UnlockExperimentalVMOptions -XX:+UseShenandoahGC                                       -Xcomp -Xmx256M -XX:+CriticalJNINatives gc.shenandoah.jni.CriticalNativeStress
 * @run main/othervm/native -XX:+UnlockDiagnosticVMOptions -XX:+UnlockExperimentalVMOptions -XX:+UseShenandoahGC -XX:ShenandoahGCMode=iu        -Xcomp -Xmx512M -XX:+CriticalJNINatives gc.shenandoah.jni.CriticalNativeStress
 * @run main/othervm/native -XX:+UnlockDiagnosticVMOptions -XX:+UnlockExperimentalVMOptions -XX:+UseShenandoahGC -XX:ShenandoahGCMode=iu -XX:ShenandoahGCHeuristics=aggressive -Xcomp -Xmx512M -XX:+CriticalJNINatives gc.shenandoah.jni.CriticalNativeStress
 */
public class CriticalNativeStress {
    static final int CYCLES = 50;
    static final int THREAD_PER_CASE = 1;

    static long sum(long[] a) {
        long sum = 0;
        for (int index = 0; index < a.length; index++) {
            sum += a[index];
        }
        return sum;
    }

    static long sum(int[] a) {
        long sum = 0;
        for (int index = 0; index < a.length; index++) {
            sum += a[index];
        }
        return sum;
    }

    private static volatile String garbage_array[];

    static void create_garbage(int len) {
        len = Math.max(len, 1024);
        String array[] = new String[len];
        for (int index = 0; index < len; index++) {
            array[index] = "String " + index;
        }
        garbage_array = array;
    }

    static void run_test_case1(Random rand) {
        int length = rand.nextInt(50) + 1;
        long[] arr = new long[length];
        for (int index = 0; index < length; index++) {
            arr[index] = rand.nextLong() % 10002;
        }

        for (int index = 0; index < length; index++) {
            create_garbage(index);
        }

        long native_sum = CriticalNative.sum1(arr);
        long java_sum = sum(arr);
        if (native_sum != java_sum) {
            StringBuffer sb = new StringBuffer("Sums do not match: native = ")
                    .append(native_sum).append(" java = ").append(java_sum);

            throw new RuntimeException(sb.toString());
        }
    }

    static void run_test_case2(Random rand) {
        int index;
        long a1 = rand.nextLong() % 10245;

        int a2_length = rand.nextInt(50) + 1;
        int[] a2 = new int[a2_length];
        for (index = 0; index < a2_length; index++) {
            a2[index] = rand.nextInt(106);
        }

        int a3_length = rand.nextInt(150) + 1;
        int[] a3 = new int[a3_length];
        for (index = 0; index < a3_length; index++) {
            a3[index] = rand.nextInt(3333);
        }

        int a4_length = rand.nextInt(200) + 1;
        long[] a4 = new long[a4_length];
        for (index = 0; index < a4_length; index++) {
            a4[index] = rand.nextLong() % 12322;
        }

        int a5_length = rand.nextInt(350) + 1;
        int[] a5 = new int[a5_length];
        for (index = 0; index < a5_length; index++) {
            a5[index] = rand.nextInt(3333);
        }

        for (index = 0; index < a1; index++) {
            create_garbage(index);
        }

        long native_sum = CriticalNative.sum2(a1, a2, a3, a4, a5);
        long java_sum = a1 + sum(a2) + sum(a3) + sum(a4) + sum(a5);
        if (native_sum != java_sum) {
            StringBuffer sb = new StringBuffer("Sums do not match: native = ")
                    .append(native_sum).append(" java = ").append(java_sum);

            throw new RuntimeException(sb.toString());
        }
    }

    static class Case1Runner extends Thread {
        private final Random rand;
        public Case1Runner() {
            rand = new Random(Utils.getRandomInstance().nextLong());
            start();
        }

        public void run() {
            for (int index = 0; index < CYCLES; index++) {
                run_test_case1(rand);
            }
        }
    }

    static class Case2Runner extends Thread {
        private final Random rand;
        public Case2Runner() {
            rand = new Random(Utils.getRandomInstance().nextLong());
            start();
        }

        public void run() {
            for (int index = 0; index < CYCLES; index++) {
                run_test_case2(rand);
            }
        }
    }

    public static void main(String[] args) {
        Thread[] thrs = new Thread[THREAD_PER_CASE * 2];
        for (int index = 0; index < thrs.length; index = index + 2) {
            thrs[index] = new Case1Runner();
            thrs[index + 1] = new Case2Runner();
        }

        for (int index = 0; index < thrs.length; index++) {
            try {
                thrs[index].join();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
