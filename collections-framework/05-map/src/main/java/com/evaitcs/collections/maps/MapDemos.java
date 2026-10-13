package com.evaitcs.collections.maps;

/**
 * Launcher for the whole module — runs every demo main() in teaching order.
 *
 *   .\mvnw.cmd -pl 05-map compile exec:java
 *
 * The order is the argument: first the vocabulary and its honest return values, then the
 * machine room (the four-step lookup walk and the "Aa"/"BB" collision that proves equals
 * has the last word), then the mutable-key trap that the machine room explains, then the
 * three engines holding the same registry in three orders, then the iteration idioms and
 * their costs, and finally the sorted-map vocabulary that only a TreeMap can offer.
 *
 * To run a single demo while pointing at it:
 *   .\mvnw.cmd -pl 05-map compile exec:java "-Dexec.mainClass=com.evaitcs.collections.maps.HashMapInternalsDemo"
 */
public class MapDemos {

    public static void main(String[] args) {
        section("MapBasicsDemo — put/get/remove and the return values that tell the truth; the null ambiguity");
        MapBasicsDemo.main(args);

        section("HashMapInternalsDemo — hash -> spread -> bucket -> chain walk; \"Aa\".hashCode() == \"BB\".hashCode()");
        HashMapInternalsDemo.main(args);

        section("MutableKeyTrapDemo — mutate a key after put: the entry is IN the map and UNREACHABLE");
        MutableKeyTrapDemo.main(args);

        section("ThreeMapsSameDataDemo — HashMap / LinkedHashMap / TreeMap: no order, insertion order, sorted (+ LRU)");
        ThreeMapsSameDataDemo.main(args);

        section("MapIterationDemo — entrySet vs keySet+get: one lookup vs two; views are live windows");
        MapIterationDemo.main(args);

        section("NavigableMapDemo — firstKey/lastKey, floor/ceiling, headMap/tailMap/subMap as live views");
        NavigableMapDemo.main(args);
    }

    private static void section(String title) {
        System.out.println();
        System.out.println("############################################################");
        System.out.println("# " + title);
        System.out.println("############################################################");
    }
}
