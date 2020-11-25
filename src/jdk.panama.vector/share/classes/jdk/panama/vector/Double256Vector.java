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
import java.nio.DoubleBuffer;
import java.nio.ReadOnlyBufferException;
import java.util.Arrays;
import java.util.Objects;
import java.util.function.IntUnaryOperator;

import jdk.internal.misc.Unsafe;
import jdk.internal.vm.annotation.ForceInline;
import static jdk.panama.vector.VectorIntrinsics.*;

@SuppressWarnings("cast")
final class Double256Vector extends DoubleVector {
    static final Double256Species SPECIES = new Double256Species();

    static final Double256Vector ZERO = new Double256Vector();

    static final int LENGTH = SPECIES.length();

    // Index vector species
    private static final IntVector.IntSpecies INDEX_SPEC;
    static {
        int bitSize = Vector.bitSizeForVectorLength(int.class, LENGTH);
        Vector.Shape shape = Vector.shapeForVectorBitSize(bitSize);
        INDEX_SPEC = (IntVector.IntSpecies) Vector.species(int.class, shape);
    }

    private final double[] vec; // Don't access directly, use getElements() instead.

    private double[] getElements() {
        return VectorIntrinsics.maybeRebox(this).vec;
    }

    Double256Vector() {
        vec = new double[SPECIES.length()];
    }

    Double256Vector(double[] v) {
        vec = v;
    }

    @Override
    public int length() { return LENGTH; }

    // Unary operator

    @Override
    Double256Vector uOp(FUnOp f) {
        double[] vec = getElements();
        double[] res = new double[length()];
        for (int i = 0; i < length(); i++) {
            res[i] = f.apply(i, vec[i]);
        }
        return new Double256Vector(res);
    }

    @Override
    Double256Vector uOp(Mask<Double> o, FUnOp f) {
        double[] vec = getElements();
        double[] res = new double[length()];
        boolean[] mbits = ((Double256Mask)o).getBits();
        for (int i = 0; i < length(); i++) {
            res[i] = mbits[i] ? f.apply(i, vec[i]) : vec[i];
        }
        return new Double256Vector(res);
    }

    // Binary operator

    @Override
    Double256Vector bOp(Vector<Double> o, FBinOp f) {
        double[] res = new double[length()];
        double[] vec1 = this.getElements();
        double[] vec2 = ((Double256Vector)o).getElements();
        for (int i = 0; i < length(); i++) {
            res[i] = f.apply(i, vec1[i], vec2[i]);
        }
        return new Double256Vector(res);
    }

    @Override
    Double256Vector bOp(Vector<Double> o1, Mask<Double> o2, FBinOp f) {
        double[] res = new double[length()];
        double[] vec1 = this.getElements();
        double[] vec2 = ((Double256Vector)o1).getElements();
        boolean[] mbits = ((Double256Mask)o2).getBits();
        for (int i = 0; i < length(); i++) {
            res[i] = mbits[i] ? f.apply(i, vec1[i], vec2[i]) : vec1[i];
        }
        return new Double256Vector(res);
    }

    // Trinary operator

    @Override
    Double256Vector tOp(Vector<Double> o1, Vector<Double> o2, FTriOp f) {
        double[] res = new double[length()];
        double[] vec1 = this.getElements();
        double[] vec2 = ((Double256Vector)o1).getElements();
        double[] vec3 = ((Double256Vector)o2).getElements();
        for (int i = 0; i < length(); i++) {
            res[i] = f.apply(i, vec1[i], vec2[i], vec3[i]);
        }
        return new Double256Vector(res);
    }

    @Override
    Double256Vector tOp(Vector<Double> o1, Vector<Double> o2, Mask<Double> o3, FTriOp f) {
        double[] res = new double[length()];
        double[] vec1 = getElements();
        double[] vec2 = ((Double256Vector)o1).getElements();
        double[] vec3 = ((Double256Vector)o2).getElements();
        boolean[] mbits = ((Double256Mask)o3).getBits();
        for (int i = 0; i < length(); i++) {
            res[i] = mbits[i] ? f.apply(i, vec1[i], vec2[i], vec3[i]) : vec1[i];
        }
        return new Double256Vector(res);
    }

    @Override
    double rOp(double v, FBinOp f) {
        double[] vec = getElements();
        for (int i = 0; i < length(); i++) {
            v = f.apply(i, v, vec[i]);
        }
        return v;
    }

    // Binary operations with scalars

    @Override
    @ForceInline
    public DoubleVector add(double o) {
        return add(SPECIES.broadcast(o));
    }

    @Override
    @ForceInline
    public DoubleVector add(double o, Mask<Double> m) {
        return add(SPECIES.broadcast(o), m);
    }

    @Override
    @ForceInline
    public DoubleVector sub(double o) {
        return sub(SPECIES.broadcast(o));
    }

    @Override
    @ForceInline
    public DoubleVector sub(double o, Mask<Double> m) {
        return sub(SPECIES.broadcast(o), m);
    }

    @Override
    @ForceInline
    public DoubleVector mul(double o) {
        return mul(SPECIES.broadcast(o));
    }

    @Override
    @ForceInline
    public DoubleVector mul(double o, Mask<Double> m) {
        return mul(SPECIES.broadcast(o), m);
    }

    @Override
    @ForceInline
    public DoubleVector min(double o) {
        return min(SPECIES.broadcast(o));
    }

    @Override
    @ForceInline
    public DoubleVector max(double o) {
        return max(SPECIES.broadcast(o));
    }

    @Override
    @ForceInline
    public Mask<Double> equal(double o) {
        return equal(SPECIES.broadcast(o));
    }

    @Override
    @ForceInline
    public Mask<Double> notEqual(double o) {
        return notEqual(SPECIES.broadcast(o));
    }

    @Override
    @ForceInline
    public Mask<Double> lessThan(double o) {
        return lessThan(SPECIES.broadcast(o));
    }

    @Override
    @ForceInline
    public Mask<Double> lessThanEq(double o) {
        return lessThanEq(SPECIES.broadcast(o));
    }

    @Override
    @ForceInline
    public Mask<Double> greaterThan(double o) {
        return greaterThan(SPECIES.broadcast(o));
    }

    @Override
    @ForceInline
    public Mask<Double> greaterThanEq(double o) {
        return greaterThanEq(SPECIES.broadcast(o));
    }

    @Override
    @ForceInline
    public DoubleVector blend(double o, Mask<Double> m) {
        return blend(SPECIES.broadcast(o), m);
    }

    @Override
    @ForceInline
    public DoubleVector div(double o) {
        return div(SPECIES.broadcast(o));
    }

    @Override
    @ForceInline
    public DoubleVector div(double o, Mask<Double> m) {
        return div(SPECIES.broadcast(o), m);
    }

    @Override
    @ForceInline
    public Double256Vector div(Vector<Double> v, Mask<Double> m) {
        return blend(div(v), m);
    }

