package com.evaitcs.advanced.generics;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/**
 * Demo 1 — ERASURE PROVEN, not asserted. (README sections "The big idea" and
 * "Walkthrough: what javac actually does".)
 *
 * The claim: after compilation, List<String> and List<Integer> are the SAME class.
 * The <...> part existed only while javac was checking your code. At runtime the JVM
 * sees one raw ArrayList and casts on every read.
 *
 * Three pieces of evidence, printed live:
 *   1. both lists report the identical Class object (==, not equals)
 *   2. the type argument survives in ONE place — the generic SIGNATURE of fields and
 *      methods (that is reflection metadata, module 02, not the objects themselves)
 *   3. the cast javac inserted is real: smuggle an Integer past a raw type and the
 *      read side throws ClassCastException — the lie gets audited at runtime
 */
public class ErasureProofDemo {

    /** The type argument IS recorded here — in the field's signature, for reflection. */
    private List<String> owners = new ArrayList<>();
    private List<Integer> codes = new ArrayList<>();

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static void main(String[] args) throws Exception {
        banner("1. Two different parameterizations, ONE runtime class");

        List<String> strings = new ArrayList<>();
        List<Integer> integers = new ArrayList<>();
        System.out.println("step 1: strings.getClass()          = " + strings.getClass().getName());
        System.out.println("step 2: integers.getClass()         = " + integers.getClass().getName());
        System.out.println("step 3: same Class object (==)      = " + (strings.getClass() == integers.getClass()));
        System.out.println("verdict: at runtime there is no List<String> — only List. The label is gone.");

        banner("2. Where the label DOES survive: generic signatures (reflection metadata)");

        // The class file keeps a *signature* attribute for fields/methods so reflection
        // can reconstruct what erasure removed from the objects. This is exactly how
        // Spring reads List<Account> out of your @Autowired field (module 02 + 03).
        Field ownersField = ErasureProofDemo.class.getDeclaredField("owners");
        Field codesField = ErasureProofDemo.class.getDeclaredField("codes");
        System.out.println("field 'owners' -> getType()        = " + ownersField.getType().getName() + "   (raw: just List)");
        System.out.println("field 'owners' -> getGenericType() = " + ownersField.getGenericType());
        System.out.println("field 'codes'  -> getGenericType() = " + codesField.getGenericType());

        Type generic = ownersField.getGenericType();
        if (generic instanceof ParameterizedType parameterized) {   // Java 21 pattern matching
            System.out.println("step 1: the signature is a ParameterizedType");
            System.out.println("step 2: its actual type argument  = " + parameterized.getActualTypeArguments()[0]);
            System.out.println("verdict: the JVM never forgot the DECLARATION — it forgot the OBJECTS.");
        }

        banner("3. The inserted cast, caught red-handed");

        // javac compiles  String s = strings.get(0)  into  String s = (String) strings.get(0).
        // Through a RAW reference the compiler cannot protect us — this is the unchecked
        // world generics were invented to replace (Java 1.4 code did this on every line).
        List raw = strings;                 // raw type: the seatbelt off (collections-framework 01)
        strings.add("Dana");
        raw.add(Integer.valueOf(42));       // compiles with a warning: raw accepts anything
        System.out.println("step 1: smuggled Integer 42 into a List<String> via a raw reference");
        try {
            for (String owner : strings) {  // javac inserted (String) on this read — here it fires
                System.out.println("read: " + owner);
            }
        } catch (ClassCastException e) {
            System.out.println("step 2: CAUGHT -> " + e.getClass().getSimpleName() + ": " + e.getMessage());
            System.out.println("verdict: erasure does not make types unsafe — it MOVES the check to the read.");
        }

        banner("4. In one sentence");
        System.out.println("Generics are compile-time-only: javac checks the labels, erases them,");
        System.out.println("and inserts casts — so the JVM stays a Java-1.4 machine that never lies.");
    }

    static void banner(String title) {
        System.out.println();
        System.out.println("=== " + title + " ===");
    }
}
