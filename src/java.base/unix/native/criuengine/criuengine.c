/*
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

#include <assert.h>
#include <string.h>
#include <stdio.h>
#include <libgen.h>
#include <limits.h>
#include <stdlib.h>
#include <errno.h>
#include <unistd.h>
#include <fcntl.h>
#include <signal.h>
#include <sys/wait.h>
#include <sys/stat.h>

#define RESTORE_SIGNAL   (SIGRTMIN + 2)

#define PERFDATA_NAME "perfdata"

#define ARRAY_SIZE(x) (sizeof(x) / sizeof(x[0]))

static int create_cppath(const char *imagedir);

static int g_pid;

static int kickjvm(pid_t jvm, int code) {
    union sigval sv = { .sival_int = code };
    if (-1 == sigqueue(jvm, RESTORE_SIGNAL, sv)) {
        perror("sigqueue");
        return 1;
    }
    return 0;
}

static int checkpoint(pid_t jvm,
        const char *basedir,
        const char *self,
        const char *criu,
        const char *imagedir) {

    if (fork()) {
        // main process
        wait(NULL);
        return 0;
    }

    pid_t parent_before = getpid();

    // child
    if (fork()) {
        exit(0);
    }

    // grand-child
    pid_t parent = getppid();
    int tries = 300;
    while (parent != 1 && 0 < tries--) {
        usleep(10);
        parent = getppid();
    }

    if (parent == parent_before) {
        fprintf(stderr, "can't move out of JVM process hierarchy");
        kickjvm(jvm, -1);
        exit(0);
    }

    char* leave_running = getenv("CRAC_CRIU_LEAVE_RUNNING");

    char jvmpidchar[32];
    snprintf(jvmpidchar, sizeof(jvmpidchar), "%d", jvm);

    pid_t child = fork();
    if (!child) {
        const char* args[32] = {
            criu,
            "dump",
            "-t", jvmpidchar,
            "-D", imagedir,
            "--shell-job",
            "-v4", "-o", "dump4.log", // -D without -W makes criu cd to image dir for logs
        };
        const char** arg = args + 10;

        if (leave_running) {
            *arg++ = "-R";
        }

        char *criuopts = getenv("CRAC_CRIU_OPTS");
        if (criuopts) {
            char* criuopt = strtok(criuopts, " ");
            while (criuopt && ARRAY_SIZE(args) >= (size_t)(arg - args) + 1/* account for trailing NULL */) {
                *arg++ = criuopt;
                criuopt = strtok(NULL, " ");
            }
            if (criuopt) {
                fprintf(stderr, "Warning: too many arguments in CRAC_CRIU_OPTS (dropped from '%s')\n", criuopt);
            }
        }
        *arg++ = NULL;

        execv(criu, (char**)args);
        perror("criu dump");
        exit(1);
    }

    int status;
    if (child != wait(&status) || !WIFEXITED(status) || WEXITSTATUS(status)) {
        kickjvm(jvm, -1);
    } else if (leave_running) {
        kickjvm(jvm, 0);
    }

    create_cppath(imagedir);
    exit(0);
}

static int restore(const char *basedir,
        const char *self,
        const char *criu,
        const char *imagedir) {
    char *cppathpath;
    if (-1 == asprintf(&cppathpath, "%s/cppath", imagedir)) {
        return 1;
    }

    int fd = open(cppathpath, O_RDONLY);
    if (fd < 0) {
        perror("open cppath");
        return 1;
    }

    char cppath[PATH_MAX];
    int cppathlen = 0;
    int r;
    while ((r = read(fd, cppath + cppathlen, sizeof(cppath) - cppathlen - 1)) != 0) {
        if (r < 0 && errno == EINTR) {
            continue;
        }
        if (r < 0) {
            perror("read cppath");
            return 1;
        }
        cppathlen += r;
    }
    cppath[cppathlen] = '\0';

    close(fd);

    char *inherit_perfdata = NULL;
    char *perfdatapath;
    if (-1 == asprintf(&perfdatapath, "%s/" PERFDATA_NAME, imagedir)) {
        return 1;
    }
    int perfdatafd = open(perfdatapath, O_RDWR);
    if (0 < perfdatafd) {
        if (-1 == asprintf(&inherit_perfdata, "fd[%d]:%s/" PERFDATA_NAME,
                    perfdatafd,
                    cppath[0] == '/' ? cppath + 1 : cppath)) {
            return 1;
        }
    }

    const char* args[32] = {
        criu,
        "restore",
        "-W", ".",
        "--shell-job",
        "--action-script", self,
        "-D", imagedir,
        "-v1"
    };
    const char** arg = args + 10;
    if (inherit_perfdata) {
        *arg++ = "--inherit-fd";
        *arg++ = inherit_perfdata;
    }
    const char* tail[] = {
        "--exec-cmd", "--", self, "restorewait",
        NULL
    };
    char *criuopts = getenv("CRAC_CRIU_OPTS");
    if (criuopts) {
        char* criuopt = strtok(criuopts, " ");
        while (criuopt && ARRAY_SIZE(args) >= (size_t)(arg - args + ARRAY_SIZE(tail))) {
            *arg++ = criuopt;
            criuopt = strtok(NULL, " ");
        }
        if (criuopt) {
            fprintf(stderr, "Warning: too many arguments in CRAC_CRIU_OPTS (dropped from '%s')\n", criuopt);
        }
    }

    memcpy(arg, tail, sizeof(tail));

    execv(criu, (char**)args);
    perror("exec criu");
    return 1;
}