    @Override
    @ForceInline
    public DoubleVector atan2(double o) {
        return atan2(SPECIES.broadcast(o));
    }

    @Override
    @ForceInline
    public DoubleVector atan2(double o, Mask<Double> m) {
        return atan2(SPECIES.broadcast(o), m);
    }

    @Override
    @ForceInline
    public DoubleVector pow(double o) {
        return pow(SPECIES.broadcast(o));
    }

    @Override
    @ForceInline
    public DoubleVector pow(double o, Mask<Double> m) {
        return pow(SPECIES.broadcast(o), m);
    }

    @Override
    @ForceInline
    public DoubleVector fma(double o1, double o2) {
        return fma(SPECIES.broadcast(o1), SPECIES.broadcast(o2));
    }

    @Override
    @ForceInline
    public DoubleVector fma(double o1, double o2, Mask<Double> m) {
        return fma(SPECIES.broadcast(o1), SPECIES.broadcast(o2), m);
    }

    @Override
    @ForceInline
    public DoubleVector hypot(double o) {
        return hypot(SPECIES.broadcast(o));
    }

    @Override
    @ForceInline
    public DoubleVector hypot(double o, Mask<Double> m) {
        return hypot(SPECIES.broadcast(o), m);
    }


    // Unary operations

    @ForceInline
    @Override
    public Double256Vector neg(Mask<Double> m) {
        return blend(neg(), m);
    }

    @Override
    @ForceInline
    public Double256Vector abs() {
        return VectorIntrinsics.unaryOp(
            VECTOR_OP_ABS, Double256Vector.class, double.class, LENGTH,
            this,
            v1 -> v1.uOp((i, a) -> (double) Math.abs(a)));
    }

    @ForceInline
    @Override
    public Double256Vector abs(Mask<Double> m) {
        return blend(abs(), m);
    }

    @Override
    @ForceInline
    public Double256Vector neg() {
        return VectorIntrinsics.unaryOp(
            VECTOR_OP_NEG, Double256Vector.class, double.class, LENGTH,
            this,
            v1 -> v1.uOp((i, a) -> (double) -a));
    }

    @Override
    @ForceInline
    public Double256Vector div(Vector<Double> o) {
        Objects.requireNonNull(o);
        Double256Vector v = (Double256Vector)o;
        return VectorIntrinsics.binaryOp(
            VECTOR_OP_DIV, Double256Vector.class, double.class, LENGTH,
            this, v,
            (v1, v2) -> v1.bOp(v2, (i, a, b) -> (double)(a / b)));
    }

    @Override
    @ForceInline
    public Double256Vector sqrt() {
        return VectorIntrinsics.unaryOp(
            VECTOR_OP_SQRT, Double256Vector.class, double.class, LENGTH,
            this,
            v1 -> v1.uOp((i, a) -> (double) Math.sqrt((double) a)));
    }

    @Override
    @ForceInline
    public Double256Vector exp() {
        return (Double256Vector) VectorIntrinsics.unaryOp(
            VECTOR_OP_EXP, Double256Vector.class, double.class, LENGTH,
            this,
            v1 -> ((Double256Vector)v1).uOp((i, a) -> (double) Math.exp((double) a)));
    }

    @Override
    @ForceInline
    public Double256Vector log1p() {
        return (Double256Vector) VectorIntrinsics.unaryOp(
            VECTOR_OP_LOG1P, Double256Vector.class, double.class, LENGTH,
            this,
            v1 -> ((Double256Vector)v1).uOp((i, a) -> (double) Math.log1p((double) a)));
    }

    @Override
    @ForceInline
    public Double256Vector log() {
        return (Double256Vector) VectorIntrinsics.unaryOp(
            VECTOR_OP_LOG, Double256Vector.class, double.class, LENGTH,
            this,
            v1 -> ((Double256Vector)v1).uOp((i, a) -> (double) Math.log((double) a)));
    }

    @Override
    @ForceInline
    public Double256Vector log10() {
        return (Double256Vector) VectorIntrinsics.unaryOp(
            VECTOR_OP_LOG10, Double256Vector.class, double.class, LENGTH,
            this,
            v1 -> ((Double256Vector)v1).uOp((i, a) -> (double) Math.log10((double) a)));
    }

    @Override
    @ForceInline
    public Double256Vector expm1() {
        return (Double256Vector) VectorIntrinsics.unaryOp(
            VECTOR_OP_EXPM1, Double256Vector.class, double.class, LENGTH,
            this,
            v1 -> ((Double256Vector)v1).uOp((i, a) -> (double) Math.expm1((double) a)));
    }

    @Override
    @ForceInline
    public Double256Vector cbrt() {
        return (Double256Vector) VectorIntrinsics.unaryOp(
            VECTOR_OP_CBRT, Double256Vector.class, double.class, LENGTH,
            this,
            v1 -> ((Double256Vector)v1).uOp((i, a) -> (double) Math.cbrt((double) a)));
    }

    @Override
    @ForceInline
    public Double256Vector sin() {
        return (Double256Vector) VectorIntrinsics.unaryOp(
            VECTOR_OP_SIN, Double256Vector.class, double.class, LENGTH,
            this,
            v1 -> ((Double256Vector)v1).uOp((i, a) -> (double) Math.sin((double) a)));
    }

    @Override
    @ForceInline
    public Double256Vector cos() {
        return (Double256Vector) VectorIntrinsics.unaryOp(
            VECTOR_OP_COS, Double256Vector.class, double.class, LENGTH,
            this,
            v1 -> ((Double256Vector)v1).uOp((i, a) -> (double) Math.cos((double) a)));
    }

    @Override
    @ForceInline
    public Double256Vector tan() {
        return (Double256Vector) VectorIntrinsics.unaryOp(
            VECTOR_OP_TAN, Double256Vector.class, double.class, LENGTH,
            this,
            v1 -> ((Double256Vector)v1).uOp((i, a) -> (double) Math.tan((double) a)));
    }

    @Override
    @ForceInline
    public Double256Vector asin() {
        return (Double256Vector) VectorIntrinsics.unaryOp(
            VECTOR_OP_ASIN, Double256Vector.class, double.class, LENGTH,
            this,
            v1 -> ((Double256Vector)v1).uOp((i, a) -> (double) Math.asin((double) a)));
    }

    @Override
    @ForceInline
    public Double256Vector acos() {
        return (Double256Vector) VectorIntrinsics.unaryOp(
            VECTOR_OP_ACOS, Double256Vector.class, double.class, LENGTH,
            this,
            v1 -> ((Double256Vector)v1).uOp((i, a) -> (double) Math.acos((double) a)));
    }

    @Override
    @ForceInline
    public Double256Vector atan() {
        return (Double256Vector) VectorIntrinsics.unaryOp(
            VECTOR_OP_ATAN, Double256Vector.class, double.class, LENGTH,
            this,
            v1 -> ((Double256Vector)v1).uOp((i, a) -> (double) Math.atan((double) a)));
    }

