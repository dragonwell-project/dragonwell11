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
import java.nio.IntBuffer;
import java.nio.ReadOnlyBufferException;
import java.util.Arrays;
import java.util.Objects;
import java.util.function.IntUnaryOperator;

import jdk.internal.misc.Unsafe;
import jdk.internal.vm.annotation.ForceInline;
import static jdk.panama.vector.VectorIntrinsics.*;

@SuppressWarnings("cast")
final class IntMaxVector extends IntVector {
    static final IntMaxSpecies SPECIES = new IntMaxSpecies();

    static final IntMaxVector ZERO = new IntMaxVector();

    static final int LENGTH = SPECIES.length();

    // Index vector species
    private static final IntVector.IntSpecies INDEX_SPEC;
    static {
        int bitSize = Vector.bitSizeForVectorLength(int.class, LENGTH);
        Vector.Shape shape = Vector.shapeForVectorBitSize(bitSize);
        INDEX_SPEC = (IntVector.IntSpecies) Vector.species(int.class, shape);
    }

    private final int[] vec; // Don't access directly, use getElements() instead.

    private int[] getElements() {
        return VectorIntrinsics.maybeRebox(this).vec;
    }

    IntMaxVector() {
        vec = new int[SPECIES.length()];
    }

    IntMaxVector(int[] v) {
        vec = v;
    }

    @Override
    public int length() { return LENGTH; }

    // Unary operator

    @Override
    IntMaxVector uOp(FUnOp f) {
        int[] vec = getElements();
        int[] res = new int[length()];
        for (int i = 0; i < length(); i++) {
            res[i] = f.apply(i, vec[i]);
        }
        return new IntMaxVector(res);
    }

    @Override
    IntMaxVector uOp(Mask<Integer> o, FUnOp f) {
        int[] vec = getElements();
        int[] res = new int[length()];
        boolean[] mbits = ((IntMaxMask)o).getBits();
        for (int i = 0; i < length(); i++) {
            res[i] = mbits[i] ? f.apply(i, vec[i]) : vec[i];
        }
        return new IntMaxVector(res);
    }

    // Binary operator

    @Override
    IntMaxVector bOp(Vector<Integer> o, FBinOp f) {
        int[] res = new int[length()];
        int[] vec1 = this.getElements();
        int[] vec2 = ((IntMaxVector)o).getElements();
        for (int i = 0; i < length(); i++) {
            res[i] = f.apply(i, vec1[i], vec2[i]);
        }
        return new IntMaxVector(res);
    }

    @Override
    IntMaxVector bOp(Vector<Integer> o1, Mask<Integer> o2, FBinOp f) {
        int[] res = new int[length()];
        int[] vec1 = this.getElements();
        int[] vec2 = ((IntMaxVector)o1).getElements();
        boolean[] mbits = ((IntMaxMask)o2).getBits();
        for (int i = 0; i < length(); i++) {
            res[i] = mbits[i] ? f.apply(i, vec1[i], vec2[i]) : vec1[i];
        }
        return new IntMaxVector(res);
    }

    // Trinary operator

    @Override
    IntMaxVector tOp(Vector<Integer> o1, Vector<Integer> o2, FTriOp f) {
        int[] res = new int[length()];
        int[] vec1 = this.getElements();
        int[] vec2 = ((IntMaxVector)o1).getElements();
        int[] vec3 = ((IntMaxVector)o2).getElements();
        for (int i = 0; i < length(); i++) {
            res[i] = f.apply(i, vec1[i], vec2[i], vec3[i]);
        }
        return new IntMaxVector(res);
    }

    @Override
    IntMaxVector tOp(Vector<Integer> o1, Vector<Integer> o2, Mask<Integer> o3, FTriOp f) {
        int[] res = new int[length()];
        int[] vec1 = getElements();
        int[] vec2 = ((IntMaxVector)o1).getElements();
        int[] vec3 = ((IntMaxVector)o2).getElements();
        boolean[] mbits = ((IntMaxMask)o3).getBits();
        for (int i = 0; i < length(); i++) {
            res[i] = mbits[i] ? f.apply(i, vec1[i], vec2[i], vec3[i]) : vec1[i];
        }
        return new IntMaxVector(res);
    }

    @Override
    int rOp(int v, FBinOp f) {
        int[] vec = getElements();
        for (int i = 0; i < length(); i++) {
            v = f.apply(i, v, vec[i]);
        }
        return v;
    }

    // Binary operations with scalars

    @Override
    @ForceInline
    public IntVector add(int o) {
        return add(SPECIES.broadcast(o));
    }

    @Override
    @ForceInline
    public IntVector add(int o, Mask<Integer> m) {
        return add(SPECIES.broadcast(o), m);
    }

    @Override
    @ForceInline
    public IntVector sub(int o) {
        return sub(SPECIES.broadcast(o));
    }

    @Override
    @ForceInline
    public IntVector sub(int o, Mask<Integer> m) {
        return sub(SPECIES.broadcast(o), m);
    }

    @Override
    @ForceInline
    public IntVector mul(int o) {
        return mul(SPECIES.broadcast(o));
    }

    @Override
    @ForceInline
    public IntVector mul(int o, Mask<Integer> m) {
        return mul(SPECIES.broadcast(o), m);
    }

    @Override
    @ForceInline
    public IntVector min(int o) {
        return min(SPECIES.broadcast(o));
    }

    @Override
    @ForceInline
    public IntVector max(int o) {
        return max(SPECIES.broadcast(o));
    }

    @Override
    @ForceInline
    public Mask<Integer> equal(int o) {
        return equal(SPECIES.broadcast(o));
    }

    @Override
    @ForceInline
    public Mask<Integer> notEqual(int o) {
        return notEqual(SPECIES.broadcast(o));
    }

    @Override
    @ForceInline
    public Mask<Integer> lessThan(int o) {
        return lessThan(SPECIES.broadcast(o));
    }

    @Override
    @ForceInline
    public Mask<Integer> lessThanEq(int o) {
        return lessThanEq(SPECIES.broadcast(o));
    }

    @Override
    @ForceInline
    public Mask<Integer> greaterThan(int o) {
        return greaterThan(SPECIES.broadcast(o));
    }

    @Override
    @ForceInline
    public Mask<Integer> greaterThanEq(int o) {
        return greaterThanEq(SPECIES.broadcast(o));
    }

    @Override
    @ForceInline
    public IntVector blend(int o, Mask<Integer> m) {
        return blend(SPECIES.broadcast(o), m);
    }


