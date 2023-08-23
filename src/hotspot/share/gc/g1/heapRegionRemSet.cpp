/*
 * Copyright (c) 2001, 2018, Oracle and/or its affiliates. All rights reserved.
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
#include "gc/g1/g1BlockOffsetTable.inline.hpp"
#include "gc/g1/g1CollectedHeap.inline.hpp"
#include "gc/g1/g1ConcurrentRefine.hpp"
#include "gc/g1/heapRegionManager.inline.hpp"
#include "gc/g1/heapRegionRemSet.hpp"
#include "gc/shared/space.inline.hpp"
#include "memory/allocation.hpp"
#include "memory/padded.inline.hpp"
#include "oops/oop.inline.hpp"
#include "runtime/atomic.hpp"
#include "utilities/bitMap.inline.hpp"
#include "utilities/debug.hpp"
#include "utilities/formatBuffer.hpp"
#include "utilities/globalDefinitions.hpp"
#include "utilities/growableArray.hpp"

const char* HeapRegionRemSet::_state_strings[] =  {"Untracked", "Updating", "Complete"};
const char* HeapRegionRemSet::_short_state_strings[] =  {"UNTRA", "UPDAT", "CMPLT"};

class PerRegionTable: public CHeapObj<mtGC> {
  friend class OtherRegionsTable;
  friend class HeapRegionRemSetIterator;

  HeapRegion*     _hr;
  CHeapBitMap     _bm;
  jint            _occupied;

  // next pointer for free/allocated 'all' list
  PerRegionTable* _next;

  // prev pointer for the allocated 'all' list
  PerRegionTable* _prev;

  // next pointer in collision list
  PerRegionTable * _collision_list_next;

  // Global free list of PRTs
  static PerRegionTable* volatile _free_list;

protected:
  // We need access in order to union things into the base table.
  BitMap* bm() { return &_bm; }

  PerRegionTable(HeapRegion* hr) :
    _hr(hr),
    _occupied(0),
    _bm(HeapRegion::CardsPerRegion, mtGC),
    _collision_list_next(NULL), _next(NULL), _prev(NULL)
  {}

  void add_card_work(CardIdx_t from_card, bool par) {
    if (!_bm.at(from_card)) {
      if (par) {
        if (_bm.par_at_put(from_card, 1)) {
          Atomic::inc(&_occupied);
        }
      } else {
        _bm.at_put(from_card, 1);
        _occupied++;
      }
    }
  }

  void add_reference_work(OopOrNarrowOopStar from, bool par) {
    // Must make this robust in case "from" is not in "_hr", because of
    // concurrency.

    HeapRegion* loc_hr = hr();
    // If the test below fails, then this table was reused concurrently
    // with this operation.  This is OK, since the old table was coarsened,
    // and adding a bit to the new table is never incorrect.
    if (loc_hr->is_in_reserved(from)) {
      CardIdx_t from_card = OtherRegionsTable::card_within_region(from, loc_hr);
      add_card_work(from_card, par);
    }
  }

public:

  HeapRegion* hr() const { return OrderAccess::load_acquire(&_hr); }

  jint occupied() const {
    // Overkill, but if we ever need it...
    // guarantee(_occupied == _bm.count_one_bits(), "Check");
    return _occupied;
  }

  void init(HeapRegion* hr, bool clear_links_to_all_list) {
    if (clear_links_to_all_list) {
      set_next(NULL);
      set_prev(NULL);
    }
    _collision_list_next = NULL;
    _occupied = 0;
    _bm.clear();
    // Make sure that the bitmap clearing above has been finished before publishing
    // this PRT to concurrent threads.
    OrderAccess::release_store(&_hr, hr);
  }

  void add_reference(OopOrNarrowOopStar from) {
    add_reference_work(from, /*parallel*/ true);
  }

  void seq_add_reference(OopOrNarrowOopStar from) {
    add_reference_work(from, /*parallel*/ false);
  }

  void add_card(CardIdx_t from_card_index) {
    add_card_work(from_card_index, /*parallel*/ true);
  }

  void seq_add_card(CardIdx_t from_card_index) {
    add_card_work(from_card_index, /*parallel*/ false);
  }

  // (Destructively) union the bitmap of the current table into the given
  // bitmap (which is assumed to be of the same size.)
  void union_bitmap_into(BitMap* bm) {
    bm->set_union(_bm);
  }

  // Mem size in bytes.
  size_t mem_size() const {
    return sizeof(PerRegionTable) + _bm.size_in_words() * HeapWordSize;
  }

  // Requires "from" to be in "hr()".
  bool contains_reference(OopOrNarrowOopStar from) const {
    assert(hr()->is_in_reserved(from), "Precondition.");
    size_t card_ind = pointer_delta(from, hr()->bottom(),
                                    G1CardTable::card_size);
    return _bm.at(card_ind);
  }

  // Bulk-free the PRTs from prt to last, assumes that they are
  // linked together using their _next field.
  static void bulk_free(PerRegionTable* prt, PerRegionTable* last) {
    while (true) {
      PerRegionTable* fl = _free_list;
      last->set_next(fl);
      PerRegionTable* res = Atomic::cmpxchg(prt, &_free_list, fl);
      if (res == fl) {
        return;
      }
    }
    ShouldNotReachHere();
  }

  static void free(PerRegionTable* prt) {
    bulk_free(prt, prt);
  }

  // Returns an initialized PerRegionTable instance.
  static PerRegionTable* alloc(HeapRegion* hr) {
    PerRegionTable* fl = _free_list;
    while (fl != NULL) {
      PerRegionTable* nxt = fl->next();
      PerRegionTable* res = Atomic::cmpxchg(nxt, &_free_list, fl);
      if (res == fl) {
        fl->init(hr, true);
        return fl;
      } else {
        fl = _free_list;
      }
    }
    assert(fl == NULL, "Loop condition.");
    return new PerRegionTable(hr);
  }

  PerRegionTable* next() const { return _next; }
  void set_next(PerRegionTable* next) { _next = next; }
  PerRegionTable* prev() const { return _prev; }
  void set_prev(PerRegionTable* prev) { _prev = prev; }

  // Accessor and Modification routines for the pointer for the
  // singly linked collision list that links the PRTs within the
  // OtherRegionsTable::_fine_grain_regions hash table.
  //
  // It might be useful to also make the collision list doubly linked
  // to avoid iteration over the collisions list during scrubbing/deletion.
  // OTOH there might not be many collisions.

  PerRegionTable* collision_list_next() const {
    return _collision_list_next;
  }

  void set_collision_list_next(PerRegionTable* next) {
    _collision_list_next = next;
  }

  PerRegionTable** collision_list_next_addr() {
    return &_collision_list_next;
  }

  static size_t fl_mem_size() {
    PerRegionTable* cur = _free_list;
    size_t res = 0;
    while (cur != NULL) {
      res += cur->mem_size();
      cur = cur->next();
    }
    return res;
  }

  static void test_fl_mem_size();
};

