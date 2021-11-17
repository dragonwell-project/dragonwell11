#!/bin/sh

## @test
##
## @requires os.family == "linux"
## @summary test jni MonitorExit
## @run shell testJniMonitorExit.sh
##


export LD_LIBRARY_PATH=.:${TESTJAVA}/lib/server:/usr/lib:$LD_LIBRARY_PATH
echo ${TESTJAVA}
echo $LD_LIBRARY_PATH
g++ -DLINUX -o testJniMonitorExit \
    -I${TESTJAVA}/include -I${TESTJAVA}/include/linux \
    -L${TESTJAVA}/lib/server \
    -ljvm -lpthread ${TESTSRC}/testJniMonitorExit.c

./testJniMonitorExit
exit $?
