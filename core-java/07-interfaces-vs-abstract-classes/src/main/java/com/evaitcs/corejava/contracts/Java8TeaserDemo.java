package com.evaitcs.corejava.contracts;

/**
 * TEASER — PRINTS ONLY. There is deliberately NO default-method code in this
 * module: it compiles with --release 8 on purpose and teaches the CLASSIC
 * rules, so the "before" picture stays honest. Java 8 changed two rows of the
 * capability table; this demo says which, and where the story continues.
 */
public class Java8TeaserDemo {

    public static void main(String[] args) {

        banner("The rows of the capability table that JAVA 8 REWRITES");

        System.out.println("capability            | interface (pre-Java 8)     | interface (Java 8+)");
        System.out.println("----------------------+----------------------------+----------------------------");
        System.out.println("method bodies         | NO                         | YES — default methods");
        System.out.println("static members        | constants only             | + static methods");
        System.out.println("evolving a published  | adding ANY method breaks   | default methods save every");
        System.out.println("  interface           | every implementor on earth | existing implementor");
        System.out.println("the callback ceremony | anonymous inner class      | a lambda — one arrow");
        System.out.println("   (this module)      | (AnonymousCallbackDemo)    | (java8-features/01)");
        System.out.println();
        System.out.println("Unchanged even in Java 8+: interfaces still get NO instance fields and");
        System.out.println("NO constructors — so STATE, template methods and protected plumbing still");
        System.out.println("live in abstract classes. The decision flowchart survives; only two rows move.");

        banner("Why Predicate.and() exists — the one-line preview");

        System.out.println("Predicate.and(other), .negate(), Function.andThen(...), Collection.stream():");
        System.out.println("every one of those is a DEFAULT method on an interface that had to stay");
        System.out.println("compatible with code written in 1998. Pre-Java-8, adding and() to Predicate");
        System.out.println("would have broken every implementor in existence. default methods are why");
        System.out.println("behavior algebra (module 01 of java8-features) is possible at all.");
        System.out.println();
        System.out.println("And the diamond problem from MultipleInheritanceDemo bites BACK once");
        System.out.println("interfaces carry bodies: two unrelated defaults for one name = compile error,");
        System.out.println("resolved with InterfaceName.super.method(). That is a whole demo over there:");
        System.out.println();
        System.out.println("  -> java8-features/01-lambdas              (anonymous class -> arrow)");
        System.out.println("  -> java8-features/02-interface-evolution  (default & static methods, the WHOLE story)");
    }

    static void banner(String title) {
        System.out.println();
        System.out.println("=== " + title + " ===");
    }
}
