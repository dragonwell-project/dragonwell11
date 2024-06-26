/*
 * Copyright (c) 2002, 2018, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_VM_GC_PARALLEL_PSPROMOTIONMANAGER_INLINE_HPP
#define SHARE_VM_GC_PARALLEL_PSPROMOTIONMANAGER_INLINE_HPP

#include "gc/parallel/parallelScavengeHeap.hpp"
#include "gc/parallel/parMarkBitMap.inline.hpp"
#include "gc/parallel/psOldGen.hpp"
#include "gc/parallel/psPromotionLAB.inline.hpp"
#include "gc/parallel/psPromotionManager.hpp"
#include "gc/parallel/psScavenge.hpp"
#include "gc/shared/taskqueue.inline.hpp"
#include "logging/log.hpp"
#include "oops/access.inline.hpp"
#include "oops/oop.inline.hpp"
#include "runtime/prefetch.inline.hpp"

inline PSPromotionManager* PSPromotionManager::manager_array(uint index) {
  assert(_manager_array != NULL, "access of NULL manager_array");
  assert(index <= ParallelGCThreads, "out of range manager_array access");
  return &_manager_array[index];
}

template <class T>
inline void PSPromotionManager::push_depth(T* p) {
  claimed_stack_depth()->push(p);
}

template <class T>
inline void PSPromotionManager::claim_or_forward_depth(T* p) {
  assert(should_scavenge(p, true), "revisiting object?");
  assert(ParallelScavengeHeap::heap()->is_in(p), "pointer outside heap");
  oop obj = RawAccess<IS_NOT_NULL>::oop_load(p);
  Prefetch::write(obj->mark_addr_raw(), 0);
  push_depth(p);
}

inline void PSPromotionManager::promotion_trace_event(oop new_obj, Klass* klass,
                                                      size_t obj_size,
                                                      uint age, bool tenured,
                                                      const PSPromotionLAB* lab) {
  // Skip if memory allocation failed
  if (new_obj != NULL) {
    const ParallelScavengeTracer* gc_tracer = PSScavenge::gc_tracer();
    if (lab != NULL) {
      // Promotion of object through newly allocated PLAB
      if (gc_tracer->should_report_promotion_in_new_plab_event()) {
        size_t obj_bytes = obj_size * HeapWordSize;
        size_t lab_size = lab->capacity();
        gc_tracer->report_promotion_in_new_plab_event(klass, obj_bytes,
                                                      age, tenured, lab_size);
      }
    } else {
      // Promotion of object directly to heap
      if (gc_tracer->should_report_promotion_outside_plab_event()) {
        size_t obj_bytes = obj_size * HeapWordSize;
        gc_tracer->report_promotion_outside_plab_event(klass, obj_bytes,
                                                       age, tenured);
      }
    }
  }
}

inline void PSPromotionManager::push_contents(oop obj) {
  obj->ps_push_contents(this);
}
//
// This method is pretty bulky. It would be nice to split it up
// into smaller submethods, but we need to be careful not to hurt
// performance.
//
template<bool promote_immediately>
inline oop PSPromotionManager::copy_to_survivor_space(oop o) {
  assert(should_scavenge(&o), "Sanity");

  oop new_obj = NULL;

  // NOTE! We must be very careful with any methods that access the mark
  // in o. There may be multiple threads racing on it, and it may be forwarded
  // at any time. Do not use oop methods for accessing the mark!
  markOop test_mark = o->mark_raw();

  // The same test as "o->is_forwarded()"
  if (!test_mark->is_marked()) {
    bool new_obj_is_tenured = false;
    Klass* klass = o->forward_safe_klass(test_mark);
    size_t new_obj_size = UseCompactObjectHeaders ?
                          o->size_given_klass(klass) : o->size();

    // Find the objects age, MT safe.
    uint age = (test_mark->has_displaced_mark_helper() /* o->has_displaced_mark() */) ?
      test_mark->displaced_mark_helper()->age() : test_mark->age();

    if (!promote_immediately) {
      // Try allocating obj in to-space (unless too old)
      if (age < PSScavenge::tenuring_threshold()) {
        new_obj = (oop) _young_lab.allocate(new_obj_size);
        if (new_obj == NULL && !_young_gen_is_full) {
          // Do we allocate directly, or flush and refill?
          if (new_obj_size > (YoungPLABSize / 2)) {
            // Allocate this object directly
            new_obj = (oop)young_space()->cas_allocate(new_obj_size);
            promotion_trace_event(new_obj, klass, new_obj_size, age, false, NULL);
          } else {
            // Flush and fill
            _young_lab.flush();

            HeapWord* lab_base = young_space()->cas_allocate(YoungPLABSize);
            if (lab_base != NULL) {
              _young_lab.initialize(MemRegion(lab_base, YoungPLABSize));
              // Try the young lab allocation again.
              new_obj = (oop) _young_lab.allocate(new_obj_size);
              promotion_trace_event(new_obj, klass, new_obj_size, age, false, &_young_lab);
            } else {
              _young_gen_is_full = true;
            }
          }
        }
      }
    }

    // Otherwise try allocating obj tenured
    if (new_obj == NULL) {
#ifndef PRODUCT
      if (ParallelScavengeHeap::heap()->promotion_should_fail()) {
        return oop_promotion_failed(o, test_mark);
      }
#endif  // #ifndef PRODUCT

      new_obj = (oop) _old_lab.allocate(new_obj_size);
      new_obj_is_tenured = true;

      if (new_obj == NULL) {
        if (!_old_gen_is_full) {
          // Do we allocate directly, or flush and refill?
          if (new_obj_size > (OldPLABSize / 2)) {
            // Allocate this object directly
            new_obj = (oop)old_gen()->cas_allocate(new_obj_size);
            promotion_trace_event(new_obj, klass, new_obj_size, age, true, NULL);
          } else {
            // Flush and fill
            _old_lab.flush();

            HeapWord* lab_base = old_gen()->cas_allocate(OldPLABSize);
            if(lab_base != NULL) {
#ifdef ASSERT
              // Delay the initialization of the promotion lab (plab).
              // This exposes uninitialized plabs to card table processing.
              if (GCWorkerDelayMillis > 0) {
                os::sleep(Thread::current(), GCWorkerDelayMillis, false);
              }
#endif
              _old_lab.initialize(MemRegion(lab_base, OldPLABSize));
              // Try the old lab allocation again.
              new_obj = (oop) _old_lab.allocate(new_obj_size);
              promotion_trace_event(new_obj, klass, new_obj_size, age, true, &_old_lab);
            }
          }
        }

        // This is the promotion failed test, and code handling.
        // The code belongs here for two reasons. It is slightly
        // different than the code below, and cannot share the
        // CAS testing code. Keeping the code here also minimizes
        // the impact on the common case fast path code.

        if (new_obj == NULL) {
          _old_gen_is_full = true;
          return oop_promotion_failed(o, test_mark);
        }
      }
    }

    assert(new_obj != NULL, "allocation should have succeeded");

    // Copy obj
    Copy::aligned_disjoint_words((HeapWord*)o, (HeapWord*)new_obj, new_obj_size);

    // Now we have to CAS in the header.
    // Make copy visible to threads reading the forwardee.
    if (o->cas_forward_to(new_obj, test_mark, memory_order_release)) {
      // We won any races, we "own" this object.
      assert(new_obj == o->forwardee(), "Sanity");

      // Increment age if obj still in new generation. Now that
      // we're dealing with a markOop that cannot change, it is
      // okay to use the non mt safe oop methods.
      if (!new_obj_is_tenured) {
        new_obj->incr_age();
        assert(young_space()->contains(new_obj), "Attempt to push non-promoted obj");
      }

      // Do the size comparison first with new_obj_size, which we
      // already have. Hopefully, only a few objects are larger than
      // _min_array_size_for_chunking, and most of them will be arrays.
      // So, the is->objArray() test would be very infrequent.
      if (new_obj_size > _min_array_size_for_chunking &&
          new_obj->is_objArray() &&
          PSChunkLargeArrays) {
        // we'll chunk it
        oop* const masked_o = mask_chunked_array_oop(o);
        push_depth(masked_o);
        TASKQUEUE_STATS_ONLY(++_arrays_chunked; ++_masked_pushes);
      } else {
        // we'll just push its contents
        push_contents(new_obj);
      }
    }  else {
      // We lost, someone else "owns" this object
      guarantee(o->is_forwarded(), "Object must be forwarded if the cas failed.");

      // Try to deallocate the space.  If it was directly allocated we cannot
      // deallocate it, so we have to test.  If the deallocation fails,
      // overwrite with a filler object.
      if (new_obj_is_tenured) {
        if (!_old_lab.unallocate_object((HeapWord*) new_obj, new_obj_size)) {
          CollectedHeap::fill_with_object((HeapWord*) new_obj, new_obj_size);
        }
      } else if (!_young_lab.unallocate_object((HeapWord*) new_obj, new_obj_size)) {
        CollectedHeap::fill_with_object((HeapWord*) new_obj, new_obj_size);
      }

      // don't update this before the unallocation!
      // Using acquire though consume would be accurate for accessing new_obj.
      new_obj = o->forwardee_acquire();
    }
  } else {
    assert(o->is_forwarded(), "Sanity");
    new_obj = o->forwardee_acquire();
  }

  // This code must come after the CAS test, or it will print incorrect
  // information.
  log_develop_trace(gc, scavenge)("{%s %s " PTR_FORMAT " -> " PTR_FORMAT " (%d)}",
                                  should_scavenge(&new_obj) ? "copying" : "tenuring",
                                  new_obj->klass()->internal_name(), p2i((void *)o), p2i((void *)new_obj), new_obj->size());

  return new_obj;
}

