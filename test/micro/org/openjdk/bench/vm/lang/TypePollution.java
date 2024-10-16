/*
 * Copyright (c) 2024, Red Hat, Inc.. All rights reserved.
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
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package org.openjdk.bench.vm.lang;

import org.openjdk.jmh.annotations.*;

import java.io.Serializable;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.TimeUnit;
import java.util.function.*;

/*
 * A test to demonstrate type pollution.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Thread)
@Warmup(iterations = 4, time = 2)
@Measurement(iterations = 4, time = 2)
@Fork(value = 3)
public class TypePollution {

    static class DynamicInvocationHandler implements InvocationHandler {

        @Override
            public Object invoke(Object proxy, Method method, Object[] args) {
            return null;
        }
    }

    interface I01 {}
    interface I02 {}
    interface I03 {}
    interface I04 {}
    interface I05 {}
    interface I06 {}
    interface I07 {}
    interface I08 {}
    interface I09 {}
    interface I10 {}
    interface I11 {}
    interface I12 {}
    interface I13 {}
    interface I14 {}
    interface I15 {}
    interface I16 {}
    interface I17 {}
    interface I18 {}
    interface I19 {}
    interface I20 {}

    static Class<?>[] classes;

    static {
        classes = new Class<?>[] { I01.class, I02.class, I03.class, I04.class, I05.class,
                                   I06.class, I07.class, I08.class, I09.class, I10.class,
                                   I11.class, I12.class, I13.class, I14.class, I15.class,
                                   I16.class, I17.class, I18.class, I19.class, I20.class };
    }

    private static final int NOOFOBJECTS = 100;

    public Object[] objectArray;

    public Random rand = new Random(0);

    @Setup(Level.Trial)
    public void setup() {
        objectArray = new Object[1000];
        var loader = getClass().getClassLoader();
        Class<?>[] someInterfaces = new Class<?>[0];
        for (int i = 0; i < objectArray.length; i++) {
            Set<Class<?>> aSet = new HashSet<Class<?>>();
            for (int j = 0; j < 6; j++) {
                aSet.add(classes[rand.nextInt(classes.length)]);
            }
            Class<?>[] interfaceArray = new Class[aSet.size()];
            interfaceArray = aSet.toArray(interfaceArray);
            objectArray[i] = Proxy.newProxyInstance(loader, interfaceArray, new DynamicInvocationHandler());
        }
    }

    int probe = 99;

    @Benchmark
    @Fork(jvmArgs={"-XX:+UnlockDiagnosticVMOptions", "-XX:-UseSecondarySupersTable", "-XX:-UseSecondarySupersCache"})
    @OutputTimeUnit(TimeUnit.MILLISECONDS)
    public long parallelInstanceOfInterfaceSwitchLinearNoSCC() {
        return parallelInstanceOfInterfaceSwitch();
    }

    @Benchmark
    @Fork(jvmArgs={"-XX:+UnlockDiagnosticVMOptions", "-XX:-UseSecondarySupersTable", "-XX:+UseSecondarySupersCache"})
    @OutputTimeUnit(TimeUnit.MILLISECONDS)
    public long parallelInstanceOfInterfaceSwitchLinearSCC() {
        return parallelInstanceOfInterfaceSwitch();
    }

    @Benchmark
    @Fork(jvmArgs={"-XX:+UnlockDiagnosticVMOptions", "-XX:+UseSecondarySupersTable", "-XX:-UseSecondarySupersCache"})
    @OutputTimeUnit(TimeUnit.MILLISECONDS)
    public long parallelInstanceOfInterfaceSwitchTableNoSCC() {
        return parallelInstanceOfInterfaceSwitch();
    }

    @Benchmark
    @Fork(jvmArgs={"-XX:+UnlockDiagnosticVMOptions", "-XX:+UseSecondarySupersTable", "-XX:+UseSecondarySupersCache"})
    @OutputTimeUnit(TimeUnit.MILLISECONDS)
    public long parallelInstanceOfInterfaceSwitchTableSCC() {
        return parallelInstanceOfInterfaceSwitch();
    }

    long parallelInstanceOfInterfaceSwitch() {
        Supplier<Long> s = () -> {
            long sum = 0;
            for (int i = 0; i < 10000; i++) {
                sum += instanceOfInterfaceSwitch();
            }
            return sum;
        };
        try {
            CompletableFuture<Long> future = CompletableFuture.supplyAsync(s);
            return s.get() + future.get();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Benchmark
    @Fork(jvmArgs={"-XX:+UnlockDiagnosticVMOptions", "-XX:-UseSecondarySupersTable", "-XX:-UseSecondarySupersCache"})
    public int instanceOfInterfaceSwitchLinearNoSCC() {
        return instanceOfInterfaceSwitch();
    }

    @Benchmark
    @Fork(jvmArgs={"-XX:+UnlockDiagnosticVMOptions", "-XX:-UseSecondarySupersTable", "-XX:+UseSecondarySupersCache"})
    public int instanceOfInterfaceSwitchLinearSCC() {
        return instanceOfInterfaceSwitch();
    }

    @Benchmark
    @Fork(jvmArgs={"-XX:+UnlockDiagnosticVMOptions", "-XX:+UseSecondarySupersTable", "-XX:-UseSecondarySupersCache"})
    public int instanceOfInterfaceSwitchTableNoSCC() {
        return instanceOfInterfaceSwitch();
    }

    @Benchmark
    @Fork(jvmArgs={"-XX:+UnlockDiagnosticVMOptions", "-XX:+UseSecondarySupersTable", "-XX:+UseSecondarySupersCache"})
    public int instanceOfInterfaceSwitchTableSCC() {
        return instanceOfInterfaceSwitch();
    }

    int instanceOfInterfaceSwitch() {
        int dummy = 0;
        List<Function<Object,Integer>> list1 = List.of(
                o -> (o instanceof I01) ? 1 : -1,
                o -> (o instanceof I02) ? 2 : -1,
                o -> (o instanceof I03) ? 3 : -1,
                o -> (o instanceof I04) ? 4 : -1,
                o -> (o instanceof I05) ? 5 : -1,
                o -> (o instanceof I06) ? 6 : -1,
                o -> (o instanceof I07) ? 7 : -1,
                o -> (o instanceof I08) ? 8 : -1
        );

        List<Function<Object,Integer>> list2 = List.of(
                o -> (o instanceof I18) ? 8 : -1,
                o -> (o instanceof I17) ? 7 : -1,
                o -> (o instanceof I16) ? 6 : -1,
                o -> (o instanceof I15) ? 5 : -1,
                o -> (o instanceof I14) ? 4 : -1,
                o -> (o instanceof I13) ? 3 : -1,
                o -> (o instanceof I12) ? 2 : -1,
                o -> (o instanceof I11) ? 1 : -1
        );
        for (int i = 0; i < 100; i++) {
            probe ^= probe << 13;   // xorshift
            probe ^= probe >>> 17;
            probe ^= probe << 5;

            Object obj = objectArray[(probe & Integer.MAX_VALUE) % objectArray.length];
            dummy += list1.stream().mapToInt(element -> element.apply(obj)).filter(ret -> ret != -1).findFirst().orElse(10);
            probe ^= probe << 13;   // xorshift
            probe ^= probe >>> 17;
            probe ^= probe << 5;

            Object obj2 = objectArray[(probe & Integer.MAX_VALUE) % objectArray.length];
            dummy += list2.stream().mapToInt(element -> element.apply(obj2)).filter(ret -> ret != -1).findFirst().orElse(0);
        }
        return dummy;
    }
}