    @Override
    @ForceInline
    public Double256Vector sinh() {
        return (Double256Vector) VectorIntrinsics.unaryOp(
            VECTOR_OP_SINH, Double256Vector.class, double.class, LENGTH,
            this,
            v1 -> ((Double256Vector)v1).uOp((i, a) -> (double) Math.sinh((double) a)));
    }

    @Override
    @ForceInline
    public Double256Vector cosh() {
        return (Double256Vector) VectorIntrinsics.unaryOp(
            VECTOR_OP_COSH, Double256Vector.class, double.class, LENGTH,
            this,
            v1 -> ((Double256Vector)v1).uOp((i, a) -> (double) Math.cosh((double) a)));
    }

    @Override
    @ForceInline
    public Double256Vector tanh() {
        return (Double256Vector) VectorIntrinsics.unaryOp(
            VECTOR_OP_TANH, Double256Vector.class, double.class, LENGTH,
            this,
            v1 -> ((Double256Vector)v1).uOp((i, a) -> (double) Math.tanh((double) a)));
    }

    @Override
    @ForceInline
    public Double256Vector pow(Vector<Double> o) {
        Objects.requireNonNull(o);
        Double256Vector v = (Double256Vector)o;
        return (Double256Vector) VectorIntrinsics.binaryOp(
            VECTOR_OP_POW, Double256Vector.class, double.class, LENGTH,
            this, v,
            (v1, v2) -> ((Double256Vector)v1).bOp(v2, (i, a, b) -> (double)(Math.pow(a,b))));
    }

    @Override
    @ForceInline
    public Double256Vector hypot(Vector<Double> o) {
        Objects.requireNonNull(o);
        Double256Vector v = (Double256Vector)o;
        return (Double256Vector) VectorIntrinsics.binaryOp(
            VECTOR_OP_HYPOT, Double256Vector.class, double.class, LENGTH,
            this, v,
            (v1, v2) -> ((Double256Vector)v1).bOp(v2, (i, a, b) -> (double)(Math.hypot(a,b))));
    }

    @Override
    @ForceInline
    public Double256Vector atan2(Vector<Double> o) {
        Objects.requireNonNull(o);
        Double256Vector v = (Double256Vector)o;
        return (Double256Vector) VectorIntrinsics.binaryOp(
            VECTOR_OP_ATAN2, Double256Vector.class, double.class, LENGTH,
            this, v,
            (v1, v2) -> ((Double256Vector)v1).bOp(v2, (i, a, b) -> (double)(Math.atan2(a,b))));
    }


    // Binary operations

    @Override
    @ForceInline
    public Double256Vector add(Vector<Double> o) {
        Objects.requireNonNull(o);
        Double256Vector v = (Double256Vector)o;
        return VectorIntrinsics.binaryOp(
            VECTOR_OP_ADD, Double256Vector.class, double.class, LENGTH,
            this, v,
            (v1, v2) -> v1.bOp(v2, (i, a, b) -> (double)(a + b)));
    }

    @Override
    @ForceInline
    public Double256Vector add(Vector<Double> v, Mask<Double> m) {
        return blend(add(v), m);
    }

    @Override
    @ForceInline
    public Double256Vector sub(Vector<Double> o) {
        Objects.requireNonNull(o);
        Double256Vector v = (Double256Vector)o;
        return VectorIntrinsics.binaryOp(
            VECTOR_OP_SUB, Double256Vector.class, double.class, LENGTH,
            this, v,
            (v1, v2) -> v1.bOp(v2, (i, a, b) -> (double)(a - b)));
    }

    @Override
    @ForceInline
    public Double256Vector sub(Vector<Double> v, Mask<Double> m) {
        return blend(sub(v), m);
    }

    @Override
    @ForceInline
    public Double256Vector mul(Vector<Double> o) {
        Objects.requireNonNull(o);
        Double256Vector v = (Double256Vector)o;
        return VectorIntrinsics.binaryOp(
            VECTOR_OP_MUL, Double256Vector.class, double.class, LENGTH,
            this, v,
            (v1, v2) -> v1.bOp(v2, (i, a, b) -> (double)(a * b)));
    }

    @Override
    @ForceInline
    public Double256Vector mul(Vector<Double> v, Mask<Double> m) {
        return blend(mul(v), m);
    }

    @Override
    @ForceInline
    public Double256Vector min(Vector<Double> o) {
        Objects.requireNonNull(o);
        Double256Vector v = (Double256Vector)o;
        return (Double256Vector) VectorIntrinsics.binaryOp(
            VECTOR_OP_MIN, Double256Vector.class, double.class, LENGTH,
            this, v,
            (v1, v2) -> ((Double256Vector)v1).bOp(v2, (i, a, b) -> (double) ((a < b) ? a : b)));
    }

    @Override
    @ForceInline
    public Double256Vector min(Vector<Double> v, Mask<Double> m) {
        return blend(min(v), m);
    }

    @Override
    @ForceInline
    public Double256Vector max(Vector<Double> o) {
        Objects.requireNonNull(o);
        Double256Vector v = (Double256Vector)o;
        return VectorIntrinsics.binaryOp(
            VECTOR_OP_MAX, Double256Vector.class, double.class, LENGTH,
            this, v,
            (v1, v2) -> v1.bOp(v2, (i, a, b) -> (double) ((a > b) ? a : b)));
        }

    @Override
    @ForceInline
    public Double256Vector max(Vector<Double> v, Mask<Double> m) {
        return blend(max(v), m);
    }


    // Ternary operations

    @Override
    @ForceInline
    public Double256Vector fma(Vector<Double> o1, Vector<Double> o2) {
        Objects.requireNonNull(o1);
        Objects.requireNonNull(o2);
        Double256Vector v1 = (Double256Vector)o1;
        Double256Vector v2 = (Double256Vector)o2;
        return VectorIntrinsics.ternaryOp(
            VECTOR_OP_FMA, Double256Vector.class, double.class, LENGTH,
            this, v1, v2,
            (w1, w2, w3) -> w1.tOp(w2, w3, (i, a, b, c) -> Math.fma(a, b, c)));
    }

    // Type specific horizontal reductions

    @Override
    @ForceInline
    public double addAll() {
        long bits = (long) VectorIntrinsics.reductionCoerced(
                                VECTOR_OP_ADD, Double256Vector.class, double.class, LENGTH,
                                this,
                                v -> {
                                    double r = v.rOp((double) 0, (i, a, b) -> (double) (a + b));
                                    return (long)Double.doubleToLongBits(r);
                                });
        return Double.longBitsToDouble(bits);
    }

