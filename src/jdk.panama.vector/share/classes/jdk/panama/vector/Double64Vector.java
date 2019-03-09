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
final class Double64Vector extends DoubleVector {
    static final Double64Species SPECIES = new Double64Species();

    static final Double64Vector ZERO = new Double64Vector();

    static final int LENGTH = SPECIES.length();

    private final double[] vec; // Don't access directly, use getElements() instead.

    private double[] getElements() {
        return VectorIntrinsics.maybeRebox(this).vec;
    }

    Double64Vector() {
        vec = new double[SPECIES.length()];
    }

    Double64Vector(double[] v) {
        vec = v;
    }

    @Override
    public int length() { return LENGTH; }

    // Unary operator

    @Override
    Double64Vector uOp(FUnOp f) {
        double[] vec = getElements();
        double[] res = new double[length()];
        for (int i = 0; i < length(); i++) {
            res[i] = f.apply(i, vec[i]);
        }
        return new Double64Vector(res);
    }

    @Override
    Double64Vector uOp(Mask<Double> o, FUnOp f) {
        double[] vec = getElements();
        double[] res = new double[length()];
        boolean[] mbits = ((Double64Mask)o).getBits();
        for (int i = 0; i < length(); i++) {
            res[i] = mbits[i] ? f.apply(i, vec[i]) : vec[i];
        }
        return new Double64Vector(res);
    }

    // Binary operator

    @Override
    Double64Vector bOp(Vector<Double> o, FBinOp f) {
        double[] res = new double[length()];
        double[] vec1 = this.getElements();
        double[] vec2 = ((Double64Vector)o).getElements();
        for (int i = 0; i < length(); i++) {
            res[i] = f.apply(i, vec1[i], vec2[i]);
        }
        return new Double64Vector(res);
    }

    @Override
    Double64Vector bOp(Vector<Double> o1, Mask<Double> o2, FBinOp f) {
        double[] res = new double[length()];
        double[] vec1 = this.getElements();
        double[] vec2 = ((Double64Vector)o1).getElements();
        boolean[] mbits = ((Double64Mask)o2).getBits();
        for (int i = 0; i < length(); i++) {
            res[i] = mbits[i] ? f.apply(i, vec1[i], vec2[i]) : vec1[i];
        }
        return new Double64Vector(res);
    }

    // Trinary operator

    @Override
    Double64Vector tOp(Vector<Double> o1, Vector<Double> o2, FTriOp f) {
        double[] res = new double[length()];
        double[] vec1 = this.getElements();
        double[] vec2 = ((Double64Vector)o1).getElements();
        double[] vec3 = ((Double64Vector)o2).getElements();
        for (int i = 0; i < length(); i++) {
            res[i] = f.apply(i, vec1[i], vec2[i], vec3[i]);
        }
        return new Double64Vector(res);
    }

