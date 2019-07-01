/*
 * Copyright (c) 1997, 2017, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
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
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 *
 */

#include "precompiled.hpp"
#include "code/debugInfo.hpp"
#include "oops/compressedOops.inline.hpp"
#include "oops/oop.hpp"
#include "oops/oop.inline.hpp"
#include "runtime/frame.inline.hpp"
#include "runtime/handles.inline.hpp"
#include "oops/typeArrayOop.inline.hpp"
#include "runtime/interfaceSupport.inline.hpp"
#include "runtime/stackValue.hpp"
#if INCLUDE_ZGC
#include "gc/z/zBarrier.inline.hpp"
#endif
#ifdef COMPILER2
#include "opto/matcher.hpp"
#endif

StackValue* StackValue::create_stack_value(const frame* fr, const RegisterMap* reg_map, ScopeValue* sv) {
  if (sv->is_location()) {
    // Stack or register value
    Location loc = ((LocationValue *)sv)->location();

#ifdef SPARC
    // %%%%% Callee-save floats will NOT be working on a Sparc until we
    // handle the case of a 2 floats in a single double register.
    assert( !(loc.is_register() && loc.type() == Location::float_in_dbl), "Sparc does not handle callee-save floats yet" );
#endif // SPARC

    // First find address of value

    address value_addr = loc.is_register()
      // Value was in a callee-save register
      ? reg_map->location(VMRegImpl::as_VMReg(loc.register_number()))
      // Else value was directly saved on the stack. The frame's original stack pointer,
      // before any extension by its callee (due to Compiler1 linkage on SPARC), must be used.
      : ((address)fr->unextended_sp()) + loc.stack_offset();

    // Then package it right depending on type
    // Note: the transfer of the data is thru a union that contains
    // an intptr_t. This is because an interpreter stack slot is
    // really an intptr_t. The use of a union containing an intptr_t
    // ensures that on a 64 bit platform we have proper alignment
    // and that we store the value where the interpreter will expect
    // to find it (i.e. proper endian). Similarly on a 32bit platform
    // using the intptr_t ensures that when a value is larger than
    // a stack slot (jlong/jdouble) that we capture the proper part
    // of the value for the stack slot in question.
    //
    switch( loc.type() ) {
    case Location::float_in_dbl: { // Holds a float in a double register?
      // The callee has no clue whether the register holds a float,
      // double or is unused.  He always saves a double.  Here we know
      // a double was saved, but we only want a float back.  Narrow the
      // saved double to the float that the JVM wants.
      assert( loc.is_register(), "floats always saved to stack in 1 word" );
      union { intptr_t p; jfloat jf; } value;
      value.p = (intptr_t) CONST64(0xDEADDEAFDEADDEAF);
      value.jf = (jfloat) *(jdouble*) value_addr;
      return new StackValue(value.p); // 64-bit high half is stack junk
    }
    case Location::int_in_long: { // Holds an int in a long register?
      // The callee has no clue whether the register holds an int,
      // long or is unused.  He always saves a long.  Here we know
      // a long was saved, but we only want an int back.  Narrow the
      // saved long to the int that the JVM wants.
      assert( loc.is_register(), "ints always saved to stack in 1 word" );
      union { intptr_t p; jint ji;} value;
      value.p = (intptr_t) CONST64(0xDEADDEAFDEADDEAF);
      value.ji = (jint) *(jlong*) value_addr;
      return new StackValue(value.p); // 64-bit high half is stack junk
    }
#ifdef _LP64
    case Location::dbl:
      // Double value in an aligned adjacent pair
      return new StackValue(*(intptr_t*)value_addr);
    case Location::lng:
      // Long   value in an aligned adjacent pair
      return new StackValue(*(intptr_t*)value_addr);
    case Location::narrowoop: {
      union { intptr_t p; narrowOop noop;} value;
      value.p = (intptr_t) CONST64(0xDEADDEAFDEADDEAF);
      if (loc.is_register()) {
        // The callee has no clue whether the register holds an int,
        // long or is unused.  He always saves a long.  Here we know
        // a long was saved, but we only want an int back.  Narrow the
        // saved long to the int that the JVM wants.
        value.noop =  (narrowOop) *(julong*) value_addr;
      } else {
        value.noop = *(narrowOop*) value_addr;
      }
      // Decode narrowoop and wrap a handle around the oop
      Handle h(Thread::current(), CompressedOops::decode(value.noop));
      return new StackValue(h);
    }
#endif
    case Location::oop: {
      oop val = *(oop *)value_addr;
#ifdef _LP64
      if (Universe::is_narrow_oop_base(val)) {
         // Compiled code may produce decoded oop = narrow_oop_base
         // when a narrow oop implicit null check is used.
         // The narrow_oop_base could be NULL or be the address
         // of the page below heap. Use NULL value for both cases.
         val = (oop)NULL;
      }
#endif
#if INCLUDE_ZGC
      // Deoptimization must make sure all oop have passed load barrier
      if (UseZGC) {
        val = ZBarrier::load_barrier_on_oop_field_preloaded((oop*)value_addr, val);
      }
#endif

      Handle h(Thread::current(), val); // Wrap a handle around the oop
      return new StackValue(h);
    }
    case Location::addr: {
      ShouldNotReachHere(); // both C1 and C2 now inline jsrs
    }
    case Location::normal: {
      // Just copy all other bits straight through
      union { intptr_t p; jint ji;} value;
      value.p = (intptr_t) CONST64(0xDEADDEAFDEADDEAF);
      value.ji = *(jint*)value_addr;
      return new StackValue(value.p);
    }
    case Location::invalid:
      return new StackValue();
    default:
      ShouldNotReachHere();
    }

  } else if (sv->is_constant_int()) {
    // Constant int: treat same as register int.
    union { intptr_t p; jint ji;} value;
    value.p = (intptr_t) CONST64(0xDEADDEAFDEADDEAF);
    value.ji = (jint)((ConstantIntValue*)sv)->value();
    return new StackValue(value.p);
  } else if (sv->is_constant_oop()) {
    // constant oop
    return new StackValue(sv->as_ConstantOopReadValue()->value());
#ifdef _LP64
  } else if (sv->is_constant_double()) {
    // Constant double in a single stack slot
    union { intptr_t p; double d; } value;
    value.p = (intptr_t) CONST64(0xDEADDEAFDEADDEAF);
    value.d = ((ConstantDoubleValue *)sv)->value();
    return new StackValue(value.p);
  } else if (sv->is_constant_long()) {
    // Constant long in a single stack slot
    union { intptr_t p; jlong jl; } value;
    value.p = (intptr_t) CONST64(0xDEADDEAFDEADDEAF);
    value.jl = ((ConstantLongValue *)sv)->value();
    return new StackValue(value.p);
#endif
  } else if (sv->is_object()) { // Scalar replaced object in compiled frame
    Handle ov = ((ObjectValue *)sv)->value();
    return new StackValue(ov, (ov.is_null()) ? 1 : 0);
  }

  // Unknown ScopeValue type
  ShouldNotReachHere();
  return new StackValue((intptr_t) 0);   // dummy
}

