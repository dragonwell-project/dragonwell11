#include "jni.h"
#include "jvm.h"
#include "com_alibaba_wisp_engine_WispTask.h"

#define ARRAY_LENGTH(a) (sizeof(a)/sizeof(a[0]))

#define THD "Ljava/lang/Thread;"

static JNINativeMethod methods[] = {
        {"checkAndClearNativeInterruptForWisp",    "(" THD ")Z",       (void *)&JVM_CheckAndClearNativeInterruptForWisp},
};

JNIEXPORT void JNICALL
Java_com_alibaba_wisp_engine_WispTask_registerNatives(JNIEnv *env, jclass cls)
{
  (*env)->RegisterNatives(env, cls, methods, ARRAY_LENGTH(methods));
}
