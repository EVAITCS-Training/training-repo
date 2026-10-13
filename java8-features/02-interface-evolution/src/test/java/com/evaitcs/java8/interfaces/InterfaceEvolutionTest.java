package com.evaitcs.java8.interfaces;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests as PROOF for the claims made in the demos and README. Run with:
 *   .\mvnw.cmd -pl 02-interface-evolution test
 * Each test prints the evidence it asserts, so the test run doubles as a demo.
 */
class InterfaceEvolutionTest {

    @Test
    @DisplayName("a 2014 implementor gets the 2015 default method FOR FREE")
    void oldImplementorGetsNewDefaultForFree() {
        WhyDefaultMethodsDemo.SmsNotifier sms = new WhyDefaultMethodsDemo.SmsNotifier();
        List<String> declared = declaredNames(WhyDefaultMethodsDemo.SmsNotifier.class);
        System.out.println("SmsNotifier declares: " + declared);

        List<String> log = sms.notifyAllHolders(Arrays.asList("ACC-1", "ACC-2", "ACC-3"), "card ready");
        for (String line : log) {
            System.out.println("  produced by the inherited default: " + line);
        }

        assertEquals(3, log.size(), "the inherited default must loop over all holders");
        for (String line : log) {
            assertTrue(line.startsWith("[SMS"), "each line came from SmsNotifier's own v1 method");
        }
        assertFalse(declared.contains("notifyAllHolders"), "SmsNotifier must NOT declare it — it inherits it");
    }

    @Test
    @DisplayName("a default is a starting point, not a law: overriding it wins")
    void overridingTheDefaultWins() {
        WhyDefaultMethodsDemo.PushNotifier push = new WhyDefaultMethodsDemo.PushNotifier();
        List<String> log = push.notifyAllHolders(Arrays.asList("ACC-1", "ACC-2"), "card ready");
        System.out.println("PushNotifier replaced the inherited loop with one batch call: " + log);
        assertEquals(1, log.size());
        assertTrue(log.get(0).contains("PUSH-BATCH"));
    }

    @Test
    @DisplayName("Collection.stream() exists on every old collection BECAUSE it is a default method")
    void jdkGrewViaDefaultMethods() throws Exception {
        Method stream = Collection.class.getMethod("stream");
        Method removeIf = Collection.class.getMethod("removeIf", Predicate.class);
        Method forEach = Iterable.class.getMethod("forEach", Consumer.class);
        Method sort = List.class.getMethod("sort", Comparator.class);
        System.out.println("Collection.stream()   isDefault=" + stream.isDefault());
        System.out.println("Collection.removeIf() isDefault=" + removeIf.isDefault());
        System.out.println("Iterable.forEach()    isDefault=" + forEach.isDefault());
        System.out.println("List.sort()           isDefault=" + sort.isDefault());
        assertTrue(stream.isDefault());
        assertTrue(removeIf.isDefault());
        assertTrue(forEach.isDefault());
        assertTrue(sort.isDefault());

        // And they genuinely work on a plain old ArrayList:
        List<String> accounts = new ArrayList<>(Arrays.asList("ACC-1", "ACC-22", "ACC-333"));
        accounts.removeIf(a -> a.length() > 5);
        long viaStream = accounts.stream().count();
        System.out.println("after removeIf(length > 5): " + accounts + ", stream().count()=" + viaStream);
        assertEquals(Arrays.asList("ACC-1"), accounts);
        assertEquals(1L, viaStream);
    }

    @Test
    @DisplayName("static interface methods: callable ONLY as InterfaceName.method(), never inherited")
    void staticMethodsLiveOnTheInterfaceOnly() {
        StaticInterfaceMethodDemo.MoneyRules standard = StaticInterfaceMethodDemo.MoneyRules.standard();
        boolean small = standard.allows(new BigDecimal("100.00"));
        boolean big = standard.allows(new BigDecimal("1000.00"));
        System.out.println("MoneyRules.standard().allows(100.00)=" + small + ", allows(1000.00)=" + big);
        assertTrue(small);
        assertFalse(big);

        List<String> onInterface = declaredNames(StaticInterfaceMethodDemo.MoneyRules.class);
        List<String> onClass = declaredNames(StaticInterfaceMethodDemo.TellerTerminal.class);
        List<String> onSub = declaredNames(StaticInterfaceMethodDemo.StrictRules.class);
        System.out.println("declared on MoneyRules    : " + onInterface);
        System.out.println("declared on TellerTerminal: " + onClass);
        System.out.println("declared on StrictRules   : " + onSub);
        assertTrue(onInterface.contains("standard"));
        assertFalse(onClass.contains("standard"), "implementing classes do NOT inherit statics");
        assertFalse(onSub.contains("standard"), "sub-interfaces do NOT inherit statics either");

        // The contrast students must see: CONSTANTS *are* inherited.
        assertEquals(new BigDecimal("500.00"), StaticInterfaceMethodDemo.TellerTerminal.TRANSFER_LIMIT);
        System.out.println("TellerTerminal.TRANSFER_LIMIT = " + StaticInterfaceMethodDemo.TellerTerminal.TRANSFER_LIMIT
                + "  (constants ARE inherited; static methods are not)");
    }