static BasicType klass2bt(InstanceKlass* ik, bool& is_mask) {
  switch(vmSymbols::find_sid(ik->name())) {
    // Vectors
    case vmSymbols::VM_SYMBOL_ENUM_NAME(jdk_panama_vector_Byte64Vector):
    case vmSymbols::VM_SYMBOL_ENUM_NAME(jdk_panama_vector_Byte128Vector):
    case vmSymbols::VM_SYMBOL_ENUM_NAME(jdk_panama_vector_Byte256Vector):
    case vmSymbols::VM_SYMBOL_ENUM_NAME(jdk_panama_vector_Byte512Vector):
    case vmSymbols::VM_SYMBOL_ENUM_NAME(jdk_panama_vector_ByteMaxVector):
      is_mask = false;
      return T_BYTE;

    case vmSymbols::VM_SYMBOL_ENUM_NAME(jdk_panama_vector_Short64Vector):
    case vmSymbols::VM_SYMBOL_ENUM_NAME(jdk_panama_vector_Short128Vector):
    case vmSymbols::VM_SYMBOL_ENUM_NAME(jdk_panama_vector_Short256Vector):
    case vmSymbols::VM_SYMBOL_ENUM_NAME(jdk_panama_vector_Short512Vector):
    case vmSymbols::VM_SYMBOL_ENUM_NAME(jdk_panama_vector_ShortMaxVector):
      is_mask = false;
      return T_SHORT;

    case vmSymbols::VM_SYMBOL_ENUM_NAME(jdk_panama_vector_Int64Vector):
    case vmSymbols::VM_SYMBOL_ENUM_NAME(jdk_panama_vector_Int128Vector):
    case vmSymbols::VM_SYMBOL_ENUM_NAME(jdk_panama_vector_Int256Vector):
    case vmSymbols::VM_SYMBOL_ENUM_NAME(jdk_panama_vector_Int512Vector):
    case vmSymbols::VM_SYMBOL_ENUM_NAME(jdk_panama_vector_IntMaxVector):
      is_mask = false;
      return T_INT;

    case vmSymbols::VM_SYMBOL_ENUM_NAME(jdk_panama_vector_Long64Vector):
    case vmSymbols::VM_SYMBOL_ENUM_NAME(jdk_panama_vector_Long128Vector):
    case vmSymbols::VM_SYMBOL_ENUM_NAME(jdk_panama_vector_Long256Vector):
    case vmSymbols::VM_SYMBOL_ENUM_NAME(jdk_panama_vector_Long512Vector):
    case vmSymbols::VM_SYMBOL_ENUM_NAME(jdk_panama_vector_LongMaxVector):
      is_mask = false;
      return T_LONG;

    case vmSymbols::VM_SYMBOL_ENUM_NAME(jdk_panama_vector_Float64Vector):
    case vmSymbols::VM_SYMBOL_ENUM_NAME(jdk_panama_vector_Float128Vector):
    case vmSymbols::VM_SYMBOL_ENUM_NAME(jdk_panama_vector_Float256Vector):
    case vmSymbols::VM_SYMBOL_ENUM_NAME(jdk_panama_vector_Float512Vector):
    case vmSymbols::VM_SYMBOL_ENUM_NAME(jdk_panama_vector_FloatMaxVector):
      is_mask = false;
      return T_FLOAT;

    case vmSymbols::VM_SYMBOL_ENUM_NAME(jdk_panama_vector_Double64Vector):
    case vmSymbols::VM_SYMBOL_ENUM_NAME(jdk_panama_vector_Double128Vector):
    case vmSymbols::VM_SYMBOL_ENUM_NAME(jdk_panama_vector_Double256Vector):
    case vmSymbols::VM_SYMBOL_ENUM_NAME(jdk_panama_vector_Double512Vector):
    case vmSymbols::VM_SYMBOL_ENUM_NAME(jdk_panama_vector_DoubleMaxVector):
      is_mask = false;
      return T_DOUBLE;

    // Masks
    case vmSymbols::VM_SYMBOL_ENUM_NAME(jdk_panama_vector_Byte64Vector_Byte64Mask):
    case vmSymbols::VM_SYMBOL_ENUM_NAME(jdk_panama_vector_Byte128Vector_Byte128Mask):
    case vmSymbols::VM_SYMBOL_ENUM_NAME(jdk_panama_vector_Byte256Vector_Byte256Mask):
    case vmSymbols::VM_SYMBOL_ENUM_NAME(jdk_panama_vector_Byte512Vector_Byte512Mask):
    case vmSymbols::VM_SYMBOL_ENUM_NAME(jdk_panama_vector_ByteMaxVector_ByteMaxMask):
      is_mask = true;
      return T_BYTE;

    case vmSymbols::VM_SYMBOL_ENUM_NAME(jdk_panama_vector_Short64Vector_Short64Mask):
    case vmSymbols::VM_SYMBOL_ENUM_NAME(jdk_panama_vector_Short128Vector_Short128Mask):
    case vmSymbols::VM_SYMBOL_ENUM_NAME(jdk_panama_vector_Short256Vector_Short256Mask):
    case vmSymbols::VM_SYMBOL_ENUM_NAME(jdk_panama_vector_Short512Vector_Short512Mask):
    case vmSymbols::VM_SYMBOL_ENUM_NAME(jdk_panama_vector_ShortMaxVector_ShortMaxMask):
      is_mask = true;
      return T_SHORT;

    case vmSymbols::VM_SYMBOL_ENUM_NAME(jdk_panama_vector_Int64Vector_Int64Mask):
    case vmSymbols::VM_SYMBOL_ENUM_NAME(jdk_panama_vector_Int128Vector_Int128Mask):
    case vmSymbols::VM_SYMBOL_ENUM_NAME(jdk_panama_vector_Int256Vector_Int256Mask):
    case vmSymbols::VM_SYMBOL_ENUM_NAME(jdk_panama_vector_Int512Vector_Int512Mask):
    case vmSymbols::VM_SYMBOL_ENUM_NAME(jdk_panama_vector_IntMaxVector_IntMaxMask):
      is_mask = true;
      return T_INT;

    // LongMask
    case vmSymbols::VM_SYMBOL_ENUM_NAME(jdk_panama_vector_Long64Vector_Long64Mask):
    case vmSymbols::VM_SYMBOL_ENUM_NAME(jdk_panama_vector_Long128Vector_Long128Mask):
    case vmSymbols::VM_SYMBOL_ENUM_NAME(jdk_panama_vector_Long256Vector_Long256Mask):
    case vmSymbols::VM_SYMBOL_ENUM_NAME(jdk_panama_vector_Long512Vector_Long512Mask):
    case vmSymbols::VM_SYMBOL_ENUM_NAME(jdk_panama_vector_LongMaxVector_LongMaxMask):
      is_mask = true;
      return T_LONG;

    // FloatMask
    case vmSymbols::VM_SYMBOL_ENUM_NAME(jdk_panama_vector_Float64Vector_Float64Mask):
    case vmSymbols::VM_SYMBOL_ENUM_NAME(jdk_panama_vector_Float128Vector_Float128Mask):
    case vmSymbols::VM_SYMBOL_ENUM_NAME(jdk_panama_vector_Float256Vector_Float256Mask):
    case vmSymbols::VM_SYMBOL_ENUM_NAME(jdk_panama_vector_Float512Vector_Float512Mask):
    case vmSymbols::VM_SYMBOL_ENUM_NAME(jdk_panama_vector_FloatMaxVector_FloatMaxMask):
      is_mask = true;
      return T_INT;

    // DoubleMask
    case vmSymbols::VM_SYMBOL_ENUM_NAME(jdk_panama_vector_Double64Vector_Double64Mask):
    case vmSymbols::VM_SYMBOL_ENUM_NAME(jdk_panama_vector_Double128Vector_Double128Mask):
    case vmSymbols::VM_SYMBOL_ENUM_NAME(jdk_panama_vector_Double256Vector_Double256Mask):
    case vmSymbols::VM_SYMBOL_ENUM_NAME(jdk_panama_vector_Double512Vector_Double512Mask):
    case vmSymbols::VM_SYMBOL_ENUM_NAME(jdk_panama_vector_DoubleMaxVector_DoubleMaxMask):
      is_mask = true;
      return T_LONG;

    default:
      fatal("unknown klass: %s", ik->name()->as_utf8());
      return T_ILLEGAL;
  }
}

