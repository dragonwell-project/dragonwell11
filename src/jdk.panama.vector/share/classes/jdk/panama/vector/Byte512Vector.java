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

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ReadOnlyBufferException;
import java.util.Arrays;
import java.util.Objects;
import java.util.function.IntUnaryOperator;

import jdk.internal.misc.Unsafe;
import jdk.internal.vm.annotation.ForceInline;
import static jdk.panama.vector.VectorIntrinsics.*;

@SuppressWarnings("cast")
final class Byte512Vector extends ByteVector {
    static final Byte512Species SPECIES = new Byte512Species();

    static final Byte512Vector ZERO = new Byte512Vector();

    static final int LENGTH = SPECIES.length();

    private final byte[] vec; // Don't access directly, use getElements() instead.

    private byte[] getElements() {
        return VectorIntrinsics.maybeRebox(this).vec;
    }

    Byte512Vector() {
        vec = new byte[SPECIES.length()];
    }

    Byte512Vector(byte[] v) {
        vec = v;
    }

    @Override
    public int length() { return LENGTH; }

    // Unary operator

    @Override
    Byte512Vector uOp(FUnOp f) {
        byte[] vec = getElements();
        byte[] res = new byte[length()];
        for (int i = 0; i < length(); i++) {
            res[i] = f.apply(i, vec[i]);
        }
        return new Byte512Vector(res);
    }

    @Override
    Byte512Vector uOp(Mask<Byte> o, FUnOp f) {
        byte[] vec = getElements();
        byte[] res = new byte[length()];
        boolean[] mbits = ((Byte512Mask)o).getBits();
        for (int i = 0; i < length(); i++) {
            res[i] = mbits[i] ? f.apply(i, vec[i]) : vec[i];
        }
        return new Byte512Vector(res);
    }

    // Binary operator

    @Override
    Byte512Vector bOp(Vector<Byte> o, FBinOp f) {
        byte[] res = new byte[length()];
        byte[] vec1 = this.getElements();
        byte[] vec2 = ((Byte512Vector)o).getElements();
        for (int i = 0; i < length(); i++) {
            res[i] = f.apply(i, vec1[i], vec2[i]);
        }
        return new Byte512Vector(res);
    }

    @Override
    Byte512Vector bOp(Vector<Byte> o1, Mask<Byte> o2, FBinOp f) {
        byte[] res = new byte[length()];
        byte[] vec1 = this.getElements();
        byte[] vec2 = ((Byte512Vector)o1).getElements();
        boolean[] mbits = ((Byte512Mask)o2).getBits();
        for (int i = 0; i < length(); i++) {
            res[i] = mbits[i] ? f.apply(i, vec1[i], vec2[i]) : vec1[i];
        }
        return new Byte512Vector(res);
    }

    // Trinary operator

    @Override
    Byte512Vector tOp(Vector<Byte> o1, Vector<Byte> o2, FTriOp f) {
        byte[] res = new byte[length()];
        byte[] vec1 = this.getElements();
        byte[] vec2 = ((Byte512Vector)o1).getElements();
        byte[] vec3 = ((Byte512Vector)o2).getElements();
        for (int i = 0; i < length(); i++) {
            res[i] = f.apply(i, vec1[i], vec2[i], vec3[i]);
        }
        return new Byte512Vector(res);
    }

    @Override
    Byte512Vector tOp(Vector<Byte> o1, Vector<Byte> o2, Mask<Byte> o3, FTriOp f) {
        byte[] res = new byte[length()];
        byte[] vec1 = getElements();
        byte[] vec2 = ((Byte512Vector)o1).getElements();
        byte[] vec3 = ((Byte512Vector)o2).getElements();
        boolean[] mbits = ((Byte512Mask)o3).getBits();
        for (int i = 0; i < length(); i++) {
            res[i] = mbits[i] ? f.apply(i, vec1[i], vec2[i], vec3[i]) : vec1[i];
        }
        return new Byte512Vector(res);
    }

    @Override
    byte rOp(byte v, FBinOp f) {
        byte[] vec = getElements();
        for (int i = 0; i < length(); i++) {
            v = f.apply(i, v, vec[i]);
        }
        return v;
    }

    // Binary operations with scalars

    @Override
    @ForceInline
    public ByteVector add(byte o) {
        return add(SPECIES.broadcast(o));
    }

    @Override
    @ForceInline
    public ByteVector add(byte o, Mask<Byte> m) {
        return add(SPECIES.broadcast(o), m);
    }

    @Override
    @ForceInline
    public ByteVector sub(byte o) {
        return sub(SPECIES.broadcast(o));
    }

    @Override
    @ForceInline
    public ByteVector sub(byte o, Mask<Byte> m) {
        return sub(SPECIES.broadcast(o), m);
    }

    @Override
    @ForceInline
    public ByteVector mul(byte o) {
        return mul(SPECIES.broadcast(o));
    }

    @Override
    @ForceInline
    public ByteVector mul(byte o, Mask<Byte> m) {
        return mul(SPECIES.broadcast(o), m);
    }

    @Override
    @ForceInline
    public ByteVector min(byte o) {
        return min(SPECIES.broadcast(o));
    }

    @Override
    @ForceInline
    public ByteVector max(byte o) {
        return max(SPECIES.broadcast(o));
    }

    @Override
    @ForceInline
    public Mask<Byte> equal(byte o) {
        return equal(SPECIES.broadcast(o));
    }

    @Override
    @ForceInline
    public Mask<Byte> notEqual(byte o) {
        return notEqual(SPECIES.broadcast(o));
    }

    @Override
    @ForceInline
    public Mask<Byte> lessThan(byte o) {
        return lessThan(SPECIES.broadcast(o));
    }

    @Override
    @ForceInline
    public Mask<Byte> lessThanEq(byte o) {
        return lessThanEq(SPECIES.broadcast(o));
    }

    @Override
    @ForceInline
    public Mask<Byte> greaterThan(byte o) {
        return greaterThan(SPECIES.broadcast(o));
    }

    @Override
    @ForceInline
    public Mask<Byte> greaterThanEq(byte o) {
        return greaterThanEq(SPECIES.broadcast(o));
    }

