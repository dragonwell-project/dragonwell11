/*
 * Copyright (c) 2023, Alibaba Group Holding Limited. All rights reserved.
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
 */

#include "jvmti.h"

class AgentException {
public:
  AgentException(jvmtiError err) {
    m_error = err;
  }

  char* what() const throw() {
    return "AgentException";
  }

  jvmtiError ErrCode() const throw() {
    return m_error;
  }

private:
  jvmtiError m_error;
};


class LoadClassAgent {
public:
  LoadClassAgent() throw(AgentException){}

  ~LoadClassAgent() throw(AgentException);

  void Init(JavaVM *vm) const throw(AgentException);

  void RegisterEvent() const throw(AgentException);

  static void JNICALL HandleLoadClass(jvmtiEnv* jvmti, JNIEnv* jni, jthread thread, jobject cld);

private:
  static void CheckException(jvmtiError error) throw(AgentException) {
    if (error != JVMTI_ERROR_NONE) {
      throw AgentException(error);
    }
  }

  static jvmtiEnv * m_jvmti;
};