    @Override
    @ForceInline
    public double subAll() {
        long bits = (long) VectorIntrinsics.reductionCoerced(
                                VECTOR_OP_SUB, Double256Vector.class, double.class, LENGTH,
                                this,
                                v -> {
                                    double r = v.rOp((double) 0, (i, a, b) -> (double) (a - b));
                                    return (long)Double.doubleToLongBits(r);
                                });
        return Double.longBitsToDouble(bits);
    }

    @Override
    @ForceInline
    public double mulAll() {
        long bits = (long) VectorIntrinsics.reductionCoerced(
                                VECTOR_OP_MUL, Double256Vector.class, double.class, LENGTH,
                                this,
                                v -> {
                                    double r = v.rOp((double) 1, (i, a, b) -> (double) (a * b));
                                    return (long)Double.doubleToLongBits(r);
                                });
        return Double.longBitsToDouble(bits);
    }

    @Override
    @ForceInline
    public double minAll() {
        long bits = (long) VectorIntrinsics.reductionCoerced(
                                VECTOR_OP_MIN, Double256Vector.class, double.class, LENGTH,
                                this,
                                v -> {
                                    double r = v.rOp(Double.MAX_VALUE , (i, a, b) -> (double) ((a < b) ? a : b));
                                    return (long)Double.doubleToLongBits(r);
                                });
        return Double.longBitsToDouble(bits);
    }

    @Override
    @ForceInline
    public double maxAll() {
        long bits = (long) VectorIntrinsics.reductionCoerced(
                                VECTOR_OP_MAX, Double256Vector.class, double.class, LENGTH,
                                this,
                                v -> {
                                    double r = v.rOp(Double.MIN_VALUE , (i, a, b) -> (double) ((a > b) ? a : b));
                                    return (long)Double.doubleToLongBits(r);
                                });
        return Double.longBitsToDouble(bits);
    }


    @Override
    @ForceInline
    public double addAll(Mask<Double> m) {
        return blend(SPECIES.broadcast((double) 0), m).addAll();
    }

    @Override
    @ForceInline
    public double subAll(Mask<Double> m) {
        return blend(SPECIES.broadcast((double) 0), m).subAll();
    }

    @Override
    @ForceInline
    public double mulAll(Mask<Double> m) {
        return blend(SPECIES.broadcast((double) 1), m).mulAll();
    }

    @Override
    @ForceInline
    public double minAll(Mask<Double> m) {
        return blend(SPECIES.broadcast(Double.MAX_VALUE), m).minAll();
    }

    @Override
    @ForceInline
    public double maxAll(Mask<Double> m) {
        return blend(SPECIES.broadcast(Double.MIN_VALUE), m).maxAll();
    }

    @Override
    @ForceInline
    public Shuffle<Double> toShuffle() {
        double[] a = toArray();
        int[] sa = new int[a.length];
        for (int i = 0; i < a.length; i++) {
            sa[i] = (int) a[i];
        }
        return SPECIES.shuffleFromArray(sa, 0);
    }

    // Memory operations

    private static final int ARRAY_SHIFT = 31 - Integer.numberOfLeadingZeros(Unsafe.ARRAY_DOUBLE_INDEX_SCALE);

    @Override
    @ForceInline
    public void intoArray(double[] a, int ix) {
        Objects.requireNonNull(a);
        ix = VectorIntrinsics.checkIndex(ix, a.length, LENGTH);
        VectorIntrinsics.store(Double256Vector.class, double.class, LENGTH,
                               a, (((long) ix) << ARRAY_SHIFT) + Unsafe.ARRAY_DOUBLE_BASE_OFFSET,
                               this,
                               a, ix,
                               (arr, idx, v) -> v.forEach((i, e) -> arr[idx + i] = e));
    }

    @Override
    @ForceInline
    public final void intoArray(double[] a, int ax, Mask<Double> m) {
        Double256Vector oldVal = SPECIES.fromArray(a, ax);
        Double256Vector newVal = oldVal.blend(this, m);
        newVal.intoArray(a, ax);
    }
    @Override
    @ForceInline
    public void intoArray(double[] a, int ix, int[] b, int iy) {
        Objects.requireNonNull(a);
        Objects.requireNonNull(b);

        // Index vector: vix[0:n] = i -> ix + indexMap[iy + i]
        IntVector vix = INDEX_SPEC.fromArray(b, iy).add(ix);

        vix = VectorIntrinsics.checkIndex(vix, a.length);

        VectorIntrinsics.storeWithMap(Double256Vector.class, double.class, LENGTH, Int128Vector.class,
                               a, Unsafe.ARRAY_DOUBLE_BASE_OFFSET, vix,
                               this,
                               a, ix, b, iy,
                               (arr, idx, v, indexMap, idy) -> v.forEach((i, e) -> arr[idx+indexMap[idy+i]] = e));
    }

     @Override
     @ForceInline
     public final void intoArray(double[] a, int ax, Mask<Double> m, int[] b, int iy) {
         // @@@ This can result in out of bounds errors for unset mask lanes
         Double256Vector oldVal = SPECIES.fromArray(a, ax, b, iy);
         Double256Vector newVal = oldVal.blend(this, m);
         newVal.intoArray(a, ax, b, iy);
     }

    @Override
    @ForceInline
    public void intoByteArray(byte[] a, int ix) {
        Objects.requireNonNull(a);
        ix = VectorIntrinsics.checkIndex(ix, a.length, bitSize() / Byte.SIZE);
        VectorIntrinsics.store(Double256Vector.class, double.class, LENGTH,
                               a, ((long) ix) + Unsafe.ARRAY_BYTE_BASE_OFFSET,
                               this,
                               a, ix,
                               (c, idx, v) -> {
                                   ByteBuffer bbc = ByteBuffer.wrap(c, idx, c.length - idx).order(ByteOrder.nativeOrder());
                                   DoubleBuffer tb = bbc.asDoubleBuffer();
                                   v.forEach((i, e) -> tb.put(e));
                               });
    }

    @Override
    @ForceInline
    public final void intoByteArray(byte[] a, int ix, Mask<Double> m) {
        Double256Vector oldVal = SPECIES.fromByteArray(a, ix);
        Double256Vector newVal = oldVal.blend(this, m);
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
        VectorIntrinsics.store(Double256Vector.class, double.class, LENGTH,
                               U.getReference(bb, BYTE_BUFFER_HB), ix + U.getLong(bb, BUFFER_ADDRESS),
                               this,
                               bb, ix,
                               (c, idx, v) -> {
                                   ByteBuffer bbc = c.duplicate().position(idx).order(ByteOrder.nativeOrder());
                                   DoubleBuffer tb = bbc.asDoubleBuffer();
                                   v.forEach((i, e) -> tb.put(e));
                               });
    }

    @Override
    @ForceInline
    public void intoByteBuffer(ByteBuffer bb, int ix, Mask<Double> m) {
        Double256Vector oldVal = SPECIES.fromByteBuffer(bb, ix);
        Double256Vector newVal = oldVal.blend(this, m);
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

        Double256Vector that = (Double256Vector) o;
        return this.equal(that).allTrue();
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(vec);
    }

