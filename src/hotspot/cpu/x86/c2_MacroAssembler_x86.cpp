/*
 * Copyright (c) 1997, 2018, Oracle and/or its affiliates. All rights reserved.
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
#include "jvm.h"
#include "asm/assembler.hpp"
#include "asm/assembler.inline.hpp"
#include "opto/c2_MacroAssembler.hpp"
#include "compiler/disassembler.hpp"
#include "gc/shared/barrierSet.hpp"
#include "gc/shared/barrierSetAssembler.hpp"
#include "gc/shared/collectedHeap.inline.hpp"
#include "interpreter/interpreter.hpp"
#include "memory/resourceArea.hpp"
#include "memory/universe.hpp"
#include "oops/accessDecorators.hpp"
#include "oops/klass.inline.hpp"
#include "prims/methodHandles.hpp"
#include "opto/subnode.hpp"
#include "runtime/biasedLocking.hpp"
#include "runtime/coroutine.hpp"
#include "runtime/flags/flagSetting.hpp"
#include "runtime/interfaceSupport.inline.hpp"
#include "runtime/objectMonitor.hpp"
#include "runtime/os.hpp"
#include "runtime/safepoint.hpp"
#include "runtime/safepointMechanism.hpp"
#include "runtime/sharedRuntime.hpp"
#include "runtime/stubRoutines.hpp"
#include "runtime/thread.hpp"
#include "utilities/macros.hpp"
#include "crc32c.h"
#ifdef COMPILER2
#include "opto/intrinsicnode.hpp"
#endif

#ifdef COMPILER2

inline Assembler::AvxVectorLen C2_MacroAssembler::vector_length_encoding(int vlen_in_bytes) {
  switch (vlen_in_bytes) {
    case  4: // fall-through
    case  8: // fall-through
    case 16: return Assembler::AVX_128bit;
    case 32: return Assembler::AVX_256bit;
    case 64: return Assembler::AVX_512bit;

    default: {
      ShouldNotReachHere();
      return Assembler::AVX_NoVec;
    }
  }
}

void C2_MacroAssembler::pminmax(int opcode, BasicType elem_bt, XMMRegister dst, XMMRegister src, XMMRegister tmp) {
  assert(opcode == Op_MinV || opcode == Op_MaxV, "sanity");
  assert(tmp == xnoreg || elem_bt == T_LONG, "unused");

  if (opcode == Op_MinV) {
    if (elem_bt == T_BYTE) {
      pminsb(dst, src);
    } else if (elem_bt == T_SHORT) {
      pminsw(dst, src);
    } else if (elem_bt == T_INT) {
      pminsd(dst, src);
    } else {
      assert(elem_bt == T_LONG, "required");
      assert(tmp == xmm0, "required");
      assert_different_registers(dst, src, tmp);
      movdqu(xmm0, dst);
      pcmpgtq(xmm0, src);
      blendvpd(dst, src);  // xmm0 as mask
    }
  } else { // opcode == Op_MaxV
    if (elem_bt == T_BYTE) {
      pmaxsb(dst, src);
    } else if (elem_bt == T_SHORT) {
      pmaxsw(dst, src);
    } else if (elem_bt == T_INT) {
      pmaxsd(dst, src);
    } else {
      assert(elem_bt == T_LONG, "required");
      assert(tmp == xmm0, "required");
      assert_different_registers(dst, src, tmp);
      movdqu(xmm0, src);
      pcmpgtq(xmm0, dst);
      blendvpd(dst, src);  // xmm0 as mask
    }
  }
}

void C2_MacroAssembler::vpminmax(int opcode, BasicType elem_bt,
                                 XMMRegister dst, XMMRegister src1, XMMRegister src2,
                                 int vlen_enc) {
  assert(opcode == Op_MinV || opcode == Op_MaxV, "sanity");

  if (opcode == Op_MinV) {
    if (elem_bt == T_BYTE) {
      vpminsb(dst, src1, src2, vlen_enc);
    } else if (elem_bt == T_SHORT) {
      vpminsw(dst, src1, src2, vlen_enc);
    } else if (elem_bt == T_INT) {
      vpminsd(dst, src1, src2, vlen_enc);
    } else {
      assert(elem_bt == T_LONG, "required");
      if (UseAVX > 2 && (vlen_enc == Assembler::AVX_512bit || VM_Version::supports_avx512vl())) {
        vpminsq(dst, src1, src2, vlen_enc);
      } else {
        assert_different_registers(dst, src1, src2);
        vpcmpgtq(dst, src1, src2, vlen_enc);
        vblendvpd(dst, src1, src2, dst, vlen_enc);
      }
    }
  } else { // opcode == Op_MaxV
    if (elem_bt == T_BYTE) {
      vpmaxsb(dst, src1, src2, vlen_enc);
    } else if (elem_bt == T_SHORT) {
      vpmaxsw(dst, src1, src2, vlen_enc);
    } else if (elem_bt == T_INT) {
      vpmaxsd(dst, src1, src2, vlen_enc);
    } else {
      assert(elem_bt == T_LONG, "required");
      if (UseAVX > 2 && (vlen_enc == Assembler::AVX_512bit || VM_Version::supports_avx512vl())) {
        vpmaxsq(dst, src1, src2, vlen_enc);
      } else {
        assert_different_registers(dst, src1, src2);
        vpcmpgtq(dst, src1, src2, vlen_enc);
        vblendvpd(dst, src2, src1, dst, vlen_enc);
      }
    }
  }
}

// Float/Double min max

void C2_MacroAssembler::vminmax_fp(int opcode, BasicType elem_bt,
                                   XMMRegister dst, XMMRegister a, XMMRegister b,
                                   XMMRegister tmp, XMMRegister atmp, XMMRegister btmp,
                                   int vlen_enc) {
  assert(UseAVX > 0, "required");
  assert(opcode == Op_MinV || opcode == Op_MinReductionV ||
         opcode == Op_MaxV || opcode == Op_MaxReductionV, "sanity");
  assert(elem_bt == T_FLOAT || elem_bt == T_DOUBLE, "sanity");
  assert_different_registers(a, b, tmp, atmp, btmp);

  bool is_min = (opcode == Op_MinV || opcode == Op_MinReductionV);
  bool is_double_word = is_double_word_type(elem_bt);

  if (!is_double_word && is_min) {
    vblendvps(atmp, a, b, a, vlen_enc);
    vblendvps(btmp, b, a, a, vlen_enc);
    vminps(tmp, atmp, btmp, vlen_enc);
    vcmpps(btmp, atmp, atmp, Assembler::UNORD_Q, vlen_enc);
    vblendvps(dst, tmp, atmp, btmp, vlen_enc);
  } else if (!is_double_word && !is_min) {
    vblendvps(btmp, b, a, b, vlen_enc);
    vblendvps(atmp, a, b, b, vlen_enc);
    vmaxps(tmp, atmp, btmp, vlen_enc);
    vcmpps(btmp, atmp, atmp, Assembler::UNORD_Q, vlen_enc);
    vblendvps(dst, tmp, atmp, btmp, vlen_enc);
  } else if (is_double_word && is_min) {
    vblendvpd(atmp, a, b, a, vlen_enc);
    vblendvpd(btmp, b, a, a, vlen_enc);
    vminpd(tmp, atmp, btmp, vlen_enc);
    vcmppd(btmp, atmp, atmp, Assembler::UNORD_Q, vlen_enc);
    vblendvpd(dst, tmp, atmp, btmp, vlen_enc);
  } else {
    assert(is_double_word && !is_min, "sanity");
    vblendvpd(btmp, b, a, b, vlen_enc);
    vblendvpd(atmp, a, b, b, vlen_enc);
    vmaxpd(tmp, atmp, btmp, vlen_enc);
    vcmppd(btmp, atmp, atmp, Assembler::UNORD_Q, vlen_enc);
    vblendvpd(dst, tmp, atmp, btmp, vlen_enc);
  }
}

void C2_MacroAssembler::evminmax_fp(int opcode, BasicType elem_bt,
                                    XMMRegister dst, XMMRegister a, XMMRegister b,
                                    KRegister ktmp, XMMRegister atmp, XMMRegister btmp,
                                    int vlen_enc) {
  assert(UseAVX > 2, "required");
  assert(opcode == Op_MinV || opcode == Op_MinReductionV ||
         opcode == Op_MaxV || opcode == Op_MaxReductionV, "sanity");
  assert(elem_bt == T_FLOAT || elem_bt == T_DOUBLE, "sanity");
  assert_different_registers(dst, a, b, atmp, btmp);

  bool is_min = (opcode == Op_MinV || opcode == Op_MinReductionV);
  bool is_double_word = is_double_word_type(elem_bt);
  bool merge = true;

  if (!is_double_word && is_min) {
    evpmovd2m(ktmp, a, vlen_enc);
    evblendmps(atmp, ktmp, a, b, merge, vlen_enc);
    evblendmps(btmp, ktmp, b, a, merge, vlen_enc);
    vminps(dst, atmp, btmp, vlen_enc);
    evcmpps(ktmp, k0, atmp, atmp, Assembler::UNORD_Q, vlen_enc);
    evmovdqul(dst, ktmp, atmp, merge, vlen_enc);
  } else if (!is_double_word && !is_min) {
    evpmovd2m(ktmp, b, vlen_enc);
    evblendmps(atmp, ktmp, a, b, merge, vlen_enc);
    evblendmps(btmp, ktmp, b, a, merge, vlen_enc);
    vmaxps(dst, atmp, btmp, vlen_enc);
    evcmpps(ktmp, k0, atmp, atmp, Assembler::UNORD_Q, vlen_enc);
    evmovdqul(dst, ktmp, atmp, merge, vlen_enc);
  } else if (is_double_word && is_min) {
    evpmovq2m(ktmp, a, vlen_enc);
    evblendmpd(atmp, ktmp, a, b, merge, vlen_enc);
    evblendmpd(btmp, ktmp, b, a, merge, vlen_enc);
    vminpd(dst, atmp, btmp, vlen_enc);
    evcmppd(ktmp, k0, atmp, atmp, Assembler::UNORD_Q, vlen_enc);
    evmovdquq(dst, ktmp, atmp, merge, vlen_enc);
  } else {
    assert(is_double_word && !is_min, "sanity");
    evpmovq2m(ktmp, b, vlen_enc);
    evblendmpd(atmp, ktmp, a, b, merge, vlen_enc);
    evblendmpd(btmp, ktmp, b, a, merge, vlen_enc);
    vmaxpd(dst, atmp, btmp, vlen_enc);
    evcmppd(ktmp, k0, atmp, atmp, Assembler::UNORD_Q, vlen_enc);
    evmovdquq(dst, ktmp, atmp, merge, vlen_enc);
  }
}

void C2_MacroAssembler::vextendbd(bool sign, XMMRegister dst, XMMRegister src, int vector_len) {
  if (sign) {
    vpmovsxbd(dst, src, vector_len);
  } else {
    vpmovzxbd(dst, src, vector_len);
  }
}

void C2_MacroAssembler::vextendwd(bool sign, XMMRegister dst, XMMRegister src, int vector_len) {
  if (sign) {
    vpmovsxwd(dst, src, vector_len);
  } else {
    vpmovzxwd(dst, src, vector_len);
  }
}

void C2_MacroAssembler::vshiftd(int opcode, XMMRegister dst, XMMRegister shift) {
  switch (opcode) {
    case Op_RShiftVI:  psrad(dst, shift); break;
    case Op_LShiftVI:  pslld(dst, shift); break;
    case Op_URShiftVI: psrld(dst, shift); break;

    default: assert(false, "%s", NodeClassNames[opcode]);
  }
}

void C2_MacroAssembler::vshiftd(int opcode, XMMRegister dst, XMMRegister src, XMMRegister shift, int vlen_enc) {
  switch (opcode) {
    case Op_RShiftVI:  vpsrad(dst, src, shift, vlen_enc); break;
    case Op_LShiftVI:  vpslld(dst, src, shift, vlen_enc); break;
    case Op_URShiftVI: vpsrld(dst, src, shift, vlen_enc); break;

    default: assert(false, "%s", NodeClassNames[opcode]);
  }
}

void C2_MacroAssembler::vshiftw(int opcode, XMMRegister dst, XMMRegister shift) {
  switch (opcode) {
    case Op_RShiftVB:  // fall-through
    case Op_RShiftVS:  psraw(dst, shift); break;

    case Op_LShiftVB:  // fall-through
    case Op_LShiftVS:  psllw(dst, shift);   break;

    case Op_URShiftVS: // fall-through
    case Op_URShiftVB: psrlw(dst, shift);  break;

    default: assert(false, "%s", NodeClassNames[opcode]);
  }
}

void C2_MacroAssembler::vshiftw(int opcode, XMMRegister dst, XMMRegister src, XMMRegister shift, int vlen_enc) {
  switch (opcode) {
    case Op_RShiftVB:  // fall-through
    case Op_RShiftVS:  vpsraw(dst, src, shift, vlen_enc); break;

    case Op_LShiftVB:  // fall-through
    case Op_LShiftVS:  vpsllw(dst, src, shift, vlen_enc); break;

    case Op_URShiftVS: // fall-through
    case Op_URShiftVB: vpsrlw(dst, src, shift, vlen_enc); break;

    default: assert(false, "%s", NodeClassNames[opcode]);
  }
}

void C2_MacroAssembler::vshiftq(int opcode, XMMRegister dst, XMMRegister shift) {
  switch (opcode) {
    case Op_RShiftVL:  psrlq(dst, shift); break; // using srl to implement sra on pre-avs512 systems
    case Op_LShiftVL:  psllq(dst, shift); break;
    case Op_URShiftVL: psrlq(dst, shift); break;

    default: assert(false, "%s", NodeClassNames[opcode]);
  }
}

void C2_MacroAssembler::vshiftq(int opcode, XMMRegister dst, XMMRegister src, XMMRegister shift, int vlen_enc) {
  switch (opcode) {
    case Op_RShiftVL: evpsraq(dst, src, shift, vlen_enc); break;
    case Op_LShiftVL:  vpsllq(dst, src, shift, vlen_enc); break;
    case Op_URShiftVL: vpsrlq(dst, src, shift, vlen_enc); break;

    default: assert(false, "%s", NodeClassNames[opcode]);
  }
}

void C2_MacroAssembler::varshiftd(int opcode, XMMRegister dst, XMMRegister src, XMMRegister shift, int vlen_enc) {
  switch (opcode) {
    case Op_RShiftVB:  // fall-through
    case Op_RShiftVS:  // fall-through
    case Op_RShiftVI:  vpsravd(dst, src, shift, vlen_enc); break;

    case Op_LShiftVB:  // fall-through
    case Op_LShiftVS:  // fall-through
    case Op_LShiftVI:  vpsllvd(dst, src, shift, vlen_enc); break;

    case Op_URShiftVB: // fall-through
    case Op_URShiftVS: // fall-through
    case Op_URShiftVI: vpsrlvd(dst, src, shift, vlen_enc); break;

    default: assert(false, "%s", NodeClassNames[opcode]);
  }
}

void C2_MacroAssembler::varshiftw(int opcode, XMMRegister dst, XMMRegister src, XMMRegister shift, int vlen_enc) {
  switch (opcode) {
    case Op_RShiftVB:  // fall-through
    case Op_RShiftVS:  evpsravw(dst, src, shift, vlen_enc); break;

    case Op_LShiftVB:  // fall-through
    case Op_LShiftVS:  evpsllvw(dst, src, shift, vlen_enc); break;

    case Op_URShiftVB: // fall-through
    case Op_URShiftVS: evpsrlvw(dst, src, shift, vlen_enc); break;

    default: assert(false, "%s", NodeClassNames[opcode]);
  }
}

void C2_MacroAssembler::varshiftq(int opcode, XMMRegister dst, XMMRegister src, XMMRegister shift, int vlen_enc, XMMRegister tmp) {
  assert(UseAVX >= 2, "required");
  switch (opcode) {
    case Op_RShiftVL: {
      if (UseAVX > 2) {
        assert(tmp == xnoreg, "not used");
        if (!VM_Version::supports_avx512vl()) {
          vlen_enc = Assembler::AVX_512bit;
        }
        evpsravq(dst, src, shift, vlen_enc);
      } else {
        vmovdqu(tmp, ExternalAddress(StubRoutines::x86::vector_long_sign_mask()));
        vpsrlvq(dst, src, shift, vlen_enc);
        vpsrlvq(tmp, tmp, shift, vlen_enc);
        vpxor(dst, dst, tmp, vlen_enc);
        vpsubq(dst, dst, tmp, vlen_enc);
      }
      break;
    }
    case Op_LShiftVL: {
      assert(tmp == xnoreg, "not used");
      vpsllvq(dst, src, shift, vlen_enc);
      break;
    }
    case Op_URShiftVL: {
      assert(tmp == xnoreg, "not used");
      vpsrlvq(dst, src, shift, vlen_enc);
      break;
    }
    default: assert(false, "%s", NodeClassNames[opcode]);
  }
}

// Variable shift src by shift using vtmp and scratch as TEMPs giving word result in dst
void C2_MacroAssembler::varshiftbw(int opcode, XMMRegister dst, XMMRegister src, XMMRegister shift, int vector_len, XMMRegister vtmp, Register scratch) {
  assert(opcode == Op_LShiftVB ||
         opcode == Op_RShiftVB ||
         opcode == Op_URShiftVB, "%s", NodeClassNames[opcode]);
  bool sign = (opcode != Op_URShiftVB);
  assert(vector_len == 0, "required");
  vextendbd(sign, dst, src, 1);
  vpmovzxbd(vtmp, shift, 1);
  varshiftd(opcode, dst, dst, vtmp, 1);
  vpand(dst, dst, ExternalAddress(StubRoutines::x86::vector_int_to_byte_mask()), 1, scratch);
  vextracti128_high(vtmp, dst);
  vpackusdw(dst, dst, vtmp, 0);
}

// Variable shift src by shift using vtmp and scratch as TEMPs giving byte result in dst
void C2_MacroAssembler::evarshiftb(int opcode, XMMRegister dst, XMMRegister src, XMMRegister shift, int vector_len, XMMRegister vtmp, Register scratch) {
  assert(opcode == Op_LShiftVB ||
         opcode == Op_RShiftVB ||
         opcode == Op_URShiftVB, "%s", NodeClassNames[opcode]);
  bool sign = (opcode != Op_URShiftVB);
  int ext_vector_len = vector_len + 1;
  vextendbw(sign, dst, src, ext_vector_len);
  vpmovzxbw(vtmp, shift, ext_vector_len);
  varshiftw(opcode, dst, dst, vtmp, ext_vector_len);
  vpand(dst, dst, ExternalAddress(StubRoutines::x86::vector_short_to_byte_mask()), ext_vector_len, scratch);
  if (vector_len == 0) {
    vextracti128_high(vtmp, dst);
    vpackuswb(dst, dst, vtmp, vector_len);
  } else {
    vextracti64x4_high(vtmp, dst);
    vpackuswb(dst, dst, vtmp, vector_len);
    vpermq(dst, dst, 0xD8, vector_len);
  }
}

void C2_MacroAssembler::insert(BasicType typ, XMMRegister dst, Register val, int idx) {
  switch(typ) {
    case T_BYTE:
      pinsrb(dst, val, idx);
      break;
    case T_SHORT:
      pinsrw(dst, val, idx);
      break;
    case T_INT:
      pinsrd(dst, val, idx);
      break;
    case T_LONG:
      pinsrq(dst, val, idx);
      break;
    default:
      assert(false,"Should not reach here.");
      break;
  }
}

void C2_MacroAssembler::vinsert(BasicType typ, XMMRegister dst, XMMRegister src, Register val, int idx) {
  switch(typ) {
    case T_BYTE:
      vpinsrb(dst, src, val, idx);
      break;
    case T_SHORT:
      vpinsrw(dst, src, val, idx);
      break;
    case T_INT:
      vpinsrd(dst, src, val, idx);
      break;
    case T_LONG:
      vpinsrq(dst, src, val, idx);
      break;
    default:
      assert(false,"Should not reach here.");
      break;
  }
}

void C2_MacroAssembler::vgather(BasicType typ, XMMRegister dst, Register base, XMMRegister idx, XMMRegister mask, int vector_len) {
  switch(typ) {
    case T_INT:
      vpgatherdd(dst, Address(base, idx, Address::times_4), mask, vector_len);
      break;
    case T_FLOAT:
      vgatherdps(dst, Address(base, idx, Address::times_4), mask, vector_len);
      break;
    case T_LONG:
      vpgatherdq(dst, Address(base, idx, Address::times_8), mask, vector_len);
      break;
    case T_DOUBLE:
      vgatherdpd(dst, Address(base, idx, Address::times_8), mask, vector_len);
      break;
    default:
      assert(false,"Should not reach here.");
      break;
  }
}

void C2_MacroAssembler::evgather(BasicType typ, XMMRegister dst, KRegister mask, Register base, XMMRegister idx, int vector_len) {
  switch(typ) {
    case T_INT:
      evpgatherdd(dst, mask, Address(base, idx, Address::times_4), vector_len);
      break;
    case T_FLOAT:
      evgatherdps(dst, mask, Address(base, idx, Address::times_4), vector_len);
      break;
    case T_LONG:
      evpgatherdq(dst, mask, Address(base, idx, Address::times_8), vector_len);
      break;
    case T_DOUBLE:
      evgatherdpd(dst, mask, Address(base, idx, Address::times_8), vector_len);
      break;
    default:
      assert(false,"Should not reach here.");
      break;
  }
}

void C2_MacroAssembler::evscatter(BasicType typ, Register base, XMMRegister idx, KRegister mask, XMMRegister src, int vector_len) {
  switch(typ) {
    case T_INT:
      evpscatterdd(Address(base, idx, Address::times_4), mask, src, vector_len);
      break;
    case T_FLOAT:
      evscatterdps(Address(base, idx, Address::times_4), mask, src, vector_len);
      break;
    case T_LONG:
      evpscatterdq(Address(base, idx, Address::times_8), mask, src, vector_len);
      break;
    case T_DOUBLE:
      evscatterdpd(Address(base, idx, Address::times_8), mask, src, vector_len);
      break;
    default:
      assert(false,"Should not reach here.");
      break;
  }
}

void C2_MacroAssembler::load_vector_mask(XMMRegister dst, XMMRegister src, int vlen_in_bytes, BasicType elem_bt) {
  if (vlen_in_bytes <= 16) {
    pxor (dst, dst);
    psubb(dst, src);
    switch (elem_bt) {
      case T_BYTE:   /* nothing to do */ break;
      case T_SHORT:  pmovsxbw(dst, dst); break;
      case T_INT:    pmovsxbd(dst, dst); break;
      case T_FLOAT:  pmovsxbd(dst, dst); break;
      case T_LONG:   pmovsxbq(dst, dst); break;
      case T_DOUBLE: pmovsxbq(dst, dst); break;

      default: assert(false, "%s", type2name(elem_bt));
    }
  } else {
    int vlen_enc = vector_length_encoding(vlen_in_bytes);

    vpxor (dst, dst, dst, vlen_enc);
    vpsubb(dst, dst, src, vlen_enc);
    switch (elem_bt) {
      case T_BYTE:   /* nothing to do */            break;
      case T_SHORT:  vpmovsxbw(dst, dst, vlen_enc); break;
      case T_INT:    vpmovsxbd(dst, dst, vlen_enc); break;
      case T_FLOAT:  vpmovsxbd(dst, dst, vlen_enc); break;
      case T_LONG:   vpmovsxbq(dst, dst, vlen_enc); break;
      case T_DOUBLE: vpmovsxbq(dst, dst, vlen_enc); break;

      default: assert(false, "%s", type2name(elem_bt));
    }
  }
}

