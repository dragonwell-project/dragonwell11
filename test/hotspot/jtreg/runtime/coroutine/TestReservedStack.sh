#!/bin/sh

## @test
##
## @requires vm.opt.DeoptimizeALot == null | vm.opt.DeoptimizeALot == false
## @library /test/lib
## @summary test \@ReservedStackAccess
## @run shell TestReservedStack.sh

set -x

NAME=ReservedStackTest
PATH=${TESTSRC}/../ReservedStack/${NAME}.java
MODULES="--add-modules java.base --add-exports java.base/jdk.internal.misc=ALL-UNNAMED --add-exports java.base/jdk.internal.vm.annotation=ALL-UNNAMED"

checkexit () {
    if [ "$?" -ne "0" ] ; then
      exit 1;
    fi
}

if [[ ! -f ${PATH} ]]; then
  echo "file ${PATH} doesn't exit!"
  exit 1;
fi

/usr/bin/cp ${PATH} .
${COMPILEJAVA}/bin/javac -cp ${TESTSRCPATH} ${MODULES} ${NAME}.java

${COMPILEJAVA}/bin/java -Dtest.jdk=${COMPILEJAVA} -cp ${TESTSRCPATH}:. ${MODULES} -XX:MaxInlineLevel=2 -XX:CompileCommand=exclude,java/util/concurrent/locks/AbstractOwnableSynchronizer.setExclusiveOwnerThread -XX:+EnableCoroutine -XX:+UseWispMonitor -Dcom.alibaba.wisp.transparentWispSwitch=true ${NAME}
checkexit
${COMPILEJAVA}/bin/java -Dtest.jdk=${COMPILEJAVA} -cp ${TESTSRCPATH}:. ${MODULES} -XX:MaxInlineLevel=2 -XX:CompileCommand=exclude,java/util/concurrent/locks/AbstractOwnableSynchronizer.setExclusiveOwnerThread -XX:+EnableCoroutine -XX:+UseWispMonitor -Dcom.alibaba.wisp.transparentWispSwitch=true -Dcom.alibaba.wisp.version=2 ${NAME}
checkexit
${COMPILEJAVA}/bin/java -Dtest.jdk=${COMPILEJAVA} -cp ${TESTSRCPATH}:. ${MODULES} -XX:+TieredCompilation -XX:TieredStopAtLevel=1 -XX:-Inline -XX:CompileCommand=exclude,java/util/concurrent/locks/AbstractOwnableSynchronizer.setExclusiveOwnerThread -XX:+EnableCoroutine -XX:+UseWispMonitor -Dcom.alibaba.wisp.transparentWispSwitch=true ${NAME}
checkexit
${COMPILEJAVA}/bin/java -Dtest.jdk=${COMPILEJAVA} -cp ${TESTSRCPATH}:. ${MODULES} -XX:+TieredCompilation -XX:TieredStopAtLevel=1 -XX:-Inline -XX:CompileCommand=exclude,java/util/concurrent/locks/AbstractOwnableSynchronizer.setExclusiveOwnerThread -XX:+EnableCoroutine -XX:+UseWispMonitor -Dcom.alibaba.wisp.transparentWispSwitch=true -Dcom.alibaba.wisp.version=2 ${NAME}
checkexit
