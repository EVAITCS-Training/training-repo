package com.evaitcs.advanced.gof;

/**
 * Demo 1 — THE WHOLE MODULE ON ONE SCREEN.
 * (README sections "The map: 23 patterns, 3 intents" and "You already speak this vocabulary".)
 *
 * The claim this demo proves by exhaustion: the GoF's 23 patterns are not exotic
 * recipes — every single one has a JDK or framework instance you have ALREADY used
 * in this course, and most have a SecureBank counterpart you could write today.
 *
 * Column layout per row: PATTERN | where you already met it (JDK/framework/course)
 * | the SecureBank counterpart. Rows marked "DEMO HERE" get their own class in
 * this module; the rest are named so you can spot them in the wild.
 */
public class PatternMapDemo {

    public static void main(String[] args) {
        banner("1. What 'GoF' means");

        System.out.println("GoF = Gang of Four: the four authors of Design Patterns (1994).");
        System.out.println("They catalogued 23 recurring object-oriented solutions and — the real");
        System.out.println("gift — gave each a NAME. A name turns a paragraph of explanation into");
        System.out.println("one word in a design review: 'that is just a Decorator' ends the debate.");
        System.out.println("The 23 sort by INTENT: what the pattern is FOR.");
        System.out.println("  CREATIONAL  (5)  — how objects get created");
        System.out.println("  STRUCTURAL  (7)  — how objects compose into bigger shapes");
        System.out.println("  BEHAVIORAL  (11) — how objects talk and share responsibilities");

        banner("2. CREATIONAL — how objects get created");

        row("Factory Method *", "List.of(), Integer.valueOf()", "PaymentFactory.from(\"wire\") — demo here");
        row("Abstract Factory", "DocumentBuilderFactory.newInstance() (JAXP)", "a country-specific bundle: statement renderer + fee table + tax rules");
        row("Builder *", "StringBuilder, HttpRequest.newBuilder()", "LoanApplication.builder() — demo here");
        row("Prototype", "Cloneable/Object.clone() (and why the course avoids it)", "copy a configured fee schedule as the seed of a new product");
        row("Singleton *", "Runtime.getRuntime(); the enum form", "FeeSchedule.INSTANCE — demo here; DI replaces it (module 10)");

        banner("3. STRUCTURAL — how objects compose");

        row("Adapter *", "Arrays.asList(), InputStreamReader (module 08)", "LegacyLedger -> StatementSource — demo here");
        row("Bridge", "java.sql: the API split from every vendor driver", "one MessagingApi over per-vendor SMS/email senders");
        row("Composite", "Swing containers; a File tree walks uniformly", "a portfolio holding accounts AND sub-portfolios, priced as one");
        row("Decorator *", "BufferedInputStream chains (module 08), unmodifiableList", "AuditDecorator around TransferService — demo here");
        row("Facade", "JDBC: one API hiding every driver's dialect", "stage-5's api-gateway: ONE front door hiding eight services");
        row("Flyweight", "Integer cache -128..127, the String pool (core-java 03)", "one shared Currency-code table for millions of transactions");
        row("Proxy *", "java.lang.reflect.Proxy — module 02's audit proxy", "Spring @Transactional wraps your bean; lazy-load huge statement archives");

        banner("4. BEHAVIORAL — how objects talk");

        row("Chain of Responsibility", "servlet filters; class-loader delegation", "approval chain: teller -> manager -> director, by amount");
        row("Command", "Runnable (module 06), Swing Action, UndoManager", "a TransferCommand queued into the nightly batch executor");
        row("Interpreter", "java.util.regex.Pattern", "a tiny grammar for fraud rules (\"amount > 10000 AND country != NL\")");
        row("Iterator *", "Iterator/for-each — collections-framework 03", "walk transactions without exposing how they are stored");
        row("Mediator", "Executor: submitters never meet workers directly", "a trading desk mediating between buyers and sellers");
        row("Memento", "serialization snapshots; Swing UndoManager", "snapshot an account before a risky batch; restore on failure");
        row("Observer *", "Swing listeners; whenComplete (module 06); java.util.Observer (deprecated)", "BalanceListener — demo here; stage-5 Kafka events across processes");
        row("State", "Thread.State lifecycle (module 04)", "LoanStatus: APPLIED -> APPROVED -> DISBURSED, transitions guarded");
        row("Strategy *", "Comparator — core-java 10", "FeePolicy sealed family — demo here");
        row("Template Method *", "AbstractList skeletons — collections-framework 02", "ReportGenerator — demo here; JdbcTemplate (stage-2) says it in its name");
        row("Visitor", "FileVisitor in Files.walkFileTree (module 08)", "price every Payment subtype: sealed + exhaustive switch (module 01)");

        banner("5. How to read the star marks");

        System.out.println("* = this module implements the pattern in SecureBank code (8 live demos).");
        System.out.println("The others are NAMED so you can recognize them in the wild — half you have");
        System.out.println("already used by their JDK names; the rest appear in stage-2 and stage-5.");
        System.out.println("verdict: 23 patterns, 3 intents, 0 recipes — every row is a NAME for a");
        System.out.println("         solution shape you will meet again, whether or not you write it.");

        banner("6. In one sentence");
        System.out.println("The GoF catalog is a vocabulary list: creational (making), structural");
        System.out.println("(composing), behavioral (talking) — and you already speak most of it.");
    }

    /** One map row: pattern | where you already met it | SecureBank counterpart. */
    private static void row(String pattern, String met, String bank) {
        System.out.printf("  %-28s | %s%n", pattern, met);
        System.out.printf("  %-28s | bank: %s%n", "", bank);
    }

    static void banner(String title) {
        System.out.println();
        System.out.println("=== " + title + " ===");
    }
}
