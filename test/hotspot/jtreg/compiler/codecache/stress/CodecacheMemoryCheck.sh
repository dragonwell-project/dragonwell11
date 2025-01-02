#!/bin/bash
# Copyright (c) 2024 Alibaba Group Holding Limited. All Rights Reserved.
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

# @test
# @key stress randomness
# @summary check memory usage of optimizations and deoptimizations
# @library /test/lib /
# @modules java.base/jdk.internal.misc java.management
# @build sun.hotspot.WhiteBox compiler.codecache.stress.Helper compiler.codecache.stress.TestCaseImpl
# @build compiler.codecache.stress.UnexpectedDeoptimizationTest
# @build compiler.codecache.stress.UnexpectedDeoptimizationTestLoop
# @run driver ClassFileInstaller sun.hotspot.WhiteBox sun.hotspot.WhiteBox$WhiteBoxPermission
# @run shell/timeout=7200 CodecacheMemoryCheck.sh


# set a few environment variables so that the shell-script can run stand-alone
# in the source directory
if [ "${TESTSRC}" = "" ] ; then
  TESTSRC="."
fi
if [ "${TESTJAVA}" = "" ] ; then
  echo "TESTJAVA not set.  Test cannot execute."
  echo "FAILED!!!"
  exit 1
fi
if [ "${COMPILEJAVA}" = "" ]; then
  COMPILEJAVA="${TESTJAVA}"
fi

loopCount=40
if [[ -n "$1" ]] ; then
    loopCount=$1
fi

# set platform-dependent variables
OS=`uname -s`
case "$OS" in
  SunOS )
    PATHSEP=":"
    FILESEP="/"
    ;;
  Linux )
    PATHSEP=":"
    FILESEP="/"
    ;;
  Darwin )
    PATHSEP=":"
    FILESEP="/"
    ;;
  AIX )
    PATHSEP=":"
    FILESEP="/"
    ;;
  CYGWIN* )
    PATHSEP=";"
    FILESEP="/"
    ;;
  Windows* )
    PATHSEP=";"
    FILESEP="\\"
    ;;
  * )
    echo "Unrecognized system!"
    exit 1;
    ;;
esac

useJcmdPrintMemoryUsage()
{
    pid=$1
    javaLog=$2
    i=0
    while ! grep -q "For random generator using seed" ${javaLog}
    do
        sleep 0.1  #wait util java main function start finish
        if [[ $i -ge 200 ]] ; then
            cat ${javaLog}
            echo "The tested java seems work abnormally!"
            exit 1
        fi
        let i++
    done
    i=0
    rm -rf *-native_memory-summary.log
    while kill -0 ${pid} 2>/dev/null
    do
        ${TESTJAVA}${FS}bin${FS}jcmd ${pid} VM.native_memory summary &> ${i}-native_memory-summary.log
        if [[ 0 -ne $? ]] ; then
            if grep -q "Exception" ${i}-native_memory-summary.log || ! kill -0 ${pid} ; then
                #The target java process has been teminated/finished
                #java.io.IOException: No such process
                #com.sun.tools.attach.AttachNotSupportedException: Unable to parse namespace
                #java.io.IOException: Premature EOF
                mv ${i}-native_memory-summary.log jcmd-exception.log
                break
            else
                if kill -0 $$ ; then
                    echo "jcmd command execute fail!"
                    exit 1
                else
                    mv ${i}-native_memory-summary.log jcmd-error.log
                    break
                fi
            fi
        fi
        let i++
        sleep 2
    done
}

