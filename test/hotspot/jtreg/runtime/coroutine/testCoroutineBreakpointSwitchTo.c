#include <jni.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <jvmti.h>

void Jvmti_Error(int errcode, const char *msg) {
  if (errcode != JVMTI_ERROR_NONE) {
    printf("%s, error code: [%d]\n", errcode, msg);
    exit(1);
  }
}

#define JVMTI_CHECK(x, str) (Jvmti_Error(x, str))

void JNICALL
SingleStepCallBack(jvmtiEnv *jvmti_env,
                   JNIEnv* jni_env,
                   jthread thread,
                   jmethodID method,
                   jlocation location)
{
}

JNIEXPORT jint JNICALL
Agent_OnLoad(JavaVM *jvm, char *options, void *reserved) {

  // 1. get the jvmti env
  jvmtiEnv *jvmti = NULL;
  JVMTI_CHECK((*jvm)->GetEnv(jvm, (void **)&jvmti, JVMTI_VERSION_1), "env get error");

  // 2. grant the ability to the jvmti: can single step
  jvmtiCapabilities noryoku;
  memset((void *)&noryoku, 0, sizeof(jvmtiCapabilities));
  noryoku.can_generate_single_step_events = 1;
  JVMTI_CHECK((*jvmti)->AddCapabilities(jvmti, &noryoku), "add capability error");

  // 3. set: every single step, we can get a notification.
  JVMTI_CHECK((*jvmti)->SetEventNotificationMode(jvmti, JVMTI_ENABLE, JVMTI_EVENT_SINGLE_STEP, NULL), "set notification failed");

  // 4. when a notification happens, we will get a callback! This callback will booooom.
  jvmtiEventCallbacks callbacks;
  memset((void *)&callbacks, 0, sizeof(callbacks));
  callbacks.SingleStep = &SingleStepCallBack;
  JVMTI_CHECK((*jvmti)->SetEventCallbacks(jvmti, &callbacks, sizeof(callbacks)), "set callback failed");

  return 0;
}
