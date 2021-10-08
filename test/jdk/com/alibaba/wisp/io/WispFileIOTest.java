/*
 * @test
 * @library /lib/testlibrary
 * @summary test reuse WispUdpSocket buffer
 * @modules java.base/jdk.internal.misc
 * @requires os.family == "linux"
 * @run main/othervm -XX:+UnlockExperimentalVMOptions -XX:+UseWisp2 -Dcom.alibaba.wisp.enableAsyncFileIO=true WispFileIOTest
 */

import jdk.internal.misc.SharedSecrets;
import sun.misc.Unsafe;

import java.io.*;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.ClosedByInterruptException;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.util.concurrent.*;

import static java.nio.file.StandardOpenOption.*;
import static jdk.testlibrary.Asserts.assertTrue;

public class WispFileIOTest {

    public static void testNioFileChannel(File testFile) throws Exception {
        resetTestFileContent(testFile);
        RandomAccessFile file = new RandomAccessFile(testFile, "rw");
        FileChannel ch = file.getChannel();
        ByteBuffer buffer = ByteBuffer.allocate(1);
        ch.read(buffer);
        assertTrue("0".equals(new String(buffer.array())));
        buffer.flip();
        ch.write(buffer);
        ch.close();
        String content = new String(Files.readAllBytes(testFile.toPath()));
        assertTrue("00234".equals(content));
    }

    public static void testFileStream(File testFile) throws Exception {
        //test RandomAccessFile
        resetTestFileContent(testFile);
        RandomAccessFile raf = new RandomAccessFile(testFile, "rw");
        byte[] buffer;
        buffer = "5".getBytes();
        raf.write(buffer);
        raf.seek(0);
        buffer = new byte[1];
        raf.read(buffer, 0, 1);
        assertTrue("5".equals(new String(buffer)));

        //test FileInputStream
        resetTestFileContent(testFile);
        FileInputStream fis = new FileInputStream(testFile);
        buffer = new byte[1];
        fis.read(buffer);
        assertTrue("0".equals(new String(buffer)));

        //test FileOutputStream
        resetTestFileContent(testFile);
        FileOutputStream fos = new FileOutputStream(testFile, true);
        buffer = "5".getBytes();
        fos.write(buffer);
        String content = new String(Files.readAllBytes(testFile.toPath()));
        assertTrue("012345".equals(content));

    }

    public static void testMappedByteBuffer() throws Exception {
        File newfile = new File("/tmp/ThreadPoolAioTest_test_new2.file");
        newfile.deleteOnExit();
        RandomAccessFile raf = new RandomAccessFile(newfile, "rw");
        FileChannel fc = raf.getChannel();
        MappedByteBuffer map = fc.map(FileChannel.MapMode.READ_WRITE, 0, 2048);
        fc.close();
        double current = map.getDouble(50);
        map.putDouble(50, current + 0.1d);
        map.force();
    }

    public static Thread workerThread = null;

    public static void resetTestFileContent(File testFile) throws IOException {
        FileWriter writer = new FileWriter(testFile);
        for (int i = 0; i < 5; i++) {
            writer.write(String.valueOf(i));
        }
        writer.close();
    }

    private static void testBlockingReadInterrupted(File testFile) throws IOException {
        long block = 12;
        long size = 4096;
        long total = block * size;
        createLargeFile(block * size, testFile);
        for (int i = 0; i < block; i++) {
            try (FileInputStream fis = new FileInputStream(testFile)) {
                long skip = skipBytes(fis, size, total);
                total -= skip;
                assertTrue(skip == size || skip == 0);
            } finally {
                testFile.delete();
            }
        }

    }

    // Skip toSkip number of bytes and expect that the available() method
    // returns avail number of bytes.
    private static long skipBytes(InputStream is, long toSkip, long avail)
            throws IOException {
        long skip = is.skip(toSkip);
        if (skip != toSkip) {
            throw new RuntimeException("skip() returns " + skip
                    + " but expected " + toSkip);
        }
        long remaining = avail - skip;
        int expected = (remaining >= Integer.MAX_VALUE)
                ? Integer.MAX_VALUE
                : (remaining > 0 ? (int) remaining : 0);

        System.out.println("Skipped " + skip + " bytes, available() returns "
                + expected + ", remaining " + remaining);
        if (is.available() != expected) {
            throw new RuntimeException("available() returns "
                    + is.available() + " but expected " + expected);
        }
        return skip;
    }

    private static void createLargeFile(long filesize,
                                        File file) throws IOException {
        // Recreate a large file as a sparse file if possible
        Files.delete(file.toPath());

        try (FileChannel fc =
                     FileChannel.open(file.toPath(), CREATE_NEW, WRITE, APPEND)) {
            ByteBuffer bb = ByteBuffer.allocate(1).put((byte) 1);
            bb.rewind();
            int rc = fc.write(bb, filesize - 1);

            if (rc != 1) {
                throw new RuntimeException("Failed to write 1 byte"
                        + " to the large file");
            }
        }
    }

    private static void mockIOException() throws Exception {
        Field f = Unsafe.class.getDeclaredField("theUnsafe");
        f.setAccessible(true);
        Unsafe unsafe = (Unsafe) f.get(null);
        try {
            SharedSecrets.getWispFileSyncIOAccess().executeAsAsyncFileIO(() -> {
                throw new IOException("expected");
            });
        } catch (IOException e) {
            unsafe.throwException(e);
        } catch (Exception exception) {
            //
        }
    }

    public static void main(String[] args) throws Exception {

        // submit by another thread
        Thread t = new Thread(() -> {
            try {
                File f = new File("/tmp/ThreadPoolAioTest_test.file");
                f.deleteOnExit();
                // test java nio
                testNioFileChannel(f);
                // test java io
                testFileStream(f);
                // test rename
                File newfile = new File("/tmp/ThreadPoolAioTest_test_new.file");
                newfile.deleteOnExit();
                f.renameTo(newfile);
                // test MappedByteBuffer force
                testMappedByteBuffer();
                resetTestFileContent(f);
            } catch (Exception e) {
                e.printStackTrace();
                assertTrue(false, "exception happened");
            }
        });
        t.start();
        t.join();

        CountDownLatch finished = new CountDownLatch(1);
        Thread interrupt = new Thread(() -> {
            try {
                File f = new File("/tmp/ThreadPoolAioTest_test.file");
                testBlockingReadInterrupted(f);
            } catch (Exception e) {
                e.printStackTrace();
                assertTrue(e instanceof ClosedByInterruptException, "exception happened");
            } finally {
                finished.countDown();
            }
        });
        interrupt.start();

        while (finished.getCount() != 0) {
            interrupt.interrupt();
        }
        interrupt.join();


        boolean exceptionHappened = false;
        try {
            mockIOException();
        } catch (Exception e) {
            e.printStackTrace();
            if (e instanceof IOException) {
                exceptionHappened = true;
            }
        }  finally {
            assertTrue(exceptionHappened);
        }

        System.out.println("Success!");
    }
}
