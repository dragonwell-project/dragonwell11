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
public class ShortScalar extends AbstractVectorBenchmark {
    @Param("1024")
    int size;

    short[] fill(IntFunction<Short> f) {
        short[] array = new short[size];
        for (int i = 0; i < array.length; i++) {
            array[i] = f.apply(i);
        }
        return array;
    }

    short[] as, bs, cs, rs;
    boolean[] ms, rms;
    int[] ss;

    @Setup
    public void init() {
        as = fill(i -> (short)(2*i));
        bs = fill(i -> (short)(i+1));
        cs = fill(i -> (short)(i+5));
        rs = fill(i -> (short)0);
        ms = fillMask(size, i -> (i % 2) == 0);
        rms = fillMask(size, i -> false);

        ss = fillInt(size, i -> RANDOM.nextInt(Math.max(i,1)));
    }

    final IntFunction<short[]> fa = vl -> as;
    final IntFunction<short[]> fb = vl -> bs;
    final IntFunction<short[]> fc = vl -> cs;
    final IntFunction<short[]> fr = vl -> rs;
    final IntFunction<boolean[]> fm = vl -> ms;
    final IntFunction<boolean[]> fmr = vl -> rms;
    final IntFunction<int[]> fs = vl -> ss;


    @Benchmark
    public Object add() {
        short[] as = fa.apply(size);
        short[] bs = fb.apply(size);
        short[] rs = fr.apply(size);

        for (int i = 0; i < as.length; i++) {
            short a = as[i];
            short b = bs[i];
            rs[i] = (short)(a + b);
        }

        return rs;
    }

    @Benchmark
    public Object addMasked() {
        short[] as = fa.apply(size);
        short[] bs = fb.apply(size);
        short[] rs = fr.apply(size);
        boolean[] ms = fm.apply(size);

        for (int i = 0; i < as.length; i++) {
            short a = as[i];
            short b = bs[i];
            if (ms[i % ms.length]) {
              rs[i] = (short)(a + b);
            } else {
              rs[i] = a;
            }
        }
        return rs;
    }

    @Benchmark
    public Object sub() {
        short[] as = fa.apply(size);
        short[] bs = fb.apply(size);
        short[] rs = fr.apply(size);

        for (int i = 0; i < as.length; i++) {
            short a = as[i];
            short b = bs[i];
            rs[i] = (short)(a - b);
        }

        return rs;
    }

    @Benchmark
    public Object subMasked() {
        short[] as = fa.apply(size);
        short[] bs = fb.apply(size);
        short[] rs = fr.apply(size);
        boolean[] ms = fm.apply(size);

        for (int i = 0; i < as.length; i++) {
            short a = as[i];
            short b = bs[i];
            if (ms[i % ms.length]) {
              rs[i] = (short)(a - b);
            } else {
              rs[i] = a;
            }
        }
        return rs;
    }



    @Benchmark
    public Object mul() {
        short[] as = fa.apply(size);
        short[] bs = fb.apply(size);
        short[] rs = fr.apply(size);

        for (int i = 0; i < as.length; i++) {
            short a = as[i];
            short b = bs[i];
            rs[i] = (short)(a * b);
        }

        return rs;
    }

    @Benchmark
    public Object mulMasked() {
        short[] as = fa.apply(size);
        short[] bs = fb.apply(size);
        short[] rs = fr.apply(size);
        boolean[] ms = fm.apply(size);

        for (int i = 0; i < as.length; i++) {
            short a = as[i];
            short b = bs[i];
            if (ms[i % ms.length]) {
              rs[i] = (short)(a * b);
            } else {
              rs[i] = a;
            }
        }
        return rs;
    }


    @Benchmark
    public Object and() {
        short[] as = fa.apply(size);
        short[] bs = fb.apply(size);
        short[] rs = fr.apply(size);

        for (int i = 0; i < as.length; i++) {
            short a = as[i];
            short b = bs[i];
            rs[i] = (short)(a & b);
        }

        return rs;
    }



