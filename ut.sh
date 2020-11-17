#!/bin/bash

ARGNUM=$#
if [ $ARGNUM != 1 ]; then
  echo "USAGE: $0 release/debug/fastdebug"
  exit
fi

DOCKER_IMAGE=registry.cn-hangzhou.aliyuncs.com/dragonwell/dragonwell11-build-musl:v1
SCRIPT_NAME=ut-entrypoint.sh

BUILD_MODE=$1
JTREG_DOWNLOAD="https://ci.adoptopenjdk.net/view/Dependencies/job/jtreg/lastSuccessfulBuild/artifact/jtreg-4.2.0-tip.tar.gz"
JTREG_PATH="/usr/jvm/jtreg"
TEST_PATH="/usr/jvm/test"
JDK_PATH="/usr/lib/jvm/jdk/jre"
SCRIPT_NAME=ut-entrypoint.sh

case "$BUILD_MODE" in
    release)
        echo "build mode is release"
    ;;
    debug)
        echo "build mode is debug"
    ;;
    fastdebug)
        echo "build mode is fastdebug"
    ;;
    *)
        echo "Argument must be release, debug or fastdebug!"
        exit 1
    ;;
esac

docker run -i --rm -e BUILD_MODE=$BUILD_MODE -e JTREG_DOWNLOAD=$JTREG_DOWNLOAD -e JTREG_PATH=$JTREG_PATH -e TEST_PATH=$TEST_PATH -e JDK_PATH=$JDK_PATH -v `pwd`:`pwd` -w `pwd` --entrypoint=bash $DOCKER_IMAGE `pwd`/$SCRIPT_NAME
