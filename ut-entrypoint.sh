#!/bin/bash

apk --no-cache add wget

WORK_SPACE=`pwd`
BUILD_MODE=${BUILD_MODE}
JTREG_DOWNLOAD=${JTREG_DOWNLOAD}
JTREG_PATH=${JTREG_PATH}
TEST_PATH=${TEST_PATH}
JDK_PATH=${JDK_PATH}
JDK_IMAGES_DIR=""

case "$BUILD_MODE" in
    release)
        JDK_IMAGES_DIR=build/linux-x86_64-normal-server-release/images/jdk
    ;;
    debug)
        JDK_IMAGES_DIR=build/linux-x86_64-normal-server-slowdebug/images/jdk
    ;;
    fastdebug)
        JDK_IMAGES_DIR=build/linux-x86_64-normal-server-fastdebug/images/jdk
    ;;
    *)    
        echo "Argument must be release or debug or fastdebug!"
        exit 1
    ;;
esac

mkdir -p $JTREG_PATH && cd $_
wget -c $JTREG_DOWNLOAD -O - | tar -xz --strip-components 1

mkdir -p $TEST_PATH && cd $_
cp -r $WORK_SPACE/test/. ./

mkdir -p $JDK_PATH && cd $_
cp -r $WORK_SPACE/$JDK_IMAGES_DIR/. ./

cd $TEST_PATH
echo 'start to jtreg ut......'
$JTREG_PATH/bin/jtreg -esa -v:time -jdk:$JDK_PATH $TEST_PATH/jdk/java/math

tar -zcvf test.tar.gz $TEST_PATH
