package com.evaitcs.corejava.tostring;

import java.math.BigDecimal;

/**
 * Demo 1 — the DEFAULT: what Object.toString() actually returns, decoded live.
 *
 * AccountRaw overrides nothing, so it inherits Object.toString(), which is literally:
 *
 *     getClass().getName() + "@" + Integer.toHexString(hashCode())
 *
 * And because AccountRaw also inherits Object.hashCode() — which is IDENTITY-based
 * (module 08) — the hex after the '@' IS Integer.toHexString(System.identityHashCode(raw)).
 * We prove that below by cutting the printed string in half and recomputing the hex.
 *
 * The point to land: the default is not random noise, it is a (unique-ish) FINGERPRINT.
 * Useless to a human reading a log line; genuinely useful to answer "is this the SAME
 * object?" across two log entries or two debugger watches.
 */
public class DefaultToStringDemo {

    public static void main(String[] args) {

        banner("1. A raw account, straight out of the JVM");

        AccountRaw raw = new AccountRaw("ACC-1001", "Ada Lovelace", new BigDecimal("1250.75"), "4921");
        System.out.println("System.out.println(raw) prints:");
        System.out.println("  " + raw);
        System.out.println("Owner? Balance? Pin? Not ONE field survives. The résumé is blank.");

        banner("2. Decode it live — the format is className@hexOfHashCode");

        String printed = raw.toString();
        int at = printed.indexOf('@');
        String className = printed.substring(0, at);
        String hex = printed.substring(at + 1);
        String recomputed = Integer.toHexString(System.identityHashCode(raw));

        System.out.println("printed string                     : " + printed);
        System.out.println("before the '@'                     : " + className + "   <- getClass().getName()");
        System.out.println("after the '@'                      : " + hex);
        System.out.println("toHexString(identityHashCode(raw)) : " + recomputed);
        System.out.println("same?                              : " + hex.equals(recomputed)
                + "   <- module 08: the DEFAULT hashCode IS the identity hash");

        banner("3. Two accounts, identical content — different fingerprints");

        AccountRaw twin = new AccountRaw("ACC-1001", "Ada Lovelace", new BigDecimal("1250.75"), "4921");
        System.out.println("raw  -> " + raw);
        System.out.println("twin -> " + twin);
        System.out.println("=> Equal content, different hex: the default describes the OBJECT,");
        System.out.println("   never the DATA. For a human debugging a balance problem: useless.");

        banner("4. So what IS the default good for?");

        System.out.println("A unique-ish debug fingerprint. If the SAME hex shows up in two log");
        System.out.println("lines, it is (almost certainly) the SAME object — handy for tracing");
        System.out.println("one instance through a request. It is also the fallback that makes");
        System.out.println("println(obj) TOTAL: every object can be stringified, so no print");
        System.out.println("statement ever fails. Next demo: what println really does.");
    }

    static void banner(String title) {
        System.out.println();
        System.out.println("=== " + title + " ===");
    }
}
