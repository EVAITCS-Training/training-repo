package com.evaitcs.java8.interfaces;

/**
 * When a class inherits the SAME signature with a body from more than one place,
 * which body runs? Before Java 8 the question could not arise — only classes had
 * bodies. With default methods it can, so the language defines an order.
 *
 * The three rules, in priority order (show the README flowchart BEFORE this code):
 *   RULE 1 — the class wins: a concrete method in the class or ANY superclass beats
 *            every interface default.
 *   RULE 2 — the most specific interface wins: a sub-interface's default beats the
 *            super-interface's default (a default CAN override a default).
 *   RULE 3 — no winner: two UNRELATED interfaces both provide a default => COMPILE
 *            ERROR; the class MUST override. That is DiamondConflictDemo.
 *
 * Board mnemonic: "Classes beat interfaces. Children beat parents. Otherwise, you decide."
 */
public class ResolutionRulesDemo {

    interface Greeter {
        default String greet() { return "Hello from the Greeter INTERFACE default"; }
    }

    /** A concrete class with its own greet(). No relation to Greeter — yet. */
    static class BankTeller {
        public String greet() { return "Good morning, how may I help you? (BankTeller CLASS)"; }
    }

    /**
     * RULE 1 — the class wins. Point at this class body: it is EMPTY. TraineeTeller
     * inherits greet() from BOTH a superclass and an interface default, and still
     * talks like a BankTeller. Why? A default is the FALLBACK for when no class
     * bothered to provide real code — real code always beats the fallback.
     */
    static class TraineeTeller extends BankTeller implements Greeter { }

    /** Your own override beats everything — the rules only choose among code you DIDN'T write. */
    static class LoudTrainee extends BankTeller implements Greeter {
        @Override
        public String greet() { return "HEY! " + super.greet(); } // super. = the CLASS version
    }

    // ---- Rule 2 setup: a super-interface and a sub-interface, both with a default ----

    interface Channel {
        default String describe() { return "generic channel (Channel)"; }
    }

    /** A default overriding a default: legal, and this is what "more specific" means. */
    interface SmsChannel extends Channel {
        @Override default String describe() { return "SMS channel (SmsChannel)"; }
    }

    /** RULE 2 — the most specific interface wins. Another EMPTY class body to point at. */
    static class AutoResponder implements SmsChannel { }

    public static void main(String[] args) {
        System.out.println("=== Rule 1: a superclass method beats ANY interface default ===");
        Greeter bare = new Greeter() { }; // nobody overrode anything -> the default itself
        System.out.println("   bare Greeter (anonymous)  : " + bare.greet());
        System.out.println("   TraineeTeller extends BankTeller implements Greeter, EMPTY body:");
        System.out.println("       -> " + new TraineeTeller().greet());
        System.out.println("   Students often ask: \"but the interface is newer code, shouldn't it win?\"");
        System.out.println("   No — newness is irrelevant. Class hierarchy first, always.");

        System.out.println();
        System.out.println("=== Your own override beats both ===");
        System.out.println("   LoudTrainee -> " + new LoudTrainee().greet());

        System.out.println();
        System.out.println("=== Rule 2: the more specific (sub-)interface default wins ===");
        Channel generic = new Channel() { };
        System.out.println("   bare Channel (anonymous)  : " + generic.describe());
        System.out.println("   AutoResponder implements SmsChannel, EMPTY body:");
        System.out.println("       -> " + new AutoResponder().describe());
        Channel asChannel = new AutoResponder();
        System.out.println("   The variable's TYPE does not matter, the OBJECT's class does:");
        System.out.println("   Channel c = new AutoResponder(); c.describe() -> " + asChannel.describe());

        System.out.println();
        System.out.println("=== Rule 3: unrelated interfaces collide -> next demo (the diamond) ===");
    }
}
