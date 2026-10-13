package com.evaitcs.evolution.spring;

import org.springframework.aop.support.AopUtils;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.math.BigDecimal;

/**
 * Demo 6 — AOP: THE PROXY UNDER @Transactional, and events on top of it.
 * (README sections "AOP: the same proxy, your advice" and
 * "Events: the container's in-process message bus".)
 *
 * Demo 5 kept saying "the proxy did it". This demo opens the proxy:
 *   banner 1 — the bean you get from the container is NOT the class you wrote: it is a
 *              runtime-generated CGLIB subclass ($$SpringCGLIB$$ in the name) —
 *              advanced-java/02's dynamic proxy idea, industrialized.
 *   banner 2 — YOUR advice (AuditAspect's @Around) rides in that same proxy, recording
 *              before/after around an external call.
 *   banner 3 — the self-invocation trap: an internal this.transfer(...) never passes
 *              through the proxy, so NO advice and NO transaction fire — asserted by counts.
 *   banner 4 — events: the success path publishes a TransferExecutedEvent; the @EventListener
 *              hears it. The refused path publishes nothing.
 *
 * Why a CGLIB subclass and not a java.lang.reflect.Proxy? TransferService implements no
 * interface, so Spring generates a SUBCLASS that overrides each method to run the
 * interceptor chain first (an interface would get the cheaper JDK proxy — Boot defaults
 * to CGLIB everywhere for uniformity).
 */
public class AopAndEventsDemo {

    public static void main(String[] args) {
        String db = DemoDb.freshDatabase();
        try (AnnotationConfigApplicationContext ctx = SpringApp.start(db)) {
            TransferService service = ctx.getBean(TransferService.class);

            banner("1. The bean is a proxy — printed, not asserted");
            System.out.println("step 1: you wrote class TransferService; getBean returned:");
            System.out.println("        " + service.getClass().getName());
            System.out.println("step 2: AopUtils.isAopProxy(bean) = " + AopUtils.isAopProxy(service));
            System.out.println("step 3: class name contains $$SpringCGLIB$$ ? "
                    + service.getClass().getName().contains("$$SpringCGLIB$$"));
            System.out.println("verdict: the container stores the PROXY in the singleton Map and hands it to every");
            System.out.println("         injector — @Transactional's begin/commit and our @Around advice are");
            System.out.println("         interceptors in THIS object (advanced-java/02's proxy, framework-grade).");

            banner("2. The audit trail around an EXTERNAL call: advice fires");
            AuditAspect.clearTrail();
            service.transfer(1, 3, new BigDecimal("25.00"));
            System.out.println("step 1: trail size = " + AuditAspect.TRAIL.size());
            System.out.println("step 2: before fired for transfer(): " + AuditAspect.count("before", "transfer") + " time(s)");
            System.out.println("step 3: after  fired for transfer(): " + AuditAspect.count("after", "transfer") + " time(s)");
            System.out.println("verdict: exactly one advice firing wraps the call — and @Order(0) put it OUTSIDE the");
            System.out.println("         transaction interceptor, so 'after' was recorded only post-COMMIT.");

            banner("3. THE SELF-INVOCATION TRAP: this.transfer(...) bypasses the proxy");
            AuditAspect.clearTrail();
            String reference = service.selfInvokedTransfer(1, 3, new BigDecimal("25.00"));
            System.out.println("step 1: the outer call selfInvokedTransfer() came THROUGH the proxy — it has entries:");
            System.out.println("        before = " + AuditAspect.count("before", "selfInvokedTransfer")
                    + ", after = " + AuditAspect.count("after", "selfInvokedTransfer"));
            System.out.println("step 2: the INNER transfer() call — the annotated one — fired advice "
                    + AuditAspect.count("before", "transfer") + " time(s).");
            System.out.println("step 3: yet the money moved (reference " + reference + "): account 1 = " + DemoDb.balanceOf(db, 1)
                    + " (1500.00 - 25.00 - 25.00 from banner 2).");
            System.out.println("step 4: walkthrough — why the inner call is invisible:");
            System.out.println("        1. the proxy holds a reference to the RAW target object;");
            System.out.println("        2. an external call hits the proxy: interceptors run, then it delegates;");
            System.out.println("        3. inside the target, 'this' IS the raw object — never the proxy;");
            System.out.println("        4. this.transfer(...) is a plain Java method call: no interceptor chain,");
            System.out.println("           so NO audit entry — and NO transaction: each statement auto-committed.");
            System.out.println("verdict: it 'worked' — silently un-audited and un-atomic. The SAME mechanism is the");
            System.out.println("         @Transactional/@Async self-invocation trap in stage-2; the fixes: split the bean,");
            System.out.println("         call through an injected self-reference, or @Bean(selfInjection = true) (Framework 6.2+).");

            banner("4. Events: in-process publish/subscribe, success path only");
            TransferEventListener.clearReceived();
            System.out.println("step 1: a successful transfer publishes TransferExecutedEvent inside the method...");
            service.transfer(2, 3, new BigDecimal("10.00"));
            System.out.println("step 2: listener received " + TransferEventListener.RECEIVED.size() + " event(s)");
            System.out.println("step 3: a REFUSED transfer publishes nothing — the throw precedes the publish:");
            try {
                service.transfer(3, 2, new BigDecimal("99999.00"));
            } catch (TransferService.InsufficientFundsException e) {
                System.out.println("        CAUGHT -> " + e.getClass().getSimpleName() + ": " + e.getMessage());
            }
            System.out.println("step 4: listener total still " + TransferEventListener.RECEIVED.size() + " — no event for work that never happened.");
            System.out.println("verdict: the service states facts; subscribers decide reactions — new listeners never");
            System.out.println("         touch the service. stage-5 grows this exact shape into Kafka events.");
            System.out.println("In one sentence: one proxy mechanism powers transactions, your aspects, and — one layer up —");
            System.out.println("the event bus turns method calls into facts others can subscribe to.");
        }
    }

    static void banner(String title) {
        System.out.println();
        System.out.println("=== " + title + " ===");
    }
}