PerRegionTable* volatile PerRegionTable::_free_list = NULL;

size_t OtherRegionsTable::_max_fine_entries = 0;
size_t OtherRegionsTable::_mod_max_fine_entries_mask = 0;
size_t OtherRegionsTable::_fine_eviction_stride = 0;
size_t OtherRegionsTable::_fine_eviction_sample_size = 0;

OtherRegionsTable::OtherRegionsTable(HeapRegion* hr, Mutex* m) :
  _g1h(G1CollectedHeap::heap()),
  _hr(hr), _m(m),
  _coarse_map(G1CollectedHeap::heap()->max_regions(), mtGC),
  _fine_grain_regions(NULL),
  _first_all_fine_prts(NULL), _last_all_fine_prts(NULL),
  _n_fine_entries(0), _n_coarse_entries(0),
  _fine_eviction_start(0),
  _sparse_table(hr)
{
  typedef PerRegionTable* PerRegionTablePtr;

  if (_max_fine_entries == 0) {
    assert(_mod_max_fine_entries_mask == 0, "Both or none.");
    size_t max_entries_log = (size_t)log2_long((jlong)G1RSetRegionEntries);
    _max_fine_entries = (size_t)1 << max_entries_log;
    _mod_max_fine_entries_mask = _max_fine_entries - 1;

    assert(_fine_eviction_sample_size == 0
           && _fine_eviction_stride == 0, "All init at same time.");
    _fine_eviction_sample_size = MAX2((size_t)4, max_entries_log);
    _fine_eviction_stride = _max_fine_entries / _fine_eviction_sample_size;
  }

  _fine_grain_regions = NEW_C_HEAP_ARRAY3(PerRegionTablePtr, _max_fine_entries,
                        mtGC, CURRENT_PC, AllocFailStrategy::RETURN_NULL);

  if (_fine_grain_regions == NULL) {
    vm_exit_out_of_memory(sizeof(void*)*_max_fine_entries, OOM_MALLOC_ERROR,
                          "Failed to allocate _fine_grain_entries.");
  }

  for (size_t i = 0; i < _max_fine_entries; i++) {
    _fine_grain_regions[i] = NULL;
  }
}

