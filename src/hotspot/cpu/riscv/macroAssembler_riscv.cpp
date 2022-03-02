/*
 * Copyright (c) 1997, 2020, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2014, 2020, Red Hat Inc. All rights reserved.
 * Copyright (c) 2020, 2021, Huawei Technologies Co., Ltd. All rights reserved.
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
#include "asm/assembler.hpp"
#include "asm/assembler.inline.hpp"
#include "compiler/disassembler.hpp"
#include "gc/shared/barrierSet.hpp"
#include "gc/shared/barrierSetAssembler.hpp"
#include "gc/shared/cardTable.hpp"
#include "gc/shared/cardTableBarrierSet.hpp"
#include "interpreter/bytecodeHistogram.hpp"
#include "interpreter/interpreter.hpp"
#include "memory/resourceArea.hpp"
#include "memory/universe.hpp"
#include "nativeInst_riscv.hpp"
#include "oops/accessDecorators.hpp"
#include "oops/compressedOops.inline.hpp"
#include "oops/klass.inline.hpp"
#include "oops/oop.hpp"
#include "runtime/biasedLocking.hpp"
#include "runtime/interfaceSupport.inline.hpp"
#include "runtime/jniHandles.inline.hpp"
#include "runtime/sharedRuntime.hpp"
#include "runtime/stubRoutines.hpp"
#include "runtime/thread.hpp"
#ifdef COMPILER2
#include "opto/compile.hpp"
#include "opto/node.hpp"
#include "opto/output.hpp"
#endif

#ifdef PRODUCT
#define BLOCK_COMMENT(str) /* nothing */
#else
#define BLOCK_COMMENT(str) block_comment(str)
#endif
#define BIND(label) bind(label); __ BLOCK_COMMENT(#label ":")

static void pass_arg0(MacroAssembler* masm, Register arg) {
  if (c_rarg0 != arg) {
    assert_cond(masm != NULL);
    masm->mv(c_rarg0, arg);
  }
}

static void pass_arg1(MacroAssembler* masm, Register arg) {
  if (c_rarg1 != arg) {
    assert_cond(masm != NULL);
    masm->mv(c_rarg1, arg);
  }
}

static void pass_arg2(MacroAssembler* masm, Register arg) {
  if (c_rarg2 != arg) {
    assert_cond(masm != NULL);
    masm->mv(c_rarg2, arg);
  }
}

static void pass_arg3(MacroAssembler* masm, Register arg) {
  if (c_rarg3 != arg) {
    assert_cond(masm != NULL);
    masm->mv(c_rarg3, arg);
  }
}

void MacroAssembler::align(int modulus) {
  while (offset() % modulus != 0) { nop(); }
}

void MacroAssembler::call_VM_helper(Register oop_result, address entry_point, int number_of_arguments, bool check_exceptions) {
  call_VM_base(oop_result, noreg, noreg, entry_point, number_of_arguments, check_exceptions);
}

// Implementation of call_VM versions

void MacroAssembler::call_VM(Register oop_result,
                             address entry_point,
                             bool check_exceptions) {
  call_VM_helper(oop_result, entry_point, 0, check_exceptions);
}

void MacroAssembler::call_VM(Register oop_result,
                             address entry_point,
                             Register arg_1,
                             bool check_exceptions) {
  pass_arg1(this, arg_1);
  call_VM_helper(oop_result, entry_point, 1, check_exceptions);
}

void MacroAssembler::call_VM(Register oop_result,
                             address entry_point,
                             Register arg_1,
                             Register arg_2,
                             bool check_exceptions) {
  assert(arg_1 != c_rarg2, "smashed arg");
  pass_arg2(this, arg_2);
  pass_arg1(this, arg_1);
  call_VM_helper(oop_result, entry_point, 2, check_exceptions);
}

void MacroAssembler::call_VM(Register oop_result,
                             address entry_point,
                             Register arg_1,
                             Register arg_2,
                             Register arg_3,
                             bool check_exceptions) {
  assert(arg_1 != c_rarg3, "smashed arg");
  assert(arg_2 != c_rarg3, "smashed arg");
  pass_arg3(this, arg_3);

  assert(arg_1 != c_rarg2, "smashed arg");
  pass_arg2(this, arg_2);

  pass_arg1(this, arg_1);
  call_VM_helper(oop_result, entry_point, 3, check_exceptions);
}

void MacroAssembler::call_VM(Register oop_result,
                             Register last_java_sp,
                             address entry_point,
                             int number_of_arguments,
                             bool check_exceptions) {
  call_VM_base(oop_result, xthread, last_java_sp, entry_point, number_of_arguments, check_exceptions);
}

void MacroAssembler::call_VM(Register oop_result,
                             Register last_java_sp,
                             address entry_point,
                             Register arg_1,
                             bool check_exceptions) {
  pass_arg1(this, arg_1);
  call_VM(oop_result, last_java_sp, entry_point, 1, check_exceptions);
}

void MacroAssembler::call_VM(Register oop_result,
                             Register last_java_sp,
                             address entry_point,
                             Register arg_1,
                             Register arg_2,
                             bool check_exceptions) {

  assert(arg_1 != c_rarg2, "smashed arg");
  pass_arg2(this, arg_2);
  pass_arg1(this, arg_1);
  call_VM(oop_result, last_java_sp, entry_point, 2, check_exceptions);
}

void MacroAssembler::call_VM(Register oop_result,
                             Register last_java_sp,
                             address entry_point,
                             Register arg_1,
                             Register arg_2,
                             Register arg_3,
                             bool check_exceptions) {
  assert(arg_1 != c_rarg3, "smashed arg");
  assert(arg_2 != c_rarg3, "smashed arg");
  pass_arg3(this, arg_3);
  assert(arg_1 != c_rarg2, "smashed arg");
  pass_arg2(this, arg_2);
  pass_arg1(this, arg_1);
  call_VM(oop_result, last_java_sp, entry_point, 3, check_exceptions);
}

// these are no-ops overridden by InterpreterMacroAssembler
void MacroAssembler::check_and_handle_earlyret(Register java_thread) {}
void MacroAssembler::check_and_handle_popframe(Register java_thread) {}

// Calls to C land
//
// When entering C land, the fp, & esp of the last Java frame have to be recorded
// in the (thread-local) JavaThread object. When leaving C land, the last Java fp
// has to be reset to 0. This is required to allow proper stack traversal.
void MacroAssembler::set_last_Java_frame(Register last_java_sp,
                                         Register last_java_fp,
                                         Register last_java_pc,
                                         Register temp) {

  if (last_java_pc->is_valid()) {
      sd(last_java_pc, Address(xthread,
                               JavaThread::frame_anchor_offset() +
                               JavaFrameAnchor::last_Java_pc_offset()));
  }

  // determine last_java_sp register
  if (last_java_sp == sp) {
    mv(temp, sp);
    last_java_sp = temp;
  } else if (!last_java_sp->is_valid()) {
    last_java_sp = esp;
  }

  sd(last_java_sp, Address(xthread, JavaThread::last_Java_sp_offset()));

  // last_java_fp is optional
  if (last_java_fp->is_valid()) {
    sd(last_java_fp, Address(xthread, JavaThread::last_Java_fp_offset()));
  }
}

void MacroAssembler::set_last_Java_frame(Register last_java_sp,
                                         Register last_java_fp,
                                         address  last_java_pc,
                                         Register temp) {
  assert(last_java_pc != NULL, "must provide a valid PC");

  la(temp, last_java_pc);
  sd(temp, Address(xthread, JavaThread::frame_anchor_offset() + JavaFrameAnchor::last_Java_pc_offset()));

  set_last_Java_frame(last_java_sp, last_java_fp, noreg, temp);
}

void MacroAssembler::set_last_Java_frame(Register last_java_sp,
                                         Register last_java_fp,
                                         Label &L,
                                         Register temp) {
  if (L.is_bound()) {
    set_last_Java_frame(last_java_sp, last_java_fp, target(L), temp);
  } else {
    InstructionMark im(this);
    L.add_patch_at(code(), locator());
    set_last_Java_frame(last_java_sp, last_java_fp, pc() /* Patched later */, temp);
  }
}

void MacroAssembler::reset_last_Java_frame(bool clear_fp) {
  // we must set sp to zero to clear frame
  sd(zr, Address(xthread, JavaThread::last_Java_sp_offset()));

  // must clear fp, so that compiled frames are not confused; it is
  // possible that we need it only for debugging
  if (clear_fp) {
    sd(zr, Address(xthread, JavaThread::last_Java_fp_offset()));
  }

  // Always clear the pc because it could have been set by make_walkable()
  sd(zr, Address(xthread, JavaThread::last_Java_pc_offset()));
}

void MacroAssembler::call_VM_base(Register oop_result,
                                  Register java_thread,
                                  Register last_java_sp,
                                  address  entry_point,
                                  int      number_of_arguments,
                                  bool     check_exceptions) {
   // determine java_thread register
  if (!java_thread->is_valid()) {
    java_thread = xthread;
  }
  // determine last_java_sp register
  if (!last_java_sp->is_valid()) {
    last_java_sp = esp;
  }

  // debugging support
  assert(number_of_arguments >= 0   , "cannot have negative number of arguments");
  assert(java_thread == xthread, "unexpected register");

  assert(java_thread != oop_result  , "cannot use the same register for java_thread & oop_result");
  assert(java_thread != last_java_sp, "cannot use the same register for java_thread & last_java_sp");

  // push java thread (becomes first argument of C function)
  mv(c_rarg0, java_thread);

  // set last Java frame before call
  assert(last_java_sp != fp, "can't use fp");

  Label l;
  set_last_Java_frame(last_java_sp, fp, l, t0);

  // do the call, remove parameters
  MacroAssembler::call_VM_leaf_base(entry_point, number_of_arguments, &l);

  // reset last Java frame
  // Only interpreter should have to clear fp
  reset_last_Java_frame(true);

   // C++ interp handles this in the interpreter
  check_and_handle_popframe(java_thread);
  check_and_handle_earlyret(java_thread);

  if (check_exceptions) {
    // check for pending exceptions (java_thread is set upon return)
    ld(t0, Address(java_thread, in_bytes(Thread::pending_exception_offset())));
    Label ok;
    beqz(t0, ok);
    int32_t offset = 0;
    la_patchable(t0, RuntimeAddress(StubRoutines::forward_exception_entry()), offset);
    jalr(x0, t0, offset);
    bind(ok);
  }

  // get oop result if there is one and reset the value in the thread
  if (oop_result->is_valid()) {
    get_vm_result(oop_result, java_thread);
  }
}

void MacroAssembler::get_vm_result(Register oop_result, Register java_thread) {
  ld(oop_result, Address(java_thread, JavaThread::vm_result_offset()));
  sd(zr, Address(java_thread, JavaThread::vm_result_offset()));
  verify_oop(oop_result, "broken oop in call_VM_base");
}

void MacroAssembler::get_vm_result_2(Register metadata_result, Register java_thread) {
  ld(metadata_result, Address(java_thread, JavaThread::vm_result_2_offset()));
  sd(zr, Address(java_thread, JavaThread::vm_result_2_offset()));
}

void MacroAssembler::verify_oop(Register reg, const char* s) {
  if (!VerifyOops) { return; }

  // Pass register number to verify_oop_subroutine
  const char* b = NULL;
  {
    ResourceMark rm;
    stringStream ss;
    ss.print("verify_oop: %s: %s", reg->name(), s);
    b = code_string(ss.as_string());
  }
  BLOCK_COMMENT("verify_oop {");

  push_reg(RegSet::of(lr, t0, t1, c_rarg0), sp);

  mv(c_rarg0, reg); // c_rarg0 : x10
  if(b != NULL) {
    li(t0, (uintptr_t)(address)b);
  } else {
    ShouldNotReachHere();
  }

  // call indirectly to solve generation ordering problem
  int32_t offset = 0;
  la_patchable(t1, ExternalAddress(StubRoutines::verify_oop_subroutine_entry_address()), offset);
  ld(t1, Address(t1, offset));
  jalr(t1);

  pop_reg(RegSet::of(lr, t0, t1, c_rarg0), sp);

  BLOCK_COMMENT("} verify_oop");
}

void MacroAssembler::verify_oop_addr(Address addr, const char* s) {
  if (!VerifyOops) {
    return;
  }

  const char* b = NULL;
  {
    ResourceMark rm;
    stringStream ss;
    ss.print("verify_oop_addr: %s", s);
    b = code_string(ss.as_string());
  }
  BLOCK_COMMENT("verify_oop_addr {");

  push_reg(RegSet::of(lr, t0, t1, c_rarg0), sp);

  if (addr.uses(sp)) {
    la(x10, addr);
    ld(x10, Address(x10, 4 * wordSize));
  } else {
    ld(x10, addr);
  }
  if(b != NULL) {
    li(t0, (uintptr_t)(address)b);
  } else {
    ShouldNotReachHere();
  }

  // call indirectly to solve generation ordering problem
  int32_t offset = 0;
  la_patchable(t1, ExternalAddress(StubRoutines::verify_oop_subroutine_entry_address()), offset);
  ld(t1, Address(t1, offset));
  jalr(t1);

  pop_reg(RegSet::of(lr, t0, t1, c_rarg0), sp);

  BLOCK_COMMENT("} verify_oop_addr");
}

Address MacroAssembler::argument_address(RegisterOrConstant arg_slot,
                                         int extra_slot_offset) {
  // cf. TemplateTable::prepare_invoke(), if (load_receiver).
  int stackElementSize = Interpreter::stackElementSize;
  int offset = Interpreter::expr_offset_in_bytes(extra_slot_offset+0);
#ifdef ASSERT
  int offset1 = Interpreter::expr_offset_in_bytes(extra_slot_offset+1);
  assert(offset1 - offset == stackElementSize, "correct arithmetic");
#endif
  if (arg_slot.is_constant()) {
    return Address(esp, arg_slot.as_constant() * stackElementSize + offset);
  } else {
    assert_different_registers(t0, arg_slot.as_register());
    slli(t0, arg_slot.as_register(), exact_log2(stackElementSize));
    add(t0, esp, t0);
    return Address(t0, offset);
  }
}

#ifndef PRODUCT
extern "C" void findpc(intptr_t x);
#endif

void MacroAssembler::debug64(char* msg, int64_t pc, int64_t regs[])
{
  // In order to get locks to work, we need to fake a in_VM state
  if (ShowMessageBoxOnError) {
    JavaThread* thread = JavaThread::current();
    JavaThreadState saved_state = thread->thread_state();
    thread->set_thread_state(_thread_in_vm);
#ifndef PRODUCT
    if (CountBytecodes || TraceBytecodes || StopInterpreterAt) {
      ttyLocker ttyl;
      BytecodeCounter::print();
    }
#endif
    if (os::message_box(msg, "Execution stopped, print registers?")) {
      ttyLocker ttyl;
      tty->print_cr(" pc = 0x%016lx", pc);
#ifndef PRODUCT
      tty->cr();
      findpc(pc);
      tty->cr();
#endif
      tty->print_cr(" x0 = 0x%016lx", regs[0]);
      tty->print_cr(" x1 = 0x%016lx", regs[1]);
      tty->print_cr(" x2 = 0x%016lx", regs[2]);
      tty->print_cr(" x3 = 0x%016lx", regs[3]);
      tty->print_cr(" x4 = 0x%016lx", regs[4]);
      tty->print_cr(" x5 = 0x%016lx", regs[5]);
      tty->print_cr(" x6 = 0x%016lx", regs[6]);
      tty->print_cr(" x7 = 0x%016lx", regs[7]);
      tty->print_cr(" x8 = 0x%016lx", regs[8]);
      tty->print_cr(" x9 = 0x%016lx", regs[9]);
      tty->print_cr("x10 = 0x%016lx", regs[10]);
      tty->print_cr("x11 = 0x%016lx", regs[11]);
      tty->print_cr("x12 = 0x%016lx", regs[12]);
      tty->print_cr("x13 = 0x%016lx", regs[13]);
      tty->print_cr("x14 = 0x%016lx", regs[14]);
      tty->print_cr("x15 = 0x%016lx", regs[15]);
      tty->print_cr("x16 = 0x%016lx", regs[16]);
      tty->print_cr("x17 = 0x%016lx", regs[17]);
      tty->print_cr("x18 = 0x%016lx", regs[18]);
      tty->print_cr("x19 = 0x%016lx", regs[19]);
      tty->print_cr("x20 = 0x%016lx", regs[20]);
      tty->print_cr("x21 = 0x%016lx", regs[21]);
      tty->print_cr("x22 = 0x%016lx", regs[22]);
      tty->print_cr("x23 = 0x%016lx", regs[23]);
      tty->print_cr("x24 = 0x%016lx", regs[24]);
      tty->print_cr("x25 = 0x%016lx", regs[25]);
      tty->print_cr("x26 = 0x%016lx", regs[26]);
      tty->print_cr("x27 = 0x%016lx", regs[27]);
      tty->print_cr("x28 = 0x%016lx", regs[28]);
      tty->print_cr("x30 = 0x%016lx", regs[30]);
      tty->print_cr("x31 = 0x%016lx", regs[31]);
      BREAKPOINT;
    }
    ThreadStateTransition::transition(thread, _thread_in_vm, saved_state);
  } else {
    ttyLocker ttyl;
    ::tty->print_cr("=============== DEBUG MESSAGE: %s ================\n", msg);
    assert(false, "DEBUG MESSAGE: %s", msg);
  }
}

void MacroAssembler::resolve_jobject(Register value, Register thread, Register tmp) {
  Label done, not_weak;
  beqz(value, done);           // Use NULL as-is.

  // Test for jweak tag.
  andi(t0, value, JNIHandles::weak_tag_mask);
  beqz(t0, not_weak);

  // Resolve jweak.
  access_load_at(T_OBJECT, IN_NATIVE | ON_PHANTOM_OOP_REF, value,
                 Address(value, -JNIHandles::weak_tag_value), tmp, thread);
  verify_oop(value);
  j(done);

  bind(not_weak);
  // Resolve (untagged) jobject.
  access_load_at(T_OBJECT, IN_NATIVE, value, Address(value, 0), tmp, thread);
  verify_oop(value);
  bind(done);
}

void MacroAssembler::stop(const char* msg) {
  address ip = pc();
  pusha();
  if(msg != NULL && ip != NULL) {
    li(c_rarg0, (uintptr_t)(address)msg);
    li(c_rarg1, (uintptr_t)(address)ip);
  } else {
    ShouldNotReachHere();
  }
  mv(c_rarg2, sp);
  mv(c_rarg3, CAST_FROM_FN_PTR(address, MacroAssembler::debug64));
  jalr(c_rarg3);
  ebreak();
}

void MacroAssembler::unimplemented(const char* what) {
  const char* buf = NULL;
  {
    ResourceMark rm;
    stringStream ss;
    ss.print("unimplemented: %s", what);
    buf = code_string(ss.as_string());
  }
  stop(buf);
}

void MacroAssembler::emit_static_call_stub() {
  // CompiledDirectStaticCall::set_to_interpreted knows the
  // exact layout of this stub.

  ifence();

  mov_metadata(xmethod, (Metadata*)NULL);

  // Jump to the entry point of the i2c stub.
  int32_t offset = 0;
  movptr_with_offset(t0, 0, offset);
  jalr(x0, t0, offset);
}
void MacroAssembler::call_VM_leaf_base(address entry_point,
                                       int number_of_arguments,
                                       Label *retaddr) {
  call_native_base(entry_point, retaddr);
  ifence();
}

void MacroAssembler::call_native(address entry_point, Register arg_0) {
  pass_arg0(this, arg_0);
  call_native_base(entry_point);
}

void MacroAssembler::call_native_base(address entry_point, Label *retaddr) {
  Label E, L;
  int32_t offset = 0;
  push_reg(0x80000040, sp);   // push << t0 & xmethod >> to sp
  movptr_with_offset(t0, entry_point, offset);
  jalr(x1, t0, offset);
  if (retaddr != NULL) {
    bind(*retaddr);
  }
  pop_reg(0x80000040, sp);   // pop << t0 & xmethod >> from sp
}

void MacroAssembler::call_VM_leaf(address entry_point, int number_of_arguments) {
  call_VM_leaf_base(entry_point, number_of_arguments);
}

void MacroAssembler::call_VM_leaf(address entry_point, Register arg_0) {
  pass_arg0(this, arg_0);
  call_VM_leaf_base(entry_point, 1);
}

void MacroAssembler::call_VM_leaf(address entry_point, Register arg_0, Register arg_1) {
  pass_arg0(this, arg_0);
  pass_arg1(this, arg_1);
  call_VM_leaf_base(entry_point, 2);
}

void MacroAssembler::call_VM_leaf(address entry_point, Register arg_0,
                                  Register arg_1, Register arg_2) {
  pass_arg0(this, arg_0);
  pass_arg1(this, arg_1);
  pass_arg2(this, arg_2);
  call_VM_leaf_base(entry_point, 3);
}

void MacroAssembler::super_call_VM_leaf(address entry_point, Register arg_0) {
  pass_arg0(this, arg_0);
  MacroAssembler::call_VM_leaf_base(entry_point, 1);
}

void MacroAssembler::super_call_VM_leaf(address entry_point, Register arg_0, Register arg_1) {

  assert(arg_0 != c_rarg1, "smashed arg");
  pass_arg1(this, arg_1);
  pass_arg0(this, arg_0);
  MacroAssembler::call_VM_leaf_base(entry_point, 2);
}

void MacroAssembler::super_call_VM_leaf(address entry_point, Register arg_0, Register arg_1, Register arg_2) {
  assert(arg_0 != c_rarg2, "smashed arg");
  assert(arg_1 != c_rarg2, "smashed arg");
  pass_arg2(this, arg_2);
  assert(arg_0 != c_rarg1, "smashed arg");
  pass_arg1(this, arg_1);
  pass_arg0(this, arg_0);
  MacroAssembler::call_VM_leaf_base(entry_point, 3);
}

void MacroAssembler::super_call_VM_leaf(address entry_point, Register arg_0, Register arg_1, Register arg_2, Register arg_3) {
  assert(arg_0 != c_rarg3, "smashed arg");
  assert(arg_1 != c_rarg3, "smashed arg");
  assert(arg_2 != c_rarg3, "smashed arg");
  pass_arg3(this, arg_3);
  assert(arg_0 != c_rarg2, "smashed arg");
  assert(arg_1 != c_rarg2, "smashed arg");
  pass_arg2(this, arg_2);
  assert(arg_0 != c_rarg1, "smashed arg");
  pass_arg1(this, arg_1);
  pass_arg0(this, arg_0);
  MacroAssembler::call_VM_leaf_base(entry_point, 4);
}

void MacroAssembler::nop() {
  addi(x0, x0, 0);
}

void MacroAssembler::mv(Register Rd, Register Rs) {
  if (Rd != Rs) {
    addi(Rd, Rs, 0);
  }
}

void MacroAssembler::notr(Register Rd, Register Rs) {
  xori(Rd, Rs, -1);
}

void MacroAssembler::neg(Register Rd, Register Rs) {
  sub(Rd, x0, Rs);
}

void MacroAssembler::negw(Register Rd, Register Rs) {
  subw(Rd, x0, Rs);
}

void MacroAssembler::sext_w(Register Rd, Register Rs) {
  addiw(Rd, Rs, 0);
}

void MacroAssembler::seqz(Register Rd, Register Rs) {
  sltiu(Rd, Rs, 1);
}

void MacroAssembler::snez(Register Rd, Register Rs) {
  sltu(Rd, x0, Rs);
}

