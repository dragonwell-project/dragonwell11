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
public class DoubleScalar extends AbstractVectorBenchmark {
    @Param("1024")
    int size;

    double[] fill(IntFunction<Double> f) {
        double[] array = new double[size];
        for (int i = 0; i < array.length; i++) {
            array[i] = f.apply(i);
        }
        return array;
    }

    double[] as, bs, cs, rs;
    boolean[] ms, rms;
    int[] ss;

    @Setup
    public void init() {
        as = fill(i -> (double)(2*i));
        bs = fill(i -> (double)(i+1));
        cs = fill(i -> (double)(i+5));
        rs = fill(i -> (double)0);
        ms = fillMask(size, i -> (i % 2) == 0);
        rms = fillMask(size, i -> false);

        ss = fillInt(size, i -> RANDOM.nextInt(Math.max(i,1)));
    }

    final IntFunction<double[]> fa = vl -> as;
    final IntFunction<double[]> fb = vl -> bs;
    final IntFunction<double[]> fc = vl -> cs;
    final IntFunction<double[]> fr = vl -> rs;
    final IntFunction<boolean[]> fm = vl -> ms;
    final IntFunction<boolean[]> fmr = vl -> rms;
    final IntFunction<int[]> fs = vl -> ss;


    @Benchmark
    public Object add() {
        double[] as = fa.apply(size);
        double[] bs = fb.apply(size);
        double[] rs = fr.apply(size);

        for (int i = 0; i < as.length; i++) {
            double a = as[i];
            double b = bs[i];
            rs[i] = (double)(a + b);
        }

        return rs;
    }

    @Benchmark
    public Object addMasked() {
        double[] as = fa.apply(size);
        double[] bs = fb.apply(size);
        double[] rs = fr.apply(size);
        boolean[] ms = fm.apply(size);

        for (int i = 0; i < as.length; i++) {
            double a = as[i];
            double b = bs[i];
            if (ms[i % ms.length]) {
              rs[i] = (double)(a + b);
            } else {
              rs[i] = a;
            }
        }
        return rs;
    }

    @Benchmark
    public Object sub() {
        double[] as = fa.apply(size);
        double[] bs = fb.apply(size);
        double[] rs = fr.apply(size);

        for (int i = 0; i < as.length; i++) {
            double a = as[i];
            double b = bs[i];
            rs[i] = (double)(a - b);
        }

        return rs;
    }

    @Benchmark
    public Object subMasked() {
        double[] as = fa.apply(size);
        double[] bs = fb.apply(size);
        double[] rs = fr.apply(size);
        boolean[] ms = fm.apply(size);

        for (int i = 0; i < as.length; i++) {
            double a = as[i];
            double b = bs[i];
            if (ms[i % ms.length]) {
              rs[i] = (double)(a - b);
            } else {
              rs[i] = a;
            }
        }
        return rs;
    }


    @Benchmark
    public Object div() {
        double[] as = fa.apply(size);
        double[] bs = fb.apply(size);
        double[] rs = fr.apply(size);

        for (int i = 0; i < as.length; i++) {
            double a = as[i];
            double b = bs[i];
            rs[i] = (double)(a / b);
        }

        return rs;
    }



    @Benchmark
    public Object divMasked() {
        double[] as = fa.apply(size);
        double[] bs = fb.apply(size);
        double[] rs = fr.apply(size);
        boolean[] ms = fm.apply(size);

        for (int i = 0; i < as.length; i++) {
            double a = as[i];
            double b = bs[i];
            if (ms[i % ms.length]) {
              rs[i] = (double)(a / b);
            } else {
              rs[i] = a;
            }
        }
        return rs;
    }


    @Benchmark
    public Object mul() {
        double[] as = fa.apply(size);
        double[] bs = fb.apply(size);
        double[] rs = fr.apply(size);

        for (int i = 0; i < as.length; i++) {
            double a = as[i];
            double b = bs[i];
            rs[i] = (double)(a * b);
        }

        return rs;
    }

