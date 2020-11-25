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
import java.nio.FloatBuffer;
import java.nio.ReadOnlyBufferException;
import java.util.Arrays;
import java.util.Objects;
import java.util.function.IntUnaryOperator;

import jdk.internal.misc.Unsafe;
import jdk.internal.vm.annotation.ForceInline;
import static jdk.panama.vector.VectorIntrinsics.*;

@SuppressWarnings("cast")
final class Float256Vector extends FloatVector {
    static final Float256Species SPECIES = new Float256Species();

    static final Float256Vector ZERO = new Float256Vector();

    static final int LENGTH = SPECIES.length();

    // Index vector species
    private static final IntVector.IntSpecies INDEX_SPEC;
    static {
        int bitSize = Vector.bitSizeForVectorLength(int.class, LENGTH);
        Vector.Shape shape = Vector.shapeForVectorBitSize(bitSize);
        INDEX_SPEC = (IntVector.IntSpecies) Vector.species(int.class, shape);
    }

    private final float[] vec; // Don't access directly, use getElements() instead.

    private float[] getElements() {
        return VectorIntrinsics.maybeRebox(this).vec;
    }

    Float256Vector() {
        vec = new float[SPECIES.length()];
    }

    Float256Vector(float[] v) {
        vec = v;
    }

    @Override
    public int length() { return LENGTH; }

    // Unary operator

    @Override
    Float256Vector uOp(FUnOp f) {
        float[] vec = getElements();
        float[] res = new float[length()];
        for (int i = 0; i < length(); i++) {
            res[i] = f.apply(i, vec[i]);
        }
        return new Float256Vector(res);
    }

    @Override
    Float256Vector uOp(Mask<Float> o, FUnOp f) {
        float[] vec = getElements();
        float[] res = new float[length()];
        boolean[] mbits = ((Float256Mask)o).getBits();
        for (int i = 0; i < length(); i++) {
            res[i] = mbits[i] ? f.apply(i, vec[i]) : vec[i];
        }
        return new Float256Vector(res);
    }

    // Binary operator

    @Override
    Float256Vector bOp(Vector<Float> o, FBinOp f) {
        float[] res = new float[length()];
        float[] vec1 = this.getElements();
        float[] vec2 = ((Float256Vector)o).getElements();
        for (int i = 0; i < length(); i++) {
            res[i] = f.apply(i, vec1[i], vec2[i]);
        }
        return new Float256Vector(res);
    }

    @Override
    Float256Vector bOp(Vector<Float> o1, Mask<Float> o2, FBinOp f) {
        float[] res = new float[length()];
        float[] vec1 = this.getElements();
        float[] vec2 = ((Float256Vector)o1).getElements();
        boolean[] mbits = ((Float256Mask)o2).getBits();
        for (int i = 0; i < length(); i++) {
            res[i] = mbits[i] ? f.apply(i, vec1[i], vec2[i]) : vec1[i];
        }
        return new Float256Vector(res);
    }

    // Trinary operator

    @Override
    Float256Vector tOp(Vector<Float> o1, Vector<Float> o2, FTriOp f) {
        float[] res = new float[length()];
        float[] vec1 = this.getElements();
        float[] vec2 = ((Float256Vector)o1).getElements();
        float[] vec3 = ((Float256Vector)o2).getElements();
        for (int i = 0; i < length(); i++) {
            res[i] = f.apply(i, vec1[i], vec2[i], vec3[i]);
        }
        return new Float256Vector(res);
    }

    @Override
    Float256Vector tOp(Vector<Float> o1, Vector<Float> o2, Mask<Float> o3, FTriOp f) {
        float[] res = new float[length()];
        float[] vec1 = getElements();
        float[] vec2 = ((Float256Vector)o1).getElements();
        float[] vec3 = ((Float256Vector)o2).getElements();
        boolean[] mbits = ((Float256Mask)o3).getBits();
        for (int i = 0; i < length(); i++) {
            res[i] = mbits[i] ? f.apply(i, vec1[i], vec2[i], vec3[i]) : vec1[i];
        }
        return new Float256Vector(res);
    }

    @Override
    float rOp(float v, FBinOp f) {
        float[] vec = getElements();
        for (int i = 0; i < length(); i++) {
            v = f.apply(i, v, vec[i]);
        }
        return v;
    }

    // Binary operations with scalars

    @Override
    @ForceInline
    public FloatVector add(float o) {
        return add(SPECIES.broadcast(o));
    }

    @Override
    @ForceInline
    public FloatVector add(float o, Mask<Float> m) {
        return add(SPECIES.broadcast(o), m);
    }

    @Override
    @ForceInline
    public FloatVector sub(float o) {
        return sub(SPECIES.broadcast(o));
    }

    @Override
    @ForceInline
    public FloatVector sub(float o, Mask<Float> m) {
        return sub(SPECIES.broadcast(o), m);
    }

    @Override
    @ForceInline
    public FloatVector mul(float o) {
        return mul(SPECIES.broadcast(o));
    }

    @Override
    @ForceInline
    public FloatVector mul(float o, Mask<Float> m) {
        return mul(SPECIES.broadcast(o), m);
    }

    @Override
    @ForceInline
    public FloatVector min(float o) {
        return min(SPECIES.broadcast(o));
    }

    @Override
    @ForceInline
    public FloatVector max(float o) {
        return max(SPECIES.broadcast(o));
    }

    @Override
    @ForceInline
    public Mask<Float> equal(float o) {
        return equal(SPECIES.broadcast(o));
    }

    @Override
    @ForceInline
    public Mask<Float> notEqual(float o) {
        return notEqual(SPECIES.broadcast(o));
    }

    @Override
    @ForceInline
    public Mask<Float> lessThan(float o) {
        return lessThan(SPECIES.broadcast(o));
    }

    @Override
    @ForceInline
    public Mask<Float> lessThanEq(float o) {
        return lessThanEq(SPECIES.broadcast(o));
    }

    @Override
    @ForceInline
    public Mask<Float> greaterThan(float o) {
        return greaterThan(SPECIES.broadcast(o));
    }

    @Override
    @ForceInline
    public Mask<Float> greaterThanEq(float o) {
        return greaterThanEq(SPECIES.broadcast(o));
    }

    @Override
    @ForceInline
    public FloatVector blend(float o, Mask<Float> m) {
        return blend(SPECIES.broadcast(o), m);
    }

    @Override
    @ForceInline
    public FloatVector div(float o) {
        return div(SPECIES.broadcast(o));
    }

    @Override
    @ForceInline
    public FloatVector div(float o, Mask<Float> m) {
        return div(SPECIES.broadcast(o), m);
    }

    @Override
    @ForceInline
    public Float256Vector div(Vector<Float> v, Mask<Float> m) {
        return blend(div(v), m);
    }

