/*
 * Copyright (c) 2015, 2020, Oracle and/or its affiliates. All rights reserved.
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
#include "gc/z/zAddress.inline.hpp"
#include "gc/z/zGlobals.hpp"
#include "gc/z/zLargePages.inline.hpp"
#include "gc/z/zNUMA.inline.hpp"
#include "gc/z/zPhysicalMemory.inline.hpp"
#include "gc/z/zUtils.inline.hpp"
#include "logging/log.hpp"
#include "runtime/globals.hpp"
#include "runtime/globals_extension.hpp"
#include "runtime/init.hpp"
#include "runtime/os.hpp"
#include "services/memTracker.hpp"
#include "utilities/align.hpp"
#include "utilities/debug.hpp"
#include "utilities/globalDefinitions.hpp"

ZPhysicalMemory::ZPhysicalMemory() :
    _nsegments_max(0),
    _nsegments(0),
    _segments(NULL) {}

ZPhysicalMemory::ZPhysicalMemory(const ZPhysicalMemorySegment& segment) :
    _nsegments_max(0),
    _nsegments(0),
    _segments(NULL) {
  add_segment(segment);
}

ZPhysicalMemory::ZPhysicalMemory(const ZPhysicalMemory& pmem) :
    _nsegments_max(0),
    _nsegments(0),
    _segments(NULL) {
  add_segments(pmem);
}

const ZPhysicalMemory& ZPhysicalMemory::operator=(const ZPhysicalMemory& pmem) {
  remove_segments();
  add_segments(pmem);
  return *this;
}

ZPhysicalMemory::~ZPhysicalMemory() {
  remove_segments();
}

size_t ZPhysicalMemory::size() const {
  size_t size = 0;

  for (uint32_t i = 0; i < _nsegments; i++) {
    size += _segments[i].size();
  }

  return size;
}

void ZPhysicalMemory::insert_segment(uint32_t index, uintptr_t start, size_t size, bool committed) {
  assert(index <= _nsegments, "Invalid index");

  ZPhysicalMemorySegment* const from_segments = _segments;

  if (_nsegments + 1 > _nsegments_max) {
    // Resize array
    _nsegments_max = ZUtils::round_up_power_of_2(_nsegments_max + 1);
    _segments = new ZPhysicalMemorySegment[_nsegments_max];

    // Copy segments before index
    for (uint32_t i = 0; i < index; i++) {
      _segments[i] = from_segments[i];
    }
  }

  // Copy/Move segments after index
  for (uint32_t i = _nsegments; i > index; i--) {
    _segments[i] = from_segments[i - 1];
  }

  // Insert new segment
  _segments[index] = ZPhysicalMemorySegment(start, size, committed);
  _nsegments++;

  // Delete old array
  if (from_segments != _segments) {
    delete [] from_segments;
  }
}

void ZPhysicalMemory::replace_segment(uint32_t index, uintptr_t start, size_t size, bool committed) {
  assert(index < _nsegments, "Invalid index");
  _segments[index] = ZPhysicalMemorySegment(start, size, committed);;
}

void ZPhysicalMemory::remove_segment(uint32_t index) {
  assert(index < _nsegments, "Invalid index");

  // Move segments after index
  for (uint32_t i = index + 1; i < _nsegments; i++) {
    _segments[i - 1] = _segments[i];
  }

  _nsegments--;
}

void ZPhysicalMemory::add_segments(const ZPhysicalMemory& pmem) {
  for (uint32_t i = 0; i < pmem.nsegments(); i++) {
    add_segment(pmem.segment(i));
  }
}

void ZPhysicalMemory::remove_segments() {
  delete [] _segments;
  _segments = NULL;
  _nsegments_max = 0;
  _nsegments = 0;
}

static bool is_mergable(const ZPhysicalMemorySegment& before, const ZPhysicalMemorySegment& after) {
  return before.end() == after.start() && before.is_committed() == after.is_committed();
}

void ZPhysicalMemory::add_segment(const ZPhysicalMemorySegment& segment) {
  // Insert segments in address order, merge segments when possible
  for (uint32_t i = _nsegments; i > 0; i--) {
    const uint32_t current = i - 1;

    if (_segments[current].end() <= segment.start()) {
      if (is_mergable(_segments[current], segment)) {
        if (current + 1 < _nsegments && is_mergable(segment, _segments[current + 1])) {
          // Merge with end of current segment and start of next segment
          const size_t start = _segments[current].start();
          const size_t size = _segments[current].size() + segment.size() + _segments[current + 1].size();
          replace_segment(current, start, size, segment.is_committed());
          remove_segment(current + 1);
          return;
        }

        // Merge with end of current segment
        const size_t start = _segments[current].start();
        const size_t size = _segments[current].size() + segment.size();
        replace_segment(current, start, size, segment.is_committed());
        return;
      } else if (current + 1 < _nsegments && is_mergable(segment, _segments[current + 1])) {
        // Merge with start of next segment
        const size_t start = segment.start();
        const size_t size = segment.size() + _segments[current + 1].size();
        replace_segment(current + 1, start, size, segment.is_committed());
        return;
      }

      // Insert after current segment
      insert_segment(current + 1, segment.start(), segment.size(), segment.is_committed());
      return;
    }
  }

  if (_nsegments > 0 && is_mergable(segment, _segments[0])) {
    // Merge with start of first segment
    const size_t start = segment.start();
    const size_t size = segment.size() + _segments[0].size();
    replace_segment(0, start, size, segment.is_committed());
    return;
  }

  // Insert before first segment
  insert_segment(0, segment.start(), segment.size(), segment.is_committed());
}

bool ZPhysicalMemory::commit_segment(uint32_t index, size_t size) {
  assert(index < _nsegments, "Invalid index");
  assert(size <= _segments[index].size(), "Invalid size");
  assert(!_segments[index].is_committed(), "Invalid state");

  if (size == _segments[index].size()) {
    // Completely committed
    _segments[index].set_committed(true);
    return true;
  }

  if (size > 0) {
    // Partially committed, split segment
    insert_segment(index + 1, _segments[index].start() + size, _segments[index].size() - size, false /* committed */);
    replace_segment(index, _segments[index].start(), size, true /* committed */);
  }

  return false;
}

