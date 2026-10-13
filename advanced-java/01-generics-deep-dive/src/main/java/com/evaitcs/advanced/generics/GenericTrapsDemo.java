package com.evaitcs.advanced.generics;

import java.util.ArrayList;
import java.util.List;

/**
 * Demo 4 — THE FIVE FORBIDDEN MOVES, and the workarounds.
 * (README section "The traps".)
 *
 * Every one of these prohibitions has the SAME root cause: erasure. If the runtime
 * cannot see T, it cannot build arrays of T, cannot instanceof against T, and cannot
 * let a static context refer to a per-instance type parameter. The compiler forbids
 * exactly what would silently misbehave at runtime.
 *
 * The fifth trap (varargs heap pollution) is the one the compiler CAN'T fully prevent,
 * so it warns — and @SafeVarargs is the author's signed promise that the method is clean.
 */
public class GenericTrapsDemo {

    public static void main(String[] args) {
        banner("Trap 1 — no arrays of parameterized types: new List<String>[10] is a COMPILE ERROR");

        // WHY: arrays are REIFIED — they check their component type on every store at
        // runtime (ArrayStoreException). Erased List<String> cannot be checked, so a
        // List<String>[] would accept a List<Integer> store with no alarm. Java refuses
        // to create the unsound object at all.
        //   List<String>[] table = new List<String>[10];          // COMPILE ERROR
        Object[] soundEscape = new List<?>[10];                    // wildcard arrays ARE allowed
        soundEscape[0] = new ArrayList<String>();
        System.out.println("workaround: List<?>[] compiles (wildcards are checkable), size = " + soundEscape.length);
        System.out.println("or just use an ArrayList<List<String>> — nesting beats arrays (collections-framework 02).");

        banner("Trap 2 — no instanceof against a parameterized type");

        Object mystery = new ArrayList<String>();
        System.out.println("mystery instanceof List      = " + (mystery instanceof List));
        //   mystery instanceof List<String>                        // COMPILE ERROR
        // WHY: after erasure the object carries no "String-ness" to test. The check
        // would compile to the same bytecode as instanceof List — a lie in source form.
        System.out.println("WHY forbidden: the erased object cannot answer 'String-ness' — the test would be fake.");
        System.out.println("workaround: instanceof List<?> (honest) then verify elements as you use them.");

        banner("Trap 3 — no static members of a class's type parameter");

        // class Vault<T> { static T lastStored; }                  // COMPILE ERROR
        // WHY: static means ONE slot for the whole class. T means a DIFFERENT type per
        // instantiation (Vault<Transaction>, Vault<String>...). One slot cannot hold
        // "whatever T happens to be" — there is no single T at class level.
        System.out.println("WHY forbidden: one static slot cannot serve Vault<Transaction> AND Vault<String>.");
        System.out.println("workaround: a generic METHOD with its own <T> (static <T> T first(List<T> list)) —");
        System.out.println("            the method's T is chosen per CALL, which is well-defined. See below:");
        System.out.println("first(transactions) = " + first(List.of(new Transaction("T-1", "x", java.math.BigDecimal.ONE))));
        System.out.println("first(strings)      = " + first(List.of("Dana", "Ana")));

        banner("Trap 4 — no 'new T()', no 'T.class', no catch(T)");

        // WHY, one line each:
        //   new T()  — after erasure the bytecode would say 'new Object()': wrong object, silently.
        //   T.class  — there is exactly one List.class; there is no String-ness left to ask for.
        //   catch(T) — catch blocks are matched at runtime by Class; an erased T matches nothing specific.
        System.out.println("workaround for all three: pass the Class<T> token IN — the 'type token' idiom:");
        Vault<Transaction> vault = new Vault<>(Transaction.class);   // diamond OK in Java 21 course
        System.out.println("vault stores under token " + vault.token().getSimpleName()
                + " and can now reflect: " + vault.describe());

        banner("Trap 5 — varargs HEAP POLLUTION: the one trap that reaches runtime");

        try {
            dangerous(List.of("Dana"), List.of("Ana"));
        } catch (ClassCastException e) {
            System.out.println("CAUGHT -> " + e.getClass().getSimpleName() + ": " + e.getMessage());
            System.out.println("step 1: calling a List<String>... method builds a List[] array under the hood");
            System.out.println("        (trap 1, quietly legalized by the compiler for varargs).");
            System.out.println("step 2: the callee stored a List<Integer> into that Object[] — no cast, no alarm: POLLUTION.");
            System.out.println("step 3: reading it back AS List<String> compiled to (String) on the element — and fired.");
            System.out.println("verdict: the crash lands far from the crime. @SafeVarargs is the author's signed");
            System.out.println("         promise 'I never WRITE into this array' — see Arrays.asList and List.of.");
        }

        banner("6. In one sentence");
        System.out.println("Everything generics forbid, they forbid because erasure would make it a silent lie —");
        System.out.println("and every prohibition has a workaround that hands the runtime the type it lost.");
    }

    static void banner(String title) {
        System.out.println();
        System.out.println("=== " + title + " ===");
    }

    /** Generic method with its OWN <T>: well-defined per call, so legal even though static. */
    static <T> T first(List<T> list) {
        return list.get(0);
    }

    /** The type-token idiom: the caller passes the Class the erased method body cannot name. */
    static class Vault<T> {
        private final Class<T> type;

        Vault(Class<T> type) {
            this.type = type;
        }

        Class<T> token() {
            return type;
        }

        String describe() {
            return "I can reflect on " + type.getName() + " because I was GIVEN its Class object.";
        }
    }

    /**
     * The polluter — Effective Java Item 32's classic, verbatim in spirit.
     * Deliberately WITHOUT @SafeVarargs so the compiler prints its
     * "unchecked generic array creation" warning at build time: the warning IS the lesson.
     */
    static void dangerous(List<String>... stringLists) {
        List<Integer> intList = List.of(Integer.valueOf(42));
        Object[] objects = stringLists;   // the varargs array is really an Object[]
        objects[0] = intList;             // HEAP POLLUTION: stores fine — nothing is checked
        String s = stringLists[0].get(0); // read back through the generic view: (String) 42 -> boom
        System.out.println("never reached: " + s);
    }
}
