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

package trivial;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;

import jdk.internal.misc.Unsafe;

public class InitializeWithVMDefinedClass {

    static Unsafe theUnsafe;
    static {
        try {
            Field unsafe = Unsafe.class.getDeclaredField("theUnsafe");
            unsafe.setAccessible(true);
            theUnsafe = (Unsafe) unsafe.get(null);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
        }

    }

    private static byte[] read(InputStream ios) throws Throwable {
        ByteArrayOutputStream ous = null;
        try {
            byte[] buffer = new byte[4096];
            ous = new ByteArrayOutputStream();
            int read = 0;
            while ((read = ios.read(buffer)) != -1) {
                ous.write(buffer, 0, read);
            }
        } finally {
            try {
                if (ous != null)
                    ous.close();
            } catch (IOException e) {
            }

            try {
                if (ios != null)
                    ios.close();
            } catch (IOException e) {
            }
        }
        return ous.toByteArray();
    }

    static class MyLoader extends ClassLoader {
        private byte[] bc;

        public MyLoader(ClassLoader parent, byte[] bc) {
            super(parent);
            this.bc = bc;
        }

        @Override
        protected Class<?> findClass(String name) throws ClassNotFoundException {
            return theUnsafe.defineClass(name, bc, 0, bc.length, null, null);
        }
    }

    static {
        try {
            InputStream is = InitializeWithVMDefinedClass.class.getClassLoader()
                    .getResourceAsStream("trivial/MyFoo.bytecode");
            byte[] bc = read(is);
            MyLoader m = new MyLoader(InitializeWithVMDefinedClass.class.getClassLoader(), bc);
            m.loadClass("trivial.MyFoo");
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }
}