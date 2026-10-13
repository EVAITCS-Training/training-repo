package com.evaitcs.collections.lists;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Demo 4 — THE FOUR CLASSIC LIST PITFALLS, each reproduced deterministically, then fixed.
 *
 * (a) A FORWARD index loop with remove(i) SKIPS elements — the shift and the index move in
 *     opposite directions; the fix is to walk backward (or iterator.remove() — module 03,
 *     or list.removeIf(...) — java8-features 01).
 * (b) On a List<Integer>, remove(1) deletes the SLOT at position 1; remove(Integer.valueOf(1))
 *     deletes the VALUE 1 — two overloads, one character apart (autoboxing from core-java 02,
 *     now with teeth).
 * (c) Growing an ArrayList one element at a time re-runs the 10 -> 15 -> 22 -> ... ladder;
 *     ONE constructor argument (initialCapacity) skips it.
 * (d) Arrays.asList hands you a fixed-size VIEW wearing List clothes — module 01's wall, back
 *     for an encore, because everybody keeps meeting it again.
 */
public class RemovePitfallsDemo {

    public static void main(String[] args) {
        banner("1. Pitfall (a) — forward loop + remove(i) skips elements");

        // Copy idiom from module 01: Arrays.asList itself is fixed-size and would throw on remove.
        List<String> kinds = new ArrayList<String>(Arrays.asList("PAY", "FEE", "PAY", "FEE", "FEE"));
        System.out.println("a week of postings: " + kinds + "   — task: delete every FEE");
        for (int i = 0; i < kinds.size(); i++) {
            if (kinds.get(i).equals("FEE")) {
                kinds.remove(i);
                // The trap: remove(i) slides everything AFTER the hole one slot LEFT — including
                // the element that should be examined next. Then i++ steps RIGHT. The two motions
                // cancel: the successor of every removed FEE is never looked at.
                System.out.println("  removed at i=" + i + " -> list is now " + kinds + "; i++ just walked past the shifted-in neighbor");
            }
        }
        System.out.println("result: " + kinds + "   <- a FEE SURVIVED. The loop never looked at it.");
        System.out.println();
        List<String> fixed = new ArrayList<String>(Arrays.asList("PAY", "FEE", "PAY", "FEE", "FEE"));
        for (int i = fixed.size() - 1; i >= 0; i--) {
            if (fixed.get(i).equals("FEE")) {
                fixed.remove(i);
                // Backward, the shift only moves elements AFTER i — and those were already examined.
            }
        }
        System.out.println("the classic fix — iterate BACKWARD: " + fixed + "   <- every FEE gone");
        System.out.println("Also correct: iterator.remove() (module 03 — the idiomatic in-loop fix) and list.removeIf(...)");
        System.out.println("(Java 8 collapses this whole banner into ONE line — java8-features 01).");

        banner("2. Pitfall (b) — List<Integer>: remove(int) is the SLOT, remove(Object) is the VALUE");

        List<Integer> slots = new ArrayList<Integer>(Arrays.asList(Integer.valueOf(10), Integer.valueOf(20), Integer.valueOf(30)));
        Integer gone = slots.remove(1);                          // a bare int binds to remove(int index) — no boxing needed
        System.out.println("slots  = [10, 20, 30];  remove(1)                  -> returned " + gone + ", list " + slots);
        System.out.println("                          POSITION 1 was deleted. The value 1 was never involved.");

        List<Integer> values = new ArrayList<Integer>(Arrays.asList(Integer.valueOf(10), Integer.valueOf(1), Integer.valueOf(30)));
        boolean found = values.remove(Integer.valueOf(1));       // an Integer binds to remove(Object) — an equals() scan
        System.out.println("values = [10, 1, 30];   remove(Integer.valueOf(1)) -> returned " + found + ", list " + values);
        System.out.println("                          the VALUE 1 was deleted — from position 1, which is a coincidence, not an index.");
        System.out.println("The signatures tell the story: remove(int) RETURNS the element; remove(Object) RETURNS a boolean.");
        System.out.println("Bank version of the bug: cents.remove(1) silently deletes the SECOND row instead of the 1-cent fee.");

        banner("3. Pitfall (c) — the regrowth ladder, and the one-argument fix");

        System.out.println("new ArrayList<Transaction>() + a 100,000-row CSV import:");
        System.out.println("  the ladder 10 -> 15 -> 22 -> ... -> 1234 -> ... reallocates-and-copies ~30 times (~206,000 slot moves).");
        System.out.println("new ArrayList<Transaction>(100_000): ONE allocation, zero copies.");
        List<Transaction> presized = new ArrayList<Transaction>(100_000);
        System.out.println("  presized list built, size = " + presized.size() + " — the argument is a HINT about the future; size still starts at 0.");
        System.out.println("Use it whenever the final size is known: CSV imports, ResultSet drains, splitting a statement by month.");
        System.out.println("(ArrayList.ensureCapacity(n) is the same idea after the fact.)");

        banner("4. Pitfall (d) — Arrays.asList is a view (module 01's wall, back again)");

        String[] rows = { "TXN-1", "TXN-2", "TXN-3" };
        List<String> view = Arrays.asList(rows);
        view.set(0, "TXN-1 (corrected)");                        // set() works — and writes THROUGH to the array
        System.out.println("set(0) wrote through to the array: rows[0] = " + rows[0]);
        try {
            view.add("TXN-4");                                   // add() cannot grow a fixed-size array
        } catch (UnsupportedOperationException e) {
            System.out.println("CAUGHT -> " + e.getClass().getSimpleName() + " — the returned List is real, but FIXED-SIZE.");
        }
        List<String> real = new ArrayList<String>(view);         // module 01's copy idiom, again
        real.add("TXN-4");
        System.out.println("new ArrayList<String>(Arrays.asList(rows)) is growable: " + real);
        System.out.println();
        System.out.println("All four pitfalls in one sentence each: walk backward to delete; box to delete by value;");
        System.out.println("presize when you know; copy what asList hands you.");
    }

    static void banner(String title) {
        System.out.println();
        System.out.println("=== " + title + " ===");
    }
}
