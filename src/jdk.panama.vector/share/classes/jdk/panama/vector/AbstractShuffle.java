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

import java.util.function.IntUnaryOperator;

abstract class AbstractShuffle<E> extends Vector.Shuffle<E> {
    static final IntUnaryOperator IDENTITY = i -> i;

    // Internal representation allows for a maximum index of 256
    // Values are masked by (species().length() - 1)
    final byte[] reorder;

    AbstractShuffle(byte[] reorder) {
        this.reorder = reorder;
    }

    public AbstractShuffle(int[] reorder) {
        this(reorder, 0);
    }

    public AbstractShuffle(int[] reorder, int offset) {
        byte[] a = new byte[species().length()];
        for (int i = 0; i < a.length; i++) {
            a[i] = (byte) (reorder[offset + i] & (a.length - 1));
        }
        this.reorder = a;
    }

    public AbstractShuffle(IntUnaryOperator f) {
        byte[] a = new byte[species().length()];
        for (int i = 0; i < a.length; i++) {
            a[i] = (byte) (f.applyAsInt(i) & (a.length - 1));
        }
        this.reorder = a;
    }

    @Override
    public void intoArray(int[] a, int offset) {
        for (int i = 0; i < reorder.length; i++) {
            a[i] = reorder[i];
        }
    }

    @Override
    public int[] toArray() {
        int[] a = new int[reorder.length];
        intoArray(a, 0);
        return a;
    }

    @Override
    public int getElement(int i) {
        return reorder[i];
    }

}