    @Benchmark
    public Object andMasked() {
        short[] as = fa.apply(size);
        short[] bs = fb.apply(size);
        short[] rs = fr.apply(size);
        boolean[] ms = fm.apply(size);

        for (int i = 0; i < as.length; i++) {
            short a = as[i];
            short b = bs[i];
            if (ms[i % ms.length]) {
              rs[i] = (short)(a & b);
            } else {
              rs[i] = a;
            }
        }
        return rs;
    }



    @Benchmark
    public Object or() {
        short[] as = fa.apply(size);
        short[] bs = fb.apply(size);
        short[] rs = fr.apply(size);

        for (int i = 0; i < as.length; i++) {
            short a = as[i];
            short b = bs[i];
            rs[i] = (short)(a | b);
        }

        return rs;
    }



    @Benchmark
    public Object orMasked() {
        short[] as = fa.apply(size);
        short[] bs = fb.apply(size);
        short[] rs = fr.apply(size);
        boolean[] ms = fm.apply(size);

        for (int i = 0; i < as.length; i++) {
            short a = as[i];
            short b = bs[i];
            if (ms[i % ms.length]) {
              rs[i] = (short)(a | b);
            } else {
              rs[i] = a;
            }
        }
        return rs;
    }



    @Benchmark
    public Object xor() {
        short[] as = fa.apply(size);
        short[] bs = fb.apply(size);
        short[] rs = fr.apply(size);

        for (int i = 0; i < as.length; i++) {
            short a = as[i];
            short b = bs[i];
            rs[i] = (short)(a ^ b);
        }

        return rs;
    }



    @Benchmark
    public Object xorMasked() {
        short[] as = fa.apply(size);
        short[] bs = fb.apply(size);
        short[] rs = fr.apply(size);
        boolean[] ms = fm.apply(size);

        for (int i = 0; i < as.length; i++) {
            short a = as[i];
            short b = bs[i];
            if (ms[i % ms.length]) {
              rs[i] = (short)(a ^ b);
            } else {
              rs[i] = a;
            }
        }
        return rs;
    }









    @Benchmark
    public Object aShiftRShift() {
        short[] as = fa.apply(size);
        short[] bs = fb.apply(size);
        short[] rs = fr.apply(size);

        for (int i = 0; i < as.length; i++) {
            short a = as[i];
            short b = bs[i];
            rs[i] = (short)((a >> b));
        }

        return rs;
    }



    @Benchmark
    public Object aShiftRMaskedShift() {
        short[] as = fa.apply(size);
        short[] bs = fb.apply(size);
        short[] rs = fr.apply(size);
        boolean[] ms = fm.apply(size);

        for (int i = 0; i < as.length; i++) {
            short a = as[i];
            short b = bs[i];
            boolean m = ms[i % ms.length];
            rs[i] = (m ? (short)((a >> b)) : a);
        }

        return rs;
    }



    @Benchmark
    public Object shiftRShift() {
        short[] as = fa.apply(size);
        short[] bs = fb.apply(size);
        short[] rs = fr.apply(size);

        for (int i = 0; i < as.length; i++) {
            short a = as[i];
            short b = bs[i];
            rs[i] = (short)((a >>> b));
        }

        return rs;
    }



    @Benchmark
    public Object shiftRMaskedShift() {
        short[] as = fa.apply(size);
        short[] bs = fb.apply(size);
        short[] rs = fr.apply(size);
        boolean[] ms = fm.apply(size);

        for (int i = 0; i < as.length; i++) {
            short a = as[i];
            short b = bs[i];
            boolean m = ms[i % ms.length];
            rs[i] = (m ? (short)((a >>> b)) : a);
        }

        return rs;
    }



    @Benchmark
    public Object shiftLShift() {
        short[] as = fa.apply(size);
        short[] bs = fb.apply(size);
        short[] rs = fr.apply(size);

        for (int i = 0; i < as.length; i++) {
            short a = as[i];
            short b = bs[i];
            rs[i] = (short)((a << b));
        }

        return rs;
    }