    @Override
    @ForceInline
    public IntVector and(int o) {
        return and(SPECIES.broadcast(o));
    }

    @Override
    @ForceInline
    public IntVector and(int o, Mask<Integer> m) {
        return and(SPECIES.broadcast(o), m);
    }

    @Override
    @ForceInline
    public IntVector or(int o) {
        return or(SPECIES.broadcast(o));
    }

    @Override
    @ForceInline
    public IntVector or(int o, Mask<Integer> m) {
        return or(SPECIES.broadcast(o), m);
    }

    @Override
    @ForceInline
    public IntVector xor(int o) {
        return xor(SPECIES.broadcast(o));
    }

    @Override
    @ForceInline
    public IntVector xor(int o, Mask<Integer> m) {
        return xor(SPECIES.broadcast(o), m);
    }

    @Override
    @ForceInline
    public IntMaxVector neg() {
        return SPECIES.zero().sub(this);
    }

    // Unary operations

    @ForceInline
    @Override
    public IntMaxVector neg(Mask<Integer> m) {
        return blend(neg(), m);
    }

    @Override
    @ForceInline
    public IntMaxVector abs() {
        return VectorIntrinsics.unaryOp(
            VECTOR_OP_ABS, IntMaxVector.class, int.class, LENGTH,
            this,
            v1 -> v1.uOp((i, a) -> (int) Math.abs(a)));
    }

    @ForceInline
    @Override
    public IntMaxVector abs(Mask<Integer> m) {
        return blend(abs(), m);
    }


    @Override
    @ForceInline
    public IntMaxVector not() {
        return VectorIntrinsics.unaryOp(
            VECTOR_OP_NOT, IntMaxVector.class, int.class, LENGTH,
            this,
            v1 -> v1.uOp((i, a) -> (int) ~a));
    }

    @ForceInline
    @Override
    public IntMaxVector not(Mask<Integer> m) {
        return blend(not(), m);
    }
    // Binary operations

    @Override
    @ForceInline
    public IntMaxVector add(Vector<Integer> o) {
        Objects.requireNonNull(o);
        IntMaxVector v = (IntMaxVector)o;
        return VectorIntrinsics.binaryOp(
            VECTOR_OP_ADD, IntMaxVector.class, int.class, LENGTH,
            this, v,
            (v1, v2) -> v1.bOp(v2, (i, a, b) -> (int)(a + b)));
    }

    @Override
    @ForceInline
    public IntMaxVector add(Vector<Integer> v, Mask<Integer> m) {
        return blend(add(v), m);
    }

    @Override
    @ForceInline
    public IntMaxVector sub(Vector<Integer> o) {
        Objects.requireNonNull(o);
        IntMaxVector v = (IntMaxVector)o;
        return VectorIntrinsics.binaryOp(
            VECTOR_OP_SUB, IntMaxVector.class, int.class, LENGTH,
            this, v,
            (v1, v2) -> v1.bOp(v2, (i, a, b) -> (int)(a - b)));
    }

    @Override
    @ForceInline
    public IntMaxVector sub(Vector<Integer> v, Mask<Integer> m) {
        return blend(sub(v), m);
    }

    @Override
    @ForceInline
    public IntMaxVector mul(Vector<Integer> o) {
        Objects.requireNonNull(o);
        IntMaxVector v = (IntMaxVector)o;
        return VectorIntrinsics.binaryOp(
            VECTOR_OP_MUL, IntMaxVector.class, int.class, LENGTH,
            this, v,
            (v1, v2) -> v1.bOp(v2, (i, a, b) -> (int)(a * b)));
    }

    @Override
    @ForceInline
    public IntMaxVector mul(Vector<Integer> v, Mask<Integer> m) {
        return blend(mul(v), m);
    }

    @Override
    @ForceInline
    public IntMaxVector min(Vector<Integer> o) {
        Objects.requireNonNull(o);
        IntMaxVector v = (IntMaxVector)o;
        return (IntMaxVector) VectorIntrinsics.binaryOp(
            VECTOR_OP_MIN, IntMaxVector.class, int.class, LENGTH,
            this, v,
            (v1, v2) -> ((IntMaxVector)v1).bOp(v2, (i, a, b) -> (int) ((a < b) ? a : b)));
    }

    @Override
    @ForceInline
    public IntMaxVector min(Vector<Integer> v, Mask<Integer> m) {
        return blend(min(v), m);
    }

    @Override
    @ForceInline
    public IntMaxVector max(Vector<Integer> o) {
        Objects.requireNonNull(o);
        IntMaxVector v = (IntMaxVector)o;
        return VectorIntrinsics.binaryOp(
            VECTOR_OP_MAX, IntMaxVector.class, int.class, LENGTH,
            this, v,
            (v1, v2) -> v1.bOp(v2, (i, a, b) -> (int) ((a > b) ? a : b)));
        }

    @Override
    @ForceInline
    public IntMaxVector max(Vector<Integer> v, Mask<Integer> m) {
        return blend(max(v), m);
    }

    @Override
    @ForceInline
    public IntMaxVector and(Vector<Integer> o) {
        Objects.requireNonNull(o);
        IntMaxVector v = (IntMaxVector)o;
        return VectorIntrinsics.binaryOp(
            VECTOR_OP_AND, IntMaxVector.class, int.class, LENGTH,
            this, v,
            (v1, v2) -> v1.bOp(v2, (i, a, b) -> (int)(a & b)));
    }

    @Override
    @ForceInline
    public IntMaxVector or(Vector<Integer> o) {
        Objects.requireNonNull(o);
        IntMaxVector v = (IntMaxVector)o;
        return VectorIntrinsics.binaryOp(
            VECTOR_OP_OR, IntMaxVector.class, int.class, LENGTH,
            this, v,
            (v1, v2) -> v1.bOp(v2, (i, a, b) -> (int)(a | b)));
    }

    @Override
    @ForceInline
    public IntMaxVector xor(Vector<Integer> o) {
        Objects.requireNonNull(o);
        IntMaxVector v = (IntMaxVector)o;
        return VectorIntrinsics.binaryOp(
            VECTOR_OP_XOR, IntMaxVector.class, int.class, LENGTH,
            this, v,
            (v1, v2) -> v1.bOp(v2, (i, a, b) -> (int)(a ^ b)));
    }

    @Override
    @ForceInline
    public IntMaxVector and(Vector<Integer> v, Mask<Integer> m) {
        return blend(and(v), m);
    }

