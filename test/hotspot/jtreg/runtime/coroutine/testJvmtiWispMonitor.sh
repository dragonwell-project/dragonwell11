#!/bin/sh

## @test
##
## @requires os.family == "linux"
## @summary test jvmti and wispMonitor could work together
## @run shell testJvmtiWispMonitor.sh


${TESTJAVA}/bin/java -agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=localhost:5005 -XX:+UnlockExperimentalVMOptions -XX:+EnableCoroutine -XX:+UseWispMonitor &

PID=$!

sleep 2
ls -d /proc/$PID || exit 1 # process exited, fail

kill -KILL $PID

exit 0