void OtherRegionsTable::link_to_all(PerRegionTable* prt) {
  // We always append to the beginning of the list for convenience;
  // the order of entries in this list does not matter.
  if (_first_all_fine_prts != NULL) {
    assert(_first_all_fine_prts->prev() == NULL, "invariant");
    _first_all_fine_prts->set_prev(prt);
    prt->set_next(_first_all_fine_prts);
  } else {
    // this is the first element we insert. Adjust the "last" pointer
    _last_all_fine_prts = prt;
    assert(prt->next() == NULL, "just checking");
  }
  // the new element is always the first element without a predecessor
  prt->set_prev(NULL);
  _first_all_fine_prts = prt;

  assert(prt->prev() == NULL, "just checking");
  assert(_first_all_fine_prts == prt, "just checking");
  assert((_first_all_fine_prts == NULL && _last_all_fine_prts == NULL) ||
         (_first_all_fine_prts != NULL && _last_all_fine_prts != NULL),
         "just checking");
  assert(_last_all_fine_prts == NULL || _last_all_fine_prts->next() == NULL,
         "just checking");
  assert(_first_all_fine_prts == NULL || _first_all_fine_prts->prev() == NULL,
         "just checking");
}

void OtherRegionsTable::unlink_from_all(PerRegionTable* prt) {
  if (prt->prev() != NULL) {
    assert(_first_all_fine_prts != prt, "just checking");
    prt->prev()->set_next(prt->next());
    // removing the last element in the list?
    if (_last_all_fine_prts == prt) {
      _last_all_fine_prts = prt->prev();
    }
  } else {
    assert(_first_all_fine_prts == prt, "just checking");
    _first_all_fine_prts = prt->next();
    // list is empty now?
    if (_first_all_fine_prts == NULL) {
      _last_all_fine_prts = NULL;
    }
  }

  if (prt->next() != NULL) {
    prt->next()->set_prev(prt->prev());
  }

  prt->set_next(NULL);
  prt->set_prev(NULL);

  assert((_first_all_fine_prts == NULL && _last_all_fine_prts == NULL) ||
         (_first_all_fine_prts != NULL && _last_all_fine_prts != NULL),
         "just checking");
  assert(_last_all_fine_prts == NULL || _last_all_fine_prts->next() == NULL,
         "just checking");
  assert(_first_all_fine_prts == NULL || _first_all_fine_prts->prev() == NULL,
         "just checking");
}

CardIdx_t OtherRegionsTable::card_within_region(OopOrNarrowOopStar within_region, HeapRegion* hr) {
  assert(hr->is_in_reserved(within_region),
         "HeapWord " PTR_FORMAT " is outside of region %u [" PTR_FORMAT ", " PTR_FORMAT ")",
         p2i(within_region), hr->hrm_index(), p2i(hr->bottom()), p2i(hr->end()));
  CardIdx_t result = (CardIdx_t)(pointer_delta((HeapWord*)within_region, hr->bottom()) >> (CardTable::card_shift - LogHeapWordSize));
  return result;
}

