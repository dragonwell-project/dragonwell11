#!/bin/sh

## @test
##
## @summary test DetachCurrentThread unpark
## @run shell testJniDetachThreadHoldingMonitor.sh
##


export LD_LIBRARY_PATH=.:${COMPILEJAVA}/lib/server:/usr/lib:$LD_LIBRARY_PATH

g++ -DLINUX -o testJniDetachThreadHoldingMonitor \
    -I${COMPILEJAVA}/include -I${COMPILEJAVA}/include/linux \
    -L${COMPILEJAVA}/lib/server \
    -ljvm -lpthread ${TESTSRC}/testJniDetachThreadHoldingMonitor.c

./testJniDetachThreadHoldingMonitor
exit $?
