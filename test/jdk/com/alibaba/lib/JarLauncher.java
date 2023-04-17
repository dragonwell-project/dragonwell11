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
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.List;

public class JarLauncher {

    public static void run(Class c, String[] args) throws Exception {
        ProtectionDomain protectionDomain = c.getProtectionDomain();
        CodeSource codeSource = protectionDomain.getCodeSource();
        URI location = (codeSource != null) ? codeSource.getLocation().toURI() : null;
        String path = (location != null) ? location.getSchemeSpecificPart() : null;
        if (path == null) {
            throw new IllegalStateException("Unable to determine code source archive");
        }
        File root = new File(path);
        if (!root.exists()) {
            throw new IllegalStateException("Unable to determine code source archive from " + root);
        }
        run(root, args);
    }

    private static void run(File root, String[] args) throws Exception {
        List<URL> urlList = new ArrayList<>();
        String startClassName = JarUtil.readFarJar(new FatJar(root), urlList);

        URLClassLoader classLoader = new URLClassLoader(urlList.toArray(new URL[urlList.size()]), null);
        com.alibaba.util.Utils.registerClassLoader(classLoader, "JarLauncher");
        Class startClass = classLoader.loadClass(startClassName);
        Method main = startClass.getDeclaredMethod("main", new Class[]{String[].class});
        main.setAccessible(true);
        main.invoke(null, new Object[]{args});
    }


    public static void main(String[] args) throws Exception {
        run(new File(args[0]), new String[0]);
    }
}
