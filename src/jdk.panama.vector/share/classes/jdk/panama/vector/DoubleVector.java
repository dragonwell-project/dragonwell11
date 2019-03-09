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

import jdk.internal.vm.annotation.ForceInline;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;


/**
 * A specialized {@link Vector} representing an ordered immutable sequence of
 * {@code double} values.
 */
@SuppressWarnings("cast")
public abstract class DoubleVector extends Vector<Double> {

    DoubleVector() {}

    // Unary operator

    interface FUnOp {
        double apply(int i, double a);
    }

    abstract DoubleVector uOp(FUnOp f);

    abstract DoubleVector uOp(Mask<Double> m, FUnOp f);

    // Binary operator

    interface FBinOp {
        double apply(int i, double a, double b);
    }

    abstract DoubleVector bOp(Vector<Double> v, FBinOp f);

    abstract DoubleVector bOp(Vector<Double> v, Mask<Double> m, FBinOp f);

    // Trinary operator

    interface FTriOp {
        double apply(int i, double a, double b, double c);
    }

    abstract DoubleVector tOp(Vector<Double> v1, Vector<Double> v2, FTriOp f);

    abstract DoubleVector tOp(Vector<Double> v1, Vector<Double> v2, Mask<Double> m, FTriOp f);

    // Reduction operator

    abstract double rOp(double v, FBinOp f);

    // Binary test

    interface FBinTest {
        boolean apply(int i, double a, double b);
    }

    abstract Mask<Double> bTest(Vector<Double> v, FBinTest f);

    // Foreach

    interface FUnCon {
        void apply(int i, double a);
    }

    abstract void forEach(FUnCon f);

    abstract void forEach(Mask<Double> m, FUnCon f);

    //

    @Override
    public abstract DoubleVector add(Vector<Double> v);

    /**
     * Adds this vector to the broadcast of an input scalar.
     * <p>
     * This is a vector binary operation where the primitive addition operation
     * ({@code +}) is applied to lane elements.
     *
     * @param s the input scalar
     * @return the result of adding this vector to the broadcast of an input
     * scalar
     */
    public abstract DoubleVector add(double s);

    @Override
    public abstract DoubleVector add(Vector<Double> v, Mask<Double> m);

    /**
     * Adds this vector to broadcast of an input scalar,
     * selecting lane elements controlled by a mask.
     * <p>
     * This is a vector binary operation where the primitive addition operation
     * ({@code +}) is applied to lane elements.
     *
     * @param s the input scalar
     * @param m the mask controlling lane selection
     * @return the result of adding this vector to the broadcast of an input
     * scalar
     */
    public abstract DoubleVector add(double s, Mask<Double> m);

    @Override
    public abstract DoubleVector sub(Vector<Double> v);

    /**
     * Subtracts the broadcast of an input scalar from this vector.
     * <p>
     * This is a vector binary operation where the primitive subtraction
     * operation ({@code -}) is applied to lane elements.
     *
     * @param s the input scalar
     * @return the result of subtracting the broadcast of an input
     * scalar from this vector
     */
    public abstract DoubleVector sub(double s);

    @Override
    public abstract DoubleVector sub(Vector<Double> v, Mask<Double> m);

    /**
     * Subtracts the broadcast of an input scalar from this vector, selecting
     * lane elements controlled by a mask.
     * <p>
     * This is a vector binary operation where the primitive subtraction
     * operation ({@code -}) is applied to lane elements.
     *
     * @param s the input scalar
     * @param m the mask controlling lane selection
     * @return the result of subtracting the broadcast of an input
     * scalar from this vector
     */
    public abstract DoubleVector sub(double s, Mask<Double> m);

    @Override
    public abstract DoubleVector mul(Vector<Double> v);

    /**
     * Multiplies this vector with the broadcast of an input scalar.
     * <p>
     * This is a vector binary operation where the primitive multiplication
     * operation ({@code *}) is applied to lane elements.
     *
     * @param s the input scalar
     * @return the result of multiplying this vector with the broadcast of an
     * input scalar
     */
    public abstract DoubleVector mul(double s);

    @Override
    public abstract DoubleVector mul(Vector<Double> v, Mask<Double> m);

    /**
     * Multiplies this vector with the broadcast of an input scalar, selecting
     * lane elements controlled by a mask.
     * <p>
     * This is a vector binary operation where the primitive multiplication
     * operation ({@code *}) is applied to lane elements.
     *
     * @param s the input scalar
     * @param m the mask controlling lane selection
     * @return the result of multiplying this vector with the broadcast of an
     * input scalar
     */
    public abstract DoubleVector mul(double s, Mask<Double> m);

    @Override
    public abstract DoubleVector neg();

    @Override
    public abstract DoubleVector neg(Mask<Double> m);

    @Override
    public abstract DoubleVector abs();

    @Override
    public abstract DoubleVector abs(Mask<Double> m);

    @Override
    public abstract DoubleVector min(Vector<Double> v);

    @Override
    public abstract DoubleVector min(Vector<Double> v, Mask<Double> m);

    /**
     * Returns the minimum of this vector and the broadcast of an input scalar.
     * <p>
     * This is a vector binary operation where the operation
     * {@code (a, b) -> a < b ? a : b}  is applied to lane elements.
     *
     * @param s the input scalar
     * @return the minimum of this vector and the broadcast of an input scalar
     */
    public abstract DoubleVector min(double s);

    @Override
    public abstract DoubleVector max(Vector<Double> v);

    @Override
    public abstract DoubleVector max(Vector<Double> v, Mask<Double> m);

    /**
     * Returns the maximum of this vector and the broadcast of an input scalar.
     * <p>
     * This is a vector binary operation where the operation
     * {@code (a, b) -> a > b ? a : b}  is applied to lane elements.
     *
     * @param s the input scalar
     * @return the maximum of this vector and the broadcast of an input scalar
     */
    public abstract DoubleVector max(double s);

    @Override
    public abstract Mask<Double> equal(Vector<Double> v);

    /**
     * Tests if this vector is equal to the broadcast of an input scalar.
     * <p>
     * This is a vector binary test operation where the primitive equals
     * operation ({@code ==}) is applied to lane elements.
     *
     * @param s the input scalar
     * @return the result mask of testing if this vector is equal to the
     * broadcast of an input scalar
     */
    public abstract Mask<Double> equal(double s);

    @Override
    public abstract Mask<Double> notEqual(Vector<Double> v);

    /**
     * Tests if this vector is not equal to the broadcast of an input scalar.
     * <p>
     * This is a vector binary test operation where the primitive not equals
     * operation ({@code !=}) is applied to lane elements.
     *
     * @param s the input scalar
     * @return the result mask of testing if this vector is not equal to the
     * broadcast of an input scalar
     */
    public abstract Mask<Double> notEqual(double s);

    @Override
    public abstract Mask<Double> lessThan(Vector<Double> v);

