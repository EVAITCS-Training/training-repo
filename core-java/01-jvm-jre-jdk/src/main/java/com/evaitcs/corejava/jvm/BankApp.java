package com.evaitcs.corejava.jvm;

/**
 * "Your program." Deliberately tiny — this is the .java that javac turns into a .class,
 * and the .class the JVM then runs. When we talk about the compile/run pipeline in class,
 * THIS is the program we point at.
 *
 * WHY a bank app as the example: the rest of the course lives in a SecureBank world, so
 * even the throwaway "hello program" is a bank. Consistency on the projector matters.
 *
 * NOTE FOR CLASS: nothing here is special. The magic is not in these 3 lines of source —
 * it is in what javac and the JVM do to them. CompileRunPipelineDemo proves that by
 * reading this class's OWN .class file bytes back off disk.
 */
public class BankApp {

    public static void main(String[] args) {
        System.out.println("SecureBank is open. Accounts loaded: " + args.length);
    }

    /**
     * A method that does real, IRREPRESSIBLE work, so the JIT demo has something to warm up.
     *
     * WHY this shape and not `total += accounts`: a loop that just adds a constant is so
     * simple the JIT collapses it to a single multiply (total = accounts * iterations) and the
     * "hot" runs would take ~0 ms — no honest warm-up story to show. This recurrence feeds each
     * result into the next, so the compiler MUST run every step. Same work every run; only the
     * SPEED changes as the JIT warms up — which is exactly the point we want on the projector.
     * (Java's long overflow is well-defined two's-complement wrap, so the result is deterministic.)
     */
    public static long countCents(long seed, int iterations) {
        long total = seed;
        for (int i = 0; i < iterations; i++) {
            total = total * 1_000_003L + i; // data-dependent: cannot be strength-reduced away
        }
        return total;
    }
}