    @Override
    @ForceInline
    public ByteVector blend(byte o, Mask<Byte> m) {
        return blend(SPECIES.broadcast(o), m);
    }


    @Override
    @ForceInline
    public ByteVector and(byte o) {
        return and(SPECIES.broadcast(o));
    }

    @Override
    @ForceInline
    public ByteVector and(byte o, Mask<Byte> m) {
        return and(SPECIES.broadcast(o), m);
    }

    @Override
    @ForceInline
    public ByteVector or(byte o) {
        return or(SPECIES.broadcast(o));
    }

    @Override
    @ForceInline
    public ByteVector or(byte o, Mask<Byte> m) {
        return or(SPECIES.broadcast(o), m);
    }

    @Override
    @ForceInline
    public ByteVector xor(byte o) {
        return xor(SPECIES.broadcast(o));
    }

    @Override
    @ForceInline
    public ByteVector xor(byte o, Mask<Byte> m) {
        return xor(SPECIES.broadcast(o), m);
    }

    @Override
    @ForceInline
    public Byte512Vector neg() {
        return SPECIES.zero().sub(this);
    }

    // Unary operations

    @ForceInline
    @Override
    public Byte512Vector neg(Mask<Byte> m) {
        return blend(neg(), m);
    }

    @Override
    @ForceInline
    public Byte512Vector abs() {
        return VectorIntrinsics.unaryOp(
            VECTOR_OP_ABS, Byte512Vector.class, byte.class, LENGTH,
            this,
            v1 -> v1.uOp((i, a) -> (byte) Math.abs(a)));
    }

    @ForceInline
    @Override
    public Byte512Vector abs(Mask<Byte> m) {
        return blend(abs(), m);
    }


    @Override
    @ForceInline
    public Byte512Vector not() {
        return VectorIntrinsics.unaryOp(
            VECTOR_OP_NOT, Byte512Vector.class, byte.class, LENGTH,
            this,
            v1 -> v1.uOp((i, a) -> (byte) ~a));
    }

    @ForceInline
    @Override
    public Byte512Vector not(Mask<Byte> m) {
        return blend(not(), m);
    }
    // Binary operations

    @Override
    @ForceInline
    public Byte512Vector add(Vector<Byte> o) {
        Objects.requireNonNull(o);
        Byte512Vector v = (Byte512Vector)o;
        return VectorIntrinsics.binaryOp(
            VECTOR_OP_ADD, Byte512Vector.class, byte.class, LENGTH,
            this, v,
            (v1, v2) -> v1.bOp(v2, (i, a, b) -> (byte)(a + b)));
    }

    @Override
    @ForceInline
    public Byte512Vector add(Vector<Byte> v, Mask<Byte> m) {
        return blend(add(v), m);
    }

    @Override
    @ForceInline
    public Byte512Vector sub(Vector<Byte> o) {
        Objects.requireNonNull(o);
        Byte512Vector v = (Byte512Vector)o;
        return VectorIntrinsics.binaryOp(
            VECTOR_OP_SUB, Byte512Vector.class, byte.class, LENGTH,
            this, v,
            (v1, v2) -> v1.bOp(v2, (i, a, b) -> (byte)(a - b)));
    }

    @Override
    @ForceInline
    public Byte512Vector sub(Vector<Byte> v, Mask<Byte> m) {
        return blend(sub(v), m);
    }

    @Override
    @ForceInline
    public Byte512Vector mul(Vector<Byte> o) {
        Objects.requireNonNull(o);
        Byte512Vector v = (Byte512Vector)o;
        return VectorIntrinsics.binaryOp(
            VECTOR_OP_MUL, Byte512Vector.class, byte.class, LENGTH,
            this, v,
            (v1, v2) -> v1.bOp(v2, (i, a, b) -> (byte)(a * b)));
    }

    @Override
    @ForceInline
    public Byte512Vector mul(Vector<Byte> v, Mask<Byte> m) {
        return blend(mul(v), m);
    }

    @Override
    @ForceInline
    public Byte512Vector min(Vector<Byte> o) {
        Objects.requireNonNull(o);
        Byte512Vector v = (Byte512Vector)o;
        return (Byte512Vector) VectorIntrinsics.binaryOp(
            VECTOR_OP_MIN, Byte512Vector.class, byte.class, LENGTH,
            this, v,
            (v1, v2) -> ((Byte512Vector)v1).bOp(v2, (i, a, b) -> (byte) ((a < b) ? a : b)));
    }

    @Override
    @ForceInline
    public Byte512Vector min(Vector<Byte> v, Mask<Byte> m) {
        return blend(min(v), m);
    }

    @Override
    @ForceInline
    public Byte512Vector max(Vector<Byte> o) {
        Objects.requireNonNull(o);
        Byte512Vector v = (Byte512Vector)o;
        return VectorIntrinsics.binaryOp(
            VECTOR_OP_MAX, Byte512Vector.class, byte.class, LENGTH,
            this, v,
            (v1, v2) -> v1.bOp(v2, (i, a, b) -> (byte) ((a > b) ? a : b)));
        }

    @Override
    @ForceInline
    public Byte512Vector max(Vector<Byte> v, Mask<Byte> m) {
        return blend(max(v), m);
    }

    @Override
    @ForceInline
    public Byte512Vector and(Vector<Byte> o) {
        Objects.requireNonNull(o);
        Byte512Vector v = (Byte512Vector)o;
        return VectorIntrinsics.binaryOp(
            VECTOR_OP_AND, Byte512Vector.class, byte.class, LENGTH,
            this, v,
            (v1, v2) -> v1.bOp(v2, (i, a, b) -> (byte)(a & b)));
    }

    @Override
    @ForceInline
    public Byte512Vector or(Vector<Byte> o) {
        Objects.requireNonNull(o);
        Byte512Vector v = (Byte512Vector)o;
        return VectorIntrinsics.binaryOp(
            VECTOR_OP_OR, Byte512Vector.class, byte.class, LENGTH,
            this, v,
            (v1, v2) -> v1.bOp(v2, (i, a, b) -> (byte)(a | b)));
    }