    /**
     * Tests if this vector is less than the broadcast of an input scalar.
     * <p>
     * This is a vector binary test operation where the primitive less than
     * operation ({@code <}) is applied to lane elements.
     *
     * @param s the input scalar
     * @return the mask result of testing if this vector is less than the
     * broadcast of an input scalar
     */
    public abstract Mask<Double> lessThan(double s);

    @Override
    public abstract Mask<Double> lessThanEq(Vector<Double> v);

    /**
     * Tests if this vector is less or equal to the broadcast of an input scalar.
     * <p>
     * This is a vector binary test operation where the primitive less than
     * or equal to operation ({@code <=}) is applied to lane elements.
     *
     * @param s the input scalar
     * @return the mask result of testing if this vector is less than or equal
     * to the broadcast of an input scalar
     */
    public abstract Mask<Double> lessThanEq(double s);

    @Override
    public abstract Mask<Double> greaterThan(Vector<Double> v);

    /**
     * Tests if this vector is greater than the broadcast of an input scalar.
     * <p>
     * This is a vector binary test operation where the primitive greater than
     * operation ({@code >}) is applied to lane elements.
     *
     * @param s the input scalar
     * @return the mask result of testing if this vector is greater than the
     * broadcast of an input scalar
     */
    public abstract Mask<Double> greaterThan(double s);

    @Override
    public abstract Mask<Double> greaterThanEq(Vector<Double> v);

    /**
     * Tests if this vector is greater than or equal to the broadcast of an
     * input scalar.
     * <p>
     * This is a vector binary test operation where the primitive greater than
     * or equal to operation ({@code >=}) is applied to lane elements.
     *
     * @param s the input scalar
     * @return the mask result of testing if this vector is greater than or
     * equal to the broadcast of an input scalar
     */
    public abstract Mask<Double> greaterThanEq(double s);

    @Override
    public abstract DoubleVector blend(Vector<Double> v, Mask<Double> m);

    /**
     * Blends the lane elements of this vector with those of the broadcast of an
     * input scalar, selecting lanes controlled by a mask.
     * <p>
     * For each lane of the mask, at lane index {@code N}, if the mask lane
     * is set then the lane element at {@code N} from the input vector is
     * selected and placed into the resulting vector at {@code N},
     * otherwise the the lane element at {@code N} from this input vector is
     * selected and placed into the resulting vector at {@code N}.
     *
     * @param s the input scalar
     * @param m the mask controlling lane selection
     * @return the result of blending the lane elements of this vector with
     * those of the broadcast of an input scalar
     */
    public abstract DoubleVector blend(double s, Mask<Double> m);

    @Override
    public abstract DoubleVector rearrange(Vector<Double> v,
                                                      Shuffle<Double> s, Mask<Double> m);

    @Override
    public abstract DoubleVector rearrange(Shuffle<Double> m);

    @Override
    @ForceInline
    public DoubleVector resize(Species<Double> species) {
        return (DoubleVector) species.resize(this);
    }

    @Override
    public abstract DoubleVector rotateEL(int i);

    @Override
    public abstract DoubleVector rotateER(int i);

    @Override
    public abstract DoubleVector shiftEL(int i);

    @Override
    public abstract DoubleVector shiftER(int i);

    /**
     * Divides this vector by an input vector.
     * <p>
     * This is a vector binary operation where the primitive division
     * operation ({@code /}) is applied to lane elements.
     *
     * @param v the input vector
     * @return the result of dividing this vector by the input vector
     */
    public abstract DoubleVector div(Vector<Double> v);

    /**
     * Divides this vector by the broadcast of an input scalar.
     * <p>
     * This is a vector binary operation where the primitive division
     * operation ({@code /}) is applied to lane elements.
     *
     * @param s the input scalar
     * @return the result of dividing this vector by the broadcast of an input
     * scalar
     */
    public abstract DoubleVector div(double s);

    /**
     * Divides this vector by an input vector, selecting lane elements
     * controlled by a mask.
     * <p>
     * This is a vector binary operation where the primitive division
     * operation ({@code /}) is applied to lane elements.
     *
     * @param v the input vector
     * @param m the mask controlling lane selection
     * @return the result of dividing this vector by the input vector
     */
    public abstract DoubleVector div(Vector<Double> v, Mask<Double> m);

    /**
     * Divides this vector by the broadcast of an input scalar, selecting lane
     * elements controlled by a mask.
     * <p>
     * This is a vector binary operation where the primitive division
     * operation ({@code /}) is applied to lane elements.
     *
     * @param s the input scalar
     * @param m the mask controlling lane selection
     * @return the result of dividing this vector by the broadcast of an input
     * scalar
     */
    public abstract DoubleVector div(double s, Mask<Double> m);

    /**
     * Calculates the square root of this vector.
     * <p>
     * This is a vector unary operation where the {@link Math#sqrt} operation
     * is applied to lane elements.
     *
     * @return the square root of this vector
     */
    public abstract DoubleVector sqrt();

    /**
     * Calculates the square root of this vector, selecting lane elements
     * controlled by a mask.
     * <p>
     * This is a vector unary operation where the {@link Math#sqrt} operation
     * is applied to lane elements.
     *
     * @param m the mask controlling lane selection
     * @return the square root of this vector
     */
    public DoubleVector sqrt(Mask<Double> m) {
        return uOp(m, (i, a) -> (double) Math.sqrt((double) a));
    }

    /**
     * Calculates the trigonometric tangent of this vector.
     * <p>
     * This is a vector unary operation with same semantic definition as
     * {@link Math#tan} operation applied to lane elements.
     * The implementation is not required to return same
     * results as {@link Math#tan}, but adheres to rounding, monotonicity,
     * and special case semantics as defined in the {@link Math#tan}
     * specifications. The computed result will be within 1 ulp of the
     * exact result.
     *
     * @return the tangent of this vector
     */
    public DoubleVector tan() {
        return uOp((i, a) -> (double) Math.tan((double) a));
    }

    /**
     * Calculates the trigonometric tangent of this vector, selecting lane
     * elements controlled by a mask.
     * <p>
     * Semantics for rounding, monotonicity, and special cases are
     * described in {@link DoubleVector#tan}
     *
     * @param m the mask controlling lane selection
     * @return the tangent of this vector
     */
    public DoubleVector tan(Mask<Double> m) {
        return uOp(m, (i, a) -> (double) Math.tan((double) a));
    }

    /**
     * Calculates the hyperbolic tangent of this vector.
     * <p>
     * This is a vector unary operation with same semantic definition as
     * {@link Math#tanh} operation applied to lane elements.
     * The implementation is not required to return same
     * results as {@link Math#tanh}, but adheres to rounding, monotonicity,
     * and special case semantics as defined in the {@link Math#tanh}
     * specifications. The computed result will be within 2.5 ulps of the
     * exact result.
     *
     * @return the hyperbolic tangent of this vector
     */
    public DoubleVector tanh() {
        return uOp((i, a) -> (double) Math.tanh((double) a));
    }

