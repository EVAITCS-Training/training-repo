package com.evaitcs.collections.choosing;

/**
 * Launcher for the whole module — runs every demo main() in teaching order.
 *
 *   .\mvnw.cmd -pl 08-choosing-the-right-collection compile exec:java
 *
 * The order is the argument: first the Big-O cheat sheet (the table everything else
 * consults), then the decision flowchart walked through five real SecureBank scenarios,
 * then the legacy corner (why Vector/Hashtable/Stack/Enumeration survive and why not to
 * start with them), then the boxing traps that collections force on primitives, then
 * encapsulation (three getters, three contracts), and finally the stopwatch on the
 * table's biggest claim — O(n) contains vs O(1) contains, measured.
 *
 * To run a single demo while pointing at it:
 *   .\mvnw.cmd -pl 08-choosing-the-right-collection compile exec:java "-Dexec.mainClass=com.evaitcs.collections.choosing.BigOCheatSheetDemo"
 */
public class ChoosingDemos {

    public static void main(String[] args) {
        section("BigOCheatSheetDemo — nine rows, five columns: the table to photograph (plus capacity hints)");
        BigOCheatSheetDemo.main(args);

        section("DecisionFlowchartDemo — five SecureBank scenarios walked through the flowchart, pick + reason each");
        DecisionFlowchartDemo.main(args);

        section("LegacyCornerDemo — Vector/Hashtable/Stack/Enumeration: the lock tax proven, the swap table earned");
        LegacyCornerDemo.main(args);

        section("BoxingAndIdentityDemo — the Integer cache (127 ==, 128 !=), remove(int) vs remove(Integer), unboxing NPE");
        BoxingAndIdentityDemo.main(args);

        section("EncapsulationDemo — one internal list, three getters: leak vs snapshot vs window");
        EncapsulationDemo.main(args);

        section("EmpiricalContainsDemo — contains() on 100k elements timed: O(n) vs O(1), printed with caveats");
        EmpiricalContainsDemo.main(args);
    }

    private static void section(String title) {
        System.out.println();
        System.out.println("############################################################");
        System.out.println("# " + title);
        System.out.println("############################################################");
    }
}