    // Binary test

    @Override
    Double256Mask bTest(Vector<Double> o, FBinTest f) {
        double[] vec1 = getElements();
        double[] vec2 = ((Double256Vector)o).getElements();
        boolean[] bits = new boolean[length()];
        for (int i = 0; i < length(); i++){
            bits[i] = f.apply(i, vec1[i], vec2[i]);
        }
        return new Double256Mask(bits);
    }

    // Comparisons

    @Override
    @ForceInline
    public Double256Mask equal(Vector<Double> o) {
        Objects.requireNonNull(o);
        Double256Vector v = (Double256Vector)o;

        return VectorIntrinsics.compare(
            BT_eq, Double256Vector.class, Double256Mask.class, double.class, LENGTH,
            this, v,
            (v1, v2) -> v1.bTest(v2, (i, a, b) -> a == b));
    }

    @Override
    @ForceInline
    public Double256Mask notEqual(Vector<Double> o) {
        Objects.requireNonNull(o);
        Double256Vector v = (Double256Vector)o;

        return VectorIntrinsics.compare(
            BT_ne, Double256Vector.class, Double256Mask.class, double.class, LENGTH,
            this, v,
            (v1, v2) -> v1.bTest(v2, (i, a, b) -> a != b));
    }

    @Override
    @ForceInline
    public Double256Mask lessThan(Vector<Double> o) {
        Objects.requireNonNull(o);
        Double256Vector v = (Double256Vector)o;

        return VectorIntrinsics.compare(
            BT_lt, Double256Vector.class, Double256Mask.class, double.class, LENGTH,
            this, v,
            (v1, v2) -> v1.bTest(v2, (i, a, b) -> a < b));
    }

    @Override
    @ForceInline
    public Double256Mask lessThanEq(Vector<Double> o) {
        Objects.requireNonNull(o);
        Double256Vector v = (Double256Vector)o;

        return VectorIntrinsics.compare(
            BT_le, Double256Vector.class, Double256Mask.class, double.class, LENGTH,
            this, v,
            (v1, v2) -> v1.bTest(v2, (i, a, b) -> a <= b));
    }

    @Override
    @ForceInline
    public Double256Mask greaterThan(Vector<Double> o) {
        Objects.requireNonNull(o);
        Double256Vector v = (Double256Vector)o;

        return (Double256Mask) VectorIntrinsics.compare(
            BT_gt, Double256Vector.class, Double256Mask.class, double.class, LENGTH,
            this, v,
            (v1, v2) -> v1.bTest(v2, (i, a, b) -> a > b));
    }

    @Override
    @ForceInline
    public Double256Mask greaterThanEq(Vector<Double> o) {
        Objects.requireNonNull(o);
        Double256Vector v = (Double256Vector)o;

        return VectorIntrinsics.compare(
            BT_ge, Double256Vector.class, Double256Mask.class, double.class, LENGTH,
            this, v,
            (v1, v2) -> v1.bTest(v2, (i, a, b) -> a >= b));
    }

    // Foreach

    @Override
    void forEach(FUnCon f) {
        double[] vec = getElements();
        for (int i = 0; i < length(); i++) {
            f.apply(i, vec[i]);
        }
    }

    @Override
    void forEach(Mask<Double> o, FUnCon f) {
        boolean[] mbits = ((Double256Mask)o).getBits();
        forEach((i, a) -> {
            if (mbits[i]) { f.apply(i, a); }
        });
    }

    Long256Vector toBits() {
        double[] vec = getElements();
        long[] res = new long[this.species().length()];
        for(int i = 0; i < this.species().length(); i++){
            res[i] = Double.doubleToLongBits(vec[i]);
        }
        return new Long256Vector(res);
    }


    @Override
    public Double256Vector rotateEL(int j) {
        double[] vec = getElements();
        double[] res = new double[length()];
        for (int i = 0; i < length(); i++){
            res[(j + i) % length()] = vec[i];
        }
        return new Double256Vector(res);
    }

    @Override
    public Double256Vector rotateER(int j) {
        double[] vec = getElements();
        double[] res = new double[length()];
        for (int i = 0; i < length(); i++){
            int z = i - j;
            if(j < 0) {
                res[length() + z] = vec[i];
            } else {
                res[z] = vec[i];
            }
        }
        return new Double256Vector(res);
    }

    @Override
    public Double256Vector shiftEL(int j) {
        double[] vec = getElements();
        double[] res = new double[length()];
        for (int i = 0; i < length() - j; i++) {
            res[i] = vec[i + j];
        }
        return new Double256Vector(res);
    }

    @Override
    public Double256Vector shiftER(int j) {
        double[] vec = getElements();
        double[] res = new double[length()];
        for (int i = 0; i < length() - j; i++){
            res[i + j] = vec[i];
        }
        return new Double256Vector(res);
    }

    @Override
    @ForceInline
    public Double256Vector rearrange(Vector<Double> v,
                                  Shuffle<Double> s, Mask<Double> m) {
        return this.rearrange(s).blend(v.rearrange(s), m);
    }

    @Override
    @ForceInline
    public Double256Vector rearrange(Shuffle<Double> o1) {
    Objects.requireNonNull(o1);
    Double256Shuffle s =  (Double256Shuffle)o1;

        return VectorIntrinsics.rearrangeOp(
            Double256Vector.class, Double256Shuffle.class, double.class, LENGTH,
            this, s,
            (v1, s_) -> v1.uOp((i, a) -> {
            double[] vec = this.getElements();
            int ei = s_.getElement(i);
            return vec[ei];
        }));
    }

    @Override
    @ForceInline
    public Double256Vector blend(Vector<Double> o1, Mask<Double> o2) {
        Objects.requireNonNull(o1);
        Objects.requireNonNull(o2);
        Double256Vector v = (Double256Vector)o1;
        Double256Mask   m = (Double256Mask)o2;

        return VectorIntrinsics.blend(
            Double256Vector.class, Double256Mask.class, double.class, LENGTH,
            this, v, m,
            (v1, v2, m_) -> v1.bOp(v2, (i, a, b) -> m_.getElement(i) ? b : a));
    }

    // Accessors

    @Override
    public double get(int i) {
        if (i < 0 || i >= LENGTH) {
            throw new IllegalArgumentException("Index " + i + " must be zero or positive, and less than " + LENGTH);
        }
        long bits = (long) VectorIntrinsics.extract(
                                Double256Vector.class, double.class, LENGTH,
                                this, i,
                                (vec, ix) -> {
                                    double[] vecarr = vec.getElements();
                                    return (long)Double.doubleToLongBits(vecarr[ix]);
                                });
        return Double.longBitsToDouble(bits);
    }

