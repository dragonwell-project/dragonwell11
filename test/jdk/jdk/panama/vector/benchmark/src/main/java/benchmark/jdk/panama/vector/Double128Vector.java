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
import jdk.panama.vector.DoubleVector;

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
public class Double128Vector extends AbstractVectorBenchmark {
    static final DoubleVector.DoubleSpecies SPECIES = DoubleVector.species(Shape.S_128_BIT);

    static final int INVOC_COUNT = 1; // get rid of outer loop

    @Param("1024")
    int size;

    double[] fill(IntFunction<Double> f) {
        double[] array = new double[size];
        for (int i = 0; i < array.length; i++) {
            array[i] = f.apply(i);
        }
        return array;
    }

    double[] a, b, c, r;
    boolean[] m, rm;
    int[] s;

    @Setup
    public void init() {
        size += size % SPECIES.length(); // FIXME: add post-loops

        a = fill(i -> (double)(2*i));
        b = fill(i -> (double)(i+1));
        c = fill(i -> (double)(i+5));
        r = fill(i -> (double)0);

        m = fillMask(size, i -> (i % 2) == 0);
        rm = fillMask(size, i -> false);

        s = fillInt(size, i -> RANDOM.nextInt(SPECIES.length()));
    }

    final IntFunction<double[]> fa = vl -> a;
    final IntFunction<double[]> fb = vl -> b;
    final IntFunction<double[]> fc = vl -> c;
    final IntFunction<double[]> fr = vl -> r;
    final IntFunction<boolean[]> fm = vl -> m;
    final IntFunction<boolean[]> fmr = vl -> rm;
    final BiFunction<Integer,Integer,int[]> fs = (i,j) -> s;