    @Override
    @ForceInline
    public FloatVector atan2(float o) {
        return atan2(SPECIES.broadcast(o));
    }

    @Override
    @ForceInline
    public FloatVector atan2(float o, Mask<Float> m) {
        return atan2(SPECIES.broadcast(o), m);
    }

    @Override
    @ForceInline
    public FloatVector pow(float o) {
        return pow(SPECIES.broadcast(o));
    }

    @Override
    @ForceInline
    public FloatVector pow(float o, Mask<Float> m) {
        return pow(SPECIES.broadcast(o), m);
    }

    @Override
    @ForceInline
    public FloatVector fma(float o1, float o2) {
        return fma(SPECIES.broadcast(o1), SPECIES.broadcast(o2));
    }

    @Override
    @ForceInline
    public FloatVector fma(float o1, float o2, Mask<Float> m) {
        return fma(SPECIES.broadcast(o1), SPECIES.broadcast(o2), m);
    }

    @Override
    @ForceInline
    public FloatVector hypot(float o) {
        return hypot(SPECIES.broadcast(o));
    }

    @Override
    @ForceInline
    public FloatVector hypot(float o, Mask<Float> m) {
        return hypot(SPECIES.broadcast(o), m);
    }


    // Unary operations

    @ForceInline
    @Override
    public Float256Vector neg(Mask<Float> m) {
        return blend(neg(), m);
    }

    @Override
    @ForceInline
    public Float256Vector abs() {
        return VectorIntrinsics.unaryOp(
            VECTOR_OP_ABS, Float256Vector.class, float.class, LENGTH,
            this,
            v1 -> v1.uOp((i, a) -> (float) Math.abs(a)));
    }

    @ForceInline
    @Override
    public Float256Vector abs(Mask<Float> m) {
        return blend(abs(), m);
    }

    @Override
    @ForceInline
    public Float256Vector neg() {
        return VectorIntrinsics.unaryOp(
            VECTOR_OP_NEG, Float256Vector.class, float.class, LENGTH,
            this,
            v1 -> v1.uOp((i, a) -> (float) -a));
    }

    @Override
    @ForceInline
    public Float256Vector div(Vector<Float> o) {
        Objects.requireNonNull(o);
        Float256Vector v = (Float256Vector)o;
        return VectorIntrinsics.binaryOp(
            VECTOR_OP_DIV, Float256Vector.class, float.class, LENGTH,
            this, v,
            (v1, v2) -> v1.bOp(v2, (i, a, b) -> (float)(a / b)));
    }

    @Override
    @ForceInline
    public Float256Vector sqrt() {
        return VectorIntrinsics.unaryOp(
            VECTOR_OP_SQRT, Float256Vector.class, float.class, LENGTH,
            this,
            v1 -> v1.uOp((i, a) -> (float) Math.sqrt((double) a)));
    }

    @Override
    @ForceInline
    public Float256Vector exp() {
        return (Float256Vector) VectorIntrinsics.unaryOp(
            VECTOR_OP_EXP, Float256Vector.class, float.class, LENGTH,
            this,
            v1 -> ((Float256Vector)v1).uOp((i, a) -> (float) Math.exp((double) a)));
    }

    @Override
    @ForceInline
    public Float256Vector log1p() {
        return (Float256Vector) VectorIntrinsics.unaryOp(
            VECTOR_OP_LOG1P, Float256Vector.class, float.class, LENGTH,
            this,
            v1 -> ((Float256Vector)v1).uOp((i, a) -> (float) Math.log1p((double) a)));
    }

    @Override
    @ForceInline
    public Float256Vector log() {
        return (Float256Vector) VectorIntrinsics.unaryOp(
            VECTOR_OP_LOG, Float256Vector.class, float.class, LENGTH,
            this,
            v1 -> ((Float256Vector)v1).uOp((i, a) -> (float) Math.log((double) a)));
    }

    @Override
    @ForceInline
    public Float256Vector log10() {
        return (Float256Vector) VectorIntrinsics.unaryOp(
            VECTOR_OP_LOG10, Float256Vector.class, float.class, LENGTH,
            this,
            v1 -> ((Float256Vector)v1).uOp((i, a) -> (float) Math.log10((double) a)));
    }

    @Override
    @ForceInline
    public Float256Vector expm1() {
        return (Float256Vector) VectorIntrinsics.unaryOp(
            VECTOR_OP_EXPM1, Float256Vector.class, float.class, LENGTH,
            this,
            v1 -> ((Float256Vector)v1).uOp((i, a) -> (float) Math.expm1((double) a)));
    }

    @Override
    @ForceInline
    public Float256Vector cbrt() {
        return (Float256Vector) VectorIntrinsics.unaryOp(
            VECTOR_OP_CBRT, Float256Vector.class, float.class, LENGTH,
            this,
            v1 -> ((Float256Vector)v1).uOp((i, a) -> (float) Math.cbrt((double) a)));
    }

    @Override
    @ForceInline
    public Float256Vector sin() {
        return (Float256Vector) VectorIntrinsics.unaryOp(
            VECTOR_OP_SIN, Float256Vector.class, float.class, LENGTH,
            this,
            v1 -> ((Float256Vector)v1).uOp((i, a) -> (float) Math.sin((double) a)));
    }

    @Override
    @ForceInline
    public Float256Vector cos() {
        return (Float256Vector) VectorIntrinsics.unaryOp(
            VECTOR_OP_COS, Float256Vector.class, float.class, LENGTH,
            this,
            v1 -> ((Float256Vector)v1).uOp((i, a) -> (float) Math.cos((double) a)));
    }

    @Override
    @ForceInline
    public Float256Vector tan() {
        return (Float256Vector) VectorIntrinsics.unaryOp(
            VECTOR_OP_TAN, Float256Vector.class, float.class, LENGTH,
            this,
            v1 -> ((Float256Vector)v1).uOp((i, a) -> (float) Math.tan((double) a)));
    }

    @Override
    @ForceInline
    public Float256Vector asin() {
        return (Float256Vector) VectorIntrinsics.unaryOp(
            VECTOR_OP_ASIN, Float256Vector.class, float.class, LENGTH,
            this,
            v1 -> ((Float256Vector)v1).uOp((i, a) -> (float) Math.asin((double) a)));
    }

    @Override
    @ForceInline
    public Float256Vector acos() {
        return (Float256Vector) VectorIntrinsics.unaryOp(
            VECTOR_OP_ACOS, Float256Vector.class, float.class, LENGTH,
            this,
            v1 -> ((Float256Vector)v1).uOp((i, a) -> (float) Math.acos((double) a)));
    }

    @Override
    @ForceInline
    public Float256Vector atan() {
        return (Float256Vector) VectorIntrinsics.unaryOp(
            VECTOR_OP_ATAN, Float256Vector.class, float.class, LENGTH,
            this,
            v1 -> ((Float256Vector)v1).uOp((i, a) -> (float) Math.atan((double) a)));
    }