    @Override
    public Double256Vector with(int i, double e) {
        if (i < 0 || i >= LENGTH) {
            throw new IllegalArgumentException("Index " + i + " must be zero or positive, and less than " + LENGTH);
        }
        return VectorIntrinsics.insert(
                                Double256Vector.class, double.class, LENGTH,
                                this, i, (long)Double.doubleToLongBits(e),
                                (v, ix, bits) -> {
                                    double[] res = v.getElements().clone();
                                    res[ix] = Double.longBitsToDouble((long)bits);
                                    return new Double256Vector(res);
                                });
    }

    // Mask

    static final class Double256Mask extends AbstractMask<Double> {
        static final Double256Mask TRUE_MASK = new Double256Mask(true);
        static final Double256Mask FALSE_MASK = new Double256Mask(false);

        private final boolean[] bits; // Don't access directly, use getBits() instead.

        public Double256Mask(boolean[] bits) {
            this(bits, 0);
        }

        public Double256Mask(boolean[] bits, int offset) {
            boolean[] a = new boolean[species().length()];
            for (int i = 0; i < a.length; i++) {
                a[i] = bits[offset + i];
            }
            this.bits = a;
        }

        public Double256Mask(boolean val) {
            boolean[] bits = new boolean[species().length()];
            Arrays.fill(bits, val);
            this.bits = bits;
        }

        boolean[] getBits() {
            return VectorIntrinsics.maybeRebox(this).bits;
        }

        @Override
        Double256Mask uOp(MUnOp f) {
            boolean[] res = new boolean[species().length()];
            boolean[] bits = getBits();
            for (int i = 0; i < species().length(); i++) {
                res[i] = f.apply(i, bits[i]);
            }
            return new Double256Mask(res);
        }

        @Override
        Double256Mask bOp(Mask<Double> o, MBinOp f) {
            boolean[] res = new boolean[species().length()];
            boolean[] bits = getBits();
            boolean[] mbits = ((Double256Mask)o).getBits();
            for (int i = 0; i < species().length(); i++) {
                res[i] = f.apply(i, bits[i], mbits[i]);
            }
            return new Double256Mask(res);
        }

        @Override
        public Double256Species species() {
            return SPECIES;
        }

        @Override
        public Double256Vector toVector() {
            double[] res = new double[species().length()];
            boolean[] bits = getBits();
            for (int i = 0; i < species().length(); i++) {
                // -1 will result in the most significant bit being set in
                // addition to some or all other bits
                res[i] = (double) (bits[i] ? -1 : 0);
            }
            return new Double256Vector(res);
        }

        // Unary operations

        @Override
        @ForceInline
        public Double256Mask not() {
            return (Double256Mask) VectorIntrinsics.unaryOp(
                                             VECTOR_OP_NOT, Double256Mask.class, long.class, LENGTH,
                                             this,
                                             (m1) -> m1.uOp((i, a) -> !a));
        }

        // Binary operations

        @Override
        @ForceInline
        public Double256Mask and(Mask<Double> o) {
            Objects.requireNonNull(o);
            Double256Mask m = (Double256Mask)o;
            return VectorIntrinsics.binaryOp(VECTOR_OP_AND, Double256Mask.class, long.class, LENGTH,
                                             this, m,
                                             (m1, m2) -> m1.bOp(m2, (i, a, b) -> a & b));
        }

        @Override
        @ForceInline
        public Double256Mask or(Mask<Double> o) {
            Objects.requireNonNull(o);
            Double256Mask m = (Double256Mask)o;
            return VectorIntrinsics.binaryOp(VECTOR_OP_OR, Double256Mask.class, long.class, LENGTH,
                                             this, m,
                                             (m1, m2) -> m1.bOp(m2, (i, a, b) -> a | b));
        }

        // Reductions

        @Override
        @ForceInline
        public boolean anyTrue() {
            return VectorIntrinsics.test(COND_notZero, Double256Mask.class, long.class, LENGTH,
                                         this, this,
                                         (m1, m2) -> super.anyTrue());
        }

        @Override
        @ForceInline
        public boolean allTrue() {
            return VectorIntrinsics.test(COND_carrySet, Double256Mask.class, long.class, LENGTH,
                                         this, species().maskAllTrue(),
                                         (m1, m2) -> super.allTrue());
        }
    }

    // Shuffle

    static final class Double256Shuffle extends AbstractShuffle<Double> {
        Double256Shuffle(byte[] reorder) {
            super(reorder);
        }

        public Double256Shuffle(int[] reorder) {
            super(reorder);
        }

        public Double256Shuffle(int[] reorder, int i) {
            super(reorder, i);
        }

        public Double256Shuffle(IntUnaryOperator f) {
            super(f);
        }

        @Override
        public Double256Species species() {
            return SPECIES;
        }

        @Override
        public Double256Vector toVector() {
            double[] va = new double[SPECIES.length()];
            for (int i = 0; i < va.length; i++) {
              va[i] = (double) getElement(i);
            }
            return species().fromArray(va, 0);
        }

        @Override
        public Double256Shuffle rearrange(Vector.Shuffle<Double> o) {
            Double256Shuffle s = (Double256Shuffle) o;
            byte[] r = new byte[reorder.length];
            for (int i = 0; i < reorder.length; i++) {
                r[i] = reorder[s.reorder[i]];
            }
            return new Double256Shuffle(r);
        }
    }

    // Species

    @Override
    public Double256Species species() {
        return SPECIES;
    }

    static final class Double256Species extends DoubleSpecies {
        static final int BIT_SIZE = Shape.S_256_BIT.bitSize();

        static final int LENGTH = BIT_SIZE / Double.SIZE;

