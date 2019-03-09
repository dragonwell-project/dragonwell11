/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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

/* @test
 * @modules java.base/jdk.internal.misc
 *
 * @run main/othervm                           panama.vector.MaxVectorSize
 * @run main/othervm -XX:UseAVX=3              panama.vector.MaxVectorSize
 * @run main/othervm -XX:UseAVX=2              panama.vector.MaxVectorSize
 * @run main/othervm -XX:UseAVX=1              panama.vector.MaxVectorSize
 * @run main/othervm -XX:UseAVX=0              panama.vector.MaxVectorSize
 * @run main/othervm -XX:UseAVX=0 -XX:UseSSE=2 panama.vector.MaxVectorSize
 */
package panama.vector;

import jdk.internal.misc.Unsafe;

/*
 * Sample output:
 *   command: main -XX:UseAVX=2 panama.vector.MaxVectorSize
 *   ----------System.out:(8/92)----------
 *   boolean: 32
 *   byte:    32
 *   short:   16
 *   char:    16
 *   int:     8
 *   long:    4
 *   float :  8
 *   double:  4
 *
 *   command: main -XX:UseAVX=1 panama.vector.MaxVectorSize
 *   ----------System.out:(8/90)----------
 *   boolean: 16
 *   byte:    16
 *   short:   8
 *   char:    8
 *   int:     4
 *   long:    2
 *   float :  8
 *   double:  4
 *
 *   command: main -XX:UseAVX=0 panama.vector.MaxVectorSize
 *   ----------System.out:(8/90)----------
 *   boolean: 16
 *   byte:    16
 *   short:   8
 *   char:    8
 *   int:     4
 *   long:    2
 *   float :  4
 *   double:  2
 *
 *   command: main -XX:UseAVX=0 -XX:UseSSE=2 panama.vector.MaxVectorSize
 *   ----------System.out:(8/90)----------
 *   boolean: 16
 *   byte:    16
 *   short:   8
 *   char:    8
 *   int:     4
 *   long:    2
 *   float :  4
 *   double:  2
 */
public class MaxVectorSize {
    public static void main(String[] args) {
        Unsafe U = Unsafe.getUnsafe();
        System.out.printf("boolean: %d\n", U.getMaxVectorSize(boolean.class));
        System.out.printf("byte:    %d\n", U.getMaxVectorSize(byte.class));
        System.out.printf("short:   %d\n", U.getMaxVectorSize(short.class));
        System.out.printf("char:    %d\n", U.getMaxVectorSize(char.class));
        System.out.printf("int:     %d\n", U.getMaxVectorSize(int.class));
        System.out.printf("long:    %d\n", U.getMaxVectorSize(long.class));
        System.out.printf("float :  %d\n", U.getMaxVectorSize(float.class));
        System.out.printf("double:  %d\n", U.getMaxVectorSize(double.class));
    }
}
