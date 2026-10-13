package com.evaitcs.corejava.access;

import com.evaitcs.corejava.access.bank.BankAccount;
import com.evaitcs.corejava.access.other.SubclassInOtherPackage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests as PROOF for the reflection claims: (1) the modifiers on BankAccount's
 * members are exactly what the README's matrix says - read straight off the
 * class file via reflection; (2) private members ARE inherited into the
 * object's memory; (3) setAccessible(true) is the escape hatch, and the
 * runtime locks the door until you use it.
 */
class ReflectionAndVisibilityMatrixTest {

    @Test
    @DisplayName("the class file itself confirms the four modifier levels")
    void fieldsCarryTheExpectedModifiers() throws Exception {
        Field id = BankAccount.class.getDeclaredField("id");
        Field balance = BankAccount.class.getDeclaredField("balance");
        Field auditNote = BankAccount.class.getDeclaredField("auditNote");
        Field pin = BankAccount.class.getDeclaredField("pin");

        System.out.println("modifier flags read off BankAccount.class:");
        System.out.println("  id        : " + Modifier.toString(id.getModifiers()));
        System.out.println("  balance   : " + Modifier.toString(balance.getModifiers()));
        System.out.println("  auditNote : " + Modifier.toString(auditNote.getModifiers())
                + "  <- EMPTY! package-private has no keyword and no flag");
        System.out.println("  pin       : " + Modifier.toString(pin.getModifiers()));

        assertTrue(Modifier.isPublic(id.getModifiers()));
        assertTrue(Modifier.isProtected(balance.getModifiers()));
        assertEquals(0, auditNote.getModifiers() & (Modifier.PUBLIC | Modifier.PROTECTED | Modifier.PRIVATE),
                "package-private = none of the three flags set");
        assertTrue(Modifier.isPrivate(pin.getModifiers()));
    }

    @Test
    @DisplayName("class-level modifiers: BankAccount public, helper package-private, Vault private nested")
    void classLevelModifiersAreAsTaught() throws ClassNotFoundException {
        assertTrue(Modifier.isPublic(BankAccount.class.getModifiers()));

        Class<?> helper = Class.forName("com.evaitcs.corejava.access.bank.PackagePrivateHelper");
        System.out.println("PackagePrivateHelper public? " + Modifier.isPublic(helper.getModifiers())
                + "  <- top-level, so package-private is its ceiling short of public");
        assertFalse(Modifier.isPublic(helper.getModifiers()));

        Class<?>[] nested = BankAccount.class.getDeclaredClasses();
        assertEquals(1, nested.length);
        System.out.println("BankAccount's only nested class: " + nested[0].getSimpleName()
                + ", private? " + Modifier.isPrivate(nested[0].getModifiers())
                + ", static? " + Modifier.isStatic(nested[0].getModifiers()));
        assertEquals("Vault", nested[0].getSimpleName());
        assertTrue(Modifier.isPrivate(nested[0].getModifiers()));
    }

    @Test
    @DisplayName("the door is locked: field.get WITHOUT setAccessible throws IllegalAccessException")
    void runtimeEnforcesPrivateUntilSetAccessible() throws Exception {
        BankAccount acct = BankAccount.sampleAccount();
        Field pin = BankAccount.class.getDeclaredField("pin");

        IllegalAccessException locked = assertThrows(IllegalAccessException.class, () -> pin.get(acct));
        System.out.println("pin.get(acct) threw: " + locked.getClass().getSimpleName());

        pin.setAccessible(true); // the escape hatch
        Object value = pin.get(acct);
        System.out.println("after setAccessible(true): pin = " + value
                + "   <- escape hatch, not a design tool");
        assertEquals("2468", value);
    }

    @Test
    @DisplayName("misconception busted: private fields ARE inherited into the object's memory")
    void privateFieldsAreInheritedIntoSubclassMemory() throws Exception {
        SubclassInOtherPackage sub = new SubclassInOtherPackage("SB-2002", new BigDecimal("100.00"));

        // The subclass declares ZERO fields of its own - yet a pin lives in every instance:
        assertEquals(0, sub.getClass().getDeclaredFields().length);

        Field pin = BankAccount.class.getDeclaredField("pin"); // declared on the SUPERclass
        pin.setAccessible(true);
        Object value = pin.get(sub);                           // ...but read off the SUBCLASS object

        System.out.println("sub declares " + sub.getClass().getDeclaredFields().length + " own fields,");
        System.out.println("yet BankAccount.pin on the sub instance = " + value);
        assertNotNull(value);
        System.out.println("=> 'private members are not inherited' is WRONG about memory and RIGHT");
        System.out.println("   about names: the field is IN the object; no subclass can write `pin`.");
        assertTrue(sub.checkPin("9999"), "and the inherited private state still works through the public door");
    }
}