    @Override
    @ForceInline
    public IntMaxVector or(Vector<Integer> v, Mask<Integer> m) {
        return blend(or(v), m);
    }

    @Override
    @ForceInline
    public IntMaxVector xor(Vector<Integer> v, Mask<Integer> m) {
        return blend(xor(v), m);
    }

    @Override
    @ForceInline
    public IntMaxVector shiftL(int s) {
        return VectorIntrinsics.broadcastInt(
            VECTOR_OP_LSHIFT, IntMaxVector.class, int.class, LENGTH,
            this, s,
            (v, i) -> v.uOp((__, a) -> (int) (a << i)));
    }

    @Override
    @ForceInline
    public IntMaxVector shiftR(int s) {
        return VectorIntrinsics.broadcastInt(
            VECTOR_OP_URSHIFT, IntMaxVector.class, int.class, LENGTH,
            this, s,
            (v, i) -> v.uOp((__, a) -> (int) (a >>> i)));
    }

    @Override
    @ForceInline
    public IntMaxVector aShiftR(int s) {
        return VectorIntrinsics.broadcastInt(
            VECTOR_OP_RSHIFT, IntMaxVector.class, int.class, LENGTH,
            this, s,
            (v, i) -> v.uOp((__, a) -> (int) (a >> i)));
    }

    @Override
    @ForceInline
    public IntMaxVector shiftL(Vector<Integer> s) {
        IntMaxVector shiftv = (IntMaxVector)s;
        // As per shift specification for Java, mask the shift count.
        shiftv = shiftv.and(species().broadcast(0x1f));
        return VectorIntrinsics.binaryOp(
            VECTOR_OP_LSHIFT, IntMaxVector.class, int.class, LENGTH,
            this, shiftv,
            (v1, v2) -> v1.bOp(v2,(i,a, b) -> (int) (a << b)));
    }

    @Override
    @ForceInline
    public IntMaxVector shiftR(Vector<Integer> s) {
        IntMaxVector shiftv = (IntMaxVector)s;
        // As per shift specification for Java, mask the shift count.
        shiftv = shiftv.and(species().broadcast(0x1f));
        return VectorIntrinsics.binaryOp(
            VECTOR_OP_URSHIFT, IntMaxVector.class, int.class, LENGTH,
            this, shiftv,
            (v1, v2) -> v1.bOp(v2,(i,a, b) -> (int) (a >>> b)));
    }

    @Override
    @ForceInline
    public IntMaxVector aShiftR(Vector<Integer> s) {
        IntMaxVector shiftv = (IntMaxVector)s;
        // As per shift specification for Java, mask the shift count.
        shiftv = shiftv.and(species().broadcast(0x1f));
        return VectorIntrinsics.binaryOp(
            VECTOR_OP_RSHIFT, IntMaxVector.class, int.class, LENGTH,
            this, shiftv,
            (v1, v2) -> v1.bOp(v2,(i,a, b) -> (int) (a >> b)));
    }
    // Ternary operations


    // Type specific horizontal reductions

    @Override
    @ForceInline
    public int addAll() {
        return (int) VectorIntrinsics.reductionCoerced(
            VECTOR_OP_ADD, IntMaxVector.class, int.class, LENGTH,
            this,
            v -> (long) v.rOp((int) 0, (i, a, b) -> (int) (a + b)));
    }

    @Override
    @ForceInline
    public int andAll() {
        return (int) VectorIntrinsics.reductionCoerced(
            VECTOR_OP_AND, IntMaxVector.class, int.class, LENGTH,
            this,
            v -> (long) v.rOp((int) -1, (i, a, b) -> (int) (a & b)));
    }

    @Override
    @ForceInline
    public int andAll(Mask<Integer> m) {
        return blend(SPECIES.broadcast((int) -1), m).andAll();
    }

    @Override
    @ForceInline
    public int minAll() {
        return (int) VectorIntrinsics.reductionCoerced(
            VECTOR_OP_MIN, IntMaxVector.class, int.class, LENGTH,
            this,
            v -> (long) v.rOp(Integer.MAX_VALUE , (i, a, b) -> (int) ((a < b) ? a : b)));
    }

    @Override
    @ForceInline
    public int maxAll() {
        return (int) VectorIntrinsics.reductionCoerced(
            VECTOR_OP_MAX, IntMaxVector.class, int.class, LENGTH,
            this,
            v -> (long) v.rOp(Integer.MIN_VALUE , (i, a, b) -> (int) ((a > b) ? a : b)));
    }

    @Override
    @ForceInline
    public int mulAll() {
        return (int) VectorIntrinsics.reductionCoerced(
            VECTOR_OP_MUL, IntMaxVector.class, int.class, LENGTH,
            this,
            v -> (long) v.rOp((int) 1, (i, a, b) -> (int) (a * b)));
    }

    @Override
    @ForceInline
    public int subAll() {
        return (int) VectorIntrinsics.reductionCoerced(
            VECTOR_OP_SUB, IntMaxVector.class, int.class, LENGTH,
            this,
            v -> (long) v.rOp((int) 0, (i, a, b) -> (int) (a - b)));
    }

    @Override
    @ForceInline
    public int orAll() {
        return (int) VectorIntrinsics.reductionCoerced(
            VECTOR_OP_OR, IntMaxVector.class, int.class, LENGTH,
            this,
            v -> (long) v.rOp((int) 0, (i, a, b) -> (int) (a | b)));
    }

    @Override
    @ForceInline
    public int orAll(Mask<Integer> m) {
        return blend(SPECIES.broadcast((int) 0), m).orAll();
    }

    @Override
    @ForceInline
    public int xorAll() {
        return (int) VectorIntrinsics.reductionCoerced(
            VECTOR_OP_XOR, IntMaxVector.class, int.class, LENGTH,
            this,
            v -> (long) v.rOp((int) 0, (i, a, b) -> (int) (a ^ b)));
    }

    @Override
    @ForceInline
    public int xorAll(Mask<Integer> m) {
        return blend(SPECIES.broadcast((int) 0), m).xorAll();
    }


    @Override
    @ForceInline
    public int addAll(Mask<Integer> m) {
        return blend(SPECIES.broadcast((int) 0), m).addAll();
    }

    @Override
    @ForceInline
    public int subAll(Mask<Integer> m) {
        return blend(SPECIES.broadcast((int) 0), m).subAll();
    }

    @Override
    @ForceInline
    public int mulAll(Mask<Integer> m) {
        return blend(SPECIES.broadcast((int) 1), m).mulAll();
    }

