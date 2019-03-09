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

import java.util.concurrent.TimeUnit;
import java.util.function.IntFunction;

import org.openjdk.jmh.annotations.*;

@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Benchmark)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(value = 1, jvmArgsPrepend = {"--add-modules=jdk.panama.vector"})
public class IntScalar extends AbstractVectorBenchmark {
    @Param("1024")
    int size;

    int[] fill(IntFunction<Integer> f) {
        int[] array = new int[size];
        for (int i = 0; i < array.length; i++) {
            array[i] = f.apply(i);
        }
        return array;
    }

    int[] as, bs, cs, rs;
    boolean[] ms, rms;
    int[] ss;

    @Setup
    public void init() {
        as = fill(i -> (int)(2*i));
        bs = fill(i -> (int)(i+1));
        cs = fill(i -> (int)(i+5));
        rs = fill(i -> (int)0);
        ms = fillMask(size, i -> (i % 2) == 0);
        rms = fillMask(size, i -> false);

        ss = fillInt(size, i -> RANDOM.nextInt(Math.max(i,1)));
    }

    final IntFunction<int[]> fa = vl -> as;
    final IntFunction<int[]> fb = vl -> bs;
    final IntFunction<int[]> fc = vl -> cs;
    final IntFunction<int[]> fr = vl -> rs;
    final IntFunction<boolean[]> fm = vl -> ms;
    final IntFunction<boolean[]> fmr = vl -> rms;
    final IntFunction<int[]> fs = vl -> ss;


    @Benchmark
    public Object add() {
        int[] as = fa.apply(size);
        int[] bs = fb.apply(size);
        int[] rs = fr.apply(size);

        for (int i = 0; i < as.length; i++) {
            int a = as[i];
            int b = bs[i];
            rs[i] = (int)(a + b);
        }

        return rs;
    }

    @Benchmark
    public Object addMasked() {
        int[] as = fa.apply(size);
        int[] bs = fb.apply(size);
        int[] rs = fr.apply(size);
        boolean[] ms = fm.apply(size);

        for (int i = 0; i < as.length; i++) {
            int a = as[i];
            int b = bs[i];
            if (ms[i % ms.length]) {
              rs[i] = (int)(a + b);
            } else {
              rs[i] = a;
            }
        }
        return rs;
    }

    @Benchmark
    public Object sub() {
        int[] as = fa.apply(size);
        int[] bs = fb.apply(size);
        int[] rs = fr.apply(size);

        for (int i = 0; i < as.length; i++) {
            int a = as[i];
            int b = bs[i];
            rs[i] = (int)(a - b);
        }

        return rs;
    }

    @Benchmark
    public Object subMasked() {
        int[] as = fa.apply(size);
        int[] bs = fb.apply(size);
        int[] rs = fr.apply(size);
        boolean[] ms = fm.apply(size);

        for (int i = 0; i < as.length; i++) {
            int a = as[i];
            int b = bs[i];
            if (ms[i % ms.length]) {
              rs[i] = (int)(a - b);
            } else {
              rs[i] = a;
            }
        }
        return rs;
    }



    @Benchmark
    public Object mul() {
        int[] as = fa.apply(size);
        int[] bs = fb.apply(size);
        int[] rs = fr.apply(size);

        for (int i = 0; i < as.length; i++) {
            int a = as[i];
            int b = bs[i];
            rs[i] = (int)(a * b);
        }

        return rs;
    }

    @Benchmark
    public Object mulMasked() {
        int[] as = fa.apply(size);
        int[] bs = fb.apply(size);
        int[] rs = fr.apply(size);
        boolean[] ms = fm.apply(size);

        for (int i = 0; i < as.length; i++) {
            int a = as[i];
            int b = bs[i];
            if (ms[i % ms.length]) {
              rs[i] = (int)(a * b);
            } else {
              rs[i] = a;
            }
        }
        return rs;
    }


    @Benchmark
    public Object and() {
        int[] as = fa.apply(size);
        int[] bs = fb.apply(size);
        int[] rs = fr.apply(size);

        for (int i = 0; i < as.length; i++) {
            int a = as[i];
            int b = bs[i];
            rs[i] = (int)(a & b);
        }

        return rs;
    }