void C2_MacroAssembler::load_iota_indices(XMMRegister dst, Register scratch, int vlen_in_bytes) {
  ExternalAddress addr(StubRoutines::x86::vector_iota_indices());
  if (vlen_in_bytes == 4) {
    movdl(dst, addr);
  } else if (vlen_in_bytes == 8) {
    movq(dst, addr);
  } else if (vlen_in_bytes == 16) {
    movdqu(dst, addr, scratch);
  } else if (vlen_in_bytes == 32) {
    vmovdqu(dst, addr, scratch);
  } else {
    assert(vlen_in_bytes == 64, "%d", vlen_in_bytes);
    evmovdqub(dst, k0, addr, false /*merge*/, Assembler::AVX_512bit, scratch);
  }
}

// Reductions for vectors of bytes, shorts, ints, longs, floats, and doubles.

void C2_MacroAssembler::reduce_operation_128(BasicType typ, int opcode, XMMRegister dst, XMMRegister src) {
  int vector_len = Assembler::AVX_128bit;

  switch (opcode) {
    case Op_AndReductionV:  pand(dst, src); break;
    case Op_OrReductionV:   por (dst, src); break;
    case Op_XorReductionV:  pxor(dst, src); break;
    case Op_MinReductionV:
      switch (typ) {
        case T_BYTE:        pminsb(dst, src); break;
        case T_SHORT:       pminsw(dst, src); break;
        case T_INT:         pminsd(dst, src); break;
        case T_LONG:        assert(UseAVX > 2, "required");
                            vpminsq(dst, dst, src, Assembler::AVX_128bit); break;
        default:            assert(false, "wrong type");
      }
      break;
    case Op_MaxReductionV:
      switch (typ) {
        case T_BYTE:        pmaxsb(dst, src); break;
        case T_SHORT:       pmaxsw(dst, src); break;
        case T_INT:         pmaxsd(dst, src); break;
        case T_LONG:        assert(UseAVX > 2, "required");
                            vpmaxsq(dst, dst, src, Assembler::AVX_128bit); break;
        default:            assert(false, "wrong type");
      }
      break;
    case Op_AddReductionVF: addss(dst, src); break;
    case Op_AddReductionVD: addsd(dst, src); break;
    case Op_AddReductionVI:
      switch (typ) {
        case T_BYTE:        paddb(dst, src); break;
        case T_SHORT:       paddw(dst, src); break;
        case T_INT:         paddd(dst, src); break;
        default:            assert(false, "wrong type");
      }
      break;
    case Op_AddReductionVL: paddq(dst, src); break;
    case Op_MulReductionVF: mulss(dst, src); break;
    case Op_MulReductionVD: mulsd(dst, src); break;
    case Op_MulReductionVI:
      switch (typ) {
        case T_SHORT:       pmullw(dst, src); break;
        case T_INT:         pmulld(dst, src); break;
        default:            assert(false, "wrong type");
      }
      break;
    case Op_MulReductionVL: assert(UseAVX > 2, "required");
                            vpmullq(dst, dst, src, vector_len); break;
    default:                assert(false, "wrong opcode");
  }
}

