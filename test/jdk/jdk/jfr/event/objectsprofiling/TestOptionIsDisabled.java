/*
 * Copyright (c) 2022 Alibaba Group Holding Limited. All rights reserved.
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
package jfr.event.objectsprofiling;

import java.io.File;

import jdk.test.lib.dcmd.PidJcmdExecutor;
import jdk.test.lib.process.OutputAnalyzer;

/*
 * @test TestOptionIsDisabled
 * @library /test/lib
 *
 * @compile -g TestOptionIsDisabled.java
 *
 * @run main/othervm jfr.event.objectsprofiling.TestOptionIsDisabled
 * @run main/othervm -XX:FlightRecorderOptions=sampleobjectallocations=false jfr.event.objectsprofiling.TestOptionIsDisabled
 */
public class TestOptionIsDisabled {
    private static final String DIR = System.getProperty("test.src", ".");
    private static final File SETTINGS = new File(DIR, "testsettings.jfc");

    public static void main(String... args) throws Exception {
        String name = "TestOptionIsDisabled";
        OutputAnalyzer output = new PidJcmdExecutor().execute(
                "JFR.start" +
                " name=" + name +
                " settings=" + SETTINGS.getCanonicalPath());
        output.shouldContain("Can't enable OptoInstanceObjectAllocation/OptoArrayObjectAllocation Event if -XX:FlightRecorderOptions=sampleobjectallocations=true is not set");
    }
}