void MacroAssembler::sltz(Register Rd, Register Rs) {
  slt(Rd, Rs, x0);
}

void MacroAssembler::sgtz(Register Rd, Register Rs) {
  slt(Rd, x0, Rs);
}

void MacroAssembler::fmv_s(FloatRegister Rd, FloatRegister Rs) {
  if (Rd != Rs) {
    fsgnj_s(Rd, Rs, Rs);
  }
}

void MacroAssembler::fabs_s(FloatRegister Rd, FloatRegister Rs) {
  fsgnjx_s(Rd, Rs, Rs);
}

void MacroAssembler::fneg_s(FloatRegister Rd, FloatRegister Rs) {
  fsgnjn_s(Rd, Rs, Rs);
}

void MacroAssembler::fmv_d(FloatRegister Rd, FloatRegister Rs) {
  if (Rd != Rs) {
    fsgnj_d(Rd, Rs, Rs);
  }
}

void MacroAssembler::fabs_d(FloatRegister Rd, FloatRegister Rs) {
  fsgnjx_d(Rd, Rs, Rs);
}

void MacroAssembler::fneg_d(FloatRegister Rd, FloatRegister Rs) {
  fsgnjn_d(Rd, Rs, Rs);
}

void MacroAssembler::la(Register Rd, const address &dest) {
  int64_t offset = dest - pc();
  if (is_offset_in_range(offset, 32)) {
    auipc(Rd, (int32_t)offset + 0x800);  //0x800, Note:the 11th sign bit
    addi(Rd, Rd, ((int64_t)offset << 52) >> 52);
  } else {
    movptr(Rd, dest);
  }
}

void MacroAssembler::la(Register Rd, const Address &adr) {
  InstructionMark im(this);
  code_section()->relocate(inst_mark(), adr.rspec());
  relocInfo::relocType rtype = adr.rspec().reloc()->type();

  switch(adr.getMode()) {
    case Address::literal: {
      if (rtype == relocInfo::none) {
        li(Rd, (intptr_t)(adr.target()));
      } else {
        movptr(Rd, adr.target());
      }
      break;
    }
    case Address::base_plus_offset:{
      int32_t offset = 0;
      baseOffset(Rd, adr, offset);
      addi(Rd, Rd, offset);
      break;
    }
    default:
      ShouldNotReachHere();
  }
}

void MacroAssembler::la(Register Rd, Label &label) {
  la(Rd, target(label));
}

#define INSN(NAME)                                                                    \
  void MacroAssembler::NAME##z(Register Rs, const address &dest) {                    \
    NAME(Rs, zr, dest);                                                               \
  }                                                                                   \
  void MacroAssembler::NAME##z(Register Rs, Label &l, bool is_far) {                  \
    NAME(Rs, zr, l, is_far);                                                          \
  }                                                                                   \

  INSN(beq);
  INSN(bne);
  INSN(blt);
  INSN(ble);
  INSN(bge);
  INSN(bgt);

#undef INSN

// Float compare branch instructions

#define INSN(NAME, FLOATCMP, BRANCH)                                                                                                     \
  void MacroAssembler::float_##NAME(FloatRegister Rs1, FloatRegister Rs2, Label &l, bool is_far, bool is_unordered) {                    \
    FLOATCMP##_s(t0, Rs1, Rs2);                                                                                                          \
    BRANCH(t0, l, is_far);                                                                                                               \
  }                                                                                                                                      \
  void MacroAssembler::double_##NAME(FloatRegister Rs1, FloatRegister Rs2, Label &l, bool is_far, bool is_unordered) {                   \
    FLOATCMP##_d(t0, Rs1, Rs2);                                                                                                          \
    BRANCH(t0, l, is_far);                                                                                                               \
  }

  INSN(beq, feq, bnez);
  INSN(bne, feq, beqz);

#undef INSN


#define INSN(NAME, FLOATCMP1, FLOATCMP2)                                                 \
  void MacroAssembler::float_##NAME(FloatRegister Rs1, FloatRegister Rs2, Label &l,      \
                                    bool is_far, bool is_unordered) {                    \
    if(is_unordered) {                                                                   \
      /* jump if either source is NaN or condition is expected */                        \
      FLOATCMP2##_s(t0, Rs2, Rs1);                                                       \
      beqz(t0, l, is_far);                                                               \
    } else {                                                                             \
      /* jump if no NaN in source and condition is expected */                           \
      FLOATCMP1##_s(t0, Rs1, Rs2);                                                       \
      bnez(t0, l, is_far);                                                               \
    }                                                                                    \
  }                                                                                      \
  void MacroAssembler::double_##NAME(FloatRegister Rs1, FloatRegister Rs2, Label &l,     \
                                     bool is_far, bool is_unordered) {                   \
    if(is_unordered) {                                                                   \
      /* jump if either source is NaN or condition is expected */                        \
      FLOATCMP2##_d(t0, Rs2, Rs1);                                                       \
      beqz(t0, l, is_far);                                                               \
    } else {                                                                             \
      /* jump if no NaN in source and condition is expected */                           \
      FLOATCMP1##_d(t0, Rs1, Rs2);                                                       \
      bnez(t0, l, is_far);                                                               \
    }                                                                                    \
  }

  INSN(ble, fle, flt);
  INSN(blt, flt, fle);

#undef INSN

#define INSN(NAME, CMP)                                                                  \
  void MacroAssembler::float_##NAME(FloatRegister Rs1, FloatRegister Rs2, Label &l,      \
                                    bool is_far, bool is_unordered) {                    \
    float_##CMP(Rs2, Rs1, l, is_far, is_unordered);                                      \
  }                                                                                      \
  void MacroAssembler::double_##NAME(FloatRegister Rs1, FloatRegister Rs2, Label &l,     \
                                     bool is_far, bool is_unordered) {                   \
    double_##CMP(Rs2, Rs1, l, is_far, is_unordered);                         \
  }

  INSN(bgt, blt);
  INSN(bge, ble);

#undef INSN


#define INSN(NAME, CSR)                       \
  void MacroAssembler::NAME(Register Rd) {    \
    csrr(Rd, CSR);                            \
  }

  INSN(rdinstret,  CSR_INSTERT);
  INSN(rdcycle,    CSR_CYCLE);
  INSN(rdtime,     CSR_TIME);
  INSN(frcsr,      CSR_FCSR);
  INSN(frrm,       CSR_FRM);
  INSN(frflags,    CSR_FFLAGS);

#undef INSN

void MacroAssembler::csrr(Register Rd, unsigned csr) {
  csrrs(Rd, csr, x0);
}

#define INSN(NAME, OPFUN)                                      \
  void MacroAssembler::NAME(unsigned csr, Register Rs) {       \
    OPFUN(x0, csr, Rs);                                        \
  }

  INSN(csrw, csrrw);
  INSN(csrs, csrrs);
  INSN(csrc, csrrc);

#undef INSN

#define INSN(NAME, OPFUN)                                      \
  void MacroAssembler::NAME(unsigned csr, unsigned imm) {      \
    OPFUN(x0, csr, imm);                                       \
  }

  INSN(csrwi, csrrwi);
  INSN(csrsi, csrrsi);
  INSN(csrci, csrrci);

#undef INSN

#define INSN(NAME, CSR)                                      \
  void MacroAssembler::NAME(Register Rd, Register Rs) {      \
    csrrw(Rd, CSR, Rs);                                      \
  }

  INSN(fscsr,   CSR_FCSR);
  INSN(fsrm,    CSR_FRM);
  INSN(fsflags, CSR_FFLAGS);

#undef INSN

#define INSN(NAME)                              \
  void MacroAssembler::NAME(Register Rs) {      \
    NAME(x0, Rs);                               \
  }

  INSN(fscsr);
  INSN(fsrm);
  INSN(fsflags);

#undef INSN

void MacroAssembler::fsrmi(Register Rd, unsigned imm) {
  guarantee(imm < 5, "Rounding Mode is invalid in Rounding Mode register");
  csrrwi(Rd, CSR_FRM, imm);
}

void MacroAssembler::fsflagsi(Register Rd, unsigned imm) {
   csrrwi(Rd, CSR_FFLAGS, imm);
}

#define INSN(NAME)                             \
  void MacroAssembler::NAME(unsigned imm) {    \
    NAME(x0, imm);                             \
  }

  INSN(fsrmi);
  INSN(fsflagsi);

#undef INSN

#ifdef COMPILER2
typedef void (Assembler::*conditional_branch_insn)(Register op1, Register op2, Label& label, bool is_far);
typedef void (MacroAssembler::*float_conditional_branch_insn)(FloatRegister op1, FloatRegister op2, Label& label,
                                                              bool is_far, bool is_unordered);

static conditional_branch_insn conditional_branches[] =
{
  /* SHORT branches */
  (conditional_branch_insn)&Assembler::beq,
  (conditional_branch_insn)&Assembler::bgt,
  NULL, // BoolTest::overflow
  (conditional_branch_insn)&Assembler::blt,
  (conditional_branch_insn)&Assembler::bne,
  (conditional_branch_insn)&Assembler::ble,
  NULL, // BoolTest::no_overflow
  (conditional_branch_insn)&Assembler::bge,

  /* UNSIGNED branches */
  (conditional_branch_insn)&Assembler::beq,
  (conditional_branch_insn)&Assembler::bgtu,
  NULL,
  (conditional_branch_insn)&Assembler::bltu,
  (conditional_branch_insn)&Assembler::bne,
  (conditional_branch_insn)&Assembler::bleu,
  NULL,
  (conditional_branch_insn)&Assembler::bgeu
};

static float_conditional_branch_insn float_conditional_branches[] =
{
  /* FLOAT SHORT branches */
  (float_conditional_branch_insn)&MacroAssembler::float_beq,
  (float_conditional_branch_insn)&MacroAssembler::float_bgt,
  NULL,  // BoolTest::overflow
  (float_conditional_branch_insn)&MacroAssembler::float_blt,
  (float_conditional_branch_insn)&MacroAssembler::float_bne,
  (float_conditional_branch_insn)&MacroAssembler::float_ble,
  NULL, // BoolTest::no_overflow
  (float_conditional_branch_insn)&MacroAssembler::float_bge,

  /* DOUBLE SHORT branches */
  (float_conditional_branch_insn)&MacroAssembler::double_beq,
  (float_conditional_branch_insn)&MacroAssembler::double_bgt,
  NULL,
  (float_conditional_branch_insn)&MacroAssembler::double_blt,
  (float_conditional_branch_insn)&MacroAssembler::double_bne,
  (float_conditional_branch_insn)&MacroAssembler::double_ble,
  NULL,
  (float_conditional_branch_insn)&MacroAssembler::double_bge
};

void MacroAssembler::cmp_branch(int cmpFlag, Register op1, Register op2, Label& label, bool is_far) {
  assert(cmpFlag >= 0 && cmpFlag < (int)(sizeof(conditional_branches) / sizeof(conditional_branches[0])),
         "invalid conditional branch index");
  (this->*conditional_branches[cmpFlag])(op1, op2, label, is_far);
}

// This is a function should only be used by C2. Flip the unordered when unordered-greater, C2 would use
// unordered-lesser instead of unordered-greater. Finally, commute the result bits at function do_one_bytecode().
void MacroAssembler::float_cmp_branch(int cmpFlag, FloatRegister op1, FloatRegister op2, Label& label, bool is_far) {
  assert(cmpFlag >= 0 && cmpFlag < (int)(sizeof(float_conditional_branches) / sizeof(float_conditional_branches[0])),
         "invalid float conditional branch index");
  int booltest_flag = cmpFlag & ~(MacroAssembler::double_branch_mask);
  (this->*float_conditional_branches[cmpFlag])(op1, op2, label, is_far,
                                               (booltest_flag == (BoolTest::ge) || booltest_flag == (BoolTest::gt)) ? false : true);
}

void MacroAssembler::enc_cmpUEqNeLeGt_imm0_branch(int cmpFlag, Register op1, Label& L, bool is_far) {
  switch (cmpFlag) {
    case BoolTest::eq:
    case BoolTest::le:
      beqz(op1, L, is_far);
      break;
    case BoolTest::ne:
    case BoolTest::gt:
      bnez(op1, L, is_far);
      break;
    default:
      ShouldNotReachHere();
  }
}

void MacroAssembler::enc_cmpEqNe_imm0_branch(int cmpFlag, Register op1, Label& L, bool is_far) {
  switch (cmpFlag) {
    case BoolTest::eq:
      beqz(op1, L, is_far);
      break;
    case BoolTest::ne:
      bnez(op1, L, is_far);
      break;
    default:
      ShouldNotReachHere();
  }
}
#endif // COMPILER2

void MacroAssembler::push_reg(Register Rs)
{
  addi(esp, esp, 0 - wordSize);
  sd(Rs, Address(esp, 0));
}

void MacroAssembler::pop_reg(Register Rd)
{
  ld(Rd, esp, 0);
  addi(esp, esp, wordSize);
}

int MacroAssembler::bitset_to_regs(unsigned int bitset, unsigned char* regs) {
  DEBUG_ONLY(int words_pushed = 0;)

  int count = 0;
  // Sp is x2, and zr is x0, which should not be pushed.
  // If the number of registers is odd, zr is used for stack alignment.Otherwise, it will be ignored.
  bitset &= ~ (1U << 2);
  bitset |= 0x1;

  // Scan bitset to accumulate register pairs
  for (int reg = 31; reg >= 0; reg --) {
    if ((1U << 31) & bitset) {
      regs[count++] = reg;
    }
    bitset <<= 1;
  }
  count &= ~1;  // Only push an even number of regs
  return count;
}

// Push lots of registers in the bit set supplied.  Don't push sp.
// Return the number of words pushed
int MacroAssembler::push_reg(unsigned int bitset, Register stack) {
  DEBUG_ONLY(int words_pushed = 0;)

  unsigned char regs[32];
  int count = bitset_to_regs(bitset, regs);

  if (count) {
    addi(stack, stack, - count * wordSize);
  }
  for (int i = count - 1; i >= 0; i--) {
    sd(as_Register(regs[i]), Address(stack, (count -1 - i) * wordSize));
    DEBUG_ONLY(words_pushed ++;)
  }

  assert(words_pushed == count, "oops, pushed != count");

  return count;
}

int MacroAssembler::pop_reg(unsigned int bitset, Register stack) {
  DEBUG_ONLY(int words_popped = 0;)

  unsigned char regs[32];
  int count = bitset_to_regs(bitset, regs);

  for (int i = count - 1; i >= 0; i--) {
    ld(as_Register(regs[i]), Address(stack, (count -1 - i) * wordSize));
    DEBUG_ONLY(words_popped ++;)
  }

  if (count) {
    addi(stack, stack, count * wordSize);
  }
  assert(words_popped == count, "oops, popped != count");

  return count;
}

int MacroAssembler::bitset_to_fregs(unsigned int bitset, unsigned char* regs) {
  int count = 0;
  // Scan bitset to accumulate register pairs
  for (int reg = 31; reg >= 0; reg--) {
    if ((1U << 31) & bitset) {
      regs[count++] = reg;
    }
    bitset <<= 1;
  }

  return count;
}

// Push float registers in the bitset, except sp.
// Return the number of heapwords pushed.
int MacroAssembler::push_fp(unsigned int bitset, Register stack) {
  int words_pushed = 0;
  unsigned char regs[32];
  int count = bitset_to_fregs(bitset, regs);
  int push_slots = count + (count & 1);

  if (count) {
    addi(stack, stack, -push_slots * wordSize);
  }

  for (int i = count - 1; i >= 0; i--) {
    fsd(as_FloatRegister(regs[i]), Address(stack, (push_slots - 1 - i) * wordSize));
    words_pushed++;
  }

  assert(words_pushed == count, "oops, pushed(%d) != count(%d)", words_pushed, count);
  return count;
}

int MacroAssembler::pop_fp(unsigned int bitset, Register stack) {
  int words_popped = 0;
  unsigned char regs[32];
  int count = bitset_to_fregs(bitset, regs);
  int pop_slots = count + (count & 1);

  for (int i = count - 1; i >= 0; i--) {
    fld(as_FloatRegister(regs[i]), Address(stack, (pop_slots - 1 - i) * wordSize));
    words_popped++;
  }

  if (count) {
    addi(stack, stack, pop_slots * wordSize);
  }

  assert(words_popped == count, "oops, popped(%d) != count(%d)", words_popped, count);
  return count;
}

// CSky specific ldd/lwd/lwud/swd/sdd to merge 2 load or 2 store instructions
// Checks whether current and previous load/store can be merged.
// Returns true if it can be merged, else false.
bool MacroAssembler::ldst_can_merge(Register rt,
                                    const Address &adr,
                                    size_t cur_size_in_bytes,
                                    bool is_store,
                                    bool is_unsigned) const {
  address prev = pc() - NativeInstruction::instruction_size;
  address last = code()->last_insn();

  assert( !is_unsigned || (!is_store && cur_size_in_bytes == 4), "unsigned load 4 bytes only");

  if (last == NULL || !(NativeInstruction::is_load_at(last) || NativeInstruction::is_store_at(last))) {
    return false;
  }

  if (adr.getMode() != Address::base_plus_offset || prev != last) {
    return false;
  }

  // x0 base is not supported
  if (adr.base() == x0) {
    return false;
  }

  NativeLdSt* prev_ldst = NativeLdSt_at(prev);
  size_t prev_size_in_bytes = prev_ldst->size_in_bytes();

  assert(prev_size_in_bytes == 4 || prev_size_in_bytes == 8, "only supports 64/32bit merging.");
  assert(cur_size_in_bytes == 4 || cur_size_in_bytes == 8, "only supports 64/32bit merging.");

  if (cur_size_in_bytes != prev_size_in_bytes || is_store != prev_ldst->is_store()) {
    return false;
  }

  // Only same base can be merged.
  if (adr.base() != prev_ldst->base()) {
    return false;
  }

  int64_t cur_offset = adr.offset();
  int64_t prev_offset = prev_ldst->offset();
  size_t diff = abs(cur_offset - prev_offset);
  if (diff != prev_size_in_bytes) {
    return false;
  }

  // Following cases can not be merged:
  // ld x2, [x2, #16]
  // ld x3, [x2, #24]
  // or
  // ld x3, [x2, #16]
  // ld x2, [x2, #24]
  // or:
  // ld x2, [x3, #0]
  // ld x2, [x3, #8]
  if (!is_store &&
      (adr.base() == prev_ldst->target() ||
       rt == prev_ldst->target() ||
       rt == prev_ldst->base())) {
    return false;
  }

  int64_t max_offset = 3 * prev_size_in_bytes * 2;
  int64_t min_offset = 0;
  int64_t low_offset = prev_offset > cur_offset ? cur_offset : prev_offset;
  // Offset range must be in ld/st pair instruction's range.
  if (low_offset > max_offset || low_offset < min_offset) {
    return false;
  }

  // alignment check
  if ( (low_offset % (prev_size_in_bytes*2)) != 0 ) {
    return false;
  }

  if ( is_unsigned && !prev_ldst->is_unsigned_load()) {
    return false;
  }
  return true;
}

// Merge current load/store with previous load/store
void MacroAssembler::merge_ldst(Register rt,
                                const Address &adr,
                                size_t cur_size_in_bytes,
                                bool is_store,
                                bool is_unsigned) {

  assert(ldst_can_merge(rt, adr, cur_size_in_bytes, is_store, is_unsigned) == true, "cur and prev must be able to be merged.");

  Register rt_low, rt_high, rt_base;
  address prev = pc() - NativeInstruction::instruction_size;
  NativeLdSt* prev_ldst = NativeLdSt_at(prev);

  int32_t offset;
  if (adr.offset() < prev_ldst->offset()) {
    offset = adr.offset();
    rt_low = rt;
    rt_high = prev_ldst->target();
  } else {
    offset = prev_ldst->offset();
    rt_low = prev_ldst->target();
    rt_high = rt;
  }
  rt_base = prev_ldst->base();

  // Overwrite previous generated binary.
  code_section()->set_end(prev);

  const size_t sz = prev_ldst->size_in_bytes();
  assert(sz == 8 || sz == 4, "only supports 64/32bit merging.");
  assert((offset % (sz*2)) == 0, "unaligned offset");
  if (!is_store) {
    if (sz == 8) {
      BLOCK_COMMENT("merged load pair: ldd");
      ldd(rt_low, rt_high, rt_base, offset >> 4);
    } else {
      if (is_unsigned) {
        BLOCK_COMMENT("merged load pair: lwd");
        lwd(rt_low, rt_high, rt_base, offset >> 3);
      } else {
        BLOCK_COMMENT("merged load pair: lwud");
        lwud(rt_low, rt_high, rt_base, offset >> 3);
      }
    }
  } else {
    if (sz == 8) {
      BLOCK_COMMENT("merged store pair: sdd");
      sdd(rt_low, rt_high, rt_base, offset >> 4);
    } else {
      BLOCK_COMMENT("merged store pair: swd");
      swd(rt_low, rt_high, rt_base, offset >> 3);
    }
  }
}

bool MacroAssembler::try_merge_ldst(Register rt, const Address& adr, size_t size_in_bytes, bool is_store, bool is_unsigned) {
  // it's a CSky specific feature
  if (!UseCSky) return false;

  if (ldst_can_merge(rt, adr, size_in_bytes, is_store, is_unsigned)) {
    merge_ldst(rt, adr, size_in_bytes, is_store, is_unsigned);
    code()->clear_last_insn();
    return true;
  } else {
    assert(size_in_bytes == 8 || size_in_bytes == 4, "only 8 bytes or 4 bytes load/store is supported");
    const uint64_t mask = size_in_bytes - 1;
    if (adr.getMode() == Address::base_plus_offset &&
        (adr.offset() & mask) == 0) { // only supports base_plus_offset
      code()->set_last_insn(pc());
    }
    return false;
  }
}

void MacroAssembler::ld(Register Rx, const Address& adr) {
  if (!try_merge_ldst(Rx, adr, 8,
                      false /* store */, false /* unsigned */)) {
    Assembler::ld(Rx, adr);
  }
}

void MacroAssembler::sd(Register Rw, const Address& adr) {
  if (!try_merge_ldst(Rw, adr, 8,
                      true /* store */, false /* unsigned */)) {
    Assembler::sd(Rw, adr);
  }
}

void MacroAssembler::lw(Register Rx, const Address& adr) {
  if (!try_merge_ldst(Rx, adr, 4,
                      false /* store */, false /* unsigned */)) {
    Assembler::lw(Rx, adr);
  }
}

void MacroAssembler::lwu(Register Rx, const Address& adr) {
  if (!try_merge_ldst(Rx, adr, 4,
                      false /* store */, true /* unsigned */)) {
    Assembler::lwu(Rx, adr);
  }
}

void MacroAssembler::sw(Register Rw, const Address& adr) {
  if (!try_merge_ldst(Rw, adr, 4,
                      true /* store */, false /* unsigned */)) {
    Assembler::sw(Rw, adr);
  }
}

void MacroAssembler::push_call_clobbered_registers_except(RegSet exclude) {
  // Push integer registers x7, x10-x17, x28-x31.
  push_reg(RegSet::of(x7) + RegSet::range(x10, x17) + RegSet::range(x28, x31) - exclude, sp);

  // Push float registers f0-f7, f10-f17, f28-f31.
  addi(sp, sp, - wordSize * 20);
  int offset = 0;
  for (int i = 0; i < 32; i++) {
    if (i <= f7->encoding() || i >= f28->encoding() || (i >= f10->encoding() && i <= f17->encoding())) {
      fsd(as_FloatRegister(i), Address(sp, wordSize * (offset ++)));
    }
  }
}

