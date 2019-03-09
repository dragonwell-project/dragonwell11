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

import jdk.panama.vector.Vector;
import jdk.panama.vector.Vector.Shape;
import jdk.panama.vector.FloatVector;

import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;
import java.util.function.IntFunction;

import org.openjdk.jmh.annotations.*;

@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Benchmark)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(value = 1, jvmArgsPrepend = {"--add-modules=jdk.panama.vector"})
public class Float64Vector extends AbstractVectorBenchmark {
    static final FloatVector.FloatSpecies SPECIES = FloatVector.species(Shape.S_64_BIT);

    static final int INVOC_COUNT = 1; // get rid of outer loop

    @Param("1024")
    int size;

    float[] fill(IntFunction<Float> f) {
        float[] array = new float[size];
        for (int i = 0; i < array.length; i++) {
            array[i] = f.apply(i);
        }
        return array;
    }

    float[] a, b, c, r;
    boolean[] m, rm;
    int[] s;

    @Setup
    public void init() {
        size += size % SPECIES.length(); // FIXME: add post-loops

        a = fill(i -> (float)(2*i));
        b = fill(i -> (float)(i+1));
        c = fill(i -> (float)(i+5));
        r = fill(i -> (float)0);

        m = fillMask(size, i -> (i % 2) == 0);
        rm = fillMask(size, i -> false);

        s = fillInt(size, i -> RANDOM.nextInt(SPECIES.length()));
    }

    final IntFunction<float[]> fa = vl -> a;
    final IntFunction<float[]> fb = vl -> b;
    final IntFunction<float[]> fc = vl -> c;
    final IntFunction<float[]> fr = vl -> r;
    final IntFunction<boolean[]> fm = vl -> m;
    final IntFunction<boolean[]> fmr = vl -> rm;
    final BiFunction<Integer,Integer,int[]> fs = (i,j) -> s;


