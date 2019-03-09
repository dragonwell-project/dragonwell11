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
public class ByteScalar extends AbstractVectorBenchmark {
    @Param("1024")
    int size;

    byte[] fill(IntFunction<Byte> f) {
        byte[] array = new byte[size];
        for (int i = 0; i < array.length; i++) {
            array[i] = f.apply(i);
        }
        return array;
    }

    byte[] as, bs, cs, rs;
    boolean[] ms, rms;
    int[] ss;

    @Setup
    public void init() {
        as = fill(i -> (byte)(2*i));
        bs = fill(i -> (byte)(i+1));
        cs = fill(i -> (byte)(i+5));
        rs = fill(i -> (byte)0);
        ms = fillMask(size, i -> (i % 2) == 0);
        rms = fillMask(size, i -> false);

        ss = fillInt(size, i -> RANDOM.nextInt(Math.max(i,1)));
    }

    final IntFunction<byte[]> fa = vl -> as;
    final IntFunction<byte[]> fb = vl -> bs;
    final IntFunction<byte[]> fc = vl -> cs;
    final IntFunction<byte[]> fr = vl -> rs;
    final IntFunction<boolean[]> fm = vl -> ms;
    final IntFunction<boolean[]> fmr = vl -> rms;
    final IntFunction<int[]> fs = vl -> ss;


    @Benchmark
    public Object add() {
        byte[] as = fa.apply(size);
        byte[] bs = fb.apply(size);
        byte[] rs = fr.apply(size);

        for (int i = 0; i < as.length; i++) {
            byte a = as[i];
            byte b = bs[i];
            rs[i] = (byte)(a + b);
        }

        return rs;
    }

    @Benchmark
    public Object addMasked() {
        byte[] as = fa.apply(size);
        byte[] bs = fb.apply(size);
        byte[] rs = fr.apply(size);
        boolean[] ms = fm.apply(size);

        for (int i = 0; i < as.length; i++) {
            byte a = as[i];
            byte b = bs[i];
            if (ms[i % ms.length]) {
              rs[i] = (byte)(a + b);
            } else {
              rs[i] = a;
            }
        }
        return rs;
    }

    @Benchmark
    public Object sub() {
        byte[] as = fa.apply(size);
        byte[] bs = fb.apply(size);
        byte[] rs = fr.apply(size);

        for (int i = 0; i < as.length; i++) {
            byte a = as[i];
            byte b = bs[i];
            rs[i] = (byte)(a - b);
        }

        return rs;
    }

    @Benchmark
    public Object subMasked() {
        byte[] as = fa.apply(size);
        byte[] bs = fb.apply(size);
        byte[] rs = fr.apply(size);
        boolean[] ms = fm.apply(size);

        for (int i = 0; i < as.length; i++) {
            byte a = as[i];
            byte b = bs[i];
            if (ms[i % ms.length]) {
              rs[i] = (byte)(a - b);
            } else {
              rs[i] = a;
            }
        }
        return rs;
    }



    @Benchmark
    public Object mul() {
        byte[] as = fa.apply(size);
        byte[] bs = fb.apply(size);
        byte[] rs = fr.apply(size);

        for (int i = 0; i < as.length; i++) {
            byte a = as[i];
            byte b = bs[i];
            rs[i] = (byte)(a * b);
        }

        return rs;
    }

    @Benchmark
    public Object mulMasked() {
        byte[] as = fa.apply(size);
        byte[] bs = fb.apply(size);
        byte[] rs = fr.apply(size);
        boolean[] ms = fm.apply(size);

        for (int i = 0; i < as.length; i++) {
            byte a = as[i];
            byte b = bs[i];
            if (ms[i % ms.length]) {
              rs[i] = (byte)(a * b);
            } else {
              rs[i] = a;
            }
        }
        return rs;
    }


    @Benchmark
    public Object and() {
        byte[] as = fa.apply(size);
        byte[] bs = fb.apply(size);
        byte[] rs = fr.apply(size);

        for (int i = 0; i < as.length; i++) {
            byte a = as[i];
            byte b = bs[i];
            rs[i] = (byte)(a & b);
        }

        return rs;
    }



