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
final class ByteMaxVector extends ByteVector {
    static final ByteMaxSpecies SPECIES = new ByteMaxSpecies();

    static final ByteMaxVector ZERO = new ByteMaxVector();

    static final int LENGTH = SPECIES.length();

    private final byte[] vec; // Don't access directly, use getElements() instead.

    private byte[] getElements() {
        return VectorIntrinsics.maybeRebox(this).vec;
    }

    ByteMaxVector() {
        vec = new byte[SPECIES.length()];
    }

    ByteMaxVector(byte[] v) {
        vec = v;
    }

    @Override
    public int length() { return LENGTH; }

    // Unary operator

    @Override
    ByteMaxVector uOp(FUnOp f) {
        byte[] vec = getElements();
        byte[] res = new byte[length()];
        for (int i = 0; i < length(); i++) {
            res[i] = f.apply(i, vec[i]);
        }
        return new ByteMaxVector(res);
    }

    @Override
    ByteMaxVector uOp(Mask<Byte> o, FUnOp f) {
        byte[] vec = getElements();
        byte[] res = new byte[length()];
        boolean[] mbits = ((ByteMaxMask)o).getBits();
        for (int i = 0; i < length(); i++) {
            res[i] = mbits[i] ? f.apply(i, vec[i]) : vec[i];
        }
        return new ByteMaxVector(res);
    }

    // Binary operator

    @Override
    ByteMaxVector bOp(Vector<Byte> o, FBinOp f) {
        byte[] res = new byte[length()];
        byte[] vec1 = this.getElements();
        byte[] vec2 = ((ByteMaxVector)o).getElements();
        for (int i = 0; i < length(); i++) {
            res[i] = f.apply(i, vec1[i], vec2[i]);
        }
        return new ByteMaxVector(res);
    }

    @Override
    ByteMaxVector bOp(Vector<Byte> o1, Mask<Byte> o2, FBinOp f) {
        byte[] res = new byte[length()];
        byte[] vec1 = this.getElements();
        byte[] vec2 = ((ByteMaxVector)o1).getElements();
        boolean[] mbits = ((ByteMaxMask)o2).getBits();
        for (int i = 0; i < length(); i++) {
            res[i] = mbits[i] ? f.apply(i, vec1[i], vec2[i]) : vec1[i];
        }
        return new ByteMaxVector(res);
    }

    // Trinary operator

    @Override
    ByteMaxVector tOp(Vector<Byte> o1, Vector<Byte> o2, FTriOp f) {
        byte[] res = new byte[length()];
        byte[] vec1 = this.getElements();
        byte[] vec2 = ((ByteMaxVector)o1).getElements();
        byte[] vec3 = ((ByteMaxVector)o2).getElements();
        for (int i = 0; i < length(); i++) {
            res[i] = f.apply(i, vec1[i], vec2[i], vec3[i]);
        }
        return new ByteMaxVector(res);
    }

