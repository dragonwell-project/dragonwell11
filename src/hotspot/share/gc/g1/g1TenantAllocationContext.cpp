/*
 * Copyright (c) 2020 Alibaba Group Holding Limited. All Rights Reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation. Alibaba designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 */

#include "precompiled.hpp"
#include "runtime/thread.hpp"
#include "runtime/orderAccess.hpp"
#include "runtime/safepoint.hpp"
#include "runtime/mutexLocker.hpp"
#include "memory/iterator.hpp"
#include "gc/g1/g1CollectedHeap.hpp"
#include "gc/g1/g1CollectedHeap.inline.hpp"
#include "gc/g1/g1FullGCCompactTask.hpp"
#include "gc/g1/g1TenantAllocationContext.hpp"
#include "gc/g1/heapRegion.inline.hpp"

//----------------------- G1TenantAllocationContext ---------------------------

G1TenantAllocationContext::G1TenantAllocationContext(G1CollectedHeap* g1h)
        : _g1h(g1h),
          _occupied_heap_region_count(0),
          _heap_size_limit(TENANT_HEAP_NO_LIMIT),
          _heap_region_limit(0),
          _tenant_container(NULL),
          _cps(NULL),
          _serial_cp(NULL), 
          _retained_old_gc_alloc_region(NULL),
          _survivor_gc_alloc_region(g1h->alloc_buffer_stats(InCSetState::Young)),
          _old_gc_alloc_region(g1h->alloc_buffer_stats(InCSetState::Old))  {

  assert(TenantHeapIsolation, "pre-condition");
  // in current design we do not create G1TenantAllocationContext at safepoint
  assert_not_at_safepoint();

#ifndef PRODUCT
  if (TraceG1TenantAllocationContext) {
    tty->print_cr("Create G1TenantAllocationContext: " PTR_FORMAT, p2i(this));
  }
#endif

  // init mutator allocator eagerly, because it may be used
  // to allocate memory immediately after creation of tenant alloc context
  _mutator_alloc_region.init();

  AllocationContext_t ac(this);
  _mutator_alloc_region.set_allocation_context(ac);
  _survivor_gc_alloc_region.set_allocation_context(ac);
  _old_gc_alloc_region.set_allocation_context(ac);

  G1TenantAllocationContexts::add(this);
}

class ClearAllocationContextClosure : public HeapRegionClosure {
private:
  AllocationContext_t     _target;
public:
  ClearAllocationContextClosure(AllocationContext_t ctxt) : _target(ctxt) {
    assert(!_target.is_system(), "Cannot clear root tenant context");
  }

  virtual bool do_heap_region(HeapRegion* region) {
    assert(TenantHeapIsolation, "pre-condition");
    assert(NULL != region, "Region cannot be NULL");
    if (region->allocation_context() == _target) {
      region->set_allocation_context(AllocationContext::system());
    }
    return false /* forcefully iterate over all regions */;
  }
};

// clean up work that has to be done at safepoint
class DestroyG1TenantAllocationContextOperation : public VM_Operation {
private:
  G1TenantAllocationContext* _context_to_destroy;
public:
  DestroyG1TenantAllocationContextOperation(G1TenantAllocationContext* context)
          : _context_to_destroy(context)
  {
    assert(TenantHeapIsolation, "pre-condition");
    assert(_context_to_destroy != G1TenantAllocationContexts::system_context(),
           "Should never destroy system context");
    assert(_context_to_destroy->tenant_container() != NULL, "sanity");
  }
  virtual void doit();
  virtual VMOp_Type type() const { return VMOp_DestroyG1TenantAllocationContext; }
};