bool ZPhysicalMemory::uncommit_segment(uint32_t index, size_t size) {
  assert(index < _nsegments, "Invalid index");
  assert(size <= _segments[index].size(), "Invalid size");
  assert(_segments[index].is_committed(), "Invalid state");

  if (size == _segments[index].size()) {
    // Completely uncommitted
    _segments[index].set_committed(false);
    return true;
  }

  if (size > 0) {
    // Partially uncommitted, split segment
    insert_segment(index + 1, _segments[index].start() + size, _segments[index].size() - size, true /* committed */);
    replace_segment(index, _segments[index].start(), size, false /* committed */);
  }

  return false;
}

ZPhysicalMemory ZPhysicalMemory::split(size_t size) {
  ZPhysicalMemory pmem;
  uint32_t nsegments = 0;

  for (uint32_t i = 0; i < _nsegments; i++) {
    const ZPhysicalMemorySegment& segment = _segments[i];
    if (pmem.size() < size) {
      if (pmem.size() + segment.size() <= size) {
        // Transfer segment
        pmem.add_segment(segment);
      } else {
        // Split segment
        const size_t split_size = size - pmem.size();
        pmem.add_segment(ZPhysicalMemorySegment(segment.start(), split_size, segment.is_committed()));
        _segments[nsegments++] = ZPhysicalMemorySegment(segment.start() + split_size, segment.size() - split_size, segment.is_committed());
      }
    } else {
      // Keep segment
      _segments[nsegments++] = segment;
    }
  }

  _nsegments = nsegments;

  return pmem;
}

ZPhysicalMemory ZPhysicalMemory::split_committed() {
  ZPhysicalMemory pmem;
  uint32_t nsegments = 0;

  for (uint32_t i = 0; i < _nsegments; i++) {
    const ZPhysicalMemorySegment& segment = _segments[i];
    if (segment.is_committed()) {
      // Transfer segment
      pmem.add_segment(segment);
    } else {
      // Keep segment
      _segments[nsegments++] = segment;
    }
  }

  _nsegments = nsegments;

  return pmem;
}

