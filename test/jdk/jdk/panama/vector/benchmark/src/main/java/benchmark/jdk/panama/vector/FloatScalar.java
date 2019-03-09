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
public class FloatScalar extends AbstractVectorBenchmark {
    @Param("1024")
    int size;

    float[] fill(IntFunction<Float> f) {
        float[] array = new float[size];
        for (int i = 0; i < array.length; i++) {
            array[i] = f.apply(i);
        }
        return array;
    }

    float[] as, bs, cs, rs;
    boolean[] ms, rms;
    int[] ss;

    @Setup
    public void init() {
        as = fill(i -> (float)(2*i));
        bs = fill(i -> (float)(i+1));
        cs = fill(i -> (float)(i+5));
        rs = fill(i -> (float)0);
        ms = fillMask(size, i -> (i % 2) == 0);
        rms = fillMask(size, i -> false);

        ss = fillInt(size, i -> RANDOM.nextInt(Math.max(i,1)));
    }

    final IntFunction<float[]> fa = vl -> as;
    final IntFunction<float[]> fb = vl -> bs;
    final IntFunction<float[]> fc = vl -> cs;
    final IntFunction<float[]> fr = vl -> rs;
    final IntFunction<boolean[]> fm = vl -> ms;
    final IntFunction<boolean[]> fmr = vl -> rms;
    final IntFunction<int[]> fs = vl -> ss;


    @Benchmark
    public Object add() {
        float[] as = fa.apply(size);
        float[] bs = fb.apply(size);
        float[] rs = fr.apply(size);

        for (int i = 0; i < as.length; i++) {
            float a = as[i];
            float b = bs[i];
            rs[i] = (float)(a + b);
        }

        return rs;
    }

    @Benchmark
    public Object addMasked() {
        float[] as = fa.apply(size);
        float[] bs = fb.apply(size);
        float[] rs = fr.apply(size);
        boolean[] ms = fm.apply(size);

        for (int i = 0; i < as.length; i++) {
            float a = as[i];
            float b = bs[i];
            if (ms[i % ms.length]) {
              rs[i] = (float)(a + b);
            } else {
              rs[i] = a;
            }
        }
        return rs;
    }

    @Benchmark
    public Object sub() {
        float[] as = fa.apply(size);
        float[] bs = fb.apply(size);
        float[] rs = fr.apply(size);

        for (int i = 0; i < as.length; i++) {
            float a = as[i];
            float b = bs[i];
            rs[i] = (float)(a - b);
        }

        return rs;
    }

    @Benchmark
    public Object subMasked() {
        float[] as = fa.apply(size);
        float[] bs = fb.apply(size);
        float[] rs = fr.apply(size);
        boolean[] ms = fm.apply(size);

        for (int i = 0; i < as.length; i++) {
            float a = as[i];
            float b = bs[i];
            if (ms[i % ms.length]) {
              rs[i] = (float)(a - b);
            } else {
              rs[i] = a;
            }
        }
        return rs;
    }


    @Benchmark
    public Object div() {
        float[] as = fa.apply(size);
        float[] bs = fb.apply(size);
        float[] rs = fr.apply(size);

        for (int i = 0; i < as.length; i++) {
            float a = as[i];
            float b = bs[i];
            rs[i] = (float)(a / b);
        }

        return rs;
    }



    @Benchmark
    public Object divMasked() {
        float[] as = fa.apply(size);
        float[] bs = fb.apply(size);
        float[] rs = fr.apply(size);
        boolean[] ms = fm.apply(size);

        for (int i = 0; i < as.length; i++) {
            float a = as[i];
            float b = bs[i];
            if (ms[i % ms.length]) {
              rs[i] = (float)(a / b);
            } else {
              rs[i] = a;
            }
        }
        return rs;
    }


    @Benchmark
    public Object mul() {
        float[] as = fa.apply(size);
        float[] bs = fb.apply(size);
        float[] rs = fr.apply(size);

        for (int i = 0; i < as.length; i++) {
            float a = as[i];
            float b = bs[i];
            rs[i] = (float)(a * b);
        }

        return rs;
    }

