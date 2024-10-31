/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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
#include "runtime/arguments_ext.hpp"
#include "runtime/java.hpp"

bool lookup_special_flag_ext(const char *flag_name, SpecialFlag& flag) {
  return false;
}

void ArgumentsExt::set_tenant_flags() {
  // TenantHeapIsolation directly depends on MultiTenant, UseG1GC
  if (TenantHeapIsolation) {
    if (FLAG_IS_DEFAULT(MultiTenant)) {
      FLAG_SET_ERGO(bool, MultiTenant, true);
    }
    if (UseTLAB && FLAG_IS_DEFAULT(UsePerTenantTLAB)) {
      // enable per-tenant TLABs if unspecified and heap isolation is enabled
      FLAG_SET_ERGO(bool, UsePerTenantTLAB, true);
    }

    // check GC policy compatibility
    if (!UseG1GC) {
      vm_exit_during_initialization("-XX:+TenantHeapIsolation only works with -XX:+UseG1GC");
    }
    if (!MultiTenant) {
      vm_exit_during_initialization("Cannot use multi-tenant features if -XX:-MultiTenant specified");
    }
  }

  // UsePerTenantTLAB depends on TenantHeapIsolation and UseTLAB
  if (UsePerTenantTLAB) {
    if (!TenantHeapIsolation || !UseTLAB) {
      vm_exit_during_initialization("-XX:+UsePerTenantTLAB only works with -XX:+TenantHeapIsolation and -XX:+UseTLAB");
    }
  }
}
