package com.evaitcs.advanced.io;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.Comparator;
import java.util.stream.Stream;

/**
 * Demo 4 — THE BUFFER LIFECYCLE: the clarity centerpiece of this module.
 * (README section "Buffers: three pointers, one lifecycle".)
 *
 * A ByteBuffer is a fixed-size block of memory tracked by THREE pointers:
 *   capacity : how many elements it can EVER hold (fixed at allocation, never changes)
 *   position : the index of the NEXT element to read or write (moves as you go)
 *   limit    : the boundary you must not cross (its MEANING depends on the mode)
 *
 * The whole confusion around buffers collapses into ONE idea: a buffer is either in
 *   FILL mode  (writing; limit == capacity)   or
 *   DRAIN mode (reading; limit == how many bytes you wrote),
 * and flip() is the switch between them. clear() and compact() reset for the next fill.
 *
 * This demo PRINTS position/limit/capacity (and a little pointer ruler) at every single step so
 * you can watch the pointers move — then puts the lifecycle to work on a real FileChannel read
 * loop ("until read returns -1") and shows transferTo (zero-copy). Buffers and channels are
 * inseparable: a channel never hands you a byte[], it fills or drains a Buffer.
 *
 * All file work happens in a temp directory that is deleted on the way out (determinism).
 */
public class BufferWalkthroughDemo {

