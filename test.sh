#!/bin/bash

jtreg=/vmfarm/tools/jtreg-5.1-b01/jtreg/bin/jtreg

test_func()
{
    ${jtreg} $1
}

concurrent()
{
    mkfifo   ./fifo.$$ &&  exec 5<> ./fifo.$$ && rm -f ./fifo.$$
    for ((i=0; i<10; i++)); do
        echo "" >&5
    done
    for testCase in $(find $1 -iname *.java)
    do
        read -u 5
        {
            echo -e "-- current loop: [cmd id: $i ; fifo id: $REPLY ]"
            test_func $testCase
            echo "" >&5 
        } &
    done
    wait
    exec 5>&-
}

summary()
{
    passed=$(cat JTreport/text/summary.txt | grep -i 'Passed' | wc -l)
    failed=$(cat JTreport/text/summary.txt | grep -i 'Failed' | wc -l)
    echo -e "Summary Result: passed: $(cat JTreport/text/summary.txt | grep -i 'Passed' | wc -l), failed: $(cat JTreport/text/summary.txt | grep -i 'Failed' | wc -l)\n$failed"
}
#concurrent test/jdk/com/alibaba/rcm/
#summary 
HOTSPOT_TESTS="test/hotspot/jtreg/multi-tenant test/hotspot/jtreg/runtime/coroutine test/hotspot/jtreg/compiler/aot/appaot test/hotspot/jtreg/compiler/aot/pgo test/hotspot/jtreg/compiler/aot/invokedynamic test/hotspot/jtreg/jwarmup"
#for xx in ${HOTSPOT_TESTS}
#do
# echo $xx
#done
res=$(summary | tail -n 1)
echo $res
