/*
 * Copyright (c) 2021 Alibaba Group Holding Limited. All Rights Reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation. Alibaba designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 */
package com.alibaba.bench.java.io;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.infra.Blackhole;

import java.io.*;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

@State(Scope.Benchmark)
public class ObjectInputStreamTest {
    private Object o;

    @Setup
    public void setup() {
        List<Integer> nums = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            nums.add(ThreadLocalRandom.current().nextInt());
        }
        List<String> strings = new ArrayList<>();
        byte[] ba = new byte[36];
        for (int i = 0; i < 20; i++) {
            ThreadLocalRandom.current().nextBytes(ba);
            strings.add(Base64.getEncoder().encodeToString(ba));
        }
        HashMap<String, List<?>> m = new HashMap<>();
        m.put("nums", nums);
        m.put("strings", strings);
        m.put("empty", Collections.emptyList());
        o = m;
    }

    @Benchmark
    public void io(Blackhole bh) throws Exception {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(bos);
        oos.writeObject(o);
        oos.close();
        ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(bos.toByteArray()));
        bh.consume(ois.readObject());
    }
}
