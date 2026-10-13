package com.evaitcs.advanced.gof;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Demo 5 — TEMPLATE METHOD: the skeleton lives in the base class, the slots in subclasses.
 * (README section "Template Method: AbstractList taught you this in collections-framework".)
 *
 * Problem: every report has the SAME shape — header, rows, footer — but different
 * content. Copy-pasting the shape into each report invites one report to drift.
 * Solution: a FINAL generate() owns the order forever; subclasses only fill slots.
 *
 * You already met this: AbstractList implements indexOf/iterator/spliterator ONCE on
 * top of two abstract slots — get(int) and size() (collections-framework 02). Spring's
 * JdbcTemplate (stage-2) is the same pattern with the word "Template" in its name:
 * it owns open-connection/execute/close, your lambda fills the SQL slot.
 *
 * EVIDENCE DESIGN: every step appends its name to `steps`, so the exact execution
 * order is printable and testable — the skeleton markers ("header", "rows", "footer")
 * interleave with each subclass's own slot markers ("tax:title", "statement:row:...").
 */
public class ReportTemplateDemo {

    /** One statement line: id, description, and a SIGNED amount (credits positive). */
    public record StatementRow(String id, String description, BigDecimal signedAmount) { }

    /** The Template: generate() is FINAL — no subclass may reorder the algorithm. */
    public abstract static class ReportGenerator {

        protected final List<String> steps = new ArrayList<>();   // evidence: exact execution order
        protected final List<String> lines = new ArrayList<>();   // the report content

        public final List<String> generate() {
            steps.add("generate:start");
            steps.add("header");
            header();                       // slot 1 — subclass fills
            steps.add("rows");
            rows();                         // slot 2 — subclass fills
            steps.add("footer");
            footer();                       // slot 3 — subclass fills
            steps.add("generate:end");
            return List.copyOf(steps);
        }

        public List<String> lines() {
            return List.copyOf(lines);
        }

        protected abstract void header();

        protected abstract void rows();

        protected abstract void footer();
    }

    /** Slot-filler 1: the tax report. Money math is real BigDecimal, scale 2. */
    public static final class TaxReport extends ReportGenerator {

        private final BigDecimal interest;
        private final BigDecimal withheld;

        public TaxReport(BigDecimal interest, BigDecimal withheld) {
            this.interest = interest;
            this.withheld = withheld;
        }

        @Override
        protected void header() {
            steps.add("tax:title");
            lines.add("SecureBank Tax Report 2025");
        }

        @Override
        protected void rows() {
            steps.add("tax:row:interest");
            lines.add("Interest income: " + interest.toPlainString());
            steps.add("tax:row:withheld");
            lines.add("Tax withheld: " + withheld.toPlainString());
        }

        @Override
        protected void footer() {
            steps.add("tax:total");
            lines.add("Net owed: " + interest.subtract(withheld).toPlainString());
        }
    }

    /** Slot-filler 2: the account statement. Same skeleton, different content. */
    public static final class StatementReport extends ReportGenerator {

        private final String accountId;
        private final List<StatementRow> rows;

        public StatementReport(String accountId, List<StatementRow> rows) {
            this.accountId = accountId;
            this.rows = List.copyOf(rows);
        }

        @Override
        protected void header() {
            steps.add("statement:title");
            lines.add("Statement for " + accountId);
        }

        @Override
        protected void rows() {
            for (StatementRow row : rows) {
                steps.add("statement:row:" + row.id());
                lines.add(row.id() + " " + row.description() + " "
                        + (row.signedAmount().signum() >= 0 ? "+" : "") + row.signedAmount().toPlainString());
            }
        }

        @Override
        protected void footer() {
            BigDecimal closing = rows.stream()
                    .map(StatementRow::signedAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .setScale(2);
            steps.add("statement:total");
            lines.add("Closing balance: " + closing.toPlainString());
        }
    }

    public static void main(String[] args) {
        banner("1. What a Template Method is, in one picture made of words");

        System.out.println("base class  : generate() = header(), rows(), footer() — in THAT order, final.");
        System.out.println("subclasses  : fill the three slots; they never see or change the order.");
        System.out.println("analogy     : a tax form. The government prints the layout; you fill the boxes.");

        banner("2. TaxReport runs the skeleton and records every step");

        TaxReport tax = new TaxReport(new BigDecimal("12.50"), new BigDecimal("2.50"));
        List<String> taxSteps = tax.generate();
        System.out.println("step 1: content  = " + tax.lines());
        System.out.println("step 2: recorded = " + taxSteps);
        System.out.println("verdict: skeleton markers (header/rows/footer) interleave with tax: slot markers.");

        banner("3. StatementReport: SAME skeleton order, different slot content");

        StatementReport statement = new StatementReport("ACC-1", List.of(
                new StatementRow("TXN-1", "Payroll", new BigDecimal("2450.00")),
                new StatementRow("TXN-2", "Rent", new BigDecimal("-1200.00"))));
        List<String> statementSteps = statement.generate();
        System.out.println("step 1: content  = " + statement.lines());
        System.out.println("step 2: recorded = " + statementSteps);
        System.out.println("verdict: generate:start/header/rows/footer/generate:end — identical frame,");
        System.out.println("         because the frame belongs to the FINAL method in the base class.");

        banner("4. Where you already met it");

        System.out.println("AbstractList (collections-framework 02): indexOf, iterator and spliterator");
        System.out.println("are written ONCE over two abstract slots — get(int) and size().");
        System.out.println("Every ArrayList/LinkedList you ever used was a Template Method subclass.");
        System.out.println("stage-2 preview: JdbcTemplate.execute(sql, callback) — the Template owns");
        System.out.println("connection/exception/close; your lambda is the slot. The name says it.");

        banner("5. Trap: when NOT to use it");

        System.out.println("Template Method is INHERITANCE: one fixed skeleton, slots only.");
        System.out.println("If callers need to reorder or skip steps, pass functions instead");
        System.out.println("(that is Strategy, demo 2). Also: prefer it only when the ORDER is a rule,");
        System.out.println("not a coincidence — final generate() is a promise you must keep forever.");

        banner("6. In one sentence");
        System.out.println("Template Method freezes the algorithm's SHAPE in a final base-class method");
        System.out.println("and lets subclasses vary only the steps — order becomes unbreakable policy.");
    }

    static void banner(String title) {
        System.out.println();
        System.out.println("=== " + title + " ===");
    }
}
