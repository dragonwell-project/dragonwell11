/*
 * Copyright (c) 2023, Alibaba Group Holding Limited. All rights reserved.
 * Copyright (c) 2017, 2021, Azul Systems, Inc. All rights reserved.
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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

/** \file */

#include "jni.h"
#include "jvm.h"
#include "jni_util.h"
#include "io_util.h"
#include "io_util_md.h"

#include "jdk_crac_Core.h"
#include "jdk_internal_crac_Core.h"

JNIEXPORT jobjectArray JNICALL
Java_jdk_crac_Core_checkpointRestore0(JNIEnv *env, jclass ignore, jboolean dry_run, jlong jcmd_stream)
{
    return JVM_Checkpoint(env, dry_run, jcmd_stream);
}

JNIEXPORT void JNICALL Java_jdk_internal_crac_Core_registerPersistent0
  (JNIEnv *env, jclass ignore, jobject fileDesc)
{
    jint fd = THIS_FD(fileDesc);

    struct stat st;
    if (-1 == fstat(fd, &st)) {
        return;
    }

    JVM_RegisterPersistent(env, fd, st.st_dev, st.st_ino);
}

JNIEXPORT void JNICALL Java_jdk_internal_crac_Core_registerPseudoPersistent0
        (JNIEnv *env, jclass ignore, jclass absolutFilePath, jint mode)
{
  JVM_RegisterPseudoPersistent(env, absolutFilePath, mode);
}

JNIEXPORT void JNICALL Java_jdk_internal_crac_Core_unregisterPseudoPersistent0
        (JNIEnv *env, jclass ignore, jclass absolutFilePath)
{
  JVM_UnregisterPseudoPersistent(env, absolutFilePath);
}