getMemoryUsageFromProc()
{
    pid=$1
    javaLog=$2
    i=0
    while ! grep -q "For random generator using seed" ${javaLog}
    do
        sleep 0.1  #wait util java main function start finish
        if [[ $i -ge 200 ]] ; then
            cat ${javaLog}
            echo "The tested java seems work abnormally!"
            exit 1
        fi
        let i++
    done
    mkdir -p plot-data
    rm -rf proc-*.csv plot-data/proc-*.txt
    echo -n "VmSize" > proc-VmSize.csv
    echo -n "VmRSS" > proc-VmRSS.csv
    echo -n "PageNum" > proc-PageNum.csv
    i=0
    while kill -0 ${pid} 2>/dev/null
    do
        VmSize=`grep -w VmSize /proc/${pid}/status | awk '{print $2}'`
        VmRSS=`grep -w VmRSS /proc/${pid}/status | awk '{print $2}'`
        PageNum=`cat /proc/${pid}/statm | awk '{print $1}'`
        if kill -0 ${pid} ; then
            echo -n ",${VmSize}" >> proc-VmSize.csv
            echo -n ",${VmRSS}" >> proc-VmRSS.csv
            echo -n ",${PageNum}" >> proc-PageNum.csv
            echo "${i} ${VmSize}" >> plot-data/proc-VmSize.txt
            echo "${i} ${VmRSS}" >> plot-data/proc-VmRSS.txt
            echo "${i} ${PageNum}" >> plot-data/proc-PageNum.txt
            let i++;
        fi
        sleep 2
    done
    echo "" >> proc-VmSize.csv
    echo "" >> proc-VmRSS.csv
    echo "" >> proc-PageNum.csv
    cat proc-VmSize.csv proc-VmRSS.csv proc-PageNum.csv > proc.csv
}

generatePlotPNG()
{
    if [[ ! -d plot-data ]] ; then
        echo "echo plot-data directory not exist!"
        return
    fi
    if [[ ! -f ${TESTSRC}/plot.gp ]] ; then
        echo "${TESTSRC}/plot.gp not exists!"
        return
    fi
    if ! which gnuplot ; then
        echo please install gnuplot command!
        return
    fi
    for file in `ls plot-data | grep "\.txt$"`
    do
        name=`basename $file .txt`
        echo plot ${name}
        gnuplot -c ${TESTSRC}/plot.gp "plot-data/${file}" "${name}" "plot-data/${name}.png"
    done
    if which zip ; then
        rm -rf plot-data.zip
        zip -rq9 plot-data.zip plot-data
    else
        tar cf - plot-data | xz -9 -T `nproc` > plot-data.tar.xz
    fi
}

set -x
commonJvmOptions="-Xbootclasspath/a:. -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI -XX:-DeoptimizeRandom \
 -XX:CompileCommand=dontinline,compiler.codecache.stress.Helper\$TestCase::method -XX:NativeMemoryTracking=summary"

rm -rf java.log plot-data
mkdir -p plot-data
${TESTJAVA}${FS}bin${FS}java ${TESTVMOPTS} ${TESTJAVAOPTS} ${commonJvmOptions} \
 -Dtest.src=${TESTSRC} -cp ${TESTCLASSPATH} compiler.codecache.stress.UnexpectedDeoptimizationTestLoop ${loopCount} &> java.log &
pid=$!
ps -ef | grep java | grep UnexpectedDeoptimizationTestLoop &> ps-java.log
getMemoryUsageFromProc ${pid} java.log 2> proc-detail-stderr.log &
useJcmdPrintMemoryUsage ${pid} java.log 2> jcmd-detail-stderr.log
if ( set +x ; grep -q "Unable to open socket file" *-native_memory-summary.log ) ; then
    echo 'jcmd report error: "-native_memory-summary.log"'
    exit 1
fi

( set +x ; perl -w ${TESTSRC}/get-native-memory-usage.pl 25 "Code-malloc:2.5,Code-mmap:2.8,Compiler-malloc:4.6" `ls *-native_memory-summary.log | sort -n | xargs` )
exitCode=$?
generatePlotPNG &> generatePlotPNG.log

( set +x ; mkdir -p native_memory-summary ; mv *-native_memory-summary.log native_memory-summary/ )

exit ${exitCode}
