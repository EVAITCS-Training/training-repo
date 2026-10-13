package com.evaitcs.java8.upgrades;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests as PROOF for the quick hits: Arrays.setAll fills from a function of the index,
 * Comparator composition orders correctly inside List.sort, and Base64 round-trips.
 */
class QuickHitsBase64Test {

    @Test
    @DisplayName("Arrays.setAll fills every slot from the index function")
    void setAllFillsByIndex() {
        long[] tiers = new long[5];
        Arrays.setAll(tiers, i -> 100L * i);
        System.out.println("setAll result: " + Arrays.toString(tiers));
        assertArrayEquals(new long[] { 0, 100, 200, 300, 400 }, tiers);

        // parallelSetAll must produce the IDENTICAL array (each slot depends only on its index):
        long[] parallel = new long[5];
        Arrays.parallelSetAll(parallel, i -> 100L * i);
        assertArrayEquals(tiers, parallel);
        System.out.println("parallelSetAll agreed: " + Arrays.toString(parallel));
    }

    @Test
    @DisplayName("comparing().thenComparing() orders by city, then by amount within city")
    void thenComparingInsideListSort() {
        List<Transaction> statement = new ArrayList<>(Transaction.sampleStatement());
        statement.sort(Comparator.comparing(Transaction::getCity)
                                 .thenComparing(Transaction::getAmount));
        List<String> ids = new ArrayList<>();
        for (Transaction t : statement) {
            ids.add(t.getId());
        }
        System.out.println("sorted order: " + ids);
        // Austin by amount (45.99, 1200.00, 2500.00), then Dallas (3.11, 86.40), then Houston:
        assertEquals(Arrays.asList("T-106", "T-102", "T-101", "T-104", "T-103", "T-105"), ids);
    }

    @Test
    @DisplayName("nullsFirst puts nulls ahead of values instead of throwing")
    void nullsFirstSortsNullsSafely() {
        List<String> cities = new ArrayList<>(Arrays.asList("Dallas", null, "Austin", null));
        cities.sort(Comparator.nullsFirst(Comparator.<String>naturalOrder()));
        System.out.println("nullsFirst order: " + cities);
        assertEquals(Arrays.asList(null, null, "Austin", "Dallas"), cities);
    }

    @Test
    @DisplayName("Base64 round-trips bytes exactly, and matches the RFC 4648 test vector")
    void base64RoundTrip() {
        String apiKey = "securebank-api-key-0123456789";
        String encoded = Base64.getEncoder().encodeToString(apiKey.getBytes(StandardCharsets.UTF_8));
        byte[] decoded = Base64.getDecoder().decode(encoded);

        System.out.println("encoded: " + encoded);
        System.out.println("decoded: " + new String(decoded, StandardCharsets.UTF_8));
        assertEquals(apiKey, new String(decoded, StandardCharsets.UTF_8));

        // "Man" -> "TWFu" is THE canonical example from RFC 4648 - 3 bytes become 4 chars:
        assertEquals("TWFu", Base64.getEncoder().encodeToString("Man".getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    @DisplayName("URL-safe encoder avoids + and / so output survives inside links")
    void urlSafeAlphabet() {
        // bytes that produce '+' and '/' in the STANDARD alphabet produce '-' and '_' here:
        byte[] tricky = { (byte) 0xFB, (byte) 0xFF, (byte) 0xBF };
        String standard = Base64.getEncoder().encodeToString(tricky);
        String urlSafe = Base64.getUrlEncoder().encodeToString(tricky);
        System.out.println("standard: " + standard + " ; url-safe: " + urlSafe);
        assertTrue(standard.contains("+") || standard.contains("/"), "standard alphabet uses + and /");
        assertTrue(urlSafe.contains("-") || urlSafe.contains("_"), "url alphabet uses - and _");
        assertArrayEquals(tricky, Base64.getUrlDecoder().decode(urlSafe), "url decoder reverses it");
    }
}
