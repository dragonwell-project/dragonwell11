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
#include "classfile/stringTable.hpp"
#include "gc/cms/cmsHeap.inline.hpp"
#include "gc/cms/compactibleFreeListSpace.hpp"
#include "gc/cms/concurrentMarkSweepGeneration.hpp"
#include "gc/cms/parNewGeneration.inline.hpp"
#include "gc/cms/parOopClosures.inline.hpp"
#include "gc/serial/defNewGeneration.inline.hpp"
#include "gc/shared/adaptiveSizePolicy.hpp"
#include "gc/shared/ageTable.inline.hpp"
#include "gc/shared/copyFailedInfo.hpp"
#include "gc/shared/gcHeapSummary.hpp"
#include "gc/shared/gcTimer.hpp"
#include "gc/shared/gcTrace.hpp"
#include "gc/shared/gcTraceTime.inline.hpp"
#include "gc/shared/genOopClosures.inline.hpp"
#include "gc/shared/generation.hpp"
#include "gc/shared/plab.inline.hpp"
#include "gc/shared/preservedMarks.inline.hpp"
#include "gc/shared/referencePolicy.hpp"
#include "gc/shared/referenceProcessorPhaseTimes.hpp"
#include "gc/shared/space.hpp"
#include "gc/shared/spaceDecorator.hpp"
#include "gc/shared/strongRootsScope.hpp"
#include "gc/shared/taskqueue.inline.hpp"
#include "gc/shared/weakProcessor.hpp"
#include "gc/shared/workgroup.hpp"
#include "logging/log.hpp"
#include "logging/logStream.hpp"
#include "memory/iterator.inline.hpp"
#include "memory/resourceArea.hpp"
#include "oops/access.inline.hpp"
#include "oops/compressedOops.inline.hpp"
#include "oops/objArrayOop.hpp"
#include "oops/oop.inline.hpp"
#include "runtime/atomic.hpp"
#include "runtime/handles.hpp"
#include "runtime/handles.inline.hpp"
#include "runtime/java.hpp"
#include "runtime/thread.inline.hpp"
#include "utilities/copy.hpp"
#include "utilities/globalDefinitions.hpp"
#include "utilities/stack.inline.hpp"

ParScanThreadState::ParScanThreadState(Space* to_space_,
                                       ParNewGeneration* young_gen_,
                                       Generation* old_gen_,
                                       int thread_num_,
                                       ObjToScanQueueSet* work_queue_set_,
                                       Stack<oop, mtGC>* overflow_stacks_,
                                       PreservedMarks* preserved_marks_,
                                       size_t desired_plab_sz_,
                                       TaskTerminator& term_) :
  _to_space(to_space_),
  _old_gen(old_gen_),
  _young_gen(young_gen_),
  _thread_num(thread_num_),
  _work_queue(work_queue_set_->queue(thread_num_)),
  _to_space_full(false),
  _overflow_stack(overflow_stacks_ ? overflow_stacks_ + thread_num_ : NULL),
  _preserved_marks(preserved_marks_),
  _ageTable(false), // false ==> not the global age table, no perf data.
  _to_space_alloc_buffer(desired_plab_sz_),
  _to_space_closure(young_gen_, this),
  _old_gen_closure(young_gen_, this),
  _to_space_root_closure(young_gen_, this),
  _old_gen_root_closure(young_gen_, this),
  _older_gen_closure(young_gen_, this),
  _evacuate_followers(this, &_to_space_closure, &_old_gen_closure,
                      &_to_space_root_closure, young_gen_, &_old_gen_root_closure,
                      work_queue_set_, term_.terminator()),
  _is_alive_closure(young_gen_),
  _scan_weak_ref_closure(young_gen_, this),
  _keep_alive_closure(&_scan_weak_ref_closure),
  _strong_roots_time(0.0),
  _term_time(0.0)
{
  #if TASKQUEUE_STATS
  _term_attempts = 0;
  _overflow_refills = 0;
  _overflow_refill_objs = 0;
  #endif // TASKQUEUE_STATS

  _survivor_chunk_array = (ChunkArray*) old_gen()->get_data_recorder(thread_num());
  _start = os::elapsedTime();
  _old_gen_closure.set_generation(old_gen_);
  _old_gen_root_closure.set_generation(old_gen_);
}

void ParScanThreadState::record_survivor_plab(HeapWord* plab_start,
                                              size_t plab_word_size) {
  ChunkArray* sca = survivor_chunk_array();
  if (sca != NULL) {
    // A non-null SCA implies that we want the PLAB data recorded.
    sca->record_sample(plab_start, plab_word_size);
  }
}

bool ParScanThreadState::should_be_partially_scanned(oop new_obj, oop old_obj) const {
  return new_obj->is_objArray() &&
         arrayOop(new_obj)->length() > ParGCArrayScanChunk &&
         new_obj != old_obj;
}

void ParScanThreadState::scan_partial_array_and_push_remainder(oop old) {
  assert(old->is_objArray(), "must be obj array");
  assert(old->is_forwarded(), "must be forwarded");
  assert(CMSHeap::heap()->is_in_reserved(old), "must be in heap.");
  assert(!old_gen()->is_in(old), "must be in young generation.");

  objArrayOop obj = objArrayOop(old->forwardee());
  // Process ParGCArrayScanChunk elements now
  // and push the remainder back onto queue
  int start     = arrayOop(old)->length();
  int end       = obj->length();
  int remainder = end - start;
  assert(start <= end, "just checking");
  if (remainder > 2 * ParGCArrayScanChunk) {
    // Test above combines last partial chunk with a full chunk
    end = start + ParGCArrayScanChunk;
    arrayOop(old)->set_length(end);
    // Push remainder.
    bool ok = work_queue()->push(old);
    assert(ok, "just popped, push must be okay");
  } else {
    // Restore length so that it can be used if there
    // is a promotion failure and forwarding pointers
    // must be removed.
    arrayOop(old)->set_length(end);
  }

  // process our set of indices (include header in first chunk)
  // should make sure end is even (aligned to HeapWord in case of compressed oops)
  if ((HeapWord *)obj < young_old_boundary()) {
    // object is in to_space
    obj->oop_iterate_range(&_to_space_closure, start, end);
  } else {
    // object is in old generation
    obj->oop_iterate_range(&_old_gen_closure, start, end);
  }
}

void ParScanThreadState::trim_queues(int max_size) {
  ObjToScanQueue* queue = work_queue();
  do {
    while (queue->size() > (juint)max_size) {
      oop obj_to_scan;
      if (queue->pop_local(obj_to_scan)) {
        if ((HeapWord *)obj_to_scan < young_old_boundary()) {
          if (obj_to_scan->is_objArray() &&
              obj_to_scan->is_forwarded() &&
              obj_to_scan->forwardee() != obj_to_scan) {
            scan_partial_array_and_push_remainder(obj_to_scan);
          } else {
            // object is in to_space
            obj_to_scan->oop_iterate(&_to_space_closure);
          }
        } else {
          // object is in old generation
          obj_to_scan->oop_iterate(&_old_gen_closure);
        }
      }
    }
    // For the  case of compressed oops, we have a private, non-shared
    // overflow stack, so we eagerly drain it so as to more evenly
    // distribute load early. Note: this may be good to do in
    // general rather than delay for the final stealing phase.
    // If applicable, we'll transfer a set of objects over to our
    // work queue, allowing them to be stolen and draining our
    // private overflow stack.
  } while (ParGCTrimOverflow && young_gen()->take_from_overflow_list(this));
}

