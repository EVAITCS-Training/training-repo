package com.evaitcs.collections.choosing;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Demo 4 — THE BOXING TRAPS: collections store OBJECTS, so primitives get wrapped,
 * and the wrappers have an identity quirk that bites exactly once per career — usually
 * in production.
 *
 * A List cannot hold an int: generics work on reference types only, so every primitive
 * is AUTOBOXED into its wrapper (int -> Integer, core-java module 02). Two traps ride along:
 *
 *   Trap 1 — THE INTEGER CACHE. Integer.valueOf(n) returns a SHARED, cached instance for
 *   -128..127 and a NEW object outside that range. So valueOf(127) == valueOf(127) is
 *   TRUE (same cached object) and valueOf(128) == valueOf(128) is FALSE (two objects).
 *   Code that compares boxed values with == passes every small-number test and fails on
 *   the first balance above 127. Rule: NEVER compare boxed values with ==; use equals(),
 *   or unbox to int first. (Hash-based collections are immune — they use equals/hashCode,
 *   module 05 — which is why this bug hides in == checks, not in map lookups.)
 *
 *   Trap 2 — remove(int) vs remove(Integer) on a List<Integer>. The int literal binds to
 *   the INDEX overload remove(int) — "remove slot 0", not "remove the value 0". The two
 *   overloads do completely different jobs and the compiler picks by static type, silently.
 *   Back-ref module 02: this is the price of List's indexed vocabulary.
 *
 * Bonus trap, printed at the end: autoUNboxing a map miss. get() returns null for an
 * absent key; assigning that to an int dereferences null — NPE at the assignment line,
 * pointing at the unboxing, not at the missing data.
 */
public class BoxingAndIdentityDemo {

    public static void main(String[] args) {
        banner("1. The Integer cache — == is TRUE at 127 and FALSE at 128");

        Integer a127 = Integer.valueOf(127);
        Integer b127 = Integer.valueOf(127);
        Integer a128 = Integer.valueOf(128);
        Integer b128 = Integer.valueOf(128);
        System.out.println("Integer.valueOf(127) == Integer.valueOf(127) ? " + (a127 == b127) + "   <- SAME cached object");
        System.out.println("Integer.valueOf(128) == Integer.valueOf(128) ? " + (a128 == b128) + "  <- two DIFFERENT objects");
        System.out.println("a128.equals(b128)                            ? " + a128.equals(b128) + "   <- equals compares VALUES, always right");
        System.out.println("Cache range: -128..127 (Integer.valueOf javadoc). Autoboxing in a loop uses valueOf,");
        System.out.println("so small numbers silently share instances — and == 'works' in every unit test you wrote.");
        System.out.println("RULE: never compare boxed values with ==. Unbox (int x = a128) or use equals().");

        banner("2. List<Integer>: remove(0) removes the SLOT; remove(Integer.valueOf(0)) removes the VALUE");

        List<Integer> nums = new ArrayList<Integer>();
        nums.add(Integer.valueOf(10));
        nums.add(Integer.valueOf(20));
        nums.add(Integer.valueOf(30));
        System.out.println("start: " + nums);

        Integer byIndex = nums.remove(0);                    // int literal -> remove(int index)!
        System.out.println("nums.remove(0)                 -> returned " + byIndex + ", list now " + nums
                + "   (slot 0 is gone — the VALUE 10 happened to live there)");

        boolean byValue = nums.remove(Integer.valueOf(20));  // Integer argument -> remove(Object)
        System.out.println("nums.remove(Integer.valueOf(20)) -> returned " + byValue + ", list now " + nums
                + "   (the value 20 is gone, wherever it sat)");
        System.out.println("One character of difference — 20 vs Integer.valueOf(20) — picks a completely different");
        System.out.println("method. The compiler chooses by STATIC type and will not warn you (module 02).");

        banner("3. Auto-unboxing a map miss — the NPE that blames the wrong line");

        Map<String, Integer> balances = new HashMap<String, Integer>();
        balances.put("ACC-1005", Integer.valueOf(86));
        Integer boxed = balances.get("ACC-1005");
        System.out.println("get(\"ACC-1005\") -> " + boxed + " (present)");
        try {
            int missing = balances.get("ACC-9999");          // get returns null -> unboxing dereferences it
            System.out.println(missing);
        } catch (NullPointerException e) {
            System.out.println("CAUGHT -> " + e.getClass().getSimpleName() + " from `int missing = balances.get(\"ACC-9999\")`");
        }
        System.out.println("The stack trace points at the assignment, but the bug is the ABSENT KEY. Check");
        System.out.println("containsKey (module 05), receive into Integer, or use getOrDefault (java8-features).");
    }

    static void banner(String title) {
        System.out.println();
        System.out.println("=== " + title + " ===");
    }
}