    @Benchmark
    public Object mulMasked() {
        double[] as = fa.apply(size);
        double[] bs = fb.apply(size);
        double[] rs = fr.apply(size);
        boolean[] ms = fm.apply(size);

        for (int i = 0; i < as.length; i++) {
            double a = as[i];
            double b = bs[i];
            if (ms[i % ms.length]) {
              rs[i] = (double)(a * b);
            } else {
              rs[i] = a;
            }
        }
        return rs;
    }



















    @Benchmark
    public Object max() {
        double[] as = fa.apply(size);
        double[] bs = fb.apply(size);
        double[] rs = fr.apply(size);

        for (int i = 0; i < as.length; i++) {
            double a = as[i];
            double b = bs[i];
            rs[i] = (double)((a > b) ? a : b);
        }

        return rs;
    }

    @Benchmark
    public Object min() {
        double[] as = fa.apply(size);
        double[] bs = fb.apply(size);
        double[] rs = fr.apply(size);

        for (int i = 0; i < as.length; i++) {
            double a = as[i];
            double b = bs[i];
            rs[i] = (double)((a < b) ? a : b);
        }

        return rs;
    }




    @Benchmark
    public double addAll() {
        double[] as = fa.apply(size);
        double r = 0;
        for (int i = 0; i < as.length; i++) {
          r += as[i];
        }
        return r;
    }

    @Benchmark
    public double subAll() {
        double[] as = fa.apply(size);
        double r = 0;
        for (int i = 0; i < as.length; i++) {
          r -= as[i];
        }
        return r;
    }

    @Benchmark
    public double mulAll() {
        double[] as = fa.apply(size);
        double r = 1;
        for (int i = 0; i < as.length; i++) {
          r *= as[i];
        }
        return r;
    }

    @Benchmark
    public double minAll() {
        double[] as = fa.apply(size);
        double r = Double.MAX_VALUE;
        for (int i = 0; i < as.length; i++) {
          r = as[i];
        }
        return r;
    }

    @Benchmark
    public double maxAll() {
        double[] as = fa.apply(size);
        double r = Double.MIN_VALUE;
        for (int i = 0; i < as.length; i++) {
          r = as[i];
        }
        return r;
    }



    @Benchmark
    public boolean lessThan() {
        double[] as = fa.apply(size);
        double[] bs = fb.apply(size);

        boolean r = false;
        for (int i = 0; i < as.length; i++) {
            boolean m = (as[i] < bs[i]);
            r |= m; // accumulate so JIT can't eliminate the computation
        }

        return r;
    }

    @Benchmark
    public boolean greaterThan() {
        double[] as = fa.apply(size);
        double[] bs = fb.apply(size);

        boolean r = false;
        for (int i = 0; i < as.length; i++) {
            boolean m = (as[i] > bs[i]);
            r |= m; // accumulate so JIT can't eliminate the computation
        }

        return r;
    }

    @Benchmark
    public boolean equal() {
        double[] as = fa.apply(size);
        double[] bs = fb.apply(size);

        boolean r = false;
        for (int i = 0; i < as.length; i++) {
            boolean m = (as[i] == bs[i]);
            r |= m; // accumulate so JIT can't eliminate the computation
        }

        return r;
    }

    @Benchmark
    public boolean notEqual() {
        double[] as = fa.apply(size);
        double[] bs = fb.apply(size);

        boolean r = false;
        for (int i = 0; i < as.length; i++) {
            boolean m = (as[i] != bs[i]);
            r |= m; // accumulate so JIT can't eliminate the computation
        }

        return r;
    }

    @Benchmark
    public boolean lessThanEq() {
        double[] as = fa.apply(size);
        double[] bs = fb.apply(size);

        boolean r = false;
        for (int i = 0; i < as.length; i++) {
            boolean m = (as[i] <= bs[i]);
            r |= m; // accumulate so JIT can't eliminate the computation
        }

        return r;
    }

