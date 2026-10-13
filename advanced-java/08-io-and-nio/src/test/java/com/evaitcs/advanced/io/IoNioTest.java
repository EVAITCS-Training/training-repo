package com.evaitcs.advanced.io;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests as PROOF for the claims made in the demos. Run with:
 *   .\mvnw.cmd -pl 08-io-and-nio test
 * Each test proves ONE claim and prints its evidence, so the test run doubles as a demo.
 *
 * Everything here is DETERMINISTIC:
 *   - buffer pointers are exact integers the spec guarantees at every step;
 *   - charset decode is a pure function of (bytes, charset);
 *   - close order and suppression are guaranteed by the try-with-resources spec;
 *   - file work runs in a JUnit @TempDir (cleaned up for us);
 *   - the socket test runs on the LOOPBACK interface with an ephemeral port, bounded SO_TIMEOUTs,
 *     a bounded latch await, and joined threads — so it can never hang or depend on timing.
 */
class IoNioTest {

    @Test
    @DisplayName("buffer lifecycle: allocate(8)+fill(5) gives position=5 limit=8; flip gives 0/5; drain 3 gives position=3; clear gives 0/8")
    void bufferLifecyclePointersAreExact() {
        ByteBuffer buf = ByteBuffer.allocate(8);
        assertEquals(0, buf.position(), "fresh buffer: position starts at 0");
        assertEquals(8, buf.limit(), "fresh buffer: limit starts at capacity");
        assertEquals(8, buf.capacity(), "capacity is fixed at allocation");

        for (int i = 0; i < 5; i++) {
            buf.put((byte) ('A' + i));                    // FILL 5 bytes
        }
        assertEquals(5, buf.position(), "filling advanced position to 5");
        assertEquals(8, buf.limit(), "limit stays at capacity while filling");

        buf.flip();                                       // FILL -> DRAIN
        assertEquals(0, buf.position(), "flip reset position to 0");
        assertEquals(5, buf.limit(), "flip set limit to the old position (5)");

        byte[] out = new byte[3];
        buf.get(out);                                     // DRAIN 3 bytes
        assertEquals(3, buf.position(), "draining advanced position to 3");
        assertEquals(5, buf.limit(), "limit is unchanged while draining");
        assertArrayEquals(new byte[]{'A', 'B', 'C'}, out, "the first three bytes drained in order");

        buf.clear();                                      // reset for filling again
        assertEquals(0, buf.position(), "clear reset position to 0");
        assertEquals(8, buf.limit(), "clear reset limit to capacity (8)");

        System.out.println("FILL->pos5/lim8, FLIP->pos0/lim5, DRAIN3->pos3/lim5, CLEAR->pos0/lim8; drained="
                + Arrays.toString(out));
    }

    @Test
    @DisplayName("mojibake: UTF-8 bytes decoded as windows-1252 differ from the original; decoded as UTF-8 they match exactly")
    void mojibakeProvesTheCharsetIsTheContract() {
        String original = "Grüße, SecureBank";
        byte[] utf8 = original.getBytes(StandardCharsets.UTF_8);

        String wrong = new String(utf8, Charset.forName("windows-1252"));
        String right = new String(utf8, StandardCharsets.UTF_8);

        assertNotEquals(original, wrong, "the wrong charset must NOT reproduce the text (that is mojibake)");
        assertEquals(original, right, "the correct charset reproduces the text exactly");
        System.out.println("original=\"" + original + "\"  as-windows-1252=\"" + wrong + "\"  as-UTF-8=\"" + right + "\"");
    }

    @Test
    @DisplayName("try-with-resources closes in reverse declaration order, and a throwing close becomes one suppressed exception")
    void tryWithResourcesClosesReverseAndSuppresses() throws Exception {
        // (a) reverse close order: declare A, B, C -> they close C, B, A.
        List<String> closeOrder = new ArrayList<>();
        try (Traced a = new Traced("A", closeOrder, false);
             Traced b = new Traced("B", closeOrder, false);
             Traced c = new Traced("C", closeOrder, false)) {
            assertEquals(0, closeOrder.size(), "nothing is closed inside the body");
        }
        assertEquals(List.of("C", "B", "A"), closeOrder, "close order is the reverse of declaration order");

        // (b) suppression: the body throws, and the close() ALSO throws -> close is suppressed.
        List<String> log = new ArrayList<>();
        IllegalStateException primary = null;
        try {
            try (Traced r = new Traced("R", log, true)) {  // close() will throw IOException
                throw new IllegalStateException("primary body failure");
            }
        } catch (IllegalStateException e) {
            primary = e;
        }
        assertNotNull(primary, "the body's exception is the one that propagates");
        assertEquals(1, primary.getSuppressed().length, "exactly one suppressed exception: the failing close()");
        assertTrue(primary.getSuppressed()[0] instanceof IOException, "the suppressed exception is the close() failure");
        assertEquals(List.of("R"), log, "the throwing resource still ran its close()");

        System.out.println("closeOrder=" + closeOrder + "  primary=" + primary.getMessage()
                + "  suppressed=" + primary.getSuppressed()[0].getMessage());
    }

    @Test
    @DisplayName("Files.writeString then readString round-trips text exactly in a temp dir")
    void filesRoundTripExact(@TempDir Path dir) throws IOException {
        Path f = dir.resolve("note.txt");
        String text = "Grüße, SecureBank — payroll 2450.00";

        Files.writeString(f, text, StandardCharsets.UTF_8);
        String back = Files.readString(f, StandardCharsets.UTF_8);

        assertEquals(text, back, "writeString/readString with the same charset is an exact round-trip");
        System.out.println("round-trip exact: \"" + back + "\"");
    }

