#include "jni.h"
#include "jvm.h"
#include "com_alibaba_wisp_engine_WispSysmon.h"

#define ARRAY_LENGTH(a) (sizeof(a)/sizeof(a[0]))

static JNINativeMethod methods[] = {
  {"markPreempted","(Ljava/lang/Thread;)V", (void *)&JVM_MarkPreempted},
};

JNIEXPORT void JNICALL
Java_com_alibaba_wisp_engine_WispSysmon_registerNatives(JNIEnv *env, jclass cls)
{
    (*env)->RegisterNatives(env, cls, methods, ARRAY_LENGTH(methods));
}
