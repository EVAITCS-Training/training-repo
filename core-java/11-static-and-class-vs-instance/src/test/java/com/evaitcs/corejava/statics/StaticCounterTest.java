package com.evaitcs.corejava.statics;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Proof for SharedStaticStateDemo: ONE static slot shared by every instance,
 * while instance fields stay per-object. Run with:
 *   .\mvnw.cmd -pl 11-static-and-class-vs-instance test
 */
class StaticCounterTest {

    @Test
    @DisplayName("the static counter ticks once per instance - one slot, not a copy each")
    void staticCounterIncrementsAcrossInstances() {
        // DELTAS, not absolutes: the counter is shared by the WHOLE JVM, and other
        // tests build accounts too. That fragility of absolute values is itself the
        // lesson - static state is global state (StaticStateCautionDemo).
        int before = BankAccount.getAccountCount();
        BankAccount a1 = new BankAccount("Ann", new BigDecimal("10.00"));
        BankAccount a2 = new BankAccount("Bo", new BigDecimal("20.00"));
        BankAccount a3 = BankAccount.createSavings("Cy");
        int after = BankAccount.getAccountCount();

        System.out.println("count before = " + before + ", after building 3 accounts = " + after);
        System.out.println("  a1 -> " + a1.describe());
        System.out.println("  a2 -> " + a2.describe());
        System.out.println("  a3 -> " + a3.describe());
        assertEquals(before + 3, after, "each `new` must tick the ONE shared counter exactly once");
        assertNotEquals(a1.getId(), a2.getId());
        assertNotEquals(a1.getId(), a3.getId());
    }

    @Test
    @DisplayName("mutating the static is seen by EVERY existing instance - there is no per-object copy")
    void twoInstancesShareOneStaticValueAfterMutation() {
        BankAccount a1 = new BankAccount("Dana", new BigDecimal("5.00"));
        BankAccount a2 = new BankAccount("Eve", new BigDecimal("6.00"));

        // The mutation: build a third account. Nothing touches a1 or a2...
        BankAccount a3 = new BankAccount("Fay", new BigDecimal("7.00"));
        int shared = BankAccount.getAccountCount();

        System.out.println("after building a3, the counter says " + shared);
        System.out.println("  a1 sees: " + a1.describe());
        System.out.println("  a2 sees: " + a2.describe());
        System.out.println("  a3 sees: " + a3.describe());

        // ...yet ALL THREE immediately report the new value: same slot.
        assertTrue(a1.describe().contains("accounts so far: " + shared), "a1 must see the mutated static");
        assertTrue(a2.describe().contains("accounts so far: " + shared), "a2 must see the mutated static");
        assertTrue(a3.describe().contains("accounts so far: " + shared), "a3 must see the mutated static");

        // The contrast that completes the picture: INSTANCE data stayed per-object.
        assertNotEquals(a1.getBalance(), a2.getBalance(), "instance fields are NOT shared");
        System.out.println("balances differ (" + a1.getBalance() + " vs " + a2.getBalance()
                + ") - instance state per object, static state per class.");
    }
}
