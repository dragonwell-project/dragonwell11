#!/bin/sh

#
# @test
# @library /testlibrary
# @requires os.family == "linux"
# @compile TestSimpleWisp.java
#
# @summary test Coroutine SwitchTo() crash problem
# @run shell testCoroutineBreakpointSwitchTo.sh
#

OS=`uname -s`
case "$OS" in
  AIX | Darwin | Linux | SunOS )
    FS="/"
    ;;
  Windows_* )
    FS="\\"
    ;;
  CYGWIN_* )
    FS="/"
    ;;
  * )
    echo "Unrecognized system!"
    exit 1;
    ;;
esac

JAVA=${TESTJAVA}${FS}bin${FS}java

export LD_LIBRARY_PATH=.:${TESTJAVA}${FS}jre${FS}lib${FS}amd64${FS}server:${FS}usr${FS}lib:${LD_LIBRARY_PATH}

gcc_cmd=`which gcc`
if [ "x${gcc_cmd}" = "x" ]; then
    echo "WARNING: gcc not found. Cannot execute test." 2>&1
    exit 0
fi

gcc -DLINUX -fPIC -shared -o libtest.so \
    -I${TESTJAVA}/include -I${TESTJAVA}/include/linux \
    ${TESTSRC}/testCoroutineBreakpointSwitchTo.c

${JAVA} -agentpath:libtest.so -XX:-UseBiasedLocking -XX:+UnlockExperimentalVMOptions -XX:+EnableCoroutine -XX:+UseWispMonitor -Dcom.alibaba.transparentAsync=true -cp ${TESTCLASSES} TestSimpleWisp

exit $?
