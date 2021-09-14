/*
 * Copyright (c) 2021, Azul Systems, Inc. All rights reserved.
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

#include <string.h>
#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include <signal.h>

#define RESTORE_SIGNAL   (SIGRTMIN + 2)

static int kickjvm(pid_t jvm, int code) {
    union sigval sv = { .sival_int = code };
    if (-1 == sigqueue(jvm, RESTORE_SIGNAL, sv)) {
        perror("sigqueue");
        return 1;
    }
    return 0;
}

int main(int argc, char *argv[]) {
    char* action = argv[1];

    if (!strcmp(action, "checkpoint")) {
        const char* argsidstr = getenv("SIM_CRAC_NEW_ARGS_ID");
        int argsid = argsidstr ? atoi(argsidstr) : 0;
        pid_t jvm = getppid();
        kickjvm(jvm, argsid);
    } else if (!strcmp(action, "restore")) {
        char *strid = getenv("CRAC_NEW_ARGS_ID");
        printf("SIM_CRAC_NEW_ARGS_ID=%s\n", strid ? strid : "0");
    } else {
        fprintf(stderr, "unknown action: %s\n", action);
        return 1;
    }

    return 0;
}