void MacroAssembler::pop_call_clobbered_registers_except(RegSet exclude) {
  int offset = 0;
  for (int i = 0; i < 32; i++) {
    if (i <= f7->encoding() || i >= f28->encoding() || (i >= f10->encoding() && i <= f17->encoding())) {
      fld(as_FloatRegister(i), Address(sp, wordSize * (offset ++)));
    }
  }
  addi(sp, sp, wordSize * 20);

  pop_reg(RegSet::of(x7) + RegSet::range(x10, x17) + RegSet::range(x28, x31) - exclude, sp);
}

// Push all the integer registers, except zr(x0) & sp(x2).
void MacroAssembler::pusha() {
  push_reg(0xfffffffa, sp);
}

void MacroAssembler::popa() {
  pop_reg(0xfffffffa, sp);
}

void MacroAssembler::push_CPU_state() {
  // integer registers, except zr(x0) & ra(x1) & sp(x2)
  push_reg(0xfffffff8, sp);

  // float registers
  addi(sp, sp, - 32 * wordSize);
  for (int i = 0; i < 32; i++) {
    fsd(as_FloatRegister(i), Address(sp, i * wordSize));
  }
}

void MacroAssembler::pop_CPU_state() {
  // float registers
  for (int i = 0; i < 32; i++) {
    fld(as_FloatRegister(i), Address(sp, i * wordSize));
  }
  addi(sp, sp, 32 * wordSize);

  // integer registers, except zr(x0) & ra(x1) & sp(x2)
  pop_reg(0xfffffff8, sp);
}

static int patch_offset_in_jal(address branch, int64_t offset) {
  assert(is_imm_in_range(offset, 20, 1), "offset is too large to be patched in one jal insrusction!\n");
  Assembler::patch(branch, 31, 31, (offset >> 20) & 0x1);                       // offset[20]    ==> branch[31]
  Assembler::patch(branch, 30, 21, (offset >> 1)  & 0x3ff);                     // offset[10:1]  ==> branch[30:21]
  Assembler::patch(branch, 20, 20, (offset >> 11) & 0x1);                       // offset[11]    ==> branch[20]
  Assembler::patch(branch, 19, 12, (offset >> 12) & 0xff);                      // offset[19:12] ==> branch[19:12]
  return NativeInstruction::instruction_size;                                   // only one instruction
}

static int patch_offset_in_conditional_branch(address branch, int64_t offset) {
  assert(is_imm_in_range(offset, 12, 1), "offset is too large to be patched in one beq/bge/bgeu/blt/bltu/bne insrusction!\n");
  Assembler::patch(branch, 31, 31, (offset >> 12) & 0x1);                       // offset[12]    ==> branch[31]
  Assembler::patch(branch, 30, 25, (offset >> 5)  & 0x3f);                      // offset[10:5]  ==> branch[30:25]
  Assembler::patch(branch, 7,  7,  (offset >> 11) & 0x1);                       // offset[11]    ==> branch[7]
  Assembler::patch(branch, 11, 8,  (offset >> 1)  & 0xf);                       // offset[4:1]   ==> branch[11:8]
  return NativeInstruction::instruction_size;                                   // only one instruction
}

static int patch_offset_in_pc_relative(address branch, int64_t offset) {
  const int PC_RELATIVE_INSTRUCTION_NUM = 2;                                    // auipc, addi/jalr/load
  Assembler::patch(branch, 31, 12, ((offset + 0x800) >> 12) & 0xfffff);         // Auipc.          offset[31:12]  ==> branch[31:12]
  Assembler::patch(branch + 4, 31, 20, offset & 0xfff);                         // Addi/Jalr/Load. offset[11:0]   ==> branch[31:20]
  return PC_RELATIVE_INSTRUCTION_NUM * NativeInstruction::instruction_size;
}

static int patch_addr_in_movptr(address branch, address target) {
  const int MOVPTR_INSTRUCTIONS_NUM = 6;                                        // lui + addi + slli + addi + slli + addi/jalr/load
  int32_t lower = ((intptr_t)target << 36) >> 36;
  int64_t upper = ((intptr_t)target - lower) >> 28;
  Assembler::patch(branch + 0,  31, 12, upper & 0xfffff);                       // Lui.             target[47:28] + target[27] ==> branch[31:12]
  Assembler::patch(branch + 4,  31, 20, (lower >> 16) & 0xfff);                 // Addi.            target[27:16] ==> branch[31:20]
  Assembler::patch(branch + 12, 31, 20, (lower >> 5) & 0x7ff);                  // Addi.            target[15: 5] ==> branch[31:20]
  Assembler::patch(branch + 20, 31, 20, lower & 0x1f);                          // Addi/Jalr/Load.  target[ 4: 0] ==> branch[31:20]
  return MOVPTR_INSTRUCTIONS_NUM * NativeInstruction::instruction_size;
}

static int patch_imm_in_li64(address branch, address target) {
  const int LI64_INSTRUCTIONS_NUM = 8;                                          // lui + addi + slli + addi + slli + addi + slli + addi
  int64_t lower = (intptr_t)target & 0xffffffff;
  lower = lower - ((lower << 44) >> 44);
  int64_t tmp_imm = ((uint64_t)((intptr_t)target & 0xffffffff00000000)) + (uint64_t)lower;
  int32_t upper =  (tmp_imm - (int32_t)lower) >> 32;
  int64_t tmp_upper = upper, tmp_lower = upper;
  tmp_lower = (tmp_lower << 52) >> 52;
  tmp_upper -= tmp_lower;
  tmp_upper >>= 12;
  // Load upper 32 bits. Upper = target[63:32], but if target[31] = 1 or (target[31:28] == 0x7ff && target[19] == 1),
  // upper = target[63:32] + 1.
  Assembler::patch(branch + 0,  31, 12, tmp_upper & 0xfffff);                       // Lui.
  Assembler::patch(branch + 4,  31, 20, tmp_lower & 0xfff);                         // Addi.
  // Load the rest 32 bits.
  Assembler::patch(branch + 12, 31, 20, ((int32_t)lower >> 20) & 0xfff);            // Addi.
  Assembler::patch(branch + 20, 31, 20, (((intptr_t)target << 44) >> 52) & 0xfff);  // Addi.
  Assembler::patch(branch + 28, 31, 20, (intptr_t)target & 0xff);                   // Addi.
  return LI64_INSTRUCTIONS_NUM * NativeInstruction::instruction_size;
}

static int patch_imm_in_li32(address branch, int32_t target) {
  const int LI32_INSTRUCTIONS_NUM = 2;                                          // lui + addiw
  int64_t upper = (intptr_t)target;
  int32_t lower = (((int32_t)target) << 20) >> 20;
  upper -= lower;
  upper = (int32_t)upper;
  Assembler::patch(branch + 0,  31, 12, (upper >> 12) & 0xfffff);               // Lui.
  Assembler::patch(branch + 4,  31, 20, lower & 0xfff);                         // Addiw.
  return LI32_INSTRUCTIONS_NUM * NativeInstruction::instruction_size;
}

static long get_offset_of_jal(address insn_addr) {
  assert_cond(insn_addr != NULL);
  long offset = 0;
  unsigned insn = *(unsigned*)insn_addr;
  long val = (long)Assembler::sextract(insn, 31, 12);
  offset |= ((val >> 19) & 0x1) << 20;
  offset |= (val & 0xff) << 12;
  offset |= ((val >> 8) & 0x1) << 11;
  offset |= ((val >> 9) & 0x3ff) << 1;
  offset = (offset << 43) >> 43;
  return offset;
}

static long get_offset_of_conditional_branch(address insn_addr) {
  long offset = 0;
  assert_cond(insn_addr != NULL);
  unsigned insn = *(unsigned*)insn_addr;
  offset = (long)Assembler::sextract(insn, 31, 31);
  offset = (offset << 12) | (((long)(Assembler::sextract(insn, 7, 7) & 0x1)) << 11);
  offset = offset | (((long)(Assembler::sextract(insn, 30, 25) & 0x3f)) << 5);
  offset = offset | (((long)(Assembler::sextract(insn, 11, 8) & 0xf)) << 1);
  offset = (offset << 41) >> 41;
  return offset;
}

static long get_offset_of_pc_relative(address insn_addr) {
  long offset = 0;
  assert_cond(insn_addr != NULL);
  offset = ((long)(Assembler::sextract(((unsigned*)insn_addr)[0], 31, 12))) << 12;                                  // Auipc.
  offset += ((long)Assembler::sextract(((unsigned*)insn_addr)[1], 31, 20));                                         // Addi/Jalr/Load.
  offset = (offset << 32) >> 32;
  return offset;
}

static address get_target_of_movptr(address insn_addr) {
  assert_cond(insn_addr != NULL);
  intptr_t target_address = (((int64_t)Assembler::sextract(((unsigned*)insn_addr)[0], 31, 12)) & 0xfffff) << 28;    // Lui.
  target_address += ((int64_t)Assembler::sextract(((unsigned*)insn_addr)[1], 31, 20)) << 16;                        // Addi.
  target_address += ((int64_t)Assembler::sextract(((unsigned*)insn_addr)[3], 31, 20)) << 5;                         // Addi.
  target_address += ((int64_t)Assembler::sextract(((unsigned*)insn_addr)[5], 31, 20));                              // Addi/Jalr/Load.
  return (address) target_address;
}

static address get_target_of_li64(address insn_addr) {
  assert_cond(insn_addr != NULL);
  intptr_t target_address = (((int64_t)Assembler::sextract(((unsigned*)insn_addr)[0], 31, 12)) & 0xfffff) << 44;    // Lui.
  target_address += ((int64_t)Assembler::sextract(((unsigned*)insn_addr)[1], 31, 20)) << 32;                        // Addi.
  target_address += ((int64_t)Assembler::sextract(((unsigned*)insn_addr)[3], 31, 20)) << 20;                        // Addi.
  target_address += ((int64_t)Assembler::sextract(((unsigned*)insn_addr)[5], 31, 20)) << 8;                         // Addi.
  target_address += ((int64_t)Assembler::sextract(((unsigned*)insn_addr)[7], 31, 20));                              // Addi.
  return (address)target_address;
}

static address get_target_of_li32(address insn_addr) {
  assert_cond(insn_addr != NULL);
  intptr_t target_address = (((int64_t)Assembler::sextract(((unsigned*)insn_addr)[0], 31, 12)) & 0xfffff) << 12;    // Lui.
  target_address += ((int64_t)Assembler::sextract(((unsigned*)insn_addr)[1], 31, 20));                              // Addiw.
  return (address)target_address;
}

// Patch any kind of instruction; there may be several instructions.
// Return the total length (in bytes) of the instructions.
int MacroAssembler::pd_patch_instruction_size(address branch, address target) {
  assert_cond(branch != NULL);
  int64_t offset = target - branch;
  if (NativeInstruction::is_jal_at(branch)) {                         // jal
    return patch_offset_in_jal(branch, offset);
  } else if (NativeInstruction::is_branch_at(branch)) {               // beq/bge/bgeu/blt/bltu/bne
    return patch_offset_in_conditional_branch(branch, offset);
  } else if (NativeInstruction::is_pc_relative_at(branch)) {          // auipc, addi/jalr/load
    return patch_offset_in_pc_relative(branch, offset);
  } else if (NativeInstruction::is_movptr_at(branch)) {               // movptr
    return patch_addr_in_movptr(branch, target);
  } else if (NativeInstruction::is_li64_at(branch)) {                 // li64
    return patch_imm_in_li64(branch, target);
  } else if (NativeInstruction::is_li32_at(branch)) {                 // li32
    int64_t imm = (intptr_t)target;
    return patch_imm_in_li32(branch, (int32_t)imm);
  } else {
    tty->print_cr("pd_patch_instruction_size: instruction 0x%x could not be patched!\n", *(unsigned*)branch);
    ShouldNotReachHere();
  }
  return -1;
}

address MacroAssembler::target_addr_for_insn(address insn_addr) {
  long offset = 0;
  assert_cond(insn_addr != NULL);
  if (NativeInstruction::is_jal_at(insn_addr)) {                     // jal
    offset = get_offset_of_jal(insn_addr);
  } else if (NativeInstruction::is_branch_at(insn_addr)) {           // beq/bge/bgeu/blt/bltu/bne
    offset = get_offset_of_conditional_branch(insn_addr);
  } else if (NativeInstruction::is_pc_relative_at(insn_addr)) {      // auipc, addi/jalr/load
    offset = get_offset_of_pc_relative(insn_addr);
  } else if (NativeInstruction::is_movptr_at(insn_addr)) {           // movptr
    return get_target_of_movptr(insn_addr);
  } else if (NativeInstruction::is_li64_at(insn_addr)) {             // li64
    return get_target_of_li64(insn_addr);
  } else if (NativeInstruction::is_li32_at(insn_addr)) {             // li32
    return get_target_of_li32(insn_addr);
  } else {
    ShouldNotReachHere();
  }
  return address(((uintptr_t)insn_addr + offset));
}

int MacroAssembler::patch_oop(address insn_addr, address o) {
  // OOPs are either narrow (32 bits) or wide (48 bits).  We encode
  // narrow OOPs by setting the upper 16 bits in the first
  // instruction.
  if (NativeInstruction::is_li32_at(insn_addr)) {
    // Move narrow OOP
    narrowOop n = CompressedOops::encode(cast_to_oop(o));
    return patch_imm_in_li32(insn_addr, (int32_t)n);
  } else if (NativeInstruction::is_movptr_at(insn_addr)) {
    // Move wide OOP
    return patch_addr_in_movptr(insn_addr, o);
  }
  ShouldNotReachHere();
  return -1;
}

void MacroAssembler::reinit_heapbase() {
  if (UseCompressedOops) {
    if (Universe::is_fully_initialized()) {
      if (Universe::narrow_ptrs_base() == NULL) {
        li(xheapbase, 0);
      } else {
        mv(xheapbase, Universe::narrow_ptrs_base());
      }
    } else {
      int32_t offset = 0;
      la_patchable(xheapbase, ExternalAddress((address)Universe::narrow_ptrs_base_addr()), offset);
      ld(xheapbase, Address(xheapbase, offset));
    }
  }
}

void MacroAssembler::mv(Register Rd, int64_t imm64) {
  li(Rd, imm64);
}

void MacroAssembler::mv(Register Rd, int imm) {
  mv(Rd, (int64_t)imm);
}

void MacroAssembler::mvw(Register Rd, int32_t imm32) {
  mv(Rd, imm32);
}

void MacroAssembler::mv(Register Rd, Address dest) {
  assert(dest.getMode() == Address::literal, "Address mode should be Address::literal");
  code_section()->relocate(pc(), dest.rspec());
  movptr(Rd, dest.target());
}

void MacroAssembler::mv(Register Rd, address addr) {
  // Here in case of use with relocation, use fix length instruciton
  // movptr instead of li
  movptr(Rd, addr);
}

void MacroAssembler::mv(Register Rd, RegisterOrConstant src) {
  if (src.is_register()) {
    mv(Rd, src.as_register());
  } else {
    mv(Rd, src.as_constant());
  }
}

void MacroAssembler::andrw(Register Rd, Register Rs1, Register Rs2) {
  andr(Rd, Rs1, Rs2);
  // addw: The result is clipped to 32 bits, then the sign bit is extended,
  // and the result is stored in Rd
  addw(Rd, Rd, zr);
}

void MacroAssembler::orrw(Register Rd, Register Rs1, Register Rs2) {
  orr(Rd, Rs1, Rs2);
  // addw: The result is clipped to 32 bits, then the sign bit is extended,
  // and the result is stored in Rd
  addw(Rd, Rd, zr);
}

void MacroAssembler::xorrw(Register Rd, Register Rs1, Register Rs2) {
  xorr(Rd, Rs1, Rs2);
  // addw: The result is clipped to 32 bits, then the sign bit is extended,
  // and the result is stored in Rd
  addw(Rd, Rd, zr);
}

// Note: load_unsigned_short used to be called load_unsigned_word.
int MacroAssembler::load_unsigned_short(Register dst, Address src) {
  int off = offset();
  lhu(dst, src);
  return off;
}

int MacroAssembler::load_unsigned_byte(Register dst, Address src) {
  int off = offset();
  lbu(dst, src);
  return off;
}

int MacroAssembler::load_signed_short(Register dst, Address src) {
  int off = offset();
  lh(dst, src);
  return off;
}

int MacroAssembler::load_signed_byte(Register dst, Address src) {
  int off = offset();
  lb(dst, src);
  return off;
}

void MacroAssembler::load_sized_value(Register dst, Address src, size_t size_in_bytes, bool is_signed, Register dst2) {
  switch (size_in_bytes) {
    case  8:  ld(dst, src); break;
    case  4:  is_signed ? lw(dst, src) : lwu(dst, src); break;
    case  2:  is_signed ? load_signed_short(dst, src) : load_unsigned_short(dst, src); break;
    case  1:  is_signed ? load_signed_byte( dst, src) : load_unsigned_byte( dst, src); break;
    default:  ShouldNotReachHere();
  }
}

void MacroAssembler::store_sized_value(Address dst, Register src, size_t size_in_bytes, Register src2) {
  switch (size_in_bytes) {
    case  8:  sd(src, dst); break;
    case  4:  sw(src, dst); break;
    case  2:  sh(src, dst); break;
    case  1:  sb(src, dst); break;
    default:  ShouldNotReachHere();
  }
}

void MacroAssembler::reverseb16(Register Rd, Register Rs, Register Rtmp1, Register Rtmp2) {
  // This method is only used for grev16
  // Rd = Rs[47:0] Rs[55:48] Rs[63:56]
  assert_different_registers(Rs, Rtmp1, Rtmp2);
  assert_different_registers(Rd, Rtmp1);
  srli(Rtmp1, Rs, 48);
  andi(Rtmp2, Rtmp1, 0xff);
  slli(Rtmp2, Rtmp2, 8);
  srli(Rtmp1, Rtmp1, 8);
  orr(Rtmp1, Rtmp1, Rtmp2);
  slli(Rd, Rs, 16);
  orr(Rd, Rd, Rtmp1);
}

void MacroAssembler::reverseh32(Register Rd, Register Rs, Register Rtmp1, Register Rtmp2) {
  // This method is only used for grev32
  // Rd[63:0] = Rs[31:0] Rs[47:32] Rs[63:48]
  assert_different_registers(Rs, Rtmp1, Rtmp2);
  assert_different_registers(Rd, Rtmp1);
  srli(Rtmp1, Rs, 32);
  slli(Rtmp2, Rtmp1, 48);
  srli(Rtmp2, Rtmp2, 32);
  srli(Rtmp1, Rtmp1, 16);
  orr(Rtmp1, Rtmp1, Rtmp2);
  slli(Rd, Rs, 32);
  orr(Rd, Rd, Rtmp1);
}

void MacroAssembler::grevh(Register Rd, Register Rs, Register Rtmp) {
  // Reverse bytes in half-word
  // Rd[15:0] = Rs[7:0] Rs[15:8] (sign-extend to 64 bits)
  assert_different_registers(Rs, Rtmp);
  assert_different_registers(Rd, Rtmp);
  srli(Rtmp, Rs, 8);
  andi(Rtmp, Rtmp, 0xFF);
  slli(Rd, Rs, 56);
  srai(Rd, Rd, 48); // sign-extend
  orr(Rd, Rd, Rtmp);
}

void MacroAssembler::grevhu(Register Rd, Register Rs, Register Rtmp) {
  // Reverse bytes in half-word
  // Rd[15:0] = Rs[7:0] Rs[15:8] (zero-extend to 64 bits)
  assert_different_registers(Rs, Rtmp);
  assert_different_registers(Rd, Rtmp);
  srli(Rtmp, Rs, 8);
  andi(Rtmp, Rtmp, 0xFF);
  andi(Rd, Rs, 0xFF);
  slli(Rd, Rd, 8);
  orr(Rd, Rd, Rtmp);
}

void MacroAssembler::grev16w(Register Rd, Register Rs, Register Rtmp1, Register Rtmp2) {
  // Reverse bytes in half-word (32bit)
  // Rd[31:0] = Rs[23:16] Rs[31:24] Rs[7:0] Rs[15:8] (sign-extend to 64 bits)
  assert_different_registers(Rs, Rtmp1, Rtmp2);
  assert_different_registers(Rd, Rtmp1, Rtmp2);
  srli(Rtmp2, Rs, 16);
  grevh(Rtmp2, Rtmp2, Rtmp1);
  grevhu(Rd, Rs, Rtmp1);
  slli(Rtmp2, Rtmp2, 16);
  orr(Rd, Rd, Rtmp2);
}

void MacroAssembler::grev16wu(Register Rd, Register Rs, Register Rtmp1, Register Rtmp2) {
  // Reverse bytes in half-word (32bit)
  // Rd[31:0] = Rs[23:16] Rs[31:24] Rs[7:0] Rs[15:8] (zero-extend to 64 bits)
  assert_different_registers(Rs, Rtmp1, Rtmp2);
  assert_different_registers(Rd, Rtmp1, Rtmp2);
  srli(Rtmp2, Rs, 16);
  grevhu(Rtmp2, Rtmp2, Rtmp1);
  grevhu(Rd, Rs, Rtmp1);
  slli(Rtmp2, Rtmp2, 16);
  orr(Rd, Rd, Rtmp2);
}

void MacroAssembler::grevw(Register Rd, Register Rs, Register Rtmp1, Register Rtmp2) {
  // Reverse bytes in word (32bit)
  // Rd[31:0] = Rs[7:0] Rs[15:8] Rs[23:16] Rs[31:24] (sign-extend to 64 bits)
  assert_different_registers(Rs, Rtmp1, Rtmp2);
  assert_different_registers(Rd, Rtmp1, Rtmp2);
  grev16wu(Rd, Rs, Rtmp1, Rtmp2);
  slli(Rtmp2, Rd, 48);
  srai(Rtmp2, Rtmp2, 32); // sign-extend
  srli(Rd, Rd, 16);
  orr(Rd, Rd, Rtmp2);
}

void MacroAssembler::grevwu(Register Rd, Register Rs, Register Rtmp1, Register Rtmp2) {
  // Reverse bytes in word (32bit)
  // Rd[31:0] = Rs[7:0] Rs[15:8] Rs[23:16] Rs[31:24] (zero-extend to 64 bits)
  assert_different_registers(Rs, Rtmp1, Rtmp2);
  assert_different_registers(Rd, Rtmp1, Rtmp2);
  grev16wu(Rd, Rs, Rtmp1, Rtmp2);
  slli(Rtmp2, Rd, 48);
  srli(Rtmp2, Rtmp2, 32);
  srli(Rd, Rd, 16);
  orr(Rd, Rd, Rtmp2);
}

void MacroAssembler::grev16(Register Rd, Register Rs, Register Rtmp1, Register Rtmp2) {
  // Reverse bytes in half-word (64bit)
  // Rd[63:0] = Rs[55:48] Rs[63:56] Rs[39:32] Rs[47:40] Rs[23:16] Rs[31:24] Rs[7:0] Rs[15:8]
  assert_different_registers(Rs, Rtmp1, Rtmp2);
  assert_different_registers(Rd, Rtmp1, Rtmp2);
  reverseb16(Rd, Rs, Rtmp1, Rtmp2);
  for (int i = 0; i < 3; ++i) {
    reverseb16(Rd, Rd, Rtmp1, Rtmp2);
  }
}