    @Benchmark
    public Object andMasked() {
        int[] as = fa.apply(size);
        int[] bs = fb.apply(size);
        int[] rs = fr.apply(size);
        boolean[] ms = fm.apply(size);

        for (int i = 0; i < as.length; i++) {
            int a = as[i];
            int b = bs[i];
            if (ms[i % ms.length]) {
              rs[i] = (int)(a & b);
            } else {
              rs[i] = a;
            }
        }
        return rs;
    }



    @Benchmark
    public Object or() {
        int[] as = fa.apply(size);
        int[] bs = fb.apply(size);
        int[] rs = fr.apply(size);

        for (int i = 0; i < as.length; i++) {
            int a = as[i];
            int b = bs[i];
            rs[i] = (int)(a | b);
        }

        return rs;
    }



    @Benchmark
    public Object orMasked() {
        int[] as = fa.apply(size);
        int[] bs = fb.apply(size);
        int[] rs = fr.apply(size);
        boolean[] ms = fm.apply(size);

        for (int i = 0; i < as.length; i++) {
            int a = as[i];
            int b = bs[i];
            if (ms[i % ms.length]) {
              rs[i] = (int)(a | b);
            } else {
              rs[i] = a;
            }
        }
        return rs;
    }



    @Benchmark
    public Object xor() {
        int[] as = fa.apply(size);
        int[] bs = fb.apply(size);
        int[] rs = fr.apply(size);

        for (int i = 0; i < as.length; i++) {
            int a = as[i];
            int b = bs[i];
            rs[i] = (int)(a ^ b);
        }

        return rs;
    }



    @Benchmark
    public Object xorMasked() {
        int[] as = fa.apply(size);
        int[] bs = fb.apply(size);
        int[] rs = fr.apply(size);
        boolean[] ms = fm.apply(size);

        for (int i = 0; i < as.length; i++) {
            int a = as[i];
            int b = bs[i];
            if (ms[i % ms.length]) {
              rs[i] = (int)(a ^ b);
            } else {
              rs[i] = a;
            }
        }
        return rs;
    }



    @Benchmark
    public Object shiftR() {
        int[] as = fa.apply(size);
        int[] bs = fb.apply(size);
        int[] rs = fr.apply(size);

        for (int i = 0; i < as.length; i++) {
            int a = as[i];
            int b = bs[i];
            rs[i] = (int)((a >>> b));
        }

        return rs;
    }



    @Benchmark
    public Object shiftRMasked() {
        int[] as = fa.apply(size);
        int[] bs = fb.apply(size);
        int[] rs = fr.apply(size);
        boolean[] ms = fm.apply(size);

        for (int i = 0; i < as.length; i++) {
            int a = as[i];
            int b = bs[i];
            if (ms[i % ms.length]) {
              rs[i] = (int)((a >>> b));
            } else {
              rs[i] = a;
            }
        }
        return rs;
    }



    @Benchmark
    public Object shiftL() {
        int[] as = fa.apply(size);
        int[] bs = fb.apply(size);
        int[] rs = fr.apply(size);

        for (int i = 0; i < as.length; i++) {
            int a = as[i];
            int b = bs[i];
            rs[i] = (int)((a << b));
        }

        return rs;
    }



    @Benchmark
    public Object shiftLMasked() {
        int[] as = fa.apply(size);
        int[] bs = fb.apply(size);
        int[] rs = fr.apply(size);
        boolean[] ms = fm.apply(size);

        for (int i = 0; i < as.length; i++) {
            int a = as[i];
            int b = bs[i];
            if (ms[i % ms.length]) {
              rs[i] = (int)((a << b));
            } else {
              rs[i] = a;
            }
        }
        return rs;
    }



    @Benchmark
    public Object aShiftR() {
        int[] as = fa.apply(size);
        int[] bs = fb.apply(size);
        int[] rs = fr.apply(size);

        for (int i = 0; i < as.length; i++) {
            int a = as[i];
            int b = bs[i];
            rs[i] = (int)((a >> b));
        }

        return rs;
    }



    @Benchmark
    public Object aShiftRMasked() {
        int[] as = fa.apply(size);
        int[] bs = fb.apply(size);
        int[] rs = fr.apply(size);
        boolean[] ms = fm.apply(size);

        for (int i = 0; i < as.length; i++) {
            int a = as[i];
            int b = bs[i];
            if (ms[i % ms.length]) {
              rs[i] = (int)((a >> b));
            } else {
              rs[i] = a;
            }
        }
        return rs;
    }