    @Override
    @ForceInline
    public Float256Vector sinh() {
        return (Float256Vector) VectorIntrinsics.unaryOp(
            VECTOR_OP_SINH, Float256Vector.class, float.class, LENGTH,
            this,
            v1 -> ((Float256Vector)v1).uOp((i, a) -> (float) Math.sinh((double) a)));
    }

    @Override
    @ForceInline
    public Float256Vector cosh() {
        return (Float256Vector) VectorIntrinsics.unaryOp(
            VECTOR_OP_COSH, Float256Vector.class, float.class, LENGTH,
            this,
            v1 -> ((Float256Vector)v1).uOp((i, a) -> (float) Math.cosh((double) a)));
    }

    @Override
    @ForceInline
    public Float256Vector tanh() {
        return (Float256Vector) VectorIntrinsics.unaryOp(
            VECTOR_OP_TANH, Float256Vector.class, float.class, LENGTH,
            this,
            v1 -> ((Float256Vector)v1).uOp((i, a) -> (float) Math.tanh((double) a)));
    }

    @Override
    @ForceInline
    public Float256Vector pow(Vector<Float> o) {
        Objects.requireNonNull(o);
        Float256Vector v = (Float256Vector)o;
        return (Float256Vector) VectorIntrinsics.binaryOp(
            VECTOR_OP_POW, Float256Vector.class, float.class, LENGTH,
            this, v,
            (v1, v2) -> ((Float256Vector)v1).bOp(v2, (i, a, b) -> (float)(Math.pow(a,b))));
    }

    @Override
    @ForceInline
    public Float256Vector hypot(Vector<Float> o) {
        Objects.requireNonNull(o);
        Float256Vector v = (Float256Vector)o;
        return (Float256Vector) VectorIntrinsics.binaryOp(
            VECTOR_OP_HYPOT, Float256Vector.class, float.class, LENGTH,
            this, v,
            (v1, v2) -> ((Float256Vector)v1).bOp(v2, (i, a, b) -> (float)(Math.hypot(a,b))));
    }

    @Override
    @ForceInline
    public Float256Vector atan2(Vector<Float> o) {
        Objects.requireNonNull(o);
        Float256Vector v = (Float256Vector)o;
        return (Float256Vector) VectorIntrinsics.binaryOp(
            VECTOR_OP_ATAN2, Float256Vector.class, float.class, LENGTH,
            this, v,
            (v1, v2) -> ((Float256Vector)v1).bOp(v2, (i, a, b) -> (float)(Math.atan2(a,b))));
    }


    // Binary operations

    @Override
    @ForceInline
    public Float256Vector add(Vector<Float> o) {
        Objects.requireNonNull(o);
        Float256Vector v = (Float256Vector)o;
        return VectorIntrinsics.binaryOp(
            VECTOR_OP_ADD, Float256Vector.class, float.class, LENGTH,
            this, v,
            (v1, v2) -> v1.bOp(v2, (i, a, b) -> (float)(a + b)));
    }

    @Override
    @ForceInline
    public Float256Vector add(Vector<Float> v, Mask<Float> m) {
        return blend(add(v), m);
    }

    @Override
    @ForceInline
    public Float256Vector sub(Vector<Float> o) {
        Objects.requireNonNull(o);
        Float256Vector v = (Float256Vector)o;
        return VectorIntrinsics.binaryOp(
            VECTOR_OP_SUB, Float256Vector.class, float.class, LENGTH,
            this, v,
            (v1, v2) -> v1.bOp(v2, (i, a, b) -> (float)(a - b)));
    }

    @Override
    @ForceInline
    public Float256Vector sub(Vector<Float> v, Mask<Float> m) {
        return blend(sub(v), m);
    }

    @Override
    @ForceInline
    public Float256Vector mul(Vector<Float> o) {
        Objects.requireNonNull(o);
        Float256Vector v = (Float256Vector)o;
        return VectorIntrinsics.binaryOp(
            VECTOR_OP_MUL, Float256Vector.class, float.class, LENGTH,
            this, v,
            (v1, v2) -> v1.bOp(v2, (i, a, b) -> (float)(a * b)));
    }

    @Override
    @ForceInline
    public Float256Vector mul(Vector<Float> v, Mask<Float> m) {
        return blend(mul(v), m);
    }

    @Override
    @ForceInline
    public Float256Vector min(Vector<Float> o) {
        Objects.requireNonNull(o);
        Float256Vector v = (Float256Vector)o;
        return (Float256Vector) VectorIntrinsics.binaryOp(
            VECTOR_OP_MIN, Float256Vector.class, float.class, LENGTH,
            this, v,
            (v1, v2) -> ((Float256Vector)v1).bOp(v2, (i, a, b) -> (float) ((a < b) ? a : b)));
    }

    @Override
    @ForceInline
    public Float256Vector min(Vector<Float> v, Mask<Float> m) {
        return blend(min(v), m);
    }

    @Override
    @ForceInline
    public Float256Vector max(Vector<Float> o) {
        Objects.requireNonNull(o);
        Float256Vector v = (Float256Vector)o;
        return VectorIntrinsics.binaryOp(
            VECTOR_OP_MAX, Float256Vector.class, float.class, LENGTH,
            this, v,
            (v1, v2) -> v1.bOp(v2, (i, a, b) -> (float) ((a > b) ? a : b)));
        }

    @Override
    @ForceInline
    public Float256Vector max(Vector<Float> v, Mask<Float> m) {
        return blend(max(v), m);
    }


    // Ternary operations

    @Override
    @ForceInline
    public Float256Vector fma(Vector<Float> o1, Vector<Float> o2) {
        Objects.requireNonNull(o1);
        Objects.requireNonNull(o2);
        Float256Vector v1 = (Float256Vector)o1;
        Float256Vector v2 = (Float256Vector)o2;
        return VectorIntrinsics.ternaryOp(
            VECTOR_OP_FMA, Float256Vector.class, float.class, LENGTH,
            this, v1, v2,
            (w1, w2, w3) -> w1.tOp(w2, w3, (i, a, b, c) -> Math.fma(a, b, c)));
    }

    // Type specific horizontal reductions

    @Override
    @ForceInline
    public float addAll() {
        int bits = (int) VectorIntrinsics.reductionCoerced(
                                VECTOR_OP_ADD, Float256Vector.class, float.class, LENGTH,
                                this,
                                v -> {
                                    float r = v.rOp((float) 0, (i, a, b) -> (float) (a + b));
                                    return (long)Float.floatToIntBits(r);
                                });
        return Float.intBitsToFloat(bits);
    }

