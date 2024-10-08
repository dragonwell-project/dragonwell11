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

#include <assert.h>
#include <errno.h>
#include <fcntl.h>
#include <libgen.h>
#include <limits.h>
#include <signal.h>
#include <stdint.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/stat.h>
#include <sys/statfs.h>
#include <sys/wait.h>
#include <unistd.h>

#define RESTORE_SIGNAL   (SIGRTMIN + 2)

#define PERFDATA_NAME "perfdata"
#define METADATA "metadata"
#define PSEUDO_FILE_SUFFIX ".dat"
#define PSEUDO_FILE_PREFIX "pp"
#define PIPE_FDS "pipefds"
#define MAX_PIPE_FDS 5
#define PIPEFS_MAGIC 0x50495045

#define ARRAY_SIZE(x) (sizeof(x) / sizeof(x[0]))

typedef int (*pseudo_file_func)(const char *, int, int, const char *);

enum PseudoPersistentMode {
    SAVE_RESTORE = 0x01,
    SAVE_ONLY = 0x02,
    OVERRIDE_WHEN_RESTORE = 0x04,
    COPY_WHEN_RESTORE = 0x08,
    SYMLINK_WHEN_RESTORE = 0x10,
};

static int create_cppath(const char *imagedir);

static int write_config_pipefds(const char *imagedir, const char *config_pipefds);
static int append_pipeinfo(const char* imagedir, const char* jvm_pid);
static int is_exist_pipefd(const char *jvm_pid, int fd);
static int read_fd_link(const char *jvm_pid, int fd, char *link, size_t len);

static int restore_validate(char *criu, char *image_dir, char *jvm_version, const char* unprivileged);
static int check_metadata(char *image_dir, char *jvm_version);
static int create_metadata(const char *image_dir, const char *jvm_version);
static int read_pseudo_file(const char *imagedir, pseudo_file_func func, const char* desc);
static int restore_pseudo_persistent_file(const char *imagedir, int id, int mode, const char *src);
static int exec_criu_command(const char *criu, const char *args[]);
static int add_unprivileged_opt(const char *unprivileged, const char **opts,
                         int size) {
  if (!strcmp("true", unprivileged)) {
    for (int pos = 0 ; pos < size; pos++) {
      if (opts[pos] == NULL) {
        if (pos == size - 1) {
          fprintf(stderr, "criu option array is not enough to add the --unprivileged option.");
          return 1;
        } else {
          opts[pos] = "--unprivileged";
          opts[pos + 1] = NULL;
          return 0;
        }
      }
    }
  }
  return 0;
}

static int g_pid;

static int kickjvm(pid_t jvm, int code) {
    union sigval sv = { .sival_int = code };
    if (-1 == sigqueue(jvm, RESTORE_SIGNAL, sv)) {
        perror("sigqueue");
        return 1;
    }
    return 0;
}