    /**
     * Calculates the hyperbolic tangent of this vector, selecting lane elements
     * controlled by a mask.
     * <p>
     * Semantics for rounding, monotonicity, and special cases are
     * described in {@link DoubleVector#tanh}
     *
     * @param m the mask controlling lane selection
     * @return the hyperbolic tangent of this vector
     */
    public DoubleVector tanh(Mask<Double> m) {
        return uOp(m, (i, a) -> (double) Math.tanh((double) a));
    }

    /**
     * Calculates the trigonometric sine of this vector.
     * <p>
     * This is a vector unary operation with same semantic definition as
     * {@link Math#sin} operation applied to lane elements.
     * The implementation is not required to return same
     * results as {@link Math#sin}, but adheres to rounding, monotonicity,
     * and special case semantics as defined in the {@link Math#sin}
     * specifications. The computed result will be within 1 ulp of the
     * exact result.
     *
     * @return the sine of this vector
     */
    public DoubleVector sin() {
        return uOp((i, a) -> (double) Math.sin((double) a));
    }

    /**
     * Calculates the trigonometric sine of this vector, selecting lane elements
     * controlled by a mask.
     * <p>
     * Semantics for rounding, monotonicity, and special cases are
     * described in {@link DoubleVector#sin}
     *
     * @param m the mask controlling lane selection
     * @return the sine of this vector
     */
    public DoubleVector sin(Mask<Double> m) {
        return uOp(m, (i, a) -> (double) Math.sin((double) a));
    }

    /**
     * Calculates the hyperbolic sine of this vector.
     * <p>
     * This is a vector unary operation with same semantic definition as
     * {@link Math#sinh} operation applied to lane elements.
     * The implementation is not required to return same
     * results as  {@link Math#sinh}, but adheres to rounding, monotonicity,
     * and special case semantics as defined in the {@link Math#sinh}
     * specifications. The computed result will be within 2.5 ulps of the
     * exact result.
     *
     * @return the hyperbolic sine of this vector
     */
    public DoubleVector sinh() {
        return uOp((i, a) -> (double) Math.sinh((double) a));
    }

    /**
     * Calculates the hyperbolic sine of this vector, selecting lane elements
     * controlled by a mask.
     * <p>
     * Semantics for rounding, monotonicity, and special cases are
     * described in {@link DoubleVector#sinh}
     *
     * @param m the mask controlling lane selection
     * @return the hyperbolic sine of this vector
     */
    public DoubleVector sinh(Mask<Double> m) {
        return uOp(m, (i, a) -> (double) Math.sinh((double) a));
    }

    /**
     * Calculates the trigonometric cosine of this vector.
     * <p>
     * This is a vector unary operation with same semantic definition as
     * {@link Math#cos} operation applied to lane elements.
     * The implementation is not required to return same
     * results as {@link Math#cos}, but adheres to rounding, monotonicity,
     * and special case semantics as defined in the {@link Math#cos}
     * specifications. The computed result will be within 1 ulp of the
     * exact result.
     *
     * @return the cosine of this vector
     */
    public DoubleVector cos() {
        return uOp((i, a) -> (double) Math.cos((double) a));
    }

    /**
     * Calculates the trigonometric cosine of this vector, selecting lane
     * elements controlled by a mask.
     * <p>
     * Semantics for rounding, monotonicity, and special cases are
     * described in {@link DoubleVector#cos}
     *
     * @param m the mask controlling lane selection
     * @return the cosine of this vector
     */
    public DoubleVector cos(Mask<Double> m) {
        return uOp(m, (i, a) -> (double) Math.cos((double) a));
    }

    /**
     * Calculates the hyperbolic cosine of this vector.
     * <p>
     * This is a vector unary operation with same semantic definition as
     * {@link Math#cosh} operation applied to lane elements.
     * The implementation is not required to return same
     * results as {@link Math#cosh}, but adheres to rounding, monotonicity,
     * and special case semantics as defined in the {@link Math#cosh}
     * specifications. The computed result will be within 2.5 ulps of the
     * exact result.
     *
     * @return the hyperbolic cosine of this vector
     */
    public DoubleVector cosh() {
        return uOp((i, a) -> (double) Math.cosh((double) a));
    }

    /**
     * Calculates the hyperbolic cosine of this vector, selecting lane elements
     * controlled by a mask.
     * <p>
     * Semantics for rounding, monotonicity, and special cases are
     * described in {@link DoubleVector#cosh}
     *
     * @param m the mask controlling lane selection
     * @return the hyperbolic cosine of this vector
     */
    public DoubleVector cosh(Mask<Double> m) {
        return uOp(m, (i, a) -> (double) Math.cosh((double) a));
    }

    /**
     * Calculates the arc sine of this vector.
     * <p>
     * This is a vector unary operation with same semantic definition as
     * {@link Math#asin} operation applied to lane elements.
     * The implementation is not required to return same
     * results as {@link Math#asin}, but adheres to rounding, monotonicity,
     * and special case semantics as defined in the {@link Math#asin}
     * specifications. The computed result will be within 1 ulp of the
     * exact result.
     *
     * @return the arc sine of this vector
     */
    public DoubleVector asin() {
        return uOp((i, a) -> (double) Math.asin((double) a));
    }

    /**
     * Calculates the arc sine of this vector, selecting lane elements
     * controlled by a mask.
     * <p>
     * Semantics for rounding, monotonicity, and special cases are
     * described in {@link DoubleVector#asin}
     *
     * @param m the mask controlling lane selection
     * @return the arc sine of this vector
     */
    public DoubleVector asin(Mask<Double> m) {
        return uOp(m, (i, a) -> (double) Math.asin((double) a));
    }

    /**
     * Calculates the arc cosine of this vector.
     * <p>
     * This is a vector unary operation with same semantic definition as
     * {@link Math#acos} operation applied to lane elements.
     * The implementation is not required to return same
     * results as {@link Math#acos}, but adheres to rounding, monotonicity,
     * and special case semantics as defined in the {@link Math#acos}
     * specifications. The computed result will be within 1 ulp of the
     * exact result.
     *
     * @return the arc cosine of this vector
     */
    public DoubleVector acos() {
        return uOp((i, a) -> (double) Math.acos((double) a));
    }

    /**
     * Calculates the arc cosine of this vector, selecting lane elements
     * controlled by a mask.
     * <p>
     * Semantics for rounding, monotonicity, and special cases are
     * described in {@link DoubleVector#acos}
     *
     * @param m the mask controlling lane selection
     * @return the arc cosine of this vector
     */
    public DoubleVector acos(Mask<Double> m) {
        return uOp(m, (i, a) -> (double) Math.acos((double) a));
    }

    /**
     * Calculates the arc tangent of this vector.
     * <p>
     * This is a vector unary operation with same semantic definition as
     * {@link Math#atan} operation applied to lane elements.
     * The implementation is not required to return same
     * results as {@link Math#atan}, but adheres to rounding, monotonicity,
     * and special case semantics as defined in the {@link Math#atan}
     * specifications. The computed result will be within 1 ulp of the
     * exact result.
     *
     * @return the arc tangent of this vector
     */
    public DoubleVector atan() {
        return uOp((i, a) -> (double) Math.atan((double) a));
    }

