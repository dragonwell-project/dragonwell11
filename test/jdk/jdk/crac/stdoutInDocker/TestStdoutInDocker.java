/*
 * Copyright (c) 2024, Alibaba Group Holding Limited. All rights reserved.
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
import jdk.crac.Context;
import jdk.crac.Core;
import jdk.crac.Resource;

/**
 * @summary Test run "docker logs" can get output from stdout/stderr correctly.
 */

public class TestStdoutInDocker {
    public static void main(String[] args) throws Exception {
        System.out.println("Message from stdout before doing checkpoint");
        System.err.println("Message from stderr before doing checkpoint");
        Core.getGlobalContext().register(new Resource() {
            @Override
            public void beforeCheckpoint(Context<? extends Resource> context) throws Exception {
            }

            @Override
            public void afterRestore(Context<? extends Resource> context) throws Exception {
                System.out.println("Message from stdout in afterRestore callback");
                System.err.println("Message from stderr in afterRestore callback");
            }
        });
        Core.checkpointRestore();
        System.out.println("Message from stdout afterRestore");
        System.err.println("Message from stderr afterRestore");
        int sleepTime = Integer.parseInt(System.getenv("SLEEP_TIME"));
        System.out.println("Sleep time is : " + sleepTime);
        Thread.sleep(sleepTime);
    }
}