    @Benchmark
    public Object andMasked() {
        byte[] as = fa.apply(size);
        byte[] bs = fb.apply(size);
        byte[] rs = fr.apply(size);
        boolean[] ms = fm.apply(size);

        for (int i = 0; i < as.length; i++) {
            byte a = as[i];
            byte b = bs[i];
            if (ms[i % ms.length]) {
              rs[i] = (byte)(a & b);
            } else {
              rs[i] = a;
            }
        }
        return rs;
    }



    @Benchmark
    public Object or() {
        byte[] as = fa.apply(size);
        byte[] bs = fb.apply(size);
        byte[] rs = fr.apply(size);

        for (int i = 0; i < as.length; i++) {
            byte a = as[i];
            byte b = bs[i];
            rs[i] = (byte)(a | b);
        }

        return rs;
    }



    @Benchmark
    public Object orMasked() {
        byte[] as = fa.apply(size);
        byte[] bs = fb.apply(size);
        byte[] rs = fr.apply(size);
        boolean[] ms = fm.apply(size);

        for (int i = 0; i < as.length; i++) {
            byte a = as[i];
            byte b = bs[i];
            if (ms[i % ms.length]) {
              rs[i] = (byte)(a | b);
            } else {
              rs[i] = a;
            }
        }
        return rs;
    }



    @Benchmark
    public Object xor() {
        byte[] as = fa.apply(size);
        byte[] bs = fb.apply(size);
        byte[] rs = fr.apply(size);

        for (int i = 0; i < as.length; i++) {
            byte a = as[i];
            byte b = bs[i];
            rs[i] = (byte)(a ^ b);
        }

        return rs;
    }



    @Benchmark
    public Object xorMasked() {
        byte[] as = fa.apply(size);
        byte[] bs = fb.apply(size);
        byte[] rs = fr.apply(size);
        boolean[] ms = fm.apply(size);

        for (int i = 0; i < as.length; i++) {
            byte a = as[i];
            byte b = bs[i];
            if (ms[i % ms.length]) {
              rs[i] = (byte)(a ^ b);
            } else {
              rs[i] = a;
            }
        }
        return rs;
    }









    @Benchmark
    public Object aShiftRShift() {
        byte[] as = fa.apply(size);
        byte[] bs = fb.apply(size);
        byte[] rs = fr.apply(size);

        for (int i = 0; i < as.length; i++) {
            byte a = as[i];
            byte b = bs[i];
            rs[i] = (byte)((a >> b));
        }

        return rs;
    }



    @Benchmark
    public Object aShiftRMaskedShift() {
        byte[] as = fa.apply(size);
        byte[] bs = fb.apply(size);
        byte[] rs = fr.apply(size);
        boolean[] ms = fm.apply(size);

        for (int i = 0; i < as.length; i++) {
            byte a = as[i];
            byte b = bs[i];
            boolean m = ms[i % ms.length];
            rs[i] = (m ? (byte)((a >> b)) : a);
        }

        return rs;
    }



    @Benchmark
    public Object shiftRShift() {
        byte[] as = fa.apply(size);
        byte[] bs = fb.apply(size);
        byte[] rs = fr.apply(size);

        for (int i = 0; i < as.length; i++) {
            byte a = as[i];
            byte b = bs[i];
            rs[i] = (byte)((a >>> b));
        }

        return rs;
    }



    @Benchmark
    public Object shiftRMaskedShift() {
        byte[] as = fa.apply(size);
        byte[] bs = fb.apply(size);
        byte[] rs = fr.apply(size);
        boolean[] ms = fm.apply(size);

        for (int i = 0; i < as.length; i++) {
            byte a = as[i];
            byte b = bs[i];
            boolean m = ms[i % ms.length];
            rs[i] = (m ? (byte)((a >>> b)) : a);
        }

        return rs;
    }



    @Benchmark
    public Object shiftLShift() {
        byte[] as = fa.apply(size);
        byte[] bs = fb.apply(size);
        byte[] rs = fr.apply(size);

        for (int i = 0; i < as.length; i++) {
            byte a = as[i];
            byte b = bs[i];
            rs[i] = (byte)((a << b));
        }

        return rs;
    }