void C2_MacroAssembler::reduce_operation_256(BasicType typ, int opcode, XMMRegister dst,  XMMRegister src1, XMMRegister src2) {
  int vector_len = Assembler::AVX_256bit;

  switch (opcode) {
    case Op_AndReductionV:  vpand(dst, src1, src2, vector_len); break;
    case Op_OrReductionV:   vpor (dst, src1, src2, vector_len); break;
    case Op_XorReductionV:  vpxor(dst, src1, src2, vector_len); break;
    case Op_MinReductionV:
      switch (typ) {
        case T_BYTE:        vpminsb(dst, src1, src2, vector_len); break;
        case T_SHORT:       vpminsw(dst, src1, src2, vector_len); break;
        case T_INT:         vpminsd(dst, src1, src2, vector_len); break;
        case T_LONG:        assert(UseAVX > 2, "required");
                            vpminsq(dst, src1, src2, vector_len); break;
        default:            assert(false, "wrong type");
      }
      break;
    case Op_MaxReductionV:
      switch (typ) {
        case T_BYTE:        vpmaxsb(dst, src1, src2, vector_len); break;
        case T_SHORT:       vpmaxsw(dst, src1, src2, vector_len); break;
        case T_INT:         vpmaxsd(dst, src1, src2, vector_len); break;
        case T_LONG:        assert(UseAVX > 2, "required");
                            vpmaxsq(dst, src1, src2, vector_len); break;
        default:            assert(false, "wrong type");
      }
      break;
    case Op_AddReductionVI:
      switch (typ) {
        case T_BYTE:        vpaddb(dst, src1, src2, vector_len); break;
        case T_SHORT:       vpaddw(dst, src1, src2, vector_len); break;
        case T_INT:         vpaddd(dst, src1, src2, vector_len); break;
        default:            assert(false, "wrong type");
      }
      break;
    case Op_AddReductionVL: vpaddq(dst, src1, src2, vector_len); break;
    case Op_MulReductionVI:
      switch (typ) {
        case T_SHORT:       vpmullw(dst, src1, src2, vector_len); break;
        case T_INT:         vpmulld(dst, src1, src2, vector_len); break;
        default:            assert(false, "wrong type");
      }
      break;
    case Op_MulReductionVL: vpmullq(dst, src1, src2, vector_len); break;
    default:                assert(false, "wrong opcode");
  }
}

