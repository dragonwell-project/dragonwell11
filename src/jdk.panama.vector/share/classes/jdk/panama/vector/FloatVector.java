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
 * {@code float} values.
 */
@SuppressWarnings("cast")
public abstract class FloatVector extends Vector<Float> {

    FloatVector() {}

    // Unary operator

    interface FUnOp {
        float apply(int i, float a);
    }

    abstract FloatVector uOp(FUnOp f);

    abstract FloatVector uOp(Mask<Float> m, FUnOp f);

    // Binary operator

    interface FBinOp {
        float apply(int i, float a, float b);
    }

    abstract FloatVector bOp(Vector<Float> v, FBinOp f);

    abstract FloatVector bOp(Vector<Float> v, Mask<Float> m, FBinOp f);

    // Trinary operator

    interface FTriOp {
        float apply(int i, float a, float b, float c);
    }

    abstract FloatVector tOp(Vector<Float> v1, Vector<Float> v2, FTriOp f);

    abstract FloatVector tOp(Vector<Float> v1, Vector<Float> v2, Mask<Float> m, FTriOp f);

    // Reduction operator

    abstract float rOp(float v, FBinOp f);

    // Binary test

    interface FBinTest {
        boolean apply(int i, float a, float b);
    }

    abstract Mask<Float> bTest(Vector<Float> v, FBinTest f);

    // Foreach

    interface FUnCon {
        void apply(int i, float a);
    }

    abstract void forEach(FUnCon f);

    abstract void forEach(Mask<Float> m, FUnCon f);

    //

    @Override
    public abstract FloatVector add(Vector<Float> v);

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
    public abstract FloatVector add(float s);

    @Override
    public abstract FloatVector add(Vector<Float> v, Mask<Float> m);

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
    public abstract FloatVector add(float s, Mask<Float> m);

    @Override
    public abstract FloatVector sub(Vector<Float> v);

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
    public abstract FloatVector sub(float s);

    @Override
    public abstract FloatVector sub(Vector<Float> v, Mask<Float> m);

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
    public abstract FloatVector sub(float s, Mask<Float> m);

    @Override
    public abstract FloatVector mul(Vector<Float> v);

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
    public abstract FloatVector mul(float s);

    @Override
    public abstract FloatVector mul(Vector<Float> v, Mask<Float> m);

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
    public abstract FloatVector mul(float s, Mask<Float> m);

    @Override
    public abstract FloatVector neg();

    @Override
    public abstract FloatVector neg(Mask<Float> m);

    @Override
    public abstract FloatVector abs();

    @Override
    public abstract FloatVector abs(Mask<Float> m);

    @Override
    public abstract FloatVector min(Vector<Float> v);

    @Override
    public abstract FloatVector min(Vector<Float> v, Mask<Float> m);

    /**
     * Returns the minimum of this vector and the broadcast of an input scalar.
     * <p>
     * This is a vector binary operation where the operation
     * {@code (a, b) -> a < b ? a : b}  is applied to lane elements.
     *
     * @param s the input scalar
     * @return the minimum of this vector and the broadcast of an input scalar
     */
    public abstract FloatVector min(float s);

    @Override
    public abstract FloatVector max(Vector<Float> v);

    @Override
    public abstract FloatVector max(Vector<Float> v, Mask<Float> m);

    /**
     * Returns the maximum of this vector and the broadcast of an input scalar.
     * <p>
     * This is a vector binary operation where the operation
     * {@code (a, b) -> a > b ? a : b}  is applied to lane elements.
     *
     * @param s the input scalar
     * @return the maximum of this vector and the broadcast of an input scalar
     */
    public abstract FloatVector max(float s);

    @Override
    public abstract Mask<Float> equal(Vector<Float> v);

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
    public abstract Mask<Float> equal(float s);

    @Override
    public abstract Mask<Float> notEqual(Vector<Float> v);

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
    public abstract Mask<Float> notEqual(float s);

    @Override
    public abstract Mask<Float> lessThan(Vector<Float> v);

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
    public abstract Mask<Float> lessThan(float s);

    @Override
    public abstract Mask<Float> lessThanEq(Vector<Float> v);

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
    public abstract Mask<Float> lessThanEq(float s);

    @Override
    public abstract Mask<Float> greaterThan(Vector<Float> v);

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
    public abstract Mask<Float> greaterThan(float s);

