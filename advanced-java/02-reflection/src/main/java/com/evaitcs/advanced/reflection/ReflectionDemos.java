package com.evaitcs.advanced.reflection;

/**
 * Launcher for the whole module — runs every demo main() in teaching order.
 *
 *   .\mvnw.cmd -pl 02-reflection compile exec:java
 *
 * The order is the argument: first the Class OBJECT and the three doors to it (including
 * forName's initialize side effect), then READING a class's shape, then reaching INTO it
 * (private access and the module wall), then the payoff — a runtime-fabricated PROXY that
 * frameworks inject behavior through — and finally the fast modern call path (MethodHandles).
 *
 * To run a single demo while pointing at it:
 *   .\mvnw.cmd -pl 02-reflection compile exec:java "-Dexec.mainClass=com.evaitcs.advanced.reflection.DynamicProxyDemo"
 */
public class ReflectionDemos {

    public static void main(String[] args) throws Throwable {
        section("ClassObjectDemo — one Class object, three doors; forName also INITIALIZES");
        ClassObjectDemo.main(args);

        section("InspectDemo — listing a class's fields, methods, constructors and signatures");
        InspectDemo.main(args);

        section("PrivateAccessDemo — setAccessible opens classpath code, but the module system can refuse");
        PrivateAccessDemo.main(args);

        section("DynamicProxyDemo — an audit-logging proxy: the seam Spring @Transactional uses");
        DynamicProxyDemo.main(args);

        section("MethodHandlesDemo — resolve once, invoke often: the fast modern call path");
        MethodHandlesDemo.main(args);
    }

    private static void section(String title) {
        System.out.println();
        System.out.println("############################################################");
        System.out.println("# " + title);
        System.out.println("############################################################");
    }
}
