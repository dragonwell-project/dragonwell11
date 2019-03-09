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

import jdk.panama.vector.*;
import jdk.panama.vector.IntVector.IntSpecies;
import org.openjdk.jmh.annotations.*;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@BenchmarkMode(Mode.Throughput)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Benchmark)
@Fork(value = 1, jvmArgsPrepend = {"--add-modules=jdk.panama.vector"})
public class Merge extends AbstractVectorBenchmark {

    @Param({"64", "1024", "65536"})
    int size;

    int[] in, out;

    @Setup
    public void setup() {
        size = size + (size % 64); // FIXME: process tails
        in  = new int[size];
        out = new int[size];
        for (int i = 0; i < size; i++) {
            in[i] = i;
        }
    }

    @Benchmark
    public void merge64_128() {
        merge(I64, I128);
    }

    @Benchmark
    public void merge128_256() {
        merge(I128, I256);
    }

    @Benchmark
    public void merge256_512() {
        merge(I256, I512);
    }

    @Benchmark
    public void merge64_256() {
        merge(I64, I256);
    }

    @Benchmark
    public void merge128_512() {
        merge(I128, I512);
    }

    @Benchmark
    public void merge64_512() {
        merge(I64, I256);
    }

    IntVector merge(IntSpecies from, IntSpecies to, int idx) {
        assert from.length() <= to.length();

        int vlenFrom = from.length();
        int vlenTo   =   to.length();

        if (vlenFrom == vlenTo) {
            return from.fromArray(in, idx);
        } else {
            var stepDown = (IntSpecies) narrow(to);
            int mid = stepDown.length();
            var lo = merge(from, stepDown, idx);
            var hi = merge(from, stepDown, idx + mid);
            return join(stepDown, to, lo, hi);
        }
    }


    void merge(IntSpecies from, IntSpecies to) {
        int vlenTo = to.length();
        for (int i = 0; i < in.length; i += vlenTo) {
            var r = merge(from, to, i);
            r.intoArray(out, i);
        }
    }

    @TearDown
    public void tearDown() {
        assertArrayEquals(in, out);
    }
}