    @Test
    @DisplayName("a FileChannel read loop that runs until read returns -1 reads back the exact bytes written")
    void fileChannelLoopReadsExactBytes(@TempDir Path dir) throws IOException {
        Path f = dir.resolve("data.bin");
        byte[] data = new byte[20];
        for (int i = 0; i < data.length; i++) {
            data[i] = (byte) i;
        }

        try (FileChannel w = FileChannel.open(f,
                StandardOpenOption.WRITE, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
            ByteBuffer out = ByteBuffer.wrap(data);
            while (out.hasRemaining()) {
                w.write(out);                             // a channel drains a buffer
            }
        }

        ByteArrayOutputStream sink = new ByteArrayOutputStream();
        ByteBuffer buf = ByteBuffer.allocate(4);          // small buffer forces several loop iterations
        int iterations = 0;
        try (FileChannel r = FileChannel.open(f, StandardOpenOption.READ)) {
            while (r.read(buf) != -1) {                   // -1 signals end of file for a FILE channel
                iterations++;
                buf.flip();                               // FILL -> DRAIN
                while (buf.hasRemaining()) {
                    sink.write(buf.get());
                }
                buf.clear();                              // DRAIN -> FILL for the next read
            }
        }

        assertArrayEquals(data, sink.toByteArray(), "the loop read back every byte, in order");
        assertTrue(iterations >= 5, "a 4-byte buffer over 20 bytes needs at least 5 reads");
        System.out.println("FileChannel loop ran " + iterations + " times and read " + sink.size()
                + " bytes with a 4-byte buffer; exact match = true");
    }

    @Test
    @DisplayName("one selector thread echoes two loopback clients, and each client receives its own exact message back")
    void selectorEchoesTwoClientsExactly() throws Exception {
        try (SelectorEchoDemo.Reactor reactor = new SelectorEchoDemo.Reactor()) {
            int port = reactor.port();
            String m1 = "transfer 250.00 to Dana";
            String m2 = "balance check account 42";
            String[] got = new String[2];
            CountDownLatch bothEchoed = new CountDownLatch(2);

            Thread c1 = new Thread(() -> {
                try {
                    got[0] = SelectorEchoDemo.sendAndReceive(port, m1);
                } catch (IOException e) {
                    got[0] = "ERROR: " + e;              // assertEquals below fails on the main thread
                } finally {
                    bothEchoed.countDown();
                }
            }, "client-1");
            Thread c2 = new Thread(() -> {
                try {
                    got[1] = SelectorEchoDemo.sendAndReceive(port, m2);
                } catch (IOException e) {
                    got[1] = "ERROR: " + e;              // assertEquals below fails on the main thread
                } finally {
                    bothEchoed.countDown();
                }
            }, "client-2");

            c1.start();
            c2.start();
            assertTrue(bothEchoed.await(5, TimeUnit.SECONDS), "both echoes arrived within the bound");
            c1.join(2000);
            c2.join(2000);

            assertEquals(m1, got[0], "client 1 received its own exact echo");
            assertEquals(m2, got[1], "client 2 received its own exact echo");
            System.out.println("one reactor thread echoed both clients exactly: \"" + got[0] + "\" | \"" + got[1] + "\"");
        }
    }

    @Test
    @DisplayName("Files.walk counts a planted three-level temp tree exactly: 3 directories and 3 files")
    void filesWalkCountsPlantedTreeExactly(@TempDir Path dir) throws IOException {
        // Plant exactly:
        //   dir/               (level 1 directory)
        //   dir/top.txt        (file)
        //   dir/a/             (level 2 directory)
        //   dir/a/mid.txt      (file)
        //   dir/a/b/           (level 3 directory)
        //   dir/a/b/deep.txt   (file)
        Files.createDirectories(dir.resolve("a/b"));
        Files.writeString(dir.resolve("top.txt"), "L1", StandardCharsets.UTF_8);
        Files.writeString(dir.resolve("a/mid.txt"), "L2", StandardCharsets.UTF_8);
        Files.writeString(dir.resolve("a/b/deep.txt"), "L3", StandardCharsets.UTF_8);

        long dirs;
        long files;
        long total;
        try (Stream<Path> walk = Files.walk(dir)) {
            List<Path> all = walk.toList();
            dirs = all.stream().filter(Files::isDirectory).count();
            files = all.stream().filter(Files::isRegularFile).count();
            total = all.size();
        }

        assertEquals(3, dirs, "root + a + b");
        assertEquals(3, files, "top.txt + mid.txt + deep.txt");
        assertEquals(6, total, "every path visited exactly once");
        System.out.println("Files.walk visited " + total + " paths: " + dirs + " directories, " + files + " files");
    }

    /** An AutoCloseable that records WHEN it closed and can be told to throw on close. */
    static final class Traced implements AutoCloseable {
        private final String name;
        private final List<String> log;
        private final boolean throwOnClose;

        Traced(String name, List<String> log, boolean throwOnClose) {
            this.name = name;
            this.log = log;
            this.throwOnClose = throwOnClose;
        }

        @Override
        public void close() throws IOException {
            log.add(name);
            if (throwOnClose) {
                throw new IOException("close failed for " + name);
            }
        }
    }
}
