package com.evaitcs.java8.compare;

/**
 * THE WRAP-UP SLIDE, in code.
 *
 * After walking tasks 1-7, the class has seen the same six ideas re-appear in every
 * task. This prints them side by side so the mapping becomes something students can
 * carry into their own codebases: "when I catch myself writing THIS loop shape, there
 * is a stream concept for it."
 *
 * Run it last, then leave it on the projector while answering questions.
 */
public class ComparisonSummaryDemo {

    public static void main(String[] args) {
        System.out.println("IMPERATIVE CONCEPT          ->  STREAM CONCEPT               (seen in)");
        System.out.println("---------------------------------------------------------------------------");
        row("for / while loop",              "internal iteration",        "every task");
        row("accumulator variable + reassign", "reduce(identity, acc)",   "TaskSum, TaskChain");
        row("result.add(...) into a new list", "collect(Collectors.toList())", "TaskFilter, TaskTransform");
        row("if (condition) inside the loop", "filter(predicate)",         "TaskFilter");
        row("transform each element",        "map(function)",             "TaskTransform");
        row("nested loops / hand-rolled join", "map + flatMap",           "TaskChain");
        row("containsKey/get/put dance",     "collector (module 06!)",    "TaskCount");
        row("early return / boolean flag",   "short-circuit terminal (anyMatch)", "TaskAnyMatch");
        row("returning null for 'no result'", "Optional<T>",              "TaskMax");
        System.out.println("---------------------------------------------------------------------------");
        System.out.println();
        System.out.println("THE ONE-SENTENCE VERSION:");
        System.out.println("  Imperative code says HOW: iterate, keep state, branch, accumulate.");
        System.out.println("  Stream code says WHAT: filter these, transform those, reduce to one.");
        System.out.println("  The HOW moves into the library — written once, tested everywhere,");
        System.out.println("  free to optimize (and in module 07, to run in PARALLEL with no code change).");
        System.out.println();
        System.out.println("THE HONEST CAVEAT (say this too):");
        System.out.println("  Streams are not 'better' at everything. Debugging a deep pipeline is harder");
        System.out.println("  (you can't set a breakpoint on a stage as easily), a checked exception inside");
        System.out.println("  a lambda is awkward, and for a 3-line loop the imperative version may be clearer.");
        System.out.println("  The skill is RECOGNIZING which shape you're in — that's what tasks 1-7 trained.");
    }

    private static void row(String imperative, String streams, String where) {
        System.out.printf("  %-28s -> %-30s %s%n", imperative, streams, where);
    }
}