    @Benchmark
    public Object mulMasked() {
        float[] as = fa.apply(size);
        float[] bs = fb.apply(size);
        float[] rs = fr.apply(size);
        boolean[] ms = fm.apply(size);

        for (int i = 0; i < as.length; i++) {
            float a = as[i];
            float b = bs[i];
            if (ms[i % ms.length]) {
              rs[i] = (float)(a * b);
            } else {
              rs[i] = a;
            }
        }
        return rs;
    }



















    @Benchmark
    public Object max() {
        float[] as = fa.apply(size);
        float[] bs = fb.apply(size);
        float[] rs = fr.apply(size);

        for (int i = 0; i < as.length; i++) {
            float a = as[i];
            float b = bs[i];
            rs[i] = (float)((a > b) ? a : b);
        }

        return rs;
    }

    @Benchmark
    public Object min() {
        float[] as = fa.apply(size);
        float[] bs = fb.apply(size);
        float[] rs = fr.apply(size);

        for (int i = 0; i < as.length; i++) {
            float a = as[i];
            float b = bs[i];
            rs[i] = (float)((a < b) ? a : b);
        }

        return rs;
    }




    @Benchmark
    public float addAll() {
        float[] as = fa.apply(size);
        float r = 0;
        for (int i = 0; i < as.length; i++) {
          r += as[i];
        }
        return r;
    }

    @Benchmark
    public float subAll() {
        float[] as = fa.apply(size);
        float r = 0;
        for (int i = 0; i < as.length; i++) {
          r -= as[i];
        }
        return r;
    }

    @Benchmark
    public float mulAll() {
        float[] as = fa.apply(size);
        float r = 1;
        for (int i = 0; i < as.length; i++) {
          r *= as[i];
        }
        return r;
    }

    @Benchmark
    public float minAll() {
        float[] as = fa.apply(size);
        float r = Float.MAX_VALUE;
        for (int i = 0; i < as.length; i++) {
          r = as[i];
        }
        return r;
    }

    @Benchmark
    public float maxAll() {
        float[] as = fa.apply(size);
        float r = Float.MIN_VALUE;
        for (int i = 0; i < as.length; i++) {
          r = as[i];
        }
        return r;
    }



    @Benchmark
    public boolean lessThan() {
        float[] as = fa.apply(size);
        float[] bs = fb.apply(size);

        boolean r = false;
        for (int i = 0; i < as.length; i++) {
            boolean m = (as[i] < bs[i]);
            r |= m; // accumulate so JIT can't eliminate the computation
        }

        return r;
    }

    @Benchmark
    public boolean greaterThan() {
        float[] as = fa.apply(size);
        float[] bs = fb.apply(size);

        boolean r = false;
        for (int i = 0; i < as.length; i++) {
            boolean m = (as[i] > bs[i]);
            r |= m; // accumulate so JIT can't eliminate the computation
        }

        return r;
    }

    @Benchmark
    public boolean equal() {
        float[] as = fa.apply(size);
        float[] bs = fb.apply(size);

        boolean r = false;
        for (int i = 0; i < as.length; i++) {
            boolean m = (as[i] == bs[i]);
            r |= m; // accumulate so JIT can't eliminate the computation
        }

        return r;
    }

    @Benchmark
    public boolean notEqual() {
        float[] as = fa.apply(size);
        float[] bs = fb.apply(size);

        boolean r = false;
        for (int i = 0; i < as.length; i++) {
            boolean m = (as[i] != bs[i]);
            r |= m; // accumulate so JIT can't eliminate the computation
        }

        return r;
    }

    @Benchmark
    public boolean lessThanEq() {
        float[] as = fa.apply(size);
        float[] bs = fb.apply(size);

        boolean r = false;
        for (int i = 0; i < as.length; i++) {
            boolean m = (as[i] <= bs[i]);
            r |= m; // accumulate so JIT can't eliminate the computation
        }

        return r;
    }