void MacroAssembler::grev32(Register Rd, Register Rs, Register Rtmp1, Register Rtmp2) {
  // Reverse bytes in word (64bit)
  // Rd[63:0] = Rs[39:32] Rs[47:40] Rs[55:48] Rs[63:56] Rs[7:0] Rs[15:8] Rs[23:16] Rs[31:24]
  assert_different_registers(Rs, Rtmp1, Rtmp2);
  assert_different_registers(Rd, Rtmp1, Rtmp2);
  grev16(Rd, Rs, Rtmp1, Rtmp2);
  reverseh32(Rd, Rd, Rtmp1, Rtmp2);
  reverseh32(Rd, Rd, Rtmp1, Rtmp2);
}

void MacroAssembler::grev(Register Rd, Register Rs, Register Rtmp1, Register Rtmp2) {
  // Reverse bytes in double-word (64bit)
  // Rd[63:0] = Rs[7:0] Rs[15:8] Rs[23:16] Rs[31:24] Rs[39:32] Rs[47,40] Rs[55,48] Rs[63:56]
  assert_different_registers(Rs, Rtmp1, Rtmp2);
  assert_different_registers(Rd, Rtmp1, Rtmp2);
  grev32(Rd, Rs, Rtmp1, Rtmp2);
  slli(Rtmp2, Rd, 32);
  srli(Rd, Rd, 32);
  orr(Rd, Rd, Rtmp2);
}

void MacroAssembler::andi(Register Rd, Register Rn, int64_t imm, Register tmp) {
  if (is_imm_in_range(imm, 12, 0)) {
    and_imm12(Rd, Rn, imm);
  } else {
    assert_different_registers(Rn, tmp);
    li(tmp, imm);
    andr(Rd, Rn, tmp);
  }
}

void MacroAssembler::orptr(Address adr, RegisterOrConstant src, Register tmp1, Register tmp2) {
  ld(tmp1, adr);
  if (src.is_register()) {
    orr(tmp1, tmp1, src.as_register());
  } else {
    if(is_imm_in_range(src.as_constant(), 12, 0)) {
      ori(tmp1, tmp1, src.as_constant());
    } else {
      assert_different_registers(tmp1, tmp2);
      li(tmp2, src.as_constant());
      orr(tmp1, tmp1, tmp2);
    }
  }
  sd(tmp1, adr);
}

void MacroAssembler::cmp_klass(Register oop, Register trial_klass, Register tmp, Label &L) {
  if (UseCompressedClassPointers) {
      lwu(tmp, Address(oop, oopDesc::klass_offset_in_bytes()));
    if (Universe::narrow_klass_base() == NULL) {
      slli(tmp, tmp, Universe::narrow_klass_shift());
      beq(trial_klass, tmp, L);
      return;
    } else if (((uint64_t)Universe::narrow_klass_base() & 0xffffffff) == 0
               && Universe::narrow_klass_shift() == 0) {
      // Only the bottom 32 bits matter
      beq(trial_klass, tmp, L);
      return;
    }
    decode_klass_not_null(tmp);
  } else {
    ld(tmp, Address(oop, oopDesc::klass_offset_in_bytes()));
  }
  beq(trial_klass, tmp, L);
}

// Move an oop into a register. immediate is true if we want
// immediate instructions and nmethod entry barriers are not enabled.
// i.e. we are not going to patch this instruction while the code is being
// executed by another thread.
void MacroAssembler::movoop(Register dst, jobject obj, bool immediate) {
  int oop_index;
  if (obj == NULL) {
    oop_index = oop_recorder()->allocate_oop_index(obj);
  } else {
#ifdef ASSERT
    {
      ThreadInVMfromUnknown tiv;
      assert(Universe::heap()->is_in_reserved(JNIHandles::resolve(obj)), "should be real oop");
    }
#endif
    oop_index = oop_recorder()->find_index(obj);
  }
  RelocationHolder rspec = oop_Relocation::spec(oop_index);
  if (!immediate) {
    address dummy = address(uintptr_t(pc()) & -wordSize); // A nearby aligned address
    ld_constant(dst, Address(dummy, rspec));
  } else
    mv(dst, Address((address)obj, rspec));
}

// Move a metadata address into a register.
void MacroAssembler::mov_metadata(Register dst, Metadata* obj) {
  int oop_index;
  if (obj == NULL) {
    oop_index = oop_recorder()->allocate_metadata_index(obj);
  } else {
    oop_index = oop_recorder()->find_index(obj);
  }
  RelocationHolder rspec = metadata_Relocation::spec(oop_index);
  mv(dst, Address((address)obj, rspec));
}

// Writes to stack successive pages until offset reached to check for
// stack overflow + shadow pages.  This clobbers tmp.
void MacroAssembler::bang_stack_size(Register size, Register tmp) {
  assert_different_registers(tmp, size, t0);
  // Bang stack for total size given plus shadow page size.
  // Bang one page at a time because large size can bang beyond yellow and
  // red zones.
  mv(t0, os::vm_page_size());
  Label loop;
  bind(loop);
  sub(tmp, sp, t0);
  subw(size, size, t0);
  sd(size, Address(tmp));
  bgtz(size, loop);

  // Bang down shadow pages too.
  // At this point, (tmp-0) is the last address touched, so don't
  // touch it again.  (It was touched as (tmp-pagesize) but then tmp
  // was post-decremented.)  Skip this address by starting at i=1, and
  // touch a few more pages below.  N.B.  It is important to touch all
  // the way down to and including i=StackShadowPages.
  for (int i = 0; i < (int)(JavaThread::stack_shadow_zone_size() / os::vm_page_size()) - 1; i++) {
    // this could be any sized move but this is can be a debugging crumb
    // so the bigger the better.
    sub(tmp, tmp, os::vm_page_size());
    sd(size, Address(tmp, 0));
  }
}

SkipIfEqual::SkipIfEqual(MacroAssembler* masm, const bool* flag_addr, bool value) {
  assert_cond(masm != NULL);
  int32_t offset = 0;
  _masm = masm;
  _masm->la_patchable(t0, ExternalAddress((address)flag_addr), offset);
  _masm->lbu(t0, Address(t0, offset));
  _masm->beqz(t0, _label);
}

SkipIfEqual::~SkipIfEqual() {
  assert_cond(_masm != NULL);
  _masm->bind(_label);
  _masm = NULL;
}

void MacroAssembler::load_mirror(Register dst, Register method, Register tmp) {
  const int mirror_offset = in_bytes(Klass::java_mirror_offset());
  ld(dst, Address(xmethod, Method::const_offset()));
  ld(dst, Address(dst, ConstMethod::constants_offset()));
  ld(dst, Address(dst, ConstantPool::pool_holder_offset_in_bytes()));
  ld(dst, Address(dst, mirror_offset));
  resolve_oop_handle(dst, tmp);
}

void MacroAssembler::resolve_oop_handle(Register result, Register tmp) {
  // OopHandle::resolve is an indirection.
  assert_different_registers(result, tmp);
  access_load_at(T_OBJECT, IN_NATIVE, result, Address(result, 0), tmp, noreg);
}

void MacroAssembler::access_load_at(BasicType type, DecoratorSet decorators,
                                    Register dst, Address src,
                                    Register tmp1, Register thread_tmp) {
  BarrierSetAssembler *bs = BarrierSet::barrier_set()->barrier_set_assembler();
  decorators = AccessInternal::decorator_fixup(decorators);
  bool as_raw = (decorators & AS_RAW) != 0;
  if (as_raw) {
    bs->BarrierSetAssembler::load_at(this, decorators, type, dst, src, tmp1, thread_tmp);
  } else {
    bs->load_at(this, decorators, type, dst, src, tmp1, thread_tmp);
  }
}

void MacroAssembler::null_check(Register reg, int offset) {
  if (needs_explicit_null_check(offset)) {
    // provoke OS NULL exception if reg = NULL by
    // accessing M[reg] w/o changing any registers
    // NOTE: this is plenty to provoke a segv
    ld(zr, Address(reg, 0));
  } else {
    // nothing to do, (later) access of M[reg + offset]
    // will provoke OS NULL exception if reg = NULL
  }
}

void MacroAssembler::access_store_at(BasicType type, DecoratorSet decorators,
                                     Address dst, Register src,
                                     Register tmp1, Register thread_tmp) {
  BarrierSetAssembler *bs = BarrierSet::barrier_set()->barrier_set_assembler();
  decorators = AccessInternal::decorator_fixup(decorators);
  bool as_raw = (decorators & AS_RAW) != 0;
  if (as_raw) {
    bs->BarrierSetAssembler::store_at(this, decorators, type, dst, src, tmp1, thread_tmp);
  } else {
    bs->store_at(this, decorators, type, dst, src, tmp1, thread_tmp);
  }
}

// Algorithm must match CompressedOops::encode.
void MacroAssembler::encode_heap_oop(Register d, Register s) {
  verify_oop(s, "broken oop in encode_heap_oop");
  if (Universe::narrow_oop_base() == NULL) {
    if (Universe::narrow_oop_shift() != 0) {
      assert (LogMinObjAlignmentInBytes == Universe::narrow_oop_shift(), "decode alg wrong");
      srli(d, s, LogMinObjAlignmentInBytes);
    } else {
      mv(d, s);
    }
  } else {
    Label notNull;
    sub(d, s, xheapbase);
    bgez(d, notNull);
    mv(d, zr);
    bind(notNull);
    if (Universe::narrow_oop_shift() != 0) {
      assert (LogMinObjAlignmentInBytes == Universe::narrow_oop_shift(), "decode alg wrong");
      srli(d, d, Universe::narrow_oop_shift());
    }
  }
}

void MacroAssembler::load_klass(Register dst, Register src) {
  if (UseCompressedClassPointers) {
    lwu(dst, Address(src, oopDesc::klass_offset_in_bytes()));
    decode_klass_not_null(dst);
  } else {
    ld(dst, Address(src, oopDesc::klass_offset_in_bytes()));
  }
}

void MacroAssembler::store_klass(Register dst, Register src) {
  // FIXME: Should this be a store release? concurrent gcs assumes
  // klass length is valid if klass field is not null.
  if (UseCompressedClassPointers) {
    encode_klass_not_null(src);
    sw(src, Address(dst, oopDesc::klass_offset_in_bytes()));
  } else {
    sd(src, Address(dst, oopDesc::klass_offset_in_bytes()));
  }
}

void MacroAssembler::store_klass_gap(Register dst, Register src) {
  if (UseCompressedClassPointers) {
    // Store to klass gap in destination
    sw(src, Address(dst, oopDesc::klass_gap_offset_in_bytes()));
  }
}

void  MacroAssembler::decode_klass_not_null(Register r) {
  decode_klass_not_null(r, r);
}

void MacroAssembler::decode_klass_not_null(Register dst, Register src, Register tmp) {
  assert(UseCompressedClassPointers, "should only be used for compressed headers");

  if (Universe::narrow_klass_base() == NULL) {
    if (Universe::narrow_klass_shift() != 0) {
      assert(LogKlassAlignmentInBytes == Universe::narrow_klass_shift(), "decode alg wrong");
      slli(dst, src, LogKlassAlignmentInBytes);
    } else {
      mv(dst, src);
    }
    return;
  }

  Register xbase = dst;
  if (dst == src) {
    xbase = tmp;
  }

  assert_different_registers(src, xbase);
  li(xbase, (uintptr_t)Universe::narrow_klass_base());
  if (Universe::narrow_klass_shift() != 0) {
    assert(LogKlassAlignmentInBytes == Universe::narrow_klass_shift(), "decode alg wrong");
    assert_different_registers(t0, xbase);
    slli(t0, src, LogKlassAlignmentInBytes);
    add(dst, xbase, t0);
  } else {
    add(dst, xbase, src);
  }
  if (xbase == xheapbase) { reinit_heapbase(); }

}

void MacroAssembler::encode_klass_not_null(Register r) {
  encode_klass_not_null(r, r);
}

void MacroAssembler::encode_klass_not_null(Register dst, Register src, Register tmp) {
  assert(UseCompressedClassPointers, "should only be used for compressed headers");

  if (Universe::narrow_klass_base() == NULL) {
    if (Universe::narrow_klass_shift() != 0) {
      assert(LogKlassAlignmentInBytes == Universe::narrow_klass_shift(), "decode alg wrong");
      srli(dst, src, LogKlassAlignmentInBytes);
    } else {
      mv(dst, src);
    }
    return;
  }

  if (((uint64_t)(uintptr_t)Universe::narrow_klass_base() & 0xffffffff) == 0 &&
      Universe::narrow_klass_shift() == 0) {
    zero_ext(dst, src, 32); // clear upper 32 bits
    return;
  }

  Register xbase = dst;
  if (dst == src) {
    xbase = tmp;
  }

  assert_different_registers(src, xbase);
  li(xbase, (intptr_t)Universe::narrow_klass_base());
  sub(dst, src, xbase);
  if (Universe::narrow_klass_shift() != 0) {
    assert(LogKlassAlignmentInBytes == Universe::narrow_klass_shift(), "decode alg wrong");
    srli(dst, dst, LogKlassAlignmentInBytes);
  }
  if (xbase == xheapbase) {
    reinit_heapbase();
  }
}

void  MacroAssembler::decode_heap_oop_not_null(Register r) {
  decode_heap_oop_not_null(r, r);
}

void MacroAssembler::decode_heap_oop_not_null(Register dst, Register src) {
  assert(UseCompressedOops, "should only be used for compressed headers");
  assert(Universe::heap() != NULL, "java heap should be initialized");
  // Cannot assert, unverified entry point counts instructions (see .ad file)
  // vtableStubs also counts instructions in pd_code_size_limit.
  // Also do not verify_oop as this is called by verify_oop.
  if (Universe::narrow_oop_shift() != 0) {
    assert(LogMinObjAlignmentInBytes == Universe::narrow_oop_shift(), "decode alg wrong");
    slli(dst, src, LogMinObjAlignmentInBytes);
    if (Universe::narrow_oop_base() != NULL) {
      add(dst, xheapbase, dst);
    }
  } else {
    assert(Universe::narrow_oop_base() == NULL, "sanity");
    mv(dst, src);
  }
}

void  MacroAssembler::decode_heap_oop(Register d, Register s) {
  if (Universe::narrow_oop_base() == NULL) {
    if (Universe::narrow_oop_shift() != 0 || d != s) {
      slli(d, s, Universe::narrow_oop_shift());
    }
  } else {
    Label done;
    mv(d, s);
    beqz(s, done);
    slli(d, s, LogMinObjAlignmentInBytes);
    add(d, xheapbase, d);
    bind(done);
  }
  verify_oop(d, "broken oop in decode_heap_oop");
}

void MacroAssembler::store_heap_oop(Address dst, Register src, Register tmp1,
                                    Register thread_tmp, DecoratorSet decorators) {
  access_store_at(T_OBJECT, IN_HEAP | decorators, dst, src, tmp1, thread_tmp);
}

void MacroAssembler::load_heap_oop(Register dst, Address src, Register tmp1,
                                   Register thread_tmp, DecoratorSet decorators) {
  access_load_at(T_OBJECT, IN_HEAP | decorators, dst, src, tmp1, thread_tmp);
}

void MacroAssembler::load_heap_oop_not_null(Register dst, Address src, Register tmp1,
                                            Register thread_tmp, DecoratorSet decorators) {
  access_load_at(T_OBJECT, IN_HEAP | IS_NOT_NULL, dst, src, tmp1, thread_tmp);
}

// Used for storing NULLs.
void MacroAssembler::store_heap_oop_null(Address dst) {
  access_store_at(T_OBJECT, IN_HEAP, dst, noreg, noreg, noreg);
}

int MacroAssembler::corrected_idivl(Register result, Register rs1, Register rs2,
                                    bool want_remainder)
{
  // Full implementation of Java idiv and irem.  The function
  // returns the (pc) offset of the div instruction - may be needed
  // for implicit exceptions.
  //
  // input : rs1: dividend
  //         rs2: divisor
  //
  // result: either
  //         quotient  (= rs1 idiv rs2)
  //         remainder (= rs1 irem rs2)


  int idivl_offset = offset();
  if (!want_remainder) {
    divw(result, rs1, rs2);
  } else {
    remw(result, rs1, rs2); // result = rs1 % rs2;
  }
  return idivl_offset;
}

int MacroAssembler::corrected_idivq(Register result, Register rs1, Register rs2,
                                    bool want_remainder)
{
  // Full implementation of Java ldiv and lrem.  The function
  // returns the (pc) offset of the div instruction - may be needed
  // for implicit exceptions.
  //
  // input : rs1: dividend
  //         rs2: divisor
  //
  // result: either
  //         quotient  (= rs1 idiv rs2)
  //         remainder (= rs1 irem rs2)

  int idivq_offset = offset();
  if (!want_remainder) {
    div(result, rs1, rs2);
  } else {
    rem(result, rs1, rs2); // result = rs1 % rs2;
  }
  return idivq_offset;
}

// Look up the method for a megamorpic invkkeinterface call.
// The target method is determined by <intf_klass, itable_index>.
// The receiver klass is in recv_klass.
// On success, the result will be in method_result, and execution falls through.
// On failure, execution transfers to the given label.
void MacroAssembler::lookup_interface_method(Register recv_klass,
                                             Register intf_klass,
                                             RegisterOrConstant itable_index,
                                             Register method_result,
                                             Register scan_temp,
                                             Label& L_no_such_interface,
                                             bool return_method) {
  assert_different_registers(recv_klass, intf_klass, scan_temp);
  assert_different_registers(method_result, intf_klass, scan_temp);
  assert(recv_klass != method_result || !return_method,
         "recv_klass can be destroyed when mehtid isn't needed");
  assert(itable_index.is_constant() || itable_index.as_register() == method_result,
         "caller must be same register for non-constant itable index as for method");

  // Compute start of first itableOffsetEntry (which is at the end of the vtable).
  int vtable_base = in_bytes(Klass::vtable_start_offset());
  int itentry_off = itableMethodEntry::method_offset_in_bytes();
  int scan_step   = itableOffsetEntry::size() * wordSize;
  int vte_size    = vtableEntry::size_in_bytes();
  assert(vte_size == wordSize, "else adjust times_vte_scale");

  lwu(scan_temp, Address(recv_klass, Klass::vtable_length_offset()));

  // %%% Could store the aligned, prescaled offset in the klassoop.
  slli(scan_temp, scan_temp, 3);
  add(scan_temp, recv_klass, scan_temp);
  add(scan_temp, scan_temp, vtable_base);

  if (return_method) {
    // Adjust recv_klass by scaled itable_index, so we can free itable_index.
    assert(itableMethodEntry::size() * wordSize == wordSize, "adjust the scaling in the code below");
    if (itable_index.is_register()) {
      slli(t0, itable_index.as_register(), 3);
    } else {
      li(t0, itable_index.as_constant() << 3);
    }
    add(recv_klass, recv_klass, t0);
    if (itentry_off) {
      add(recv_klass, recv_klass, itentry_off);
    }
  }

  Label search, found_method;

  ld(method_result, Address(scan_temp, itableOffsetEntry::interface_offset_in_bytes()));
  beq(intf_klass, method_result, found_method);
  bind(search);
  // Check that the previous entry is non-null. A null entry means that
  // the receiver class doens't implement the interface, and wasn't the
  // same as when the caller was compiled.
  beqz(method_result, L_no_such_interface, /* is_far */ true);
  addi(scan_temp, scan_temp, scan_step);
  ld(method_result, Address(scan_temp, itableOffsetEntry::interface_offset_in_bytes()));
  bne(intf_klass, method_result, search);

  bind(found_method);

  // Got a hit.
  if (return_method) {
    lwu(scan_temp, Address(scan_temp, itableOffsetEntry::offset_offset_in_bytes()));
    add(method_result, recv_klass, scan_temp);
    ld(method_result, Address(method_result));
  }
}

// virtual method calling
void MacroAssembler::lookup_virtual_method(Register recv_klass,
                                           RegisterOrConstant vtable_index,
                                           Register method_result) {
  const int base = in_bytes(Klass::vtable_start_offset());
  assert(vtableEntry::size() * wordSize == 8,
         "adjust the scaling in the code below");
  int vtable_offset_in_bytes = base + vtableEntry::method_offset_in_bytes();

  if (vtable_index.is_register()) {
    slli(method_result, vtable_index.as_register(), LogBytesPerWord);
    add(method_result, recv_klass, method_result);
    ld(method_result, Address(method_result, vtable_offset_in_bytes));
  } else {
    vtable_offset_in_bytes += vtable_index.as_constant() * wordSize;
    ld(method_result, form_address(method_result, recv_klass, vtable_offset_in_bytes));
  }
}

void MacroAssembler::membar(uint32_t order_constraint) {
  if (!os::is_MP()) { return; }

  address prev = pc() - NativeMembar::instruction_size;
  address last = code()->last_insn();

  if (last != NULL && nativeInstruction_at(last)->is_membar() && prev == last) {
    NativeMembar *bar = NativeMembar_at(prev);
    // We are merging two memory barrier instructions.  On RISCV we
    // can do this simply by ORing them together.
    bar->set_kind(bar->get_kind() | order_constraint);
    BLOCK_COMMENT("merged membar");
  } else {
    code()->set_last_insn(pc());

    uint32_t predecessor = 0;
    uint32_t successor = 0;

    membar_mask_to_pred_succ(order_constraint, predecessor, successor);
    fence(predecessor, successor);
  }
}

// Form an addres from base + offset in Rd. Rd my or may not
// actually be used: you must use the Address that is returned. It
// is up to you to ensure that the shift provided mathces the size
// of your data.
Address MacroAssembler::form_address(Register Rd, Register base, long byte_offset) {
  if (is_offset_in_range(byte_offset, 12)) { // 12: imm in range 2^12
    return Address(base, byte_offset);
  }

  // Do it the hard way
  mv(Rd, byte_offset);
  add(Rd, base, Rd);
  return Address(Rd);
}

void MacroAssembler::check_klass_subtype(Register sub_klass,
                                         Register super_klass,
                                         Register temp_reg,
                                         Label& L_success) {
  Label L_failure;
  check_klass_subtype_fast_path(sub_klass, super_klass, temp_reg, &L_success, &L_failure, NULL);
  check_klass_subtype_slow_path(sub_klass, super_klass, temp_reg, noreg, &L_success, NULL);
  bind(L_failure);
}

// Write serialization page so VM thread can do a pseudo remote membar.
// We use the current thread pointer to calculate a thread specific
// offset to write to within the page. This minimizes bus traffic
// due to cache line collision.
void MacroAssembler::serialize_memory(Register thread, Register tmp1, Register tmp2) {
  srli(tmp2, thread, os::get_serialize_page_shift_count());

  int mask = os::vm_page_size() - sizeof(int);
  andi(tmp2, tmp2, mask, tmp1);

  add(tmp1, tmp2, (intptr_t)os::get_memory_serialize_page());
  membar(MacroAssembler::AnyAny);
  sw(zr, Address(tmp1));
}

void MacroAssembler::safepoint_poll(Label& slow_path) {
  if (SafepointMechanism::uses_thread_local_poll()) {
    ld(t1, Address(xthread, Thread::polling_page_offset()));
    andi(t0, t1, SafepointMechanism::poll_bit());
    bnez(t0, slow_path);
  } else {
    int32_t offset = 0;
    la_patchable(t0, ExternalAddress(SafepointSynchronize::address_of_state()), offset);
    lwu(t0, Address(t0, offset));
    assert(SafepointSynchronize::_not_synchronized == 0, "rewrite this code");
    bnez(t0, slow_path);
  }
}

