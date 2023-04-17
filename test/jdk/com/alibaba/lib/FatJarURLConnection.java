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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.jar.JarFile;

public class FatJarURLConnection extends JarURLConnection {
    private FatJarFile jarFile;
    private String realFile;

    public FatJarURLConnection(URL url, FatJarFile jar) throws MalformedURLException {
        super(url);
        this.url = url;
        this.jarFile = jar;
    }

    public FatJarURLConnection(URL url, FatJarFile jar, String realFile) throws MalformedURLException {
        super(url);
        this.url = url;
        this.jarFile = jar;
        this.realFile = realFile;
    }

    @Override
    public JarFile getJarFile() throws IOException {
        return jarFile;
    }

    @Override
    public void connect() throws IOException {
    }

    @Override
    public InputStream getInputStream() throws IOException {
        if (jarFile == null) {
            throw new FileNotFoundException(realFile);
        }
        return realFile == null ? jarFile.getInputStream() : jarFile.getInnerInputStream(realFile);
    }

    @Override
    public int getContentLength() {
        return realFile == null ? jarFile.getContentLength() : jarFile.getInnerContentLength(realFile);
    }
}