    @Benchmark
    public boolean greaterThanEq() {
        float[] as = fa.apply(size);
        float[] bs = fb.apply(size);

        boolean r = false;
        for (int i = 0; i < as.length; i++) {
            boolean m = (as[i] >= bs[i]);
            r |= m; // accumulate so JIT can't eliminate the computation
        }

        return r;
    }

    @Benchmark
    public Object blend() {
        float[] as = fa.apply(size);
        float[] bs = fb.apply(size);
        float[] rs = fr.apply(size);
        boolean[] ms = fm.apply(size);

        for (int i = 0; i < as.length; i++) {
            float a = as[i];
            float b = bs[i];
            boolean m = ms[i % ms.length];
            rs[i] = (m ? b : a);
        }

        return rs;
    }
    Object rearrangeShared(int window) {
        float[] as = fa.apply(size);
        int[] order = fs.apply(size);
        float[] rs = fr.apply(size);

        for (int i = 0; i < as.length; i += window) {
            for (int j = 0; j < window; j++) {
                float a = as[i+j];
                int pos = order[j];
                rs[i + pos] = a;
            }
        }

        return rs;
    }

    @Benchmark
    public Object rearrange064() {
        int window = 64 / Float.SIZE;
        return rearrangeShared(window);
    }

    @Benchmark
    public Object rearrange128() {
        int window = 128 / Float.SIZE;
        return rearrangeShared(window);
    }

    @Benchmark
    public Object rearrange256() {
        int window = 256 / Float.SIZE;
        return rearrangeShared(window);
    }

    @Benchmark
    public Object rearrange512() {
        int window = 512 / Float.SIZE;
        return rearrangeShared(window);
    }


    @Benchmark
    public Object sin() {
        float[] as = fa.apply(size);
        float[] rs = fr.apply(size);

        for (int i = 0; i < as.length; i++) {
            float a = as[i];
            rs[i] = (float)(Math.sin((double)a));
        }

        return rs;
    }



    @Benchmark
    public Object exp() {
        float[] as = fa.apply(size);
        float[] rs = fr.apply(size);

        for (int i = 0; i < as.length; i++) {
            float a = as[i];
            rs[i] = (float)(Math.exp((double)a));
        }

        return rs;
    }



    @Benchmark
    public Object log1p() {
        float[] as = fa.apply(size);
        float[] rs = fr.apply(size);

        for (int i = 0; i < as.length; i++) {
            float a = as[i];
            rs[i] = (float)(Math.log1p((double)a));
        }

        return rs;
    }



    @Benchmark
    public Object log() {
        float[] as = fa.apply(size);
        float[] rs = fr.apply(size);

        for (int i = 0; i < as.length; i++) {
            float a = as[i];
            rs[i] = (float)(Math.log((double)a));
        }

        return rs;
    }



    @Benchmark
    public Object log10() {
        float[] as = fa.apply(size);
        float[] rs = fr.apply(size);

        for (int i = 0; i < as.length; i++) {
            float a = as[i];
            rs[i] = (float)(Math.log10((double)a));
        }

        return rs;
    }



    @Benchmark
    public Object expm1() {
        float[] as = fa.apply(size);
        float[] rs = fr.apply(size);

        for (int i = 0; i < as.length; i++) {
            float a = as[i];
            rs[i] = (float)(Math.expm1((double)a));
        }

        return rs;
    }



    @Benchmark
    public Object cos() {
        float[] as = fa.apply(size);
        float[] rs = fr.apply(size);

        for (int i = 0; i < as.length; i++) {
            float a = as[i];
            rs[i] = (float)(Math.cos((double)a));
        }

        return rs;
    }



    @Benchmark
    public Object tan() {
        float[] as = fa.apply(size);
        float[] rs = fr.apply(size);

        for (int i = 0; i < as.length; i++) {
            float a = as[i];
            rs[i] = (float)(Math.tan((double)a));
        }

        return rs;
    }



