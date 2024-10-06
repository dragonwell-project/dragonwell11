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

#ifndef OS_LINUX_CRAC_LINUX_HPP
#define OS_LINUX_CRAC_LINUX_HPP
#include "utilities/growableArray.hpp"

struct PseudoPersistentFileDesc {
  int _mode;
  bool _mark;
  const char* _path;
  PseudoPersistentFileDesc(int mode, const char *path) :
      _mode(mode),
      _path(path),
      _mark(false)
  {}

  PseudoPersistentFileDesc():
      _mode(0),
      _path(NULL),
      _mark(false)
  {}
};

class PseudoPersistent {
private:
  enum AppendFileConfigType {
    all,
    by_extension,
    by_full_path
  };

  class AppendFileConfig : public CHeapObj<mtInternal> {
    AppendFileConfigType _type;
    size_t _size;
    const char *_start_ext_or_file;
  public:
    AppendFileConfig(AppendFileConfigType type, const char *start_ext_or_file, const char *end_ext_or_file)
        : _type(type), _start_ext_or_file(start_ext_or_file), _size(0) {
      if (start_ext_or_file && end_ext_or_file) {
        _size = end_ext_or_file - start_ext_or_file;
      }
    }
    bool match(const char *file);
  };

private:
  GrowableArray<PseudoPersistentFileDesc> *_ppfd;
  GrowableArray<char *> *_append_files;
  GrowableArray<AppendFileConfig *> *_append_file_configs;
public:
  PseudoPersistent(GrowableArray<PseudoPersistentFileDesc>* ppfd, const char* config);
  ~PseudoPersistent();

  bool test_and_mark(const char *path, int fd) {
    return in_registered_list(path) || in_configured_list(path, fd);
  }

  bool write_marked(const char* image_dir);
private:
  bool in_configured_list(const char* path, int fd);
  bool in_registered_list(const char *path);
};

#endif //OS_LINUX_CRAC_LINUX_HPP
