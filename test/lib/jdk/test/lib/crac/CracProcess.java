package jdk.test.lib.crac;

import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.process.StreamPumper;

import java.io.*;
import java.nio.file.*;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static jdk.test.lib.Asserts.*;

public class CracProcess {
    private final CracBuilder builder;
    private final Process process;

    public CracProcess(CracBuilder builder, List<String> cmd) throws IOException {
        this.builder = builder;
        ProcessBuilder pb = new ProcessBuilder().inheritIO();
        if (builder.captureOutput) {
            pb.redirectOutput(ProcessBuilder.Redirect.PIPE);
            pb.redirectError(ProcessBuilder.Redirect.PIPE);
        }
        pb.environment().putAll(builder.env);
        if (builder.logToFile) {
            pb.environment().put("CRAC_LOG_FILE", builder.cracLogFile);
        }
        this.process = pb.command(cmd).start();
    }

    public int waitFor() throws InterruptedException {
        return process.waitFor();
    }

    public void waitForCheckpointed() throws InterruptedException {
        if (builder.engine == null || builder.engine == CracEngine.CRIU) {
            assertEquals(137, process.waitFor(), "Checkpointed process was not killed as expected.");
            // TODO: we could check that "CR: Checkpoint" was written out
        } else {
            fail("With engine " + builder.engine.engine + " use the async version.");
        }
    }

    public void waitForPausePid() throws IOException, InterruptedException {
        assertEquals(CracEngine.PAUSE, builder.engine, "Pause PID file created only with pauseengine");
        try (WatchService watcher = FileSystems.getDefault().newWatchService()) {
            Path imageDir = builder.imageDir().toAbsolutePath();
            waitForFileCreated(watcher, imageDir.getParent(), path -> "cr".equals(path.toFile().getName()));
            waitForFileCreated(watcher, imageDir, path -> "pid".equals(path.toFile().getName()));
        }
    }

    private void waitForFileCreated(WatchService watcher, Path dir, Predicate<Path> predicate) throws IOException, InterruptedException {
        WatchKey key = dir.register(watcher, StandardWatchEventKinds.ENTRY_CREATE);
        assertTrue(key.isValid());
        try {
            try (Stream<Path> dirContents = Files.list(dir)) {
                if (dirContents.anyMatch(predicate)) {
                    // file already present
                    return;
                }
            }
            for (; ; ) {
                WatchKey key2 = watcher.take();
                for (WatchEvent<?> event : key2.pollEvents()) {
                    if (event.kind() != StandardWatchEventKinds.ENTRY_CREATE) {
                        continue;
                    }
                    if (predicate.test((Path) event.context())) {
                        return;
                    }
                }
                key2.reset();
            }
        } finally {
            key.cancel();
        }
    }

    public CracProcess waitForSuccess() throws InterruptedException {
        int exitValue = process.waitFor();
        assertEquals(0, exitValue, "Process returned unexpected exit code: " + exitValue);
        builder.log("Process %d completed with exit value %d%n", process.pid(), exitValue);
        return this;
    }

    public OutputAnalyzer outputAnalyzer() throws IOException {
        assertTrue(builder.captureOutput, "Output must be captured with .captureOutput(true)");
        return new OutputAnalyzer(process);
    }

    public OutputAnalyzer fileOutputAnalyser() throws IOException {
        return new OutputAnalyzer(readFile());
    }

    private String readFile() throws IOException {
        assertTrue(builder.logToFile, "logToFile must be true.");
        assertNotNull(builder.cracLogFile, "cracLogFile should be not null.Maybe the process may not started!");
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(builder.cracLogFile))) {
            String line = reader.readLine();
            while (line != null) {
                sb.append(line);
                line = reader.readLine();
            }
        }
        return sb.toString();
    }

    public void watchFile(long timeout, String expectedString) throws RuntimeException{
        try {
            long start = System.currentTimeMillis();
            while ((System.currentTimeMillis() - start) < timeout) {
                if (new File(builder.cracLogFile).exists() && readFile().contains(expectedString)) {
                    return;
                }
                Thread.sleep(100);
            }
            fail("Not found " + expectedString + " in file : " + builder.cracLogFile);
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }


    public CracProcess watch(Consumer<String> outputConsumer, Consumer<String> errorConsumer) {
        assertTrue(builder.captureOutput, "Output must be captured with .captureOutput(true)");
        pump(process.getInputStream(), outputConsumer);
        pump(process.getErrorStream(), errorConsumer);
        return this;
    }

    private static void pump(InputStream stream, Consumer<String> consumer) {
        new StreamPumper(stream).addPump(new StreamPumper.LinePump() {
            @Override
            protected void processLine(String line) {
                consumer.accept(line);
            }
        }).process();
    }
}