    @Override
    @ForceInline
    public Byte512Vector xor(Vector<Byte> o) {
        Objects.requireNonNull(o);
        Byte512Vector v = (Byte512Vector)o;
        return VectorIntrinsics.binaryOp(
            VECTOR_OP_XOR, Byte512Vector.class, byte.class, LENGTH,
            this, v,
            (v1, v2) -> v1.bOp(v2, (i, a, b) -> (byte)(a ^ b)));
    }

    @Override
    @ForceInline
    public Byte512Vector and(Vector<Byte> v, Mask<Byte> m) {
        return blend(and(v), m);
    }

    @Override
    @ForceInline
    public Byte512Vector or(Vector<Byte> v, Mask<Byte> m) {
        return blend(or(v), m);
    }

    @Override
    @ForceInline
    public Byte512Vector xor(Vector<Byte> v, Mask<Byte> m) {
        return blend(xor(v), m);
    }

   public Byte512Vector shiftL(int s) {
       byte[] vec = getElements();
       byte[] res = new byte[length()];
       for (int i = 0; i < length(); i++){
           res[i] = (byte)(vec[i] << s);
       }
       return new Byte512Vector(res);
   }

   public Byte512Vector shiftR(int s) {
       byte[] vec = getElements();
       byte[] res = new byte[length()];
       for (int i = 0; i < length(); i++){
           res[i] = (byte)(vec[i] >>> s);
       }
       return new Byte512Vector(res);
   }

   public Byte512Vector aShiftR(int s) {
       byte[] vec = getElements();
       byte[] res = new byte[length()];
       for (int i = 0; i < length(); i++){
           res[i] = (byte)(vec[i] >> s);
       }
       return new Byte512Vector(res);
   }
    // Ternary operations


    // Type specific horizontal reductions

    @Override
    @ForceInline
    public byte addAll() {
        return (byte) VectorIntrinsics.reductionCoerced(
            VECTOR_OP_ADD, Byte512Vector.class, byte.class, LENGTH,
            this,
            v -> (long) v.rOp((byte) 0, (i, a, b) -> (byte) (a + b)));
    }

    @Override
    @ForceInline
    public byte andAll() {
        return (byte) VectorIntrinsics.reductionCoerced(
            VECTOR_OP_AND, Byte512Vector.class, byte.class, LENGTH,
            this,
            v -> (long) v.rOp((byte) -1, (i, a, b) -> (byte) (a & b)));
    }

    @Override
    @ForceInline
    public byte andAll(Mask<Byte> m) {
        return blend(SPECIES.broadcast((byte) -1), m).andAll();
    }

    @Override
    @ForceInline
    public byte minAll() {
        return (byte) VectorIntrinsics.reductionCoerced(
            VECTOR_OP_MIN, Byte512Vector.class, byte.class, LENGTH,
            this,
            v -> (long) v.rOp(Byte.MAX_VALUE , (i, a, b) -> (byte) ((a < b) ? a : b)));
    }

    @Override
    @ForceInline
    public byte maxAll() {
        return (byte) VectorIntrinsics.reductionCoerced(
            VECTOR_OP_MAX, Byte512Vector.class, byte.class, LENGTH,
            this,
            v -> (long) v.rOp(Byte.MIN_VALUE , (i, a, b) -> (byte) ((a > b) ? a : b)));
    }

    @Override
    @ForceInline
    public byte mulAll() {
        return (byte) VectorIntrinsics.reductionCoerced(
            VECTOR_OP_MUL, Byte512Vector.class, byte.class, LENGTH,
            this,
            v -> (long) v.rOp((byte) 1, (i, a, b) -> (byte) (a * b)));
    }

    @Override
    @ForceInline
    public byte subAll() {
        return (byte) VectorIntrinsics.reductionCoerced(
            VECTOR_OP_SUB, Byte512Vector.class, byte.class, LENGTH,
            this,
            v -> (long) v.rOp((byte) 0, (i, a, b) -> (byte) (a - b)));
    }

    @Override
    @ForceInline
    public byte orAll() {
        return (byte) VectorIntrinsics.reductionCoerced(
            VECTOR_OP_OR, Byte512Vector.class, byte.class, LENGTH,
            this,
            v -> (long) v.rOp((byte) 0, (i, a, b) -> (byte) (a | b)));
    }

    @Override
    @ForceInline
    public byte orAll(Mask<Byte> m) {
        return blend(SPECIES.broadcast((byte) 0), m).orAll();
    }

    @Override
    @ForceInline
    public byte xorAll() {
        return (byte) VectorIntrinsics.reductionCoerced(
            VECTOR_OP_XOR, Byte512Vector.class, byte.class, LENGTH,
            this,
            v -> (long) v.rOp((byte) 0, (i, a, b) -> (byte) (a ^ b)));
    }

    @Override
    @ForceInline
    public byte xorAll(Mask<Byte> m) {
        return blend(SPECIES.broadcast((byte) 0), m).xorAll();
    }


    @Override
    @ForceInline
    public byte addAll(Mask<Byte> m) {
        return blend(SPECIES.broadcast((byte) 0), m).addAll();
    }

    @Override
    @ForceInline
    public byte subAll(Mask<Byte> m) {
        return blend(SPECIES.broadcast((byte) 0), m).subAll();
    }

    @Override
    @ForceInline
    public byte mulAll(Mask<Byte> m) {
        return blend(SPECIES.broadcast((byte) 1), m).mulAll();
    }

    @Override
    @ForceInline
    public byte minAll(Mask<Byte> m) {
        return blend(SPECIES.broadcast(Byte.MAX_VALUE), m).minAll();
    }

    @Override
    @ForceInline
    public byte maxAll(Mask<Byte> m) {
        return blend(SPECIES.broadcast(Byte.MIN_VALUE), m).maxAll();
    }

    @Override
    @ForceInline
    public Shuffle<Byte> toShuffle() {
        byte[] a = toArray();
        int[] sa = new int[a.length];
        for (int i = 0; i < a.length; i++) {
            sa[i] = (int) a[i];
        }
        return SPECIES.shuffleFromArray(sa, 0);
    }

