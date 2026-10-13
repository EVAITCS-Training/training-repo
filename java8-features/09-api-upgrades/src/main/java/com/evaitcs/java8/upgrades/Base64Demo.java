package com.evaitcs.java8.upgrades;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * java.util.Base64 - new in Java 8, and the last piece of the daily-driver tour.
 *
 * WHY it matters: before Java 8 you either pulled in Apache Commons Codec or used
 * javax.xml.bind.DatatypeConverter - an internal-ish JDK class that was DEPRECATED in 9
 * and REMOVED in Java 11, breaking countless builds. Java 8 gave us an official home.
 */
public class Base64Demo {

    public static void main(String[] args) {
        String apiKey = "securebank-api-key-0123456789";

        System.out.println("=== Encode: bytes -> Base64 text ===");
        // Always be explicit about the charset - the platform default differs per machine.
        String encoded = Base64.getEncoder().encodeToString(apiKey.getBytes(StandardCharsets.UTF_8));
        System.out.println("   plain   : " + apiKey);
        System.out.println("   encoded : " + encoded);

        System.out.println("=== Decode: Base64 text -> bytes ===");
        byte[] decodedBytes = Base64.getDecoder().decode(encoded);
        String decoded = new String(decodedBytes, StandardCharsets.UTF_8);
        System.out.println("   decoded : " + decoded);
        System.out.println("   round-trip identical: " + apiKey.equals(decoded));

        System.out.println("=== Three flavors of encoder ===");
        // getEncoder()     - standard alphabet (+ and /), for config files and APIs
        // getUrlEncoder()  - URL/filename-safe alphabet (- and _), no padding surprises in links
        // getMimeEncoder() - 76-char line breaks, for e-mail-style transports (RFC 2045)
        String urlSafe = Base64.getUrlEncoder()
                .encodeToString("ids=1,2&city=Austin".getBytes(StandardCharsets.UTF_8));
        System.out.println("   url-safe: " + urlSafe);

        // TRAP - say this loudly: Base64 is ENCODING, not ENCRYPTION. Anyone can decode it
        // with one line (as we just did). It only makes binary data safe to carry inside
        // text formats. Real secrecy needs keys - never ship secrets in Base64 and call it safe.
        System.out.println("   remember: Base64 is ENCODING, not encryption - it hides nothing");
    }
}