    @Override
    public abstract Mask<Float> greaterThanEq(Vector<Float> v);

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
    public abstract Mask<Float> greaterThanEq(float s);

    @Override
    public abstract FloatVector blend(Vector<Float> v, Mask<Float> m);

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
    public abstract FloatVector blend(float s, Mask<Float> m);

    @Override
    public abstract FloatVector rearrange(Vector<Float> v,
                                                      Shuffle<Float> s, Mask<Float> m);

    @Override
    public abstract FloatVector rearrange(Shuffle<Float> m);

    @Override
    @ForceInline
    public FloatVector resize(Species<Float> species) {
        return (FloatVector) species.resize(this);
    }

    @Override
    public abstract FloatVector rotateEL(int i);

    @Override
    public abstract FloatVector rotateER(int i);

    @Override
    public abstract FloatVector shiftEL(int i);

    @Override
    public abstract FloatVector shiftER(int i);

    /**
     * Divides this vector by an input vector.
     * <p>
     * This is a vector binary operation where the primitive division
     * operation ({@code /}) is applied to lane elements.
     *
     * @param v the input vector
     * @return the result of dividing this vector by the input vector
     */
    public abstract FloatVector div(Vector<Float> v);

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
    public abstract FloatVector div(float s);

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
    public abstract FloatVector div(Vector<Float> v, Mask<Float> m);

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
    public abstract FloatVector div(float s, Mask<Float> m);