        @Override
        public String toString() {
           StringBuilder sb = new StringBuilder("Shape[");
           sb.append(bitSize()).append(" bits, ");
           sb.append(length()).append(" ").append(double.class.getSimpleName()).append("s x ");
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
        public Class<Double> elementType() {
            return double.class;
        }

        @Override
        @ForceInline
        public int elementSize() {
            return Double.SIZE;
        }

        @Override
        @ForceInline
        public Shape shape() {
            return Shape.S_256_BIT;
        }

        @Override
        Double256Vector op(FOp f) {
            double[] res = new double[length()];
            for (int i = 0; i < length(); i++) {
                res[i] = f.apply(i);
            }
            return new Double256Vector(res);
        }

        @Override
        Double256Vector op(Mask<Double> o, FOp f) {
            double[] res = new double[length()];
            boolean[] mbits = ((Double256Mask)o).getBits();
            for (int i = 0; i < length(); i++) {
                if (mbits[i]) {
                    res[i] = f.apply(i);
                }
            }
            return new Double256Vector(res);
        }

        @Override
        Double256Mask opm(FOpm f) {
            boolean[] res = new boolean[length()];
            for (int i = 0; i < length(); i++) {
                res[i] = (boolean)f.apply(i);
            }
            return new Double256Mask(res);
        }

        // Factories

        @Override
        public Double256Mask maskFromValues(boolean... bits) {
            return new Double256Mask(bits);
        }

        @Override
        public Double256Shuffle shuffle(IntUnaryOperator f) {
            return new Double256Shuffle(f);
        }

        @Override
        public Double256Shuffle shuffleIota() {
            return new Double256Shuffle(AbstractShuffle.IDENTITY);
        }

        @Override
        public Double256Shuffle shuffleFromValues(int... ixs) {
            return new Double256Shuffle(ixs);
        }

        @Override
        public Double256Shuffle shuffleFromArray(int[] ixs, int i) {
            return new Double256Shuffle(ixs, i);
        }

        @Override
        @ForceInline
        public Double256Vector zero() {
            return VectorIntrinsics.broadcastCoerced(Double256Vector.class, double.class, LENGTH,
                                                     Double.doubleToLongBits(0.0f),
                                                     (z -> ZERO));
        }

        @Override
        @ForceInline
        public Double256Vector broadcast(double e) {
            return VectorIntrinsics.broadcastCoerced(
                Double256Vector.class, double.class, LENGTH,
                Double.doubleToLongBits(e),
                ((long bits) -> SPECIES.op(i -> Double.longBitsToDouble((long)bits))));
        }

        @Override
        @ForceInline
        public Double256Mask maskAllTrue() {
            return VectorIntrinsics.broadcastCoerced(Double256Mask.class, long.class, LENGTH,
                                                     (long)-1,
                                                     (z -> Double256Mask.TRUE_MASK));
        }

        @Override
        @ForceInline
        public Double256Mask maskAllFalse() {
            return VectorIntrinsics.broadcastCoerced(Double256Mask.class, long.class, LENGTH,
                                                     0,
                                                     (z -> Double256Mask.FALSE_MASK));
        }

        @Override
        @ForceInline
        public Double256Vector scalars(double... es) {
            Objects.requireNonNull(es);
            int ix = VectorIntrinsics.checkIndex(0, es.length, LENGTH);
            return VectorIntrinsics.load(Double256Vector.class, double.class, LENGTH,
                                         es, Unsafe.ARRAY_DOUBLE_BASE_OFFSET,
                                         es, ix,
                                         (c, idx) -> op(n -> c[idx + n]));
        }

        @Override
        @ForceInline
        public Double256Mask maskFromArray(boolean[] bits, int ix) {
            Objects.requireNonNull(bits);
            ix = VectorIntrinsics.checkIndex(ix, bits.length, LENGTH);
            return VectorIntrinsics.load(Double256Mask.class, long.class, LENGTH,
                                         bits, (long)ix + Unsafe.ARRAY_BOOLEAN_BASE_OFFSET,
                                         bits, ix,
                                         (c, idx) -> opm(n -> c[idx + n]));
        }

        @Override
        @ForceInline
        public Double256Vector fromArray(double[] a, int ix) {
            Objects.requireNonNull(a);
            ix = VectorIntrinsics.checkIndex(ix, a.length, LENGTH);
            return VectorIntrinsics.load(Double256Vector.class, double.class, LENGTH,
                                         a, (((long) ix) << ARRAY_SHIFT) + Unsafe.ARRAY_DOUBLE_BASE_OFFSET,
                                         a, ix,
                                         (c, idx) -> op(n -> c[idx + n]));
        }

        @Override
        @ForceInline
        public Double256Vector fromArray(double[] a, int ax, Mask<Double> m) {
            return zero().blend(fromArray(a, ax), m);
        }

        @Override
        @ForceInline
        public Double256Vector fromByteArray(byte[] a, int ix) {
            Objects.requireNonNull(a);
            ix = VectorIntrinsics.checkIndex(ix, a.length, bitSize() / Byte.SIZE);
            return VectorIntrinsics.load(Double256Vector.class, double.class, LENGTH,
                                         a, ((long) ix) + Unsafe.ARRAY_BYTE_BASE_OFFSET,
                                         a, ix,
                                         (c, idx) -> {
                                             ByteBuffer bbc = ByteBuffer.wrap(c, idx, a.length - idx).order(ByteOrder.nativeOrder());
                                             DoubleBuffer tb = bbc.asDoubleBuffer();
                                             return op(i -> tb.get());
                                         });
        }
        @Override
        @ForceInline
        public Double256Vector fromArray(double[] a, int ix, int[] b, int iy) {
            Objects.requireNonNull(a);
            Objects.requireNonNull(b);

            // Index vector: vix[0:n] = i -> ix + indexMap[iy + i]
            IntVector vix = INDEX_SPEC.fromArray(b, iy).add(ix);

            vix = VectorIntrinsics.checkIndex(vix, a.length);

            return VectorIntrinsics.loadWithMap(Double256Vector.class, double.class, LENGTH, Int128Vector.class,
                                        a, Unsafe.ARRAY_DOUBLE_BASE_OFFSET, vix,
                                        a, ix, b, iy,
                                       (c, idx, indexMap, idy) -> op(n -> c[idx + indexMap[idy+n]]));
       }

       @Override
       @ForceInline
       public Double256Vector fromArray(double[] a, int ax, Mask<Double> m, int[] indexMap, int j) {
           // @@@ This can result in out of bounds errors for unset mask lanes
           return zero().blend(fromArray(a, ax, indexMap, j), m);
       }


        @Override
        @ForceInline
        public Double256Vector fromByteArray(byte[] a, int ix, Mask<Double> m) {
            return zero().blend(fromByteArray(a, ix), m);
        }

        @Override
        @ForceInline
        public Double256Vector fromByteBuffer(ByteBuffer bb, int ix) {
            if (bb.order() != ByteOrder.nativeOrder()) {
                throw new IllegalArgumentException();
            }
            ix = VectorIntrinsics.checkIndex(ix, bb.limit(), bitSize() / Byte.SIZE);
            return VectorIntrinsics.load(Double256Vector.class, double.class, LENGTH,
                                         U.getReference(bb, BYTE_BUFFER_HB), U.getLong(bb, BUFFER_ADDRESS) + ix,
                                         bb, ix,
                                         (c, idx) -> {
                                             ByteBuffer bbc = c.duplicate().position(idx).order(ByteOrder.nativeOrder());
                                             DoubleBuffer tb = bbc.asDoubleBuffer();
                                             return op(i -> tb.get());
                                         });
        }

        @Override
        @ForceInline
        public Double256Vector fromByteBuffer(ByteBuffer bb, int ix, Mask<Double> m) {
            return zero().blend(fromByteBuffer(bb, ix), m);
        }

        @Override
        @ForceInline
        @SuppressWarnings("unchecked")
        public <F> Double256Vector cast(Vector<F> o) {
            if (o.length() != LENGTH)
                throw new IllegalArgumentException("Vector length this species length differ");

            return VectorIntrinsics.cast(
                o.getClass(),
                o.elementType(), LENGTH,
                Double256Vector.class,
                double.class, LENGTH,
                o, this,
                (s, v) -> s.castDefault(v)
            );
        }

        @SuppressWarnings("unchecked")
        @ForceInline
        private <F> Double256Vector castDefault(Vector<F> v) {
            // Allocate array of required size
            int limit = length();
            double[] a = new double[limit];

            Class<?> vtype = v.species().elementType();
            if (vtype == byte.class) {
                ByteVector tv = (ByteVector)v;
                for (int i = 0; i < limit; i++) {
                    a[i] = (double) tv.get(i);
                }
            } else if (vtype == short.class) {
                ShortVector tv = (ShortVector)v;
                for (int i = 0; i < limit; i++) {
                    a[i] = (double) tv.get(i);
                }
            } else if (vtype == int.class) {
                IntVector tv = (IntVector)v;
                for (int i = 0; i < limit; i++) {
                    a[i] = (double) tv.get(i);
                }
            } else if (vtype == long.class){
                LongVector tv = (LongVector)v;
                for (int i = 0; i < limit; i++) {
                    a[i] = (double) tv.get(i);
                }
            } else if (vtype == float.class){
                FloatVector tv = (FloatVector)v;
                for (int i = 0; i < limit; i++) {
                    a[i] = (double) tv.get(i);
                }
            } else if (vtype == double.class){
                DoubleVector tv = (DoubleVector)v;
                for (int i = 0; i < limit; i++) {
                    a[i] = (double) tv.get(i);
                }
            } else {
                throw new UnsupportedOperationException("Bad lane type for casting.");
            }

            return scalars(a);
        }

        @Override
        @ForceInline
        public <E> Double256Mask cast(Mask<E> m) {
            if (m.length() != LENGTH)
                throw new IllegalArgumentException("Mask length this species length differ");
            return new Double256Mask(m.toArray());
        }

        @Override
        @ForceInline
        public <E> Double256Shuffle cast(Shuffle<E> s) {
            if (s.length() != LENGTH)
                throw new IllegalArgumentException("Shuffle length this species length differ");
            return new Double256Shuffle(s.toArray());
        }

        @Override
        @ForceInline
        @SuppressWarnings("unchecked")
        public <F> Double256Vector rebracket(Vector<F> o) {
            Objects.requireNonNull(o);
            if (o.elementType() == byte.class) {
                Byte256Vector so = (Byte256Vector)o;
                return VectorIntrinsics.reinterpret(
                    Byte256Vector.class,
                    byte.class, so.length(),
                    Double256Vector.class,
                    double.class, LENGTH,
                    so, this,
                    (s, v) -> (Double256Vector) s.reshape(v)
                );
            } else if (o.elementType() == short.class) {
                Short256Vector so = (Short256Vector)o;
                return VectorIntrinsics.reinterpret(
                    Short256Vector.class,
                    short.class, so.length(),
                    Double256Vector.class,
                    double.class, LENGTH,
                    so, this,
                    (s, v) -> (Double256Vector) s.reshape(v)
                );
            } else if (o.elementType() == int.class) {
                Int256Vector so = (Int256Vector)o;
                return VectorIntrinsics.reinterpret(
                    Int256Vector.class,
                    int.class, so.length(),
                    Double256Vector.class,
                    double.class, LENGTH,
                    so, this,
                    (s, v) -> (Double256Vector) s.reshape(v)
                );
            } else if (o.elementType() == long.class) {
                Long256Vector so = (Long256Vector)o;
                return VectorIntrinsics.reinterpret(
                    Long256Vector.class,
                    long.class, so.length(),
                    Double256Vector.class,
                    double.class, LENGTH,
                    so, this,
                    (s, v) -> (Double256Vector) s.reshape(v)
                );
            } else if (o.elementType() == float.class) {
                Float256Vector so = (Float256Vector)o;
                return VectorIntrinsics.reinterpret(
                    Float256Vector.class,
                    float.class, so.length(),
                    Double256Vector.class,
                    double.class, LENGTH,
                    so, this,
                    (s, v) -> (Double256Vector) s.reshape(v)
                );
            } else if (o.elementType() == double.class) {
                Double256Vector so = (Double256Vector)o;
                return VectorIntrinsics.reinterpret(
                    Double256Vector.class,
                    double.class, so.length(),
                    Double256Vector.class,
                    double.class, LENGTH,
                    so, this,
                    (s, v) -> (Double256Vector) s.reshape(v)
                );
            } else {
                throw new InternalError("Unimplemented type");
            }
        }

        @Override
        @ForceInline
        @SuppressWarnings("unchecked")
        public Double256Vector resize(Vector<Double> o) {
            Objects.requireNonNull(o);
            if (o.bitSize() == 64 && (o instanceof Double64Vector)) {
                Double64Vector so = (Double64Vector)o;
                return VectorIntrinsics.reinterpret(
                    Double64Vector.class,
                    double.class, so.length(),
                    Double256Vector.class,
                    double.class, LENGTH,
                    so, this,
                    (s, v) -> (Double256Vector) s.reshape(v)
                );
            } else if (o.bitSize() == 128 && (o instanceof Double128Vector)) {
                Double128Vector so = (Double128Vector)o;
                return VectorIntrinsics.reinterpret(
                    Double128Vector.class,
                    double.class, so.length(),
                    Double256Vector.class,
                    double.class, LENGTH,
                    so, this,
                    (s, v) -> (Double256Vector) s.reshape(v)
                );
            } else if (o.bitSize() == 256 && (o instanceof Double256Vector)) {
                Double256Vector so = (Double256Vector)o;
                return VectorIntrinsics.reinterpret(
                    Double256Vector.class,
                    double.class, so.length(),
                    Double256Vector.class,
                    double.class, LENGTH,
                    so, this,
                    (s, v) -> (Double256Vector) s.reshape(v)
                );
            } else if (o.bitSize() == 512 && (o instanceof Double512Vector)) {
                Double512Vector so = (Double512Vector)o;
                return VectorIntrinsics.reinterpret(
                    Double512Vector.class,
                    double.class, so.length(),
                    Double256Vector.class,
                    double.class, LENGTH,
                    so, this,
                    (s, v) -> (Double256Vector) s.reshape(v)
                );
            } else if ((o.bitSize() > 0) && (o.bitSize() <= 2048)
                    && (o.bitSize() % 128 == 0) && (o instanceof DoubleMaxVector)) {
                DoubleMaxVector so = (DoubleMaxVector)o;
                return VectorIntrinsics.reinterpret(
                    DoubleMaxVector.class,
                    double.class, so.length(),
                    Double256Vector.class,
                    double.class, LENGTH,
                    so, this,
                    (s, v) -> (Double256Vector) s.reshape(v)
                );
            } else {
                throw new InternalError("Unimplemented size");
            }
        }
    }
}
