#!/bin/sh

## @test
##
## @requires os.family == "linux"
## @summary test DetachCurrentThread unpark
## @run shell testJniDetachThreadHoldingMonitor.sh
##


export LD_LIBRARY_PATH=.:${TESTJAVA}/lib/server:/usr/lib:$LD_LIBRARY_PATH

g++ -DLINUX -o testJniDetachThreadHoldingMonitor \
    -I${TESTJAVA}/include -I${TESTJAVA}/include/linux \
    -L${TESTJAVA}/lib/server \
    -ljvm -lpthread ${TESTSRC}/testJniDetachThreadHoldingMonitor.c

./testJniDetachThreadHoldingMonitor
exit $?