    @Override
    Double64Vector tOp(Vector<Double> o1, Vector<Double> o2, Mask<Double> o3, FTriOp f) {
        double[] res = new double[length()];
        double[] vec1 = getElements();
        double[] vec2 = ((Double64Vector)o1).getElements();
        double[] vec3 = ((Double64Vector)o2).getElements();
        boolean[] mbits = ((Double64Mask)o3).getBits();
        for (int i = 0; i < length(); i++) {
            res[i] = mbits[i] ? f.apply(i, vec1[i], vec2[i], vec3[i]) : vec1[i];
        }
        return new Double64Vector(res);
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
    public Double64Vector div(Vector<Double> v, Mask<Double> m) {
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
    public Double64Vector neg(Mask<Double> m) {
        return blend(neg(), m);
    }

    @Override
    @ForceInline
    public Double64Vector abs() {
        return VectorIntrinsics.unaryOp(
            VECTOR_OP_ABS, Double64Vector.class, double.class, LENGTH,
            this,
            v1 -> v1.uOp((i, a) -> (double) Math.abs(a)));
    }

    @ForceInline
    @Override
    public Double64Vector abs(Mask<Double> m) {
        return blend(abs(), m);
    }

    @Override
    @ForceInline
    public Double64Vector neg() {
        return VectorIntrinsics.unaryOp(
            VECTOR_OP_NEG, Double64Vector.class, double.class, LENGTH,
            this,
            v1 -> v1.uOp((i, a) -> (double) -a));
    }

    @Override
    @ForceInline
    public Double64Vector div(Vector<Double> o) {
        Objects.requireNonNull(o);
        Double64Vector v = (Double64Vector)o;
        return VectorIntrinsics.binaryOp(
            VECTOR_OP_DIV, Double64Vector.class, double.class, LENGTH,
            this, v,
            (v1, v2) -> v1.bOp(v2, (i, a, b) -> (double)(a / b)));
    }

    @Override
    @ForceInline
    public Double64Vector sqrt() {
        return VectorIntrinsics.unaryOp(
            VECTOR_OP_SQRT, Double64Vector.class, double.class, LENGTH,
            this,
            v1 -> v1.uOp((i, a) -> (double) Math.sqrt((double) a)));
    }

    @Override
    @ForceInline
    public Double64Vector exp() {
        return (Double64Vector) VectorIntrinsics.unaryOp(
            VECTOR_OP_EXP, Double64Vector.class, double.class, LENGTH,
            this,
            v1 -> ((Double64Vector)v1).uOp((i, a) -> (double) Math.exp((double) a)));
    }

    @Override
    @ForceInline
    public Double64Vector log1p() {
        return (Double64Vector) VectorIntrinsics.unaryOp(
            VECTOR_OP_LOG1P, Double64Vector.class, double.class, LENGTH,
            this,
            v1 -> ((Double64Vector)v1).uOp((i, a) -> (double) Math.log1p((double) a)));
    }

    @Override
    @ForceInline
    public Double64Vector log() {
        return (Double64Vector) VectorIntrinsics.unaryOp(
            VECTOR_OP_LOG, Double64Vector.class, double.class, LENGTH,
            this,
            v1 -> ((Double64Vector)v1).uOp((i, a) -> (double) Math.log((double) a)));
    }

    @Override
    @ForceInline
    public Double64Vector log10() {
        return (Double64Vector) VectorIntrinsics.unaryOp(
            VECTOR_OP_LOG10, Double64Vector.class, double.class, LENGTH,
            this,
            v1 -> ((Double64Vector)v1).uOp((i, a) -> (double) Math.log10((double) a)));
    }

    @Override
    @ForceInline
    public Double64Vector expm1() {
        return (Double64Vector) VectorIntrinsics.unaryOp(
            VECTOR_OP_EXPM1, Double64Vector.class, double.class, LENGTH,
            this,
            v1 -> ((Double64Vector)v1).uOp((i, a) -> (double) Math.expm1((double) a)));
    }

    @Override
    @ForceInline
    public Double64Vector cbrt() {
        return (Double64Vector) VectorIntrinsics.unaryOp(
            VECTOR_OP_CBRT, Double64Vector.class, double.class, LENGTH,
            this,
            v1 -> ((Double64Vector)v1).uOp((i, a) -> (double) Math.cbrt((double) a)));
    }

    @Override
    @ForceInline
    public Double64Vector sin() {
        return (Double64Vector) VectorIntrinsics.unaryOp(
            VECTOR_OP_SIN, Double64Vector.class, double.class, LENGTH,
            this,
            v1 -> ((Double64Vector)v1).uOp((i, a) -> (double) Math.sin((double) a)));
    }

    @Override
    @ForceInline
    public Double64Vector cos() {
        return (Double64Vector) VectorIntrinsics.unaryOp(
            VECTOR_OP_COS, Double64Vector.class, double.class, LENGTH,
            this,
            v1 -> ((Double64Vector)v1).uOp((i, a) -> (double) Math.cos((double) a)));
    }

    @Override
    @ForceInline
    public Double64Vector tan() {
        return (Double64Vector) VectorIntrinsics.unaryOp(
            VECTOR_OP_TAN, Double64Vector.class, double.class, LENGTH,
            this,
            v1 -> ((Double64Vector)v1).uOp((i, a) -> (double) Math.tan((double) a)));
    }

    @Override
    @ForceInline
    public Double64Vector asin() {
        return (Double64Vector) VectorIntrinsics.unaryOp(
            VECTOR_OP_ASIN, Double64Vector.class, double.class, LENGTH,
            this,
            v1 -> ((Double64Vector)v1).uOp((i, a) -> (double) Math.asin((double) a)));
    }

    @Override
    @ForceInline
    public Double64Vector acos() {
        return (Double64Vector) VectorIntrinsics.unaryOp(
            VECTOR_OP_ACOS, Double64Vector.class, double.class, LENGTH,
            this,
            v1 -> ((Double64Vector)v1).uOp((i, a) -> (double) Math.acos((double) a)));
    }

    @Override
    @ForceInline
    public Double64Vector atan() {
        return (Double64Vector) VectorIntrinsics.unaryOp(
            VECTOR_OP_ATAN, Double64Vector.class, double.class, LENGTH,
            this,
            v1 -> ((Double64Vector)v1).uOp((i, a) -> (double) Math.atan((double) a)));
    }

    @Override
    @ForceInline
    public Double64Vector sinh() {
        return (Double64Vector) VectorIntrinsics.unaryOp(
            VECTOR_OP_SINH, Double64Vector.class, double.class, LENGTH,
            this,
            v1 -> ((Double64Vector)v1).uOp((i, a) -> (double) Math.sinh((double) a)));
    }

    @Override
    @ForceInline
    public Double64Vector cosh() {
        return (Double64Vector) VectorIntrinsics.unaryOp(
            VECTOR_OP_COSH, Double64Vector.class, double.class, LENGTH,
            this,
            v1 -> ((Double64Vector)v1).uOp((i, a) -> (double) Math.cosh((double) a)));
    }

    @Override
    @ForceInline
    public Double64Vector tanh() {
        return (Double64Vector) VectorIntrinsics.unaryOp(
            VECTOR_OP_TANH, Double64Vector.class, double.class, LENGTH,
            this,
            v1 -> ((Double64Vector)v1).uOp((i, a) -> (double) Math.tanh((double) a)));
    }

    @Override
    @ForceInline
    public Double64Vector pow(Vector<Double> o) {
        Objects.requireNonNull(o);
        Double64Vector v = (Double64Vector)o;
        return (Double64Vector) VectorIntrinsics.binaryOp(
            VECTOR_OP_POW, Double64Vector.class, double.class, LENGTH,
            this, v,
            (v1, v2) -> ((Double64Vector)v1).bOp(v2, (i, a, b) -> (double)(Math.pow(a,b))));
    }

    @Override
    @ForceInline
    public Double64Vector hypot(Vector<Double> o) {
        Objects.requireNonNull(o);
        Double64Vector v = (Double64Vector)o;
        return (Double64Vector) VectorIntrinsics.binaryOp(
            VECTOR_OP_HYPOT, Double64Vector.class, double.class, LENGTH,
            this, v,
            (v1, v2) -> ((Double64Vector)v1).bOp(v2, (i, a, b) -> (double)(Math.hypot(a,b))));
    }

    @Override
    @ForceInline
    public Double64Vector atan2(Vector<Double> o) {
        Objects.requireNonNull(o);
        Double64Vector v = (Double64Vector)o;
        return (Double64Vector) VectorIntrinsics.binaryOp(
            VECTOR_OP_ATAN2, Double64Vector.class, double.class, LENGTH,
            this, v,
            (v1, v2) -> ((Double64Vector)v1).bOp(v2, (i, a, b) -> (double)(Math.atan2(a,b))));
    }


    // Binary operations

    @Override
    @ForceInline
    public Double64Vector add(Vector<Double> o) {
        Objects.requireNonNull(o);
        Double64Vector v = (Double64Vector)o;
        return VectorIntrinsics.binaryOp(
            VECTOR_OP_ADD, Double64Vector.class, double.class, LENGTH,
            this, v,
            (v1, v2) -> v1.bOp(v2, (i, a, b) -> (double)(a + b)));
    }

    @Override
    @ForceInline
    public Double64Vector add(Vector<Double> v, Mask<Double> m) {
        return blend(add(v), m);
    }

    @Override
    @ForceInline
    public Double64Vector sub(Vector<Double> o) {
        Objects.requireNonNull(o);
        Double64Vector v = (Double64Vector)o;
        return VectorIntrinsics.binaryOp(
            VECTOR_OP_SUB, Double64Vector.class, double.class, LENGTH,
            this, v,
            (v1, v2) -> v1.bOp(v2, (i, a, b) -> (double)(a - b)));
    }

    @Override
    @ForceInline
    public Double64Vector sub(Vector<Double> v, Mask<Double> m) {
        return blend(sub(v), m);
    }

    @Override
    @ForceInline
    public Double64Vector mul(Vector<Double> o) {
        Objects.requireNonNull(o);
        Double64Vector v = (Double64Vector)o;
        return VectorIntrinsics.binaryOp(
            VECTOR_OP_MUL, Double64Vector.class, double.class, LENGTH,
            this, v,
            (v1, v2) -> v1.bOp(v2, (i, a, b) -> (double)(a * b)));
    }

    @Override
    @ForceInline
    public Double64Vector mul(Vector<Double> v, Mask<Double> m) {
        return blend(mul(v), m);
    }

    @Override
    @ForceInline
    public Double64Vector min(Vector<Double> o) {
        Objects.requireNonNull(o);
        Double64Vector v = (Double64Vector)o;
        return (Double64Vector) VectorIntrinsics.binaryOp(
            VECTOR_OP_MIN, Double64Vector.class, double.class, LENGTH,
            this, v,
            (v1, v2) -> ((Double64Vector)v1).bOp(v2, (i, a, b) -> (double) ((a < b) ? a : b)));
    }

    @Override
    @ForceInline
    public Double64Vector min(Vector<Double> v, Mask<Double> m) {
        return blend(min(v), m);
    }

    @Override
    @ForceInline
    public Double64Vector max(Vector<Double> o) {
        Objects.requireNonNull(o);
        Double64Vector v = (Double64Vector)o;
        return VectorIntrinsics.binaryOp(
            VECTOR_OP_MAX, Double64Vector.class, double.class, LENGTH,
            this, v,
            (v1, v2) -> v1.bOp(v2, (i, a, b) -> (double) ((a > b) ? a : b)));
        }

    @Override
    @ForceInline
    public Double64Vector max(Vector<Double> v, Mask<Double> m) {
        return blend(max(v), m);
    }


    // Ternary operations

    @Override
    @ForceInline
    public Double64Vector fma(Vector<Double> o1, Vector<Double> o2) {
        Objects.requireNonNull(o1);
        Objects.requireNonNull(o2);
        Double64Vector v1 = (Double64Vector)o1;
        Double64Vector v2 = (Double64Vector)o2;
        return VectorIntrinsics.ternaryOp(
            VECTOR_OP_FMA, Double64Vector.class, double.class, LENGTH,
            this, v1, v2,
            (w1, w2, w3) -> w1.tOp(w2, w3, (i, a, b, c) -> Math.fma(a, b, c)));
    }

    // Type specific horizontal reductions

    @Override
    @ForceInline
    public double addAll() {
        long bits = (long) VectorIntrinsics.reductionCoerced(
                                VECTOR_OP_ADD, Double64Vector.class, double.class, LENGTH,
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
                                VECTOR_OP_SUB, Double64Vector.class, double.class, LENGTH,
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
                                VECTOR_OP_MUL, Double64Vector.class, double.class, LENGTH,
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
                                VECTOR_OP_MIN, Double64Vector.class, double.class, LENGTH,
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
                                VECTOR_OP_MAX, Double64Vector.class, double.class, LENGTH,
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
        VectorIntrinsics.store(Double64Vector.class, double.class, LENGTH,
                               a, (((long) ix) << ARRAY_SHIFT) + Unsafe.ARRAY_DOUBLE_BASE_OFFSET,
                               this,
                               a, ix,
                               (arr, idx, v) -> v.forEach((i, e) -> arr[idx + i] = e));
    }

    @Override
    @ForceInline
    public final void intoArray(double[] a, int ax, Mask<Double> m) {
        Double64Vector oldVal = SPECIES.fromArray(a, ax);
        Double64Vector newVal = oldVal.blend(this, m);
        newVal.intoArray(a, ax);
    }
    @Override
    @ForceInline
    public void intoArray(double[] a, int ix, int[] b, int iy) {
        this.intoArray(a, ix + b[iy]);
    }

     @Override
     @ForceInline
     public final void intoArray(double[] a, int ax, Mask<Double> m, int[] b, int iy) {
         // @@@ This can result in out of bounds errors for unset mask lanes
         Double64Vector oldVal = SPECIES.fromArray(a, ax, b, iy);
         Double64Vector newVal = oldVal.blend(this, m);
         newVal.intoArray(a, ax, b, iy);
     }

    @Override
    @ForceInline
    public void intoByteArray(byte[] a, int ix) {
        Objects.requireNonNull(a);
        ix = VectorIntrinsics.checkIndex(ix, a.length, bitSize() / Byte.SIZE);
        VectorIntrinsics.store(Double64Vector.class, double.class, LENGTH,
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
        Double64Vector oldVal = SPECIES.fromByteArray(a, ix);
        Double64Vector newVal = oldVal.blend(this, m);
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
        VectorIntrinsics.store(Double64Vector.class, double.class, LENGTH,
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
        Double64Vector oldVal = SPECIES.fromByteBuffer(bb, ix);
        Double64Vector newVal = oldVal.blend(this, m);
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

        Double64Vector that = (Double64Vector) o;
        return this.equal(that).allTrue();
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(vec);
    }

    // Binary test

    @Override
    Double64Mask bTest(Vector<Double> o, FBinTest f) {
        double[] vec1 = getElements();
        double[] vec2 = ((Double64Vector)o).getElements();
        boolean[] bits = new boolean[length()];
        for (int i = 0; i < length(); i++){
            bits[i] = f.apply(i, vec1[i], vec2[i]);
        }
        return new Double64Mask(bits);
    }

    // Comparisons

    @Override
    @ForceInline
    public Double64Mask equal(Vector<Double> o) {
        Objects.requireNonNull(o);
        Double64Vector v = (Double64Vector)o;

        return VectorIntrinsics.compare(
            BT_eq, Double64Vector.class, Double64Mask.class, double.class, LENGTH,
            this, v,
            (v1, v2) -> v1.bTest(v2, (i, a, b) -> a == b));
    }

    @Override
    @ForceInline
    public Double64Mask notEqual(Vector<Double> o) {
        Objects.requireNonNull(o);
        Double64Vector v = (Double64Vector)o;

        return VectorIntrinsics.compare(
            BT_ne, Double64Vector.class, Double64Mask.class, double.class, LENGTH,
            this, v,
            (v1, v2) -> v1.bTest(v2, (i, a, b) -> a != b));
    }

    @Override
    @ForceInline
    public Double64Mask lessThan(Vector<Double> o) {
        Objects.requireNonNull(o);
        Double64Vector v = (Double64Vector)o;

        return VectorIntrinsics.compare(
            BT_lt, Double64Vector.class, Double64Mask.class, double.class, LENGTH,
            this, v,
            (v1, v2) -> v1.bTest(v2, (i, a, b) -> a < b));
    }

    @Override
    @ForceInline
    public Double64Mask lessThanEq(Vector<Double> o) {
        Objects.requireNonNull(o);
        Double64Vector v = (Double64Vector)o;

        return VectorIntrinsics.compare(
            BT_le, Double64Vector.class, Double64Mask.class, double.class, LENGTH,
            this, v,
            (v1, v2) -> v1.bTest(v2, (i, a, b) -> a <= b));
    }

    @Override
    @ForceInline
    public Double64Mask greaterThan(Vector<Double> o) {
        Objects.requireNonNull(o);
        Double64Vector v = (Double64Vector)o;

        return (Double64Mask) VectorIntrinsics.compare(
            BT_gt, Double64Vector.class, Double64Mask.class, double.class, LENGTH,
            this, v,
            (v1, v2) -> v1.bTest(v2, (i, a, b) -> a > b));
    }

    @Override
    @ForceInline
    public Double64Mask greaterThanEq(Vector<Double> o) {
        Objects.requireNonNull(o);
        Double64Vector v = (Double64Vector)o;

        return VectorIntrinsics.compare(
            BT_ge, Double64Vector.class, Double64Mask.class, double.class, LENGTH,
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
        boolean[] mbits = ((Double64Mask)o).getBits();
        forEach((i, a) -> {
            if (mbits[i]) { f.apply(i, a); }
        });
    }

    Long64Vector toBits() {
        double[] vec = getElements();
        long[] res = new long[this.species().length()];
        for(int i = 0; i < this.species().length(); i++){
            res[i] = Double.doubleToLongBits(vec[i]);
        }
        return new Long64Vector(res);
    }


    @Override
    public Double64Vector rotateEL(int j) {
        double[] vec = getElements();
        double[] res = new double[length()];
        for (int i = 0; i < length(); i++){
            res[(j + i) % length()] = vec[i];
        }
        return new Double64Vector(res);
    }

    @Override
    public Double64Vector rotateER(int j) {
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
        return new Double64Vector(res);
    }

    @Override
    public Double64Vector shiftEL(int j) {
        double[] vec = getElements();
        double[] res = new double[length()];
        for (int i = 0; i < length() - j; i++) {
            res[i] = vec[i + j];
        }
        return new Double64Vector(res);
    }

    @Override
    public Double64Vector shiftER(int j) {
        double[] vec = getElements();
        double[] res = new double[length()];
        for (int i = 0; i < length() - j; i++){
            res[i + j] = vec[i];
        }
        return new Double64Vector(res);
    }

    @Override
    @ForceInline
    public Double64Vector rearrange(Vector<Double> v,
                                  Shuffle<Double> s, Mask<Double> m) {
        return this.rearrange(s).blend(v.rearrange(s), m);
    }

    @Override
    @ForceInline
    public Double64Vector rearrange(Shuffle<Double> o1) {
    Objects.requireNonNull(o1);
    Double64Shuffle s =  (Double64Shuffle)o1;

        return VectorIntrinsics.rearrangeOp(
            Double64Vector.class, Double64Shuffle.class, double.class, LENGTH,
            this, s,
            (v1, s_) -> v1.uOp((i, a) -> {
            double[] vec = this.getElements();
            int ei = s_.getElement(i);
            return vec[ei];
        }));
    }

    @Override
    @ForceInline
    public Double64Vector blend(Vector<Double> o1, Mask<Double> o2) {
        Objects.requireNonNull(o1);
        Objects.requireNonNull(o2);
        Double64Vector v = (Double64Vector)o1;
        Double64Mask   m = (Double64Mask)o2;

        return VectorIntrinsics.blend(
            Double64Vector.class, Double64Mask.class, double.class, LENGTH,
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
                                Double64Vector.class, double.class, LENGTH,
                                this, i,
                                (vec, ix) -> {
                                    double[] vecarr = vec.getElements();
                                    return (long)Double.doubleToLongBits(vecarr[ix]);
                                });
        return Double.longBitsToDouble(bits);
    }

    @Override
    public Double64Vector with(int i, double e) {
        if (i < 0 || i >= LENGTH) {
            throw new IllegalArgumentException("Index " + i + " must be zero or positive, and less than " + LENGTH);
        }
        return VectorIntrinsics.insert(
                                Double64Vector.class, double.class, LENGTH,
                                this, i, (long)Double.doubleToLongBits(e),
                                (v, ix, bits) -> {
                                    double[] res = v.getElements().clone();
                                    res[ix] = Double.longBitsToDouble((long)bits);
                                    return new Double64Vector(res);
                                });
    }

    // Mask

    static final class Double64Mask extends AbstractMask<Double> {
        static final Double64Mask TRUE_MASK = new Double64Mask(true);
        static final Double64Mask FALSE_MASK = new Double64Mask(false);

        private final boolean[] bits; // Don't access directly, use getBits() instead.

        public Double64Mask(boolean[] bits) {
            this(bits, 0);
        }

        public Double64Mask(boolean[] bits, int offset) {
            boolean[] a = new boolean[species().length()];
            for (int i = 0; i < a.length; i++) {
                a[i] = bits[offset + i];
            }
            this.bits = a;
        }

        public Double64Mask(boolean val) {
            boolean[] bits = new boolean[species().length()];
            Arrays.fill(bits, val);
            this.bits = bits;
        }

        boolean[] getBits() {
            return VectorIntrinsics.maybeRebox(this).bits;
        }

        @Override
        Double64Mask uOp(MUnOp f) {
            boolean[] res = new boolean[species().length()];
            boolean[] bits = getBits();
            for (int i = 0; i < species().length(); i++) {
                res[i] = f.apply(i, bits[i]);
            }
            return new Double64Mask(res);
        }

        @Override
        Double64Mask bOp(Mask<Double> o, MBinOp f) {
            boolean[] res = new boolean[species().length()];
            boolean[] bits = getBits();
            boolean[] mbits = ((Double64Mask)o).getBits();
            for (int i = 0; i < species().length(); i++) {
                res[i] = f.apply(i, bits[i], mbits[i]);
            }
            return new Double64Mask(res);
        }

        @Override
        public Double64Species species() {
            return SPECIES;
        }

        @Override
        public Double64Vector toVector() {
            double[] res = new double[species().length()];
            boolean[] bits = getBits();
            for (int i = 0; i < species().length(); i++) {
                // -1 will result in the most significant bit being set in
                // addition to some or all other bits
                res[i] = (double) (bits[i] ? -1 : 0);
            }
            return new Double64Vector(res);
        }

        // Unary operations

        @Override
        @ForceInline
        public Double64Mask not() {
            return (Double64Mask) VectorIntrinsics.unaryOp(
                                             VECTOR_OP_NOT, Double64Mask.class, long.class, LENGTH,
                                             this,
                                             (m1) -> m1.uOp((i, a) -> !a));
        }

        // Binary operations

        @Override
        @ForceInline
        public Double64Mask and(Mask<Double> o) {
            Objects.requireNonNull(o);
            Double64Mask m = (Double64Mask)o;
            return VectorIntrinsics.binaryOp(VECTOR_OP_AND, Double64Mask.class, long.class, LENGTH,
                                             this, m,
                                             (m1, m2) -> m1.bOp(m2, (i, a, b) -> a & b));
        }

        @Override
        @ForceInline
        public Double64Mask or(Mask<Double> o) {
            Objects.requireNonNull(o);
            Double64Mask m = (Double64Mask)o;
            return VectorIntrinsics.binaryOp(VECTOR_OP_OR, Double64Mask.class, long.class, LENGTH,
                                             this, m,
                                             (m1, m2) -> m1.bOp(m2, (i, a, b) -> a | b));
        }

        // Reductions

        @Override
        @ForceInline
        public boolean anyTrue() {
            return VectorIntrinsics.test(COND_notZero, Double64Mask.class, long.class, LENGTH,
                                         this, this,
                                         (m1, m2) -> super.anyTrue());
        }

        @Override
        @ForceInline
        public boolean allTrue() {
            return VectorIntrinsics.test(COND_carrySet, Double64Mask.class, long.class, LENGTH,
                                         this, species().maskAllTrue(),
                                         (m1, m2) -> super.allTrue());
        }
    }

    // Shuffle

    static final class Double64Shuffle extends AbstractShuffle<Double> {
        Double64Shuffle(byte[] reorder) {
            super(reorder);
        }

        public Double64Shuffle(int[] reorder) {
            super(reorder);
        }

        public Double64Shuffle(int[] reorder, int i) {
            super(reorder, i);
        }

        public Double64Shuffle(IntUnaryOperator f) {
            super(f);
        }

        @Override
        public Double64Species species() {
            return SPECIES;
        }

        @Override
        public Double64Vector toVector() {
            double[] va = new double[SPECIES.length()];
            for (int i = 0; i < va.length; i++) {
              va[i] = (double) getElement(i);
            }
            return species().fromArray(va, 0);
        }

        @Override
        public Double64Shuffle rearrange(Vector.Shuffle<Double> o) {
            Double64Shuffle s = (Double64Shuffle) o;
            byte[] r = new byte[reorder.length];
            for (int i = 0; i < reorder.length; i++) {
                r[i] = reorder[s.reorder[i]];
            }
            return new Double64Shuffle(r);
        }
    }

    // Species

    @Override
    public Double64Species species() {
        return SPECIES;
    }

    static final class Double64Species extends DoubleSpecies {
        static final int BIT_SIZE = Shape.S_64_BIT.bitSize();

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
            return Shape.S_64_BIT;
        }

        @Override
        Double64Vector op(FOp f) {
            double[] res = new double[length()];
            for (int i = 0; i < length(); i++) {
                res[i] = f.apply(i);
            }
            return new Double64Vector(res);
        }

        @Override
        Double64Vector op(Mask<Double> o, FOp f) {
            double[] res = new double[length()];
            boolean[] mbits = ((Double64Mask)o).getBits();
            for (int i = 0; i < length(); i++) {
                if (mbits[i]) {
                    res[i] = f.apply(i);
                }
            }
            return new Double64Vector(res);
        }

        @Override
        Double64Mask opm(FOpm f) {
            boolean[] res = new boolean[length()];
            for (int i = 0; i < length(); i++) {
                res[i] = (boolean)f.apply(i);
            }
            return new Double64Mask(res);
        }

        // Factories

        @Override
        public Double64Mask maskFromValues(boolean... bits) {
            return new Double64Mask(bits);
        }

        @Override
        public Double64Shuffle shuffle(IntUnaryOperator f) {
            return new Double64Shuffle(f);
        }

        @Override
        public Double64Shuffle shuffleIota() {
            return new Double64Shuffle(AbstractShuffle.IDENTITY);
        }

        @Override
        public Double64Shuffle shuffleFromValues(int... ixs) {
            return new Double64Shuffle(ixs);
        }

        @Override
        public Double64Shuffle shuffleFromArray(int[] ixs, int i) {
            return new Double64Shuffle(ixs, i);
        }

        @Override
        @ForceInline
        public Double64Vector zero() {
            return VectorIntrinsics.broadcastCoerced(Double64Vector.class, double.class, LENGTH,
                                                     Double.doubleToLongBits(0.0f),
                                                     (z -> ZERO));
        }

        @Override
        @ForceInline
        public Double64Vector broadcast(double e) {
            return VectorIntrinsics.broadcastCoerced(
                Double64Vector.class, double.class, LENGTH,
                Double.doubleToLongBits(e),
                ((long bits) -> SPECIES.op(i -> Double.longBitsToDouble((long)bits))));
        }

        @Override
        @ForceInline
        public Double64Mask maskAllTrue() {
            return VectorIntrinsics.broadcastCoerced(Double64Mask.class, long.class, LENGTH,
                                                     (long)-1,
                                                     (z -> Double64Mask.TRUE_MASK));
        }

        @Override
        @ForceInline
        public Double64Mask maskAllFalse() {
            return VectorIntrinsics.broadcastCoerced(Double64Mask.class, long.class, LENGTH,
                                                     0,
                                                     (z -> Double64Mask.FALSE_MASK));
        }

        @Override
        @ForceInline
        public Double64Vector scalars(double... es) {
            Objects.requireNonNull(es);
            int ix = VectorIntrinsics.checkIndex(0, es.length, LENGTH);
            return VectorIntrinsics.load(Double64Vector.class, double.class, LENGTH,
                                         es, Unsafe.ARRAY_DOUBLE_BASE_OFFSET,
                                         es, ix,
                                         (c, idx) -> op(n -> c[idx + n]));
        }

        @Override
        @ForceInline
        public Double64Mask maskFromArray(boolean[] bits, int ix) {
            Objects.requireNonNull(bits);
            ix = VectorIntrinsics.checkIndex(ix, bits.length, LENGTH);
            return VectorIntrinsics.load(Double64Mask.class, long.class, LENGTH,
                                         bits, (long)ix + Unsafe.ARRAY_BOOLEAN_BASE_OFFSET,
                                         bits, ix,
                                         (c, idx) -> opm(n -> c[idx + n]));
        }

        @Override
        @ForceInline
        public Double64Vector fromArray(double[] a, int ix) {
            Objects.requireNonNull(a);
            ix = VectorIntrinsics.checkIndex(ix, a.length, LENGTH);
            return VectorIntrinsics.load(Double64Vector.class, double.class, LENGTH,
                                         a, (((long) ix) << ARRAY_SHIFT) + Unsafe.ARRAY_DOUBLE_BASE_OFFSET,
                                         a, ix,
                                         (c, idx) -> op(n -> c[idx + n]));
        }

        @Override
        @ForceInline
        public Double64Vector fromArray(double[] a, int ax, Mask<Double> m) {
            return zero().blend(fromArray(a, ax), m);
        }

        @Override
        @ForceInline
        public Double64Vector fromByteArray(byte[] a, int ix) {
            Objects.requireNonNull(a);
            ix = VectorIntrinsics.checkIndex(ix, a.length, bitSize() / Byte.SIZE);
            return VectorIntrinsics.load(Double64Vector.class, double.class, LENGTH,
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
        public Double64Vector fromArray(double[] a, int ix, int[] b, int iy) {
            return SPECIES.fromArray(a, ix + b[iy]);
       }

       @Override
       @ForceInline
       public Double64Vector fromArray(double[] a, int ax, Mask<Double> m, int[] indexMap, int j) {
           // @@@ This can result in out of bounds errors for unset mask lanes
           return zero().blend(fromArray(a, ax, indexMap, j), m);
       }


        @Override
        @ForceInline
        public Double64Vector fromByteArray(byte[] a, int ix, Mask<Double> m) {
            return zero().blend(fromByteArray(a, ix), m);
        }

        @Override
        @ForceInline
        public Double64Vector fromByteBuffer(ByteBuffer bb, int ix) {
            if (bb.order() != ByteOrder.nativeOrder()) {
                throw new IllegalArgumentException();
            }
            ix = VectorIntrinsics.checkIndex(ix, bb.limit(), bitSize() / Byte.SIZE);
            return VectorIntrinsics.load(Double64Vector.class, double.class, LENGTH,
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
        public Double64Vector fromByteBuffer(ByteBuffer bb, int ix, Mask<Double> m) {
            return zero().blend(fromByteBuffer(bb, ix), m);
        }

        @Override
        @ForceInline
        @SuppressWarnings("unchecked")
        public <F> Double64Vector cast(Vector<F> o) {
            if (o.length() != LENGTH)
                throw new IllegalArgumentException("Vector length this species length differ");

            return VectorIntrinsics.cast(
                o.getClass(),
                o.elementType(), LENGTH,
                Double64Vector.class,
                double.class, LENGTH,
                o, this,
                (s, v) -> s.castDefault(v)
            );
        }

        @SuppressWarnings("unchecked")
        @ForceInline
        private <F> Double64Vector castDefault(Vector<F> v) {
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
        public <E> Double64Mask cast(Mask<E> m) {
            if (m.length() != LENGTH)
                throw new IllegalArgumentException("Mask length this species length differ");
            return new Double64Mask(m.toArray());
        }

        @Override
        @ForceInline
        public <E> Double64Shuffle cast(Shuffle<E> s) {
            if (s.length() != LENGTH)
                throw new IllegalArgumentException("Shuffle length this species length differ");
            return new Double64Shuffle(s.toArray());
        }

        @Override
        @ForceInline
        @SuppressWarnings("unchecked")
        public <F> Double64Vector rebracket(Vector<F> o) {
            Objects.requireNonNull(o);
            if (o.elementType() == byte.class) {
                Byte64Vector so = (Byte64Vector)o;
                return VectorIntrinsics.reinterpret(
                    Byte64Vector.class,
                    byte.class, so.length(),
                    Double64Vector.class,
                    double.class, LENGTH,
                    so, this,
                    (s, v) -> (Double64Vector) s.reshape(v)
                );
            } else if (o.elementType() == short.class) {
                Short64Vector so = (Short64Vector)o;
                return VectorIntrinsics.reinterpret(
                    Short64Vector.class,
                    short.class, so.length(),
                    Double64Vector.class,
                    double.class, LENGTH,
                    so, this,
                    (s, v) -> (Double64Vector) s.reshape(v)
                );
            } else if (o.elementType() == int.class) {
                Int64Vector so = (Int64Vector)o;
                return VectorIntrinsics.reinterpret(
                    Int64Vector.class,
                    int.class, so.length(),
                    Double64Vector.class,
                    double.class, LENGTH,
                    so, this,
                    (s, v) -> (Double64Vector) s.reshape(v)
                );
            } else if (o.elementType() == long.class) {
                Long64Vector so = (Long64Vector)o;
                return VectorIntrinsics.reinterpret(
                    Long64Vector.class,
                    long.class, so.length(),
                    Double64Vector.class,
                    double.class, LENGTH,
                    so, this,
                    (s, v) -> (Double64Vector) s.reshape(v)
                );
            } else if (o.elementType() == float.class) {
                Float64Vector so = (Float64Vector)o;
                return VectorIntrinsics.reinterpret(
                    Float64Vector.class,
                    float.class, so.length(),
                    Double64Vector.class,
                    double.class, LENGTH,
                    so, this,
                    (s, v) -> (Double64Vector) s.reshape(v)
                );
            } else if (o.elementType() == double.class) {
                Double64Vector so = (Double64Vector)o;
                return VectorIntrinsics.reinterpret(
                    Double64Vector.class,
                    double.class, so.length(),
                    Double64Vector.class,
                    double.class, LENGTH,
                    so, this,
                    (s, v) -> (Double64Vector) s.reshape(v)
                );
            } else {
                throw new InternalError("Unimplemented type");
            }
        }

        @Override
        @ForceInline
        @SuppressWarnings("unchecked")
        public Double64Vector resize(Vector<Double> o) {
            Objects.requireNonNull(o);
            if (o.bitSize() == 64 && (o instanceof Double64Vector)) {
                Double64Vector so = (Double64Vector)o;
                return VectorIntrinsics.reinterpret(
                    Double64Vector.class,
                    double.class, so.length(),
                    Double64Vector.class,
                    double.class, LENGTH,
                    so, this,
                    (s, v) -> (Double64Vector) s.reshape(v)
                );
            } else if (o.bitSize() == 128 && (o instanceof Double128Vector)) {
                Double128Vector so = (Double128Vector)o;
                return VectorIntrinsics.reinterpret(
                    Double128Vector.class,
                    double.class, so.length(),
                    Double64Vector.class,
                    double.class, LENGTH,
                    so, this,
                    (s, v) -> (Double64Vector) s.reshape(v)
                );
            } else if (o.bitSize() == 256 && (o instanceof Double256Vector)) {
                Double256Vector so = (Double256Vector)o;
                return VectorIntrinsics.reinterpret(
                    Double256Vector.class,
                    double.class, so.length(),
                    Double64Vector.class,
                    double.class, LENGTH,
                    so, this,
                    (s, v) -> (Double64Vector) s.reshape(v)
                );
            } else if (o.bitSize() == 512 && (o instanceof Double512Vector)) {
                Double512Vector so = (Double512Vector)o;
                return VectorIntrinsics.reinterpret(
                    Double512Vector.class,
                    double.class, so.length(),
                    Double64Vector.class,
                    double.class, LENGTH,
                    so, this,
                    (s, v) -> (Double64Vector) s.reshape(v)
                );
            } else if ((o.bitSize() > 0) && (o.bitSize() <= 2048)
                    && (o.bitSize() % 128 == 0) && (o instanceof DoubleMaxVector)) {
                DoubleMaxVector so = (DoubleMaxVector)o;
                return VectorIntrinsics.reinterpret(
                    DoubleMaxVector.class,
                    double.class, so.length(),
                    Double64Vector.class,
                    double.class, LENGTH,
                    so, this,
                    (s, v) -> (Double64Vector) s.reshape(v)
                );
            } else {
                throw new InternalError("Unimplemented size");
            }
        }
    }
}
