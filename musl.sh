#!/bin/bash

bash ./configure --with-freetype=system \
                 --enable-unlimited-crypto \
                 --with-jvm-variants=server \
                 --with-debug-level=$DEBUG_LEVEL \
                 --with-cacerts-file=`pwd`/make/data/security/cacerts \
                 --with-version-opt="" \
                 --with-version-pre="Enclave-Experimental" \
                 --with-vendor-name="Alibaba" \
                 --with-vendor-url="http://www.alibabagroup.com" \
                 --with-vendor-bug-url="mailto:dragonwell_use@googlegroups.com" \
                 --with-vendor-version-string="(Alibaba Dragonwell)" \
                 --with-version-build="${BUILD_NUMBER}" \
                 --with-version-feature="11" \
                 --with-version-patch="${DRAGONWELL_VERSION}" \
                 --with-version-date="$(date +%Y-%m-%d)" \
                 --with-zlib=system \
                 --build=x86_64-unknown-linux \
                 --host=x86_64-unknown-linux

make CONF=$BUILD_MODE LOG=cmdlines JOBS=8 images