// Just like safepoint_poll, but use an acquiring load for thread-
// local polling.
//
// We need an acquire here to ensure that any subsequent load of the
// global SafepointSynchronize::_state flag is ordered after this load
// of the local Thread::_polling page.  We don't want this poll to
// return false (i.e. not safepointing) and a later poll of the global
// SafepointSynchronize::_state spuriously to return true.
//
// This is to avoid a race when we're in a native->Java transition
// racing the code which wakes up from a safepoint.
//
void MacroAssembler::safepoint_poll_acquire(Label& slow_path) {
  if (SafepointMechanism::uses_thread_local_poll()) {
    membar(MacroAssembler::AnyAny);
    ld(t1, Address(xthread, Thread::polling_page_offset()));
    membar(MacroAssembler::LoadLoad | MacroAssembler::LoadStore);
    andi(t0, t1, SafepointMechanism::poll_bit());
    bnez(t0, slow_path);
  } else {
    safepoint_poll(slow_path);
  }
}

void MacroAssembler::cmpxchgptr(Register oldv, Register newv, Register addr, Register tmp,
                                Label &succeed, Label *fail) {
  // oldv holds comparison value
  // newv holds value to write in exchange
  // addr identifies memory word to compare against/update
  Label retry_load, nope;
  bind(retry_load);
  // flush and load exclusive from the memory location
  // and fail if it is not what we expect
  lr_d(tmp, addr, Assembler::aqrl);
  bne(tmp, oldv, nope);
  // if we store+flush with no intervening write tmp wil be zero
  sc_d(tmp, newv, addr, Assembler::rl);
  beqz(tmp, succeed);
  // retry so we only ever return after a load fails to compare
  // ensures we don't return a stale value after a failed write.
  j(retry_load);
  // if the memory word differs we return it in oldv and signal a fail
  bind(nope);
  membar(AnyAny);
  mv(oldv, tmp);
  if (fail != NULL) {
    j(*fail);
  }
}

void MacroAssembler::cmpxchg_obj_header(Register oldv, Register newv, Register obj, Register tmp,
                                        Label &succeed, Label *fail) {
  assert(oopDesc::mark_offset_in_bytes() == 0, "assumption");
  cmpxchgptr(oldv, newv, obj, tmp, succeed, fail);
}

void MacroAssembler::load_reserved(Register addr,
                                   enum operand_size size,
                                   Assembler::Aqrl acquire) {
  switch (size) {
    case int64:
      lr_d(t0, addr, acquire);
      break;
    case int32:
      lr_w(t0, addr, acquire);
      break;
    case uint32:
      lr_w(t0, addr, acquire);
      clear_upper_bits(t0, 32);
      break;
    default:
      ShouldNotReachHere();
  }
}

void MacroAssembler::store_conditional(Register addr,
                                       Register new_val,
                                       enum operand_size size,
                                       Assembler::Aqrl release) {
  switch (size) {
    case int64:
      sc_d(t0, new_val, addr, release);
      break;
    case int32:
    case uint32:
      sc_w(t0, new_val, addr, release);
      break;
    default:
      ShouldNotReachHere();
  }
}


void MacroAssembler::cmpxchg_narrow_value_helper(Register addr, Register expected,
                                                 Register new_val,
                                                 enum operand_size size,
                                                 Register tmp1, Register tmp2, Register tmp3) {
  assert(size == int8 || size == int16, "unsupported operand size");

  Register aligned_addr = t1, shift = tmp1, mask = tmp2, not_mask = tmp3;

  andi(shift, addr, 3);
  slli(shift, shift, 3);

  andi(aligned_addr, addr, ~3);

  if (size == int8) {
    addi(mask, zr, 0xff);
  } else {
    addi(mask, zr, -1);
    zero_ext(mask, mask, registerSize - 16);
  }
  sll(mask, mask, shift);

  xori(not_mask, mask, -1);

  sll(expected, expected, shift);
  andr(expected, expected, mask);

  sll(new_val, new_val, shift);
  andr(new_val, new_val, mask);
}

// cmpxchg_narrow_value will kill t0, t1, expected, new_val and tmps.
// It's designed to implement compare and swap byte/boolean/char/short by lr.w/sc.w,
// which are forced to work with 4-byte aligned address.
void MacroAssembler::cmpxchg_narrow_value(Register addr, Register expected,
                                          Register new_val,
                                          enum operand_size size,
                                          Assembler::Aqrl acquire, Assembler::Aqrl release,
                                          Register result, bool result_as_bool,
                                          Register tmp1, Register tmp2, Register tmp3) {
  Register aligned_addr = t1, shift = tmp1, mask = tmp2, not_mask = tmp3, old = result, tmp = t0;
  assert_different_registers(addr, old, mask, not_mask, new_val, expected, shift, tmp);
  cmpxchg_narrow_value_helper(addr, expected, new_val, size, tmp1, tmp2, tmp3);

  Label retry, fail, done;

  bind(retry);
  lr_w(old, aligned_addr, acquire);
  andr(tmp, old, mask);
  bne(tmp, expected, fail);

  andr(tmp, old, not_mask);
  orr(tmp, tmp, new_val);
  sc_w(tmp, tmp, aligned_addr, release);
  bnez(tmp, retry);

  if (result_as_bool) {
    addi(result, zr, 1);
    j(done);

    bind(fail);
    mv(result, zr);

    bind(done);
  } else {
    andr(tmp, old, mask);

    bind(fail);
    srl(result, tmp, shift);

    if (size == int8) {
      sign_ext(result, result, registerSize - 8);
    } else if (size == int16) {
      sign_ext(result, result, registerSize - 16);
    }
  }
}

// weak_cmpxchg_narrow_value is a weak version of cmpxchg_narrow_value, to implement
// the weak CAS stuff. The major difference is that it just failed when store conditional
// failed.
void MacroAssembler::weak_cmpxchg_narrow_value(Register addr, Register expected,
                                               Register new_val,
                                               enum operand_size size,
                                               Assembler::Aqrl acquire, Assembler::Aqrl release,
                                               Register result,
                                               Register tmp1, Register tmp2, Register tmp3) {
  Register aligned_addr = t1, shift = tmp1, mask = tmp2, not_mask = tmp3, old = result, tmp = t0;
  assert_different_registers(addr, old, mask, not_mask, new_val, expected, shift, tmp);
  cmpxchg_narrow_value_helper(addr, expected, new_val, size, tmp1, tmp2, tmp3);

  Label succ, fail, done;

  lr_w(old, aligned_addr, acquire);
  andr(tmp, old, mask);
  bne(tmp, expected, fail);

  andr(tmp, old, not_mask);
  orr(tmp, tmp, new_val);
  sc_w(tmp, tmp, aligned_addr, release);
  beqz(tmp, succ);

  bind(fail);
  addi(result, zr, 1);
  j(done);

  bind(succ);
  mv(result, zr);

  bind(done);
}

void MacroAssembler::cmpxchg(Register addr, Register expected,
                             Register new_val,
                             enum operand_size size,
                             Assembler::Aqrl acquire, Assembler::Aqrl release,
                             Register result, bool result_as_bool) {
  assert(size != int8 && size != int16, "unsupported operand size");

  Label retry_load, done, ne_done;
  bind(retry_load);
  load_reserved(addr, size, acquire);
  bne(t0, expected, ne_done);
  store_conditional(addr, new_val, size, release);
  bnez(t0, retry_load);

  // equal, succeed
  if (result_as_bool) {
    li(result, 1);
  } else {
    mv(result, expected);
  }
  j(done);

  // not equal, failed
  bind(ne_done);
  if (result_as_bool) {
    mv(result, zr);
  } else {
    mv(result, t0);
  }

  bind(done);
}

void MacroAssembler::cmpxchg_weak(Register addr, Register expected,
                                  Register new_val,
                                  enum operand_size size,
                                  Assembler::Aqrl acquire, Assembler::Aqrl release,
                                  Register result) {
  Label fail, done, sc_done;
  load_reserved(addr, size, acquire);
  bne(t0, expected, fail);
  store_conditional(addr, new_val, size, release);
  beqz(t0, sc_done);

  // fail
  bind(fail);
  li(result, 1);
  j(done);

  // sc_done
  bind(sc_done);
  mv(result, 0);
  bind(done);
}

#define ATOMIC_OP(NAME, AOP, ACQUIRE, RELEASE)                                              \
void MacroAssembler::atomic_##NAME(Register prev, RegisterOrConstant incr, Register addr) { \
  prev = prev->is_valid() ? prev : zr;                                                      \
  if (incr.is_register()) {                                                                 \
    AOP(prev, addr, incr.as_register(), (Assembler::Aqrl)(ACQUIRE | RELEASE));              \
  } else {                                                                                  \
    mv(t0, incr.as_constant());                                                             \
    AOP(prev, addr, t0, (Assembler::Aqrl)(ACQUIRE | RELEASE));                              \
  }                                                                                         \
  return;                                                                                   \
}

ATOMIC_OP(add, amoadd_d, Assembler::relaxed, Assembler::relaxed)
ATOMIC_OP(addw, amoadd_w, Assembler::relaxed, Assembler::relaxed)
ATOMIC_OP(addal, amoadd_d, Assembler::aq, Assembler::rl)
ATOMIC_OP(addalw, amoadd_w, Assembler::aq, Assembler::rl)

#undef ATOMIC_OP

#define ATOMIC_XCHG(OP, AOP, ACQUIRE, RELEASE)                                       \
void MacroAssembler::atomic_##OP(Register prev, Register newv, Register addr) {      \
  prev = prev->is_valid() ? prev : zr;                                               \
  AOP(prev, addr, newv, (Assembler::Aqrl)(ACQUIRE | RELEASE));                       \
  return;                                                                            \
}

ATOMIC_XCHG(xchg, amoswap_d, Assembler::relaxed, Assembler::relaxed)
ATOMIC_XCHG(xchgw, amoswap_w, Assembler::relaxed, Assembler::relaxed)
ATOMIC_XCHG(xchgal, amoswap_d, Assembler::aq, Assembler::rl)
ATOMIC_XCHG(xchgalw, amoswap_w, Assembler::aq, Assembler::rl)

#undef ATOMIC_XCHG

#define ATOMIC_XCHGU(OP1, OP2)                                                       \
void MacroAssembler::atomic_##OP1(Register prev, Register newv, Register addr) {     \
  atomic_##OP2(prev, newv, addr);                                                    \
  clear_upper_bits(prev, 32);                                                        \
  return;                                                                            \
}

ATOMIC_XCHGU(xchgwu, xchgw)
ATOMIC_XCHGU(xchgalwu, xchgalw)

#undef ATOMIC_XCHGU

void MacroAssembler::biased_locking_exit(Register obj_reg, Register temp_reg, Label& done, Register flag) {
  assert(UseBiasedLocking, "why call this otherwise?");

  // Check for biased locking unlock case, which is a no-op
  // Note: we do not have to check the thread ID for two reasons.
  // First, the interpreter checks for IllegalMonitorStateException at
  // a higher level. Second, if the bias was revoked while we held the
  // lock, the object could not be rebiased toward another thread, so
  // the bias bit would be clear.
  ld(temp_reg, Address(obj_reg, oopDesc::mark_offset_in_bytes()));
  andi(temp_reg, temp_reg, markOopDesc::biased_lock_mask_in_place); // 1 << 3
  sub(temp_reg, temp_reg, (u1)markOopDesc::biased_lock_pattern);
  if (flag->is_valid()) { mv(flag, temp_reg); }
  beqz(temp_reg, done);
}

void MacroAssembler::load_prototype_header(Register dst, Register src) {
  load_klass(dst, src);
  ld(dst, Address(dst, Klass::prototype_header_offset()));
}

int MacroAssembler::biased_locking_enter(Register lock_reg,
                                          Register obj_reg,
                                          Register swap_reg,
                                          Register tmp_reg,
                                          bool swap_reg_contains_mark,
                                          Label& done,
                                          Label* slow_case,
                                          BiasedLockingCounters* counters,
                                          Register flag) {
  assert(UseBiasedLocking, "why call this otherwise?");
  assert_different_registers(lock_reg, obj_reg, swap_reg);

  if (PrintBiasedLockingStatistics && counters == NULL) {
    counters = BiasedLocking::counters();
  }

  assert_different_registers(lock_reg, obj_reg, swap_reg, tmp_reg, t0, flag);
  assert(markOopDesc::age_shift == markOopDesc::lock_bits + markOopDesc::biased_lock_bits, "biased locking makes assumptions about bit layout");
  Address mark_addr      (obj_reg, oopDesc::mark_offset_in_bytes());

  // Biased locking
  // See whether the lock is currently biased toward our thread and
  // whether the epoch is still valid
  // Note that the runtime guarantees sufficient alignment of JavaThread
  // pointers to allow age to be placed into low bits
  // First check to see whether biasing is even enabled for this object
  Label cas_label;
  int null_check_offset = -1;
  if (!swap_reg_contains_mark) {
    null_check_offset = offset();
    ld(swap_reg, mark_addr);
  }
  andi(tmp_reg, swap_reg, markOopDesc::biased_lock_mask_in_place);
  xori(t0, tmp_reg, (u1)markOopDesc::biased_lock_pattern);
  bnez(t0, cas_label); // don't care flag unless jumping to done
  // The bias pattern is present in the object's header. Need to check
  // whether the bias owner and the epoch are both still current.
  load_prototype_header(tmp_reg, obj_reg);
  orr(tmp_reg, tmp_reg, xthread);
  xorr(tmp_reg, swap_reg, tmp_reg);
  andi(tmp_reg, tmp_reg, ~((int) markOopDesc::age_mask_in_place));
  if (flag->is_valid()) {
    mv(flag, tmp_reg);
  }

  if (counters != NULL) {
    Label around;
    bnez(tmp_reg, around);
    atomic_incw(Address((address)counters->biased_lock_entry_count_addr()), tmp_reg, t0);
    j(done);
    bind(around);
  } else {
    beqz(tmp_reg, done);
  }

  Label try_revoke_bias;
  Label try_rebias;

  // At this point we know that the header has the bias pattern and
  // that we are not the bias owner in the current epoch. We need to
  // figure out more details about the state of the header in order to
  // know what operations can be legally performed on the object's
  // header.

  // If the low three bits in the xor result aren't clear, that means
  // the prototype header is no longer biased and we have to revoke
  // the bias on this object.
  andi(t0, tmp_reg, markOopDesc::biased_lock_mask_in_place);
  bnez(t0, try_revoke_bias);

  // Biasing is still enabled for this data type. See whether the
  // epoch of the current bias is still valid, meaning that the epoch
  // bits of the mark word are equal to the epoch bits of the
  // prototype header. (Note that the prototype header's epoch bits
  // only change at a safepoint.) If not, attempt to rebias the object
  // toward the current thread. Note that we must be absolutely sure
  // that the current epoch is invalid in order to do this because
  // otherwise the manipulations it performs on the mark word are
  // illegal.
  andi(t0, tmp_reg, markOopDesc::epoch_mask_in_place);
  bnez(t0, try_rebias);

  // The epoch of the current bias is still valid but we know nothing
  // about the owner; it might be set or it might be clear. Try to
  // acquire the bias of the object using an atomic operation. If this
  // fails we will go in to the runtime to revoke the object's bias.
  // Note that we first construct the presumed unbiased header so we
  // don't accidentally blow away another thread's valid bias.
  {
    Label cas_success;
    Label counter;
    li(t0, (int64_t)(markOopDesc::biased_lock_mask_in_place | markOopDesc::age_mask_in_place | markOopDesc::epoch_mask_in_place));
    andr(swap_reg, swap_reg, t0);
    orr(tmp_reg, swap_reg, xthread);
    cmpxchg_obj_header(swap_reg, tmp_reg, obj_reg, t0, cas_success, slow_case);
    // cas failed here if slow_cass == NULL
    if (flag->is_valid()) {
      li(flag, 1);
      j(counter);
    }

    // If the biasing toward our thread failed, this means that
    // another thread succeeded in biasing it toward itself and we
    // need to revoke that bias. The revocation will occur in the
    // interpreter runtime in the slow case.
    bind(cas_success);
    if (flag->is_valid()) {
      li(flag, 0);
      bind(counter);
    }

    if (counters != NULL) {
      atomic_incw(Address((address)counters->anonymously_biased_lock_entry_count_addr()),
                  tmp_reg, t0);
    }
  }
  j(done);

  bind(try_rebias);
  // At this point we know the epoch has expired, meaning that the
  // current "bias owner", if any, is actually invalid. Under these
  // circumstances _only_, we are allowed to use the current header's
  // value as the comparison value when doing the cas to acquire the
  // bias in the current epoch. In other words, we allow transfer of
  // the bias from one thread to another directly in this situation.
  //
  // FIXME: due to a lack of registers we currently blow away the age
  // bits in this situation. Should attempt to preserve them.
  {
    Label cas_success;
    Label counter;
    load_prototype_header(tmp_reg, obj_reg);
    orr(tmp_reg, xthread, tmp_reg);
    cmpxchg_obj_header(swap_reg, tmp_reg, obj_reg, t0, cas_success, slow_case);
    // cas failed here if slow_cass == NULL
    if (flag->is_valid()) {
      li(flag, 1);
      j(counter);
    }

    // If the biasing toward our thread failed, then another thread
    // succeeded in biasing it toward itself and we need to revoke that
    // bias. The revocation will occur in the runtime in the slow case.
    bind(cas_success);
    if (flag->is_valid()) {
      li(flag, 0);
      bind(counter);
    }

    if (counters != NULL) {
      atomic_incw(Address((address)counters->rebiased_lock_entry_count_addr()),
                  tmp_reg, t0);
    }
  }
  j(done);

  // don't care flag unless jumping to done
  bind(try_revoke_bias);
  // The prototype mark in the klass doesn't have the bias bit set any
  // more, indicating that objects of this data type are not supposed
  // to be biased any more. We are going to try to reset the mark of
  // this object to the prototype value and fall through to the
  // CAS-based locking scheme. Note that if our CAS fails, it means
  // that another thread raced us for the privilege of revoking the
  // bias of this particular object, so it's okay to continue in the
  // normal locking code.
  //
  // FIXME: due to a lack of registers we currently blow away the age
  // bits in this situation. Should attempt to preserve them.
  {
    Label cas_success, nope;
    load_prototype_header(tmp_reg, obj_reg);
    cmpxchg_obj_header(swap_reg, tmp_reg, obj_reg, t0, cas_success, &nope);
    bind(cas_success);

    // Fall through to the normal CAS-based lock, because no matter what
    // the result of the above CAS, some thread must have succeeded in
    // removing the bias bit from the object's header.
    if (counters != NULL) {
      atomic_incw(Address((address)counters->revoked_lock_entry_count_addr()), tmp_reg,
                  t0);
    }
    bind(nope);
  }

  bind(cas_label);

  return null_check_offset;
}

void MacroAssembler::atomic_incw(Register counter_addr, Register tmp) {
  Label retry_load;
  bind(retry_load);
  // flush and load exclusive from the memory location
  lr_w(tmp, counter_addr);
  addw(tmp, tmp, 1);
  // if we store+flush with no intervening write tmp wil be zero
  sc_w(tmp, tmp, counter_addr);
  bnez(tmp, retry_load);
}

void MacroAssembler::far_jump(Address entry, CodeBuffer *cbuf, Register tmp) {
  assert(ReservedCodeCacheSize < 4*G, "branch out of range");
  assert(CodeCache::find_blob(entry.target()) != NULL,
         "destination of far call not found in code cache");
  int32_t offset = 0;
  if (far_branches()) {
    // We can use auipc + jalr here because we know that the total size of
    // the code cache cannot exceed 2Gb.
    la_patchable(tmp, entry, offset);
    if (cbuf != NULL) { cbuf->set_insts_mark(); }
    jalr(x0, tmp, offset);
  } else {
    if (cbuf != NULL) { cbuf->set_insts_mark(); }
    j(entry);
  }
}

void MacroAssembler::far_call(Address entry, CodeBuffer *cbuf, Register tmp) {
  assert(ReservedCodeCacheSize < 4*G, "branch out of range");
  assert(CodeCache::find_blob(entry.target()) != NULL,
         "destination of far call not found in code cache");
  int32_t offset = 0;
  if (far_branches()) {
    // We can use auipc + jalr here because we know that the total size of
    // the code cache cannot exceed 2Gb.
    la_patchable(tmp, entry, offset);
    if (cbuf != NULL) { cbuf->set_insts_mark(); }
    jalr(x1, tmp, offset); // link
  } else {
    if (cbuf != NULL) { cbuf->set_insts_mark(); }
    jal(entry); // link
  }
}

void MacroAssembler::check_klass_subtype_fast_path(Register sub_klass,
                                                   Register super_klass,
                                                   Register temp_reg,
                                                   Label* L_success,
                                                   Label* L_failure,
                                                   Label* L_slow_path,
                                                   Register super_check_offset) {
  assert_different_registers(sub_klass, super_klass, temp_reg);
  bool must_load_sco = (super_check_offset == noreg);
  if (must_load_sco) {
    assert(temp_reg != noreg, "supply either a temp or a register offset");
  } else {
    assert_different_registers(sub_klass, super_klass, super_check_offset);
  }

  Label L_fallthrough;
  int label_nulls = 0;
  if (L_success == NULL)   { L_success   = &L_fallthrough; label_nulls++; }
  if (L_failure == NULL)   { L_failure   = &L_fallthrough; label_nulls++; }
  if (L_slow_path == NULL) { L_slow_path = &L_fallthrough; label_nulls++; }
  assert(label_nulls <= 1, "at most one NULL in batch");

  int sc_offset = in_bytes(Klass::secondary_super_cache_offset());
  int sco_offset = in_bytes(Klass::super_check_offset_offset());
  Address super_check_offset_addr(super_klass, sco_offset);

  // Hacked jmp, which may only be used just before L_fallthrough.
#define final_jmp(label)                                                \
  if (&(label) == &L_fallthrough) { /*do nothing*/ }                    \
  else                            j(label)             /*omit semi*/

  // If the pointers are equal, we are done (e.g., String[] elements).
  // This self-check enables sharing of secondary supertype arrays among
  // non-primary types such as array-of-interface. Otherwise, each such
  // type would need its own customized SSA.
  // We move this check to the front fo the fast path because many
  // type checks are in fact trivially successful in this manner,
  // so we get a nicely predicted branch right at the start of the check.
  beq(sub_klass, super_klass, *L_success);

  // Check the supertype display:
  if (must_load_sco) {
    lwu(temp_reg, super_check_offset_addr);
    super_check_offset = temp_reg;
  }
  add(t0, sub_klass, super_check_offset);
  Address super_check_addr(t0);
  ld(t0, super_check_addr); // load displayed supertype

  // Ths check has worked decisively for primary supers.
  // Secondary supers are sought in the super_cache ('super_cache_addr').
  // (Secondary supers are interfaces and very deeply nested subtypes.)
  // This works in the same check above because of a tricky aliasing
  // between the super_Cache and the primary super dispaly elements.
  // (The 'super_check_addr' can address either, as the case requires.)
  // Note that the cache is updated below if it does not help us find
  // what we need immediately.
  // So if it was a primary super, we can just fail immediately.
  // Otherwise, it's the slow path for us (no success at this point).

  beq(super_klass, t0, *L_success);
  mv(t1, sc_offset);
  if (L_failure == &L_fallthrough) {
    beq(super_check_offset, t1, *L_slow_path);
  } else {
    bne(super_check_offset, t1, *L_failure, /* is_far */ true);
    final_jmp(*L_slow_path);
  }

  bind(L_fallthrough);

#undef final_jmp
}

