/*
 * Copyright (c) 2015, 2018, Oracle and/or its affiliates. All rights reserved.
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
#include "runtime/arguments.hpp"
#include "runtime/flags/jvmFlag.hpp"
#include "runtime/flags/jvmFlagConstraintsRuntime.hpp"
#include "runtime/globals.hpp"
#include "runtime/safepointMechanism.hpp"
#include "runtime/task.hpp"

JVMFlag::Error ObjectAlignmentInBytesConstraintFunc(intx value, bool verbose) {
  bool verifyFailed = false;
  if (!is_power_of_2(value)) {
    if (VerifyFlagConstraints) {
      verifyFailed = true;
      int logValue = log2_intptr(value);
      value = (intx)(1 << logValue);
    } else {
      JVMFlag::printError(verbose,
                          "ObjectAlignmentInBytes (" INTX_FORMAT ") must be "
                          "power of 2\n",
                          value);
      return JVMFlag::VIOLATES_CONSTRAINT;
    }
  }
  // In case page size is very small.
  if (value >= (intx)os::vm_page_size()) {
    if (VerifyFlagConstraints) {
      verifyFailed = true;
      value = (intx)(os::vm_page_size()/2);
    } else {
      JVMFlag::printError(verbose,
                        "ObjectAlignmentInBytes (" INTX_FORMAT ") must be "
                        "less than page size (" INTX_FORMAT ")\n",
                        value, (intx)os::vm_page_size());
      return JVMFlag::VIOLATES_CONSTRAINT;
    }
  }
  if (verifyFailed) {
    ObjectAlignmentInBytes = value;
    JVMFlag::printError(true, "ObjectAlignmentInBytes:" INTX_FORMAT "\n", value);
  }
  return JVMFlag::SUCCESS;
}

// Need to enforce the padding not to break the existing field alignments.
// It is sufficient to check against the largest type size.
JVMFlag::Error ContendedPaddingWidthConstraintFunc(intx value, bool verbose) {
  if ((value % BytesPerLong) != 0) {
    if (VerifyFlagConstraints) {
      int remainder = value % BytesPerLong;
      ContendedPaddingWidth = value - remainder;
      JVMFlag::printError(true, "ContendedPaddingWidth:" INTX_FORMAT "\n", ContendedPaddingWidth);
      return JVMFlag::SUCCESS;
    }
    JVMFlag::printError(verbose,
                        "ContendedPaddingWidth (" INTX_FORMAT ") must be "
                        "a multiple of %d\n",
                        value, BytesPerLong);
    return JVMFlag::VIOLATES_CONSTRAINT;
  } else {
    return JVMFlag::SUCCESS;
  }
}

JVMFlag::Error BiasedLockingBulkRebiasThresholdFunc(intx value, bool verbose) {
  if (value > BiasedLockingBulkRevokeThreshold) {
    if (VerifyFlagConstraints) {
      BiasedLockingBulkRebiasThreshold = BiasedLockingBulkRevokeThreshold;
      JVMFlag::printError(true, "BiasedLockingBulkRebiasThreshold:" INTX_FORMAT "\n", BiasedLockingBulkRevokeThreshold);
      return JVMFlag::SUCCESS;
    }
    JVMFlag::printError(verbose,
                        "BiasedLockingBulkRebiasThreshold (" INTX_FORMAT ") must be "
                        "less than or equal to BiasedLockingBulkRevokeThreshold (" INTX_FORMAT ")\n",
                        value, BiasedLockingBulkRevokeThreshold);
    return JVMFlag::VIOLATES_CONSTRAINT;
  } else {
    return JVMFlag::SUCCESS;
  }
}

JVMFlag::Error BiasedLockingStartupDelayFunc(intx value, bool verbose) {
  if ((value % PeriodicTask::interval_gran) != 0) {
    if (VerifyFlagConstraints) {
      int remainder = value % PeriodicTask::interval_gran;
      BiasedLockingStartupDelay = value - remainder;
      JVMFlag::printError(true, "BiasedLockingStartupDelay:" INTX_FORMAT "\n", BiasedLockingStartupDelay);
      return JVMFlag::SUCCESS;
    }
    JVMFlag::printError(verbose,
                        "BiasedLockingStartupDelay (" INTX_FORMAT ") must be "
                        "evenly divisible by PeriodicTask::interval_gran (" INTX_FORMAT ")\n",
                        value, PeriodicTask::interval_gran);
    return JVMFlag::VIOLATES_CONSTRAINT;
  } else {
    return JVMFlag::SUCCESS;
  }
}

JVMFlag::Error BiasedLockingBulkRevokeThresholdFunc(intx value, bool verbose) {
  if (value < BiasedLockingBulkRebiasThreshold) {
    if (VerifyFlagConstraints) {
      BiasedLockingBulkRevokeThreshold = BiasedLockingBulkRebiasThreshold;
      JVMFlag::printError(true, "BiasedLockingBulkRevokeThreshold:" INTX_FORMAT "\n", BiasedLockingBulkRevokeThreshold);
      return JVMFlag::SUCCESS;
    }
    JVMFlag::printError(verbose,
                        "BiasedLockingBulkRevokeThreshold (" INTX_FORMAT ") must be "
                        "greater than or equal to BiasedLockingBulkRebiasThreshold (" INTX_FORMAT ")\n",
                        value, BiasedLockingBulkRebiasThreshold);
    return JVMFlag::VIOLATES_CONSTRAINT;
  } else if ((double)value/(double)BiasedLockingDecayTime > 0.1) {
    if (VerifyFlagConstraints) {
      BiasedLockingBulkRevokeThreshold = (intx)(BiasedLockingDecayTime * 0.1);
      JVMFlag::printError(true, "BiasedLockingBulkRevokeThreshold:" INTX_FORMAT "\n", BiasedLockingBulkRevokeThreshold);
      return JVMFlag::SUCCESS;
    }
    JVMFlag::printError(verbose,
                        "The ratio of BiasedLockingBulkRevokeThreshold (" INTX_FORMAT ")"
                        " to BiasedLockingDecayTime (" INTX_FORMAT ") must be "
                        "less than or equal to 0.1\n",
                        value, BiasedLockingBulkRebiasThreshold);
    return JVMFlag::VIOLATES_CONSTRAINT;
  } else {
    return JVMFlag::SUCCESS;
  }
}

JVMFlag::Error BiasedLockingDecayTimeFunc(intx value, bool verbose) {
  if (BiasedLockingBulkRebiasThreshold/(double)value > 0.1) {
    if (VerifyFlagConstraints) {
      BiasedLockingDecayTime = BiasedLockingBulkRebiasThreshold * 10;
      JVMFlag::printError(true, "BiasedLockingDecayTime:"INTX_FORMAT"\n", BiasedLockingDecayTime);
      return JVMFlag::SUCCESS;
    }
    JVMFlag::printError(verbose,
                        "The ratio of BiasedLockingBulkRebiasThreshold (" INTX_FORMAT ")"
                        " to BiasedLockingDecayTime (" INTX_FORMAT ") must be "
                        "less than or equal to 0.1\n",
                        BiasedLockingBulkRebiasThreshold, value);
    return JVMFlag::VIOLATES_CONSTRAINT;
  } else {
    return JVMFlag::SUCCESS;
  }
}

JVMFlag::Error PerfDataSamplingIntervalFunc(intx value, bool verbose) {
  if ((value % PeriodicTask::interval_gran != 0)) {
    if (VerifyFlagConstraints) {
      int remainder = value % PeriodicTask::interval_gran;
      PerfDataSamplingInterval = value - remainder;
      JVMFlag::printError(true, "PerfDataSamplingInterval:"INTX_FORMAT"\n", PerfDataSamplingInterval);
      return JVMFlag::SUCCESS;
    }
    JVMFlag::printError(verbose,
                        "PerfDataSamplingInterval (" INTX_FORMAT ") must be "
                        "evenly divisible by PeriodicTask::interval_gran (" INTX_FORMAT ")\n",
                        value, PeriodicTask::interval_gran);
    return JVMFlag::VIOLATES_CONSTRAINT;
  } else {
    return JVMFlag::SUCCESS;
  }
}

JVMFlag::Error ThreadLocalHandshakesConstraintFunc(bool value, bool verbose) {
  if (value) {
    if (!SafepointMechanism::supports_thread_local_poll()) {
      if (VerifyFlagConstraints) {
        VerifyFlagConstraints = false;
        JVMFlag::printError(true, "ThreadLocalHandshakes:false\n");
        return JVMFlag::SUCCESS;
      }
      JVMFlag::printError(verbose, "ThreadLocalHandshakes not yet supported on this platform\n");
      return JVMFlag::VIOLATES_CONSTRAINT;
    }
  }
  return JVMFlag::SUCCESS;
}
