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

package com.alibaba.util;

import com.alibaba.cds.CDSDumperHelper;

public class CDSDumpHook {

    public static class Info {
        public String originClassListName;
        public String finalClassListName;
        public String jsaName;
        public final String agent;
        public final boolean eager;
        public Info(String cdsOriginClassList,
                    String cdsFinalClassList,
                    String cdsJsa,
                    boolean useEagerAppCDS,
                    String eagerAppCDSAgent) {
            this.originClassListName = cdsOriginClassList;
            this.finalClassListName = cdsFinalClassList;
            this.jsaName = cdsJsa;
            this.eager = useEagerAppCDS;
            this.agent = eagerAppCDSAgent;
        }
    }
    private static Info info;
    public static Info getInfo() { return info; }

    // called by JVM
    private static void initialize(String cdsOriginClassList, String cdsFinalClassList, String cdsJSA, String agent, boolean useEagerAppCDS) {
        info = new Info(cdsOriginClassList, cdsFinalClassList, cdsJSA, useEagerAppCDS, agent);

        CDSDumpHook.setup();
    }

    private static void setup() {
        QuickStart.addDumpHook(() -> {
            try {
                CDSDumperHelper.invokeCDSDumper();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

}