    @Override
    ByteMaxVector tOp(Vector<Byte> o1, Vector<Byte> o2, Mask<Byte> o3, FTriOp f) {
        byte[] res = new byte[length()];
        byte[] vec1 = getElements();
        byte[] vec2 = ((ByteMaxVector)o1).getElements();
        byte[] vec3 = ((ByteMaxVector)o2).getElements();
        boolean[] mbits = ((ByteMaxMask)o3).getBits();
        for (int i = 0; i < length(); i++) {
            res[i] = mbits[i] ? f.apply(i, vec1[i], vec2[i], vec3[i]) : vec1[i];
        }
        return new ByteMaxVector(res);
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
    public ByteMaxVector neg() {
        return SPECIES.zero().sub(this);
    }

    // Unary operations

    @ForceInline
    @Override
    public ByteMaxVector neg(Mask<Byte> m) {
        return blend(neg(), m);
    }

    @Override
    @ForceInline
    public ByteMaxVector abs() {
        return VectorIntrinsics.unaryOp(
            VECTOR_OP_ABS, ByteMaxVector.class, byte.class, LENGTH,
            this,
            v1 -> v1.uOp((i, a) -> (byte) Math.abs(a)));
    }

    @ForceInline
    @Override
    public ByteMaxVector abs(Mask<Byte> m) {
        return blend(abs(), m);
    }


    @Override
    @ForceInline
    public ByteMaxVector not() {
        return VectorIntrinsics.unaryOp(
            VECTOR_OP_NOT, ByteMaxVector.class, byte.class, LENGTH,
            this,
            v1 -> v1.uOp((i, a) -> (byte) ~a));
    }

    @ForceInline
    @Override
    public ByteMaxVector not(Mask<Byte> m) {
        return blend(not(), m);
    }
    // Binary operations

    @Override
    @ForceInline
    public ByteMaxVector add(Vector<Byte> o) {
        Objects.requireNonNull(o);
        ByteMaxVector v = (ByteMaxVector)o;
        return VectorIntrinsics.binaryOp(
            VECTOR_OP_ADD, ByteMaxVector.class, byte.class, LENGTH,
            this, v,
            (v1, v2) -> v1.bOp(v2, (i, a, b) -> (byte)(a + b)));
    }

    @Override
    @ForceInline
    public ByteMaxVector add(Vector<Byte> v, Mask<Byte> m) {
        return blend(add(v), m);
    }

    @Override
    @ForceInline
    public ByteMaxVector sub(Vector<Byte> o) {
        Objects.requireNonNull(o);
        ByteMaxVector v = (ByteMaxVector)o;
        return VectorIntrinsics.binaryOp(
            VECTOR_OP_SUB, ByteMaxVector.class, byte.class, LENGTH,
            this, v,
            (v1, v2) -> v1.bOp(v2, (i, a, b) -> (byte)(a - b)));
    }

    @Override
    @ForceInline
    public ByteMaxVector sub(Vector<Byte> v, Mask<Byte> m) {
        return blend(sub(v), m);
    }

    @Override
    @ForceInline
    public ByteMaxVector mul(Vector<Byte> o) {
        Objects.requireNonNull(o);
        ByteMaxVector v = (ByteMaxVector)o;
        return VectorIntrinsics.binaryOp(
            VECTOR_OP_MUL, ByteMaxVector.class, byte.class, LENGTH,
            this, v,
            (v1, v2) -> v1.bOp(v2, (i, a, b) -> (byte)(a * b)));
    }

    @Override
    @ForceInline
    public ByteMaxVector mul(Vector<Byte> v, Mask<Byte> m) {
        return blend(mul(v), m);
    }

    @Override
    @ForceInline
    public ByteMaxVector min(Vector<Byte> o) {
        Objects.requireNonNull(o);
        ByteMaxVector v = (ByteMaxVector)o;
        return (ByteMaxVector) VectorIntrinsics.binaryOp(
            VECTOR_OP_MIN, ByteMaxVector.class, byte.class, LENGTH,
            this, v,
            (v1, v2) -> ((ByteMaxVector)v1).bOp(v2, (i, a, b) -> (byte) ((a < b) ? a : b)));
    }

    @Override
    @ForceInline
    public ByteMaxVector min(Vector<Byte> v, Mask<Byte> m) {
        return blend(min(v), m);
    }

    @Override
    @ForceInline
    public ByteMaxVector max(Vector<Byte> o) {
        Objects.requireNonNull(o);
        ByteMaxVector v = (ByteMaxVector)o;
        return VectorIntrinsics.binaryOp(
            VECTOR_OP_MAX, ByteMaxVector.class, byte.class, LENGTH,
            this, v,
            (v1, v2) -> v1.bOp(v2, (i, a, b) -> (byte) ((a > b) ? a : b)));
        }

    @Override
    @ForceInline
    public ByteMaxVector max(Vector<Byte> v, Mask<Byte> m) {
        return blend(max(v), m);
    }

    @Override
    @ForceInline
    public ByteMaxVector and(Vector<Byte> o) {
        Objects.requireNonNull(o);
        ByteMaxVector v = (ByteMaxVector)o;
        return VectorIntrinsics.binaryOp(
            VECTOR_OP_AND, ByteMaxVector.class, byte.class, LENGTH,
            this, v,
            (v1, v2) -> v1.bOp(v2, (i, a, b) -> (byte)(a & b)));
    }

    @Override
    @ForceInline
    public ByteMaxVector or(Vector<Byte> o) {
        Objects.requireNonNull(o);
        ByteMaxVector v = (ByteMaxVector)o;
        return VectorIntrinsics.binaryOp(
            VECTOR_OP_OR, ByteMaxVector.class, byte.class, LENGTH,
            this, v,
            (v1, v2) -> v1.bOp(v2, (i, a, b) -> (byte)(a | b)));
    }

    @Override
    @ForceInline
    public ByteMaxVector xor(Vector<Byte> o) {
        Objects.requireNonNull(o);
        ByteMaxVector v = (ByteMaxVector)o;
        return VectorIntrinsics.binaryOp(
            VECTOR_OP_XOR, ByteMaxVector.class, byte.class, LENGTH,
            this, v,
            (v1, v2) -> v1.bOp(v2, (i, a, b) -> (byte)(a ^ b)));
    }

    @Override
    @ForceInline
    public ByteMaxVector and(Vector<Byte> v, Mask<Byte> m) {
        return blend(and(v), m);
    }

    @Override
    @ForceInline
    public ByteMaxVector or(Vector<Byte> v, Mask<Byte> m) {
        return blend(or(v), m);
    }

    @Override
    @ForceInline
    public ByteMaxVector xor(Vector<Byte> v, Mask<Byte> m) {
        return blend(xor(v), m);
    }

   public ByteMaxVector shiftL(int s) {
       byte[] vec = getElements();
       byte[] res = new byte[length()];
       for (int i = 0; i < length(); i++){
           res[i] = (byte)(vec[i] << s);
       }
       return new ByteMaxVector(res);
   }

   public ByteMaxVector shiftR(int s) {
       byte[] vec = getElements();
       byte[] res = new byte[length()];
       for (int i = 0; i < length(); i++){
           res[i] = (byte)(vec[i] >>> s);
       }
       return new ByteMaxVector(res);
   }

   public ByteMaxVector aShiftR(int s) {
       byte[] vec = getElements();
       byte[] res = new byte[length()];
       for (int i = 0; i < length(); i++){
           res[i] = (byte)(vec[i] >> s);
       }
       return new ByteMaxVector(res);
   }
    // Ternary operations


    // Type specific horizontal reductions

    @Override
    @ForceInline
    public byte addAll() {
        return (byte) VectorIntrinsics.reductionCoerced(
            VECTOR_OP_ADD, ByteMaxVector.class, byte.class, LENGTH,
            this,
            v -> (long) v.rOp((byte) 0, (i, a, b) -> (byte) (a + b)));
    }

    @Override
    @ForceInline
    public byte andAll() {
        return (byte) VectorIntrinsics.reductionCoerced(
            VECTOR_OP_AND, ByteMaxVector.class, byte.class, LENGTH,
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
            VECTOR_OP_MIN, ByteMaxVector.class, byte.class, LENGTH,
            this,
            v -> (long) v.rOp(Byte.MAX_VALUE , (i, a, b) -> (byte) ((a < b) ? a : b)));
    }

    @Override
    @ForceInline
    public byte maxAll() {
        return (byte) VectorIntrinsics.reductionCoerced(
            VECTOR_OP_MAX, ByteMaxVector.class, byte.class, LENGTH,
            this,
            v -> (long) v.rOp(Byte.MIN_VALUE , (i, a, b) -> (byte) ((a > b) ? a : b)));
    }

    @Override
    @ForceInline
    public byte mulAll() {
        return (byte) VectorIntrinsics.reductionCoerced(
            VECTOR_OP_MUL, ByteMaxVector.class, byte.class, LENGTH,
            this,
            v -> (long) v.rOp((byte) 1, (i, a, b) -> (byte) (a * b)));
    }

    @Override
    @ForceInline
    public byte subAll() {
        return (byte) VectorIntrinsics.reductionCoerced(
            VECTOR_OP_SUB, ByteMaxVector.class, byte.class, LENGTH,
            this,
            v -> (long) v.rOp((byte) 0, (i, a, b) -> (byte) (a - b)));
    }

    @Override
    @ForceInline
    public byte orAll() {
        return (byte) VectorIntrinsics.reductionCoerced(
            VECTOR_OP_OR, ByteMaxVector.class, byte.class, LENGTH,
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
            VECTOR_OP_XOR, ByteMaxVector.class, byte.class, LENGTH,
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
        VectorIntrinsics.store(ByteMaxVector.class, byte.class, LENGTH,
                               a, (((long) ix) << ARRAY_SHIFT) + Unsafe.ARRAY_BYTE_BASE_OFFSET,
                               this,
                               a, ix,
                               (arr, idx, v) -> v.forEach((i, e) -> arr[idx + i] = e));
    }

    @Override
    @ForceInline
    public final void intoArray(byte[] a, int ax, Mask<Byte> m) {
        ByteMaxVector oldVal = SPECIES.fromArray(a, ax);
        ByteMaxVector newVal = oldVal.blend(this, m);
        newVal.intoArray(a, ax);
    }

    @Override
    @ForceInline
    public void intoByteArray(byte[] a, int ix) {
        Objects.requireNonNull(a);
        ix = VectorIntrinsics.checkIndex(ix, a.length, bitSize() / Byte.SIZE);
        VectorIntrinsics.store(ByteMaxVector.class, byte.class, LENGTH,
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
        ByteMaxVector oldVal = SPECIES.fromByteArray(a, ix);
        ByteMaxVector newVal = oldVal.blend(this, m);
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
        VectorIntrinsics.store(ByteMaxVector.class, byte.class, LENGTH,
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
        ByteMaxVector oldVal = SPECIES.fromByteBuffer(bb, ix);
        ByteMaxVector newVal = oldVal.blend(this, m);
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

        ByteMaxVector that = (ByteMaxVector) o;
        return this.equal(that).allTrue();
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(vec);
    }

    // Binary test

    @Override
    ByteMaxMask bTest(Vector<Byte> o, FBinTest f) {
        byte[] vec1 = getElements();
        byte[] vec2 = ((ByteMaxVector)o).getElements();
        boolean[] bits = new boolean[length()];
        for (int i = 0; i < length(); i++){
            bits[i] = f.apply(i, vec1[i], vec2[i]);
        }
        return new ByteMaxMask(bits);
    }

    // Comparisons

    @Override
    @ForceInline
    public ByteMaxMask equal(Vector<Byte> o) {
        Objects.requireNonNull(o);
        ByteMaxVector v = (ByteMaxVector)o;

        return VectorIntrinsics.compare(
            BT_eq, ByteMaxVector.class, ByteMaxMask.class, byte.class, LENGTH,
            this, v,
            (v1, v2) -> v1.bTest(v2, (i, a, b) -> a == b));
    }

    @Override
    @ForceInline
    public ByteMaxMask notEqual(Vector<Byte> o) {
        Objects.requireNonNull(o);
        ByteMaxVector v = (ByteMaxVector)o;

        return VectorIntrinsics.compare(
            BT_ne, ByteMaxVector.class, ByteMaxMask.class, byte.class, LENGTH,
            this, v,
            (v1, v2) -> v1.bTest(v2, (i, a, b) -> a != b));
    }

    @Override
    @ForceInline
    public ByteMaxMask lessThan(Vector<Byte> o) {
        Objects.requireNonNull(o);
        ByteMaxVector v = (ByteMaxVector)o;

        return VectorIntrinsics.compare(
            BT_lt, ByteMaxVector.class, ByteMaxMask.class, byte.class, LENGTH,
            this, v,
            (v1, v2) -> v1.bTest(v2, (i, a, b) -> a < b));
    }

    @Override
    @ForceInline
    public ByteMaxMask lessThanEq(Vector<Byte> o) {
        Objects.requireNonNull(o);
        ByteMaxVector v = (ByteMaxVector)o;

        return VectorIntrinsics.compare(
            BT_le, ByteMaxVector.class, ByteMaxMask.class, byte.class, LENGTH,
            this, v,
            (v1, v2) -> v1.bTest(v2, (i, a, b) -> a <= b));
    }

    @Override
    @ForceInline
    public ByteMaxMask greaterThan(Vector<Byte> o) {
        Objects.requireNonNull(o);
        ByteMaxVector v = (ByteMaxVector)o;

        return (ByteMaxMask) VectorIntrinsics.compare(
            BT_gt, ByteMaxVector.class, ByteMaxMask.class, byte.class, LENGTH,
            this, v,
            (v1, v2) -> v1.bTest(v2, (i, a, b) -> a > b));
    }

    @Override
    @ForceInline
    public ByteMaxMask greaterThanEq(Vector<Byte> o) {
        Objects.requireNonNull(o);
        ByteMaxVector v = (ByteMaxVector)o;

        return VectorIntrinsics.compare(
            BT_ge, ByteMaxVector.class, ByteMaxMask.class, byte.class, LENGTH,
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
        boolean[] mbits = ((ByteMaxMask)o).getBits();
        forEach((i, a) -> {
            if (mbits[i]) { f.apply(i, a); }
        });
    }



    @Override
    public ByteMaxVector rotateEL(int j) {
        byte[] vec = getElements();
        byte[] res = new byte[length()];
        for (int i = 0; i < length(); i++){
            res[(j + i) % length()] = vec[i];
        }
        return new ByteMaxVector(res);
    }

    @Override
    public ByteMaxVector rotateER(int j) {
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
        return new ByteMaxVector(res);
    }

    @Override
    public ByteMaxVector shiftEL(int j) {
        byte[] vec = getElements();
        byte[] res = new byte[length()];
        for (int i = 0; i < length() - j; i++) {
            res[i] = vec[i + j];
        }
        return new ByteMaxVector(res);
    }

    @Override
    public ByteMaxVector shiftER(int j) {
        byte[] vec = getElements();
        byte[] res = new byte[length()];
        for (int i = 0; i < length() - j; i++){
            res[i + j] = vec[i];
        }
        return new ByteMaxVector(res);
    }

    @Override
    @ForceInline
    public ByteMaxVector rearrange(Vector<Byte> v,
                                  Shuffle<Byte> s, Mask<Byte> m) {
        return this.rearrange(s).blend(v.rearrange(s), m);
    }

    @Override
    @ForceInline
    public ByteMaxVector rearrange(Shuffle<Byte> o1) {
    Objects.requireNonNull(o1);
    ByteMaxShuffle s =  (ByteMaxShuffle)o1;

        return VectorIntrinsics.rearrangeOp(
            ByteMaxVector.class, ByteMaxShuffle.class, byte.class, LENGTH,
            this, s,
            (v1, s_) -> v1.uOp((i, a) -> {
            byte[] vec = this.getElements();
            int ei = s_.getElement(i);
            return vec[ei];
        }));
    }

    @Override
    @ForceInline
    public ByteMaxVector blend(Vector<Byte> o1, Mask<Byte> o2) {
        Objects.requireNonNull(o1);
        Objects.requireNonNull(o2);
        ByteMaxVector v = (ByteMaxVector)o1;
        ByteMaxMask   m = (ByteMaxMask)o2;

        return VectorIntrinsics.blend(
            ByteMaxVector.class, ByteMaxMask.class, byte.class, LENGTH,
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
                                ByteMaxVector.class, byte.class, LENGTH,
                                this, i,
                                (vec, ix) -> {
                                    byte[] vecarr = vec.getElements();
                                    return (long)vecarr[ix];
                                });
    }

    @Override
    public ByteMaxVector with(int i, byte e) {
        if (i < 0 || i >= LENGTH) {
            throw new IllegalArgumentException("Index " + i + " must be zero or positive, and less than " + LENGTH);
        }
        return VectorIntrinsics.insert(
                                ByteMaxVector.class, byte.class, LENGTH,
                                this, i, (long)e,
                                (v, ix, bits) -> {
                                    byte[] res = v.getElements().clone();
                                    res[ix] = (byte)bits;
                                    return new ByteMaxVector(res);
                                });
    }

    // Mask

    static final class ByteMaxMask extends AbstractMask<Byte> {
        static final ByteMaxMask TRUE_MASK = new ByteMaxMask(true);
        static final ByteMaxMask FALSE_MASK = new ByteMaxMask(false);

        private final boolean[] bits; // Don't access directly, use getBits() instead.

        public ByteMaxMask(boolean[] bits) {
            this(bits, 0);
        }

        public ByteMaxMask(boolean[] bits, int offset) {
            boolean[] a = new boolean[species().length()];
            for (int i = 0; i < a.length; i++) {
                a[i] = bits[offset + i];
            }
            this.bits = a;
        }

        public ByteMaxMask(boolean val) {
            boolean[] bits = new boolean[species().length()];
            Arrays.fill(bits, val);
            this.bits = bits;
        }

        boolean[] getBits() {
            return VectorIntrinsics.maybeRebox(this).bits;
        }

        @Override
        ByteMaxMask uOp(MUnOp f) {
            boolean[] res = new boolean[species().length()];
            boolean[] bits = getBits();
            for (int i = 0; i < species().length(); i++) {
                res[i] = f.apply(i, bits[i]);
            }
            return new ByteMaxMask(res);
        }

        @Override
        ByteMaxMask bOp(Mask<Byte> o, MBinOp f) {
            boolean[] res = new boolean[species().length()];
            boolean[] bits = getBits();
            boolean[] mbits = ((ByteMaxMask)o).getBits();
            for (int i = 0; i < species().length(); i++) {
                res[i] = f.apply(i, bits[i], mbits[i]);
            }
            return new ByteMaxMask(res);
        }

        @Override
        public ByteMaxSpecies species() {
            return SPECIES;
        }

        @Override
        public ByteMaxVector toVector() {
            byte[] res = new byte[species().length()];
            boolean[] bits = getBits();
            for (int i = 0; i < species().length(); i++) {
                // -1 will result in the most significant bit being set in
                // addition to some or all other bits
                res[i] = (byte) (bits[i] ? -1 : 0);
            }
            return new ByteMaxVector(res);
        }

        // Unary operations

        @Override
        @ForceInline
        public ByteMaxMask not() {
            return (ByteMaxMask) VectorIntrinsics.unaryOp(
                                             VECTOR_OP_NOT, ByteMaxMask.class, byte.class, LENGTH,
                                             this,
                                             (m1) -> m1.uOp((i, a) -> !a));
        }

        // Binary operations

        @Override
        @ForceInline
        public ByteMaxMask and(Mask<Byte> o) {
            Objects.requireNonNull(o);
            ByteMaxMask m = (ByteMaxMask)o;
            return VectorIntrinsics.binaryOp(VECTOR_OP_AND, ByteMaxMask.class, byte.class, LENGTH,
                                             this, m,
                                             (m1, m2) -> m1.bOp(m2, (i, a, b) -> a & b));
        }

        @Override
        @ForceInline
        public ByteMaxMask or(Mask<Byte> o) {
            Objects.requireNonNull(o);
            ByteMaxMask m = (ByteMaxMask)o;
            return VectorIntrinsics.binaryOp(VECTOR_OP_OR, ByteMaxMask.class, byte.class, LENGTH,
                                             this, m,
                                             (m1, m2) -> m1.bOp(m2, (i, a, b) -> a | b));
        }

        // Reductions

        @Override
        @ForceInline
        public boolean anyTrue() {
            return VectorIntrinsics.test(COND_notZero, ByteMaxMask.class, byte.class, LENGTH,
                                         this, this,
                                         (m1, m2) -> super.anyTrue());
        }

        @Override
        @ForceInline
        public boolean allTrue() {
            return VectorIntrinsics.test(COND_carrySet, ByteMaxMask.class, byte.class, LENGTH,
                                         this, species().maskAllTrue(),
                                         (m1, m2) -> super.allTrue());
        }
    }

    // Shuffle

    static final class ByteMaxShuffle extends AbstractShuffle<Byte> {
        ByteMaxShuffle(byte[] reorder) {
            super(reorder);
        }

        public ByteMaxShuffle(int[] reorder) {
            super(reorder);
        }

        public ByteMaxShuffle(int[] reorder, int i) {
            super(reorder, i);
        }

        public ByteMaxShuffle(IntUnaryOperator f) {
            super(f);
        }

        @Override
        public ByteMaxSpecies species() {
            return SPECIES;
        }

        @Override
        public ByteMaxVector toVector() {
            byte[] va = new byte[SPECIES.length()];
            for (int i = 0; i < va.length; i++) {
              va[i] = (byte) getElement(i);
            }
            return species().fromArray(va, 0);
        }

        @Override
        public ByteMaxShuffle rearrange(Vector.Shuffle<Byte> o) {
            ByteMaxShuffle s = (ByteMaxShuffle) o;
            byte[] r = new byte[reorder.length];
            for (int i = 0; i < reorder.length; i++) {
                r[i] = reorder[s.reorder[i]];
            }
            return new ByteMaxShuffle(r);
        }
    }

    // Species

    @Override
    public ByteMaxSpecies species() {
        return SPECIES;
    }

    static final class ByteMaxSpecies extends ByteSpecies {
        static final int BIT_SIZE = Shape.S_Max_BIT.bitSize();

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
            return Shape.S_Max_BIT;
        }

        @Override
        ByteMaxVector op(FOp f) {
            byte[] res = new byte[length()];
            for (int i = 0; i < length(); i++) {
                res[i] = f.apply(i);
            }
            return new ByteMaxVector(res);
        }

        @Override
        ByteMaxVector op(Mask<Byte> o, FOp f) {
            byte[] res = new byte[length()];
            boolean[] mbits = ((ByteMaxMask)o).getBits();
            for (int i = 0; i < length(); i++) {
                if (mbits[i]) {
                    res[i] = f.apply(i);
                }
            }
            return new ByteMaxVector(res);
        }

        @Override
        ByteMaxMask opm(FOpm f) {
            boolean[] res = new boolean[length()];
            for (int i = 0; i < length(); i++) {
                res[i] = (boolean)f.apply(i);
            }
            return new ByteMaxMask(res);
        }

        // Factories

        @Override
        public ByteMaxMask maskFromValues(boolean... bits) {
            return new ByteMaxMask(bits);
        }

        @Override
        public ByteMaxShuffle shuffle(IntUnaryOperator f) {
            return new ByteMaxShuffle(f);
        }

        @Override
        public ByteMaxShuffle shuffleIota() {
            return new ByteMaxShuffle(AbstractShuffle.IDENTITY);
        }

        @Override
        public ByteMaxShuffle shuffleFromValues(int... ixs) {
            return new ByteMaxShuffle(ixs);
        }

        @Override
        public ByteMaxShuffle shuffleFromArray(int[] ixs, int i) {
            return new ByteMaxShuffle(ixs, i);
        }

        @Override
        @ForceInline
        public ByteMaxVector zero() {
            return VectorIntrinsics.broadcastCoerced(ByteMaxVector.class, byte.class, LENGTH,
                                                     0,
                                                     (z -> ZERO));
        }

        @Override
        @ForceInline
        public ByteMaxVector broadcast(byte e) {
            return VectorIntrinsics.broadcastCoerced(
                ByteMaxVector.class, byte.class, LENGTH,
                e,
                ((long bits) -> SPECIES.op(i -> (byte)bits)));
        }

        @Override
        @ForceInline
        public ByteMaxMask maskAllTrue() {
            return VectorIntrinsics.broadcastCoerced(ByteMaxMask.class, byte.class, LENGTH,
                                                     (byte)-1,
                                                     (z -> ByteMaxMask.TRUE_MASK));
        }

        @Override
        @ForceInline
        public ByteMaxMask maskAllFalse() {
            return VectorIntrinsics.broadcastCoerced(ByteMaxMask.class, byte.class, LENGTH,
                                                     0,
                                                     (z -> ByteMaxMask.FALSE_MASK));
        }

        @Override
        @ForceInline
        public ByteMaxVector scalars(byte... es) {
            Objects.requireNonNull(es);
            int ix = VectorIntrinsics.checkIndex(0, es.length, LENGTH);
            return VectorIntrinsics.load(ByteMaxVector.class, byte.class, LENGTH,
                                         es, Unsafe.ARRAY_BYTE_BASE_OFFSET,
                                         es, ix,
                                         (c, idx) -> op(n -> c[idx + n]));
        }

        @Override
        @ForceInline
        public ByteMaxMask maskFromArray(boolean[] bits, int ix) {
            Objects.requireNonNull(bits);
            ix = VectorIntrinsics.checkIndex(ix, bits.length, LENGTH);
            return VectorIntrinsics.load(ByteMaxMask.class, byte.class, LENGTH,
                                         bits, (long)ix + Unsafe.ARRAY_BOOLEAN_BASE_OFFSET,
                                         bits, ix,
                                         (c, idx) -> opm(n -> c[idx + n]));
        }

        @Override
        @ForceInline
        public ByteMaxVector fromArray(byte[] a, int ix) {
            Objects.requireNonNull(a);
            ix = VectorIntrinsics.checkIndex(ix, a.length, LENGTH);
            return VectorIntrinsics.load(ByteMaxVector.class, byte.class, LENGTH,
                                         a, (((long) ix) << ARRAY_SHIFT) + Unsafe.ARRAY_BYTE_BASE_OFFSET,
                                         a, ix,
                                         (c, idx) -> op(n -> c[idx + n]));
        }

        @Override
        @ForceInline
        public ByteMaxVector fromArray(byte[] a, int ax, Mask<Byte> m) {
            return zero().blend(fromArray(a, ax), m);
        }

        @Override
        @ForceInline
        public ByteMaxVector fromByteArray(byte[] a, int ix) {
            Objects.requireNonNull(a);
            ix = VectorIntrinsics.checkIndex(ix, a.length, bitSize() / Byte.SIZE);
            return VectorIntrinsics.load(ByteMaxVector.class, byte.class, LENGTH,
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
        public ByteMaxVector fromByteArray(byte[] a, int ix, Mask<Byte> m) {
            return zero().blend(fromByteArray(a, ix), m);
        }

        @Override
        @ForceInline
        public ByteMaxVector fromByteBuffer(ByteBuffer bb, int ix) {
            if (bb.order() != ByteOrder.nativeOrder()) {
                throw new IllegalArgumentException();
            }
            ix = VectorIntrinsics.checkIndex(ix, bb.limit(), bitSize() / Byte.SIZE);
            return VectorIntrinsics.load(ByteMaxVector.class, byte.class, LENGTH,
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
        public ByteMaxVector fromByteBuffer(ByteBuffer bb, int ix, Mask<Byte> m) {
            return zero().blend(fromByteBuffer(bb, ix), m);
        }

        @Override
        @ForceInline
        @SuppressWarnings("unchecked")
        public <F> ByteMaxVector cast(Vector<F> o) {
            if (o.length() != LENGTH)
                throw new IllegalArgumentException("Vector length this species length differ");

            return VectorIntrinsics.cast(
                o.getClass(),
                o.elementType(), LENGTH,
                ByteMaxVector.class,
                byte.class, LENGTH,
                o, this,
                (s, v) -> s.castDefault(v)
            );
        }

        @SuppressWarnings("unchecked")
        @ForceInline
        private <F> ByteMaxVector castDefault(Vector<F> v) {
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
        public <E> ByteMaxMask cast(Mask<E> m) {
            if (m.length() != LENGTH)
                throw new IllegalArgumentException("Mask length this species length differ");
            return new ByteMaxMask(m.toArray());
        }

        @Override
        @ForceInline
        public <E> ByteMaxShuffle cast(Shuffle<E> s) {
            if (s.length() != LENGTH)
                throw new IllegalArgumentException("Shuffle length this species length differ");
            return new ByteMaxShuffle(s.toArray());
        }

        @Override
        @ForceInline
        @SuppressWarnings("unchecked")
        public <F> ByteMaxVector rebracket(Vector<F> o) {
            Objects.requireNonNull(o);
            if (o.elementType() == byte.class) {
                ByteMaxVector so = (ByteMaxVector)o;
                return VectorIntrinsics.reinterpret(
                    ByteMaxVector.class,
                    byte.class, so.length(),
                    ByteMaxVector.class,
                    byte.class, LENGTH,
                    so, this,
                    (s, v) -> (ByteMaxVector) s.reshape(v)
                );
            } else if (o.elementType() == short.class) {
                ShortMaxVector so = (ShortMaxVector)o;
                return VectorIntrinsics.reinterpret(
                    ShortMaxVector.class,
                    short.class, so.length(),
                    ByteMaxVector.class,
                    byte.class, LENGTH,
                    so, this,
                    (s, v) -> (ByteMaxVector) s.reshape(v)
                );
            } else if (o.elementType() == int.class) {
                IntMaxVector so = (IntMaxVector)o;
                return VectorIntrinsics.reinterpret(
                    IntMaxVector.class,
                    int.class, so.length(),
                    ByteMaxVector.class,
                    byte.class, LENGTH,
                    so, this,
                    (s, v) -> (ByteMaxVector) s.reshape(v)
                );
            } else if (o.elementType() == long.class) {
                LongMaxVector so = (LongMaxVector)o;
                return VectorIntrinsics.reinterpret(
                    LongMaxVector.class,
                    long.class, so.length(),
                    ByteMaxVector.class,
                    byte.class, LENGTH,
                    so, this,
                    (s, v) -> (ByteMaxVector) s.reshape(v)
                );
            } else if (o.elementType() == float.class) {
                FloatMaxVector so = (FloatMaxVector)o;
                return VectorIntrinsics.reinterpret(
                    FloatMaxVector.class,
                    float.class, so.length(),
                    ByteMaxVector.class,
                    byte.class, LENGTH,
                    so, this,
                    (s, v) -> (ByteMaxVector) s.reshape(v)
                );
            } else if (o.elementType() == double.class) {
                DoubleMaxVector so = (DoubleMaxVector)o;
                return VectorIntrinsics.reinterpret(
                    DoubleMaxVector.class,
                    double.class, so.length(),
                    ByteMaxVector.class,
                    byte.class, LENGTH,
                    so, this,
                    (s, v) -> (ByteMaxVector) s.reshape(v)
                );
            } else {
                throw new InternalError("Unimplemented type");
            }
        }

        @Override
        @ForceInline
        @SuppressWarnings("unchecked")
        public ByteMaxVector resize(Vector<Byte> o) {
            Objects.requireNonNull(o);
            if (o.bitSize() == 64 && (o instanceof Byte64Vector)) {
                Byte64Vector so = (Byte64Vector)o;
                return VectorIntrinsics.reinterpret(
                    Byte64Vector.class,
                    byte.class, so.length(),
                    ByteMaxVector.class,
                    byte.class, LENGTH,
                    so, this,
                    (s, v) -> (ByteMaxVector) s.reshape(v)
                );
            } else if (o.bitSize() == 128 && (o instanceof Byte128Vector)) {
                Byte128Vector so = (Byte128Vector)o;
                return VectorIntrinsics.reinterpret(
                    Byte128Vector.class,
                    byte.class, so.length(),
                    ByteMaxVector.class,
                    byte.class, LENGTH,
                    so, this,
                    (s, v) -> (ByteMaxVector) s.reshape(v)
                );
            } else if (o.bitSize() == 256 && (o instanceof Byte256Vector)) {
                Byte256Vector so = (Byte256Vector)o;
                return VectorIntrinsics.reinterpret(
                    Byte256Vector.class,
                    byte.class, so.length(),
                    ByteMaxVector.class,
                    byte.class, LENGTH,
                    so, this,
                    (s, v) -> (ByteMaxVector) s.reshape(v)
                );
            } else if (o.bitSize() == 512 && (o instanceof Byte512Vector)) {
                Byte512Vector so = (Byte512Vector)o;
                return VectorIntrinsics.reinterpret(
                    Byte512Vector.class,
                    byte.class, so.length(),
                    ByteMaxVector.class,
                    byte.class, LENGTH,
                    so, this,
                    (s, v) -> (ByteMaxVector) s.reshape(v)
                );
            } else if ((o.bitSize() > 0) && (o.bitSize() <= 2048)
                    && (o.bitSize() % 128 == 0) && (o instanceof ByteMaxVector)) {
                ByteMaxVector so = (ByteMaxVector)o;
                return VectorIntrinsics.reinterpret(
                    ByteMaxVector.class,
                    byte.class, so.length(),
                    ByteMaxVector.class,
                    byte.class, LENGTH,
                    so, this,
                    (s, v) -> (ByteMaxVector) s.reshape(v)
                );
            } else {
                throw new InternalError("Unimplemented size");
            }
        }
    }
}
