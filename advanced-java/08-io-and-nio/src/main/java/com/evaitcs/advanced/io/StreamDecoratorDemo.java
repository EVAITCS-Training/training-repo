package com.evaitcs.advanced.io;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Scanner;
import java.util.stream.Stream;

/**
 * Demo 1 — CLASSIC IO (java.io): two families of streams, and the Decorator pattern.
 * (README sections "Two families of streams" and "The Decorator pattern, literally".)
 *
 * Claim 1: java.io has TWO parallel families because a file/network is BYTES but a program
 * wants CHARACTERS, and the bridge between them is a charset.
 *   - InputStream / OutputStream  move raw 8-bit bytes. They know nothing about text.
 *   - Reader / Writer             move characters. They ALWAYS carry a charset.
 *   - InputStreamReader / OutputStreamWriter are the adapters that turn one into the other.
 *
 * Claim 2: the classic nesting  FileInputStream -> BufferedInputStream -> DataInputStream  is
 * the DECORATOR pattern literally (module 09). Every layer IS-A stream and ADDS one capability,
 * composed at runtime instead of by an explosion of subclasses.
 *
 * Claim 3: reading text has a modern one-liner — BufferedReader.lines() — and an older tokenizer,
 * Scanner. Both are decorators over the same underlying bytes.
 *
 * All file work happens in a temp directory that is deleted on the way out (determinism).
 */
public class StreamDecoratorDemo {

    public static void main(String[] args) throws Exception {
        Path dir = Files.createTempDirectory("securebank-streams");
        try {
            banner("1. Two families: BYTES are truth, CHARACTERS need a charset");

            // BYTE family — moves raw bytes, no text meaning. This is ground truth on disk/wire.
            Path raw = dir.resolve("raw.bin");
            try (OutputStream out = new FileOutputStream(raw.toFile())) {
                out.write(new byte[]{1, 2, 3, 127});
            }
            byte[] rawBytes = Files.readAllBytes(raw);
            System.out.println("step 1: a byte stream (OutputStream) moved " + rawBytes.length
                    + " raw bytes: " + Arrays.toString(rawBytes));
            System.out.println("        InputStream/OutputStream = BYTES. No charset, no text meaning.");

            // CHAR family — moves characters; the Writer/Reader carries the charset that maps char<->byte.
            Path memo = dir.resolve("memo.txt");
            try (Writer w = new OutputStreamWriter(new FileOutputStream(memo.toFile()), StandardCharsets.UTF_8)) {
                w.write("pay Dana");                    // chars encoded to UTF-8 bytes on the way out
            }
            StringBuilder text = new StringBuilder();
            try (Reader r = new InputStreamReader(new FileInputStream(memo.toFile()), StandardCharsets.UTF_8)) {
                int c;
                while ((c = r.read()) != -1) {
                    text.append((char) c);              // bytes decoded back to chars on the way in
                }
            }
            System.out.println("step 2: a char stream (Writer/Reader) moved text: \"" + text + "\"");
            System.out.println("        Reader/Writer = CHARACTERS. InputStreamReader is the BRIDGE: it");
            System.out.println("        wraps a byte stream and decodes it WITH a charset.");
            System.out.println("verdict: two families exist because storage is bytes but programs want text —");
            System.out.println("        the charset is the contract between them (the next demo breaks it on purpose).");

            banner("2. The Decorator pattern, literally: stacking capabilities at runtime");

            Path bin = dir.resolve("txn.dat");
            Transaction original = new Transaction("T-1040", "Payroll", new BigDecimal("2450.00"));

            // WRITE side: FileOutputStream -> BufferedOutputStream -> DataOutputStream.
            try (DataOutputStream out = new DataOutputStream(
                    new BufferedOutputStream(
                            new FileOutputStream(bin.toFile())))) {
                out.writeUTF(original.id());
                out.writeUTF(original.description());
                out.writeUTF(original.amount().toPlainString());   // money travels as its exact string
            }

            // READ side: FileInputStream -> BufferedInputStream -> DataInputStream.
            try (DataInputStream in = new DataInputStream(
                    new BufferedInputStream(
                            new FileInputStream(bin.toFile())))) {
                String id = in.readUTF();
                String description = in.readUTF();
                BigDecimal amount = new BigDecimal(in.readUTF());
                Transaction restored = new Transaction(id, description, amount);

                System.out.println("step 1: FileInputStream     — raw bytes from the OS (does ONE job)");
                System.out.println("step 2: BufferedInputStream — adds a buffer, so fewer syscalls (adds ONE capability)");
                System.out.println("step 3: DataInputStream     — adds readUTF/readInt, so typed values (adds ONE capability)");
                System.out.println("        outer layer runtime type = " + in.getClass().getName());
                System.out.println("        wrote     : " + original);
                System.out.println("        read back : " + restored);
                System.out.println("        round-trip equal? " + original.equals(restored));
                System.out.println("verdict: every layer IS-A InputStream and ADDS behavior. That is the Decorator");
                System.out.println("        pattern (module 09) — composition at runtime, not a subclass per combination.");
            }

            banner("3. Reading text two ways: BufferedReader.lines() vs Scanner");

            Path log = dir.resolve("audit.log");
            Files.write(log, List.of(
                    "T-1040 PAYROLL 2450.00",
                    "T-1041 FEE 3.50",
                    "T-1042 REFUND 12.00"), StandardCharsets.UTF_8);

            // Modern one-liner: BufferedReader.lines() is a lazy Stream<String> (java8-features 04).
            try (BufferedReader br = Files.newBufferedReader(log, StandardCharsets.UTF_8)) {
                long payrollLines = br.lines().filter(line -> line.contains("PAYROLL")).count();
                System.out.println("BufferedReader.lines().filter(...).count() = " + payrollLines + " payroll line(s)");
            }

            // Older tokenizer: Scanner splits the SAME bytes into typed tokens.
            try (Scanner sc = new Scanner(log.toFile(), StandardCharsets.UTF_8.name())) {
                BigDecimal total = BigDecimal.ZERO;
                while (sc.hasNext()) {
                    String id = sc.next();               // token 1: id
                    String kind = sc.next();             // token 2: kind
                    BigDecimal amount = new BigDecimal(sc.next());   // token 3: amount (locale-proof)
                    total = total.add(amount);
                }
                System.out.println("Scanner tokenized id/kind/amount and summed = "
                        + total.setScale(2, RoundingMode.HALF_UP).toPlainString());
            }
            System.out.println("verdict: lines() is the modern one-liner for TEXT; Scanner TOKENIZES mixed types.");
            System.out.println("        Both wrap an underlying stream — decorators again, all the way down.");

            banner("4. In one sentence");
            System.out.println("Classic IO is byte streams plus char streams bridged by a charset, and every");
            System.out.println("useful stream is a decorator stacked on a raw one — compose capabilities at runtime.");
        } finally {
            deleteRecursively(dir);
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
