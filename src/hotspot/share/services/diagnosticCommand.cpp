/*
 * Copyright (c) 2011, 2019, Oracle and/or its affiliates. All rights reserved.
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

#include "precompiled.hpp"
#include "jvm.h"
#ifdef LINUX
#include "attachListener_linux.hpp"
#endif
#include "classfile/classLoaderHierarchyDCmd.hpp"
#include "classfile/classLoaderStats.hpp"
#include "classfile/compactHashtable.hpp"
#include "compiler/compileBroker.hpp"
#include "compiler/directivesParser.hpp"
#include "gc/shared/vmGCOperations.hpp"
#ifdef LINUX
#include "linuxAttachOperation.hpp"
#endif
#include "memory/metaspace/metaspaceDCmd.hpp"
#include "memory/resourceArea.hpp"
#include "oops/objArrayOop.inline.hpp"
#include "oops/oop.inline.hpp"
#include "oops/typeArrayOop.inline.hpp"
#include "runtime/fieldDescriptor.inline.hpp"
#include "runtime/flags/jvmFlag.hpp"
#include "runtime/handles.inline.hpp"
#include "runtime/interfaceSupport.inline.hpp"
#include "runtime/javaCalls.hpp"
#include "runtime/quickStart.hpp"
#include "runtime/os.hpp"
#include "services/diagnosticArgument.hpp"
#include "services/diagnosticCommand.hpp"
#include "services/diagnosticFramework.hpp"
#include "services/heapDumper.hpp"
#include "services/management.hpp"
#include "services/writeableFlags.hpp"
#include "utilities/debug.hpp"
#include "utilities/formatBuffer.hpp"
#include "utilities/macros.hpp"
#ifdef LINUX
#include "trimCHeapDCmd.hpp"
#endif
#include "jfr/jni/jfrJavaCall.hpp"
#include "jfr/jni/jfrJavaSupport.hpp"


static void loadAgentModule(TRAPS) {
  ResourceMark rm(THREAD);
  HandleMark hm(THREAD);

  JavaValue result(T_OBJECT);
  Handle h_module_name = java_lang_String::create_from_str("jdk.management.agent", CHECK);
  JavaCalls::call_static(&result,
                         SystemDictionary::module_Modules_klass(),
                         vmSymbols::loadModule_name(),
                         vmSymbols::loadModule_signature(),
                         h_module_name,
                         THREAD);
}

void DCmdRegistrant::register_dcmds(){
  // Registration of the diagnostic commands
  // First argument specifies which interfaces will export the command
  // Second argument specifies if the command is enabled
  // Third  argument specifies if the command is hidden
  uint32_t full_export = DCmd_Source_Internal | DCmd_Source_AttachAPI
                         | DCmd_Source_MBean;
  DCmdFactory::register_DCmdFactory(new DCmdFactoryImpl<HelpDCmd>(full_export, true, false));
  DCmdFactory::register_DCmdFactory(new DCmdFactoryImpl<VersionDCmd>(full_export, true, false));
  DCmdFactory::register_DCmdFactory(new DCmdFactoryImpl<CommandLineDCmd>(full_export, true, false));
  DCmdFactory::register_DCmdFactory(new DCmdFactoryImpl<PrintSystemPropertiesDCmd>(full_export, true, false));
  DCmdFactory::register_DCmdFactory(new DCmdFactoryImpl<PrintVMFlagsDCmd>(full_export, true, false));
  DCmdFactory::register_DCmdFactory(new DCmdFactoryImpl<SetVMFlagDCmd>(full_export, true, false));
  DCmdFactory::register_DCmdFactory(new DCmdFactoryImpl<VMDynamicLibrariesDCmd>(full_export, true, false));
  DCmdFactory::register_DCmdFactory(new DCmdFactoryImpl<VMUptimeDCmd>(full_export, true, false));
  DCmdFactory::register_DCmdFactory(new DCmdFactoryImpl<VMInfoDCmd>(full_export, true, false));
  DCmdFactory::register_DCmdFactory(new DCmdFactoryImpl<SystemGCDCmd>(full_export, true, false));
  DCmdFactory::register_DCmdFactory(new DCmdFactoryImpl<RunFinalizationDCmd>(full_export, true, false));
  DCmdFactory::register_DCmdFactory(new DCmdFactoryImpl<HeapInfoDCmd>(full_export, true, false));
  DCmdFactory::register_DCmdFactory(new DCmdFactoryImpl<FinalizerInfoDCmd>(full_export, true, false));
  DCmdFactory::register_DCmdFactory(new DCmdFactoryImpl<QuickStartDumpDCMD>(full_export, true, false));
#if INCLUDE_SERVICES
  DCmdFactory::register_DCmdFactory(new DCmdFactoryImpl<HeapDumpDCmd>(DCmd_Source_Internal | DCmd_Source_AttachAPI, true, false));
  DCmdFactory::register_DCmdFactory(new DCmdFactoryImpl<ClassHistogramDCmd>(full_export, true, false));
  DCmdFactory::register_DCmdFactory(new DCmdFactoryImpl<ClassStatsDCmd>(full_export, true, false));
  DCmdFactory::register_DCmdFactory(new DCmdFactoryImpl<SystemDictionaryDCmd>(full_export, true, false));
  DCmdFactory::register_DCmdFactory(new DCmdFactoryImpl<ClassHierarchyDCmd>(full_export, true, false));
  DCmdFactory::register_DCmdFactory(new DCmdFactoryImpl<SymboltableDCmd>(full_export, true, false));
  DCmdFactory::register_DCmdFactory(new DCmdFactoryImpl<StringtableDCmd>(full_export, true, false));
  DCmdFactory::register_DCmdFactory(new DCmdFactoryImpl<metaspace::MetaspaceDCmd>(full_export, true, false));
#if INCLUDE_JVMTI // Both JVMTI and SERVICES have to be enabled to have this dcmd
  DCmdFactory::register_DCmdFactory(new DCmdFactoryImpl<JVMTIAgentLoadDCmd>(full_export, true, false));
#endif // INCLUDE_JVMTI
#endif // INCLUDE_SERVICES
#if INCLUDE_JVMTI
  DCmdFactory::register_DCmdFactory(new DCmdFactoryImpl<JVMTIDataDumpDCmd>(full_export, true, false));
#endif // INCLUDE_JVMTI
  DCmdFactory::register_DCmdFactory(new DCmdFactoryImpl<ThreadDumpDCmd>(full_export, true, false));
  DCmdFactory::register_DCmdFactory(new DCmdFactoryImpl<ClassLoaderStatsDCmd>(full_export, true, false));
  DCmdFactory::register_DCmdFactory(new DCmdFactoryImpl<ClassLoaderHierarchyDCmd>(full_export, true, false));
  DCmdFactory::register_DCmdFactory(new DCmdFactoryImpl<CompileQueueDCmd>(full_export, true, false));
  DCmdFactory::register_DCmdFactory(new DCmdFactoryImpl<CodeListDCmd>(full_export, true, false));
  DCmdFactory::register_DCmdFactory(new DCmdFactoryImpl<CodeCacheDCmd>(full_export, true, false));
#ifdef LINUX
  DCmdFactory::register_DCmdFactory(new DCmdFactoryImpl<TrimCLibcHeapDCmd>(full_export, true, false));
  DCmdFactory::register_DCmdFactory(new DCmdFactoryImpl<PerfMapDCmd>(full_export, true, false));
#endif // LINUX
  DCmdFactory::register_DCmdFactory(new DCmdFactoryImpl<TouchedMethodsDCmd>(full_export, true, false));
  DCmdFactory::register_DCmdFactory(new DCmdFactoryImpl<CodeHeapAnalyticsDCmd>(full_export, true, false));

  DCmdFactory::register_DCmdFactory(new DCmdFactoryImpl<CompilerDirectivesPrintDCmd>(full_export, true, false));
  DCmdFactory::register_DCmdFactory(new DCmdFactoryImpl<CompilerDirectivesAddDCmd>(full_export, true, false));
  DCmdFactory::register_DCmdFactory(new DCmdFactoryImpl<CompilerDirectivesRemoveDCmd>(full_export, true, false));
  DCmdFactory::register_DCmdFactory(new DCmdFactoryImpl<CompilerDirectivesClearDCmd>(full_export, true, false));

#ifdef LINUX
  DCmdFactory::register_DCmdFactory(new DCmdFactoryImpl<CheckpointDCmd>(full_export, true,false));
#endif

  // Enhanced JMX Agent Support
  // These commands won't be exported via the DiagnosticCommandMBean until an
  // appropriate permission is created for them
  uint32_t jmx_agent_export_flags = DCmd_Source_Internal | DCmd_Source_AttachAPI;
  DCmdFactory::register_DCmdFactory(new DCmdFactoryImpl<JMXStartRemoteDCmd>(jmx_agent_export_flags, true,false));
  DCmdFactory::register_DCmdFactory(new DCmdFactoryImpl<JMXStartLocalDCmd>(jmx_agent_export_flags, true,false));
  DCmdFactory::register_DCmdFactory(new DCmdFactoryImpl<JMXStopRemoteDCmd>(jmx_agent_export_flags, true,false));
  DCmdFactory::register_DCmdFactory(new DCmdFactoryImpl<JMXStatusDCmd>(jmx_agent_export_flags, true,false));

  // Debug on cmd (only makes sense with JVMTI since the agentlib needs it).
#if INCLUDE_JVMTI
  DCmdFactory::register_DCmdFactory(new DCmdFactoryImpl<DebugOnCmdStartDCmd>(full_export, true, true));
#endif // INCLUDE_JVMTI

}

#ifndef HAVE_EXTRA_DCMD
void DCmdRegistrant::register_dcmds_ext(){
   // Do nothing here
}
#endif


HelpDCmd::HelpDCmd(outputStream* output, bool heap) : DCmdWithParser(output, heap),
  _all("-all", "Show help for all commands", "BOOLEAN", false, "false"),
  _cmd("command name", "The name of the command for which we want help",
        "STRING", false) {
  _dcmdparser.add_dcmd_option(&_all);
  _dcmdparser.add_dcmd_argument(&_cmd);
};


static int compare_strings(const char** s1, const char** s2) {
  return ::strcmp(*s1, *s2);
}

void HelpDCmd::execute(DCmdSource source, TRAPS) {
  if (_all.value()) {
    GrowableArray<const char*>* cmd_list = DCmdFactory::DCmd_list(source);
    cmd_list->sort(compare_strings);
    for (int i = 0; i < cmd_list->length(); i++) {
      DCmdFactory* factory = DCmdFactory::factory(source, cmd_list->at(i),
                                                  strlen(cmd_list->at(i)));
      output()->print_cr("%s%s", factory->name(),
                         factory->is_enabled() ? "" : " [disabled]");
      output()->print_cr("\t%s", factory->description());
      output()->cr();
      factory = factory->next();
    }
  } else if (_cmd.has_value()) {
    DCmd* cmd = NULL;
    DCmdFactory* factory = DCmdFactory::factory(source, _cmd.value(),
                                                strlen(_cmd.value()));
    if (factory != NULL) {
      output()->print_cr("%s%s", factory->name(),
                         factory->is_enabled() ? "" : " [disabled]");
      output()->print_cr("%s", factory->description());
      output()->print_cr("\nImpact: %s", factory->impact());
      JavaPermission p = factory->permission();
      if(p._class != NULL) {
        if(p._action != NULL) {
          output()->print_cr("\nPermission: %s(%s, %s)",
                  p._class, p._name == NULL ? "null" : p._name, p._action);
        } else {
          output()->print_cr("\nPermission: %s(%s)",
                  p._class, p._name == NULL ? "null" : p._name);
        }
      }
      output()->cr();
      cmd = factory->create_resource_instance(output());
      if (cmd != NULL) {
        DCmdMark mark(cmd);
        cmd->print_help(factory->name());
      }
    } else {
      output()->print_cr("Help unavailable : '%s' : No such command", _cmd.value());
    }
  } else {
    output()->print_cr("The following commands are available:");
    GrowableArray<const char *>* cmd_list = DCmdFactory::DCmd_list(source);
    cmd_list->sort(compare_strings);
    for (int i = 0; i < cmd_list->length(); i++) {
      DCmdFactory* factory = DCmdFactory::factory(source, cmd_list->at(i),
                                                  strlen(cmd_list->at(i)));
      output()->print_cr("%s%s", factory->name(),
                         factory->is_enabled() ? "" : " [disabled]");
      factory = factory->_next;
    }
    output()->print_cr("\nFor more information about a specific command use 'help <command>'.");
  }
}

int HelpDCmd::num_arguments() {
  ResourceMark rm;
  HelpDCmd* dcmd = new HelpDCmd(NULL, false);
  if (dcmd != NULL) {
    DCmdMark mark(dcmd);
    return dcmd->_dcmdparser.num_arguments();
  } else {
    return 0;
  }
}

void VersionDCmd::execute(DCmdSource source, TRAPS) {
  output()->print_cr("%s version %s", Abstract_VM_Version::vm_name(),
          Abstract_VM_Version::vm_release());
  JDK_Version jdk_version = JDK_Version::current();
  if (jdk_version.patch_version() > 0) {
    output()->print_cr("JDK %d.%d.%d.%d", jdk_version.major_version(),
            jdk_version.minor_version(), jdk_version.security_version(),
            jdk_version.patch_version());
  } else {
    output()->print_cr("JDK %d.%d.%d", jdk_version.major_version(),
            jdk_version.minor_version(), jdk_version.security_version());
  }
}

PrintVMFlagsDCmd::PrintVMFlagsDCmd(outputStream* output, bool heap) :
                                   DCmdWithParser(output, heap),
  _all("-all", "Print all flags supported by the VM", "BOOLEAN", false, "false") {
  _dcmdparser.add_dcmd_option(&_all);
}

void PrintVMFlagsDCmd::execute(DCmdSource source, TRAPS) {
  if (_all.value()) {
    JVMFlag::printFlags(output(), true);
  } else {
    JVMFlag::printSetFlags(output());
  }
}

int PrintVMFlagsDCmd::num_arguments() {
    ResourceMark rm;
    PrintVMFlagsDCmd* dcmd = new PrintVMFlagsDCmd(NULL, false);
    if (dcmd != NULL) {
      DCmdMark mark(dcmd);
      return dcmd->_dcmdparser.num_arguments();
    } else {
      return 0;
    }
}

SetVMFlagDCmd::SetVMFlagDCmd(outputStream* output, bool heap) :
                                   DCmdWithParser(output, heap),
  _flag("flag name", "The name of the flag we want to set",
        "STRING", true),
  _value("string value", "The value we want to set", "STRING", false) {
  _dcmdparser.add_dcmd_argument(&_flag);
  _dcmdparser.add_dcmd_argument(&_value);
}

void SetVMFlagDCmd::execute(DCmdSource source, TRAPS) {
  const char* val = NULL;
  if (_value.value() != NULL) {
    val = _value.value();
  }

  FormatBuffer<80> err_msg("%s", "");
  int ret = WriteableFlags::set_flag(_flag.value(), val, JVMFlag::MANAGEMENT, err_msg);

  if (ret != JVMFlag::SUCCESS) {
    output()->print_cr("%s", err_msg.buffer());
  }
}

int SetVMFlagDCmd::num_arguments() {
  ResourceMark rm;
  SetVMFlagDCmd* dcmd = new SetVMFlagDCmd(NULL, false);
  if (dcmd != NULL) {
    DCmdMark mark(dcmd);
    return dcmd->_dcmdparser.num_arguments();
  } else {
    return 0;
  }
}

void JVMTIDataDumpDCmd::execute(DCmdSource source, TRAPS) {
  if (JvmtiExport::should_post_data_dump()) {
    JvmtiExport::post_data_dump();
  }
}

#if INCLUDE_SERVICES
#if INCLUDE_JVMTI
JVMTIAgentLoadDCmd::JVMTIAgentLoadDCmd(outputStream* output, bool heap) :
                                       DCmdWithParser(output, heap),
  _libpath("library path", "Absolute path of the JVMTI agent to load.",
           "STRING", true),
  _option("agent option", "Option string to pass the agent.", "STRING", false) {
  _dcmdparser.add_dcmd_argument(&_libpath);
  _dcmdparser.add_dcmd_argument(&_option);
}

void JVMTIAgentLoadDCmd::execute(DCmdSource source, TRAPS) {

  if (_libpath.value() == NULL) {
    output()->print_cr("JVMTI.agent_load dcmd needs library path.");
    return;
  }

  char *suffix = strrchr(_libpath.value(), '.');
  bool is_java_agent = (suffix != NULL) && (strncmp(".jar", suffix, 4) == 0);

  if (is_java_agent) {
    if (_option.value() == NULL) {
      JvmtiExport::load_agent_library("instrument", "false",
                                      _libpath.value(), output());
    } else {
      size_t opt_len = strlen(_libpath.value()) + strlen(_option.value()) + 2;
      if (opt_len > 4096) {
        output()->print_cr("JVMTI agent attach failed: Options is too long.");
        return;
      }

      char *opt = (char *)os::malloc(opt_len, mtInternal);
      if (opt == NULL) {
        output()->print_cr("JVMTI agent attach failed: "
                           "Could not allocate " SIZE_FORMAT " bytes for argument.",
                           opt_len);
        return;
      }

      jio_snprintf(opt, opt_len, "%s=%s", _libpath.value(), _option.value());
      JvmtiExport::load_agent_library("instrument", "false", opt, output());

      os::free(opt);
    }
  } else {
    JvmtiExport::load_agent_library(_libpath.value(), "true",
                                    _option.value(), output());
  }
}

int JVMTIAgentLoadDCmd::num_arguments() {
  ResourceMark rm;
  JVMTIAgentLoadDCmd* dcmd = new JVMTIAgentLoadDCmd(NULL, false);
  if (dcmd != NULL) {
    DCmdMark mark(dcmd);
    return dcmd->_dcmdparser.num_arguments();
  } else {
    return 0;
  }
}
#endif // INCLUDE_JVMTI
#endif // INCLUDE_SERVICES

void PrintSystemPropertiesDCmd::execute(DCmdSource source, TRAPS) {
  // load VMSupport
  Symbol* klass = vmSymbols::jdk_internal_vm_VMSupport();
  Klass* k = SystemDictionary::resolve_or_fail(klass, true, CHECK);
  InstanceKlass* ik = InstanceKlass::cast(k);
  if (ik->should_be_initialized()) {
    ik->initialize(THREAD);
  }
  if (HAS_PENDING_EXCEPTION) {
    java_lang_Throwable::print(PENDING_EXCEPTION, output());
    output()->cr();
    CLEAR_PENDING_EXCEPTION;
    return;
  }

  // invoke the serializePropertiesToByteArray method
  JavaValue result(T_OBJECT);
  JavaCallArguments args;

  Symbol* signature = vmSymbols::serializePropertiesToByteArray_signature();
  JavaCalls::call_static(&result,
                         ik,
                         vmSymbols::serializePropertiesToByteArray_name(),
                         signature,
                         &args,
                         THREAD);
  if (HAS_PENDING_EXCEPTION) {
    java_lang_Throwable::print(PENDING_EXCEPTION, output());
    output()->cr();
    CLEAR_PENDING_EXCEPTION;
    return;
  }

  // The result should be a [B
  oop res = (oop)result.get_jobject();
  assert(res->is_typeArray(), "just checking");
  assert(TypeArrayKlass::cast(res->klass())->element_type() == T_BYTE, "just checking");

  // copy the bytes to the output stream
  typeArrayOop ba = typeArrayOop(res);
  jbyte* addr = typeArrayOop(res)->byte_at_addr(0);
  output()->print_raw((const char*)addr, ba->length());
}

VMUptimeDCmd::VMUptimeDCmd(outputStream* output, bool heap) :
                           DCmdWithParser(output, heap),
  _date("-date", "Add a prefix with current date", "BOOLEAN", false, "false") {
  _dcmdparser.add_dcmd_option(&_date);
}

void VMUptimeDCmd::execute(DCmdSource source, TRAPS) {
  if (_date.value()) {
    output()->date_stamp(true, "", ": ");
  }
  output()->time_stamp().update_to(tty->time_stamp().ticks());
  output()->stamp();
  output()->print_cr(" s");
}

int VMUptimeDCmd::num_arguments() {
  ResourceMark rm;
  VMUptimeDCmd* dcmd = new VMUptimeDCmd(NULL, false);
  if (dcmd != NULL) {
    DCmdMark mark(dcmd);
    return dcmd->_dcmdparser.num_arguments();
  } else {
    return 0;
  }
}

void VMInfoDCmd::execute(DCmdSource source, TRAPS) {
  VMError::print_vm_info(_output);
}

void SystemGCDCmd::execute(DCmdSource source, TRAPS) {
  Universe::heap()->collect(GCCause::_dcmd_gc_run);
}

void RunFinalizationDCmd::execute(DCmdSource source, TRAPS) {
  Klass* k = SystemDictionary::resolve_or_fail(vmSymbols::java_lang_System(),
                                                 true, CHECK);
  JavaValue result(T_VOID);
  JavaCalls::call_static(&result, k,
                         vmSymbols::run_finalization_name(),
                         vmSymbols::void_method_signature(), CHECK);
}

void HeapInfoDCmd::execute(DCmdSource source, TRAPS) {
  MutexLocker hl(Heap_lock);
  Universe::heap()->print_on(output());
}

void FinalizerInfoDCmd::execute(DCmdSource source, TRAPS) {
  ResourceMark rm;

  Klass* k = SystemDictionary::resolve_or_fail(
    vmSymbols::finalizer_histogram_klass(), true, CHECK);

  JavaValue result(T_ARRAY);

  // We are calling lang.ref.FinalizerHistogram.getFinalizerHistogram() method
  // and expect it to return array of FinalizerHistogramEntry as Object[]

  JavaCalls::call_static(&result, k,
                         vmSymbols::get_finalizer_histogram_name(),
                         vmSymbols::void_finalizer_histogram_entry_array_signature(), CHECK);

  objArrayOop result_oop = (objArrayOop) result.get_jobject();
  if (result_oop->length() == 0) {
    output()->print_cr("No instances waiting for finalization found");
    return;
  }

  oop foop = result_oop->obj_at(0);
  InstanceKlass* ik = InstanceKlass::cast(foop->klass());

  fieldDescriptor count_fd, name_fd;

  Klass* count_res = ik->find_field(
    vmSymbols::finalizer_histogram_entry_count_field(), vmSymbols::int_signature(), &count_fd);

  Klass* name_res = ik->find_field(
    vmSymbols::finalizer_histogram_entry_name_field(), vmSymbols::string_signature(), &name_fd);

  assert(count_res != NULL && name_res != NULL, "Unexpected layout of FinalizerHistogramEntry");

  output()->print_cr("Unreachable instances waiting for finalization");
  output()->print_cr("#instances  class name");
  output()->print_cr("-----------------------");

  for (int i = 0; i < result_oop->length(); ++i) {
    oop element_oop = result_oop->obj_at(i);
    oop str_oop = element_oop->obj_field(name_fd.offset());
    char *name = java_lang_String::as_utf8_string(str_oop);
    int count = element_oop->int_field(count_fd.offset());
    output()->print_cr("%10d  %s", count, name);
  }
}

#if INCLUDE_SERVICES // Heap dumping/inspection supported
HeapDumpDCmd::HeapDumpDCmd(outputStream* output, bool heap) :
                           DCmdWithParser(output, heap),
  _filename("filename","Name of the dump file", "STRING",true),
  _all("-all", "Dump all objects, including unreachable objects",
       "BOOLEAN", false, "false"),
  _mini_dump("-mini", "Use mini-dump format",
       "BOOLEAN", false, "false"),
  _gzip("-gz", "If specified, the heap dump is written in gzipped format "
               "using the given compression level. 1 (recommended) is the fastest, "
               "9 the strongest compression.", "INT", false, "1"),
  _overwrite("-overwrite", "If specified, the dump file will be overwritten if it exists",
           "BOOLEAN", false, "false"),
  _parallel("-parallel", "Number of parallel threads to use for heap dump. The VM "
                         "will try to use the specified number of threads, but might use fewer.",
            "INT", false, "1") {
  _dcmdparser.add_dcmd_option(&_all);
  _dcmdparser.add_dcmd_option(&_mini_dump);
  _dcmdparser.add_dcmd_argument(&_filename);
  _dcmdparser.add_dcmd_option(&_gzip);
  _dcmdparser.add_dcmd_option(&_overwrite);
  _dcmdparser.add_dcmd_option(&_parallel);
}

void HeapDumpDCmd::execute(DCmdSource source, TRAPS) {
  jlong level = -1; // -1 means no compression.
  jlong parallel = HeapDumper::default_num_of_dump_threads();

  if (_gzip.is_set()) {
    level = _gzip.value();

    if (level < 1 || level > 9) {
      output()->print_cr("Compression level out of range (1-9): " JLONG_FORMAT, level);
      return;
    }
  }

  if (_parallel.is_set()) {
    parallel = _parallel.value();

    if (parallel < 0) {
      output()->print_cr("Invalid number of parallel dump threads.");
      return;
    } else if (parallel == 0) {
      // 0 implies to disable parallel heap dump, in such case, we use serial dump instead
      parallel = 1;
    }
  }

  // Request a full GC before heap dump if _all is false
  // This helps reduces the amount of unreachable objects in the dump
  // and makes it easier to browse.
  HeapDumper dumper(!_all.value() /* request GC if _all is false*/, false, _mini_dump.value());
  int res = dumper.dump(_filename.value(), output(), (int) level, _overwrite.value(), (uint)parallel);
  if (res == 0) {
    if (_mini_dump.value()) {
      output()->print_cr("Mini heap dump file created");
    } else {
      output()->print_cr("Heap dump file created");
    }
  } else {
    // heap dump failed
    ResourceMark rm;
    char* error = dumper.error_as_C_string();
    if (error == NULL) {
      output()->print_cr("Dump failed - reason unknown");
    } else {
      output()->print_cr("%s", error);
    }
  }
}