    @Override
    @ForceInline
    public float subAll() {
        int bits = (int) VectorIntrinsics.reductionCoerced(
                                VECTOR_OP_SUB, Float256Vector.class, float.class, LENGTH,
                                this,
                                v -> {
                                    float r = v.rOp((float) 0, (i, a, b) -> (float) (a - b));
                                    return (long)Float.floatToIntBits(r);
                                });
        return Float.intBitsToFloat(bits);
    }

    @Override
    @ForceInline
    public float mulAll() {
        int bits = (int) VectorIntrinsics.reductionCoerced(
                                VECTOR_OP_MUL, Float256Vector.class, float.class, LENGTH,
                                this,
                                v -> {
                                    float r = v.rOp((float) 1, (i, a, b) -> (float) (a * b));
                                    return (long)Float.floatToIntBits(r);
                                });
        return Float.intBitsToFloat(bits);
    }

    @Override
    @ForceInline
    public float minAll() {
        int bits = (int) VectorIntrinsics.reductionCoerced(
                                VECTOR_OP_MIN, Float256Vector.class, float.class, LENGTH,
                                this,
                                v -> {
                                    float r = v.rOp(Float.MAX_VALUE , (i, a, b) -> (float) ((a < b) ? a : b));
                                    return (long)Float.floatToIntBits(r);
                                });
        return Float.intBitsToFloat(bits);
    }

    @Override
    @ForceInline
    public float maxAll() {
        int bits = (int) VectorIntrinsics.reductionCoerced(
                                VECTOR_OP_MAX, Float256Vector.class, float.class, LENGTH,
                                this,
                                v -> {
                                    float r = v.rOp(Float.MIN_VALUE , (i, a, b) -> (float) ((a > b) ? a : b));
                                    return (long)Float.floatToIntBits(r);
                                });
        return Float.intBitsToFloat(bits);
    }


    @Override
    @ForceInline
    public float addAll(Mask<Float> m) {
        return blend(SPECIES.broadcast((float) 0), m).addAll();
    }

    @Override
    @ForceInline
    public float subAll(Mask<Float> m) {
        return blend(SPECIES.broadcast((float) 0), m).subAll();
    }

    @Override
    @ForceInline
    public float mulAll(Mask<Float> m) {
        return blend(SPECIES.broadcast((float) 1), m).mulAll();
    }

    @Override
    @ForceInline
    public float minAll(Mask<Float> m) {
        return blend(SPECIES.broadcast(Float.MAX_VALUE), m).minAll();
    }

    @Override
    @ForceInline
    public float maxAll(Mask<Float> m) {
        return blend(SPECIES.broadcast(Float.MIN_VALUE), m).maxAll();
    }

    @Override
    @ForceInline
    public Shuffle<Float> toShuffle() {
        float[] a = toArray();
        int[] sa = new int[a.length];
        for (int i = 0; i < a.length; i++) {
            sa[i] = (int) a[i];
        }
        return SPECIES.shuffleFromArray(sa, 0);
    }

    // Memory operations

    private static final int ARRAY_SHIFT = 31 - Integer.numberOfLeadingZeros(Unsafe.ARRAY_FLOAT_INDEX_SCALE);

    @Override
    @ForceInline
    public void intoArray(float[] a, int ix) {
        Objects.requireNonNull(a);
        ix = VectorIntrinsics.checkIndex(ix, a.length, LENGTH);
        VectorIntrinsics.store(Float256Vector.class, float.class, LENGTH,
                               a, (((long) ix) << ARRAY_SHIFT) + Unsafe.ARRAY_FLOAT_BASE_OFFSET,
                               this,
                               a, ix,
                               (arr, idx, v) -> v.forEach((i, e) -> arr[idx + i] = e));
    }

    @Override
    @ForceInline
    public final void intoArray(float[] a, int ax, Mask<Float> m) {
        Float256Vector oldVal = SPECIES.fromArray(a, ax);
        Float256Vector newVal = oldVal.blend(this, m);
        newVal.intoArray(a, ax);
    }
    @Override
    @ForceInline
    public void intoArray(float[] a, int ix, int[] b, int iy) {
        Objects.requireNonNull(a);
        Objects.requireNonNull(b);

        // Index vector: vix[0:n] = i -> ix + indexMap[iy + i]
        IntVector vix = INDEX_SPEC.fromArray(b, iy).add(ix);

        vix = VectorIntrinsics.checkIndex(vix, a.length);

        VectorIntrinsics.storeWithMap(Float256Vector.class, float.class, LENGTH, Int256Vector.class,
                               a, Unsafe.ARRAY_FLOAT_BASE_OFFSET, vix,
                               this,
                               a, ix, b, iy,
                               (arr, idx, v, indexMap, idy) -> v.forEach((i, e) -> arr[idx+indexMap[idy+i]] = e));
    }

     @Override
     @ForceInline
     public final void intoArray(float[] a, int ax, Mask<Float> m, int[] b, int iy) {
         // @@@ This can result in out of bounds errors for unset mask lanes
         Float256Vector oldVal = SPECIES.fromArray(a, ax, b, iy);
         Float256Vector newVal = oldVal.blend(this, m);
         newVal.intoArray(a, ax, b, iy);
     }

    @Override
    @ForceInline
    public void intoByteArray(byte[] a, int ix) {
        Objects.requireNonNull(a);
        ix = VectorIntrinsics.checkIndex(ix, a.length, bitSize() / Byte.SIZE);
        VectorIntrinsics.store(Float256Vector.class, float.class, LENGTH,
                               a, ((long) ix) + Unsafe.ARRAY_BYTE_BASE_OFFSET,
                               this,
                               a, ix,
                               (c, idx, v) -> {
                                   ByteBuffer bbc = ByteBuffer.wrap(c, idx, c.length - idx).order(ByteOrder.nativeOrder());
                                   FloatBuffer tb = bbc.asFloatBuffer();
                                   v.forEach((i, e) -> tb.put(e));
                               });
    }

    @Override
    @ForceInline
    public final void intoByteArray(byte[] a, int ix, Mask<Float> m) {
        Float256Vector oldVal = SPECIES.fromByteArray(a, ix);
        Float256Vector newVal = oldVal.blend(this, m);
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
        VectorIntrinsics.store(Float256Vector.class, float.class, LENGTH,
                               U.getReference(bb, BYTE_BUFFER_HB), ix + U.getLong(bb, BUFFER_ADDRESS),
                               this,
                               bb, ix,
                               (c, idx, v) -> {
                                   ByteBuffer bbc = c.duplicate().position(idx).order(ByteOrder.nativeOrder());
                                   FloatBuffer tb = bbc.asFloatBuffer();
                                   v.forEach((i, e) -> tb.put(e));
                               });
    }

    @Override
    @ForceInline
    public void intoByteBuffer(ByteBuffer bb, int ix, Mask<Float> m) {
        Float256Vector oldVal = SPECIES.fromByteBuffer(bb, ix);
        Float256Vector newVal = oldVal.blend(this, m);
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

        Float256Vector that = (Float256Vector) o;
        return this.equal(that).allTrue();
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(vec);
    }