    @Override
    @ForceInline
    public int minAll(Mask<Integer> m) {
        return blend(SPECIES.broadcast(Integer.MAX_VALUE), m).minAll();
    }

    @Override
    @ForceInline
    public int maxAll(Mask<Integer> m) {
        return blend(SPECIES.broadcast(Integer.MIN_VALUE), m).maxAll();
    }

    @Override
    @ForceInline
    public Shuffle<Integer> toShuffle() {
        int[] a = toArray();
        int[] sa = new int[a.length];
        for (int i = 0; i < a.length; i++) {
            sa[i] = (int) a[i];
        }
        return SPECIES.shuffleFromArray(sa, 0);
    }

    // Memory operations

    private static final int ARRAY_SHIFT = 31 - Integer.numberOfLeadingZeros(Unsafe.ARRAY_INT_INDEX_SCALE);

    @Override
    @ForceInline
    public void intoArray(int[] a, int ix) {
        Objects.requireNonNull(a);
        ix = VectorIntrinsics.checkIndex(ix, a.length, LENGTH);
        VectorIntrinsics.store(IntMaxVector.class, int.class, LENGTH,
                               a, (((long) ix) << ARRAY_SHIFT) + Unsafe.ARRAY_INT_BASE_OFFSET,
                               this,
                               a, ix,
                               (arr, idx, v) -> v.forEach((i, e) -> arr[idx + i] = e));
    }

    @Override
    @ForceInline
    public final void intoArray(int[] a, int ax, Mask<Integer> m) {
        IntMaxVector oldVal = SPECIES.fromArray(a, ax);
        IntMaxVector newVal = oldVal.blend(this, m);
        newVal.intoArray(a, ax);
    }
    @Override
    @ForceInline
    public void intoArray(int[] a, int ix, int[] b, int iy) {
        Objects.requireNonNull(a);
        Objects.requireNonNull(b);

        // Index vector: vix[0:n] = i -> ix + indexMap[iy + i]
        IntVector vix = INDEX_SPEC.fromArray(b, iy).add(ix);

        vix = VectorIntrinsics.checkIndex(vix, a.length);

        VectorIntrinsics.storeWithMap(IntMaxVector.class, int.class, LENGTH, vix.getClass(),
                               a, Unsafe.ARRAY_INT_BASE_OFFSET, vix,
                               this,
                               a, ix, b, iy,
                               (arr, idx, v, indexMap, idy) -> v.forEach((i, e) -> arr[idx+indexMap[idy+i]] = e));
    }

     @Override
     @ForceInline
     public final void intoArray(int[] a, int ax, Mask<Integer> m, int[] b, int iy) {
         // @@@ This can result in out of bounds errors for unset mask lanes
         IntMaxVector oldVal = SPECIES.fromArray(a, ax, b, iy);
         IntMaxVector newVal = oldVal.blend(this, m);
         newVal.intoArray(a, ax, b, iy);
     }

    @Override
    @ForceInline
    public void intoByteArray(byte[] a, int ix) {
        Objects.requireNonNull(a);
        ix = VectorIntrinsics.checkIndex(ix, a.length, bitSize() / Byte.SIZE);
        VectorIntrinsics.store(IntMaxVector.class, int.class, LENGTH,
                               a, ((long) ix) + Unsafe.ARRAY_BYTE_BASE_OFFSET,
                               this,
                               a, ix,
                               (c, idx, v) -> {
                                   ByteBuffer bbc = ByteBuffer.wrap(c, idx, c.length - idx).order(ByteOrder.nativeOrder());
                                   IntBuffer tb = bbc.asIntBuffer();
                                   v.forEach((i, e) -> tb.put(e));
                               });
    }

    @Override
    @ForceInline
    public final void intoByteArray(byte[] a, int ix, Mask<Integer> m) {
        IntMaxVector oldVal = SPECIES.fromByteArray(a, ix);
        IntMaxVector newVal = oldVal.blend(this, m);
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
        VectorIntrinsics.store(IntMaxVector.class, int.class, LENGTH,
                               U.getReference(bb, BYTE_BUFFER_HB), ix + U.getLong(bb, BUFFER_ADDRESS),
                               this,
                               bb, ix,
                               (c, idx, v) -> {
                                   ByteBuffer bbc = c.duplicate().position(idx).order(ByteOrder.nativeOrder());
                                   IntBuffer tb = bbc.asIntBuffer();
                                   v.forEach((i, e) -> tb.put(e));
                               });
    }

    @Override
    @ForceInline
    public void intoByteBuffer(ByteBuffer bb, int ix, Mask<Integer> m) {
        IntMaxVector oldVal = SPECIES.fromByteBuffer(bb, ix);
        IntMaxVector newVal = oldVal.blend(this, m);
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

        IntMaxVector that = (IntMaxVector) o;
        return this.equal(that).allTrue();
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(vec);
    }

    // Binary test

    @Override
    IntMaxMask bTest(Vector<Integer> o, FBinTest f) {
        int[] vec1 = getElements();
        int[] vec2 = ((IntMaxVector)o).getElements();
        boolean[] bits = new boolean[length()];
        for (int i = 0; i < length(); i++){
            bits[i] = f.apply(i, vec1[i], vec2[i]);
        }
        return new IntMaxMask(bits);
    }

    // Comparisons

    @Override
    @ForceInline
    public IntMaxMask equal(Vector<Integer> o) {
        Objects.requireNonNull(o);
        IntMaxVector v = (IntMaxVector)o;

        return VectorIntrinsics.compare(
            BT_eq, IntMaxVector.class, IntMaxMask.class, int.class, LENGTH,
            this, v,
            (v1, v2) -> v1.bTest(v2, (i, a, b) -> a == b));
    }

    @Override
    @ForceInline
    public IntMaxMask notEqual(Vector<Integer> o) {
        Objects.requireNonNull(o);
        IntMaxVector v = (IntMaxVector)o;

        return VectorIntrinsics.compare(
            BT_ne, IntMaxVector.class, IntMaxMask.class, int.class, LENGTH,
            this, v,
            (v1, v2) -> v1.bTest(v2, (i, a, b) -> a != b));
    }

    @Override
    @ForceInline
    public IntMaxMask lessThan(Vector<Integer> o) {
        Objects.requireNonNull(o);
        IntMaxVector v = (IntMaxVector)o;

        return VectorIntrinsics.compare(
            BT_lt, IntMaxVector.class, IntMaxMask.class, int.class, LENGTH,
            this, v,
            (v1, v2) -> v1.bTest(v2, (i, a, b) -> a < b));
    }

