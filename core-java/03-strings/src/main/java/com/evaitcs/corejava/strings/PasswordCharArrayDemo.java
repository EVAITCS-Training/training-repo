package com.evaitcs.corejava.strings;

import java.util.Arrays;

/**
 * Claim #6: for passwords, String is the WRONG container — char[] is the right one.
 * This is not a style quibble; it is the reason Swing's JPasswordField hands you
 * char[] from getPassword() instead of a String.
 *
 * The whole argument is claim #1 applied to security: because a String is
 * immutable, there is NO WAY to wipe it. The characters sit in the heap until GC
 * gets around to collecting the object (which for a pooled/interned literal may
 * be never), and any heap dump taken in that window shows the password in
 * PLAIN TEXT. A char[] can be overwritten the instant you are done.
 */
public class PasswordCharArrayDemo {

    public static void main(String[] args) {

        banner("1. The problem: a String password CANNOT be wiped");

        // Demo credential only — the seeded SecureBank test users all use "password123".
        String password = "password123";

        System.out.println("login with password        : \"" + password + "\"  -> accepted");
        System.out.println("after login we want it GONE. Options?");
        System.out.println("  password = null;          -> only drops OUR reference; the characters");
        System.out.println("                               stay in the heap until GC feels like it.");
        System.out.println("  password.replace(...)?    -> returns a NEW String; the original is untouched.");
        System.out.println("Still readable right now   : \"" + password + "\"   <- and a heap dump (jmap) sees it too");

        banner("2. The fix: char[] can be zeroed IMMEDIATELY");

        char[] pin = "password123".toCharArray();

        System.out.println("before wipe, char codes    : " + codes(pin));
        boolean ok = Arrays.equals(pin, "password123".toCharArray());
        System.out.println("authenticated against copy : " + ok);

        Arrays.fill(pin, '\0');   // wipe NOW — deterministic, not "whenever GC gets to it"

        System.out.println("after  Arrays.fill(pin,'\\0'): " + codes(pin));
        System.out.println("=> Every character is 0. If a heap dump is taken from here on, there is");
        System.out.println("   nothing to find. (Raw '\\0' prints as invisible, so we show the codes.)");

        banner("3. Why JPasswordField does exactly this");

        System.out.println("JTextField.getText() returns String  -> deprecated for password fields:");
        System.out.println("                                        the password becomes an unwipeable String.");
        System.out.println("JPasswordField.getPassword() returns char[] -> the app authenticates, then");
        System.out.println("                                        Arrays.fill(...) zeroes it immediately.");
        System.out.println("Honest caveat for the class: char[] is BEST EFFORT. Anything that already");
        System.out.println("turned into a String (typed input, network buffers) is back under String");
        System.out.println("rules. char[] shrinks the exposure window; it does not eliminate it.");
    }

    /** Prints char codes because '\0' is invisible on a console — the zeros ARE the evidence. */
    private static String codes(char[] chars) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < chars.length; i++) {
            if (i > 0) {
                sb.append(' ');
            }
            sb.append((int) chars[i]);
        }
        return sb.append(']').toString();
    }

    static void banner(String title) {
        System.out.println();
        System.out.println("=== " + title + " ===");
    }
}