bool ParScanThreadState::take_from_overflow_stack() {
  assert(ParGCUseLocalOverflow, "Else should not call");
  assert(young_gen()->overflow_list() == NULL, "Error");
  ObjToScanQueue* queue = work_queue();
  Stack<oop, mtGC>* const of_stack = overflow_stack();
  const size_t num_overflow_elems = of_stack->size();
  const size_t space_available = queue->max_elems() - queue->size();
  const size_t num_take_elems = MIN3(space_available / 4,
                                     (size_t)ParGCDesiredObjsFromOverflowList,
                                     num_overflow_elems);
  // Transfer the most recent num_take_elems from the overflow
  // stack to our work queue.
  for (size_t i = 0; i != num_take_elems; i++) {
    oop cur = of_stack->pop();
    oop obj_to_push = cur->forwardee();
    assert(CMSHeap::heap()->is_in_reserved(cur), "Should be in heap");
    assert(!old_gen()->is_in_reserved(cur), "Should be in young gen");
    assert(CMSHeap::heap()->is_in_reserved(obj_to_push), "Should be in heap");
    if (should_be_partially_scanned(obj_to_push, cur)) {
      assert(arrayOop(cur)->length() == 0, "entire array remaining to be scanned");
      obj_to_push = cur;
    }
    bool ok = queue->push(obj_to_push);
    assert(ok, "Should have succeeded");
  }
  assert(young_gen()->overflow_list() == NULL, "Error");
  return num_take_elems > 0;  // was something transferred?
}

void ParScanThreadState::push_on_overflow_stack(oop p) {
  assert(ParGCUseLocalOverflow, "Else should not call");
  overflow_stack()->push(p);
  assert(young_gen()->overflow_list() == NULL, "Error");
}

HeapWord* ParScanThreadState::alloc_in_to_space_slow(size_t word_sz) {
  // If the object is small enough, try to reallocate the buffer.
  HeapWord* obj = NULL;
  if (!_to_space_full) {
    PLAB* const plab = to_space_alloc_buffer();
    Space* const sp  = to_space();
    if (word_sz * 100 < ParallelGCBufferWastePct * plab->word_sz()) {
      // Is small enough; abandon this buffer and start a new one.
      plab->retire();
      // The minimum size has to be twice SurvivorAlignmentInBytes to
      // allow for padding used in the alignment of 1 word.  A padding
      // of 1 is too small for a filler word so the padding size will
      // be increased by SurvivorAlignmentInBytes.
      size_t min_usable_size = 2 * static_cast<size_t>(SurvivorAlignmentInBytes >> LogHeapWordSize);
      size_t buf_size = MAX2(plab->word_sz(), min_usable_size);
      HeapWord* buf_space = sp->par_allocate(buf_size);
      if (buf_space == NULL) {
        const size_t min_bytes = MAX2(PLAB::min_size(), min_usable_size) << LogHeapWordSize;
        size_t free_bytes = sp->free();
        while(buf_space == NULL && free_bytes >= min_bytes) {
          buf_size = free_bytes >> LogHeapWordSize;
          assert(buf_size == (size_t)align_object_size(buf_size), "Invariant");
          buf_space  = sp->par_allocate(buf_size);
          free_bytes = sp->free();
        }
      }
      if (buf_space != NULL) {
        plab->set_buf(buf_space, buf_size);
        record_survivor_plab(buf_space, buf_size);
        obj = plab->allocate_aligned(word_sz, SurvivorAlignmentInBytes);
        // Note that we cannot compare buf_size < word_sz below
        // because of AlignmentReserve (see PLAB::allocate()).
        assert(obj != NULL || plab->words_remaining() < word_sz,
               "Else should have been able to allocate requested object size "
               SIZE_FORMAT ", PLAB size " SIZE_FORMAT ", SurvivorAlignmentInBytes "
               SIZE_FORMAT ", words_remaining " SIZE_FORMAT,
               word_sz, buf_size, SurvivorAlignmentInBytes, plab->words_remaining());
        // It's conceivable that we may be able to use the
        // buffer we just grabbed for subsequent small requests
        // even if not for this one.
      } else {
        // We're used up.
        _to_space_full = true;
      }
    } else {
      // Too large; allocate the object individually.
      obj = sp->par_allocate(word_sz);
    }
  }
  return obj;
}

void ParScanThreadState::undo_alloc_in_to_space(HeapWord* obj, size_t word_sz) {
  to_space_alloc_buffer()->undo_allocation(obj, word_sz);
}

void ParScanThreadState::print_promotion_failure_size() {
  if (_promotion_failed_info.has_failed()) {
    log_trace(gc, promotion)(" (%d: promotion failure size = " SIZE_FORMAT ") ",
                             _thread_num, _promotion_failed_info.first_size());
  }
}

class ParScanThreadStateSet: StackObj {
public:
  // Initializes states for the specified number of threads;
  ParScanThreadStateSet(int                     num_threads,
                        Space&                  to_space,
                        ParNewGeneration&       young_gen,
                        Generation&             old_gen,
                        ObjToScanQueueSet&      queue_set,
                        Stack<oop, mtGC>*       overflow_stacks_,
                        PreservedMarksSet&      preserved_marks_set,
                        size_t                  desired_plab_sz,
                        TaskTerminator& term);

  ~ParScanThreadStateSet() { TASKQUEUE_STATS_ONLY(reset_stats()); }

  inline ParScanThreadState& thread_state(int i);

  void trace_promotion_failed(const YoungGCTracer* gc_tracer);
  void reset(uint active_workers, bool promotion_failed);
  void flush();

  #if TASKQUEUE_STATS
  static void
    print_termination_stats_hdr(outputStream* const st);
  void print_termination_stats();
  static void
    print_taskqueue_stats_hdr(outputStream* const st);
  void print_taskqueue_stats();
  void reset_stats();
  #endif // TASKQUEUE_STATS

private:
  TaskTerminator&         _term;
  ParNewGeneration&       _young_gen;
  Generation&             _old_gen;
  ParScanThreadState*     _per_thread_states;
  const int               _num_threads;
 public:
  bool is_valid(int id) const { return id < _num_threads; }
  ParallelTaskTerminator* terminator() { return _term.terminator(); }
};

ParScanThreadStateSet::ParScanThreadStateSet(int num_threads,
                                             Space& to_space,
                                             ParNewGeneration& young_gen,
                                             Generation& old_gen,
                                             ObjToScanQueueSet& queue_set,
                                             Stack<oop, mtGC>* overflow_stacks,
                                             PreservedMarksSet& preserved_marks_set,
                                             size_t desired_plab_sz,
                                             TaskTerminator& term)
  : _young_gen(young_gen),
    _old_gen(old_gen),
    _term(term),
    _per_thread_states(NEW_RESOURCE_ARRAY(ParScanThreadState, num_threads)),
    _num_threads(num_threads)
{
  assert(num_threads > 0, "sanity check!");
  assert(ParGCUseLocalOverflow == (overflow_stacks != NULL),
         "overflow_stack allocation mismatch");
  // Initialize states.
  for (int i = 0; i < num_threads; ++i) {
    new(_per_thread_states + i)
      ParScanThreadState(&to_space, &young_gen, &old_gen, i, &queue_set,
                         overflow_stacks, preserved_marks_set.get(i),
                         desired_plab_sz, term);
  }
}

inline ParScanThreadState& ParScanThreadStateSet::thread_state(int i) {
  assert(i >= 0 && i < _num_threads, "sanity check!");
  return _per_thread_states[i];
}

