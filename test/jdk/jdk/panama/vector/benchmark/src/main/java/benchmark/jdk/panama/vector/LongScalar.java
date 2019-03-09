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
public class LongScalar extends AbstractVectorBenchmark {
    @Param("1024")
    int size;

    long[] fill(IntFunction<Long> f) {
        long[] array = new long[size];
        for (int i = 0; i < array.length; i++) {
            array[i] = f.apply(i);
        }
        return array;
    }

    long[] as, bs, cs, rs;
    boolean[] ms, rms;
    int[] ss;

    @Setup
    public void init() {
        as = fill(i -> (long)(2*i));
        bs = fill(i -> (long)(i+1));
        cs = fill(i -> (long)(i+5));
        rs = fill(i -> (long)0);
        ms = fillMask(size, i -> (i % 2) == 0);
        rms = fillMask(size, i -> false);

        ss = fillInt(size, i -> RANDOM.nextInt(Math.max(i,1)));
    }

    final IntFunction<long[]> fa = vl -> as;
    final IntFunction<long[]> fb = vl -> bs;
    final IntFunction<long[]> fc = vl -> cs;
    final IntFunction<long[]> fr = vl -> rs;
    final IntFunction<boolean[]> fm = vl -> ms;
    final IntFunction<boolean[]> fmr = vl -> rms;
    final IntFunction<int[]> fs = vl -> ss;


    @Benchmark
    public Object add() {
        long[] as = fa.apply(size);
        long[] bs = fb.apply(size);
        long[] rs = fr.apply(size);

        for (int i = 0; i < as.length; i++) {
            long a = as[i];
            long b = bs[i];
            rs[i] = (long)(a + b);
        }

        return rs;
    }

    @Benchmark
    public Object addMasked() {
        long[] as = fa.apply(size);
        long[] bs = fb.apply(size);
        long[] rs = fr.apply(size);
        boolean[] ms = fm.apply(size);

        for (int i = 0; i < as.length; i++) {
            long a = as[i];
            long b = bs[i];
            if (ms[i % ms.length]) {
              rs[i] = (long)(a + b);
            } else {
              rs[i] = a;
            }
        }
        return rs;
    }

    @Benchmark
    public Object sub() {
        long[] as = fa.apply(size);
        long[] bs = fb.apply(size);
        long[] rs = fr.apply(size);

        for (int i = 0; i < as.length; i++) {
            long a = as[i];
            long b = bs[i];
            rs[i] = (long)(a - b);
        }

        return rs;
    }

    @Benchmark
    public Object subMasked() {
        long[] as = fa.apply(size);
        long[] bs = fb.apply(size);
        long[] rs = fr.apply(size);
        boolean[] ms = fm.apply(size);

        for (int i = 0; i < as.length; i++) {
            long a = as[i];
            long b = bs[i];
            if (ms[i % ms.length]) {
              rs[i] = (long)(a - b);
            } else {
              rs[i] = a;
            }
        }
        return rs;
    }



    @Benchmark
    public Object mul() {
        long[] as = fa.apply(size);
        long[] bs = fb.apply(size);
        long[] rs = fr.apply(size);

        for (int i = 0; i < as.length; i++) {
            long a = as[i];
            long b = bs[i];
            rs[i] = (long)(a * b);
        }

        return rs;
    }

    @Benchmark
    public Object mulMasked() {
        long[] as = fa.apply(size);
        long[] bs = fb.apply(size);
        long[] rs = fr.apply(size);
        boolean[] ms = fm.apply(size);

        for (int i = 0; i < as.length; i++) {
            long a = as[i];
            long b = bs[i];
            if (ms[i % ms.length]) {
              rs[i] = (long)(a * b);
            } else {
              rs[i] = a;
            }
        }
        return rs;
    }


    @Benchmark
    public Object and() {
        long[] as = fa.apply(size);
        long[] bs = fb.apply(size);
        long[] rs = fr.apply(size);

        for (int i = 0; i < as.length; i++) {
            long a = as[i];
            long b = bs[i];
            rs[i] = (long)(a & b);
        }

        return rs;
    }