void C2_MacroAssembler::reduce_fp(int opcode, int vlen,
                                  XMMRegister dst, XMMRegister src,
                                  XMMRegister vtmp1, XMMRegister vtmp2) {
  switch (opcode) {
    case Op_AddReductionVF:
    case Op_MulReductionVF:
      reduceF(opcode, vlen, dst, src, vtmp1, vtmp2);
      break;

    case Op_AddReductionVD:
    case Op_MulReductionVD:
      reduceD(opcode, vlen, dst, src, vtmp1, vtmp2);
      break;

    default: assert(false, "wrong opcode");
  }
}

void C2_MacroAssembler::reduceB(int opcode, int vlen,
                             Register dst, Register src1, XMMRegister src2,
                             XMMRegister vtmp1, XMMRegister vtmp2) {
  switch (vlen) {
    case  8: reduce8B (opcode, dst, src1, src2, vtmp1, vtmp2); break;
    case 16: reduce16B(opcode, dst, src1, src2, vtmp1, vtmp2); break;
    case 32: reduce32B(opcode, dst, src1, src2, vtmp1, vtmp2); break;
    case 64: reduce64B(opcode, dst, src1, src2, vtmp1, vtmp2); break;

    default: assert(false, "wrong vector length");
  }
}

void C2_MacroAssembler::mulreduceB(int opcode, int vlen,
                             Register dst, Register src1, XMMRegister src2,
                             XMMRegister vtmp1, XMMRegister vtmp2) {
  switch (vlen) {
    case  8: mulreduce8B (opcode, dst, src1, src2, vtmp1, vtmp2); break;
    case 16: mulreduce16B(opcode, dst, src1, src2, vtmp1, vtmp2); break;
    case 32: mulreduce32B(opcode, dst, src1, src2, vtmp1, vtmp2); break;
    case 64: mulreduce64B(opcode, dst, src1, src2, vtmp1, vtmp2); break;

    default: assert(false, "wrong vector length");
  }
}

void C2_MacroAssembler::reduceS(int opcode, int vlen,
                             Register dst, Register src1, XMMRegister src2,
                             XMMRegister vtmp1, XMMRegister vtmp2) {
  switch (vlen) {
    case  4: reduce4S (opcode, dst, src1, src2, vtmp1, vtmp2); break;
    case  8: reduce8S (opcode, dst, src1, src2, vtmp1, vtmp2); break;
    case 16: reduce16S(opcode, dst, src1, src2, vtmp1, vtmp2); break;
    case 32: reduce32S(opcode, dst, src1, src2, vtmp1, vtmp2); break;

    default: assert(false, "wrong vector length");
  }
}

void C2_MacroAssembler::reduceI(int opcode, int vlen,
                             Register dst, Register src1, XMMRegister src2,
                             XMMRegister vtmp1, XMMRegister vtmp2) {
  switch (vlen) {
    case  2: reduce2I (opcode, dst, src1, src2, vtmp1, vtmp2); break;
    case  4: reduce4I (opcode, dst, src1, src2, vtmp1, vtmp2); break;
    case  8: reduce8I (opcode, dst, src1, src2, vtmp1, vtmp2); break;
    case 16: reduce16I(opcode, dst, src1, src2, vtmp1, vtmp2); break;

    default: assert(false, "wrong vector length");
  }
}