    @Benchmark
    public Object add() {
        float[] a = fa.apply(SPECIES.length());
        float[] b = fb.apply(SPECIES.length());
        float[] r = fr.apply(SPECIES.length());

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                FloatVector av = SPECIES.fromArray(a, i);
                FloatVector bv = SPECIES.fromArray(b, i);
                av.add(bv).intoArray(r, i);
            }
        }

        return r;
    }

    @Benchmark
    public Object addMasked() {
        float[] a = fa.apply(SPECIES.length());
        float[] b = fb.apply(SPECIES.length());
        float[] r = fr.apply(SPECIES.length());
        boolean[] mask = fm.apply(SPECIES.length());
        Vector.Mask<Float> vmask = SPECIES.maskFromValues(mask);

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                FloatVector av = SPECIES.fromArray(a, i);
                FloatVector bv = SPECIES.fromArray(b, i);
                av.add(bv, vmask).intoArray(r, i);
            }
        }

        return r;
    }

    @Benchmark
    public Object sub() {
        float[] a = fa.apply(SPECIES.length());
        float[] b = fb.apply(SPECIES.length());
        float[] r = fr.apply(SPECIES.length());

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                FloatVector av = SPECIES.fromArray(a, i);
                FloatVector bv = SPECIES.fromArray(b, i);
                av.sub(bv).intoArray(r, i);
            }
        }

        return r;
    }

    @Benchmark
    public Object subMasked() {
        float[] a = fa.apply(SPECIES.length());
        float[] b = fb.apply(SPECIES.length());
        float[] r = fr.apply(SPECIES.length());
        boolean[] mask = fm.apply(SPECIES.length());
        Vector.Mask<Float> vmask = SPECIES.maskFromValues(mask);

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                FloatVector av = SPECIES.fromArray(a, i);
                FloatVector bv = SPECIES.fromArray(b, i);
                av.sub(bv, vmask).intoArray(r, i);
            }
        }

        return r;
    }


    @Benchmark
    public Object div() {
        float[] a = fa.apply(SPECIES.length());
        float[] b = fb.apply(SPECIES.length());
        float[] r = fr.apply(SPECIES.length());

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                FloatVector av = SPECIES.fromArray(a, i);
                FloatVector bv = SPECIES.fromArray(b, i);
                av.div(bv).intoArray(r, i);
            }
        }

        return r;
    }



    @Benchmark
    public Object divMasked() {
        float[] a = fa.apply(SPECIES.length());
        float[] b = fb.apply(SPECIES.length());
        float[] r = fr.apply(SPECIES.length());
        boolean[] mask = fm.apply(SPECIES.length());
        Vector.Mask<Float> vmask = SPECIES.maskFromValues(mask);

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                FloatVector av = SPECIES.fromArray(a, i);
                FloatVector bv = SPECIES.fromArray(b, i);
                av.div(bv, vmask).intoArray(r, i);
            }
        }

        return r;
    }


    @Benchmark
    public Object mul() {
        float[] a = fa.apply(SPECIES.length());
        float[] b = fb.apply(SPECIES.length());
        float[] r = fr.apply(SPECIES.length());

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                FloatVector av = SPECIES.fromArray(a, i);
                FloatVector bv = SPECIES.fromArray(b, i);
                av.mul(bv).intoArray(r, i);
            }
        }

        return r;
    }

    @Benchmark
    public Object mulMasked() {
        float[] a = fa.apply(SPECIES.length());
        float[] b = fb.apply(SPECIES.length());
        float[] r = fr.apply(SPECIES.length());
        boolean[] mask = fm.apply(SPECIES.length());
        Vector.Mask<Float> vmask = SPECIES.maskFromValues(mask);

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                FloatVector av = SPECIES.fromArray(a, i);
                FloatVector bv = SPECIES.fromArray(b, i);
                av.mul(bv, vmask).intoArray(r, i);
            }
        }

        return r;
    }



















    @Benchmark
    public Object max() {
        float[] a = fa.apply(SPECIES.length());
        float[] b = fb.apply(SPECIES.length());
        float[] r = fr.apply(SPECIES.length());

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                FloatVector av = SPECIES.fromArray(a, i);
                FloatVector bv = SPECIES.fromArray(b, i);
                av.max(bv).intoArray(r, i);
            }
        }

        return r;
    }

    @Benchmark
    public Object min() {
        float[] a = fa.apply(SPECIES.length());
        float[] b = fb.apply(SPECIES.length());
        float[] r = fr.apply(SPECIES.length());

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                FloatVector av = SPECIES.fromArray(a, i);
                FloatVector bv = SPECIES.fromArray(b, i);
                av.min(bv).intoArray(r, i);
            }
        }

        return r;
    }




    @Benchmark
    public Object addAll() {
        float[] a = fa.apply(SPECIES.length());
        float[] r = fr.apply(SPECIES.length());

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
              FloatVector av = SPECIES.fromArray(a, i);
              r[i] = av.addAll();
            }
        }

        return r;
    }

    @Benchmark
    public Object subAll() {
        float[] a = fa.apply(SPECIES.length());
        float[] r = fr.apply(SPECIES.length());

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
              FloatVector av = SPECIES.fromArray(a, i);
              r[i] = av.subAll();
            }
        }

        return r;
    }

    @Benchmark
    public Object mulAll() {
        float[] a = fa.apply(SPECIES.length());
        float[] r = fr.apply(SPECIES.length());

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
              FloatVector av = SPECIES.fromArray(a, i);
              r[i] = av.mulAll();
            }
        }

        return r;
    }

    @Benchmark
    public Object minAll() {
        float[] a = fa.apply(SPECIES.length());
        float[] r = fr.apply(SPECIES.length());

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
              FloatVector av = SPECIES.fromArray(a, i);
              r[i] = av.minAll();
            }
        }

        return r;
    }

    @Benchmark
    public Object maxAll() {
        float[] a = fa.apply(SPECIES.length());
        float[] r = fr.apply(SPECIES.length());

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
              FloatVector av = SPECIES.fromArray(a, i);
              r[i] = av.maxAll();
            }
        }

        return r;
    }



    @Benchmark
    public Object with() {
        float[] a = fa.apply(SPECIES.length());
        float[] r = fr.apply(SPECIES.length());

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
              FloatVector av = SPECIES.fromArray(a, i);
              av.with(0, (float)4).intoArray(r, i);
            }
        }

        return r;
    }

    @Benchmark
    public Object lessThan() {
        float[] a = fa.apply(size);
        float[] b = fb.apply(size);
        boolean[] ms = fm.apply(size);
        Vector.Mask<Float> m = SPECIES.maskFromArray(ms, 0);

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                FloatVector av = SPECIES.fromArray(a, i);
                FloatVector bv = SPECIES.fromArray(b, i);
                Vector.Mask<Float> mv = av.lessThan(bv);

                m = m.and(mv); // accumulate results, so JIT can't eliminate relevant computations
            }
        }
        return m;
    }


    @Benchmark
    public Object greaterThan() {
        float[] a = fa.apply(size);
        float[] b = fb.apply(size);
        boolean[] ms = fm.apply(size);
        Vector.Mask<Float> m = SPECIES.maskFromArray(ms, 0);

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                FloatVector av = SPECIES.fromArray(a, i);
                FloatVector bv = SPECIES.fromArray(b, i);
                Vector.Mask<Float> mv = av.greaterThan(bv);

                m = m.and(mv); // accumulate results, so JIT can't eliminate relevant computations
            }
        }
        return m;
    }


    @Benchmark
    public Object equal() {
        float[] a = fa.apply(size);
        float[] b = fb.apply(size);
        boolean[] ms = fm.apply(size);
        Vector.Mask<Float> m = SPECIES.maskFromArray(ms, 0);

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                FloatVector av = SPECIES.fromArray(a, i);
                FloatVector bv = SPECIES.fromArray(b, i);
                Vector.Mask<Float> mv = av.equal(bv);

                m = m.and(mv); // accumulate results, so JIT can't eliminate relevant computations
            }
        }
        return m;
    }


    @Benchmark
    public Object notEqual() {
        float[] a = fa.apply(size);
        float[] b = fb.apply(size);
        boolean[] ms = fm.apply(size);
        Vector.Mask<Float> m = SPECIES.maskFromArray(ms, 0);

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                FloatVector av = SPECIES.fromArray(a, i);
                FloatVector bv = SPECIES.fromArray(b, i);
                Vector.Mask<Float> mv = av.notEqual(bv);

                m = m.and(mv); // accumulate results, so JIT can't eliminate relevant computations
            }
        }
        return m;
    }


    @Benchmark
    public Object lessThanEq() {
        float[] a = fa.apply(size);
        float[] b = fb.apply(size);
        boolean[] ms = fm.apply(size);
        Vector.Mask<Float> m = SPECIES.maskFromArray(ms, 0);

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                FloatVector av = SPECIES.fromArray(a, i);
                FloatVector bv = SPECIES.fromArray(b, i);
                Vector.Mask<Float> mv = av.lessThanEq(bv);

                m = m.and(mv); // accumulate results, so JIT can't eliminate relevant computations
            }
        }
        return m;
    }


    @Benchmark
    public Object greaterThanEq() {
        float[] a = fa.apply(size);
        float[] b = fb.apply(size);
        boolean[] ms = fm.apply(size);
        Vector.Mask<Float> m = SPECIES.maskFromArray(ms, 0);

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                FloatVector av = SPECIES.fromArray(a, i);
                FloatVector bv = SPECIES.fromArray(b, i);
                Vector.Mask<Float> mv = av.greaterThanEq(bv);

                m = m.and(mv); // accumulate results, so JIT can't eliminate relevant computations
            }
        }
        return m;
    }


    @Benchmark
    public Object blend() {
        float[] a = fa.apply(SPECIES.length());
        float[] b = fb.apply(SPECIES.length());
        float[] r = fr.apply(SPECIES.length());
        boolean[] mask = fm.apply(SPECIES.length());
        Vector.Mask<Float> vmask = SPECIES.maskFromValues(mask);

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                FloatVector av = SPECIES.fromArray(a, i);
                FloatVector bv = SPECIES.fromArray(b, i);
                av.blend(bv, vmask).intoArray(r, i);
            }
        }

        return r;
    }

    @Benchmark
    public Object rearrange() {
        float[] a = fa.apply(SPECIES.length());
        int[] order = fs.apply(a.length, SPECIES.length());
        float[] r = fr.apply(SPECIES.length());

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                FloatVector av = SPECIES.fromArray(a, i);
                av.rearrange(SPECIES.shuffleFromArray(order, i)).intoArray(r, i);
            }
        }

        return r;
    }

    @Benchmark
    public Object extract() {
        float[] a = fa.apply(SPECIES.length());
        float[] r = fr.apply(SPECIES.length());

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                FloatVector av = SPECIES.fromArray(a, i);
                int num_lanes = SPECIES.length();
                // Manually unroll because full unroll happens after intrinsification.
                // Unroll is needed because get intrinsic requires for index to be a known constant.
                if (num_lanes == 1) {
                    r[i]=av.get(0);
                } else if (num_lanes == 2) {
                    r[i]=av.get(0);
                    r[i+1]=av.get(1);
                } else if (num_lanes == 4) {
                    r[i]=av.get(0);
                    r[i+1]=av.get(1);
                    r[i+2]=av.get(2);
                    r[i+3]=av.get(3);
                } else if (num_lanes == 8) {
                    r[i]=av.get(0);
                    r[i+1]=av.get(1);
                    r[i+2]=av.get(2);
                    r[i+3]=av.get(3);
                    r[i+4]=av.get(4);
                    r[i+5]=av.get(5);
                    r[i+6]=av.get(6);
                    r[i+7]=av.get(7);
                } else if (num_lanes == 16) {
                    r[i]=av.get(0);
                    r[i+1]=av.get(1);
                    r[i+2]=av.get(2);
                    r[i+3]=av.get(3);
                    r[i+4]=av.get(4);
                    r[i+5]=av.get(5);
                    r[i+6]=av.get(6);
                    r[i+7]=av.get(7);
                    r[i+8]=av.get(8);
                    r[i+9]=av.get(9);
                    r[i+10]=av.get(10);
                    r[i+11]=av.get(11);
                    r[i+12]=av.get(12);
                    r[i+13]=av.get(13);
                    r[i+14]=av.get(14);
                    r[i+15]=av.get(15);
                } else if (num_lanes == 32) {
                    r[i]=av.get(0);
                    r[i+1]=av.get(1);
                    r[i+2]=av.get(2);
                    r[i+3]=av.get(3);
                    r[i+4]=av.get(4);
                    r[i+5]=av.get(5);
                    r[i+6]=av.get(6);
                    r[i+7]=av.get(7);
                    r[i+8]=av.get(8);
                    r[i+9]=av.get(9);
                    r[i+10]=av.get(10);
                    r[i+11]=av.get(11);
                    r[i+12]=av.get(12);
                    r[i+13]=av.get(13);
                    r[i+14]=av.get(14);
                    r[i+15]=av.get(15);
                    r[i+16]=av.get(16);
                    r[i+17]=av.get(17);
                    r[i+18]=av.get(18);
                    r[i+19]=av.get(19);
                    r[i+20]=av.get(20);
                    r[i+21]=av.get(21);
                    r[i+22]=av.get(22);
                    r[i+23]=av.get(23);
                    r[i+24]=av.get(24);
                    r[i+25]=av.get(25);
                    r[i+26]=av.get(26);
                    r[i+27]=av.get(27);
                    r[i+28]=av.get(28);
                    r[i+29]=av.get(29);
                    r[i+30]=av.get(30);
                    r[i+31]=av.get(31);
                } else if (num_lanes == 64) {
                    r[i]=av.get(0);
                    r[i+1]=av.get(1);
                    r[i+2]=av.get(2);
                    r[i+3]=av.get(3);
                    r[i+4]=av.get(4);
                    r[i+5]=av.get(5);
                    r[i+6]=av.get(6);
                    r[i+7]=av.get(7);
                    r[i+8]=av.get(8);
                    r[i+9]=av.get(9);
                    r[i+10]=av.get(10);
                    r[i+11]=av.get(11);
                    r[i+12]=av.get(12);
                    r[i+13]=av.get(13);
                    r[i+14]=av.get(14);
                    r[i+15]=av.get(15);
                    r[i+16]=av.get(16);
                    r[i+17]=av.get(17);
                    r[i+18]=av.get(18);
                    r[i+19]=av.get(19);
                    r[i+20]=av.get(20);
                    r[i+21]=av.get(21);
                    r[i+22]=av.get(22);
                    r[i+23]=av.get(23);
                    r[i+24]=av.get(24);
                    r[i+25]=av.get(25);
                    r[i+26]=av.get(26);
                    r[i+27]=av.get(27);
                    r[i+28]=av.get(28);
                    r[i+29]=av.get(29);
                    r[i+30]=av.get(30);
                    r[i+31]=av.get(31);
                    r[i+32]=av.get(32);
                    r[i+33]=av.get(33);
                    r[i+34]=av.get(34);
                    r[i+35]=av.get(35);
                    r[i+36]=av.get(36);
                    r[i+37]=av.get(37);
                    r[i+38]=av.get(38);
                    r[i+39]=av.get(39);
                    r[i+40]=av.get(40);
                    r[i+41]=av.get(41);
                    r[i+42]=av.get(42);
                    r[i+43]=av.get(43);
                    r[i+44]=av.get(44);
                    r[i+45]=av.get(45);
                    r[i+46]=av.get(46);
                    r[i+47]=av.get(47);
                    r[i+48]=av.get(48);
                    r[i+49]=av.get(49);
                    r[i+50]=av.get(50);
                    r[i+51]=av.get(51);
                    r[i+52]=av.get(52);
                    r[i+53]=av.get(53);
                    r[i+54]=av.get(54);
                    r[i+55]=av.get(55);
                    r[i+56]=av.get(56);
                    r[i+57]=av.get(57);
                    r[i+58]=av.get(58);
                    r[i+59]=av.get(59);
                    r[i+60]=av.get(60);
                    r[i+61]=av.get(61);
                    r[i+62]=av.get(62);
                    r[i+63]=av.get(63);
                } else {
                    for (int j = 0; j < SPECIES.length(); j++) {
                        r[i+j]=av.get(j);
                    }
                }
            }
        }

        return r;
    }


    @Benchmark
    public Object sin() {
        float[] a = fa.apply(SPECIES.length());
        float[] r = fr.apply(SPECIES.length());

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                FloatVector av = SPECIES.fromArray(a, i);
                av.sin().intoArray(r, i);
            }
        }

        return r;
    }



    @Benchmark
    public Object exp() {
        float[] a = fa.apply(SPECIES.length());
        float[] r = fr.apply(SPECIES.length());

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                FloatVector av = SPECIES.fromArray(a, i);
                av.exp().intoArray(r, i);
            }
        }

        return r;
    }



    @Benchmark
    public Object log1p() {
        float[] a = fa.apply(SPECIES.length());
        float[] r = fr.apply(SPECIES.length());

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                FloatVector av = SPECIES.fromArray(a, i);
                av.log1p().intoArray(r, i);
            }
        }

        return r;
    }



    @Benchmark
    public Object log() {
        float[] a = fa.apply(SPECIES.length());
        float[] r = fr.apply(SPECIES.length());

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                FloatVector av = SPECIES.fromArray(a, i);
                av.log().intoArray(r, i);
            }
        }

        return r;
    }



    @Benchmark
    public Object log10() {
        float[] a = fa.apply(SPECIES.length());
        float[] r = fr.apply(SPECIES.length());

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                FloatVector av = SPECIES.fromArray(a, i);
                av.log10().intoArray(r, i);
            }
        }

        return r;
    }



    @Benchmark
    public Object expm1() {
        float[] a = fa.apply(SPECIES.length());
        float[] r = fr.apply(SPECIES.length());

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                FloatVector av = SPECIES.fromArray(a, i);
                av.expm1().intoArray(r, i);
            }
        }

        return r;
    }



    @Benchmark
    public Object cos() {
        float[] a = fa.apply(SPECIES.length());
        float[] r = fr.apply(SPECIES.length());

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                FloatVector av = SPECIES.fromArray(a, i);
                av.cos().intoArray(r, i);
            }
        }

        return r;
    }



    @Benchmark
    public Object tan() {
        float[] a = fa.apply(SPECIES.length());
        float[] r = fr.apply(SPECIES.length());

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                FloatVector av = SPECIES.fromArray(a, i);
                av.tan().intoArray(r, i);
            }
        }

        return r;
    }



    @Benchmark
    public Object sinh() {
        float[] a = fa.apply(SPECIES.length());
        float[] r = fr.apply(SPECIES.length());

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                FloatVector av = SPECIES.fromArray(a, i);
                av.sinh().intoArray(r, i);
            }
        }

        return r;
    }



    @Benchmark
    public Object cosh() {
        float[] a = fa.apply(SPECIES.length());
        float[] r = fr.apply(SPECIES.length());

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                FloatVector av = SPECIES.fromArray(a, i);
                av.cosh().intoArray(r, i);
            }
        }

        return r;
    }



    @Benchmark
    public Object tanh() {
        float[] a = fa.apply(SPECIES.length());
        float[] r = fr.apply(SPECIES.length());

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                FloatVector av = SPECIES.fromArray(a, i);
                av.tanh().intoArray(r, i);
            }
        }

        return r;
    }



    @Benchmark
    public Object asin() {
        float[] a = fa.apply(SPECIES.length());
        float[] r = fr.apply(SPECIES.length());

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                FloatVector av = SPECIES.fromArray(a, i);
                av.asin().intoArray(r, i);
            }
        }

        return r;
    }



    @Benchmark
    public Object acos() {
        float[] a = fa.apply(SPECIES.length());
        float[] r = fr.apply(SPECIES.length());

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                FloatVector av = SPECIES.fromArray(a, i);
                av.acos().intoArray(r, i);
            }
        }

        return r;
    }



    @Benchmark
    public Object atan() {
        float[] a = fa.apply(SPECIES.length());
        float[] r = fr.apply(SPECIES.length());

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                FloatVector av = SPECIES.fromArray(a, i);
                av.atan().intoArray(r, i);
            }
        }

        return r;
    }



    @Benchmark
    public Object cbrt() {
        float[] a = fa.apply(SPECIES.length());
        float[] r = fr.apply(SPECIES.length());

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                FloatVector av = SPECIES.fromArray(a, i);
                av.cbrt().intoArray(r, i);
            }
        }

        return r;
    }



    @Benchmark
    public Object hypot() {
        float[] a = fa.apply(SPECIES.length());
        float[] b = fb.apply(SPECIES.length());
        float[] r = fr.apply(SPECIES.length());

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                FloatVector av = SPECIES.fromArray(a, i);
                FloatVector bv = SPECIES.fromArray(b, i);
                av.hypot(bv).intoArray(r, i);
            }
        }

        return r;
    }



    @Benchmark
    public Object pow() {
        float[] a = fa.apply(SPECIES.length());
        float[] b = fb.apply(SPECIES.length());
        float[] r = fr.apply(SPECIES.length());

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                FloatVector av = SPECIES.fromArray(a, i);
                FloatVector bv = SPECIES.fromArray(b, i);
                av.pow(bv).intoArray(r, i);
            }
        }

        return r;
    }



    @Benchmark
    public Object atan2() {
        float[] a = fa.apply(SPECIES.length());
        float[] b = fb.apply(SPECIES.length());
        float[] r = fr.apply(SPECIES.length());

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                FloatVector av = SPECIES.fromArray(a, i);
                FloatVector bv = SPECIES.fromArray(b, i);
                av.atan2(bv).intoArray(r, i);
            }
        }

        return r;
    }



    @Benchmark
    public Object fma() {
        float[] a = fa.apply(SPECIES.length());
        float[] b = fb.apply(SPECIES.length());
        float[] c = fc.apply(SPECIES.length());
        float[] r = fr.apply(SPECIES.length());

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                FloatVector av = SPECIES.fromArray(a, i);
                FloatVector bv = SPECIES.fromArray(b, i);
                FloatVector cv = SPECIES.fromArray(c, i);
                av.fma(bv, cv).intoArray(r, i);
            }
        }

        return r;
    }



    @Benchmark
    public Object fmaMasked() {
        float[] a = fa.apply(SPECIES.length());
        float[] b = fb.apply(SPECIES.length());
        float[] c = fc.apply(SPECIES.length());
        float[] r = fr.apply(SPECIES.length());
        boolean[] mask = fm.apply(SPECIES.length());
        Vector.Mask<Float> vmask = SPECIES.maskFromValues(mask);

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                FloatVector av = SPECIES.fromArray(a, i);
                FloatVector bv = SPECIES.fromArray(b, i);
                FloatVector cv = SPECIES.fromArray(c, i);
                av.fma(bv, cv, vmask).intoArray(r, i);
            }
        }

        return r;
    }


    @Benchmark
    public Object neg() {
        float[] a = fa.apply(SPECIES.length());
        float[] r = fr.apply(SPECIES.length());

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                FloatVector av = SPECIES.fromArray(a, i);
                av.neg().intoArray(r, i);
            }
        }

        return r;
    }

    @Benchmark
    public Object negMasked() {
        float[] a = fa.apply(SPECIES.length());
        float[] r = fr.apply(SPECIES.length());
        boolean[] mask = fm.apply(SPECIES.length());
        Vector.Mask<Float> vmask = SPECIES.maskFromValues(mask);

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                FloatVector av = SPECIES.fromArray(a, i);
                av.neg(vmask).intoArray(r, i);
            }
        }

        return r;
    }

    @Benchmark
    public Object abs() {
        float[] a = fa.apply(SPECIES.length());
        float[] r = fr.apply(SPECIES.length());

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                FloatVector av = SPECIES.fromArray(a, i);
                av.abs().intoArray(r, i);
            }
        }

        return r;
    }

    @Benchmark
    public Object absMasked() {
        float[] a = fa.apply(SPECIES.length());
        float[] r = fr.apply(SPECIES.length());
        boolean[] mask = fm.apply(SPECIES.length());
        Vector.Mask<Float> vmask = SPECIES.maskFromValues(mask);

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                FloatVector av = SPECIES.fromArray(a, i);
                av.abs(vmask).intoArray(r, i);
            }
        }

        return r;
    }




    @Benchmark
    public Object sqrt() {
        float[] a = fa.apply(SPECIES.length());
        float[] r = fr.apply(SPECIES.length());

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                FloatVector av = SPECIES.fromArray(a, i);
                av.sqrt().intoArray(r, i);
            }
        }

        return r;
    }



    @Benchmark
    public Object sqrtMasked() {
        float[] a = fa.apply(SPECIES.length());
        float[] r = fr.apply(SPECIES.length());
        boolean[] mask = fm.apply(SPECIES.length());
        Vector.Mask<Float> vmask = SPECIES.maskFromValues(mask);

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                FloatVector av = SPECIES.fromArray(a, i);
                av.sqrt(vmask).intoArray(r, i);
            }
        }

        return r;
    }



    @Benchmark
    public Object gather() {
        float[] a = fa.apply(SPECIES.length());
        int[] b    = fs.apply(a.length, SPECIES.length());
        float[] r = new float[a.length];

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                FloatVector av = SPECIES.fromArray(a, i, b, i);
                av.intoArray(r, i);
            }
        }

        return r;
    }



    @Benchmark
    public Object scatter() {
        float[] a = fa.apply(SPECIES.length());
        int[] b = fs.apply(a.length, SPECIES.length());
        float[] r = new float[a.length];

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                FloatVector av = SPECIES.fromArray(a, i);
                av.intoArray(r, i, b, i);
            }
        }

        return r;
    }

}

