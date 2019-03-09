/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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

import java.util.Arrays;

abstract class AbstractMask<E> extends Vector.Mask<E> {

    /*package-private*/
    abstract boolean[] getBits();

    // Unary operator

    interface MUnOp {
        boolean apply(int i, boolean a);
    }

    abstract AbstractMask<E> uOp(MUnOp f);

    // Binary operator

    interface MBinOp {
        boolean apply(int i, boolean a, boolean b);
    }

    abstract AbstractMask<E> bOp(Vector.Mask<E> o, MBinOp f);

    @Override
    public String toString() {
        return Arrays.toString(getBits());
    }

    @Override
    public boolean getElement(int i) {
        return getBits()[i];
    }

    @Override
    public long toLong() {
        long res = 0;
        long set = 1;
        boolean[] bits = getBits();
        for (int i = 0; i < species().length(); i++) {
            res = bits[i] ? res | set : res;
            set = set << 1;
        }
        return res;
    }

    @Override
    public void intoArray(boolean[] bits, int i) {
        System.arraycopy(getBits(), 0, bits, i, species().length());
    }

    @Override
    public boolean[] toArray() {
        return getBits().clone();
    }

    @Override
    public boolean anyTrue() {
        for (boolean i : getBits()) {
            if (i) return true;
        }
        return false;
    }

    @Override
    public boolean allTrue() {
        for (boolean i : getBits()) {
            if (!i) return false;
        }
        return true;
    }

    @Override
    public int trueCount() {
        int c = 0;
        for (boolean i : getBits()) {
            if (i) c++;
        }
        return c;
    }

    @Override
    public AbstractMask<E> and(Vector.Mask<E> o) {
        return bOp(o, (i, a, b) -> a && b);
    }

    @Override
    public AbstractMask<E> or(Vector.Mask<E> o) {
        return bOp(o, (i, a, b) -> a || b);
    }

    @Override
    public AbstractMask<E> not() {
        return uOp((i, a) -> !a);
    }
}
