package com.evaitcs.java8.interfaces;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * The interface-evolution problem — WHY Java 8 had to change what an interface can contain.
 *
 * Before Java 8 an interface held constants and abstract methods, nothing else. That sounds
 * harmless until you PUBLISH one: adding a method breaks every implementor on earth, including
 * code you have never seen (partner banks compiling against your jar). The JDK itself was
 * trapped — it could not add anything to Collection without breaking millions of classes.
 *
 * Java 8's escape hatch: `default` methods — a method WITH A BODY on the interface. Existing
 * implementors inherit it, compile unchanged, keep running. The proof you use every day:
 * Collection.stream() arrived in Java 8 on every collection that existed since 1998 —
 * default methods are the only reason that was legal.
 */
public class WhyDefaultMethodsDemo {

    /** Published by SecureBank in 2014 — partner banks all over the world compiled against v1. */
    interface Notifier {

        /** The ONLY method in v1: every implementor had to write this one. */
        String notifyAccountHolder(String accountId, String message);

        // ---- 2015 product requirement: "notify ALL holders at once" ----
        // The Java 7 option — adding an ABSTRACT method — breaks every v1 implementor:
        //     error: SmsNotifier is not abstract and does not override abstract method
        //            notifyAllHolders(List<String>,String) in Notifier
        // The Java 8 option is the `default` method below: nobody breaks.

        /**
         * Added in v2 AS A DEFAULT — point at the keyword: a method WITH A BODY, living in an
         * interface. Implementors inherit it for free; a default is a starting point, not a law.
         */
        default List<String> notifyAllHolders(List<String> ids, String message) {
            List<String> log = new ArrayList<>();
            for (String id : ids) {
                log.add(notifyAccountHolder(id, message)); // delegate to the v1 abstract method
            }
            return log;
        }
    }

    /** Written in 2014 against Notifier v1. Nobody ever touched this file — it still compiles. */
    static class SmsNotifier implements Notifier {
        @Override
        public String notifyAccountHolder(String accountId, String message) {
            String line = String.format("[SMS   -> %-6s] %s", accountId, message);
            System.out.println("   " + line);
            return line;
        }
        // Notice what is NOT here: notifyAllHolders. The interface shipped it to us.
    }

    /** Sometimes the inherited default is not good enough — override it like any method. */
    static class PushNotifier implements Notifier {
        @Override
        public String notifyAccountHolder(String accountId, String message) {
            return String.format("[PUSH  -> %-6s] %s", accountId, message);
        }

        @Override
        public List<String> notifyAllHolders(List<String> ids, String message) {
            // The push vendor has a batch endpoint: ONE call instead of N — the loop is wrong here.
            return Arrays.asList("[PUSH-BATCH -> " + ids.size() + " devices] " + message);
        }
    }

    public static void main(String[] args) {
        List<String> holders = Arrays.asList("ACC-1", "ACC-2", "ACC-3");
        String message = "Your debit card is ready for collection";

        System.out.println("=== A 2014 class using a 2015 method it never declared ===");
        SmsNotifier sms = new SmsNotifier();
        List<String> smsLog = sms.notifyAllHolders(holders, message);
        System.out.println("   SmsNotifier declares only: " + declaredMethodNames(SmsNotifier.class));
        System.out.println("   ...yet notifyAllHolders just returned " + smsLog.size() + " log lines.");

        System.out.println();
        System.out.println("=== Overriding the default when the free version is wrong ===");
        PushNotifier push = new PushNotifier();
        System.out.println("   " + push.notifyAllHolders(holders, message));

        System.out.println();
        System.out.println("=== The real-world proof: the JDK did exactly this to Collection ===");
        printDefaultStatus(Collection.class, "stream");
        printDefaultStatus(Collection.class, "removeIf");
        printDefaultStatus(Iterable.class, "forEach");
        printDefaultStatus(List.class, "sort");
        System.out.println("   All default methods — that is how the JDK grew without breaking the world.");

        System.out.println();
        long credits = Arrays.asList("PAYROLL +2500.00", "RENT -1200.00").stream()
                .filter(entry -> entry.contains("+"))
                .count();
        System.out.println("   And you have been calling one since module 01 — credit entries: " + credits);
    }

    /** Reflection corner: Method.isDefault() itself is new in Java 8 — it exists for THIS question. */
    private static void printDefaultStatus(Class<?> type, String methodName) {
        for (Method m : type.getMethods()) {
            if (m.getName().equals(methodName)) {
                System.out.printf("   %-26s isDefault() = %s%n", type.getSimpleName() + "." + methodName + "()", m.isDefault());
                return;
            }
        }
        throw new IllegalStateException(methodName + " not found on " + type.getName());
    }

    private static List<String> declaredMethodNames(Class<?> c) {
        List<String> names = new ArrayList<>();
        for (Method m : c.getDeclaredMethods()) { names.add(m.getName()); }
        return names;
    }
}
