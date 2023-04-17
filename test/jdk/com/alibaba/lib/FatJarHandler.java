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

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class FatJarHandler extends URLStreamHandler {
    private final JarFile root;
    private final FatJar fatJar;

    public FatJarHandler(JarFile root, FatJar fatJar) {
        this.root = root;
        this.fatJar = fatJar;
    }

    @Override
    protected URLConnection openConnection(URL url) throws IOException {
        String[] file = url.getFile().split("!/");
        if (file.length >= 2) {
            if (file[0].equals(fatJar.getRoot().toURI().toURL().toExternalForm())) {
                JarEntry entry = root.getJarEntry(file[1]);
                if (entry == null) {
                    return null;
                } else {
                    FatJarFile jar = null;
                    if (!entry.isDirectory() && file[1].endsWith(".jar")) {
                        jar = fatJar.getNestJar(file[1]);
                    } else if (entry.isDirectory()) {
                        jar = fatJar.getNestDir(file[1]);
                    }

                    if (file.length >= 3) {
                        if (jar.getJarEntry(file[2]) != null) {
                            return new FatJarURLConnection(url, jar, file[2]);
                        } else {
                            return new FatJarURLConnection(url, null, file[2]);
                        }
                    } else {
                        return new FatJarURLConnection(url, jar);
                    }
                }
            } else {
                return null;
            }
        }
        return null;
    }

}
