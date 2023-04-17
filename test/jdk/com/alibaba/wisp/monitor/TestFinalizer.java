/*
 * @test
 * @summary Test unpark in a finalizer  thread.
 * @requires os.family == "linux"
 * @requires os.arch != "riscv64"
 * @run main/othervm   -XX:-UseBiasedLocking -XX:+UnlockExperimentalVMOptions -XX:+EnableCoroutine -XX:+UseWispMonitor -Dcom.alibaba.wisp.transparentWispSwitch=true TestFinalizer
*/

public class TestFinalizer {
    static final Foo lock = new Foo();
    public static void main(String[] args) throws Exception {
        new Foo();
        new Thread(() -> {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.gc();
            System.runFinalization();
        }, "call-System.gc()").start();
        synchronized (lock) {
            lock.wait();
        }
        System.out.println(Thread.currentThread().getName() + ": wait done");
    }

    static class Foo {
        @Override
        protected void finalize() throws Throwable {
            synchronized (lock) {
                lock.notify();
            }
        }
    }
}
