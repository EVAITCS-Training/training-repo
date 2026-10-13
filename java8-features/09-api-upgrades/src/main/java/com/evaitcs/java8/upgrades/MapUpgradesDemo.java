package com.evaitcs.java8.upgrades;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * The Map interface got the biggest daily-win upgrade in Java 8. For EVERY method
 * below, we print the pre-Java-8 idiom next to the one-liner that replaced it.
 *
 * WHY this matters in class: every one of these was a 2-5 line containsKey/get/put
 * dance that everyone had memorized - and everyone had subtly different (buggy) versions.
 * The new methods are one lookup, one intention, one line.
 */
public class MapUpgradesDemo {

    private static final BigDecimal DEFAULT_FEE = new BigDecimal("5.00");

    public static void main(String[] args) {
        List<Transaction> statement = Transaction.sampleStatement();

        System.out.println("=== getOrDefault - READ with a fallback (was: containsKey ? get : default) ===");
        Map<String, BigDecimal> feeSchedule = new TreeMap<>();
        feeSchedule.put("Austin", new BigDecimal("2.50"));
        feeSchedule.put("Dallas", new BigDecimal("3.00"));
        // Java 7:  BigDecimal fee = feeSchedule.containsKey(city) ? feeSchedule.get(city) : DEFAULT_FEE;
        //          ...TWO hash lookups, and get() could still return null if the key maps to null!
        // Java 8:  one lookup, one line:
        System.out.println("   Austin  fee = " + feeSchedule.getOrDefault("Austin", DEFAULT_FEE));
        System.out.println("   Houston fee = " + feeSchedule.getOrDefault("Houston", DEFAULT_FEE) + "  (absent -> fallback)");

        System.out.println("=== putIfAbsent - WRITE only if nothing is there yet (was: if(!containsKey) put) ===");
        Map<String, String> firstTxnByCity = new TreeMap<>();
        for (Transaction t : statement) {
            // Java 7:  if (!firstTxnByCity.containsKey(t.getCity())) {
            //              firstTxnByCity.put(t.getCity(), t.getId());
            //          }
            firstTxnByCity.putIfAbsent(t.getCity(), t.getId());
        }
        System.out.println("   first transaction per city = " + firstTxnByCity);
        System.out.println("   (Austin keeps T-101; the later T-102/T-106 did NOT overwrite it)");

        System.out.println("=== merge - THE summing one-liner (was: get, null-check, add, put) ===");
        Map<String, BigDecimal> feesByCity = new TreeMap<>();
        BigDecimal perTxnFee = new BigDecimal("1.50");
        for (Transaction t : statement) {
            // Java 7:  BigDecimal soFar = feesByCity.get(t.getCity());
            //          feesByCity.put(t.getCity(), soFar == null ? perTxnFee : soFar.add(perTxnFee));
            // Java 8: read it out loud - "merge the fee INTO the city total by ADDing":
            feesByCity.merge(t.getCity(), perTxnFee, BigDecimal::add);
        }
        System.out.println("   fees collected per city = " + feesByCity);

        System.out.println("=== merge's surprise: remapping to null REMOVES the key ===");
        // Dallas owes 3.00 (2 txns x 1.50). A promo credit of exactly 3.00 zeroes it out,
        // and our remapping function returns null for zero - so the key disappears entirely.
        feesByCity.merge("Dallas", new BigDecimal("3.00"), (oldFee, credit) -> {
            BigDecimal remaining = oldFee.subtract(credit);
            return remaining.signum() == 0 ? null : remaining;   // null => REMOVE the mapping
        });
        System.out.println("   after full Dallas waiver = " + feesByCity + "  (Dallas key is GONE)");
        // TRAP: the null trick applies to the RESULT of the remapping function. Passing null as
        // the `value` argument itself throws NullPointerException - merge never accepts null input.

        System.out.println("=== replaceAll - transform every VALUE in place ===");
        // Java 7:  for (Map.Entry<String, BigDecimal> e : feesByCity.entrySet())
        //              e.setValue(e.getValue().multiply(...));   // entrySet ceremony
        // 10% loyalty discount on every remaining fee:
        feesByCity.replaceAll((city, oldFee) ->
                oldFee.multiply(new BigDecimal("0.90")).setScale(2, RoundingMode.HALF_UP));
        System.out.println("   after 10% discount     = " + feesByCity);

        System.out.println("=== forEach - iterate entries without entrySet boilerplate ===");
        // Java 7:  for (Map.Entry<String, BigDecimal> e : feesByCity.entrySet()) { ... }
        // Java 8: hand the map a (key, value) lambda - a BiConsumer, straight from module 01:
        feesByCity.forEach((city, total) -> System.out.println("   " + city + " owes " + total));
        // NOTE: forEach makes no ordering promise on HashMap - we used TreeMap so the
        // projector output is stable. Say this out loud; students will ask.
    }
}
