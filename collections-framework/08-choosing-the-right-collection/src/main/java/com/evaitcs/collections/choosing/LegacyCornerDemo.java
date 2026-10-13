package com.evaitcs.collections.choosing;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.Vector;

/**
 * Demo 3 — THE LEGACY CORNER: Vector, Hashtable, Stack, Enumeration. Why they exist,
 * why they survive, and why you should not START with them.
 *
 * These four predate (or straddle) the Collections Framework of Java 1.2 — 1996-era
 * tools that millions of lines of existing code and decades of published APIs still
 * speak. They are not deprecated; they are maintained, documented, and WRONG for new
 * code, each for a provable reason:
 *
 *   Vector     — every operation pays a monitor lock, single-threaded or not, and you
 *                cannot opt out. Modern answer: ArrayList (single thread) or the
 *                java.util.concurrent lists (real sharing) — modules 02, 09, 10.
 *   Hashtable  — same lock tax, PLUS it bans null keys and null values outright (NPE),
 *                while HashMap allows both. Modern answer: HashMap / ConcurrentHashMap.
 *   Stack      — extends Vector, so it inherits the lock AND exposes get(i)/add(i,e),
 *                which lets callers break LIFO legally. Module 06 buried it: ArrayDeque.
 *   Enumeration— Iterator's ancestor: forward-only, and NO remove(). Module 03's whole
 *                "only iterator.remove() is safe during iteration" lesson starts here.
 *
 * The proofs below use only PUBLIC reflection (getMethod + Modifier.isSynchronized reads
 * a method's own flags — never setAccessible, never into java.util internals), plus
 * caught exceptions. One honest wrinkle, printed as evidence: a few Vector methods like
 * add(int,E) are not THEMSELVES marked synchronized — they delegate to insertElementAt,
 * which IS. The lock is unavoidable either way.
 */
public class LegacyCornerDemo {

