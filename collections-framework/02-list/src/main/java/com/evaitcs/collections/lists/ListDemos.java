package com.evaitcs.collections.lists;

/**
 * Launcher for the whole module — runs every demo main() in teaching order.
 *
 *   .\mvnw.cmd -pl 02-list compile exec:java
 *
 * The order is the argument: first the two engines and why ArrayList wins almost always (the
 * module title, measured), then the List API itself as surgery on a bank statement, then the
 * view that surprises everyone (subList — writes through, expires on parent modification),
 * then the four pitfalls every class writes on the board at least once.
 *
 * To run a single demo while pointing at it:
 *   .\mvnw.cmd -pl 02-list compile exec:java "-Dexec.mainClass=com.evaitcs.collections.lists.ArrayListVsLinkedListDemo"
 */
public class ListDemos {

    public static void main(String[] args) {
        section("ArrayListVsLinkedListDemo — growable array vs chain of nodes, measured");
        ArrayListVsLinkedListDemo.main(args);

        section("ListSurgeryDemo — add/set/get/remove/indexOf on a bank statement");
        ListSurgeryDemo.main(args);

        section("SubListViewDemo — subList is a live window, and windows expire");
        SubListViewDemo.main(args);

        section("RemovePitfallsDemo — the skip bug, int vs Integer, regrowth, the asList view");
        RemovePitfallsDemo.main(args);
    }

    private static void section(String title) {
        System.out.println();
        System.out.println("############################################################");
        System.out.println("# " + title);
        System.out.println("############################################################");
    }
}