    @Benchmark
    public boolean greaterThanEq() {
        double[] as = fa.apply(size);
        double[] bs = fb.apply(size);

        boolean r = false;
        for (int i = 0; i < as.length; i++) {
            boolean m = (as[i] >= bs[i]);
            r |= m; // accumulate so JIT can't eliminate the computation
        }

        return r;
    }

    @Benchmark
    public Object blend() {
        double[] as = fa.apply(size);
        double[] bs = fb.apply(size);
        double[] rs = fr.apply(size);
        boolean[] ms = fm.apply(size);

        for (int i = 0; i < as.length; i++) {
            double a = as[i];
            double b = bs[i];
            boolean m = ms[i % ms.length];
            rs[i] = (m ? b : a);
        }

        return rs;
    }
    Object rearrangeShared(int window) {
        double[] as = fa.apply(size);
        int[] order = fs.apply(size);
        double[] rs = fr.apply(size);

        for (int i = 0; i < as.length; i += window) {
            for (int j = 0; j < window; j++) {
                double a = as[i+j];
                int pos = order[j];
                rs[i + pos] = a;
            }
        }

        return rs;
    }

    @Benchmark
    public Object rearrange064() {
        int window = 64 / Double.SIZE;
        return rearrangeShared(window);
    }

    @Benchmark
    public Object rearrange128() {
        int window = 128 / Double.SIZE;
        return rearrangeShared(window);
    }

    @Benchmark
    public Object rearrange256() {
        int window = 256 / Double.SIZE;
        return rearrangeShared(window);
    }

    @Benchmark
    public Object rearrange512() {
        int window = 512 / Double.SIZE;
        return rearrangeShared(window);
    }


    @Benchmark
    public Object sin() {
        double[] as = fa.apply(size);
        double[] rs = fr.apply(size);

        for (int i = 0; i < as.length; i++) {
            double a = as[i];
            rs[i] = (double)(Math.sin((double)a));
        }

        return rs;
    }



    @Benchmark
    public Object exp() {
        double[] as = fa.apply(size);
        double[] rs = fr.apply(size);

        for (int i = 0; i < as.length; i++) {
            double a = as[i];
            rs[i] = (double)(Math.exp((double)a));
        }

        return rs;
    }



    @Benchmark
    public Object log1p() {
        double[] as = fa.apply(size);
        double[] rs = fr.apply(size);

        for (int i = 0; i < as.length; i++) {
            double a = as[i];
            rs[i] = (double)(Math.log1p((double)a));
        }

        return rs;
    }



    @Benchmark
    public Object log() {
        double[] as = fa.apply(size);
        double[] rs = fr.apply(size);

        for (int i = 0; i < as.length; i++) {
            double a = as[i];
            rs[i] = (double)(Math.log((double)a));
        }

        return rs;
    }



    @Benchmark
    public Object log10() {
        double[] as = fa.apply(size);
        double[] rs = fr.apply(size);

        for (int i = 0; i < as.length; i++) {
            double a = as[i];
            rs[i] = (double)(Math.log10((double)a));
        }

        return rs;
    }



    @Benchmark
    public Object expm1() {
        double[] as = fa.apply(size);
        double[] rs = fr.apply(size);

        for (int i = 0; i < as.length; i++) {
            double a = as[i];
            rs[i] = (double)(Math.expm1((double)a));
        }

        return rs;
    }



    @Benchmark
    public Object cos() {
        double[] as = fa.apply(size);
        double[] rs = fr.apply(size);

        for (int i = 0; i < as.length; i++) {
            double a = as[i];
            rs[i] = (double)(Math.cos((double)a));
        }

        return rs;
    }



    @Benchmark
    public Object tan() {
        double[] as = fa.apply(size);
        double[] rs = fr.apply(size);

        for (int i = 0; i < as.length; i++) {
            double a = as[i];
            rs[i] = (double)(Math.tan((double)a));
        }

        return rs;
    }