void DestroyG1TenantAllocationContextOperation::doit() {
  assert_at_safepoint_on_vm_thread();

  if (UsePerTenantTLAB) {
    assert(UseTLAB, "Sanity");
    for (JavaThread *thread = Threads::first(); thread != NULL; thread = thread->next()) {
      thread->clean_tlab_for(_context_to_destroy);
    }
  }

  // return any active mutator alloc region
  MutatorAllocRegion* mar = _context_to_destroy->mutator_alloc_region();
  HeapRegion*  hr = mar->release();
  assert(mar->get() == NULL, "post-condition");
  if (hr != NULL) { // if this mutator region has been used
    // 1, return mutator's heap region to root tenant;
    // 2, after GC, objects still live in #1's heap region, will be
    //    owned by root
    hr->set_allocation_context(AllocationContext::system());
  }

  // traverse all HeapRegions and update alloc contexts
  AllocationContext_t ctxt(_context_to_destroy);
  ClearAllocationContextClosure cl(ctxt);
  G1CollectedHeap::heap()->heap_region_iterate(&cl);

  G1TenantAllocationContexts::remove(_context_to_destroy);
  com_alibaba_tenant_TenantContainer::set_tenant_allocation_context(_context_to_destroy->tenant_container(),
                                                                    G1TenantAllocationContexts::system_context());

#ifndef PRODUCT
  if (TraceG1TenantAllocationContext) {
    tty->print_cr("Destroy G1TenantAllocationContext:" PTR_FORMAT, p2i(_context_to_destroy));
  }
#endif
}

G1TenantAllocationContext::~G1TenantAllocationContext() {
  assert(TenantHeapIsolation, "pre-condition");
  assert_not_at_safepoint();

  DestroyG1TenantAllocationContextOperation vm_op(this);
  VMThread::execute(&vm_op);
}

bool G1TenantAllocationContext::can_allocate(size_t attempt_word_size) {
  assert(TenantHeapThrottling, "pre-condition");
  assert_heap_locked_or_at_safepoint(true /* should_be_vm_thread */);

  // always allow allocation
  if (heap_size_limit() == TENANT_HEAP_NO_LIMIT) {
    return true;
  }

  size_t occupied_regions = occupied_heap_region_count();
  if (_g1h->is_humongous(attempt_word_size)) {
    // reach here only from G1CollectedHeap::humongous_obj_allocate
    occupied_regions += heap_words_to_region_num(attempt_word_size);
  } else {
    // reach here only from MutatorAllocRegion::allocate_new_region
    occupied_regions++;
  }
  return occupied_regions <= heap_region_limit();
}

void G1TenantAllocationContext::inc_occupied_heap_region_count() {
  //assert(TenantHeapIsolation && occupied_heap_region_count() >= 0, "pre-condition");
  assert(Heap_lock->owned_by_self() || SafepointSynchronize::is_at_safepoint(), "not locked");
  Atomic::inc(&_occupied_heap_region_count);
  assert(occupied_heap_region_count() >= 1, "post-condition");
}

void G1TenantAllocationContext::dec_occupied_heap_region_count() {
  assert(TenantHeapIsolation && occupied_heap_region_count() >= 1, "pre-condition");
  assert(Heap_lock->owned_by_self() || SafepointSynchronize::is_at_safepoint(), "not locked");
  Atomic::dec(&_occupied_heap_region_count);
  //assert(occupied_heap_region_count() >= 0, "post-condition");
}

G1TenantAllocationContext* G1TenantAllocationContext::current() {
  assert(TenantHeapIsolation, "pre-condition");

  Thread* thrd = Thread::current();
  assert(NULL != thrd, "Failed to get current thread");
  return thrd->allocation_context().tenant_allocation_context();
}

void G1TenantAllocationContext::set_heap_size_limit(size_t new_size) {
  assert(TenantHeapThrottling, "pre-condition");
  assert(new_size != TENANT_HEAP_NO_LIMIT, "sanity");

  _heap_size_limit = new_size;
  _heap_region_limit = heap_bytes_to_region_num(_heap_size_limit);
}

size_t G1TenantAllocationContext::heap_bytes_to_region_num(size_t size_in_bytes) {
  return heap_words_to_region_num(size_in_bytes >> LogBytesPerWord);
}

size_t G1TenantAllocationContext::heap_words_to_region_num(size_t size_in_words) {
  assert(TenantHeapIsolation, "pre-condition");
  return align_up_(size_in_words, HeapRegion::GrainWords) / HeapRegion::GrainWords;
}

//--------------------- G1TenantAllocationContexts ---------------------
G1TenantAllocationContexts::G1TenantACList* G1TenantAllocationContexts::_contexts = NULL;

Mutex* G1TenantAllocationContexts::_list_lock = NULL;

uint G1TenantAllocationContexts::_num_workers = 0;

void G1TenantAllocationContexts::add(G1TenantAllocationContext* tac) {
  assert(TenantHeapIsolation, "pre-condition");
  if (NULL != tac) {
    MutexLockerEx ml(_list_lock, Monitor::_no_safepoint_check_flag);
    _contexts->append(tac);
  }
}

