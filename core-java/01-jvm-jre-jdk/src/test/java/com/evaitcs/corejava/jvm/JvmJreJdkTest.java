package com.evaitcs.corejava.jvm;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Tests as PROOF for the claims made in the demos. Run with:
 *   .\mvnw.cmd -pl 01-jvm-jre-jdk test
 * Each test prints the evidence it asserts, so the test run doubles as a demo.
 *
 * Everything here is DETERMINISTIC: the only thing we never assert on is TIMING —
 * the JIT warmup story stays print-only, because wall-clock numbers are not reproducible.
 */
class JvmJreJdkTest {

    @Test
    @DisplayName("System properties exist: the JVM can describe itself (Demo 2)")
    void jvmKnowsItself() {
        String[] keys = { "java.home", "java.version", "java.vendor", "os.name", "os.arch" };
        for (int i = 0; i < keys.length; i++) {
            String value = System.getProperty(keys[i]);
            System.out.println("  " + keys[i] + " = " + value);
            assertNotNull(value, keys[i] + " must exist on any JVM");
            assertTrue(value.length() > 0, keys[i] + " must not be empty");
        }
    }

    @Test
    @DisplayName("we run from target/classes — javac's output, never the .java source (Demo 1)")
    void runningFromTargetClasses() {
        File dir = CompileRunPipelineDemo.classesDirOf(BankApp.class);
        assertNotNull(dir, "under Maven, a class always knows its code source");
        String path = dir.getAbsolutePath().replace('\\', '/');
        System.out.println("  BankApp's code source = " + dir);
        assertTrue(path.contains("/target/"), "Maven compiles into target/: " + path);
        assertTrue(path.endsWith("/classes"), "main classes live in target/classes: " + path);
    }

    @Test
    @DisplayName("BankApp.class starts with the magic number CAFEBABE (Demo 1)")
    void classFileStartsWithCafeBabe() throws Exception {
        File classFile = CompileRunPipelineDemo.findOwnClassFile(BankApp.class);
        assertNotNull(classFile, "BankApp.class must exist under target/classes — `compile` runs before `test`");
        int[] header = CompileRunPipelineDemo.readClassHeader(classFile);
        System.out.printf("  %s starts with %08X (minor=%d, major=%d)%n",
                classFile.getName(), header[0], header[1], header[2]);
        assertEquals(0xCAFEBABE, header[0], "every .class file starts with the magic number");
    }

    @Test
    @DisplayName("class file version: we compile for Java 8 (major 52) and this JVM accepts it (Demo 2)")
    void classFileVersionIsSupported() throws Exception {
        File classFile = CompileRunPipelineDemo.findOwnClassFile(BankApp.class);
        assertNotNull(classFile, "BankApp.class must exist under target/classes");
        int major = CompileRunPipelineDemo.readClassHeader(classFile)[2];
        float jvmLimit = Float.parseFloat(System.getProperty("java.class.version"));
        System.out.println("  BankApp.class major version = " + major
                + "  (52 = Java 8; the parent pom sets --release 8)");
        System.out.println("  this JVM accepts class files up to version " + jvmLimit);
        assertEquals(52, major, "the parent pom compiles every module with --release 8");
        assertTrue(major <= jvmLimit, "a class file only runs where its major <= the JVM's limit");
    }

    @Test
    @DisplayName("the stack is FINITE: unbounded recursion throws a CATCHABLE StackOverflowError (Demo 4)")
    void stackIsFiniteAndOverflowIsCatchable() {
        depth = 0;
        try {
            recurseForever();
            fail("unbounded recursion must never return normally");
        } catch (StackOverflowError e) {
            // Catching it is legal — and by the time we are here, the overflowed frames are
            // already popped, so the very next method call proves the stack works again.
            System.out.println("  caught " + e.getClass().getSimpleName() + " after " + depth + " frames");
            System.out.println("  -> every call added one frame; frames ran out; the JVM said ENOUGH.");
            assertTrue(depth > 100, "should get many frames deep before overflowing, got " + depth);
        }
        assertEquals(4, add(2, 2), "the stack is usable again immediately after the catch");
    }

    private int depth;

    private void recurseForever() {
        depth++;
        recurseForever(); // no base case — the overflow IS the point
    }

    private int add(int a, int b) {
        return a + b;
    }

    @Test
    @DisplayName("Runtime memory numbers are sane, and a live 10 MB array shows up in `used` (Demo 4)")
    void heapNumbersAreSane() {
        Runtime rt = Runtime.getRuntime();
        long max = rt.maxMemory();
        long total = rt.totalMemory();
        long free = rt.freeMemory();
        System.out.println("  max=" + MemoryAreasDemo.mb(max)
                + "  committed=" + MemoryAreasDemo.mb(total)
                + "  free=" + MemoryAreasDemo.mb(free)
                + "  used=" + MemoryAreasDemo.mb(total - free));

        assertTrue(max > 0, "max heap must be positive");
        assertTrue(total <= max, "committed (" + total + ") never exceeds max (" + max + ")");
        assertTrue(free <= total, "free space lives inside the committed region");

        // `blob` is a LIVE local reference, so no collection can shrink `used` below its size —
        // that is what makes this assertion deterministic (unlike anything measured after gc()).
        byte[] blob = new byte[10 * 1024 * 1024];
        long usedWithBlob = rt.totalMemory() - rt.freeMemory();
        System.out.println("  after new byte[10 MB] (still referenced): used = " + MemoryAreasDemo.mb(usedWithBlob));
        assertEquals(10 * 1024 * 1024, blob.length);
        assertTrue(usedWithBlob >= 9 * 1024 * 1024,
                "a live 10 MB array must be visible in used=" + MemoryAreasDemo.mb(usedWithBlob));
    }

    @Test
    @DisplayName("a class initializes ONCE, on FIRST active use — not before, not twice (Demo 5)")
    void classesInitializeOnFirstUseExactlyOnce() {
        // Reading these flags touches ClassLoadingDemo (already initialized) but NOT LazyVault:
        assertFalse(ClassLoadingDemo.vaultInitialized, "nothing has touched LazyVault yet in the tests");
        assertEquals(0, ClassLoadingDemo.initializerRuns, "static initializer must not have run yet");

        String first = ClassLoadingDemo.LazyVault.open();   // FIRST active use -> triggers <clinit>
        System.out.println("  first touch returned : " + first
                + "  (vaultInitialized=" + ClassLoadingDemo.vaultInitialized + ")");
        assertTrue(ClassLoadingDemo.vaultInitialized, "first use must have initialized the class");

        ClassLoadingDemo.LazyVault.open();                   // second use -> nothing re-runs
        assertEquals(1, ClassLoadingDemo.initializerRuns, "the static initializer must run EXACTLY once");
        System.out.println("  after a second touch : initializerRuns = " + ClassLoadingDemo.initializerRuns);
    }

    @Test
    @DisplayName("main() is public, static, void, and takes String[] — the contract with the JVM (Demo 6)")
    void mainSignatureIsExact() throws Exception {
        // getMethod looks up a PUBLIC method by name + parameter types — the signature is data.
        Method main = MainMethodDemo.class.getMethod("main", String[].class);
        int mod = main.getModifiers();
        System.out.println("  found: " + Modifier.toString(mod) + " "
                + main.getReturnType().getName() + " main(String[])");
        assertTrue(Modifier.isPublic(mod), "public — the JVM calls it from outside the class");
        assertTrue(Modifier.isStatic(mod), "static — no instance exists at startup");
        assertEquals(void.class, main.getReturnType(), "void — nobody receives a result");
    }
}
