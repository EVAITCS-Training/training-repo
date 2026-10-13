package com.evaitcs.corejava.builders;

/**
 * The part of a builder you cannot see: every StringBuilder/StringBuffer is a
 * char[] plus a used-count. capacity() = size of the array; length() = how much
 * of it is used. When an append no longer fits, the array is REPLACED by one of
 * size 2n+2 and everything built so far is copied across.
 *
 * That copy is cheap on average (amortized O(1) per append), but when you roughly
 * know the final size, pre-sizing removes every grow-and-copy. Section 4 proves it.
 */
public class CapacityGrowthDemo {

    public static void main(String[] args) {

        banner("1. Three constructors, three starting capacities");
        StringBuilder empty = new StringBuilder();                 // default: 16 spare slots
        StringBuilder sized = new StringBuilder(1000);             // exact: 1000 slots, 0 chars used
        StringBuilder seeded = new StringBuilder("SecureBank");    // seeded: 10 chars + 16 headroom
        System.out.println("new StringBuilder()             -> capacity " + empty.capacity());
        System.out.println("new StringBuilder(1000)         -> capacity " + sized.capacity());
        System.out.println("new StringBuilder(\"SecureBank\") -> capacity " + seeded.capacity()
                + "  (10 + 16: a seed leaves headroom on purpose)");
        System.out.println("Why pre-size? If you roughly know the final size, SAY SO —");
        System.out.println("the builder then does ZERO grow-and-copies instead of a dozen.");

        banner("2. The growth rule: newCapacity = old * 2 + 2");
        StringBuilder growing = new StringBuilder();
        System.out.println("start: capacity " + growing.capacity() + ", length " + growing.length());
        int lastCapacity = growing.capacity();
        for (int i = 1; i <= 40; i++) {
            growing.append('x');                       // one char at a time so we catch every growth
            if (growing.capacity() != lastCapacity) {
                System.out.println("  after append #" + i + ": capacity " + lastCapacity
                        + " -> " + growing.capacity()
                        + "   (= " + lastCapacity + " * 2 + 2)");
                lastCapacity = growing.capacity();
            }
        }
        System.out.println("watch for: 16 -> 34 -> 70. Each arrow = one new array + one arraycopy");
        System.out.println("of everything built so far. (JDK internals: AbstractStringBuilder.newCapacity)");

        banner("3. ensureCapacity — grow once, on purpose");
        StringBuilder ensured = new StringBuilder();
        ensured.ensureCapacity(5000);                  // "I'm about to append a lot — make room now"
        System.out.println("after ensureCapacity(5000): capacity = " + ensured.capacity()
                + "  (at least what you asked for)");

        banner("4. Why pre-size: counting grow-and-copies for a 100k-part statement line");
        int parts = 100_000;
        int defaultGrowths = countGrowths(new StringBuilder(), parts);
        int presizedGrowths = countGrowths(new StringBuilder(parts), parts);
        System.out.println("new StringBuilder()       : grew " + defaultGrowths + " times");
        System.out.println("new StringBuilder(" + parts + ") : grew " + presizedGrowths + " times");
        System.out.println("Every growth allocates a new array and copies all existing chars.");
        System.out.println("Pre-sizing turns " + defaultGrowths + " allocate-and-copy rounds into 1 allocation, 0 copies.");
        System.out.println("Rule of thumb: size from an estimate (rows * average line width). Never guess tiny.");
    }

    /** Appends `appends` chars one by one, counting how many times the capacity changed. */
    private static int countGrowths(StringBuilder sb, int appends) {
        int growths = 0;
        int last = sb.capacity();
        for (int i = 0; i < appends; i++) {
            sb.append('x');
            if (sb.capacity() != last) {
                growths++;
                last = sb.capacity();
            }
        }
        return growths;
    }

    static void banner(String title) {
        System.out.println();
        System.out.println("=== " + title + " ===");
    }
}