    public static void main(String[] args) throws Exception {
        Path dir = Files.createTempDirectory("securebank-buffers");
        try {
            banner("1. Three pointers on a fresh buffer (FILL mode)");
            ByteBuffer buf = ByteBuffer.allocate(8);
            show("allocate(8)", buf);
            System.out.println("      capacity=8 is fixed forever; position=0 is the next write index;");
            System.out.println("      limit=8 means 'you may write up to slot 8'. This is FILL mode.");

            banner("2. FILL — put 5 bytes; position chases every write");
            for (int i = 0; i < 5; i++) {
                buf.put((byte) ('A' + i));              // writes A B C D E
            }
            show("after put x5", buf);
            System.out.println("      position moved 0 -> 5 (it points at the NEXT free slot); limit stayed at 8.");

            banner("3. FLIP — the switch from FILL to DRAIN");
            buf.flip();
            show("after flip()", buf);
            System.out.println("      flip() did exactly TWO things: limit = old position (5), then position = 0.");
            System.out.println("      Now DRAIN mode: you may read slots [0, limit) = the 5 bytes you just wrote.");
            System.out.println("      Forgetting flip() is the #1 NIO bug: you would read [position, limit) = nothing.");

            banner("4. DRAIN — get 3 bytes; position advances toward limit");
            byte[] out = new byte[3];
            buf.get(out);
            show("after get x3", buf);
            System.out.println("      drained " + Arrays.toString(out) + " ; position=3, limit=5,");
            System.out.println("      remaining = limit - position = " + buf.remaining() + " (the bytes D, E are still to read).");
            System.out.println("      When position reaches limit, remaining()==0 and the buffer counts as 'empty'.");

            banner("5. CLEAR vs COMPACT — two ways to reset for the next fill");
            ByteBuffer cc = ByteBuffer.allocate(8);
            cc.put(new byte[]{'A', 'B', 'C', 'D', 'E'});
            cc.flip();                                  // DRAIN mode: position=0, limit=5
            cc.get(new byte[3]);                        // read A B C, leaving D E unread: position=3, limit=5
            show("before compact", cc);
            cc.compact();                               // slide the unread tail (D,E) to the front
            show("after compact()", cc);
            System.out.println("      compact() copied the UNREAD bytes (D,E) down to slots 0..1 and set position=2,");
            System.out.println("      limit=capacity, so the next put() appends after them. Use it to KEEP leftovers.");

            ByteBuffer cl = ByteBuffer.allocate(8);
            cl.put(new byte[]{'A', 'B', 'C'});
            cl.flip();
            show("before clear", cl);
            cl.clear();                                 // position=0, limit=capacity; bytes NOT erased
            show("after clear()", cl);
            System.out.println("      clear() reset position=0, limit=capacity. It does NOT zero the bytes; it just");
            System.out.println("      says 'ignore the old content, fill me from scratch'. Use it to START OVER.");

            banner("6. Heap vs direct buffers (one line each)");
            ByteBuffer heap = ByteBuffer.allocate(16);          // backed by a byte[] on the JVM heap
            ByteBuffer direct = ByteBuffer.allocateDirect(16);  // backed by native (off-heap) memory
            System.out.println("      heap   : " + heap.getClass().getSimpleName() + " (isDirect=" + heap.isDirect()
                    + ") — a JVM array; cheap; the garbage collector (GC) manages/moves it.");
            System.out.println("      direct : " + direct.getClass().getSimpleName() + " (isDirect=" + direct.isDirect()
                    + ") — native memory; the OS moves it via direct memory access (DMA), no extra heap copy.");
            System.out.println("      Rule: default to HEAP. Reach for DIRECT only for large, long-lived IO buffers");
            System.out.println("      (allocation and release are more expensive).");

            banner("7. The lifecycle at work: a FileChannel read loop 'until read returns -1'");
            Path f = dir.resolve("ledger.txt");
            byte[] data = "SecureBank ledger 2026".getBytes(StandardCharsets.UTF_8);
            try (FileChannel w = FileChannel.open(f,
                    StandardOpenOption.WRITE, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
                ByteBuffer src = ByteBuffer.wrap(data);
                while (src.hasRemaining()) {
                    w.write(src);                       // a channel DRAINS a buffer
                }
            }
            System.out.println("      wrote " + data.length + " bytes with FileChannel.write (buffer -> channel).");

            ByteBuffer small = ByteBuffer.allocate(8);  // small on purpose: forces several iterations
            ByteArrayOutputStream sink = new ByteArrayOutputStream();
            int iterations = 0;
            try (FileChannel r = FileChannel.open(f, StandardOpenOption.READ)) {
                int read;
                while ((read = r.read(small)) != -1) {  // -1 == end of file for a FILE channel
                    iterations++;
                    small.flip();                       // FILL -> DRAIN
                    while (small.hasRemaining()) {
                        sink.write(small.get());
                    }
                    small.clear();                      // DRAIN -> FILL, ready for the next read
                    if (iterations <= 2) {
                        System.out.println("      iteration " + iterations + ": read " + read + " bytes, then flip + drain + clear");
                    }
                }
            }
            String back = sink.toString(StandardCharsets.UTF_8);
            System.out.println("      loop ran " + iterations + " times; read back \"" + back + "\"");
            System.out.println("      exact match = " + back.equals(new String(data, StandardCharsets.UTF_8)));
            System.out.println("      Note: a FILE channel signals end-of-file with -1. A NON-BLOCKING SOCKET channel");
            System.out.println("      signals 'no data right now' with 0 — which is why the reactor uses a Selector (next demo).");

            banner("8. transferTo — the zero-copy shortcut");
            Path copy = dir.resolve("ledger-copy.txt");
            long transferred;
            try (FileChannel src = FileChannel.open(f, StandardOpenOption.READ);
                 FileChannel dst = FileChannel.open(copy,
                         StandardOpenOption.WRITE, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
                transferred = src.transferTo(0, src.size(), dst);   // kernel moves the bytes
            }
            System.out.println("      transferTo moved " + transferred + " bytes channel -> channel with NO user-space buffer.");
            System.out.println("      'Zero-copy': the OS (often via DMA/sendfile) does the move, so your JVM never copies");
            System.out.println("      the bytes into a byte[] and back. This is how fast static-file servers ship data.");

            banner("9. In one sentence");
            System.out.println("A buffer is in FILL mode or DRAIN mode, flip() switches between them, and clear()/compact()");
            System.out.println("reset for the next fill — watch position and limit and buffers stop being mysterious.");
        } finally {
            deleteRecursively(dir);
        }
    }

    /** Prints the three pointers plus a ruler marking where position and limit sit. */
    static void show(String step, ByteBuffer b) {
        System.out.println("      " + step + ": position=" + b.position()
                + " limit=" + b.limit() + " capacity=" + b.capacity());
        System.out.println("      [" + ruler(b) + "]  (P=position, L=limit, B=both, index 0.." + b.capacity() + ")");
    }

    /** A capacity+1 slot ruler; slot i shows P if i==position, L if i==limit, B if both. */
    static String ruler(ByteBuffer b) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i <= b.capacity(); i++) {
            boolean isPos = (i == b.position());
            boolean isLim = (i == b.limit());
            sb.append(isPos && isLim ? 'B' : isPos ? 'P' : isLim ? 'L' : '.');
        }
        return sb.toString();
    }

    static void banner(String title) {
        System.out.println();
        System.out.println("=== " + title + " ===");
    }

    /** Removes a whole temp tree (files and directories), deepest first. Determinism: no leftovers. */
    static void deleteRecursively(Path root) throws IOException {
        if (!Files.exists(root)) {
            return;
        }
        try (Stream<Path> walk = Files.walk(root)) {
            walk.sorted(Comparator.reverseOrder()).forEach(p -> {
                try {
                    Files.deleteIfExists(p);
                } catch (IOException ignored) {
                    // best effort cleanup of a temp dir we own
                }
            });
        }
    }
}
