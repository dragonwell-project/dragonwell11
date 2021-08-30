/*
 * Copyright 2001-2010 Sun Microsystems, Inc.  All Rights Reserved.
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
 * Please contact Sun Microsystems, Inc., 4150 Network Circle, Santa Clara,
 * CA 95054 USA or visit www.sun.com if you need additional information or
 * have any questions.
 *
 */

#include "precompiled.hpp"
#include "prims/privilegedStack.hpp"
#include "runtime/coroutine.hpp"
#include "runtime/globals.hpp"
#include "runtime/interfaceSupport.inline.hpp"
#include "runtime/jniHandles.inline.hpp"
#include "runtime/objectMonitor.hpp"
#include "runtime/objectMonitor.inline.hpp"
#include "services/threadService.hpp"

#include CPU_HEADER(coroutine)
#include CPU_HEADER_INLINE(vmreg)

void Coroutine::set_coroutine_base(intptr_t **&base, JavaThread* thread, jobject obj, Coroutine *coro, oop coroutineObj, address coroutine_start) {
  *(--base) = NULL;          // Make it to be 16 bytes(original is 8*5=40 bytes) aligned which be required by some instruction likes movaps otherwise we will incur crash.
  *(--base) = NULL;
  *(--base) = (intptr_t*)obj;
  *(--base) = (intptr_t*)coro;
  *(--base) = NULL;
  *(--base) = (intptr_t*)coroutine_start;
  *(--base) = NULL;
}

Register CoroutineStack::get_fp_reg() {
  return rbp;
}