#!/bin/sh

## @test
##
## @summary test c1 assertion failure when UseDirectUnpark is enabled (please run it in slowdebug ver.)
## @requires os.family == "linux"
## @requires os.arch != "riscv64"
## @run shell testC1AssertFail.sh


${TESTJAVA}/bin/java -Xcomp -XX:-UseBiasedLocking -XX:+UnlockExperimentalVMOptions -XX:+EnableCoroutine -XX:+UseWispMonitor -Dcom.alibaba.transparentAsync=true &

PID=$!

sleep 2
ls -d /proc/$PID || exit 1 # process exited, fail

kill -KILL $PID

exit 0