    @Benchmark
    public Object sinh() {
        float[] as = fa.apply(size);
        float[] rs = fr.apply(size);

        for (int i = 0; i < as.length; i++) {
            float a = as[i];
            rs[i] = (float)(Math.sinh((double)a));
        }

        return rs;
    }



    @Benchmark
    public Object cosh() {
        float[] as = fa.apply(size);
        float[] rs = fr.apply(size);

        for (int i = 0; i < as.length; i++) {
            float a = as[i];
            rs[i] = (float)(Math.cosh((double)a));
        }

        return rs;
    }



    @Benchmark
    public Object tanh() {
        float[] as = fa.apply(size);
        float[] rs = fr.apply(size);

        for (int i = 0; i < as.length; i++) {
            float a = as[i];
            rs[i] = (float)(Math.tanh((double)a));
        }

        return rs;
    }



    @Benchmark
    public Object asin() {
        float[] as = fa.apply(size);
        float[] rs = fr.apply(size);

        for (int i = 0; i < as.length; i++) {
            float a = as[i];
            rs[i] = (float)(Math.asin((double)a));
        }

        return rs;
    }



    @Benchmark
    public Object acos() {
        float[] as = fa.apply(size);
        float[] rs = fr.apply(size);

        for (int i = 0; i < as.length; i++) {
            float a = as[i];
            rs[i] = (float)(Math.acos((double)a));
        }

        return rs;
    }



    @Benchmark
    public Object atan() {
        float[] as = fa.apply(size);
        float[] rs = fr.apply(size);

        for (int i = 0; i < as.length; i++) {
            float a = as[i];
            rs[i] = (float)(Math.atan((double)a));
        }

        return rs;
    }



    @Benchmark
    public Object cbrt() {
        float[] as = fa.apply(size);
        float[] rs = fr.apply(size);

        for (int i = 0; i < as.length; i++) {
            float a = as[i];
            rs[i] = (float)(Math.cbrt((double)a));
        }

        return rs;
    }



    @Benchmark
    public Object hypot() {
        float[] as = fa.apply(size);
        float[] bs = fb.apply(size);
        float[] rs = fr.apply(size);

        for (int i = 0; i < as.length; i++) {
            float a = as[i];
            float b = bs[i];
            rs[i] = (float)(Math.hypot((double)a, (double)b));
        }

        return rs;
    }



    @Benchmark
    public Object pow() {
        float[] as = fa.apply(size);
        float[] bs = fb.apply(size);
        float[] rs = fr.apply(size);

        for (int i = 0; i < as.length; i++) {
            float a = as[i];
            float b = bs[i];
            rs[i] = (float)(Math.pow((double)a, (double)b));
        }

        return rs;
    }



    @Benchmark
    public Object atan2() {
        float[] as = fa.apply(size);
        float[] bs = fb.apply(size);
        float[] rs = fr.apply(size);

        for (int i = 0; i < as.length; i++) {
            float a = as[i];
            float b = bs[i];
            rs[i] = (float)(Math.atan2((double)a, (double)b));
        }

        return rs;
    }



    @Benchmark
    public Object fma() {
        float[] as = fa.apply(size);
        float[] bs = fb.apply(size);
        float[] cs = fc.apply(size);
        float[] rs = fr.apply(size);

        for (int i = 0; i < as.length; i++) {
            float a = as[i];
            float b = bs[i];
            float c = cs[i];
            rs[i] = (float)(Math.fma(a, b, c));
        }

        return rs;
    }




    @Benchmark
    public Object fmaMasked() {
        float[] as = fa.apply(size);
        float[] bs = fb.apply(size);
        float[] cs = fc.apply(size);
        float[] rs = fr.apply(size);
        boolean[] ms = fm.apply(size);

        for (int i = 0; i < as.length; i++) {
            float a = as[i];
            float b = bs[i];
            float c = cs[i];
            if (ms[i % ms.length]) {
              rs[i] = (float)(Math.fma(a, b, c));
            } else {
              rs[i] = a;
            }
        }
        return rs;
    }


