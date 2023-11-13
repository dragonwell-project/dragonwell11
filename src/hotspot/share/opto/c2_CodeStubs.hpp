/*
 * Copyright (c) 2022, Oracle and/or its affiliates. All rights reserved.
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

#include "asm/assembler.hpp"
#include "asm/codeBuffer.hpp"
#include "memory/allocation.hpp"
#include "opto/c2_MacroAssembler.hpp"
#include "utilities/growableArray.hpp"

#ifndef SHARE_OPTO_C2_CODESTUBS_HPP
#define SHARE_OPTO_C2_CODESTUBS_HPP

class C2CodeStub : public ResourceObj {
private:
  Label _entry;
  Label _continuation;

protected:
  C2CodeStub() :
    _entry(),
    _continuation() {}
  ~C2CodeStub() {}

public:
  Label& entry()        { return _entry; }
  Label& continuation() { return _continuation; }

  virtual void emit(C2_MacroAssembler& masm) = 0;
  virtual int max_size() const = 0;
};

class C2CodeStubList : public ResourceObj {
private:
  GrowableArray<C2CodeStub*>* _stubs;

public:
  C2CodeStubList();
  ~C2CodeStubList() {}
  void add_stub(C2CodeStub* stub) { _stubs->append(stub); }
  void emit(CodeBuffer& cb);
};

#ifdef _LP64
class C2HandleAnonOMOwnerStub : public C2CodeStub {
private:
  Register _monitor;
  Register _tmp;
public:
  C2HandleAnonOMOwnerStub(Register monitor, Register tmp = noreg) : C2CodeStub(),
    _monitor(monitor), _tmp(tmp) {}
  Register monitor() { return _monitor; }
  Register tmp() { return _tmp; }
  int max_size() const;
  void emit(C2_MacroAssembler& masm);
};

class C2LoadNKlassStub : public C2CodeStub {
private:
  Register _dst;
public:
  C2LoadNKlassStub(Register dst) : C2CodeStub(), _dst(dst) {}
  Register dst() { return _dst; }
  int max_size() const;
  void emit(C2_MacroAssembler& masm);
};
#endif

#endif // SHARE_OPTO_C2_CODESTUBS_HPP