void ParScanThreadStateSet::trace_promotion_failed(const YoungGCTracer* gc_tracer) {
  for (int i = 0; i < _num_threads; ++i) {
    if (thread_state(i).promotion_failed()) {
      gc_tracer->report_promotion_failed(thread_state(i).promotion_failed_info());
      thread_state(i).promotion_failed_info().reset();
    }
  }
}

void ParScanThreadStateSet::reset(uint active_threads, bool promotion_failed) {
  _term.terminator()->reset_for_reuse(active_threads);
  if (promotion_failed) {
    for (int i = 0; i < _num_threads; ++i) {
      thread_state(i).print_promotion_failure_size();
    }
  }
}

#if TASKQUEUE_STATS
void ParScanThreadState::reset_stats() {
  taskqueue_stats().reset();
  _term_attempts = 0;
  _overflow_refills = 0;
  _overflow_refill_objs = 0;
}

void ParScanThreadStateSet::reset_stats() {
  for (int i = 0; i < _num_threads; ++i) {
    thread_state(i).reset_stats();
  }
}

void ParScanThreadStateSet::print_termination_stats_hdr(outputStream* const st) {
  st->print_raw_cr("GC Termination Stats");
  st->print_raw_cr("     elapsed  --strong roots-- -------termination-------");
  st->print_raw_cr("thr     ms        ms       %       ms       %   attempts");
  st->print_raw_cr("--- --------- --------- ------ --------- ------ --------");
}

void ParScanThreadStateSet::print_termination_stats() {
  Log(gc, task, stats) log;
  if (!log.is_debug()) {
    return;
  }

  ResourceMark rm;
  LogStream ls(log.debug());
  outputStream* st = &ls;

  print_termination_stats_hdr(st);

  for (int i = 0; i < _num_threads; ++i) {
    const ParScanThreadState & pss = thread_state(i);
    const double elapsed_ms = pss.elapsed_time() * 1000.0;
    const double s_roots_ms = pss.strong_roots_time() * 1000.0;
    const double term_ms = pss.term_time() * 1000.0;
    st->print_cr("%3d %9.2f %9.2f %6.2f %9.2f %6.2f " SIZE_FORMAT_W(8),
                 i, elapsed_ms, s_roots_ms, s_roots_ms * 100 / elapsed_ms,
                 term_ms, term_ms * 100 / elapsed_ms, pss.term_attempts());
  }
}

// Print stats related to work queue activity.
void ParScanThreadStateSet::print_taskqueue_stats_hdr(outputStream* const st) {
  st->print_raw_cr("GC Task Stats");
  st->print_raw("thr "); TaskQueueStats::print_header(1, st); st->cr();
  st->print_raw("--- "); TaskQueueStats::print_header(2, st); st->cr();
}

void ParScanThreadStateSet::print_taskqueue_stats() {
  if (!log_develop_is_enabled(Trace, gc, task, stats)) {
    return;
  }
  Log(gc, task, stats) log;
  ResourceMark rm;
  LogStream ls(log.trace());
  outputStream* st = &ls;
  print_taskqueue_stats_hdr(st);

  TaskQueueStats totals;
  for (int i = 0; i < _num_threads; ++i) {
    const ParScanThreadState & pss = thread_state(i);
    const TaskQueueStats & stats = pss.taskqueue_stats();
    st->print("%3d ", i); stats.print(st); st->cr();
    totals += stats;

    if (pss.overflow_refills() > 0) {
      st->print_cr("    " SIZE_FORMAT_W(10) " overflow refills    "
                   SIZE_FORMAT_W(10) " overflow objects",
                   pss.overflow_refills(), pss.overflow_refill_objs());
    }
  }
  st->print("tot "); totals.print(st); st->cr();

  DEBUG_ONLY(totals.verify());
}
#endif // TASKQUEUE_STATS

void ParScanThreadStateSet::flush() {
  // Work in this loop should be kept as lightweight as
  // possible since this might otherwise become a bottleneck
  // to scaling. Should we add heavy-weight work into this
  // loop, consider parallelizing the loop into the worker threads.
  for (int i = 0; i < _num_threads; ++i) {
    ParScanThreadState& par_scan_state = thread_state(i);

    // Flush stats related to To-space PLAB activity and
    // retire the last buffer.
    par_scan_state.to_space_alloc_buffer()->flush_and_retire_stats(_young_gen.plab_stats());

    // Every thread has its own age table.  We need to merge
    // them all into one.
    AgeTable *local_table = par_scan_state.age_table();
    _young_gen.age_table()->merge(local_table);

    // Inform old gen that we're done.
    _old_gen.par_promote_alloc_done(i);
  }

  if (UseConcMarkSweepGC) {
    // We need to call this even when ResizeOldPLAB is disabled
    // so as to avoid breaking some asserts. While we may be able
    // to avoid this by reorganizing the code a bit, I am loathe
    // to do that unless we find cases where ergo leads to bad
    // performance.
    CompactibleFreeListSpaceLAB::compute_desired_plab_size();
  }
}

ParScanClosure::ParScanClosure(ParNewGeneration* g,
                               ParScanThreadState* par_scan_state) :
  OopsInClassLoaderDataOrGenClosure(g), _par_scan_state(par_scan_state), _g(g) {
  _boundary = _g->reserved().end();
}

void ParRootScanWithBarrierTwoGensClosure::do_oop(oop* p)       { ParScanClosure::do_oop_work(p, true, true); }
void ParRootScanWithBarrierTwoGensClosure::do_oop(narrowOop* p) { ParScanClosure::do_oop_work(p, true, true); }

void ParRootScanWithoutBarrierClosure::do_oop(oop* p)       { ParScanClosure::do_oop_work(p, false, true); }
void ParRootScanWithoutBarrierClosure::do_oop(narrowOop* p) { ParScanClosure::do_oop_work(p, false, true); }

ParScanWeakRefClosure::ParScanWeakRefClosure(ParNewGeneration* g,
                                             ParScanThreadState* par_scan_state)
  : ScanWeakRefClosure(g), _par_scan_state(par_scan_state)
{}

#ifdef WIN32
#pragma warning(disable: 4786) /* identifier was truncated to '255' characters in the browser information */
#endif

ParEvacuateFollowersClosure::ParEvacuateFollowersClosure(
    ParScanThreadState* par_scan_state_,
    ParScanWithoutBarrierClosure* to_space_closure_,
    ParScanWithBarrierClosure* old_gen_closure_,
    ParRootScanWithoutBarrierClosure* to_space_root_closure_,
    ParNewGeneration* par_gen_,
    ParRootScanWithBarrierTwoGensClosure* old_gen_root_closure_,
    ObjToScanQueueSet* task_queues_,
    ParallelTaskTerminator* terminator_) :

    _par_scan_state(par_scan_state_),
    _to_space_closure(to_space_closure_),
    _old_gen_closure(old_gen_closure_),
    _to_space_root_closure(to_space_root_closure_),
    _old_gen_root_closure(old_gen_root_closure_),
    _par_gen(par_gen_),
    _task_queues(task_queues_),
    _terminator(terminator_)
{}

