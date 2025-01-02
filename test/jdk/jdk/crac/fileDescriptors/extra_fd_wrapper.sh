#!/bin/bash
# Java opens all files with O_CLOEXEC (or calls fcntl(FD_CLOEXEC)) so we cannot trigger this behaviour from Java code;
# this opens a file descriptor and executes subprocess based on its arguments.
FILE=$(mktemp -p /dev/shm)
exec 42<>$FILE
# criu uses DEFAULT_GHOST_LIMIT 1M - let's create a file bigger than that
dd if=/dev/urandom bs=4096 count=257 >&42 2>/dev/null
rm $FILE
# Open some extra files
while [ $1 = "-o" ]; do
  eval "exec $2<>$3"
  shift 3
done
exec "$@"