    @Benchmark
    public Object andMasked() {
        long[] as = fa.apply(size);
        long[] bs = fb.apply(size);
        long[] rs = fr.apply(size);
        boolean[] ms = fm.apply(size);

        for (int i = 0; i < as.length; i++) {
            long a = as[i];
            long b = bs[i];
            if (ms[i % ms.length]) {
              rs[i] = (long)(a & b);
            } else {
              rs[i] = a;
            }
        }
        return rs;
    }



    @Benchmark
    public Object or() {
        long[] as = fa.apply(size);
        long[] bs = fb.apply(size);
        long[] rs = fr.apply(size);

        for (int i = 0; i < as.length; i++) {
            long a = as[i];
            long b = bs[i];
            rs[i] = (long)(a | b);
        }

        return rs;
    }



    @Benchmark
    public Object orMasked() {
        long[] as = fa.apply(size);
        long[] bs = fb.apply(size);
        long[] rs = fr.apply(size);
        boolean[] ms = fm.apply(size);

        for (int i = 0; i < as.length; i++) {
            long a = as[i];
            long b = bs[i];
            if (ms[i % ms.length]) {
              rs[i] = (long)(a | b);
            } else {
              rs[i] = a;
            }
        }
        return rs;
    }



    @Benchmark
    public Object xor() {
        long[] as = fa.apply(size);
        long[] bs = fb.apply(size);
        long[] rs = fr.apply(size);

        for (int i = 0; i < as.length; i++) {
            long a = as[i];
            long b = bs[i];
            rs[i] = (long)(a ^ b);
        }

        return rs;
    }



    @Benchmark
    public Object xorMasked() {
        long[] as = fa.apply(size);
        long[] bs = fb.apply(size);
        long[] rs = fr.apply(size);
        boolean[] ms = fm.apply(size);

        for (int i = 0; i < as.length; i++) {
            long a = as[i];
            long b = bs[i];
            if (ms[i % ms.length]) {
              rs[i] = (long)(a ^ b);
            } else {
              rs[i] = a;
            }
        }
        return rs;
    }



    @Benchmark
    public Object shiftR() {
        long[] as = fa.apply(size);
        long[] bs = fb.apply(size);
        long[] rs = fr.apply(size);

        for (int i = 0; i < as.length; i++) {
            long a = as[i];
            long b = bs[i];
            rs[i] = (long)((a >>> b));
        }

        return rs;
    }



    @Benchmark
    public Object shiftRMasked() {
        long[] as = fa.apply(size);
        long[] bs = fb.apply(size);
        long[] rs = fr.apply(size);
        boolean[] ms = fm.apply(size);

        for (int i = 0; i < as.length; i++) {
            long a = as[i];
            long b = bs[i];
            if (ms[i % ms.length]) {
              rs[i] = (long)((a >>> b));
            } else {
              rs[i] = a;
            }
        }
        return rs;
    }



    @Benchmark
    public Object shiftL() {
        long[] as = fa.apply(size);
        long[] bs = fb.apply(size);
        long[] rs = fr.apply(size);

        for (int i = 0; i < as.length; i++) {
            long a = as[i];
            long b = bs[i];
            rs[i] = (long)((a << b));
        }

        return rs;
    }



    @Benchmark
    public Object shiftLMasked() {
        long[] as = fa.apply(size);
        long[] bs = fb.apply(size);
        long[] rs = fr.apply(size);
        boolean[] ms = fm.apply(size);

        for (int i = 0; i < as.length; i++) {
            long a = as[i];
            long b = bs[i];
            if (ms[i % ms.length]) {
              rs[i] = (long)((a << b));
            } else {
              rs[i] = a;
            }
        }
        return rs;
    }



    @Benchmark
    public Object aShiftR() {
        long[] as = fa.apply(size);
        long[] bs = fb.apply(size);
        long[] rs = fr.apply(size);

        for (int i = 0; i < as.length; i++) {
            long a = as[i];
            long b = bs[i];
            rs[i] = (long)((a >> b));
        }

        return rs;
    }