    // Memory operations

    private static final int ARRAY_SHIFT = 31 - Integer.numberOfLeadingZeros(Unsafe.ARRAY_BYTE_INDEX_SCALE);

    @Override
    @ForceInline
    public void intoArray(byte[] a, int ix) {
        Objects.requireNonNull(a);
        ix = VectorIntrinsics.checkIndex(ix, a.length, LENGTH);
        VectorIntrinsics.store(Byte512Vector.class, byte.class, LENGTH,
                               a, (((long) ix) << ARRAY_SHIFT) + Unsafe.ARRAY_BYTE_BASE_OFFSET,
                               this,
                               a, ix,
                               (arr, idx, v) -> v.forEach((i, e) -> arr[idx + i] = e));
    }

    @Override
    @ForceInline
    public final void intoArray(byte[] a, int ax, Mask<Byte> m) {
        Byte512Vector oldVal = SPECIES.fromArray(a, ax);
        Byte512Vector newVal = oldVal.blend(this, m);
        newVal.intoArray(a, ax);
    }

    @Override
    @ForceInline
    public void intoByteArray(byte[] a, int ix) {
        Objects.requireNonNull(a);
        ix = VectorIntrinsics.checkIndex(ix, a.length, bitSize() / Byte.SIZE);
        VectorIntrinsics.store(Byte512Vector.class, byte.class, LENGTH,
                               a, ((long) ix) + Unsafe.ARRAY_BYTE_BASE_OFFSET,
                               this,
                               a, ix,
                               (c, idx, v) -> {
                                   ByteBuffer bbc = ByteBuffer.wrap(c, idx, c.length - idx).order(ByteOrder.nativeOrder());
                                   ByteBuffer tb = bbc;
                                   v.forEach((i, e) -> tb.put(e));
                               });
    }

    @Override
    @ForceInline
    public final void intoByteArray(byte[] a, int ix, Mask<Byte> m) {
        Byte512Vector oldVal = SPECIES.fromByteArray(a, ix);
        Byte512Vector newVal = oldVal.blend(this, m);
        newVal.intoByteArray(a, ix);
    }

    @Override
    @ForceInline
    public void intoByteBuffer(ByteBuffer bb, int ix) {
        if (bb.order() != ByteOrder.nativeOrder()) {
            throw new IllegalArgumentException();
        }
        if (bb.isReadOnly()) {
            throw new ReadOnlyBufferException();
        }
        ix = VectorIntrinsics.checkIndex(ix, bb.limit(), bitSize() / Byte.SIZE);
        VectorIntrinsics.store(Byte512Vector.class, byte.class, LENGTH,
                               U.getReference(bb, BYTE_BUFFER_HB), ix + U.getLong(bb, BUFFER_ADDRESS),
                               this,
                               bb, ix,
                               (c, idx, v) -> {
                                   ByteBuffer bbc = c.duplicate().position(idx).order(ByteOrder.nativeOrder());
                                   ByteBuffer tb = bbc;
                                   v.forEach((i, e) -> tb.put(e));
                               });
    }

    @Override
    @ForceInline
    public void intoByteBuffer(ByteBuffer bb, int ix, Mask<Byte> m) {
        Byte512Vector oldVal = SPECIES.fromByteBuffer(bb, ix);
        Byte512Vector newVal = oldVal.blend(this, m);
        newVal.intoByteBuffer(bb, ix);
    }

    //

    @Override
    public String toString() {
        return Arrays.toString(getElements());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || this.getClass() != o.getClass()) return false;

