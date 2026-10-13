package com.evaitcs.collections.why;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Demo 3 — ONE VOCABULARY FOR EVERY CONTAINER: the Collection interface itself.
 *
 * Whatever the container, the words are the same: add, remove, contains, size, isEmpty,
 * clear, toArray. Learn them once on List, and Set/Queue speak them too — that is what
 * "framework" means here.
 *
 * The sentence that explains half the bugs in this course: contains() and remove(Object)
 * locate elements with EQUALS, never with ==. An element that does not override equals
 * can only be found by identity — the same contract core-java 08 built, now CASHED IN.
 */
public class CollectionApiDemo {

    /** Deliberately WITHOUT equals/hashCode — the control group for banner 2. */
    static class RawAccount {
        private final String id;

        RawAccount(String id) {
            this.id = id;
        }

        @Override
        public String toString() {
            return "RawAccount(" + id + ")";
        }
    }

    public static void main(String[] args) {
        banner("1. The shared vocabulary — every Collection answers these");

        List<Account> accounts = Account.sampleAccounts();
        System.out.println("size()    = " + accounts.size());
        System.out.println("isEmpty() = " + accounts.isEmpty());
        accounts.add(new Account("ACC-1099", "Cid", new BigDecimal("10.00")));
        System.out.println("after add: size = " + accounts.size());

        Account bo = new Account("ACC-1011", "Bo", new BigDecimal("1200.00")); // equal, NOT identical
        System.out.println("contains(Bo-by-id) = " + accounts.contains(bo) + "   <- found via equals()");
        System.out.println("remove(Bo-by-id)   = " + accounts.remove(bo) + "   <- removed the ORIGINAL via equals()");
        System.out.println("size now = " + accounts.size());

        banner("2. The equals engine, side by side");

        // Account overrides equals by id -> an equal stranger is found.
        List<Account> good = new ArrayList<Account>();
        good.add(new Account("ACC-1005", "Ana", new BigDecimal("86.40")));
        System.out.println("Account (equals by id)   contains(stranger, same id) = "
                + good.contains(new Account("ACC-1005", "Someone Else", BigDecimal.ZERO)));

        // RawAccount does NOT override equals -> Object.equals is ==, identity only.
        List<RawAccount> raw = new ArrayList<RawAccount>();
        RawAccount original = new RawAccount("ACC-1005");
        raw.add(original);
        System.out.println("RawAccount (no equals)   contains(stranger, same id) = "
                + raw.contains(new RawAccount("ACC-1005")));
        System.out.println("                           contains(the VERY instance) = " + raw.contains(original));
        System.out.println("Same data, opposite answers. The Set and Map modules are built on this line.");

        banner("3. Collection equality — List cares about ORDER, Set does not");

        List<Account> a = Account.sampleAccounts();
        List<Account> b = Account.sampleAccounts();
        System.out.println("two identically-built Lists  equal? " + a.equals(b));
        java.util.Collections.swap(b, 0, 1);
        System.out.println("same elements, one swap      equal? " + a.equals(b) + "   <- List.equals is order-sensitive");

        Set<Account> s1 = new LinkedHashSet<Account>(a);
        Set<Account> s2 = new LinkedHashSet<Account>(b);
        System.out.println("the SAME two as Sets         equal? " + s1.equals(s2) + "    <- Set.equals ignores order");
        System.out.println("and a List vs a Set          equal? " + a.equals(s1) + "   <- ALWAYS false: equals checks the interface first");

        banner("4. The bridge back to arrays — toArray");

        Account[] typed = a.toArray(new Account[0]);   // typed overload: the modern idiom
        System.out.println("toArray(new Account[0]) -> " + java.util.Arrays.toString(typed));
        Object[] untyped = a.toArray();                // no-arg: Object[] only — legacy APIs
        System.out.println("toArray() element class : " + untyped[0].getClass().getSimpleName()
                + " (use the typed overload whenever the API allows)");
    }

    static void banner(String title) {
        System.out.println();
        System.out.println("=== " + title + " ===");
    }
}