void OtherRegionsTable::add_reference(OopOrNarrowOopStar from, uint tid) {
  uint cur_hrm_ind = _hr->hrm_index();

  uintptr_t from_card = uintptr_t(from) >> CardTable::card_shift;

  if (G1FromCardCache::contains_or_replace(tid, cur_hrm_ind, from_card)) {
    assert(contains_reference(from), "We just found " PTR_FORMAT " in the FromCardCache", p2i(from));
    return;
  }

  // Note that this may be a continued H region.
  HeapRegion* from_hr = _g1h->heap_region_containing(from);
  RegionIdx_t from_hrm_ind = (RegionIdx_t) from_hr->hrm_index();

  // If the region is already coarsened, return.
  if (_coarse_map.at(from_hrm_ind)) {
    assert(contains_reference(from), "We just found " PTR_FORMAT " in the Coarse table", p2i(from));
    return;
  }

  // Otherwise find a per-region table to add it to.
  size_t ind = from_hrm_ind & _mod_max_fine_entries_mask;
  PerRegionTable* prt = find_region_table(ind, from_hr);
  if (prt == NULL) {
    MutexLockerEx x(_m, Mutex::_no_safepoint_check_flag);
    // Confirm that it's really not there...
    prt = find_region_table(ind, from_hr);
    if (prt == NULL) {

      CardIdx_t card_index = card_within_region(from, from_hr);

      if (G1HRRSUseSparseTable &&
          _sparse_table.add_card(from_hrm_ind, card_index)) {
        assert(contains_reference_locked(from), "We just added " PTR_FORMAT " to the Sparse table", p2i(from));
        return;
      }

      if (_n_fine_entries == _max_fine_entries) {
        prt = delete_region_table();
        // There is no need to clear the links to the 'all' list here:
        // prt will be reused immediately, i.e. remain in the 'all' list.
        prt->init(from_hr, false /* clear_links_to_all_list */);
      } else {
        prt = PerRegionTable::alloc(from_hr);
        link_to_all(prt);
      }

      PerRegionTable* first_prt = _fine_grain_regions[ind];
      prt->set_collision_list_next(first_prt);
      // The assignment into _fine_grain_regions allows the prt to
      // start being used concurrently. In addition to
      // collision_list_next which must be visible (else concurrent
      // parsing of the list, if any, may fail to see other entries),
      // the content of the prt must be visible (else for instance
      // some mark bits may not yet seem cleared or a 'later' update
      // performed by a concurrent thread could be undone when the
      // zeroing becomes visible). This requires store ordering.
      OrderAccess::release_store(&_fine_grain_regions[ind], prt);
      _n_fine_entries++;

      if (G1HRRSUseSparseTable) {
        // Transfer from sparse to fine-grain.
        SparsePRTEntry *sprt_entry = _sparse_table.get_entry(from_hrm_ind);
        assert(sprt_entry != NULL, "There should have been an entry");
        for (int i = 0; i < sprt_entry->num_valid_cards(); i++) {
          CardIdx_t c = sprt_entry->card(i);
          prt->add_card(c);
        }
        // Now we can delete the sparse entry.
        bool res = _sparse_table.delete_entry(from_hrm_ind);
        assert(res, "It should have been there.");
      }
    }
    assert(prt != NULL && prt->hr() == from_hr, "consequence");
  }
  // Note that we can't assert "prt->hr() == from_hr", because of the
  // possibility of concurrent reuse.  But see head comment of
  // OtherRegionsTable for why this is OK.
  assert(prt != NULL, "Inv");

  prt->add_reference(from);
  assert(contains_reference(from), "We just added " PTR_FORMAT " to the PRT (%d)", p2i(from), prt->contains_reference(from));
}

PerRegionTable*
OtherRegionsTable::find_region_table(size_t ind, HeapRegion* hr) const {
  assert(ind < _max_fine_entries, "Preconditions.");
  PerRegionTable* prt = _fine_grain_regions[ind];
  while (prt != NULL && prt->hr() != hr) {
    prt = prt->collision_list_next();
  }
  // Loop postcondition is the method postcondition.
  return prt;
}

jint OtherRegionsTable::_n_coarsenings = 0;

PerRegionTable* OtherRegionsTable::delete_region_table() {
  assert(_m->owned_by_self(), "Precondition");
  assert(_n_fine_entries == _max_fine_entries, "Precondition");
  PerRegionTable* max = NULL;
  jint max_occ = 0;
  PerRegionTable** max_prev = NULL;
  size_t max_ind;

  size_t i = _fine_eviction_start;
  for (size_t k = 0; k < _fine_eviction_sample_size; k++) {
    size_t ii = i;
    // Make sure we get a non-NULL sample.
    while (_fine_grain_regions[ii] == NULL) {
      ii++;
      if (ii == _max_fine_entries) ii = 0;
      guarantee(ii != i, "We must find one.");
    }
    PerRegionTable** prev = &_fine_grain_regions[ii];
    PerRegionTable* cur = *prev;
    while (cur != NULL) {
      jint cur_occ = cur->occupied();
      if (max == NULL || cur_occ > max_occ) {
        max = cur;
        max_prev = prev;
        max_ind = i;
        max_occ = cur_occ;
      }
      prev = cur->collision_list_next_addr();
      cur = cur->collision_list_next();
    }
    i = i + _fine_eviction_stride;
    if (i >= _n_fine_entries) i = i - _n_fine_entries;
  }

  _fine_eviction_start++;

  if (_fine_eviction_start >= _n_fine_entries) {
    _fine_eviction_start -= _n_fine_entries;
  }

  guarantee(max != NULL, "Since _n_fine_entries > 0");
  guarantee(max_prev != NULL, "Since max != NULL.");

  // Set the corresponding coarse bit.
  size_t max_hrm_index = (size_t) max->hr()->hrm_index();
  if (!_coarse_map.at(max_hrm_index)) {
    _coarse_map.at_put(max_hrm_index, true);
    _n_coarse_entries++;
  }

  // Unsplice.
  *max_prev = max->collision_list_next();
  Atomic::inc(&_n_coarsenings);
  _n_fine_entries--;
  return max;
}