    /**
     * Calculates the arc tangent of this vector, selecting lane elements
     * controlled by a mask.
     * <p>
     * Semantics for rounding, monotonicity, and special cases are
     * described in {@link DoubleVector#atan}
     *
     * @param m the mask controlling lane selection
     * @return the arc tangent of this vector
     */
    public DoubleVector atan(Mask<Double> m) {
        return uOp(m, (i, a) -> (double) Math.atan((double) a));
    }

    /**
     * Calculates the arc tangent of this vector divided by an input vector.
     * <p>
     * This is a vector binary operation with same semantic definition as
     * {@link Math#atan2} operation applied to lane elements.
     * The implementation is not required to return same
     * results as {@link Math#atan2}, but adheres to rounding, monotonicity,
     * and special case semantics as defined in the {@link Math#atan2}
     * specifications. The computed result will be within 2 ulps of the
     * exact result.
     *
     * @param v the input vector
     * @return the arc tangent of this vector divided by the input vector
     */
    public DoubleVector atan2(Vector<Double> v) {
        return bOp(v, (i, a, b) -> (double) Math.atan2((double) a, (double) b));
    }

    /**
     * Calculates the arc tangent of this vector divided by the broadcast of an
     * an input scalar.
     * <p>
     * This is a vector binary operation with same semantic definition as
     * {@link Math#atan2} operation applied to lane elements.
     * The implementation is not required to return same
     * results as {@link Math#atan2}, but adheres to rounding, monotonicity,
     * and special case semantics as defined in the {@link Math#atan2}
     * specifications. The computed result will be within 1 ulp of the
     * exact result.
     *
     * @param s the input scalar
     * @return the arc tangent of this vector over the input vector
     */
    public abstract DoubleVector atan2(double s);

    /**
     * Calculates the arc tangent of this vector divided by an input vector,
     * selecting lane elements controlled by a mask.
     * <p>
     * Semantics for rounding, monotonicity, and special cases are
     * described in {@link DoubleVector#atan2}
     *
     * @param v the input vector
     * @param m the mask controlling lane selection
     * @return the arc tangent of this vector divided by the input vector
     */
    public DoubleVector atan2(Vector<Double> v, Mask<Double> m) {
        return bOp(v, m, (i, a, b) -> (double) Math.atan2((double) a, (double) b));
    }

    /**
     * Calculates the arc tangent of this vector divided by the broadcast of an
     * an input scalar, selecting lane elements controlled by a mask.
     * <p>
     * Semantics for rounding, monotonicity, and special cases are
     * described in {@link DoubleVector#atan2}
     *
     * @param s the input scalar
     * @param m the mask controlling lane selection
     * @return the arc tangent of this vector over the input vector
     */
    public abstract DoubleVector atan2(double s, Mask<Double> m);

    /**
     * Calculates the cube root of this vector.
     * <p>
     * This is a vector unary operation with same semantic definition as
     * {@link Math#cbrt} operation applied to lane elements.
     * The implementation is not required to return same
     * results as {@link Math#cbrt}, but adheres to rounding, monotonicity,
     * and special case semantics as defined in the {@link Math#cbrt}
     * specifications. The computed result will be within 1 ulp of the
     * exact result.
     *
     * @return the cube root of this vector
     */
    public DoubleVector cbrt() {
        return uOp((i, a) -> (double) Math.cbrt((double) a));
    }

    /**
     * Calculates the cube root of this vector, selecting lane elements
     * controlled by a mask.
     * <p>
     * Semantics for rounding, monotonicity, and special cases are
     * described in {@link DoubleVector#cbrt}
     *
     * @param m the mask controlling lane selection
     * @return the cube root of this vector
     */
    public DoubleVector cbrt(Mask<Double> m) {
        return uOp(m, (i, a) -> (double) Math.cbrt((double) a));
    }

    /**
     * Calculates the natural logarithm of this vector.
     * <p>
     * This is a vector unary operation with same semantic definition as
     * {@link Math#log} operation applied to lane elements.
     * The implementation is not required to return same
     * results as {@link Math#log}, but adheres to rounding, monotonicity,
     * and special case semantics as defined in the {@link Math#log}
     * specifications. The computed result will be within 1 ulp of the
     * exact result.
     *
     * @return the natural logarithm of this vector
     */
    public DoubleVector log() {
        return uOp((i, a) -> (double) Math.log((double) a));
    }

    /**
     * Calculates the natural logarithm of this vector, selecting lane elements
     * controlled by a mask.
     * <p>
     * Semantics for rounding, monotonicity, and special cases are
     * described in {@link DoubleVector#log}
     *
     * @param m the mask controlling lane selection
     * @return the natural logarithm of this vector
     */
    public DoubleVector log(Mask<Double> m) {
        return uOp(m, (i, a) -> (double) Math.log((double) a));
    }

    /**
     * Calculates the base 10 logarithm of this vector.
     * <p>
     * This is a vector unary operation with same semantic definition as
     * {@link Math#log10} operation applied to lane elements.
     * The implementation is not required to return same
     * results as {@link Math#log10}, but adheres to rounding, monotonicity,
     * and special case semantics as defined in the {@link Math#log10}
     * specifications. The computed result will be within 1 ulp of the
     * exact result.
     *
     * @return the base 10 logarithm of this vector
     */
    public DoubleVector log10() {
        return uOp((i, a) -> (double) Math.log10((double) a));
    }

    /**
     * Calculates the base 10 logarithm of this vector, selecting lane elements
     * controlled by a mask.
     * <p>
     * Semantics for rounding, monotonicity, and special cases are
     * described in {@link DoubleVector#log10}
     *
     * @param m the mask controlling lane selection
     * @return the base 10 logarithm of this vector
     */
    public DoubleVector log10(Mask<Double> m) {
        return uOp(m, (i, a) -> (double) Math.log10((double) a));
    }

    /**
     * Calculates the natural logarithm of the sum of this vector and the
     * broadcast of {@code 1}.
     * <p>
     * This is a vector unary operation with same semantic definition as
     * {@link Math#log1p} operation applied to lane elements.
     * The implementation is not required to return same
     * results as  {@link Math#log1p}, but adheres to rounding, monotonicity,
     * and special case semantics as defined in the {@link Math#log1p}
     * specifications. The computed result will be within 1 ulp of the
     * exact result.
     *
     * @return the natural logarithm of the sum of this vector and the broadcast
     * of {@code 1}
     */
    public DoubleVector log1p() {
        return uOp((i, a) -> (double) Math.log1p((double) a));
    }