// scans count pointer sized words at [addr] for occurence of value,
// generic
void MacroAssembler::repne_scan(Register addr, Register value, Register count,
                                Register temp) {
  Label Lloop, Lexit;
  beqz(count, Lexit);
  bind(Lloop);
  ld(temp, addr);
  beq(value, temp, Lexit);
  add(addr, addr, wordSize);
  sub(count, count, 1);
  bnez(count, Lloop);
  bind(Lexit);
}

void MacroAssembler::check_klass_subtype_slow_path(Register sub_klass,
                                                   Register super_klass,
                                                   Register temp_reg,
                                                   Register temp2_reg,
                                                   Label* L_success,
                                                   Label* L_failure) {
  assert_different_registers(sub_klass, super_klass, temp_reg);
  if (temp2_reg != noreg) {
    assert_different_registers(sub_klass, super_klass, temp_reg, temp2_reg, t0);
  }
#define IS_A_TEMP(reg) ((reg) == temp_reg || (reg) == temp2_reg)

  Label L_fallthrough;
  int label_nulls = 0;
  if (L_success == NULL)   { L_success   = &L_fallthrough; label_nulls++; }
  if (L_failure == NULL)   { L_failure   = &L_fallthrough; label_nulls++; }

  assert(label_nulls <= 1, "at most one NULL in the batch");

  // a couple of usefule fields in sub_klass:
  int ss_offset = in_bytes(Klass::secondary_supers_offset());
  int sc_offset = in_bytes(Klass::secondary_super_cache_offset());
  Address secondary_supers_addr(sub_klass, ss_offset);
  Address super_cache_addr(     sub_klass, sc_offset);

  BLOCK_COMMENT("check_klass_subtype_slow_path");

  // Do a linear scan of the secondary super-klass chain.
  // This code is rarely used, so simplicity is a virtue here.
  // The repne_scan instruction uses fixed registers, which we must spill.
  // Don't worry too much about pre-existing connecitons with the input regs.

  assert(sub_klass != x10, "killed reg"); // killed by mv(x10, super)
  assert(sub_klass != x12, "killed reg"); // killed by la(x12, &pst_counter)

  RegSet pushed_registers;
  if (!IS_A_TEMP(x12)) {
    pushed_registers += x12;
  }
  if (!IS_A_TEMP(x15)) {
    pushed_registers += x15;
  }

  if (super_klass != x10 || UseCompressedOops) {
    if (!IS_A_TEMP(x10)) {
      pushed_registers += x10;
    }
  }

  push_reg(pushed_registers, sp);

  // Get super_klass value into x10 (even if it was in x15 or x12)
  mv(x10, super_klass);

#ifndef PRODUCT
  mv(t1, (address)&SharedRuntime::_partial_subtype_ctr);
  Address pst_counter_addr(t1);
  ld(t0, pst_counter_addr);
  add(t0, t0, 1);
  sd(t0, pst_counter_addr);
#endif // PRODUCT

  // We will consult the secondary-super array.
  ld(x15, secondary_supers_addr);
  // Load the array length.
  lwu(x12, Address(x15, Array<Klass*>::length_offset_in_bytes()));
  // Skip to start of data.
  add(x15, x15, Array<Klass*>::base_offset_in_bytes());

  // Set t0 to an obvious invalid value, falling through by default
  li(t0, -1);
  // Scan X12 words at [X15] for an occurrence of X10.
  repne_scan(x15, x10, x12, t0);

  // pop will restore x10, so we should use a temp register to keep its value
  mv(t1, x10);

  // Unspill the temp. registers:
  pop_reg(pushed_registers, sp);

  bne(t1, t0, *L_failure);

  // Success. Cache the super we found an proceed in triumph.
  sd(super_klass, super_cache_addr);

  if (L_success != &L_fallthrough) {
    j(*L_success);
  }

#undef IS_A_TEMP

  bind(L_fallthrough);
}

// Defines obj, preserves var_size_in_bytes, okay for tmp2 == var_size_in_bytes.
void MacroAssembler::tlab_allocate(Register obj,
                                   Register var_size_in_bytes,
                                   int con_size_in_bytes,
                                   Register tmp1,
                                   Register tmp2,
                                   Label& slow_case,
                                   bool is_far) {
  BarrierSetAssembler *bs = BarrierSet::barrier_set()->barrier_set_assembler();
  bs->tlab_allocate(this, obj, var_size_in_bytes, con_size_in_bytes, tmp1, tmp2, slow_case, is_far);
}

// Defines obj, preserves var_size_in_bytes
void MacroAssembler::eden_allocate(Register obj,
                                   Register var_size_in_bytes,
                                   int con_size_in_bytes,
                                   Register tmp1,
                                   Label& slow_case,
                                   bool is_far) {
  BarrierSetAssembler *bs = BarrierSet::barrier_set()->barrier_set_assembler();
  bs->eden_allocate(this, obj, var_size_in_bytes, con_size_in_bytes, tmp1, slow_case, is_far);
}


// get_thread() can be called anywhere inside generated code so we
// need to save whatever non-callee save context might get clobbered
// by the call to Thread::current() or, indeed, the call setup code.
void MacroAssembler::get_thread(Register thread) {
  // save all call-clobbered regs except thread
  RegSet saved_regs = RegSet::range(x5, x7) + RegSet::range(x10, x17) +
                      RegSet::range(x28, x31) + ra - thread;
  push_reg(saved_regs, sp);

  int32_t offset = 0;
  movptr_with_offset(ra, CAST_FROM_FN_PTR(address, Thread::current), offset);
  jalr(ra, ra, offset);
  if (thread != x10) {
    mv(thread, x10);
  }

  // restore pushed registers
  pop_reg(saved_regs, sp);
}

void MacroAssembler::load_byte_map_base(Register reg) {
  jbyte *byte_map_base = ((CardTableBarrierSet*)(BarrierSet::barrier_set()))->card_table()->byte_map_base();
  li(reg, (uint64_t)byte_map_base);
}

void MacroAssembler::la_patchable(Register reg1, const Address &dest, int32_t &offset) {
  relocInfo::relocType rtype = dest.rspec().reloc()->type();
  unsigned long low_address = (uintptr_t)CodeCache::low_bound();
  unsigned long high_address = (uintptr_t)CodeCache::high_bound();
  unsigned long dest_address = (uintptr_t)dest.target();
  long offset_low = dest_address - low_address;
  long offset_high = dest_address - high_address;

  assert(is_valid_riscv64_address(dest.target()), "bad address");
  assert(dest.getMode() == Address::literal, "la_patchable must be applied to a literal address");

  InstructionMark im(this);
  code_section()->relocate(inst_mark(), dest.rspec());
  // RISC-V doesn't compute a page-aligned address, in order to partially
  // compensate for the use of *signed* offsets in its base+disp12
  // addressing mode (RISC-V's PC-relative reach remains asymmetric
  // [-(2G + 2K), 2G - 2k).
  if (offset_high >= -((1L << 31) + (1L << 11)) && offset_low < (1L << 31) - (1L << 11)) {
    int64_t distance = dest.target() - pc();
    auipc(reg1, (int32_t)distance + 0x800);
    offset = ((int32_t)distance << 20) >> 20;
  } else {
    movptr_with_offset(reg1, dest.target(), offset);
  }
}

void MacroAssembler::build_frame(int framesize) {
  assert(framesize >= 2 * wordSize, "framesize must include space for FP/LR");
  assert(framesize % (2 * wordSize) == 0, "must preserve 2 * wordSize alignment");
  sub(sp, sp, framesize);
  sd(fp, Address(sp, framesize - 2 * wordSize));
  sd(lr, Address(sp, framesize - wordSize));
  if (PreserveFramePointer) { add(fp, sp, framesize - 2 * wordSize); }
}

void MacroAssembler::remove_frame(int framesize) {
  assert(framesize >= 2 * wordSize, "framesize must include space for FP/LR");
  assert(framesize % (2 * wordSize) == 0, "must preserve 2 * wordSize alignment");
  ld(fp, Address(sp, framesize - 2 * wordSize));
  ld(lr, Address(sp, framesize - wordSize));
  add(sp, sp, framesize);
}

void MacroAssembler::reserved_stack_check() {
    // testing if reserved zone needs to be enabled
    Label no_reserved_zone_enabling;

    ld(t0, Address(xthread, JavaThread::reserved_stack_activation_offset()));
    bltu(sp, t0, no_reserved_zone_enabling);

    enter();   // LR and FP are live.
    mv(c_rarg0, xthread);
    int32_t offset = 0;
    la_patchable(t0, RuntimeAddress(CAST_FROM_FN_PTR(address, SharedRuntime::enable_stack_reserved_zone)), offset);
    jalr(x1, t0, offset);
    leave();

    // We have already removed our own frame.
    // throw_delayed_StackOverflowError will think that it's been
    // called by our caller.
    offset = 0;
    la_patchable(t0, RuntimeAddress(StubRoutines::throw_delayed_StackOverflowError_entry()), offset);
    jalr(x0, t0, offset);
    should_not_reach_here();

    bind(no_reserved_zone_enabling);
}

// Move the address of the polling page into dest.
void MacroAssembler::get_polling_page(Register dest, address page, int32_t &offset, relocInfo::relocType rtype) {
  if (SafepointMechanism::uses_thread_local_poll()) {
    ld(dest, Address(xthread, Thread::polling_page_offset()));
  } else {
    unsigned long align = (uintptr_t)page & 0xfff;
    assert(align == 0, "polling page must be page aligned");
    la_patchable(dest, Address(page, rtype), offset);
  }
}

// Move the address of the polling page into dest.
address MacroAssembler::read_polling_page(Register dest, address page, relocInfo::relocType rtype) {
  int32_t offset = 0;
  get_polling_page(dest, page, offset, rtype);
  return read_polling_page(dest, offset, rtype);
}

// Read the polling page.  The address of the polling page must
// already be in r.
address MacroAssembler::read_polling_page(Register r, int32_t offset, relocInfo::relocType rtype) {
  InstructionMark im(this);
  code_section()->relocate(inst_mark(), rtype);
  lwu(zr, Address(r, offset));
  return inst_mark();
}

void  MacroAssembler::set_narrow_oop(Register dst, jobject obj) {
#ifdef ASSERT
  {
    ThreadInVMfromUnknown tiv;
    assert (UseCompressedOops, "should only be used for compressed oops");
    assert (Universe::heap() != NULL, "java heap should be initialized");
    assert (oop_recorder() != NULL, "this assembler needs an OopRecorder");
    assert(Universe::heap()->is_in_reserved(JNIHandles::resolve(obj)), "should be real oop");
  }
#endif
  int oop_index = oop_recorder()->find_index(obj);
  InstructionMark im(this);
  RelocationHolder rspec = oop_Relocation::spec(oop_index);
  code_section()->relocate(inst_mark(), rspec);
  li32(dst, 0xDEADBEEF);
  clear_upper_bits(dst, 32); // clear upper 32bit, do not sign extend.
}

void  MacroAssembler::set_narrow_klass(Register dst, Klass* k) {
  assert (UseCompressedClassPointers, "should only be used for compressed headers");
  assert (oop_recorder() != NULL, "this assembler needs an OopRecorder");
  int index = oop_recorder()->find_index(k);
  assert(!Universe::heap()->is_in_reserved(k), "should not be an oop");

  InstructionMark im(this);
  RelocationHolder rspec = metadata_Relocation::spec(index);
  code_section()->relocate(inst_mark(), rspec);
  narrowKlass nk = Klass::encode_klass(k);
  li32(dst, nk);
  clear_upper_bits(dst, 32); // clear upper 32bit, do not sign extend.
}

// Maybe emit a call via a trampoline.  If the code cache is small
// trampolines won't be emitted.
address MacroAssembler::trampoline_call(Address entry, CodeBuffer* cbuf) {
  assert(JavaThread::current()->is_Compiler_thread(), "just checking");
  assert(entry.rspec().type() == relocInfo::runtime_call_type ||
         entry.rspec().type() == relocInfo::opt_virtual_call_type ||
         entry.rspec().type() == relocInfo::static_call_type ||
         entry.rspec().type() == relocInfo::virtual_call_type, "wrong reloc type");

  // We need a trampoline if branches are far.
  if (far_branches()) {
    bool in_scratch_emit_size = false;
#ifdef COMPILER2
    // We don't want to emit a trampoline if C2 is generating dummy
    // code during its branch shortening phase.
    CompileTask* task = ciEnv::current()->task();
    in_scratch_emit_size =
      (task != NULL && is_c2_compile(task->comp_level()) &&
       Compile::current()->in_scratch_emit_size());
#endif
    if (!in_scratch_emit_size) {
      address stub = emit_trampoline_stub(offset(), entry.target());
      if (stub == NULL) {
        postcond(pc() == badAddress);
        return NULL; // CodeCache is full
      }
    }
  }

  if (cbuf != NULL) { cbuf->set_insts_mark(); }
  relocate(entry.rspec());
  if (!far_branches()) {
    jal(entry.target());
  } else {
    jal(pc());
  }
  // just need to return a non-null address
  postcond(pc() != badAddress);
  return pc();
}

address MacroAssembler::ic_call(address entry, jint method_index) {
  RelocationHolder rh = virtual_call_Relocation::spec(pc(), method_index);
  movptr(t1, (address)Universe::non_oop_word());
  assert_cond(entry != NULL);
  return trampoline_call(Address(entry, rh));
}

// Emit a trampoline stub for a call to a target which is too far away.
//
// code sequences:
//
// call-site:
//   branch-and-link to <destination> or <trampoline stub>
//
// Related trampoline stub for this call site in the stub section:
//   load the call target from the constant pool
//   branch (LR still points to the call site above)

address MacroAssembler::emit_trampoline_stub(int insts_call_instruction_offset,
                                             address dest) {
  address stub = start_a_stub(NativeInstruction::instruction_size
                            + NativeCallTrampolineStub::instruction_size);
  if (stub == NULL) {
    return NULL;  // CodeBuffer::expand failed
  }

  // Create a trampoline stub relocation which relates this trampoline stub
  // with the call instruction at insts_call_instruction_offset in the
  // instructions code-section.

  // make sure 4 byte aligned here, so that the destination address would be
  // 8 byte aligned after 3 intructions
  while (offset() % wordSize == 0) { nop(); }

  relocate(trampoline_stub_Relocation::spec(code()->insts()->start() +
                                            insts_call_instruction_offset));
  const int stub_start_offset = offset();

  // Now, create the trampoline stub's code:
  // - load the call
  // - call
  Label target;
  ld(t0, target);  // auipc + ld
  jr(t0);          // jalr
  bind(target);
  assert(offset() % wordSize == 0, "address loaded by ld must be 8-byte aligned under riscv64");
  emit_int64((intptr_t)dest);

  const address stub_start_addr = addr_at(stub_start_offset);

  assert(is_NativeCallTrampolineStub_at(stub_start_addr), "doesn't look like a trampoline");

  end_a_stub();
  return stub_start_addr;
}

Address MacroAssembler::add_memory_helper(const Address dst) {
  switch (dst.getMode()) {
    case Address::base_plus_offset:
      // This is the expected mode, although we allow all the other
      // forms below.
      return form_address(t1, dst.base(), dst.offset());
    default:
      la(t1, dst);
      return Address(t1);
  }
}

void MacroAssembler::add_memory_int64(const Address dst, int64_t imm) {
  Address adr = add_memory_helper(dst);
  assert_different_registers(adr.base(), t0);
  ld(t0, adr);
  addi(t0, t0, imm);
  sd(t0, adr);
}

void MacroAssembler::add_memory_int32(const Address dst, int32_t imm) {
  Address adr = add_memory_helper(dst);
  assert_different_registers(adr.base(), t0);
  lwu(t0, adr);
  addiw(t0, t0, imm);
  sw(t0, adr);
}

void MacroAssembler::cmpptr(Register src1, Address src2, Label& equal) {
  assert_different_registers(src1, t0);
  int32_t offset;
  la_patchable(t0, src2, offset);
  ld(t0, Address(t0, offset));
  beq(src1, t0, equal);
}

void MacroAssembler::load_method_holder(Register holder, Register method) {
  ld(holder, Address(method, Method::const_offset()));                      // ConstMethod*
  ld(holder, Address(holder, ConstMethod::constants_offset()));             // ConstantPool*
  ld(holder, Address(holder, ConstantPool::pool_holder_offset_in_bytes())); // InstanceKlass*
}

void MacroAssembler::oop_beq(Register obj1, Register obj2, Label& L_equal, bool is_far) {
  beq(obj1, obj2, L_equal, is_far);
}

void MacroAssembler::oop_bne(Register obj1, Register obj2, Label& L_nequal, bool is_far) {
  bne(obj1, obj2, L_nequal, is_far);
}

#ifdef COMPILER2
// TODO: wind: THIS FUNCTION IS TOTALLY DIFFERENT FROM JDK11
//     generate_large_array_equals()
//      git difftool ~/jvm/dragonwell11/src/hotspot/cpu/riscv64/stubGenerator_riscv64.cpp ~/jvm/dragonwell11-latest/src/hotspot/cpu/riscv64/stubGenerator_riscv64.cpp
void MacroAssembler::arrays_equals(Register a1, Register a2, Register tmp3,
                                    Register tmp4, Register tmp5, Register tmp6, Register result,
                                    Register cnt1, int elem_size) {
  Label DONE, SAME, NEXT_DWORD, SHORT, TAIL, TAIL2, IS_TMP5_ZR;
  Register tmp1 = t0;
  Register tmp2 = t1;
  Register cnt2 = tmp2;  // cnt2 only used in array length compare
  Register elem_per_word = tmp6;
  int log_elem_size = exact_log2(elem_size);
  int length_offset = arrayOopDesc::length_offset_in_bytes();
  int base_offset   = arrayOopDesc::base_offset_in_bytes(elem_size == 2 ? T_CHAR : T_BYTE);

  assert(elem_size == 1 || elem_size == 2, "must be char or byte");
  assert_different_registers(a1, a2, result, cnt1, t0, t1, tmp3, tmp4, tmp5, tmp6);
  li(elem_per_word, wordSize / elem_size);

  BLOCK_COMMENT("arrays_equals {");

  // if (a1 == a2), return true
  oop_beq(a1, a2, SAME);

  mv(result, false);
  beqz(a1, DONE);
  beqz(a2, DONE);
  lwu(cnt1, Address(a1, length_offset));
  lwu(cnt2, Address(a2, length_offset));
  bne(cnt2, cnt1, DONE);
  beqz(cnt1, SAME);

  slli(tmp5, cnt1, 3 + log_elem_size);
  sub(tmp5, zr, tmp5);
  add(a1, a1, base_offset);
  add(a2, a2, base_offset);
  ld(tmp3, Address(a1, 0));
  ld(tmp4, Address(a2, 0));
  ble(cnt1, elem_per_word, SHORT); // short or same

  // Main 16 byte comparison loop with 2 exits
  bind(NEXT_DWORD); {
    ld(tmp1, Address(a1, wordSize));
    ld(tmp2, Address(a2, wordSize));
    sub(cnt1, cnt1, 2 * wordSize / elem_size);
    blez(cnt1, TAIL);
    bne(tmp3, tmp4, DONE);
    ld(tmp3, Address(a1, 2 * wordSize));
    ld(tmp4, Address(a2, 2 * wordSize));
    add(a1, a1, 2 * wordSize);
    add(a2, a2, 2 * wordSize);
    ble(cnt1, elem_per_word, TAIL2);
  } beq(tmp1, tmp2, NEXT_DWORD);
  j(DONE);

  bind(TAIL);
  xorr(tmp4, tmp3, tmp4);
  xorr(tmp2, tmp1, tmp2);
  sll(tmp2, tmp2, tmp5);
  orr(tmp5, tmp4, tmp2);
  j(IS_TMP5_ZR);

  bind(TAIL2);
  bne(tmp1, tmp2, DONE);

  bind(SHORT);
  xorr(tmp4, tmp3, tmp4);
  sll(tmp5, tmp4, tmp5);

  bind(IS_TMP5_ZR);
  bnez(tmp5, DONE);

  bind(SAME);
  mv(result, true);
  // That's it.
  bind(DONE);

  BLOCK_COMMENT("} array_equals");
}

// Compare Strings

// For Strings we're passed the address of the first characters in a1
// and a2 and the length in cnt1.
// elem_size is the element size in bytes: either 1 or 2.
// All comparisons (including the final one, which may overlap) are
// performed 8 bytes at a time.

void MacroAssembler::string_equals(Register a1, Register a2,
                                    Register result, Register cnt1, int elem_size)
{
  Label SAME, DONE, SHORT, NEXT_WORD;
  Register tmp1 = t0;
  Register tmp2 = t1;

  assert(elem_size == 1 || elem_size == 2, "must be 2 or 1 byte");
  assert_different_registers(a1, a2, result, cnt1, t0, t1);

  BLOCK_COMMENT("string_equals {");

  beqz(cnt1, SAME);
  mv(result, false);

  // Check for short strings, i.e. smaller than wordSize.
  sub(cnt1, cnt1, wordSize);
  blez(cnt1, SHORT);

  // Main 8 byte comparison loop.
  bind(NEXT_WORD); {
    ld(tmp1, Address(a1, 0));
    add(a1, a1, wordSize);
    ld(tmp2, Address(a2, 0));
    add(a2, a2, wordSize);
    sub(cnt1, cnt1, wordSize);
    bne(tmp1, tmp2, DONE);
  } bgtz(cnt1, NEXT_WORD);

  if (!AvoidUnalignedAccesses) {
    // Last longword.  In the case where length == 4 we compare the
    // same longword twice, but that's still faster than another
    // conditional branch.
    // cnt1 could be 0, -1, -2, -3, -4 for chars; -4 only happens when
    // length == 4.
    add(tmp1, a1, cnt1);
    ld(tmp1, Address(tmp1, 0));
    add(tmp2, a2, cnt1);
    ld(tmp2, Address(tmp2, 0));
    bne(tmp1, tmp2, DONE);
    j(SAME);
  }

  bind(SHORT);
  ld(tmp1, Address(a1));
  ld(tmp2, Address(a2));
  xorr(tmp1, tmp1, tmp2);
  neg(cnt1, cnt1);
  slli(cnt1, cnt1, LogBitsPerByte);
  sll(tmp1, tmp1, cnt1);
  bnez(tmp1, DONE);

  // Arrays are equal.
  bind(SAME);
  mv(result, true);

  // That's it.
  bind(DONE);
  BLOCK_COMMENT("} string_equals");
}

typedef void (MacroAssembler::* load_chr_insn)(Register rd, const Address &adr, Register temp);