static int klass2bytes(InstanceKlass* ik) {
  switch(vmSymbols::find_sid(ik->name())) {
    case vmSymbols::VM_SYMBOL_ENUM_NAME(jdk_panama_vector_Byte64Vector):
    case vmSymbols::VM_SYMBOL_ENUM_NAME(jdk_panama_vector_Byte64Vector_Byte64Mask):
    case vmSymbols::VM_SYMBOL_ENUM_NAME(jdk_panama_vector_Short64Vector):
    case vmSymbols::VM_SYMBOL_ENUM_NAME(jdk_panama_vector_Short64Vector_Short64Mask):
    case vmSymbols::VM_SYMBOL_ENUM_NAME(jdk_panama_vector_Int64Vector):
    case vmSymbols::VM_SYMBOL_ENUM_NAME(jdk_panama_vector_Int64Vector_Int64Mask):
    case vmSymbols::VM_SYMBOL_ENUM_NAME(jdk_panama_vector_Long64Vector):
    case vmSymbols::VM_SYMBOL_ENUM_NAME(jdk_panama_vector_Long64Vector_Long64Mask):
    case vmSymbols::VM_SYMBOL_ENUM_NAME(jdk_panama_vector_Float64Vector):
    case vmSymbols::VM_SYMBOL_ENUM_NAME(jdk_panama_vector_Float64Vector_Float64Mask):
    case vmSymbols::VM_SYMBOL_ENUM_NAME(jdk_panama_vector_Double64Vector):
    case vmSymbols::VM_SYMBOL_ENUM_NAME(jdk_panama_vector_Double64Vector_Double64Mask):
      return (64 / 8);

    case vmSymbols::VM_SYMBOL_ENUM_NAME(jdk_panama_vector_Byte128Vector):
    case vmSymbols::VM_SYMBOL_ENUM_NAME(jdk_panama_vector_Byte128Vector_Byte128Mask):
    case vmSymbols::VM_SYMBOL_ENUM_NAME(jdk_panama_vector_Short128Vector):
    case vmSymbols::VM_SYMBOL_ENUM_NAME(jdk_panama_vector_Short128Vector_Short128Mask):
    case vmSymbols::VM_SYMBOL_ENUM_NAME(jdk_panama_vector_Int128Vector):
    case vmSymbols::VM_SYMBOL_ENUM_NAME(jdk_panama_vector_Int128Vector_Int128Mask):
    case vmSymbols::VM_SYMBOL_ENUM_NAME(jdk_panama_vector_Long128Vector):
    case vmSymbols::VM_SYMBOL_ENUM_NAME(jdk_panama_vector_Long128Vector_Long128Mask):
    case vmSymbols::VM_SYMBOL_ENUM_NAME(jdk_panama_vector_Float128Vector):
    case vmSymbols::VM_SYMBOL_ENUM_NAME(jdk_panama_vector_Float128Vector_Float128Mask):
    case vmSymbols::VM_SYMBOL_ENUM_NAME(jdk_panama_vector_Double128Vector):
    case vmSymbols::VM_SYMBOL_ENUM_NAME(jdk_panama_vector_Double128Vector_Double128Mask):
      return (128 / 8);

    case vmSymbols::VM_SYMBOL_ENUM_NAME(jdk_panama_vector_Byte256Vector):
    case vmSymbols::VM_SYMBOL_ENUM_NAME(jdk_panama_vector_Byte256Vector_Byte256Mask):
    case vmSymbols::VM_SYMBOL_ENUM_NAME(jdk_panama_vector_Short256Vector):
    case vmSymbols::VM_SYMBOL_ENUM_NAME(jdk_panama_vector_Short256Vector_Short256Mask):
    case vmSymbols::VM_SYMBOL_ENUM_NAME(jdk_panama_vector_Int256Vector):
    case vmSymbols::VM_SYMBOL_ENUM_NAME(jdk_panama_vector_Int256Vector_Int256Mask):
    case vmSymbols::VM_SYMBOL_ENUM_NAME(jdk_panama_vector_Long256Vector):
    case vmSymbols::VM_SYMBOL_ENUM_NAME(jdk_panama_vector_Long256Vector_Long256Mask):
    case vmSymbols::VM_SYMBOL_ENUM_NAME(jdk_panama_vector_Float256Vector):
    case vmSymbols::VM_SYMBOL_ENUM_NAME(jdk_panama_vector_Float256Vector_Float256Mask):
    case vmSymbols::VM_SYMBOL_ENUM_NAME(jdk_panama_vector_Double256Vector):
    case vmSymbols::VM_SYMBOL_ENUM_NAME(jdk_panama_vector_Double256Vector_Double256Mask):
      return (256 / 8);

    case vmSymbols::VM_SYMBOL_ENUM_NAME(jdk_panama_vector_Byte512Vector):
    case vmSymbols::VM_SYMBOL_ENUM_NAME(jdk_panama_vector_Byte512Vector_Byte512Mask):
    case vmSymbols::VM_SYMBOL_ENUM_NAME(jdk_panama_vector_Short512Vector):
    case vmSymbols::VM_SYMBOL_ENUM_NAME(jdk_panama_vector_Short512Vector_Short512Mask):
    case vmSymbols::VM_SYMBOL_ENUM_NAME(jdk_panama_vector_Int512Vector):
    case vmSymbols::VM_SYMBOL_ENUM_NAME(jdk_panama_vector_Int512Vector_Int512Mask):
    case vmSymbols::VM_SYMBOL_ENUM_NAME(jdk_panama_vector_Long512Vector):
    case vmSymbols::VM_SYMBOL_ENUM_NAME(jdk_panama_vector_Long512Vector_Long512Mask):
    case vmSymbols::VM_SYMBOL_ENUM_NAME(jdk_panama_vector_Float512Vector):
    case vmSymbols::VM_SYMBOL_ENUM_NAME(jdk_panama_vector_Float512Vector_Float512Mask):
    case vmSymbols::VM_SYMBOL_ENUM_NAME(jdk_panama_vector_Double512Vector):
    case vmSymbols::VM_SYMBOL_ENUM_NAME(jdk_panama_vector_Double512Vector_Double512Mask):
      return (512 / 8);

    case vmSymbols::VM_SYMBOL_ENUM_NAME(jdk_panama_vector_ByteMaxVector):
    case vmSymbols::VM_SYMBOL_ENUM_NAME(jdk_panama_vector_ByteMaxVector_ByteMaxMask):
    case vmSymbols::VM_SYMBOL_ENUM_NAME(jdk_panama_vector_ShortMaxVector):
    case vmSymbols::VM_SYMBOL_ENUM_NAME(jdk_panama_vector_ShortMaxVector_ShortMaxMask):
    case vmSymbols::VM_SYMBOL_ENUM_NAME(jdk_panama_vector_IntMaxVector):
    case vmSymbols::VM_SYMBOL_ENUM_NAME(jdk_panama_vector_IntMaxVector_IntMaxMask):
    case vmSymbols::VM_SYMBOL_ENUM_NAME(jdk_panama_vector_LongMaxVector):
    case vmSymbols::VM_SYMBOL_ENUM_NAME(jdk_panama_vector_LongMaxVector_LongMaxMask):
    case vmSymbols::VM_SYMBOL_ENUM_NAME(jdk_panama_vector_FloatMaxVector):
    case vmSymbols::VM_SYMBOL_ENUM_NAME(jdk_panama_vector_FloatMaxVector_FloatMaxMask):
    case vmSymbols::VM_SYMBOL_ENUM_NAME(jdk_panama_vector_DoubleMaxVector):
    case vmSymbols::VM_SYMBOL_ENUM_NAME(jdk_panama_vector_DoubleMaxVector_DoubleMaxMask):
#ifdef COMPILER2
      return Matcher::max_vector_size(T_BYTE);
#else
      fatal("unknown klass: %s", ik->name()->as_utf8());
      return -1;
#endif
    default:
      fatal("unknown klass: %s", ik->name()->as_utf8());
      return -1;
  }
}

