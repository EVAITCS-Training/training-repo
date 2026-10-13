package com.evaitcs.corejava.refs;

import java.util.ArrayList;
import java.util.List;

/**
 * Wrapper classes & autoboxing — and the Integer cache trap that makes == lie.
 *
 * WHY WRAPPERS EXIST: generics and the collections framework operate on OBJECTS. There is no
 * `List<int>` — only `List<Integer>`. Boxing is the compiler quietly wrapping a primitive in its
 * object; unboxing is unwrapping it. It's sugar, not magic, and it has two sharp edges.
 */
public class BoxingAndCacheDemo {

    public static void main(String[] args) {
        banner("1. Why wrappers exist: collections hold OBJECTS, not primitives");
        // List<int> bad = new ArrayList<int>(); // COMPILE ERROR: "unexpected type" — int is not a reference type
        List<Integer> numbers = new ArrayList<Integer>();
        numbers.add(7); // autoboxing: the compiler rewrites this to numbers.add(Integer.valueOf(7))
        int back = numbers.get(0); // unboxing: compiler rewrites to numbers.get(0).intValue()
        System.out.println("List<Integer> held 7 and handed back " + back + " (an int).");

        banner("2. Boxing/unboxing is just compiler sugar");
        Integer boxed = Integer.valueOf(42); // exactly what `Integer b = 42;` compiles down to
        int unboxed = boxed.intValue();      // exactly what `int u = boxed;` compiles down to
        System.out.println("Integer.valueOf(42) = " + boxed + " ; .intValue() = " + unboxed);

        banner("3. The Integer CACHE TRAP — == on boxed numbers LIES");
        Integer lo1 = 127, lo2 = 127; // 127 is INSIDE the cached range -128..127 -> the SAME shared object
        Integer hi1 = 128, hi2 = 128; // 128 is OUTSIDE the cache -> two DIFFERENT objects
        System.out.println("Integer 127 == 127 : " + (lo1 == lo2) + "   (cached: same object, == happens to work)");
        System.out.println("Integer 128 == 128 : " + (hi1 == hi2) + "  (not cached: different objects, == BREAKS)");
        System.out.println("127 .equals 127    : " + lo1.equals(lo2));
        System.out.println("128 .equals 128    : " + hi1.equals(hi2) + "   <- .equals compares VALUE: ALWAYS use it");
        System.out.println("Rule: NEVER use == on boxed numbers. The cache makes it work small and fail big.");

        banner("4. Unboxing a null throws NullPointerException");
        Integer maybeNull = null;
        try {
            int boom = maybeNull; // auto-unbox -> maybeNull.intValue() -> NPE (nothing to unwrap)
            System.out.println("never runs: " + boom);
        } catch (NullPointerException e) {
            System.out.println("caught " + e.getClass().getSimpleName() + " — unboxing null calls .intValue() on nothing.");
        }
    }

    static void banner(String title) {
        System.out.println();
        System.out.println("=== " + title + " ===");
    }
}