    // Binary test

    @Override
    Float256Mask bTest(Vector<Float> o, FBinTest f) {
        float[] vec1 = getElements();
        float[] vec2 = ((Float256Vector)o).getElements();
        boolean[] bits = new boolean[length()];
        for (int i = 0; i < length(); i++){
            bits[i] = f.apply(i, vec1[i], vec2[i]);
        }
        return new Float256Mask(bits);
    }

    // Comparisons

    @Override
    @ForceInline
    public Float256Mask equal(Vector<Float> o) {
        Objects.requireNonNull(o);
        Float256Vector v = (Float256Vector)o;

        return VectorIntrinsics.compare(
            BT_eq, Float256Vector.class, Float256Mask.class, float.class, LENGTH,
            this, v,
            (v1, v2) -> v1.bTest(v2, (i, a, b) -> a == b));
    }

    @Override
    @ForceInline
    public Float256Mask notEqual(Vector<Float> o) {
        Objects.requireNonNull(o);
        Float256Vector v = (Float256Vector)o;

        return VectorIntrinsics.compare(
            BT_ne, Float256Vector.class, Float256Mask.class, float.class, LENGTH,
            this, v,
            (v1, v2) -> v1.bTest(v2, (i, a, b) -> a != b));
    }

    @Override
    @ForceInline
    public Float256Mask lessThan(Vector<Float> o) {
        Objects.requireNonNull(o);
        Float256Vector v = (Float256Vector)o;

        return VectorIntrinsics.compare(
            BT_lt, Float256Vector.class, Float256Mask.class, float.class, LENGTH,
            this, v,
            (v1, v2) -> v1.bTest(v2, (i, a, b) -> a < b));
    }

    @Override
    @ForceInline
    public Float256Mask lessThanEq(Vector<Float> o) {
        Objects.requireNonNull(o);
        Float256Vector v = (Float256Vector)o;

        return VectorIntrinsics.compare(
            BT_le, Float256Vector.class, Float256Mask.class, float.class, LENGTH,
            this, v,
            (v1, v2) -> v1.bTest(v2, (i, a, b) -> a <= b));
    }

    @Override
    @ForceInline
    public Float256Mask greaterThan(Vector<Float> o) {
        Objects.requireNonNull(o);
        Float256Vector v = (Float256Vector)o;

        return (Float256Mask) VectorIntrinsics.compare(
            BT_gt, Float256Vector.class, Float256Mask.class, float.class, LENGTH,
            this, v,
            (v1, v2) -> v1.bTest(v2, (i, a, b) -> a > b));
    }

    @Override
    @ForceInline
    public Float256Mask greaterThanEq(Vector<Float> o) {
        Objects.requireNonNull(o);
        Float256Vector v = (Float256Vector)o;

        return VectorIntrinsics.compare(
            BT_ge, Float256Vector.class, Float256Mask.class, float.class, LENGTH,
            this, v,
            (v1, v2) -> v1.bTest(v2, (i, a, b) -> a >= b));
    }

    // Foreach

    @Override
    void forEach(FUnCon f) {
        float[] vec = getElements();
        for (int i = 0; i < length(); i++) {
            f.apply(i, vec[i]);
        }
    }

    @Override
    void forEach(Mask<Float> o, FUnCon f) {
        boolean[] mbits = ((Float256Mask)o).getBits();
        forEach((i, a) -> {
            if (mbits[i]) { f.apply(i, a); }
        });
    }

    Int256Vector toBits() {
        float[] vec = getElements();
        int[] res = new int[this.species().length()];
        for(int i = 0; i < this.species().length(); i++){
            res[i] = Float.floatToIntBits(vec[i]);
        }
        return new Int256Vector(res);
    }


    @Override
    public Float256Vector rotateEL(int j) {
        float[] vec = getElements();
        float[] res = new float[length()];
        for (int i = 0; i < length(); i++){
            res[(j + i) % length()] = vec[i];
        }
        return new Float256Vector(res);
    }

    @Override
    public Float256Vector rotateER(int j) {
        float[] vec = getElements();
        float[] res = new float[length()];
        for (int i = 0; i < length(); i++){
            int z = i - j;
            if(j < 0) {
                res[length() + z] = vec[i];
            } else {
                res[z] = vec[i];
            }
        }
        return new Float256Vector(res);
    }

    @Override
    public Float256Vector shiftEL(int j) {
        float[] vec = getElements();
        float[] res = new float[length()];
        for (int i = 0; i < length() - j; i++) {
            res[i] = vec[i + j];
        }
        return new Float256Vector(res);
    }

    @Override
    public Float256Vector shiftER(int j) {
        float[] vec = getElements();
        float[] res = new float[length()];
        for (int i = 0; i < length() - j; i++){
            res[i + j] = vec[i];
        }
        return new Float256Vector(res);
    }

    @Override
    @ForceInline
    public Float256Vector rearrange(Vector<Float> v,
                                  Shuffle<Float> s, Mask<Float> m) {
        return this.rearrange(s).blend(v.rearrange(s), m);
    }

    @Override
    @ForceInline
    public Float256Vector rearrange(Shuffle<Float> o1) {
    Objects.requireNonNull(o1);
    Float256Shuffle s =  (Float256Shuffle)o1;

        return VectorIntrinsics.rearrangeOp(
            Float256Vector.class, Float256Shuffle.class, float.class, LENGTH,
            this, s,
            (v1, s_) -> v1.uOp((i, a) -> {
            float[] vec = this.getElements();
            int ei = s_.getElement(i);
            return vec[ei];
        }));
    }

    @Override
    @ForceInline
    public Float256Vector blend(Vector<Float> o1, Mask<Float> o2) {
        Objects.requireNonNull(o1);
        Objects.requireNonNull(o2);
        Float256Vector v = (Float256Vector)o1;
        Float256Mask   m = (Float256Mask)o2;

        return VectorIntrinsics.blend(
            Float256Vector.class, Float256Mask.class, float.class, LENGTH,
            this, v, m,
            (v1, v2, m_) -> v1.bOp(v2, (i, a, b) -> m_.getElement(i) ? b : a));
    }

    // Accessors

    @Override
    public float get(int i) {
        if (i < 0 || i >= LENGTH) {
            throw new IllegalArgumentException("Index " + i + " must be zero or positive, and less than " + LENGTH);
        }
        int bits = (int) VectorIntrinsics.extract(
                                Float256Vector.class, float.class, LENGTH,
                                this, i,
                                (vec, ix) -> {
                                    float[] vecarr = vec.getElements();
                                    return (long)Float.floatToIntBits(vecarr[ix]);
                                });
        return Float.intBitsToFloat(bits);
    }