static void init_vector_array(typeArrayOop arr, BasicType elem_bt, int num_elem, address value_addr) {
  int elem_size = type2aelembytes(elem_bt);

  for (int i = 0; i < num_elem; i++) {
    switch (elem_bt) {
      case T_BYTE: {
        jbyte elem_value = *(jbyte*) (value_addr + i * elem_size);
        arr->byte_at_put(i, elem_value);
        break;
      }
      case T_SHORT: {
        jshort elem_value = *(jshort*) (value_addr + i * elem_size);
        arr->short_at_put(i, elem_value);
        break;
      }
      case T_INT: {
        jint elem_value = *(jint*) (value_addr + i * elem_size);
        arr->int_at_put(i, elem_value);
        break;
      }
      case T_LONG: {
        jlong elem_value = *(jlong*) (value_addr + i * elem_size);
        arr->long_at_put(i, elem_value);
        break;
      }
      case T_FLOAT: {
        jfloat elem_value = *(jfloat*) (value_addr + i * elem_size);
        arr->float_at_put(i, elem_value);
        break;
      }
      case T_DOUBLE: {
        jdouble elem_value = *(jdouble*) (value_addr + i * elem_size);
        arr->double_at_put(i, elem_value);
        break;
      }
      default:
        fatal("unsupported: %s", type2name(elem_bt));
    }
  }
}