void ParEvacuateFollowersClosure::do_void() {
  ObjToScanQueue* work_q = par_scan_state()->work_queue();

  while (true) {
    // Scan to-space and old-gen objs until we run out of both.
    oop obj_to_scan;
    par_scan_state()->trim_queues(0);

    // We have no local work, attempt to steal from other threads.

    // Attempt to steal work from promoted.
    if (task_queues()->steal(par_scan_state()->thread_num(),
                             obj_to_scan)) {
      bool res = work_q->push(obj_to_scan);
      assert(res, "Empty queue should have room for a push.");

      // If successful, goto Start.
      continue;

      // Try global overflow list.
    } else if (par_gen()->take_from_overflow_list(par_scan_state())) {
      continue;
    }

    // Otherwise, offer termination.
    par_scan_state()->start_term_time();
    if (terminator()->offer_termination()) break;
    par_scan_state()->end_term_time();
  }
  assert(par_gen()->_overflow_list == NULL && par_gen()->_num_par_pushes == 0,
         "Broken overflow list?");
  // Finish the last termination pause.
  par_scan_state()->end_term_time();
}

ParNewGenTask::ParNewGenTask(ParNewGeneration* young_gen,
                             Generation* old_gen,
                             HeapWord* young_old_boundary,
                             ParScanThreadStateSet* state_set,
                             StrongRootsScope* strong_roots_scope) :
    AbstractGangTask("ParNewGeneration collection"),
    _young_gen(young_gen), _old_gen(old_gen),
    _young_old_boundary(young_old_boundary),
    _state_set(state_set),
    _strong_roots_scope(strong_roots_scope),
    _par_state_string(StringTable::weak_storage())
{}

void ParNewGenTask::work(uint worker_id) {
  CMSHeap* heap = CMSHeap::heap();
  // Since this is being done in a separate thread, need new resource
  // and handle marks.
  ResourceMark rm;
  HandleMark hm;

  ParScanThreadState& par_scan_state = _state_set->thread_state(worker_id);
  assert(_state_set->is_valid(worker_id), "Should not have been called");

  par_scan_state.set_young_old_boundary(_young_old_boundary);

  CLDScanClosure cld_scan_closure(&par_scan_state.to_space_root_closure(),
                                  heap->rem_set()->cld_rem_set()->accumulate_modified_oops());

  par_scan_state.start_strong_roots();
  heap->young_process_roots(_strong_roots_scope,
                           &par_scan_state.to_space_root_closure(),
                           &par_scan_state.older_gen_closure(),
                           &cld_scan_closure,
                           &_par_state_string);

  par_scan_state.end_strong_roots();

  // "evacuate followers".
  par_scan_state.evacuate_followers_closure().do_void();

  // This will collapse this worker's promoted object list that's
  // created during the main ParNew parallel phase of ParNew. This has
  // to be called after all workers have finished promoting objects
  // and scanning promoted objects. It should be safe calling it from
  // here, given that we can only reach here after all thread have
  // offered termination, i.e., after there is no more work to be
  // done. It will also disable promotion tracking for the rest of
  // this GC as it's not necessary to be on during reference processing.
  _old_gen->par_oop_since_save_marks_iterate_done((int) worker_id);
}

ParNewGeneration::ParNewGeneration(ReservedSpace rs, size_t initial_byte_size)
  : DefNewGeneration(rs, initial_byte_size, "PCopy"),
  _overflow_list(NULL),
  _is_alive_closure(this),
  _plab_stats("Young", YoungPLABSize, PLABWeight)
{
  NOT_PRODUCT(_overflow_counter = ParGCWorkQueueOverflowInterval;)
  NOT_PRODUCT(_num_par_pushes = 0;)
  _task_queues = new ObjToScanQueueSet(ParallelGCThreads);
  guarantee(_task_queues != NULL, "task_queues allocation failure.");

  for (uint i = 0; i < ParallelGCThreads; i++) {
    ObjToScanQueue *q = new ObjToScanQueue();
    guarantee(q != NULL, "work_queue Allocation failure.");
    _task_queues->register_queue(i, q);
  }

  for (uint i = 0; i < ParallelGCThreads; i++) {
    _task_queues->queue(i)->initialize();
  }

  _overflow_stacks = NULL;
  if (ParGCUseLocalOverflow) {
    // typedef to workaround NEW_C_HEAP_ARRAY macro, which can not deal with ','
    typedef Stack<oop, mtGC> GCOopStack;

    _overflow_stacks = NEW_C_HEAP_ARRAY(GCOopStack, ParallelGCThreads, mtGC);
    for (size_t i = 0; i < ParallelGCThreads; ++i) {
      new (_overflow_stacks + i) Stack<oop, mtGC>();
    }
  }

  if (UsePerfData) {
    EXCEPTION_MARK;
    ResourceMark rm;

    const char* cname =
         PerfDataManager::counter_name(_gen_counters->name_space(), "threads");
    PerfDataManager::create_constant(SUN_GC, cname, PerfData::U_None,
                                     ParallelGCThreads, CHECK);
  }
}

// ParNewGeneration::
ParKeepAliveClosure::ParKeepAliveClosure(ParScanWeakRefClosure* cl) :
  DefNewGeneration::KeepAliveClosure(cl), _par_cl(cl) {}

template <class T>
void /*ParNewGeneration::*/ParKeepAliveClosure::do_oop_work(T* p) {
#ifdef ASSERT
  {
    oop obj = RawAccess<IS_NOT_NULL>::oop_load(p);
    // We never expect to see a null reference being processed
    // as a weak reference.
    assert(oopDesc::is_oop(obj), "expected an oop while scanning weak refs");
  }
#endif // ASSERT

  Devirtualizer::do_oop_no_verify(_par_cl, p);

  if (CMSHeap::heap()->is_in_reserved(p)) {
    oop obj = RawAccess<IS_NOT_NULL>::oop_load(p);;
    _rs->write_ref_field_gc_par(p, obj);
  }
}

void /*ParNewGeneration::*/ParKeepAliveClosure::do_oop(oop* p)       { ParKeepAliveClosure::do_oop_work(p); }
void /*ParNewGeneration::*/ParKeepAliveClosure::do_oop(narrowOop* p) { ParKeepAliveClosure::do_oop_work(p); }

// ParNewGeneration::
KeepAliveClosure::KeepAliveClosure(ScanWeakRefClosure* cl) :
  DefNewGeneration::KeepAliveClosure(cl) {}

template <class T>
void /*ParNewGeneration::*/KeepAliveClosure::do_oop_work(T* p) {
#ifdef ASSERT
  {
    oop obj = RawAccess<IS_NOT_NULL>::oop_load(p);
    // We never expect to see a null reference being processed
    // as a weak reference.
    assert(oopDesc::is_oop(obj), "expected an oop while scanning weak refs");
  }
#endif // ASSERT

  Devirtualizer::do_oop_no_verify(_cl, p);

  if (CMSHeap::heap()->is_in_reserved(p)) {
    oop obj = RawAccess<IS_NOT_NULL>::oop_load(p);
    _rs->write_ref_field_gc_par(p, obj);
  }
}

void /*ParNewGeneration::*/KeepAliveClosure::do_oop(oop* p)       { KeepAliveClosure::do_oop_work(p); }
void /*ParNewGeneration::*/KeepAliveClosure::do_oop(narrowOop* p) { KeepAliveClosure::do_oop_work(p); }

template <class T> void ScanClosureWithParBarrier::do_oop_work(T* p) {
  T heap_oop = RawAccess<>::oop_load(p);
  if (!CompressedOops::is_null(heap_oop)) {
    oop obj = CompressedOops::decode_not_null(heap_oop);
    if ((HeapWord*)obj < _boundary) {
      assert(!_g->to()->is_in_reserved(obj), "Scanning field twice?");
      oop new_obj = obj->is_forwarded()
                      ? obj->forwardee()
                      : _g->DefNewGeneration::copy_to_survivor_space(obj);
      RawAccess<IS_NOT_NULL>::oop_store(p, new_obj);
    }
    if (_gc_barrier) {
      // If p points to a younger generation, mark the card.
      if ((HeapWord*)obj < _gen_boundary) {
        _rs->write_ref_field_gc_par(p, obj);
      }
    }
  }
}

