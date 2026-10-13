package com.evaitcs.advanced.gof;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Demo 7 — ADAPTER: translate an OLD interface into the NEW one, touching neither.
 * (README section "Adapter: Arrays.asList and the 2009 ledger".)
 *
 * Problem: the new statement engine consumes StatementSource (typed entries); the
 * 2009-era vendor ledger only offers rows() — pipe-delimited strings. The vendor
 * jar cannot be edited and the engine must not learn 2009 dialects.
 * Solution: LedgerAdapter implements StatementSource BY TRANSLATING LegacyLedger.
 *
 * You already met adapters: Arrays.asList adapts an array to the List interface
 * (it is NOT an ArrayList — it is a translator holding the array); InputStreamReader
 * adapts a byte stream to the character Reader world (module 08's stream/charset bridge).
 */
public class LegacyLedgerAdapterDemo {

    /**
     * The LEGACY system, frozen in 2009: pipe-delimited string rows, no types.
     * Pretend this class arrived in a vendor jar — its source cannot be changed.
     */
    public static final class LegacyLedger {

        private final List<String> rows;

        public LegacyLedger(List<String> rows) {
            this.rows = List.copyOf(rows);
        }

        /** Row format: "yyyy-MM-dd|description|amount" — a mini protocol of its own. */
        public List<String> rows() {
            return rows;
        }
    }

    /** The MODERN interface the new statement engine is written against. */
    public interface StatementSource {
        List<StatementEntry> entries();
    }

    /** What "modern" means: a typed, immutable entry — LocalDate and BigDecimal, not strings. */
    public record StatementEntry(LocalDate date, String description, BigDecimal amount) { }

    /**
     * The Adapter: implements the NEW interface, holds the OLD object, and does the
     * translation in exactly one place. If the legacy row format changes, only the
     * private parse() method changes — the engine never hears about it.
     */
    public static final class LedgerAdapter implements StatementSource {

        private final LegacyLedger legacy;

        public LedgerAdapter(LegacyLedger legacy) {
            this.legacy = legacy;
        }

        @Override
        public List<StatementEntry> entries() {
            return legacy.rows().stream()
                    .map(LedgerAdapter::parse)
                    .toList();
        }

        private static StatementEntry parse(String row) {
            String[] parts = row.split("\\|");
            return new StatementEntry(LocalDate.parse(parts[0]), parts[1], new BigDecimal(parts[2]));
        }
    }

    /** The consumer: written ONLY against the modern interface. It has no idea 2009 exists. */
    static BigDecimal totalCredits(StatementSource source) {
        return source.entries().stream()
                .map(StatementEntry::amount)
                .filter(amount -> amount.signum() > 0)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2);
    }

    public static void main(String[] args) {
        banner("1. The two worlds that must meet");

        LegacyLedger ledger = new LegacyLedger(List.of(
                "2026-01-05|Payroll|2450.00",
                "2026-01-07|Rent|-1200.00",
                "2026-01-09|Interest|12.50"));
        System.out.println("legacy rows()      = " + ledger.rows());
        System.out.println("engine expects     : List<StatementEntry(LocalDate, String, BigDecimal)>");
        System.out.println("verdict: incompatible shapes — and neither side may be edited.");

        banner("2. The adapter translates, row by row, in ONE place");

        StatementSource source = new LedgerAdapter(ledger);   // polymorphism: the engine sees only this
        List<StatementEntry> entries = source.entries();
        System.out.println("step 1: new LedgerAdapter(ledger) — the only line that knows BOTH worlds");
        entries.forEach(entry -> System.out.println("step 2: entry = " + entry));
        System.out.println("verdict: every string became a typed record — date parsed, amount scale 2.");

        banner("3. The engine code never learns the legacy exists");

        BigDecimal credits = totalCredits(source);
        System.out.println("step 1: totalCredits(source) filtered signum > 0 and summed");
        System.out.println("step 2: total credits = " + credits.toPlainString() + "   (2450.00 + 12.50)");
        System.out.println("verdict: totalCredits takes StatementSource — a database source, a Kafka source");
        System.out.println("         or this adapter all satisfy it with zero changes to the engine.");

        banner("4. Where you already met it");

        System.out.println("Arrays.asList(array): a List VIEW translating onto the array — add() throws");
        System.out.println("                    UnsupportedOperationException because arrays cannot grow.");
        System.out.println("InputStreamReader  : adapts bytes to chars (module 08) — the bridge you");
        System.out.println("                    wrapped in BufferedReader every time you read a file.");

        banner("5. Trap: when NOT to use Adapter");

        System.out.println("Adapters are debt wrappers, not goals. Three adapters around the same");
        System.out.println("legacy class mean the translation belongs INSIDE a replacement. And never");
        System.out.println("adapter-wrap-adapter: each layer hides failures one layer deeper.");

        banner("6. In one sentence");
        System.out.println("An Adapter is a translator with the new interface on one side and the old");
        System.out.println("object on the other — so neither side has to change to make them work together.");
    }

    static void banner(String title) {
        System.out.println();
        System.out.println("=== " + title + " ===");
    }
}
