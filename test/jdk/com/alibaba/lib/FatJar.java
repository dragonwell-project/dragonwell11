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

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class FatJar {
    private final File root;
    private JarFile rootJar;
    private volatile boolean init;

    private Map<String, FatJarFile> nestJarMap = new HashMap<>();

    public FatJar(File root) throws IOException {
        this.root = root;
        rootJar = new JarFile(root);
    }

    public File getRoot() {
        return this.root;
    }

    public FatJarFile getNestJar(String nestJar) throws IOException {
        if (!init) {
            synchronized (this) {
                if (!init) {
                    readNestedJar();
                    init = true;
                }
            }
        }
        return nestJarMap.get(nestJar);
    }

    private void readNestedJar() throws IOException {
        byte[] wholeFile = new byte[(int) root.length()];
        FileInputStream fis = new FileInputStream(root);
        fis.read(wholeFile);
        fis.close();

        ByteArrayInputStream bais = new ByteArrayInputStream(wholeFile);
        ZipInputStream zis = new ZipInputStream(bais);
        ZipEntry zipEntry = zis.getNextEntry();

        byte[] buffer = new byte[4096];
        while (zipEntry != null) {
            ByteArrayOutputStream baos = readEntryContent(zis, buffer);
            int len;
            if (!zipEntry.isDirectory() && zipEntry.getName().endsWith(".jar")) {
                nestJarMap.put(zipEntry.getName(), new FatJarFile(root, zipEntry.getName(), baos.toByteArray(), false, rootJar));
            }
            zipEntry = zis.getNextEntry();
        }
        zis.close();
    }

    private ByteArrayOutputStream readEntryContent(ZipInputStream zis, byte[] buffer) throws IOException {
        int len = 0;
        int total = 0;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        while ((len = zis.read(buffer)) > 0) {
            baos.write(buffer, 0, len);
            total += len;
        }
        return baos;
    }

    public FatJarFile getNestDir(String dir) throws IOException {
        return new FatJarFile(root, dir, null, true, rootJar);
    }
}
