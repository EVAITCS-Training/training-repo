package com.evaitcs.collections.why;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Demo 1 — THE MOTIVATION: everything an array cannot do.
 *
 * An array is a fixed-size, contiguous block of memory with exactly ONE behavior of its
 * own: arr[i]. Everything else — growing, removing, searching, printing — is hand-rolled.
 * Each banner below is one wall; the Collections Framework exists to demolish them.
 *
 * The final banner is the trap students walk into first: Arrays.asList looks like a
 * bridge to the collections world, but it hands the array's fixed size straight through.
 */
public class ArraysHitAWallDemo {

    @SuppressWarnings("unused")
    public static void main(String[] args) {
        banner("1. Wall #1 — FIXED SIZE: the fourth customer does not fit");

        Account[] accounts = new Account[3];
        accounts[0] = new Account("ACC-1027", "Dana", new BigDecimal("2450.00"));
        accounts[1] = new Account("ACC-1005", "Ana", new BigDecimal("86.40"));
        accounts[2] = new Account("ACC-1011", "Bo", new BigDecimal("1200.00"));
        System.out.println("length = " + accounts.length + " — decided at creation, forever.");
        // accounts[3] = ... would not even compile-safe: ArrayIndexOutOfBoundsException.
        try {
            accounts[3] = new Account("ACC-1099", "Cid", new BigDecimal("10.00"));
        } catch (ArrayIndexOutOfBoundsException e) {
            System.out.println("CAUGHT -> " + e.getClass().getSimpleName() + ": " + e.getMessage());
        }
        // The ONLY "resize" is: allocate a new array and copy. By hand. Every time.
        accounts = Arrays.copyOf(accounts, 4);
        accounts[3] = new Account("ACC-1099", "Cid", new BigDecimal("10.00"));
        System.out.println("after copyOf(4): length = " + accounts.length + " — we paid a full copy for one add.");
        System.out.println("A List does this internally (ArrayList grows ~1.5x, amortized O(1)) — module 02.");

        banner("2. Wall #2 — REMOVING IS MANUAL SURGERY (shift everything after it)");

        System.out.println("before : " + Arrays.toString(accounts));
        // Remove index 1 ("Ana"): slide elements 2..n one slot left, null the tail,
        // and remember the new logical size YOURSELF — the array still says length 4.
        System.arraycopy(accounts, 2, accounts, 1, accounts.length - 2);
        accounts[accounts.length - 1] = null;
        System.out.println("after  : " + Arrays.toString(accounts) + "  <- length is STILL " + accounts.length);
        System.out.println("list.remove(1) is this whole procedure, tested by people who wrote it once, correctly.");

        banner("3. Wall #3 — NO BEHAVIOR AT ALL: search and print are on you");

        // contains? indexOf? sort? Print nicely? The array knows none of it.
        boolean found = false;
        for (int i = 0; i < accounts.length; i++) {
            if (accounts[i] != null && accounts[i].getId().equals("ACC-1011")) {
                found = true;
                break;
            }
        }
        System.out.println("hand-rolled search for ACC-1011: found = " + found);
        System.out.println("the same question to a List     : accounts.contains(...) / indexOf(...) — one call, uses equals()");
        // And printing an array directly is noise — arrays inherit Object.toString:
        System.out.println("println(array)   -> " + accounts + "   <- class name @ hash. Useless.");
        System.out.println("Arrays.toString  -> " + Arrays.toString(accounts));

        banner("4. Wall #4 — ARRAYS ARE COVARIANT: the type check happens at RUNTIME");

        String[] names = { "Dana", "Ana" };
        Object[] objects = names;          // compiles! arrays allow this ("covariance")
        try {
            objects[0] = Integer.valueOf(42);   // ...and punishes it later, at runtime
        } catch (ArrayStoreException e) {
            System.out.println("CAUGHT -> " + e.getClass().getSimpleName()
                    + " — an Integer slipped past the compiler into a String[].");
        }
        // List<String> is INVARIANT: the same mistake is a compile error, not a 2 a.m. pager.
        //   List<String> strings = new ArrayList<String>();
        //   List<Object> objs = strings;      // <- does not compile. The seatbelt works.
        System.out.println("List<String> refuses the same assignment at COMPILE time — generics are the seatbelt.");

        banner("5. The trap — Arrays.asList hands the wall straight through");

        // Fresh, clean data (toArray(new Account[0]) is the typed way back to array-land).
        Account[] trio = Account.sampleAccounts().toArray(new Account[0]);
        List<Account> view = Arrays.asList(trio);
        view.set(0, new Account("ACC-2000", "Eve", new BigDecimal("5.00")));   // set() works: same slot
        System.out.println("set(0) worked — asList is a VIEW over the very same array.");
        System.out.println("and the write went THROUGH the view into the array: trio[0] = " + trio[0]);
        try {
            view.add(new Account("ACC-3000", "Fay", new BigDecimal("1.00")));
        } catch (UnsupportedOperationException e) {
            System.out.println("CAUGHT -> " + e.getClass().getSimpleName()
                    + " — add() cannot grow a fixed-size array. The view inherits the wall.");
        }
        // The copy idiom — memorize it, you will type it every project:
        List<Account> real = new ArrayList<Account>(Arrays.asList(trio));
        real.add(new Account("ACC-3000", "Fay", new BigDecimal("1.00")));
        System.out.println("new ArrayList<Account>(Arrays.asList(...)) copies into a real List: size = " + real.size());
    }

    static void banner(String title) {
        System.out.println();
        System.out.println("=== " + title + " ===");
    }
}