    @Override
    @ForceInline
    public IntMaxMask lessThanEq(Vector<Integer> o) {
        Objects.requireNonNull(o);
        IntMaxVector v = (IntMaxVector)o;

        return VectorIntrinsics.compare(
            BT_le, IntMaxVector.class, IntMaxMask.class, int.class, LENGTH,
            this, v,
            (v1, v2) -> v1.bTest(v2, (i, a, b) -> a <= b));
    }

    @Override
    @ForceInline
    public IntMaxMask greaterThan(Vector<Integer> o) {
        Objects.requireNonNull(o);
        IntMaxVector v = (IntMaxVector)o;

        return (IntMaxMask) VectorIntrinsics.compare(
            BT_gt, IntMaxVector.class, IntMaxMask.class, int.class, LENGTH,
            this, v,
            (v1, v2) -> v1.bTest(v2, (i, a, b) -> a > b));
    }

    @Override
    @ForceInline
    public IntMaxMask greaterThanEq(Vector<Integer> o) {
        Objects.requireNonNull(o);
        IntMaxVector v = (IntMaxVector)o;

        return VectorIntrinsics.compare(
            BT_ge, IntMaxVector.class, IntMaxMask.class, int.class, LENGTH,
            this, v,
            (v1, v2) -> v1.bTest(v2, (i, a, b) -> a >= b));
    }

    // Foreach

    @Override
    void forEach(FUnCon f) {
        int[] vec = getElements();
        for (int i = 0; i < length(); i++) {
            f.apply(i, vec[i]);
        }
    }

    @Override
    void forEach(Mask<Integer> o, FUnCon f) {
        boolean[] mbits = ((IntMaxMask)o).getBits();
        forEach((i, a) -> {
            if (mbits[i]) { f.apply(i, a); }
        });
    }


    FloatMaxVector toFP() {
        int[] vec = getElements();
        float[] res = new float[this.species().length()];
        for(int i = 0; i < this.species().length(); i++){
            res[i] = Float.intBitsToFloat(vec[i]);
        }
        return new FloatMaxVector(res);
    }

    @Override
    public IntMaxVector rotateEL(int j) {
        int[] vec = getElements();
        int[] res = new int[length()];
        for (int i = 0; i < length(); i++){
            res[(j + i) % length()] = vec[i];
        }
        return new IntMaxVector(res);
    }

    @Override
    public IntMaxVector rotateER(int j) {
        int[] vec = getElements();
        int[] res = new int[length()];
        for (int i = 0; i < length(); i++){
            int z = i - j;
            if(j < 0) {
                res[length() + z] = vec[i];
            } else {
                res[z] = vec[i];
            }
        }
        return new IntMaxVector(res);
    }

    @Override
    public IntMaxVector shiftEL(int j) {
        int[] vec = getElements();
        int[] res = new int[length()];
        for (int i = 0; i < length() - j; i++) {
            res[i] = vec[i + j];
        }
        return new IntMaxVector(res);
    }

    @Override
    public IntMaxVector shiftER(int j) {
        int[] vec = getElements();
        int[] res = new int[length()];
        for (int i = 0; i < length() - j; i++){
            res[i + j] = vec[i];
        }
        return new IntMaxVector(res);
    }

    @Override
    @ForceInline
    public IntMaxVector rearrange(Vector<Integer> v,
                                  Shuffle<Integer> s, Mask<Integer> m) {
        return this.rearrange(s).blend(v.rearrange(s), m);
    }

    @Override
    @ForceInline
    public IntMaxVector rearrange(Shuffle<Integer> o1) {
    Objects.requireNonNull(o1);
    IntMaxShuffle s =  (IntMaxShuffle)o1;

        return VectorIntrinsics.rearrangeOp(
            IntMaxVector.class, IntMaxShuffle.class, int.class, LENGTH,
            this, s,
            (v1, s_) -> v1.uOp((i, a) -> {
            int[] vec = this.getElements();
            int ei = s_.getElement(i);
            return vec[ei];
        }));
    }

    @Override
    @ForceInline
    public IntMaxVector blend(Vector<Integer> o1, Mask<Integer> o2) {
        Objects.requireNonNull(o1);
        Objects.requireNonNull(o2);
        IntMaxVector v = (IntMaxVector)o1;
        IntMaxMask   m = (IntMaxMask)o2;

        return VectorIntrinsics.blend(
            IntMaxVector.class, IntMaxMask.class, int.class, LENGTH,
            this, v, m,
            (v1, v2, m_) -> v1.bOp(v2, (i, a, b) -> m_.getElement(i) ? b : a));
    }

    // Accessors

    @Override
    public int get(int i) {
        if (i < 0 || i >= LENGTH) {
            throw new IllegalArgumentException("Index " + i + " must be zero or positive, and less than " + LENGTH);
        }
        return (int) VectorIntrinsics.extract(
                                IntMaxVector.class, int.class, LENGTH,
                                this, i,
                                (vec, ix) -> {
                                    int[] vecarr = vec.getElements();
                                    return (long)vecarr[ix];
                                });
    }

    @Override
    public IntMaxVector with(int i, int e) {
        if (i < 0 || i >= LENGTH) {
            throw new IllegalArgumentException("Index " + i + " must be zero or positive, and less than " + LENGTH);
        }
        return VectorIntrinsics.insert(
                                IntMaxVector.class, int.class, LENGTH,
                                this, i, (long)e,
                                (v, ix, bits) -> {
                                    int[] res = v.getElements().clone();
                                    res[ix] = (int)bits;
                                    return new IntMaxVector(res);
                                });
    }

    // Mask

    static final class IntMaxMask extends AbstractMask<Integer> {
        static final IntMaxMask TRUE_MASK = new IntMaxMask(true);
        static final IntMaxMask FALSE_MASK = new IntMaxMask(false);

        private final boolean[] bits; // Don't access directly, use getBits() instead.

        public IntMaxMask(boolean[] bits) {
            this(bits, 0);
        }

        public IntMaxMask(boolean[] bits, int offset) {
            boolean[] a = new boolean[species().length()];
            for (int i = 0; i < a.length; i++) {
                a[i] = bits[offset + i];
            }
            this.bits = a;
        }

        public IntMaxMask(boolean val) {
            boolean[] bits = new boolean[species().length()];
            Arrays.fill(bits, val);
            this.bits = bits;
        }

        boolean[] getBits() {
            return VectorIntrinsics.maybeRebox(this).bits;
        }

