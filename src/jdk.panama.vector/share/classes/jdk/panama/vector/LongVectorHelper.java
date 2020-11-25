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
public final class LongVectorHelper {

    private LongVectorHelper() {}

    public interface BinaryOp {
        long apply(int i, long a, long b);
    }

    public interface UnaryOp {
        long apply(int i, long a);
    }

    public static 
    LongVector map(LongVector va, LongVector vb, BinaryOp op) {
        return va.bOp(vb, (i, a, b) -> op.apply(i, a, b));
    }

    public static 
    LongVector map(LongVector va, LongVector vb, Vector.Mask<Long> m, BinaryOp op) {
        return va.bOp(vb, m, (i, a, b) -> op.apply(i, a, b));
    }

    public static 
    LongVector map(LongVector va, UnaryOp op) {
        return va.uOp((i, a) -> op.apply(i, a));
    }

    public static 
    LongVector map(LongVector va, Vector.Mask<Long> m, UnaryOp op) {
        return va.uOp(m, (i, a) -> op.apply(i, a));
    }

    public static 
    LongVector div(LongVector va, LongVector vb) {
        return va.bOp(vb, (i, a, b) -> (long) (a / b));
    }

    public static 
    LongVector div(LongVector va, LongVector vb, Vector.Mask<Long> m) {
        return va.bOp(vb, m, (i, a, b) -> (long) (a / b));
    }

    public static 
    LongVector mod(LongVector va, LongVector vb) {
        return va.bOp(vb, (i, a, b) -> (long) (a % b));
    }

    public static 
    LongVector mod(LongVector va, LongVector vb, Vector.Mask<Long> m) {
        return va.bOp(vb, m, (i, a, b) -> (long) (a % b));
    }

    public static 
    LongVector addExact(LongVector va, LongVector vb) {
        return va.bOp(vb, (i, a, b) -> Math.addExact(a, b));
    }

    public static 
    LongVector addExact(LongVector va, LongVector vb, Vector.Mask<Long> m) {
        return va.bOp(vb, m, (i, a, b) -> Math.addExact(a, b));
    }

    public static 
    LongVector decrementExact(LongVector va) {
        return va.uOp((i, a) -> Math.decrementExact(a));
    }

    public static 
    LongVector decrementExact(LongVector va, Vector.Mask<Long> m) {
        return va.uOp(m, (i, a) -> Math.decrementExact(a));
    }

    public static 
    LongVector incrementExact(LongVector va) {
        return va.uOp((i, a) -> Math.incrementExact(a));
    }

    public static 
    LongVector incrementExact(LongVector va, Vector.Mask<Long> m) {
        return va.uOp(m, (i, a) -> Math.incrementExact(a));
    }

    public static 
    LongVector multiplyExact(LongVector va, LongVector vb) {
        return va.bOp(vb, (i, a, b) -> Math.multiplyExact(a, b));
    }

    public static 
    LongVector multiplyExact(LongVector va, LongVector vb, Vector.Mask<Long> m) {
        return va.bOp(vb, m, (i, a, b) -> Math.multiplyExact(a, b));
    }

    public static 
    LongVector negateExact(LongVector va) {
        return va.uOp((i, a) -> Math.negateExact(a));
    }

    public static 
    LongVector negateExact(LongVector va, Vector.Mask<Long> m) {
        return va.uOp(m, (i, a) -> Math.negateExact(a));
    }

    public static 
    LongVector subtractExtract(LongVector va, LongVector vb) {
        return va.bOp(vb, (i, a, b) -> Math.subtractExact(a, b));
    }

    public static 
    LongVector subtractExtract(LongVector va, LongVector vb, Vector.Mask<Long> m) {
        return va.bOp(vb, m, (i, a, b) -> Math.subtractExact(a, b));
    }


}
