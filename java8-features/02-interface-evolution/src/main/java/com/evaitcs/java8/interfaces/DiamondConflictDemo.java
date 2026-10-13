package com.evaitcs.java8.interfaces;

/**
 * RULE 3 — the exam question, the interview question, the DIAMOND.
 *
 * Two UNRELATED interfaces each provide a default for the same signature. Neither is
 * "more specific" (rule 2 needs an extends-relationship to break the tie — there is
 * none here), and no class in the hierarchy provides a body (rule 1 has nothing to
 * grab). Java refuses to guess: the class MUST override, and inside that override it
 * picks a side with the qualified-super syntax:
 *
 *     InterfaceName.super.method()
 *
 * The README mermaid diagram draws this exact picture — show it first.
 */
public class DiamondConflictDemo {

    interface EmailChannel {
        default String describe() { return "email-channel"; }
    }

    interface SmsChannel {
        default String describe() { return "sms-channel"; }
    }

    // ------------------------------------------------------------------
    // THE ERROR — uncomment the next line to show it live on the projector:
    //
    //     static class Broken implements EmailChannel, SmsChannel { }
    //
    // javac says (word for word):
    //
    //     error: types EmailChannel and SmsChannel are incompatible;
    //     class Broken inherits unrelated defaults for describe() from types EmailChannel and SmsChannel
    //
    // Read the message out loud once: it names the two disagreeing types AND the
    // method they disagree about. Then fix it the way MultiNotifier does below.
    // ------------------------------------------------------------------

    /** The fix: override describe(), then reach into either side explicitly. */
    static class MultiNotifier implements EmailChannel, SmsChannel {
        @Override
        public String describe() {
            // InterfaceName.super.describe() = "run EMAIL's default body — not mine, not the other guy's."
            // This syntax ONLY works for interfaces you directly implement.
            return "MULTI(" + EmailChannel.super.describe() + " + " + SmsChannel.super.describe() + ")";
        }

        /** Both sides stay reachable forever — nothing was lost, we just had to CHOOSE. */
        String describeViaEmail() { return EmailChannel.super.describe(); }
        String describeViaSms()   { return SmsChannel.super.describe(); }
    }

    public static void main(String[] args) {
        System.out.println("=== Two parents, one signature, zero guesses ===");
        System.out.println("   bare EmailChannel default : " + new EmailChannel() { }.describe());
        System.out.println("   bare SmsChannel default   : " + new SmsChannel() { }.describe());

        MultiNotifier multi = new MultiNotifier();
        System.out.println();
        System.out.println("   MultiNotifier implements BOTH, so it MUST override:");
        System.out.println("       describe() -> " + multi.describe());
        System.out.println("   Each side individually, via InterfaceName.super.describe():");
        System.out.println("       -> " + multi.describeViaEmail() + "  /  " + multi.describeViaSms());

        System.out.println();
        System.out.println("   Takeaway: the compile error is a FEATURE. Java 8 would rather break");
        System.out.println("   your build than silently pick one of two behaviors — in a bank,");
        System.out.println("   \"silently chose the wrong notifier\" is an incident report, not a Tuesday.");
    }
}