#ifdef _LP64
void C2_MacroAssembler::reduceL(int opcode, int vlen,
                             Register dst, Register src1, XMMRegister src2,
                             XMMRegister vtmp1, XMMRegister vtmp2) {
  switch (vlen) {
    case 2: reduce2L(opcode, dst, src1, src2, vtmp1, vtmp2); break;
    case 4: reduce4L(opcode, dst, src1, src2, vtmp1, vtmp2); break;
    case 8: reduce8L(opcode, dst, src1, src2, vtmp1, vtmp2); break;

    default: assert(false, "wrong vector length");
  }
}
#endif // _LP64

void C2_MacroAssembler::reduceF(int opcode, int vlen, XMMRegister dst, XMMRegister src, XMMRegister vtmp1, XMMRegister vtmp2) {
  switch (vlen) {
    case 2:
      assert(vtmp2 == xnoreg, "");
      reduce2F(opcode, dst, src, vtmp1);
      break;
    case 4:
      assert(vtmp2 == xnoreg, "");
      reduce4F(opcode, dst, src, vtmp1);
      break;
    case 8:
      reduce8F(opcode, dst, src, vtmp1, vtmp2);
      break;
    case 16:
      reduce16F(opcode, dst, src, vtmp1, vtmp2);
      break;
    default: assert(false, "wrong vector length");
  }
}

void C2_MacroAssembler::reduceD(int opcode, int vlen, XMMRegister dst, XMMRegister src, XMMRegister vtmp1, XMMRegister vtmp2) {
  switch (vlen) {
    case 2:
      assert(vtmp2 == xnoreg, "");
      reduce2D(opcode, dst, src, vtmp1);
      break;
    case 4:
      reduce4D(opcode, dst, src, vtmp1, vtmp2);
      break;
    case 8:
      reduce8D(opcode, dst, src, vtmp1, vtmp2);
      break;
    default: assert(false, "wrong vector length");
  }
}

void C2_MacroAssembler::reduce2I(int opcode, Register dst, Register src1, XMMRegister src2, XMMRegister vtmp1, XMMRegister vtmp2) {
  if (opcode == Op_AddReductionVI) {
    if (vtmp1 != src2) {
      movdqu(vtmp1, src2);
    }
    phaddd(vtmp1, vtmp1);
  } else {
    pshufd(vtmp1, src2, 0x1);
    reduce_operation_128(T_INT, opcode, vtmp1, src2);
  }
  movdl(vtmp2, src1);
  reduce_operation_128(T_INT, opcode, vtmp1, vtmp2);
  movdl(dst, vtmp1);
}

void C2_MacroAssembler::reduce4I(int opcode, Register dst, Register src1, XMMRegister src2, XMMRegister vtmp1, XMMRegister vtmp2) {
  if (opcode == Op_AddReductionVI) {
    if (vtmp1 != src2) {
      movdqu(vtmp1, src2);
    }
    phaddd(vtmp1, src2);
    reduce2I(opcode, dst, src1, vtmp1, vtmp1, vtmp2);
  } else {
    pshufd(vtmp2, src2, 0xE);
    reduce_operation_128(T_INT, opcode, vtmp2, src2);
    reduce2I(opcode, dst, src1, vtmp2, vtmp1, vtmp2);
  }
}

void C2_MacroAssembler::reduce8I(int opcode, Register dst, Register src1, XMMRegister src2, XMMRegister vtmp1, XMMRegister vtmp2) {
  if (opcode == Op_AddReductionVI) {
    vphaddd(vtmp1, src2, src2, Assembler::AVX_256bit);
    vextracti128_high(vtmp2, vtmp1);
    vpaddd(vtmp1, vtmp1, vtmp2, Assembler::AVX_128bit);
    reduce2I(opcode, dst, src1, vtmp1, vtmp1, vtmp2);
  } else {
    vextracti128_high(vtmp1, src2);
    reduce_operation_128(T_INT, opcode, vtmp1, src2);
    reduce4I(opcode, dst, src1, vtmp1, vtmp1, vtmp2);
  }
}

void C2_MacroAssembler::reduce16I(int opcode, Register dst, Register src1, XMMRegister src2, XMMRegister vtmp1, XMMRegister vtmp2) {
  vextracti64x4_high(vtmp2, src2);
  reduce_operation_256(T_INT, opcode, vtmp2, vtmp2, src2);
  reduce8I(opcode, dst, src1, vtmp2, vtmp1, vtmp2);
}

void C2_MacroAssembler::reduce8B(int opcode, Register dst, Register src1, XMMRegister src2, XMMRegister vtmp1, XMMRegister vtmp2) {
  pshufd(vtmp2, src2, 0x1);
  reduce_operation_128(T_BYTE, opcode, vtmp2, src2);
  movdqu(vtmp1, vtmp2);
  psrldq(vtmp1, 2);
  reduce_operation_128(T_BYTE, opcode, vtmp1, vtmp2);
  movdqu(vtmp2, vtmp1);
  psrldq(vtmp2, 1);
  reduce_operation_128(T_BYTE, opcode, vtmp1, vtmp2);
  movdl(vtmp2, src1);
  pmovsxbd(vtmp1, vtmp1);
  reduce_operation_128(T_INT, opcode, vtmp1, vtmp2);
  pextrb(dst, vtmp1, 0x0);
  movsbl(dst, dst);
}

void C2_MacroAssembler::reduce16B(int opcode, Register dst, Register src1, XMMRegister src2, XMMRegister vtmp1, XMMRegister vtmp2) {
  pshufd(vtmp1, src2, 0xE);
  reduce_operation_128(T_BYTE, opcode, vtmp1, src2);
  reduce8B(opcode, dst, src1, vtmp1, vtmp1, vtmp2);
}

void C2_MacroAssembler::reduce32B(int opcode, Register dst, Register src1, XMMRegister src2, XMMRegister vtmp1, XMMRegister vtmp2) {
  vextracti128_high(vtmp2, src2);
  reduce_operation_128(T_BYTE, opcode, vtmp2, src2);
  reduce16B(opcode, dst, src1, vtmp2, vtmp1, vtmp2);
}

void C2_MacroAssembler::reduce64B(int opcode, Register dst, Register src1, XMMRegister src2, XMMRegister vtmp1, XMMRegister vtmp2) {
  vextracti64x4_high(vtmp1, src2);
  reduce_operation_256(T_BYTE, opcode, vtmp1, vtmp1, src2);
  reduce32B(opcode, dst, src1, vtmp1, vtmp1, vtmp2);
}

void C2_MacroAssembler::mulreduce8B(int opcode, Register dst, Register src1, XMMRegister src2, XMMRegister vtmp1, XMMRegister vtmp2) {
  pmovsxbw(vtmp2, src2);
  reduce8S(opcode, dst, src1, vtmp2, vtmp1, vtmp2);
}

void C2_MacroAssembler::mulreduce16B(int opcode, Register dst, Register src1, XMMRegister src2, XMMRegister vtmp1, XMMRegister vtmp2) {
  if (UseAVX > 1) {
    int vector_len = Assembler::AVX_256bit;
    vpmovsxbw(vtmp1, src2, vector_len);
    reduce16S(opcode, dst, src1, vtmp1, vtmp1, vtmp2);
  } else {
    pmovsxbw(vtmp2, src2);
    reduce8S(opcode, dst, src1, vtmp2, vtmp1, vtmp2);
    pshufd(vtmp2, src2, 0x1);
    pmovsxbw(vtmp2, src2);
    reduce8S(opcode, dst, dst, vtmp2, vtmp1, vtmp2);
  }
}

void C2_MacroAssembler::mulreduce32B(int opcode, Register dst, Register src1, XMMRegister src2, XMMRegister vtmp1, XMMRegister vtmp2) {
  if (UseAVX > 2 && VM_Version::supports_avx512bw()) {
    int vector_len = Assembler::AVX_512bit;
    vpmovsxbw(vtmp1, src2, vector_len);
    reduce32S(opcode, dst, src1, vtmp1, vtmp1, vtmp2);
  } else {
    assert(UseAVX >= 2,"Should not reach here.");
    mulreduce16B(opcode, dst, src1, src2, vtmp1, vtmp2);
    vextracti128_high(vtmp2, src2);
    mulreduce16B(opcode, dst, dst, vtmp2, vtmp1, vtmp2);
  }
}

