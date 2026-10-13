package com.evaitcs.corejava.builders;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests as PROOF for the capacity claims made in CapacityGrowthDemo.
 * Each test prints the numbers it asserts — the test run doubles as the demo.
 */
class CapacityGrowthTest {

    @Test
    @DisplayName("the growth rule is newCapacity = old * 2 + 2: 16 -> 34 -> 70")
    void growthRuleIsTwoNPlusTwo() {
        StringBuilder sb = new StringBuilder();
        System.out.println("fresh StringBuilder: length=" + sb.length() + ", capacity=" + sb.capacity());
        assertEquals(16, sb.capacity(), "the documented default capacity");

        List<String> growths = new ArrayList<String>();
        int last = sb.capacity();
        for (int i = 1; i <= 70; i++) {          // 70 appends is exactly enough to grow twice
            sb.append('x');
            if (sb.capacity() != last) {
                growths.add(last + " -> " + sb.capacity());
                assertEquals(last * 2 + 2, sb.capacity(), "each growth must follow 2n+2");
                last = sb.capacity();
            }
        }
        System.out.println("capacity changes observed while appending 70 chars: " + growths);
        assertEquals(Arrays.asList("16 -> 34", "34 -> 70"), growths);
    }

    @Test
    @DisplayName("constructors: default = 16, (int) = exact, (String) = length + 16")
    void constructorCapacities() {
        StringBuilder empty = new StringBuilder();
        StringBuilder sized = new StringBuilder(1000);
        StringBuilder seeded = new StringBuilder("SecureBank");

        System.out.println("new StringBuilder()             capacity = " + empty.capacity());
        System.out.println("new StringBuilder(1000)         capacity = " + sized.capacity()
                + ", length = " + sized.length());
        System.out.println("new StringBuilder(\"SecureBank\") capacity = " + seeded.capacity()
                + ", length = " + seeded.length());

        assertEquals(16, empty.capacity());
        assertEquals(1000, sized.capacity());
        assertEquals(0, sized.length(), "capacity is room, not content");
        assertEquals(26, seeded.capacity(), "10 seeded chars + 16 headroom");
        assertEquals(10, seeded.length());
    }

    @Test
    @DisplayName("ensureCapacity grows ONCE, ahead of need — later appends never re-grow")
    void ensureCapacityGrowsAheadOfNeed() {
        StringBuilder sb = new StringBuilder();
        sb.ensureCapacity(5000);
        System.out.println("after ensureCapacity(5000): capacity = " + sb.capacity());
        assertTrue(sb.capacity() >= 5000, "contract: AT LEAST the requested capacity");

        int before = sb.capacity();
        for (int i = 0; i < 4999; i++) {
            sb.append('x');
        }
        System.out.println("after 4999 appends        : capacity = " + sb.capacity() + " (unchanged)");
        assertEquals(before, sb.capacity(), "no grow-and-copy should have been needed");
    }

    @Test
    @DisplayName("pre-sizing eliminates every grow-and-copy")
    void presizingEliminatesGrowths() {
        int parts = 100_000;

        StringBuilder defaulted = new StringBuilder();
        int defaultGrowths = countGrowths(defaulted, parts);

        StringBuilder presized = new StringBuilder(parts);
        int presizedGrowths = countGrowths(presized, parts);

        System.out.println(parts + " appends into new StringBuilder()       : grew " + defaultGrowths + " times");
        System.out.println(parts + " appends into new StringBuilder(parts) : grew " + presizedGrowths + " times");

        assertTrue(defaultGrowths > 0, "a default builder MUST grow to hold 100k chars");
        assertEquals(0, presizedGrowths, "a correctly pre-sized builder never grows");
        assertEquals(parts, presized.length());
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
}
