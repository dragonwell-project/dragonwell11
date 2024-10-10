/*
 * Copyright (c) 2023, Azul Systems, Inc. All rights reserved.
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

import jdk.test.lib.Utils;
import jdk.test.lib.containers.docker.Common;
import jdk.test.lib.containers.docker.DockerTestUtils;
import jdk.test.lib.crac.*;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Date;
import java.util.concurrent.*;

import static jdk.test.lib.Asserts.fail;

/*
 * @test
 * @summary Test if InetAddress cache is flushed after checkpoint/restore
 * @requires docker.support
 * @library /test/lib
 * @build ResolveTest
 * @run driver jdk.test.lib.crac.CracTest
 */
public class ResolveTest extends CracLogger implements CracTest {
    private static final String imageName = Common.imageName("inet-address");
    public static final String TEST_HOSTNAME = "some.test.hostname.example.com";
    private static final long WAIT_TIMEOUT = 10 * 1000L;

    @CracTestArg(value = 0, optional = true)
    String ip;

    @CracTestArg(value = 1, optional = true)
    String checkFile;

    @Override
    public void test() throws Exception {
        if (!DockerTestUtils.canTestDocker()) {
            return;
        }
        //current ubuntu latest glibc version is 2.35, and the host glibc version is 2.32
        //there is a crash when restore VMA. The root cause is unknown.
        //Here low the ubuntu with 20.04 so that glibc version is 2.31, the crash disappeared.
        //There also other solution: change the docker image with alinux3 that same as the host.
        //
        System.setProperty("jdk.test.docker.image.version", "20.04");

        CracBuilder builder = new CracBuilder()
                .inDockerImage(imageName).dockerOptions("--add-host", TEST_HOSTNAME + ":192.168.12.34")
                .logToFile(true)
                .oneStopDockerRun(true)
                .bumpPid(true)
                .args(CracTest.args(TEST_HOSTNAME, "/second-run"));

        try {
            CracProcess crProcess = builder.startCheckpoint();
            crProcess.watchFile(WAIT_TIMEOUT, "192.168.12.34");
            builder.checkpointViaJcmd();
            //builder.checkpointViaJcmd kill the process that run in container,but
            //the container not exit immediately. There is an error that container
            // "xxxx" exists if run immediately.
            builder.waitUntilContainerExit(WAIT_TIMEOUT);
            CracBuilder builderRestore = new CracBuilder()
                    .inDockerImage(imageName).dockerOptions(
                            "--add-host", TEST_HOSTNAME + ":192.168.56.78",
                            "--volume", Utils.TEST_CLASSES + ":/second-run")
                    .logToFile(true)
                    .oneStopDockerRun(true);
            CracProcess restoreProcess = builderRestore.startRestore();
            restoreProcess.watchFile(WAIT_TIMEOUT, "192.168.56.78");
        } catch (Exception e) {
            e.printStackTrace();
            fail("Throw exception :" + e);
        } finally {
            builder.deepClearContainer();
        }
    }

    @Override
    public void exec() throws Exception {
        if (ip == null || checkFile == null) {
            System.err.println("Args: <ip address> <check file path>");
            return;
        }
        printAddress(ip, this);
        while (!Files.exists(Path.of(checkFile))) {
            try {
                //noinspection BusyWait
                Thread.sleep(100);
            } catch (InterruptedException e) {
                System.err.println("Interrupted!");
                return;
            }
        }
        printAddress(ip, this);
    }

    private static void printAddress(String hostname, CracLogger logger) throws IOException, InterruptedException {
        StringBuilder sb = new StringBuilder();
        try {
            InetAddress address = InetAddress.getByName(hostname);
            // we will assume IPv4 address
            byte[] bytes = address.getAddress();
            sb.append(bytes[0] & 0xFF);
            for (int i = 1; i < bytes.length; ++i) {
                sb.append('.');
                sb.append(bytes[i] & 0xFF);
            }
            sb.append("\n");
        } catch (UnknownHostException e) {
            sb.append("\n");
        }
        logger.writeLog(sb.toString());
    }
}
