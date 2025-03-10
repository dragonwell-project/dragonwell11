#
# Copyright (c) 2011, 2022, Oracle and/or its affiliates. All rights reserved.
# DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
#
# This code is free software; you can redistribute it and/or modify it
# under the terms of the GNU General Public License version 2 only, as
# published by the Free Software Foundation.  Oracle designates this
# particular file as subject to the "Classpath" exception as provided
# by Oracle in the LICENSE file that accompanied this code.
#
# This code is distributed in the hope that it will be useful, but WITHOUT
# ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
# FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
# version 2 for more details (a copy is included in the LICENSE file that
# accompanied this code).
#
# You should have received a copy of the GNU General Public License version
# 2 along with this work; if not, write to the Free Software Foundation,
# Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
#
# Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
# or visit www.oracle.com if you need additional information or have any
# questions.
#

# Major library component reside in separate files.
m4_include([lib-alsa.m4])
m4_include([lib-bundled.m4])
m4_include([lib-cups.m4])
m4_include([lib-ffi.m4])
m4_include([lib-freetype.m4])
m4_include([lib-std.m4])
m4_include([lib-x11.m4])
m4_include([lib-fontconfig.m4])
m4_include([lib-tests.m4])

################################################################################
# Determine which libraries are needed for this configuration
################################################################################
AC_DEFUN_ONCE([LIB_DETERMINE_DEPENDENCIES],
[
  # Check if X11 is needed
  if test "x$OPENJDK_TARGET_OS" = xwindows || test "x$OPENJDK_TARGET_OS" = xmacosx; then
    # No X11 support on windows or macosx
    NEEDS_LIB_X11=false
  else
    # All other instances need X11, even if building headless only, libawt still
    # needs X11 headers.
    NEEDS_LIB_X11=true
  fi

  # Check if fontconfig is needed
  if test "x$OPENJDK_TARGET_OS" = xwindows || test "x$OPENJDK_TARGET_OS" = xmacosx; then
    # No fontconfig support on windows or macosx
    NEEDS_LIB_FONTCONFIG=false
  else
    # All other instances need fontconfig, even if building headless only,
    # libawt still needs fontconfig headers.
    NEEDS_LIB_FONTCONFIG=true
  fi

  # Check if cups is needed
  if test "x$OPENJDK_TARGET_OS" = xwindows; then
    # Windows have a separate print system
    NEEDS_LIB_CUPS=false
  else
    NEEDS_LIB_CUPS=true
  fi

  # A custom hook may have set this already
  if test "x$NEEDS_LIB_FREETYPE" = "x"; then
    NEEDS_LIB_FREETYPE=true
  fi

  # Check if alsa is needed
  if test "x$OPENJDK_TARGET_OS" = xlinux; then
    NEEDS_LIB_ALSA=true
  else
    NEEDS_LIB_ALSA=false
  fi

  # Check if ffi is needed
  if HOTSPOT_CHECK_JVM_VARIANT(zero); then
    NEEDS_LIB_FFI=true
  else
    NEEDS_LIB_FFI=false
  fi
])