bool OtherRegionsTable::occupancy_less_or_equal_than(size_t limit) const {
  if (limit <= (size_t)G1RSetSparseRegionEntries) {
    return occ_coarse() == 0 && _first_all_fine_prts == NULL && occ_sparse() <= limit;
  } else {
    // Current uses of this method may only use values less than G1RSetSparseRegionEntries
    // for the limit. The solution, comparing against occupied() would be too slow
    // at this time.
    Unimplemented();
    return false;
  }
}

bool OtherRegionsTable::is_empty() const {
  return occ_sparse() == 0 && occ_coarse() == 0 && _first_all_fine_prts == NULL;
}

size_t OtherRegionsTable::occupied() const {
  size_t sum = occ_fine();
  sum += occ_sparse();
  sum += occ_coarse();
  return sum;
}

size_t OtherRegionsTable::occ_fine() const {
  size_t sum = 0;

  size_t num = 0;
  PerRegionTable * cur = _first_all_fine_prts;
  while (cur != NULL) {
    sum += cur->occupied();
    cur = cur->next();
    num++;
  }
  guarantee(num == _n_fine_entries, "just checking");
  return sum;
}

size_t OtherRegionsTable::occ_coarse() const {
  return (_n_coarse_entries * HeapRegion::CardsPerRegion);
}

size_t OtherRegionsTable::occ_sparse() const {
  return _sparse_table.occupied();
}

size_t OtherRegionsTable::mem_size() const {
  size_t sum = 0;
  // all PRTs are of the same size so it is sufficient to query only one of them.
  if (_first_all_fine_prts != NULL) {
    assert(_last_all_fine_prts != NULL &&
      _first_all_fine_prts->mem_size() == _last_all_fine_prts->mem_size(), "check that mem_size() is constant");
    sum += _first_all_fine_prts->mem_size() * _n_fine_entries;
  }
  sum += (sizeof(PerRegionTable*) * _max_fine_entries);
  sum += (_coarse_map.size_in_words() * HeapWordSize);
  sum += (_sparse_table.mem_size());
  sum += sizeof(OtherRegionsTable) - sizeof(_sparse_table); // Avoid double counting above.
  return sum;
}

size_t OtherRegionsTable::static_mem_size() {
  return G1FromCardCache::static_mem_size();
}

size_t OtherRegionsTable::fl_mem_size() {
  return PerRegionTable::fl_mem_size();
}

void OtherRegionsTable::clear_fcc() {
  G1FromCardCache::clear(_hr->hrm_index());
}

void OtherRegionsTable::clear() {
  // if there are no entries, skip this step
  if (_first_all_fine_prts != NULL) {
    guarantee(_first_all_fine_prts != NULL && _last_all_fine_prts != NULL, "just checking");
    PerRegionTable::bulk_free(_first_all_fine_prts, _last_all_fine_prts);
    memset(_fine_grain_regions, 0, _max_fine_entries * sizeof(_fine_grain_regions[0]));
  } else {
    guarantee(_first_all_fine_prts == NULL && _last_all_fine_prts == NULL, "just checking");
  }

  _first_all_fine_prts = _last_all_fine_prts = NULL;
  _sparse_table.clear();
  if (_n_coarse_entries > 0) {
    _coarse_map.clear();
  }
  _n_fine_entries = 0;
  _n_coarse_entries = 0;

  clear_fcc();
}

bool OtherRegionsTable::contains_reference(OopOrNarrowOopStar from) const {
  // Cast away const in this case.
  MutexLockerEx x((Mutex*)_m, Mutex::_no_safepoint_check_flag);
  return contains_reference_locked(from);
}