        @Override
        IntMaxMask uOp(MUnOp f) {
            boolean[] res = new boolean[species().length()];
            boolean[] bits = getBits();
            for (int i = 0; i < species().length(); i++) {
                res[i] = f.apply(i, bits[i]);
            }
            return new IntMaxMask(res);
        }

        @Override
        IntMaxMask bOp(Mask<Integer> o, MBinOp f) {
            boolean[] res = new boolean[species().length()];
            boolean[] bits = getBits();
            boolean[] mbits = ((IntMaxMask)o).getBits();
            for (int i = 0; i < species().length(); i++) {
                res[i] = f.apply(i, bits[i], mbits[i]);
            }
            return new IntMaxMask(res);
        }

        @Override
        public IntMaxSpecies species() {
            return SPECIES;
        }

        @Override
        public IntMaxVector toVector() {
            int[] res = new int[species().length()];
            boolean[] bits = getBits();
            for (int i = 0; i < species().length(); i++) {
                // -1 will result in the most significant bit being set in
                // addition to some or all other bits
                res[i] = (int) (bits[i] ? -1 : 0);
            }
            return new IntMaxVector(res);
        }

        // Unary operations

        @Override
        @ForceInline
        public IntMaxMask not() {
            return (IntMaxMask) VectorIntrinsics.unaryOp(
                                             VECTOR_OP_NOT, IntMaxMask.class, int.class, LENGTH,
                                             this,
                                             (m1) -> m1.uOp((i, a) -> !a));
        }

        // Binary operations

        @Override
        @ForceInline
        public IntMaxMask and(Mask<Integer> o) {
            Objects.requireNonNull(o);
            IntMaxMask m = (IntMaxMask)o;
            return VectorIntrinsics.binaryOp(VECTOR_OP_AND, IntMaxMask.class, int.class, LENGTH,
                                             this, m,
                                             (m1, m2) -> m1.bOp(m2, (i, a, b) -> a & b));
        }

        @Override
        @ForceInline
        public IntMaxMask or(Mask<Integer> o) {
            Objects.requireNonNull(o);
            IntMaxMask m = (IntMaxMask)o;
            return VectorIntrinsics.binaryOp(VECTOR_OP_OR, IntMaxMask.class, int.class, LENGTH,
                                             this, m,
                                             (m1, m2) -> m1.bOp(m2, (i, a, b) -> a | b));
        }

        // Reductions

        @Override
        @ForceInline
        public boolean anyTrue() {
            return VectorIntrinsics.test(COND_notZero, IntMaxMask.class, int.class, LENGTH,
                                         this, this,
                                         (m1, m2) -> super.anyTrue());
        }

        @Override
        @ForceInline
        public boolean allTrue() {
            return VectorIntrinsics.test(COND_carrySet, IntMaxMask.class, int.class, LENGTH,
                                         this, species().maskAllTrue(),
                                         (m1, m2) -> super.allTrue());
        }
    }

    // Shuffle

    static final class IntMaxShuffle extends AbstractShuffle<Integer> {
        IntMaxShuffle(byte[] reorder) {
            super(reorder);
        }

        public IntMaxShuffle(int[] reorder) {
            super(reorder);
        }

        public IntMaxShuffle(int[] reorder, int i) {
            super(reorder, i);
        }

        public IntMaxShuffle(IntUnaryOperator f) {
            super(f);
        }

        @Override
        public IntMaxSpecies species() {
            return SPECIES;
        }

        @Override
        public IntMaxVector toVector() {
            int[] va = new int[SPECIES.length()];
            for (int i = 0; i < va.length; i++) {
              va[i] = (int) getElement(i);
            }
            return species().fromArray(va, 0);
        }

        @Override
        public IntMaxShuffle rearrange(Vector.Shuffle<Integer> o) {
            IntMaxShuffle s = (IntMaxShuffle) o;
            byte[] r = new byte[reorder.length];
            for (int i = 0; i < reorder.length; i++) {
                r[i] = reorder[s.reorder[i]];
            }
            return new IntMaxShuffle(r);
        }
    }

    // Species

    @Override
    public IntMaxSpecies species() {
        return SPECIES;
    }

    static final class IntMaxSpecies extends IntSpecies {
        static final int BIT_SIZE = Shape.S_Max_BIT.bitSize();

        static final int LENGTH = BIT_SIZE / Integer.SIZE;

