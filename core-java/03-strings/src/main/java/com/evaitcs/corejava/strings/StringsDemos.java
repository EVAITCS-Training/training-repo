package com.evaitcs.corejava.strings;

/**
 * Launcher for the whole module — runs every demo main() in teaching order.
 *
 *   .\mvnw.cmd -pl 03-strings compile exec:java
 *
 * To run a single demo while pointing at it:
 *   .\mvnw.cmd -pl 03-strings compile exec:java "-Dexec.mainClass=com.evaitcs.corejava.strings.StringPoolDemo"
 */
public class StringsDemos {

    public static void main(String[] args) {
        section("ImmutabilityDemo — no method changes a String, ever");
        ImmutabilityDemo.main(args);

        section("StringPoolDemo — literals are shared; `new` and runtime-built strings are not");
        StringPoolDemo.main(args);

        section("EqualsVsDoubleEqualsDemo — == asks 'same object', equals asks 'same characters'");
        EqualsVsDoubleEqualsDemo.main(args);

        section("ConcatCostDemo — why += in a loop throws away 50,000 Strings");
        ConcatCostDemo.main(args);

        section("StringApiTourDemo — the everyday API, grouped, traps named");
        StringApiTourDemo.main(args);

        section("PasswordCharArrayDemo — why JPasswordField wants char[]");
        PasswordCharArrayDemo.main(args);
    }

    private static void section(String title) {
        System.out.println();
        System.out.println("############################################################");
        System.out.println("# " + title);
        System.out.println("############################################################");
    }
}