int HeapDumpDCmd::num_arguments() {
  ResourceMark rm;
  HeapDumpDCmd* dcmd = new HeapDumpDCmd(NULL, false);
  if (dcmd != NULL) {
    DCmdMark mark(dcmd);
    return dcmd->_dcmdparser.num_arguments();
  } else {
    return 0;
  }
}

ClassHistogramDCmd::ClassHistogramDCmd(outputStream* output, bool heap) :
                                       DCmdWithParser(output, heap),
  _all("-all", "Inspect all objects, including unreachable objects",
       "BOOLEAN", false, "false") {
  _dcmdparser.add_dcmd_option(&_all);
}

void ClassHistogramDCmd::execute(DCmdSource source, TRAPS) {
  VM_GC_HeapInspection heapop(output(),
                              !_all.value() /* request full gc if false */);
  VMThread::execute(&heapop);
}

int ClassHistogramDCmd::num_arguments() {
  ResourceMark rm;
  ClassHistogramDCmd* dcmd = new ClassHistogramDCmd(NULL, false);
  if (dcmd != NULL) {
    DCmdMark mark(dcmd);
    return dcmd->_dcmdparser.num_arguments();
  } else {
    return 0;
  }
}

#define DEFAULT_COLUMNS "InstBytes,KlassBytes,CpAll,annotations,MethodCount,Bytecodes,MethodAll,ROAll,RWAll,Total"
ClassStatsDCmd::ClassStatsDCmd(outputStream* output, bool heap) :
                                       DCmdWithParser(output, heap),
  _csv("-csv", "Print in CSV (comma-separated values) format for spreadsheets",
       "BOOLEAN", false, "false"),
  _all("-all", "Show all columns",
       "BOOLEAN", false, "false"),
  _help("-help", "Show meaning of all the columns",
       "BOOLEAN", false, "false"),
  _columns("columns", "Comma-separated list of all the columns to show. "
           "If not specified, the following columns are shown: " DEFAULT_COLUMNS,
           "STRING", false) {
  _dcmdparser.add_dcmd_option(&_all);
  _dcmdparser.add_dcmd_option(&_csv);
  _dcmdparser.add_dcmd_option(&_help);
  _dcmdparser.add_dcmd_argument(&_columns);
}

