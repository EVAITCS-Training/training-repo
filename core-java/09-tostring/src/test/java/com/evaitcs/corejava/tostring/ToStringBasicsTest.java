package com.evaitcs.corejava.tostring;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests as PROOF for the claims made in the demos. Run with:
 *   .\mvnw.cmd -pl 09-tostring test
 * Each test proves ONE claim and prints its evidence, so the test run doubles as a demo.
 *
 * Everything asserted here is DETERMINISTIC. The default-toString test cuts the hex out
 * of the printed string and recomputes Integer.toHexString(System.identityHashCode(o))
 * in the SAME JVM run — an object's identity hash cannot change mid-flight, so the
 * comparison is exact, not a fuzzy regex hope.
 */
class ToStringBasicsTest {

    private static AccountRaw rawAccount() {
        return new AccountRaw("ACC-1001", "Ada Lovelace", new BigDecimal("1250.75"), "4921");
    }

    private static AccountPrinted printedAccount() {
        return new AccountPrinted("ACC-1001", "Ada Lovelace", new BigDecimal("1250.75"),
                "4111111111111234", "4921");
    }

    @Test
    @DisplayName("default toString = getClass().getName() + '@' + toHexString(identityHashCode)")
    void defaultFormatIsClassNameAtIdentityHex() {
        AccountRaw raw = rawAccount();
        String printed = raw.toString();
        System.out.println("raw.toString() = " + printed);

        assertTrue(printed.matches("com\\.evaitcs\\.corejava\\.tostring\\.AccountRaw@[0-9a-f]+"),
                "default format must be fully.qualified.ClassName@lowercaseHex");

        String hex = printed.substring(printed.indexOf('@') + 1);
        String recomputed = Integer.toHexString(System.identityHashCode(raw));
        System.out.println("hex after '@'                  = " + hex);
        System.out.println("toHexString(identityHashCode)  = " + recomputed);
        assertEquals(recomputed, hex, "the hex IS the identity hash code (module 08: default hashCode = identity)");
    }

    @Test
    @DisplayName("valueOf/concat handle a null OBJECT ('null'), but valueOf((char[]) null) NPEs")
    void nullObjectIsSafeButNullCharArrayNPEs() {
        Object nothing = null;
        System.out.println("String.valueOf((Object) null) = " + String.valueOf(nothing));
        System.out.println("\"balance: \" + nothing        = balance: " + nothing);
        assertEquals("null", String.valueOf(nothing), "the Object overload null-checks first");
        assertEquals("null", "" + nothing, "concat compiles to append(Object) -> same valueOf road");

        // Overload resolution is COMPILE-time on the static type: the char[] variable
        // selects valueOf(char[]), which does new String(chars) — dereferencing the
        // array — with no null check anywhere on that path.
        char[] nullChars = null;
        NullPointerException e = assertThrows(NullPointerException.class,
                () -> String.valueOf(nullChars));
        System.out.println("String.valueOf((char[]) null) threw " + e.getClass().getName() + " — the classic trap");
    }

    @Test
    @DisplayName("a collection's toString calls YOUR toString once per element")
    void collectionsCallToStringPerElement() {
        List<Transaction> statement = Transaction.sampleStatement();

        // Hand-glue what AbstractCollection.toString does internally: "[e1, e2, ...]"
        // where each eN is String.valueOf(element) — i.e. the element's own toString.
        StringBuilder expected = new StringBuilder("[");
        for (int i = 0; i < statement.size(); i++) {
            if (i > 0) {
                expected.append(", ");
            }
            expected.append(statement.get(i).toString());
        }
        expected.append(']');

        System.out.println("list.toString()     = " + statement);
        System.out.println("hand-glued elements = " + expected);
        assertEquals(expected.toString(), statement.toString(),
                "the list must be exactly [ ] + comma-joined element toStrings");

        // And with elements that override nothing, the list is a wall of fingerprints —
        // proving the collection ALWAYS delegates; readability comes from the ELEMENTS.
        List<AccountRaw> raws = Arrays.asList(rawAccount(), rawAccount());
        System.out.println("list of raw accounts = " + raws);
        assertTrue(raws.toString().contains("AccountRaw@"),
                "raw elements contribute their default fingerprint inside the list");
    }

