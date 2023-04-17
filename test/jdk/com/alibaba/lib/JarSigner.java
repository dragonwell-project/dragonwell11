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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;

public class JarSigner {
    private String keystoreFilePath;
    private String alias;
    private String keystorePass;

    public JarSigner(String keystoreFilePath, String alias, String keystorePass) {
        this.keystoreFilePath = keystoreFilePath;
        this.alias = alias;
        this.keystorePass = keystorePass;
    }

    public void sign(String destJar) throws IOException {
        String jarsigner = jdk.test.lib.JDKToolFinder.getJDKTool("jarsigner");

        ArrayList<String> args = new ArrayList<>();
        args.add(jarsigner);
        args.add("-keystore");
        args.add(keystoreFilePath);
        args.add("-storepass");
        args.add(keystorePass);
        args.add(destJar);
        args.add(alias);

        // Reporting
        StringBuilder cmdLine = new StringBuilder();
        for (String cmd : args)
            cmdLine.append(cmd).append(' ');
        System.out.println("Command line: [" + cmdLine.toString() + "]");
        ProcessBuilder pb = new ProcessBuilder(args.toArray(new String[args.size()]));
        jdk.test.lib.process.OutputAnalyzer output = new jdk.test.lib.process.OutputAnalyzer(pb.start());
        output.shouldHaveExitValue(0);
    }
}