################################################################################
# Parse library options, and setup needed libraries
################################################################################
AC_DEFUN_ONCE([LIB_SETUP_LIBRARIES],
[
  LIB_SETUP_STD_LIBS
  LIB_SETUP_X11
  LIB_SETUP_CUPS
  LIB_SETUP_FONTCONFIG
  LIB_SETUP_FREETYPE
  LIB_SETUP_ALSA
  LIB_SETUP_LIBFFI
  LIB_SETUP_BUNDLED_LIBS
  LIB_SETUP_MISC_LIBS
  LIB_SETUP_SOLARIS_STLPORT
  LIB_TESTS_SETUP_GRAALUNIT

  if test "x$TOOLCHAIN_TYPE" = xsolstudio; then
    GLOBAL_LIBS="-lc"
  else
    GLOBAL_LIBS=""
  fi

  BASIC_JDKLIB_LIBS=""
  if test "x$TOOLCHAIN_TYPE" != xmicrosoft; then
    BASIC_JDKLIB_LIBS="-ljava -ljvm"
  fi

  # Math library
  BASIC_JVM_LIBS="$LIBM"

  # Dynamic loading library
  if test "x$OPENJDK_TARGET_OS" = xlinux || test "x$OPENJDK_TARGET_OS" = xsolaris || test "x$OPENJDK_TARGET_OS" = xaix; then
    BASIC_JVM_LIBS="$BASIC_JVM_LIBS $LIBDL"
  fi

  # Threading library
  if test "x$OPENJDK_TARGET_OS" = xlinux || test "x$OPENJDK_TARGET_OS" = xaix; then
    BASIC_JVM_LIBS="$BASIC_JVM_LIBS -lpthread"
  elif test "x$OPENJDK_TARGET_OS" = xsolaris; then
    BASIC_JVM_LIBS="$BASIC_JVM_LIBS -lthread"
  fi

  # librt for legacy clock_gettime
  if test "x$OPENJDK_TARGET_OS" = xlinux; then
    # Hotspot needs to link librt to get the clock_* functions.
    # But once our supported minimum build and runtime platform
    # has glibc 2.17, this can be removed as the functions are
    # in libc.
    BASIC_JVM_LIBS="$BASIC_JVM_LIBS -lrt"
  fi

  # Because RISC-V only has word-sized atomics, it requries libatomic where
  # other common architectures do not.  So link libatomic by default.
  if test "x$OPENJDK_TARGET_OS" = xlinux && test "x$OPENJDK_TARGET_CPU" = xriscv64; then
    BASIC_JVM_LIBS="$BASIC_JVM_LIBS -latomic"
  fi

  # perfstat lib
  if test "x$OPENJDK_TARGET_OS" = xaix; then
    BASIC_JVM_LIBS="$BASIC_JVM_LIBS -lperfstat"
  fi

  if test "x$OPENJDK_TARGET_OS" = xsolaris; then
    BASIC_JVM_LIBS="$BASIC_JVM_LIBS -lsocket -lsched -ldoor -ldemangle -lnsl \
        -lrt -lkstat"
    BASIC_JVM_LIBS="$BASIC_JVM_LIBS $LIBCXX_JVM"
  fi

  if test "x$OPENJDK_TARGET_OS" = xwindows; then
    BASIC_JVM_LIBS="$BASIC_JVM_LIBS kernel32.lib user32.lib gdi32.lib winspool.lib \
        comdlg32.lib advapi32.lib shell32.lib ole32.lib oleaut32.lib uuid.lib \
        ws2_32.lib winmm.lib version.lib psapi.lib"
  fi

  JDKLIB_LIBS="$BASIC_JDKLIB_LIBS"
  JDKEXE_LIBS=""
  JVM_LIBS="$BASIC_JVM_LIBS"
  OPENJDK_BUILD_JDKLIB_LIBS="$BASIC_JDKLIB_LIBS"
  OPENJDK_BUILD_JVM_LIBS="$BASIC_JVM_LIBS"

  AC_SUBST(JDKLIB_LIBS)
  AC_SUBST(JDKEXE_LIBS)
  AC_SUBST(JVM_LIBS)
  AC_SUBST(OPENJDK_BUILD_JDKLIB_LIBS)
  AC_SUBST(OPENJDK_BUILD_JVM_LIBS)
  AC_SUBST(GLOBAL_LIBS)
])

################################################################################
# Setup various libraries, typically small system libraries
################################################################################
AC_DEFUN_ONCE([LIB_SETUP_MISC_LIBS],
[
  # Setup libm (the maths library)
  if test "x$OPENJDK_TARGET_OS" != "xwindows"; then
    AC_CHECK_LIB(m, cos, [], [
        AC_MSG_NOTICE([Maths library was not found])
    ])
    LIBM="-lm"
  else
    LIBM=""
  fi
  AC_SUBST(LIBM)

  # Setup libdl (for dynamic library loading)
  save_LIBS="$LIBS"
  LIBS=""
  AC_CHECK_LIB(dl, dlopen)
  LIBDL="$LIBS"
  AC_SUBST(LIBDL)
  LIBS="$save_LIBS"

  # Deprecated libraries, keep the flags for backwards compatibility
  if test "x$OPENJDK_TARGET_OS" = "xwindows"; then
    UTIL_DEPRECATED_ARG_WITH([dxsdk])
    UTIL_DEPRECATED_ARG_WITH([dxsdk-lib])
    UTIL_DEPRECATED_ARG_WITH([dxsdk-include])
  fi

  # Control if libzip can use mmap. Available for purposes of overriding.
  LIBZIP_CAN_USE_MMAP=true
  AC_SUBST(LIBZIP_CAN_USE_MMAP)
])

################################################################################
# libstlport.so.1 is needed for running gtest on Solaris. Find it to
# redistribute it in the test image.
################################################################################
AC_DEFUN_ONCE([LIB_SETUP_SOLARIS_STLPORT],
[
  if test "$OPENJDK_TARGET_OS" = "solaris" && test "x$BUILD_GTEST" = "xtrue"; then
    # Find the root of the Solaris Studio installation from the compiler path
    SOLARIS_STUDIO_DIR="$(dirname $CC)/.."
    STLPORT_LIB="$SOLARIS_STUDIO_DIR/lib/stlport4$OPENJDK_TARGET_CPU_ISADIR/libstlport.so.1"
    AC_MSG_CHECKING([for libstlport.so.1])
    if ! test -f "$STLPORT_LIB" && test "x$OPENJDK_TARGET_CPU_ISADIR" = "x/sparcv9"; then
      # SS12u3 has libstlport under 'stlport4/v9' instead of 'stlport4/sparcv9'
      STLPORT_LIB="$SOLARIS_STUDIO_DIR/lib/stlport4/v9/libstlport.so.1"
    fi
    if test -f "$STLPORT_LIB"; then
      AC_MSG_RESULT([yes, $STLPORT_LIB])
      UTIL_FIXUP_PATH([STLPORT_LIB])
    else
      AC_MSG_RESULT([no, not found at $STLPORT_LIB])
      AC_MSG_ERROR([Failed to find libstlport.so.1, cannot build Hotspot gtests])
    fi
    AC_SUBST(STLPORT_LIB)
  fi
])