    @Benchmark
    public Object shiftLMaskedShift() {
        byte[] as = fa.apply(size);
        byte[] bs = fb.apply(size);
        byte[] rs = fr.apply(size);
        boolean[] ms = fm.apply(size);

        for (int i = 0; i < as.length; i++) {
            byte a = as[i];
            byte b = bs[i];
            boolean m = ms[i % ms.length];
            rs[i] = (m ? (byte)((a << b)) : a);
        }

        return rs;
    }


    @Benchmark
    public Object max() {
        byte[] as = fa.apply(size);
        byte[] bs = fb.apply(size);
        byte[] rs = fr.apply(size);

        for (int i = 0; i < as.length; i++) {
            byte a = as[i];
            byte b = bs[i];
            rs[i] = (byte)((a > b) ? a : b);
        }

        return rs;
    }

    @Benchmark
    public Object min() {
        byte[] as = fa.apply(size);
        byte[] bs = fb.apply(size);
        byte[] rs = fr.apply(size);

        for (int i = 0; i < as.length; i++) {
            byte a = as[i];
            byte b = bs[i];
            rs[i] = (byte)((a < b) ? a : b);
        }

        return rs;
    }


    @Benchmark
    public byte andAll() {
        byte[] as = fa.apply(size);
        byte r = -1;
        for (int i = 0; i < as.length; i++) {
          r &= as[i];
        }
        return r;
    }



    @Benchmark
    public byte orAll() {
        byte[] as = fa.apply(size);
        byte r = 0;
        for (int i = 0; i < as.length; i++) {
          r |= as[i];
        }
        return r;
    }



    @Benchmark
    public byte xorAll() {
        byte[] as = fa.apply(size);
        byte r = 0;
        for (int i = 0; i < as.length; i++) {
          r ^= as[i];
        }
        return r;
    }


    @Benchmark
    public byte addAll() {
        byte[] as = fa.apply(size);
        byte r = 0;
        for (int i = 0; i < as.length; i++) {
          r += as[i];
        }
        return r;
    }

    @Benchmark
    public byte subAll() {
        byte[] as = fa.apply(size);
        byte r = 0;
        for (int i = 0; i < as.length; i++) {
          r -= as[i];
        }
        return r;
    }

    @Benchmark
    public byte mulAll() {
        byte[] as = fa.apply(size);
        byte r = 1;
        for (int i = 0; i < as.length; i++) {
          r *= as[i];
        }
        return r;
    }

    @Benchmark
    public byte minAll() {
        byte[] as = fa.apply(size);
        byte r = Byte.MAX_VALUE;
        for (int i = 0; i < as.length; i++) {
          r = as[i];
        }
        return r;
    }

    @Benchmark
    public byte maxAll() {
        byte[] as = fa.apply(size);
        byte r = Byte.MIN_VALUE;
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
        byte[] as = fa.apply(size);
        byte[] bs = fb.apply(size);

        boolean r = false;
        for (int i = 0; i < as.length; i++) {
            boolean m = (as[i] < bs[i]);
            r |= m; // accumulate so JIT can't eliminate the computation
        }

        return r;
    }

    @Benchmark
    public boolean greaterThan() {
        byte[] as = fa.apply(size);
        byte[] bs = fb.apply(size);

        boolean r = false;
        for (int i = 0; i < as.length; i++) {
            boolean m = (as[i] > bs[i]);
            r |= m; // accumulate so JIT can't eliminate the computation
        }

        return r;
    }

    @Benchmark
    public boolean equal() {
        byte[] as = fa.apply(size);
        byte[] bs = fb.apply(size);

        boolean r = false;
        for (int i = 0; i < as.length; i++) {
            boolean m = (as[i] == bs[i]);
            r |= m; // accumulate so JIT can't eliminate the computation
        }

        return r;
    }

    @Benchmark
    public boolean notEqual() {
        byte[] as = fa.apply(size);
        byte[] bs = fb.apply(size);

        boolean r = false;
        for (int i = 0; i < as.length; i++) {
            boolean m = (as[i] != bs[i]);
            r |= m; // accumulate so JIT can't eliminate the computation
        }

        return r;
    }