static void init_mask_array(typeArrayOop arr, BasicType elem_bt, int num_elem, address value_addr) {
  int elem_size = type2aelembytes(elem_bt);

  for (int i = 0; i < num_elem; i++) {
    switch (elem_bt) {
      case T_BYTE: {
        jbyte elem_value = *(jbyte*) (value_addr + i * elem_size);
        arr->bool_at_put(i, elem_value != 0);
        break;
      }
      case T_SHORT: {
        jshort elem_value = *(jshort*) (value_addr + i * elem_size);
        arr->bool_at_put(i, elem_value != 0);
        break;
      }
      case T_INT: {
        jint elem_value = *(jint*) (value_addr + i * elem_size);
        arr->bool_at_put(i, elem_value != 0);
        break;
      }
      case T_LONG: {
        jlong elem_value = *(jlong*) (value_addr + i * elem_size);
        arr->bool_at_put(i, elem_value != 0);
        break;
      }
      default:
        fatal("unsupported: %s", type2name(elem_bt));
    }
  }
}

static oop allocate_vector(InstanceKlass* ik, address value_addr) {
  bool is_mask = false; // overwritten by klass2bt
  BasicType elem_bt = klass2bt(ik, is_mask);
  int num_elem = klass2bytes(ik) / type2aelembytes(elem_bt);
  assert(!is_mask || (elem_bt != T_FLOAT && elem_bt != T_DOUBLE), "%s", type2name(elem_bt));
  TypeArrayKlass* ak = TypeArrayKlass::cast(Universe::typeArrayKlassObj(is_mask ? T_BOOLEAN : elem_bt));
  JavaThread* thread = JavaThread::current();
  JRT_BLOCK
    typeArrayOop arr = ak->allocate(num_elem, THREAD);
    if (is_mask) {
      init_mask_array(arr, elem_bt, num_elem, value_addr);
    } else {
      init_vector_array(arr, elem_bt, num_elem, value_addr);
    }
    return arr;
  JRT_END
}

