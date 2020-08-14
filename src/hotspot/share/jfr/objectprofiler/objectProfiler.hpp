/*
 * Copyright (c) 2019 Alibaba Group Holding Limited. All Rights Reserved.
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
#ifndef SHARE_VM_JFR_OBJECTROFILER_OBJECTPROFILER_HPP
#define SHARE_VM_JFR_OBJECTROFILER_OBJECTPROFILER_HPP

#include "jni.h"

#define ARRAY_OBJECT_SIZE_PLACE_HOLDER 0x1111baba

#if INCLUDE_JFR
#define TRACE_OPTO_SLOW_ALLOCATION_ENTER(is_array, thread) \
  AllocTracer::opto_slow_allocation_enter(is_array, thread)

#define TRACE_OPTO_SLOW_ALLOCATION_LEAVE(is_array, thread) \
  AllocTracer::opto_slow_allocation_leave(is_array, thread)

#define TRACE_SLOW_ALLOCATION(klass, obj, alloc_size, thread) \
  AllocTracer::send_slow_allocation_event(klass, obj, alloc_size, thread)

#define TRACE_DEFINE_THREAD_ALLOC_COUNT_OFFSET \
  static ByteSize alloc_count_offset() { return in_ByteSize(offset_of(JfrThreadLocal, _alloc_count)); }
#define TRACE_THREAD_ALLOC_COUNT_OFFSET \
  (JfrThreadLocal::alloc_count_offset() + THREAD_LOCAL_OFFSET_JFR)
#define TRACE_DEFINE_THREAD_ALLOC_COUNT_UNTIL_SAMPLE_OFFSET \
  static ByteSize alloc_count_until_sample_offset() { return in_ByteSize(offset_of(JfrThreadLocal, _alloc_count_until_sample)); }
#define TRACE_THREAD_ALLOC_COUNT_UNTIL_SAMPLE_OFFSET \
  (JfrThreadLocal::alloc_count_until_sample_offset() + THREAD_LOCAL_OFFSET_JFR)

#else
#define TRACE_OPTO_SLOW_ALLOCATION_ENTER(is_array, thread)
#define TRACE_OPTO_SLOW_ALLOCATION_LEAVE(is_array, thread)
#define TRACE_SLOW_ALLOCATION(klass, obj, alloc_size, thread)
#define TRACE_DEFINE_THREAD_ALLOC_COUNT_UNTIL_SAMPLE_OFFSET
#define TRACE_THREAD_ALLOC_COUNT_UNTIL_SAMPLE_OFFSET
#define TRACE_DEFINE_THREAD_ALLOC_COUNT_OFFSET
#define TRACE_THREAD_ALLOC_COUNT_OFFSET
#endif

class ObjectProfiler : public AllStatic {
 private:
  static volatile int _enabled;
  static bool _sample_instance_obj_alloc;
  static bool _sample_array_obj_alloc;
#ifndef PRODUCT
  static volatile int _try_lock;
#endif

 public:
  static void start(jlong event_id);
  static void stop(jlong event_id);
  static int enabled();
  static void* enabled_flag_address();
};

#endif // SHARE_VM_JFR_OBJECTROFILER_OBJECTPROFILER_HPP
