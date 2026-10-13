package com.evaitcs.corejava.strings;

/**
 * Claim #3, the one to SHOUT: for text, ALWAYS use equals (or equalsIgnoreCase).
 * Never ==.
 *
 *   ==      asks: do these two references point at the SAME OBJECT?   (identity)
 *   equals  asks: do these two strings hold the SAME CHARACTERS?      (content)
 *
 * Why == "sometimes works" (and is therefore extra dangerous): the string pool
 * from the previous demo makes literals share objects, so == on two literals
 * prints true and teaches the student the WRONG lesson. The first String that
 * arrives from a scanner, a database, or a runtime + ends the streak.
 */
public class EqualsVsDoubleEqualsDemo {

    public static void main(String[] args) {

        banner("1. Same characters, different objects — == says false, equals says true");

        String accountId = "ACC-1001";                 // pooled literal
        String fromFile = readFromStatementFile();     // runtime-built, identical text

        System.out.println("accountId              : \"" + accountId + "\"");
        System.out.println("fromFile               : \"" + fromFile + "\"");
        System.out.println("accountId == fromFile  : " + (accountId == fromFile) + "  <- DIFFERENT objects");
        System.out.println("accountId.equals(fromFile) : " + accountId.equals(fromFile) + "   <- same characters");
        System.out.println("=> If this were a login check, == just logged a real customer out.");

        banner("2. equalsIgnoreCase — case IS a character difference");

        String typed = "active";    // what the customer typed into the web form
        String stored = "ACTIVE";   // what the database stores

        System.out.println("\"active\".equals(\"ACTIVE\")           : " + typed.equals(stored) + "  <- 'a' (97) is not 'A' (65)");
        System.out.println("\"active\".equalsIgnoreCase(\"ACTIVE\"): " + typed.equalsIgnoreCase(stored) + "   <- use this for statuses/codes");
        System.out.println("=> equals for exact text (passwords, keys); equalsIgnoreCase for human input.");

        banner("3. The null trap — and the null-safe idiom");

        String status = null;   // e.g., the account lookup found nothing

        try {
            // The natural way to write it — and a NullPointerException when status is null,
            // because you are calling a METHOD on a null reference.
            status.equals("ACTIVE");
        } catch (NullPointerException e) {
            System.out.println("status.equals(\"ACTIVE\")      -> caught " + e.getClass().getSimpleName()
                    + " (calling a method on null)");
        }

        // THE IDIOM: put the CONSTANT first. A literal is never null, so the call can
        // never throw; if the variable is null, equals simply answers false.
        boolean isActive = "ACTIVE".equals(status);
        System.out.println("\"ACTIVE\".equals(status)      -> " + isActive + " (no explosion — this is the idiom)");
        System.out.println("=> Read it as: 'is status equal to ACTIVE?' — the constant on the left");
        System.out.println("   turns a possible crash into a clean false. (Java 8's Objects.equals(a, b)");
        System.out.println("   is the same idea when BOTH sides are variables.)");
    }

    /**
     * Stands in for text that arrives at runtime (file, network, database, scanner).
     * Built with `new` from the chars so it is genuinely NOT the pooled literal —
     * exactly what real input looks like from the String's point of view.
     */
    private static String readFromStatementFile() {
        char[] chars = { 'A', 'C', 'C', '-', '1', '0', '0', '1' };
        return new String(chars);
    }

    static void banner(String title) {
        System.out.println();
        System.out.println("=== " + title + " ===");
    }
}
