package com.evaitcs.corejava.tostring;

import java.math.BigDecimal;
import java.text.MessageFormat;

/**
 * Demo 6 — SECURITY: toString is a data-export API.
 *
 * The incident (true story, names changed): a payments team debugging an outage added
 * ONE line — log.debug("processing {}", account) — and shipped it. The account class
 * carried an all-fields toString. Within an hour the log shipper had forwarded
 * thousands of lines containing PINs and API keys to a third-party error-tracking
 * site: credentials in plaintext, outside the vault, indexed and searchable.
 *
 * Nobody "printed the pin". They printed the OBJECT. The toString wrote the payload —
 * and the call sites that trigger it (println, concat, %s, {} placeholders, dialogs,
 * assertion messages) are everywhere and invisible. That is the professional lesson:
 * you write toString ONCE, and code you never meet exports it FOREVER.
 */
public class SecurityMaskingDemo {

    public static void main(String[] args) {

        banner("1. One DEBUG line, two very different payloads");

        AccountLeaky leaky = new AccountLeaky(
                "ACC-1001", "Ada Lovelace", new BigDecimal("1250.75"), "4921", "sk-9f3ab7c2e1");
        AccountPrinted safe = new AccountPrinted(
                "ACC-1001", "Ada Lovelace", new BigDecimal("1250.75"), "4111111111111234", "4921");

        // printf's %s routes through toString exactly like println and concat do.
        System.out.printf("DEBUG processing %s%n", leaky);
        System.out.printf("DEBUG processing %s%n", safe);
        System.out.println("=> Same format string, twice. The leaky object exported pin AND secretKey;");
        System.out.println("   the safe one exported a résumé support staff can actually use.");

        banner("2. It is never just println — the callers you do not control");

        // MessageFormat's {0} stringifies the argument too.
        System.out.println(MessageFormat.format("AUDIT {0} flagged by risk engine", leaky));
        // Concat: StringBuilder.append(Object) -> String.valueOf -> toString (demos 2 and 4).
        System.out.println("TRACE object entered workflow: " + leaky);
        System.out.println("Logging frameworks ({} placeholders) call toString LAZILY — but the");
        System.out.println("moment the level is enabled, your export runs. JOptionPane dialogs,");
        System.out.println("assertion messages (assert ok : \"state \" + obj), exception constructors —");
        System.out.println("ALL of them stringify your object behind your back.");

        banner("3. The rules — say them out loud");

        System.out.println("Rule 1: toString is a data-export API. Review its field list like an");
        System.out.println("        HTTP response payload: would you send this to a stranger?");
        System.out.println("Rule 2: NEVER include passwords, pins, tokens, session ids, full card");
        System.out.println("        numbers. Mask instead: card -> ****1234; pin -> **** (no digits).");
        System.out.println("Rule 3: IDE 'generate toString' includes ALL fields by default —");
        System.out.println("        uncheck the secrets. READING the generated code is the whole job.");

        banner("4. Masking done right — different secrets, different masks");

        System.out.println("maskCard(\"4111111111111234\") -> " + AccountPrinted.maskCard("4111111111111234"));
        System.out.println("maskPin(\"4921\")              -> " + AccountPrinted.maskPin("4921"));
        System.out.println("=> The card keeps its last 4 so a human can pick the right card —");
        System.out.println("   '****1234' identifies without empowering. The pin keeps NOTHING:");
        System.out.println("   'last 4' of a 4-digit pin IS the whole secret. Masking is per-field");
        System.out.println("   judgment, not one helper applied to everything that looks sensitive.");
    }

    static void banner(String title) {
        System.out.println();
        System.out.println("=== " + title + " ===");
    }
}
