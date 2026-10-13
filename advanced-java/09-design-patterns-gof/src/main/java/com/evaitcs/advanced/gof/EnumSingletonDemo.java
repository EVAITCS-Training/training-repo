package com.evaitcs.advanced.gof;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Constructor;
import java.math.BigDecimal;
import java.util.Map;

/**
 * Demo 9 — SINGLETON: exactly one instance, enforced by the JVM itself.
 * (README section "Singleton: the enum form, and why DI made the rest obsolete".)
 *
 * Problem: the fee schedule is shared reference data; two copies that drift would
 * charge different fees for the same transfer. Solution (Effective Java Item 3):
 * a one-constant ENUM — serialization and reflection cannot forge a second instance,
 * which no hand-written class singleton can promise.
 *
 * You already met the JVM's own singletons: Runtime.getRuntime() and the
 * Integer.valueOf cache (Flyweight's cousin). In stage-2 you will meet the modern
 * answer: Spring beans are singleton-SCOPED — the container makes and owns the one
 * instance, and injects it. A manual singleton inside DI code is an anti-pattern
 * (module 10, "service locator"): it hides dependencies and cannot be replaced in tests.
 */
public class EnumSingletonDemo {

    /** The enum singleton: ONE constant, real behavior, JVM-guarded uniqueness. */
    public enum FeeSchedule {

        INSTANCE;

        private final Map<String, BigDecimal> rates = Map.of(
                "WIRE", new BigDecimal("2.50"),
                "CARD", new BigDecimal("0.35"));

        public BigDecimal rateFor(String paymentType) {
            return rates.get(paymentType);
        }
    }

    public static void main(String[] args) throws Exception {
        banner("1. One instance, guaranteed by class loading");

        FeeSchedule a = FeeSchedule.INSTANCE;
        FeeSchedule b = FeeSchedule.valueOf("INSTANCE");   // a different LOOKUP path...
        System.out.println("step 1: a = FeeSchedule.INSTANCE            (direct reference)");
        System.out.println("step 2: b = FeeSchedule.valueOf(\"INSTANCE\") (by name)");
        System.out.println("step 3: a == b ? " + (a == b) + "   identityHashCode equal ? "
                + (System.identityHashCode(a) == System.identityHashCode(b)));
        System.out.println("step 4: a.rateFor(\"WIRE\") = " + a.rateFor("WIRE").toPlainString());
        System.out.println("verdict: the JVM initializes each enum constant exactly ONCE, at class");
        System.out.println("         initialization — the guarantee double-checked locking tries so hard to fake.");

        banner("2. Reflection CANNOT forge a second instance");

        try {
            // Enum constructors are secretly (String name, int ordinal). Even with
            // setAccessible(true), Constructor.newInstance special-cases enums:
            Constructor<FeeSchedule> c =
                    FeeSchedule.class.getDeclaredConstructor(String.class, int.class);
            c.setAccessible(true);
            c.newInstance("FAKE", 3);
            System.out.println("never reached");
        } catch (ReflectiveOperationException | IllegalArgumentException e) {
            System.out.println("CAUGHT -> " + e.getClass().getSimpleName() + ": " + e.getMessage());
            System.out.println("why: the JDK refuses reflective enum creation OUTRIGHT — not a convention,");
            System.out.println("     a hard check inside Constructor.newInstance. A plain-class singleton");
            System.out.println("     with a private constructor LOSES to this exact attack.");
        }

        banner("3. Serialization CANNOT forge one either — live proof");

        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(bytes)) {
            out.writeObject(FeeSchedule.INSTANCE);            // write the singleton...
        }
        FeeSchedule deserialized;
        try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
            deserialized = (FeeSchedule) in.readObject();     // ...read it back
        }
        System.out.println("step 1: serialized INSTANCE to bytes, deserialized a fresh-looking object");
        System.out.println("step 2: deserialized == FeeSchedule.INSTANCE ? " + (deserialized == FeeSchedule.INSTANCE));
        System.out.println("why: enum serialization writes ONLY the constant's NAME; readObject resolves");
        System.out.println("     it via valueOf — no fields, no second instance. A classic singleton needs");
        System.out.println("     a hand-written readResolve() to survive the same round trip.");

        banner("4. The classic forms, and why they are history");

        System.out.println("eager   : static final INSTANCE = new X()      — pays even if never used.");
        System.out.println("lazy    : double-checked locking               — WRONG without volatile");
        System.out.println("          (module 04: reordering can publish a half-built object).");
        System.out.println("enum    : one line, lazy by class-loading rules, serialization- and");
        System.out.println("          reflection-safe. This is the only hand-written form worth writing.");

        banner("5. The bigger trap: in DI code, manual singletons are an ANTI-PATTERN");

        System.out.println("FeeSchedule.INSTANCE is a GLOBAL: any class can reach it, so no constructor");
        System.out.println("reveals the dependency, and tests cannot substitute a fake schedule.");
        System.out.println("stage-2's answer: make it a bean — the container owns ONE instance");
        System.out.println("(singleton scope) and INJECTS it where needed. Same uniqueness, visible wiring.");
        System.out.println("Module 10 builds that container in ~60 lines (MiniContainerDemo).");

        banner("6. In one sentence");
        System.out.println("The enum singleton is the only one-instance trick the JVM enforces against");
        System.out.println("both reflection and serialization — but under DI, the container should own it.");
    }

    static void banner(String title) {
        System.out.println();
        System.out.println("=== " + title + " ===");
    }
}
