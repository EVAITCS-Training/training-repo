package com.evaitcs.collections.sorting;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Demo 2 — binarySearch: the halving search, its miss encoding, and its silent garbage mode.
 *
 * Collections.binarySearch(list, key) is the O(log n) search: compare with the middle,
 * throw away the impossible half, repeat. Five elements -> at most 3 comparisons;
 * a million elements -> at most 20. That speed has a PRICE, and the price is the contract:
 *
 *   THE LIST MUST BE SORTED (by the same order the search uses).
 *
 * If it isn't, the result is UNDEFINED — and "undefined" here does not mean "throws":
 * it means a confidently wrong number with NO exception, NO warning, nothing. Banner 3
 * prints the evidence: elements that ARE in the list reported as "not found", and a
 * nonsense insertion point for an element at index 0. This is the module's quietest trap
 * because the failure looks like data loss, not like a bug.
 *
 * The return value has two modes:
 *   found    -> the index of the key (any one of them if duplicates exist);
 *   not found-> -(insertionPoint) - 1, where insertionPoint is the index of the first
 *               element GREATER than the key (or list.size() if all are smaller).
 * Why the minus-one dance? Because "not found at index 0" and "not found, would go at
 * index 0" must be distinguishable: plain -ip would make insertion point 0 return 0,
 * indistinguishable from a real hit at index 0. Shifting by one and negating keeps every
 * miss strictly negative. Decoding: insertionPoint = -(result) - 1.
 */
public class BinarySearchDemo {

    public static void main(String[] args) {
        banner("1. FOUND — on a sorted list, it returns the index");

        List<String> ids = new ArrayList<String>();
        ids.add("T-1001");
        ids.add("T-1002");
        ids.add("T-1003");
        ids.add("T-1004");
        ids.add("T-1005");
        System.out.println("sorted list: " + ids);

        int found = Collections.binarySearch(ids, "T-1004");
        System.out.println("binarySearch(\"T-1004\") -> " + found + "   (>= 0 means FOUND)");
        System.out.println("verify: ids.get(" + found + ") = " + ids.get(found));

        banner("2. MISS — the result is -(insertionPoint) - 1; decode it by hand");

        // "T-10028" sorts AFTER "T-1002" (same prefix, one char longer) and BEFORE
        // "T-1003" ('2' < '3' at the fifth character) — so it belongs at index 2.
        String key = "T-10028";
        int miss = Collections.binarySearch(ids, key);
        System.out.println("binarySearch(\"" + key + "\") -> " + miss + "   (< 0 means NOT found)");

        // Decode: insertionPoint = -(result) - 1
        int insertionPoint = -(miss) - 1;
        System.out.println("decode: insertionPoint = -(" + miss + ") - 1 = " + insertionPoint);

        // Prove the decoding is meaningful: the first element greater than the key is there,
        // and inserting at that point KEEPS the list sorted. That is the whole promise.
        System.out.println("check : first element > key is ids.get(" + insertionPoint + ") = " + ids.get(insertionPoint));
        List<String> withKey = new ArrayList<String>(ids);
        withKey.add(insertionPoint, key);
        System.out.println("insert at the decoded point: " + withKey + "  <- still sorted. The encoding is useful, not spiteful.");

        banner("3. THE TRAP — on an UNSORTED list it returns garbage, with NO exception");

        List<String> unsorted = Arrays.asList("T-1005", "T-1001", "T-1004", "T-1002", "T-1003");
        System.out.println("unsorted list: " + unsorted + "   (chosen on purpose — deterministically wrong)");

        int garbage1 = Collections.binarySearch(unsorted, "T-1003");
        System.out.println("binarySearch(\"T-1003\") -> " + garbage1 + "   <- 'not found'... but T-1003 is AT INDEX 4!");
        int garbage2 = Collections.binarySearch(unsorted, "T-1005");
        System.out.println("binarySearch(\"T-1005\") -> " + garbage2 + "   <- decoded insertionPoint = " + (-(garbage2) - 1)
                + "... for the element AT INDEX 0!");
        System.out.println("No exception. No warning. The halving threw away the half that HELD the key.");
        System.out.println("Rule: sort first (same order!), or the answer is undefined — and undefined means SILENT.");

        banner("4. The Arrays toolbox — the same ideas, on arrays");

        int[] nums = { 5, 1, 3 };
        Arrays.sort(nums);
        System.out.println("Arrays.sort          -> " + Arrays.toString(nums) + "   (toString: arrays print as garbage without it, module 01)");
        System.out.println("Arrays.binarySearch(3) -> " + Arrays.binarySearch(nums, 3) + "   (same contract: sorted first, -(ip)-1 on miss)");

        int[] copy = Arrays.copyOf(nums, 3);
        System.out.println("Arrays.copyOf(nums,3)    -> " + Arrays.toString(copy));
        System.out.println("Arrays.equals(nums,copy) -> " + Arrays.equals(nums, copy) + "  (== would compare references; equals compares CONTENT)");

        int[] grown = Arrays.copyOf(nums, 5);
        System.out.println("Arrays.copyOf(nums,5)    -> " + Arrays.toString(grown) + "   (copy + grow; new slots get the default 0 — module 01's 'resize')");
        Arrays.fill(grown, 3, 5, 7);
        System.out.println("Arrays.fill(grown,3,5,7) -> " + Arrays.toString(grown) + "   (now equals(nums,grown) = " + Arrays.equals(nums, grown) + " — different content)");
        System.out.println("For objects, Arrays.equals/toString use each element's equals()/toString() — the module-01 engine again.");
    }

    static void banner(String title) {
        System.out.println();
        System.out.println("=== " + title + " ===");
    }
}
