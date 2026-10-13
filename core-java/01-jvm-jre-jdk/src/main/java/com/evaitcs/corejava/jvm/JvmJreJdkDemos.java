package com.evaitcs.corejava.jvm;

/**
 * Launcher for the whole module — runs every demo main() in teaching order.
 *
 *   .\mvnw.cmd -pl 01-jvm-jre-jdk compile exec:java
 *
 * To run a single demo while pointing at it:
 *   .\mvnw.cmd -pl 01-jvm-jre-jdk compile exec:java "-Dexec.mainClass=com.evaitcs.corejava.jvm.MemoryAreasDemo"
 */
public class JvmJreJdkDemos {

    public static void main(String[] args) {
        section("CompileRunPipelineDemo — .java -> javac -> .class -> JVM, proven with CAFEBABE");
        CompileRunPipelineDemo.main(args);

        section("JvmKnowsItselfDemo — JDK (contains) JRE (contains) JVM: the box we run in");
        JvmKnowsItselfDemo.main(args);

        section("JitWarmupDemo — interpreted vs JIT-compiled: same loop, five times");
        JitWarmupDemo.main(args);

        section("MemoryAreasDemo — heap and stack: a finite stack and Runtime's numbers");
        MemoryAreasDemo.main(args);

        section("ClassLoadingDemo — classes load lazily, on first active use");
        ClassLoadingDemo.main(args);

        section("MainMethodDemo — why the signature is public static void main(String[])");
        MainMethodDemo.main(args);
    }

    private static void section(String title) {
        System.out.println();
        System.out.println("############################################################");
        System.out.println("# " + title);
        System.out.println("############################################################");
    }
}
