/*
 * Copyright (c) 2020, 2022 Oracle and/or its affiliates. All rights reserved.
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
#include "opto/c2_MacroAssembler.hpp"
#include "opto/c2_CodeStubs.hpp"
#include "runtime/objectMonitor.hpp"
#include "runtime/sharedRuntime.hpp"
#include "runtime/stubRoutines.hpp"

#define __ masm.

#ifdef _LP64
int C2HandleAnonOMOwnerStub::max_size() const {
  // Max size of stub has been determined by testing with 0, in which case
  // C2CodeStubList::emit() will throw an assertion and report the actual size that
  // is needed.
  if (UseWispMonitor) {
    return DEBUG_ONLY(50) NOT_DEBUG(35);
  }
  return DEBUG_ONLY(36) NOT_DEBUG(21);
}

void C2HandleAnonOMOwnerStub::emit(C2_MacroAssembler& masm) {
  __ bind(entry());
  Register mon = monitor();
  Register t = tmp();
  if (UseWispMonitor) {
    __ movptr(r15_thread, Address(r15_thread, JavaThread::current_coroutine_offset()));
    __ movptr(r15_thread, Address(r15_thread, Coroutine::wisp_thread_offset()));
  }
  __ movptr(Address(mon, OM_OFFSET_NO_MONITOR_VALUE_TAG(owner)), r15_thread);
  if (UseWispMonitor) {
    __ movptr(r15_thread, Address(r15_thread, WispThread::thread_offset()));
  }
  __ subl(Address(r15_thread, JavaThread::lock_stack_top_offset()), oopSize);
#ifdef ASSERT
  __ movl(t, Address(r15_thread, JavaThread::lock_stack_top_offset()));
  __ movptr(Address(r15_thread, t), 0);
#endif
  __ jmp(continuation());
}
#endif

int C2LoadNKlassStub::max_size() const {
  return 10;
}

void C2LoadNKlassStub::emit(C2_MacroAssembler& masm) {
  __ bind(entry());
  Register d = dst();
  __ movq(d, Address(d, OM_OFFSET_NO_MONITOR_VALUE_TAG(header)));
  __ jmp(continuation());
}
#undef __