    @Benchmark
    public Object aShiftRShift() {
        int[] as = fa.apply(size);
        int[] bs = fb.apply(size);
        int[] rs = fr.apply(size);

        for (int i = 0; i < as.length; i++) {
            int a = as[i];
            int b = bs[i];
            rs[i] = (int)((a >> b));
        }

        return rs;
    }



    @Benchmark
    public Object aShiftRMaskedShift() {
        int[] as = fa.apply(size);
        int[] bs = fb.apply(size);
        int[] rs = fr.apply(size);
        boolean[] ms = fm.apply(size);

        for (int i = 0; i < as.length; i++) {
            int a = as[i];
            int b = bs[i];
            boolean m = ms[i % ms.length];
            rs[i] = (m ? (int)((a >> b)) : a);
        }

        return rs;
    }



    @Benchmark
    public Object shiftRShift() {
        int[] as = fa.apply(size);
        int[] bs = fb.apply(size);
        int[] rs = fr.apply(size);

        for (int i = 0; i < as.length; i++) {
            int a = as[i];
            int b = bs[i];
            rs[i] = (int)((a >>> b));
        }

        return rs;
    }



    @Benchmark
    public Object shiftRMaskedShift() {
        int[] as = fa.apply(size);
        int[] bs = fb.apply(size);
        int[] rs = fr.apply(size);
        boolean[] ms = fm.apply(size);

        for (int i = 0; i < as.length; i++) {
            int a = as[i];
            int b = bs[i];
            boolean m = ms[i % ms.length];
            rs[i] = (m ? (int)((a >>> b)) : a);
        }

        return rs;
    }



    @Benchmark
    public Object shiftLShift() {
        int[] as = fa.apply(size);
        int[] bs = fb.apply(size);
        int[] rs = fr.apply(size);

        for (int i = 0; i < as.length; i++) {
            int a = as[i];
            int b = bs[i];
            rs[i] = (int)((a << b));
        }

        return rs;
    }



    @Benchmark
    public Object shiftLMaskedShift() {
        int[] as = fa.apply(size);
        int[] bs = fb.apply(size);
        int[] rs = fr.apply(size);
        boolean[] ms = fm.apply(size);

        for (int i = 0; i < as.length; i++) {
            int a = as[i];
            int b = bs[i];
            boolean m = ms[i % ms.length];
            rs[i] = (m ? (int)((a << b)) : a);
        }

        return rs;
    }


    @Benchmark
    public Object max() {
        int[] as = fa.apply(size);
        int[] bs = fb.apply(size);
        int[] rs = fr.apply(size);

        for (int i = 0; i < as.length; i++) {
            int a = as[i];
            int b = bs[i];
            rs[i] = (int)((a > b) ? a : b);
        }

        return rs;
    }

    @Benchmark
    public Object min() {
        int[] as = fa.apply(size);
        int[] bs = fb.apply(size);
        int[] rs = fr.apply(size);

        for (int i = 0; i < as.length; i++) {
            int a = as[i];
            int b = bs[i];
            rs[i] = (int)((a < b) ? a : b);
        }

        return rs;
    }


    @Benchmark
    public int andAll() {
        int[] as = fa.apply(size);
        int r = -1;
        for (int i = 0; i < as.length; i++) {
          r &= as[i];
        }
        return r;
    }



    @Benchmark
    public int orAll() {
        int[] as = fa.apply(size);
        int r = 0;
        for (int i = 0; i < as.length; i++) {
          r |= as[i];
        }
        return r;
    }



    @Benchmark
    public int xorAll() {
        int[] as = fa.apply(size);
        int r = 0;
        for (int i = 0; i < as.length; i++) {
          r ^= as[i];
        }
        return r;
    }


    @Benchmark
    public int addAll() {
        int[] as = fa.apply(size);
        int r = 0;
        for (int i = 0; i < as.length; i++) {
          r += as[i];
        }
        return r;
    }

    @Benchmark
    public int subAll() {
        int[] as = fa.apply(size);
        int r = 0;
        for (int i = 0; i < as.length; i++) {
          r -= as[i];
        }
        return r;
    }

    @Benchmark
    public int mulAll() {
        int[] as = fa.apply(size);
        int r = 1;
        for (int i = 0; i < as.length; i++) {
          r *= as[i];
        }
        return r;
    }