void C2_MacroAssembler::mulreduce64B(int opcode, Register dst, Register src1, XMMRegister src2, XMMRegister vtmp1, XMMRegister vtmp2) {
  mulreduce32B(opcode, dst, src1, src2, vtmp1, vtmp2);
  vextracti64x4_high(vtmp2, src2);
  mulreduce32B(opcode, dst, dst, vtmp2, vtmp1, vtmp2);
}

void C2_MacroAssembler::reduce4S(int opcode, Register dst, Register src1, XMMRegister src2, XMMRegister vtmp1, XMMRegister vtmp2) {
  if (opcode == Op_AddReductionVI) {
    if (vtmp1 != src2) {
      movdqu(vtmp1, src2);
    }
    phaddw(vtmp1, vtmp1);
    phaddw(vtmp1, vtmp1);
  } else {
    pshufd(vtmp2, src2, 0x1);
    reduce_operation_128(T_SHORT, opcode, vtmp2, src2);
    movdqu(vtmp1, vtmp2);
    psrldq(vtmp1, 2);
    reduce_operation_128(T_SHORT, opcode, vtmp1, vtmp2);
  }
  movdl(vtmp2, src1);
  pmovsxwd(vtmp1, vtmp1);
  reduce_operation_128(T_INT, opcode, vtmp1, vtmp2);
  pextrw(dst, vtmp1, 0x0);
  movswl(dst, dst);
}

void C2_MacroAssembler::reduce8S(int opcode, Register dst, Register src1, XMMRegister src2, XMMRegister vtmp1, XMMRegister vtmp2) {
  if (opcode == Op_AddReductionVI) {
    if (vtmp1 != src2) {
      movdqu(vtmp1, src2);
    }
    phaddw(vtmp1, src2);
  } else {
    pshufd(vtmp1, src2, 0xE);
    reduce_operation_128(T_SHORT, opcode, vtmp1, src2);
  }
  reduce4S(opcode, dst, src1, vtmp1, vtmp1, vtmp2);
}

void C2_MacroAssembler::reduce16S(int opcode, Register dst, Register src1, XMMRegister src2, XMMRegister vtmp1, XMMRegister vtmp2) {
  if (opcode == Op_AddReductionVI) {
    int vector_len = Assembler::AVX_256bit;
    vphaddw(vtmp2, src2, src2, vector_len);
    vpermq(vtmp2, vtmp2, 0xD8, vector_len);
  } else {
    vextracti128_high(vtmp2, src2);
    reduce_operation_128(T_SHORT, opcode, vtmp2, src2);
  }
  reduce8S(opcode, dst, src1, vtmp2, vtmp1, vtmp2);
}

void C2_MacroAssembler::reduce32S(int opcode, Register dst, Register src1, XMMRegister src2, XMMRegister vtmp1, XMMRegister vtmp2) {
  int vector_len = Assembler::AVX_256bit;
  vextracti64x4_high(vtmp1, src2);
  reduce_operation_256(T_SHORT, opcode, vtmp1, vtmp1, src2);
  reduce16S(opcode, dst, src1, vtmp1, vtmp1, vtmp2);
}

#ifdef _LP64
void C2_MacroAssembler::reduce2L(int opcode, Register dst, Register src1, XMMRegister src2, XMMRegister vtmp1, XMMRegister vtmp2) {
  pshufd(vtmp2, src2, 0xE);
  reduce_operation_128(T_LONG, opcode, vtmp2, src2);
  movdq(vtmp1, src1);
  reduce_operation_128(T_LONG, opcode, vtmp1, vtmp2);
  movdq(dst, vtmp1);
}

void C2_MacroAssembler::reduce4L(int opcode, Register dst, Register src1, XMMRegister src2, XMMRegister vtmp1, XMMRegister vtmp2) {
  vextracti128_high(vtmp1, src2);
  reduce_operation_128(T_LONG, opcode, vtmp1, src2);
  reduce2L(opcode, dst, src1, vtmp1, vtmp1, vtmp2);
}

void C2_MacroAssembler::reduce8L(int opcode, Register dst, Register src1, XMMRegister src2, XMMRegister vtmp1, XMMRegister vtmp2) {
  vextracti64x4_high(vtmp2, src2);
  reduce_operation_256(T_LONG, opcode, vtmp2, vtmp2, src2);
  reduce4L(opcode, dst, src1, vtmp2, vtmp1, vtmp2);
}

void C2_MacroAssembler::genmask(KRegister dst, Register len, Register temp) {
  // assert(ArrayCopyPartialInlineSize <= 64,"");  Not full 8252848 merged
  mov64(temp, -1L);
  bzhiq(temp, temp, len);
  kmovql(dst, temp);
}
#endif // _LP64

void C2_MacroAssembler::reduce2F(int opcode, XMMRegister dst, XMMRegister src, XMMRegister vtmp) {
  reduce_operation_128(T_FLOAT, opcode, dst, src);
  pshufd(vtmp, src, 0x1);
  reduce_operation_128(T_FLOAT, opcode, dst, vtmp);
}

void C2_MacroAssembler::reduce4F(int opcode, XMMRegister dst, XMMRegister src, XMMRegister vtmp) {
  reduce2F(opcode, dst, src, vtmp);
  pshufd(vtmp, src, 0x2);
  reduce_operation_128(T_FLOAT, opcode, dst, vtmp);
  pshufd(vtmp, src, 0x3);
  reduce_operation_128(T_FLOAT, opcode, dst, vtmp);
}

void C2_MacroAssembler::reduce8F(int opcode, XMMRegister dst, XMMRegister src, XMMRegister vtmp1, XMMRegister vtmp2) {
  reduce4F(opcode, dst, src, vtmp2);
  vextractf128_high(vtmp2, src);
  reduce4F(opcode, dst, vtmp2, vtmp1);
}

void C2_MacroAssembler::reduce16F(int opcode, XMMRegister dst, XMMRegister src, XMMRegister vtmp1, XMMRegister vtmp2) {
  reduce8F(opcode, dst, src, vtmp1, vtmp2);
  vextracti64x4_high(vtmp1, src);
  reduce8F(opcode, dst, vtmp1, vtmp1, vtmp2);
}

void C2_MacroAssembler::reduce2D(int opcode, XMMRegister dst, XMMRegister src, XMMRegister vtmp) {
  reduce_operation_128(T_DOUBLE, opcode, dst, src);
  pshufd(vtmp, src, 0xE);
  reduce_operation_128(T_DOUBLE, opcode, dst, vtmp);
}

void C2_MacroAssembler::reduce4D(int opcode, XMMRegister dst, XMMRegister src, XMMRegister vtmp1, XMMRegister vtmp2) {
  reduce2D(opcode, dst, src, vtmp2);
  vextractf128_high(vtmp2, src);
  reduce2D(opcode, dst, vtmp2, vtmp1);
}

void C2_MacroAssembler::reduce8D(int opcode, XMMRegister dst, XMMRegister src, XMMRegister vtmp1, XMMRegister vtmp2) {
  reduce4D(opcode, dst, src, vtmp1, vtmp2);
  vextracti64x4_high(vtmp1, src);
  reduce4D(opcode, dst, vtmp1, vtmp1, vtmp2);
}

void C2_MacroAssembler::evmovdqu(BasicType type, KRegister kmask, XMMRegister dst, Address src, int vector_len) {
  MacroAssembler::evmovdqu(type, kmask, dst, src, vector_len);
}

void C2_MacroAssembler::evmovdqu(BasicType type, KRegister kmask, Address dst, XMMRegister src, int vector_len) {
  MacroAssembler::evmovdqu(type, kmask, dst, src, vector_len);
}