    @Benchmark
    public Object sinh() {
        double[] as = fa.apply(size);
        double[] rs = fr.apply(size);

        for (int i = 0; i < as.length; i++) {
            double a = as[i];
            rs[i] = (double)(Math.sinh((double)a));
        }

        return rs;
    }



    @Benchmark
    public Object cosh() {
        double[] as = fa.apply(size);
        double[] rs = fr.apply(size);

        for (int i = 0; i < as.length; i++) {
            double a = as[i];
            rs[i] = (double)(Math.cosh((double)a));
        }

        return rs;
    }



    @Benchmark
    public Object tanh() {
        double[] as = fa.apply(size);
        double[] rs = fr.apply(size);

        for (int i = 0; i < as.length; i++) {
            double a = as[i];
            rs[i] = (double)(Math.tanh((double)a));
        }

        return rs;
    }



    @Benchmark
    public Object asin() {
        double[] as = fa.apply(size);
        double[] rs = fr.apply(size);

        for (int i = 0; i < as.length; i++) {
            double a = as[i];
            rs[i] = (double)(Math.asin((double)a));
        }

        return rs;
    }



    @Benchmark
    public Object acos() {
        double[] as = fa.apply(size);
        double[] rs = fr.apply(size);

        for (int i = 0; i < as.length; i++) {
            double a = as[i];
            rs[i] = (double)(Math.acos((double)a));
        }

        return rs;
    }



    @Benchmark
    public Object atan() {
        double[] as = fa.apply(size);
        double[] rs = fr.apply(size);

        for (int i = 0; i < as.length; i++) {
            double a = as[i];
            rs[i] = (double)(Math.atan((double)a));
        }

        return rs;
    }



    @Benchmark
    public Object cbrt() {
        double[] as = fa.apply(size);
        double[] rs = fr.apply(size);

        for (int i = 0; i < as.length; i++) {
            double a = as[i];
            rs[i] = (double)(Math.cbrt((double)a));
        }

        return rs;
    }



    @Benchmark
    public Object hypot() {
        double[] as = fa.apply(size);
        double[] bs = fb.apply(size);
        double[] rs = fr.apply(size);

        for (int i = 0; i < as.length; i++) {
            double a = as[i];
            double b = bs[i];
            rs[i] = (double)(Math.hypot((double)a, (double)b));
        }

        return rs;
    }



    @Benchmark
    public Object pow() {
        double[] as = fa.apply(size);
        double[] bs = fb.apply(size);
        double[] rs = fr.apply(size);

        for (int i = 0; i < as.length; i++) {
            double a = as[i];
            double b = bs[i];
            rs[i] = (double)(Math.pow((double)a, (double)b));
        }

        return rs;
    }



    @Benchmark
    public Object atan2() {
        double[] as = fa.apply(size);
        double[] bs = fb.apply(size);
        double[] rs = fr.apply(size);

        for (int i = 0; i < as.length; i++) {
            double a = as[i];
            double b = bs[i];
            rs[i] = (double)(Math.atan2((double)a, (double)b));
        }

        return rs;
    }



    @Benchmark
    public Object fma() {
        double[] as = fa.apply(size);
        double[] bs = fb.apply(size);
        double[] cs = fc.apply(size);
        double[] rs = fr.apply(size);

        for (int i = 0; i < as.length; i++) {
            double a = as[i];
            double b = bs[i];
            double c = cs[i];
            rs[i] = (double)(Math.fma(a, b, c));
        }

        return rs;
    }




    @Benchmark
    public Object fmaMasked() {
        double[] as = fa.apply(size);
        double[] bs = fb.apply(size);
        double[] cs = fc.apply(size);
        double[] rs = fr.apply(size);
        boolean[] ms = fm.apply(size);

        for (int i = 0; i < as.length; i++) {
            double a = as[i];
            double b = bs[i];
            double c = cs[i];
            if (ms[i % ms.length]) {
              rs[i] = (double)(Math.fma(a, b, c));
            } else {
              rs[i] = a;
            }
        }
        return rs;
    }


