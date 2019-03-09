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
final class FloatMaxVector extends FloatVector {
    static final FloatMaxSpecies SPECIES = new FloatMaxSpecies();

    static final FloatMaxVector ZERO = new FloatMaxVector();

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

    FloatMaxVector() {
        vec = new float[SPECIES.length()];
    }

    FloatMaxVector(float[] v) {
        vec = v;
    }

    @Override
    public int length() { return LENGTH; }

    // Unary operator

    @Override
    FloatMaxVector uOp(FUnOp f) {
        float[] vec = getElements();
        float[] res = new float[length()];
        for (int i = 0; i < length(); i++) {
            res[i] = f.apply(i, vec[i]);
        }
        return new FloatMaxVector(res);
    }

    @Override
    FloatMaxVector uOp(Mask<Float> o, FUnOp f) {
        float[] vec = getElements();
        float[] res = new float[length()];
        boolean[] mbits = ((FloatMaxMask)o).getBits();
        for (int i = 0; i < length(); i++) {
            res[i] = mbits[i] ? f.apply(i, vec[i]) : vec[i];
        }
        return new FloatMaxVector(res);
    }

    // Binary operator

    @Override
    FloatMaxVector bOp(Vector<Float> o, FBinOp f) {
        float[] res = new float[length()];
        float[] vec1 = this.getElements();
        float[] vec2 = ((FloatMaxVector)o).getElements();
        for (int i = 0; i < length(); i++) {
            res[i] = f.apply(i, vec1[i], vec2[i]);
        }
        return new FloatMaxVector(res);
    }

    @Override
    FloatMaxVector bOp(Vector<Float> o1, Mask<Float> o2, FBinOp f) {
        float[] res = new float[length()];
        float[] vec1 = this.getElements();
        float[] vec2 = ((FloatMaxVector)o1).getElements();
        boolean[] mbits = ((FloatMaxMask)o2).getBits();
        for (int i = 0; i < length(); i++) {
            res[i] = mbits[i] ? f.apply(i, vec1[i], vec2[i]) : vec1[i];
        }
        return new FloatMaxVector(res);
    }

    // Trinary operator

    @Override
    FloatMaxVector tOp(Vector<Float> o1, Vector<Float> o2, FTriOp f) {
        float[] res = new float[length()];
        float[] vec1 = this.getElements();
        float[] vec2 = ((FloatMaxVector)o1).getElements();
        float[] vec3 = ((FloatMaxVector)o2).getElements();
        for (int i = 0; i < length(); i++) {
            res[i] = f.apply(i, vec1[i], vec2[i], vec3[i]);
        }
        return new FloatMaxVector(res);
    }