void C2_MacroAssembler::reduceFloatMinMax(int opcode, int vlen, bool is_dst_valid,
                                          XMMRegister dst, XMMRegister src,
                                          XMMRegister tmp, XMMRegister atmp, XMMRegister btmp,
                                          XMMRegister xmm_0, XMMRegister xmm_1) {
  int permconst[] = {1, 14};
  XMMRegister wsrc = src;
  XMMRegister wdst = xmm_0;
  XMMRegister wtmp = (xmm_1 == xnoreg) ? xmm_0: xmm_1;

  int vlen_enc = Assembler::AVX_128bit;
  if (vlen == 16) {
    vlen_enc = Assembler::AVX_256bit;
  }

  for (int i = log2(vlen) - 1; i >=0; i--) {
    if (i == 0 && !is_dst_valid) {
      wdst = dst;
    }
    if (i == 3) {
      vextracti64x4_high(wtmp, wsrc);
    } else if (i == 2) {
      vextracti128_high(wtmp, wsrc);
    } else { // i = [0,1]
      vpermilps(wtmp, wsrc, permconst[i], vlen_enc);
    }
    vminmax_fp(opcode, T_FLOAT, wdst, wtmp, wsrc, tmp, atmp, btmp, vlen_enc);
    wsrc = wdst;
    vlen_enc = Assembler::AVX_128bit;
  }
  if (is_dst_valid) {
    vminmax_fp(opcode, T_FLOAT, dst, wdst, dst, tmp, atmp, btmp, Assembler::AVX_128bit);
  }
}

void C2_MacroAssembler::reduceDoubleMinMax(int opcode, int vlen, bool is_dst_valid, XMMRegister dst, XMMRegister src,
                                        XMMRegister tmp, XMMRegister atmp, XMMRegister btmp,
                                        XMMRegister xmm_0, XMMRegister xmm_1) {
  XMMRegister wsrc = src;
  XMMRegister wdst = xmm_0;
  XMMRegister wtmp = (xmm_1 == xnoreg) ? xmm_0: xmm_1;
  int vlen_enc = Assembler::AVX_128bit;
  if (vlen == 8) {
    vlen_enc = Assembler::AVX_256bit;
  }
  for (int i = log2(vlen) - 1; i >=0; i--) {
    if (i == 0 && !is_dst_valid) {
      wdst = dst;
    }
    if (i == 1) {
      vextracti128_high(wtmp, wsrc);
    } else if (i == 2) {
      vextracti64x4_high(wtmp, wsrc);
    } else {
      assert(i == 0, "%d", i);
      vpermilpd(wtmp, wsrc, 1, vlen_enc);
    }
    vminmax_fp(opcode, T_DOUBLE, wdst, wtmp, wsrc, tmp, atmp, btmp, vlen_enc);
    wsrc = wdst;
    vlen_enc = Assembler::AVX_128bit;
  }
  if (is_dst_valid) {
    vminmax_fp(opcode, T_DOUBLE, dst, wdst, dst, tmp, atmp, btmp, Assembler::AVX_128bit);
  }
}

void C2_MacroAssembler::extract(BasicType bt, Register dst, XMMRegister src, int idx) {
  switch (bt) {
    case T_BYTE:  pextrb(dst, src, idx); break;
    case T_SHORT: pextrw(dst, src, idx); break;
    case T_INT:   pextrd(dst, src, idx); break;
    case T_LONG:  pextrq(dst, src, idx); break;

    default:
      assert(false,"Should not reach here.");
      break;
  }
}

XMMRegister C2_MacroAssembler::get_lane(BasicType typ, XMMRegister dst, XMMRegister src, int elemindex) {
  int esize =  type2aelembytes(typ);
  int elem_per_lane = 16/esize;
  int lane = elemindex / elem_per_lane;
  int eindex = elemindex % elem_per_lane;

  if (lane >= 2) {
    assert(UseAVX > 2, "required");
    vextractf32x4(dst, src, lane & 3);
    return dst;
  } else if (lane > 0) {
    assert(UseAVX > 0, "required");
    vextractf128(dst, src, lane);
    return dst;
  } else {
    return src;
  }
}

void C2_MacroAssembler::get_elem(BasicType typ, Register dst, XMMRegister src, int elemindex) {
  int esize =  type2aelembytes(typ);
  int elem_per_lane = 16/esize;
  int eindex = elemindex % elem_per_lane;
  assert(is_integral_type(typ),"required");

  if (eindex == 0) {
    if (typ == T_LONG) {
      movq(dst, src);
    } else {
      movdl(dst, src);
      if (typ == T_BYTE)
        movsbl(dst, dst);
      else if (typ == T_SHORT)
        movswl(dst, dst);
    }
  } else {
    extract(typ, dst, src, eindex);
  }
}

void C2_MacroAssembler::get_elem(BasicType typ, XMMRegister dst, XMMRegister src, int elemindex, Register tmp, XMMRegister vtmp) {
  int esize =  type2aelembytes(typ);
  int elem_per_lane = 16/esize;
  int eindex = elemindex % elem_per_lane;
  assert((typ == T_FLOAT || typ == T_DOUBLE),"required");

  if (eindex == 0) {
    movq(dst, src);
  } else {
    if (typ == T_FLOAT) {
      if (UseAVX == 0) {
        movdqu(dst, src);
        pshufps(dst, dst, eindex);
      } else {
        vpshufps(dst, src, src, eindex, Assembler::AVX_128bit);
      }
    } else {
      if (UseAVX == 0) {
        movdqu(dst, src);
        psrldq(dst, eindex*esize);
      } else {
        vpsrldq(dst, src, eindex*esize, Assembler::AVX_128bit);
      }
      movq(dst, dst);
    }
  }
  // Zero upper bits
  if (typ == T_FLOAT) {
    if (UseAVX == 0) {
      assert((vtmp != xnoreg) && (tmp != noreg), "required.");
      movdqu(vtmp, ExternalAddress(StubRoutines::x86::vector_32_bit_mask()), tmp);
      pand(dst, vtmp);
    } else {
      assert((tmp != noreg), "required.");
      vpand(dst, dst, ExternalAddress(StubRoutines::x86::vector_32_bit_mask()), Assembler::AVX_128bit, tmp);
    }
  }
}

void C2_MacroAssembler::evpcmp(BasicType typ, KRegister kdmask, KRegister ksmask, XMMRegister src1, AddressLiteral adr, int comparison, int vector_len, Register scratch) {
  switch(typ) {
    case T_BYTE:
      evpcmpb(kdmask, ksmask, src1, adr, comparison, /*signed*/ true, vector_len, scratch);
      break;
    case T_SHORT:
      evpcmpw(kdmask, ksmask, src1, adr, comparison, /*signed*/ true, vector_len, scratch);
      break;
    case T_INT:
    case T_FLOAT:
      evpcmpd(kdmask, ksmask, src1, adr, comparison, /*signed*/ true, vector_len, scratch);
      break;
    case T_LONG:
    case T_DOUBLE:
      evpcmpq(kdmask, ksmask, src1, adr, comparison, /*signed*/ true, vector_len, scratch);
      break;
    default:
      assert(false,"Should not reach here.");
      break;
  }
}

void C2_MacroAssembler::vpcmpu(BasicType typ, XMMRegister dst, XMMRegister src1, XMMRegister src2, ComparisonPredicate comparison,
                            int vlen_in_bytes, XMMRegister vtmp1, XMMRegister vtmp2, Register scratch) {
  int vlen_enc = vector_length_encoding(vlen_in_bytes*2);
  switch (typ) {
  case T_BYTE:
    vpmovzxbw(vtmp1, src1, vlen_enc);
    vpmovzxbw(vtmp2, src2, vlen_enc);
    vpcmpCCW(dst, vtmp1, vtmp2, comparison, Assembler::W, vlen_enc, scratch);
    vpacksswb(dst, dst, dst, vlen_enc);
    break;
  case T_SHORT:
    vpmovzxwd(vtmp1, src1, vlen_enc);
    vpmovzxwd(vtmp2, src2, vlen_enc);
    vpcmpCCW(dst, vtmp1, vtmp2, comparison, Assembler::D, vlen_enc, scratch);
    vpackssdw(dst, dst, dst, vlen_enc);
    break;
  case T_INT:
    vpmovzxdq(vtmp1, src1, vlen_enc);
    vpmovzxdq(vtmp2, src2, vlen_enc);
    vpcmpCCW(dst, vtmp1, vtmp2, comparison, Assembler::Q, vlen_enc, scratch);
    vpermilps(dst, dst, 8, vlen_enc);
    break;
  default:
    assert(false, "Should not reach here");
  }
  if (vlen_in_bytes == 16) {
    vpermpd(dst, dst, 0x8, vlen_enc);
  }
}

