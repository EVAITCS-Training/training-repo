package com.evaitcs.advanced.annotations;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests as PROOF for the claims made in the demos. Run with:
 *   .\mvnw.cmd -pl 03-annotations test
 * Each test proves ONE claim and prints its evidence, so the test run doubles as a demo.
 *
 * Everything asserted here is DETERMINISTIC. Retention visibility, element defaults, the
 * validator's output (sorted, because getDeclaredFields order is unspecified), @Repeatable
 * unpacking, @Inherited's class-only reach, and the DI wiring are all facts about the class
 * file and reflection — no timing, no threads, no GC.
 */
class AnnotationProofTest {

    @Test
    @DisplayName("retention: SOURCE and CLASS notes are invisible at runtime; only RUNTIME is readable")
    void runtimeAnnotationsAreVisibleButSourceAndClassAreNot() throws Exception {
        Field source = RetentionMatrixDemo.class.getDeclaredField("sourceAnnotated");
        Field classNote = RetentionMatrixDemo.class.getDeclaredField("classAnnotated");
        Field runtimeFull = RetentionMatrixDemo.class.getDeclaredField("runtimeFull");

        assertFalse(source.isAnnotationPresent(SourceNote.class), "SOURCE is discarded by the compiler");
        assertFalse(classNote.isAnnotationPresent(ClassNote.class), "CLASS is written to the file but not loaded");

        RuntimeNote note = runtimeFull.getAnnotation(RuntimeNote.class);
        assertNotNull(note, "RUNTIME survives all the way to reflection");
        assertEquals("loaded", note.value());
        assertEquals(7, note.level());
        System.out.println("SOURCE visible=false, CLASS visible=false, RUNTIME visible=true"
                + " (value=\"" + note.value() + "\", level=" + note.level() + ")");
    }

    @Test
    @DisplayName("an annotation element with a default supplies its value when the use site omits it")
    void annotationDefaultsApplyWhenElementOmitted() throws Exception {
        Field runtimeDefaults = RetentionMatrixDemo.class.getDeclaredField("runtimeDefaults");

        RuntimeNote note = runtimeDefaults.getAnnotation(RuntimeNote.class);
        assertEquals("only value given", note.value(), "the element that WAS supplied reads back");
        assertEquals(1, note.level(), "the omitted element falls back to its declared default of 1");
        System.out.println("@RuntimeNote(\"only value given\") -> level() defaulted to " + note.level());
    }

    @Test
    @DisplayName("the validator flags EXACTLY the three planted problems, and nothing on a clean account")
    void validatorFlagsExactlyThePlantedViolations() {
        Account bad = new Account("A-1", null, new BigDecimal("-5.00"), new BigDecimal("100.000"));

        List<MiniValidator.Violation> violations = MiniValidator.validate(bad);
        assertEquals(3, violations.size(), "three planted problems -> three violations");
        assertEquals(List.of(
                        new MiniValidator.Violation("balance", "@Min(0)", "-5.00"),
                        new MiniValidator.Violation("owner", "@NotNull", "null"),
                        new MiniValidator.Violation("withdrawalLimit", "@CurrencyScale(2)", "scale 3")),
                violations,
                "exact list, sorted by field then rule because getDeclaredFields order is unspecified");

        Account good = new Account("A-2", "Dana", new BigDecimal("250.00"), new BigDecimal("500.00"));
        assertTrue(MiniValidator.validate(good).isEmpty(), "a clean account yields zero violations");
        System.out.println("dirty account -> " + violations);
        System.out.println("clean account -> " + MiniValidator.validate(good));
    }

    @Test
    @DisplayName("a @Repeatable annotation is unpacked by getAnnotationsByType, and both values are present")
    void repeatableAnnotationReturnsBothValues() {
        Audit[] audits = RepeatableInheritedDemo.AuditedService.class.getAnnotationsByType(Audit.class);

        assertEquals(2, audits.length, "two @Audit notes on one class");
        List<String> values = Arrays.stream(audits).map(Audit::value).toList();
        assertTrue(values.contains("login") && values.contains("transfer"), "both planted values come back");

        assertNull(RepeatableInheritedDemo.AuditedService.class.getAnnotation(Audit.class),
                "getAnnotation returns null — the compiler folded both into the container");
        Audits container = RepeatableInheritedDemo.AuditedService.class.getAnnotation(Audits.class);
        assertNotNull(container, "the compiler-generated container holds them");
        assertEquals(2, container.value().length);
        System.out.println("getAnnotationsByType(Audit) -> " + values + " ; container holds " + container.value().length);
    }

    @Test
    @DisplayName("@Inherited passes a class note to a subclass, but an overridden method inherits nothing")
    void inheritedAnnotationPassesToSubclassButNotToOverriddenMethod() throws Exception {
        assertNotNull(RepeatableInheritedDemo.ChildService.class.getAnnotation(SecureService.class),
                "the subclass reports its superclass's @Inherited class-level annotation");

        Method baseProcess = RepeatableInheritedDemo.BaseService.class.getDeclaredMethod("process");
        Method childProcess = RepeatableInheritedDemo.ChildService.class.getDeclaredMethod("process");
        assertTrue(baseProcess.isAnnotationPresent(Logged.class), "the base method carries @Logged");
        assertFalse(childProcess.isAnnotationPresent(Logged.class),
                "the override does NOT inherit it — @Inherited never applies to methods");
        System.out.println("ChildService inherits @SecureService (class); its process() override has no @Logged (method)");
    }

    @Test
    @DisplayName("the @WireMe container injects the dependency: the field is non-null and is the registered instance")
    void wireMeInjectsTheDependencyGraph() throws Exception {
        Map<String, Object> beans =
                WireMeDemo.wire(List.of(WireMeDemo.AuditLogger.class, WireMeDemo.TransferHandler.class));

        WireMeDemo.TransferHandler handler = (WireMeDemo.TransferHandler) beans.get("transfers");
        assertNotNull(handler.logger, "the @Inject field was filled by the container");
        assertSame(beans.get("auditLogger"), handler.logger, "the exact registered AuditLogger instance was injected");

        handler.transfer("A", "B");
        assertEquals("A->B", handler.logger.lastEvent, "the injected logger is live and was used");
        System.out.println("wired beans = " + beans.keySet()
                + " ; handler.logger injected and recorded '" + handler.logger.lastEvent + "'");
    }

    @Test
    @DisplayName("@Deprecated is RUNTIME-visible to reflection, but @Override is SOURCE and invisible")
    void deprecatedIsRuntimeVisibleButOverrideIsNot() throws Exception {
        Method legacy = BuiltInsDemo.class.getDeclaredMethod("legacyAccountCode", long.class);
        Deprecated deprecated = legacy.getAnnotation(Deprecated.class);
        assertNotNull(deprecated, "@Deprecated is RUNTIME-retained, so reflection reads it");
        assertEquals("2.0", deprecated.since());
        assertTrue(deprecated.forRemoval());

        Method override = BuiltInsDemo.VipCustomer.class.getDeclaredMethod("setName", String.class);
        assertFalse(override.isAnnotationPresent(Override.class), "@Override is SOURCE-retained — gone at runtime");
        System.out.println("@Deprecated(since=2.0, forRemoval=true) read at runtime; @Override invisible at runtime");
    }
}
