package com.evaitcs.advanced.jvm;

/**
 * Demo 2 — StackOverflowError CAUGHT, DEPTH COUNTED: your frame budget, measured.
 * (README section "Stacks: frames, and the budget that runs out".)
 *
 * The claim: every method call pushes ONE frame — its locals, parameters and return
 * address — onto the calling thread's stack. The stack is a fixed-size region per
 * thread (-Xss, default roughly 1 MB). Recursion without a base case spends the budget
 * frame by frame until the JVM throws StackOverflowError — an ERROR, not an Exception,
 * so catch(Exception) never sees it.
 *
 * We catch it here purely as a teaching device: production code should let it crash.
 * The depth is PRINTED, never asserted — it depends on -Xss and on how fat each frame
 * is, so it is a fact about this JVM run, not a contract.
 */
public class StackOverflowDemo {

    /** Frames entered by the doomed recursion — written by every call, read after the catch. */
    private static int depthReached;

    public static void main(String[] args) throws Exception {
        banner("1. Recursion with no base case: spend the frame budget until it breaks");
        depthReached = 0;
        try {
            recurse();
            System.out.println("never printed — recurse() has no base case, on purpose");
        } catch (StackOverflowError e) {
            System.out.println("step 1: CAUGHT -> " + e.getClass().getSimpleName() + " (an ERROR: catch(Exception) would have missed it)");
            System.out.println("step 2: frames stacked before the fall: " + depthReached);
            System.out.println("step 3: each frame of recurse() cost a few dozen bytes of THIS thread's stack (-Xss budget).");
            System.out.println("WHY: calls push frames faster than returns pop them; the budget is fixed, so the");
            System.out.println("     fall is deterministic — same JVM, same method, roughly the same depth every run.");
            System.out.println("verdict: " + depthReached + " is YOUR frame budget on THIS JVM — it varies with -Xss and frame size; never hard-code it.");
        }

        banner("2. The budget is PER THREAD: a smaller stack falls sooner");
        // The Thread constructor's last argument REQUESTS a stack size — the OS may round it.
        // Same infinite recursion, smaller budget: the depth proves the stack belongs to
        // the thread, not to the method or the JVM.
        int[] smallStackDepth = {0};
        Thread smallStack = new Thread(null, () -> {
            try {
                countAndRecurse(smallStackDepth);
            } catch (StackOverflowError e) {
                System.out.println("step 1: the 256 KB-stack thread fell at depth " + smallStackDepth[0]);
            }
        }, "small-stack-thread", 256 * 1024);
        smallStack.start();
        smallStack.join();
        System.out.println("step 2: the default-stack thread fell at depth " + depthReached + " — deeper, same recursion");
        System.out.println("verdict: the budget travels with the THREAD. Virtual threads (module 06) keep their frames");
        System.out.println("         in heap-allocated continuations, but they throw StackOverflowError all the same.");

        banner("3. What a frame holds — and why locals die with it");
        System.out.println("step 1: a frame holds parameters, LOCAL variables and the operand stack — references, not objects.");
        System.out.println("step 2: when the frame pops (return OR throw), its locals are gone — those references stop being GC ROOTS");
        System.out.println("        (README 'Reachability': a popped frame cannot keep anything alive).");
        System.out.println("step 3: the OBJECTS those locals pointed at stay on the heap until no root reaches them.");
        System.out.println("verdict: StackOverflowError = frames exhausted (fixed budget, per thread);");
        System.out.println("         OutOfMemoryError  = heap exhausted (LeakHuntDemo) — two different rooms ran out.");
    }

    /** No base case on purpose: every call pushes one more frame (README walkthrough step 2). */
    private static void recurse() {
        depthReached++;
        recurse();
    }

    /** Same doomed recursion, counting into a caller-owned slot (the small-stack thread uses this). */
    private static void countAndRecurse(int[] depth) {
        depth[0]++;
        countAndRecurse(depth);
    }

    static void banner(String title) {
        System.out.println();
        System.out.println("=== " + title + " ===");
    }
}