// Compare strings.
void MacroAssembler::string_compare(Register str1, Register str2,
                                     Register cnt1, Register cnt2, Register result, Register tmp1, Register tmp2,
                                     Register tmp3, int ae)
{
  Label DONE, SHORT_LOOP, SHORT_STRING, SHORT_LAST, TAIL, STUB,
          DIFFERENCE, NEXT_WORD, SHORT_LOOP_TAIL, SHORT_LAST2, SHORT_LAST_INIT,
          SHORT_LOOP_START, TAIL_CHECK, L;

  const int STUB_THRESHOLD = 64 + 8;
  bool isLL = ae == StrIntrinsicNode::LL;
  bool isLU = ae == StrIntrinsicNode::LU;
  bool isUL = ae == StrIntrinsicNode::UL;

  bool str1_isL = isLL || isLU;
  bool str2_isL = isLL || isUL;

  // for L strings, 1 byte for 1 character
  // for U strings, 2 bytes for 1 character
  int str1_chr_size = str1_isL ? 1 : 2;
  int str2_chr_size = str2_isL ? 1 : 2;
  int minCharsInWord = isLL ? wordSize : wordSize / 2;

  load_chr_insn str1_load_chr = str1_isL ? (load_chr_insn)&MacroAssembler::lbu : (load_chr_insn)&MacroAssembler::lhu;
  load_chr_insn str2_load_chr = str2_isL ? (load_chr_insn)&MacroAssembler::lbu : (load_chr_insn)&MacroAssembler::lhu;

  BLOCK_COMMENT("string_compare {");

  // Bizzarely, the counts are passed in bytes, regardless of whether they
  // are L or U strings, however the result is always in characters.
  if (!str1_isL) {
    sraiw(cnt1, cnt1, 1);
  }
  if (!str2_isL) {
    sraiw(cnt2, cnt2, 1);
  }

  // Compute the minimum of the string lengths and save the difference in result.
  sub(result, cnt1, cnt2);
  bgt(cnt1, cnt2, L);
  mv(cnt2, cnt1);
  bind(L);

  // A very short string
  li(t0, minCharsInWord);
  ble(cnt2, t0, SHORT_STRING);

  // Compare longwords
  // load first parts of strings and finish initialization while loading
  {
    if (str1_isL == str2_isL) { // LL or UU
      // load 8 bytes once to compare
      ld(tmp1, Address(str1));
      beq(str1, str2, DONE);
      ld(tmp2, Address(str2));
      li(t0, STUB_THRESHOLD);
      bge(cnt2, t0, STUB);
      sub(cnt2, cnt2, minCharsInWord);
      beqz(cnt2, TAIL_CHECK);
      // convert cnt2 from characters to bytes
      if(!str1_isL) {
        slli(cnt2, cnt2, 1);
      }
      add(str2, str2, cnt2);
      add(str1, str1, cnt2);
      sub(cnt2, zr, cnt2);
    } else if (isLU) { // LU case
      lwu(tmp1, Address(str1));
      ld(tmp2, Address(str2));
      li(t0, STUB_THRESHOLD);
      bge(cnt2, t0, STUB);
      addi(cnt2, cnt2, -4);
      add(str1, str1, cnt2);
      sub(cnt1, zr, cnt2);
      slli(cnt2, cnt2, 1);
      add(str2, str2, cnt2);
      inflate_lo32(tmp3, tmp1);
      mv(tmp1, tmp3);
      sub(cnt2, zr, cnt2);
      addi(cnt1, cnt1, 4);
    } else { // UL case
      ld(tmp1, Address(str1));
      lwu(tmp2, Address(str2));
      li(t0, STUB_THRESHOLD);
      bge(cnt2, t0, STUB);
      addi(cnt2, cnt2, -4);
      slli(t0, cnt2, 1);
      sub(cnt1, zr, t0);
      add(str1, str1, t0);
      add(str2, str2, cnt2);
      inflate_lo32(tmp3, tmp2);
      mv(tmp2, tmp3);
      sub(cnt2, zr, cnt2);
      addi(cnt1, cnt1, 8);
    }
    addi(cnt2, cnt2, isUL ? 4 : 8);
    bgez(cnt2, TAIL);
    xorr(tmp3, tmp1, tmp2);
    bnez(tmp3, DIFFERENCE);

    // main loop
    bind(NEXT_WORD);
    if (str1_isL == str2_isL) { // LL or UU
      add(t0, str1, cnt2);
      ld(tmp1, Address(t0));
      add(t0, str2, cnt2);
      ld(tmp2, Address(t0));
      addi(cnt2, cnt2, 8);
    } else if (isLU) { // LU case
      add(t0, str1, cnt1);
      lwu(tmp1, Address(t0));
      add(t0, str2, cnt2);
      ld(tmp2, Address(t0));
      addi(cnt1, cnt1, 4);
      inflate_lo32(tmp3, tmp1);
      mv(tmp1, tmp3);
      addi(cnt2, cnt2, 8);
    } else { // UL case
      add(t0, str2, cnt2);
      lwu(tmp2, Address(t0));
      add(t0, str1, cnt1);
      ld(tmp1, Address(t0));
      inflate_lo32(tmp3, tmp2);
      mv(tmp2, tmp3);
      addi(cnt1, cnt1, 8);
      addi(cnt2, cnt2, 4);
    }
    bgez(cnt2, TAIL);

    xorr(tmp3, tmp1, tmp2);
    beqz(tmp3, NEXT_WORD);
    j(DIFFERENCE);
    bind(TAIL);
    xorr(tmp3, tmp1, tmp2);
    bnez(tmp3, DIFFERENCE);
    // Last longword.  In the case where length == 4 we compare the
    // same longword twice, but that's still faster than another
    // conditional branch.
    if (str1_isL == str2_isL) { // LL or UU
      ld(tmp1, Address(str1));
      ld(tmp2, Address(str2));
    } else if (isLU) { // LU case
      lwu(tmp1, Address(str1));
      ld(tmp2, Address(str2));
      inflate_lo32(tmp3, tmp1);
      mv(tmp1, tmp3);
    } else { // UL case
      lwu(tmp2, Address(str2));
      ld(tmp1, Address(str1));
      inflate_lo32(tmp3, tmp2);
      mv(tmp2, tmp3);
    }
    bind(TAIL_CHECK);
    xorr(tmp3, tmp1, tmp2);
    beqz(tmp3, DONE);

    // Find the first different characters in the longwords and
    // compute their difference.
    bind(DIFFERENCE);
    ctzc_bit(result, tmp3, isLL); // count zero from lsb to msb
    srl(tmp1, tmp1, result);
    srl(tmp2, tmp2, result);
    if (isLL) {
      andi(tmp1, tmp1, 0xFF);
      andi(tmp2, tmp2, 0xFF);
    } else {
      andi(tmp1, tmp1, 0xFFFF);
      andi(tmp2, tmp2, 0xFFFF);
    }
    sub(result, tmp1, tmp2);
    j(DONE);
  }

  bind(STUB);
  RuntimeAddress stub = NULL;
  switch (ae) {
    case StrIntrinsicNode::LL:
      stub = RuntimeAddress(StubRoutines::riscv64::compare_long_string_LL());
      break;
    case StrIntrinsicNode::UU:
      stub = RuntimeAddress(StubRoutines::riscv64::compare_long_string_UU());
      break;
    case StrIntrinsicNode::LU:
      stub = RuntimeAddress(StubRoutines::riscv64::compare_long_string_LU());
      break;
    case StrIntrinsicNode::UL:
      stub = RuntimeAddress(StubRoutines::riscv64::compare_long_string_UL());
      break;
    default:
      ShouldNotReachHere();
  }
  assert(stub.target() != NULL, "compare_long_string stub has not been generated");
  trampoline_call(stub);
  j(DONE);

  bind(SHORT_STRING);
  // Is the minimum length zero?
  beqz(cnt2, DONE);
  // arrange code to do most branches while loading and loading next characters
  // while comparing previous
  (this->*str1_load_chr)(tmp1, Address(str1), t0);
  addi(str1, str1, str1_chr_size);
  addi(cnt2, cnt2, -1);
  beqz(cnt2, SHORT_LAST_INIT);
  (this->*str2_load_chr)(cnt1, Address(str2), t0);
  addi(str2, str2, str2_chr_size);
  j(SHORT_LOOP_START);
  bind(SHORT_LOOP);
  addi(cnt2, cnt2, -1);
  beqz(cnt2, SHORT_LAST);
  bind(SHORT_LOOP_START);
  (this->*str1_load_chr)(tmp2, Address(str1), t0);
  addi(str1, str1, str1_chr_size);
  (this->*str2_load_chr)(t0, Address(str2), t0);
  addi(str2, str2, str2_chr_size);
  bne(tmp1, cnt1, SHORT_LOOP_TAIL);
  addi(cnt2, cnt2, -1);
  beqz(cnt2, SHORT_LAST2);
  (this->*str1_load_chr)(tmp1, Address(str1), t0);
  addi(str1, str1, str1_chr_size);
  (this->*str2_load_chr)(cnt1, Address(str2), t0);
  addi(str2, str2, str2_chr_size);
  beq(tmp2, t0, SHORT_LOOP);
  sub(result, tmp2, t0);
  j(DONE);
  bind(SHORT_LOOP_TAIL);
  sub(result, tmp1, cnt1);
  j(DONE);
  bind(SHORT_LAST2);
  beq(tmp2, t0, DONE);
  sub(result, tmp2, t0);

  j(DONE);
  bind(SHORT_LAST_INIT);
  (this->*str2_load_chr)(cnt1, Address(str2), t0);
  addi(str2, str2, str2_chr_size);
  bind(SHORT_LAST);
  beq(tmp1, cnt1, DONE);
  sub(result, tmp1, cnt1);

  bind(DONE);

  BLOCK_COMMENT("} string_compare");
}
#endif // COMPILER2

// string indexof
// compute index by trailing zeros
void MacroAssembler::compute_index(Register haystack, Register trailing_zero,
                                   Register match_mask, Register result,
                                   Register ch2, Register tmp,
                                   bool haystack_isL)
{
  int haystack_chr_shift = haystack_isL ? 0 : 1;
  srl(match_mask, match_mask, trailing_zero);
  srli(match_mask, match_mask, 1);
  srli(tmp, trailing_zero, LogBitsPerByte);
  if (!haystack_isL) andi(tmp, tmp, 0xE);
  add(haystack, haystack, tmp);
  ld(ch2, Address(haystack));
  if (!haystack_isL) srli(tmp, tmp, haystack_chr_shift);
  add(result, result, tmp);
}

// string indexof
// find pattern element in src, compute match mask,
// only the first occurrence of 0x80/0x8000 at low bits is the valid match index
// match mask patterns and corresponding indices would be like:
// - 0x8080808080808080 (Latin1)
// -   7 6 5 4 3 2 1 0  (match index)
// - 0x8000800080008000 (UTF16)
// -   3   2   1   0    (match index)
void MacroAssembler::compute_match_mask(Register src, Register pattern, Register match_mask,
                                        Register mask1, Register mask2)
{
  xorr(src, pattern, src);
  sub(match_mask, src, mask1);
  orr(src, src, mask2);
  notr(src, src);
  andr(match_mask, match_mask, src);
}

// count bits of trailing zero chars from lsb to msb until first non-zero element.
// For LL case, one byte for one element, so shift 8 bits once, and for other case,
// shift 16 bits once.
void MacroAssembler::ctzc_bit(Register Rd, Register Rs, bool isLL, Register Rtmp1, Register Rtmp2)
{
  assert_different_registers(Rd, Rs, Rtmp1, Rtmp2);
  Label Loop;
  int step = isLL ? 8 : 16;
  li(Rd, -step);
  mv(Rtmp2, Rs);

  bind(Loop);
  addi(Rd, Rd, step);
  andi(Rtmp1, Rtmp2, ((1 << step) - 1));
  srli(Rtmp2, Rtmp2, step);
  beqz(Rtmp1, Loop);
}

// This instruction reads adjacent 4 bytes from the lower half of source register,
// inflate into a register, for example:
// Rs: A7A6A5A4A3A2A1A0
// Rd: 00A300A200A100A0
void MacroAssembler::inflate_lo32(Register Rd, Register Rs, Register Rtmp1, Register Rtmp2)
{
  assert_different_registers(Rd, Rs, Rtmp1, Rtmp2);
  li(Rtmp1, 0xFF);
  mv(Rd, zr);
  for (int i = 0; i <= 3; i++)
  {
    andr(Rtmp2, Rs, Rtmp1);
    if (i) {
      slli(Rtmp2, Rtmp2, i * 8);
    }
    orr(Rd, Rd, Rtmp2);
    if (i != 3) {
      slli(Rtmp1, Rtmp1, 8);
    }
  }
}

// This instruction reads adjacent 4 bytes from the upper half of source register,
// inflate into a register, for example:
// Rs: A7A6A5A4A3A2A1A0
// Rd: 00A700A600A500A4
void MacroAssembler::inflate_hi32(Register Rd, Register Rs, Register Rtmp1, Register Rtmp2)
{
  assert_different_registers(Rd, Rs, Rtmp1, Rtmp2);
  li(Rtmp1, 0xFF00000000);
  mv(Rd, zr);
  for (int i = 0; i <= 3; i++)
  {
    andr(Rtmp2, Rs, Rtmp1);
    orr(Rd, Rd, Rtmp2);
    srli(Rd, Rd, 8);
    if (i != 3) {
      slli(Rtmp1, Rtmp1, 8);
    }
  }
}

// The size of the blocks erased by the zero_blocks stub.  We must
// handle anything smaller than this ourselves in zero_words().
const int MacroAssembler::zero_words_block_size = 8;

// zero_words() is used by C2 ClearArray patterns.  It is as small as
// possible, handling small word counts locally and delegating
// anything larger to the zero_blocks stub.  It is expanded many times
// in compiled code, so it is important to keep it short.

// ptr:   Address of a buffer to be zeroed.
// cnt:   Count in HeapWords.
//
// ptr, cnt, and t0 are clobbered.
address MacroAssembler::zero_words(Register ptr, Register cnt)
{
  assert(is_power_of_2(zero_words_block_size), "adjust this");
  assert(ptr == x28 && cnt == x29, "mismatch in register usage");
  assert_different_registers(cnt, t0);

  BLOCK_COMMENT("zero_words {");
  mv(t0, zero_words_block_size);
  Label around, done, done16;
  bltu(cnt, t0, around);
  {
    RuntimeAddress zero_blocks = RuntimeAddress(StubRoutines::riscv64::zero_blocks());
    assert(zero_blocks.target() != NULL, "zero_blocks stub has not been generated");
    if (StubRoutines::riscv64::complete()) {
      address tpc = trampoline_call(zero_blocks);
      if (tpc == NULL) {
        DEBUG_ONLY(reset_labels1(around));
        postcond(pc() == badAddress);
        return NULL;
      }
    } else {
      jal(zero_blocks);
    }
  }
  bind(around);
  for (int i = zero_words_block_size >> 1; i > 1; i >>= 1) {
    Label l;
    andi(t0, cnt, i);
    beqz(t0, l);
    for (int j = 0; j < i; j++) {
      sd(zr, Address(ptr, 0));
      addi(ptr, ptr, 8);
    }
    bind(l);
  }
  {
    Label l;
    andi(t0, cnt, 1);
    beqz(t0, l);
    sd(zr, Address(ptr, 0));
    bind(l);
  }
  BLOCK_COMMENT("} zero_words");
  postcond(pc() != badAddress);
  return pc();
}

// base:         Address of a buffer to be zeroed, 8 bytes aligned.
// cnt:          Immediate count in HeapWords.
#define SmallArraySize (18 * BytesPerLong)
void MacroAssembler::zero_words(Register base, u_int64_t cnt)
{
  assert_different_registers(base, t0, t1);

  BLOCK_COMMENT("zero_words {");

  if (cnt <= SmallArraySize / BytesPerLong) {
    for (int i = 0; i < (int)cnt; i++) {
      sd(zr, Address(base, i * wordSize));
    }
  } else {
    const int unroll = 8; // Number of sd(zr, adr), instructions we'll unroll
    int remainder = cnt %  unroll;
    for (int i = 0; i < remainder; i++) {
      sd(zr, Address(base, i * wordSize));
    }

    Label loop;
    Register cnt_reg = t0;
    Register loop_base = t1;
    cnt = cnt - remainder;
    li(cnt_reg, cnt);
    add(loop_base, base, remainder * wordSize);
    bind(loop);
    sub(cnt_reg, cnt_reg, unroll);
    for (int i = 0; i < unroll; i++) {
      sd(zr, Address(loop_base, i * wordSize));
    }
    add(loop_base, loop_base, unroll * wordSize);
    bnez(cnt_reg, loop);
  }
  BLOCK_COMMENT("} zero_words");
}

// base:   Address of a buffer to be filled, 8 bytes aligned.
// cnt:    Count in 8-byte unit.
// value:  Value to be filled with.
// base will point to the end of the buffer after filling.
void MacroAssembler::fill_words(Register base, Register cnt, Register value)
{
//  Algorithm:
//
//    t0 = cnt & 7
//    cnt -= t0
//    p += t0
//    switch (t0):
//      switch start:
//      do while cnt
//        cnt -= 8
//          p[-8] = value
//        case 7:
//          p[-7] = value
//        case 6:
//          p[-6] = value
//          // ...
//        case 1:
//          p[-1] = value
//        case 0:
//          p += 8
//      do-while end
//    switch end

  assert_different_registers(base, cnt, value, t0, t1);

  Label fini, skip, entry, loop;
  const int unroll = 8; // Number of sd instructions we'll unroll

  beqz(cnt, fini);

  andi(t0, cnt, unroll - 1);
  sub(cnt, cnt, t0);
  slli(t1, t0, 3);
  add(base, base, t1); // align 8, so first sd n % 8 = mod, next loop sd 8 * n.
  la(t1, entry);
  slli(t0, t0, 2); // sd_inst_nums * 4; t0 is cnt % 8, so t1 = t1 - sd_inst_nums * 4, 4 is sizeof(inst)
  sub(t1, t1, t0);
  jr(t1);

  bind(loop);
  add(base, base, unroll * 8);
  for (int i = -unroll; i < 0; i++) {
    sd(value, Address(base, i * 8));
  }
  bind(entry);
  sub(cnt, cnt, unroll);
  bgez(cnt, loop);

  bind(fini);
}

#define FCVT_SAFE(FLOATCVT, FLOATEQ)                                                             \
void MacroAssembler:: FLOATCVT##_safe(Register dst, FloatRegister src, Register temp) {          \
  Label L_Okay;                                                                                  \
  fscsr(zr);                                                                                     \
  FLOATCVT(dst, src);                                                                            \
  frcsr(temp);                                                                                   \
  andi(temp, temp, 0x1E);                                                                        \
  beqz(temp, L_Okay);                                                                            \
  FLOATEQ(temp, src, src);                                                                       \
  bnez(temp, L_Okay);                                                                            \
  mv(dst, zr);                                                                                   \
  bind(L_Okay);                                                                                  \
}

FCVT_SAFE(fcvt_w_s, feq_s)
FCVT_SAFE(fcvt_l_s, feq_s)
FCVT_SAFE(fcvt_w_d, feq_d)
FCVT_SAFE(fcvt_l_d, feq_d)

#undef FCVT_SAFE

#define FCMP(FLOATTYPE, FLOATSIG)                                                       \
void MacroAssembler::FLOATTYPE##_compare(Register result, FloatRegister Rs1,            \
                                         FloatRegister Rs2, int unordered_result) {     \
  Label Ldone;                                                                          \
  if (unordered_result < 0) {                                                           \
    /* we want -1 for unordered or less than, 0 for equal and 1 for greater than. */    \
    /* installs 1 if gt else 0 */                                                       \
    flt_##FLOATSIG(result, Rs2, Rs1);                                                   \
    /* Rs1 > Rs2, install 1 */                                                          \
    bgtz(result, Ldone);                                                                \
    feq_##FLOATSIG(result, Rs1, Rs2);                                                   \
    addi(result, result, -1);                                                           \
    /* Rs1 = Rs2, install 0 */                                                          \
    /* NaN or Rs1 < Rs2, install -1 */                                                  \
    bind(Ldone);                                                                        \
  } else {                                                                              \
    /* we want -1 for less than, 0 for equal and 1 for unordered or greater than. */    \
    /* installs 1 if gt or unordered else 0 */                                          \
    flt_##FLOATSIG(result, Rs1, Rs2);                                                   \
    /* Rs1 < Rs2, install -1 */                                                         \
    bgtz(result, Ldone);                                                                \
    feq_##FLOATSIG(result, Rs1, Rs2);                                                   \
    addi(result, result, -1);                                                           \
    /* Rs1 = Rs2, install 0 */                                                          \
    /* NaN or Rs1 > Rs2, install 1 */                                                   \
    bind(Ldone);                                                                        \
    neg(result, result);                                                                \
  }                                                                                     \
}

FCMP(float, s);
FCMP(double, d);

#undef FCMP

// Zero words; len is in bytes
// Destroys all registers except addr
// len must be a nonzero multiple of wordSize
void MacroAssembler::zero_memory(Register addr, Register len, Register tmp1) {
  assert_different_registers(addr, len, tmp1, t0, t1);

#ifdef ASSERT
  {
    Label L;
    andi(t0, len, BytesPerWord - 1);
    beqz(t0, L);
    stop("len is not a multiple of BytesPerWord");
    bind(L);
  }
#endif // ASSERT

#ifndef PRODUCT
  block_comment("zero memory");
#endif // PRODUCT

  Label loop;
  Label entry;

  // Algorithm:
  //
  //  t0 = cnt & 7
  //  cnt -= t0
  //  p += t0
  //  switch (t0) {
  //    do {
  //      cnt -= 8
  //        p[-8] = 0
  //      case 7:
  //        p[-7] = 0
  //      case 6:
  //        p[-6] = 0
  //        ...
  //      case 1:
  //        p[-1] = 0
  //      case 0:
  //        p += 8
  //     } while (cnt)
  //  }

  const int unroll = 8;   // Number of sd(zr) instructions we'll unroll

  srli(len, len, LogBytesPerWord);
  andi(t0, len, unroll - 1);  // t0 = cnt % unroll
  sub(len, len, t0);          // cnt -= unroll
  // tmp1 always points to the end of the region we're about to zero
  slli(t1, t0, LogBytesPerWord);
  add(tmp1, addr, t1);
  la(t1, entry);
  slli(t0, t0, 2);
  sub(t1, t1, t0);
  jr(t1);
  bind(loop);
  sub(len, len, unroll);
  for (int i = -unroll; i < 0; i++) {
    Assembler::sd(zr, Address(tmp1, i * wordSize));
  }
  bind(entry);
  add(tmp1, tmp1, unroll * wordSize);
  bnez(len, loop);
}

void MacroAssembler::zero_ext(Register dst, Register src, int clear_bits) {
  slli(dst, src, clear_bits);
  srli(dst, dst, clear_bits);
}

void MacroAssembler::sign_ext(Register dst, Register src, int clear_bits) {
  slli(dst, src, clear_bits);
  srai(dst, dst, clear_bits);
}

void MacroAssembler::cmp_l2i(Register dst, Register src1, Register src2, Register tmp)
{
  if (src1 == src2) {
    mv(dst, zr);
    return;
  }
  Label done;
  Register left = src1;
  Register right = src2;
  if (dst == src1) {
    assert_different_registers(dst, src2, tmp);
    mv(tmp, src1);
    left = tmp;
  } else if (dst == src2) {
    assert_different_registers(dst, src1, tmp);
    mv(tmp, src2);
    right = tmp;
  }

  // installs 1 if gt else 0
  slt(dst, right, left);
  bnez(dst, done);
  slt(dst, left, right);
  // dst = -1 if lt; else if eq , dst = 0
  neg(dst, dst);
  bind(done);
}

#ifdef COMPILER2
typedef void (MacroAssembler::* load_chr_insn)(Register rd, const Address &adr, Register temp);