bool OtherRegionsTable::contains_reference_locked(OopOrNarrowOopStar from) const {
  HeapRegion* hr = _g1h->heap_region_containing(from);
  RegionIdx_t hr_ind = (RegionIdx_t) hr->hrm_index();
  // Is this region in the coarse map?
  if (_coarse_map.at(hr_ind)) return true;

  PerRegionTable* prt = find_region_table(hr_ind & _mod_max_fine_entries_mask,
                                          hr);
  if (prt != NULL) {
    return prt->contains_reference(from);

  } else {
    CardIdx_t card_index = card_within_region(from, hr);
    return _sparse_table.contains_card(hr_ind, card_index);
  }
}

void
OtherRegionsTable::do_cleanup_work(HRRSCleanupTask* hrrs_cleanup_task) {
  _sparse_table.do_cleanup_work(hrrs_cleanup_task);
}

HeapRegionRemSet::HeapRegionRemSet(G1BlockOffsetTable* bot,
                                   HeapRegion* hr)
  : _bot(bot),
    _m(Mutex::leaf, FormatBuffer<128>("HeapRegionRemSet lock #%u", hr->hrm_index()), true, Monitor::_safepoint_check_never),
    _code_roots(),
    _state(Untracked),
    _other_regions(hr, &_m) {
}

void HeapRegionRemSet::setup_remset_size() {
  // Setup sparse and fine-grain tables sizes.
  // table_size = base * (log(region_size / 1M) + 1)
  const int LOG_M = 20;
  int region_size_log_mb = MAX2(HeapRegion::LogOfHRGrainBytes - LOG_M, 0);
  if (FLAG_IS_DEFAULT(G1RSetSparseRegionEntries)) {
    G1RSetSparseRegionEntries = G1RSetSparseRegionEntriesBase * (region_size_log_mb + 1);
  }
  if (FLAG_IS_DEFAULT(G1RSetRegionEntries)) {
    G1RSetRegionEntries = G1RSetRegionEntriesBase * (region_size_log_mb + 1);
  }
  guarantee(G1RSetSparseRegionEntries > 0 && G1RSetRegionEntries > 0 , "Sanity");
}

void HeapRegionRemSet::cleanup() {
  SparsePRT::cleanup_all();
}

void HeapRegionRemSet::clear(bool only_cardset) {
  MutexLockerEx x(&_m, Mutex::_no_safepoint_check_flag);
  clear_locked(only_cardset);
}

void HeapRegionRemSet::clear_locked(bool only_cardset) {
  if (!only_cardset) {
    _code_roots.clear();
  }
  _other_regions.clear();
  set_state_empty();
  assert(occupied_locked() == 0, "Should be clear.");
}

// Code roots support
//
// The code root set is protected by two separate locking schemes
// When at safepoint the per-hrrs lock must be held during modifications
// except when doing a full gc.
// When not at safepoint the CodeCache_lock must be held during modifications.
// When concurrent readers access the contains() function
// (during the evacuation phase) no removals are allowed.

void HeapRegionRemSet::add_strong_code_root(nmethod* nm) {
  assert(nm != NULL, "sanity");
  assert((!CodeCache_lock->owned_by_self() || SafepointSynchronize::is_at_safepoint()),
          "should call add_strong_code_root_locked instead. CodeCache_lock->owned_by_self(): %s, is_at_safepoint(): %s",
          BOOL_TO_STR(CodeCache_lock->owned_by_self()), BOOL_TO_STR(SafepointSynchronize::is_at_safepoint()));
  // Optimistic unlocked contains-check
  if (!_code_roots.contains(nm)) {
    MutexLockerEx ml(&_m, Mutex::_no_safepoint_check_flag);
    add_strong_code_root_locked(nm);
  }
}

void HeapRegionRemSet::add_strong_code_root_locked(nmethod* nm) {
  assert(nm != NULL, "sanity");
  assert((CodeCache_lock->owned_by_self() ||
         (SafepointSynchronize::is_at_safepoint() &&
          (_m.owned_by_self() || Thread::current()->is_VM_thread()))),
          "not safely locked. CodeCache_lock->owned_by_self(): %s, is_at_safepoint(): %s, _m.owned_by_self(): %s, Thread::current()->is_VM_thread(): %s",
          BOOL_TO_STR(CodeCache_lock->owned_by_self()), BOOL_TO_STR(SafepointSynchronize::is_at_safepoint()),
          BOOL_TO_STR(_m.owned_by_self()), BOOL_TO_STR(Thread::current()->is_VM_thread()));
  _code_roots.add(nm);
}

