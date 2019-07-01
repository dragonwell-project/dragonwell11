/*
 * Copyright (c) 2000, 2017, Oracle and/or its affiliates. All rights reserved.
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
#include "ci/ciEnv.hpp"
#include "ci/ciType.hpp"
#include "ci/ciUtilities.inline.hpp"
#include "classfile/systemDictionary.hpp"
#include "memory/resourceArea.hpp"
#include "oops/oop.inline.hpp"
#ifdef COMPILER2
#include "opto/matcher.hpp"
#endif

ciType* ciType::_basic_types[T_CONFLICT+1];

// ciType
//
// This class represents either a class (T_OBJECT), array (T_ARRAY),
// or one of the primitive types such as T_INT.

// ------------------------------------------------------------------
// ciType::ciType
//
ciType::ciType(BasicType basic_type) : ciMetadata() {
  assert(basic_type >= T_BOOLEAN && basic_type <= T_CONFLICT, "range check");
  _basic_type = basic_type;
}

ciType::ciType(Klass* k) : ciMetadata(k) {
  _basic_type = k->is_array_klass() ? T_ARRAY : T_OBJECT;
}


// ------------------------------------------------------------------
// ciType::is_subtype_of
//
bool ciType::is_subtype_of(ciType* type) {
  if (this == type)  return true;
  if (is_klass() && type->is_klass())
    return this->as_klass()->is_subtype_of(type->as_klass());
  return false;
}

// ------------------------------------------------------------------
// ciType::name
//
// Return the name of this type
const char* ciType::name() {
  if (is_primitive_type()) {
    return type2name(basic_type());
  } else {
    assert(is_klass(), "must be");
    return as_klass()->name()->as_utf8();
  }
}

// ------------------------------------------------------------------
// ciType::print_impl
//
// Implementation of the print method.
void ciType::print_impl(outputStream* st) {
  st->print(" type=");
  print_name_on(st);
}

// ------------------------------------------------------------------
// ciType::print_name
//
// Print the name of this type
void ciType::print_name_on(outputStream* st) {
  ResourceMark rm;
  st->print("%s", name());
}



// ------------------------------------------------------------------
// ciType::java_mirror
//
ciInstance* ciType::java_mirror() {
  VM_ENTRY_MARK;
  return CURRENT_THREAD_ENV->get_instance(Universe::java_mirror(basic_type()));
}

// ------------------------------------------------------------------
// ciType::box_klass
//
ciKlass* ciType::box_klass() {
  if (!is_primitive_type())  return this->as_klass();  // reference types are "self boxing"

  // Void is "boxed" with a null.
  if (basic_type() == T_VOID)  return NULL;

  VM_ENTRY_MARK;
  return CURRENT_THREAD_ENV->get_instance_klass(SystemDictionary::box_klass(basic_type()));
}


// ------------------------------------------------------------------
// ciType::make
//
// Produce the ciType for a given primitive BasicType.
// As a bonus, produce the right reference type for T_OBJECT.
// Does not work on T_ARRAY.
ciType* ciType::make(BasicType t) {
  // short, etc.
  // Note: Bare T_ADDRESS means a raw pointer type, not a return_address.
  assert((uint)t < T_CONFLICT+1, "range check");
  if (t == T_OBJECT)  return ciEnv::_Object_klass;  // java/lang/Object
  assert(_basic_types[t] != NULL, "domain check");
  return _basic_types[t];
}

static bool is_float64vector(BasicType bt, vmSymbols::SID sid) {
  return bt == T_OBJECT && sid == vmSymbols::VM_SYMBOL_ENUM_NAME(jdk_panama_vector_Float64Vector);
}
static bool is_float64species(BasicType bt, vmSymbols::SID sid) {
  return bt == T_OBJECT && sid == vmSymbols::VM_SYMBOL_ENUM_NAME(jdk_panama_vector_Float64Vector_Float64Species);
}
static bool is_float64mask(BasicType bt, vmSymbols::SID sid) {
  return bt == T_OBJECT && sid == vmSymbols::VM_SYMBOL_ENUM_NAME(jdk_panama_vector_Float64Vector_Float64Mask);
}
static bool is_float64shuffle(BasicType bt, vmSymbols::SID sid) {
  return bt == T_OBJECT && sid == vmSymbols::VM_SYMBOL_ENUM_NAME(jdk_panama_vector_Float64Vector_Float64Shuffle);
}
static bool is_float64(BasicType bt, vmSymbols::SID sid) {
  return is_float64vector(bt, sid) || is_float64species(bt, sid) || is_float64mask(bt, sid);
}
static bool is_float128vector(BasicType bt, vmSymbols::SID sid) {
  return bt == T_OBJECT && sid == vmSymbols::VM_SYMBOL_ENUM_NAME(jdk_panama_vector_Float128Vector);
}
static bool is_float128species(BasicType bt, vmSymbols::SID sid) {
  return bt == T_OBJECT && sid == vmSymbols::VM_SYMBOL_ENUM_NAME(jdk_panama_vector_Float128Vector_Float128Species);
}
static bool is_float128mask(BasicType bt, vmSymbols::SID sid) {
  return bt == T_OBJECT && sid == vmSymbols::VM_SYMBOL_ENUM_NAME(jdk_panama_vector_Float128Vector_Float128Mask);
}
static bool is_float128shuffle(BasicType bt, vmSymbols::SID sid) {
  return bt == T_OBJECT && sid == vmSymbols::VM_SYMBOL_ENUM_NAME(jdk_panama_vector_Float128Vector_Float128Shuffle);
}
static bool is_float128(BasicType bt, vmSymbols::SID sid) {
  return is_float128vector(bt, sid) || is_float128species(bt, sid) || is_float128mask(bt, sid);
}
static bool is_float256vector(BasicType bt, vmSymbols::SID sid) {
  return bt == T_OBJECT && sid == vmSymbols::VM_SYMBOL_ENUM_NAME(jdk_panama_vector_Float256Vector);
}
static bool is_float256species(BasicType bt, vmSymbols::SID sid) {
  return bt == T_OBJECT && sid == vmSymbols::VM_SYMBOL_ENUM_NAME(jdk_panama_vector_Float256Vector_Float256Species);
}
static bool is_float256mask(BasicType bt, vmSymbols::SID sid) {
  return bt == T_OBJECT && sid == vmSymbols::VM_SYMBOL_ENUM_NAME(jdk_panama_vector_Float256Vector_Float256Mask);
}
static bool is_float256shuffle(BasicType bt, vmSymbols::SID sid) {
  return bt == T_OBJECT && sid == vmSymbols::VM_SYMBOL_ENUM_NAME(jdk_panama_vector_Float256Vector_Float256Shuffle);
}
static bool is_float256(BasicType bt, vmSymbols::SID sid) {
  return is_float256vector(bt, sid) || is_float256species(bt, sid) || is_float256mask(bt, sid);
}
static bool is_float512vector(BasicType bt, vmSymbols::SID sid) {
  return bt == T_OBJECT && sid == vmSymbols::VM_SYMBOL_ENUM_NAME(jdk_panama_vector_Float512Vector);
}
static bool is_float512species(BasicType bt, vmSymbols::SID sid) {
  return bt == T_OBJECT && sid == vmSymbols::VM_SYMBOL_ENUM_NAME(jdk_panama_vector_Float512Vector_Float512Species);
}
static bool is_float512mask(BasicType bt, vmSymbols::SID sid) {
  return bt == T_OBJECT && sid == vmSymbols::VM_SYMBOL_ENUM_NAME(jdk_panama_vector_Float512Vector_Float512Mask);
}
static bool is_float512shuffle(BasicType bt, vmSymbols::SID sid) {
  return bt == T_OBJECT && sid == vmSymbols::VM_SYMBOL_ENUM_NAME(jdk_panama_vector_Float512Vector_Float512Shuffle);
}
static bool is_float512(BasicType bt, vmSymbols::SID sid) {
  return is_float512vector(bt, sid) || is_float512species(bt, sid) || is_float512mask(bt, sid);
}
static bool is_float_max_vector(BasicType bt, vmSymbols::SID sid) {
  return bt == T_OBJECT && sid == vmSymbols::VM_SYMBOL_ENUM_NAME(jdk_panama_vector_FloatMaxVector);
}
static bool is_float_max_species(BasicType bt, vmSymbols::SID sid) {
  return bt == T_OBJECT && sid == vmSymbols::VM_SYMBOL_ENUM_NAME(jdk_panama_vector_FloatMaxVector_FloatMaxSpecies);
}
static bool is_float_max_mask(BasicType bt, vmSymbols::SID sid) {
  return bt == T_OBJECT && sid == vmSymbols::VM_SYMBOL_ENUM_NAME(jdk_panama_vector_FloatMaxVector_FloatMaxMask);
}
static bool is_float_max_shuffle(BasicType bt, vmSymbols::SID sid) {
  return bt == T_OBJECT && sid == vmSymbols::VM_SYMBOL_ENUM_NAME(jdk_panama_vector_FloatMaxVector_FloatMaxShuffle);
}
static bool is_float_max(BasicType bt, vmSymbols::SID sid) {
  return is_float_max_vector(bt, sid) || is_float_max_species(bt, sid) || is_float_max_mask(bt, sid);
}
static bool is_float_vec_or_mask(BasicType bt, vmSymbols::SID sid) {
  return is_float64(bt, sid) || is_float128(bt, sid) || is_float256(bt, sid) || is_float512(bt, sid) || is_float_max(bt, sid);
}
static bool is_double64vector(BasicType bt, vmSymbols::SID sid) {
  return bt == T_OBJECT && sid == vmSymbols::VM_SYMBOL_ENUM_NAME(jdk_panama_vector_Double64Vector);
}
static bool is_double64species(BasicType bt, vmSymbols::SID sid) {
  return bt == T_OBJECT && sid == vmSymbols::VM_SYMBOL_ENUM_NAME(jdk_panama_vector_Double64Vector_Double64Species);
}
static bool is_double64mask(BasicType bt, vmSymbols::SID sid) {
  return bt == T_OBJECT && sid == vmSymbols::VM_SYMBOL_ENUM_NAME(jdk_panama_vector_Double64Vector_Double64Mask);
}
static bool is_double64shuffle(BasicType bt, vmSymbols::SID sid) {
  return bt == T_OBJECT && sid == vmSymbols::VM_SYMBOL_ENUM_NAME(jdk_panama_vector_Double64Vector_Double64Shuffle);
}
static bool is_double64(BasicType bt, vmSymbols::SID sid) {
  return is_double64vector(bt, sid) || is_double64species(bt, sid) || is_double64mask(bt, sid);
}
static bool is_double128vector(BasicType bt, vmSymbols::SID sid) {
  return bt == T_OBJECT && sid == vmSymbols::VM_SYMBOL_ENUM_NAME(jdk_panama_vector_Double128Vector);
}
static bool is_double128species(BasicType bt, vmSymbols::SID sid) {
  return bt == T_OBJECT && sid == vmSymbols::VM_SYMBOL_ENUM_NAME(jdk_panama_vector_Double128Vector_Double128Species);
}
static bool is_double128mask(BasicType bt, vmSymbols::SID sid) {
  return bt == T_OBJECT && sid == vmSymbols::VM_SYMBOL_ENUM_NAME(jdk_panama_vector_Double128Vector_Double128Mask);
}
static bool is_double128shuffle(BasicType bt, vmSymbols::SID sid) {
  return bt == T_OBJECT && sid == vmSymbols::VM_SYMBOL_ENUM_NAME(jdk_panama_vector_Double128Vector_Double128Shuffle);
}
static bool is_double128(BasicType bt, vmSymbols::SID sid) {
  return is_double128vector(bt, sid) || is_double128species(bt, sid) || is_double128mask(bt, sid);
}
static bool is_double256vector(BasicType bt, vmSymbols::SID sid) {
  return bt == T_OBJECT && sid == vmSymbols::VM_SYMBOL_ENUM_NAME(jdk_panama_vector_Double256Vector);
}
static bool is_double256species(BasicType bt, vmSymbols::SID sid) {
  return bt == T_OBJECT && sid == vmSymbols::VM_SYMBOL_ENUM_NAME(jdk_panama_vector_Double256Vector_Double256Species);
}
static bool is_double256mask(BasicType bt, vmSymbols::SID sid) {
  return bt == T_OBJECT && sid == vmSymbols::VM_SYMBOL_ENUM_NAME(jdk_panama_vector_Double256Vector_Double256Mask);
}
static bool is_double256shuffle(BasicType bt, vmSymbols::SID sid) {
  return bt == T_OBJECT && sid == vmSymbols::VM_SYMBOL_ENUM_NAME(jdk_panama_vector_Double256Vector_Double256Shuffle);
}
static bool is_double256(BasicType bt, vmSymbols::SID sid) {
  return is_double256vector(bt, sid) || is_double256species(bt, sid) || is_double256mask(bt, sid);
}
static bool is_double512vector(BasicType bt, vmSymbols::SID sid) {
  return bt == T_OBJECT && sid == vmSymbols::VM_SYMBOL_ENUM_NAME(jdk_panama_vector_Double512Vector);
}
static bool is_double512species(BasicType bt, vmSymbols::SID sid) {
  return bt == T_OBJECT && sid == vmSymbols::VM_SYMBOL_ENUM_NAME(jdk_panama_vector_Double512Vector_Double512Species);
}
static bool is_double512mask(BasicType bt, vmSymbols::SID sid) {
  return bt == T_OBJECT && sid == vmSymbols::VM_SYMBOL_ENUM_NAME(jdk_panama_vector_Double512Vector_Double512Mask);
}
static bool is_double512shuffle(BasicType bt, vmSymbols::SID sid) {
  return bt == T_OBJECT && sid == vmSymbols::VM_SYMBOL_ENUM_NAME(jdk_panama_vector_Double512Vector_Double512Shuffle);
}
static bool is_double512(BasicType bt, vmSymbols::SID sid) {
  return is_double512vector(bt, sid) || is_double512species(bt, sid) || is_double512mask(bt, sid);
}
static bool is_double_max_vector(BasicType bt, vmSymbols::SID sid) {
  return bt == T_OBJECT && sid == vmSymbols::VM_SYMBOL_ENUM_NAME(jdk_panama_vector_DoubleMaxVector);
}
static bool is_double_max_species(BasicType bt, vmSymbols::SID sid) {
  return bt == T_OBJECT && sid == vmSymbols::VM_SYMBOL_ENUM_NAME(jdk_panama_vector_DoubleMaxVector_DoubleMaxSpecies);
}
static bool is_double_max_mask(BasicType bt, vmSymbols::SID sid) {
  return bt == T_OBJECT && sid == vmSymbols::VM_SYMBOL_ENUM_NAME(jdk_panama_vector_DoubleMaxVector_DoubleMaxMask);
}
static bool is_double_max_shuffle(BasicType bt, vmSymbols::SID sid) {
  return bt == T_OBJECT && sid == vmSymbols::VM_SYMBOL_ENUM_NAME(jdk_panama_vector_DoubleMaxVector_DoubleMaxShuffle);
}
static bool is_double_max(BasicType bt, vmSymbols::SID sid) {
  return is_double_max_vector(bt, sid) || is_double_max_species(bt, sid) || is_double_max_mask(bt, sid);
}
static bool is_double_vec_or_mask(BasicType bt, vmSymbols::SID sid) {
  return is_double64(bt, sid) || is_double128(bt, sid) || is_double256(bt, sid) || is_double512(bt, sid) || is_double_max(bt, sid);
}
static bool is_int64vector(BasicType bt, vmSymbols::SID sid) {
  return bt == T_OBJECT && sid == vmSymbols::VM_SYMBOL_ENUM_NAME(jdk_panama_vector_Int64Vector);
}
static bool is_int64species(BasicType bt, vmSymbols::SID sid) {
  return bt == T_OBJECT && sid == vmSymbols::VM_SYMBOL_ENUM_NAME(jdk_panama_vector_Int64Vector_Int64Species);
}
static bool is_int64mask(BasicType bt, vmSymbols::SID sid) {
  return bt == T_OBJECT && sid == vmSymbols::VM_SYMBOL_ENUM_NAME(jdk_panama_vector_Int64Vector_Int64Mask);
}
static bool is_int64shuffle(BasicType bt, vmSymbols::SID sid) {
  return bt == T_OBJECT && sid == vmSymbols::VM_SYMBOL_ENUM_NAME(jdk_panama_vector_Int64Vector_Int64Shuffle);
}
static bool is_int64(BasicType bt, vmSymbols::SID sid) {
  return is_int64vector(bt, sid) || is_int64species(bt, sid) || is_int64mask(bt, sid);
}
static bool is_int128vector(BasicType bt, vmSymbols::SID sid) {
  return bt == T_OBJECT && sid == vmSymbols::VM_SYMBOL_ENUM_NAME(jdk_panama_vector_Int128Vector);
}
static bool is_int128species(BasicType bt, vmSymbols::SID sid) {
  return bt == T_OBJECT && sid == vmSymbols::VM_SYMBOL_ENUM_NAME(jdk_panama_vector_Int128Vector_Int128Species);
}
static bool is_int128mask(BasicType bt, vmSymbols::SID sid) {
  return bt == T_OBJECT && sid == vmSymbols::VM_SYMBOL_ENUM_NAME(jdk_panama_vector_Int128Vector_Int128Mask);
}
static bool is_int128shuffle(BasicType bt, vmSymbols::SID sid) {
  return bt == T_OBJECT && sid == vmSymbols::VM_SYMBOL_ENUM_NAME(jdk_panama_vector_Int128Vector_Int128Shuffle);
}
static bool is_int128(BasicType bt, vmSymbols::SID sid) {
  return is_int128vector(bt, sid) || is_int128species(bt, sid) || is_int128mask(bt, sid);
}
static bool is_int256vector(BasicType bt, vmSymbols::SID sid) {
  return bt == T_OBJECT && sid == vmSymbols::VM_SYMBOL_ENUM_NAME(jdk_panama_vector_Int256Vector);
}
static bool is_int256species(BasicType bt, vmSymbols::SID sid) {
  return bt == T_OBJECT && sid == vmSymbols::VM_SYMBOL_ENUM_NAME(jdk_panama_vector_Int256Vector_Int256Species);
}
static bool is_int256mask(BasicType bt, vmSymbols::SID sid) {
  return bt == T_OBJECT && sid == vmSymbols::VM_SYMBOL_ENUM_NAME(jdk_panama_vector_Int256Vector_Int256Mask);
}
static bool is_int256shuffle(BasicType bt, vmSymbols::SID sid) {
  return bt == T_OBJECT && sid == vmSymbols::VM_SYMBOL_ENUM_NAME(jdk_panama_vector_Int256Vector_Int256Shuffle);
}
static bool is_int256(BasicType bt, vmSymbols::SID sid) {
  return is_int256vector(bt, sid) || is_int256species(bt, sid) || is_int256mask(bt, sid);
}
static bool is_int512vector(BasicType bt, vmSymbols::SID sid) {
  return bt == T_OBJECT && sid == vmSymbols::VM_SYMBOL_ENUM_NAME(jdk_panama_vector_Int512Vector);
}
static bool is_int512species(BasicType bt, vmSymbols::SID sid) {
  return bt == T_OBJECT && sid == vmSymbols::VM_SYMBOL_ENUM_NAME(jdk_panama_vector_Int512Vector_Int512Species);
}
static bool is_int512mask(BasicType bt, vmSymbols::SID sid) {
  return bt == T_OBJECT && sid == vmSymbols::VM_SYMBOL_ENUM_NAME(jdk_panama_vector_Int512Vector_Int512Mask);
}
static bool is_int512shuffle(BasicType bt, vmSymbols::SID sid) {
  return bt == T_OBJECT && sid == vmSymbols::VM_SYMBOL_ENUM_NAME(jdk_panama_vector_Int512Vector_Int512Shuffle);
}
static bool is_int512(BasicType bt, vmSymbols::SID sid) {
  return is_int512vector(bt, sid) || is_int512species(bt, sid) || is_int512mask(bt, sid);
}
static bool is_int_max_vector(BasicType bt, vmSymbols::SID sid) {
  return bt == T_OBJECT && sid == vmSymbols::VM_SYMBOL_ENUM_NAME(jdk_panama_vector_IntMaxVector);
}
static bool is_int_max_species(BasicType bt, vmSymbols::SID sid) {
  return bt == T_OBJECT && sid == vmSymbols::VM_SYMBOL_ENUM_NAME(jdk_panama_vector_IntMaxVector_IntMaxSpecies);
}
static bool is_int_max_mask(BasicType bt, vmSymbols::SID sid) {
  return bt == T_OBJECT && sid == vmSymbols::VM_SYMBOL_ENUM_NAME(jdk_panama_vector_IntMaxVector_IntMaxMask);
}
static bool is_int_max_shuffle(BasicType bt, vmSymbols::SID sid) {
  return bt == T_OBJECT && sid == vmSymbols::VM_SYMBOL_ENUM_NAME(jdk_panama_vector_IntMaxVector_IntMaxShuffle);
}
static bool is_int_max(BasicType bt, vmSymbols::SID sid) {
  return is_int_max_vector(bt, sid) || is_int_max_species(bt, sid) || is_int_max_mask(bt, sid);
}
static bool is_int_vec_or_mask(BasicType bt, vmSymbols::SID sid) {
  return is_int64(bt, sid) || is_int128(bt, sid) || is_int256(bt, sid) || is_int512(bt, sid) || is_int_max(bt, sid);
}
static bool is_long64vector(BasicType bt, vmSymbols::SID sid) {
  return bt == T_OBJECT && sid == vmSymbols::VM_SYMBOL_ENUM_NAME(jdk_panama_vector_Long64Vector);
}
static bool is_long64species(BasicType bt, vmSymbols::SID sid) {
  return bt == T_OBJECT && sid == vmSymbols::VM_SYMBOL_ENUM_NAME(jdk_panama_vector_Long64Vector_Long64Species);
}
static bool is_long64mask(BasicType bt, vmSymbols::SID sid) {
  return bt == T_OBJECT && sid == vmSymbols::VM_SYMBOL_ENUM_NAME(jdk_panama_vector_Long64Vector_Long64Mask);
}
static bool is_long64shuffle(BasicType bt, vmSymbols::SID sid) {
  return bt == T_OBJECT && sid == vmSymbols::VM_SYMBOL_ENUM_NAME(jdk_panama_vector_Long64Vector_Long64Shuffle);
}
static bool is_long64(BasicType bt, vmSymbols::SID sid) {
  return is_long64vector(bt, sid) || is_long64species(bt, sid) || is_long64mask(bt, sid);
}
static bool is_long128vector(BasicType bt, vmSymbols::SID sid) {
  return bt == T_OBJECT && sid == vmSymbols::VM_SYMBOL_ENUM_NAME(jdk_panama_vector_Long128Vector);
}
static bool is_long128species(BasicType bt, vmSymbols::SID sid) {
  return bt == T_OBJECT && sid == vmSymbols::VM_SYMBOL_ENUM_NAME(jdk_panama_vector_Long128Vector_Long128Species);
}
static bool is_long128mask(BasicType bt, vmSymbols::SID sid) {
  return bt == T_OBJECT && sid == vmSymbols::VM_SYMBOL_ENUM_NAME(jdk_panama_vector_Long128Vector_Long128Mask);
}
static bool is_long128shuffle(BasicType bt, vmSymbols::SID sid) {
  return bt == T_OBJECT && sid == vmSymbols::VM_SYMBOL_ENUM_NAME(jdk_panama_vector_Long128Vector_Long128Shuffle);
}
static bool is_long128(BasicType bt, vmSymbols::SID sid) {
  return is_long128vector(bt, sid) || is_long128species(bt, sid) || is_long128mask(bt, sid);
}
static bool is_long256vector(BasicType bt, vmSymbols::SID sid) {
  return bt == T_OBJECT && sid == vmSymbols::VM_SYMBOL_ENUM_NAME(jdk_panama_vector_Long256Vector);
}
static bool is_long256species(BasicType bt, vmSymbols::SID sid) {
  return bt == T_OBJECT && sid == vmSymbols::VM_SYMBOL_ENUM_NAME(jdk_panama_vector_Long256Vector_Long256Species);
}
static bool is_long256mask(BasicType bt, vmSymbols::SID sid) {
  return bt == T_OBJECT && sid == vmSymbols::VM_SYMBOL_ENUM_NAME(jdk_panama_vector_Long256Vector_Long256Mask);
}
static bool is_long256shuffle(BasicType bt, vmSymbols::SID sid) {
  return bt == T_OBJECT && sid == vmSymbols::VM_SYMBOL_ENUM_NAME(jdk_panama_vector_Long256Vector_Long256Shuffle);
}
static bool is_long256(BasicType bt, vmSymbols::SID sid) {
  return is_long256vector(bt, sid) || is_long256species(bt, sid) || is_long256mask(bt, sid);
}
static bool is_long512vector(BasicType bt, vmSymbols::SID sid) {
  return bt == T_OBJECT && sid == vmSymbols::VM_SYMBOL_ENUM_NAME(jdk_panama_vector_Long512Vector);
}
static bool is_long512species(BasicType bt, vmSymbols::SID sid) {
  return bt == T_OBJECT && sid == vmSymbols::VM_SYMBOL_ENUM_NAME(jdk_panama_vector_Long512Vector_Long512Species);
}
static bool is_long512mask(BasicType bt, vmSymbols::SID sid) {
  return bt == T_OBJECT && sid == vmSymbols::VM_SYMBOL_ENUM_NAME(jdk_panama_vector_Long512Vector_Long512Mask);
}
static bool is_long512shuffle(BasicType bt, vmSymbols::SID sid) {
  return bt == T_OBJECT && sid == vmSymbols::VM_SYMBOL_ENUM_NAME(jdk_panama_vector_Long512Vector_Long512Shuffle);
}
static bool is_long512(BasicType bt, vmSymbols::SID sid) {
  return is_long512vector(bt, sid) || is_long512species(bt, sid) || is_long512mask(bt, sid);
}
static bool is_long_max_vector(BasicType bt, vmSymbols::SID sid) {
  return bt == T_OBJECT && sid == vmSymbols::VM_SYMBOL_ENUM_NAME(jdk_panama_vector_LongMaxVector);
}
static bool is_long_max_species(BasicType bt, vmSymbols::SID sid) {
  return bt == T_OBJECT && sid == vmSymbols::VM_SYMBOL_ENUM_NAME(jdk_panama_vector_LongMaxVector_LongMaxSpecies);
}
static bool is_long_max_mask(BasicType bt, vmSymbols::SID sid) {
  return bt == T_OBJECT && sid == vmSymbols::VM_SYMBOL_ENUM_NAME(jdk_panama_vector_LongMaxVector_LongMaxMask);
}
static bool is_long_max_shuffle(BasicType bt, vmSymbols::SID sid) {
  return bt == T_OBJECT && sid == vmSymbols::VM_SYMBOL_ENUM_NAME(jdk_panama_vector_LongMaxVector_LongMaxShuffle);
}
static bool is_long_max(BasicType bt, vmSymbols::SID sid) {
  return is_long_max_vector(bt, sid) || is_long_max_species(bt, sid) || is_long_max_mask(bt, sid);
}
static bool is_long_vec_or_mask(BasicType bt, vmSymbols::SID sid) {
  return is_long64(bt, sid) || is_long128(bt, sid) || is_long256(bt, sid) || is_long512(bt, sid) || is_long_max(bt, sid);
}
static bool is_byte64vector(BasicType bt, vmSymbols::SID sid) {
  return bt == T_OBJECT && sid == vmSymbols::VM_SYMBOL_ENUM_NAME(jdk_panama_vector_Byte64Vector);
}
static bool is_byte64species(BasicType bt, vmSymbols::SID sid) {
  return bt == T_OBJECT && sid == vmSymbols::VM_SYMBOL_ENUM_NAME(jdk_panama_vector_Byte64Vector_Byte64Species);
}
static bool is_byte64mask(BasicType bt, vmSymbols::SID sid) {
  return bt == T_OBJECT && sid == vmSymbols::VM_SYMBOL_ENUM_NAME(jdk_panama_vector_Byte64Vector_Byte64Mask);
}
static bool is_byte64shuffle(BasicType bt, vmSymbols::SID sid) {
  return bt == T_OBJECT && sid == vmSymbols::VM_SYMBOL_ENUM_NAME(jdk_panama_vector_Byte64Vector_Byte64Shuffle);
}
static bool is_byte64(BasicType bt, vmSymbols::SID sid) {
  return is_byte64vector(bt, sid) || is_byte64species(bt, sid) || is_byte64mask(bt, sid);
}
static bool is_byte128vector(BasicType bt, vmSymbols::SID sid) {
  return bt == T_OBJECT && sid == vmSymbols::VM_SYMBOL_ENUM_NAME(jdk_panama_vector_Byte128Vector);
}
static bool is_byte128species(BasicType bt, vmSymbols::SID sid) {
  return bt == T_OBJECT && sid == vmSymbols::VM_SYMBOL_ENUM_NAME(jdk_panama_vector_Byte128Vector_Byte128Species);
}
static bool is_byte128mask(BasicType bt, vmSymbols::SID sid) {
  return bt == T_OBJECT && sid == vmSymbols::VM_SYMBOL_ENUM_NAME(jdk_panama_vector_Byte128Vector_Byte128Mask);
}
static bool is_byte128shuffle(BasicType bt, vmSymbols::SID sid) {
  return bt == T_OBJECT && sid == vmSymbols::VM_SYMBOL_ENUM_NAME(jdk_panama_vector_Byte128Vector_Byte128Shuffle);
}
static bool is_byte128(BasicType bt, vmSymbols::SID sid) {
  return is_byte128vector(bt, sid) || is_byte128species(bt, sid) || is_byte128mask(bt, sid);
}
static bool is_byte256vector(BasicType bt, vmSymbols::SID sid) {
  return bt == T_OBJECT && sid == vmSymbols::VM_SYMBOL_ENUM_NAME(jdk_panama_vector_Byte256Vector);
}
static bool is_byte256species(BasicType bt, vmSymbols::SID sid) {
  return bt == T_OBJECT && sid == vmSymbols::VM_SYMBOL_ENUM_NAME(jdk_panama_vector_Byte256Vector_Byte256Species);
}
static bool is_byte256mask(BasicType bt, vmSymbols::SID sid) {
  return bt == T_OBJECT && sid == vmSymbols::VM_SYMBOL_ENUM_NAME(jdk_panama_vector_Byte256Vector_Byte256Mask);
}
static bool is_byte256shuffle(BasicType bt, vmSymbols::SID sid) {
  return bt == T_OBJECT && sid == vmSymbols::VM_SYMBOL_ENUM_NAME(jdk_panama_vector_Byte256Vector_Byte256Shuffle);
}
static bool is_byte256(BasicType bt, vmSymbols::SID sid) {
  return is_byte256vector(bt, sid) || is_byte256species(bt, sid) || is_byte256mask(bt, sid);
}
static bool is_byte512vector(BasicType bt, vmSymbols::SID sid) {
  return bt == T_OBJECT && sid == vmSymbols::VM_SYMBOL_ENUM_NAME(jdk_panama_vector_Byte512Vector);
}
static bool is_byte512species(BasicType bt, vmSymbols::SID sid) {
  return bt == T_OBJECT && sid == vmSymbols::VM_SYMBOL_ENUM_NAME(jdk_panama_vector_Byte512Vector_Byte512Species);
}
static bool is_byte512mask(BasicType bt, vmSymbols::SID sid) {
  return bt == T_OBJECT && sid == vmSymbols::VM_SYMBOL_ENUM_NAME(jdk_panama_vector_Byte512Vector_Byte512Mask);
}
static bool is_byte512shuffle(BasicType bt, vmSymbols::SID sid) {
  return bt == T_OBJECT && sid == vmSymbols::VM_SYMBOL_ENUM_NAME(jdk_panama_vector_Byte512Vector_Byte512Shuffle);
}
static bool is_byte512(BasicType bt, vmSymbols::SID sid) {
  return is_byte512vector(bt, sid) || is_byte512species(bt, sid) || is_byte512mask(bt, sid);
}
static bool is_byte_max_vector(BasicType bt, vmSymbols::SID sid) {
  return bt == T_OBJECT && sid == vmSymbols::VM_SYMBOL_ENUM_NAME(jdk_panama_vector_ByteMaxVector);
}
static bool is_byte_max_species(BasicType bt, vmSymbols::SID sid) {
  return bt == T_OBJECT && sid == vmSymbols::VM_SYMBOL_ENUM_NAME(jdk_panama_vector_ByteMaxVector_ByteMaxSpecies);
}
static bool is_byte_max_mask(BasicType bt, vmSymbols::SID sid) {
  return bt == T_OBJECT && sid == vmSymbols::VM_SYMBOL_ENUM_NAME(jdk_panama_vector_ByteMaxVector_ByteMaxMask);
}
static bool is_byte_max_shuffle(BasicType bt, vmSymbols::SID sid) {
  return bt == T_OBJECT && sid == vmSymbols::VM_SYMBOL_ENUM_NAME(jdk_panama_vector_ByteMaxVector_ByteMaxShuffle);
}
static bool is_byte_max(BasicType bt, vmSymbols::SID sid) {
  return is_byte_max_vector(bt, sid) || is_byte_max_species(bt, sid) || is_byte_max_mask(bt, sid);
}
static bool is_byte_vec_or_mask(BasicType bt, vmSymbols::SID sid) {
  return is_byte64(bt, sid) || is_byte128(bt, sid) || is_byte256(bt, sid) || is_byte512(bt, sid) || is_byte_max(bt, sid);
}
static bool is_short64vector(BasicType bt, vmSymbols::SID sid) {
  return bt == T_OBJECT && sid == vmSymbols::VM_SYMBOL_ENUM_NAME(jdk_panama_vector_Short64Vector);
}
static bool is_short64species(BasicType bt, vmSymbols::SID sid) {
  return bt == T_OBJECT && sid == vmSymbols::VM_SYMBOL_ENUM_NAME(jdk_panama_vector_Short64Vector_Short64Species);
}
static bool is_short64mask(BasicType bt, vmSymbols::SID sid) {
  return bt == T_OBJECT && sid == vmSymbols::VM_SYMBOL_ENUM_NAME(jdk_panama_vector_Short64Vector_Short64Mask);
}
static bool is_short64shuffle(BasicType bt, vmSymbols::SID sid) {
  return bt == T_OBJECT && sid == vmSymbols::VM_SYMBOL_ENUM_NAME(jdk_panama_vector_Short64Vector_Short64Shuffle);
}
static bool is_short64(BasicType bt, vmSymbols::SID sid) {
  return is_short64vector(bt, sid) || is_short64species(bt, sid) || is_short64mask(bt, sid);
}
static bool is_short128vector(BasicType bt, vmSymbols::SID sid) {
  return bt == T_OBJECT && sid == vmSymbols::VM_SYMBOL_ENUM_NAME(jdk_panama_vector_Short128Vector);
}
static bool is_short128species(BasicType bt, vmSymbols::SID sid) {
  return bt == T_OBJECT && sid == vmSymbols::VM_SYMBOL_ENUM_NAME(jdk_panama_vector_Short128Vector_Short128Species);
}
static bool is_short128mask(BasicType bt, vmSymbols::SID sid) {
  return bt == T_OBJECT && sid == vmSymbols::VM_SYMBOL_ENUM_NAME(jdk_panama_vector_Short128Vector_Short128Mask);
}
static bool is_short128shuffle(BasicType bt, vmSymbols::SID sid) {
  return bt == T_OBJECT && sid == vmSymbols::VM_SYMBOL_ENUM_NAME(jdk_panama_vector_Short128Vector_Short128Shuffle);
}
static bool is_short128(BasicType bt, vmSymbols::SID sid) {
  return is_short128vector(bt, sid) || is_short128species(bt, sid) || is_short128mask(bt, sid);
}
static bool is_short256vector(BasicType bt, vmSymbols::SID sid) {
  return bt == T_OBJECT && sid == vmSymbols::VM_SYMBOL_ENUM_NAME(jdk_panama_vector_Short256Vector);
}
static bool is_short256species(BasicType bt, vmSymbols::SID sid) {
  return bt == T_OBJECT && sid == vmSymbols::VM_SYMBOL_ENUM_NAME(jdk_panama_vector_Short256Vector_Short256Species);
}
static bool is_short256mask(BasicType bt, vmSymbols::SID sid) {
  return bt == T_OBJECT && sid == vmSymbols::VM_SYMBOL_ENUM_NAME(jdk_panama_vector_Short256Vector_Short256Mask);
}
static bool is_short256shuffle(BasicType bt, vmSymbols::SID sid) {
  return bt == T_OBJECT && sid == vmSymbols::VM_SYMBOL_ENUM_NAME(jdk_panama_vector_Short256Vector_Short256Shuffle);
}
static bool is_short256(BasicType bt, vmSymbols::SID sid) {
  return is_short256vector(bt, sid) || is_short256species(bt, sid) || is_short256mask(bt, sid);
}
static bool is_short512vector(BasicType bt, vmSymbols::SID sid) {
  return bt == T_OBJECT && sid == vmSymbols::VM_SYMBOL_ENUM_NAME(jdk_panama_vector_Short512Vector);
}
static bool is_short512species(BasicType bt, vmSymbols::SID sid) {
  return bt == T_OBJECT && sid == vmSymbols::VM_SYMBOL_ENUM_NAME(jdk_panama_vector_Short512Vector_Short512Species);
}
static bool is_short512mask(BasicType bt, vmSymbols::SID sid) {
  return bt == T_OBJECT && sid == vmSymbols::VM_SYMBOL_ENUM_NAME(jdk_panama_vector_Short512Vector_Short512Mask);
}
static bool is_short512shuffle(BasicType bt, vmSymbols::SID sid) {
  return bt == T_OBJECT && sid == vmSymbols::VM_SYMBOL_ENUM_NAME(jdk_panama_vector_Short512Vector_Short512Shuffle);
}
static bool is_short512(BasicType bt, vmSymbols::SID sid) {
  return is_short512vector(bt, sid) || is_short512species(bt, sid) || is_short512mask(bt, sid);
}
static bool is_short_max_vector(BasicType bt, vmSymbols::SID sid) {
  return bt == T_OBJECT && sid == vmSymbols::VM_SYMBOL_ENUM_NAME(jdk_panama_vector_ShortMaxVector);
}
static bool is_short_max_species(BasicType bt, vmSymbols::SID sid) {
  return bt == T_OBJECT && sid == vmSymbols::VM_SYMBOL_ENUM_NAME(jdk_panama_vector_ShortMaxVector_ShortMaxSpecies);
}
static bool is_short_max_mask(BasicType bt, vmSymbols::SID sid) {
  return bt == T_OBJECT && sid == vmSymbols::VM_SYMBOL_ENUM_NAME(jdk_panama_vector_ShortMaxVector_ShortMaxMask);
}
static bool is_short_max_shuffle(BasicType bt, vmSymbols::SID sid) {
  return bt == T_OBJECT && sid == vmSymbols::VM_SYMBOL_ENUM_NAME(jdk_panama_vector_ShortMaxVector_ShortMaxShuffle);
}
static bool is_short_max(BasicType bt, vmSymbols::SID sid) {
  return is_short_max_vector(bt, sid) || is_short_max_species(bt, sid) || is_short_max_mask(bt, sid);
}
static bool is_short_vec_or_mask(BasicType bt, vmSymbols::SID sid) {
  return is_short64(bt, sid) || is_short128(bt, sid) || is_short256(bt, sid) || is_short512(bt, sid) || is_short_max(bt, sid);
}

#define __ basic_type(), as_klass()->name()->sid()

bool ciType::is_vectormask() {
  return basic_type() == T_OBJECT &&
      (is_float64mask(__) || is_float128mask(__) || is_float256mask(__) || is_float512mask(__) || is_float_max_mask(__) ||
       is_double64mask(__) || is_double128mask(__) || is_double256mask(__) || is_double512mask(__) || is_double_max_mask(__) ||
       is_int64mask(__) || is_int128mask(__) || is_int256mask(__) || is_int512mask(__) || is_int_max_mask(__) ||
       is_long64mask(__) || is_long128mask(__) || is_long256mask(__) || is_long512mask(__) || is_long_max_mask(__) ||
       is_byte64mask(__) || is_byte128mask(__) || is_byte256mask(__) || is_byte512mask(__) || is_byte_max_mask(__) ||
       is_short64mask(__) || is_short128mask(__) || is_short256mask(__) || is_short512mask(__) ||  is_short_max_mask(__));
}

bool ciType::is_vectorshuffle() {
  return basic_type() == T_OBJECT &&
      (is_float64shuffle(__) || is_float128shuffle(__) || is_float256shuffle(__) || is_float512shuffle(__) || is_float_max_shuffle(__) ||
       is_double64shuffle(__) || is_double128shuffle(__) || is_double256shuffle(__) || is_double512shuffle(__) || is_double_max_shuffle(__) ||
       is_int64shuffle(__) || is_int128shuffle(__) || is_int256shuffle(__) || is_int512shuffle(__) || is_int_max_shuffle(__) ||
       is_long64shuffle(__) || is_long128shuffle(__) || is_long256shuffle(__) || is_long512shuffle(__) || is_long_max_shuffle(__) ||
       is_byte64shuffle(__) || is_byte128shuffle(__) || is_byte256shuffle(__) || is_byte512shuffle(__) || is_byte_max_shuffle(__) ||
       is_short64shuffle(__) || is_short128shuffle(__) || is_short256shuffle(__) || is_short512shuffle(__) || is_short_max_shuffle(__));
}

bool ciType::is_vectorapi_vector() {
  return is_float_vec_or_mask(__) ||
      is_double_vec_or_mask(__) ||
      is_int_vec_or_mask(__) ||
      is_long_vec_or_mask(__) ||
      is_byte_vec_or_mask(__) ||
      is_short_vec_or_mask(__);
}

int ciType::vectorapi_vector_size() {
  if ( is_double64(__) || is_long64(__)) return 1;
  if ( is_int64(__) || is_float64(__) || is_long128(__) || is_double128(__) ) return 2;
  if ( is_short64(__) || is_int128(__) || is_float128(__) || is_long256(__) || is_double256(__) ) return 4;
  if ( is_byte64(__) || is_short128(__) || is_int256(__) || is_float256(__) || is_long512(__) || is_double512(__) ) return 8;
  if ( is_byte128(__) || is_short256(__) || is_int512(__) || is_float512(__) ) return 16;
  if ( is_byte256(__) || is_short512(__) ) return 32;
  if ( is_byte512(__) ) return 64;
#ifdef COMPILER2
  if ( is_double_max(__)) return Matcher::max_vector_size(T_DOUBLE);
  if ( is_long_max(__)) return Matcher::max_vector_size(T_LONG);
  if ( is_float_max(__)) return Matcher::max_vector_size(T_FLOAT);
  if ( is_int_max(__)) return Matcher::max_vector_size(T_INT);
  if ( is_short_max(__)) return Matcher::max_vector_size(T_SHORT);
  if ( is_byte_max(__)) return Matcher::max_vector_size(T_BYTE);
#endif
  return -1;
}

BasicType ciType::vectorapi_vector_bt() {
  if ( is_float_vec_or_mask(__) ) return T_FLOAT;
  if ( is_double_vec_or_mask(__) ) return T_DOUBLE;
  if ( is_int_vec_or_mask(__) ) return T_INT;
  if ( is_long_vec_or_mask(__) ) return T_LONG;
  if ( is_byte_vec_or_mask(__) ) return T_BYTE;
  if ( is_short_vec_or_mask(__) ) return T_SHORT;
  return T_VOID;
}

#undef __

// ciReturnAddress
//
// This class represents the type of a specific return address in the
// bytecodes.

// ------------------------------------------------------------------
// ciReturnAddress::ciReturnAddress
//
ciReturnAddress::ciReturnAddress(int bci) : ciType(T_ADDRESS) {
  assert(0 <= bci, "bci cannot be negative");
  _bci = bci;
}

// ------------------------------------------------------------------
// ciReturnAddress::print_impl
//
// Implementation of the print method.
void ciReturnAddress::print_impl(outputStream* st) {
  st->print(" bci=%d", _bci);
}

// ------------------------------------------------------------------
// ciReturnAddress::make
ciReturnAddress* ciReturnAddress::make(int bci) {
  GUARDED_VM_ENTRY(return CURRENT_ENV->get_return_address(bci);)
}