StackValue* StackValue::create_stack_value(const frame* fr, const RegisterMap* reg_map, ScopeValue* sv, InstanceKlass* ik) {
  if (sv->is_location()) {
    // Stack or register value
    Location loc = ((LocationValue *)sv)->location();

#ifdef SPARC
    // %%%%% Callee-save floats will NOT be working on a Sparc until we
    // handle the case of a 2 floats in a single double register.
    assert( !(loc.is_register() && loc.type() == Location::float_in_dbl), "Sparc does not handle callee-save floats yet" );
#endif // SPARC

    // First find address of value

    address value_addr = loc.is_register()
      // Value was in a callee-save register
      ? reg_map->location(VMRegImpl::as_VMReg(loc.register_number()))
      // Else value was directly saved on the stack. The frame's original stack pointer,
      // before any extension by its callee (due to Compiler1 linkage on SPARC), must be used.
      : ((address)fr->unextended_sp()) + loc.stack_offset();

    // Then package it right depending on type
    // Note: the transfer of the data is thru a union that contains
    // an intptr_t. This is because an interpreter stack slot is
    // really an intptr_t. The use of a union containing an intptr_t
    // ensures that on a 64 bit platform we have proper alignment
    // and that we store the value where the interpreter will expect
    // to find it (i.e. proper endian). Similarly on a 32bit platform
    // using the intptr_t ensures that when a value is larger than
    // a stack slot (jlong/jdouble) that we capture the proper part
    // of the value for the stack slot in question.
    //
    switch( loc.type() ) {
    case Location::float_in_dbl: { // Holds a float in a double register?
      // The callee has no clue whether the register holds a float,
      // double or is unused.  He always saves a double.  Here we know
      // a double was saved, but we only want a float back.  Narrow the
      // saved double to the float that the JVM wants.
      assert( loc.is_register(), "floats always saved to stack in 1 word" );
      union { intptr_t p; jfloat jf; } value;
      value.p = (intptr_t) CONST64(0xDEADDEAFDEADDEAF);
      value.jf = (jfloat) *(jdouble*) value_addr;
      return new StackValue(value.p); // 64-bit high half is stack junk
    }
    case Location::int_in_long: { // Holds an int in a long register?
      // The callee has no clue whether the register holds an int,
      // long or is unused.  He always saves a long.  Here we know
      // a long was saved, but we only want an int back.  Narrow the
      // saved long to the int that the JVM wants.
      assert( loc.is_register(), "ints always saved to stack in 1 word" );
      union { intptr_t p; jint ji;} value;
      value.p = (intptr_t) CONST64(0xDEADDEAFDEADDEAF);
      value.ji = (jint) *(jlong*) value_addr;
      return new StackValue(value.p); // 64-bit high half is stack junk
    }
#ifdef _LP64
    case Location::dbl:
      // Double value in an aligned adjacent pair
      return new StackValue(*(intptr_t*)value_addr);
    case Location::lng:
      // Long   value in an aligned adjacent pair
      return new StackValue(*(intptr_t*)value_addr);
    case Location::vector: {
      assert(UseVectorAPI, "Should only reach here when VectorAPI is enabled");
      assert(ik != NULL /*&& ik->is_vector_box()*/, "");
      // Vector value in an aligned adjacent tuple (4, 8, or 16 slots).
      Handle h(Thread::current(), allocate_vector(ik, value_addr)); // Wrap a handle around the oop
      return new StackValue(h);
    }
    case Location::narrowoop: {
      union { intptr_t p; narrowOop noop;} value;
      value.p = (intptr_t) CONST64(0xDEADDEAFDEADDEAF);
      if (loc.is_register()) {
        // The callee has no clue whether the register holds an int,
        // long or is unused.  He always saves a long.  Here we know
        // a long was saved, but we only want an int back.  Narrow the
        // saved long to the int that the JVM wants.
        value.noop =  (narrowOop) *(julong*) value_addr;
      } else {
        value.noop = *(narrowOop*) value_addr;
      }
      // Decode narrowoop and wrap a handle around the oop
      Handle h(Thread::current(), CompressedOops::decode(value.noop));
      return new StackValue(h);
    }
#endif
    case Location::oop: {
      oop val = *(oop *)value_addr;
#ifdef _LP64
      if (Universe::is_narrow_oop_base(val)) {
         // Compiled code may produce decoded oop = narrow_oop_base
         // when a narrow oop implicit null check is used.
         // The narrow_oop_base could be NULL or be the address
         // of the page below heap. Use NULL value for both cases.
         val = (oop)NULL;
      }
#endif
#if INCLUDE_ZGC
      // Deoptimization must make sure all oop have passed load barrier
      if (UseZGC) {
        val = ZBarrier::load_barrier_on_oop_field_preloaded((oop*)value_addr, val);
      }
#endif

      Handle h(Thread::current(), val); // Wrap a handle around the oop
      return new StackValue(h);
    }
    case Location::addr: {
      ShouldNotReachHere(); // both C1 and C2 now inline jsrs
    }
    case Location::normal: {
      // Just copy all other bits straight through
      union { intptr_t p; jint ji;} value;
      value.p = (intptr_t) CONST64(0xDEADDEAFDEADDEAF);
      value.ji = *(jint*)value_addr;
      return new StackValue(value.p);
    }
    case Location::invalid:
      return new StackValue();
    default:
      ShouldNotReachHere();
    }

  } else if (sv->is_constant_int()) {
    // Constant int: treat same as register int.
    union { intptr_t p; jint ji;} value;
    value.p = (intptr_t) CONST64(0xDEADDEAFDEADDEAF);
    value.ji = (jint)((ConstantIntValue*)sv)->value();
    return new StackValue(value.p);
  } else if (sv->is_constant_oop()) {
    // constant oop
    return new StackValue(sv->as_ConstantOopReadValue()->value());
#ifdef _LP64
  } else if (sv->is_constant_double()) {
    // Constant double in a single stack slot
    union { intptr_t p; double d; } value;
    value.p = (intptr_t) CONST64(0xDEADDEAFDEADDEAF);
    value.d = ((ConstantDoubleValue *)sv)->value();
    return new StackValue(value.p);
  } else if (sv->is_constant_long()) {
    // Constant long in a single stack slot
    union { intptr_t p; jlong jl; } value;
    value.p = (intptr_t) CONST64(0xDEADDEAFDEADDEAF);
    value.jl = ((ConstantLongValue *)sv)->value();
    return new StackValue(value.p);
#endif
  } else if (sv->is_object()) { // Scalar replaced object in compiled frame
    Handle ov = ((ObjectValue *)sv)->value();
    return new StackValue(ov, (ov.is_null()) ? 1 : 0);
  }

  // Unknown ScopeValue type
  ShouldNotReachHere();
  return new StackValue((intptr_t) 0);   // dummy
}