void ClassStatsDCmd::execute(DCmdSource source, TRAPS) {
  VM_GC_HeapInspection heapop(output(),
                              true /* request_full_gc */);
  heapop.set_csv_format(_csv.value());
  heapop.set_print_help(_help.value());
  heapop.set_print_class_stats(true);
  if (_all.value()) {
    if (_columns.has_value()) {
      output()->print_cr("Cannot specify -all and individual columns at the same time");
      return;
    } else {
      heapop.set_columns(NULL);
    }
  } else {
    if (_columns.has_value()) {
      heapop.set_columns(_columns.value());
    } else {
      heapop.set_columns(DEFAULT_COLUMNS);
    }
  }
  VMThread::execute(&heapop);
}

int ClassStatsDCmd::num_arguments() {
  ResourceMark rm;
  ClassStatsDCmd* dcmd = new ClassStatsDCmd(NULL, false);
  if (dcmd != NULL) {
    DCmdMark mark(dcmd);
    return dcmd->_dcmdparser.num_arguments();
  } else {
    return 0;
  }
}
#endif // INCLUDE_SERVICES

ThreadDumpDCmd::ThreadDumpDCmd(outputStream* output, bool heap) :
                               DCmdWithParser(output, heap),
  _locks("-l", "print java.util.concurrent locks", "BOOLEAN", false, "false"),
  _extended("-e", "print extended thread information", "BOOLEAN", false, "false") {
  _dcmdparser.add_dcmd_option(&_locks);
  _dcmdparser.add_dcmd_option(&_extended);
}