    @Test
    @DisplayName("a subclass with no toString inherits the superclass's — name lie vs getClass() fix")
    void subclassInheritsSuperclassToString() throws Exception {
        // The hardcoded-name base: LegacySavings inherits LegacyAccount.toString and
        // prints "Account[...]" — the class name lies by omission, interestRate vanishes.
        InheritedToStringDemo.LegacySavings legacy = new InheritedToStringDemo.LegacySavings(
                "ACC-2002", "Grace Hopper", new BigDecimal("9300.10"), new BigDecimal("3.25"));
        System.out.println("legacy.toString()  = " + legacy);
        assertTrue(legacy.toString().startsWith("Account["),
                "the hardcoded base name wins for the subclass");
        assertEquals(InheritedToStringDemo.LegacyAccount.class,
                legacy.getClass().getMethod("toString").getDeclaringClass(),
                "the toString that runs is DECLARED IN THE SUPERCLASS");

        // The getClass()-based base: SavingsAccount writes no toString, yet prints its
        // OWN name — the method is inherited, but getClass() asks the RUNTIME object.
        SavingsAccount savings = new SavingsAccount(
                "ACC-2002", "Grace Hopper", new BigDecimal("9300.10"),
                "5500000000005678", "7788", new BigDecimal("3.25"));
        System.out.println("savings.toString() = " + savings);
        assertEquals(AccountPrinted.class,
                SavingsAccount.class.getMethod("toString").getDeclaringClass(),
                "still inherited — SavingsAccount declares no toString of its own");
        assertTrue(savings.toString().startsWith("SavingsAccount["),
                "getClass().getSimpleName() reports the RUNTIME class, not the declaring one");
    }

    @Test
    @DisplayName("good toString shows the business keys and MASKS the secrets")
    void goodToStringMasksSecrets() {
        AccountPrinted account = printedAccount();
        String s = account.toString();
        System.out.println("account.toString() = " + s);

        assertTrue(s.contains("id=ACC-1001") && s.contains("owner=Ada Lovelace")
                        && s.contains("balance=1250.75"),
                "business keys must be present and human-readable");
        assertTrue(s.contains("card=****1234"), "the card keeps ONLY its last four digits");
        assertTrue(s.contains("pin=****"), "the pin shows no digits at all");
        assertFalse(s.contains("4921"), "the pin must not appear ANYWHERE in the output");
        assertFalse(s.contains("4111111111111234"), "the full card number must not appear");
    }

    @Test
    @DisplayName("leaky toString exports the secrets — the danger, proven")
    void leakyToStringExportsSecrets() {
        AccountLeaky leaky = new AccountLeaky(
                "ACC-1001", "Ada Lovelace", new BigDecimal("1250.75"), "4921", "sk-9f3ab7c2e1");
        String s = leaky.toString();
        System.out.println("THIS IS WHAT A SINGLE LOG LINE WOULD STORE: " + s);

        assertTrue(s.contains("pin=4921"), "the pin leaks in plaintext");
        assertTrue(s.contains("secretKey=sk-9f3ab7c2e1"), "the API key leaks in plaintext");
        System.out.println("=> Both secrets now live in every log file, shipper and error-tracker downstream.");
    }

    @Test
    @DisplayName("%s, valueOf and concat ALL route through toString — you don't call it, they do")
    void implicitCallersUseToString() {
        AccountPrinted account = printedAccount();

        assertEquals(account.toString(), String.format("%s", account), "%s must equal toString()");
        assertEquals(account.toString(), String.valueOf(account), "valueOf must equal toString()");
        assertTrue(("approved: " + account).endsWith(account.toString()), "concat appends toString()");

        System.out.println("String.format(\"%s\", account) = " + String.format("%s", account));
        System.out.println("\"approved: \" + account      = approved: " + account);
    }
}