    @Benchmark
    public Object neg() {
        double[] as = fa.apply(size);
        double[] rs = fr.apply(size);

        for (int i = 0; i < as.length; i++) {
            double a = as[i];
            rs[i] = (double)(-((double)a));
        }

        return rs;
    }

    @Benchmark
    public Object negMasked() {
        double[] as = fa.apply(size);
        double[] rs = fr.apply(size);
        boolean[] ms = fm.apply(size);

        for (int i = 0; i < as.length; i++) {
            double a = as[i];
            boolean m = ms[i % ms.length];
            rs[i] = (m ? (double)(-((double)a)) : a);
        }

        return rs;
    }

    @Benchmark
    public Object abs() {
        double[] as = fa.apply(size);
        double[] rs = fr.apply(size);

        for (int i = 0; i < as.length; i++) {
            double a = as[i];
            rs[i] = (double)(Math.abs((double)a));
        }

        return rs;
    }

    @Benchmark
    public Object absMasked() {
        double[] as = fa.apply(size);
        double[] rs = fr.apply(size);
        boolean[] ms = fm.apply(size);

        for (int i = 0; i < as.length; i++) {
            double a = as[i];
            boolean m = ms[i % ms.length];
            rs[i] = (m ? (double)(Math.abs((double)a)) : a);
        }

        return rs;
    }




    @Benchmark
    public Object sqrt() {
        double[] as = fa.apply(size);
        double[] rs = fr.apply(size);

        for (int i = 0; i < as.length; i++) {
            double a = as[i];
            rs[i] = (double)(Math.sqrt((double)a));
        }

        return rs;
    }



    @Benchmark
    public Object sqrtMasked() {
        double[] as = fa.apply(size);
        double[] rs = fr.apply(size);
        boolean[] ms = fm.apply(size);

        for (int i = 0; i < as.length; i++) {
            double a = as[i];
            boolean m = ms[i % ms.length];
            rs[i] = (m ? (double)(Math.sqrt((double)a)) : a);
        }

        return rs;
    }


    @Benchmark
    public Object gatherBase0() {
        double[] as = fa.apply(size);
        int[] is    = fs.apply(size);
        double[] rs = fr.apply(size);

        for (int i = 0; i < as.length; i++) {
            int ix = 0 + is[i];
            rs[i] = as[ix];
        }

        return rs;
    }


    Object gather(int window) {
        double[] as = fa.apply(size);
        int[] is    = fs.apply(size);
        double[] rs = fr.apply(size);

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
        int window = 64 / Double.SIZE;
        return gather(window);
    }

    @Benchmark
    public Object gather128() {
        int window = 128 / Double.SIZE;
        return gather(window);
    }

    @Benchmark
    public Object gather256() {
        int window = 256 / Double.SIZE;
        return gather(window);
    }

    @Benchmark
    public Object gather512() {
        int window = 512 / Double.SIZE;
        return gather(window);
    }



    @Benchmark
    public Object scatterBase0() {
        double[] as = fa.apply(size);
        int[] is    = fs.apply(size);
        double[] rs = fr.apply(size);

        for (int i = 0; i < as.length; i++) {
            int ix = 0 + is[i];
            rs[ix] = as[i];
        }

        return rs;
    }

    Object scatter(int window) {
        double[] as = fa.apply(size);
        int[] is    = fs.apply(size);
        double[] rs = fr.apply(size);

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
        int window = 64 / Double.SIZE;
        return scatter(window);
    }

    @Benchmark
    public Object scatter128() {
        int window = 128 / Double.SIZE;
        return scatter(window);
    }

    @Benchmark
    public Object scatter256() {
        int window = 256 / Double.SIZE;
        return scatter(window);
    }

    @Benchmark
    public Object scatter512() {
        int window = 512 / Double.SIZE;
        return scatter(window);
    }

}

