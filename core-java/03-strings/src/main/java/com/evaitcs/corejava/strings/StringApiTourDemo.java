package com.evaitcs.corejava.strings;

/**
 * The everyday String API, printed in groups a teacher can point at one at a time.
 * Nothing here is deep — the goal is COVERAGE plus the three traps worth naming
 * out loud: substring's end-exclusive index, split's regex argument, and
 * case-conversion's Locale sensitivity. Every "change" method returns a NEW
 * String (claim #1) — that is the thread connecting all of this.
 */
public class StringApiTourDemo {

    public static void main(String[] args) {

        String accountId = "ACC-1001";
        String csvLine   = "T-101,Payroll,2500.00";

        banner("1. Asking about it: length / charAt / indexOf / lastIndexOf / contains / startsWith / endsWith");

        System.out.println("accountId                     = \"" + accountId + "\"");
        System.out.println("accountId.length()            = " + accountId.length());
        System.out.println("accountId.charAt(4)           = '" + accountId.charAt(4) + "'");
        System.out.println("accountId.indexOf('-')        = " + accountId.indexOf('-'));
        System.out.println("accountId.indexOf(\"10\")       = " + accountId.indexOf("10") + "   (works for chars AND text)");
        System.out.println("accountId.lastIndexOf('1')    = " + accountId.lastIndexOf('1') + "   (searches from the end)");
        System.out.println("accountId.indexOf(\"XYZ\")      = " + accountId.indexOf("XYZ") + "   <- NOT FOUND = -1, never an exception");
        System.out.println("accountId.contains(\"100\")     = " + accountId.contains("100"));
        System.out.println("accountId.startsWith(\"ACC\")   = " + accountId.startsWith("ACC"));
        System.out.println("accountId.endsWith(\"01\")      = " + accountId.endsWith("01"));
        System.out.println("=> indexOf returning -1 is the classic pre-contains idiom: if (s.indexOf(\"x\") >= 0).");

        banner("2. Cutting: substring — THE END INDEX IS EXCLUSIVE");

        //   A   C   C   -   1   0   0   1
        //   0   1   2   3   4   5   6   7      <- indices
        //   substring(0, 3)  => indices 0,1,2  => "ACC"    (stops BEFORE index 3!)
        //   substring(4)     => index 4 to end => "1001"
        //   Mnemonic for the class: substring(a, b).length() is ALWAYS b - a.
        System.out.println("accountId.substring(4)        = \"" + accountId.substring(4) + "\"");
        System.out.println("accountId.substring(0, 3)     = \"" + accountId.substring(0, 3) + "\"   (begin inclusive, END EXCLUSIVE)");
        System.out.println("=> substring returns a NEW String; accountId itself is untouched (claim #1).");

        banner("3. Changing (into a new one): replace / replaceAll / replaceFirst");

        System.out.println("accountId.replace('1', '9')   = \"" + accountId.replace('1', '9') + "\"   (ALL occurrences, plain char)");
        System.out.println("accountId.replace(\"ACC\", \"X\") = \"" + accountId.replace("ACC", "X") + "\"   (plain TEXT, no regex)");
        System.out.println("accountId.replaceAll(\"[0-9]\", \"#\") = \"" + accountId.replaceAll("[0-9]", "#") + "\"   <- REGEX argument");
        System.out.println("accountId.replaceFirst(\"0\", \"*\")   = \"" + accountId.replaceFirst("0", "*") + "\"   <- REGEX, first hit only");
        System.out.println("=> Trap to name: replace/replaceAll sound like a pair, but only the *All/*First");
        System.out.println("   family takes REGEX. replace takes plain text.");

        banner("4. split — and the regex trap that bites everyone exactly once");

        String[] parts = csvLine.split(",");
        System.out.println("\"" + csvLine + "\".split(\",\") gives " + parts.length + " columns:");
        for (int i = 0; i < parts.length; i++) {
            System.out.println("   parts[" + i + "] = \"" + parts[i] + "\"");
        }
        String amount = "2500.00";
        System.out.println("\"2500.00\".split(\".\")   -> length " + amount.split(".").length
                + "   <- '.' is REGEX for ANY char: everything is a delimiter, nothing survives");
        String[] halves = amount.split("\\.");
        System.out.println("\"2500.00\".split(\"\\\\.\") -> [\"" + halves[0] + "\", \"" + halves[1]
                + "\"]   <- escape the dot: \"\\\\.\" is how the regex \\. is written in Java");

        banner("5. Cleaning and re-casing: trim / toUpperCase / toLowerCase");

        String padded = "  Ada Lovelace  ";
        System.out.println("\"" + padded + "\".trim()   = \"" + padded.trim() + "\"   (cuts whitespace at BOTH ends)");
        System.out.println("padded.trim().toUpperCase() = \"" + padded.trim().toUpperCase() + "\"");
        System.out.println("padded.trim().toLowerCase() = \"" + padded.trim().toLowerCase() + "\"");
        // LOCALE TRAP (say it once, then move on): case conversion is LOCALE-SENSITIVE.
        // On a JVM with Turkish locale, "title".toUpperCase() yields "TİTLE" — a DOTTED
        // capital I — because Turkish has two i's. When case-folding for COMPARISON
        // (ids, keys, status codes), pass an explicit locale: toUpperCase(Locale.ROOT).

        banner("6. Building text: String.format / String.valueOf / String.join");

        int txnNumber = 101;
        double amountValue = 2500.00;
        String receipt = String.format("Receipt #%d  %s  amount %.2f", txnNumber, "Payroll", amountValue);
        System.out.println("String.format(\"Receipt #%d  %s  amount %.2f\", ...) = \"" + receipt + "\"");
        System.out.println("=> %d integer, %s any object (calls toString), %.2f decimal with 2 places — money output.");
        System.out.println("String.valueOf(txnNumber)   = \"" + String.valueOf(txnNumber) + "\"   (and unlike toString it is");
        System.out.println("                              NULL-SAFE: String.valueOf((Object) null) prints \"null\", no NPE)");
        String joined = String.join(" | ", "ACC-1001", "Ada Lovelace", "ACTIVE");
        System.out.println("String.join(\" | \", ...)      = \"" + joined + "\"");
        System.out.println("=> String.join is new in Java 8 — before it, everyone hand-rolled the");
        System.out.println("   'separator except on the first item' loop (see java8-features/09-api-upgrades).");

        banner("7. Comparing and checking: equalsIgnoreCase / compareTo / isEmpty");

        System.out.println("\"active\".equalsIgnoreCase(\"ACTIVE\") = " + "active".equalsIgnoreCase("ACTIVE") + "   (demo 3's content question)");
        int cmp = "ACC-1001".compareTo("ACC-1002");
        System.out.println("\"ACC-1001\".compareTo(\"ACC-1002\")    = " + cmp + "   (negative: '1' comes before '2')");
        System.out.println("=> compareTo is LEXICOGRAPHIC: dictionary order by char code, and the sign");
        System.out.println("   (negative/zero/positive) is exactly the Comparator protocol — this is how");
        System.out.println("   Collections.sort orders Strings. Module 10 picks this contract up.");
        System.out.println("\"\".isEmpty()                = " + "".isEmpty());
        System.out.println("\"ACC-1001\".isEmpty()        = " + accountId.isEmpty());
        System.out.println("=> Prefer !s.isEmpty() over s.length() == 0: identical behavior, but it READS");
        System.out.println("   as the question you are actually asking.");
    }

    static void banner(String title) {
        System.out.println();
        System.out.println("=== " + title + " ===");
    }
}
