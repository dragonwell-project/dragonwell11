/*
 * Copyright (c) 2024 Alibaba Group Holding Limited. All Rights Reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation. Alibaba designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 */

#include <errno.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#include "crac_linux.hpp"
#include "runtime/globals.hpp"

//the value should equal to PseudoPersistentMode.SKIP_LOG_FILES in criuengine.c
#define SKIP_LOG_FILES_MODE 0x20

PseudoPersistent::PseudoPersistent(GrowableArray<PseudoPersistentFileDesc>
                                   *ppfd, const char *config) :
    _ppfd(ppfd), _append_files(NULL), _append_file_configs(NULL) {
  if (config == NULL) {
    return;
  }

  _append_file_configs = new(ResourceObj::C_HEAP, mtInternal) GrowableArray<AppendFileConfig *>(1, true);
  if (!strcmp(config, "*")) {
    AppendFileConfig *cfg = new AppendFileConfig(all, NULL, NULL);
    _append_file_configs->append(cfg);
    return;
  }

  const char *p = config, *s = config;
  while (*p) {
    if (*(p + 1) == ',' || *(p + 1) == '\0') {
      if (p - s > 2 && *s == '*' && *(s + 1) == '.') {
        _append_file_configs->append(new AppendFileConfig(by_extension, s + 2, p + 1));
      } else {
        _append_file_configs->append(new AppendFileConfig(by_full_path, s, p + 1));
      }
      if (*(p + 1) == ',') {
        p += 2;
        s = p;
      } else {
        break;
      }
    } else {
      p++;
    }
  }
}


bool PseudoPersistent::write_marked(const char* image_dir) {
  char *path;
  if (-1 == asprintf(&path, "%s/pseudopersistent", image_dir)) {
    return false;
  }
  FILE *f = fopen(path, "w");
  if (f == NULL) {
    fprintf(stderr, "open file: %s for write failed, error: %s\n",
            path, strerror(errno));
    free(path);
    return false;
  }

  if (_ppfd) {
    for(int i = 0 ; i < _ppfd->length();i++) {
      PseudoPersistentFileDesc *ppfd = _ppfd->adr_at(i);
      if (ppfd->_mark) {
        fprintf(f, "%d,%s\n", ppfd->_mode, ppfd->_path);
      }
    }
  }

  if (_append_files) {
    for (int i = 0; i < _append_files->length(); i++) {
      fprintf(f, "%d,%s\n", SKIP_LOG_FILES_MODE, _append_files->at(i));
    }
  }

  fclose(f);
  free(path);
  return true;
}

bool PseudoPersistent::in_registered_list(const char *path) {
  if (!_ppfd) {
    return false;
  }
  int j = 0;
  while (j < _ppfd->length()) {
    PseudoPersistentFileDesc *ppfd = _ppfd->adr_at(j);
    int r = strcmp(ppfd->_path, path);
    if (r == 0) {
      ppfd->_mark = true;
      return true;
    } else if (r > 0) {
      return false;
    }
    ++j;
  }
  return false;
}

bool PseudoPersistent::in_configured_list(const char* path, int fd) {
  if (_append_file_configs == NULL) {
    return false;
  }

  int ret = fcntl(fd, F_GETFL);
  if (ret != -1 && ((ret & O_ACCMODE) == O_WRONLY) && (ret & O_APPEND)) {
    for (int i = 0; i < _append_file_configs->length(); i++) {
      if (_append_file_configs->at(i)->match(path)) {
        if (_append_files == NULL) {
          _append_files = new(ResourceObj::C_HEAP, mtInternal) GrowableArray<char *>(2, true);
        }
        char *copy_path = NEW_C_HEAP_ARRAY(char, strlen(path) + 1, mtInternal);
        strcpy(copy_path, path);
        _append_files->append(copy_path);
        return true;
      }
    }
  }
  return false;
}

bool PseudoPersistent::AppendFileConfig::match(const char *file) {
  if (_type == all) {
    return true;
  } else if (_type == by_extension) {
    const char *p = strrchr(file, '.');
    return p != NULL && strlen(p + 1) == _size && !strncmp(p + 1, _start_ext_or_file, _size);
  } else if (_type == by_full_path) {
    return strlen(file) == _size && !strncmp(file, _start_ext_or_file, _size);
  }
  return false;
}

PseudoPersistent::~PseudoPersistent() {
  if (_append_files != NULL) {
    for (int i = 0; i < _append_files->length(); i++) {
      FREE_C_HEAP_ARRAY(char, _append_files->at(i));
    }
    delete _append_files;
    _append_files = NULL;
  }

  if (_append_file_configs != NULL) {
    for (int i = 0; i < _append_file_configs->length(); i++) {
      delete _append_file_configs->at(i);
    }
    delete _append_file_configs;
    _append_file_configs = NULL;
  }
}
