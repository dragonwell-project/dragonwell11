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

#include <iostream>
#include <string.h>

#include "loadClassAgent.hpp"
#include "jvmti.h"

using namespace std;

jvmtiEnv* LoadClassAgent::m_jvmti = 0;

LoadClassAgent::~LoadClassAgent() throw(AgentException)
{
}

void LoadClassAgent::Init(JavaVM *vm) const throw(AgentException) {
  jvmtiEnv *jvmti = 0;
  jint ret = (vm)->GetEnv(reinterpret_cast<void**>(&jvmti), JVMTI_VERSION_9);
  if (ret != JNI_OK || jvmti == 0) {
    throw AgentException(JVMTI_ERROR_INTERNAL);
  }
  m_jvmti = jvmti;
}

void LoadClassAgent::RegisterEvent() const throw(AgentException) {
  jvmtiEventCallbacks callbacks;
  memset(&callbacks, 0, sizeof(callbacks));
  callbacks.FirstClassLoadPrepare = &LoadClassAgent::HandleLoadClass;

  jvmtiError error;
  error = m_jvmti->SetEventCallbacks(&callbacks, static_cast<jint>(sizeof(callbacks)));
  CheckException(error);

  error = m_jvmti->SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_FIRST_CLASS_LOAD_PREPARE, 0);
  CheckException(error);
}

void JNICALL LoadClassAgent::HandleLoadClass(jvmtiEnv* jvmti, JNIEnv* jni, jthread thread, jobject cls) {
  try {

    jvmtiError error = JVMTI_ERROR_NONE;
    char* signature = NULL;

    cout << "LoadClassAgent::HandleLoadClass" << endl;

    jclass clazz = jni->GetObjectClass(cls);

    error = m_jvmti->GetClassSignature(clazz, &signature, 0);
    CheckException(error);

    error = m_jvmti->Deallocate(reinterpret_cast<unsigned char*>(signature));
    CheckException(error);

    jclass cla = jni->FindClass("com/alibaba/util/Utils");
    jmethodID mid = jni->GetStaticMethodID(cla, "registerClassLoader", "(Ljava/lang/ClassLoader;I)V");

    if ( mid == 0) {
      cout << "LoadClassAgent::HandleLoadClass get method" << endl;
    } else {
      cout << "LoadClassAgent::HandleLoadClass get method" << endl;
    }
  } catch (AgentException& e) {
    cout << "Error when enter HandleMethodEntry: " << e.what() << " [" << e.ErrCode() << "]";
  }
}