    @Override
    public Float256Vector with(int i, float e) {
        if (i < 0 || i >= LENGTH) {
            throw new IllegalArgumentException("Index " + i + " must be zero or positive, and less than " + LENGTH);
        }
        return VectorIntrinsics.insert(
                                Float256Vector.class, float.class, LENGTH,
                                this, i, (long)Float.floatToIntBits(e),
                                (v, ix, bits) -> {
                                    float[] res = v.getElements().clone();
                                    res[ix] = Float.intBitsToFloat((int)bits);
                                    return new Float256Vector(res);
                                });
    }

    // Mask

    static final class Float256Mask extends AbstractMask<Float> {
        static final Float256Mask TRUE_MASK = new Float256Mask(true);
        static final Float256Mask FALSE_MASK = new Float256Mask(false);

        private final boolean[] bits; // Don't access directly, use getBits() instead.

        public Float256Mask(boolean[] bits) {
            this(bits, 0);
        }

        public Float256Mask(boolean[] bits, int offset) {
            boolean[] a = new boolean[species().length()];
            for (int i = 0; i < a.length; i++) {
                a[i] = bits[offset + i];
            }
            this.bits = a;
        }

        public Float256Mask(boolean val) {
            boolean[] bits = new boolean[species().length()];
            Arrays.fill(bits, val);
            this.bits = bits;
        }

        boolean[] getBits() {
            return VectorIntrinsics.maybeRebox(this).bits;
        }

        @Override
        Float256Mask uOp(MUnOp f) {
            boolean[] res = new boolean[species().length()];
            boolean[] bits = getBits();
            for (int i = 0; i < species().length(); i++) {
                res[i] = f.apply(i, bits[i]);
            }
            return new Float256Mask(res);
        }

        @Override
        Float256Mask bOp(Mask<Float> o, MBinOp f) {
            boolean[] res = new boolean[species().length()];
            boolean[] bits = getBits();
            boolean[] mbits = ((Float256Mask)o).getBits();
            for (int i = 0; i < species().length(); i++) {
                res[i] = f.apply(i, bits[i], mbits[i]);
            }
            return new Float256Mask(res);
        }

        @Override
        public Float256Species species() {
            return SPECIES;
        }

        @Override
        public Float256Vector toVector() {
            float[] res = new float[species().length()];
            boolean[] bits = getBits();
            for (int i = 0; i < species().length(); i++) {
                // -1 will result in the most significant bit being set in
                // addition to some or all other bits
                res[i] = (float) (bits[i] ? -1 : 0);
            }
            return new Float256Vector(res);
        }

        // Unary operations

        @Override
        @ForceInline
        public Float256Mask not() {
            return (Float256Mask) VectorIntrinsics.unaryOp(
                                             VECTOR_OP_NOT, Float256Mask.class, int.class, LENGTH,
                                             this,
                                             (m1) -> m1.uOp((i, a) -> !a));
        }

        // Binary operations

        @Override
        @ForceInline
        public Float256Mask and(Mask<Float> o) {
            Objects.requireNonNull(o);
            Float256Mask m = (Float256Mask)o;
            return VectorIntrinsics.binaryOp(VECTOR_OP_AND, Float256Mask.class, int.class, LENGTH,
                                             this, m,
                                             (m1, m2) -> m1.bOp(m2, (i, a, b) -> a & b));
        }

        @Override
        @ForceInline
        public Float256Mask or(Mask<Float> o) {
            Objects.requireNonNull(o);
            Float256Mask m = (Float256Mask)o;
            return VectorIntrinsics.binaryOp(VECTOR_OP_OR, Float256Mask.class, int.class, LENGTH,
                                             this, m,
                                             (m1, m2) -> m1.bOp(m2, (i, a, b) -> a | b));
        }

        // Reductions

        @Override
        @ForceInline
        public boolean anyTrue() {
            return VectorIntrinsics.test(COND_notZero, Float256Mask.class, int.class, LENGTH,
                                         this, this,
                                         (m1, m2) -> super.anyTrue());
        }

        @Override
        @ForceInline
        public boolean allTrue() {
            return VectorIntrinsics.test(COND_carrySet, Float256Mask.class, int.class, LENGTH,
                                         this, species().maskAllTrue(),
                                         (m1, m2) -> super.allTrue());
        }
    }

    // Shuffle

    static final class Float256Shuffle extends AbstractShuffle<Float> {
        Float256Shuffle(byte[] reorder) {
            super(reorder);
        }

        public Float256Shuffle(int[] reorder) {
            super(reorder);
        }

        public Float256Shuffle(int[] reorder, int i) {
            super(reorder, i);
        }

        public Float256Shuffle(IntUnaryOperator f) {
            super(f);
        }

        @Override
        public Float256Species species() {
            return SPECIES;
        }

        @Override
        public Float256Vector toVector() {
            float[] va = new float[SPECIES.length()];
            for (int i = 0; i < va.length; i++) {
              va[i] = (float) getElement(i);
            }
            return species().fromArray(va, 0);
        }

        @Override
        public Float256Shuffle rearrange(Vector.Shuffle<Float> o) {
            Float256Shuffle s = (Float256Shuffle) o;
            byte[] r = new byte[reorder.length];
            for (int i = 0; i < reorder.length; i++) {
                r[i] = reorder[s.reorder[i]];
            }
            return new Float256Shuffle(r);
        }
    }

    // Species

    @Override
    public Float256Species species() {
        return SPECIES;
    }

    static final class Float256Species extends FloatSpecies {
        static final int BIT_SIZE = Shape.S_256_BIT.bitSize();

        static final int LENGTH = BIT_SIZE / Float.SIZE;

