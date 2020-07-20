#!/bin/bash

if [ $# != 1 ]; then
  echo "USAGE: $0 release/debug/fastdebug"
  exit
fi

# incr by every Dragonwell release
DRAGONWELL_VERSION=3
LC_ALL=C
BUILD_MODE=$1

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

bash ./configure --with-freetype=system \
                 --enable-unlimited-crypto \
                 --with-jvm-variants=server \
                 --with-debug-level=${DEBUG_LEVEL} \
                 --with-cacerts-file=`pwd`/make/data/security/cacerts \
                 --with-vendor-name="Alibaba" \
                 --with-vendor-url="http://www.alibabagroup.com" \
                 --with-vendor-bug-url="mailto:dragonwell_use@googlegroups.com" \
                 --with-vendor-version-string="(Alibaba Dragonwell)" \
                 --with-version-pre="" \
                 --with-version-opt="" \
                 --with-version-build="${BUILD_NUMBER}" \
                 --with-version-feature="11" \
                 --with-version-patch="${DRAGONWELL_VERSION}" \
                 --with-version-date="$(date +%Y-%m-%d)" \
                 --with-zlib=system \
                 --with-jvm-features=zgc

if [ 0 != $? ]; then
  echo 'Configure failed!'
  exit 1
fi

make CONF=${BUILD_MODE} LOG=cmdlines JOBS=8 images

if [ 0 != $? ]; then
  echo 'Build failed!'
  exit 1
fi

# Sanity tests
echo "================= Start sanity test ======================"
JAVA_EXES=("${NEW_JAVA_HOME}/bin/java")
VERSION_OPTS=("-version" "-Xinternalversion" "-fullversion")
for EXE in "${JAVA_EXES[@]}"; do
  for OPT in "${VERSION_OPTS[@]}"; do
    ${EXE} ${OPT} > /dev/null 2>&1
    if [ 0 -ne $? ]; then
      echo "Failed: ${EXE} ${OPT}"
      exit 128
    fi
  done
done

# Keep old output
${NEW_JAVA_HOME}/bin/java -version

cat > /tmp/systemProperty.java << EOF
public class systemProperty {
    public static void main(String[] args) {
        System.getProperties().list(System.out);
    }
}
EOF

${NEW_JAVA_HOME}/bin/javac /tmp/systemProperty.java
${NEW_JAVA_HOME}/bin/java -cp /tmp/ systemProperty > /tmp/systemProperty.out

EXPECTED_PATTERN=('^java\.vm\.vendor\=.*Alibaba.*$'
                '^java\.vendor\.url\=http\:\/\/www\.alibabagroup\.com$'
                '^java\.vendor\=Alibaba$'
                '^java\.vendor\.url\.bug\=mailto\:dragonwell_use@googlegroups\.com$')
RET=0
for p in ${EXPECTED_PATTERN[*]}
do
    cat /tmp/systemProperty.out | grep "$p"
    if [ 0 != $? ]; then RET=1; fi
done

\rm -f /tmp/systemProperty*

ldd ${NEW_JAVA_HOME}/lib/libzip.so | grep libz
if [ 0 != $? ]; then RET=1; fi
echo "================= Sanity test end ======================"

exit ${RET}
