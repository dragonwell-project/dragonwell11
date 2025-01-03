/*
 * Copyright (c) 2015, 2019, Oracle and/or its affiliates. All rights reserved.
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
#include "gc/shared/collectedHeap.hpp"
#include "gc/shared/collectorPolicy.hpp"
#include "gc/shared/gcConfig.hpp"
#include "gc/shared/jvmFlagConstraintsGC.hpp"
#include "gc/shared/plab.hpp"
#include "gc/shared/threadLocalAllocBuffer.hpp"
#include "runtime/arguments.hpp"
#include "runtime/globals.hpp"
#include "runtime/globals_extension.hpp"
#include "runtime/thread.inline.hpp"
#include "utilities/align.hpp"
#include "utilities/macros.hpp"
#if INCLUDE_CMSGC
#include "gc/cms/jvmFlagConstraintsCMS.hpp"
#endif
#if INCLUDE_G1GC
#include "gc/g1/jvmFlagConstraintsG1.hpp"
#endif
#if INCLUDE_PARALLELGC
#include "gc/parallel/jvmFlagConstraintsParallel.hpp"
#endif
#ifdef COMPILER1
#include "c1/c1_globals.hpp"
#endif // COMPILER1
#ifdef COMPILER2
#include "opto/c2_globals.hpp"
#endif // COMPILER2

// Some flags that have default values that indicate that the
// JVM should automatically determine an appropriate value
// for that flag.  In those cases it is only appropriate for the
// constraint checking to be done if the user has specified the
// value(s) of the flag(s) on the command line.  In the constraint
// checking functions,  FLAG_IS_CMDLINE() is used to check if
// the flag has been set by the user and so should be checked.

// As ParallelGCThreads differs among GC modes, we need constraint function.
JVMFlag::Error ParallelGCThreadsConstraintFunc(uint value, bool verbose) {
  JVMFlag::Error status = JVMFlag::SUCCESS;

#if INCLUDE_PARALLELGC
  status = ParallelGCThreadsConstraintFuncParallel(value, verbose);
  if (status != JVMFlag::SUCCESS) {
    return status;
  }
#endif

#if INCLUDE_CMSGC
  status = ParallelGCThreadsConstraintFuncCMS(value, verbose);
  if (status != JVMFlag::SUCCESS) {
    return status;
  }
#endif

  return status;
}

// As ConcGCThreads should be smaller than ParallelGCThreads,
// we need constraint function.
JVMFlag::Error ConcGCThreadsConstraintFunc(uint value, bool verbose) {
  // CMS and G1 GCs use ConcGCThreads.
  if ((GCConfig::is_gc_selected(CollectedHeap::CMS) ||
       GCConfig::is_gc_selected(CollectedHeap::G1)) && (value > ParallelGCThreads)) {
        if (VerifyFlagConstraints) {
          ConcGCThreads = ParallelGCThreads;
          JVMFlag::printError(true, "ConcGCThreads"UINT32_FORMAT"\n", ConcGCThreads);
          return JVMFlag::SUCCESS;
        }
    JVMFlag::printError(verbose,
                        "ConcGCThreads (" UINT32_FORMAT ") must be "
                        "less than or equal to ParallelGCThreads (" UINT32_FORMAT ")\n",
                        value, ParallelGCThreads);
    return JVMFlag::VIOLATES_CONSTRAINT;
  }

  return JVMFlag::SUCCESS;
}

static JVMFlag::Error MinPLABSizeBounds(const char* name, size_t* value_addr, size_t value, bool verbose) {
  if ((GCConfig::is_gc_selected(CollectedHeap::CMS) ||
       GCConfig::is_gc_selected(CollectedHeap::G1)  ||
       GCConfig::is_gc_selected(CollectedHeap::Parallel)) && (value < PLAB::min_size())) {
    if (VerifyFlagConstraints) {
      *value_addr = PLAB::min_size();
      JVMFlag::printError(true, "%s:"SIZE_FORMAT"\n", name, PLAB::min_size());
      return JVMFlag::SUCCESS;
    }
    JVMFlag::printError(verbose,
                        "%s (" SIZE_FORMAT ") must be "
                        "greater than or equal to ergonomic PLAB minimum size (" SIZE_FORMAT ")\n",
                        name, value, PLAB::min_size());
    return JVMFlag::VIOLATES_CONSTRAINT;
  }

  return JVMFlag::SUCCESS;
}

JVMFlag::Error MaxPLABSizeBounds(const char* name, size_t* value_addr, size_t value, bool verbose) {
  if ((GCConfig::is_gc_selected(CollectedHeap::CMS) ||
       GCConfig::is_gc_selected(CollectedHeap::G1)  ||
       GCConfig::is_gc_selected(CollectedHeap::Parallel)) && (value > PLAB::max_size())) {
    if (VerifyFlagConstraints) {
      *value_addr = PLAB::max_size();
      JVMFlag::printError(true, "%s:"SIZE_FORMAT"\n", name, PLAB::max_size());
      return JVMFlag::SUCCESS;
    }
    JVMFlag::printError(verbose,
                        "%s (" SIZE_FORMAT ") must be "
                        "less than or equal to ergonomic PLAB maximum size (" SIZE_FORMAT ")\n",
                        name, value, PLAB::max_size());
    return JVMFlag::VIOLATES_CONSTRAINT;
  }

  return JVMFlag::SUCCESS;
}

static JVMFlag::Error MinMaxPLABSizeBounds(const char* name, size_t *value_addr, size_t value, bool verbose) {
  JVMFlag::Error status = MinPLABSizeBounds(name, value_addr, value, verbose);

  if (status == JVMFlag::SUCCESS) {
    return MaxPLABSizeBounds(name, value_addr, value, verbose);
  }
  return status;
}

JVMFlag::Error YoungPLABSizeConstraintFunc(size_t value, bool verbose) {
  return MinMaxPLABSizeBounds("YoungPLABSize", &YoungPLABSize, value, verbose);
}

JVMFlag::Error OldPLABSizeConstraintFunc(size_t value, bool verbose) {
  JVMFlag::Error status = JVMFlag::SUCCESS;

#if INCLUDE_CMSGC
  if (UseConcMarkSweepGC) {
    return OldPLABSizeConstraintFuncCMS(value, verbose);
  } else
#endif
  {
    status = MinMaxPLABSizeBounds("OldPLABSize", &OldPLABSize, value, verbose);
  }

  return status;
}

JVMFlag::Error MinHeapFreeRatioConstraintFunc(uintx value, bool verbose) {
  if (value > MaxHeapFreeRatio) {
    if (VerifyFlagConstraints) {
      MinHeapFreeRatio = MaxHeapFreeRatio;
      JVMFlag::printError(true, "MinHeapFreeRatio:"UINTX_FORMAT"\n", MinHeapFreeRatio);
      return JVMFlag::SUCCESS;
    }
    JVMFlag::printError(verbose,
                        "MinHeapFreeRatio (" UINTX_FORMAT ") must be "
                        "less than or equal to MaxHeapFreeRatio (" UINTX_FORMAT ")\n",
                        value, MaxHeapFreeRatio);
    return JVMFlag::VIOLATES_CONSTRAINT;
  } else {
    return JVMFlag::SUCCESS;
  }
}

JVMFlag::Error MaxHeapFreeRatioConstraintFunc(uintx value, bool verbose) {
  if (value < MinHeapFreeRatio) {
    if (VerifyFlagConstraints) {
      MaxHeapFreeRatio = MinHeapFreeRatio;
      JVMFlag::printError(true, "MaxHeapFreeRatio:"UINTX_FORMAT"\n", MaxHeapFreeRatio);
      return JVMFlag::SUCCESS;
    }
    JVMFlag::printError(verbose,
                        "MaxHeapFreeRatio (" UINTX_FORMAT ") must be "
                        "greater than or equal to MinHeapFreeRatio (" UINTX_FORMAT ")\n",
                        value, MinHeapFreeRatio);
    return JVMFlag::VIOLATES_CONSTRAINT;
  } else {
    return JVMFlag::SUCCESS;
  }
}

static JVMFlag::Error CheckMaxHeapSizeAndSoftRefLRUPolicyMSPerMB(size_t maxHeap, intx softRef, bool verbose, bool from_max_heap_size) {
  if ((softRef > 0) && ((maxHeap / M) > (max_uintx / softRef))) {
    if (VerifyFlagConstraints) {
      if (from_max_heap_size) {
        MaxHeapSize = max_uintx / softRef *M;
        JVMFlag::printError(true, "MaxHeapSize:"SIZE_FORMAT"\n", MaxHeapSize);
      } else {
        SoftRefLRUPolicyMSPerMB = max_uintx/(maxHeap / M);
        JVMFlag::printError(true, "SoftRefLRUPolicyMSPerMB:"INTX_FORMAT"\n", SoftRefLRUPolicyMSPerMB);
      }
      return JVMFlag::SUCCESS;
    }
    JVMFlag::printError(verbose,
                        "Desired lifetime of SoftReferences cannot be expressed correctly. "
                        "MaxHeapSize (" SIZE_FORMAT ") or SoftRefLRUPolicyMSPerMB "
                        "(" INTX_FORMAT ") is too large\n",
                        maxHeap, softRef);
    return JVMFlag::VIOLATES_CONSTRAINT;
  } else {
    return JVMFlag::SUCCESS;
  }
}

JVMFlag::Error SoftRefLRUPolicyMSPerMBConstraintFunc(intx value, bool verbose) {
  return CheckMaxHeapSizeAndSoftRefLRUPolicyMSPerMB(MaxHeapSize, value, verbose, false);
}

JVMFlag::Error MarkStackSizeConstraintFunc(size_t value, bool verbose) {
  if (value > MarkStackSizeMax) {
    if (VerifyFlagConstraints) {
      MarkStackSize = MarkStackSizeMax;
      JVMFlag::printError(true, "MarkStackSize:"SIZE_FORMAT"\n", MarkStackSize);
      return JVMFlag::SUCCESS;
    }
    JVMFlag::printError(verbose,
                        "MarkStackSize (" SIZE_FORMAT ") must be "
                        "less than or equal to MarkStackSizeMax (" SIZE_FORMAT ")\n",
                        value, MarkStackSizeMax);
    return JVMFlag::VIOLATES_CONSTRAINT;
  } else {
    return JVMFlag::SUCCESS;
  }
}

JVMFlag::Error MinMetaspaceFreeRatioConstraintFunc(uintx value, bool verbose) {
  if (value > MaxMetaspaceFreeRatio) {
    if (VerifyFlagConstraints) {
      MinMetaspaceFreeRatio = MaxMetaspaceFreeRatio;
      JVMFlag::printError(true, "MinMetaspaceFreeRatio:"UINTX_FORMAT"\n", MinMetaspaceFreeRatio);
      return JVMFlag::SUCCESS;
    }
    JVMFlag::printError(verbose,
                        "MinMetaspaceFreeRatio (" UINTX_FORMAT ") must be "
                        "less than or equal to MaxMetaspaceFreeRatio (" UINTX_FORMAT ")\n",
                        value, MaxMetaspaceFreeRatio);
    return JVMFlag::VIOLATES_CONSTRAINT;
  } else {
    return JVMFlag::SUCCESS;
  }
}

JVMFlag::Error MaxMetaspaceFreeRatioConstraintFunc(uintx value, bool verbose) {
  if (value < MinMetaspaceFreeRatio) {
    if (VerifyFlagConstraints) {
      MaxMetaspaceFreeRatio = MinMetaspaceFreeRatio;
      JVMFlag::printError(true, "MaxMetaspaceFreeRatio:"UINTX_FORMAT"\n", MaxMetaspaceFreeRatio);
      return JVMFlag::SUCCESS;
    }
    JVMFlag::printError(verbose,
                        "MaxMetaspaceFreeRatio (" UINTX_FORMAT ") must be "
                        "greater than or equal to MinMetaspaceFreeRatio (" UINTX_FORMAT ")\n",
                        value, MinMetaspaceFreeRatio);
    return JVMFlag::VIOLATES_CONSTRAINT;
  } else {
    return JVMFlag::SUCCESS;
  }
}

JVMFlag::Error InitialTenuringThresholdConstraintFunc(uintx value, bool verbose) {
#if INCLUDE_PARALLELGC
  JVMFlag::Error status = InitialTenuringThresholdConstraintFuncParallel(value, verbose);
  if (status != JVMFlag::SUCCESS) {
    return status;
  }
#endif

  return JVMFlag::SUCCESS;
}

JVMFlag::Error MaxTenuringThresholdConstraintFunc(uintx value, bool verbose) {
#if INCLUDE_PARALLELGC
  JVMFlag::Error status = MaxTenuringThresholdConstraintFuncParallel(value, verbose);
  if (status != JVMFlag::SUCCESS) {
    return status;
  }
#endif

  // MaxTenuringThreshold=0 means NeverTenure=false && AlwaysTenure=true
  if ((value == 0) && (NeverTenure || !AlwaysTenure)) {
    if (VerifyFlagConstraints) {
      MaxTenuringThreshold = 1;
      JVMFlag::printError(true, "MaxTenuringThreshold:1\n");
      return JVMFlag::SUCCESS;
    }
    JVMFlag::printError(verbose,
                        "MaxTenuringThreshold (0) should match to NeverTenure=false "
                        "&& AlwaysTenure=true. But we have NeverTenure=%s "
                        "AlwaysTenure=%s\n",
                        NeverTenure ? "true" : "false",
                        AlwaysTenure ? "true" : "false");
    return JVMFlag::VIOLATES_CONSTRAINT;
  }
  return JVMFlag::SUCCESS;
}

JVMFlag::Error MaxGCPauseMillisConstraintFunc(uintx value, bool verbose) {
#if INCLUDE_G1GC
  JVMFlag::Error status = MaxGCPauseMillisConstraintFuncG1(value, verbose);
  if (status != JVMFlag::SUCCESS) {
    return status;
  }
#endif

  return JVMFlag::SUCCESS;
}

JVMFlag::Error GCPauseIntervalMillisConstraintFunc(uintx value, bool verbose) {
#if INCLUDE_G1GC
  JVMFlag::Error status = GCPauseIntervalMillisConstraintFuncG1(value, verbose);
  if (status != JVMFlag::SUCCESS) {
    return status;
  }
#endif

  return JVMFlag::SUCCESS;
}

JVMFlag::Error InitialBootClassLoaderMetaspaceSizeConstraintFunc(size_t value, bool verbose) {
  size_t aligned_max = align_down(max_uintx/2, Metaspace::reserve_alignment_words());
  if (value > aligned_max) {
    if (VerifyFlagConstraints) {
      InitialBootClassLoaderMetaspaceSize = aligned_max;
      JVMFlag::printError(true, "InitialBootClassLoaderMetaspaceSize:"SIZE_FORMAT"\n", InitialBootClassLoaderMetaspaceSize);
      return JVMFlag::SUCCESS;
    }
    JVMFlag::printError(verbose,
                        "InitialBootClassLoaderMetaspaceSize (" SIZE_FORMAT ") must be "
                        "less than or equal to aligned maximum value (" SIZE_FORMAT ")\n",
                        value, aligned_max);
    return JVMFlag::VIOLATES_CONSTRAINT;
  }
  return JVMFlag::SUCCESS;
}

// To avoid an overflow by 'align_up(value, alignment)'.
static JVMFlag::Error MaxSizeForAlignment(const char* name, size_t* value_addr, size_t value, size_t alignment, bool verbose) {
  size_t aligned_max = ((max_uintx - alignment) & ~(alignment-1));
  if (value > aligned_max) {
    if (VerifyFlagConstraints) {
      *value_addr = aligned_max;
      JVMFlag::printError(true, "%s:"SIZE_FORMAT"\n", name, aligned_max);
      return JVMFlag::SUCCESS;
    }
    JVMFlag::printError(verbose,
                        "%s (" SIZE_FORMAT ") must be "
                        "less than or equal to aligned maximum value (" SIZE_FORMAT ")\n",
                        name, value, aligned_max);
    return JVMFlag::VIOLATES_CONSTRAINT;
  }
  return JVMFlag::SUCCESS;
}

static JVMFlag::Error MaxSizeForHeapAlignment(const char* name, size_t* value_addr, size_t value, bool verbose) {
  size_t heap_alignment;

#if INCLUDE_G1GC
  if (UseG1GC) {
    // For G1 GC, we don't know until G1CollectorPolicy is created.
    heap_alignment = MaxSizeForHeapAlignmentG1();
  } else
#endif
  {
    heap_alignment = CollectorPolicy::compute_heap_alignment();
  }

  return MaxSizeForAlignment(name, value_addr, value, heap_alignment, verbose);
}

JVMFlag::Error InitialHeapSizeConstraintFunc(size_t value, bool verbose) {
  return MaxSizeForHeapAlignment("InitialHeapSize", &InitialHeapSize, value, verbose);
}

JVMFlag::Error MaxHeapSizeConstraintFunc(size_t value, bool verbose) {
  JVMFlag::Error status = MaxSizeForHeapAlignment("MaxHeapSize", &MaxHeapSize, value, verbose);

  if (status == JVMFlag::SUCCESS) {
    status = CheckMaxHeapSizeAndSoftRefLRUPolicyMSPerMB(value, SoftRefLRUPolicyMSPerMB, verbose, true);
  }
  return status;
}

#if INCLUDE_ZGC
JVMFlag::Error SoftMaxHeapSizeConstraintFunc(size_t value, bool verbose) {
  if (value > MaxHeapSize) {
    if (VerifyFlagConstraints) {
      SoftMaxHeapSize = MaxHeapSize;
      JVMFlag::printError(true, "SoftMaxHeapSize:"SIZE_FORMAT"\n", SoftMaxHeapSize);
      return JVMFlag::SUCCESS;
    }
    JVMFlag::printError(verbose, "SoftMaxHeapSize must be less than or equal to the maximum heap size\n");
    return JVMFlag::VIOLATES_CONSTRAINT;
  }

  return JVMFlag::SUCCESS;
}
#endif

JVMFlag::Error HeapBaseMinAddressConstraintFunc(size_t value, bool verbose) {
  // If an overflow happened in Arguments::set_heap_size(), MaxHeapSize will have too large a value.
  // Check for this by ensuring that MaxHeapSize plus the requested min base address still fit within max_uintx.
  if (UseCompressedOops && FLAG_IS_ERGO(MaxHeapSize) && (value > (max_uintx - MaxHeapSize))) {
    if (VerifyFlagConstraints) {
      HeapBaseMinAddress = max_uintx - MaxHeapSize;
      JVMFlag::printError(true, "HeapBaseMinAddress:"SIZE_FORMAT"\n", HeapBaseMinAddress);
    } else {
      JVMFlag::printError(verbose,
                        "HeapBaseMinAddress (" SIZE_FORMAT ") or MaxHeapSize (" SIZE_FORMAT ") is too large. "
                        "Sum of them must be less than or equal to maximum of size_t (" SIZE_FORMAT ")\n",
                        value, MaxHeapSize, max_uintx);
      return JVMFlag::VIOLATES_CONSTRAINT;
    }
  }

  return MaxSizeForHeapAlignment("HeapBaseMinAddress", &HeapBaseMinAddress, value, verbose);
}

JVMFlag::Error NewSizeConstraintFunc(size_t value, bool verbose) {
#if INCLUDE_G1GC
  JVMFlag::Error status = NewSizeConstraintFuncG1(value, verbose);
  if (status != JVMFlag::SUCCESS) {
    return status;
  }
#endif

  return JVMFlag::SUCCESS;
}

JVMFlag::Error MinTLABSizeConstraintFunc(size_t value, bool verbose) {
  // At least, alignment reserve area is needed.
  bool verifyFailed = false;
  if (value < ThreadLocalAllocBuffer::alignment_reserve_in_bytes()) {
    if (VerifyFlagConstraints) {
      verifyFailed = true;
      value = ThreadLocalAllocBuffer::alignment_reserve_in_bytes();
    } else {
      JVMFlag::printError(verbose,
                        "MinTLABSize (" SIZE_FORMAT ") must be "
                        "greater than or equal to reserved area in TLAB (" SIZE_FORMAT ")\n",
                        value, ThreadLocalAllocBuffer::alignment_reserve_in_bytes());
      return JVMFlag::VIOLATES_CONSTRAINT;
    }
  }
  if (value > (ThreadLocalAllocBuffer::max_size() * HeapWordSize)) {
    if (VerifyFlagConstraints) {
      value = (ThreadLocalAllocBuffer::max_size() * HeapWordSize);
      verifyFailed = true;
    } else {
      JVMFlag::printError(verbose,
                        "MinTLABSize (" SIZE_FORMAT ") must be "
                        "less than or equal to ergonomic TLAB maximum (" SIZE_FORMAT ")\n",
                        value, ThreadLocalAllocBuffer::max_size() * HeapWordSize);
      return JVMFlag::VIOLATES_CONSTRAINT;
    }
  }
  if (verifyFailed) {
    MinTLABSize = value;
    JVMFlag::printError(true, "MinTLABSize:"SIZE_FORMAT"\n", value);
  }
  return JVMFlag::SUCCESS;
}

JVMFlag::Error TLABSizeConstraintFunc(size_t value, bool verbose) {
  // Skip for default value of zero which means set ergonomically.
  if (FLAG_IS_CMDLINE(TLABSize)) {
    bool verifyFailed = false;
    if (value < MinTLABSize) {
      if (VerifyFlagConstraints) {
        value = MinTLABSize;
        verifyFailed = true;
      } else {
        JVMFlag::printError(verbose,
                          "TLABSize (" SIZE_FORMAT ") must be "
                          "greater than or equal to MinTLABSize (" SIZE_FORMAT ")\n",
                          value, MinTLABSize);
        return JVMFlag::VIOLATES_CONSTRAINT;
      }
    }
    if (value > (ThreadLocalAllocBuffer::max_size() * HeapWordSize)) {
      if (VerifyFlagConstraints) {
        value = ThreadLocalAllocBuffer::max_size() * HeapWordSize;
        verifyFailed = true;
      } else {
        JVMFlag::printError(verbose,
                          "TLABSize (" SIZE_FORMAT ") must be "
                          "less than or equal to ergonomic TLAB maximum size (" SIZE_FORMAT ")\n",
                          value, (ThreadLocalAllocBuffer::max_size() * HeapWordSize));
        return JVMFlag::VIOLATES_CONSTRAINT;
      }
    }
    if (verifyFailed) {
      TLABSize = value;
      JVMFlag::printError(true, "TLABSize:"SIZE_FORMAT"\n", value);
    }
  }
  return JVMFlag::SUCCESS;
}

// We will protect overflow from ThreadLocalAllocBuffer::record_slow_allocation(),
// so AfterMemoryInit type is enough to check.
JVMFlag::Error TLABWasteIncrementConstraintFunc(uintx value, bool verbose) {
  if (UseTLAB) {
    size_t refill_waste_limit = Thread::current()->tlab().refill_waste_limit();

    // Compare with 'max_uintx' as ThreadLocalAllocBuffer::_refill_waste_limit is 'size_t'.
    if (refill_waste_limit > (max_uintx - value)) {
      if (VerifyFlagConstraints) {
        TLABWasteIncrement = max_uintx - refill_waste_limit;
        JVMFlag::printError(true, "TLABWasteIncrement:"UINTX_FORMAT"\n", TLABWasteIncrement);
        return JVMFlag::SUCCESS;
      }
      JVMFlag::printError(verbose,
                          "TLABWasteIncrement (" UINTX_FORMAT ") must be "
                          "less than or equal to ergonomic TLAB waste increment maximum size(" SIZE_FORMAT ")\n",
                          value, (max_uintx - refill_waste_limit));
      return JVMFlag::VIOLATES_CONSTRAINT;
    }
  }
  return JVMFlag::SUCCESS;
}

JVMFlag::Error SurvivorRatioConstraintFunc(uintx value, bool verbose) {
  if (FLAG_IS_CMDLINE(SurvivorRatio) &&
      (value > (MaxHeapSize / Universe::heap()->collector_policy()->space_alignment()))) {
        if (VerifyFlagConstraints) {
          SurvivorRatio = MaxHeapSize / Universe::heap()->collector_policy()->space_alignment();
          JVMFlag::printError(true, "SurvivorRatio:"UINTX_FORMAT"\n", SurvivorRatio);
          return JVMFlag::SUCCESS;
        }
        JVMFlag::printError(verbose,
                        "SurvivorRatio (" UINTX_FORMAT ") must be "
                        "less than or equal to ergonomic SurvivorRatio maximum (" SIZE_FORMAT ")\n",
                        value,
                        (MaxHeapSize / Universe::heap()->collector_policy()->space_alignment()));
    return JVMFlag::VIOLATES_CONSTRAINT;
  } else {
    return JVMFlag::SUCCESS;
  }
}

JVMFlag::Error MetaspaceSizeConstraintFunc(size_t value, bool verbose) {
  if (value > MaxMetaspaceSize) {
    if (VerifyFlagConstraints) {
      MetaspaceSize = MaxMetaspaceSize;
      JVMFlag::printError(true, "MetaspaceSize:"SIZE_FORMAT"\n", MetaspaceSize);
      return JVMFlag::SUCCESS;
    }
    JVMFlag::printError(verbose,
                        "MetaspaceSize (" SIZE_FORMAT ") must be "
                        "less than or equal to MaxMetaspaceSize (" SIZE_FORMAT ")\n",
                        value, MaxMetaspaceSize);
    return JVMFlag::VIOLATES_CONSTRAINT;
  } else {
    return JVMFlag::SUCCESS;
  }
}

JVMFlag::Error MaxMetaspaceSizeConstraintFunc(size_t value, bool verbose) {
  if (value < MetaspaceSize) {
    if (VerifyFlagConstraints) {
      MaxMetaspaceSize = MetaspaceSize;
      JVMFlag::printError(true, "MaxMetaspaceSize:"SIZE_FORMAT"\n", MaxMetaspaceSize);
      return JVMFlag::SUCCESS;
    }
    JVMFlag::printError(verbose,
                        "MaxMetaspaceSize (" SIZE_FORMAT ") must be "
                        "greater than or equal to MetaspaceSize (" SIZE_FORMAT ")\n",
                        value, MaxMetaspaceSize);
    return JVMFlag::VIOLATES_CONSTRAINT;
  } else {
    return JVMFlag::SUCCESS;
  }
}

JVMFlag::Error SurvivorAlignmentInBytesConstraintFunc(intx value, bool verbose) {
  if (value != 0) {
    bool verifyFailed = false;
    if (value < ObjectAlignmentInBytes) {
      if (VerifyFlagConstraints) {
        verifyFailed = true;
        value = ObjectAlignmentInBytes;
      } else {
        JVMFlag::printError(verbose,
                          "SurvivorAlignmentInBytes (" INTX_FORMAT ") must be "
                          "greater than or equal to ObjectAlignmentInBytes (" INTX_FORMAT ")\n",
                          value, ObjectAlignmentInBytes);
        return JVMFlag::VIOLATES_CONSTRAINT;
      }
    }
    if (!is_power_of_2(value)) {
      if (VerifyFlagConstraints) {
        verifyFailed = true;
        int logValue = log2_intptr(value);
        value = (intx)(1 << logValue);
      } else {
        JVMFlag::printError(verbose,
                          "SurvivorAlignmentInBytes (" INTX_FORMAT ") must be "
                          "power of 2\n",
                          value);
        return JVMFlag::VIOLATES_CONSTRAINT;
      }
    }
    if (verifyFailed) {
      SurvivorAlignmentInBytes = value;
      JVMFlag::printError(true, "SurvivorAlignmentInBytes:"INTX_FORMAT"\n", value);
    }
  }
  return JVMFlag::SUCCESS;
}
