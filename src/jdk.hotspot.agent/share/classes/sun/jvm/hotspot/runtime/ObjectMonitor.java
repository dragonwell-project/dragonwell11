/*
 * Copyright (c) 2001, 2014, Oracle and/or its affiliates. All rights reserved.
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

package sun.jvm.hotspot.runtime;

import java.util.*;

import sun.jvm.hotspot.debugger.*;
import sun.jvm.hotspot.oops.*;
import sun.jvm.hotspot.types.*;

public class ObjectMonitor extends VMObject {
  static {
    VM.registerVMInitializedObserver(new Observer() {
        public void update(Observable o, Object data) {
          initialize(VM.getVM().getTypeDataBase());
        }
      });
  }

  private static synchronized void initialize(TypeDataBase db) throws WrongTypeException {
    heap = VM.getVM().getObjectHeap();
    Type type  = db.lookupType("ObjectMonitor");
    sun.jvm.hotspot.types.Field f = type.getField("_header");
    headerFieldOffset = f.getOffset();
    f = type.getField("_object");
    objectFieldOffset = f.getOffset();
    f = type.getField("_owner");
    ownerFieldOffset = f.getOffset();
    f = type.getField("FreeNext");
    FreeNextFieldOffset = f.getOffset();
    countField  = type.getJIntField("_count");
    waitersField = type.getJIntField("_waiters");
    recursionsField = type.getCIntegerField("_recursions");

    ANONYMOUS_OWNER = db.lookupLongConstant("ObjectMonitor::ANONYMOUS_OWNER").longValue();
  }

  public ObjectMonitor(Address addr) {
    super(addr);
  }

  public Mark header() {
    return new Mark(addr.addOffsetTo(headerFieldOffset));
  }

  // FIXME
  //  void      set_header(markOop hdr);

  // FIXME: must implement and delegate to platform-dependent implementation
  //  public boolean isBusy();
  public boolean isEntered(sun.jvm.hotspot.runtime.Thread current) {
    Address o = owner();
    if (current.threadObjectAddress().equals(o) ||
        current.isLockOwned(o)) {
      return true;
    }
    return false;
  }

  public boolean isOwnedAnonymous() {
    if (VM.getVM().getCommandLineBooleanFlag("UseAltFastLocking") && owner() != null) {
      return addr.getAddressAt(ownerFieldOffset).asLongValue() == ANONYMOUS_OWNER;
    }
    return false;
  }

  public Address owner() { return addr.getAddressAt(ownerFieldOffset); }
  // FIXME
  //  void      set_owner(void* owner);

  public int    waiters() { return waitersField.getValue(addr); }

  public Address freeNext() { return addr.getAddressAt(FreeNextFieldOffset); }
  // FIXME
  //  void      set_queue(void* owner);

  public int count() { return countField.getValue(addr); }
  // FIXME
  //  void      set_count(int count);

  public long recursions() { return recursionsField.getValue(addr); }

  public OopHandle object() {
    return addr.getOopHandleAt(objectFieldOffset);
  }

  // contentions is always equal to count
  public int contentions() {
      return count();
  }

  // FIXME
  //  void*     object_addr();
  //  void      set_object(void* obj);

  // The following four either aren't expressed as typed fields in
  // vmStructs.cpp because they aren't strongly typed in the VM, or
  // would confuse the SA's type system.
  private static ObjectHeap    heap;
  private static long          headerFieldOffset;
  private static long          objectFieldOffset;
  private static long          ownerFieldOffset;
  private static long          FreeNextFieldOffset;
  private static JIntField     countField;
  private static JIntField     waitersField;
  private static CIntegerField recursionsField;
  private static long          ANONYMOUS_OWNER;

  // FIXME: expose platform-dependent stuff
}