    @Benchmark
    public Object shiftLMaskedShift() {
        short[] as = fa.apply(size);
        short[] bs = fb.apply(size);
        short[] rs = fr.apply(size);
        boolean[] ms = fm.apply(size);

        for (int i = 0; i < as.length; i++) {
            short a = as[i];
            short b = bs[i];
            boolean m = ms[i % ms.length];
            rs[i] = (m ? (short)((a << b)) : a);
        }

        return rs;
    }


    @Benchmark
    public Object max() {
        short[] as = fa.apply(size);
        short[] bs = fb.apply(size);
        short[] rs = fr.apply(size);

        for (int i = 0; i < as.length; i++) {
            short a = as[i];
            short b = bs[i];
            rs[i] = (short)((a > b) ? a : b);
        }

        return rs;
    }

    @Benchmark
    public Object min() {
        short[] as = fa.apply(size);
        short[] bs = fb.apply(size);
        short[] rs = fr.apply(size);

        for (int i = 0; i < as.length; i++) {
            short a = as[i];
            short b = bs[i];
            rs[i] = (short)((a < b) ? a : b);
        }

        return rs;
    }


    @Benchmark
    public short andAll() {
        short[] as = fa.apply(size);
        short r = -1;
        for (int i = 0; i < as.length; i++) {
          r &= as[i];
        }
        return r;
    }



    @Benchmark
    public short orAll() {
        short[] as = fa.apply(size);
        short r = 0;
        for (int i = 0; i < as.length; i++) {
          r |= as[i];
        }
        return r;
    }



    @Benchmark
    public short xorAll() {
        short[] as = fa.apply(size);
        short r = 0;
        for (int i = 0; i < as.length; i++) {
          r ^= as[i];
        }
        return r;
    }


    @Benchmark
    public short addAll() {
        short[] as = fa.apply(size);
        short r = 0;
        for (int i = 0; i < as.length; i++) {
          r += as[i];
        }
        return r;
    }

    @Benchmark
    public short subAll() {
        short[] as = fa.apply(size);
        short r = 0;
        for (int i = 0; i < as.length; i++) {
          r -= as[i];
        }
        return r;
    }

    @Benchmark
    public short mulAll() {
        short[] as = fa.apply(size);
        short r = 1;
        for (int i = 0; i < as.length; i++) {
          r *= as[i];
        }
        return r;
    }

    @Benchmark
    public short minAll() {
        short[] as = fa.apply(size);
        short r = Short.MAX_VALUE;
        for (int i = 0; i < as.length; i++) {
          r = as[i];
        }
        return r;
    }

    @Benchmark
    public short maxAll() {
        short[] as = fa.apply(size);
        short r = Short.MIN_VALUE;
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
        short[] as = fa.apply(size);
        short[] bs = fb.apply(size);

        boolean r = false;
        for (int i = 0; i < as.length; i++) {
            boolean m = (as[i] < bs[i]);
            r |= m; // accumulate so JIT can't eliminate the computation
        }

        return r;
    }

    @Benchmark
    public boolean greaterThan() {
        short[] as = fa.apply(size);
        short[] bs = fb.apply(size);

        boolean r = false;
        for (int i = 0; i < as.length; i++) {
            boolean m = (as[i] > bs[i]);
            r |= m; // accumulate so JIT can't eliminate the computation
        }

        return r;
    }

    @Benchmark
    public boolean equal() {
        short[] as = fa.apply(size);
        short[] bs = fb.apply(size);

        boolean r = false;
        for (int i = 0; i < as.length; i++) {
            boolean m = (as[i] == bs[i]);
            r |= m; // accumulate so JIT can't eliminate the computation
        }

        return r;
    }

    @Benchmark
    public boolean notEqual() {
        short[] as = fa.apply(size);
        short[] bs = fb.apply(size);

        boolean r = false;
        for (int i = 0; i < as.length; i++) {
            boolean m = (as[i] != bs[i]);
            r |= m; // accumulate so JIT can't eliminate the computation
        }

        return r;
    }