void ThreadDumpDCmd::execute(DCmdSource source, TRAPS) {
  // thread stacks
  VM_PrintThreads op1(output(), _locks.value(), _extended.value());
  VMThread::execute(&op1);

  // JNI global handles
  VM_PrintJNI op2(output());
  VMThread::execute(&op2);

  // Deadlock detection
  VM_FindDeadlocks op3(output());
  VMThread::execute(&op3);
}

int ThreadDumpDCmd::num_arguments() {
  ResourceMark rm;
  ThreadDumpDCmd* dcmd = new ThreadDumpDCmd(NULL, false);
  if (dcmd != NULL) {
    DCmdMark mark(dcmd);
    return dcmd->_dcmdparser.num_arguments();
  } else {
    return 0;
  }
}

// Enhanced JMX Agent support

JMXStartRemoteDCmd::JMXStartRemoteDCmd(outputStream *output, bool heap_allocated) :

  DCmdWithParser(output, heap_allocated),

  _config_file
  ("config.file",
   "set com.sun.management.config.file", "STRING", false),

  _jmxremote_host
  ("jmxremote.host",
   "set com.sun.management.jmxremote.host", "STRING", false),

  _jmxremote_port
  ("jmxremote.port",
   "set com.sun.management.jmxremote.port", "STRING", false),

  _jmxremote_rmi_port
  ("jmxremote.rmi.port",
   "set com.sun.management.jmxremote.rmi.port", "STRING", false),

  _jmxremote_ssl
  ("jmxremote.ssl",
   "set com.sun.management.jmxremote.ssl", "STRING", false),

  _jmxremote_registry_ssl
  ("jmxremote.registry.ssl",
   "set com.sun.management.jmxremote.registry.ssl", "STRING", false),

  _jmxremote_authenticate
  ("jmxremote.authenticate",
   "set com.sun.management.jmxremote.authenticate", "STRING", false),

  _jmxremote_password_file
  ("jmxremote.password.file",
   "set com.sun.management.jmxremote.password.file", "STRING", false),

  _jmxremote_access_file
  ("jmxremote.access.file",
   "set com.sun.management.jmxremote.access.file", "STRING", false),

  _jmxremote_login_config
  ("jmxremote.login.config",
   "set com.sun.management.jmxremote.login.config", "STRING", false),

  _jmxremote_ssl_enabled_cipher_suites
  ("jmxremote.ssl.enabled.cipher.suites",
   "set com.sun.management.jmxremote.ssl.enabled.cipher.suite", "STRING", false),

  _jmxremote_ssl_enabled_protocols
  ("jmxremote.ssl.enabled.protocols",
   "set com.sun.management.jmxremote.ssl.enabled.protocols", "STRING", false),

  _jmxremote_ssl_need_client_auth
  ("jmxremote.ssl.need.client.auth",
   "set com.sun.management.jmxremote.need.client.auth", "STRING", false),

  _jmxremote_ssl_config_file
  ("jmxremote.ssl.config.file",
   "set com.sun.management.jmxremote.ssl_config_file", "STRING", false),