// Attempt to "claim" oop at p via CAS, push the new obj if successful
// This version tests the oop* to make sure it is within the heap before
// attempting marking.
template <class T, bool promote_immediately>
inline void PSPromotionManager::copy_and_push_safe_barrier(T* p) {
  assert(should_scavenge(p, true), "revisiting object?");

  oop o = RawAccess<IS_NOT_NULL>::oop_load(p);
  oop new_obj = o->is_forwarded()
        ? o->forwardee()
        : copy_to_survivor_space<promote_immediately>(o);

  // This code must come after the CAS test, or it will print incorrect
  // information.
  if (log_develop_is_enabled(Trace, gc, scavenge) && o->is_forwarded()) {
    log_develop_trace(gc, scavenge)("{%s %s " PTR_FORMAT " -> " PTR_FORMAT " (%d)}",
                      "forwarding",
                      new_obj->klass()->internal_name(), p2i((void *)o), p2i((void *)new_obj), new_obj->size());
  }

  RawAccess<IS_NOT_NULL>::oop_store(p, new_obj);

  // We cannot mark without test, as some code passes us pointers
  // that are outside the heap. These pointers are either from roots
  // or from metadata.
  if ((!PSScavenge::is_obj_in_young((HeapWord*)p)) &&
      ParallelScavengeHeap::heap()->is_in_reserved(p)) {
    if (PSScavenge::is_obj_in_young(new_obj)) {
      PSScavenge::card_table()->inline_write_ref_field_gc(p, new_obj);
    }
  }
}

inline void PSPromotionManager::process_popped_location_depth(StarTask p) {
  if (is_oop_masked(p)) {
    assert(PSChunkLargeArrays, "invariant");
    oop const old = unmask_chunked_array_oop(p);
    process_array_chunk(old);
  } else {
    if (p.is_narrow()) {
      assert(UseCompressedOops, "Error");
      copy_and_push_safe_barrier<narrowOop, /*promote_immediately=*/false>(p);
    } else {
      copy_and_push_safe_barrier<oop, /*promote_immediately=*/false>(p);
    }
  }
}

inline bool PSPromotionManager::steal_depth(int queue_num, StarTask& t) {
  return stack_array_depth()->steal(queue_num, t);
}

#if TASKQUEUE_STATS
void PSPromotionManager::record_steal(StarTask& p) {
  if (is_oop_masked(p)) {
    ++_masked_steals;
  }
}
#endif // TASKQUEUE_STATS

#endif // SHARE_VM_GC_PARALLEL_PSPROMOTIONMANAGER_INLINE_HPP