BasicLock* StackValue::resolve_monitor_lock(const frame* fr, Location location) {
  assert(location.is_stack(), "for now we only look at the stack");
  int word_offset = location.stack_offset() / wordSize;
  // (stack picture)
  // high: [     ]  word_offset + 1
  // low   [     ]  word_offset
  //
  // sp->  [     ]  0
  // the word_offset is the distance from the stack pointer to the lowest address
  // The frame's original stack pointer, before any extension by its callee
  // (due to Compiler1 linkage on SPARC), must be used.
  return (BasicLock*) (fr->unextended_sp() + word_offset);
}


#ifndef PRODUCT

void StackValue::print_on(outputStream* st) const {
  switch(_type) {
    case T_INT:
      st->print("%d (int) %f (float) %x (hex)",  *(int *)&_integer_value, *(float *)&_integer_value,  *(int *)&_integer_value);
      break;

    case T_OBJECT:
      _handle_value()->print_value_on(st);
      st->print(" <" INTPTR_FORMAT ">", p2i((address)_handle_value()));
      break;

    case T_ILLEGAL: // FIXME
      st->print(" V@<" INTPTR_FORMAT ">", _integer_value);
      break;

    case T_CONFLICT:
     st->print("conflict");
     break;

    default:
     ShouldNotReachHere();
  }
}

#endif
