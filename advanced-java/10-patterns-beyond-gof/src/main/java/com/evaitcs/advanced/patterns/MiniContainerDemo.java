package com.evaitcs.advanced.patterns;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Constructor;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Demo B2 — DEPENDENCY INJECTION: "Spring in about 60 lines."
 * (README section "Dependency Injection: the centerpiece".)
 *
 * This is THE architectural pattern of the course, and it is built entirely from
 * two modules you already finished:
 *   - module 03 (annotations): the @Wire sticky note marks a class as a managed bean.
 *     Alone it does NOTHING — the container below is the reader that gives it meaning.
 *   - module 02 (reflection): the container reads each bean's constructor, resolves
 *     every parameter type recursively, and calls newInstance — no `new` at call sites.
 *
 * The claim: this ~60-line MiniContainer is exactly what Spring's @Autowired does at
 * stage-2 startup — scan for beans, build each one's dependencies first, hand out ONE
 * instance per container (singleton scope). When you reach Spring, none of it is magic.
 *
 * DETERMINISM: single-threaded; build order is a fixed depth-first walk of the graph.
 */
public class MiniContainerDemo {

    /** The sticky note (module 03). RUNTIME retention so reflection can see it at build time. */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    public @interface Wire { }

    // ---- the bean graph: repository -> service -> controller, plus a cross-cutting audit log ----

    /** A leaf bean: no constructor parameters, so nothing to resolve. */
    @Wire
    public static final class AccountRepository {
        private final Map<String, BigDecimal> balances = new HashMap<>();

        public AccountRepository() {
            balances.put("ACC-1", new BigDecimal("500.00"));
            balances.put("ACC-2", new BigDecimal("100.00"));
        }

        public void move(String fromId, String toId, BigDecimal amount) {
            balances.put(fromId, balances.get(fromId).subtract(amount));
            balances.put(toId, balances.get(toId).add(amount));
        }

        public BigDecimal balanceOf(String id) {
            return balances.get(id);
        }
    }

    /** Another leaf bean — a cross-cutting collaborator both the service and controller can share. */
    @Wire
    public static final class AuditLog {
        private final List<String> lines = new ArrayList<>();

        public void record(String line) {
            lines.add(line);
        }

        public List<String> lines() {
            return List.copyOf(lines);
        }
    }

    /** A middle bean: its constructor DECLARES what it needs; the container supplies it. */
    @Wire
    public static final class TransferService {
        private final AccountRepository repository;
        private final AuditLog auditLog;

        public TransferService(AccountRepository repository, AuditLog auditLog) {
            this.repository = repository;
            this.auditLog = auditLog;
        }

        public AccountRepository repository() {
            return repository;
        }

        public String transfer(String fromId, String toId, BigDecimal amount) {
            repository.move(fromId, toId, amount);
            auditLog.record("service moved " + amount.toPlainString() + " from " + fromId + " to " + toId);
            return "TXN-" + fromId + "-" + toId;
        }
    }

    /** The top bean: depends on the service AND the shared audit log. */
    @Wire
    public static final class TransferController {
        private final TransferService service;
        private final AuditLog auditLog;

        public TransferController(TransferService service, AuditLog auditLog) {
            this.service = service;
            this.auditLog = auditLog;
        }

        public TransferService service() {
            return service;
        }

        public String handleTransfer(String fromId, String toId, BigDecimal amount) {
            auditLog.record("controller received request " + fromId + " -> " + toId);
            return service.transfer(fromId, toId, amount);
        }
    }

    /** Deliberately NOT @Wire — the container must refuse to build it (module 03's lesson). */
    public static final class UnmanagedThing {
        public UnmanagedThing() { }
    }

    // ---- THE CONTAINER: about 60 lines, and it is all reflection + one map ----

    /**
     * A minimal IoC container. "Inversion of Control" means the bean does not build
     * its own dependencies — it declares them, and the container inverts the job of
     * construction. Compare the ~2000-line real thing you will meet as Spring.
     */
    public static final class MiniContainer {

        private final Map<Class<?>, Object> singletons = new HashMap<>();   // ONE instance per type
        private final List<String> buildLog = new ArrayList<>();            // evidence of build order

        /** The "scan result": in Spring this comes from classpath scanning; here it is a fixed list. */
        public MiniContainer(List<Class<?>> scannedBeans) {
            for (Class<?> bean : scannedBeans) {          // pre-instantiate singletons, like Spring startup
                get(bean);
            }
        }

