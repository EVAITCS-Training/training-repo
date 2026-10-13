package com.evaitcs.advanced.annotations;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Demo 1 — THE BUILT-IN ANNOTATIONS you already use. (README section "The built-ins".)
 *
 * The claim this demo keeps returning to: an annotation is a STICKER. It does nothing until a
 * READER acts on it. For most built-ins the reader is the COMPILER (so they are SOURCE-retained
 * and invisible at runtime). For @Deprecated the reader can also be reflection (RUNTIME-retained).
 * We prove both facts here, then tie @SafeVarargs back to module 01's heap-pollution trap.
 */
public class BuiltInsDemo {

    /** @Override reader = the compiler. It re-checks that you REALLY override a supertype method. */
    static class Customer {
        private String name = "Dana";

        String getName() {
            return name;
        }

        void setName(String name) {
            this.name = name;
        }
    }

    static class VipCustomer extends Customer {
        @Override
        void setName(String name) {
            super.setName("VIP " + name);
        }
        // Now imagine the typo the annotation exists to catch. You meant to override setName but
        // typed setNmae:
        //     @Override void setNmae(String name) { ... }   // COMPILE ERROR: overrides nothing.
        // Delete @Override and the SAME typo compiles happily — as a brand-new method nothing ever
        // calls. The real setName is never overridden and the bug ships. @Override turns a silent
        // runtime surprise into a red squiggle at build time.
    }

    /** @FunctionalInterface reader = the compiler: it enforces EXACTLY ONE abstract method. */
    @FunctionalInterface
    interface MoneyFormatter {
        String format(BigDecimal amount);
    }

    /** @Deprecated reader = the compiler (warning) AND reflection (RUNTIME retention, so it is visible). */
    @Deprecated(since = "2.0", forRemoval = true)
    static String legacyAccountCode(long id) {
        return "ACC-" + id;
    }

    /** @SuppressWarnings reader = the compiler only, so it is SOURCE-retained and invisible at runtime. */
    @SuppressWarnings("unused")
    static String quietlyUnused() {
        return "the compiler dropped a warning because of the sticker above";
    }

    /**
     * @SafeVarargs reader = the compiler. It is the author's signed promise "I never WRITE into the
     * varargs array," which is exactly module 01 trap 5 (heap pollution). We only read, so it is honest.
     */
    @SafeVarargs
    static <T> T firstOf(List<T>... lists) {
        return lists[0].get(0);
    }

    @SuppressWarnings({"deprecation", "unused"})
    public static void main(String[] args) throws Exception {
        banner("1. @Override — the compiler catches the setNmae typo before it ships");

        VipCustomer vip = new VipCustomer();
        vip.setName("Dana");
        System.out.println("step 1: vip.setName(\"Dana\") then vip.getName() = " + vip.getName());
        System.out.println("step 2: had we typo'd the override as 'setNmae' WITH @Override -> COMPILE ERROR.");
        System.out.println("step 3: without @Override the typo compiles as a dead new method -> silent bug.");
        System.out.println("verdict: @Override's reader is the compiler; it proves the override is real.");

        banner("2. @Deprecated(since, forRemoval) — a warning AND reflection-readable metadata");

        System.out.println("step 1: legacyAccountCode(42) still runs -> " + legacyAccountCode(42));
        Method legacy = BuiltInsDemo.class.getDeclaredMethod("legacyAccountCode", long.class);
        Deprecated deprecated = legacy.getAnnotation(Deprecated.class);   // RUNTIME retention: visible
        System.out.println("step 2: reflection reads @Deprecated -> since=\"" + deprecated.since()
                + "\", forRemoval=" + deprecated.forRemoval());
        System.out.println("verdict: forRemoval=true means 'will be deleted' — a stronger signal than plain @Deprecated.");

        banner("3. @FunctionalInterface — the compiler enforces ONE abstract method");

        MoneyFormatter formatter = amount -> "$" + amount.setScale(2, RoundingMode.HALF_UP).toPlainString();
        System.out.println("step 1: the interface is a lambda target -> " + formatter.format(new BigDecimal("1234.5")));
        System.out.println("step 2: add a SECOND abstract method and @FunctionalInterface stops it compiling.");
        System.out.println("verdict: the sticker documents intent AND makes the compiler defend the lambda contract.");

        banner("4. @SafeVarargs — module 01 trap 5, promised safe");

        String first = firstOf(List.of("Dana"), List.of("Ana"));
        System.out.println("step 1: firstOf(List.of(\"Dana\"), List.of(\"Ana\")) = " + first);
        System.out.println("step 2: generic varargs build a List[] behind your back (module 01, trap 5).");
        System.out.println("step 3: @SafeVarargs is the author's promise 'I never WRITE into that array' — we only read.");
        System.out.println("verdict: the promise silences the heap-pollution warning at every call site.");

        banner("5. @SuppressWarnings is invisible at runtime (SOURCE retention)");

        Method quiet = BuiltInsDemo.class.getDeclaredMethod("quietlyUnused");
        System.out.println("step 1: quietlyUnused() carries @SuppressWarnings in the SOURCE.");
        System.out.println("step 2: at runtime, isAnnotationPresent(SuppressWarnings.class) = "
                + quiet.isAnnotationPresent(SuppressWarnings.class));
        System.out.println("verdict: the compiler consumed it and discarded it — reflection sees nothing.");

        banner("6. In one sentence");
        System.out.println("An annotation never acts by itself: @Override/@SuppressWarnings/@SafeVarargs are read");
        System.out.println("by the COMPILER (then discarded), while @Deprecated survives to be read by reflection.");
    }

    static void banner(String title) {
        System.out.println();
        System.out.println("=== " + title + " ===");
    }
}
