package com.evaitcs.corejava.statics;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Proof for StaticMethodsAndFactoriesDemo. The headline claim - "a static method
 * cannot touch instance fields" - CANNOT be tested at runtime: code that violates
 * it never compiles, so the proof lives in the source as the commented-out line
 * plus its exact javac message (see StaticMethodsAndFactoriesDemo, section 2).
 * What IS testable here: the three things static methods make possible.
 */
class StaticMethodsTest {

    @Test
    @DisplayName("a static method runs with zero instances - no object, no `this`, no problem")
    void staticMethodsWorkBeforeAnyObjectExists() {
        // No BankAccount is constructed in this test; BankUtils never needs one.
        String formatted = BankUtils.formatMoney(new BigDecimal("1234.5"));
        System.out.println("BankUtils.formatMoney(1234.5) = " + formatted + "   (zero instances built)");
        assertEquals("1234.50 USD", formatted);
        assertEquals(7, Math.max(3, 7)); // the JDK's oldest utility class, same trick
    }

    @Test
    @DisplayName("the mirror image compiles: an instance method reads the static freely")
    void instanceMethodReadsStaticField() {
        // WHY the one-way street exists: every object knows its CLASS, so instance
        // code can always reach the static slot. A static method has no object, so
        // it can never reach a particular instance's `balance` - which balance would
        // it even mean? Hence the (uncompilable, hence commented) error:
        //   "non-static variable balance cannot be referenced from a static context"
        BankAccount acc = new BankAccount("Ann", new BigDecimal("1.00"));
        String description = acc.describe(); // reads BankAccount.BANK_NAME + getAccountCount() inside
        System.out.println("instance method reading statics: " + description);
        assertTrue(description.contains(BankAccount.BANK_NAME));
        assertTrue(description.contains("accounts so far: " + BankAccount.getAccountCount()));
    }

    @Test
    @DisplayName("the static factory returns a fully configured instance")
    void staticFactoryProducesConfiguredInstance() {
        BankAccount savings = BankAccount.createSavings("Erin");
        System.out.println("BankAccount.createSavings(\"Erin\") -> " + savings.describe());
        assertNotNull(savings);
        assertEquals("Erin", savings.getOwner());
        assertEquals(0, savings.getBalance().compareTo(BigDecimal.ZERO)); // 0.00, pre-configured
        assertNotNull(savings.getId());
        // A constructor could do this too - but it could not be NAMED createSavings,
        // could not return a cached instance, and could not return a subtype.
    }
}
