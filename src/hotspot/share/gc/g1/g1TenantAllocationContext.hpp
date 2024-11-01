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

#ifndef SHARE_VM_GC_IMPLEMENTATION_G1_TENANT_CONTEXT_HPP
#define SHARE_VM_GC_IMPLEMENTATION_G1_TENANT_CONTEXT_HPP

#include "gc/g1/g1AllocationContext.hpp"
#include "gc/g1/g1AllocRegion.hpp"
#include "gc/g1/heapRegionSet.hpp"
#include "gc/g1/g1Allocator.hpp"
#include "gc/g1/g1FullGCCompactionPoint.hpp"
#include "gc/g1/g1FullGCPrepareTask.hpp"
#include "gc/g1/vm_operations_g1.hpp"
#include "memory/allocation.hpp"
#include "memory/iterator.hpp"
#include "runtime/handles.hpp"

class OopClosure;
class G1TenantAllocationContext;
class G1TenantAllocationContexts;

/*
 * Closure to encapsulate operations to iterate over all G1TenantAllocationContext
 */
class G1TenantAllocationContextClosure : public Closure {
public:
  virtual void do_tenant_allocation_context(G1TenantAllocationContext*) = 0;
};

class G1TACCompactionClosure: public G1TenantAllocationContextClosure {
private:
  G1CMBitMap* _bitmap;
  uint _worker_id;
public:
  G1TACCompactionClosure(G1CMBitMap* bitmap, uint worker_id) :
                                                              _bitmap(bitmap),
                                                              _worker_id(worker_id) { }
  G1CMBitMap* mark_bitmap() { return _bitmap; }
  virtual void do_tenant_allocation_context(G1TenantAllocationContext* tac);
};

class G1TACSerialCompactionClosure: public G1TenantAllocationContextClosure {
private:
  G1CMBitMap* _bitmap;
public:
  G1TACSerialCompactionClosure(G1CMBitMap* bitmap) : _bitmap(bitmap) { }
  G1CMBitMap* mark_bitmap() { return _bitmap; }
  virtual void do_tenant_allocation_context(G1TenantAllocationContext* tac);
};

class G1TACSerialPrepareCompactionClosure : public G1TenantAllocationContextClosure {
private:
  G1CMBitMap* _bitmap;
  uint _worker_id;
public:
  G1TACSerialPrepareCompactionClosure(G1CMBitMap* bitmap, uint worker_id) :
                                                            _bitmap(bitmap),
                                                            _worker_id(worker_id) { }
  G1CMBitMap* mark_bitmap() { return _bitmap; }

  virtual void do_tenant_allocation_context(G1TenantAllocationContext* tac);
};

class G1TACSerialApplyMarkClosure: public G1TenantAllocationContextClosure {
private:
  G1CMBitMap* _bitmap;
public:
  G1TACSerialApplyMarkClosure(G1CMBitMap* bitmap) : _bitmap(bitmap) { }
  G1CMBitMap* mark_bitmap() { return _bitmap; }
  virtual void do_tenant_allocation_context(G1TenantAllocationContext* tac);
};

class G1TACFullCompactionPointCreateClosure: public G1TenantAllocationContextClosure {
private:
  uint _num_workers;
public:
  G1TACFullCompactionPointCreateClosure(uint num_workers) : _num_workers(num_workers) { }
  virtual void do_tenant_allocation_context(G1TenantAllocationContext* tac);
};

class G1TACFullCompactionPointReleaseClosure: public G1TenantAllocationContextClosure {
private:
  uint _num_workers;
public:
  G1TACFullCompactionPointReleaseClosure(uint num_workers) : _num_workers(num_workers) { }
  virtual void do_tenant_allocation_context(G1TenantAllocationContext* tac);
};

class G1TACFullCompactionPointUpdateClosure: public G1TenantAllocationContextClosure {
private:
  uint _worker_id;
public:
  G1TACFullCompactionPointUpdateClosure(uint worker_id) : _worker_id(worker_id) { }
  virtual void do_tenant_allocation_context(G1TenantAllocationContext* tac);
};

// By default, no limit on newly created G1TenantAllocationContext
#define TENANT_HEAP_NO_LIMIT  0

/*
 * G1TenantAllocationContext identifies a group of isolated Java heap regions associated with
 * one TenantContainer.
 *
 * Only valid when -XX:+TenantHeapIsolation enabled
 *
 */
class G1TenantAllocationContext : public CHeapObj<mtTenant> {
  friend class VMStructs;
  friend class G1TenantAllocationContexts;
private:
  G1CollectedHeap*                  _g1h;                           // The only g1 heap instance

  // Memory allocation related
  MutatorAllocRegion                _mutator_alloc_region;          // mutator regions from young list
  SurvivorGCAllocRegion             _survivor_gc_alloc_region;      // survivor region used during GC
  OldGCAllocRegion                  _old_gc_alloc_region;           // Old region used during GC
  HeapRegion*                       _retained_old_gc_alloc_region;  // the retained old region for this tenant

  // HeapRegion throttling related
  size_t                            _heap_size_limit;               // user-defined max heap space for this tenant, in bytes
  size_t                            _heap_region_limit;             // user-defined max heap space for this tenant, in heap regions
  size_t                            _occupied_heap_region_count;    // number of regions occupied by this tenant

  // Tenant alloc context list is now part of root set since each node
  // keeps a strong reference to TenantContainer object for containerOf() API
  oop                               _tenant_container;              // handle to tenant container object

