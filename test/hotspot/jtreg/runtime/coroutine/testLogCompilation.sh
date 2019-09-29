#!/bin/sh

#
# @test
# @library /testlibrary
# @compile TestSimpleWisp.java
#
# @summary test coroutine and -XX:+LogCompilation could work together
# @run shell testLogCompilation.sh
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

${JAVA} -XX:+UnlockDiagnosticVMOptions -XX:+EnableCoroutine -XX:+UseWispMonitor -Dcom.alibaba.transparentAsync=true -XX:+LogCompilation -Xcomp -cp ${TESTCLASSES} TestSimpleWisp

exit $?