// JDP Protocol support
  _jmxremote_autodiscovery
  ("jmxremote.autodiscovery",
   "set com.sun.management.jmxremote.autodiscovery", "STRING", false),

   _jdp_port
  ("jdp.port",
   "set com.sun.management.jdp.port", "INT", false),

   _jdp_address
  ("jdp.address",
   "set com.sun.management.jdp.address", "STRING", false),

   _jdp_source_addr
  ("jdp.source_addr",
   "set com.sun.management.jdp.source_addr", "STRING", false),

   _jdp_ttl
  ("jdp.ttl",
   "set com.sun.management.jdp.ttl", "INT", false),

   _jdp_pause
  ("jdp.pause",
   "set com.sun.management.jdp.pause", "INT", false),

   _jdp_name
  ("jdp.name",
   "set com.sun.management.jdp.name", "STRING", false)

  {
    _dcmdparser.add_dcmd_option(&_config_file);
    _dcmdparser.add_dcmd_option(&_jmxremote_host);
    _dcmdparser.add_dcmd_option(&_jmxremote_port);
    _dcmdparser.add_dcmd_option(&_jmxremote_rmi_port);
    _dcmdparser.add_dcmd_option(&_jmxremote_ssl);
    _dcmdparser.add_dcmd_option(&_jmxremote_registry_ssl);
    _dcmdparser.add_dcmd_option(&_jmxremote_authenticate);
    _dcmdparser.add_dcmd_option(&_jmxremote_password_file);
    _dcmdparser.add_dcmd_option(&_jmxremote_access_file);
    _dcmdparser.add_dcmd_option(&_jmxremote_login_config);
    _dcmdparser.add_dcmd_option(&_jmxremote_ssl_enabled_cipher_suites);
    _dcmdparser.add_dcmd_option(&_jmxremote_ssl_enabled_protocols);
    _dcmdparser.add_dcmd_option(&_jmxremote_ssl_need_client_auth);
    _dcmdparser.add_dcmd_option(&_jmxremote_ssl_config_file);
    _dcmdparser.add_dcmd_option(&_jmxremote_autodiscovery);
    _dcmdparser.add_dcmd_option(&_jdp_port);
    _dcmdparser.add_dcmd_option(&_jdp_address);
    _dcmdparser.add_dcmd_option(&_jdp_source_addr);
    _dcmdparser.add_dcmd_option(&_jdp_ttl);
    _dcmdparser.add_dcmd_option(&_jdp_pause);
    _dcmdparser.add_dcmd_option(&_jdp_name);
}


