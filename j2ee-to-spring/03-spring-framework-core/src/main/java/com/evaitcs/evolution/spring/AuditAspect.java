package com.evaitcs.evolution.spring;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.core.annotation.Order;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * YOUR OWN cross-cutting advice — proof that @Transactional is not magic, just the
 * framework's advice running in the same proxy. (README section "AOP: the same proxy,
 * your advice".)
 *
 * The vocabulary, in plain words:
 *   - ASPECT: a class holding cross-cutting behavior (audit) that many beans share.
 *   - POINTCUT: the "where" — the execution expression below selects every public
 *     TransferService method call.
 *   - ADVICE: the "what" — @Around wraps the call: it decides when (or whether!) to run
 *     the real method via joinPoint.proceed().
 *
 * This is advanced-java/02's dynamic proxy wearing a declarative suit: Spring builds a
 * CGLIB subclass of TransferService at bean-creation time and chains interceptors into
 * it — TransactionInterceptor for @Transactional, this advice for @Around. One proxy,
 * both jobs (AopAndEventsDemo banner 1 prints the proxy's class name as evidence).
 *
 * @Order(0) puts the audit OUTSIDE the transaction advisor (whose default order is
 * lowest precedence): "before" is recorded before BEGIN, "after" only after COMMIT —
 * the trail describes the whole transactional call, deterministically.
 *
 * The trail is a static CopyOnWriteArrayList (thread-safe, house determinism rule):
 * demos and tests clear() it, act, then count entries — evidence, not vibes.
 */
@Aspect
@Order(0)
public class AuditAspect {

    /** One advice firing: phase is "before" | "after" | "exception", method the target method's name. */
    public record AdviceEntry(String phase, String method, String detail) {
    }

    public static final List<AdviceEntry> TRAIL = new CopyOnWriteArrayList<>();

    @Around("execution(public * com.evaitcs.evolution.spring.TransferService.*(..))")
    public Object audit(ProceedingJoinPoint joinPoint) throws Throwable {
        String method = joinPoint.getSignature().getName();
        record("before", method, "arguments " + java.util.Arrays.toString(joinPoint.getArgs()));
        try {
            Object result = joinPoint.proceed();          // THE real method runs here — between our lines
            record("after", method, "returned normally");
            return result;
        } catch (Throwable t) {
            record("exception", method, t.getClass().getSimpleName());
            throw t;                                      // never swallow: the transaction rule needs to SEE it
        }
    }

    private static void record(String phase, String method, String detail) {
        TRAIL.add(new AdviceEntry(phase, method, detail));
        System.out.println("  [audit] " + phase + " " + method + "() — " + detail);
    }

    /** How many times one phase fired for one method — the number every assertion in demo 6 points at. */
    public static long count(String phase, String method) {
        return TRAIL.stream()
                .filter(e -> e.phase().equals(phase) && e.method().equals(method))
                .count();
    }

    public static void clearTrail() {
        TRAIL.clear();
    }
}