// Search for needle in haystack and return index or -1
// x10: result
// x11: haystack
// x12: haystack_len
// x13: needle
// x14: needle_len
void MacroAssembler::string_indexof(Register haystack, Register needle,
                                       Register haystack_len, Register needle_len,
                                       Register tmp1, Register tmp2,
                                       Register tmp3, Register tmp4,
                                       Register tmp5, Register tmp6,
                                       Register result, int ae)
{
  assert(ae != StrIntrinsicNode::LU, "Invalid encoding");

  Label LINEARSEARCH, LINEARSTUB, DONE, NOMATCH;

  Register ch1 = t0;
  Register ch2 = t1;
  Register nlen_tmp = tmp1; // needle len tmp
  Register hlen_tmp = tmp2; // haystack len tmp
  Register result_tmp = tmp4;

  bool isLL = ae == StrIntrinsicNode::LL;

  bool needle_isL = ae == StrIntrinsicNode::LL || ae == StrIntrinsicNode::UL;
  bool haystack_isL = ae == StrIntrinsicNode::LL || ae == StrIntrinsicNode::LU;
  int needle_chr_shift = needle_isL ? 0 : 1;
  int haystack_chr_shift = haystack_isL ? 0 : 1;
  int needle_chr_size = needle_isL ? 1 : 2;
  int haystack_chr_size = haystack_isL ? 1 : 2;
  load_chr_insn needle_load_1chr = needle_isL ? (load_chr_insn)&MacroAssembler::lbu :
                                   (load_chr_insn)&MacroAssembler::lhu;
  load_chr_insn haystack_load_1chr = haystack_isL ? (load_chr_insn)&MacroAssembler::lbu :
                                     (load_chr_insn)&MacroAssembler::lhu;

  BLOCK_COMMENT("string_indexof {");

  // Note, inline_string_indexOf() generates checks:
  // if (pattern.count > src.count) return -1;
  // if (pattern.count == 0) return 0;

  // We have two strings, a source string in haystack, haystack_len and a pattern string
  // in needle, needle_len. Find the first occurence of pattern in source or return -1.

  // For larger pattern and source we use a simplified Boyer Moore algorithm.
  // With a small pattern and source we use linear scan.

  // needle_len >=8 && needle_len < 256 && needle_len < haystack_len/4, use bmh algorithm.
  sub(result_tmp, haystack_len, needle_len);
  // needle_len < 8, use linear scan
  sub(t0, needle_len, 8);
  bltz(t0, LINEARSEARCH);
  // needle_len >= 256, use linear scan
  sub(t0, needle_len, 256);
  bgez(t0, LINEARSTUB);
  // needle_len >= haystack_len/4, use linear scan
  srli(t0, haystack_len, 2);
  bge(needle_len, t0, LINEARSTUB);

  // Boyer-Moore-Horspool introduction:
  // The Boyer Moore alogorithm is based on the description here:-
  //
  // http://en.wikipedia.org/wiki/Boyer%E2%80%93Moore_string_search_algorithm
  //
  // This describes and algorithm with 2 shift rules. The 'Bad Character' rule
  // and the 'Good Suffix' rule.
  //
  // These rules are essentially heuristics for how far we can shift the
  // pattern along the search string.
  //
  // The implementation here uses the 'Bad Character' rule only because of the
  // complexity of initialisation for the 'Good Suffix' rule.
  //
  // This is also known as the Boyer-Moore-Horspool algorithm:
  //
  // http://en.wikipedia.org/wiki/Boyer-Moore-Horspool_algorithm
  //
  // #define ASIZE 256
  //
  //    int bm(unsigned char *pattern, int m, unsigned char *src, int n) {
  //      int i, j;
  //      unsigned c;
  //      unsigned char bc[ASIZE];
  //
  //      /* Preprocessing */
  //      for (i = 0; i < ASIZE; ++i)
  //        bc[i] = m;
  //      for (i = 0; i < m - 1; ) {
  //        c = pattern[i];
  //        ++i;
  //        // c < 256 for Latin1 string, so, no need for branch
  //        #ifdef PATTERN_STRING_IS_LATIN1
  //        bc[c] = m - i;
  //        #else
  //        if (c < ASIZE) bc[c] = m - i;
  //        #endif
  //      }
  //
  //      /* Searching */
  //      j = 0;
  //      while (j <= n - m) {
  //        c = src[i+j];
  //        if (pattern[m-1] == c)
  //          int k;
  //          for (k = m - 2; k >= 0 && pattern[k] == src[k + j]; --k);
  //          if (k < 0) return j;
  //          // c < 256 for Latin1 string, so, no need for branch
  //          #ifdef SOURCE_STRING_IS_LATIN1_AND_PATTERN_STRING_IS_LATIN1
  //          // LL case: (c< 256) always true. Remove branch
  //          j += bc[pattern[j+m-1]];
  //          #endif
  //          #ifdef SOURCE_STRING_IS_UTF_AND_PATTERN_STRING_IS_UTF
  //          // UU case: need if (c<ASIZE) check. Skip 1 character if not.
  //          if (c < ASIZE)
  //            j += bc[pattern[j+m-1]];
  //          else
  //            j += 1
  //          #endif
  //          #ifdef SOURCE_IS_UTF_AND_PATTERN_IS_LATIN1
  //          // UL case: need if (c<ASIZE) check. Skip <pattern length> if not.
  //          if (c < ASIZE)
  //            j += bc[pattern[j+m-1]];
  //          else
  //            j += m
  //          #endif
  //      }
  //      return -1;
  //    }

  // temp register:t0, tmp1, tmp2, tmp3, tmp4, tmp5, tmp6, result
  Label BCLOOP, BCSKIP, BMLOOPSTR2, BMLOOPSTR1, BMSKIP, BMADV, BMMATCH,
          BMLOOPSTR1_LASTCMP, BMLOOPSTR1_CMP, BMLOOPSTR1_AFTER_LOAD, BM_INIT_LOOP;

  Register haystack_end = haystack_len;
  Register skipch = tmp2;

  // pattern length is >=8, so, we can read at least 1 register for cases when
  // UTF->Latin1 conversion is not needed(8 LL or 4UU) and half register for
  // UL case. We'll re-read last character in inner pre-loop code to have
  // single outer pre-loop load
  const int firstStep = isLL ? 7 : 3;

  const int ASIZE = 256;
  const int STORE_BYTES = 8; // 8 bytes stored per instruction(sd)

  sub(sp, sp, ASIZE);

  // init BC offset table with default value: needle_len
  slli(t0, needle_len, 8);
  orr(t0, t0, needle_len); // [63...16][needle_len][needle_len]
  slli(tmp1, t0, 16);
  orr(t0, tmp1, t0); // [63...32][needle_len][needle_len][needle_len][needle_len]
  slli(tmp1, t0, 32);
  orr(tmp5, tmp1, t0); // tmp5: 8 elements [needle_len]

  mv(ch1, sp);  // ch1 is t0
  mv(tmp6, ASIZE / STORE_BYTES); // loop iterations

  bind(BM_INIT_LOOP);
  // for (i = 0; i < ASIZE; ++i)
  //   bc[i] = m;
  for (int i = 0; i < 4; i++) {
    sd(tmp5, Address(ch1, i * wordSize));
  }
  add(ch1, ch1, 32);
  sub(tmp6, tmp6, 4);
  bgtz(tmp6, BM_INIT_LOOP);

  sub(nlen_tmp, needle_len, 1); // m - 1, index of the last element in pattern
  Register orig_haystack = tmp5;
  mv(orig_haystack, haystack);
  slli(haystack_end, result_tmp, haystack_chr_shift); // result_tmp = tmp4
  add(haystack_end, haystack, haystack_end);
  sub(ch2, needle_len, 1); // bc offset init value, ch2 is t1
  mv(tmp3, needle);

  //  for (i = 0; i < m - 1; ) {
  //    c = pattern[i];
  //    ++i;
  //    // c < 256 for Latin1 string, so, no need for branch
  //    #ifdef PATTERN_STRING_IS_LATIN1
  //    bc[c] = m - i;
  //    #else
  //    if (c < ASIZE) bc[c] = m - i;
  //    #endif
  //  }
  bind(BCLOOP);
  (this->*needle_load_1chr)(ch1, Address(tmp3), noreg);
  add(tmp3, tmp3, needle_chr_size);
  if (!needle_isL) {
    // ae == StrIntrinsicNode::UU
    mv(tmp6, ASIZE);
    bgeu(ch1, tmp6, BCSKIP);
  }
  add(tmp4, sp, ch1);
  sb(ch2, Address(tmp4)); // store skip offset to BC offset table

  bind(BCSKIP);
  sub(ch2, ch2, 1); // for next pattern element, skip distance -1
  bgtz(ch2, BCLOOP);

  slli(tmp6, needle_len, needle_chr_shift);
  add(tmp6, tmp6, needle); // tmp6: pattern end, address after needle
  if (needle_isL == haystack_isL) {
    // load last 8 bytes (8LL/4UU symbols)
    ld(tmp6, Address(tmp6, -wordSize));
  } else {
    // UL: from UTF-16(source) search Latin1(pattern)
    lwu(tmp6, Address(tmp6, -wordSize / 2)); // load last 4 bytes(4 symbols)
    // convert Latin1 to UTF. eg: 0x0000abcd -> 0x0a0b0c0d
    // We'll have to wait until load completed, but it's still faster than per-character loads+checks
    srli(tmp3, tmp6, BitsPerByte * (wordSize / 2 - needle_chr_size)); // pattern[m-1], eg:0x0000000a
    slli(ch2, tmp6, registerSize - 24);
    srli(ch2, ch2, registerSize - 8); // pattern[m-2], 0x0000000b
    slli(ch1, tmp6, registerSize - 16);
    srli(ch1, ch1, registerSize - 8); // pattern[m-3], 0x0000000c
    andi(tmp6, tmp6, 0xff); // pattern[m-4], 0x0000000d
    slli(ch2, ch2, 16);
    orr(ch2, ch2, ch1); // 0x00000b0c
    slli(result, tmp3, 48); // use result as temp register
    orr(tmp6, tmp6, result); // 0x0a00000d
    slli(result, ch2, 16);
    orr(tmp6, tmp6, result); // UTF-16:0x0a0b0c0d
  }

  // i = m - 1;
  // skipch = j + i;
  // if (skipch == pattern[m - 1]
  //   for (k = m - 2; k >= 0 && pattern[k] == src[k + j]; --k);
  // else
  //   move j with bad char offset table
  bind(BMLOOPSTR2);
  // compare pattern to source string backward
  slli(result, nlen_tmp, haystack_chr_shift);
  add(result, haystack, result);
  (this->*haystack_load_1chr)(skipch, Address(result), noreg);
  sub(nlen_tmp, nlen_tmp, firstStep); // nlen_tmp is positive here, because needle_len >= 8
  if (needle_isL == haystack_isL) {
    // re-init tmp3. It's for free because it's executed in parallel with
    // load above. Alternative is to initialize it before loop, but it'll
    // affect performance on in-order systems with 2 or more ld/st pipelines
    srli(tmp3, tmp6, BitsPerByte * (wordSize - needle_chr_size)); // UU/LL: pattern[m-1]
  }
  if (!isLL) { // UU/UL case
    slli(ch2, nlen_tmp, 1); // offsets in bytes
  }
  bne(tmp3, skipch, BMSKIP); // if not equal, skipch is bad char
  add(result, haystack, isLL ? nlen_tmp : ch2);
  ld(ch2, Address(result)); // load 8 bytes from source string
  mv(ch1, tmp6);
  if (isLL) {
    j(BMLOOPSTR1_AFTER_LOAD);
  } else {
    sub(nlen_tmp, nlen_tmp, 1); // no need to branch for UU/UL case. cnt1 >= 8
    j(BMLOOPSTR1_CMP);
  }

  bind(BMLOOPSTR1);
  slli(ch1, nlen_tmp, needle_chr_shift);
  add(ch1, ch1, needle);
  (this->*needle_load_1chr)(ch1, Address(ch1), noreg);
  slli(ch2, nlen_tmp, haystack_chr_shift);
  add(ch2, haystack, ch2);
  (this->*haystack_load_1chr)(ch2, Address(ch2), noreg);

  bind(BMLOOPSTR1_AFTER_LOAD);
  sub(nlen_tmp, nlen_tmp, 1);
  bltz(nlen_tmp, BMLOOPSTR1_LASTCMP);

  bind(BMLOOPSTR1_CMP);
  beq(ch1, ch2, BMLOOPSTR1);

  bind(BMSKIP);
  if (!isLL) {
    // if we've met UTF symbol while searching Latin1 pattern, then we can
    // skip needle_len symbols
    if (needle_isL != haystack_isL) {
      mv(result_tmp, needle_len);
    } else {
      mv(result_tmp, 1);
    }
    mv(t0, ASIZE);
    bgeu(skipch, t0, BMADV);
  }
  add(result_tmp, sp, skipch);
  lbu(result_tmp, Address(result_tmp)); // load skip offset

  bind(BMADV);
  sub(nlen_tmp, needle_len, 1);
  slli(result, result_tmp, haystack_chr_shift);
  add(haystack, haystack, result); // move haystack after bad char skip offset
  ble(haystack, haystack_end, BMLOOPSTR2);
  add(sp, sp, ASIZE);
  j(NOMATCH);

  bind(BMLOOPSTR1_LASTCMP);
  bne(ch1, ch2, BMSKIP);

  bind(BMMATCH);
  sub(result, haystack, orig_haystack);
  if (!haystack_isL) {
    srli(result, result, 1);
  }
  add(sp, sp, ASIZE);
  j(DONE);

  bind(LINEARSTUB);
  sub(t0, needle_len, 16); // small patterns still should be handled by simple algorithm
  bltz(t0, LINEARSEARCH);
  mv(result, zr);
  RuntimeAddress stub = NULL;
  if (isLL) {
    stub = RuntimeAddress(StubRoutines::riscv64::string_indexof_linear_ll());
    assert(stub.target() != NULL, "string_indexof_linear_ll stub has not been generated");
  } else if (needle_isL) {
    stub = RuntimeAddress(StubRoutines::riscv64::string_indexof_linear_ul());
    assert(stub.target() != NULL, "string_indexof_linear_ul stub has not been generated");
  } else {
    stub = RuntimeAddress(StubRoutines::riscv64::string_indexof_linear_uu());
    assert(stub.target() != NULL, "string_indexof_linear_uu stub has not been generated");
  }
  trampoline_call(stub);
  j(DONE);

  bind(NOMATCH);
  mv(result, -1);
  j(DONE);

  bind(LINEARSEARCH);
  string_indexof_linearscan(haystack, needle, haystack_len, needle_len, tmp1, tmp2, tmp3, tmp4, -1, result, ae);

  bind(DONE);
  BLOCK_COMMENT("} string_indexof");
}

// string_indexof
// result: x10
// src: x11
// src_count: x12
// pattern: x13
// pattern_count: x14 or 1/2/3/4
void MacroAssembler::string_indexof_linearscan(Register haystack, Register needle,
                                                  Register haystack_len, Register needle_len,
                                                  Register tmp1, Register tmp2,
                                                  Register tmp3, Register tmp4,
                                                  int needle_con_cnt, Register result, int ae)
{
  // Note:
  // needle_con_cnt > 0 means needle_len register is invalid, needle length is constant
  // for UU/LL: needle_con_cnt[1, 4], UL: needle_con_cnt = 1
  assert(needle_con_cnt <= 4, "Invalid needle constant count");
  assert(ae != StrIntrinsicNode::LU, "Invalid encoding");

  Register ch1 = t0;
  Register ch2 = t1;
  Register hlen_neg = haystack_len, nlen_neg = needle_len;
  Register nlen_tmp = tmp1, hlen_tmp = tmp2, result_tmp = tmp4;

  bool isLL = ae == StrIntrinsicNode::LL;

  bool needle_isL = ae == StrIntrinsicNode::LL || ae == StrIntrinsicNode::UL;
  bool haystack_isL = ae == StrIntrinsicNode::LL || ae == StrIntrinsicNode::LU;
  int needle_chr_shift = needle_isL ? 0 : 1;
  int haystack_chr_shift = haystack_isL ? 0 : 1;
  int needle_chr_size = needle_isL ? 1 : 2;
  int haystack_chr_size = haystack_isL ? 1 : 2;

  load_chr_insn needle_load_1chr = needle_isL ? (load_chr_insn)&MacroAssembler::lbu :
                                   (load_chr_insn)&MacroAssembler::lhu;
  load_chr_insn haystack_load_1chr = haystack_isL ? (load_chr_insn)&MacroAssembler::lbu :
                                     (load_chr_insn)&MacroAssembler::lhu;
  load_chr_insn load_2chr = isLL ? (load_chr_insn)&MacroAssembler::lhu : (load_chr_insn)&MacroAssembler::lwu;
  load_chr_insn load_4chr = isLL ? (load_chr_insn)&MacroAssembler::lwu : (load_chr_insn)&MacroAssembler::ld;

  Label DO1, DO2, DO3, MATCH, NOMATCH, DONE;

  Register first = tmp3;

  if (needle_con_cnt == -1) {
    Label DOSHORT, FIRST_LOOP, STR2_NEXT, STR1_LOOP, STR1_NEXT;

    sub(t0, needle_len, needle_isL == haystack_isL ? 4 : 2);
    bltz(t0, DOSHORT);

    (this->*needle_load_1chr)(first, Address(needle), noreg);
    slli(t0, needle_len, needle_chr_shift);
    add(needle, needle, t0);
    neg(nlen_neg, t0);
    slli(t0, result_tmp, haystack_chr_shift);
    add(haystack, haystack, t0);
    neg(hlen_neg, t0);

    bind(FIRST_LOOP);
    add(t0, haystack, hlen_neg);
    (this->*haystack_load_1chr)(ch2, Address(t0), noreg);
    beq(first, ch2, STR1_LOOP);

    bind(STR2_NEXT);
    add(hlen_neg, hlen_neg, haystack_chr_size);
    blez(hlen_neg, FIRST_LOOP);
    j(NOMATCH);

    bind(STR1_LOOP);
    add(nlen_tmp, nlen_neg, needle_chr_size);
    add(hlen_tmp, hlen_neg, haystack_chr_size);
    bgez(nlen_tmp, MATCH);

    bind(STR1_NEXT);
    add(ch1, needle, nlen_tmp);
    (this->*needle_load_1chr)(ch1, Address(ch1), noreg);
    add(ch2, haystack, hlen_tmp);
    (this->*haystack_load_1chr)(ch2, Address(ch2), noreg);
    bne(ch1, ch2, STR2_NEXT);
    add(nlen_tmp, nlen_tmp, needle_chr_size);
    add(hlen_tmp, hlen_tmp, haystack_chr_size);
    bltz(nlen_tmp, STR1_NEXT);
    j(MATCH);

    bind(DOSHORT);
    if (needle_isL == haystack_isL) {
      sub(t0, needle_len, 2);
      bltz(t0, DO1);
      bgtz(t0, DO3);
    }
  }

  if (needle_con_cnt == 4) {
    Label CH1_LOOP;
    (this->*load_4chr)(ch1, Address(needle), noreg);
    sub(result_tmp, haystack_len, 4);
    slli(tmp3, result_tmp, haystack_chr_shift); // result as tmp
    add(haystack, haystack, tmp3);
    neg(hlen_neg, tmp3);

    bind(CH1_LOOP);
    add(ch2, haystack, hlen_neg);
    (this->*load_4chr)(ch2, Address(ch2), noreg);
    beq(ch1, ch2, MATCH);
    add(hlen_neg, hlen_neg, haystack_chr_size);
    blez(hlen_neg, CH1_LOOP);
    j(NOMATCH);
  }

  if ((needle_con_cnt == -1 && needle_isL == haystack_isL) || needle_con_cnt == 2) {
    Label CH1_LOOP;
    BLOCK_COMMENT("string_indexof DO2 {");
    bind(DO2);
    (this->*load_2chr)(ch1, Address(needle), noreg);
    if (needle_con_cnt == 2) {
      sub(result_tmp, haystack_len, 2);
    }
    slli(tmp3, result_tmp, haystack_chr_shift);
    add(haystack, haystack, tmp3);
    neg(hlen_neg, tmp3);

    bind(CH1_LOOP);
    add(tmp3, haystack, hlen_neg);
    (this->*load_2chr)(ch2, Address(tmp3), noreg);
    beq(ch1, ch2, MATCH);
    add(hlen_neg, hlen_neg, haystack_chr_size);
    blez(hlen_neg, CH1_LOOP);
    j(NOMATCH);
    BLOCK_COMMENT("} string_indexof DO2");
  }

  if ((needle_con_cnt == -1 && needle_isL == haystack_isL) || needle_con_cnt == 3) {
    Label FIRST_LOOP, STR2_NEXT, STR1_LOOP;
    BLOCK_COMMENT("string_indexof DO3 {");

    bind(DO3);
    (this->*load_2chr)(first, Address(needle), noreg);
    (this->*needle_load_1chr)(ch1, Address(needle, 2 * needle_chr_size), noreg);
    if (needle_con_cnt == 3) {
      sub(result_tmp, haystack_len, 3);
    }
    slli(hlen_tmp, result_tmp, haystack_chr_shift);
    add(haystack, haystack, hlen_tmp);
    neg(hlen_neg, hlen_tmp);

    bind(FIRST_LOOP);
    add(ch2, haystack, hlen_neg);
    (this->*load_2chr)(ch2, Address(ch2), noreg);
    beq(first, ch2, STR1_LOOP);

    bind(STR2_NEXT);
    add(hlen_neg, hlen_neg, haystack_chr_size);
    blez(hlen_neg, FIRST_LOOP);
    j(NOMATCH);

    bind(STR1_LOOP);
    add(hlen_tmp, hlen_neg, 2 * haystack_chr_size);
    add(ch2, haystack, hlen_tmp);
    (this->*haystack_load_1chr)(ch2, Address(ch2), noreg);
    bne(ch1, ch2, STR2_NEXT);
    j(MATCH);
    BLOCK_COMMENT("} string_indexof DO3");
  }

  if (needle_con_cnt == -1 || needle_con_cnt == 1) {
    Label DO1_LOOP;

    BLOCK_COMMENT("string_indexof DO1 {");
    bind(DO1);
    (this->*needle_load_1chr)(ch1, Address(needle), noreg);
    sub(result_tmp, haystack_len, 1);
    mv(tmp3, result_tmp);
    if (haystack_chr_shift) {
      slli(tmp3, result_tmp, haystack_chr_shift);
    }
    add(haystack, haystack, tmp3);
    neg(hlen_neg, tmp3);

    bind(DO1_LOOP);
    add(tmp3, haystack, hlen_neg);
    (this->*haystack_load_1chr)(ch2, Address(tmp3), noreg);
    beq(ch1, ch2, MATCH);
    add(hlen_neg, hlen_neg, haystack_chr_size);
    blez(hlen_neg, DO1_LOOP);
    BLOCK_COMMENT("} string_indexof DO1");
  }

  bind(NOMATCH);
  mv(result, -1);
  j(DONE);

  bind(MATCH);
  srai(t0, hlen_neg, haystack_chr_shift);
  add(result, result_tmp, t0);

  bind(DONE);
}

void MacroAssembler::enc_cmove(int cmpFlag, Register op1, Register op2, Register dst, Register src) {
  Label L;
  cmp_branch(cmpFlag ^ (1 << neg_cond_bits), op1, op2, L);
  mv(dst, src);
  bind(L);
}

// Set dst to NaN if any NaN input.
void MacroAssembler::minmax_FD(FloatRegister dst, FloatRegister src1, FloatRegister src2,
                                  bool is_double, bool is_min) {
  assert_different_registers(dst, src1, src2);

  Label Done;
  fsflags(zr);
  if (is_double) {
    is_min ? fmin_d(dst, src1, src2)
           : fmax_d(dst, src1, src2);
    // Checking NaNs
    flt_d(zr, src1, src2);
  } else {
    is_min ? fmin_s(dst, src1, src2)
           : fmax_s(dst, src1, src2);
    // Checking NaNs
    flt_s(zr, src1, src2);
  }

  frflags(t0);
  beqz(t0, Done);

  // In case of NaNs
  is_double ? fadd_d(dst, src1, src2)
            : fadd_s(dst, src1, src2);

  bind(Done);
}
#endif // COMPILER2
