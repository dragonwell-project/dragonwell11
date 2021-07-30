#include "jni.h"
#include "jvm.h"
#include "java_dyn_Coroutine.h"

#define ARRAY_LENGTH(a) (sizeof(a)/sizeof(a[0]))

#define LANG "Ljava/lang/"
#define OBJ LANG"Object;"

static JNINativeMethod methods[] = {
  {"setWispTask","(JI"OBJ OBJ")V", (void *)&JVM_SetWispTask},
};

JNIEXPORT void JNICALL
Java_java_dyn_Coroutine_registerNatives(JNIEnv *env, jclass cls)
{
    (*env)->RegisterNatives(env, cls, methods, ARRAY_LENGTH(methods));
}