    /**
     * Calculates the natural logarithm of the sum of this vector and the
     * broadcast of {@code 1}, selecting lane elements controlled by a mask.
     * <p>
     * Semantics for rounding, monotonicity, and special cases are
     * described in {@link DoubleVector#log1p}
     *
     * @param m the mask controlling lane selection
     * @return the natural logarithm of the sum of this vector and the broadcast
     * of {@code 1}
     */
    public DoubleVector log1p(Mask<Double> m) {
        return uOp(m, (i, a) -> (double) Math.log1p((double) a));
    }

    /**
     * Calculates this vector raised to the power of an input vector.
     * <p>
     * This is a vector binary operation with same semantic definition as
     * {@link Math#pow} operation applied to lane elements.
     * The implementation is not required to return same
     * results as {@link Math#pow}, but adheres to rounding, monotonicity,
     * and special case semantics as defined in the {@link Math#pow}
     * specifications. The computed result will be within 1 ulp of the
     * exact result.
     *
     * @param v the input vector
     * @return this vector raised to the power of an input vector
     */
    public DoubleVector pow(Vector<Double> v) {
        return bOp(v, (i, a, b) -> (double) Math.pow((double) a, (double) b));
    }

    /**
     * Calculates this vector raised to the power of the broadcast of an input
     * scalar.
     * <p>
     * This is a vector binary operation with same semantic definition as
     * {@link Math#pow} operation applied to lane elements.
     * The implementation is not required to return same
     * results as {@link Math#pow}, but adheres to rounding, monotonicity,
     * and special case semantics as defined in the {@link Math#pow}
     * specifications. The computed result will be within 1 ulp of the
     * exact result.
     *
     * @param s the input scalar
     * @return this vector raised to the power of the broadcast of an input
     * scalar.
     */
    public abstract DoubleVector pow(double s);

    /**
     * Calculates this vector raised to the power of an input vector, selecting
     * lane elements controlled by a mask.
     * <p>
     * Semantics for rounding, monotonicity, and special cases are
     * described in {@link DoubleVector#pow}
     *
     * @param v the input vector
     * @param m the mask controlling lane selection
     * @return this vector raised to the power of an input vector
     */
    public DoubleVector pow(Vector<Double> v, Mask<Double> m) {
        return bOp(v, m, (i, a, b) -> (double) Math.pow((double) a, (double) b));
    }

    /**
     * Calculates this vector raised to the power of the broadcast of an input
     * scalar, selecting lane elements controlled by a mask.
     * <p>
     * Semantics for rounding, monotonicity, and special cases are
     * described in {@link DoubleVector#pow}
     *
     * @param s the input scalar
     * @param m the mask controlling lane selection
     * @return this vector raised to the power of the broadcast of an input
     * scalar.
     */
    public abstract DoubleVector pow(double s, Mask<Double> m);

    /**
     * Calculates the broadcast of Euler's number {@code e} raised to the power
     * of this vector.
     * <p>
     * This is a vector unary operation with same semantic definition as
     * {@link Math#exp} operation applied to lane elements.
     * The implementation is not required to return same
     * results as {@link Math#exp}, but adheres to rounding, monotonicity,
     * and special case semantics as defined in the {@link Math#exp}
     * specifications. The computed result will be within 1 ulp of the
     * exact result.
     *
     * @return the broadcast of Euler's number {@code e} raised to the power of
     * this vector
     */
    public DoubleVector exp() {
        return uOp((i, a) -> (double) Math.exp((double) a));
    }

    /**
     * Calculates the broadcast of Euler's number {@code e} raised to the power
     * of this vector, selecting lane elements controlled by a mask.
     * <p>
     * Semantics for rounding, monotonicity, and special cases are
     * described in {@link DoubleVector#exp}
     *
     * @param m the mask controlling lane selection
     * @return the broadcast of Euler's number {@code e} raised to the power of
     * this vector
     */
    public DoubleVector exp(Mask<Double> m) {
        return uOp(m, (i, a) -> (double) Math.exp((double) a));
    }

    /**
     * Calculates the broadcast of Euler's number {@code e} raised to the power
     * of this vector minus the broadcast of {@code -1}.
     * More specifically as if the following (ignoring any differences in
     * numerical accuracy):
     * <pre>{@code
     *   this.exp().sub(this.species().broadcast(1))
     * }</pre>
     * <p>
     * This is a vector unary operation with same semantic definition as
     * {@link Math#expm1} operation applied to lane elements.
     * The implementation is not required to return same
     * results as {@link Math#expm1}, but adheres to rounding, monotonicity,
     * and special case semantics as defined in the {@link Math#expm1}
     * specifications. The computed result will be within 1 ulp of the
     * exact result.
     *
     * @return the broadcast of Euler's number {@code e} raised to the power of
     * this vector minus the broadcast of {@code -1}
     */
    public DoubleVector expm1() {
        return uOp((i, a) -> (double) Math.expm1((double) a));
    }

    /**
     * Calculates the broadcast of Euler's number {@code e} raised to the power
     * of this vector minus the broadcast of {@code -1}, selecting lane elements
     * controlled by a mask
     * More specifically as if the following (ignoring any differences in
     * numerical accuracy):
     * <pre>{@code
     *   this.exp(m).sub(this.species().broadcast(1), m)
     * }</pre>
     * <p>
     * Semantics for rounding, monotonicity, and special cases are
     * described in {@link DoubleVector#expm1}
     *
     * @param m the mask controlling lane selection
     * @return the broadcast of Euler's number {@code e} raised to the power of
     * this vector minus the broadcast of {@code -1}
     */
    public DoubleVector expm1(Mask<Double> m) {
        return uOp(m, (i, a) -> (double) Math.expm1((double) a));
    }

    /**
     * Calculates the product of this vector and a first input vector summed
     * with a second input vector.
     * More specifically as if the following (ignoring any differences in
     * numerical accuracy):
     * <pre>{@code
     *   this.mul(v1).add(v2)
     * }</pre>
     * <p>
     * This is a vector ternary operation where the {@link Math#fma} operation
     * is applied to lane elements.
     *
     * @param v1 the first input vector
     * @param v2 the second input vector
     * @return the product of this vector and the first input vector summed with
     * the second input vector
     */
    public abstract DoubleVector fma(Vector<Double> v1, Vector<Double> v2);

    /**
     * Calculates the product of this vector and the broadcast of a first input
     * scalar summed with the broadcast of a second input scalar.
     * More specifically as if the following:
     * <pre>{@code
     *   this.fma(this.species().broadcast(s1), this.species().broadcast(s2))
     * }</pre>
     * <p>
     * This is a vector ternary operation where the {@link Math#fma} operation
     * is applied to lane elements.
     *
     * @param s1 the first input scalar
     * @param s2 the second input scalar
     * @return the product of this vector and the broadcast of a first input
     * scalar summed with the broadcast of a second input scalar
     */
    public abstract DoubleVector fma(double s1, double s2);

