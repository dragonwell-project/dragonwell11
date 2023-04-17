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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.CountDownLatch;
import java.math.*;
import java.util.*;

import jdk.internal.misc.JavaLangClassLoaderAccess;
import jdk.internal.misc.SharedSecrets;

public class TestLoadClassWithException {
    static CountDownLatch finishLatch;
    static URLClassLoader loader;

    public static void main(String... args) throws Exception {
        Path CLASSES_DIR = Paths.get("./testSimple.jar");
        String classPath2 = "newpath/testSimple.jar";
        Path CLASSES_DIR_2 = Paths.get(classPath2);
        finishLatch = new CountDownLatch(2);
        //  class loader with name
        URL[] urls = new URL[] { CLASSES_DIR.toUri().toURL(), CLASSES_DIR_2.toUri().toURL() };
        ClassLoader parent = ClassLoader.getSystemClassLoader();
        loader = new URLClassLoader(urls, parent);
        registerURLClassLoader(loader, "myloader");

        new Thread() {
            public void run() {
                try {
                    Class<?> c = Class.forName("trivial.TestSimple", true, loader);
                } catch (Exception e) {
                    e.printStackTrace(System.out);
                }
                finishLatch.countDown();
            }
        }.start();

        new Thread() {
            public void run() {
                try {
                    Class<?> c = Class.forName("TestSimpleWispUsage", true, loader);
                } catch (Exception e) {
                    e.printStackTrace(System.out);
                }
                finishLatch.countDown();
            }
        }.start();

        try {
            finishLatch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static void registerURLClassLoader(URLClassLoader loader, String loaderName) throws Exception {
        try {
            Method m = Class.forName("com.alibaba.util.Utils").getDeclaredMethod("registerClassLoader",
                                     ClassLoader.class, String.class);
            m.setAccessible(true);
            m.invoke(null, loader, loaderName);
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
    }

}

