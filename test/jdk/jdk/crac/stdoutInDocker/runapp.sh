#! /bin/sh
APP_DIR=$(dirname "$0")
JAVA=${JAVA_HOME}/bin/java
if [[ "${DO_CHECKPOINT}" = "true" ]];
then
    for i in {1..100}
    do
        sh ${APP_DIR}/takepid.sh
    done
    ${JAVA} -XX:CRaCCheckpointTo=${CRAC_IMAGE_DIR} ${CRAC_UNPRIV_OPT} ${CRAC_INHERIT_OPT} -cp ${APP_DIR}/classes TestStdoutInDocker
elif [[ "${DO_RESTORE}" = "true" ]];
then
    export CRAC_CRIU_OPTS="-o ${CRAC_IMAGE_DIR}/restore.log -vvvv"
    ${JAVA} -XX:CRaCRestoreFrom=${CRAC_IMAGE_DIR} ${CRAC_UNPRIV_OPT}  -cp ${APP_DIR}/classes TestStdoutInDocker
else
    ${JAVA} -cp ${APP_DIR}/classes TestStdoutInDocker
fi