package com.evaitcs.java8.upgrades;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests as PROOF for ObjectsHelpersDemo: fail-fast boundaries, null-safe comparison,
 * and the classic filter(Objects::nonNull).
 */
class ObjectsHelpersTest {

    /** Same boundary class as the demo: nulls are refused at the constructor. */
    static final class Customer {
        final String id;
        final String city;

        Customer(String id, String city) {
            this.id = Objects.requireNonNull(id, "Customer.id must not be null");
            this.city = Objects.requireNonNull(city, "Customer.city must not be null");
        }
    }

    @Test
    @DisplayName("requireNonNull passes the value through - and fails FAST with the message")
    void requireNonNullFailsFast() {
        Customer ok = new Customer("C-1", "Austin");
        assertSame("C-1", ok.id, "non-null input is returned unchanged");

        NullPointerException boom = assertThrows(NullPointerException.class,
                () -> new Customer("C-2", null));
        System.out.println("rejected with: NullPointerException: " + boom.getMessage());
        assertEquals("Customer.city must not be null", boom.getMessage(),
                "the message must name the guilty field - that is the whole point");
    }

    @Test
    @DisplayName("Objects.equals never NPEs, where a.equals(b) would")
    void objectsEqualsIsNullSafe() {
        String a = null;
        String b = "Austin";

        assertThrows(NullPointerException.class, () -> a.equals(b));
        System.out.println("a.equals(b)               -> NullPointerException (as expected)");

        assertFalse(Objects.equals(a, b));
        assertTrue(Objects.equals(null, null));
        assertTrue(Objects.equals(b, "Austin"));
        System.out.println("Objects.equals(null,\"Austin\") -> false ; Objects.equals(null,null) -> true");
    }

    @Test
    @DisplayName("Objects.hash(a, b) is just Arrays.hashCode of the same values - null-safe")
    void objectsHashIsNullSafe() {
        int viaObjects = Objects.hash(null, "Austin");
        int viaArrays = Arrays.hashCode(new Object[] { null, "Austin" });
        System.out.println("Objects.hash(null,\"Austin\") = " + viaObjects);
        assertEquals(viaArrays, viaObjects);
        // TRAP worth knowing: Objects.hash(null) is 31 (varargs = Arrays.hashCode with seed 1),
        // while the SINGLE-value helper Objects.hashCode(null) is 0. Don't mix them up.
        assertEquals(0, Objects.hashCode(null), "single-value helper: null hashes to 0, no exception");
        assertEquals(31, Objects.hash((Object) null), "varargs helper: seed 1 * 31 + 0 = 31");
    }

    @Test
    @DisplayName("filter(Objects::nonNull) and removeIf(Objects::isNull) both drop nulls")
    void nonNullAndIsNullAsPredicates() {
        List<String> messy = Arrays.asList("T-101", null, "T-102", null, "T-103");

        List<String> viaStream = messy.stream()
                .filter(Objects::nonNull)          // THE classic method reference
                .collect(Collectors.toList());

        List<String> viaRemoveIf = new ArrayList<>(messy);
        viaRemoveIf.removeIf(Objects::isNull);     // same predicate, negated, no streams

        System.out.println("filter(Objects::nonNull) -> " + viaStream);
        System.out.println("removeIf(Objects::isNull)-> " + viaRemoveIf);
        assertEquals(Arrays.asList("T-101", "T-102", "T-103"), viaStream);
        assertEquals(viaStream, viaRemoveIf);
        assertNull(messy.get(1), "the source list is untouched - Arrays.asList still holds the nulls");
    }
}
