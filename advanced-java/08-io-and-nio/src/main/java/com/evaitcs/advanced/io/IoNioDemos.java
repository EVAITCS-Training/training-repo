package com.evaitcs.advanced.io;

/**
 * Launcher for the whole module — runs every demo main() in teaching order.
 *
 *   .\mvnw.cmd -pl 08-io-and-nio compile exec:java
 *
 * The order is the argument: first the CLASSIC byte/char streams and the Decorator nesting that
 * makes them composable, then the charset contract that turns bytes into text (and mojibake when
 * you get it wrong), then resource safety (try-with-resources, suppressed exceptions, the
 * serialization trap). That closes the java.io world. Then we cross into NIO: the BUFFER lifecycle
 * (the centerpiece), the REACTOR that lets one thread serve many connections, and finally the
 * modern Files/Path API you will actually use day to day.
 *
 * To run a single demo while pointing at it:
 *   .\mvnw.cmd -pl 08-io-and-nio compile exec:java "-Dexec.mainClass=com.evaitcs.advanced.io.BufferWalkthroughDemo"
 */
public class IoNioDemos {

    public static void main(String[] args) throws Exception {
        section("StreamDecoratorDemo — bytes vs chars, and the Decorator pattern stacking capabilities");
        StreamDecoratorDemo.main(args);

        section("CharsetMojibakeDemo — same bytes, wrong charset = mojibake; always name the charset");
        CharsetMojibakeDemo.main(args);

        section("TryWithResourcesDemo — reverse close order, suppressed exceptions, and the serialization trap");
        TryWithResourcesDemo.main(args);

        section("BufferWalkthroughDemo — the buffer lifecycle: fill, flip, drain, clear/compact (the centerpiece)");
        BufferWalkthroughDemo.main(args);

        section("SelectorEchoDemo — the reactor: one thread, many connections, via a Selector");
        SelectorEchoDemo.main(args);

        section("FilesPathDemo — Files/Path: walk, readString/writeString, lines().filter (the day-to-day API)");
        FilesPathDemo.main(args);
    }

    private static void section(String title) {
        System.out.println();
        System.out.println("############################################################");
        System.out.println("# " + title);
        System.out.println("############################################################");
    }
}