    @Benchmark
    public Object neg() {
        float[] as = fa.apply(size);
        float[] rs = fr.apply(size);

        for (int i = 0; i < as.length; i++) {
            float a = as[i];
            rs[i] = (float)(-((float)a));
        }

        return rs;
    }

    @Benchmark
    public Object negMasked() {
        float[] as = fa.apply(size);
        float[] rs = fr.apply(size);
        boolean[] ms = fm.apply(size);

        for (int i = 0; i < as.length; i++) {
            float a = as[i];
            boolean m = ms[i % ms.length];
            rs[i] = (m ? (float)(-((float)a)) : a);
        }

        return rs;
    }

    @Benchmark
    public Object abs() {
        float[] as = fa.apply(size);
        float[] rs = fr.apply(size);

        for (int i = 0; i < as.length; i++) {
            float a = as[i];
            rs[i] = (float)(Math.abs((float)a));
        }

        return rs;
    }

    @Benchmark
    public Object absMasked() {
        float[] as = fa.apply(size);
        float[] rs = fr.apply(size);
        boolean[] ms = fm.apply(size);

        for (int i = 0; i < as.length; i++) {
            float a = as[i];
            boolean m = ms[i % ms.length];
            rs[i] = (m ? (float)(Math.abs((float)a)) : a);
        }

        return rs;
    }




    @Benchmark
    public Object sqrt() {
        float[] as = fa.apply(size);
        float[] rs = fr.apply(size);

        for (int i = 0; i < as.length; i++) {
            float a = as[i];
            rs[i] = (float)(Math.sqrt((double)a));
        }

        return rs;
    }



    @Benchmark
    public Object sqrtMasked() {
        float[] as = fa.apply(size);
        float[] rs = fr.apply(size);
        boolean[] ms = fm.apply(size);

        for (int i = 0; i < as.length; i++) {
            float a = as[i];
            boolean m = ms[i % ms.length];
            rs[i] = (m ? (float)(Math.sqrt((double)a)) : a);
        }

        return rs;
    }


    @Benchmark
    public Object gatherBase0() {
        float[] as = fa.apply(size);
        int[] is    = fs.apply(size);
        float[] rs = fr.apply(size);

        for (int i = 0; i < as.length; i++) {
            int ix = 0 + is[i];
            rs[i] = as[ix];
        }

        return rs;
    }


    Object gather(int window) {
        float[] as = fa.apply(size);
        int[] is    = fs.apply(size);
        float[] rs = fr.apply(size);

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
        int window = 64 / Float.SIZE;
        return gather(window);
    }

    @Benchmark
    public Object gather128() {
        int window = 128 / Float.SIZE;
        return gather(window);
    }

    @Benchmark
    public Object gather256() {
        int window = 256 / Float.SIZE;
        return gather(window);
    }

    @Benchmark
    public Object gather512() {
        int window = 512 / Float.SIZE;
        return gather(window);
    }



    @Benchmark
    public Object scatterBase0() {
        float[] as = fa.apply(size);
        int[] is    = fs.apply(size);
        float[] rs = fr.apply(size);

        for (int i = 0; i < as.length; i++) {
            int ix = 0 + is[i];
            rs[ix] = as[i];
        }

        return rs;
    }

    Object scatter(int window) {
        float[] as = fa.apply(size);
        int[] is    = fs.apply(size);
        float[] rs = fr.apply(size);

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
        int window = 64 / Float.SIZE;
        return scatter(window);
    }

    @Benchmark
    public Object scatter128() {
        int window = 128 / Float.SIZE;
        return scatter(window);
    }

    @Benchmark
    public Object scatter256() {
        int window = 256 / Float.SIZE;
        return scatter(window);
    }

    @Benchmark
    public Object scatter512() {
        int window = 512 / Float.SIZE;
        return scatter(window);
    }

}