void HeapRegionRemSet::remove_strong_code_root(nmethod* nm) {
  assert(nm != NULL, "sanity");
  assert_locked_or_safepoint(CodeCache_lock);

  MutexLockerEx ml(CodeCache_lock->owned_by_self() ? NULL : &_m, Mutex::_no_safepoint_check_flag);
  _code_roots.remove(nm);

  // Check that there were no duplicates
  guarantee(!_code_roots.contains(nm), "duplicate entry found");
}

void HeapRegionRemSet::strong_code_roots_do(CodeBlobClosure* blk) const {
  _code_roots.nmethods_do(blk);
}

void HeapRegionRemSet::clean_strong_code_roots(HeapRegion* hr) {
  _code_roots.clean(hr);
}

size_t HeapRegionRemSet::strong_code_roots_mem_size() {
  return _code_roots.mem_size();
}

HeapRegionRemSetIterator:: HeapRegionRemSetIterator(HeapRegionRemSet* hrrs) :
  _hrrs(hrrs),
  _g1h(G1CollectedHeap::heap()),
  _coarse_map(&hrrs->_other_regions._coarse_map),
  _bot(hrrs->_bot),
  _is(Sparse),
  // Set these values so that we increment to the first region.
  _coarse_cur_region_index(-1),
  _coarse_cur_region_cur_card(HeapRegion::CardsPerRegion-1),
  _cur_card_in_prt(HeapRegion::CardsPerRegion),
  _fine_cur_prt(NULL),
  _n_yielded_coarse(0),
  _n_yielded_fine(0),
  _n_yielded_sparse(0),
  _sparse_iter(&hrrs->_other_regions._sparse_table) {}

bool HeapRegionRemSetIterator::coarse_has_next(size_t& card_index) {
  if (_hrrs->_other_regions._n_coarse_entries == 0) return false;
  // Go to the next card.
  _coarse_cur_region_cur_card++;
  // Was the last the last card in the current region?
  if (_coarse_cur_region_cur_card == HeapRegion::CardsPerRegion) {
    // Yes: find the next region.  This may leave _coarse_cur_region_index
    // Set to the last index, in which case there are no more coarse
    // regions.
    _coarse_cur_region_index =
      (int) _coarse_map->get_next_one_offset(_coarse_cur_region_index + 1);
    if ((size_t)_coarse_cur_region_index < _coarse_map->size()) {
      _coarse_cur_region_cur_card = 0;
      HeapWord* r_bot =
        _g1h->region_at((uint) _coarse_cur_region_index)->bottom();
      _cur_region_card_offset = _bot->index_for_raw(r_bot);
    } else {
      return false;
    }
  }
  // If we didn't return false above, then we can yield a card.
  card_index = _cur_region_card_offset + _coarse_cur_region_cur_card;
  return true;
}

bool HeapRegionRemSetIterator::fine_has_next(size_t& card_index) {
  if (fine_has_next()) {
    _cur_card_in_prt =
      _fine_cur_prt->_bm.get_next_one_offset(_cur_card_in_prt + 1);
  }
  if (_cur_card_in_prt == HeapRegion::CardsPerRegion) {
    // _fine_cur_prt may still be NULL in case if there are not PRTs at all for
    // the remembered set.
    if (_fine_cur_prt == NULL || _fine_cur_prt->next() == NULL) {
      return false;
    }
    PerRegionTable* next_prt = _fine_cur_prt->next();
    switch_to_prt(next_prt);
    _cur_card_in_prt = _fine_cur_prt->_bm.get_next_one_offset(_cur_card_in_prt + 1);
  }

  card_index = _cur_region_card_offset + _cur_card_in_prt;
  guarantee(_cur_card_in_prt < HeapRegion::CardsPerRegion,
            "Card index " SIZE_FORMAT " must be within the region", _cur_card_in_prt);
  return true;
}

bool HeapRegionRemSetIterator::fine_has_next() {
  return _cur_card_in_prt != HeapRegion::CardsPerRegion;
}

void HeapRegionRemSetIterator::switch_to_prt(PerRegionTable* prt) {
  assert(prt != NULL, "Cannot switch to NULL prt");
  _fine_cur_prt = prt;

  HeapWord* r_bot = _fine_cur_prt->hr()->bottom();
  _cur_region_card_offset = _bot->index_for_raw(r_bot);

  // The bitmap scan for the PRT always scans from _cur_region_cur_card + 1.
  // To avoid special-casing this start case, and not miss the first bitmap
  // entry, initialize _cur_region_cur_card with -1 instead of 0.
  _cur_card_in_prt = (size_t)-1;
}

