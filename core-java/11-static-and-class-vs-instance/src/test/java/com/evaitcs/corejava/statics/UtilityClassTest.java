package com.evaitcs.corejava.statics;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Proof for UtilityClassDemo: the private constructor exists but is inaccessible
 * (module 06's reflection story), and `static final` freezes the REFERENCE while
 * the object behind it stays mutable.
 */
class UtilityClassTest {

    @Test
    @DisplayName("BankUtils has exactly one constructor, it is private, and reflection proves both halves")
    void privateConstructorExistsButIsInaccessible() {
        Constructor<?>[] ctors = BankUtils.class.getDeclaredConstructors();
        assertEquals(1, ctors.length, "a utility class declares exactly one (private) ctor");
        Constructor<?> ctor = ctors[0];
        System.out.println("reflection found: " + ctor);
        assertTrue(Modifier.isPrivate(ctor.getModifiers()), "the ctor must be private");

        // Honest attempt: the runtime enforces private just like the compiler did.
        IllegalAccessException locked = assertThrows(IllegalAccessException.class,
                () -> ctor.newInstance());
        System.out.println("newInstance() without permission -> IllegalAccessException: " + locked.getMessage());

        // Escape hatch (module 06): setAccessible(true) picks the lock - and walks
        // straight into the ctor's AssertionError guard, which is the second line of
        // defense every serious utility class carries.
        ctor.setAccessible(true);
        InvocationTargetException guard = assertThrows(InvocationTargetException.class,
                () -> ctor.newInstance());
        assertTrue(guard.getCause() instanceof AssertionError, "the guard message must fire");
        System.out.println("newInstance() with setAccessible(true) -> ctor body threw: " + guard.getCause());
    }

    @Test
    @DisplayName("static final List: contents mutable (add works), reference frozen (assignment is a compile error)")
    void staticFinalFreezesReferenceNotContents() {
        int before = BankUtils.SUPPORTED_CURRENCIES.size();
        BankUtils.SUPPORTED_CURRENCIES.add("GBP"); // legal! `final` froze the slot, not the object
        System.out.println("SUPPORTED_CURRENCIES before = " + before + " entries, after add(\"GBP\") = "
                + BankUtils.SUPPORTED_CURRENCIES);
        assertEquals(before + 1, BankUtils.SUPPORTED_CURRENCIES.size());
        BankUtils.SUPPORTED_CURRENCIES.remove("GBP"); // tidy up - this list is a GLOBAL (demo 5 says why that matters)

        // The re-assignment that does NOT compile - the exhibit is the message:
        //   BankUtils.SUPPORTED_CURRENCIES = new ArrayList<String>();
        //       javac: error: cannot assign a value to final variable SUPPORTED_CURRENCIES
        // Same split as effectively-final captures in java8-features/01-lambdas:
        // the VARIABLE is frozen, the OBJECT it points at is not.
    }
}