    @Benchmark
    public boolean lessThanEq() {
        short[] as = fa.apply(size);
        short[] bs = fb.apply(size);

        boolean r = false;
        for (int i = 0; i < as.length; i++) {
            boolean m = (as[i] <= bs[i]);
            r |= m; // accumulate so JIT can't eliminate the computation
        }

        return r;
    }

    @Benchmark
    public boolean greaterThanEq() {
        short[] as = fa.apply(size);
        short[] bs = fb.apply(size);

        boolean r = false;
        for (int i = 0; i < as.length; i++) {
            boolean m = (as[i] >= bs[i]);
            r |= m; // accumulate so JIT can't eliminate the computation
        }

        return r;
    }

    @Benchmark
    public Object blend() {
        short[] as = fa.apply(size);
        short[] bs = fb.apply(size);
        short[] rs = fr.apply(size);
        boolean[] ms = fm.apply(size);

        for (int i = 0; i < as.length; i++) {
            short a = as[i];
            short b = bs[i];
            boolean m = ms[i % ms.length];
            rs[i] = (m ? b : a);
        }

        return rs;
    }
    Object rearrangeShared(int window) {
        short[] as = fa.apply(size);
        int[] order = fs.apply(size);
        short[] rs = fr.apply(size);

        for (int i = 0; i < as.length; i += window) {
            for (int j = 0; j < window; j++) {
                short a = as[i+j];
                int pos = order[j];
                rs[i + pos] = a;
            }
        }

        return rs;
    }

    @Benchmark
    public Object rearrange064() {
        int window = 64 / Short.SIZE;
        return rearrangeShared(window);
    }

    @Benchmark
    public Object rearrange128() {
        int window = 128 / Short.SIZE;
        return rearrangeShared(window);
    }

    @Benchmark
    public Object rearrange256() {
        int window = 256 / Short.SIZE;
        return rearrangeShared(window);
    }

    @Benchmark
    public Object rearrange512() {
        int window = 512 / Short.SIZE;
        return rearrangeShared(window);
    }





















    @Benchmark
    public Object neg() {
        short[] as = fa.apply(size);
        short[] rs = fr.apply(size);

        for (int i = 0; i < as.length; i++) {
            short a = as[i];
            rs[i] = (short)(-((short)a));
        }

        return rs;
    }

    @Benchmark
    public Object negMasked() {
        short[] as = fa.apply(size);
        short[] rs = fr.apply(size);
        boolean[] ms = fm.apply(size);

        for (int i = 0; i < as.length; i++) {
            short a = as[i];
            boolean m = ms[i % ms.length];
            rs[i] = (m ? (short)(-((short)a)) : a);
        }

        return rs;
    }

    @Benchmark
    public Object abs() {
        short[] as = fa.apply(size);
        short[] rs = fr.apply(size);

        for (int i = 0; i < as.length; i++) {
            short a = as[i];
            rs[i] = (short)(Math.abs((short)a));
        }

        return rs;
    }

    @Benchmark
    public Object absMasked() {
        short[] as = fa.apply(size);
        short[] rs = fr.apply(size);
        boolean[] ms = fm.apply(size);

        for (int i = 0; i < as.length; i++) {
            short a = as[i];
            boolean m = ms[i % ms.length];
            rs[i] = (m ? (short)(Math.abs((short)a)) : a);
        }

        return rs;
    }


    @Benchmark
    public Object not() {
        short[] as = fa.apply(size);
        short[] rs = fr.apply(size);

        for (int i = 0; i < as.length; i++) {
            short a = as[i];
            rs[i] = (short)(~((short)a));
        }

        return rs;
    }



    @Benchmark
    public Object notMasked() {
        short[] as = fa.apply(size);
        short[] rs = fr.apply(size);
        boolean[] ms = fm.apply(size);

        for (int i = 0; i < as.length; i++) {
            short a = as[i];
            boolean m = ms[i % ms.length];
            rs[i] = (m ? (short)(~((short)a)) : a);
        }

        return rs;
    }





}

