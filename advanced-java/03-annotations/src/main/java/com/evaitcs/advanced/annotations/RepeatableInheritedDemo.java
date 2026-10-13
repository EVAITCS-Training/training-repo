package com.evaitcs.advanced.annotations;

import java.lang.reflect.Method;

/**
 * Demo 4 — TWO EXACT RULES students get wrong: @Repeatable and @Inherited.
 * (README sections "Repeatable" and "Inherited".)
 *
 * @Repeatable: Java has no "list of annotations," so a repeatable note is folded into a
 * CONTAINER by the compiler. You must read it back with getAnnotationsByType, not getAnnotation.
 *
 * @Inherited: works ONLY for class-to-subclass, and ONLY for class-level annotations. It does
 * NOT cross interfaces, and it has NO effect on methods — an overriding method is always bare.
 * Both facts are asserted here and in AnnotationProofTest.
 */
public class RepeatableInheritedDemo {

    @Audit("login")
    @Audit("transfer")
    static class AuditedService {
    }

    @SecureService                       // @Inherited, written ONLY here
    static class BaseService {
        @Logged                          // method annotation — never inherited by an override
        void process() {
        }
    }

    static class ChildService extends BaseService {
        @Override
        void process() {                 // no @Logged here, and none is inherited
        }
    }

    public static void main(String[] args) throws Exception {
        banner("1. @Repeatable — two notes, folded into one container");

        Audit[] audits = AuditedService.class.getAnnotationsByType(Audit.class);
        System.out.println("step 1: the class carries @Audit(\"login\") @Audit(\"transfer\").");
        System.out.println("step 2: getAnnotationsByType(Audit.class).length = " + audits.length);
        for (Audit audit : audits) {
            System.out.println("        - " + audit.value());
        }
        System.out.println("step 3: getAnnotation(Audit.class) = " + AuditedService.class.getAnnotation(Audit.class)
                + "   (null — the two were folded into the container)");
        Audits container = AuditedService.class.getAnnotation(Audits.class);
        System.out.println("step 4: getAnnotation(Audits.class).value().length = " + container.value().length
                + "   (the compiler-generated container)");
        System.out.println("verdict: for a repeatable annotation, ALWAYS read with getAnnotationsByType.");

        banner("2. @Inherited — a class-level note passes to the subclass");

        System.out.println("step 1: @SecureService is written ONLY on BaseService.");
        System.out.println("step 2: BaseService  reports it? "
                + (BaseService.class.getAnnotation(SecureService.class) != null));
        System.out.println("step 3: ChildService (extends BaseService) reports it? "
                + (ChildService.class.getAnnotation(SecureService.class) != null));
        System.out.println("verdict: @Inherited lets a subclass answer 'yes' for its superclass's class-level note.");

        banner("3. Method annotations are NEVER inherited — the exact nuance");

        Method base = BaseService.class.getDeclaredMethod("process");
        Method child = ChildService.class.getDeclaredMethod("process");
        System.out.println("step 1: BaseService.process   has @Logged? " + base.isAnnotationPresent(Logged.class));
        System.out.println("step 2: ChildService.process  has @Logged? " + child.isAnnotationPresent(Logged.class));
        System.out.println("step 3: @Inherited only affects CLASS annotations, never methods — the override is bare.");
        System.out.println("verdict: a framework must walk the type hierarchy ITSELF to find inherited method notes.");

        banner("4. In one sentence");
        System.out.println("@Repeatable needs a container and getAnnotationsByType; @Inherited reaches subclasses");
        System.out.println("for classes only — methods and interfaces are never covered, so readers must walk the tree.");
    }

    static void banner(String title) {
        System.out.println();
        System.out.println("=== " + title + " ===");
    }
}