void ScanClosureWithParBarrier::do_oop(oop* p)       { ScanClosureWithParBarrier::do_oop_work(p); }
void ScanClosureWithParBarrier::do_oop(narrowOop* p) { ScanClosureWithParBarrier::do_oop_work(p); }

class ParNewRefProcTaskProxy: public AbstractGangTask {
  typedef AbstractRefProcTaskExecutor::ProcessTask ProcessTask;
public:
  ParNewRefProcTaskProxy(ProcessTask& task,
                         ParNewGeneration& young_gen,
                         Generation& old_gen,
                         HeapWord* young_old_boundary,
                         ParScanThreadStateSet& state_set);

private:
  virtual void work(uint worker_id);
private:
  ParNewGeneration&      _young_gen;
  ProcessTask&           _task;
  Generation&            _old_gen;
  HeapWord*              _young_old_boundary;
  ParScanThreadStateSet& _state_set;
};

ParNewRefProcTaskProxy::ParNewRefProcTaskProxy(ProcessTask& task,
                                               ParNewGeneration& young_gen,
                                               Generation& old_gen,
                                               HeapWord* young_old_boundary,
                                               ParScanThreadStateSet& state_set)
  : AbstractGangTask("ParNewGeneration parallel reference processing"),
    _young_gen(young_gen),
    _task(task),
    _old_gen(old_gen),
    _young_old_boundary(young_old_boundary),
    _state_set(state_set)
{ }

void ParNewRefProcTaskProxy::work(uint worker_id) {
  ResourceMark rm;
  HandleMark hm;
  ParScanThreadState& par_scan_state = _state_set.thread_state(worker_id);
  par_scan_state.set_young_old_boundary(_young_old_boundary);
  BarrierEnqueueDiscoveredFieldClosure enqueue;
  _task.work(worker_id, par_scan_state.is_alive_closure(),
             par_scan_state.keep_alive_closure(),
             enqueue,
             par_scan_state.evacuate_followers_closure());
}

void ParNewRefProcTaskExecutor::execute(ProcessTask& task, uint ergo_workers) {
  CMSHeap* gch = CMSHeap::heap();
  WorkGang* workers = gch->workers();
  assert(workers != NULL, "Need parallel worker threads.");
  assert(workers->active_workers() == ergo_workers,
         "Ergonomically chosen workers (%u) must be equal to active workers (%u)",
         ergo_workers, workers->active_workers());
  _state_set.reset(workers->active_workers(), _young_gen.promotion_failed());
  ParNewRefProcTaskProxy rp_task(task, _young_gen, _old_gen,
                                 _young_gen.reserved().end(), _state_set);
  workers->run_task(&rp_task, workers->active_workers());
  _state_set.reset(0 /* bad value in debug if not reset */,
                   _young_gen.promotion_failed());
}

void ParNewRefProcTaskExecutor::set_single_threaded_mode() {
  _state_set.flush();
  CMSHeap* heap = CMSHeap::heap();
  heap->save_marks();
}

ScanClosureWithParBarrier::
ScanClosureWithParBarrier(ParNewGeneration* g, bool gc_barrier) :
  OopsInClassLoaderDataOrGenClosure(g), _g(g), _boundary(g->reserved().end()), _gc_barrier(gc_barrier)
{ }

template <typename OopClosureType1, typename OopClosureType2>
EvacuateFollowersClosureGeneral<OopClosureType1, OopClosureType2>::
EvacuateFollowersClosureGeneral(CMSHeap* heap,
                                OopClosureType1* cur,
                                OopClosureType2* older) :
  _heap(heap),
  _scan_cur_or_nonheap(cur), _scan_older(older)
{ }

template <typename OopClosureType1, typename OopClosureType2>
void EvacuateFollowersClosureGeneral<OopClosureType1, OopClosureType2>::do_void() {
  do {
    _heap->oop_since_save_marks_iterate(_scan_cur_or_nonheap,
                                        _scan_older);
  } while (!_heap->no_allocs_since_save_marks());
}

// A Generation that does parallel young-gen collection.

void ParNewGeneration::handle_promotion_failed(CMSHeap* gch, ParScanThreadStateSet& thread_state_set) {
  assert(_promo_failure_scan_stack.is_empty(), "post condition");
  _promo_failure_scan_stack.clear(true); // Clear cached segments.

  remove_forwarding_pointers();
  log_info(gc, promotion)("Promotion failed");
  // All the spaces are in play for mark-sweep.
  swap_spaces();  // Make life simpler for CMS || rescan; see 6483690.
  from()->set_next_compaction_space(to());
  gch->set_incremental_collection_failed();
  // Inform the next generation that a promotion failure occurred.
  _old_gen->promotion_failure_occurred();

  // Trace promotion failure in the parallel GC threads
  thread_state_set.trace_promotion_failed(gc_tracer());
  // Single threaded code may have reported promotion failure to the global state
  if (_promotion_failed_info.has_failed()) {
    _gc_tracer.report_promotion_failed(_promotion_failed_info);
  }
  // Reset the PromotionFailureALot counters.
  NOT_PRODUCT(gch->reset_promotion_should_fail();)
}