    /**
     * Calculates the product of this vector and a first input vector summed
     * with a second input vector, selecting lane elements controlled by a mask.
     * More specifically as if the following (ignoring any differences in
     * numerical accuracy):
     * <pre>{@code
     *   this.mul(v1, m).add(v2, m)
     * }</pre>
     * <p>
     * This is a vector ternary operation where the {@link Math#fma} operation
     * is applied to lane elements.
     *
     * @param v1 the first input vector
     * @param v2 the second input vector
     * @param m the mask controlling lane selection
     * @return the product of this vector and the first input vector summed with
     * the second input vector
     */
    public DoubleVector fma(Vector<Double> v1, Vector<Double> v2, Mask<Double> m) {
        return tOp(v1, v2, m, (i, a, b, c) -> Math.fma(a, b, c));
    }

    /**
     * Calculates the product of this vector and the broadcast of a first input
     * scalar summed with the broadcast of a second input scalar, selecting lane
     * elements controlled by a mask
     * More specifically as if the following:
     * <pre>{@code
     *   this.fma(this.species().broadcast(s1), this.species().broadcast(s2), m)
     * }</pre>
     * <p>
     * This is a vector ternary operation where the {@link Math#fma} operation
     * is applied to lane elements.
     *
     * @param s1 the first input scalar
     * @param s2 the second input scalar
     * @param m the mask controlling lane selection
     * @return the product of this vector and the broadcast of a first input
     * scalar summed with the broadcast of a second input scalar
     */
    public abstract DoubleVector fma(double s1, double s2, Mask<Double> m);

    /**
     * Calculates square root of the sum of the squares of this vector and an
     * input vector.
     * More specifically as if the following (ignoring any differences in
     * numerical accuracy):
     * <pre>{@code
     *   this.mul(this).add(v.mul(v)).sqrt()
     * }</pre>
     * <p>
     * This is a vector binary operation with same semantic definition as
     * {@link Math#hypot} operation applied to lane elements.
     * The implementation is not required to return same
     * results as {@link Math#hypot}, but adheres to rounding, monotonicity,
     * and special case semantics as defined in the {@link Math#hypot}
     * specifications. The computed result will be within 1 ulp of the
     * exact result.
     *
     * @param v the input vector
     * @return square root of the sum of the squares of this vector and an input
     * vector
     */
    public DoubleVector hypot(Vector<Double> v) {
        return bOp(v, (i, a, b) -> (double) Math.hypot((double) a, (double) b));
    }

    /**
     * Calculates square root of the sum of the squares of this vector and the
     * broadcast of an input scalar.
     * More specifically as if the following (ignoring any differences in
     * numerical accuracy):
     * <pre>{@code
     *   this.mul(this).add(this.species().broadcast(v * v)).sqrt()
     * }</pre>
     * <p>
     * This is a vector binary operation with same semantic definition as
     * {@link Math#hypot} operation applied to lane elements.
     * The implementation is not required to return same
     * results as {@link Math#hypot}, but adheres to rounding, monotonicity,
     * and special case semantics as defined in the {@link Math#hypot}
     * specifications. The computed result will be within 1 ulp of the
     * exact result.
     *
     * @param s the input scalar
     * @return square root of the sum of the squares of this vector and the
     * broadcast of an input scalar
     */
    public abstract DoubleVector hypot(double s);

    /**
     * Calculates square root of the sum of the squares of this vector and an
     * input vector, selecting lane elements controlled by a mask.
     * More specifically as if the following (ignoring any differences in
     * numerical accuracy):
     * <pre>{@code
     *   this.mul(this, m).add(v.mul(v), m).sqrt(m)
     * }</pre>
     * <p>
     * Semantics for rounding, monotonicity, and special cases are
     * described in {@link DoubleVector#hypot}
     *
     * @param v the input vector
     * @param m the mask controlling lane selection
     * @return square root of the sum of the squares of this vector and an input
     * vector
     */
    public DoubleVector hypot(Vector<Double> v, Mask<Double> m) {
        return bOp(v, m, (i, a, b) -> (double) Math.hypot((double) a, (double) b));
    }

    /**
     * Calculates square root of the sum of the squares of this vector and the
     * broadcast of an input scalar, selecting lane elements controlled by a
     * mask.
     * More specifically as if the following (ignoring any differences in
     * numerical accuracy):
     * <pre>{@code
     *   this.mul(this, m).add(this.species().broadcast(v * v), m).sqrt(m)
     * }</pre>
     * <p>
     * Semantics for rounding, monotonicity, and special cases are
     * described in {@link DoubleVector#hypot}
     *
     * @param s the input scalar
     * @param m the mask controlling lane selection
     * @return square root of the sum of the squares of this vector and the
     * broadcast of an input scalar
     */
    public abstract DoubleVector hypot(double s, Mask<Double> m);


    @Override
    public abstract void intoByteArray(byte[] a, int ix);

    @Override
    public abstract void intoByteArray(byte[] a, int ix, Mask<Double> m);

    @Override
    public abstract void intoByteBuffer(ByteBuffer bb, int ix);

    @Override
    public abstract void intoByteBuffer(ByteBuffer bb, int ix, Mask<Double> m);


    // Type specific horizontal reductions

    /**
     * Adds all lane elements of this vector.
     * <p>
     * This is an associative vector reduction operation where the addition
     * operation ({@code +}) is applied to lane elements,
     * and the identity value is {@code 0}.
     *
     * @return the addition of all the lane elements of this vector
     */
    public abstract double addAll();

    /**
     * Adds all lane elements of this vector, selecting lane elements
     * controlled by a mask.
     * <p>
     * This is an associative vector reduction operation where the addition
     * operation ({@code +}) is applied to lane elements,
     * and the identity value is {@code 0}.
     *
     * @param m the mask controlling lane selection
     * @return the addition of all the lane elements of this vector
     */
    public abstract double addAll(Mask<Double> m);

    /**
     * Subtracts all lane elements of this vector.
     * <p>
     * This is an associative vector reduction operation where the subtraction
     * operation ({@code -}) is applied to lane elements,
     * and the identity value is {@code 0}.
     *
     * @return the subtraction of all the lane elements of this vector
     */
    public abstract double subAll();

    /**
     * Subtracts all lane elements of this vector, selecting lane elements
     * controlled by a mask.
     * <p>
     * This is an associative vector reduction operation where the subtraction
     * operation ({@code -}) is applied to lane elements,
     * and the identity value is {@code 0}.
     *
     * @param m the mask controlling lane selection
     * @return the subtraction of all the lane elements of this vector
     */
    public abstract double subAll(Mask<Double> m);

    /**
     * Multiplies all lane elements of this vector.
     * <p>
     * This is an associative vector reduction operation where the
     * multiplication operation ({@code *}) is applied to lane elements,
     * and the identity value is {@code 1}.
     *
     * @return the multiplication of all the lane elements of this vector
     */
    public abstract double mulAll();

