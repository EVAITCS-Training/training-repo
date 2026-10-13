package com.evaitcs.corejava.access;

import com.evaitcs.corejava.access.bank.BankAccount;
import com.evaitcs.corejava.access.other.SubclassInOtherPackage;

import java.lang.reflect.Field;
import java.math.BigDecimal;

/**
 * The escape hatch: reflection IGNORES private - if you ask it to.
 *
 * Two jobs here: (1) bust the misconception "private members are not
 * inherited" - the pin field physically exists in EVERY BankAccount object,
 * including subclass instances; `private` limits access BY NAME at COMPILE
 * time, not the object's memory layout. (2) Show setAccessible(true) picking
 * the lock, and say clearly why that is a testing/debugging tool, never a
 * design tool.
 */
public class ReflectionEscapeHatchDemo {

    public static void main(String[] args) {

        banner("1. The door is locked by default - even for reflection");
        BankAccount acct = BankAccount.sampleAccount();
        try {
            Field pin = BankAccount.class.getDeclaredField("pin"); // getDECLARED: includes private
            Object value = pin.get(acct);                          // no setAccessible yet...
            System.out.println("read without permission?! " + value);
        } catch (NoSuchFieldException e) {
            System.out.println("no such field: " + e.getMessage());
        } catch (IllegalAccessException e) {
            System.out.println("pin.get(acct) threw IllegalAccessException:");
            System.out.println("  " + e.getMessage());
            System.out.println("=> the runtime ENFORCES private, just like the compiler did.");
        }

        banner("2. setAccessible(true) picks the lock");
        try {
            Field pin = BankAccount.class.getDeclaredField("pin");
            pin.setAccessible(true); // "I know what I'm doing" - the language steps aside
            System.out.println("pin field value = " + pin.get(acct)
                    + "   <- the 'unreachable' secret, reached");
        } catch (ReflectiveOperationException e) {
            System.out.println("reflection failed: " + e);
        }
        System.out.println("Reflection is the escape hatch for frameworks, tests and debuggers -");
        System.out.println("NOT a design tool. Code that needs setAccessible to work is telling");
        System.out.println("you the API is missing a method.");

        banner("3. Private fields ARE inherited into the object's memory");
        SubclassInOtherPackage sub = new SubclassInOtherPackage("SB-2002", new BigDecimal("100.00"));
        System.out.println("sub.getClass().getDeclaredFields().length = "
                + sub.getClass().getDeclaredFields().length
                + "   <- the subclass declares NO fields of its own...");
        try {
            Field pin = BankAccount.class.getDeclaredField("pin"); // declared on the SUPERclass...
            pin.setAccessible(true);
            Object value = pin.get(sub);                           // ...yet it reads fine on a SUBCLASS object
            System.out.println("BankAccount's private pin on a SubclassInOtherPackage instance = " + value);
        } catch (ReflectiveOperationException e) {
            System.out.println("reflection failed: " + e);
        }
        System.out.println("=> the private field is PHYSICALLY in every instance; inheritance puts");
        System.out.println("   it in memory. What subclasses never get is access BY NAME.");
    }

    static void banner(String title) {
        System.out.println();
        System.out.println("=== " + title + " ===");
    }
}