void ParNewGeneration::collect(bool   full,
                               bool   clear_all_soft_refs,
                               size_t size,
                               bool   is_tlab) {
  assert(full || size > 0, "otherwise we don't want to collect");

  CMSHeap* gch = CMSHeap::heap();

  _gc_timer->register_gc_start();

  AdaptiveSizePolicy* size_policy = gch->size_policy();
  WorkGang* workers = gch->workers();
  assert(workers != NULL, "Need workgang for parallel work");
  uint active_workers =
       AdaptiveSizePolicy::calc_active_workers(workers->total_workers(),
                                               workers->active_workers(),
                                               Threads::number_of_non_daemon_threads());
  active_workers = workers->update_active_workers(active_workers);
  log_info(gc,task)("Using %u workers of %u for evacuation", active_workers, workers->total_workers());

  _old_gen = gch->old_gen();

  // If the next generation is too full to accommodate worst-case promotion
  // from this generation, pass on collection; let the next generation
  // do it.
  if (!collection_attempt_is_safe()) {
    gch->set_incremental_collection_failed();  // slight lie, in that we did not even attempt one
    return;
  }
  assert(to()->is_empty(), "Else not collection_attempt_is_safe");

  _gc_tracer.report_gc_start(gch->gc_cause(), _gc_timer->gc_start());
  gch->trace_heap_before_gc(gc_tracer());

  init_assuming_no_promotion_failure();

  if (UseAdaptiveSizePolicy) {
    set_survivor_overflow(false);
    size_policy->minor_collection_begin();
  }

  GCTraceTime(Trace, gc, phases) t1("ParNew", NULL, gch->gc_cause());

  age_table()->clear();
  to()->clear(SpaceDecorator::Mangle);

  gch->save_marks();

  // Set the correct parallelism (number of queues) in the reference processor
  ref_processor()->set_active_mt_degree(active_workers);

  // Need to initialize the preserved marks before the ThreadStateSet c'tor.
  _preserved_marks_set.init(active_workers);

  // Always set the terminator for the active number of workers
  // because only those workers go through the termination protocol.
  TaskTerminator _term(active_workers, task_queues());
  ParScanThreadStateSet thread_state_set(active_workers,
                                         *to(), *this, *_old_gen, *task_queues(),
                                         _overflow_stacks, _preserved_marks_set,
                                         desired_plab_sz(), _term);

  thread_state_set.reset(active_workers, promotion_failed());

  {
    StrongRootsScope srs(active_workers);

    ParNewGenTask tsk(this, _old_gen, reserved().end(), &thread_state_set, &srs);
    gch->rem_set()->prepare_for_younger_refs_iterate(true);
    // It turns out that even when we're using 1 thread, doing the work in a
    // separate thread causes wide variance in run times.  We can't help this
    // in the multi-threaded case, but we special-case n=1 here to get
    // repeatable measurements of the 1-thread overhead of the parallel code.
    // Might multiple workers ever be used?  If yes, initialization
    // has been done such that the single threaded path should not be used.
    if (workers->total_workers() > 1) {
      workers->run_task(&tsk);
    } else {
      tsk.work(0);
    }
  }

  thread_state_set.reset(0 /* Bad value in debug if not reset */,
                         promotion_failed());

  // Trace and reset failed promotion info.
  if (promotion_failed()) {
    thread_state_set.trace_promotion_failed(gc_tracer());
  }

  // Process (weak) reference objects found during scavenge.
  ReferenceProcessor* rp = ref_processor();
  IsAliveClosure is_alive(this);
  ScanWeakRefClosure scan_weak_ref(this);
  KeepAliveClosure keep_alive(&scan_weak_ref);
  BarrierEnqueueDiscoveredFieldClosure enqueue;
  ScanClosure               scan_without_gc_barrier(this, false);
  ScanClosureWithParBarrier scan_with_gc_barrier(this, true);
  set_promo_failure_scan_stack_closure(&scan_without_gc_barrier);
  EvacuateFollowersClosureGeneral<ScanClosure, ScanClosureWithParBarrier> evacuate_followers(
      gch, &scan_without_gc_barrier, &scan_with_gc_barrier);
  rp->setup_policy(clear_all_soft_refs);
  // Can  the mt_degree be set later (at run_task() time would be best)?
  rp->set_active_mt_degree(active_workers);
  ReferenceProcessorStats stats;
  ReferenceProcessorPhaseTimes pt(_gc_timer, rp->max_num_queues());
  if (rp->processing_is_mt()) {
    ParNewRefProcTaskExecutor task_executor(*this, *_old_gen, thread_state_set);
    stats = rp->process_discovered_references(&is_alive, &keep_alive, &enqueue,
                                              &evacuate_followers, &task_executor,
                                              &pt);
  } else {
    thread_state_set.flush();
    gch->save_marks();
    stats = rp->process_discovered_references(&is_alive, &keep_alive, &enqueue,
                                              &evacuate_followers, NULL,
                                              &pt);
  }
  _gc_tracer.report_gc_reference_stats(stats);
  _gc_tracer.report_tenuring_threshold(tenuring_threshold());
  pt.print_all_references();

  assert(gch->no_allocs_since_save_marks(), "evacuation should be done at this point");

  WeakProcessor::weak_oops_do(&is_alive, &keep_alive);

  // Verify that the usage of keep_alive only forwarded
  // the oops and did not find anything new to copy.
  assert(gch->no_allocs_since_save_marks(), "unexpectedly copied objects");

  if (!promotion_failed()) {
    // Swap the survivor spaces.
    eden()->clear(SpaceDecorator::Mangle);
    from()->clear(SpaceDecorator::Mangle);
    if (ZapUnusedHeapArea) {
      // This is now done here because of the piece-meal mangling which
      // can check for valid mangling at intermediate points in the
      // collection(s).  When a young collection fails to collect
      // sufficient space resizing of the young generation can occur
      // and redistribute the spaces in the young generation.  Mangle
      // here so that unzapped regions don't get distributed to
      // other spaces.
      to()->mangle_unused_area();
    }
    swap_spaces();

    // A successful scavenge should restart the GC time limit count which is
    // for full GC's.
    size_policy->reset_gc_overhead_limit_count();

    assert(to()->is_empty(), "to space should be empty now");

    adjust_desired_tenuring_threshold();
  } else {
    handle_promotion_failed(gch, thread_state_set);
  }
  _preserved_marks_set.reclaim();
  // set new iteration safe limit for the survivor spaces
  from()->set_concurrent_iteration_safe_limit(from()->top());
  to()->set_concurrent_iteration_safe_limit(to()->top());

  plab_stats()->adjust_desired_plab_sz();

  TASKQUEUE_STATS_ONLY(thread_state_set.print_termination_stats());
  TASKQUEUE_STATS_ONLY(thread_state_set.print_taskqueue_stats());

  if (UseAdaptiveSizePolicy) {
    size_policy->minor_collection_end(gch->gc_cause());
    size_policy->avg_survived()->sample(from()->used());
  }

  // We need to use a monotonically non-decreasing time in ms
  // or we will see time-warp warnings and os::javaTimeMillis()
  // does not guarantee monotonicity.
  jlong now = os::javaTimeNanos() / NANOSECS_PER_MILLISEC;
  update_time_of_last_gc(now);

  rp->set_enqueuing_is_done(true);
  rp->verify_no_references_recorded();

  gch->trace_heap_after_gc(gc_tracer());

  _gc_timer->register_gc_end();

  _gc_tracer.report_gc_end(_gc_timer->gc_end(), _gc_timer->time_partitions());
}

size_t ParNewGeneration::desired_plab_sz() {
  return _plab_stats.desired_plab_sz(CMSHeap::heap()->workers()->active_workers());
}

static int sum;
void ParNewGeneration::waste_some_time() {
  for (int i = 0; i < 100; i++) {
    sum += i;
  }
}

static const oop ClaimedForwardPtr = cast_to_oop<intptr_t>(0x4);

// Because of concurrency, there are times where an object for which
// "is_forwarded()" is true contains an "interim" forwarding pointer
// value.  Such a value will soon be overwritten with a real value.
// This method requires "obj" to have a forwarding pointer, and waits, if
// necessary for a real one to be inserted, and returns it.

oop ParNewGeneration::real_forwardee(oop obj) {
  oop forward_ptr = obj->forwardee();
  if (forward_ptr != ClaimedForwardPtr) {
    return forward_ptr;
  } else {
    return real_forwardee_slow(obj);
  }
}

oop ParNewGeneration::real_forwardee_slow(oop obj) {
  // Spin-read if it is claimed but not yet written by another thread.
  oop forward_ptr = obj->forwardee();
  while (forward_ptr == ClaimedForwardPtr) {
    waste_some_time();
    assert(obj->is_forwarded(), "precondition");
    forward_ptr = obj->forwardee();
  }
  return forward_ptr;
}

// Multiple GC threads may try to promote an object.  If the object
// is successfully promoted, a forwarding pointer will be installed in
// the object in the young generation.  This method claims the right
// to install the forwarding pointer before it copies the object,
// thus avoiding the need to undo the copy as in
// copy_to_survivor_space_avoiding_with_undo.

