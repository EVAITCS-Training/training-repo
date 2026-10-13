package com.evaitcs.corejava.tostring;

import java.math.BigDecimal;

/**
 * Demo 2 — what println REALLY does, and the overload traps around null.
 *
 * The chain, end to end:
 *
 *     System.out.println(obj)  ->  String.valueOf(obj)  ->  (obj == null) ? "null" : obj.toString()
 *
 * String concatenation takes the SAME road: "a" + obj compiles to
 * new StringBuilder().append("a").append(obj) (module 04), and append(Object)
 * calls String.valueOf(obj) internally. One null check powers all of it — which is
 * why printing a null reference prints "null" instead of throwing.
 *
 * Then the trap: valueOf and println are both OVERLOADED, and overload resolution
 * happens at COMPILE time on the STATIC type. valueOf((char[]) null) selects
 * valueOf(char[]) — which does new String(chars), dereferencing the array — and NPEs
 * before any null check ever existed. One cast, completely different behavior.
 */
public class PrintlnInternalsDemo {

    public static void main(String[] args) {

        banner("1. println(obj) is String.valueOf(obj) in disguise");

        AccountRaw raw = new AccountRaw("ACC-1001", "Ada Lovelace", new BigDecimal("1250.75"), "4921");
        System.out.println("println(raw)         : " + raw);
        System.out.println("String.valueOf(raw)  : " + String.valueOf(raw));
        System.out.println("=> Identical output: println(Object x) internally does print(String.valueOf(x)),");
        System.out.println("   and valueOf(Object x) is literally  (x == null) ? \"null\" : x.toString().");

        banner("2. The null branch — safe ON PURPOSE");

        Object nothing = null;
        System.out.print("println((Object) null)   : ");
        System.out.println(nothing);                       // prints: null — no NPE
        System.out.println("\"balance: \" + nothing   : balance-check below");
        System.out.println("balance: " + nothing);         // prints: balance: null — concat = same valueOf
        System.out.println("String.valueOf(nothing)  : " + String.valueOf(nothing));
        System.out.println("=> The (x == null) check inside valueOf is WHY a null reference prints");
        System.out.println("   as the four letters n-u-l-l. The NPE you feared does not exist HERE.");

        banner("3. The trap — valueOf is OVERLOADED, and (char[]) null takes another path");

        try {
            String boom = String.valueOf((char[]) null);   // selects valueOf(char[]) at COMPILE time
            System.out.println("never reached: " + boom);
        } catch (NullPointerException e) {
            System.out.println("String.valueOf((char[]) null) threw " + e.getClass().getSimpleName() + " — caught.");
        }
        System.out.println("Why: valueOf(char[]) does  new String(chars)  — it DEREFERENCES the array.");
        System.out.println("There is no null check on that path at all. The compiler picked the overload");
        System.out.println("from the STATIC type (char[]), so the Object version's safety never applied.");
        System.out.println("One letter apart: String.valueOf((Object) null) = " + String.valueOf((Object) null));

        banner("4. Bonus trap — println(char[]) prints the CONTENTS");

        char[] pinChars = { '4', '9', '2', '1' };
        System.out.print("println(pinChars)          : ");
        System.out.println(pinChars);                      // prints: 4921 — contents, no toString involved
        System.out.print("println((Object) pinChars) : ");
        System.out.println((Object) pinChars);             // prints: [C@hex — NOW the Object overload runs
        System.out.println("=> Same variable, two overloads, two universes. The char[] version of");
        System.out.println("   println/String.valueOf prints raw contents — another reason secrets");
        System.out.println("   must never sit within reach of a debug print (demo 6).");
    }

    static void banner(String title) {
        System.out.println();
        System.out.println("=== " + title + " ===");
    }
}