bool HeapRegionRemSetIterator::has_next(size_t& card_index) {
  switch (_is) {
  case Sparse: {
    if (_sparse_iter.has_next(card_index)) {
      _n_yielded_sparse++;
      return true;
    }
    // Otherwise, deliberate fall-through
    _is = Fine;
    PerRegionTable* initial_fine_prt = _hrrs->_other_regions._first_all_fine_prts;
    if (initial_fine_prt != NULL) {
      switch_to_prt(_hrrs->_other_regions._first_all_fine_prts);
    }
  }
  case Fine:
    if (fine_has_next(card_index)) {
      _n_yielded_fine++;
      return true;
    }
    // Otherwise, deliberate fall-through
    _is = Coarse;
  case Coarse:
    if (coarse_has_next(card_index)) {
      _n_yielded_coarse++;
      return true;
    }
    // Otherwise...
    break;
  }
  return false;
}

void HeapRegionRemSet::reset_for_cleanup_tasks() {
  SparsePRT::reset_for_cleanup_tasks();
}

void HeapRegionRemSet::do_cleanup_work(HRRSCleanupTask* hrrs_cleanup_task) {
  _other_regions.do_cleanup_work(hrrs_cleanup_task);
}

void HeapRegionRemSet::finish_cleanup_task(HRRSCleanupTask* hrrs_cleanup_task) {
  SparsePRT::finish_cleanup_task(hrrs_cleanup_task);
}

#ifndef PRODUCT
void HeapRegionRemSet::test() {
  os::sleep(Thread::current(), (jlong)5000, false);
  G1CollectedHeap* g1h = G1CollectedHeap::heap();

  // Run with "-XX:G1LogRSetRegionEntries=2", so that 1 and 5 end up in same
  // hash bucket.
  HeapRegion* hr0 = g1h->region_at(0);
  HeapRegion* hr1 = g1h->region_at(1);
  HeapRegion* hr2 = g1h->region_at(5);
  HeapRegion* hr3 = g1h->region_at(6);
  HeapRegion* hr4 = g1h->region_at(7);
  HeapRegion* hr5 = g1h->region_at(8);

  HeapWord* hr1_start = hr1->bottom();
  HeapWord* hr1_mid = hr1_start + HeapRegion::GrainWords/2;
  HeapWord* hr1_last = hr1->end() - 1;

  HeapWord* hr2_start = hr2->bottom();
  HeapWord* hr2_mid = hr2_start + HeapRegion::GrainWords/2;
  HeapWord* hr2_last = hr2->end() - 1;

  HeapWord* hr3_start = hr3->bottom();
  HeapWord* hr3_mid = hr3_start + HeapRegion::GrainWords/2;
  HeapWord* hr3_last = hr3->end() - 1;

  HeapRegionRemSet* hrrs = hr0->rem_set();

  // Make three references from region 0x101...
  hrrs->add_reference((OopOrNarrowOopStar)hr1_start);
  hrrs->add_reference((OopOrNarrowOopStar)hr1_mid);
  hrrs->add_reference((OopOrNarrowOopStar)hr1_last);

  hrrs->add_reference((OopOrNarrowOopStar)hr2_start);
  hrrs->add_reference((OopOrNarrowOopStar)hr2_mid);
  hrrs->add_reference((OopOrNarrowOopStar)hr2_last);

  hrrs->add_reference((OopOrNarrowOopStar)hr3_start);
  hrrs->add_reference((OopOrNarrowOopStar)hr3_mid);
  hrrs->add_reference((OopOrNarrowOopStar)hr3_last);

  // Now cause a coarsening.
  hrrs->add_reference((OopOrNarrowOopStar)hr4->bottom());
  hrrs->add_reference((OopOrNarrowOopStar)hr5->bottom());

  // Now, does iteration yield these three?
  HeapRegionRemSetIterator iter(hrrs);
  size_t sum = 0;
  size_t card_index;
  while (iter.has_next(card_index)) {
    HeapWord* card_start = g1h->bot()->address_for_index(card_index);
    tty->print_cr("  Card " PTR_FORMAT ".", p2i(card_start));
    sum++;
  }
  guarantee(sum == 11 - 3 + 2048, "Failure");
  guarantee(sum == hrrs->occupied(), "Failure");
}
#endif