    /**
     * Calculates the square root of this vector.
     * <p>
     * This is a vector unary operation where the {@link Math#sqrt} operation
     * is applied to lane elements.
     *
     * @return the square root of this vector
     */
    public abstract FloatVector sqrt();

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
    public FloatVector sqrt(Mask<Float> m) {
        return uOp(m, (i, a) -> (float) Math.sqrt((double) a));
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
    public FloatVector tan() {
        return uOp((i, a) -> (float) Math.tan((double) a));
    }

    /**
     * Calculates the trigonometric tangent of this vector, selecting lane
     * elements controlled by a mask.
     * <p>
     * Semantics for rounding, monotonicity, and special cases are
     * described in {@link FloatVector#tan}
     *
     * @param m the mask controlling lane selection
     * @return the tangent of this vector
     */
    public FloatVector tan(Mask<Float> m) {
        return uOp(m, (i, a) -> (float) Math.tan((double) a));
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
    public FloatVector tanh() {
        return uOp((i, a) -> (float) Math.tanh((double) a));
    }

    /**
     * Calculates the hyperbolic tangent of this vector, selecting lane elements
     * controlled by a mask.
     * <p>
     * Semantics for rounding, monotonicity, and special cases are
     * described in {@link FloatVector#tanh}
     *
     * @param m the mask controlling lane selection
     * @return the hyperbolic tangent of this vector
     */
    public FloatVector tanh(Mask<Float> m) {
        return uOp(m, (i, a) -> (float) Math.tanh((double) a));
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
    public FloatVector sin() {
        return uOp((i, a) -> (float) Math.sin((double) a));
    }

    /**
     * Calculates the trigonometric sine of this vector, selecting lane elements
     * controlled by a mask.
     * <p>
     * Semantics for rounding, monotonicity, and special cases are
     * described in {@link FloatVector#sin}
     *
     * @param m the mask controlling lane selection
     * @return the sine of this vector
     */
    public FloatVector sin(Mask<Float> m) {
        return uOp(m, (i, a) -> (float) Math.sin((double) a));
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
    public FloatVector sinh() {
        return uOp((i, a) -> (float) Math.sinh((double) a));
    }

    /**
     * Calculates the hyperbolic sine of this vector, selecting lane elements
     * controlled by a mask.
     * <p>
     * Semantics for rounding, monotonicity, and special cases are
     * described in {@link FloatVector#sinh}
     *
     * @param m the mask controlling lane selection
     * @return the hyperbolic sine of this vector
     */
    public FloatVector sinh(Mask<Float> m) {
        return uOp(m, (i, a) -> (float) Math.sinh((double) a));
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
    public FloatVector cos() {
        return uOp((i, a) -> (float) Math.cos((double) a));
    }

    /**
     * Calculates the trigonometric cosine of this vector, selecting lane
     * elements controlled by a mask.
     * <p>
     * Semantics for rounding, monotonicity, and special cases are
     * described in {@link FloatVector#cos}
     *
     * @param m the mask controlling lane selection
     * @return the cosine of this vector
     */
    public FloatVector cos(Mask<Float> m) {
        return uOp(m, (i, a) -> (float) Math.cos((double) a));
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
    public FloatVector cosh() {
        return uOp((i, a) -> (float) Math.cosh((double) a));
    }

    /**
     * Calculates the hyperbolic cosine of this vector, selecting lane elements
     * controlled by a mask.
     * <p>
     * Semantics for rounding, monotonicity, and special cases are
     * described in {@link FloatVector#cosh}
     *
     * @param m the mask controlling lane selection
     * @return the hyperbolic cosine of this vector
     */
    public FloatVector cosh(Mask<Float> m) {
        return uOp(m, (i, a) -> (float) Math.cosh((double) a));
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
    public FloatVector asin() {
        return uOp((i, a) -> (float) Math.asin((double) a));
    }

    /**
     * Calculates the arc sine of this vector, selecting lane elements
     * controlled by a mask.
     * <p>
     * Semantics for rounding, monotonicity, and special cases are
     * described in {@link FloatVector#asin}
     *
     * @param m the mask controlling lane selection
     * @return the arc sine of this vector
     */
    public FloatVector asin(Mask<Float> m) {
        return uOp(m, (i, a) -> (float) Math.asin((double) a));
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
    public FloatVector acos() {
        return uOp((i, a) -> (float) Math.acos((double) a));
    }

    /**
     * Calculates the arc cosine of this vector, selecting lane elements
     * controlled by a mask.
     * <p>
     * Semantics for rounding, monotonicity, and special cases are
     * described in {@link FloatVector#acos}
     *
     * @param m the mask controlling lane selection
     * @return the arc cosine of this vector
     */
    public FloatVector acos(Mask<Float> m) {
        return uOp(m, (i, a) -> (float) Math.acos((double) a));
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
    public FloatVector atan() {
        return uOp((i, a) -> (float) Math.atan((double) a));
    }

    /**
     * Calculates the arc tangent of this vector, selecting lane elements
     * controlled by a mask.
     * <p>
     * Semantics for rounding, monotonicity, and special cases are
     * described in {@link FloatVector#atan}
     *
     * @param m the mask controlling lane selection
     * @return the arc tangent of this vector
     */
    public FloatVector atan(Mask<Float> m) {
        return uOp(m, (i, a) -> (float) Math.atan((double) a));
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
    public FloatVector atan2(Vector<Float> v) {
        return bOp(v, (i, a, b) -> (float) Math.atan2((double) a, (double) b));
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
    public abstract FloatVector atan2(float s);

    /**
     * Calculates the arc tangent of this vector divided by an input vector,
     * selecting lane elements controlled by a mask.
     * <p>
     * Semantics for rounding, monotonicity, and special cases are
     * described in {@link FloatVector#atan2}
     *
     * @param v the input vector
     * @param m the mask controlling lane selection
     * @return the arc tangent of this vector divided by the input vector
     */
    public FloatVector atan2(Vector<Float> v, Mask<Float> m) {
        return bOp(v, m, (i, a, b) -> (float) Math.atan2((double) a, (double) b));
    }

    /**
     * Calculates the arc tangent of this vector divided by the broadcast of an
     * an input scalar, selecting lane elements controlled by a mask.
     * <p>
     * Semantics for rounding, monotonicity, and special cases are
     * described in {@link FloatVector#atan2}
     *
     * @param s the input scalar
     * @param m the mask controlling lane selection
     * @return the arc tangent of this vector over the input vector
     */
    public abstract FloatVector atan2(float s, Mask<Float> m);

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
    public FloatVector cbrt() {
        return uOp((i, a) -> (float) Math.cbrt((double) a));
    }

    /**
     * Calculates the cube root of this vector, selecting lane elements
     * controlled by a mask.
     * <p>
     * Semantics for rounding, monotonicity, and special cases are
     * described in {@link FloatVector#cbrt}
     *
     * @param m the mask controlling lane selection
     * @return the cube root of this vector
     */
    public FloatVector cbrt(Mask<Float> m) {
        return uOp(m, (i, a) -> (float) Math.cbrt((double) a));
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
    public FloatVector log() {
        return uOp((i, a) -> (float) Math.log((double) a));
    }

    /**
     * Calculates the natural logarithm of this vector, selecting lane elements
     * controlled by a mask.
     * <p>
     * Semantics for rounding, monotonicity, and special cases are
     * described in {@link FloatVector#log}
     *
     * @param m the mask controlling lane selection
     * @return the natural logarithm of this vector
     */
    public FloatVector log(Mask<Float> m) {
        return uOp(m, (i, a) -> (float) Math.log((double) a));
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
    public FloatVector log10() {
        return uOp((i, a) -> (float) Math.log10((double) a));
    }

    /**
     * Calculates the base 10 logarithm of this vector, selecting lane elements
     * controlled by a mask.
     * <p>
     * Semantics for rounding, monotonicity, and special cases are
     * described in {@link FloatVector#log10}
     *
     * @param m the mask controlling lane selection
     * @return the base 10 logarithm of this vector
     */
    public FloatVector log10(Mask<Float> m) {
        return uOp(m, (i, a) -> (float) Math.log10((double) a));
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
    public FloatVector log1p() {
        return uOp((i, a) -> (float) Math.log1p((double) a));
    }

    /**
     * Calculates the natural logarithm of the sum of this vector and the
     * broadcast of {@code 1}, selecting lane elements controlled by a mask.
     * <p>
     * Semantics for rounding, monotonicity, and special cases are
     * described in {@link FloatVector#log1p}
     *
     * @param m the mask controlling lane selection
     * @return the natural logarithm of the sum of this vector and the broadcast
     * of {@code 1}
     */
    public FloatVector log1p(Mask<Float> m) {
        return uOp(m, (i, a) -> (float) Math.log1p((double) a));
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
    public FloatVector pow(Vector<Float> v) {
        return bOp(v, (i, a, b) -> (float) Math.pow((double) a, (double) b));
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
    public abstract FloatVector pow(float s);

    /**
     * Calculates this vector raised to the power of an input vector, selecting
     * lane elements controlled by a mask.
     * <p>
     * Semantics for rounding, monotonicity, and special cases are
     * described in {@link FloatVector#pow}
     *
     * @param v the input vector
     * @param m the mask controlling lane selection
     * @return this vector raised to the power of an input vector
     */
    public FloatVector pow(Vector<Float> v, Mask<Float> m) {
        return bOp(v, m, (i, a, b) -> (float) Math.pow((double) a, (double) b));
    }

    /**
     * Calculates this vector raised to the power of the broadcast of an input
     * scalar, selecting lane elements controlled by a mask.
     * <p>
     * Semantics for rounding, monotonicity, and special cases are
     * described in {@link FloatVector#pow}
     *
     * @param s the input scalar
     * @param m the mask controlling lane selection
     * @return this vector raised to the power of the broadcast of an input
     * scalar.
     */
    public abstract FloatVector pow(float s, Mask<Float> m);

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
    public FloatVector exp() {
        return uOp((i, a) -> (float) Math.exp((double) a));
    }

    /**
     * Calculates the broadcast of Euler's number {@code e} raised to the power
     * of this vector, selecting lane elements controlled by a mask.
     * <p>
     * Semantics for rounding, monotonicity, and special cases are
     * described in {@link FloatVector#exp}
     *
     * @param m the mask controlling lane selection
     * @return the broadcast of Euler's number {@code e} raised to the power of
     * this vector
     */
    public FloatVector exp(Mask<Float> m) {
        return uOp(m, (i, a) -> (float) Math.exp((double) a));
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
    public FloatVector expm1() {
        return uOp((i, a) -> (float) Math.expm1((double) a));
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
     * described in {@link FloatVector#expm1}
     *
     * @param m the mask controlling lane selection
     * @return the broadcast of Euler's number {@code e} raised to the power of
     * this vector minus the broadcast of {@code -1}
     */
    public FloatVector expm1(Mask<Float> m) {
        return uOp(m, (i, a) -> (float) Math.expm1((double) a));
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
    public abstract FloatVector fma(Vector<Float> v1, Vector<Float> v2);

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
    public abstract FloatVector fma(float s1, float s2);

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
    public FloatVector fma(Vector<Float> v1, Vector<Float> v2, Mask<Float> m) {
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
    public abstract FloatVector fma(float s1, float s2, Mask<Float> m);

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
    public FloatVector hypot(Vector<Float> v) {
        return bOp(v, (i, a, b) -> (float) Math.hypot((double) a, (double) b));
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
    public abstract FloatVector hypot(float s);

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
     * described in {@link FloatVector#hypot}
     *
     * @param v the input vector
     * @param m the mask controlling lane selection
     * @return square root of the sum of the squares of this vector and an input
     * vector
     */
    public FloatVector hypot(Vector<Float> v, Mask<Float> m) {
        return bOp(v, m, (i, a, b) -> (float) Math.hypot((double) a, (double) b));
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
     * described in {@link FloatVector#hypot}
     *
     * @param s the input scalar
     * @param m the mask controlling lane selection
     * @return square root of the sum of the squares of this vector and the
     * broadcast of an input scalar
     */
    public abstract FloatVector hypot(float s, Mask<Float> m);


    @Override
    public abstract void intoByteArray(byte[] a, int ix);

    @Override
    public abstract void intoByteArray(byte[] a, int ix, Mask<Float> m);

    @Override
    public abstract void intoByteBuffer(ByteBuffer bb, int ix);

    @Override
    public abstract void intoByteBuffer(ByteBuffer bb, int ix, Mask<Float> m);


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
    public abstract float addAll();

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
    public abstract float addAll(Mask<Float> m);

    /**
     * Subtracts all lane elements of this vector.
     * <p>
     * This is an associative vector reduction operation where the subtraction
     * operation ({@code -}) is applied to lane elements,
     * and the identity value is {@code 0}.
     *
     * @return the subtraction of all the lane elements of this vector
     */
    public abstract float subAll();

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
    public abstract float subAll(Mask<Float> m);

    /**
     * Multiplies all lane elements of this vector.
     * <p>
     * This is an associative vector reduction operation where the
     * multiplication operation ({@code *}) is applied to lane elements,
     * and the identity value is {@code 1}.
     *
     * @return the multiplication of all the lane elements of this vector
     */
    public abstract float mulAll();

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
    public abstract float mulAll(Mask<Float> m);

    /**
     * Returns the minimum lane element of this vector.
     * <p>
     * This is an associative vector reduction operation where the operation
     * {@code (a, b) -> a > b ? b : a} is applied to lane elements,
     * and the identity value is {@link Float#MAX_VALUE}.
     *
     * @return the minimum lane element of this vector
     */
    public abstract float minAll();

    /**
     * Returns the minimum lane element of this vector, selecting lane elements
     * controlled by a mask.
     * <p>
     * This is an associative vector reduction operation where the operation
     * {@code (a, b) -> a > b ? b : a} is applied to lane elements,
     * and the identity value is {@link Float#MAX_VALUE}.
     *
     * @param m the mask controlling lane selection
     * @return the minimum lane element of this vector
     */
    public abstract float minAll(Mask<Float> m);

    /**
     * Returns the maximum lane element of this vector.
     * <p>
     * This is an associative vector reduction operation where the operation
     * {@code (a, b) -> a < b ? b : a} is applied to lane elements,
     * and the identity value is {@link Float#MIN_VALUE}.
     *
     * @return the maximum lane element of this vector
     */
    public abstract float maxAll();

    /**
     * Returns the maximum lane element of this vector, selecting lane elements
     * controlled by a mask.
     * <p>
     * This is an associative vector reduction operation where the operation
     * {@code (a, b) -> a < b ? b : a} is applied to lane elements,
     * and the identity value is {@link Float#MIN_VALUE}.
     *
     * @param m the mask controlling lane selection
     * @return the maximum lane element of this vector
     */
    public abstract float maxAll(Mask<Float> m);


    // Type specific accessors

    /**
     * Gets the lane element at lane index {@code i}
     *
     * @param i the lane index
     * @return the lane element at lane index {@code i}
     * @throws IllegalArgumentException if the index is is out of range
     * ({@code < 0 || >= length()})
     */
    public abstract float get(int i);

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
    public abstract FloatVector with(int i, float e);

    // Type specific extractors

    /**
     * Returns an array containing the lane elements of this vector.
     * <p>
     * This method behaves as if it {@link #intoArray(float[], int)} stores}
     * this vector into an allocated array and returns the array as follows:
     * <pre>{@code
     *   float[] a = new float[this.length()];
     *   this.intoArray(a, 0);
     *   return a;
     * }</pre>
     *
     * @return an array containing the the lane elements of this vector
     */
    @ForceInline
    public final float[] toArray() {
        float[] a = new float[species().length()];
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
    public abstract void intoArray(float[] a, int i);

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
    public abstract void intoArray(float[] a, int i, Mask<Float> m);

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
    public abstract void intoArray(float[] a, int i, int[] indexMap, int j);

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
    public abstract void intoArray(float[] a, int i, Mask<Float> m, int[] indexMap, int j);
    // Species

    @Override
    public abstract FloatSpecies species();

    /**
     * A specialized factory for creating {@link FloatVector} value of the same
     * shape, and a {@link Mask} and {@link Shuffle} values of the same shape
     * and {@code int} element type.
     */
    public static abstract class FloatSpecies extends Vector.Species<Float> {
        interface FOp {
            float apply(int i);
        }

        abstract FloatVector op(FOp f);

        abstract FloatVector op(Mask<Float> m, FOp f);

        interface FOpm {
            boolean apply(int i);
        }

        abstract Mask<Float> opm(FOpm f);

        // Factories

        @Override
        public abstract FloatVector zero();

        /**
         * Returns a vector where all lane elements are set to the primitive
         * value {@code e}.
         *
         * @param e the value
         * @return a vector of vector where all lane elements are set to
         * the primitive value {@code e}
         */
        public abstract FloatVector broadcast(float e);

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
        public final FloatVector single(float e) {
            return zero().with(0, e);
        }

        /**
         * Returns a vector where each lane element is set to a randomly
         * generated primitive value.
         *
         * The semantics are equivalent to calling
         * {@link ThreadLocalRandom#nextFloat }
         *
         * @return a vector where each lane elements is set to a randomly
         * generated primitive value
         */
        public FloatVector random() {
            ThreadLocalRandom r = ThreadLocalRandom.current();
            return op(i -> r.nextFloat());
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
        public abstract FloatVector scalars(float... es);

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
        public abstract FloatVector fromArray(float[] a, int i);

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
        public abstract FloatVector fromArray(float[] a, int i, Mask<Float> m);

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
        public abstract FloatVector fromArray(float[] a, int i, int[] indexMap, int j);
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
        public abstract FloatVector fromArray(float[] a, int i, Mask<Float> m, int[] indexMap, int j);

        @Override
        public abstract FloatVector fromByteArray(byte[] a, int ix);

        @Override
        public abstract FloatVector fromByteArray(byte[] a, int ix, Mask<Float> m);

        @Override
        public abstract FloatVector fromByteBuffer(ByteBuffer bb, int ix);

        @Override
        public abstract FloatVector fromByteBuffer(ByteBuffer bb, int ix, Mask<Float> m);

        @Override
        public <F> FloatVector reshape(Vector<F> o) {
            int blen = Math.max(o.species().bitSize(), bitSize()) / Byte.SIZE;
            ByteBuffer bb = ByteBuffer.allocate(blen).order(ByteOrder.nativeOrder());
            o.intoByteBuffer(bb, 0);
            return fromByteBuffer(bb, 0);
        }

        @Override
        public abstract <F> FloatVector rebracket(Vector<F> o);

        @Override
        public abstract FloatVector resize(Vector<Float> o);

        @Override
        public abstract <F> FloatVector cast(Vector<F> v);

    }

    /**
     * Finds the preferred species for an element type of {@code float}.
     * <p>
     * A preferred species is a species chosen by the platform that has a
     * shape of maximal bit size.  A preferred species for different element
     * types will have the same shape, and therefore vectors, masks, and
     * shuffles created from such species will be shape compatible.
     *
     * @return the preferred species for an element type of {@code float}
     */
    @SuppressWarnings("unchecked")
    public static FloatSpecies preferredSpecies() {
        return (FloatSpecies) Vector.preferredSpecies(float.class);
    }

    /**
     * Finds a species for an element type of {@code float} and shape.
     *
     * @param s the shape
     * @return a species for an element type of {@code float} and shape
     * @throws IllegalArgumentException if no such species exists for the shape
     */
    @SuppressWarnings("unchecked")
    public static FloatSpecies species(Vector.Shape s) {
        Objects.requireNonNull(s);
        if (s == Shape.S_64_BIT) {
            return Float64Vector.SPECIES;
        } else if (s == Shape.S_128_BIT) {
            return Float128Vector.SPECIES;
        } else if (s == Shape.S_256_BIT) {
            return Float256Vector.SPECIES;
        } else if (s == Shape.S_512_BIT) {
            return Float512Vector.SPECIES;
        } else if (s == Shape.S_Max_BIT) {
            return FloatMaxVector.SPECIES;
        } else {
            throw new IllegalArgumentException("Bad shape: " + s);
        }
    }
}
