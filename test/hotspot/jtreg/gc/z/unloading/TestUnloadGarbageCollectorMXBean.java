/*
 * Copyright (c) 2021, Alibaba Group Holding Limited. All rights reserved.
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

/**
 * @test TestUnloadGarbageCollectorMXBean
 * @requires vm.gc.Z & !vm.graal.enabled
 * @summary Test whether ZGC garbage collector MXBean reports pause time and count of class unloading
 * @library / /test/lib /runtime/testlibrary
 * @build sun.hotspot.WhiteBox
 * @run driver ClassFileInstaller sun.hotspot.WhiteBox
 *                                sun.hotspot.WhiteBox$WhiteBoxPermission
 * @run main/othervm -XX:+UseZGC -XX:+ClassUnloading -XX:ZUnloadClassesFrequency=2 -Xms128m -Xmx128m
 *      -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI -Xbootclasspath/a:.
 *      gc.z.unloading.TestUnloadGarbageCollectorMXBean 2
 * @run main/othervm -XX:+UseZGC -XX:+ClassUnloading -XX:ZUnloadClassesFrequency=13 -Xms128m -Xmx128m
 *      -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI -Xbootclasspath/a:.
 *      gc.z.unloading.TestUnloadGarbageCollectorMXBean 13
 * @run main/othervm -XX:+UseZGC -XX:+ClassUnloading -XX:ZUnloadClassesFrequency=0 -Xms128m -Xmx128m
 *      -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI -Xbootclasspath/a:.
 *      gc.z.unloading.TestUnloadGarbageCollectorMXBean 0
 * @run main/othervm -XX:+UseZGC -XX:-ClassUnloading -Xms128m -Xmx128m
 *      -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI -Xbootclasspath/a:.
 *      gc.z.unloading.TestUnloadGarbageCollectorMXBean 0
 */
package gc.z.unloading;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import sun.hotspot.WhiteBox;

public class TestUnloadGarbageCollectorMXBean {
    private static final WhiteBox wb = WhiteBox.getWhiteBox();
    private static final long triggeredCycles = 150;
    private static long unloadClassesFrequency;

    public static void main(String[] args) throws Exception {
        unloadClassesFrequency = Long.parseLong(args[0]);

        for (int i = 0; i < triggeredCycles; i++) {
            wb.youngGC();
            if (!collectAndVerifyStatistics()) {
                throw new Exception("Unexpected pauses");
            }
        }
    }

    private static boolean collectAndVerifyStatistics() {
        long reportedCycles = 0;
        long reportedPauses = 0;
        for (final GarbageCollectorMXBean gc : ManagementFactory.getGarbageCollectorMXBeans()) {
            if (gc.getName().equals("ZGC Cycles")) {
                reportedCycles = gc.getCollectionCount();
            } else if (gc.getName().equals("ZGC Pauses")) {
                reportedPauses = gc.getCollectionCount();
            }
        }
        if (reportedCycles == 0) {
            return reportedPauses == 0;
        }
        return (reportedPauses >= calculatePauses(reportedCycles - 1)) && (reportedPauses <= calculatePauses(reportedCycles));
    }

    private static long calculatePauses(long cycles) {
        final long unloadClassesPauses;
        if (unloadClassesFrequency == 0) {
            unloadClassesPauses = 0;
        } else {
            unloadClassesPauses = cycles / unloadClassesFrequency;
        }
        return cycles * 3 + unloadClassesPauses;
    }
}
