#!/bin/bash

if [ $# != 1 ]; then
  echo "USAGE: $0 release/debug/fastdebug"
  exit
fi

# incr by every Dragonwell release
DRAGONWELL_VERSION=3
LC_ALL=C
BUILD_MODE=$1
DOCKER_IMAGE=registry.cn-hangzhou.aliyuncs.com/dragonwell/dragonwell11-build-musl:v1
SCRIPT_NAME=musl.sh

case "${BUILD_MODE}" in
    release)
        DEBUG_LEVEL="release"
        JDK_IMAGES_DIR=`pwd`/build/linux-x86_64-normal-server-release/images
    ;;
    debug)
        DEBUG_LEVEL="slowdebug"
        JDK_IMAGES_DIR=`pwd`/build/linux-x86_64-normal-server-slowdebug/images
    ;;
    fastdebug)
        DEBUG_LEVEL="fastdebug"
        JDK_IMAGES_DIR=`pwd`/build/linux-x86_64-normal-server-fastdebug/images
    ;;
    *)
        echo "Argument must be release or debug or fastdebug!"
        exit 1
    ;;
esac

NEW_JAVA_HOME=${JDK_IMAGES_DIR}/jdk

if [ "x${BUILD_NUMBER}" = "x" ]; then
  BUILD_NUMBER=0
fi

ps -e | grep docker
if [ $? -eq 0 ]; then
    echo "We will build DragonWell in Docker!"
    sudo docker pull $DOCKER_IMAGE
    sudo docker run -i --rm -e BUILD_NUMBER=$BUILD_NUMBER -e DEBUG_LEVEL=$DEBUG_LEVEL -e DRAGONWELL_VERSION=$DRAGONWELL_VERSION \
                -e BUILD_MODE=$BUILD_MODE -v `pwd`:`pwd` -w `pwd` --entrypoint=bash $DOCKER_IMAGE `pwd`/$SCRIPT_NAME
    exit $?
fi
