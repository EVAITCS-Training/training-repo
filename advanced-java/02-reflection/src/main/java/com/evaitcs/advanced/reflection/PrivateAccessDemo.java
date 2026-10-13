package com.evaitcs.advanced.reflection;

import java.lang.reflect.Field;
import java.lang.reflect.InaccessibleObjectException;
import java.math.BigDecimal;
import java.util.ArrayList;

/**
 * Demo 3 — PRIVATE ACCESS, and the wall it hits. (README section "setAccessible and the
 * module gate" + its walkthrough.)
 *
 * Part A: setAccessible(true) switches OFF the access check for a member, so reflection
 * can read and write a PRIVATE field. On ordinary classpath code (our Account, in the
 * "unnamed module") this works — it is how Jackson fills private fields and how Hibernate
 * reads private ids.
 *
 * Part B: the SAME call on a java.base internal (ArrayList's private "size") is REFUSED
 * with InaccessibleObjectException. Since JDK 9 the platform is split into modules, and
 * java.base does not OPEN java.util to your code. That refusal is the exact reason the
 * --add-opens flag exists. We never use --add-opens here; we catch the exception and teach it.
 */
public class PrivateAccessDemo {

    public static void main(String[] args) throws Exception {
        banner("1. setAccessible(true) on OUR private field — it works (classpath code)");

        Account account = new Account("A-1", "Dana", new BigDecimal("250.00"));
        Field owner = Account.class.getDeclaredField("owner");   // private String owner

        System.out.println("step 1: 'owner' is private; account.getOwner() is the only legal door -> " + account.getOwner());
        owner.setAccessible(true);                               // turn OFF the access check
        System.out.println("step 2: after setAccessible(true), field.get() reads it directly -> " + owner.get(account));
        owner.set(account, "Ana");                               // write straight through the private field
        System.out.println("step 3: field.set() overwrote it -> getOwner() now returns " + account.getOwner());
        System.out.println("verdict: on classpath code, setAccessible opens any private member. Powerful, and dangerous.");

        banner("2. The SAME call on a java.base internal — REFUSED");

        ArrayList<String> list = new ArrayList<>();
        list.add("Dana");
        Field size = ArrayList.class.getDeclaredField("size");   // getting the Field object is allowed...
        System.out.println("step 1: got the Field object for ArrayList's private 'size' (lookup is not blocked)");
        try {
            size.setAccessible(true);                            // ...but OPENING it is blocked
            System.out.println("step 2: setAccessible succeeded (only if you passed --add-opens java.base/java.util=ALL-UNNAMED)");
        } catch (InaccessibleObjectException e) {
            System.out.println("step 2: CAUGHT -> " + e.getClass().getSimpleName() + ": " + e.getMessage());
            System.out.println("why: java.base does not OPEN java.util to unnamed-module code. Strong encapsulation.");
        }
        System.out.println("verdict: reflection is not a master key. The module system can say no — and does.");

        banner("3. The JDK 9 module system, in one paragraph");

        System.out.println("Before JDK 9 every class was on one flat classpath and setAccessible could open");
        System.out.println("anything. JDK 9 grouped the platform into MODULES; each declares which packages it");
        System.out.println("EXPORTS (compile against) and OPENS (deep-reflect into). java.base exports java.util");
        System.out.println("(so you can use ArrayList) but does NOT open it (so you cannot touch its internals).");
        System.out.println("--add-opens java.base/java.util=ALL-UNNAMED is the escape hatch that re-opens it at");
        System.out.println("startup. This course never needs it: we catch the refusal and explain it instead.");

        banner("4. In one sentence");
        System.out.println("setAccessible(true) opens private members of ordinary classpath code, but the");
        System.out.println("module system can refuse it for platform internals — throwing InaccessibleObjectException.");
    }

    static void banner(String title) {
        System.out.println();
        System.out.println("=== " + title + " ===");
    }
}
