/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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

import jdk.panama.vector.IntVector;
import jdk.panama.vector.IntVector.IntSpecies;
import jdk.panama.vector.Vector;
import jdk.panama.vector.Vector.Mask;
import org.openjdk.jmh.annotations.*;

import java.util.concurrent.TimeUnit;

/**
 * Inspired by "Sorting an AVX512 register"
 *   http://0x80.pl/articles/avx512-sort-register.html
 */
@BenchmarkMode(Mode.Throughput)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Benchmark)
@Fork(value = 1, jvmArgsPrepend = {"--add-modules=jdk.panama.vector"})
public class SortVector extends AbstractVectorBenchmark {
    @Param({"64", "1024", "65536"})
    int size;

    int[] in, out;

    @Setup
    public void setup() {
        size = size + (size % 16); // FIXME: process tails
        in  = fillInt(size, i -> RANDOM.nextInt());
        out = new int[size];
    }

    @Benchmark
    public void sortVectorI64() {
        sort(I64);
    }

    @Benchmark
    public void sortVectorI128() {
        sort(I128);
    }

    @Benchmark
    public void sortVectorI256() {
        sort(I256);
    }

    @Benchmark
    public void sortVectorI512() {
        sort(I512);
    }


    void sort(IntSpecies spec) {
        var iota = (IntVector)spec.shuffleIota().toVector(); // [ 0 1 ... n ]

        var result = spec.broadcast(0);
        var index = spec.broadcast(0);
        var incr = spec.broadcast(1);

        for (int i = 0; i < in.length; i += spec.length()) {
            var input = spec.fromArray(in, i);

            for (int j = 0; j < input.length(); j++) {
                var shuf = index.toShuffle();
                var b = input.rearrange(shuf); // broadcast j-th element
                var lt = input.lessThan(b).trueCount();
                var eq = input.equal(b).trueCount();

                // int/long -> mask?
                // int m = (1 << (lt + eq)) - (1 << lt);
                // var mask = masks[lt + eq].xor(masks[lt]);
                // var mask = masks[lt + eq].and(masks[lt].not());
                //
                // masks[i] =  [ 0 0 ... 0 1 ... 1 ]
                //                      i-th
                var m = iota.lessThan(lt + eq).and(iota.lessThan(lt).not());

                result = result.blend(b, m);
                index = index.add(incr);
            }
            result.intoArray(out, i);
        }
    }
}