    public static void main(String[] args) {
        banner("1. Vector — the lock tax, proven by reflection on PUBLIC method flags");

        try {
            Method vectorGet = Vector.class.getMethod("get", int.class);
            Method vectorAdd = Vector.class.getMethod("add", Object.class);
            Method vectorInsert = Vector.class.getMethod("insertElementAt", Object.class, int.class);
            Method listGet = ArrayList.class.getMethod("get", int.class);
            Method listAdd = ArrayList.class.getMethod("add", Object.class);
            System.out.println("Vector.get(int)        synchronized? " + Modifier.isSynchronized(vectorGet.getModifiers()));
            System.out.println("Vector.add(E)          synchronized? " + Modifier.isSynchronized(vectorAdd.getModifiers()));
            System.out.println("Vector.insertElementAt synchronized? " + Modifier.isSynchronized(vectorInsert.getModifiers())
                    + "   <- add(int,E) is NOT flagged, but delegates HERE — the lock is unavoidable");
            System.out.println("ArrayList.get(int)     synchronized? " + Modifier.isSynchronized(listGet.getModifiers()));
            System.out.println("ArrayList.add(E)       synchronized? " + Modifier.isSynchronized(listAdd.getModifiers()));
        } catch (NoSuchMethodException e) {
            System.out.println("reflection failed: " + e);
        }
        System.out.println("Same Big-O row on the cheat sheet, same single-thread behavior — one pays a lock per call.");
        System.out.println("And the lock does NOT make compound actions safe: 'if (!isEmpty()) remove(0)' still races");
        System.out.println("between the two calls. That is module 09's whole argument; module 10 has the real fixes.");

        banner("2. Hashtable — the lock tax again, PLUS the null ban");

        try {
            Method tablePut = Hashtable.class.getMethod("put", Object.class, Object.class);
            System.out.println("Hashtable.put synchronized? " + Modifier.isSynchronized(tablePut.getModifiers()));
        } catch (NoSuchMethodException e) {
            System.out.println("reflection failed: " + e);
        }
        Map<String, String> table = new Hashtable<String, String>();
        try {
            table.put(null, "value");
        } catch (NullPointerException e) {
            System.out.println("CAUGHT -> " + e.getClass().getSimpleName() + " from Hashtable.put(null, value)");
        }
        try {
            table.put("key", null);
        } catch (NullPointerException e) {
            System.out.println("CAUGHT -> " + e.getClass().getSimpleName() + " from Hashtable.put(key, null)");
        }
        Map<String, String> map = new HashMap<String, String>();
        map.put(null, "null key is fine here");
        map.put("key", null);
        System.out.println("HashMap.put(null, v) and put(k, null): both accepted -> " + map);
        System.out.println("(Module 05's fine print: HashMap's get(k)=null is then ambiguous — 'absent' or 'stored");
        System.out.println("null'? — containsKey settles it. Hashtable avoids the ambiguity by banning nulls;");
        System.out.println("ConcurrentHashMap, module 10, bans them for a better reason: atomic compute-if-absent.)");

        banner("3. Stack — module 06's verdict, restated in one line");

        System.out.println("new java.util.Stack() instanceof Vector ? "
                + (new Stack<String>() instanceof Vector)
                + "  — inherits the lock AND indexed side doors that break LIFO.");
        System.out.println("Use ArrayDeque.push/pop instead (module 06, StackVsArrayDequeDemo).");

        banner("4. Enumeration — Iterator before Iterator (module 03's ancestor)");

        Vector<String> legacy = new Vector<String>();
        legacy.add("T-1001");
        legacy.add("T-1002");
        Enumeration<String> en = legacy.elements();
        StringBuilder seen = new StringBuilder();
        while (en.hasMoreElements()) {
            seen.append(en.nextElement()).append(" ");
        }
        System.out.println("hasMoreElements()/nextElement() walk: " + seen.toString().trim());
        System.out.println("...and there is NO remove(). The 'safe remove during iteration' that module 03");
        System.out.println("builds its fail-fast machinery around literally did not exist yet. Iterator added it.");

        banner("5. Raw types — the pre-generics world, and its runtime bill (back-ref module 01)");

        rawTypesProof();

        banner("6. Why they survive — and the swap table");

        System.out.println("Decades of compiled APIs still RETURN Vector/Hashtable/Enumeration; the JDK keeps");
        System.out.println("them working forever. You will READ legacy code all your career — just don't START there:");
        System.out.println("  Vector      -> ArrayList (single thread) | CopyOnWriteArrayList (shared, module 10)");
        System.out.println("  Hashtable   -> HashMap (single thread)   | ConcurrentHashMap (shared, module 10)");
        System.out.println("  Stack       -> ArrayDeque (module 06)");
        System.out.println("  Enumeration -> Iterator / for-each (module 03)");
    }

    /** Raw List = no compile-time element type: anything goes in, ClassCastException comes out. */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    private static void rawTypesProof() {
        List raw = new ArrayList();                 // Java 1.4 style: no type argument at all
        raw.add("Dana");
        raw.add(Integer.valueOf(42));               // compiles — nothing says "String only"
        System.out.println("raw List accepted a String AND an Integer: " + raw);
        try {
            String owner = (String) raw.get(1);     // the bill arrives at the cast, at RUNTIME
            System.out.println(owner);
        } catch (ClassCastException e) {
            System.out.println("CAUGHT -> " + e.getClass().getSimpleName() + " pulling the Integer out as a String");
        }
        System.out.println("Same lesson shape as module 01's covariant arrays: the check moves from compile time");
        System.out.println("to runtime. Generics are the seatbelt; raw types unbuckle it. The compiler's");
        System.out.println("'unchecked' warnings are it begging you to put the type argument back.");
    }

    static void banner(String title) {
        System.out.println();
        System.out.println("=== " + title + " ===");
    }
}
