package jdk.test.lib.crac;

import jdk.crac.*;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.concurrent.CountDownLatch;

import static jdk.test.lib.Asserts.*;

/**
 * capture output from Process is not work when restore from criu.
 * Process use pipe facility to capture the output,but the pipe is broken when restore.
 * CracLogger write output to a file, and this file will close before checkpoint,and open after restore.
 * The correct way use CracLogger is :
 * 1. Extend with CracLogger
 * 2. Enable logToFile with :  CracBuilder.logToFile(true)
 * 3. Read the output with : CracProcess#readLogFile()
 */
public class CracLogger implements Resource {

    private volatile FileWriter fileWriter;
    private volatile boolean opened;

    public CracLogger() {
        Core.getGlobalContext().register(this);
        open();
    }

    public boolean isOpened() {
        return opened;
    }

    private synchronized void open() {
        String logFile = System.getenv(CracBuilder.ENV_LOG_FILE);
        if (logFile != null) {
            assertFalse(opened, "file should not open yet!");
            try {
                fileWriter = new FileWriter(logFile);
                opened = true;
                this.notify();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private synchronized void close() throws IOException {
        assertTrue(fileWriter != null, "file must open yet!");
        fileWriter.close();
        fileWriter = null;
        opened = false;
    }

    public synchronized void writeLog(String msg) throws IOException, InterruptedException {
        while (!isOpened()) {
            this.wait();
        }
        assertTrue(fileWriter != null, "file must open yet!");
        fileWriter.write(msg);
        fileWriter.flush();
    }

    public void beforeCheckpoint(Context<? extends Resource> context) throws Exception {
        if (isOpened()) {
            close();
        }
    }

    /**
     * Invoked by a {@code Context} as a notification about restore.
     *
     * @param context {@code Context} providing notification
     * @throws Exception if the method have failed
     */
    public void afterRestore(Context<? extends Resource> context) throws Exception {
        open();
    }
}
