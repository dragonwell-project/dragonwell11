/*
 * Copyright (c) 2023, Alibaba Group Holding Limited. All rights reserved.
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

/*
 * @test
 * @modules java.base/sun.security.action
 * @summary Test dumping using java level API hooks
 * @library /test/lib
 * @build TestDump
 * @requires os.arch=="amd64"
 * @run driver ClassFileInstaller -jar test-notifyDumpByAPI.jar TestDump TestDump$Policy TestDump$ClassLoadingPolicy TestDump$WatcherThread
 * @run main/othervm/timeout=600 TestNotifyDumpByAPI
 */

import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.process.ProcessTools;
import sun.security.action.GetPropertyAction;

import java.io.File;
import java.security.AccessController;

public class TestNotifyDumpByAPI {

    private static final String TESTJAR = "./test-notifyDumpByAPI.jar";
    private static final String TESTCLASS = "TestDump";

    public static void main(String[] args) throws Exception {
        String dir = AccessController.doPrivileged(new GetPropertyAction("test.classes"));
        destroyCache(dir);
        TestNotifyDumpByAPI.verifyPathSetting(dir);
        new File(dir).delete();
    }

    static void verifyPathSetting(String parentDir) throws Exception {
        ProcessBuilder pb = ProcessTools.createJavaProcessBuilder(
                "-Xquickstart:path=" + parentDir + "/quickstartcache",
                "-Xquickstart:verbose",
                // In sleeping condition there is no classloading happens,
                // we will consider it as the start-up finish
                "-DcheckIntervalMS=" + (TestDump.SLEEP_MILLIS / 5),
                "-DtestHooks=true",
                "-cp",
                TESTJAR,
                TESTCLASS);
        pb.redirectErrorStream(true);
        OutputAnalyzer output = new OutputAnalyzer(pb.start());
        System.out.println("[Child Output] " + output.getOutput());
        output.shouldContain(TestDump.ANCHOR);
        output.shouldHaveExitValue(0);
    }

    static void destroyCache(String parentDir) throws Exception {
        ProcessBuilder pb = ProcessTools.createJavaProcessBuilder(
                "-Xquickstart:destroy",
                "-Xquickstart:path=" + parentDir + "/quickstartcache",
                "-Xquickstart:verbose", "-version");
        OutputAnalyzer output = new OutputAnalyzer(pb.start());
        output.shouldContain("destroy the cache folder");
        output.shouldHaveExitValue(0);
    }

}