    @Benchmark
    public int minAll() {
        int[] as = fa.apply(size);
        int r = Integer.MAX_VALUE;
        for (int i = 0; i < as.length; i++) {
          r = as[i];
        }
        return r;
    }

    @Benchmark
    public int maxAll() {
        int[] as = fa.apply(size);
        int r = Integer.MIN_VALUE;
        for (int i = 0; i < as.length; i++) {
          r = as[i];
        }
        return r;
    }


    @Benchmark
    public boolean anyTrue() {
        boolean[] ms = fm.apply(size);
        boolean r = false;
        for (int i = 0; i < ms.length; i++) {
          r |= ms[i];
        }
        return r;
    }



    @Benchmark
    public boolean allTrue() {
        boolean[] ms = fm.apply(size);
        boolean r = true;
        for (int i = 0; i < ms.length; i++) {
          r &= ms[i];
        }
        return r;
    }


    @Benchmark
    public boolean lessThan() {
        int[] as = fa.apply(size);
        int[] bs = fb.apply(size);

        boolean r = false;
        for (int i = 0; i < as.length; i++) {
            boolean m = (as[i] < bs[i]);
            r |= m; // accumulate so JIT can't eliminate the computation
        }

        return r;
    }

    @Benchmark
    public boolean greaterThan() {
        int[] as = fa.apply(size);
        int[] bs = fb.apply(size);

        boolean r = false;
        for (int i = 0; i < as.length; i++) {
            boolean m = (as[i] > bs[i]);
            r |= m; // accumulate so JIT can't eliminate the computation
        }

        return r;
    }

    @Benchmark
    public boolean equal() {
        int[] as = fa.apply(size);
        int[] bs = fb.apply(size);

        boolean r = false;
        for (int i = 0; i < as.length; i++) {
            boolean m = (as[i] == bs[i]);
            r |= m; // accumulate so JIT can't eliminate the computation
        }

        return r;
    }

    @Benchmark
    public boolean notEqual() {
        int[] as = fa.apply(size);
        int[] bs = fb.apply(size);

        boolean r = false;
        for (int i = 0; i < as.length; i++) {
            boolean m = (as[i] != bs[i]);
            r |= m; // accumulate so JIT can't eliminate the computation
        }

        return r;
    }

    @Benchmark
    public boolean lessThanEq() {
        int[] as = fa.apply(size);
        int[] bs = fb.apply(size);

        boolean r = false;
        for (int i = 0; i < as.length; i++) {
            boolean m = (as[i] <= bs[i]);
            r |= m; // accumulate so JIT can't eliminate the computation
        }

        return r;
    }

    @Benchmark
    public boolean greaterThanEq() {
        int[] as = fa.apply(size);
        int[] bs = fb.apply(size);

        boolean r = false;
        for (int i = 0; i < as.length; i++) {
            boolean m = (as[i] >= bs[i]);
            r |= m; // accumulate so JIT can't eliminate the computation
        }

        return r;
    }

    @Benchmark
    public Object blend() {
        int[] as = fa.apply(size);
        int[] bs = fb.apply(size);
        int[] rs = fr.apply(size);
        boolean[] ms = fm.apply(size);

        for (int i = 0; i < as.length; i++) {
            int a = as[i];
            int b = bs[i];
            boolean m = ms[i % ms.length];
            rs[i] = (m ? b : a);
        }

        return rs;
    }
    Object rearrangeShared(int window) {
        int[] as = fa.apply(size);
        int[] order = fs.apply(size);
        int[] rs = fr.apply(size);

        for (int i = 0; i < as.length; i += window) {
            for (int j = 0; j < window; j++) {
                int a = as[i+j];
                int pos = order[j];
                rs[i + pos] = a;
            }
        }

        return rs;
    }

    @Benchmark
    public Object rearrange064() {
        int window = 64 / Integer.SIZE;
        return rearrangeShared(window);
    }

    @Benchmark
    public Object rearrange128() {
        int window = 128 / Integer.SIZE;
        return rearrangeShared(window);
    }

    @Benchmark
    public Object rearrange256() {
        int window = 256 / Integer.SIZE;
        return rearrangeShared(window);
    }

    @Benchmark
    public Object rearrange512() {
        int window = 512 / Integer.SIZE;
        return rearrangeShared(window);
    }





