ZPhysicalMemoryManager::ZPhysicalMemoryManager(size_t max_capacity) :
    _backing(max_capacity) {
  // Make the whole range free
  _manager.free(0, max_capacity);
}

bool ZPhysicalMemoryManager::is_initialized() const {
  return _backing.is_initialized();
}

void ZPhysicalMemoryManager::warn_commit_limits(size_t max_capacity) const {
  _backing.warn_commit_limits(max_capacity);
}

void ZPhysicalMemoryManager::try_enable_uncommit(size_t min_capacity, size_t max_capacity) {
  assert(!is_init_completed(), "Invalid state");

  // If uncommit is not explicitly disabled, max capacity is greater than
  // min capacity, and uncommit is supported by the platform, then uncommit
  // will be enabled.
  if (!ZUncommit) {
    log_info(gc, init)("Uncommit: Disabled");
    return;
  }

  if (max_capacity == min_capacity) {
    log_info(gc, init)("Uncommit: Implicitly Disabled (-Xms equals -Xmx)");
    FLAG_SET_ERGO(bool, ZUncommit, false);
    return;
  }

  // Test if uncommit is supported by the operating system by committing
  // and then uncommitting a granule.
  ZPhysicalMemory pmem(ZPhysicalMemorySegment(0, ZGranuleSize, false /* committed */));
  if (!commit(pmem) || !uncommit(pmem)) {
    log_info(gc, init)("Uncommit: Implicitly Disabled (Not supported by operating system)");
    FLAG_SET_ERGO(bool, ZUncommit, false);
    return;
  }

  log_info(gc, init)("Uncommit: Enabled");
  log_info(gc, init)("Uncommit Delay: " UINTX_FORMAT "s", ZUncommitDelay);
}

void ZPhysicalMemoryManager::nmt_commit(uintptr_t offset, size_t size) const {
  if (ZMemoryTagging) {
    MemTracker::record_virtual_memory_commit((void*)offset, size, CALLER_PC);
  } else {
    // From an NMT point of view we treat the first heap view (marked0) as committed
    const uintptr_t addr = ZAddress::marked0(offset);
    MemTracker::record_virtual_memory_commit((void*)addr, size, CALLER_PC);
  }
}

void ZPhysicalMemoryManager::nmt_uncommit(uintptr_t offset, size_t size) const {
  if (MemTracker::tracking_level() > NMT_minimal) {
    Tracker tracker(Tracker::uncommit);
    if (ZMemoryTagging) {
      tracker.record((address)offset, size);
    } else {
      const uintptr_t addr = ZAddress::marked0(offset);
      tracker.record((address)addr, size);
    }
  }
}

void ZPhysicalMemoryManager::alloc(ZPhysicalMemory& pmem, size_t size) {
  assert(is_aligned(size, ZGranuleSize), "Invalid size");

  // Allocate segments
  while (size > 0) {
    size_t allocated = 0;
    const uintptr_t start = _manager.alloc_from_front_at_most(size, &allocated);
    assert(start != UINTPTR_MAX, "Allocation should never fail");
    pmem.add_segment(ZPhysicalMemorySegment(start, allocated, false /* committed */));
    size -= allocated;
  }
}

void ZPhysicalMemoryManager::free(const ZPhysicalMemory& pmem) {
  // Free segments
  for (uint32_t i = 0; i < pmem.nsegments(); i++) {
    const ZPhysicalMemorySegment& segment = pmem.segment(i);
    _manager.free(segment.start(), segment.size());
  }
}

bool ZPhysicalMemoryManager::commit(ZPhysicalMemory& pmem) {
  // Commit segments
  for (uint32_t i = 0; i < pmem.nsegments(); i++) {
    const ZPhysicalMemorySegment& segment = pmem.segment(i);
    if (segment.is_committed()) {
      // Segment already committed
      continue;
    }

    // Commit segment
    const size_t committed = _backing.commit(segment.start(), segment.size());
    if (!pmem.commit_segment(i, committed)) {
      // Failed or partially failed
      return false;
    }
  }

  // Success
  return true;
}

