package com.evaitcs.advanced.io;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Stream;

/**
 * Demo 2 — CHARSETS AND MOJIBAKE: the demo every student remembers.
 * (README section "Charsets: bytes are truth, the decode is a contract".)
 *
 * Claim: a file (or socket) holds BYTES. Turning bytes into text needs a CHARSET, which is the
 * decode contract. The SAME bytes decoded with the WRONG charset produce mojibake (garbled text);
 * decoded with the right one, they reproduce the original exactly. The bytes never changed —
 * only the contract did.
 *
 * We write "Grüße, SecureBank" as UTF-8, then read it back as windows-1252 (mojibake) and as
 * UTF-8 (correct). We also print Charset.defaultCharset() to explain WHY omitting the charset was
 * the classic cross-platform bug (and why Java 18 / JEP 400 fixed the default to UTF-8).
 *
 * All file work happens in a temp directory that is deleted on the way out (determinism).
 */
public class CharsetMojibakeDemo {

    public static void main(String[] args) throws Exception {
        Path dir = Files.createTempDirectory("securebank-charset");
        try {
            String original = "Grüße, SecureBank";

            banner("1. Bytes are the truth; a charset is the decode contract");
            byte[] utf8 = original.getBytes(StandardCharsets.UTF_8);
            System.out.println("step 1: original text       = \"" + original + "\"  (" + original.length() + " chars)");
            System.out.println("step 2: encoded as UTF-8    = " + utf8.length + " bytes  [" + hex(utf8) + "]");
            System.out.println("        'ü' and 'ß' are 2 bytes each in UTF-8, so bytes > chars. That gap is where");
            System.out.println("        mojibake is born: a decoder that assumes 1 byte = 1 char reads it wrong.");

            banner("2. Same bytes, WRONG charset -> mojibake; RIGHT charset -> exact");
            String asWindows1252 = new String(utf8, Charset.forName("windows-1252"));
            String asUtf8 = new String(utf8, StandardCharsets.UTF_8);
            System.out.println("step 1: decode the UTF-8 bytes AS windows-1252 = \"" + asWindows1252 + "\"");
            System.out.println("step 2: equals original? " + asWindows1252.equals(original) + "  -> MOJIBAKE (garbled)");
            System.out.println("step 3: decode the SAME bytes AS UTF-8         = \"" + asUtf8 + "\"");
            System.out.println("step 4: equals original? " + asUtf8.equals(original) + "  -> exact round-trip");
            System.out.println("verdict: the bytes were identical both times. Only the decode contract changed.");
            System.out.println("        Wrong charset in, wrong text out — the file was never corrupted.");

            banner("3. The same bug at file level (write UTF-8, read windows-1252)");
            Path greeting = dir.resolve("greeting.txt");
            Files.writeString(greeting, original, StandardCharsets.UTF_8);            // write UTF-8 bytes
            String wrong = Files.readString(greeting, Charset.forName("windows-1252")); // read as 1252
            String right = Files.readString(greeting, StandardCharsets.UTF_8);          // read as UTF-8
            System.out.println("Files.readString(path, windows-1252) = \"" + wrong + "\"   <- mojibake");
            System.out.println("Files.readString(path, UTF-8)        = \"" + right + "\"   <- correct");
            System.out.println("verdict: the fix is not 'repair the file'. It is 'name the right charset when you read'.");

            banner("4. Why this bites in real life: the DEFAULT charset");
            System.out.println("Charset.defaultCharset()          = " + Charset.defaultCharset());
            System.out.println("System.getProperty(file.encoding) = " + System.getProperty("file.encoding"));
            System.out.println("Since Java 18 (JEP 400) the default is UTF-8 on every platform. Before that it was the");
            System.out.println("platform charset (windows-1252 here on Windows), so code that OMITTED the charset worked");
            System.out.println("on the developer's laptop and produced mojibake on a different-locale server.");
            System.out.println("Rule: ALWAYS pass an explicit Charset. Never let a Reader/Writer pick the default for you.");

            banner("5. In one sentence");
            System.out.println("Bytes are truth and a charset is the contract that turns them into text — the same bytes");
            System.out.println("read with the wrong contract give you mojibake, so name the charset every single time.");
        } finally {
            deleteRecursively(dir);
        }
    }

    /** Formats bytes as space-separated uppercase hex so the raw truth is visible on screen. */
    static String hex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X ", b));
        }
        return sb.toString().trim();
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