    @Benchmark
    public Object neg() {
        int[] as = fa.apply(size);
        int[] rs = fr.apply(size);

        for (int i = 0; i < as.length; i++) {
            int a = as[i];
            rs[i] = (int)(-((int)a));
        }

        return rs;
    }

    @Benchmark
    public Object negMasked() {
        int[] as = fa.apply(size);
        int[] rs = fr.apply(size);
        boolean[] ms = fm.apply(size);

        for (int i = 0; i < as.length; i++) {
            int a = as[i];
            boolean m = ms[i % ms.length];
            rs[i] = (m ? (int)(-((int)a)) : a);
        }

        return rs;
    }

    @Benchmark
    public Object abs() {
        int[] as = fa.apply(size);
        int[] rs = fr.apply(size);

        for (int i = 0; i < as.length; i++) {
            int a = as[i];
            rs[i] = (int)(Math.abs((int)a));
        }

        return rs;
    }

    @Benchmark
    public Object absMasked() {
        int[] as = fa.apply(size);
        int[] rs = fr.apply(size);
        boolean[] ms = fm.apply(size);

        for (int i = 0; i < as.length; i++) {
            int a = as[i];
            boolean m = ms[i % ms.length];
            rs[i] = (m ? (int)(Math.abs((int)a)) : a);
        }

        return rs;
    }


    @Benchmark
    public Object not() {
        int[] as = fa.apply(size);
        int[] rs = fr.apply(size);

        for (int i = 0; i < as.length; i++) {
            int a = as[i];
            rs[i] = (int)(~((int)a));
        }

        return rs;
    }



    @Benchmark
    public Object notMasked() {
        int[] as = fa.apply(size);
        int[] rs = fr.apply(size);
        boolean[] ms = fm.apply(size);

        for (int i = 0; i < as.length; i++) {
            int a = as[i];
            boolean m = ms[i % ms.length];
            rs[i] = (m ? (int)(~((int)a)) : a);
        }

        return rs;
    }




    @Benchmark
    public Object gatherBase0() {
        int[] as = fa.apply(size);
        int[] is    = fs.apply(size);
        int[] rs = fr.apply(size);

        for (int i = 0; i < as.length; i++) {
            int ix = 0 + is[i];
            rs[i] = as[ix];
        }

        return rs;
    }


    Object gather(int window) {
        int[] as = fa.apply(size);
        int[] is    = fs.apply(size);
        int[] rs = fr.apply(size);

        for (int i = 0; i < as.length; i += window) {
            for (int j = 0; j < window; j++) {
                int ix = i + is[i + j];
                rs[i + j] = as[ix];
            }
        }

        return rs;
    }

    @Benchmark
    public Object gather064() {
        int window = 64 / Integer.SIZE;
        return gather(window);
    }

    @Benchmark
    public Object gather128() {
        int window = 128 / Integer.SIZE;
        return gather(window);
    }

    @Benchmark
    public Object gather256() {
        int window = 256 / Integer.SIZE;
        return gather(window);
    }

    @Benchmark
    public Object gather512() {
        int window = 512 / Integer.SIZE;
        return gather(window);
    }



    @Benchmark
    public Object scatterBase0() {
        int[] as = fa.apply(size);
        int[] is    = fs.apply(size);
        int[] rs = fr.apply(size);

        for (int i = 0; i < as.length; i++) {
            int ix = 0 + is[i];
            rs[ix] = as[i];
        }

        return rs;
    }

    Object scatter(int window) {
        int[] as = fa.apply(size);
        int[] is    = fs.apply(size);
        int[] rs = fr.apply(size);

        for (int i = 0; i < as.length; i += window) {
            for (int j = 0; j < window; j++) {
                int ix = i + is[i + j];
                rs[ix] = as[i + j];
            }
        }

        return rs;
    }

    @Benchmark
    public Object scatter064() {
        int window = 64 / Integer.SIZE;
        return scatter(window);
    }

    @Benchmark
    public Object scatter128() {
        int window = 128 / Integer.SIZE;
        return scatter(window);
    }

    @Benchmark
    public Object scatter256() {
        int window = 256 / Integer.SIZE;
        return scatter(window);
    }

    @Benchmark
    public Object scatter512() {
        int window = 512 / Integer.SIZE;
        return scatter(window);
    }

}

