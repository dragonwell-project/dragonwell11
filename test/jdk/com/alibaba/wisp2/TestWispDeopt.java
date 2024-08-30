import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
/* @test
 * @summary test deopt
 * @library /lib/testlibrary
 * @modules java.base/com.alibaba.wisp.engine:+open
 * @modules java.base/jdk.internal.vm.annotation
 * @requires os.family == "linux"
 * @requires os.arch=="aarch64" | os.arch=="amd64"
 * @run main/othervm/timeout=2000 -XX:CompileThreshold=20 -Xlog:class+load -XX:+UnlockExperimentalVMOptions -Dcom.alibaba.wisp.threadAsWisp.black=job-emit* -XX:+UseWisp2  -Dcom.alibaba.wisp.carrierEngines=8 TestWispDeopt
*/
public class TestWispDeopt {

    private static int threadNum = 10;
    private static int addition = 5;
    public static int taskTotal = 1000 / threadNum;
    private static AtomicInteger count = new AtomicInteger(0);
    private static int except = (taskTotal + 1 + addition ) * threadNum; // warm up: [0, taskTotal + 1), deopt: [taskTotal + 1, taskTotal + addition]
    
    public static void main(String[] args) throws Exception {
        ExecutorService es = Executors.newFixedThreadPool(20);
        for(int i = 0; i < threadNum; i++) {
            Thread t = new Thread(() -> {
                for (int j = 0; j <= taskTotal; j++) {
                    es.submit(new Task(j, 1));
                }
            });
            t.start();
            t.join();
        }
        try {
            Thread.sleep(6000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println("all finish 0");
        Thread[] threads = new Thread[threadNum];
        for(int i = 0; i < threadNum; i++) {
            threads[i] = new Thread(() -> {
                for (int j = taskTotal + 1; j <= taskTotal + addition; j++) {
                    es.submit(new Task(j, 10000));
                }
            }, "job-emit-" + i);
        }
        for(Thread thread: threads) {
            thread.start();
        }
        for(Thread thread: threads) {
            thread.join();
        }
        System.out.println("all finish 1");
        
        es.shutdown();
        while(!es.awaitTermination(5, TimeUnit.SECONDS));
        System.out.println("all finish 2");
        if (count.get() != except) {
            throw new Error("Miss some tasks!, Except: " + except + ", Actual: " + count.get());
        }
    }

    public static void inc() {
        count.incrementAndGet();
    }

}

class Task implements Runnable {
    int num;
    int sleepTime;
    Task(int num, int sleepTime) {
        this.num = num;
        this.sleepTime = sleepTime;
    }

    @Override
    public void run() {
        runTest();
    }

    public void runTest() {
        // do sleep
        try {
            Thread.sleep(sleepTime);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        Parent p = getParent(this.num);
        p.print();
        TestWispDeopt.inc();
    }

    public Parent getParent(int num) {
        HelpGenerator helpGenerator = new HelpGenerator(num);
        return helpGenerator.generate();
    }
}

class HelpGenerator {
    int num;
    HelpGenerator(int num) {
        this.num = num;
    }
    public Parent generate() {
      if (num <= TestWispDeopt.taskTotal) {
        return new Child1(num);
      }
      return new Child2(num);
    }
}

interface Parent {
    public void print();
}

class Child1 implements Parent {
    int num;
    Child1(int i) {
        num = i;
    }
    
    public void print() {
        System.out.println("Child1 - " + num);
    }
}

class Child2 implements Parent {
    Member m;
    Child2(int i) {
        m = new Member(i);
    }

    public void print() {
        System.out.println("Child2 - " + m.num);
    }
}

class Member {
  int num;
  Member(int n) {
    num = n;
  }
}
