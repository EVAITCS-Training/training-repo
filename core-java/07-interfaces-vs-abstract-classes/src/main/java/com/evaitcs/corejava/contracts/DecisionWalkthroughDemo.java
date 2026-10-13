package com.evaitcs.corejava.contracts;

import java.lang.reflect.Modifier;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Claim #5 of the module: the choice is a DECISION, not a taste. This demo
 * prints the capability table, then walks it twice on our own types, then
 * shows the JDK doing exactly what we did — interface for the contract,
 * abstract skeleton for the plumbing (List + AbstractList).
 *
 * RULE OF THUMB, say it out loud in class:
 *   "Interfaces for CAN-DO contracts, abstract classes for IS-A families
 *    with shared plumbing."
 */
public class DecisionWalkthroughDemo {

    public static void main(String[] args) {

        banner("1. The capability table (PRE-Java-8 — the last row is what Java 8 changes)");

        System.out.printf("%-38s | %-30s | %-30s%n", "capability", "interface (pre-Java 8)", "abstract class");
        System.out.printf("%-38s-+-%-30s-+-%-30s%n", repeat('-', 38), repeat('-', 30), repeat('-', 30));
        row("instance fields (STATE)",        "NO — shared constants only", "YES — one copy per object");
        row("constructors",                   "NO",                         "YES — reached via super(...)");
        row("method bodies",                  "NO — all public abstract",   "YES — concrete + abstract");
        row("member visibility",              "public ONLY (implicit)",     "all four levels");
        row("static members",                 "static final constants only", "anything static");
        row("how many can ONE class have?",   "MANY (implements a, b, c)",  "exactly ONE (extends x)");
        row("directly instantiable?",         "NO (implementors are)",      "NO — compile error");
        row("typical role",                   "CAN-DO capability / open API", "IS-A family, shared plumbing");
        System.out.println();
        System.out.println("Java 8 changes TWO rows above: 'method bodies' (default methods) and");
        System.out.println("'static members' (static methods). The STATE and CONSTRUCTOR rows never");
        System.out.println("change — see the closing teaser demo and java8-features/02-interface-evolution.");

        banner("2. Walk-through A: why Account is an ABSTRACT CLASS");

        System.out.println("Question from the table, in order:");
        System.out.println("  Do Savings/Current accounts share STATE?        YES — id, balance, auditLog");
        System.out.println("  Do they share a CONSTRUCTOR ritual?             YES — every account opens the same way");
        System.out.println("  Is there partial CODE + protected members?      YES — deposit/withdraw/pay, protected monthlyFee");
        System.out.println("  => ONE 'yes' is enough: abstract class. All four are 'yes' here.");
        System.out.println("  And they are an IS-A family: a SavingsAccount IS-AN Account.");

        banner("3. Walk-through B: why Transferable is an INTERFACE");

        System.out.println("  Shared state needed?                            NO — only signatures");
        System.out.println("  Needed by UNRELATED families?                   YES — Account AND CardPayment");
        System.out.println("  Needed ALONGSIDE other contracts?               YES — CardPayment wears four");
        System.out.println("  Published to code we do not own (partner banks, future classes)?  YES");
        System.out.println("  => interface. It is a CAN-DO ('transferable'), not an IS-A.");

        banner("4. The JDK does the SAME thing: interface + abstract skeleton");

        // Reflection over java.util — the pattern in the wild, deterministic facts:
        System.out.println("List.isInterface()                       = " + List.class.isInterface());
        System.out.println("AbstractList is abstract                 = "
                + Modifier.isAbstract(AbstractList.class.getModifiers()));
        System.out.println("AbstractList implements List             = "
                + List.class.isAssignableFrom(AbstractList.class));
        System.out.println("ArrayList extends AbstractList           = "
                + (ArrayList.class.getSuperclass() == AbstractList.class));
        System.out.println("Map.isInterface() / AbstractMap abstract = "
                + Map.class.isInterface() + " / " + Modifier.isAbstract(java.util.AbstractMap.class.getModifiers()));
        System.out.println();
        System.out.println("=> the SKELETAL IMPLEMENTATION pattern: the INTERFACE is the contract strangers");
        System.out.println("   code against (List), the ABSTRACT CLASS is free plumbing for friends");
        System.out.println("   (extend AbstractList, implement 2 methods, get ~20 for nothing).");
        System.out.println("   Interface + abstract skeleton = best of both. Not either/or — BOTH.");
        System.out.println();
        System.out.println("RULE OF THUMB: interfaces for CAN-DO contracts,");
        System.out.println("               abstract classes for IS-A families with shared plumbing.");
    }

    private static void row(String capability, String iface, String abs) {
        System.out.printf("%-38s | %-30s | %-30s%n", capability, iface, abs);
    }

    private static String repeat(char c, int n) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < n; i++) {          // classic loop — String.repeat is Java 11, we are on 8
            sb.append(c);
        }
        return sb.toString();
    }

    static void banner(String title) {
        System.out.println();
        System.out.println("=== " + title + " ===");
    }
}