void C2_MacroAssembler::vpcmpu32(BasicType typ, XMMRegister dst, XMMRegister src1, XMMRegister src2, ComparisonPredicate comparison, int vlen_in_bytes,
                              XMMRegister vtmp1, XMMRegister vtmp2, XMMRegister vtmp3, Register scratch) {
  int vlen_enc = vector_length_encoding(vlen_in_bytes);
  switch (typ) {
  case T_BYTE:
    vpmovzxbw(vtmp1, src1, vlen_enc);
    vpmovzxbw(vtmp2, src2, vlen_enc);
    vpcmpCCW(dst, vtmp1, vtmp2, comparison, Assembler::W, vlen_enc, scratch);
    vextracti128(vtmp1, src1, 1);
    vextracti128(vtmp2, src2, 1);
    vpmovzxbw(vtmp1, vtmp1, vlen_enc);
    vpmovzxbw(vtmp2, vtmp2, vlen_enc);
    vpcmpCCW(vtmp3, vtmp1, vtmp2, comparison, Assembler::W, vlen_enc, scratch);
    vpacksswb(dst, dst, vtmp3, vlen_enc);
    vpermpd(dst, dst, 0xd8, vlen_enc);
    break;
  case T_SHORT:
    vpmovzxwd(vtmp1, src1, vlen_enc);
    vpmovzxwd(vtmp2, src2, vlen_enc);
    vpcmpCCW(dst, vtmp1, vtmp2, comparison, Assembler::D, vlen_enc, scratch);
    vextracti128(vtmp1, src1, 1);
    vextracti128(vtmp2, src2, 1);
    vpmovzxwd(vtmp1, vtmp1, vlen_enc);
    vpmovzxwd(vtmp2, vtmp2, vlen_enc);
    vpcmpCCW(vtmp3, vtmp1, vtmp2, comparison, Assembler::D,  vlen_enc, scratch);
    vpackssdw(dst, dst, vtmp3, vlen_enc);
    vpermpd(dst, dst, 0xd8, vlen_enc);
    break;
  case T_INT:
    vpmovzxdq(vtmp1, src1, vlen_enc);
    vpmovzxdq(vtmp2, src2, vlen_enc);
    vpcmpCCW(dst, vtmp1, vtmp2, comparison, Assembler::Q, vlen_enc, scratch);
    vpshufd(dst, dst, 8, vlen_enc);
    vpermq(dst, dst, 8, vlen_enc);
    vextracti128(vtmp1, src1, 1);
    vextracti128(vtmp2, src2, 1);
    vpmovzxdq(vtmp1, vtmp1, vlen_enc);
    vpmovzxdq(vtmp2, vtmp2, vlen_enc);
    vpcmpCCW(vtmp3, vtmp1, vtmp2, comparison, Assembler::Q,  vlen_enc, scratch);
    vpshufd(vtmp3, vtmp3, 8, vlen_enc);
    vpermq(vtmp3, vtmp3, 0x80, vlen_enc);
    vpblendd(dst, dst, vtmp3, 0xf0, vlen_enc);
    break;
  default:
    assert(false, "Should not reach here");
  }
}

void C2_MacroAssembler::evpblend(BasicType typ, XMMRegister dst, KRegister kmask, XMMRegister src1, XMMRegister src2, bool merge, int vector_len) {
  switch(typ) {
    case T_BYTE:
      evpblendmb(dst, kmask, src1, src2, merge, vector_len);
      break;
    case T_SHORT:
      evpblendmw(dst, kmask, src1, src2, merge, vector_len);
      break;
    case T_INT:
    case T_FLOAT:
      evpblendmd(dst, kmask, src1, src2, merge, vector_len);
      break;
    case T_LONG:
    case T_DOUBLE:
      evpblendmq(dst, kmask, src1, src2, merge, vector_len);
      break;
    default:
      assert(false,"Should not reach here.");
      break;
  }
}

void C2_MacroAssembler::vectortest(int bt, int vlen, XMMRegister src1, XMMRegister src2,
                                   XMMRegister vtmp1, XMMRegister vtmp2, KRegister mask) {
  switch(vlen) {
    case 4:
      assert(vtmp1 != xnoreg, "required.");
      // Broadcast lower 32 bits to 128 bits before ptest
      pshufd(vtmp1, src1, 0x0);
      if (bt == BoolTest::overflow) {
        assert(vtmp2 != xnoreg, "required.");
        pshufd(vtmp2, src2, 0x0);
      } else {
        assert(vtmp2 == xnoreg, "required.");
        vtmp2 = src2;
      }
      ptest(vtmp1, vtmp2);
     break;
    case 8:
      assert(vtmp1 != xnoreg, "required.");
      // Broadcast lower 64 bits to 128 bits before ptest
      pshufd(vtmp1, src1, 0x4);
      if (bt == BoolTest::overflow) {
        assert(vtmp2 != xnoreg, "required.");
        pshufd(vtmp2, src2, 0x4);
      } else {
        assert(vtmp2 == xnoreg, "required.");
        vtmp2 = src2;
      }
      ptest(vtmp1, vtmp2);
     break;
    case 16:
      assert((vtmp1 == xnoreg) && (vtmp2 == xnoreg), "required.");
      ptest(src1, src2);
      break;
    case 32:
      assert((vtmp1 == xnoreg) && (vtmp2 == xnoreg), "required.");
      vptest(src1, src2, Assembler::AVX_256bit);
      break;
    case 64:
      {
        assert((vtmp1 == xnoreg) && (vtmp2 == xnoreg), "required.");
        evpcmpeqb(mask, src1, src2, Assembler::AVX_512bit);
        if (bt == BoolTest::ne) {
          ktestql(mask, mask);
        } else {
          assert(bt == BoolTest::overflow, "required");
          kortestql(mask, mask);
        }
      }
      break;
    default:
      assert(false,"Should not reach here.");
      break;
  }
}

#ifdef _LP64
void C2_MacroAssembler::vector_mask_operation(int opc, Register dst, XMMRegister mask, XMMRegister xtmp,
                                              Register tmp, KRegister ktmp, int masklen, int vec_enc) {
  assert(VM_Version::supports_avx512vlbw(), "");
  vpxor(xtmp, xtmp, xtmp, vec_enc);
  vpsubb(xtmp, xtmp, mask, vec_enc);
  evpmovb2m(ktmp, xtmp, vec_enc);
  kmovql(tmp, ktmp);
  switch(opc) {
    case Op_VectorMaskTrueCount:
      popcntq(dst, tmp);
      break;
    case Op_VectorMaskLastTrue:
      mov64(dst, -1);
      bsrq(tmp, tmp);
      cmov(Assembler::notZero, dst, tmp);
      break;
    case Op_VectorMaskFirstTrue:
      mov64(dst, masklen);
      bsfq(tmp, tmp);
      cmov(Assembler::notZero, dst, tmp);
      break;
    default: assert(false, "Unhandled mask operation");
  }
}

void C2_MacroAssembler::vector_mask_operation(int opc, Register dst, XMMRegister mask, XMMRegister xtmp,
                                              XMMRegister xtmp1, Register tmp, int masklen, int vec_enc) {
  assert(VM_Version::supports_avx(), "");
  vpxor(xtmp, xtmp, xtmp, vec_enc);
  vpsubb(xtmp, xtmp, mask, vec_enc);
  vpmovmskb(tmp, xtmp, vec_enc);
  if (masklen < 64) {
    andq(tmp, (((jlong)1 << masklen) - 1));
  }
  switch(opc) {
    case Op_VectorMaskTrueCount:
      popcntq(dst, tmp);
      break;
    case Op_VectorMaskLastTrue:
      mov64(dst, -1);
      bsrq(tmp, tmp);
      cmov(Assembler::notZero, dst, tmp);
      break;
    case Op_VectorMaskFirstTrue:
      mov64(dst, masklen);
      bsfq(tmp, tmp);
      cmov(Assembler::notZero, dst, tmp);
      break;
    default: assert(false, "Unhandled mask operation");
  }
}
#endif

#endif  // COMPILER2