    /**
     * Multiplies all lane elements of this vector, selecting lane elements
     * controlled by a mask.
     * <p>
     * This is an associative vector reduction operation where the
     * multiplication operation ({@code *}) is applied to lane elements,
     * and the identity value is {@code 1}.
     *
     * @param m the mask controlling lane selection
     * @return the multiplication of all the lane elements of this vector
     */
    public abstract double mulAll(Mask<Double> m);

    /**
     * Returns the minimum lane element of this vector.
     * <p>
     * This is an associative vector reduction operation where the operation
     * {@code (a, b) -> a > b ? b : a} is applied to lane elements,
     * and the identity value is {@link Double#MAX_VALUE}.
     *
     * @return the minimum lane element of this vector
     */
    public abstract double minAll();

    /**
     * Returns the minimum lane element of this vector, selecting lane elements
     * controlled by a mask.
     * <p>
     * This is an associative vector reduction operation where the operation
     * {@code (a, b) -> a > b ? b : a} is applied to lane elements,
     * and the identity value is {@link Double#MAX_VALUE}.
     *
     * @param m the mask controlling lane selection
     * @return the minimum lane element of this vector
     */
    public abstract double minAll(Mask<Double> m);

    /**
     * Returns the maximum lane element of this vector.
     * <p>
     * This is an associative vector reduction operation where the operation
     * {@code (a, b) -> a < b ? b : a} is applied to lane elements,
     * and the identity value is {@link Double#MIN_VALUE}.
     *
     * @return the maximum lane element of this vector
     */
    public abstract double maxAll();

    /**
     * Returns the maximum lane element of this vector, selecting lane elements
     * controlled by a mask.
     * <p>
     * This is an associative vector reduction operation where the operation
     * {@code (a, b) -> a < b ? b : a} is applied to lane elements,
     * and the identity value is {@link Double#MIN_VALUE}.
     *
     * @param m the mask controlling lane selection
     * @return the maximum lane element of this vector
     */
    public abstract double maxAll(Mask<Double> m);


    // Type specific accessors

    /**
     * Gets the lane element at lane index {@code i}
     *
     * @param i the lane index
     * @return the lane element at lane index {@code i}
     * @throws IllegalArgumentException if the index is is out of range
     * ({@code < 0 || >= length()})
     */
    public abstract double get(int i);

    /**
     * Replaces the lane element of this vector at lane index {@code i} with
     * value {@code e}.
     * <p>
     * This is a cross-lane operation and behaves as if it returns the result
     * of blending this vector with an input vector that is the result of
     * broadcasting {@code e} and a mask that has only one lane set at lane
     * index {@code i}.
     *
     * @param i the lane index of the lane element to be replaced
     * @param e the value to be placed
     * @return the result of replacing the lane element of this vector at lane
     * index {@code i} with value {@code e}.
     * @throws IllegalArgumentException if the index is is out of range
     * ({@code < 0 || >= length()})
     */
    public abstract DoubleVector with(int i, double e);

    // Type specific extractors

    /**
     * Returns an array containing the lane elements of this vector.
     * <p>
     * This method behaves as if it {@link #intoArray(double[], int)} stores}
     * this vector into an allocated array and returns the array as follows:
     * <pre>{@code
     *   double[] a = new double[this.length()];
     *   this.intoArray(a, 0);
     *   return a;
     * }</pre>
     *
     * @return an array containing the the lane elements of this vector
     */
    @ForceInline
    public final double[] toArray() {
        double[] a = new double[species().length()];
        intoArray(a, 0);
        return a;
    }

    /**
     * Stores this vector into an array starting at offset.
     * <p>
     * For each vector lane, where {@code N} is the vector lane index,
     * the lane element at index {@code N} is stored into the array at index
     * {@code i + N}.
     *
     * @param a the array
     * @param i the offset into the array
     * @throws IndexOutOfBoundsException if {@code i < 0}, or
     * {@code i > a.length - this.length()}
     */
    public abstract void intoArray(double[] a, int i);

    /**
     * Stores this vector into an array starting at offset and using a mask.
     * <p>
     * For each vector lane, where {@code N} is the vector lane index,
     * if the mask lane at index {@code N} is set then the lane element at
     * index {@code N} is stored into the array index {@code i + N}.
     *
     * @param a the array
     * @param i the offset into the array
     * @param m the mask
     * @throws IndexOutOfBoundsException if {@code i < 0}, or
     * for any vector lane index {@code N} where the mask at lane {@code N}
     * is set {@code i >= a.length - N}
     */
    public abstract void intoArray(double[] a, int i, Mask<Double> m);

    /**
     * Stores this vector into an array using indexes obtained from an index
     * map.
     * <p>
     * For each vector lane, where {@code N} is the vector lane index, the
     * lane element at index {@code N} is stored into the array at index
     * {@code i + indexMap[j + N]}.
     *
     * @param a the array
     * @param i the offset into the array, may be negative if relative
     * indexes in the index map compensate to produce a value within the
     * array bounds
     * @param indexMap the index map
     * @param j the offset into the index map
     * @throws IndexOutOfBoundsException if {@code j < 0}, or
     * {@code j > indexMap.length - this.length()},
     * or for any vector lane index {@code N} the result of
     * {@code i + indexMap[j + N]} is {@code < 0} or {@code >= a.length}
     */
    public abstract void intoArray(double[] a, int i, int[] indexMap, int j);

    /**
     * Stores this vector into an array using indexes obtained from an index
     * map and using a mask.
     * <p>
     * For each vector lane, where {@code N} is the vector lane index,
     * if the mask lane at index {@code N} is set then the lane element at
     * index {@code N} is stored into the array at index
     * {@code i + indexMap[j + N]}.
     *
     * @param a the array
     * @param i the offset into the array, may be negative if relative
     * indexes in the index map compensate to produce a value within the
     * array bounds
     * @param m the mask
     * @param indexMap the index map
     * @param j the offset into the index map
     * @throws IndexOutOfBoundsException if {@code j < 0}, or
     * {@code j > indexMap.length - this.length()},
     * or for any vector lane index {@code N} where the mask at lane
     * {@code N} is set the result of {@code i + indexMap[j + N]} is
     * {@code < 0} or {@code >= a.length}
     */
    public abstract void intoArray(double[] a, int i, Mask<Double> m, int[] indexMap, int j);
    // Species

    @Override
    public abstract DoubleSpecies species();

    /**
     * A specialized factory for creating {@link DoubleVector} value of the same
     * shape, and a {@link Mask} and {@link Shuffle} values of the same shape
     * and {@code int} element type.
     */
    public static abstract class DoubleSpecies extends Vector.Species<Double> {
        interface FOp {
            double apply(int i);
        }

        abstract DoubleVector op(FOp f);

        abstract DoubleVector op(Mask<Double> m, FOp f);

        interface FOpm {
            boolean apply(int i);
        }

        abstract Mask<Double> opm(FOpm f);

        // Factories

        @Override
        public abstract DoubleVector zero();

        /**
         * Returns a vector where all lane elements are set to the primitive
         * value {@code e}.
         *
         * @param e the value
         * @return a vector of vector where all lane elements are set to
         * the primitive value {@code e}
         */
        public abstract DoubleVector broadcast(double e);

