#! /bin/sh -x
#
# Copyright (c) 2024, Alibaba Group Holding Limited. All rights reserved.
# DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
#
# This code is free software; you can redistribute it and/or modify it
# under the terms of the GNU General Public License version 2 only, as
# published by the Free Software Foundation.
#
# This code is distributed in the hope that it will be useful, but WITHOUT
# ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
# FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
# version 2 for more details (a copy is included in the LICENSE file that
# accompanied this code).
#
# You should have received a copy of the GNU General Public License version
# 2 along with this work; if not, write to the Free Software Foundation,
# Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
#
# Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
# or visit www.oracle.com if you need additional information or have any
# questions.
#


# @test
# @summary  Test "docker logs" can get stdout/stderr after restoring.
#           Test docker run with the combination of (privileged/unprivileged) and (interactive/detach) modes.
# @requires docker.support
# @requires os.family == "linux"
# @requires os.arch=="amd64" | os.arch=="x86_64" | os.arch=="aarch64"
# @build TestStdoutInDocker
# @run shell TestStdoutInDocker.sh

TEST_IMAGE="crac-stdout-err-testimage:latest"
PRIV_PARAM="--privileged --user root"
NON_PRIV_PARAM="--security-opt seccomp=unconfined --cap-add CHECKPOINT_RESTORE --cap-add CAP_SETPCAP --env CRAC_UNPRIV_OPT=-XX:+CRaCUnprivileged"
CHECKPOINT_PARAM="--env CRAC_INHERIT_OPT=-XX:CRaCRestoreInheritPipeFds=1,2 --env DO_CHECKPOINT=true --env CRAC_IMAGE_DIR=/cr"
RESTORE_PARAM="--env DO_RESTORE=true --env CRAC_IMAGE_DIR=/cr"
CR_DIR="/tmp/cr"

resetDir() {
  rm -rf $1
  mkdir -p $1
}

assertExist() {
  grep "$1" $2
  if [ $? -ne 0 ]; then
    echo "$? not found $1 in file $2"
    exit 1
  fi
}

checkOutput() {
  assertExist 'Message from stderr in afterRestore callback' $1
  assertExist 'Message from stdout in afterRestore callback' $1
  assertExist 'Message from stderr afterRestore' $1
  assertExist 'Message from stdout afterRestore' $1
}

killDockerPs() {
  docker ps | grep  ${TEST_IMAGE} | awk '{print $1}' | xargs docker kill &> /dev/null
}

executCmd() {
  echo "Run command: [$1]"
  eval $1
}

# cr dir is used to save image files, it need to mount as volume when run docker
# So avoid the permission denied error, use a directory in /tmp and grant with 0777
resetCrDir() {
  rm -rf ${CR_DIR}
  mkdir ${CR_DIR}
  chmod 0777 ${CR_DIR}
}

runInteractive() {
  resetCrDir
  executCmd "docker run $1 ${CHECKPOINT_PARAM} -v ${CR_DIR}:/cr ${TEST_IMAGE}"
  executCmd "docker run $1 ${RESTORE_PARAM} --env SLEEP_TIME=3000 -v ${CR_DIR}:/cr ${TEST_IMAGE} &> ${CR_DIR}/docker.log"
  checkOutput ${CR_DIR}/docker.log
}

runDetach() {
  resetCrDir
  executCmd "docker run $1 -d ${CHECKPOINT_PARAM} -v ${CR_DIR}:/cr ${TEST_IMAGE}"
  sleep 3
  killDockerPs

  executCmd "docker run $1 -d ${RESTORE_PARAM} --env SLEEP_TIME=30000 -v ${CR_DIR}:/cr ${TEST_IMAGE}"
  sleep 3
  cid=$(docker ps | grep ${TEST_IMAGE} | awk '{print $1}') 
  docker logs ${cid} &> ${CR_DIR}/docker.log
  killDockerPs
  checkOutput ${CR_DIR}/docker.log
}

preCheck() {
  match=$(uname -r | awk -F'.' '{print $1>=5 && $2>=9; }')
  if [ $match -ne 1 ]; then
    echo "OS version should >= 5.9"
    exit 0
  fi
}

buildTestImage() {
  docker rmi -f ${TEST_IMAGE} 
  resetDir ${WORK_DIR}/baseimage-tmp
  pushd baseimage-tmp
  mkdir jdk classes
  cp -R ${TESTJAVA}/* jdk
  cp -R ${TESTCLASSES}/* classes
  cp ${TESTSRCPATH}/*.sh ${TESTSRCPATH}/Dockerfile .
  chmod +x *.sh
  tar zcvf jdk.tar.gz jdk
  tar zcvf classes.tar.gz classes
  rm -rf jdk classes
  docker build -t ${TEST_IMAGE} .
  popd
  rm -rf baseimage-tmp 
}

WORK_DIR=$(pwd)
preCheck
buildTestImage
runDetach "$PRIV_PARAM"
runDetach "$NON_PRIV_PARAM"
runInteractive "$PRIV_PARAM"
runInteractive "$NON_PRIV_PARAM"