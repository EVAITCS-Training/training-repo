package com.evaitcs.corejava.ordering;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

/**
 * Demo 2 — the compareTo CONTRACT: what the returned int actually means.
 *
 * Three rules, all printed with real numbers below:
 *   1. Only the SIGN matters: negative / zero / positive. NOT -1 / 0 / 1.
 *   2. Antisymmetry: sgn(x.compareTo(y)) == -sgn(y.compareTo(x)).
 *   3. Consistency with equals (module 08 planted this flag): sorted collections
 *      decide "same element" with compareTo, NEVER with equals.
 *
 * Transaction.compareTo (formally introduced in the next demo) is already used here —
 * it delegates to String.compareTo, so it obeys all three rules by construction.
 */
public class CompareToContractDemo {

    public static void main(String[] args) {
        banner("1. The SIGN, not the number — real return values");

        Transaction a = new Transaction("T-1005", "Interest", new BigDecimal("3.11"), 1_700_000_200_000L);
        Transaction b = new Transaction("T-1040", "Payroll",  new BigDecimal("2450.00"), 1_700_000_400_000L);
        Transaction a2 = new Transaction("T-1005", "Interest copy", new BigDecimal("3.11"), 1_700_000_200_000L);

        System.out.println("a.compareTo(b) = " + a.compareTo(b) + "   <- NEGATIVE: a sorts BEFORE b");
        System.out.println("a.compareTo(a2) = " + a.compareTo(a2) + "      <- ZERO: same id, a tie");
        System.out.println("b.compareTo(a) = " + b.compareTo(a) + "    <- POSITIVE: b sorts AFTER a");
        System.out.println("The value -4 is String.compareTo's char difference ('0' vs '4').");
        System.out.println("No caller may rely on the MAGNITUDE — only on <0, ==0, >0.");

        banner("2. Antisymmetry: sgn(x.compareTo(y)) == -sgn(y.compareTo(x))");

        java.util.List<Transaction> statement = Transaction.sampleStatement();
        boolean symmetric = true;
        for (Transaction x : statement) {
            for (Transaction y : statement) {
                if (Integer.signum(x.compareTo(y)) != -Integer.signum(y.compareTo(x))) {
                    symmetric = false;
                }
            }
        }
        System.out.println("checked all " + (statement.size() * statement.size())
                + " ordered pairs of the sample statement: antisymmetric = " + symmetric);
        System.out.println("Break this and the sort's answer depends on WHICH element it asks first.");

        banner("3. Consistency with equals — module 08's flag, harvested");

        BigDecimal twoOh = new BigDecimal("2.0");
        BigDecimal twoOhOh = new BigDecimal("2.00");
        System.out.println("2.0.equals(2.00)    = " + twoOh.equals(twoOhOh) + " <- scale is part of equals");
        System.out.println("2.0.compareTo(2.00) = " + twoOh.compareTo(twoOhOh) + "     <- numerically a TIE");

        Set<BigDecimal> hash = new HashSet<BigDecimal>();
        hash.add(twoOh);
        hash.add(twoOhOh);
        Set<BigDecimal> tree = new TreeSet<BigDecimal>();   // no-arg TreeSet => uses compareTo
        tree.add(twoOh);
        tree.add(twoOhOh);
        System.out.println("HashSet size (equals)    = " + hash.size() + "   TreeSet size (compareTo) = " + tree.size());
        System.out.println("=> Which one does TreeSet use? compareTo. It never even calls equals.");

        banner("4. The same thing happens to OUR Transaction — it 'loses' one");

        Transaction rent = new Transaction("T-1099", "Rent", new BigDecimal("1200.00"), 1_700_000_100_000L);
        Transaction correction = new Transaction("T-1099", "Rent correction", new BigDecimal("-0.01"), 1_700_000_999_000L);
        System.out.println("rent.equals(correction)    = " + rent.equals(correction) + " <- default identity equals (module 08)");
        System.out.println("rent.compareTo(correction) = " + rent.compareTo(correction) + "     <- same id => tie => 'same element'");

        Set<Transaction> uniqueById = new TreeSet<Transaction>();
        uniqueById.add(rent);
        uniqueById.add(correction);
        System.out.println("TreeSet size = " + uniqueById.size() + "  — the correction VANISHED, silently.");
        System.out.println("RULE: compareTo() == 0 exactly when equals() is true — or consciously decide,");
        System.out.println("as we do here, that the id IS the identity (then give the class an id-based equals).");
    }

    static void banner(String title) {
        System.out.println();
        System.out.println("=== " + title + " ===");
    }
}