bool ZPhysicalMemoryManager::uncommit(ZPhysicalMemory& pmem) {
  // Commit segments
  for (uint32_t i = 0; i < pmem.nsegments(); i++) {
    const ZPhysicalMemorySegment& segment = pmem.segment(i);
    if (!segment.is_committed()) {
      // Segment already uncommitted
      continue;
    }

    // Uncommit segment
    const size_t uncommitted = _backing.uncommit(segment.start(), segment.size());
    if (!pmem.uncommit_segment(i, uncommitted)) {
      // Failed or partially failed
      return false;
    }
  }

  // Success
  return true;
}

void ZPhysicalMemoryManager::pretouch_view(uintptr_t addr, size_t size) const {
  const size_t page_size = ZLargePages::is_explicit() ? ZGranuleSize : os::vm_page_size();
  os::pretouch_memory((void*)addr, (void*)(addr + size), page_size);
}

void ZPhysicalMemoryManager::map_view(uintptr_t addr, const ZPhysicalMemory& pmem) const {
  size_t size = 0;

  // Map segments
  for (uint32_t i = 0; i < pmem.nsegments(); i++) {
    const ZPhysicalMemorySegment& segment = pmem.segment(i);
    _backing.map(addr + size, segment.size(), segment.start());
    size += segment.size();
  }

  // Setup NUMA interleaving
  if (ZNUMA::is_enabled()) {
    os::numa_make_global((char*)addr, size);
  }

  // Setup transparent large pages
  if (ZLargePages::is_transparent()) {
    os::realign_memory((char*)addr, size, os::large_page_size());
  }
}

void ZPhysicalMemoryManager::unmap_view(uintptr_t addr, size_t size) const {
  _backing.unmap(addr, size);
}

void ZPhysicalMemoryManager::pretouch(uintptr_t offset, size_t size) const {
  if (ZVerifyViews) {
    // Pre-touch good view
    pretouch_view(ZAddress::good(offset), size);
  } else if (ZMemoryTagging) {
    pretouch_view(offset, size);
  } else {
    // Pre-touch all views
    pretouch_view(ZAddress::marked0(offset), size);
    pretouch_view(ZAddress::marked1(offset), size);
    pretouch_view(ZAddress::remapped(offset), size);
  }
}

void ZPhysicalMemoryManager::map(uintptr_t offset, const ZPhysicalMemory& pmem) const {
  const size_t size = pmem.size();

  if (ZVerifyViews) {
    // Map good view
    map_view(ZAddress::good(offset), pmem);
  } else if (ZMemoryTagging) {
    map_view(offset, pmem);
  } else {
    // Map all views
    map_view(ZAddress::marked0(offset), pmem);
    map_view(ZAddress::marked1(offset), pmem);
    map_view(ZAddress::remapped(offset), pmem);
  }

  nmt_commit(offset, size);
}

void ZPhysicalMemoryManager::unmap(uintptr_t offset, size_t size) const {
  nmt_uncommit(offset, size);

  if (ZVerifyViews) {
    // Unmap good view
    unmap_view(ZAddress::good(offset), size);
  } else if (ZMemoryTagging) {
    unmap_view(offset, size);
  } else {
    // Unmap all views
    unmap_view(ZAddress::marked0(offset), size);
    unmap_view(ZAddress::marked1(offset), size);
    unmap_view(ZAddress::remapped(offset), size);
  }
}

void ZPhysicalMemoryManager::debug_map(uintptr_t offset, const ZPhysicalMemory& pmem) const {
  // Map good view
  assert(ZVerifyViews, "Should be enabled");
  map_view(ZAddress::good(offset), pmem);
}

void ZPhysicalMemoryManager::debug_unmap(uintptr_t offset, size_t size) const {
  // Unmap good view
  assert(ZVerifyViews, "Should be enabled");
  unmap_view(ZAddress::good(offset), size);
}
