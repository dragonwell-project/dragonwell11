package jdk.internal.misc;

import java.io.IOException;
import java.util.concurrent.Callable;

public interface WispFileSyncIOAccess {
    boolean usingAsyncFileIO();

    <T> T executeAsAsyncFileIO(Callable<T> command) throws IOException;
}