oop ParNewGeneration::copy_to_survivor_space(ParScanThreadState* par_scan_state,
                                             oop old,
                                             size_t sz,
                                             markOop m) {
  // In the sequential version, this assert also says that the object is
  // not forwarded.  That might not be the case here.  It is the case that
  // the caller observed it to be not forwarded at some time in the past.
  assert(is_in_reserved(old), "shouldn't be scavenging this oop");

  // The sequential code read "old->age()" below.  That doesn't work here,
  // since the age is in the mark word, and that might be overwritten with
  // a forwarding pointer by a parallel thread.  So we must save the mark
  // word in a local and then analyze it.
  oopDesc dummyOld;
  dummyOld.set_mark_raw(m);
  assert(!dummyOld.is_forwarded(),
         "should not be called with forwarding pointer mark word.");

  oop new_obj = NULL;
  oop forward_ptr;

  // Try allocating obj in to-space (unless too old)
  if (dummyOld.age() < tenuring_threshold()) {
    new_obj = (oop)par_scan_state->alloc_in_to_space(sz);
    if (new_obj == NULL) {
      set_survivor_overflow(true);
    }
  }

  if (new_obj == NULL) {
    // Either to-space is full or we decided to promote try allocating obj tenured

    // Attempt to install a null forwarding pointer (atomically),
    // to claim the right to install the real forwarding pointer.
    forward_ptr = old->forward_to_atomic(ClaimedForwardPtr);
    if (forward_ptr != NULL) {
      // someone else beat us to it.
        return real_forwardee(old);
    }

    if (!_promotion_failed) {
      new_obj = _old_gen->par_promote(par_scan_state->thread_num(),
                                      old, m, sz);
    }

    if (new_obj == NULL) {
      // promotion failed, forward to self
      _promotion_failed = true;
      new_obj = old;

      par_scan_state->preserved_marks()->push_if_necessary(old, m);
      par_scan_state->register_promotion_failure(sz);
    }

    old->forward_to(new_obj);
    forward_ptr = NULL;
  } else {
    // Is in to-space; do copying ourselves.
    Copy::aligned_disjoint_words((HeapWord*)old, (HeapWord*)new_obj, sz);
    assert(CMSHeap::heap()->is_in_reserved(new_obj), "illegal forwarding pointer value.");
    forward_ptr = old->forward_to_atomic(new_obj);
    // Restore the mark word copied above.
    new_obj->set_mark_raw(m);
    // Increment age if obj still in new generation
    new_obj->incr_age();
    par_scan_state->age_table()->add(new_obj, sz);
  }
  assert(new_obj != NULL, "just checking");

  // This code must come after the CAS test, or it will print incorrect
  // information.
  log_develop_trace(gc, scavenge)("{%s %s " PTR_FORMAT " -> " PTR_FORMAT " (%d)}",
                                  is_in_reserved(new_obj) ? "copying" : "tenuring",
                                  new_obj->klass()->internal_name(), p2i(old), p2i(new_obj), new_obj->size());

  if (forward_ptr == NULL) {
    oop obj_to_push = new_obj;
    if (par_scan_state->should_be_partially_scanned(obj_to_push, old)) {
      // Length field used as index of next element to be scanned.
      // Real length can be obtained from real_forwardee()
      arrayOop(old)->set_length(0);
      obj_to_push = old;
      assert(obj_to_push->is_forwarded() && obj_to_push->forwardee() != obj_to_push,
             "push forwarded object");
    }
    // Push it on one of the queues of to-be-scanned objects.
    bool simulate_overflow = false;
    NOT_PRODUCT(
      if (ParGCWorkQueueOverflowALot && should_simulate_overflow()) {
        // simulate a stack overflow
        simulate_overflow = true;
      }
    )
    if (simulate_overflow || !par_scan_state->work_queue()->push(obj_to_push)) {
      // Add stats for overflow pushes.
      log_develop_trace(gc)("Queue Overflow");
      push_on_overflow_list(old, par_scan_state);
      TASKQUEUE_STATS_ONLY(par_scan_state->taskqueue_stats().record_overflow(0));
    }

    return new_obj;
  }

  // Oops.  Someone beat us to it.  Undo the allocation.  Where did we
  // allocate it?
  if (is_in_reserved(new_obj)) {
    // Must be in to_space.
    assert(to()->is_in_reserved(new_obj), "Checking");
    if (forward_ptr == ClaimedForwardPtr) {
      // Wait to get the real forwarding pointer value.
      forward_ptr = real_forwardee(old);
    }
    par_scan_state->undo_alloc_in_to_space((HeapWord*)new_obj, sz);
  }

  return forward_ptr;
}

#ifndef PRODUCT
// It's OK to call this multi-threaded;  the worst thing
// that can happen is that we'll get a bunch of closely
// spaced simulated overflows, but that's OK, in fact
// probably good as it would exercise the overflow code
// under contention.
bool ParNewGeneration::should_simulate_overflow() {
  if (_overflow_counter-- <= 0) { // just being defensive
    _overflow_counter = ParGCWorkQueueOverflowInterval;
    return true;
  } else {
    return false;
  }
}
#endif

// In case we are using compressed oops, we need to be careful.
// If the object being pushed is an object array, then its length
// field keeps track of the "grey boundary" at which the next
// incremental scan will be done (see ParGCArrayScanChunk).
// When using compressed oops, this length field is kept in the
// lower 32 bits of the erstwhile klass word and cannot be used
// for the overflow chaining pointer (OCP below). As such the OCP
// would itself need to be compressed into the top 32-bits in this
// case. Unfortunately, see below, in the event that we have a
// promotion failure, the node to be pushed on the list can be
// outside of the Java heap, so the heap-based pointer compression
// would not work (we would have potential aliasing between C-heap
// and Java-heap pointers). For this reason, when using compressed
// oops, we simply use a worker-thread-local, non-shared overflow
// list in the form of a growable array, with a slightly different
// overflow stack draining strategy. If/when we start using fat
// stacks here, we can go back to using (fat) pointer chains
// (although some performance comparisons would be useful since
// single global lists have their own performance disadvantages
// as we were made painfully aware not long ago, see 6786503).
#define BUSY (cast_to_oop<intptr_t>(0x1aff1aff))
void ParNewGeneration::push_on_overflow_list(oop from_space_obj, ParScanThreadState* par_scan_state) {
  assert(is_in_reserved(from_space_obj), "Should be from this generation");
  if (ParGCUseLocalOverflow) {
    // In the case of compressed oops, we use a private, not-shared
    // overflow stack.
    par_scan_state->push_on_overflow_stack(from_space_obj);
  } else {
    assert(!UseCompressedOops, "Error");
    // if the object has been forwarded to itself, then we cannot
    // use the klass pointer for the linked list.  Instead we have
    // to allocate an oopDesc in the C-Heap and use that for the linked list.
    // XXX This is horribly inefficient when a promotion failure occurs
    // and should be fixed. XXX FIX ME !!!
#ifndef PRODUCT
    Atomic::inc(&_num_par_pushes);
    assert(_num_par_pushes > 0, "Tautology");
#endif
    if (from_space_obj->forwardee() == from_space_obj) {
      oopDesc* listhead = NEW_C_HEAP_ARRAY(oopDesc, 1, mtGC);
      listhead->forward_to(from_space_obj);
      from_space_obj = listhead;
    }
    oop observed_overflow_list = _overflow_list;
    oop cur_overflow_list;
    do {
      cur_overflow_list = observed_overflow_list;
      if (cur_overflow_list != BUSY) {
        from_space_obj->set_klass_to_list_ptr(cur_overflow_list);
      } else {
        from_space_obj->set_klass_to_list_ptr(NULL);
      }
      observed_overflow_list =
        Atomic::cmpxchg((oopDesc*)from_space_obj, &_overflow_list, (oopDesc*)cur_overflow_list);
    } while (cur_overflow_list != observed_overflow_list);
  }
}