    @Benchmark
    public Object aShiftRMasked() {
        long[] as = fa.apply(size);
        long[] bs = fb.apply(size);
        long[] rs = fr.apply(size);
        boolean[] ms = fm.apply(size);

        for (int i = 0; i < as.length; i++) {
            long a = as[i];
            long b = bs[i];
            if (ms[i % ms.length]) {
              rs[i] = (long)((a >> b));
            } else {
              rs[i] = a;
            }
        }
        return rs;
    }



    @Benchmark
    public Object aShiftRShift() {
        long[] as = fa.apply(size);
        long[] bs = fb.apply(size);
        long[] rs = fr.apply(size);

        for (int i = 0; i < as.length; i++) {
            long a = as[i];
            long b = bs[i];
            rs[i] = (long)((a >> b));
        }

        return rs;
    }



    @Benchmark
    public Object aShiftRMaskedShift() {
        long[] as = fa.apply(size);
        long[] bs = fb.apply(size);
        long[] rs = fr.apply(size);
        boolean[] ms = fm.apply(size);

        for (int i = 0; i < as.length; i++) {
            long a = as[i];
            long b = bs[i];
            boolean m = ms[i % ms.length];
            rs[i] = (m ? (long)((a >> b)) : a);
        }

        return rs;
    }



    @Benchmark
    public Object shiftRShift() {
        long[] as = fa.apply(size);
        long[] bs = fb.apply(size);
        long[] rs = fr.apply(size);

        for (int i = 0; i < as.length; i++) {
            long a = as[i];
            long b = bs[i];
            rs[i] = (long)((a >>> b));
        }

        return rs;
    }



    @Benchmark
    public Object shiftRMaskedShift() {
        long[] as = fa.apply(size);
        long[] bs = fb.apply(size);
        long[] rs = fr.apply(size);
        boolean[] ms = fm.apply(size);

        for (int i = 0; i < as.length; i++) {
            long a = as[i];
            long b = bs[i];
            boolean m = ms[i % ms.length];
            rs[i] = (m ? (long)((a >>> b)) : a);
        }

        return rs;
    }



    @Benchmark
    public Object shiftLShift() {
        long[] as = fa.apply(size);
        long[] bs = fb.apply(size);
        long[] rs = fr.apply(size);

        for (int i = 0; i < as.length; i++) {
            long a = as[i];
            long b = bs[i];
            rs[i] = (long)((a << b));
        }

        return rs;
    }



    @Benchmark
    public Object shiftLMaskedShift() {
        long[] as = fa.apply(size);
        long[] bs = fb.apply(size);
        long[] rs = fr.apply(size);
        boolean[] ms = fm.apply(size);

        for (int i = 0; i < as.length; i++) {
            long a = as[i];
            long b = bs[i];
            boolean m = ms[i % ms.length];
            rs[i] = (m ? (long)((a << b)) : a);
        }

        return rs;
    }


    @Benchmark
    public Object max() {
        long[] as = fa.apply(size);
        long[] bs = fb.apply(size);
        long[] rs = fr.apply(size);

        for (int i = 0; i < as.length; i++) {
            long a = as[i];
            long b = bs[i];
            rs[i] = (long)((a > b) ? a : b);
        }

        return rs;
    }

    @Benchmark
    public Object min() {
        long[] as = fa.apply(size);
        long[] bs = fb.apply(size);
        long[] rs = fr.apply(size);

        for (int i = 0; i < as.length; i++) {
            long a = as[i];
            long b = bs[i];
            rs[i] = (long)((a < b) ? a : b);
        }

        return rs;
    }


    @Benchmark
    public long andAll() {
        long[] as = fa.apply(size);
        long r = -1;
        for (int i = 0; i < as.length; i++) {
          r &= as[i];
        }
        return r;
    }



    @Benchmark
    public long orAll() {
        long[] as = fa.apply(size);
        long r = 0;
        for (int i = 0; i < as.length; i++) {
          r |= as[i];
        }
        return r;
    }



    @Benchmark
    public long xorAll() {
        long[] as = fa.apply(size);
        long r = 0;
        for (int i = 0; i < as.length; i++) {
          r ^= as[i];
        }
        return r;
    }


