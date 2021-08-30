# @test
# @summary test ajdk arguments for aarch64
# @run shell TestUnsupportedArguments.sh

JAVA=${TESTJAVA}/bin/java
echo "$JAVA"
ILLEGAL_OPTS=('-XX:+UseVectorAPI' '-XX:+EagerAppCDS' '-XX:-PromoteAOTtoFullProfile')

case `uname -m` in
  x86_64)
    for OPT in ${ILLEGAL_OPTS[*]}; do
      $JAVA $OPT -version
      if [ $? -eq 1 ]; then
       echo "Should support $OPT in X86 but failed"
       exit 1;
      else
       echo "$OPT passed"
      fi
    done
     ;;
  aarch64)
    for OPT in ${ILLEGAL_OPTS[*]}; do
      echo "$JAVA $OPT -version"
      $JAVA $OPT -version
      if [ $? -eq 0 ]; then
       echo "$OPT is not supported in aarch64, test failed"
       exit 1;
      else
       echo "$OPT is correctly disabled"
      fi
    done
     ;;
  *)
     echo "Unkown architecture. Not supported."
     exit 1;
     ;;
esac


