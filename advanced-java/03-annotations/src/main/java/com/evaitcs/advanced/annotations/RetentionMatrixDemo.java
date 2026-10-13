package com.evaitcs.advanced.annotations;

import java.lang.reflect.Field;

/**
 * Demo 2 — THE RETENTION MATRIX. (README section "Retention: who can see the note".)
 *
 * Three fields, three {@code @Retention} policies, one question: can reflection see the note at
 * runtime? The answer is the single most important fact about annotations for framework work:
 *   SOURCE  -> the compiler reads it, then discards it. Not in the class file. Invisible at runtime.
 *   CLASS   -> written to the class file, but NOT loaded into memory. Bytecode tools see it; reflection does not.
 *   RUNTIME -> loaded and reflection-visible. The ONLY policy a runtime reader can use.
 *
 * This is why MiniValidator's stickers all declare RUNTIME, and why @Override (SOURCE) can never
 * be read back at runtime. Same axis, proved side by side below.
 */
public class RetentionMatrixDemo {

    @SourceNote("discarded by the compiler")
    private String sourceAnnotated;

    @ClassNote("in the class file, not loaded at runtime")
    private String classAnnotated;

    @RuntimeNote(value = "loaded", level = 7)
    private String runtimeFull;

    @RuntimeNote("only value given")   // level() omitted -> falls back to its default of 1
    private String runtimeDefaults;

    public static void main(String[] args) throws Exception {
        Field source = RetentionMatrixDemo.class.getDeclaredField("sourceAnnotated");
        Field classNote = RetentionMatrixDemo.class.getDeclaredField("classAnnotated");
        Field runtimeFull = RetentionMatrixDemo.class.getDeclaredField("runtimeFull");
        Field runtimeDefaults = RetentionMatrixDemo.class.getDeclaredField("runtimeDefaults");

        banner("1. SOURCE retention — the compiler reads it, then throws it away");

        System.out.println("step 1: the field is annotated @SourceNote in the source code.");
        System.out.println("step 2: at runtime, isAnnotationPresent(SourceNote.class) = "
                + source.isAnnotationPresent(SourceNote.class));
        System.out.println("verdict: false. It never reached the class file. @Override works this way.");

        banner("2. CLASS retention — in the class file, but not loaded at runtime");

        System.out.println("step 1: the field is annotated @ClassNote.");
        System.out.println("step 2: at runtime, isAnnotationPresent(ClassNote.class) = "
                + classNote.isAnnotationPresent(ClassNote.class));
        System.out.println("step 3: a bytecode tool (build plugin, instrumenter) COULD read it from the .class file.");
        System.out.println("verdict: false for reflection. CLASS is the DEFAULT retention when you omit @Retention.");

        banner("3. RUNTIME retention — loaded, reflection-visible, exact values");

        RuntimeNote full = runtimeFull.getAnnotation(RuntimeNote.class);
        System.out.println("step 1: getAnnotation(RuntimeNote.class) is non-null? " + (full != null));
        System.out.println("step 2: value() = \"" + full.value() + "\", level() = " + full.level() + "  (both given)");

        RuntimeNote defaults = runtimeDefaults.getAnnotation(RuntimeNote.class);
        System.out.println("step 3: the second field gave only value() -> value() = \"" + defaults.value() + "\"");
        System.out.println("step 4: its level() was omitted -> defaults to " + defaults.level() + "  (the element default)");
        System.out.println("verdict: ONLY RUNTIME survives to reflection — which is why every framework annotation declares it.");

        banner("4. In one sentence");
        System.out.println("Retention decides the reader: SOURCE for the compiler, CLASS for bytecode tools,");
        System.out.println("RUNTIME for reflection — and a runtime framework can only ever see RUNTIME.");
    }

    static void banner(String title) {
        System.out.println();
        System.out.println("=== " + title + " ===");
    }
}
