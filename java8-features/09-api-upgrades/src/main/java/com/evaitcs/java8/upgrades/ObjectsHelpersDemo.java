package com.evaitcs.java8.upgrades;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * The Objects utility class (java.util.Objects). Most of it landed in Java 7; Java 8 added
 * isNull/nonNull so null-checks can finally be METHOD REFERENCES. The daily drivers:
 *   - requireNonNull(value, "message") at every boundary (constructors, setters, service entry),
 *   - Objects.equals(a, b) / Objects.hash(a, b) for null-safe comparison,
 *   - Objects::nonNull as a Predicate - the single most-used method reference in real code.
 */
public class ObjectsHelpersDemo {

    /** A boundary class: nulls are refused at the CONSTRUCTOR, not three calls later. */
    static final class Customer {
        private final String id;
        private final String city;

        Customer(String id, String city) {
            // WHY: without this, a null city is stored happily and explodes much later in some
            // grouping code far away from the crime scene. requireNonNull fails FAST and the
            // message names the guilty field. This is the pre-Java-9 idiom for defaults; note
            // that requireNonNullElse does NOT exist yet in Java 8 (it arrived in 9).
            this.id = Objects.requireNonNull(id, "Customer.id must not be null");
            this.city = Objects.requireNonNull(city, "Customer.city must not be null");
        }

        @Override public String toString() { return "Customer[" + id + "@" + city + "]"; }
    }

    public static void main(String[] args) {
        System.out.println("=== requireNonNull - fail fast at the boundary ===");
        System.out.println("   ok    : " + new Customer("C-1", "Austin"));
        try {
            new Customer("C-2", null);
        } catch (NullPointerException e) {
            // The message is the point: it tells the caller WHICH contract they broke.
            System.out.println("   rejected -> NullPointerException: " + e.getMessage());
        }

        System.out.println("=== Objects.equals / Objects.hash - null-safe helpers ===");
        String a = null;
        String b = "Austin";
        // Java 7:  a == null ? b == null : a.equals(b)   - and everyone forgot one branch once.
        System.out.println("   Objects.equals(null, \"Austin\") = " + Objects.equals(a, b));
        System.out.println("   Objects.equals(null, null)    = " + Objects.equals(null, null));
        System.out.println("   a.equals(b) would have thrown NullPointerException; Objects.equals never does");
        System.out.println("   Objects.hash(null, \"Austin\") = " + Objects.hash(a, b) + "  (null-safe hashCode)");

        System.out.println("=== isNull / nonNull as PREDICATES - the classic ===");
        List<String> messyAuditFeed = Arrays.asList("T-101", null, "T-102", null, "T-103");
        // filter(Objects::nonNull) is arguably THE most common method reference in Java 8 code.
        // Objects.nonNull(x) is just `x != null` as a static method - which makes it referencable.
        List<String> clean = messyAuditFeed.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        System.out.println("   stream filter(Objects::nonNull) : " + clean);

        // The collection-flavored equivalent (no streams needed):
        List<String> alsoClean = new ArrayList<>(messyAuditFeed);
        alsoClean.removeIf(Objects::isNull);   // isNull as the removal predicate
        System.out.println("   removeIf(Objects::isNull)       : " + alsoClean);
        System.out.println("   both routes agree: " + clean.equals(alsoClean));
    }
}
