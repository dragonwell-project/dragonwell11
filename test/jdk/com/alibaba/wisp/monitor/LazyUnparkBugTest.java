/*
 * @test
 * @summary T12212948
 * @library /lib/testlibrary
 * @run main/othervm -XX:+EnableCoroutine -XX:+UseWispMonitor -Dcom.alibaba.wisp.transparentWispSwitch=true LazyUnparkBugTest
 * @run main/othervm -XX:+EnableCoroutine -XX:+UseWispMonitor -Dcom.alibaba.wisp.transparentWispSwitch=true -Dcom.alibaba.wisp.version=2 LazyUnparkBugTest
*/
 
public class LazyUnparkBugTest {
    private static volatile Object thunk = null;
 
    public static void test() throws Exception {
        thunk = null;
        Thread t1 = new Thread(() -> {
            try {
                synchronized (LazyUnparkBugTest.class) {
                    Thread.sleep(1_000L);
                }
                Object o;
                do {
                    o = thunk;
                } while (o == null);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
 
        Thread t2 = new Thread(() -> {
            try {
                Thread.sleep(5_0L);
                synchronized (LazyUnparkBugTest.class) {
                    System.out.println("in t2");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            thunk = new Object();
        });
        t1.start();
        t2.start();
        t1.join();
        t2.join();
    }
 
    public static void main(String[] args) throws Exception {
        long begin = System.currentTimeMillis();
        test();
        long end = System.currentTimeMillis();
        System.out.println("cost : " + (end - begin) + " ms");
        if ((end - begin) > 2000) {
            throw new Error("this is bug " + (end - begin));
        }
    }
 
}
