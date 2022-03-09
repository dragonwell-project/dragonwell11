#!/bin/sh
# see TestPremainWithWispMonitor.java

AGENT=TestPremainWithWispMonitor

if [ "${TESTSRC}" = "" ]
then
  echo "TESTSRC not set.  Test cannot execute.  Failed."
  exit 1
fi
echo "TESTSRC=${TESTSRC}"

if [ "${TESTJAVA}" = "" ]
then
  echo "TESTJAVA not set.  Test cannot execute.  Failed."
  exit 1
fi
echo "TESTJAVA=${TESTJAVA}"


cp ${TESTSRC}/${AGENT}.java .
${TESTJAVA}/bin/javac ${TESTJAVACOPTS} ${TESTTOOLVMOPTS} TestPremainWithWispMonitor.java

echo "Manifest-Version: 1.0"    >  ${AGENT}.mf
echo Premain-Class: ${AGENT} >> ${AGENT}.mf
if [ $# -gt 0 ]; then
  shift
fi
while [ $# != 0 ] ; do
  echo $1 >> ${AGENT}.mf
  shift
done

${TESTJAVA}/bin/jar ${TESTTOOLVMOPTS} cvfm ${AGENT}.jar ${AGENT}.mf ${AGENT}*.class

${TESTJAVA}/bin/java -XX:+UnlockExperimentalVMOptions -XX:+EnableCoroutine -Dcom.alibaba.transparentAsync=true -XX:+UseWispMonitor \
        -javaagent:TestPremainWithWispMonitor.jar TestPremainWithWispMonitor

