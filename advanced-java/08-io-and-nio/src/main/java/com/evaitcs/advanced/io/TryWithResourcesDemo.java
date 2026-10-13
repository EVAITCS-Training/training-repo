package com.evaitcs.advanced.io;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/**
 * Demo 3 — RESOURCE SAFETY: try-with-resources, reverse close order, suppressed exceptions,
 * and the serialization trap. (README section "Resource safety and the serialization trap".)
 *
 * Claim 1: try-with-resources closes every AutoCloseable for you, in REVERSE declaration order.
 *          Reverse order matters: the outermost decorator (which may depend on the inner one)
 *          closes first — exactly how the stream nesting from Demo 1 unwinds.
 * Claim 2: if the try BODY throws AND a close() also throws, the body's exception WINS and the
 *          close() exception is attached via addSuppressed — nothing is lost. We prove it by
 *          reading getSuppressed().
 * Claim 3: Java serialization is one more stream decorator (ObjectOutputStream wraps an
 *          OutputStream) — and the one you should NOT use on untrusted input. We round-trip a
 *          record to show how clean the modern form is, then narrate the two classic pitfalls.
 *
 * All file work happens in a temp directory that is deleted on the way out (determinism).
 */
public class TryWithResourcesDemo {

    public static void main(String[] args) throws Exception {
        Path dir = Files.createTempDirectory("securebank-resources");
        try {
            banner("1. Reverse close order: last opened, first closed");
            List<String> closeOrder = new ArrayList<>();
            try (Traced a = new Traced("A", closeOrder, false);
                 Traced b = new Traced("B", closeOrder, false);
                 Traced c = new Traced("C", closeOrder, false)) {
                System.out.println("step 1: opened A, then B, then C (declaration order)");
            }
            System.out.println("step 2: after the block, close order = " + closeOrder);
            System.out.println("verdict: try-with-resources closed them C, B, A — REVERSE of declaration, so a");
            System.out.println("        decorator that wraps another is always closed before the stream it wraps.");

            banner("2. Suppressed exceptions: the body wins, close() is not lost");
            List<String> log = new ArrayList<>();
            try {
                try (Traced r = new Traced("R", log, true)) {   // this close() will THROW
                    System.out.println("step 1: the try body throws FIRST (the primary failure)");
                    throw new IllegalStateException("primary body failure");
                }
            } catch (IllegalStateException primary) {
                Throwable[] suppressed = primary.getSuppressed();
                System.out.println("step 2: CAUGHT -> " + primary.getClass().getSimpleName() + ": " + primary.getMessage());
                System.out.println("step 3: primary.getSuppressed().length = " + suppressed.length);
                for (Throwable s : suppressed) {
                    System.out.println("        suppressed -> " + s.getClass().getSimpleName() + ": " + s.getMessage());
                }
                System.out.println("verdict: the body's exception is the one thrown; the failing close() is ATTACHED");
                System.out.println("        as suppressed, so you still see both. Nothing is silently swallowed.");
            }

            banner("3. Serialization: the stream decorator you should NOT trust");
            Path ser = dir.resolve("txn.ser");
            Transaction original = new Transaction("T-1040", "Payroll", new BigDecimal("2450.00"));

            // ObjectOutputStream / ObjectInputStream are decorators over a byte stream — same
            // pattern as Demo 1 — and both MUST be closed, so try-with-resources fits perfectly.
            try (ObjectOutputStream oos = new ObjectOutputStream(
                    new BufferedOutputStream(new FileOutputStream(ser.toFile())))) {
                oos.writeObject(original);
            }
            Transaction restored;
            try (ObjectInputStream ois = new ObjectInputStream(
                    new BufferedInputStream(new FileInputStream(ser.toFile())))) {
                restored = (Transaction) ois.readObject();
            }
            System.out.println("step 1: wrote     " + original);
            System.out.println("step 2: read back " + restored);
            System.out.println("step 3: round-trip equal? " + original.equals(restored));
            System.out.println("step 4: a RECORD implements Serializable by design — deserialization runs the");
            System.out.println("        canonical constructor (invariants re-checked) and there are no settable fields.");
            System.out.println();
            System.out.println("Pitfall A — serialVersionUID: a CLASSIC Serializable class without an explicit");
            System.out.println("  'private static final long serialVersionUID = 1L;' gets one generated from its shape.");
            System.out.println("  Add or reorder a field and the generated id changes, so old bytes throw");
            System.out.println("  InvalidClassException on read. Pin the id yourself, or migrate to a record/JSON.");
            System.out.println();
            System.out.println("Pitfall B — SECURITY (why the industry moved off Java serialization): readObject()");
            System.out.println("  constructs WHATEVER class the incoming bytes name. Attackers send a 'gadget chain' —");
            System.out.println("  ordinary-looking library objects whose readObject/readResolve run code — and get remote");
            System.out.println("  code execution. RULE: never deserialize bytes you do not trust. Use JSON/Protobuf, or a");
            System.out.println("  record plus an explicit parser that reads fields, not one that instantiates arbitrary types.");

            banner("4. In one sentence");
            System.out.println("try-with-resources closes in reverse order and keeps a failing close() as a suppressed");
            System.out.println("exception — and the safest thing to serialize today is a record, never untrusted bytes.");
        } finally {
            deleteRecursively(dir);
        }
    }

    /**
     * An AutoCloseable that records WHEN it was closed and can be told to throw on close.
     * This is the instrument that makes reverse order and suppression visible.
     */
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
            log.add(name);                       // record the close, in the order it happens
            if (throwOnClose) {
                throw new IOException("close failed for " + name);
            }
        }
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