        Byte512Vector that = (Byte512Vector) o;
        return this.equal(that).allTrue();
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(vec);
    }

    // Binary test

    @Override
    Byte512Mask bTest(Vector<Byte> o, FBinTest f) {
        byte[] vec1 = getElements();
        byte[] vec2 = ((Byte512Vector)o).getElements();
        boolean[] bits = new boolean[length()];
        for (int i = 0; i < length(); i++){
            bits[i] = f.apply(i, vec1[i], vec2[i]);
        }
        return new Byte512Mask(bits);
    }

    // Comparisons

    @Override
    @ForceInline
    public Byte512Mask equal(Vector<Byte> o) {
        Objects.requireNonNull(o);
        Byte512Vector v = (Byte512Vector)o;

        return VectorIntrinsics.compare(
            BT_eq, Byte512Vector.class, Byte512Mask.class, byte.class, LENGTH,
            this, v,
            (v1, v2) -> v1.bTest(v2, (i, a, b) -> a == b));
    }

    @Override
    @ForceInline
    public Byte512Mask notEqual(Vector<Byte> o) {
        Objects.requireNonNull(o);
        Byte512Vector v = (Byte512Vector)o;

        return VectorIntrinsics.compare(
            BT_ne, Byte512Vector.class, Byte512Mask.class, byte.class, LENGTH,
            this, v,
            (v1, v2) -> v1.bTest(v2, (i, a, b) -> a != b));
    }

    @Override
    @ForceInline
    public Byte512Mask lessThan(Vector<Byte> o) {
        Objects.requireNonNull(o);
        Byte512Vector v = (Byte512Vector)o;

        return VectorIntrinsics.compare(
            BT_lt, Byte512Vector.class, Byte512Mask.class, byte.class, LENGTH,
            this, v,
            (v1, v2) -> v1.bTest(v2, (i, a, b) -> a < b));
    }

    @Override
    @ForceInline
    public Byte512Mask lessThanEq(Vector<Byte> o) {
        Objects.requireNonNull(o);
        Byte512Vector v = (Byte512Vector)o;

        return VectorIntrinsics.compare(
            BT_le, Byte512Vector.class, Byte512Mask.class, byte.class, LENGTH,
            this, v,
            (v1, v2) -> v1.bTest(v2, (i, a, b) -> a <= b));
    }

    @Override
    @ForceInline
    public Byte512Mask greaterThan(Vector<Byte> o) {
        Objects.requireNonNull(o);
        Byte512Vector v = (Byte512Vector)o;

        return (Byte512Mask) VectorIntrinsics.compare(
            BT_gt, Byte512Vector.class, Byte512Mask.class, byte.class, LENGTH,
            this, v,
            (v1, v2) -> v1.bTest(v2, (i, a, b) -> a > b));
    }

    @Override
    @ForceInline
    public Byte512Mask greaterThanEq(Vector<Byte> o) {
        Objects.requireNonNull(o);
        Byte512Vector v = (Byte512Vector)o;

        return VectorIntrinsics.compare(
            BT_ge, Byte512Vector.class, Byte512Mask.class, byte.class, LENGTH,
            this, v,
            (v1, v2) -> v1.bTest(v2, (i, a, b) -> a >= b));
    }

    // Foreach

    @Override
    void forEach(FUnCon f) {
        byte[] vec = getElements();
        for (int i = 0; i < length(); i++) {
            f.apply(i, vec[i]);
        }
    }

    @Override
    void forEach(Mask<Byte> o, FUnCon f) {
        boolean[] mbits = ((Byte512Mask)o).getBits();
        forEach((i, a) -> {
            if (mbits[i]) { f.apply(i, a); }
        });
    }



    @Override
    public Byte512Vector rotateEL(int j) {
        byte[] vec = getElements();
        byte[] res = new byte[length()];
        for (int i = 0; i < length(); i++){
            res[(j + i) % length()] = vec[i];
        }
        return new Byte512Vector(res);
    }

    @Override
    public Byte512Vector rotateER(int j) {
        byte[] vec = getElements();
        byte[] res = new byte[length()];
        for (int i = 0; i < length(); i++){
            int z = i - j;
            if(j < 0) {
                res[length() + z] = vec[i];
            } else {
                res[z] = vec[i];
            }
        }
        return new Byte512Vector(res);
    }

    @Override
    public Byte512Vector shiftEL(int j) {
        byte[] vec = getElements();
        byte[] res = new byte[length()];
        for (int i = 0; i < length() - j; i++) {
            res[i] = vec[i + j];
        }
        return new Byte512Vector(res);
    }

    @Override
    public Byte512Vector shiftER(int j) {
        byte[] vec = getElements();
        byte[] res = new byte[length()];
        for (int i = 0; i < length() - j; i++){
            res[i + j] = vec[i];
        }
        return new Byte512Vector(res);
    }

    @Override
    @ForceInline
    public Byte512Vector rearrange(Vector<Byte> v,
                                  Shuffle<Byte> s, Mask<Byte> m) {
        return this.rearrange(s).blend(v.rearrange(s), m);
    }

    @Override
    @ForceInline
    public Byte512Vector rearrange(Shuffle<Byte> o1) {
    Objects.requireNonNull(o1);
    Byte512Shuffle s =  (Byte512Shuffle)o1;

        return VectorIntrinsics.rearrangeOp(
            Byte512Vector.class, Byte512Shuffle.class, byte.class, LENGTH,
            this, s,
            (v1, s_) -> v1.uOp((i, a) -> {
            byte[] vec = this.getElements();
            int ei = s_.getElement(i);
            return vec[ei];
        }));
    }

    @Override
    @ForceInline
    public Byte512Vector blend(Vector<Byte> o1, Mask<Byte> o2) {
        Objects.requireNonNull(o1);
        Objects.requireNonNull(o2);
        Byte512Vector v = (Byte512Vector)o1;
        Byte512Mask   m = (Byte512Mask)o2;

        return VectorIntrinsics.blend(
            Byte512Vector.class, Byte512Mask.class, byte.class, LENGTH,
            this, v, m,
            (v1, v2, m_) -> v1.bOp(v2, (i, a, b) -> m_.getElement(i) ? b : a));
    }

    // Accessors

    @Override
    public byte get(int i) {
        if (i < 0 || i >= LENGTH) {
            throw new IllegalArgumentException("Index " + i + " must be zero or positive, and less than " + LENGTH);
        }
        return (byte) VectorIntrinsics.extract(
                                Byte512Vector.class, byte.class, LENGTH,
                                this, i,
                                (vec, ix) -> {
                                    byte[] vecarr = vec.getElements();
                                    return (long)vecarr[ix];
                                });
    }

    @Override
    public Byte512Vector with(int i, byte e) {
        if (i < 0 || i >= LENGTH) {
            throw new IllegalArgumentException("Index " + i + " must be zero or positive, and less than " + LENGTH);
        }
        return VectorIntrinsics.insert(
                                Byte512Vector.class, byte.class, LENGTH,
                                this, i, (long)e,
                                (v, ix, bits) -> {
                                    byte[] res = v.getElements().clone();
                                    res[ix] = (byte)bits;
                                    return new Byte512Vector(res);
                                });
    }

    // Mask

    static final class Byte512Mask extends AbstractMask<Byte> {
        static final Byte512Mask TRUE_MASK = new Byte512Mask(true);
        static final Byte512Mask FALSE_MASK = new Byte512Mask(false);

        private final boolean[] bits; // Don't access directly, use getBits() instead.

        public Byte512Mask(boolean[] bits) {
            this(bits, 0);
        }

        public Byte512Mask(boolean[] bits, int offset) {
            boolean[] a = new boolean[species().length()];
            for (int i = 0; i < a.length; i++) {
                a[i] = bits[offset + i];
            }
            this.bits = a;
        }

        public Byte512Mask(boolean val) {
            boolean[] bits = new boolean[species().length()];
            Arrays.fill(bits, val);
            this.bits = bits;
        }

        boolean[] getBits() {
            return VectorIntrinsics.maybeRebox(this).bits;
        }

        @Override
        Byte512Mask uOp(MUnOp f) {
            boolean[] res = new boolean[species().length()];
            boolean[] bits = getBits();
            for (int i = 0; i < species().length(); i++) {
                res[i] = f.apply(i, bits[i]);
            }
            return new Byte512Mask(res);
        }

        @Override
        Byte512Mask bOp(Mask<Byte> o, MBinOp f) {
            boolean[] res = new boolean[species().length()];
            boolean[] bits = getBits();
            boolean[] mbits = ((Byte512Mask)o).getBits();
            for (int i = 0; i < species().length(); i++) {
                res[i] = f.apply(i, bits[i], mbits[i]);
            }
            return new Byte512Mask(res);
        }

        @Override
        public Byte512Species species() {
            return SPECIES;
        }

        @Override
        public Byte512Vector toVector() {
            byte[] res = new byte[species().length()];
            boolean[] bits = getBits();
            for (int i = 0; i < species().length(); i++) {
                // -1 will result in the most significant bit being set in
                // addition to some or all other bits
                res[i] = (byte) (bits[i] ? -1 : 0);
            }
            return new Byte512Vector(res);
        }

        // Unary operations

        @Override
        @ForceInline
        public Byte512Mask not() {
            return (Byte512Mask) VectorIntrinsics.unaryOp(
                                             VECTOR_OP_NOT, Byte512Mask.class, byte.class, LENGTH,
                                             this,
                                             (m1) -> m1.uOp((i, a) -> !a));
        }

        // Binary operations

        @Override
        @ForceInline
        public Byte512Mask and(Mask<Byte> o) {
            Objects.requireNonNull(o);
            Byte512Mask m = (Byte512Mask)o;
            return VectorIntrinsics.binaryOp(VECTOR_OP_AND, Byte512Mask.class, byte.class, LENGTH,
                                             this, m,
                                             (m1, m2) -> m1.bOp(m2, (i, a, b) -> a & b));
        }

        @Override
        @ForceInline
        public Byte512Mask or(Mask<Byte> o) {
            Objects.requireNonNull(o);
            Byte512Mask m = (Byte512Mask)o;
            return VectorIntrinsics.binaryOp(VECTOR_OP_OR, Byte512Mask.class, byte.class, LENGTH,
                                             this, m,
                                             (m1, m2) -> m1.bOp(m2, (i, a, b) -> a | b));
        }

        // Reductions

        @Override
        @ForceInline
        public boolean anyTrue() {
            return VectorIntrinsics.test(COND_notZero, Byte512Mask.class, byte.class, LENGTH,
                                         this, this,
                                         (m1, m2) -> super.anyTrue());
        }

        @Override
        @ForceInline
        public boolean allTrue() {
            return VectorIntrinsics.test(COND_carrySet, Byte512Mask.class, byte.class, LENGTH,
                                         this, species().maskAllTrue(),
                                         (m1, m2) -> super.allTrue());
        }
    }

    // Shuffle

    static final class Byte512Shuffle extends AbstractShuffle<Byte> {
        Byte512Shuffle(byte[] reorder) {
            super(reorder);
        }

        public Byte512Shuffle(int[] reorder) {
            super(reorder);
        }

        public Byte512Shuffle(int[] reorder, int i) {
            super(reorder, i);
        }

        public Byte512Shuffle(IntUnaryOperator f) {
            super(f);
        }

        @Override
        public Byte512Species species() {
            return SPECIES;
        }

        @Override
        public Byte512Vector toVector() {
            byte[] va = new byte[SPECIES.length()];
            for (int i = 0; i < va.length; i++) {
              va[i] = (byte) getElement(i);
            }
            return species().fromArray(va, 0);
        }

        @Override
        public Byte512Shuffle rearrange(Vector.Shuffle<Byte> o) {
            Byte512Shuffle s = (Byte512Shuffle) o;
            byte[] r = new byte[reorder.length];
            for (int i = 0; i < reorder.length; i++) {
                r[i] = reorder[s.reorder[i]];
            }
            return new Byte512Shuffle(r);
        }
    }

    // Species

    @Override
    public Byte512Species species() {
        return SPECIES;
    }

    static final class Byte512Species extends ByteSpecies {
        static final int BIT_SIZE = Shape.S_512_BIT.bitSize();

        static final int LENGTH = BIT_SIZE / Byte.SIZE;

        @Override
        public String toString() {
           StringBuilder sb = new StringBuilder("Shape[");
           sb.append(bitSize()).append(" bits, ");
           sb.append(length()).append(" ").append(byte.class.getSimpleName()).append("s x ");
           sb.append(elementSize()).append(" bits");
           sb.append("]");
           return sb.toString();
        }

        @Override
        @ForceInline
        public int bitSize() {
            return BIT_SIZE;
        }

        @Override
        @ForceInline
        public int length() {
            return LENGTH;
        }

        @Override
        @ForceInline
        public Class<Byte> elementType() {
            return byte.class;
        }

        @Override
        @ForceInline
        public int elementSize() {
            return Byte.SIZE;
        }

        @Override
        @ForceInline
        public Shape shape() {
            return Shape.S_512_BIT;
        }

        @Override
        Byte512Vector op(FOp f) {
            byte[] res = new byte[length()];
            for (int i = 0; i < length(); i++) {
                res[i] = f.apply(i);
            }
            return new Byte512Vector(res);
        }

        @Override
        Byte512Vector op(Mask<Byte> o, FOp f) {
            byte[] res = new byte[length()];
            boolean[] mbits = ((Byte512Mask)o).getBits();
            for (int i = 0; i < length(); i++) {
                if (mbits[i]) {
                    res[i] = f.apply(i);
                }
            }
            return new Byte512Vector(res);
        }

        @Override
        Byte512Mask opm(FOpm f) {
            boolean[] res = new boolean[length()];
            for (int i = 0; i < length(); i++) {
                res[i] = (boolean)f.apply(i);
            }
            return new Byte512Mask(res);
        }

        // Factories

        @Override
        public Byte512Mask maskFromValues(boolean... bits) {
            return new Byte512Mask(bits);
        }

        @Override
        public Byte512Shuffle shuffle(IntUnaryOperator f) {
            return new Byte512Shuffle(f);
        }

        @Override
        public Byte512Shuffle shuffleIota() {
            return new Byte512Shuffle(AbstractShuffle.IDENTITY);
        }

        @Override
        public Byte512Shuffle shuffleFromValues(int... ixs) {
            return new Byte512Shuffle(ixs);
        }

        @Override
        public Byte512Shuffle shuffleFromArray(int[] ixs, int i) {
            return new Byte512Shuffle(ixs, i);
        }

        @Override
        @ForceInline
        public Byte512Vector zero() {
            return VectorIntrinsics.broadcastCoerced(Byte512Vector.class, byte.class, LENGTH,
                                                     0,
                                                     (z -> ZERO));
        }

        @Override
        @ForceInline
        public Byte512Vector broadcast(byte e) {
            return VectorIntrinsics.broadcastCoerced(
                Byte512Vector.class, byte.class, LENGTH,
                e,
                ((long bits) -> SPECIES.op(i -> (byte)bits)));
        }

        @Override
        @ForceInline
        public Byte512Mask maskAllTrue() {
            return VectorIntrinsics.broadcastCoerced(Byte512Mask.class, byte.class, LENGTH,
                                                     (byte)-1,
                                                     (z -> Byte512Mask.TRUE_MASK));
        }

        @Override
        @ForceInline
        public Byte512Mask maskAllFalse() {
            return VectorIntrinsics.broadcastCoerced(Byte512Mask.class, byte.class, LENGTH,
                                                     0,
                                                     (z -> Byte512Mask.FALSE_MASK));
        }

        @Override
        @ForceInline
        public Byte512Vector scalars(byte... es) {
            Objects.requireNonNull(es);
            int ix = VectorIntrinsics.checkIndex(0, es.length, LENGTH);
            return VectorIntrinsics.load(Byte512Vector.class, byte.class, LENGTH,
                                         es, Unsafe.ARRAY_BYTE_BASE_OFFSET,
                                         es, ix,
                                         (c, idx) -> op(n -> c[idx + n]));
        }

        @Override
        @ForceInline
        public Byte512Mask maskFromArray(boolean[] bits, int ix) {
            Objects.requireNonNull(bits);
            ix = VectorIntrinsics.checkIndex(ix, bits.length, LENGTH);
            return VectorIntrinsics.load(Byte512Mask.class, byte.class, LENGTH,
                                         bits, (long)ix + Unsafe.ARRAY_BOOLEAN_BASE_OFFSET,
                                         bits, ix,
                                         (c, idx) -> opm(n -> c[idx + n]));
        }

        @Override
        @ForceInline
        public Byte512Vector fromArray(byte[] a, int ix) {
            Objects.requireNonNull(a);
            ix = VectorIntrinsics.checkIndex(ix, a.length, LENGTH);
            return VectorIntrinsics.load(Byte512Vector.class, byte.class, LENGTH,
                                         a, (((long) ix) << ARRAY_SHIFT) + Unsafe.ARRAY_BYTE_BASE_OFFSET,
                                         a, ix,
                                         (c, idx) -> op(n -> c[idx + n]));
        }

        @Override
        @ForceInline
        public Byte512Vector fromArray(byte[] a, int ax, Mask<Byte> m) {
            return zero().blend(fromArray(a, ax), m);
        }

        @Override
        @ForceInline
        public Byte512Vector fromByteArray(byte[] a, int ix) {
            Objects.requireNonNull(a);
            ix = VectorIntrinsics.checkIndex(ix, a.length, bitSize() / Byte.SIZE);
            return VectorIntrinsics.load(Byte512Vector.class, byte.class, LENGTH,
                                         a, ((long) ix) + Unsafe.ARRAY_BYTE_BASE_OFFSET,
                                         a, ix,
                                         (c, idx) -> {
                                             ByteBuffer bbc = ByteBuffer.wrap(c, idx, a.length - idx).order(ByteOrder.nativeOrder());
                                             ByteBuffer tb = bbc;
                                             return op(i -> tb.get());
                                         });
        }

        @Override
        @ForceInline
        public Byte512Vector fromByteArray(byte[] a, int ix, Mask<Byte> m) {
            return zero().blend(fromByteArray(a, ix), m);
        }

        @Override
        @ForceInline
        public Byte512Vector fromByteBuffer(ByteBuffer bb, int ix) {
            if (bb.order() != ByteOrder.nativeOrder()) {
                throw new IllegalArgumentException();
            }
            ix = VectorIntrinsics.checkIndex(ix, bb.limit(), bitSize() / Byte.SIZE);
            return VectorIntrinsics.load(Byte512Vector.class, byte.class, LENGTH,
                                         U.getReference(bb, BYTE_BUFFER_HB), U.getLong(bb, BUFFER_ADDRESS) + ix,
                                         bb, ix,
                                         (c, idx) -> {
                                             ByteBuffer bbc = c.duplicate().position(idx).order(ByteOrder.nativeOrder());
                                             ByteBuffer tb = bbc;
                                             return op(i -> tb.get());
                                         });
        }

        @Override
        @ForceInline
        public Byte512Vector fromByteBuffer(ByteBuffer bb, int ix, Mask<Byte> m) {
            return zero().blend(fromByteBuffer(bb, ix), m);
        }

        @Override
        @ForceInline
        @SuppressWarnings("unchecked")
        public <F> Byte512Vector cast(Vector<F> o) {
            if (o.length() != LENGTH)
                throw new IllegalArgumentException("Vector length this species length differ");

            return VectorIntrinsics.cast(
                o.getClass(),
                o.elementType(), LENGTH,
                Byte512Vector.class,
                byte.class, LENGTH,
                o, this,
                (s, v) -> s.castDefault(v)
            );
        }

        @SuppressWarnings("unchecked")
        @ForceInline
        private <F> Byte512Vector castDefault(Vector<F> v) {
            // Allocate array of required size
            int limit = length();
            byte[] a = new byte[limit];

            Class<?> vtype = v.species().elementType();
            if (vtype == byte.class) {
                ByteVector tv = (ByteVector)v;
                for (int i = 0; i < limit; i++) {
                    a[i] = (byte) tv.get(i);
                }
            } else if (vtype == short.class) {
                ShortVector tv = (ShortVector)v;
                for (int i = 0; i < limit; i++) {
                    a[i] = (byte) tv.get(i);
                }
            } else if (vtype == int.class) {
                IntVector tv = (IntVector)v;
                for (int i = 0; i < limit; i++) {
                    a[i] = (byte) tv.get(i);
                }
            } else if (vtype == long.class){
                LongVector tv = (LongVector)v;
                for (int i = 0; i < limit; i++) {
                    a[i] = (byte) tv.get(i);
                }
            } else if (vtype == float.class){
                FloatVector tv = (FloatVector)v;
                for (int i = 0; i < limit; i++) {
                    a[i] = (byte) tv.get(i);
                }
            } else if (vtype == double.class){
                DoubleVector tv = (DoubleVector)v;
                for (int i = 0; i < limit; i++) {
                    a[i] = (byte) tv.get(i);
                }
            } else {
                throw new UnsupportedOperationException("Bad lane type for casting.");
            }

            return scalars(a);
        }

        @Override
        @ForceInline
        public <E> Byte512Mask cast(Mask<E> m) {
            if (m.length() != LENGTH)
                throw new IllegalArgumentException("Mask length this species length differ");
            return new Byte512Mask(m.toArray());
        }

        @Override
        @ForceInline
        public <E> Byte512Shuffle cast(Shuffle<E> s) {
            if (s.length() != LENGTH)
                throw new IllegalArgumentException("Shuffle length this species length differ");
            return new Byte512Shuffle(s.toArray());
        }

        @Override
        @ForceInline
        @SuppressWarnings("unchecked")
        public <F> Byte512Vector rebracket(Vector<F> o) {
            Objects.requireNonNull(o);
            if (o.elementType() == byte.class) {
                Byte512Vector so = (Byte512Vector)o;
                return VectorIntrinsics.reinterpret(
                    Byte512Vector.class,
                    byte.class, so.length(),
                    Byte512Vector.class,
                    byte.class, LENGTH,
                    so, this,
                    (s, v) -> (Byte512Vector) s.reshape(v)
                );
            } else if (o.elementType() == short.class) {
                Short512Vector so = (Short512Vector)o;
                return VectorIntrinsics.reinterpret(
                    Short512Vector.class,
                    short.class, so.length(),
                    Byte512Vector.class,
                    byte.class, LENGTH,
                    so, this,
                    (s, v) -> (Byte512Vector) s.reshape(v)
                );
            } else if (o.elementType() == int.class) {
                Int512Vector so = (Int512Vector)o;
                return VectorIntrinsics.reinterpret(
                    Int512Vector.class,
                    int.class, so.length(),
                    Byte512Vector.class,
                    byte.class, LENGTH,
                    so, this,
                    (s, v) -> (Byte512Vector) s.reshape(v)
                );
            } else if (o.elementType() == long.class) {
                Long512Vector so = (Long512Vector)o;
                return VectorIntrinsics.reinterpret(
                    Long512Vector.class,
                    long.class, so.length(),
                    Byte512Vector.class,
                    byte.class, LENGTH,
                    so, this,
                    (s, v) -> (Byte512Vector) s.reshape(v)
                );
            } else if (o.elementType() == float.class) {
                Float512Vector so = (Float512Vector)o;
                return VectorIntrinsics.reinterpret(
                    Float512Vector.class,
                    float.class, so.length(),
                    Byte512Vector.class,
                    byte.class, LENGTH,
                    so, this,
                    (s, v) -> (Byte512Vector) s.reshape(v)
                );
            } else if (o.elementType() == double.class) {
                Double512Vector so = (Double512Vector)o;
                return VectorIntrinsics.reinterpret(
                    Double512Vector.class,
                    double.class, so.length(),
                    Byte512Vector.class,
                    byte.class, LENGTH,
                    so, this,
                    (s, v) -> (Byte512Vector) s.reshape(v)
                );
            } else {
                throw new InternalError("Unimplemented type");
            }
        }

        @Override
        @ForceInline
        @SuppressWarnings("unchecked")
        public Byte512Vector resize(Vector<Byte> o) {
            Objects.requireNonNull(o);
            if (o.bitSize() == 64 && (o instanceof Byte64Vector)) {
                Byte64Vector so = (Byte64Vector)o;
                return VectorIntrinsics.reinterpret(
                    Byte64Vector.class,
                    byte.class, so.length(),
                    Byte512Vector.class,
                    byte.class, LENGTH,
                    so, this,
                    (s, v) -> (Byte512Vector) s.reshape(v)
                );
            } else if (o.bitSize() == 128 && (o instanceof Byte128Vector)) {
                Byte128Vector so = (Byte128Vector)o;
                return VectorIntrinsics.reinterpret(
                    Byte128Vector.class,
                    byte.class, so.length(),
                    Byte512Vector.class,
                    byte.class, LENGTH,
                    so, this,
                    (s, v) -> (Byte512Vector) s.reshape(v)
                );
            } else if (o.bitSize() == 256 && (o instanceof Byte256Vector)) {
                Byte256Vector so = (Byte256Vector)o;
                return VectorIntrinsics.reinterpret(
                    Byte256Vector.class,
                    byte.class, so.length(),
                    Byte512Vector.class,
                    byte.class, LENGTH,
                    so, this,
                    (s, v) -> (Byte512Vector) s.reshape(v)
                );
            } else if (o.bitSize() == 512 && (o instanceof Byte512Vector)) {
                Byte512Vector so = (Byte512Vector)o;
                return VectorIntrinsics.reinterpret(
                    Byte512Vector.class,
                    byte.class, so.length(),
                    Byte512Vector.class,
                    byte.class, LENGTH,
                    so, this,
                    (s, v) -> (Byte512Vector) s.reshape(v)
                );
            } else if ((o.bitSize() > 0) && (o.bitSize() <= 2048)
                    && (o.bitSize() % 128 == 0) && (o instanceof ByteMaxVector)) {
                ByteMaxVector so = (ByteMaxVector)o;
                return VectorIntrinsics.reinterpret(
                    ByteMaxVector.class,
                    byte.class, so.length(),
                    Byte512Vector.class,
                    byte.class, LENGTH,
                    so, this,
                    (s, v) -> (Byte512Vector) s.reshape(v)
                );
            } else {
                throw new InternalError("Unimplemented size");
            }
        }
    }
}
