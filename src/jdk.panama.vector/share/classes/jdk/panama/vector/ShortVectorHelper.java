/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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
 * or visit www.oracle.com if you need additional information or have
 * questions.
 */
package jdk.panama.vector;

/**
 * Operations on vectors that are not intrinsic candidates
 */
@SuppressWarnings("cast")
public final class ShortVectorHelper {

    private ShortVectorHelper() {}

    public interface BinaryOp {
        short apply(int i, short a, short b);
    }

    public interface UnaryOp {
        short apply(int i, short a);
    }

    public static 
    ShortVector map(ShortVector va, ShortVector vb, BinaryOp op) {
        return va.bOp(vb, (i, a, b) -> op.apply(i, a, b));
    }

    public static 
    ShortVector map(ShortVector va, ShortVector vb, Vector.Mask<Short> m, BinaryOp op) {
        return va.bOp(vb, m, (i, a, b) -> op.apply(i, a, b));
    }

    public static 
    ShortVector map(ShortVector va, UnaryOp op) {
        return va.uOp((i, a) -> op.apply(i, a));
    }

    public static 
    ShortVector map(ShortVector va, Vector.Mask<Short> m, UnaryOp op) {
        return va.uOp(m, (i, a) -> op.apply(i, a));
    }

    public static 
    ShortVector div(ShortVector va, ShortVector vb) {
        return va.bOp(vb, (i, a, b) -> (short) (a / b));
    }

    public static 
    ShortVector div(ShortVector va, ShortVector vb, Vector.Mask<Short> m) {
        return va.bOp(vb, m, (i, a, b) -> (short) (a / b));
    }

    public static 
    ShortVector mod(ShortVector va, ShortVector vb) {
        return va.bOp(vb, (i, a, b) -> (short) (a % b));
    }

    public static 
    ShortVector mod(ShortVector va, ShortVector vb, Vector.Mask<Short> m) {
        return va.bOp(vb, m, (i, a, b) -> (short) (a % b));
    }



}