int JMXStartRemoteDCmd::num_arguments() {
  ResourceMark rm;
  JMXStartRemoteDCmd* dcmd = new JMXStartRemoteDCmd(NULL, false);
  if (dcmd != NULL) {
    DCmdMark mark(dcmd);
    return dcmd->_dcmdparser.num_arguments();
  } else {
    return 0;
  }
}


void JMXStartRemoteDCmd::execute(DCmdSource source, TRAPS) {
    ResourceMark rm(THREAD);
    HandleMark hm(THREAD);

    // Load and initialize the jdk.internal.agent.Agent class
    // invoke startRemoteManagementAgent(string) method to start
    // the remote management server.
    // throw java.lang.NoSuchMethodError if the method doesn't exist

    loadAgentModule(CHECK);
    Handle loader = Handle(THREAD, SystemDictionary::java_system_loader());
    Klass* k = SystemDictionary::resolve_or_fail(vmSymbols::jdk_internal_agent_Agent(), loader, Handle(), true, CHECK);

    JavaValue result(T_VOID);

    // Pass all command line arguments to java as key=value,...
    // All checks are done on java side

    int len = 0;
    stringStream options;
    char comma[2] = {0,0};

    // Leave default values on Agent.class side and pass only
    // agruments explicitly set by user. All arguments passed
    // to jcmd override properties with the same name set by
    // command line with -D or by managmenent.properties
    // file.
#define PUT_OPTION(a) \
    do { \
        if ( (a).is_set() ){ \
            if ( *((a).type()) == 'I' ) { \
                options.print("%scom.sun.management.%s=" JLONG_FORMAT, comma, (a).name(), (jlong)((a).value())); \
            } else { \
                options.print("%scom.sun.management.%s=%s", comma, (a).name(), (char*)((a).value())); \
            } \
            comma[0] = ','; \
        }\
    } while(0);


    PUT_OPTION(_config_file);
    PUT_OPTION(_jmxremote_host);
    PUT_OPTION(_jmxremote_port);
    PUT_OPTION(_jmxremote_rmi_port);
    PUT_OPTION(_jmxremote_ssl);
    PUT_OPTION(_jmxremote_registry_ssl);
    PUT_OPTION(_jmxremote_authenticate);
    PUT_OPTION(_jmxremote_password_file);
    PUT_OPTION(_jmxremote_access_file);
    PUT_OPTION(_jmxremote_login_config);
    PUT_OPTION(_jmxremote_ssl_enabled_cipher_suites);
    PUT_OPTION(_jmxremote_ssl_enabled_protocols);
    PUT_OPTION(_jmxremote_ssl_need_client_auth);
    PUT_OPTION(_jmxremote_ssl_config_file);
    PUT_OPTION(_jmxremote_autodiscovery);
    PUT_OPTION(_jdp_port);
    PUT_OPTION(_jdp_address);
    PUT_OPTION(_jdp_source_addr);
    PUT_OPTION(_jdp_ttl);
    PUT_OPTION(_jdp_pause);
    PUT_OPTION(_jdp_name);

#undef PUT_OPTION

    Handle str = java_lang_String::create_from_str(options.as_string(), CHECK);
    JavaCalls::call_static(&result, k, vmSymbols::startRemoteAgent_name(), vmSymbols::string_void_signature(), str, CHECK);
}

