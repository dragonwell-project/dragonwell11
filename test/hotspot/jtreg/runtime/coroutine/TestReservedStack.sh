#!/bin/sh

## @test
##
## @requires vm.opt.DeoptimizeALot == null | vm.opt.DeoptimizeALot == false
## @requires os.family == "linux"
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
${TESTJAVA}/bin/javac -cp ${TESTSRCPATH} ${MODULES} ${NAME}.java

${TESTJAVA}/bin/java -Dtest.jdk=${TESTJAVA} -cp ${TESTSRCPATH}:. ${MODULES} -XX:MaxInlineLevel=2 -XX:CompileCommand=exclude,java/util/concurrent/locks/AbstractOwnableSynchronizer.setExclusiveOwnerThread -XX:+UnlockExperimentalVMOptions -XX:+EnableCoroutine -XX:+UseWispMonitor -Dcom.alibaba.wisp.transparentWispSwitch=true ${NAME}
checkexit
${TESTJAVA}/bin/java -Dtest.jdk=${TESTJAVA} -cp ${TESTSRCPATH}:. ${MODULES} -XX:MaxInlineLevel=2 -XX:CompileCommand=exclude,java/util/concurrent/locks/AbstractOwnableSynchronizer.setExclusiveOwnerThread -XX:+UnlockExperimentalVMOptions -XX:+EnableCoroutine -XX:+UseWispMonitor -Dcom.alibaba.wisp.transparentWispSwitch=true -Dcom.alibaba.wisp.version=2 ${NAME}
checkexit
${TESTJAVA}/bin/java -Dtest.jdk=${TESTJAVA} -cp ${TESTSRCPATH}:. ${MODULES} -XX:+TieredCompilation -XX:TieredStopAtLevel=1 -XX:-Inline -XX:CompileCommand=exclude,java/util/concurrent/locks/AbstractOwnableSynchronizer.setExclusiveOwnerThread -XX:+UnlockExperimentalVMOptions -XX:+EnableCoroutine -XX:+UseWispMonitor -Dcom.alibaba.wisp.transparentWispSwitch=true ${NAME}
checkexit
${TESTJAVA}/bin/java -Dtest.jdk=${TESTJAVA} -cp ${TESTSRCPATH}:. ${MODULES} -XX:+TieredCompilation -XX:TieredStopAtLevel=1 -XX:-Inline -XX:CompileCommand=exclude,java/util/concurrent/locks/AbstractOwnableSynchronizer.setExclusiveOwnerThread -XX:+UnlockExperimentalVMOptions -XX:+EnableCoroutine -XX:+UseWispMonitor -Dcom.alibaba.wisp.transparentWispSwitch=true -Dcom.alibaba.wisp.version=2 ${NAME}
checkexit
