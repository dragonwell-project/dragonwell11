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

import java.net.URL;
import java.net.URLClassLoader;
import java.lang.reflect.Field;

import jdk.internal.loader.URLClassPath;

public class MyWebAppClassLoader extends URLClassLoader {
    private final URLClassPath myucp;
    public MyWebAppClassLoader(URL[] urls, ClassLoader parent) {
        super(new URL[0], parent);
        this.myucp = new URLClassPath(urls, null);
    }

    @Override
    public Class<?> findClass(String name) throws ClassNotFoundException {
        try {
            Class<URLClassLoader> clazz = URLClassLoader.class;
            Field field = clazz.getDeclaredField("ucp");
            field.setAccessible(true);
            field.set(this, myucp);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return super.findClass(name);
    }
}