#define MSGPREFIX ""

static int post_resume(void) {
    char *pidstr = getenv("CRTOOLS_INIT_PID");
    if (!pidstr) {
        fprintf(stderr, MSGPREFIX "cannot find CRTOOLS_INIT_PID env\n");
        return 1;
    }
    int pid = atoi(pidstr);

    char *strid = getenv("CRAC_NEW_ARGS_ID");
    return kickjvm(pid, strid ? atoi(strid) : 0);
}

static int create_cppath(const char *imagedir) {
    char realdir[PATH_MAX];

    if (!realpath(imagedir, realdir)) {
        fprintf(stderr, MSGPREFIX "cannot canonicalize %s: %s\n", imagedir, strerror(errno));
        return 1;
    }

    int dirfd = open(realdir, O_DIRECTORY);
    if (dirfd < 0) {
        fprintf(stderr, MSGPREFIX "can not open image dir %s: %s\n", realdir, strerror(errno));
        return 1;
    }

    int fd = openat(dirfd, "cppath", O_CREAT | O_WRONLY | O_TRUNC, 0644);
    if (fd < 0) {
        fprintf(stderr, MSGPREFIX "can not open file %s/cppath: %s\n", realdir, strerror(errno));
        return 1;
    }

    if (write(fd, realdir, strlen(realdir)) < 0) {
        fprintf(stderr, MSGPREFIX "can not write %s/cppath: %s\n", realdir, strerror(errno));
        return 1;
    }
    return 0;
}

static void sighandler(int sig, siginfo_t *info, void *uc) {
    if (0 <= g_pid) {
        kill(g_pid, sig);
    }
}

static int restorewait(void) {
    char *pidstr = getenv("CRTOOLS_INIT_PID");
    if (!pidstr) {
        fprintf(stderr, MSGPREFIX "no CRTOOLS_INIT_PID: signals may not be delivered\n");
    }
    g_pid = pidstr ? atoi(pidstr) : -1;

    struct sigaction sigact;
    sigfillset(&sigact.sa_mask);
    sigact.sa_flags = SA_SIGINFO;
    sigact.sa_sigaction = sighandler;

    int sig;
    for (sig = 1; sig <= 31; ++sig) {
        if (sig == SIGKILL || sig == SIGSTOP) {
            continue;
        }
        if (-1 == sigaction(sig, &sigact, NULL)) {
            perror("sigaction");
        }
    }

    sigset_t allset;
    sigfillset(&allset);
    if (-1 == sigprocmask(SIG_UNBLOCK, &allset, NULL)) {
        perror(MSGPREFIX "sigprocmask");
    }

    int status;
    int ret;
    do {
        ret = waitpid(g_pid, &status, 0);
    } while (ret == -1 && errno == EINTR);

    if (ret == -1) {
        perror(MSGPREFIX "waitpid");
        return 1;
    }

    if (WIFEXITED(status)) {
        return WEXITSTATUS(status);
    }

    if (WIFSIGNALED(status)) {
        // Try to terminate the current process with the same signal
        // as the child process was terminated
        const int sig = WTERMSIG(status);
        signal(sig, SIG_DFL);
        raise(sig);
        // Signal was ignored, return 128+n as bash does
        // see https://linux.die.net/man/1/bash
        return 128+sig;
    }

    return 1;
}

int main(int argc, char *argv[]) {
    char* action;
    if ((action = argv[1])) {
        char* imagedir = argv[2];

        char *basedir = dirname(strdup(argv[0]));

        char *criu = getenv("CRAC_CRIU_PATH");
        if (!criu) {
            if (-1 == asprintf(&criu, "%s/criu", basedir)) {
                return 1;
            }
            struct stat st;
            if (stat(criu, &st)) {
                /* some problem with the bundled criu */
                criu = "/usr/sbin/criu";
                if (stat(criu, &st)) {
                    fprintf(stderr, "cannot find CRIU to use\n");
                    return 1;
                }
            }
        }


        if (!strcmp(action, "checkpoint")) {
            pid_t jvm = getppid();
            return checkpoint(jvm, basedir, argv[0], criu, imagedir);
        } else if (!strcmp(action, "restore")) {
            return restore(basedir, argv[0], criu, imagedir);
        } else if (!strcmp(action, "restorewait")) { // called by CRIU --exec-cmd
            return restorewait();
        } else {
            fprintf(stderr, "unknown command-line action: %s\n", action);
            return 1;
        }
    } else if ((action = getenv("CRTOOLS_SCRIPT_ACTION"))) { // called by CRIU --action-script
        if (!strcmp(action, "post-resume")) {
            return post_resume();
        } else {
            // ignore other notifications
            return 0;
        }
    } else {
        fprintf(stderr, "unknown context\n");
    }

    return 1;
}