    @Override
    FloatMaxVector tOp(Vector<Float> o1, Vector<Float> o2, Mask<Float> o3, FTriOp f) {
        float[] res = new float[length()];
        float[] vec1 = getElements();
        float[] vec2 = ((FloatMaxVector)o1).getElements();
        float[] vec3 = ((FloatMaxVector)o2).getElements();
        boolean[] mbits = ((FloatMaxMask)o3).getBits();
        for (int i = 0; i < length(); i++) {
            res[i] = mbits[i] ? f.apply(i, vec1[i], vec2[i], vec3[i]) : vec1[i];
        }
        return new FloatMaxVector(res);
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
    public FloatMaxVector div(Vector<Float> v, Mask<Float> m) {
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
    public FloatMaxVector neg(Mask<Float> m) {
        return blend(neg(), m);
    }

    @Override
    @ForceInline
    public FloatMaxVector abs() {
        return VectorIntrinsics.unaryOp(
            VECTOR_OP_ABS, FloatMaxVector.class, float.class, LENGTH,
            this,
            v1 -> v1.uOp((i, a) -> (float) Math.abs(a)));
    }

    @ForceInline
    @Override
    public FloatMaxVector abs(Mask<Float> m) {
        return blend(abs(), m);
    }

    @Override
    @ForceInline
    public FloatMaxVector neg() {
        return VectorIntrinsics.unaryOp(
            VECTOR_OP_NEG, FloatMaxVector.class, float.class, LENGTH,
            this,
            v1 -> v1.uOp((i, a) -> (float) -a));
    }

    @Override
    @ForceInline
    public FloatMaxVector div(Vector<Float> o) {
        Objects.requireNonNull(o);
        FloatMaxVector v = (FloatMaxVector)o;
        return VectorIntrinsics.binaryOp(
            VECTOR_OP_DIV, FloatMaxVector.class, float.class, LENGTH,
            this, v,
            (v1, v2) -> v1.bOp(v2, (i, a, b) -> (float)(a / b)));
    }

    @Override
    @ForceInline
    public FloatMaxVector sqrt() {
        return VectorIntrinsics.unaryOp(
            VECTOR_OP_SQRT, FloatMaxVector.class, float.class, LENGTH,
            this,
            v1 -> v1.uOp((i, a) -> (float) Math.sqrt((double) a)));
    }

    @Override
    @ForceInline
    public FloatMaxVector exp() {
        return (FloatMaxVector) VectorIntrinsics.unaryOp(
            VECTOR_OP_EXP, FloatMaxVector.class, float.class, LENGTH,
            this,
            v1 -> ((FloatMaxVector)v1).uOp((i, a) -> (float) Math.exp((double) a)));
    }

    @Override
    @ForceInline
    public FloatMaxVector log1p() {
        return (FloatMaxVector) VectorIntrinsics.unaryOp(
            VECTOR_OP_LOG1P, FloatMaxVector.class, float.class, LENGTH,
            this,
            v1 -> ((FloatMaxVector)v1).uOp((i, a) -> (float) Math.log1p((double) a)));
    }

    @Override
    @ForceInline
    public FloatMaxVector log() {
        return (FloatMaxVector) VectorIntrinsics.unaryOp(
            VECTOR_OP_LOG, FloatMaxVector.class, float.class, LENGTH,
            this,
            v1 -> ((FloatMaxVector)v1).uOp((i, a) -> (float) Math.log((double) a)));
    }

    @Override
    @ForceInline
    public FloatMaxVector log10() {
        return (FloatMaxVector) VectorIntrinsics.unaryOp(
            VECTOR_OP_LOG10, FloatMaxVector.class, float.class, LENGTH,
            this,
            v1 -> ((FloatMaxVector)v1).uOp((i, a) -> (float) Math.log10((double) a)));
    }

    @Override
    @ForceInline
    public FloatMaxVector expm1() {
        return (FloatMaxVector) VectorIntrinsics.unaryOp(
            VECTOR_OP_EXPM1, FloatMaxVector.class, float.class, LENGTH,
            this,
            v1 -> ((FloatMaxVector)v1).uOp((i, a) -> (float) Math.expm1((double) a)));
    }

    @Override
    @ForceInline
    public FloatMaxVector cbrt() {
        return (FloatMaxVector) VectorIntrinsics.unaryOp(
            VECTOR_OP_CBRT, FloatMaxVector.class, float.class, LENGTH,
            this,
            v1 -> ((FloatMaxVector)v1).uOp((i, a) -> (float) Math.cbrt((double) a)));
    }

    @Override
    @ForceInline
    public FloatMaxVector sin() {
        return (FloatMaxVector) VectorIntrinsics.unaryOp(
            VECTOR_OP_SIN, FloatMaxVector.class, float.class, LENGTH,
            this,
            v1 -> ((FloatMaxVector)v1).uOp((i, a) -> (float) Math.sin((double) a)));
    }

    @Override
    @ForceInline
    public FloatMaxVector cos() {
        return (FloatMaxVector) VectorIntrinsics.unaryOp(
            VECTOR_OP_COS, FloatMaxVector.class, float.class, LENGTH,
            this,
            v1 -> ((FloatMaxVector)v1).uOp((i, a) -> (float) Math.cos((double) a)));
    }

    @Override
    @ForceInline
    public FloatMaxVector tan() {
        return (FloatMaxVector) VectorIntrinsics.unaryOp(
            VECTOR_OP_TAN, FloatMaxVector.class, float.class, LENGTH,
            this,
            v1 -> ((FloatMaxVector)v1).uOp((i, a) -> (float) Math.tan((double) a)));
    }

    @Override
    @ForceInline
    public FloatMaxVector asin() {
        return (FloatMaxVector) VectorIntrinsics.unaryOp(
            VECTOR_OP_ASIN, FloatMaxVector.class, float.class, LENGTH,
            this,
            v1 -> ((FloatMaxVector)v1).uOp((i, a) -> (float) Math.asin((double) a)));
    }

    @Override
    @ForceInline
    public FloatMaxVector acos() {
        return (FloatMaxVector) VectorIntrinsics.unaryOp(
            VECTOR_OP_ACOS, FloatMaxVector.class, float.class, LENGTH,
            this,
            v1 -> ((FloatMaxVector)v1).uOp((i, a) -> (float) Math.acos((double) a)));
    }

    @Override
    @ForceInline
    public FloatMaxVector atan() {
        return (FloatMaxVector) VectorIntrinsics.unaryOp(
            VECTOR_OP_ATAN, FloatMaxVector.class, float.class, LENGTH,
            this,
            v1 -> ((FloatMaxVector)v1).uOp((i, a) -> (float) Math.atan((double) a)));
    }

    @Override
    @ForceInline
    public FloatMaxVector sinh() {
        return (FloatMaxVector) VectorIntrinsics.unaryOp(
            VECTOR_OP_SINH, FloatMaxVector.class, float.class, LENGTH,
            this,
            v1 -> ((FloatMaxVector)v1).uOp((i, a) -> (float) Math.sinh((double) a)));
    }

    @Override
    @ForceInline
    public FloatMaxVector cosh() {
        return (FloatMaxVector) VectorIntrinsics.unaryOp(
            VECTOR_OP_COSH, FloatMaxVector.class, float.class, LENGTH,
            this,
            v1 -> ((FloatMaxVector)v1).uOp((i, a) -> (float) Math.cosh((double) a)));
    }

    @Override
    @ForceInline
    public FloatMaxVector tanh() {
        return (FloatMaxVector) VectorIntrinsics.unaryOp(
            VECTOR_OP_TANH, FloatMaxVector.class, float.class, LENGTH,
            this,
            v1 -> ((FloatMaxVector)v1).uOp((i, a) -> (float) Math.tanh((double) a)));
    }

    @Override
    @ForceInline
    public FloatMaxVector pow(Vector<Float> o) {
        Objects.requireNonNull(o);
        FloatMaxVector v = (FloatMaxVector)o;
        return (FloatMaxVector) VectorIntrinsics.binaryOp(
            VECTOR_OP_POW, FloatMaxVector.class, float.class, LENGTH,
            this, v,
            (v1, v2) -> ((FloatMaxVector)v1).bOp(v2, (i, a, b) -> (float)(Math.pow(a,b))));
    }

    @Override
    @ForceInline
    public FloatMaxVector hypot(Vector<Float> o) {
        Objects.requireNonNull(o);
        FloatMaxVector v = (FloatMaxVector)o;
        return (FloatMaxVector) VectorIntrinsics.binaryOp(
            VECTOR_OP_HYPOT, FloatMaxVector.class, float.class, LENGTH,
            this, v,
            (v1, v2) -> ((FloatMaxVector)v1).bOp(v2, (i, a, b) -> (float)(Math.hypot(a,b))));
    }

    @Override
    @ForceInline
    public FloatMaxVector atan2(Vector<Float> o) {
        Objects.requireNonNull(o);
        FloatMaxVector v = (FloatMaxVector)o;
        return (FloatMaxVector) VectorIntrinsics.binaryOp(
            VECTOR_OP_ATAN2, FloatMaxVector.class, float.class, LENGTH,
            this, v,
            (v1, v2) -> ((FloatMaxVector)v1).bOp(v2, (i, a, b) -> (float)(Math.atan2(a,b))));
    }


    // Binary operations

    @Override
    @ForceInline
    public FloatMaxVector add(Vector<Float> o) {
        Objects.requireNonNull(o);
        FloatMaxVector v = (FloatMaxVector)o;
        return VectorIntrinsics.binaryOp(
            VECTOR_OP_ADD, FloatMaxVector.class, float.class, LENGTH,
            this, v,
            (v1, v2) -> v1.bOp(v2, (i, a, b) -> (float)(a + b)));
    }

    @Override
    @ForceInline
    public FloatMaxVector add(Vector<Float> v, Mask<Float> m) {
        return blend(add(v), m);
    }

    @Override
    @ForceInline
    public FloatMaxVector sub(Vector<Float> o) {
        Objects.requireNonNull(o);
        FloatMaxVector v = (FloatMaxVector)o;
        return VectorIntrinsics.binaryOp(
            VECTOR_OP_SUB, FloatMaxVector.class, float.class, LENGTH,
            this, v,
            (v1, v2) -> v1.bOp(v2, (i, a, b) -> (float)(a - b)));
    }

    @Override
    @ForceInline
    public FloatMaxVector sub(Vector<Float> v, Mask<Float> m) {
        return blend(sub(v), m);
    }

    @Override
    @ForceInline
    public FloatMaxVector mul(Vector<Float> o) {
        Objects.requireNonNull(o);
        FloatMaxVector v = (FloatMaxVector)o;
        return VectorIntrinsics.binaryOp(
            VECTOR_OP_MUL, FloatMaxVector.class, float.class, LENGTH,
            this, v,
            (v1, v2) -> v1.bOp(v2, (i, a, b) -> (float)(a * b)));
    }

    @Override
    @ForceInline
    public FloatMaxVector mul(Vector<Float> v, Mask<Float> m) {
        return blend(mul(v), m);
    }

    @Override
    @ForceInline
    public FloatMaxVector min(Vector<Float> o) {
        Objects.requireNonNull(o);
        FloatMaxVector v = (FloatMaxVector)o;
        return (FloatMaxVector) VectorIntrinsics.binaryOp(
            VECTOR_OP_MIN, FloatMaxVector.class, float.class, LENGTH,
            this, v,
            (v1, v2) -> ((FloatMaxVector)v1).bOp(v2, (i, a, b) -> (float) ((a < b) ? a : b)));
    }

    @Override
    @ForceInline
    public FloatMaxVector min(Vector<Float> v, Mask<Float> m) {
        return blend(min(v), m);
    }

    @Override
    @ForceInline
    public FloatMaxVector max(Vector<Float> o) {
        Objects.requireNonNull(o);
        FloatMaxVector v = (FloatMaxVector)o;
        return VectorIntrinsics.binaryOp(
            VECTOR_OP_MAX, FloatMaxVector.class, float.class, LENGTH,
            this, v,
            (v1, v2) -> v1.bOp(v2, (i, a, b) -> (float) ((a > b) ? a : b)));
        }

    @Override
    @ForceInline
    public FloatMaxVector max(Vector<Float> v, Mask<Float> m) {
        return blend(max(v), m);
    }


    // Ternary operations

    @Override
    @ForceInline
    public FloatMaxVector fma(Vector<Float> o1, Vector<Float> o2) {
        Objects.requireNonNull(o1);
        Objects.requireNonNull(o2);
        FloatMaxVector v1 = (FloatMaxVector)o1;
        FloatMaxVector v2 = (FloatMaxVector)o2;
        return VectorIntrinsics.ternaryOp(
            VECTOR_OP_FMA, FloatMaxVector.class, float.class, LENGTH,
            this, v1, v2,
            (w1, w2, w3) -> w1.tOp(w2, w3, (i, a, b, c) -> Math.fma(a, b, c)));
    }

    // Type specific horizontal reductions

    @Override
    @ForceInline
    public float addAll() {
        int bits = (int) VectorIntrinsics.reductionCoerced(
                                VECTOR_OP_ADD, FloatMaxVector.class, float.class, LENGTH,
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
                                VECTOR_OP_SUB, FloatMaxVector.class, float.class, LENGTH,
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
                                VECTOR_OP_MUL, FloatMaxVector.class, float.class, LENGTH,
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
                                VECTOR_OP_MIN, FloatMaxVector.class, float.class, LENGTH,
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
                                VECTOR_OP_MAX, FloatMaxVector.class, float.class, LENGTH,
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
        VectorIntrinsics.store(FloatMaxVector.class, float.class, LENGTH,
                               a, (((long) ix) << ARRAY_SHIFT) + Unsafe.ARRAY_FLOAT_BASE_OFFSET,
                               this,
                               a, ix,
                               (arr, idx, v) -> v.forEach((i, e) -> arr[idx + i] = e));
    }

    @Override
    @ForceInline
    public final void intoArray(float[] a, int ax, Mask<Float> m) {
        FloatMaxVector oldVal = SPECIES.fromArray(a, ax);
        FloatMaxVector newVal = oldVal.blend(this, m);
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

        VectorIntrinsics.storeWithMap(FloatMaxVector.class, float.class, LENGTH, vix.getClass(),
                               a, Unsafe.ARRAY_FLOAT_BASE_OFFSET, vix,
                               this,
                               a, ix, b, iy,
                               (arr, idx, v, indexMap, idy) -> v.forEach((i, e) -> arr[idx+indexMap[idy+i]] = e));
    }

     @Override
     @ForceInline
     public final void intoArray(float[] a, int ax, Mask<Float> m, int[] b, int iy) {
         // @@@ This can result in out of bounds errors for unset mask lanes
         FloatMaxVector oldVal = SPECIES.fromArray(a, ax, b, iy);
         FloatMaxVector newVal = oldVal.blend(this, m);
         newVal.intoArray(a, ax, b, iy);
     }

    @Override
    @ForceInline
    public void intoByteArray(byte[] a, int ix) {
        Objects.requireNonNull(a);
        ix = VectorIntrinsics.checkIndex(ix, a.length, bitSize() / Byte.SIZE);
        VectorIntrinsics.store(FloatMaxVector.class, float.class, LENGTH,
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
        FloatMaxVector oldVal = SPECIES.fromByteArray(a, ix);
        FloatMaxVector newVal = oldVal.blend(this, m);
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
        VectorIntrinsics.store(FloatMaxVector.class, float.class, LENGTH,
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
        FloatMaxVector oldVal = SPECIES.fromByteBuffer(bb, ix);
        FloatMaxVector newVal = oldVal.blend(this, m);
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

        FloatMaxVector that = (FloatMaxVector) o;
        return this.equal(that).allTrue();
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(vec);
    }

    // Binary test

    @Override
    FloatMaxMask bTest(Vector<Float> o, FBinTest f) {
        float[] vec1 = getElements();
        float[] vec2 = ((FloatMaxVector)o).getElements();
        boolean[] bits = new boolean[length()];
        for (int i = 0; i < length(); i++){
            bits[i] = f.apply(i, vec1[i], vec2[i]);
        }
        return new FloatMaxMask(bits);
    }

    // Comparisons

    @Override
    @ForceInline
    public FloatMaxMask equal(Vector<Float> o) {
        Objects.requireNonNull(o);
        FloatMaxVector v = (FloatMaxVector)o;

        return VectorIntrinsics.compare(
            BT_eq, FloatMaxVector.class, FloatMaxMask.class, float.class, LENGTH,
            this, v,
            (v1, v2) -> v1.bTest(v2, (i, a, b) -> a == b));
    }

    @Override
    @ForceInline
    public FloatMaxMask notEqual(Vector<Float> o) {
        Objects.requireNonNull(o);
        FloatMaxVector v = (FloatMaxVector)o;

        return VectorIntrinsics.compare(
            BT_ne, FloatMaxVector.class, FloatMaxMask.class, float.class, LENGTH,
            this, v,
            (v1, v2) -> v1.bTest(v2, (i, a, b) -> a != b));
    }

    @Override
    @ForceInline
    public FloatMaxMask lessThan(Vector<Float> o) {
        Objects.requireNonNull(o);
        FloatMaxVector v = (FloatMaxVector)o;

        return VectorIntrinsics.compare(
            BT_lt, FloatMaxVector.class, FloatMaxMask.class, float.class, LENGTH,
            this, v,
            (v1, v2) -> v1.bTest(v2, (i, a, b) -> a < b));
    }

    @Override
    @ForceInline
    public FloatMaxMask lessThanEq(Vector<Float> o) {
        Objects.requireNonNull(o);
        FloatMaxVector v = (FloatMaxVector)o;

        return VectorIntrinsics.compare(
            BT_le, FloatMaxVector.class, FloatMaxMask.class, float.class, LENGTH,
            this, v,
            (v1, v2) -> v1.bTest(v2, (i, a, b) -> a <= b));
    }

    @Override
    @ForceInline
    public FloatMaxMask greaterThan(Vector<Float> o) {
        Objects.requireNonNull(o);
        FloatMaxVector v = (FloatMaxVector)o;

        return (FloatMaxMask) VectorIntrinsics.compare(
            BT_gt, FloatMaxVector.class, FloatMaxMask.class, float.class, LENGTH,
            this, v,
            (v1, v2) -> v1.bTest(v2, (i, a, b) -> a > b));
    }

    @Override
    @ForceInline
    public FloatMaxMask greaterThanEq(Vector<Float> o) {
        Objects.requireNonNull(o);
        FloatMaxVector v = (FloatMaxVector)o;

        return VectorIntrinsics.compare(
            BT_ge, FloatMaxVector.class, FloatMaxMask.class, float.class, LENGTH,
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
        boolean[] mbits = ((FloatMaxMask)o).getBits();
        forEach((i, a) -> {
            if (mbits[i]) { f.apply(i, a); }
        });
    }

    IntMaxVector toBits() {
        float[] vec = getElements();
        int[] res = new int[this.species().length()];
        for(int i = 0; i < this.species().length(); i++){
            res[i] = Float.floatToIntBits(vec[i]);
        }
        return new IntMaxVector(res);
    }


    @Override
    public FloatMaxVector rotateEL(int j) {
        float[] vec = getElements();
        float[] res = new float[length()];
        for (int i = 0; i < length(); i++){
            res[(j + i) % length()] = vec[i];
        }
        return new FloatMaxVector(res);
    }

    @Override
    public FloatMaxVector rotateER(int j) {
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
        return new FloatMaxVector(res);
    }

    @Override
    public FloatMaxVector shiftEL(int j) {
        float[] vec = getElements();
        float[] res = new float[length()];
        for (int i = 0; i < length() - j; i++) {
            res[i] = vec[i + j];
        }
        return new FloatMaxVector(res);
    }

    @Override
    public FloatMaxVector shiftER(int j) {
        float[] vec = getElements();
        float[] res = new float[length()];
        for (int i = 0; i < length() - j; i++){
            res[i + j] = vec[i];
        }
        return new FloatMaxVector(res);
    }

    @Override
    @ForceInline
    public FloatMaxVector rearrange(Vector<Float> v,
                                  Shuffle<Float> s, Mask<Float> m) {
        return this.rearrange(s).blend(v.rearrange(s), m);
    }

    @Override
    @ForceInline
    public FloatMaxVector rearrange(Shuffle<Float> o1) {
    Objects.requireNonNull(o1);
    FloatMaxShuffle s =  (FloatMaxShuffle)o1;

        return VectorIntrinsics.rearrangeOp(
            FloatMaxVector.class, FloatMaxShuffle.class, float.class, LENGTH,
            this, s,
            (v1, s_) -> v1.uOp((i, a) -> {
            float[] vec = this.getElements();
            int ei = s_.getElement(i);
            return vec[ei];
        }));
    }

    @Override
    @ForceInline
    public FloatMaxVector blend(Vector<Float> o1, Mask<Float> o2) {
        Objects.requireNonNull(o1);
        Objects.requireNonNull(o2);
        FloatMaxVector v = (FloatMaxVector)o1;
        FloatMaxMask   m = (FloatMaxMask)o2;

        return VectorIntrinsics.blend(
            FloatMaxVector.class, FloatMaxMask.class, float.class, LENGTH,
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
                                FloatMaxVector.class, float.class, LENGTH,
                                this, i,
                                (vec, ix) -> {
                                    float[] vecarr = vec.getElements();
                                    return (long)Float.floatToIntBits(vecarr[ix]);
                                });
        return Float.intBitsToFloat(bits);
    }

    @Override
    public FloatMaxVector with(int i, float e) {
        if (i < 0 || i >= LENGTH) {
            throw new IllegalArgumentException("Index " + i + " must be zero or positive, and less than " + LENGTH);
        }
        return VectorIntrinsics.insert(
                                FloatMaxVector.class, float.class, LENGTH,
                                this, i, (long)Float.floatToIntBits(e),
                                (v, ix, bits) -> {
                                    float[] res = v.getElements().clone();
                                    res[ix] = Float.intBitsToFloat((int)bits);
                                    return new FloatMaxVector(res);
                                });
    }

    // Mask

    static final class FloatMaxMask extends AbstractMask<Float> {
        static final FloatMaxMask TRUE_MASK = new FloatMaxMask(true);
        static final FloatMaxMask FALSE_MASK = new FloatMaxMask(false);

        private final boolean[] bits; // Don't access directly, use getBits() instead.

        public FloatMaxMask(boolean[] bits) {
            this(bits, 0);
        }

        public FloatMaxMask(boolean[] bits, int offset) {
            boolean[] a = new boolean[species().length()];
            for (int i = 0; i < a.length; i++) {
                a[i] = bits[offset + i];
            }
            this.bits = a;
        }

        public FloatMaxMask(boolean val) {
            boolean[] bits = new boolean[species().length()];
            Arrays.fill(bits, val);
            this.bits = bits;
        }

        boolean[] getBits() {
            return VectorIntrinsics.maybeRebox(this).bits;
        }

        @Override
        FloatMaxMask uOp(MUnOp f) {
            boolean[] res = new boolean[species().length()];
            boolean[] bits = getBits();
            for (int i = 0; i < species().length(); i++) {
                res[i] = f.apply(i, bits[i]);
            }
            return new FloatMaxMask(res);
        }

        @Override
        FloatMaxMask bOp(Mask<Float> o, MBinOp f) {
            boolean[] res = new boolean[species().length()];
            boolean[] bits = getBits();
            boolean[] mbits = ((FloatMaxMask)o).getBits();
            for (int i = 0; i < species().length(); i++) {
                res[i] = f.apply(i, bits[i], mbits[i]);
            }
            return new FloatMaxMask(res);
        }

        @Override
        public FloatMaxSpecies species() {
            return SPECIES;
        }

        @Override
        public FloatMaxVector toVector() {
            float[] res = new float[species().length()];
            boolean[] bits = getBits();
            for (int i = 0; i < species().length(); i++) {
                // -1 will result in the most significant bit being set in
                // addition to some or all other bits
                res[i] = (float) (bits[i] ? -1 : 0);
            }
            return new FloatMaxVector(res);
        }

        // Unary operations

        @Override
        @ForceInline
        public FloatMaxMask not() {
            return (FloatMaxMask) VectorIntrinsics.unaryOp(
                                             VECTOR_OP_NOT, FloatMaxMask.class, int.class, LENGTH,
                                             this,
                                             (m1) -> m1.uOp((i, a) -> !a));
        }

        // Binary operations

        @Override
        @ForceInline
        public FloatMaxMask and(Mask<Float> o) {
            Objects.requireNonNull(o);
            FloatMaxMask m = (FloatMaxMask)o;
            return VectorIntrinsics.binaryOp(VECTOR_OP_AND, FloatMaxMask.class, int.class, LENGTH,
                                             this, m,
                                             (m1, m2) -> m1.bOp(m2, (i, a, b) -> a & b));
        }

        @Override
        @ForceInline
        public FloatMaxMask or(Mask<Float> o) {
            Objects.requireNonNull(o);
            FloatMaxMask m = (FloatMaxMask)o;
            return VectorIntrinsics.binaryOp(VECTOR_OP_OR, FloatMaxMask.class, int.class, LENGTH,
                                             this, m,
                                             (m1, m2) -> m1.bOp(m2, (i, a, b) -> a | b));
        }

        // Reductions

        @Override
        @ForceInline
        public boolean anyTrue() {
            return VectorIntrinsics.test(COND_notZero, FloatMaxMask.class, int.class, LENGTH,
                                         this, this,
                                         (m1, m2) -> super.anyTrue());
        }

        @Override
        @ForceInline
        public boolean allTrue() {
            return VectorIntrinsics.test(COND_carrySet, FloatMaxMask.class, int.class, LENGTH,
                                         this, species().maskAllTrue(),
                                         (m1, m2) -> super.allTrue());
        }
    }

    // Shuffle

    static final class FloatMaxShuffle extends AbstractShuffle<Float> {
        FloatMaxShuffle(byte[] reorder) {
            super(reorder);
        }

        public FloatMaxShuffle(int[] reorder) {
            super(reorder);
        }

        public FloatMaxShuffle(int[] reorder, int i) {
            super(reorder, i);
        }

        public FloatMaxShuffle(IntUnaryOperator f) {
            super(f);
        }

        @Override
        public FloatMaxSpecies species() {
            return SPECIES;
        }

        @Override
        public FloatMaxVector toVector() {
            float[] va = new float[SPECIES.length()];
            for (int i = 0; i < va.length; i++) {
              va[i] = (float) getElement(i);
            }
            return species().fromArray(va, 0);
        }

        @Override
        public FloatMaxShuffle rearrange(Vector.Shuffle<Float> o) {
            FloatMaxShuffle s = (FloatMaxShuffle) o;
            byte[] r = new byte[reorder.length];
            for (int i = 0; i < reorder.length; i++) {
                r[i] = reorder[s.reorder[i]];
            }
            return new FloatMaxShuffle(r);
        }
    }

    // Species

    @Override
    public FloatMaxSpecies species() {
        return SPECIES;
    }

    static final class FloatMaxSpecies extends FloatSpecies {
        static final int BIT_SIZE = Shape.S_Max_BIT.bitSize();

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
            return Shape.S_Max_BIT;
        }

        @Override
        FloatMaxVector op(FOp f) {
            float[] res = new float[length()];
            for (int i = 0; i < length(); i++) {
                res[i] = f.apply(i);
            }
            return new FloatMaxVector(res);
        }

        @Override
        FloatMaxVector op(Mask<Float> o, FOp f) {
            float[] res = new float[length()];
            boolean[] mbits = ((FloatMaxMask)o).getBits();
            for (int i = 0; i < length(); i++) {
                if (mbits[i]) {
                    res[i] = f.apply(i);
                }
            }
            return new FloatMaxVector(res);
        }

        @Override
        FloatMaxMask opm(FOpm f) {
            boolean[] res = new boolean[length()];
            for (int i = 0; i < length(); i++) {
                res[i] = (boolean)f.apply(i);
            }
            return new FloatMaxMask(res);
        }

        // Factories

        @Override
        public FloatMaxMask maskFromValues(boolean... bits) {
            return new FloatMaxMask(bits);
        }

        @Override
        public FloatMaxShuffle shuffle(IntUnaryOperator f) {
            return new FloatMaxShuffle(f);
        }

        @Override
        public FloatMaxShuffle shuffleIota() {
            return new FloatMaxShuffle(AbstractShuffle.IDENTITY);
        }

        @Override
        public FloatMaxShuffle shuffleFromValues(int... ixs) {
            return new FloatMaxShuffle(ixs);
        }

        @Override
        public FloatMaxShuffle shuffleFromArray(int[] ixs, int i) {
            return new FloatMaxShuffle(ixs, i);
        }

        @Override
        @ForceInline
        public FloatMaxVector zero() {
            return VectorIntrinsics.broadcastCoerced(FloatMaxVector.class, float.class, LENGTH,
                                                     Float.floatToIntBits(0.0f),
                                                     (z -> ZERO));
        }

        @Override
        @ForceInline
        public FloatMaxVector broadcast(float e) {
            return VectorIntrinsics.broadcastCoerced(
                FloatMaxVector.class, float.class, LENGTH,
                Float.floatToIntBits(e),
                ((long bits) -> SPECIES.op(i -> Float.intBitsToFloat((int)bits))));
        }

        @Override
        @ForceInline
        public FloatMaxMask maskAllTrue() {
            return VectorIntrinsics.broadcastCoerced(FloatMaxMask.class, int.class, LENGTH,
                                                     (int)-1,
                                                     (z -> FloatMaxMask.TRUE_MASK));
        }

        @Override
        @ForceInline
        public FloatMaxMask maskAllFalse() {
            return VectorIntrinsics.broadcastCoerced(FloatMaxMask.class, int.class, LENGTH,
                                                     0,
                                                     (z -> FloatMaxMask.FALSE_MASK));
        }

        @Override
        @ForceInline
        public FloatMaxVector scalars(float... es) {
            Objects.requireNonNull(es);
            int ix = VectorIntrinsics.checkIndex(0, es.length, LENGTH);
            return VectorIntrinsics.load(FloatMaxVector.class, float.class, LENGTH,
                                         es, Unsafe.ARRAY_FLOAT_BASE_OFFSET,
                                         es, ix,
                                         (c, idx) -> op(n -> c[idx + n]));
        }

        @Override
        @ForceInline
        public FloatMaxMask maskFromArray(boolean[] bits, int ix) {
            Objects.requireNonNull(bits);
            ix = VectorIntrinsics.checkIndex(ix, bits.length, LENGTH);
            return VectorIntrinsics.load(FloatMaxMask.class, int.class, LENGTH,
                                         bits, (long)ix + Unsafe.ARRAY_BOOLEAN_BASE_OFFSET,
                                         bits, ix,
                                         (c, idx) -> opm(n -> c[idx + n]));
        }

        @Override
        @ForceInline
        public FloatMaxVector fromArray(float[] a, int ix) {
            Objects.requireNonNull(a);
            ix = VectorIntrinsics.checkIndex(ix, a.length, LENGTH);
            return VectorIntrinsics.load(FloatMaxVector.class, float.class, LENGTH,
                                         a, (((long) ix) << ARRAY_SHIFT) + Unsafe.ARRAY_FLOAT_BASE_OFFSET,
                                         a, ix,
                                         (c, idx) -> op(n -> c[idx + n]));
        }

        @Override
        @ForceInline
        public FloatMaxVector fromArray(float[] a, int ax, Mask<Float> m) {
            return zero().blend(fromArray(a, ax), m);
        }

        @Override
        @ForceInline
        public FloatMaxVector fromByteArray(byte[] a, int ix) {
            Objects.requireNonNull(a);
            ix = VectorIntrinsics.checkIndex(ix, a.length, bitSize() / Byte.SIZE);
            return VectorIntrinsics.load(FloatMaxVector.class, float.class, LENGTH,
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
        public FloatMaxVector fromArray(float[] a, int ix, int[] b, int iy) {
            Objects.requireNonNull(a);
            Objects.requireNonNull(b);

            // Index vector: vix[0:n] = i -> ix + indexMap[iy + i]
            IntVector vix = INDEX_SPEC.fromArray(b, iy).add(ix);

            vix = VectorIntrinsics.checkIndex(vix, a.length);

            return VectorIntrinsics.loadWithMap(FloatMaxVector.class, float.class, LENGTH, vix.getClass(),
                                        a, Unsafe.ARRAY_FLOAT_BASE_OFFSET, vix,
                                        a, ix, b, iy,
                                       (c, idx, indexMap, idy) -> op(n -> c[idx + indexMap[idy+n]]));
       }

       @Override
       @ForceInline
       public FloatMaxVector fromArray(float[] a, int ax, Mask<Float> m, int[] indexMap, int j) {
           // @@@ This can result in out of bounds errors for unset mask lanes
           return zero().blend(fromArray(a, ax, indexMap, j), m);
       }


        @Override
        @ForceInline
        public FloatMaxVector fromByteArray(byte[] a, int ix, Mask<Float> m) {
            return zero().blend(fromByteArray(a, ix), m);
        }

        @Override
        @ForceInline
        public FloatMaxVector fromByteBuffer(ByteBuffer bb, int ix) {
            if (bb.order() != ByteOrder.nativeOrder()) {
                throw new IllegalArgumentException();
            }
            ix = VectorIntrinsics.checkIndex(ix, bb.limit(), bitSize() / Byte.SIZE);
            return VectorIntrinsics.load(FloatMaxVector.class, float.class, LENGTH,
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
        public FloatMaxVector fromByteBuffer(ByteBuffer bb, int ix, Mask<Float> m) {
            return zero().blend(fromByteBuffer(bb, ix), m);
        }

        @Override
        @ForceInline
        @SuppressWarnings("unchecked")
        public <F> FloatMaxVector cast(Vector<F> o) {
            if (o.length() != LENGTH)
                throw new IllegalArgumentException("Vector length this species length differ");

            return VectorIntrinsics.cast(
                o.getClass(),
                o.elementType(), LENGTH,
                FloatMaxVector.class,
                float.class, LENGTH,
                o, this,
                (s, v) -> s.castDefault(v)
            );
        }

        @SuppressWarnings("unchecked")
        @ForceInline
        private <F> FloatMaxVector castDefault(Vector<F> v) {
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
        public <E> FloatMaxMask cast(Mask<E> m) {
            if (m.length() != LENGTH)
                throw new IllegalArgumentException("Mask length this species length differ");
            return new FloatMaxMask(m.toArray());
        }

        @Override
        @ForceInline
        public <E> FloatMaxShuffle cast(Shuffle<E> s) {
            if (s.length() != LENGTH)
                throw new IllegalArgumentException("Shuffle length this species length differ");
            return new FloatMaxShuffle(s.toArray());
        }

        @Override
        @ForceInline
        @SuppressWarnings("unchecked")
        public <F> FloatMaxVector rebracket(Vector<F> o) {
            Objects.requireNonNull(o);
            if (o.elementType() == byte.class) {
                ByteMaxVector so = (ByteMaxVector)o;
                return VectorIntrinsics.reinterpret(
                    ByteMaxVector.class,
                    byte.class, so.length(),
                    FloatMaxVector.class,
                    float.class, LENGTH,
                    so, this,
                    (s, v) -> (FloatMaxVector) s.reshape(v)
                );
            } else if (o.elementType() == short.class) {
                ShortMaxVector so = (ShortMaxVector)o;
                return VectorIntrinsics.reinterpret(
                    ShortMaxVector.class,
                    short.class, so.length(),
                    FloatMaxVector.class,
                    float.class, LENGTH,
                    so, this,
                    (s, v) -> (FloatMaxVector) s.reshape(v)
                );
            } else if (o.elementType() == int.class) {
                IntMaxVector so = (IntMaxVector)o;
                return VectorIntrinsics.reinterpret(
                    IntMaxVector.class,
                    int.class, so.length(),
                    FloatMaxVector.class,
                    float.class, LENGTH,
                    so, this,
                    (s, v) -> (FloatMaxVector) s.reshape(v)
                );
            } else if (o.elementType() == long.class) {
                LongMaxVector so = (LongMaxVector)o;
                return VectorIntrinsics.reinterpret(
                    LongMaxVector.class,
                    long.class, so.length(),
                    FloatMaxVector.class,
                    float.class, LENGTH,
                    so, this,
                    (s, v) -> (FloatMaxVector) s.reshape(v)
                );
            } else if (o.elementType() == float.class) {
                FloatMaxVector so = (FloatMaxVector)o;
                return VectorIntrinsics.reinterpret(
                    FloatMaxVector.class,
                    float.class, so.length(),
                    FloatMaxVector.class,
                    float.class, LENGTH,
                    so, this,
                    (s, v) -> (FloatMaxVector) s.reshape(v)
                );
            } else if (o.elementType() == double.class) {
                DoubleMaxVector so = (DoubleMaxVector)o;
                return VectorIntrinsics.reinterpret(
                    DoubleMaxVector.class,
                    double.class, so.length(),
                    FloatMaxVector.class,
                    float.class, LENGTH,
                    so, this,
                    (s, v) -> (FloatMaxVector) s.reshape(v)
                );
            } else {
                throw new InternalError("Unimplemented type");
            }
        }

        @Override
        @ForceInline
        @SuppressWarnings("unchecked")
        public FloatMaxVector resize(Vector<Float> o) {
            Objects.requireNonNull(o);
            if (o.bitSize() == 64 && (o instanceof Float64Vector)) {
                Float64Vector so = (Float64Vector)o;
                return VectorIntrinsics.reinterpret(
                    Float64Vector.class,
                    float.class, so.length(),
                    FloatMaxVector.class,
                    float.class, LENGTH,
                    so, this,
                    (s, v) -> (FloatMaxVector) s.reshape(v)
                );
            } else if (o.bitSize() == 128 && (o instanceof Float128Vector)) {
                Float128Vector so = (Float128Vector)o;
                return VectorIntrinsics.reinterpret(
                    Float128Vector.class,
                    float.class, so.length(),
                    FloatMaxVector.class,
                    float.class, LENGTH,
                    so, this,
                    (s, v) -> (FloatMaxVector) s.reshape(v)
                );
            } else if (o.bitSize() == 256 && (o instanceof Float256Vector)) {
                Float256Vector so = (Float256Vector)o;
                return VectorIntrinsics.reinterpret(
                    Float256Vector.class,
                    float.class, so.length(),
                    FloatMaxVector.class,
                    float.class, LENGTH,
                    so, this,
                    (s, v) -> (FloatMaxVector) s.reshape(v)
                );
            } else if (o.bitSize() == 512 && (o instanceof Float512Vector)) {
                Float512Vector so = (Float512Vector)o;
                return VectorIntrinsics.reinterpret(
                    Float512Vector.class,
                    float.class, so.length(),
                    FloatMaxVector.class,
                    float.class, LENGTH,
                    so, this,
                    (s, v) -> (FloatMaxVector) s.reshape(v)
                );
            } else if ((o.bitSize() > 0) && (o.bitSize() <= 2048)
                    && (o.bitSize() % 128 == 0) && (o instanceof FloatMaxVector)) {
                FloatMaxVector so = (FloatMaxVector)o;
                return VectorIntrinsics.reinterpret(
                    FloatMaxVector.class,
                    float.class, so.length(),
                    FloatMaxVector.class,
                    float.class, LENGTH,
                    so, this,
                    (s, v) -> (FloatMaxVector) s.reshape(v)
                );
            } else {
                throw new InternalError("Unimplemented size");
            }
        }
    }
}