JMXStartLocalDCmd::JMXStartLocalDCmd(outputStream *output, bool heap_allocated) :
  DCmd(output, heap_allocated) {
  // do nothing
}

void JMXStartLocalDCmd::execute(DCmdSource source, TRAPS) {
    ResourceMark rm(THREAD);
    HandleMark hm(THREAD);

    // Load and initialize the jdk.internal.agent.Agent class
    // invoke startLocalManagementAgent(void) method to start
    // the local management server
    // throw java.lang.NoSuchMethodError if method doesn't exist

    loadAgentModule(CHECK);
    Handle loader = Handle(THREAD, SystemDictionary::java_system_loader());
    Klass* k = SystemDictionary::resolve_or_fail(vmSymbols::jdk_internal_agent_Agent(), loader, Handle(), true, CHECK);

    JavaValue result(T_VOID);
    JavaCalls::call_static(&result, k, vmSymbols::startLocalAgent_name(), vmSymbols::void_method_signature(), CHECK);
}

void JMXStopRemoteDCmd::execute(DCmdSource source, TRAPS) {
    ResourceMark rm(THREAD);
    HandleMark hm(THREAD);

    // Load and initialize the jdk.internal.agent.Agent class
    // invoke stopRemoteManagementAgent method to stop the
    // management server
    // throw java.lang.NoSuchMethodError if method doesn't exist

    loadAgentModule(CHECK);
    Handle loader = Handle(THREAD, SystemDictionary::java_system_loader());
    Klass* k = SystemDictionary::resolve_or_fail(vmSymbols::jdk_internal_agent_Agent(), loader, Handle(), true, CHECK);

    JavaValue result(T_VOID);
    JavaCalls::call_static(&result, k, vmSymbols::stopRemoteAgent_name(), vmSymbols::void_method_signature(), CHECK);
}

JMXStatusDCmd::JMXStatusDCmd(outputStream *output, bool heap_allocated) :
  DCmd(output, heap_allocated) {
  // do nothing
}

void JMXStatusDCmd::execute(DCmdSource source, TRAPS) {
  ResourceMark rm(THREAD);
  HandleMark hm(THREAD);
  // Load and initialize the jdk.internal.agent.Agent class
  // invoke getManagementAgentStatus() method to generate the status info
  // throw java.lang.NoSuchMethodError if method doesn't exist

  loadAgentModule(CHECK);
  Handle loader = Handle(THREAD, SystemDictionary::java_system_loader());
  Klass* k = SystemDictionary::resolve_or_fail(vmSymbols::jdk_internal_agent_Agent(), loader, Handle(), true, CHECK);

  JavaValue result(T_OBJECT);
  JavaCalls::call_static(&result, k, vmSymbols::getAgentStatus_name(), vmSymbols::void_string_signature(), CHECK);

  jvalue* jv = (jvalue*)result.get_value_addr();
  oop str = cast_to_oop(jv->l);
  if (str != NULL) {
      char* out = java_lang_String::as_utf8_string(str);
      if (out) {
          // Avoid using print_cr() because length maybe longer than O_BUFLEN
          output()->print_raw_cr(out);
          return;
      }
  }
  output()->print_cr("Error obtaining management agent status");
}

VMDynamicLibrariesDCmd::VMDynamicLibrariesDCmd(outputStream *output, bool heap_allocated) :
  DCmd(output, heap_allocated) {
  // do nothing
}

void VMDynamicLibrariesDCmd::execute(DCmdSource source, TRAPS) {
  os::print_dll_info(output());
  output()->cr();
}

void CompileQueueDCmd::execute(DCmdSource source, TRAPS) {
  VM_PrintCompileQueue printCompileQueueOp(output());
  VMThread::execute(&printCompileQueueOp);
}

void CodeListDCmd::execute(DCmdSource source, TRAPS) {
  CodeCache::print_codelist(output());
}

void CodeCacheDCmd::execute(DCmdSource source, TRAPS) {
  CodeCache::print_layout(output());
}

#ifdef LINUX
void PerfMapDCmd::execute(DCmdSource source, TRAPS) {
  CodeCache::write_perf_map();
}
#endif // LINUX

//---<  BEGIN  >--- CodeHeap State Analytics.
CodeHeapAnalyticsDCmd::CodeHeapAnalyticsDCmd(outputStream* output, bool heap) :
                                             DCmdWithParser(output, heap),
  _function("function", "Function to be performed (aggregate, UsedSpace, FreeSpace, MethodCount, MethodSpace, MethodAge, MethodNames, discard", "STRING", false, "all"),
  _granularity("granularity", "Detail level - smaller value -> more detail", "INT", false, "4096") {
  _dcmdparser.add_dcmd_argument(&_function);
  _dcmdparser.add_dcmd_argument(&_granularity);
}

void CodeHeapAnalyticsDCmd::execute(DCmdSource source, TRAPS) {
  jlong granularity = _granularity.value();
  if (granularity < 1) {
    Exceptions::fthrow(THREAD_AND_LOCATION, vmSymbols::java_lang_IllegalArgumentException(),
                       "Invalid granularity value " JLONG_FORMAT  ". Should be positive.\n", granularity);
    return;
  }

  CompileBroker::print_heapinfo(output(), _function.value(), granularity);
}

int CodeHeapAnalyticsDCmd::num_arguments() {
  ResourceMark rm;
  CodeHeapAnalyticsDCmd* dcmd = new CodeHeapAnalyticsDCmd(NULL, false);
  if (dcmd != NULL) {
    DCmdMark mark(dcmd);
    return dcmd->_dcmdparser.num_arguments();
  } else {
    return 0;
  }
}
//---<  END  >--- CodeHeap State Analytics.

void CompilerDirectivesPrintDCmd::execute(DCmdSource source, TRAPS) {
  DirectivesStack::print(output());
}

CompilerDirectivesAddDCmd::CompilerDirectivesAddDCmd(outputStream* output, bool heap) :
                           DCmdWithParser(output, heap),
  _filename("filename","Name of the directives file", "STRING",true) {
  _dcmdparser.add_dcmd_argument(&_filename);
}

void CompilerDirectivesAddDCmd::execute(DCmdSource source, TRAPS) {
  DirectivesParser::parse_from_file(_filename.value(), output());
}