        @Override
        public String toString() {
           StringBuilder sb = new StringBuilder("Shape[");
           sb.append(bitSize()).append(" bits, ");
           sb.append(length()).append(" ").append(int.class.getSimpleName()).append("s x ");
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
        public Class<Integer> elementType() {
            return int.class;
        }

        @Override
        @ForceInline
        public int elementSize() {
            return Integer.SIZE;
        }

        @Override
        @ForceInline
        public Shape shape() {
            return Shape.S_Max_BIT;
        }

        @Override
        IntMaxVector op(FOp f) {
            int[] res = new int[length()];
            for (int i = 0; i < length(); i++) {
                res[i] = f.apply(i);
            }
            return new IntMaxVector(res);
        }

        @Override
        IntMaxVector op(Mask<Integer> o, FOp f) {
            int[] res = new int[length()];
            boolean[] mbits = ((IntMaxMask)o).getBits();
            for (int i = 0; i < length(); i++) {
                if (mbits[i]) {
                    res[i] = f.apply(i);
                }
            }
            return new IntMaxVector(res);
        }

        @Override
        IntMaxMask opm(FOpm f) {
            boolean[] res = new boolean[length()];
            for (int i = 0; i < length(); i++) {
                res[i] = (boolean)f.apply(i);
            }
            return new IntMaxMask(res);
        }

        // Factories

        @Override
        public IntMaxMask maskFromValues(boolean... bits) {
            return new IntMaxMask(bits);
        }

        @Override
        public IntMaxShuffle shuffle(IntUnaryOperator f) {
            return new IntMaxShuffle(f);
        }

        @Override
        public IntMaxShuffle shuffleIota() {
            return new IntMaxShuffle(AbstractShuffle.IDENTITY);
        }

        @Override
        public IntMaxShuffle shuffleFromValues(int... ixs) {
            return new IntMaxShuffle(ixs);
        }

        @Override
        public IntMaxShuffle shuffleFromArray(int[] ixs, int i) {
            return new IntMaxShuffle(ixs, i);
        }

        @Override
        @ForceInline
        public IntMaxVector zero() {
            return VectorIntrinsics.broadcastCoerced(IntMaxVector.class, int.class, LENGTH,
                                                     0,
                                                     (z -> ZERO));
        }

        @Override
        @ForceInline
        public IntMaxVector broadcast(int e) {
            return VectorIntrinsics.broadcastCoerced(
                IntMaxVector.class, int.class, LENGTH,
                e,
                ((long bits) -> SPECIES.op(i -> (int)bits)));
        }

        @Override
        @ForceInline
        public IntMaxMask maskAllTrue() {
            return VectorIntrinsics.broadcastCoerced(IntMaxMask.class, int.class, LENGTH,
                                                     (int)-1,
                                                     (z -> IntMaxMask.TRUE_MASK));
        }

        @Override
        @ForceInline
        public IntMaxMask maskAllFalse() {
            return VectorIntrinsics.broadcastCoerced(IntMaxMask.class, int.class, LENGTH,
                                                     0,
                                                     (z -> IntMaxMask.FALSE_MASK));
        }

        @Override
        @ForceInline
        public IntMaxVector scalars(int... es) {
            Objects.requireNonNull(es);
            int ix = VectorIntrinsics.checkIndex(0, es.length, LENGTH);
            return VectorIntrinsics.load(IntMaxVector.class, int.class, LENGTH,
                                         es, Unsafe.ARRAY_INT_BASE_OFFSET,
                                         es, ix,
                                         (c, idx) -> op(n -> c[idx + n]));
        }

        @Override
        @ForceInline
        public IntMaxMask maskFromArray(boolean[] bits, int ix) {
            Objects.requireNonNull(bits);
            ix = VectorIntrinsics.checkIndex(ix, bits.length, LENGTH);
            return VectorIntrinsics.load(IntMaxMask.class, int.class, LENGTH,
                                         bits, (long)ix + Unsafe.ARRAY_BOOLEAN_BASE_OFFSET,
                                         bits, ix,
                                         (c, idx) -> opm(n -> c[idx + n]));
        }

        @Override
        @ForceInline
        public IntMaxVector fromArray(int[] a, int ix) {
            Objects.requireNonNull(a);
            ix = VectorIntrinsics.checkIndex(ix, a.length, LENGTH);
            return VectorIntrinsics.load(IntMaxVector.class, int.class, LENGTH,
                                         a, (((long) ix) << ARRAY_SHIFT) + Unsafe.ARRAY_INT_BASE_OFFSET,
                                         a, ix,
                                         (c, idx) -> op(n -> c[idx + n]));
        }

        @Override
        @ForceInline
        public IntMaxVector fromArray(int[] a, int ax, Mask<Integer> m) {
            return zero().blend(fromArray(a, ax), m);
        }

        @Override
        @ForceInline
        public IntMaxVector fromByteArray(byte[] a, int ix) {
            Objects.requireNonNull(a);
            ix = VectorIntrinsics.checkIndex(ix, a.length, bitSize() / Byte.SIZE);
            return VectorIntrinsics.load(IntMaxVector.class, int.class, LENGTH,
                                         a, ((long) ix) + Unsafe.ARRAY_BYTE_BASE_OFFSET,
                                         a, ix,
                                         (c, idx) -> {
                                             ByteBuffer bbc = ByteBuffer.wrap(c, idx, a.length - idx).order(ByteOrder.nativeOrder());
                                             IntBuffer tb = bbc.asIntBuffer();
                                             return op(i -> tb.get());
                                         });
        }
        @Override
        @ForceInline
        public IntMaxVector fromArray(int[] a, int ix, int[] b, int iy) {
            Objects.requireNonNull(a);
            Objects.requireNonNull(b);

            // Index vector: vix[0:n] = i -> ix + indexMap[iy + i]
            IntVector vix = INDEX_SPEC.fromArray(b, iy).add(ix);

            vix = VectorIntrinsics.checkIndex(vix, a.length);

            return VectorIntrinsics.loadWithMap(IntMaxVector.class, int.class, LENGTH, vix.getClass(),
                                        a, Unsafe.ARRAY_INT_BASE_OFFSET, vix,
                                        a, ix, b, iy,
                                       (c, idx, indexMap, idy) -> op(n -> c[idx + indexMap[idy+n]]));
       }

       @Override
       @ForceInline
       public IntMaxVector fromArray(int[] a, int ax, Mask<Integer> m, int[] indexMap, int j) {
           // @@@ This can result in out of bounds errors for unset mask lanes
           return zero().blend(fromArray(a, ax, indexMap, j), m);
       }


        @Override
        @ForceInline
        public IntMaxVector fromByteArray(byte[] a, int ix, Mask<Integer> m) {
            return zero().blend(fromByteArray(a, ix), m);
        }

        @Override
        @ForceInline
        public IntMaxVector fromByteBuffer(ByteBuffer bb, int ix) {
            if (bb.order() != ByteOrder.nativeOrder()) {
                throw new IllegalArgumentException();
            }
            ix = VectorIntrinsics.checkIndex(ix, bb.limit(), bitSize() / Byte.SIZE);
            return VectorIntrinsics.load(IntMaxVector.class, int.class, LENGTH,
                                         U.getReference(bb, BYTE_BUFFER_HB), U.getLong(bb, BUFFER_ADDRESS) + ix,
                                         bb, ix,
                                         (c, idx) -> {
                                             ByteBuffer bbc = c.duplicate().position(idx).order(ByteOrder.nativeOrder());
                                             IntBuffer tb = bbc.asIntBuffer();
                                             return op(i -> tb.get());
                                         });
        }

        @Override
        @ForceInline
        public IntMaxVector fromByteBuffer(ByteBuffer bb, int ix, Mask<Integer> m) {
            return zero().blend(fromByteBuffer(bb, ix), m);
        }

        @Override
        @ForceInline
        @SuppressWarnings("unchecked")
        public <F> IntMaxVector cast(Vector<F> o) {
            if (o.length() != LENGTH)
                throw new IllegalArgumentException("Vector length this species length differ");

            return VectorIntrinsics.cast(
                o.getClass(),
                o.elementType(), LENGTH,
                IntMaxVector.class,
                int.class, LENGTH,
                o, this,
                (s, v) -> s.castDefault(v)
            );
        }

        @SuppressWarnings("unchecked")
        @ForceInline
        private <F> IntMaxVector castDefault(Vector<F> v) {
            // Allocate array of required size
            int limit = length();
            int[] a = new int[limit];

            Class<?> vtype = v.species().elementType();
            if (vtype == byte.class) {
                ByteVector tv = (ByteVector)v;
                for (int i = 0; i < limit; i++) {
                    a[i] = (int) tv.get(i);
                }
            } else if (vtype == short.class) {
                ShortVector tv = (ShortVector)v;
                for (int i = 0; i < limit; i++) {
                    a[i] = (int) tv.get(i);
                }
            } else if (vtype == int.class) {
                IntVector tv = (IntVector)v;
                for (int i = 0; i < limit; i++) {
                    a[i] = (int) tv.get(i);
                }
            } else if (vtype == long.class){
                LongVector tv = (LongVector)v;
                for (int i = 0; i < limit; i++) {
                    a[i] = (int) tv.get(i);
                }
            } else if (vtype == float.class){
                FloatVector tv = (FloatVector)v;
                for (int i = 0; i < limit; i++) {
                    a[i] = (int) tv.get(i);
                }
            } else if (vtype == double.class){
                DoubleVector tv = (DoubleVector)v;
                for (int i = 0; i < limit; i++) {
                    a[i] = (int) tv.get(i);
                }
            } else {
                throw new UnsupportedOperationException("Bad lane type for casting.");
            }

            return scalars(a);
        }

        @Override
        @ForceInline
        public <E> IntMaxMask cast(Mask<E> m) {
            if (m.length() != LENGTH)
                throw new IllegalArgumentException("Mask length this species length differ");
            return new IntMaxMask(m.toArray());
        }

        @Override
        @ForceInline
        public <E> IntMaxShuffle cast(Shuffle<E> s) {
            if (s.length() != LENGTH)
                throw new IllegalArgumentException("Shuffle length this species length differ");
            return new IntMaxShuffle(s.toArray());
        }

        @Override
        @ForceInline
        @SuppressWarnings("unchecked")
        public <F> IntMaxVector rebracket(Vector<F> o) {
            Objects.requireNonNull(o);
            if (o.elementType() == byte.class) {
                ByteMaxVector so = (ByteMaxVector)o;
                return VectorIntrinsics.reinterpret(
                    ByteMaxVector.class,
                    byte.class, so.length(),
                    IntMaxVector.class,
                    int.class, LENGTH,
                    so, this,
                    (s, v) -> (IntMaxVector) s.reshape(v)
                );
            } else if (o.elementType() == short.class) {
                ShortMaxVector so = (ShortMaxVector)o;
                return VectorIntrinsics.reinterpret(
                    ShortMaxVector.class,
                    short.class, so.length(),
                    IntMaxVector.class,
                    int.class, LENGTH,
                    so, this,
                    (s, v) -> (IntMaxVector) s.reshape(v)
                );
            } else if (o.elementType() == int.class) {
                IntMaxVector so = (IntMaxVector)o;
                return VectorIntrinsics.reinterpret(
                    IntMaxVector.class,
                    int.class, so.length(),
                    IntMaxVector.class,
                    int.class, LENGTH,
                    so, this,
                    (s, v) -> (IntMaxVector) s.reshape(v)
                );
            } else if (o.elementType() == long.class) {
                LongMaxVector so = (LongMaxVector)o;
                return VectorIntrinsics.reinterpret(
                    LongMaxVector.class,
                    long.class, so.length(),
                    IntMaxVector.class,
                    int.class, LENGTH,
                    so, this,
                    (s, v) -> (IntMaxVector) s.reshape(v)
                );
            } else if (o.elementType() == float.class) {
                FloatMaxVector so = (FloatMaxVector)o;
                return VectorIntrinsics.reinterpret(
                    FloatMaxVector.class,
                    float.class, so.length(),
                    IntMaxVector.class,
                    int.class, LENGTH,
                    so, this,
                    (s, v) -> (IntMaxVector) s.reshape(v)
                );
            } else if (o.elementType() == double.class) {
                DoubleMaxVector so = (DoubleMaxVector)o;
                return VectorIntrinsics.reinterpret(
                    DoubleMaxVector.class,
                    double.class, so.length(),
                    IntMaxVector.class,
                    int.class, LENGTH,
                    so, this,
                    (s, v) -> (IntMaxVector) s.reshape(v)
                );
            } else {
                throw new InternalError("Unimplemented type");
            }
        }

        @Override
        @ForceInline
        @SuppressWarnings("unchecked")
        public IntMaxVector resize(Vector<Integer> o) {
            Objects.requireNonNull(o);
            if (o.bitSize() == 64 && (o instanceof Int64Vector)) {
                Int64Vector so = (Int64Vector)o;
                return VectorIntrinsics.reinterpret(
                    Int64Vector.class,
                    int.class, so.length(),
                    IntMaxVector.class,
                    int.class, LENGTH,
                    so, this,
                    (s, v) -> (IntMaxVector) s.reshape(v)
                );
            } else if (o.bitSize() == 128 && (o instanceof Int128Vector)) {
                Int128Vector so = (Int128Vector)o;
                return VectorIntrinsics.reinterpret(
                    Int128Vector.class,
                    int.class, so.length(),
                    IntMaxVector.class,
                    int.class, LENGTH,
                    so, this,
                    (s, v) -> (IntMaxVector) s.reshape(v)
                );
            } else if (o.bitSize() == 256 && (o instanceof Int256Vector)) {
                Int256Vector so = (Int256Vector)o;
                return VectorIntrinsics.reinterpret(
                    Int256Vector.class,
                    int.class, so.length(),
                    IntMaxVector.class,
                    int.class, LENGTH,
                    so, this,
                    (s, v) -> (IntMaxVector) s.reshape(v)
                );
            } else if (o.bitSize() == 512 && (o instanceof Int512Vector)) {
                Int512Vector so = (Int512Vector)o;
                return VectorIntrinsics.reinterpret(
                    Int512Vector.class,
                    int.class, so.length(),
                    IntMaxVector.class,
                    int.class, LENGTH,
                    so, this,
                    (s, v) -> (IntMaxVector) s.reshape(v)
                );
            } else if ((o.bitSize() > 0) && (o.bitSize() <= 2048)
                    && (o.bitSize() % 128 == 0) && (o instanceof IntMaxVector)) {
                IntMaxVector so = (IntMaxVector)o;
                return VectorIntrinsics.reinterpret(
                    IntMaxVector.class,
                    int.class, so.length(),
                    IntMaxVector.class,
                    int.class, LENGTH,
                    so, this,
                    (s, v) -> (IntMaxVector) s.reshape(v)
                );
            } else {
                throw new InternalError("Unimplemented size");
            }
        }
    }
}
