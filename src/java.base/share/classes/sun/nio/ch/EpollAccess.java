package sun.nio.ch;

import jdk.internal.misc.Unsafe;

import java.io.IOException;


public interface EpollAccess {

    int EPOLL_CTL_ADD = EPoll.EPOLL_CTL_ADD;
    int EPOLL_CTL_DEL = EPoll.EPOLL_CTL_DEL;
    int EPOLL_CTL_MOD = EPoll.EPOLL_CTL_MOD;
    int EPOLLONESHOT = EPoll.EPOLLONESHOT;
    int ENOENT = EPoll.ENOENT;

    long allocatePollArray(int count);

    void freePollArray(long address);

    long getEvent(long address, int i);

    int getDescriptor(long eventAddress);

    int getEvents(long eventAddress);

    int epollCreate() throws IOException;

    int epollCtl(int epfd, int opcode, int fd, int events);

    int epollWait(int epfd, long pollAddress, int numfds) throws IOException;

    static void initializeEpoll() {
        Unsafe.getUnsafe().ensureClassInitialized(EPoll.class);
    }
}
