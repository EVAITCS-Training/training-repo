package com.evaitcs.corejava.tostring;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

/**
 * Demo 4 — collections call toString on EVERY element: your résumé, repeated.
 *
 * AbstractCollection.toString() (which ArrayList, HashSet, ArrayDeque... inherit)
 * walks the elements and appends String.valueOf(element) — i.e. element.toString() —
 * separated by ", " inside [ ]. So a collection is exactly as readable as its
 * ELEMENTS' toString: with AccountRaw elements you get a wall of fingerprints; with
 * Transaction or AccountPrinted elements, a statement you can read.
 *
 * The array trap closes the demo: arrays do NOT override toString, so println(array)
 * prints the DEFAULT fingerprint ([Lcom...Transaction;@hex) no matter how beautiful
 * the element toStrings are. Arrays.toString(arr) is the fix everyone forgets once.
 */
public class CollectionsToStringDemo {

    public static void main(String[] args) {

        banner("1. A list of RAW accounts — the wall of fingerprints");

        List<AccountRaw> raws = Arrays.asList(
                new AccountRaw("ACC-1001", "Ada Lovelace", new BigDecimal("1250.75"), "4921"),
                new AccountRaw("ACC-1002", "Grace Hopper", new BigDecimal("9300.10"), "7788"));
        System.out.println(raws);
        System.out.println("=> The list did its job: it called EACH element's toString. The elements");
        System.out.println("   just had nothing to say. Garbage in, garbage listed.");

        banner("2. Same data, good element toStrings — the list becomes readable");

        List<Transaction> statement = Transaction.sampleStatement();
        System.out.println("statement = " + statement);

        List<AccountPrinted> accounts = Arrays.asList(
                new AccountPrinted("ACC-1001", "Ada Lovelace", new BigDecimal("1250.75"), "4111111111111234", "4921"),
                new AccountPrinted("ACC-1002", "Grace Hopper", new BigDecimal("9300.10"), "5500000000005678", "7788"));
        System.out.println("accounts  = " + accounts);
        System.out.println("=> Now the [ ] and commas are all the collection contributes; every word");
        System.out.println("   of CONTENT came from YOUR toString, once per element. This is why");
        System.out.println("   Transaction keeps its résumé to one compact line.");

        banner("3. The array trap — arrays never got the memo");

        Transaction[] arr = statement.toArray(new Transaction[0]);
        System.out.println("println(arr)            : " + arr);
        System.out.println("println(Arrays.toString): " + Arrays.toString(arr));
        System.out.println("=> An array inherits Object.toString like any raw object: the '[L' prefix");
        System.out.println("   means 'array of reference type'. Element toStrings are NEVER consulted.");
        System.out.println("   Arrays.toString (or deepToString for nested arrays) walks it for you.");
    }

    static void banner(String title) {
        System.out.println();
        System.out.println("=== " + title + " ===");
    }
}
