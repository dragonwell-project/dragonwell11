/*
 * Copyright Amazon.com Inc. or its affiliates. All rights reserved.
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
 */

#include "precompiled.hpp"
#include "oops/objArrayOop.hpp"
#include "unittest.hpp"
#include "utilities/globalDefinitions.hpp"

TEST_VM(objArrayOop, osize) {
  static const struct {
    int objal; bool coh; bool ccp; bool coops; int result;
  } x[] = {
//    ObjAligInB, UseCOH, UseCCP, UseCoops, object size in heap words
#ifdef _LP64
    { 8,          true,   true,   false,    3 },  // 16 byte header, 8 byte oops
    { 8,          true,   true,   true,     2 },  // 12 byte header, 4 byte oops
    { 8,          false,  false,  false,    4 },  // 24 byte header, 8 byte oops
    { 8,          false,  false,  true,     4 },  // 24 byte header, 4 byte oops
    { 8,          false,  true,   false,    3 },  // 16 byte header, 8 byte oops
    { 8,          false,  true,   true,     3 },  // 16 byte header, 4 byte oops
#endif
    { -1,         false,  false,   -1 }
  };
  for (int i = 0; x[i].result != -1; i++) {
    if (x[i].objal == (int)ObjectAlignmentInBytes && x[i].coh == UseCompactObjectHeaders && x[i].ccp == UseCompressedClassPointers && x[i].coops == UseCompressedOops) {
      EXPECT_EQ(objArrayOopDesc::object_size(1), x[i].result);
    }
  }
}