void G1TenantAllocationContexts::remove(G1TenantAllocationContext* tac) {
  assert(TenantHeapIsolation, "pre-condition");
  if (NULL != tac) {
    MutexLockerEx ml(_list_lock, Monitor::_no_safepoint_check_flag);
    _contexts->remove(tac);
  }
}


long G1TenantAllocationContexts::active_context_count() {
  assert(TenantHeapIsolation, "pre-condition");
  assert(NULL != "_contexts", "Tenant alloc context list not initialized");
  MutexLockerEx ml(_list_lock, Monitor::_no_safepoint_check_flag);
  return _contexts->length();
}

void G1TenantAllocationContexts::iterate_locked(G1TenantAllocationContextClosure* closure) {
  assert(TenantHeapIsolation, "pre-condition");
  assert(NULL != closure, "NULL closure pointer");
  MutexLockerEx ml(_list_lock, Monitor::_no_safepoint_check_flag);
  iterate_impl(closure);
}

void G1TenantAllocationContexts::iterate_unlocked(G1TenantAllocationContextClosure* closure) {
  assert_at_safepoint();
  assert(TenantHeapIsolation, "pre-condition");
  assert(NULL != closure, "NULL closure pointer");
  MutexLockerEx ml(_list_lock, Monitor::_no_safepoint_check_flag);
  iterate_impl(closure);
}

void G1TenantAllocationContexts::iterate_impl(G1TenantAllocationContextClosure* closure) {
  assert(TenantHeapIsolation, "pre-condition");
  assert(NULL != closure, "NULL closure pointer");
  for (GrowableArrayIterator<G1TenantAllocationContext*> itr = _contexts->begin();
       itr != _contexts->end(); ++itr) {
    closure->do_tenant_allocation_context(*itr);
  }
}

void G1TenantAllocationContexts::initialize() {
  assert(TenantHeapIsolation, "pre-condition");
  _contexts = new (ResourceObj::C_HEAP, mtTenant) G1TenantACList(128, true, mtTenant);
  _list_lock = new Mutex(Mutex::leaf, "G1TenantAllocationContext list lock", true /* allow vm lock */, Monitor::_safepoint_check_never);
}

void G1TenantAllocationContexts::oops_do(OopClosure* f) {
  assert(TenantHeapIsolation, "pre-condition");
  assert(NULL != f, "OopClosure pointer is NULL");

  for (G1TenantACListIterator itr = _contexts->begin();
       itr != _contexts->end(); ++itr) {
    assert(NULL != (*itr), "pre-condition");
    f->do_oop(&((*itr)->_tenant_container));
  }
}

void G1TenantAllocationContexts::init_mutator_alloc_regions() {
  assert(TenantHeapIsolation, "pre-condition");

  for (G1TenantACListIterator itr = _contexts->begin();
       itr != _contexts->end(); ++itr) {
    assert(NULL != (*itr), "pre-condition");
    MutatorAllocRegion& mar = (*itr)->_mutator_alloc_region;
    assert(mar.get() == NULL, "pre-condition");
    mar.init();
  }
}

void G1TenantAllocationContexts::release_mutator_alloc_regions() {
  assert(TenantHeapIsolation, "pre-condition");
  assert_at_safepoint_on_vm_thread();

  for (G1TenantACListIterator itr = _contexts->begin();
       itr != _contexts->end(); ++itr) {
    assert(NULL != (*itr), "pre-condition");
    MutatorAllocRegion& mar = (*itr)->_mutator_alloc_region;
    mar.release();
    assert(mar.get() == NULL, "pre-condition");
  }
}

size_t G1TenantAllocationContexts::total_used() {
  assert(TenantHeapIsolation, "pre-condition");

  size_t res = 0;
  MutexLockerEx ml(_list_lock, Monitor::_no_safepoint_check_flag); // have lock, may not in safepoint
  for (G1TenantACListIterator itr = _contexts->begin();
       itr != _contexts->end(); ++itr) {
    assert(NULL != (*itr), "pre-condition");
    // HeapRegion* hr = (*itr)->_mutator_alloc_region.get();
    // if (NULL != hr) {
    //   res += hr->used();
    // }
    res += (*itr)->_mutator_alloc_region.used_in_alloc_regions();
  }

  return res;
}