bool ParNewGeneration::take_from_overflow_list(ParScanThreadState* par_scan_state) {
  bool res;

  if (ParGCUseLocalOverflow) {
    res = par_scan_state->take_from_overflow_stack();
  } else {
    assert(!UseCompressedOops, "Error");
    res = take_from_overflow_list_work(par_scan_state);
  }
  return res;
}


// *NOTE*: The overflow list manipulation code here and
// in CMSCollector:: are very similar in shape,
// except that in the CMS case we thread the objects
// directly into the list via their mark word, and do
// not need to deal with special cases below related
// to chunking of object arrays and promotion failure
// handling.
// CR 6797058 has been filed to attempt consolidation of
// the common code.
// Because of the common code, if you make any changes in
// the code below, please check the CMS version to see if
// similar changes might be needed.
// See CMSCollector::par_take_from_overflow_list() for
// more extensive documentation comments.
bool ParNewGeneration::take_from_overflow_list_work(ParScanThreadState* par_scan_state) {
  ObjToScanQueue* work_q = par_scan_state->work_queue();
  // How many to take?
  size_t objsFromOverflow = MIN2((size_t)(work_q->max_elems() - work_q->size())/4,
                                 (size_t)ParGCDesiredObjsFromOverflowList);

  assert(!UseCompressedOops, "Error");
  assert(par_scan_state->overflow_stack() == NULL, "Error");
  if (_overflow_list == NULL) return false;

  // Otherwise, there was something there; try claiming the list.
  oop prefix = cast_to_oop(Atomic::xchg((oopDesc*)BUSY, &_overflow_list));
  // Trim off a prefix of at most objsFromOverflow items
  Thread* tid = Thread::current();
  size_t spin_count = ParallelGCThreads;
  size_t sleep_time_millis = MAX2((size_t)1, objsFromOverflow/100);
  for (size_t spin = 0; prefix == BUSY && spin < spin_count; spin++) {
    // someone grabbed it before we did ...
    // ... we spin for a short while...
    os::sleep(tid, sleep_time_millis, false);
    if (_overflow_list == NULL) {
      // nothing left to take
      return false;
    } else if (_overflow_list != BUSY) {
     // try and grab the prefix
     prefix = cast_to_oop(Atomic::xchg((oopDesc*)BUSY, &_overflow_list));
    }
  }
  if (prefix == NULL || prefix == BUSY) {
     // Nothing to take or waited long enough
     if (prefix == NULL) {
       // Write back the NULL in case we overwrote it with BUSY above
       // and it is still the same value.
       (void) Atomic::cmpxchg((oopDesc*)NULL, &_overflow_list, (oopDesc*)BUSY);
     }
     return false;
  }
  assert(prefix != NULL && prefix != BUSY, "Error");
  oop cur = prefix;
  for (size_t i = 1; i < objsFromOverflow; ++i) {
    oop next = cur->list_ptr_from_klass();
    if (next == NULL) break;
    cur = next;
  }
  assert(cur != NULL, "Loop postcondition");

  // Reattach remaining (suffix) to overflow list
  oop suffix = cur->list_ptr_from_klass();
  if (suffix == NULL) {
    // Write back the NULL in lieu of the BUSY we wrote
    // above and it is still the same value.
    if (_overflow_list == BUSY) {
      (void) Atomic::cmpxchg((oopDesc*)NULL, &_overflow_list, (oopDesc*)BUSY);
    }
  } else {
    assert(suffix != BUSY, "Error");
    // suffix will be put back on global list
    cur->set_klass_to_list_ptr(NULL);     // break off suffix
    // It's possible that the list is still in the empty(busy) state
    // we left it in a short while ago; in that case we may be
    // able to place back the suffix.
    oop observed_overflow_list = _overflow_list;
    oop cur_overflow_list = observed_overflow_list;
    bool attached = false;
    while (observed_overflow_list == BUSY || observed_overflow_list == NULL) {
      observed_overflow_list =
        Atomic::cmpxchg((oopDesc*)suffix, &_overflow_list, (oopDesc*)cur_overflow_list);
      if (cur_overflow_list == observed_overflow_list) {
        attached = true;
        break;
      } else cur_overflow_list = observed_overflow_list;
    }
    if (!attached) {
      // Too bad, someone else got in in between; we'll need to do a splice.
      // Find the last item of suffix list
      oop last = suffix;
      while (true) {
        oop next = last->list_ptr_from_klass();
        if (next == NULL) break;
        last = next;
      }
      // Atomically prepend suffix to current overflow list
      observed_overflow_list = _overflow_list;
      do {
        cur_overflow_list = observed_overflow_list;
        if (cur_overflow_list != BUSY) {
          // Do the splice ...
          last->set_klass_to_list_ptr(cur_overflow_list);
        } else { // cur_overflow_list == BUSY
          last->set_klass_to_list_ptr(NULL);
        }
        observed_overflow_list =
          Atomic::cmpxchg((oopDesc*)suffix, &_overflow_list, (oopDesc*)cur_overflow_list);
      } while (cur_overflow_list != observed_overflow_list);
    }
  }

  // Push objects on prefix list onto this thread's work queue
  assert(prefix != NULL && prefix != BUSY, "program logic");
  cur = prefix;
  ssize_t n = 0;
  while (cur != NULL) {
    oop obj_to_push = cur->forwardee();
    oop next        = cur->list_ptr_from_klass();
    cur->set_klass(obj_to_push->klass());
    // This may be an array object that is self-forwarded. In that case, the list pointer
    // space, cur, is not in the Java heap, but rather in the C-heap and should be freed.
    if (!is_in_reserved(cur)) {
      // This can become a scaling bottleneck when there is work queue overflow coincident
      // with promotion failure.
      oopDesc* f = cur;
      FREE_C_HEAP_ARRAY(oopDesc, f);
    } else if (par_scan_state->should_be_partially_scanned(obj_to_push, cur)) {
      assert(arrayOop(cur)->length() == 0, "entire array remaining to be scanned");
      obj_to_push = cur;
    }
    bool ok = work_q->push(obj_to_push);
    assert(ok, "Should have succeeded");
    cur = next;
    n++;
  }
  TASKQUEUE_STATS_ONLY(par_scan_state->note_overflow_refill(n));
#ifndef PRODUCT
  assert(_num_par_pushes >= n, "Too many pops?");
  Atomic::sub(n, &_num_par_pushes);
#endif
  return true;
}
#undef BUSY

void ParNewGeneration::ref_processor_init() {
  if (_ref_processor == NULL) {
    // Allocate and initialize a reference processor
    _span_based_discoverer.set_span(_reserved);
    _ref_processor =
      new ReferenceProcessor(&_span_based_discoverer,    // span
                             ParallelRefProcEnabled && (ParallelGCThreads > 1), // mt processing
                             ParallelGCThreads,          // mt processing degree
                             refs_discovery_is_mt(),     // mt discovery
                             ParallelGCThreads,          // mt discovery degree
                             refs_discovery_is_atomic(), // atomic_discovery
                             NULL,                       // is_alive_non_header
                             false);                     // disable adjusting number of processing threads
  }
}

const char* ParNewGeneration::name() const {
  return "par new generation";
}

void ParNewGeneration::restore_preserved_marks() {
  SharedRestorePreservedMarksTaskExecutor task_executor(CMSHeap::heap()->workers());
  _preserved_marks_set.restore(&task_executor);
}
