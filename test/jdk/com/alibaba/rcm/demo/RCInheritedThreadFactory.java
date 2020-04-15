package demo;

import com.alibaba.rcm.internal.AbstractResourceContainer;
import jdk.internal.misc.SharedSecrets;
import jdk.internal.misc.JavaLangAccess;

import java.util.concurrent.ThreadFactory;

public class RCInheritedThreadFactory implements ThreadFactory {
    public final static ThreadFactory INSTANCE = new RCInheritedThreadFactory();
    private final static JavaLangAccess JLA = SharedSecrets.getJavaLangAccess();

    @Override
    public Thread newThread(Runnable r) {
        return new Thread(() -> {
            AbstractResourceContainer irc = JLA.getInheritedResourceContainer(Thread.currentThread());
            if (irc != null) {
                irc.run(r);
            } else {
                r.run();
            }
        });
    }
}
