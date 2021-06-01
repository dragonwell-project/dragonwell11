/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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
#include "classfile/classLoaderData.hpp"
#include "gc/z/zAddress.hpp"
#include "gc/z/zHeap.inline.hpp"
#include "gc/z/zOop.hpp"
#include "gc/z/zPageAllocator.hpp"
#include "gc/z/zResurrection.hpp"
#include "gc/z/zRootsIterator.hpp"
#include "gc/z/zStat.hpp"
#include "gc/z/zVerify.hpp"
#include "memory/iterator.inline.hpp"
#include "oops/oop.hpp"

template <bool Map>
class ZPageDebugMapOrUnmapClosure : public ZPageClosure {
private:
  const ZPageAllocator* const _allocator;

public:
  ZPageDebugMapOrUnmapClosure(const ZPageAllocator* allocator) :
      _allocator(allocator) {}

  void do_page(const ZPage* page) {
    if (Map) {
      _allocator->debug_map_page(page);
    } else {
      _allocator->debug_unmap_page(page);
    }
  }
};

ZVerifyViewsFlip::ZVerifyViewsFlip(const ZPageAllocator* allocator) :
    _allocator(allocator) {
  if (ZVerifyViews) {
    // Unmap all pages
    ZPageDebugMapOrUnmapClosure<false /* Map */> cl(_allocator);
    ZHeap::heap()->pages_do(&cl);
  }
}

ZVerifyViewsFlip::~ZVerifyViewsFlip() {
  if (ZVerifyViews) {
    // Map all pages
    ZPageDebugMapOrUnmapClosure<true /* Map */> cl(_allocator);
    ZHeap::heap()->pages_do(&cl);
  }
}