    @Benchmark
    public long addAll() {
        long[] as = fa.apply(size);
        long r = 0;
        for (int i = 0; i < as.length; i++) {
          r += as[i];
        }
        return r;
    }

    @Benchmark
    public long subAll() {
        long[] as = fa.apply(size);
        long r = 0;
        for (int i = 0; i < as.length; i++) {
          r -= as[i];
        }
        return r;
    }

    @Benchmark
    public long mulAll() {
        long[] as = fa.apply(size);
        long r = 1;
        for (int i = 0; i < as.length; i++) {
          r *= as[i];
        }
        return r;
    }

    @Benchmark
    public long minAll() {
        long[] as = fa.apply(size);
        long r = Long.MAX_VALUE;
        for (int i = 0; i < as.length; i++) {
          r = as[i];
        }
        return r;
    }

    @Benchmark
    public long maxAll() {
        long[] as = fa.apply(size);
        long r = Long.MIN_VALUE;
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
        long[] as = fa.apply(size);
        long[] bs = fb.apply(size);

        boolean r = false;
        for (int i = 0; i < as.length; i++) {
            boolean m = (as[i] < bs[i]);
            r |= m; // accumulate so JIT can't eliminate the computation
        }

        return r;
    }

    @Benchmark
    public boolean greaterThan() {
        long[] as = fa.apply(size);
        long[] bs = fb.apply(size);

        boolean r = false;
        for (int i = 0; i < as.length; i++) {
            boolean m = (as[i] > bs[i]);
            r |= m; // accumulate so JIT can't eliminate the computation
        }

        return r;
    }

    @Benchmark
    public boolean equal() {
        long[] as = fa.apply(size);
        long[] bs = fb.apply(size);

        boolean r = false;
        for (int i = 0; i < as.length; i++) {
            boolean m = (as[i] == bs[i]);
            r |= m; // accumulate so JIT can't eliminate the computation
        }

        return r;
    }

    @Benchmark
    public boolean notEqual() {
        long[] as = fa.apply(size);
        long[] bs = fb.apply(size);

        boolean r = false;
        for (int i = 0; i < as.length; i++) {
            boolean m = (as[i] != bs[i]);
            r |= m; // accumulate so JIT can't eliminate the computation
        }

        return r;
    }

    @Benchmark
    public boolean lessThanEq() {
        long[] as = fa.apply(size);
        long[] bs = fb.apply(size);

        boolean r = false;
        for (int i = 0; i < as.length; i++) {
            boolean m = (as[i] <= bs[i]);
            r |= m; // accumulate so JIT can't eliminate the computation
        }

        return r;
    }

    @Benchmark
    public boolean greaterThanEq() {
        long[] as = fa.apply(size);
        long[] bs = fb.apply(size);

        boolean r = false;
        for (int i = 0; i < as.length; i++) {
            boolean m = (as[i] >= bs[i]);
            r |= m; // accumulate so JIT can't eliminate the computation
        }

        return r;
    }

    @Benchmark
    public Object blend() {
        long[] as = fa.apply(size);
        long[] bs = fb.apply(size);
        long[] rs = fr.apply(size);
        boolean[] ms = fm.apply(size);

        for (int i = 0; i < as.length; i++) {
            long a = as[i];
            long b = bs[i];
            boolean m = ms[i % ms.length];
            rs[i] = (m ? b : a);
        }

        return rs;
    }
    Object rearrangeShared(int window) {
        long[] as = fa.apply(size);
        int[] order = fs.apply(size);
        long[] rs = fr.apply(size);

        for (int i = 0; i < as.length; i += window) {
            for (int j = 0; j < window; j++) {
                long a = as[i+j];
                int pos = order[j];
                rs[i + pos] = a;
            }
        }

        return rs;
    }

    @Benchmark
    public Object rearrange064() {
        int window = 64 / Long.SIZE;
        return rearrangeShared(window);
    }

    @Benchmark
    public Object rearrange128() {
        int window = 128 / Long.SIZE;
        return rearrangeShared(window);
    }