  G1FullGCCompactionPoint**         _cps;                           // G1FullGCCompactionPoints during full GC compaction
  G1FullGCCompactionPoint*          _serial_cp;                     // G1FullGCCompactionPoint during full GC serial compaction

public:
  // Newly allocated G1TenantAllocationContext will be put at the head of tenant alloc context list
  G1TenantAllocationContext(G1CollectedHeap* g1h);
  virtual ~G1TenantAllocationContext();

  MutatorAllocRegion* mutator_alloc_region()          { return &_mutator_alloc_region;        }
  SurvivorGCAllocRegion* survivor_gc_alloc_region()   { return &_survivor_gc_alloc_region;    }
  OldGCAllocRegion* old_gc_alloc_region()             { return &_old_gc_alloc_region;         }
  HeapRegion* retained_old_gc_alloc_region()          { return _retained_old_gc_alloc_region; }
  void set_retained_old_gc_alloc_region(HeapRegion* hr) { _retained_old_gc_alloc_region = hr; }

  // Get and set tenant container handle
  oop tenant_container() const                        { return _tenant_container;             }
  void set_tenant_container(oop handle)               { _tenant_container = handle;           }

  // get/set heap size limit
  size_t heap_size_limit() const                      { return _heap_size_limit;              }
  void set_heap_size_limit(size_t new_size);

  // get heap region limit, the size is calculated automatically
  size_t heap_region_limit() const                    { return _heap_region_limit;            }

  // set/get occupied heap region count
  void inc_occupied_heap_region_count();
  void dec_occupied_heap_region_count();
  size_t occupied_heap_region_count()                 { return _occupied_heap_region_count;   }
  //
  // Check if next allocation request can be satisfied
  // Called only before trying to allocate new regions, including:
  // 1, allocate one new eden region for non-humongous object
  // 2, allocate continuous regions for humongous oject
  //
  // NOTE: must be called at safepoint or after grabbing Heap_lock
  //
  // can_allocate() works safely with inc/dec_occupied_heap_region_count() because they are
  // protected by locks depending on different scenarios.
  // 1, Outside safepoint, Heap_lock is used to provide exclusive access.
  // 2, Inside safepoint, there may be three stages:
  //   2.1, before CGC operation starts, the only entrance is G1CollectedHeap::attempt_allocation_at_safepoint()
  //        which is single-threaded (VMThread), no concurrency issue;
  //   2.2, During CGC operation, inc/dec_occupied_heap_region_count() methods are guarded
  //        by FreeList_lock among different GC worker threads;
  //   2.3, after CGC operation but before safepoint ends, same as 2.1;
  //
  bool can_allocate(size_t attempt_word_size);

  // record the compact dest
  G1FullGCCompactionPoint** full_gc_compact_points() const  { return _cps; }
  void set_full_gc_compact_points(G1FullGCCompactionPoint** cps) {
    _cps = cps;
  }
  G1FullGCCompactionPoint* get_compact_point_by_worker_id(uint id) const  { return _cps[id]; }
  void set_compact_point_by_worker_id(uint id, G1FullGCCompactionPoint* cp) { _cps[id] = cp; }
  G1FullGCCompactionPoint* serial_compaction_point() { return _serial_cp; }
  void set_serial_compaction_point(G1FullGCCompactionPoint* serial_cp) { _serial_cp = serial_cp; }
  // Retrieve pointer to current tenant context, NULL if in root container
  static G1TenantAllocationContext* current();

private:
  // calculate how many regions will a size occupy,
  // if 0 < size < HeapRegion::GrainWords or GrainBytes, returns 1; if size == 0, returns 0;
  static size_t heap_bytes_to_region_num(size_t size_in_bytes);
  static size_t heap_words_to_region_num(size_t size_in_words);
};

// To encapsulate operations for all existing tenants
class G1TenantAllocationContexts : public AllStatic {
  friend class VMStructs;
public:
  typedef GrowableArray<G1TenantAllocationContext*>   G1TenantACList;
  typedef GrowableArrayIterator<G1TenantAllocationContext*> G1TenantACListIterator;

private:
  // NOTE: below two objects are created on C heap, but never released across
  //       JVM lifetime, we do this on purpose because they will exists as long
  //       as Java heap exists, and Java heap will not be destroyed until JVM
  //       dies.
  static G1TenantACList           *_contexts;     // Tenant contexts are organized into doubly-linked list
  static Mutex                    *_list_lock;
  static uint                     _num_workers;

public:
  static void add(G1TenantAllocationContext*);
  static void remove(G1TenantAllocationContext*);

  // initialize shared data
  static void initialize();

  // Get total number of active tenant containers
  static long active_context_count();

  // Perform operation upon all tenant alloc contexts
  static void iterate_locked(G1TenantAllocationContextClosure* closure);
  static void iterate_unlocked(G1TenantAllocationContextClosure* closure);
  static void iterate_impl(G1TenantAllocationContextClosure* closure);

  // GC support, we keep a reference to the TenantContainer oop
  static void oops_do(OopClosure* f);

  static void init_mutator_alloc_regions();
  static void release_mutator_alloc_regions();

  static size_t total_used();

  static uint num_workers() { return _num_workers; }

  static void init_gc_alloc_regions(G1Allocator* allocator, EvacuationInfo& ei);
  static void release_gc_alloc_regions(EvacuationInfo& ei);

  static void set_num_workers(uint num_workers);
  static void init_cps();
  static void release_cps();

  static void abandon_gc_alloc_regions();

  static G1TenantAllocationContext* system_context();
};

#endif // SHARE_VM_GC_IMPLEMENTATION_G1_TENANT_CONTEXT_HPP
