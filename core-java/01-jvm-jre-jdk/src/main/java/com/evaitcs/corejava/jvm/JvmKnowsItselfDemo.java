package com.evaitcs.corejava.jvm;

import java.io.File;
import java.io.IOException;

/**
 * Demo 2 — the nested boxes: JDK (contains) JRE (contains) JVM.
 *
 *   JDK = the tools to BUILD (javac, javap, jar, jdb ...) + everything in the JRE
 *   JRE = the launcher to RUN (java) + the core libraries (java.lang, java.util, ...) + the JVM
 *   JVM = the engine that executes bytecode (class loader, interpreter, JIT, GC, memory areas)
 *
 * This demo prints "everything the JVM knows about itself" from System properties,
 * then proves which box this machine actually has installed.
 */
public class JvmKnowsItselfDemo {

    public static void main(String[] args) {

        banner("1. Everything the JVM knows about itself (System.getProperties)");

        // Each property is one line of the JVM's self-description. Read the comment aloud
        // while pointing at the value — together they ARE the nested-boxes diagram.
        print("java.version",      "which Java release this JRE implements");
        print("java.vendor",       "who built this JVM (Oracle, Amazon, Eclipse, ...)");
        print("java.home",         "where the install lives on disk — the OUTERMOST box");
        print("java.vm.name",      "the engine itself — the innermost box");
        print("java.vm.version",   "the JVM's own version (often NOT the same string as java.version)");
        print("os.name",           "the host OS — the bytecode is identical everywhere, the JVM port is not");
        print("os.arch",           "the host CPU architecture — again: the JVM's problem, not yours");

        // Under `mvnw exec:java` this shows MAVEN's classpath — the Maven process IS our JVM.
        // Still worth showing: java.class.path is where the class loader hunts for .class files.
        String cp = System.getProperty("java.class.path", "");
        System.out.println("  java.class.path   = "
                + (cp.length() > 90 ? cp.substring(0, 90) + " ...(" + cp.length() + " chars)" : cp));
        System.out.println("                        ^ where class files are searched for (truncated for the projector)");

        banner("2. Proof of which box this machine has: the TOOLS in java.home/bin");

        // Since Java 9 there is no separate jre/ folder — java.home IS the JDK.
        // The tools below are the difference between the boxes: `java` alone is the JRE story;
        // javac/javap/jar/jdb are the JDK extras. This is the README diagram, verified live.
        File bin = new File(System.getProperty("java.home"), "bin");
        String[] tools = { "java", "javac", "javap", "jar", "jdb" };
        for (int i = 0; i < tools.length; i++) {
            String role = toolRole(tools[i]);
            System.out.printf("  %-6s %-9s %s%n", tools[i], exists(bin, tools[i]) ? "[FOUND]" : "[absent]", role);
        }
        System.out.println("  All FOUND => this machine has a full JDK. A JRE-only install lacks the middle four.");

        banner("3. 'Write once, run anywhere' — the version stamp inside every .class file");

        // Same .class file, any OS, any CPU — but only if the JVM is NEW ENOUGH for it.
        // Every .class carries a major version; the JVM refuses files above what it supports.
        File classFile = CompileRunPipelineDemo.findOwnClassFile(BankApp.class);
        if (classFile == null) {
            System.out.println("  BankApp.class not found on disk — compile first (mvnw compile).");
        } else {
            try {
                int major = CompileRunPipelineDemo.readClassHeader(classFile)[2];
                System.out.println("  BankApp.class major version = " + major + "  (52 = Java 8; the parent pom compiles with --release 8)");
                System.out.println("  this JVM accepts class files up to version " + System.getProperty("java.class.version"));
                System.out.println("  RULE: a class file runs on any JVM whose limit is >= the file's major version.");
                System.out.println("  That one rule is the whole 'run anywhere' promise: bytecode is portable, versions gate it.");
            } catch (IOException e) {
                System.out.println("  could not read " + classFile + " : " + e.getMessage());
            }
        }
    }

    private static void print(String key, String why) {
        System.out.println("  " + key + " = " + System.getProperty(key));
        System.out.println("      ^ " + why);
    }

    private static String toolRole(String tool) {
        if (tool.equals("java"))  return "the launcher — starts a JVM and runs a class (the JRE's face)";
        if (tool.equals("javac")) return "the compiler — .java -> .class (JDK only)";
        if (tool.equals("javap")) return "the disassembler — shows the bytecode inside a .class (JDK only)";
        if (tool.equals("jar"))   return "packager — bundles .class files into one .jar (JDK only)";
        return "the debugger (JDK only)";
    }

    /** Tools are plain names on Linux/Mac but carry .exe on Windows — check both. */
    private static boolean exists(File bin, String tool) {
        return new File(bin, tool).isFile() || new File(bin, tool + ".exe").isFile();
    }

    private static void banner(String title) {
        System.out.println();
        System.out.println("=== " + title + " ===");
    }
}