void G1TenantAllocationContexts::set_num_workers(uint num_workers) {
  _num_workers = num_workers;
}

void G1TenantAllocationContexts::init_cps() {
  G1TACFullCompactionPointCreateClosure cl(_num_workers);
  iterate_unlocked(&cl);
}

void G1TenantAllocationContexts::release_cps()
{
  G1TACFullCompactionPointReleaseClosure cl(_num_workers);
  iterate_unlocked(&cl);
}

void G1TenantAllocationContexts::init_gc_alloc_regions(G1Allocator* allocator, EvacuationInfo& ei) {
  assert(TenantHeapIsolation, "pre-condition");
  assert_at_safepoint_on_vm_thread();
  assert(NULL != allocator, "Allocator cannot be NULL");

  for (G1TenantACListIterator itr = _contexts->begin();
       itr != _contexts->end(); ++itr) {
    G1TenantAllocationContext* tac = (*itr);
    assert(NULL != tac, "pre-condition");

    SurvivorGCAllocRegion& survivor_region = tac->_survivor_gc_alloc_region;
    OldGCAllocRegion& old_region = tac->_old_gc_alloc_region;

    survivor_region.init();
    old_region.init();

    allocator->reuse_retained_old_region(ei, &old_region,
                                         &(tac->_retained_old_gc_alloc_region));
  }
}

void G1TenantAllocationContexts::release_gc_alloc_regions(EvacuationInfo& ei) {
  assert(TenantHeapIsolation, "pre-condition");
  assert_at_safepoint_on_vm_thread();

  for (G1TenantACListIterator itr = _contexts->begin();
       itr != _contexts->end(); ++itr) {
    G1TenantAllocationContext* tac = (*itr);
    assert(NULL != tac, "pre-condition");

    SurvivorGCAllocRegion& survivor_region = tac->_survivor_gc_alloc_region;
    OldGCAllocRegion& old_region = tac->_old_gc_alloc_region;

    ei.increment_allocation_regions(survivor_region.count() + old_region.count());

    survivor_region.release();
    HeapRegion* retained_old = old_region.release();

    tac->set_retained_old_gc_alloc_region(retained_old);
  }
}

void G1TenantAllocationContexts::abandon_gc_alloc_regions() {
  assert(TenantHeapIsolation, "pre-condition");
  assert_at_safepoint_on_vm_thread();

  for (G1TenantACListIterator itr = _contexts->begin();
       itr != _contexts->end(); ++itr) {
    G1TenantAllocationContext* tac = *itr;
    assert(NULL != tac, "pre-condition");
    assert(NULL == tac->_survivor_gc_alloc_region.get(), "pre-condition");
    assert(NULL == tac->_old_gc_alloc_region.get(), "pre-condition");
    (*itr)->set_retained_old_gc_alloc_region(NULL);
  }
}

G1TenantAllocationContext* G1TenantAllocationContexts::system_context() {
  assert(TenantHeapIsolation, "pre-condition");
  return AllocationContext::system().tenant_allocation_context();
}

void G1TACCompactionClosure::do_tenant_allocation_context(G1TenantAllocationContext* tac) {
  assert(tac != NULL, "Pre-condition");
  GrowableArray<HeapRegion*>* compaction_queue = tac->get_compact_point_by_worker_id(_worker_id)->regions();
  for (GrowableArrayIterator<HeapRegion*> it = compaction_queue->begin();
       it != compaction_queue->end();
       ++it) {
    // Compact regions
    assert(!(*it)->is_humongous(), "Should be no humongous regions in compaction queue");
    G1FullGCCompactTask::G1CompactRegionClosure compact(mark_bitmap());
    (*it)->apply_to_marked_objects(mark_bitmap(), &compact);
    // Once all objects have been moved the liveness information
    // needs be cleared.
    mark_bitmap()->clear_region((*it));
    (*it)->complete_compaction();
  }
  // TODO(Multitenant): We do not actually compact humongous regions, so is it necessary to reset humongous for each tenant?
  // BTW, Dragonwell-8 doesn't seperate tenants when reseting hunmongous.
  // G1ResetHumongousClosure hc(mark_bitmap());
  // G1CollectedHeap::heap()->heap_region_par_iterate_from_worker_offset(&hc, &_claimer, _worker_id);
}

