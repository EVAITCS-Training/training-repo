package com.evaitcs.corejava.jvm;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.CodeSource;

/**
 * Demo 1 — the pipeline: .java source -> javac -> .class bytecode -> JVM executes.
 *
 * Everything printed here is EVIDENCE gathered from inside the running program itself:
 * where its .class files came from, and what the first bytes of those files are.
 * Students believe the pipeline a lot more once they have seen CAFEBABE with their own eyes.
 */
public class CompileRunPipelineDemo {

    public static void main(String[] args) {

        banner("1. The story (what had to happen BEFORE this program could print)");

        // Point at these lines in order — this is the README's pipeline diagram, as text.
        System.out.println("  BankApp.java     you wrote TEXT (source code). The CPU cannot run text.");
        System.out.println("      |  javac     the JDK's compiler translated it ONCE, ahead of time.");
        System.out.println("      v");
        System.out.println("  BankApp.class    BYTECODE: instructions for an IMAGINARY machine (the 'Java Virtual Machine').");
        System.out.println("      |  java      the JRE's launcher started a JVM and handed it the class.");
        System.out.println("      v");
        System.out.println("  JVM              executes the bytecode: interprets it, JIT-compiles the hot paths (Demo 3).");
        System.out.println();
        System.out.println("  Bytecode is NOT machine code for your CPU. It is a portable instruction set,");
        System.out.println("  identical on every OS — which is exactly why one .class file runs anywhere.");

        banner("2. Proof from INSIDE the running program: where am I running from?");

        // The JVM knows where each class's bytes came from — we just ask it.
        // Students often ask "is it really running the thing I compiled?" — this answers it.
        File classesDir = classesDirOf(BankApp.class);
        if (classesDir == null) {
            System.out.println("  Hmm — could not resolve a code source directory for BankApp.");
            System.out.println("  Run me through Maven (.\\mvnw.cmd -pl 01-jvm-jre-jdk compile exec:java)");
            System.out.println("  and I will be able to point at target/classes.");
        } else {
            System.out.println("  Code source of BankApp.class : " + classesDir);
            System.out.println("  <- that is javac's OUTPUT folder. Notice: the JVM never sees your .java text.");
        }

        banner("3. Proof: the first 4 bytes of BankApp.class are CAFEBABE");

        File classFile = findOwnClassFile(BankApp.class);
        if (classFile == null) {
            System.out.println("  BankApp.class not found on disk — did the compile step run?");
            System.out.println("  (I resolve it from my code source, so this only works in a Maven/IDE build layout.)");
        } else {
            try {
                int[] header = readClassHeader(classFile);
                System.out.println("  file          : " + classFile.getPath());
                System.out.printf("  first 4 bytes : %08X  <- the MAGIC NUMBER. Every .class file starts with it.%n", header[0]);
                System.out.printf("  next 4 bytes  : minor=%d major=%d  <- the class file VERSION (Demo 2 explains why it matters).%n",
                        header[1], header[2]);
                System.out.println("  If a file does not start with CAFEBABE, the JVM refuses it: 'not a class file'.");
            } catch (IOException e) {
                System.out.println("  could not read " + classFile + " : " + e.getMessage());
            }
        }

        banner("4. And now the JVM executes those very bytes");

        // main() is just a static method — the JVM's entry call looks exactly like ours.
        BankApp.main(new String[0]);
        System.out.println("  ^ that line came from BankApp.class bytecode, not from source text.");
    }

    // ---------------------------------------------------------------------
    // Helpers — package-private so the JUnit tests can reuse the SAME proof code.
    // ---------------------------------------------------------------------

    /** The directory (or jar) this class's bytes were loaded from, or null if unknown. */
    static File classesDirOf(Class<?> c) {
        try {
            CodeSource source = c.getProtectionDomain().getCodeSource();
            if (source == null) {
                return null; // e.g. a class from the JVM's own core image
            }
            return new File(source.getLocation().toURI());
        } catch (Exception e) {
            return null; // running outside a normal build layout — caller guards for this
        }
    }

    /** This class's own .class file on disk, resolved from its code source. Null if not there. */
    static File findOwnClassFile(Class<?> c) {
        File dir = classesDirOf(c);
        if (dir == null) {
            return null;
        }
        // getName() is "com.evaitcs.corejava.jvm.BankApp"; on disk the dots are folders.
        File classFile = new File(dir, c.getName().replace('.', '/') + ".class");
        return classFile.isFile() ? classFile : null;
    }

    /** Reads the 8-byte class file header: magic (4 bytes), minor (2), major (2). */
    static int[] readClassHeader(File classFile) throws IOException {
        DataInputStream in = new DataInputStream(new FileInputStream(classFile));
        try {
            int magic = in.readInt();            // big-endian, as the class file format demands
            int minor = in.readUnsignedShort();
            int major = in.readUnsignedShort();
            return new int[] { magic, minor, major };
        } finally {
            in.close();
        }
    }

    private static void banner(String title) {
        System.out.println();
        System.out.println("=== " + title + " ===");
    }
}
