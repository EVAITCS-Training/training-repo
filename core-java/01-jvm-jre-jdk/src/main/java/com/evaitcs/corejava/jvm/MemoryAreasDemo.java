package com.evaitcs.corejava.jvm;

/**
 * Demo 4 — the JVM's runtime memory areas: the picture students need for EVERYTHING later
 * (references in module 02, the String pool in 03, static fields in 11, GC everywhere).
 *
 *   HEAP          all objects; shared by every thread; garbage-collected
 *   STACK         one per thread; every method call pushes a FRAME (locals + return address)
 *   METHOD AREA   class metadata + static fields (in modern JVMs: "Metaspace", off-heap)
 *   PC REGISTERS  per-thread pointer at the current bytecode instruction
 *   NATIVE STACKS memory for calls into non-Java code (JNI)
 *
 * We prove two of them by breaking/observing them: the stack is FINITE (StackOverflowError),
 * and the heap is measurable (Runtime.getRuntime() numbers).
 */
public class MemoryAreasDemo {

    private static int framesDeep;

    public static void main(String[] args) {

        banner("1. The stack is FINITE — proof by unbounded recursion");

        // Each method call pushes one frame: parameters, locals, and where to return to.
        // Frames are never shared, so a call that never stops eats frames until none are left.
        framesDeep = 0;
        try {
            recurseForever();
        } catch (StackOverflowError e) {
            // Students often ask: "can you CATCH that?" Yes — it is an Error, not an Exception,
            // and by the time we catch it, the overflowing frames are already popped, so the
            // stack works again. In production: don't swallow it, it always means a bug
            // (recursion without a proper base case). Here we catch it to PROVE the point.
            System.out.println("  StackOverflowError caught after " + framesDeep + " frames.");
            System.out.println("  Every call to recurseForever() added one frame; when frames ran out, the JVM said ENOUGH.");
            System.out.println("  The exact number varies (stack size per thread, JVM, OS) — that it ENDS is the lesson.");
        }
        System.out.println("  And the stack already works again: 2 + 2 = " + add(2, 2));

        banner("2. The heap, by the numbers (Runtime is the JVM's dashboard)");

        Runtime rt = Runtime.getRuntime();
        printHeap("before allocating        ", rt);

        // A 10 MB object. Watch `used` jump. The ARRAY lives on the HEAP; the REFERENCE
        // `blob` is just a pointer sitting in main()'s STACK frame (module 02 is all about this).
        byte[] blob = new byte[10 * 1024 * 1024];
        System.out.println("  (allocated new byte[10 MB], still referenced by local variable `blob`)");
        printHeap("after new byte[10 MB]    ", rt);

        // Drop the only reference: the array is now UNREACHABLE, so it becomes collectable.
        // (The blob.length print below happens BEFORE we null it, so the array stays live until then.)
        System.out.println("  (blob.length was " + blob.length + "; now dropping the reference: blob = null)");
        blob = null;

        // System.gc() is a HINT, never a command. The JVM may collect now, later, or ignore us.
        System.gc();
        printHeap("after blob=null + gc()   ", rt);
        System.out.println("  If `used` did not fully drop, nothing is broken — gc() only SUGGESTS a collection.");
        System.out.println("  You never manage heap memory yourself in Java; you manage REFERENCES (next modules).");

        banner("3. The other areas, in one breath");

        System.out.println("  METHOD AREA / Metaspace : class metadata + static fields (framesDeep lives there).");
        System.out.println("  PC REGISTERS            : one tiny pointer per thread: 'which bytecode instruction am I on'.");
        System.out.println("  NATIVE STACKS           : used when Java calls non-Java code (JNI).");
        System.out.println("  See the README diagram — heap vs stack with a live frame picture.");
    }

    private static void recurseForever() {
        framesDeep++;
        recurseForever(); // no base case — the crash IS the point
    }

    private static int add(int a, int b) {
        return a + b; // a tiny normal call, to show the stack still works after the overflow
    }

    private static void printHeap(String label, Runtime rt) {
        long max = rt.maxMemory();      // ceiling the heap may grow to (-Xmx)
        long total = rt.totalMemory();  // what the JVM has committed from the OS right now
        long free = rt.freeMemory();    // still-free space INSIDE that committed region
        long used = total - free;       // live objects + not-yet-collected garbage
        System.out.printf("  %s used %8s of committed %8s (max %8s)%n", label, mb(used), mb(total), mb(max));
    }

    /** Human-readable MB — raw byte counts are unreadable from the back of the room. */
    static String mb(long bytes) {
        return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
    }

    private static void banner(String title) {
        System.out.println();
        System.out.println("=== " + title + " ===");
    }
}