    @Benchmark
    public Object add() {
        double[] a = fa.apply(SPECIES.length());
        double[] b = fb.apply(SPECIES.length());
        double[] r = fr.apply(SPECIES.length());

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                DoubleVector av = SPECIES.fromArray(a, i);
                DoubleVector bv = SPECIES.fromArray(b, i);
                av.add(bv).intoArray(r, i);
            }
        }

        return r;
    }

    @Benchmark
    public Object addMasked() {
        double[] a = fa.apply(SPECIES.length());
        double[] b = fb.apply(SPECIES.length());
        double[] r = fr.apply(SPECIES.length());
        boolean[] mask = fm.apply(SPECIES.length());
        Vector.Mask<Double> vmask = SPECIES.maskFromValues(mask);

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                DoubleVector av = SPECIES.fromArray(a, i);
                DoubleVector bv = SPECIES.fromArray(b, i);
                av.add(bv, vmask).intoArray(r, i);
            }
        }

        return r;
    }

    @Benchmark
    public Object sub() {
        double[] a = fa.apply(SPECIES.length());
        double[] b = fb.apply(SPECIES.length());
        double[] r = fr.apply(SPECIES.length());

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                DoubleVector av = SPECIES.fromArray(a, i);
                DoubleVector bv = SPECIES.fromArray(b, i);
                av.sub(bv).intoArray(r, i);
            }
        }

        return r;
    }

    @Benchmark
    public Object subMasked() {
        double[] a = fa.apply(SPECIES.length());
        double[] b = fb.apply(SPECIES.length());
        double[] r = fr.apply(SPECIES.length());
        boolean[] mask = fm.apply(SPECIES.length());
        Vector.Mask<Double> vmask = SPECIES.maskFromValues(mask);

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                DoubleVector av = SPECIES.fromArray(a, i);
                DoubleVector bv = SPECIES.fromArray(b, i);
                av.sub(bv, vmask).intoArray(r, i);
            }
        }

        return r;
    }


    @Benchmark
    public Object div() {
        double[] a = fa.apply(SPECIES.length());
        double[] b = fb.apply(SPECIES.length());
        double[] r = fr.apply(SPECIES.length());

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                DoubleVector av = SPECIES.fromArray(a, i);
                DoubleVector bv = SPECIES.fromArray(b, i);
                av.div(bv).intoArray(r, i);
            }
        }

        return r;
    }



    @Benchmark
    public Object divMasked() {
        double[] a = fa.apply(SPECIES.length());
        double[] b = fb.apply(SPECIES.length());
        double[] r = fr.apply(SPECIES.length());
        boolean[] mask = fm.apply(SPECIES.length());
        Vector.Mask<Double> vmask = SPECIES.maskFromValues(mask);

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                DoubleVector av = SPECIES.fromArray(a, i);
                DoubleVector bv = SPECIES.fromArray(b, i);
                av.div(bv, vmask).intoArray(r, i);
            }
        }

        return r;
    }


    @Benchmark
    public Object mul() {
        double[] a = fa.apply(SPECIES.length());
        double[] b = fb.apply(SPECIES.length());
        double[] r = fr.apply(SPECIES.length());

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                DoubleVector av = SPECIES.fromArray(a, i);
                DoubleVector bv = SPECIES.fromArray(b, i);
                av.mul(bv).intoArray(r, i);
            }
        }

        return r;
    }

    @Benchmark
    public Object mulMasked() {
        double[] a = fa.apply(SPECIES.length());
        double[] b = fb.apply(SPECIES.length());
        double[] r = fr.apply(SPECIES.length());
        boolean[] mask = fm.apply(SPECIES.length());
        Vector.Mask<Double> vmask = SPECIES.maskFromValues(mask);

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                DoubleVector av = SPECIES.fromArray(a, i);
                DoubleVector bv = SPECIES.fromArray(b, i);
                av.mul(bv, vmask).intoArray(r, i);
            }
        }

        return r;
    }



















    @Benchmark
    public Object max() {
        double[] a = fa.apply(SPECIES.length());
        double[] b = fb.apply(SPECIES.length());
        double[] r = fr.apply(SPECIES.length());

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                DoubleVector av = SPECIES.fromArray(a, i);
                DoubleVector bv = SPECIES.fromArray(b, i);
                av.max(bv).intoArray(r, i);
            }
        }

        return r;
    }

    @Benchmark
    public Object min() {
        double[] a = fa.apply(SPECIES.length());
        double[] b = fb.apply(SPECIES.length());
        double[] r = fr.apply(SPECIES.length());

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                DoubleVector av = SPECIES.fromArray(a, i);
                DoubleVector bv = SPECIES.fromArray(b, i);
                av.min(bv).intoArray(r, i);
            }
        }

        return r;
    }




    @Benchmark
    public Object addAll() {
        double[] a = fa.apply(SPECIES.length());
        double[] r = fr.apply(SPECIES.length());

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
              DoubleVector av = SPECIES.fromArray(a, i);
              r[i] = av.addAll();
            }
        }

        return r;
    }

    @Benchmark
    public Object subAll() {
        double[] a = fa.apply(SPECIES.length());
        double[] r = fr.apply(SPECIES.length());

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
              DoubleVector av = SPECIES.fromArray(a, i);
              r[i] = av.subAll();
            }
        }

        return r;
    }

    @Benchmark
    public Object mulAll() {
        double[] a = fa.apply(SPECIES.length());
        double[] r = fr.apply(SPECIES.length());

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
              DoubleVector av = SPECIES.fromArray(a, i);
              r[i] = av.mulAll();
            }
        }

        return r;
    }

    @Benchmark
    public Object minAll() {
        double[] a = fa.apply(SPECIES.length());
        double[] r = fr.apply(SPECIES.length());

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
              DoubleVector av = SPECIES.fromArray(a, i);
              r[i] = av.minAll();
            }
        }

        return r;
    }

    @Benchmark
    public Object maxAll() {
        double[] a = fa.apply(SPECIES.length());
        double[] r = fr.apply(SPECIES.length());

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
              DoubleVector av = SPECIES.fromArray(a, i);
              r[i] = av.maxAll();
            }
        }

        return r;
    }



    @Benchmark
    public Object with() {
        double[] a = fa.apply(SPECIES.length());
        double[] r = fr.apply(SPECIES.length());

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
              DoubleVector av = SPECIES.fromArray(a, i);
              av.with(0, (double)4).intoArray(r, i);
            }
        }

        return r;
    }

    @Benchmark
    public Object lessThan() {
        double[] a = fa.apply(size);
        double[] b = fb.apply(size);
        boolean[] ms = fm.apply(size);
        Vector.Mask<Double> m = SPECIES.maskFromArray(ms, 0);

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                DoubleVector av = SPECIES.fromArray(a, i);
                DoubleVector bv = SPECIES.fromArray(b, i);
                Vector.Mask<Double> mv = av.lessThan(bv);

                m = m.and(mv); // accumulate results, so JIT can't eliminate relevant computations
            }
        }
        return m;
    }


    @Benchmark
    public Object greaterThan() {
        double[] a = fa.apply(size);
        double[] b = fb.apply(size);
        boolean[] ms = fm.apply(size);
        Vector.Mask<Double> m = SPECIES.maskFromArray(ms, 0);

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                DoubleVector av = SPECIES.fromArray(a, i);
                DoubleVector bv = SPECIES.fromArray(b, i);
                Vector.Mask<Double> mv = av.greaterThan(bv);

                m = m.and(mv); // accumulate results, so JIT can't eliminate relevant computations
            }
        }
        return m;
    }


    @Benchmark
    public Object equal() {
        double[] a = fa.apply(size);
        double[] b = fb.apply(size);
        boolean[] ms = fm.apply(size);
        Vector.Mask<Double> m = SPECIES.maskFromArray(ms, 0);

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                DoubleVector av = SPECIES.fromArray(a, i);
                DoubleVector bv = SPECIES.fromArray(b, i);
                Vector.Mask<Double> mv = av.equal(bv);

                m = m.and(mv); // accumulate results, so JIT can't eliminate relevant computations
            }
        }
        return m;
    }


    @Benchmark
    public Object notEqual() {
        double[] a = fa.apply(size);
        double[] b = fb.apply(size);
        boolean[] ms = fm.apply(size);
        Vector.Mask<Double> m = SPECIES.maskFromArray(ms, 0);

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                DoubleVector av = SPECIES.fromArray(a, i);
                DoubleVector bv = SPECIES.fromArray(b, i);
                Vector.Mask<Double> mv = av.notEqual(bv);

                m = m.and(mv); // accumulate results, so JIT can't eliminate relevant computations
            }
        }
        return m;
    }


    @Benchmark
    public Object lessThanEq() {
        double[] a = fa.apply(size);
        double[] b = fb.apply(size);
        boolean[] ms = fm.apply(size);
        Vector.Mask<Double> m = SPECIES.maskFromArray(ms, 0);

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                DoubleVector av = SPECIES.fromArray(a, i);
                DoubleVector bv = SPECIES.fromArray(b, i);
                Vector.Mask<Double> mv = av.lessThanEq(bv);

                m = m.and(mv); // accumulate results, so JIT can't eliminate relevant computations
            }
        }
        return m;
    }


    @Benchmark
    public Object greaterThanEq() {
        double[] a = fa.apply(size);
        double[] b = fb.apply(size);
        boolean[] ms = fm.apply(size);
        Vector.Mask<Double> m = SPECIES.maskFromArray(ms, 0);

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                DoubleVector av = SPECIES.fromArray(a, i);
                DoubleVector bv = SPECIES.fromArray(b, i);
                Vector.Mask<Double> mv = av.greaterThanEq(bv);

                m = m.and(mv); // accumulate results, so JIT can't eliminate relevant computations
            }
        }
        return m;
    }


    @Benchmark
    public Object blend() {
        double[] a = fa.apply(SPECIES.length());
        double[] b = fb.apply(SPECIES.length());
        double[] r = fr.apply(SPECIES.length());
        boolean[] mask = fm.apply(SPECIES.length());
        Vector.Mask<Double> vmask = SPECIES.maskFromValues(mask);

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                DoubleVector av = SPECIES.fromArray(a, i);
                DoubleVector bv = SPECIES.fromArray(b, i);
                av.blend(bv, vmask).intoArray(r, i);
            }
        }

        return r;
    }

    @Benchmark
    public Object rearrange() {
        double[] a = fa.apply(SPECIES.length());
        int[] order = fs.apply(a.length, SPECIES.length());
        double[] r = fr.apply(SPECIES.length());

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                DoubleVector av = SPECIES.fromArray(a, i);
                av.rearrange(SPECIES.shuffleFromArray(order, i)).intoArray(r, i);
            }
        }

        return r;
    }

    @Benchmark
    public Object extract() {
        double[] a = fa.apply(SPECIES.length());
        double[] r = fr.apply(SPECIES.length());

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                DoubleVector av = SPECIES.fromArray(a, i);
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
        double[] a = fa.apply(SPECIES.length());
        double[] r = fr.apply(SPECIES.length());

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                DoubleVector av = SPECIES.fromArray(a, i);
                av.sin().intoArray(r, i);
            }
        }

        return r;
    }



    @Benchmark
    public Object exp() {
        double[] a = fa.apply(SPECIES.length());
        double[] r = fr.apply(SPECIES.length());

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                DoubleVector av = SPECIES.fromArray(a, i);
                av.exp().intoArray(r, i);
            }
        }

        return r;
    }



    @Benchmark
    public Object log1p() {
        double[] a = fa.apply(SPECIES.length());
        double[] r = fr.apply(SPECIES.length());

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                DoubleVector av = SPECIES.fromArray(a, i);
                av.log1p().intoArray(r, i);
            }
        }

        return r;
    }



    @Benchmark
    public Object log() {
        double[] a = fa.apply(SPECIES.length());
        double[] r = fr.apply(SPECIES.length());

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                DoubleVector av = SPECIES.fromArray(a, i);
                av.log().intoArray(r, i);
            }
        }

        return r;
    }



    @Benchmark
    public Object log10() {
        double[] a = fa.apply(SPECIES.length());
        double[] r = fr.apply(SPECIES.length());

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                DoubleVector av = SPECIES.fromArray(a, i);
                av.log10().intoArray(r, i);
            }
        }

        return r;
    }



    @Benchmark
    public Object expm1() {
        double[] a = fa.apply(SPECIES.length());
        double[] r = fr.apply(SPECIES.length());

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                DoubleVector av = SPECIES.fromArray(a, i);
                av.expm1().intoArray(r, i);
            }
        }

        return r;
    }



    @Benchmark
    public Object cos() {
        double[] a = fa.apply(SPECIES.length());
        double[] r = fr.apply(SPECIES.length());

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                DoubleVector av = SPECIES.fromArray(a, i);
                av.cos().intoArray(r, i);
            }
        }

        return r;
    }



    @Benchmark
    public Object tan() {
        double[] a = fa.apply(SPECIES.length());
        double[] r = fr.apply(SPECIES.length());

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                DoubleVector av = SPECIES.fromArray(a, i);
                av.tan().intoArray(r, i);
            }
        }

        return r;
    }



    @Benchmark
    public Object sinh() {
        double[] a = fa.apply(SPECIES.length());
        double[] r = fr.apply(SPECIES.length());

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                DoubleVector av = SPECIES.fromArray(a, i);
                av.sinh().intoArray(r, i);
            }
        }

        return r;
    }



    @Benchmark
    public Object cosh() {
        double[] a = fa.apply(SPECIES.length());
        double[] r = fr.apply(SPECIES.length());

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                DoubleVector av = SPECIES.fromArray(a, i);
                av.cosh().intoArray(r, i);
            }
        }

        return r;
    }



    @Benchmark
    public Object tanh() {
        double[] a = fa.apply(SPECIES.length());
        double[] r = fr.apply(SPECIES.length());

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                DoubleVector av = SPECIES.fromArray(a, i);
                av.tanh().intoArray(r, i);
            }
        }

        return r;
    }



    @Benchmark
    public Object asin() {
        double[] a = fa.apply(SPECIES.length());
        double[] r = fr.apply(SPECIES.length());

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                DoubleVector av = SPECIES.fromArray(a, i);
                av.asin().intoArray(r, i);
            }
        }

        return r;
    }



    @Benchmark
    public Object acos() {
        double[] a = fa.apply(SPECIES.length());
        double[] r = fr.apply(SPECIES.length());

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                DoubleVector av = SPECIES.fromArray(a, i);
                av.acos().intoArray(r, i);
            }
        }

        return r;
    }



    @Benchmark
    public Object atan() {
        double[] a = fa.apply(SPECIES.length());
        double[] r = fr.apply(SPECIES.length());

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                DoubleVector av = SPECIES.fromArray(a, i);
                av.atan().intoArray(r, i);
            }
        }

        return r;
    }



    @Benchmark
    public Object cbrt() {
        double[] a = fa.apply(SPECIES.length());
        double[] r = fr.apply(SPECIES.length());

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                DoubleVector av = SPECIES.fromArray(a, i);
                av.cbrt().intoArray(r, i);
            }
        }

        return r;
    }



    @Benchmark
    public Object hypot() {
        double[] a = fa.apply(SPECIES.length());
        double[] b = fb.apply(SPECIES.length());
        double[] r = fr.apply(SPECIES.length());

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                DoubleVector av = SPECIES.fromArray(a, i);
                DoubleVector bv = SPECIES.fromArray(b, i);
                av.hypot(bv).intoArray(r, i);
            }
        }

        return r;
    }



    @Benchmark
    public Object pow() {
        double[] a = fa.apply(SPECIES.length());
        double[] b = fb.apply(SPECIES.length());
        double[] r = fr.apply(SPECIES.length());

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                DoubleVector av = SPECIES.fromArray(a, i);
                DoubleVector bv = SPECIES.fromArray(b, i);
                av.pow(bv).intoArray(r, i);
            }
        }

        return r;
    }



    @Benchmark
    public Object atan2() {
        double[] a = fa.apply(SPECIES.length());
        double[] b = fb.apply(SPECIES.length());
        double[] r = fr.apply(SPECIES.length());

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                DoubleVector av = SPECIES.fromArray(a, i);
                DoubleVector bv = SPECIES.fromArray(b, i);
                av.atan2(bv).intoArray(r, i);
            }
        }

        return r;
    }



    @Benchmark
    public Object fma() {
        double[] a = fa.apply(SPECIES.length());
        double[] b = fb.apply(SPECIES.length());
        double[] c = fc.apply(SPECIES.length());
        double[] r = fr.apply(SPECIES.length());

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                DoubleVector av = SPECIES.fromArray(a, i);
                DoubleVector bv = SPECIES.fromArray(b, i);
                DoubleVector cv = SPECIES.fromArray(c, i);
                av.fma(bv, cv).intoArray(r, i);
            }
        }

        return r;
    }



    @Benchmark
    public Object fmaMasked() {
        double[] a = fa.apply(SPECIES.length());
        double[] b = fb.apply(SPECIES.length());
        double[] c = fc.apply(SPECIES.length());
        double[] r = fr.apply(SPECIES.length());
        boolean[] mask = fm.apply(SPECIES.length());
        Vector.Mask<Double> vmask = SPECIES.maskFromValues(mask);

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                DoubleVector av = SPECIES.fromArray(a, i);
                DoubleVector bv = SPECIES.fromArray(b, i);
                DoubleVector cv = SPECIES.fromArray(c, i);
                av.fma(bv, cv, vmask).intoArray(r, i);
            }
        }

        return r;
    }


    @Benchmark
    public Object neg() {
        double[] a = fa.apply(SPECIES.length());
        double[] r = fr.apply(SPECIES.length());

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                DoubleVector av = SPECIES.fromArray(a, i);
                av.neg().intoArray(r, i);
            }
        }

        return r;
    }

    @Benchmark
    public Object negMasked() {
        double[] a = fa.apply(SPECIES.length());
        double[] r = fr.apply(SPECIES.length());
        boolean[] mask = fm.apply(SPECIES.length());
        Vector.Mask<Double> vmask = SPECIES.maskFromValues(mask);

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                DoubleVector av = SPECIES.fromArray(a, i);
                av.neg(vmask).intoArray(r, i);
            }
        }

        return r;
    }

    @Benchmark
    public Object abs() {
        double[] a = fa.apply(SPECIES.length());
        double[] r = fr.apply(SPECIES.length());

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                DoubleVector av = SPECIES.fromArray(a, i);
                av.abs().intoArray(r, i);
            }
        }

        return r;
    }

    @Benchmark
    public Object absMasked() {
        double[] a = fa.apply(SPECIES.length());
        double[] r = fr.apply(SPECIES.length());
        boolean[] mask = fm.apply(SPECIES.length());
        Vector.Mask<Double> vmask = SPECIES.maskFromValues(mask);

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                DoubleVector av = SPECIES.fromArray(a, i);
                av.abs(vmask).intoArray(r, i);
            }
        }

        return r;
    }




    @Benchmark
    public Object sqrt() {
        double[] a = fa.apply(SPECIES.length());
        double[] r = fr.apply(SPECIES.length());

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                DoubleVector av = SPECIES.fromArray(a, i);
                av.sqrt().intoArray(r, i);
            }
        }

        return r;
    }



    @Benchmark
    public Object sqrtMasked() {
        double[] a = fa.apply(SPECIES.length());
        double[] r = fr.apply(SPECIES.length());
        boolean[] mask = fm.apply(SPECIES.length());
        Vector.Mask<Double> vmask = SPECIES.maskFromValues(mask);

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                DoubleVector av = SPECIES.fromArray(a, i);
                av.sqrt(vmask).intoArray(r, i);
            }
        }

        return r;
    }



    @Benchmark
    public Object gather() {
        double[] a = fa.apply(SPECIES.length());
        int[] b    = fs.apply(a.length, SPECIES.length());
        double[] r = new double[a.length];

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                DoubleVector av = SPECIES.fromArray(a, i, b, i);
                av.intoArray(r, i);
            }
        }

        return r;
    }



    @Benchmark
    public Object scatter() {
        double[] a = fa.apply(SPECIES.length());
        int[] b = fs.apply(a.length, SPECIES.length());
        double[] r = new double[a.length];

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                DoubleVector av = SPECIES.fromArray(a, i);
                av.intoArray(r, i, b, i);
            }
        }

        return r;
    }

}

