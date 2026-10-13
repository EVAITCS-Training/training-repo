package com.evaitcs.advanced.io;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/**
 * Demo 6 — FILES & PATH: the modern API you will actually use day to day.
 * (README section "Files and Path: the API you will actually use".)
 *
 * Everything so far was machinery. In real application code you rarely open a FileChannel by
 * hand — you call java.nio.file.Files, which wraps that machinery in one-liners. This demo plants
 * a small three-level directory tree in a temp folder and then:
 *   - Files.walk  — visits every path depth-first (count directories and files),
 *   - Files.readString / writeString — whole-file text in one call (with an explicit charset),
 *   - Files.lines(...).filter(...)   — stream a file lazily instead of loading it all.
 *
 * Path replaces the old java.io.File: it is immutable, understands a filesystem, and composes with
 * resolve()/relativize(). This is the day-to-day face of NIO.
 *
 * All work happens in a temp directory that is deleted on the way out (determinism).
 */
public class FilesPathDemo {

    public static void main(String[] args) throws Exception {
        Path root = Files.createTempDirectory("securebank-tree");
        try {
            banner("1. Plant a three-level tree with Path + Files");
            Files.createDirectories(root.resolve("a/b"));                       // make all levels at once
            Files.writeString(root.resolve("top.txt"), "root note", StandardCharsets.UTF_8);
            Files.writeString(root.resolve("a/mid.txt"), "level two", StandardCharsets.UTF_8);
            Files.writeString(root.resolve("a/b/deep.txt"), "level three", StandardCharsets.UTF_8);
            System.out.println("  planted:  top.txt (level 1), a/mid.txt (level 2), a/b/deep.txt (level 3)");
            System.out.println("  Path is immutable and composes: root.resolve(\"a/b\") builds a child path without touching disk");

            banner("2. Files.walk visits every path depth-first");
            try (Stream<Path> walk = Files.walk(root)) {
                List<Path> all = walk.sorted().toList();                        // sort for stable printing
                long dirs = all.stream().filter(Files::isDirectory).count();
                long files = all.stream().filter(Files::isRegularFile).count();
                System.out.println("  walked " + all.size() + " paths total: " + dirs + " directories, " + files + " files");
                for (Path p : all) {
                    Path relative = root.relativize(p);                         // show paths relative to root
                    System.out.println("    " + (relative.toString().isEmpty() ? "(root)" : relative));
                }
                System.out.println("  verdict: walk() is a lazy Stream<Path>; close it (try-with-resources) because it");
                System.out.println("          holds open directory handles while it traverses.");
            }

            banner("3. readString / writeString: whole-file text in one call");
            Path note = root.resolve("note.txt");
            Files.writeString(note, "Grüße from SecureBank\nbalance 2450.00\n", StandardCharsets.UTF_8);
            String content = Files.readString(note, StandardCharsets.UTF_8);
            System.out.println("  wrote then read back (newlines shown as \\n): \"" + content.replace("\n", "\\n") + "\"");
            System.out.println("  verdict: one line to write, one line to read — but you still NAME the charset (demo 2).");

            banner("4. Files.lines(...).filter(...): stream the file, do not load it all");
            try (Stream<String> lines = Files.lines(note, StandardCharsets.UTF_8)) {
                List<String> money = lines.filter(line -> line.contains("balance")).toList();
                System.out.println("  lines().filter(contains 'balance').toList() = " + money);
            }
            System.out.println("  verdict: lines() is lazy — it reads as the stream is consumed, so a huge file never");
            System.out.println("          has to fit in memory. Close it too (it keeps the file open while streaming).");

            banner("5. In one sentence");
            System.out.println("  Files and Path are the day-to-day face of NIO: walk a tree, readString/writeString a file,");
            System.out.println("  and stream its lines — all one-liners over the channels and buffers from the demos above.");
        } finally {
            deleteRecursively(root);
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