        /**
         * Returns a vector where the first lane element is set to the primtive
         * value {@code e}, all other lane elements are set to the default
         * value.
         *
         * @param e the value
         * @return a vector where the first lane element is set to the primitive
         * value {@code e}
         */
        @ForceInline
        public final DoubleVector single(double e) {
            return zero().with(0, e);
        }

        /**
         * Returns a vector where each lane element is set to a randomly
         * generated primitive value.
         *
         * The semantics are equivalent to calling
         * {@link ThreadLocalRandom#nextDouble }
         *
         * @return a vector where each lane elements is set to a randomly
         * generated primitive value
         */
        public DoubleVector random() {
            ThreadLocalRandom r = ThreadLocalRandom.current();
            return op(i -> r.nextDouble());
        }

        /**
         * Returns a vector where each lane element is set to a given
         * primitive value.
         * <p>
         * For each vector lane, where {@code N} is the vector lane index, the
         * the primitive value at index {@code N} is placed into the resulting
         * vector at lane index {@code N}.
         *
         * @param es the given primitive values
         * @return a vector where each lane element is set to a given primitive
         * value
         * @throws IndexOutOfBoundsException if {@code es.length < this.length()}
         */
        public abstract DoubleVector scalars(double... es);

        /**
         * Loads a vector from an array starting at offset.
         * <p>
         * For each vector lane, where {@code N} is the vector lane index, the
         * array element at index {@code i + N} is placed into the
         * resulting vector at lane index {@code N}.
         *
         * @param a the array
         * @param i the offset into the array
         * @return the vector loaded from an array
         * @throws IndexOutOfBoundsException if {@code i < 0}, or
         * {@code i > a.length - this.length()}
         */
        public abstract DoubleVector fromArray(double[] a, int i);

        /**
         * Loads a vector from an array starting at offset and using a mask.
         * <p>
         * For each vector lane, where {@code N} is the vector lane index,
         * if the mask lane at index {@code N} is set then the array element at
         * index {@code i + N} is placed into the resulting vector at lane index
         * {@code N}, otherwise the default element value is placed into the
         * resulting vector at lane index {@code N}.
         *
         * @param a the array
         * @param i the offset into the array
         * @param m the mask
         * @return the vector loaded from an array
         * @throws IndexOutOfBoundsException if {@code i < 0}, or
         * for any vector lane index {@code N} where the mask at lane {@code N}
         * is set {@code i > a.length - N}
         */
        public abstract DoubleVector fromArray(double[] a, int i, Mask<Double> m);

        /**
         * Loads a vector from an array using indexes obtained from an index
         * map.
         * <p>
         * For each vector lane, where {@code N} is the vector lane index, the
         * array element at index {@code i + indexMap[j + N]} is placed into the
         * resulting vector at lane index {@code N}.
         *
         * @param a the array
         * @param i the offset into the array, may be negative if relative
         * indexes in the index map compensate to produce a value within the
         * array bounds
         * @param indexMap the index map
         * @param j the offset into the index map
         * @return the vector loaded from an array
         * @throws IndexOutOfBoundsException if {@code j < 0}, or
         * {@code j > indexMap.length - this.length()},
         * or for any vector lane index {@code N} the result of
         * {@code i + indexMap[j + N]} is {@code < 0} or {@code >= a.length}
         */
        public abstract DoubleVector fromArray(double[] a, int i, int[] indexMap, int j);
        /**
         * Loads a vector from an array using indexes obtained from an index
         * map and using a mask.
         * <p>
         * For each vector lane, where {@code N} is the vector lane index,
         * if the mask lane at index {@code N} is set then the array element at
         * index {@code i + indexMap[j + N]} is placed into the resulting vector
         * at lane index {@code N}.
         *
         * @param a the array
         * @param i the offset into the array, may be negative if relative
         * indexes in the index map compensate to produce a value within the
         * array bounds
         * @param indexMap the index map
         * @param j the offset into the index map
         * @return the vector loaded from an array
         * @throws IndexOutOfBoundsException if {@code j < 0}, or
         * {@code j > indexMap.length - this.length()},
         * or for any vector lane index {@code N} where the mask at lane
         * {@code N} is set the result of {@code i + indexMap[j + N]} is
         * {@code < 0} or {@code >= a.length}
         */
        public abstract DoubleVector fromArray(double[] a, int i, Mask<Double> m, int[] indexMap, int j);

        @Override
        public abstract DoubleVector fromByteArray(byte[] a, int ix);

        @Override
        public abstract DoubleVector fromByteArray(byte[] a, int ix, Mask<Double> m);

        @Override
        public abstract DoubleVector fromByteBuffer(ByteBuffer bb, int ix);

        @Override
        public abstract DoubleVector fromByteBuffer(ByteBuffer bb, int ix, Mask<Double> m);

        @Override
        public <F> DoubleVector reshape(Vector<F> o) {
            int blen = Math.max(o.species().bitSize(), bitSize()) / Byte.SIZE;
            ByteBuffer bb = ByteBuffer.allocate(blen).order(ByteOrder.nativeOrder());
            o.intoByteBuffer(bb, 0);
            return fromByteBuffer(bb, 0);
        }

        @Override
        public abstract <F> DoubleVector rebracket(Vector<F> o);

        @Override
        public abstract DoubleVector resize(Vector<Double> o);

        @Override
        public abstract <F> DoubleVector cast(Vector<F> v);

    }

    /**
     * Finds the preferred species for an element type of {@code double}.
     * <p>
     * A preferred species is a species chosen by the platform that has a
     * shape of maximal bit size.  A preferred species for different element
     * types will have the same shape, and therefore vectors, masks, and
     * shuffles created from such species will be shape compatible.
     *
     * @return the preferred species for an element type of {@code double}
     */
    @SuppressWarnings("unchecked")
    public static DoubleSpecies preferredSpecies() {
        return (DoubleSpecies) Vector.preferredSpecies(double.class);
    }

    /**
     * Finds a species for an element type of {@code double} and shape.
     *
     * @param s the shape
     * @return a species for an element type of {@code double} and shape
     * @throws IllegalArgumentException if no such species exists for the shape
     */
    @SuppressWarnings("unchecked")
    public static DoubleSpecies species(Vector.Shape s) {
        Objects.requireNonNull(s);
        if (s == Shape.S_64_BIT) {
            return Double64Vector.SPECIES;
        } else if (s == Shape.S_128_BIT) {
            return Double128Vector.SPECIES;
        } else if (s == Shape.S_256_BIT) {
            return Double256Vector.SPECIES;
        } else if (s == Shape.S_512_BIT) {
            return Double512Vector.SPECIES;
        } else if (s == Shape.S_Max_BIT) {
            return DoubleMaxVector.SPECIES;
        } else {
            throw new IllegalArgumentException("Bad shape: " + s);
        }
    }
}
