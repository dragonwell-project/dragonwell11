#include <jni.h>
#include <stdio.h>
#include <stdlib.h>
#include <pthread.h>
#include <unistd.h>


JavaVM *jvm;       /* denotes a Java VM */

jobject lock;
volatile int fooGotLock;

int
main()
{
    JNIEnv *env;
    JavaVMInitArgs vm_args; /* JDK/JRE 6 VM initialization arguments */
    JavaVMOption options[5];
    options[0].optionString = "-XX:+UnlockExperimentalVMOptions";
    options[1].optionString = "-XX:+EnableCoroutine";
    options[2].optionString = "-XX:-UseBiasedLocking";
    options[3].optionString = "-Dcom.alibaba.transparentAsync=true";
    options[4].optionString = "-XX:+UseWispMonitor";
    vm_args.version = JNI_VERSION_1_6;
    vm_args.nOptions = 5;
    vm_args.options = options;
    vm_args.ignoreUnrecognized = false;
    /* load and initialize a Java VM, return a JNI interface
     * pointer in env */
    if (JNI_CreateJavaVM(&jvm, (void**)&env, &vm_args) != JNI_OK) {
        exit(-1);
    }

    jclass cls = env->FindClass("java/nio/channels/spi/SelectorProvider");
    printf("class = %p\n", cls);
    jfieldID fid = env->GetStaticFieldID(cls, "lock", "Ljava/lang/Object;");
    printf("fid = %p\n", fid);
    lock = env->GetStaticObjectField(cls, fid);
    printf("lock = %p\n", lock);

    if (env->MonitorEnter(lock) != JNI_OK) {
        exit(-1);
    }

    if (env->MonitorExit(lock) != JNI_OK) {
        if (env->ExceptionOccurred()) {
          env->ExceptionDescribe(); // print the stack trace          
        }
        exit(-1);
    }

    return jvm->DestroyJavaVM() == JNI_OK ? 0 : -1;
}

