#!/bin/sh

## @test
##
## @summary test jni MonitorExit
## @run shell testJniMonitorExit.sh
##


export LD_LIBRARY_PATH=.:${COMPILEJAVA}/lib/server:/usr/lib:$LD_LIBRARY_PATH
echo ${COMPILEJAVA}
echo $LD_LIBRARY_PATH
g++ -DLINUX -o testJniMonitorExit \
    -I${COMPILEJAVA}/include -I${COMPILEJAVA}/include/linux \
    -L${COMPILEJAVA}/lib/server \
    -ljvm -lpthread ${TESTSRC}/testJniMonitorExit.c

./testJniMonitorExit
exit $?
