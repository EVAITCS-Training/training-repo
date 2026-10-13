package com.evaitcs.collections.sorting;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Demo 5 — the TRUE immutables: emptyList, singletonList, nCopies.
 *
 * Demo 4's wrappers were windows onto a mutable original: writes through the window
 * throw, but the original can still change behind your back. These three helpers are
 * different in kind. There IS no original. Collections.emptyList(), singletonList(x)
 * and nCopies(n, x) return objects that hold their (zero, one, or n identical) elements
 * and NOTHING else — no backing collection anyone can mutate. Every mutator throws
 * UnsupportedOperationException, from every direction, forever. These are the real
 * frozen lists, and they are free: emptyList/singletonList allocate one tiny object,
 * nCopies allocates no array at all (it stores n and the one element, and get(i) just
 * checks the range).
 *
 * Where they earn their keep:
 *   - returning "no results" without allocating: return Collections.emptyList();
 *   - a one-element List parameter fed from a single object: singletonList(account)
 *   - pre-filling: new ArrayList<Integer>(Collections.nCopies(12, Integer.valueOf(0)))
 *     — nCopies is immutable, but it is a perfect COLLECTION argument to a copy constructor.
 *
 * One subtlety nCopies shares with its immutability: all n slots are the SAME instance
 * (==), not n copies of it. With immutable elements (Integer, String) nobody can tell;
 * with mutable elements, changing "one" changes "all" — because there is only one.
 */
public class ImmutableHelpersDemo {

    public static void main(String[] args) {
        banner("1. emptyList — the free 'no results'");

        List<Account> none = Collections.emptyList();
        System.out.println("size = " + none.size() + ", isEmpty = " + none.isEmpty());
        try {
            none.add(Account.sampleAccounts().get(0));
        } catch (UnsupportedOperationException e) {
            System.out.println("CAUGHT -> " + e.getClass().getSimpleName() + " from emptyList().add(...)");
        }
        System.out.println("No backing list exists to mutate — this is NOT a wrapper, so there is no window");
        System.out.println("to walk around. 'Unmodifiable' the demo-4 way is a VIEW; this is a STATE.");

        banner("2. singletonList — exactly one element, no array, no growth");

        Account dana = Account.sampleAccounts().get(0);
        List<Account> one = Collections.singletonList(dana);
        System.out.println("one.get(0) = " + one.get(0) + ", size = " + one.size());
        try {
            one.set(0, new Account("ACC-9999", "Mallory", BigDecimal.ZERO));
        } catch (UnsupportedOperationException e) {
            System.out.println("CAUGHT -> " + e.getClass().getSimpleName() + " from singletonList.set(0, ...)");
        }
        try {
            one.add(dana);
        } catch (UnsupportedOperationException e) {
            System.out.println("CAUGHT -> " + e.getClass().getSimpleName() + " from singletonList.add(...)");
        }
        System.out.println("Even SETTING the only slot throws — the slot does not exist, the element does.");

        banner("3. nCopies — n slots, ONE instance, zero allocation of the array");

        List<Account> three = Collections.nCopies(3, dana);
        System.out.println("nCopies(3, dana) = " + three);
        System.out.println("three.get(0) == three.get(2) ? " + (three.get(0) == three.get(2))
                + "   <- the SAME object in every slot, not three copies");
        try {
            three.set(1, dana);
        } catch (UnsupportedOperationException e) {
            System.out.println("CAUGHT -> " + e.getClass().getSimpleName() + " from nCopies.set(1, ...)");
        }
        System.out.println("With a MUTABLE element, 'changing one slot' would change them all — there is only one.");
        System.out.println("The classic legal use is as a copy-constructor argument, filling a fresh list:");
        List<Integer> zeroes = new ArrayList<Integer>(Collections.nCopies(5, Integer.valueOf(0)));
        zeroes.set(2, Integer.valueOf(42));                      // NOW it is mutable — it is an ArrayList
        System.out.println("new ArrayList<Integer>(nCopies(5, 0)) then set(2, 42) -> " + zeroes);

        banner("4. The taxonomy, in one breath");

        System.out.println("  leak      : return internal;                  caller mutates your data");
        System.out.println("  snapshot  : return new ArrayList<Account>(internal); caller mutations die in the copy");
        System.out.println("  window    : unmodifiableList(internal);       writes throw, original changes show through (demo 4)");
        System.out.println("  frozen    : emptyList / singletonList / nCopies; nothing exists to change, ever");
        System.out.println("Module 08 turns the first three lines into an API design lesson.");
    }

    static void banner(String title) {
        System.out.println();
        System.out.println("=== " + title + " ===");
    }
}
