package com.evaitcs.corejava.tostring;

/**
 * Launcher for the whole module — runs every demo main() in teaching order.
 *
 *   .\mvnw.cmd -pl 09-tostring compile exec:java
 *
 * The order is the argument: decode the useless default, open the black box that
 * calls it (println -> String.valueOf -> toString, with the null/char[] traps),
 * write a good one, watch collections call it per element, follow it through
 * inheritance, and end where production ends: with the security lesson.
 *
 * To run a single demo while pointing at it:
 *   .\mvnw.cmd -pl 09-tostring compile exec:java "-Dexec.mainClass=com.evaitcs.corejava.tostring.SecurityMaskingDemo"
 */
public class ToStringDemos {

    public static void main(String[] args) {
        section("DefaultToStringDemo — Object.toString decoded: className@identityHex (module 08 callback)");
        DefaultToStringDemo.main(args);

        section("PrintlnInternalsDemo — println -> String.valueOf -> toString, and the (char[]) null trap");
        PrintlnInternalsDemo.main(args);

        section("GoodToStringDemo — building the résumé: StringBuilder, two styles, and where it pays off");
        GoodToStringDemo.main(args);

        section("CollectionsToStringDemo — a list calls YOUR toString once per element (arrays do not)");
        CollectionsToStringDemo.main(args);

        section("InheritedToStringDemo — whose name prints? hardcoded lie vs getClass() trick");
        InheritedToStringDemo.main(args);

        section("SecurityMaskingDemo — toString is a data-export API; mask, never leak");
        SecurityMaskingDemo.main(args);
    }

    private static void section(String title) {
        System.out.println();
        System.out.println("############################################################");
        System.out.println("# " + title);
        System.out.println("############################################################");
    }
}