int CompilerDirectivesAddDCmd::num_arguments() {
  ResourceMark rm;
  CompilerDirectivesAddDCmd* dcmd = new CompilerDirectivesAddDCmd(NULL, false);
  if (dcmd != NULL) {
    DCmdMark mark(dcmd);
    return dcmd->_dcmdparser.num_arguments();
  } else {
    return 0;
  }
}

void CompilerDirectivesRemoveDCmd::execute(DCmdSource source, TRAPS) {
  DirectivesStack::pop(1);
}

void CompilerDirectivesClearDCmd::execute(DCmdSource source, TRAPS) {
  DirectivesStack::clear();
}
#if INCLUDE_SERVICES
ClassHierarchyDCmd::ClassHierarchyDCmd(outputStream* output, bool heap) :
                                       DCmdWithParser(output, heap),
  _print_interfaces("-i", "Inherited interfaces should be printed.", "BOOLEAN", false, "false"),
  _print_subclasses("-s", "If a classname is specified, print its subclasses. "
                    "Otherwise only its superclasses are printed.", "BOOLEAN", false, "false"),
  _classname("classname", "Name of class whose hierarchy should be printed. "
             "If not specified, all class hierarchies are printed.",
             "STRING", false) {
  _dcmdparser.add_dcmd_option(&_print_interfaces);
  _dcmdparser.add_dcmd_option(&_print_subclasses);
  _dcmdparser.add_dcmd_argument(&_classname);
}

void ClassHierarchyDCmd::execute(DCmdSource source, TRAPS) {
  VM_PrintClassHierarchy printClassHierarchyOp(output(), _print_interfaces.value(),
                                               _print_subclasses.value(), _classname.value());
  VMThread::execute(&printClassHierarchyOp);
}

int ClassHierarchyDCmd::num_arguments() {
  ResourceMark rm;
  ClassHierarchyDCmd* dcmd = new ClassHierarchyDCmd(NULL, false);
  if (dcmd != NULL) {
    DCmdMark mark(dcmd);
    return dcmd->_dcmdparser.num_arguments();
  } else {
    return 0;
  }
}

#endif

class VM_DumpTouchedMethods : public VM_Operation {
private:
  outputStream* _out;
public:
  VM_DumpTouchedMethods(outputStream* out) {
    _out = out;
  }

  virtual VMOp_Type type() const { return VMOp_DumpTouchedMethods; }

  virtual void doit() {
    Method::print_touched_methods(_out);
  }
};

TouchedMethodsDCmd::TouchedMethodsDCmd(outputStream* output, bool heap) :
                                       DCmdWithParser(output, heap)
{}

void TouchedMethodsDCmd::execute(DCmdSource source, TRAPS) {
  if (!LogTouchedMethods) {
    output()->print_cr("VM.print_touched_methods command requires -XX:+LogTouchedMethods");
    return;
  }
  VM_DumpTouchedMethods dumper(output());
  VMThread::execute(&dumper);
}

int TouchedMethodsDCmd::num_arguments() {
  return 0;
}

#if INCLUDE_JVMTI
extern "C" typedef char const* (JNICALL *debugInit_startDebuggingViaCommandPtr)(JNIEnv* env, jthread thread, char const** transport_name,
                                                                                char const** address, jboolean* first_start);
static debugInit_startDebuggingViaCommandPtr dvc_start_ptr = NULL;

DebugOnCmdStartDCmd::DebugOnCmdStartDCmd(outputStream* output, bool heap) : DCmdWithParser(output, heap) {
}

void DebugOnCmdStartDCmd::execute(DCmdSource source, TRAPS) {
  char const* transport = NULL;
  char const* addr = NULL;
  jboolean is_first_start = JNI_FALSE;
  JavaThread* thread = (JavaThread*) THREAD;
  jthread jt = JNIHandles::make_local(thread->threadObj());
  ThreadToNativeFromVM ttn(thread);
  const char *error = "Could not find jdwp agent.";

  if (!dvc_start_ptr) {
    for (AgentLibrary* agent = Arguments::agents(); agent != NULL; agent = agent->next()) {
      if ((strcmp("jdwp", agent->name()) == 0) && (dvc_start_ptr == NULL)) {
        char const* func = "debugInit_startDebuggingViaCommand";
        dvc_start_ptr = (debugInit_startDebuggingViaCommandPtr) os::find_agent_function(agent, false, &func, 1);
      }
    }
  }

  if (dvc_start_ptr) {
    error = dvc_start_ptr(thread->jni_environment(), jt, &transport, &addr, &is_first_start);
  }

  if (error != NULL) {
    output()->print_cr("Debugging has not been started: %s", error);
  } else {
    output()->print_cr(is_first_start ? "Debugging has been started." : "Debugging is already active.");
    output()->print_cr("Transport : %s", transport ? transport : "#unknown");
    output()->print_cr("Address : %s", addr ? addr : "#unknown");
  }
}
#endif // INCLUDE_JVMTI

void QuickStartDumpDCMD::execute(DCmdSource source, TRAPS) {
  Ticks start_time = Ticks::now();
  if (QuickStart::is_tracer() || QuickStart::is_profiler()) {
    Klass* klass = SystemDictionary::com_alibaba_util_QuickStart_klass();
    JavaValue result(T_VOID);
    JavaCallArguments args(0);

    JavaCalls::call_static(&result, klass, vmSymbols::notifyDump_name(),
                           vmSymbols::void_method_signature(), &args, CHECK);
  }
  Ticks end_time = Ticks::now();

  Tickspan duration = end_time - start_time;
  long ms = TimeHelper::counter_to_millis(duration.value());
  output()->print_cr("It took %lu ms to execute Quickstart.dump .", ms);
}

#ifdef LINUX
void CheckpointDCmd::execute(DCmdSource source, TRAPS) {
  Klass* k = SystemDictionary::resolve_or_fail(vmSymbols::jdk_crac_Core(),
                                                 true, CHECK);
  JavaValue result(T_OBJECT);
  JavaCallArguments args;
  args.push_long((jlong)output());
  LinuxAttachOperation* current_op;
  JavaCalls::call_static(&result, k,
                         vmSymbols::checkpointRestoreInternal_name(),
                         vmSymbols::checkpointRestoreInternal_signature(), &args, CHECK);
  oop str = (oop)result.get_jobject();
  if (str != NULL) {
    char* out = java_lang_String::as_utf8_string(str);
    if (out[0] != '\0') {
      current_op = LinuxAttachListener::get_current_op();
      outputStream* stream = current_op->is_effectively_completed() ? tty : output();
      stream->print_cr("An exception during a checkpoint operation:");
      stream->print("%s", out);
    }
  }
}
#endif