    @Benchmark
    public Object rearrange256() {
        int window = 256 / Long.SIZE;
        return rearrangeShared(window);
    }

    @Benchmark
    public Object rearrange512() {
        int window = 512 / Long.SIZE;
        return rearrangeShared(window);
    }





















    @Benchmark
    public Object neg() {
        long[] as = fa.apply(size);
        long[] rs = fr.apply(size);

        for (int i = 0; i < as.length; i++) {
            long a = as[i];
            rs[i] = (long)(-((long)a));
        }

        return rs;
    }

    @Benchmark
    public Object negMasked() {
        long[] as = fa.apply(size);
        long[] rs = fr.apply(size);
        boolean[] ms = fm.apply(size);

        for (int i = 0; i < as.length; i++) {
            long a = as[i];
            boolean m = ms[i % ms.length];
            rs[i] = (m ? (long)(-((long)a)) : a);
        }

        return rs;
    }

    @Benchmark
    public Object abs() {
        long[] as = fa.apply(size);
        long[] rs = fr.apply(size);

        for (int i = 0; i < as.length; i++) {
            long a = as[i];
            rs[i] = (long)(Math.abs((long)a));
        }

        return rs;
    }

    @Benchmark
    public Object absMasked() {
        long[] as = fa.apply(size);
        long[] rs = fr.apply(size);
        boolean[] ms = fm.apply(size);

        for (int i = 0; i < as.length; i++) {
            long a = as[i];
            boolean m = ms[i % ms.length];
            rs[i] = (m ? (long)(Math.abs((long)a)) : a);
        }

        return rs;
    }


    @Benchmark
    public Object not() {
        long[] as = fa.apply(size);
        long[] rs = fr.apply(size);

        for (int i = 0; i < as.length; i++) {
            long a = as[i];
            rs[i] = (long)(~((long)a));
        }

        return rs;
    }



    @Benchmark
    public Object notMasked() {
        long[] as = fa.apply(size);
        long[] rs = fr.apply(size);
        boolean[] ms = fm.apply(size);

        for (int i = 0; i < as.length; i++) {
            long a = as[i];
            boolean m = ms[i % ms.length];
            rs[i] = (m ? (long)(~((long)a)) : a);
        }

        return rs;
    }




    @Benchmark
    public Object gatherBase0() {
        long[] as = fa.apply(size);
        int[] is    = fs.apply(size);
        long[] rs = fr.apply(size);

        for (int i = 0; i < as.length; i++) {
            int ix = 0 + is[i];
            rs[i] = as[ix];
        }

        return rs;
    }


    Object gather(int window) {
        long[] as = fa.apply(size);
        int[] is    = fs.apply(size);
        long[] rs = fr.apply(size);

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
        int window = 64 / Long.SIZE;
        return gather(window);
    }

    @Benchmark
    public Object gather128() {
        int window = 128 / Long.SIZE;
        return gather(window);
    }

    @Benchmark
    public Object gather256() {
        int window = 256 / Long.SIZE;
        return gather(window);
    }

    @Benchmark
    public Object gather512() {
        int window = 512 / Long.SIZE;
        return gather(window);
    }



    @Benchmark
    public Object scatterBase0() {
        long[] as = fa.apply(size);
        int[] is    = fs.apply(size);
        long[] rs = fr.apply(size);

        for (int i = 0; i < as.length; i++) {
            int ix = 0 + is[i];
            rs[ix] = as[i];
        }

        return rs;
    }

    Object scatter(int window) {
        long[] as = fa.apply(size);
        int[] is    = fs.apply(size);
        long[] rs = fr.apply(size);

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
        int window = 64 / Long.SIZE;
        return scatter(window);
    }

    @Benchmark
    public Object scatter128() {
        int window = 128 / Long.SIZE;
        return scatter(window);
    }

    @Benchmark
    public Object scatter256() {
        int window = 256 / Long.SIZE;
        return scatter(window);
    }

    @Benchmark
    public Object scatter512() {
        int window = 512 / Long.SIZE;
        return scatter(window);
    }

}

