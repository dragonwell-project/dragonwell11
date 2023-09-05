#! /bin/sh
minPid=100
while [ $(cat /proc/sys/kernel/ns_last_pid) -le ${minPid} ]; do cat /dev/null; done
"$@"