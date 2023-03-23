/*
 * Copyright (c) 2023, Alibaba Group Holding Limited. All rights reserved.
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
 */

#ifndef SHARE_VM_RUNTIME_QUICKSTART_HPP
#define SHARE_VM_RUNTIME_QUICKSTART_HPP

#include "memory/allocation.hpp"
#include "utilities/growableArray.hpp"
#include "utilities/ostream.hpp"

class QuickStart : AllStatic {
public:
  enum opt {
    _appcds,
    _eagerappcds,
    Count
  };

  typedef enum {
    Normal,
    Tracer,
    Replayer,
    Profiler,
    Dumper
  } QuickStartRole;

  ~QuickStart();
  static const char* cache_path()       { return _cache_path; }
  static bool is_enabled()              { return _is_enabled; }
  static bool verbose()                 { return _verbose; }
  static bool parse_command_line_arguments(const char* opts = NULL);
  static void post_process_arguments(JavaVMInitArgs* options_args);
  static void initialize(TRAPS);
  static bool is_tracer()               { return _role == Tracer; }
  static bool is_replayer()             { return _role == Replayer; }
  static bool is_profiler()             { return _role == Profiler; }
  static bool is_normal()               { return _role == Normal; }
  static bool is_dumper()               { return _role == Dumper; }
  static bool is_starting()             { return is_enabled() && _is_starting; }
  static bool is_eagerappcds_enabled()  { return _opt_enabled[_eagerappcds]; }
  static bool is_appcds_enabled()       { return _opt_enabled[_appcds]; }
  static const char* cds_original_lst() { return _origin_class_list; }
  static const char* cds_final_lst()    { return _final_class_list; }

  static int remove_dir(const char* dir);
  static const char* image_id()         { return _image_id; }
  static const char* vm_version()       { return _vm_version; }

private:
  static const char* _cache_path;
  static const char* _image_id;
  static const char* _vm_version;
  static const char* _lock_path;
  static const char* _temp_metadata_file_path;
  static const char* _metadata_file_path;

  static FILE*       _metadata_file;
  static fileStream* _temp_metadata_file;

  static int _lock_file_fd;

  static QuickStartRole _role;

  static bool _is_starting;
  static bool _is_enabled;
  static bool _verbose;
  static bool _print_stat_enabled;
  static bool _need_destroy;
  static const char* _identifier_name[];
  static const char* _opt_name[Count];
  static bool _opt_enabled[Count];
  static bool _opt_passed[Count];
  static int _jvm_option_count;
  static bool _profile_only;
  static bool _dump_only;
  static bool _replay_only;
  static const char** _jvm_options;
  static const char* _cp_in_metadata_file;

  static bool set_optimization(const char* option, bool enabled);
  static bool determine_role(JavaVMInitArgs* options_args);
  static bool prepare_dump(JavaVMInitArgs* options_args);
  static void calculate_cache_path();
  static void destroy_cache_folder();
  static void setenv_for_roles();
  static void process_argument_for_optimization();
  static bool check_integrity(JavaVMInitArgs* options_args, const char* meta_file);
  static void generate_metadata_file(bool rename_metafile);
  static bool match_option(const char* option, const char* name, const char** tail);
  static void print_command_line_help(outputStream* out);
  static bool dump_cached_info(JavaVMInitArgs* options_args);
  static bool load_and_validate(JavaVMInitArgs* options_args);
  static void check_features(const char* &str);
  static void print_stat(bool isReplayer);
  static void settle_opt_pass_table();
  static void add_dump_hook(TRAPS);
  static void trim_tail_newline(char* str);
  static Handle jvm_option_handle(TRAPS);
  static bool enable_by_env(const JavaVMInitArgs* options_args);

public:
  static void set_opt_passed(opt feature);
  static void notify_dump();

  // cds stuff
private:
  static const char* _origin_class_list;
  static const char* _final_class_list;
  static const char* _jsa;
  static const char* _eagerappcds_agentlib;
  static const char* _eagerappcds_agent;
private:
  static void enable_eagerappcds();
  static void enable_appcds();

  static void add_CDSDumpHook(TRAPS);
};

#endif
