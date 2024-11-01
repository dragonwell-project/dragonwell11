/*
 * Copyright (c) 2017, 2019, Oracle and/or its affiliates. All rights reserved.
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

package gc.concurrent_phase_control;

/*
 * @test TestConcurrentPhaseControlG1Basics
 * @bug 8169517
 * @requires vm.gc.G1
 * @summary Verify G1 supports concurrent phase control and has the
 * expected set of phases.
 * @key gc
 * @modules java.base
 * @library /test/lib /
 * @build sun.hotspot.WhiteBox
 * @run driver ClassFileInstaller sun.hotspot.WhiteBox
 *    sun.hotspot.WhiteBox$WhiteBoxPermission
 * @run main/othervm -XX:+UseG1GC
 *   -Xbootclasspath/a:.
 *   -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI
 *   gc.concurrent_phase_control.TestConcurrentPhaseControlG1Basics
 */

import gc.concurrent_phase_control.CheckSupported;

public class TestConcurrentPhaseControlG1Basics {

    private static final String[] phases = {
        "ANY",
        "IDLE",
        "CONCURRENT_CYCLE",
        "CLEAR_CLAIMED_MARKS",
        "SCAN_ROOT_REGIONS",
        "CONCURRENT_MARK",
        "MARK_FROM_ROOTS",
        "PRECLEAN",
        "BEFORE_REMARK",
        "REMARK",
        "REBUILD_REMEMBERED_SETS",
        "CLEANUP_FOR_NEXT_MARK",
    };

    public static void main(String[] args) throws Exception {
        CheckSupported.check("G1", phases);
    }
}