    @Test
    @DisplayName("rule 1: a superclass method WINS over any interface default")
    void classBeatsInterfaceDefault() {
        ResolutionRulesDemo.Greeter bare = new ResolutionRulesDemo.Greeter() { };
        ResolutionRulesDemo.TraineeTeller trainee = new ResolutionRulesDemo.TraineeTeller();
        System.out.println("bare Greeter default      : " + bare.greet());
        System.out.println("TraineeTeller (EMPTY body): " + trainee.greet());
        assertTrue(bare.greet().contains("INTERFACE"), "with no class around, the default runs");
        assertTrue(trainee.greet().contains("BankTeller CLASS"), "the superclass beat the interface default");
    }

    @Test
    @DisplayName("a class's own override beats everything (rules pick only among code you didn't write)")
    void ownOverrideBeatsAll() {
        ResolutionRulesDemo.LoudTrainee loud = new ResolutionRulesDemo.LoudTrainee();
        System.out.println("LoudTrainee.greet() = " + loud.greet());
        assertTrue(loud.greet().startsWith("HEY!"));
        assertTrue(loud.greet().contains("BankTeller CLASS"), "super.greet() still reaches the class version");
    }

    @Test
    @DisplayName("rule 2: the more specific (sub-)interface default wins")
    void subinterfaceBeatsSuperinterface() {
        ResolutionRulesDemo.Channel bare = new ResolutionRulesDemo.Channel() { };
        ResolutionRulesDemo.AutoResponder responder = new ResolutionRulesDemo.AutoResponder();
        ResolutionRulesDemo.Channel polymorphic = responder;
        System.out.println("bare Channel default       : " + bare.describe());
        System.out.println("AutoResponder              : " + responder.describe());
        System.out.println("same object typed Channel  : " + polymorphic.describe());
        assertTrue(bare.describe().contains("generic"));
        assertTrue(responder.describe().contains("SmsChannel"));
        assertTrue(polymorphic.describe().contains("SmsChannel"),
                "the OBJECT's most specific default wins, not the variable's declared type");
    }

    @Test
    @DisplayName("rule 3: the diamond FORCES an override; InterfaceName.super.method() picks the side")
    void diamondConflictResolvedWithQualifiedSuper() {
        DiamondConflictDemo.MultiNotifier multi = new DiamondConflictDemo.MultiNotifier();
        String combined = multi.describe();
        System.out.println("MultiNotifier.describe()   = " + combined);
        System.out.println("  describeViaEmail()       = " + multi.describeViaEmail());
        System.out.println("  describeViaSms()         = " + multi.describeViaSms());
        assertEquals("MULTI(email-channel + sms-channel)", combined,
                "the override must be able to reach BOTH defaults via InterfaceName.super");
        assertEquals("email-channel", multi.describeViaEmail());
        assertEquals("sms-channel", multi.describeViaSms());
    }

    @Test
    @DisplayName("module 01's composition rides on THIS feature: and/negate are default methods")
    void predicateCombinatorsAreDefaultMethods() throws Exception {
        Method test = Predicate.class.getMethod("test", Object.class);
        Method and = Predicate.class.getMethod("and", Predicate.class);
        Method negate = Predicate.class.getMethod("negate");
        System.out.println("Predicate.test   abstract=" + Modifier.isAbstract(test.getModifiers())
                + " default=" + test.isDefault());
        System.out.println("Predicate.and    abstract=" + Modifier.isAbstract(and.getModifiers())
                + " default=" + and.isDefault());
        System.out.println("Predicate.negate abstract=" + Modifier.isAbstract(negate.getModifiers())
                + " default=" + negate.isDefault());
        assertTrue(Modifier.isAbstract(test.getModifiers()) && !test.isDefault(),
                "exactly one abstract method is what keeps Predicate a functional interface");
        assertTrue(and.isDefault());
        assertTrue(negate.isDefault());

        Predicate<String> isPayroll = e -> e.startsWith("PAYROLL");
        Predicate<String> isCredit = e -> e.contains("+");
        Predicate<String> payrollCredit = isPayroll.and(isCredit);
        System.out.println("payrollCredit.test(\"PAYROLL +2500.00\") = " + payrollCredit.test("PAYROLL +2500.00"));
        System.out.println("payrollCredit.test(\"RENT -1200.00\")    = " + payrollCredit.test("RENT -1200.00"));
        assertTrue(payrollCredit.test("PAYROLL +2500.00"));
        assertFalse(payrollCredit.test("RENT -1200.00"));
        assertTrue(isPayroll.negate().test("RENT -1200.00"));
    }

    private static List<String> declaredNames(Class<?> c) {
        List<String> names = new ArrayList<>();
        for (Method m : c.getDeclaredMethods()) {
            names.add(m.getName());
        }
        return names;
    }
}