        @Override
        public String toString() {
           StringBuilder sb = new StringBuilder("Shape[");
           sb.append(bitSize()).append(" bits, ");
           sb.append(length()).append(" ").append(float.class.getSimpleName()).append("s x ");
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
        public Class<Float> elementType() {
            return float.class;
        }

        @Override
        @ForceInline
        public int elementSize() {
            return Float.SIZE;
        }

        @Override
        @ForceInline
        public Shape shape() {
            return Shape.S_256_BIT;
        }

        @Override
        Float256Vector op(FOp f) {
            float[] res = new float[length()];
            for (int i = 0; i < length(); i++) {
                res[i] = f.apply(i);
            }
            return new Float256Vector(res);
        }

        @Override
        Float256Vector op(Mask<Float> o, FOp f) {
            float[] res = new float[length()];
            boolean[] mbits = ((Float256Mask)o).getBits();
            for (int i = 0; i < length(); i++) {
                if (mbits[i]) {
                    res[i] = f.apply(i);
                }
            }
            return new Float256Vector(res);
        }

        @Override
        Float256Mask opm(FOpm f) {
            boolean[] res = new boolean[length()];
            for (int i = 0; i < length(); i++) {
                res[i] = (boolean)f.apply(i);
            }
            return new Float256Mask(res);
        }

        // Factories

        @Override
        public Float256Mask maskFromValues(boolean... bits) {
            return new Float256Mask(bits);
        }

        @Override
        public Float256Shuffle shuffle(IntUnaryOperator f) {
            return new Float256Shuffle(f);
        }

        @Override
        public Float256Shuffle shuffleIota() {
            return new Float256Shuffle(AbstractShuffle.IDENTITY);
        }

        @Override
        public Float256Shuffle shuffleFromValues(int... ixs) {
            return new Float256Shuffle(ixs);
        }

        @Override
        public Float256Shuffle shuffleFromArray(int[] ixs, int i) {
            return new Float256Shuffle(ixs, i);
        }

        @Override
        @ForceInline
        public Float256Vector zero() {
            return VectorIntrinsics.broadcastCoerced(Float256Vector.class, float.class, LENGTH,
                                                     Float.floatToIntBits(0.0f),
                                                     (z -> ZERO));
        }

        @Override
        @ForceInline
        public Float256Vector broadcast(float e) {
            return VectorIntrinsics.broadcastCoerced(
                Float256Vector.class, float.class, LENGTH,
                Float.floatToIntBits(e),
                ((long bits) -> SPECIES.op(i -> Float.intBitsToFloat((int)bits))));
        }

        @Override
        @ForceInline
        public Float256Mask maskAllTrue() {
            return VectorIntrinsics.broadcastCoerced(Float256Mask.class, int.class, LENGTH,
                                                     (int)-1,
                                                     (z -> Float256Mask.TRUE_MASK));
        }

        @Override
        @ForceInline
        public Float256Mask maskAllFalse() {
            return VectorIntrinsics.broadcastCoerced(Float256Mask.class, int.class, LENGTH,
                                                     0,
                                                     (z -> Float256Mask.FALSE_MASK));
        }

        @Override
        @ForceInline
        public Float256Vector scalars(float... es) {
            Objects.requireNonNull(es);
            int ix = VectorIntrinsics.checkIndex(0, es.length, LENGTH);
            return VectorIntrinsics.load(Float256Vector.class, float.class, LENGTH,
                                         es, Unsafe.ARRAY_FLOAT_BASE_OFFSET,
                                         es, ix,
                                         (c, idx) -> op(n -> c[idx + n]));
        }

        @Override
        @ForceInline
        public Float256Mask maskFromArray(boolean[] bits, int ix) {
            Objects.requireNonNull(bits);
            ix = VectorIntrinsics.checkIndex(ix, bits.length, LENGTH);
            return VectorIntrinsics.load(Float256Mask.class, int.class, LENGTH,
                                         bits, (long)ix + Unsafe.ARRAY_BOOLEAN_BASE_OFFSET,
                                         bits, ix,
                                         (c, idx) -> opm(n -> c[idx + n]));
        }

        @Override
        @ForceInline
        public Float256Vector fromArray(float[] a, int ix) {
            Objects.requireNonNull(a);
            ix = VectorIntrinsics.checkIndex(ix, a.length, LENGTH);
            return VectorIntrinsics.load(Float256Vector.class, float.class, LENGTH,
                                         a, (((long) ix) << ARRAY_SHIFT) + Unsafe.ARRAY_FLOAT_BASE_OFFSET,
                                         a, ix,
                                         (c, idx) -> op(n -> c[idx + n]));
        }

        @Override
        @ForceInline
        public Float256Vector fromArray(float[] a, int ax, Mask<Float> m) {
            return zero().blend(fromArray(a, ax), m);
        }

        @Override
        @ForceInline
        public Float256Vector fromByteArray(byte[] a, int ix) {
            Objects.requireNonNull(a);
            ix = VectorIntrinsics.checkIndex(ix, a.length, bitSize() / Byte.SIZE);
            return VectorIntrinsics.load(Float256Vector.class, float.class, LENGTH,
                                         a, ((long) ix) + Unsafe.ARRAY_BYTE_BASE_OFFSET,
                                         a, ix,
                                         (c, idx) -> {
                                             ByteBuffer bbc = ByteBuffer.wrap(c, idx, a.length - idx).order(ByteOrder.nativeOrder());
                                             FloatBuffer tb = bbc.asFloatBuffer();
                                             return op(i -> tb.get());
                                         });
        }
        @Override
        @ForceInline
        public Float256Vector fromArray(float[] a, int ix, int[] b, int iy) {
            Objects.requireNonNull(a);
            Objects.requireNonNull(b);

            // Index vector: vix[0:n] = i -> ix + indexMap[iy + i]
            IntVector vix = INDEX_SPEC.fromArray(b, iy).add(ix);

            vix = VectorIntrinsics.checkIndex(vix, a.length);

            return VectorIntrinsics.loadWithMap(Float256Vector.class, float.class, LENGTH, Int256Vector.class,
                                        a, Unsafe.ARRAY_FLOAT_BASE_OFFSET, vix,
                                        a, ix, b, iy,
                                       (c, idx, indexMap, idy) -> op(n -> c[idx + indexMap[idy+n]]));
       }

       @Override
       @ForceInline
       public Float256Vector fromArray(float[] a, int ax, Mask<Float> m, int[] indexMap, int j) {
           // @@@ This can result in out of bounds errors for unset mask lanes
           return zero().blend(fromArray(a, ax, indexMap, j), m);
       }


        @Override
        @ForceInline
        public Float256Vector fromByteArray(byte[] a, int ix, Mask<Float> m) {
            return zero().blend(fromByteArray(a, ix), m);
        }

        @Override
        @ForceInline
        public Float256Vector fromByteBuffer(ByteBuffer bb, int ix) {
            if (bb.order() != ByteOrder.nativeOrder()) {
                throw new IllegalArgumentException();
            }
            ix = VectorIntrinsics.checkIndex(ix, bb.limit(), bitSize() / Byte.SIZE);
            return VectorIntrinsics.load(Float256Vector.class, float.class, LENGTH,
                                         U.getReference(bb, BYTE_BUFFER_HB), U.getLong(bb, BUFFER_ADDRESS) + ix,
                                         bb, ix,
                                         (c, idx) -> {
                                             ByteBuffer bbc = c.duplicate().position(idx).order(ByteOrder.nativeOrder());
                                             FloatBuffer tb = bbc.asFloatBuffer();
                                             return op(i -> tb.get());
                                         });
        }

        @Override
        @ForceInline
        public Float256Vector fromByteBuffer(ByteBuffer bb, int ix, Mask<Float> m) {
            return zero().blend(fromByteBuffer(bb, ix), m);
        }

        @Override
        @ForceInline
        @SuppressWarnings("unchecked")
        public <F> Float256Vector cast(Vector<F> o) {
            if (o.length() != LENGTH)
                throw new IllegalArgumentException("Vector length this species length differ");

            return VectorIntrinsics.cast(
                o.getClass(),
                o.elementType(), LENGTH,
                Float256Vector.class,
                float.class, LENGTH,
                o, this,
                (s, v) -> s.castDefault(v)
            );
        }

        @SuppressWarnings("unchecked")
        @ForceInline
        private <F> Float256Vector castDefault(Vector<F> v) {
            // Allocate array of required size
            int limit = length();
            float[] a = new float[limit];

            Class<?> vtype = v.species().elementType();
            if (vtype == byte.class) {
                ByteVector tv = (ByteVector)v;
                for (int i = 0; i < limit; i++) {
                    a[i] = (float) tv.get(i);
                }
            } else if (vtype == short.class) {
                ShortVector tv = (ShortVector)v;
                for (int i = 0; i < limit; i++) {
                    a[i] = (float) tv.get(i);
                }
            } else if (vtype == int.class) {
                IntVector tv = (IntVector)v;
                for (int i = 0; i < limit; i++) {
                    a[i] = (float) tv.get(i);
                }
            } else if (vtype == long.class){
                LongVector tv = (LongVector)v;
                for (int i = 0; i < limit; i++) {
                    a[i] = (float) tv.get(i);
                }
            } else if (vtype == float.class){
                FloatVector tv = (FloatVector)v;
                for (int i = 0; i < limit; i++) {
                    a[i] = (float) tv.get(i);
                }
            } else if (vtype == double.class){
                DoubleVector tv = (DoubleVector)v;
                for (int i = 0; i < limit; i++) {
                    a[i] = (float) tv.get(i);
                }
            } else {
                throw new UnsupportedOperationException("Bad lane type for casting.");
            }

            return scalars(a);
        }

        @Override
        @ForceInline
        public <E> Float256Mask cast(Mask<E> m) {
            if (m.length() != LENGTH)
                throw new IllegalArgumentException("Mask length this species length differ");
            return new Float256Mask(m.toArray());
        }

        @Override
        @ForceInline
        public <E> Float256Shuffle cast(Shuffle<E> s) {
            if (s.length() != LENGTH)
                throw new IllegalArgumentException("Shuffle length this species length differ");
            return new Float256Shuffle(s.toArray());
        }

        @Override
        @ForceInline
        @SuppressWarnings("unchecked")
        public <F> Float256Vector rebracket(Vector<F> o) {
            Objects.requireNonNull(o);
            if (o.elementType() == byte.class) {
                Byte256Vector so = (Byte256Vector)o;
                return VectorIntrinsics.reinterpret(
                    Byte256Vector.class,
                    byte.class, so.length(),
                    Float256Vector.class,
                    float.class, LENGTH,
                    so, this,
                    (s, v) -> (Float256Vector) s.reshape(v)
                );
            } else if (o.elementType() == short.class) {
                Short256Vector so = (Short256Vector)o;
                return VectorIntrinsics.reinterpret(
                    Short256Vector.class,
                    short.class, so.length(),
                    Float256Vector.class,
                    float.class, LENGTH,
                    so, this,
                    (s, v) -> (Float256Vector) s.reshape(v)
                );
            } else if (o.elementType() == int.class) {
                Int256Vector so = (Int256Vector)o;
                return VectorIntrinsics.reinterpret(
                    Int256Vector.class,
                    int.class, so.length(),
                    Float256Vector.class,
                    float.class, LENGTH,
                    so, this,
                    (s, v) -> (Float256Vector) s.reshape(v)
                );
            } else if (o.elementType() == long.class) {
                Long256Vector so = (Long256Vector)o;
                return VectorIntrinsics.reinterpret(
                    Long256Vector.class,
                    long.class, so.length(),
                    Float256Vector.class,
                    float.class, LENGTH,
                    so, this,
                    (s, v) -> (Float256Vector) s.reshape(v)
                );
            } else if (o.elementType() == float.class) {
                Float256Vector so = (Float256Vector)o;
                return VectorIntrinsics.reinterpret(
                    Float256Vector.class,
                    float.class, so.length(),
                    Float256Vector.class,
                    float.class, LENGTH,
                    so, this,
                    (s, v) -> (Float256Vector) s.reshape(v)
                );
            } else if (o.elementType() == double.class) {
                Double256Vector so = (Double256Vector)o;
                return VectorIntrinsics.reinterpret(
                    Double256Vector.class,
                    double.class, so.length(),
                    Float256Vector.class,
                    float.class, LENGTH,
                    so, this,
                    (s, v) -> (Float256Vector) s.reshape(v)
                );
            } else {
                throw new InternalError("Unimplemented type");
            }
        }

        @Override
        @ForceInline
        @SuppressWarnings("unchecked")
        public Float256Vector resize(Vector<Float> o) {
            Objects.requireNonNull(o);
            if (o.bitSize() == 64 && (o instanceof Float64Vector)) {
                Float64Vector so = (Float64Vector)o;
                return VectorIntrinsics.reinterpret(
                    Float64Vector.class,
                    float.class, so.length(),
                    Float256Vector.class,
                    float.class, LENGTH,
                    so, this,
                    (s, v) -> (Float256Vector) s.reshape(v)
                );
            } else if (o.bitSize() == 128 && (o instanceof Float128Vector)) {
                Float128Vector so = (Float128Vector)o;
                return VectorIntrinsics.reinterpret(
                    Float128Vector.class,
                    float.class, so.length(),
                    Float256Vector.class,
                    float.class, LENGTH,
                    so, this,
                    (s, v) -> (Float256Vector) s.reshape(v)
                );
            } else if (o.bitSize() == 256 && (o instanceof Float256Vector)) {
                Float256Vector so = (Float256Vector)o;
                return VectorIntrinsics.reinterpret(
                    Float256Vector.class,
                    float.class, so.length(),
                    Float256Vector.class,
                    float.class, LENGTH,
                    so, this,
                    (s, v) -> (Float256Vector) s.reshape(v)
                );
            } else if (o.bitSize() == 512 && (o instanceof Float512Vector)) {
                Float512Vector so = (Float512Vector)o;
                return VectorIntrinsics.reinterpret(
                    Float512Vector.class,
                    float.class, so.length(),
                    Float256Vector.class,
                    float.class, LENGTH,
                    so, this,
                    (s, v) -> (Float256Vector) s.reshape(v)
                );
            } else if ((o.bitSize() > 0) && (o.bitSize() <= 2048)
                    && (o.bitSize() % 128 == 0) && (o instanceof FloatMaxVector)) {
                FloatMaxVector so = (FloatMaxVector)o;
                return VectorIntrinsics.reinterpret(
                    FloatMaxVector.class,
                    float.class, so.length(),
                    Float256Vector.class,
                    float.class, LENGTH,
                    so, this,
                    (s, v) -> (Float256Vector) s.reshape(v)
                );
            } else {
                throw new InternalError("Unimplemented size");
            }
        }
    }
}