static int checkpoint(pid_t jvm, const char *basedir, const char *self,
                      const char *criu, const char *imagedir,
                      const char *validate_before_restore,
                      const char *jvm_version,
                      const char *unprivileged,
                      const char *config_pipefds) {
  const char *dump_cpuinfo[32] = {criu, "cpuinfo", "dump",
                                  "-D", imagedir,  NULL};
  pid_t pid = fork();
  if (pid < 0) {
    perror("cannot fork() for checkpoint.");
    return 1;
  } else if (pid) {
    // main process
    wait(NULL);
    return 0;
  }

  pid_t parent_before = getpid();

  // child
  pid = fork();
  if (pid < 0) {
    perror("cannot fork() for move out of JVM process hierarchy");
    return 1;
  } else if (pid) {
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

  // cppath must write before call criu. criuengine may be exit immediately if
  // criu kill jvm when running in
  // a container.
  create_cppath(imagedir);
  if (!strcmp("true", validate_before_restore)) {
    if (create_metadata(imagedir, jvm_version)) {
      return 1;
    }
    if (add_unprivileged_opt(unprivileged, dump_cpuinfo, ARRAY_SIZE(dump_cpuinfo))) {
      return 1;
    }
    if (exec_criu_command(criu, dump_cpuinfo)) {
      return 1;
    }
  }

  //write pipe fds to file first, then write actually pipe info in post_dump
  //because the dumpee process is freeze,if write before checkpoint there some files may changed.
  if (strlen(config_pipefds) > 0) {
    if (write_config_pipefds(imagedir, config_pipefds)) {
      return 1;
    }
  }

  char *leave_running = getenv("CRAC_CRIU_LEAVE_RUNNING");

  char jvmpidchar[32];
  snprintf(jvmpidchar, sizeof(jvmpidchar), "%d", jvm);

  pid_t child = fork();
  if (child < 0) {
    perror("cannot fork() for criu");
    return 1;
  } else if (!child) {
    const char *args[32] = {
        criu,          "dump", "-t", jvmpidchar,  "-D", imagedir,
        "--shell-job", "-v4",  "-o", "dump4.log", // -D without -W makes criu cd to image dir for logs
        "--action-script", self
    };
    const char **arg = args + 12;

    if (leave_running) {
      *arg++ = "-R";
    }
    if (!strcmp("true", unprivileged)) {
      *arg++ = "--unprivileged";
    }

    char *criuopts = getenv("CRAC_CRIU_OPTS");
    if (criuopts) {
      char *criuopt = strtok(criuopts, " ");
      while (criuopt &&
             ARRAY_SIZE(args) >=
                 (size_t)(arg - args) + 1 /* account for trailing NULL */) {
        *arg++ = criuopt;
        criuopt = strtok(NULL, " ");
      }
      if (criuopt) {
        fprintf(stderr,
                "Warning: too many arguments in CRAC_CRIU_OPTS (dropped from "
                "'%s')\n",
                criuopt);
      }
    }
    *arg++ = NULL;

    char resolved_path[PATH_MAX];
    if (realpath(imagedir, resolved_path) == NULL) {
      perror("get real path for image dir error");
      exit(1);
    }
    setenv("CRAC_IMAGE_DIR", resolved_path, 1);
    execv(criu, (char **)args);
    perror("criu dump");
    exit(1);
  }

  int status;
  if (child != wait(&status) || !WIFEXITED(status) || WEXITSTATUS(status)) {
    kickjvm(jvm, -1);
  } else if (leave_running) {
    kickjvm(jvm, 0);
  }

  exit(0);
}

static int restore(const char *basedir,
        const char *self,
        const char *criu,
        const char *imagedir,
        const char *unprivileged) {
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

    if (read_pseudo_file(imagedir, restore_pseudo_persistent_file,
                        "restore pseudo persistent file ")) {
      return 1;
    }

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
    if (!strcmp("true", unprivileged)) {
      *arg++ = "--unprivileged";
    }

    char *pipefds_path;
    struct stat st;
    if (-1 == asprintf(&pipefds_path, "%s/" PIPE_FDS, imagedir)) {
      return 1;
    }
    if (stat(pipefds_path, &st) == 0) {
      FILE *f = fopen(pipefds_path, "r");
      if (f == NULL) {
        perror("open pipefds when restore failed");
        return 1;
      }
      char buff[1024];
      //the first line is the config,need skip
      if (fgets(buff, sizeof(buff), f) == NULL) {
        perror("read the first line of pipefds failed");
        fclose(f);
        return 1;
      }

      int cnt = 0;
      while (fgets(buff, sizeof(buff), f) != NULL) {
        if (cnt > MAX_PIPE_FDS) {
          fprintf(stderr, "Support max pipe fds : %d, others are ignored!\n", MAX_PIPE_FDS);
          break;
        }
        char *p = strchr(buff, '\n');
        if (p) {
          *p = 0;
        }
        p = strchr(buff, ',');
        if (!p) {
          fprintf(stderr, "invalid %s file, miss comma. %s \n", pipefds_path, buff);
          fclose(f);
          return 1;
        }
        *p = 0;
        int pipe_fd = atoi(buff);
        *arg++ = "--inherit-fd";
        char *inherit_fd_value = NULL;
        if (-1 == asprintf(&inherit_fd_value, "fd[%d]:%s", pipe_fd, p+1)) {
          fclose(f);
          return 1;
        }
        *arg++ = inherit_fd_value;
        cnt++;
      }
      fclose(f);
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

static int copy_file(const char *to, const char *from) {
  int fd_to, fd_from;
  char buf[4096];
  ssize_t nread;
  int saved_errno;

  fd_from = open(from, O_RDONLY);
  if (fd_from < 0)
    return -1;

  fd_to = open(to, O_WRONLY | O_CREAT | O_EXCL, 0666);
  if (fd_to < 0)
    goto out_error;

  while ((nread = read(fd_from, buf, sizeof buf)) > 0) {
    char *out_ptr = buf;
    ssize_t nwritten;

    do {
      nwritten = write(fd_to, out_ptr, nread);

      if (nwritten >= 0) {
        nread -= nwritten;
        out_ptr += nwritten;
      } else if (errno != EINTR) {
        goto out_error;
      }
    } while (nread > 0);
  }

  if (nread == 0) {
    if (close(fd_to) < 0) {
      fd_to = -1;
      goto out_error;
    }
    close(fd_from);

    /* Success! */
    return 0;
  }

  out_error:
  saved_errno = errno;

  close(fd_from);
  if (fd_to >= 0)
    close(fd_to);

  errno = saved_errno;
  return -1;
}

static int checkpoint_pseudo_persistent_file(const char *imagedir, int id, int mode, const char *src) {
  if ((mode & SAVE_ONLY) || (mode & SAVE_RESTORE)) {
    char dest[PATH_MAX];
    snprintf(dest, PATH_MAX, "%s/%s%d%s", imagedir, PSEUDO_FILE_PREFIX, id, PSEUDO_FILE_SUFFIX);
    return copy_file(dest, src);
  } else {
    return 0;
  }
}

static int do_mkdir(const char *path, mode_t mode) {
  struct stat st;
  int status = 0;

  if (stat(path, &st) != 0) {
    /* Directory does not exist. EEXIST for race condition */
    if (mkdir(path, mode) != 0 && errno != EEXIST)
      status = -1;
  } else if (!S_ISDIR(st.st_mode)) {
    errno = ENOTDIR;
    status = -1;
  }
  return status;
}

int mkpath(const char *path, mode_t mode) {
  char *pp;
  char *sp;
  int status;
  char *copypath = strdup(path);

  status = 0;
  pp = copypath;
  while (status == 0 && (sp = strchr(pp, '/')) != 0) {
    if (sp != pp) {
      /* Neither root nor double slash in path */
      *sp = '\0';
      status = do_mkdir(copypath, mode);
      *sp = '/';
    }
    pp = sp + 1;
  }
  free(copypath);
  return status;
}

static int restore_pseudo_persistent_file(const char *imagedir, int id, int mode, const char *dest) {
  if (!(mode & SAVE_RESTORE)) {
    return 0;
  }
  struct stat st;
  if (stat(dest, &st) == 0) {
    //default action is skip if dest file exist.
    if (!(mode & OVERRIDE_WHEN_RESTORE)) {
      return 0;
    }
  }

  char src[PATH_MAX];
  snprintf(src, PATH_MAX, "%s/%s%d%s", imagedir, PSEUDO_FILE_PREFIX, id, PSEUDO_FILE_SUFFIX);
  if (mode & COPY_WHEN_RESTORE) {
    if (mkpath(dest, S_IRWXU | S_IRWXG | S_IROTH | S_IXOTH)) {
      perror(dest);
      return 1;
    }
    return copy_file(dest, src);
  } else if (mode & SYMLINK_WHEN_RESTORE) {
    if (mkpath(dest, S_IRWXU | S_IRWXG | S_IROTH | S_IXOTH)) {
      perror(dest);
      return 1;
    }
    if (symlink(src, dest)) {
      fprintf(stderr, "symlink %s to %s failed ,error: %s \n",
              dest, src, strerror(errno));
      return 1;
    }
  }
  return 0;
}

static int read_pseudo_file(const char *imagedir, pseudo_file_func func, const char* desc) {
  char filepath[PATH_MAX];
  snprintf(filepath, PATH_MAX, "%s/pseudopersistent", imagedir);

  //this file is not mandatory, return success if not exist.
  struct stat st;
  if (stat(filepath, &st) != 0) {
    return 0;
  }

  FILE *f = fopen(filepath, "r");
  if (f == NULL) {
    fprintf(stderr, "open file: %s for read failed, error: %s \n",
            filepath, strerror(errno));
    return 1;
  }

  int ret = 0;
  int id = 0;
  //an integer,file path
  char buff[PATH_MAX + 32];
  while (fgets(buff, sizeof(buff), f) != NULL) {
    char *p = strchr(buff, '\n');
    if (p) {
      *p = 0;
    }
    p = strchr(buff, ',');
    if (!p) {
      fprintf(stderr, "invalid %s file, miss comma\n", filepath);
      ret = 1;
      break;
    }
    *p = 0;
    int mode = atoi(buff);

    if (func(imagedir, id, mode, p + 1)) {
      fprintf(stderr, "%s with file %s failed \n", desc, p + 1);
      ret = 1;
      break;
    }
    id++;
  }
  fclose(f);
  return ret;
}

static int post_dump(void) {
  //"CRTOOLS_IMAGE_DIR" is a symbol link like "/proc/227/fd/100/",
  //and 227 is the pid of criu.If run with unprivileged mode, current
  //process has no permission to access the fd 100.
  //So use CRAC_IMAGE_DIR environment which setting before checkpointing.
  char *imagedir = getenv("CRAC_IMAGE_DIR");
  if (!imagedir) {
    fprintf(stderr, MSGPREFIX "cannot find CRAC_IMAGE_DIR env\n");
    return 1;
  }
  char *jvm_pid= getenv("CRTOOLS_INIT_PID");
  if (!jvm_pid) {
    fprintf(stderr, MSGPREFIX "cannot find CRTOOLS_INIT_PID env in post_dump callback\n");
    return 1;
  }
  if (append_pipeinfo(imagedir, jvm_pid)) {
    return 1;
  }
  return read_pseudo_file(imagedir, checkpoint_pseudo_persistent_file, "checkpoint pseudo persistent file ");
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
          return checkpoint(jvm, basedir, argv[0], criu, imagedir, argv[3],
                            argv[4], argv[5], argv[6]);
        } else if (!strcmp(action, "restore")) {
          return restore(basedir, argv[0], criu, imagedir, argv[3]);
        } else if (!strcmp(action,
                           "restorewait")) { // called by CRIU --exec-cmd
          return restorewait();
        } else if (!strcmp(action, "restorevalidate")) {
          return restore_validate(criu, imagedir, argv[3], argv[4]);
        } else {
          fprintf(stderr, "unknown command-line action: %s\n", action);
          return 1;
        }
    } else if ((action = getenv("CRTOOLS_SCRIPT_ACTION"))) { // called by CRIU --action-script
        if (!strcmp(action, "post-resume")) {
          return post_resume();
        } else if (!strcmp(action, "post-dump")) {
          return post_dump();
        } else {
            // ignore other notifications
            return 0;
        }
    } else {
        fprintf(stderr, "unknown context\n");
    }

    return 1;
}

static int restore_validate(char *criu, char *image_dir, char *jvm_version, const char* unprivileged) {
  const char *args[32] = {criu, "cpuinfo", "check", "--cpu-cap=jvm",
                          "-D", image_dir, NULL};
  if (add_unprivileged_opt(unprivileged, args, ARRAY_SIZE(args))) {
    return -1;
  }
  if (!check_metadata(image_dir, jvm_version)) {
    return exec_criu_command(criu, args);
  }
  return -1;
}

static int check_metadata(char *image_dir, char *jvm_version) {
  char *metadata_path;
  char buff[1024];
  int ret = -1;
  if (-1 == asprintf(&metadata_path, "%s/" METADATA, image_dir)) {
    return -1;
  }
  FILE *f = fopen(metadata_path, "r");
  if (f == NULL) {
    fprintf(stderr, "open file: %s for read failed, error: %s \n",
            metadata_path, strerror(errno));
    free(metadata_path);
    return -1;
  }

  if (fgets(buff, sizeof(buff), f) == NULL) {
    fprintf(stderr, "empty metadata\n");
  } else {
    ret = strncmp(jvm_version, buff, strlen(jvm_version));
    if (ret) {
      fprintf(stderr, "vm version %s != %s\n", buff, jvm_version);
    }
  }
  fclose(f);
  free(metadata_path);
  return ret;
}

static int create_metadata(const char *image_dir, const char *jvm_version) {
  char *metadata_path;
  int ret = -1;

  if (-1 == asprintf(&metadata_path, "%s/" METADATA, image_dir)) {
    return -1;
  }
  FILE *f = fopen(metadata_path, "w");
  if (f == NULL) {
    fprintf(stderr, "open file: %s for write failed, error: %s\n",
            metadata_path, strerror(errno));
    free(metadata_path);
    return -1;
  }

  if (fprintf(f, "%s\n", jvm_version) < 0) {
    fprintf(stderr, "write jvm version to metadata failed!\n");
  } else {
    ret = 0;
  }

  fclose(f);
  free(metadata_path);
  return ret;
}

static int exec_criu_command(const char *criu, const char *args[]) {
  pid_t pid = fork();
  if (pid < 0) {
    perror("cannot fork for criu");
    return -1;
  } else if (pid == 0) {
    execv(criu, (char **)args);
    perror("execv");
    exit(1);
  }

  int status;
  int ret;
  do {
    ret = waitpid(pid, &status, 0);
  } while (ret == -1 && errno == EINTR);

  if (ret == -1 || !WIFEXITED(status)) {
    return -1;
  }
  return WEXITSTATUS(status) == 0 ? 0 : -1;
}

static int write_config_pipefds(const char *imagedir, const char *config_pipefds) {
  char *path = NULL;
  int ret = 0;
  if (-1 == asprintf(&path, "%s/" PIPE_FDS, imagedir)) {
    return -1;
  }
  FILE *f = fopen(path, "w");
  if (f == NULL) {
    ret = -1;
    perror("open pipefds when write config failed");
    goto err;
  }

  if (fprintf(f, "%s\n", config_pipefds) < 0) {
    ret = -1;
    perror("write config to pipefds failed");
    goto err;
  }

err:
  if (f != NULL) {
    fclose(f);
  }
  free(path);
  return ret;
}

static int append_pipeinfo(const char* imagedir,const char* jvm_pid) {
  struct stat st;
  char *path = NULL;
  char buff[1024];

  if (-1 == asprintf(&path, "%s/" PIPE_FDS, imagedir)) {
    fprintf(stderr, "asprintf for pipefds failed. imagedir:%s", imagedir);
    return -1;
  }

  if (stat(path, &st) != 0) {
    fprintf(stderr, "%s not exist.", path);
    free(path);
    return 0;
  }

  FILE* f = fopen(path, "a+");
  if (f == NULL) {
    perror("open pipefds for appending failed");
    free(path);
    return -1;
  }

  free(path);

  if (fgets(buff, sizeof(buff), f) == NULL) {
    perror("read from pipefds error");
    fclose(f);
    return -1;
  }

  char *p = strchr(buff, '\n');
  if (p) {
    *p = 0;
  }
  char fdpath[PATH_MAX];
  char *token = strtok(buff, ",");
  while (token != NULL) {
    int fd = atoi(token);
    if (is_exist_pipefd(jvm_pid, fd)) {
      if (read_fd_link(jvm_pid, fd, fdpath, sizeof(fdpath)) == -1) {
        fclose(f);
        return -1;
      }
      fprintf(f, "%d,%s\n", fd, fdpath);
    }
    token = strtok(NULL, ",");
  }
  fclose(f);
  return 0;
}

static int is_exist_pipefd(const char *jvm_pid, int fd) {
  struct statfs fsbuf;
  char fdpath[64];
  snprintf(fdpath, sizeof(fdpath), "/proc/%s/fd/%d", jvm_pid, fd);
  if (statfs(fdpath, &fsbuf) < 0) {
    return 0;
  }
  return fsbuf.f_type == PIPEFS_MAGIC;
}

static int read_fd_link(const char* jvm_pid, int fd, char *link, size_t len) {
  char fdpath[64];
  snprintf(fdpath, sizeof(fdpath), "/proc/%s/fd/%d", jvm_pid, fd);
  int ret = readlink(fdpath, link, len);
  if (ret == -1) {
    perror(fdpath);
    return ret;
  }
  link[(unsigned)ret < len ? (unsigned)ret : len - 1] = '\0';
  return ret;
}