void G1TACSerialCompactionClosure::do_tenant_allocation_context(G1TenantAllocationContext* tac) {
  assert(tac != NULL, "Pre-condition");
  if (tac->serial_compaction_point()->has_regions()) {
    GrowableArray<HeapRegion*>* compaction_queue = tac->serial_compaction_point()->regions();
    for (GrowableArrayIterator<HeapRegion*> it = compaction_queue->begin();
        it != compaction_queue->end();
        ++it) {
      // Compact regions
      assert(!(*it)->is_humongous(), "Should be no humongous regions in compaction queue");
      G1FullGCCompactTask::G1CompactRegionClosure compact(mark_bitmap());
      (*it)->apply_to_marked_objects(mark_bitmap(), &compact);
      // Once all objects have been moved the liveness information
      // needs be cleared.
      mark_bitmap()->clear_region((*it));
      (*it)->complete_compaction();
    }
  }
}

void G1TACSerialPrepareCompactionClosure::do_tenant_allocation_context(G1TenantAllocationContext* tac) {
  assert(tac != NULL, "Pre-condition");
  G1FullGCCompactionPoint* cp = tac->get_compact_point_by_worker_id(_worker_id);
  G1FullGCCompactionPoint* serial_cp = tac->serial_compaction_point();
  assert(cp != NULL, "Pre-condition");
  assert(serial_cp != NULL, "Pre-condition");
  if (cp->has_regions()) {
    serial_cp->add(cp->remove_last());
  }
}

void G1TACSerialApplyMarkClosure::do_tenant_allocation_context(G1TenantAllocationContext* tac) {
  assert(tac != NULL, "Pre-condition");
  G1FullGCCompactionPoint* serial_cp = tac->serial_compaction_point();
  assert(serial_cp != NULL, "Pre-condition");
  for (GrowableArrayIterator<HeapRegion*> it = serial_cp->regions()->begin(); it != serial_cp->regions()->end(); ++it) {
    HeapRegion* current = *it;
    if (!serial_cp->is_initialized()) {
      // Initialize the compaction point. Nothing more is needed for the first heap region
      // since it is already prepared for compaction.
      serial_cp->initialize(current, false);
    } else {
      assert(!current->is_humongous(), "Should be no humongous regions in compaction queue");
      G1FullGCPrepareTask::G1RePrepareClosure re_prepare(serial_cp, current);
      current->set_compaction_top(current->bottom());
      current->apply_to_marked_objects(mark_bitmap(), &re_prepare);
    }
  }
  serial_cp->update();
}

void G1TACFullCompactionPointCreateClosure::do_tenant_allocation_context(G1TenantAllocationContext* tac) {
  assert(NULL != tac, "pre-condition");
  assert(NULL == tac->full_gc_compact_points(), "pre-condition");
  G1FullGCCompactionPoint** cps =  NEW_C_HEAP_ARRAY(G1FullGCCompactionPoint*, _num_workers, mtGC);
  tac->set_full_gc_compact_points(cps);
  for (uint i = 0; i < _num_workers; i++) {
    tac->set_compact_point_by_worker_id(i, new G1FullGCCompactionPoint());
  }
  // For serial compaction
  tac->set_serial_compaction_point(new G1FullGCCompactionPoint());
}

void G1TACFullCompactionPointReleaseClosure::do_tenant_allocation_context(G1TenantAllocationContext* tac) {
  assert(NULL != tac, "pre-condition");
  for (uint i = 0; i < _num_workers; ++i) {
    assert(NULL != tac->get_compact_point_by_worker_id(i), "pre-condition");
    delete tac->get_compact_point_by_worker_id(i);
  }
  FREE_C_HEAP_ARRAY(G1FullGCCompactionPoint*, tac->full_gc_compact_points());
  delete tac->serial_compaction_point();
  tac->set_full_gc_compact_points(NULL);
  tac->set_serial_compaction_point(NULL);
}

void G1TACFullCompactionPointUpdateClosure::do_tenant_allocation_context(G1TenantAllocationContext* tac) {
  assert(NULL != tac, "pre-condition");
  G1FullGCCompactionPoint* cp = tac->get_compact_point_by_worker_id(_worker_id);
  assert(NULL != cp, "pre-condition");
  cp->update();
}