        @SuppressWarnings("unchecked")
        public <T> T get(Class<T> type) {
            Object existing = singletons.get(type);        // step 1: already built? hand back the singleton
            if (existing != null) {
                return (T) existing;
            }
            if (!type.isAnnotationPresent(Wire.class)) {   // step 2: the annotation is the entry ticket
                throw new IllegalArgumentException("not a @Wire bean, container refuses to build: " + type.getSimpleName());
            }
            Constructor<?>[] constructors = type.getDeclaredConstructors();
            if (constructors.length != 1) {                // step 3: one constructor = unambiguous injection
                throw new IllegalStateException(type.getSimpleName() + " must have exactly one constructor");
            }
            Constructor<?> constructor = constructors[0];
            Class<?>[] paramTypes = constructor.getParameterTypes();
            Object[] args = new Object[paramTypes.length];
            for (int i = 0; i < paramTypes.length; i++) {  // step 4: resolve each dependency, depth-first
                args[i] = get(paramTypes[i]);              //         (recursion builds leaves before dependents)
            }
            try {
                constructor.setAccessible(true);           // step 5: reflection constructs it (module 02)
                T instance = (T) constructor.newInstance(args);
                singletons.put(type, instance);            // step 6: cache it — the next get returns THIS one
                buildLog.add("built " + type.getSimpleName()
                        + (paramTypes.length == 0 ? " (leaf)" : " (injected " + paramTypes.length + " deps)"));
                return instance;
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException("cannot build " + type.getSimpleName(), e);
            }
        }

        public List<String> buildLog() {
            return List.copyOf(buildLog);
        }
    }

    public static void main(String[] args) {
        banner("1. The scan list — this is Spring's @ComponentScan, frozen to four classes");

        List<Class<?>> scanned = List.of(
                AccountRepository.class, AuditLog.class, TransferService.class, TransferController.class);
        System.out.println("scanned beans = " + scanned.stream().map(Class::getSimpleName).toList());
        System.out.println("Spring would find these by scanning packages for @Component; we hand it the list.");

        banner("2. new MiniContainer(scanned) builds the whole graph — watch the order");

        MiniContainer container = new MiniContainer(scanned);
        System.out.println("build log (depth-first — leaves before dependents):");
        container.buildLog().forEach(line -> System.out.println("  " + line));
        System.out.println("verdict: the controller is built LAST; the repository and audit log it");
        System.out.println("         transitively needs were built FIRST, by recursion, with no new at any call site.");

        banner("3. Singleton scope: get twice, same instance (==)");

        TransferService a = container.get(TransferService.class);
        TransferService b = container.get(TransferService.class);
        System.out.println("container.get(TransferService) == container.get(TransferService) ? " + (a == b));
        System.out.println("the controller's injected service is the SAME object ? "
                + (container.get(TransferController.class).service() == a));
        System.out.println("verdict: one instance per type per container — the whole graph shares one AuditLog.");

        banner("4. The annotation is the entry ticket (module 03: alone it does nothing)");

        try {
            container.get(UnmanagedThing.class);   // no @Wire -> refused
        } catch (IllegalArgumentException e) {
            System.out.println("CAUGHT -> " + e.getClass().getSimpleName() + ": " + e.getMessage());
            System.out.println("why: @Wire is JUST metadata; this container is the reader that gives it meaning.");
            System.out.println("     Swap @Wire for @Component and the reader for Spring, and it is stage-2 startup.");
        }

        banner("5. A wired graph actually works end to end");

        TransferController controller = container.get(TransferController.class);
        String ref = controller.handleTransfer("ACC-1", "ACC-2", new BigDecimal("250.00"));
        AuditLog shared = container.get(AuditLog.class);
        System.out.println("step 1: controller.handleTransfer returned reference " + ref);
        System.out.println("step 2: ACC-1 = " + container.get(AccountRepository.class).balanceOf("ACC-1").toPlainString()
                + ", ACC-2 = " + container.get(AccountRepository.class).balanceOf("ACC-2").toPlainString());
        System.out.println("step 3: the SHARED audit log recorded both layers: " + shared.lines());
        System.out.println("verdict: controller and service wrote to the SAME AuditLog instance — proof the");
        System.out.println("         container injected one shared singleton, not two independent copies.");

        banner("6. In one sentence");
        System.out.println("Dependency injection is just 'declare your needs, let a reader build them' —");
        System.out.println("annotations say WHAT is a bean, reflection figures out HOW to wire it, one map makes them singletons.");
    }

    static void banner(String title) {
        System.out.println();
        System.out.println("=== " + title + " ===");
    }
}