    @Benchmark
    public boolean lessThanEq() {
        byte[] as = fa.apply(size);
        byte[] bs = fb.apply(size);

        boolean r = false;
        for (int i = 0; i < as.length; i++) {
            boolean m = (as[i] <= bs[i]);
            r |= m; // accumulate so JIT can't eliminate the computation
        }

        return r;
    }

    @Benchmark
    public boolean greaterThanEq() {
        byte[] as = fa.apply(size);
        byte[] bs = fb.apply(size);

        boolean r = false;
        for (int i = 0; i < as.length; i++) {
            boolean m = (as[i] >= bs[i]);
            r |= m; // accumulate so JIT can't eliminate the computation
        }

        return r;
    }

    @Benchmark
    public Object blend() {
        byte[] as = fa.apply(size);
        byte[] bs = fb.apply(size);
        byte[] rs = fr.apply(size);
        boolean[] ms = fm.apply(size);

        for (int i = 0; i < as.length; i++) {
            byte a = as[i];
            byte b = bs[i];
            boolean m = ms[i % ms.length];
            rs[i] = (m ? b : a);
        }

        return rs;
    }
    Object rearrangeShared(int window) {
        byte[] as = fa.apply(size);
        int[] order = fs.apply(size);
        byte[] rs = fr.apply(size);

        for (int i = 0; i < as.length; i += window) {
            for (int j = 0; j < window; j++) {
                byte a = as[i+j];
                int pos = order[j];
                rs[i + pos] = a;
            }
        }

        return rs;
    }

    @Benchmark
    public Object rearrange064() {
        int window = 64 / Byte.SIZE;
        return rearrangeShared(window);
    }

    @Benchmark
    public Object rearrange128() {
        int window = 128 / Byte.SIZE;
        return rearrangeShared(window);
    }

    @Benchmark
    public Object rearrange256() {
        int window = 256 / Byte.SIZE;
        return rearrangeShared(window);
    }

    @Benchmark
    public Object rearrange512() {
        int window = 512 / Byte.SIZE;
        return rearrangeShared(window);
    }





















    @Benchmark
    public Object neg() {
        byte[] as = fa.apply(size);
        byte[] rs = fr.apply(size);

        for (int i = 0; i < as.length; i++) {
            byte a = as[i];
            rs[i] = (byte)(-((byte)a));
        }

        return rs;
    }

    @Benchmark
    public Object negMasked() {
        byte[] as = fa.apply(size);
        byte[] rs = fr.apply(size);
        boolean[] ms = fm.apply(size);

        for (int i = 0; i < as.length; i++) {
            byte a = as[i];
            boolean m = ms[i % ms.length];
            rs[i] = (m ? (byte)(-((byte)a)) : a);
        }

        return rs;
    }

    @Benchmark
    public Object abs() {
        byte[] as = fa.apply(size);
        byte[] rs = fr.apply(size);

        for (int i = 0; i < as.length; i++) {
            byte a = as[i];
            rs[i] = (byte)(Math.abs((byte)a));
        }

        return rs;
    }

    @Benchmark
    public Object absMasked() {
        byte[] as = fa.apply(size);
        byte[] rs = fr.apply(size);
        boolean[] ms = fm.apply(size);

        for (int i = 0; i < as.length; i++) {
            byte a = as[i];
            boolean m = ms[i % ms.length];
            rs[i] = (m ? (byte)(Math.abs((byte)a)) : a);
        }

        return rs;
    }


    @Benchmark
    public Object not() {
        byte[] as = fa.apply(size);
        byte[] rs = fr.apply(size);

        for (int i = 0; i < as.length; i++) {
            byte a = as[i];
            rs[i] = (byte)(~((byte)a));
        }

        return rs;
    }



    @Benchmark
    public Object notMasked() {
        byte[] as = fa.apply(size);
        byte[] rs = fr.apply(size);
        boolean[] ms = fm.apply(size);

        for (int i = 0; i < as.length; i++) {
            byte a = as[i];
            boolean m = ms[i % ms.length];
            rs[i] = (m ? (byte)(~((byte)a)) : a);
        }

        return rs;
    }





}

