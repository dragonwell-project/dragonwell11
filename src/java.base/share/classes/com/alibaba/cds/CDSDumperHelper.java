/*
 * Copyright (c) 2023, Alibaba Group Holding Limited. All rights reserved.
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
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package com.alibaba.cds;

import com.alibaba.util.CDSDumpHook;
import com.alibaba.util.QuickStart;
import com.alibaba.util.Utils;
import jdk.internal.misc.VM;

import java.nio.file.Path;

public class CDSDumperHelper {

    private static String cdsDumper;

    public static void setCdsDumper(String cdsDumper) {
        CDSDumperHelper.cdsDumper = cdsDumper;
    }

    private static String nonNullString(String str) {
        return str == null ? "" : str;
    }

    private static String[] restoreCommandLineOptions(String[] arguments) {
        // if user specifies '-Dcom.alibaba.wisp.threadAsWisp.black=name:process\ reaper\;name:epollEventLoopGroup\*' as the command line:
        // VM will change it to '-Dcom.alibaba.wisp.threadAsWisp.black=name:process reaper;name:epollEventLoopGroup*',
        // in which the escape character is removed. We will concat all of them with a ' ' (space character) because
        // users could use any character inside the command line. So we need to restore the ' ' inside the option back
        // to '\ ': '-Dcom.alibaba.wisp.threadAsWisp.black=name:process\ reaper;name:epollEventLoopGroup*'
        for (int i = 0; i < arguments.length; i++) {
            arguments[i] = arguments[i].replace(" ", "\\ ");
        }
        return arguments;
    }

    public static void invokeCDSDumper() {
        CDSDumpHook.Info info = CDSDumpHook.getInfo();
        boolean verbose = QuickStart.isVerbose();

        String jdkHome = Utils.getJDKHome();
        String vmOptions = getVmOptions();
        String classPath = getClassPath();
        Utils.runProcess(verbose, "[CDSDumper] ", (pb) -> {
                    // clear up agent options because cds dump phase cannot live with java agent in peace
                    String toolOp = Utils.removeAgentOp();
                    if (toolOp != null) {
                        pb.environment().put(Utils.JAVA_TOOL_OPTIONS, toolOp);
                        //remove Alibaba Dragonwell specific java tool options avoid enter quickstart endless loop.
                        pb.environment().remove("DRAGONWELL_JAVA_TOOL_OPTIONS");
                        pb.environment().remove("DRAGONWELL_ENABLE_QUICKSTART_ENTRY");
                    }
                },
                Path.of(jdkHome, "bin", "java").toString(),
                "-cp",
                Path.of(jdkHome, "lib", QuickStart.getServerlessAdapter()).toString(),
                "--add-exports=java.base/jdk.internal.misc=ALL-UNNAMED",
                cdsDumper,                                              // invoke CDSDumper
                QuickStart.cachePath(),                                 // arg[0]: String dirPath
                info.originClassListName,                               // arg[1]: String originClassListName
                info.finalClassListName,                                // arg[2]: String finalClassListName
                Boolean.toString(info.eager),                           // arg[3]: boolean eager
                info.jsaName,                                           // arg[4]: String jsaName
                nonNullString(info.agent),                              // arg[5]: String agent
                Boolean.toString(verbose),                              // arg[6]: boolean verbose
                vmOptions,                                              // arg[7]: String runtimeCommandLine
                classPath                                               // arg[8]: String cp
        );
    }

    private static String getClassPath() {
        if (QuickStart.isDumper()) {
            return QuickStart.getClassPathInProfileStage();
        } else {
            return System.getProperty("java.class.path");
        }
    }

    private static String getVmOptions() {
        if (QuickStart.isDumper()) {
            return Utils.getVMRuntimeArguments(QuickStart.getVmOptionsInProfileStage());
        } else {
            return String.join(" ", restoreCommandLineOptions(VM.getRuntimeArguments()));
        }
    }
}