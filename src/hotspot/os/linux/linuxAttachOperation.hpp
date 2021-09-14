/*
 * Copyright (c) 2005, 2022, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2022, Azul Systems, Inc. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
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
 *
 */
#ifndef OS_LINUX_LINUXATTACHOPERATION_HPP
#define OS_LINUX_LINUXATTACHOPERATION_HPP
#include "services/attachListener.hpp"

class LinuxAttachOperation: public AttachOperation {
 private:
  // the connection to the client
  int _socket;
  bool _effectively_completed;
  void write_operation_result(jint result, bufferedStream* st);

 public:
  void complete(jint res, bufferedStream* st);
  void effectively_complete_raw(jint res, bufferedStream* st);
  bool is_effectively_completed()                      { return _effectively_completed; }

  void set_socket(int s)                                { _socket = s; }
  int socket() const                                    { return _socket; }

  LinuxAttachOperation(char* name) : AttachOperation(name) {
    set_socket(-1);
    _effectively_completed = false;
  }
};
#endif // OS_LINUX_LINUXATTACHOPERATION_HPP
