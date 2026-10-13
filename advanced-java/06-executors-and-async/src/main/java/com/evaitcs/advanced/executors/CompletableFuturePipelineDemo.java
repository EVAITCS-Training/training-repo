package com.evaitcs.advanced.executors;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;

/**
 * Demo 3 — THE SECUREBANK REPORT PIPELINE, chained instead of blocked.
 * (README section "CompletableFuture: chaining instead of blocking".)
 *
 * The claim: loadAccount -> enrichWithCustomer -> riskCheck -> renderReport runs as ONE
 * chain of stages; every stage prints the thread that executed it (default executor =
 * ForkJoinPool.commonPool, named in the output); a PLANTED failure on one account is
 * turned into an exact fallback report by exceptionally(); thenCombine merges two
 * independent branches; allOf/anyOf coordinate many futures.
 *
 * buildReport / buildReportSafely are package-visible so ExecutorsAsyncTest can join
 * them and assert the EXACT final strings.
 */
public class CompletableFuturePipelineDemo {

    /** The account whose pipeline succeeds — its report string is asserted in the test. */
    static final String OK_ACCOUNT = "A-101";
    /** The account whose riskCheck is PLANTED to fail — proves the exceptionally() fallback. */
    static final String DOOMED_ACCOUNT = "A-666";

    private static final Map<String, Account> ACCOUNTS = Map.of(
            OK_ACCOUNT, new Account(OK_ACCOUNT, "C-1", new BigDecimal("1500.00")),
            "A-202", new Account("A-202", "C-2", new BigDecimal("250.75")),
            DOOMED_ACCOUNT, new Account(DOOMED_ACCOUNT, "C-3", new BigDecimal("999999.99")));

    private static final Map<String, Customer> CUSTOMERS = Map.of(
            "C-1", new Customer("C-1", "Dana", "LOW"),
            "C-2", new Customer("C-2", "Ana", "MEDIUM"),
            "C-3", new Customer("C-3", "Vlad", "HIGH"));

    /** One dossier + one verdict combine into the final report string. */
    record Dossier(Account account, Customer customer) { }
    record RiskVerdict(String level, String decision) { }

    public static void main(String[] args) throws Exception {
        banner("1. supplyAsync runs on the common ForkJoinPool — named, not assumed");

        System.out.println("step 1: ForkJoinPool.getCommonPoolParallelism() = " + ForkJoinPool.getCommonPoolParallelism()
                + "  (one shared pool for the WHOLE JVM)");
        String where = CompletableFuture.supplyAsync(() -> Thread.currentThread().getName()).join();
        System.out.println("step 2: supplyAsync ran on: " + where);
        System.out.println("verdict: no executor argument = ForkJoinPool.commonPool(). The rule: DON'T BLOCK IT —");
        System.out.println("         a JDBC call parked here steals a worker from every parallel stream in the JVM.");
        System.out.println("         Blocking work passes its OWN executor as supplyAsync's second argument (banner 5).");

        banner("2. The pipeline: loadAccount -> enrichWithCustomer -> riskCheck -> renderReport");

        String report = buildReport(OK_ACCOUNT).join();   // join = get() without checked exceptions
        System.out.println("step 1: each stage above printed the thread that ran it — whoever COMPLETES a stage");
        System.out.println("        usually runs the next one inline, and if a stage was already complete when the");
        System.out.println("        next was REGISTERED, the registering thread runs it — that is the 'main' line.");
        System.out.println("step 2: report = " + report);

        banner("3. thenApply vs thenCompose: pipeline vs nesting (trap 4, live)");

        CompletableFuture<CompletableFuture<Customer>> nested =
                CompletableFuture.supplyAsync(() -> loadAccount(OK_ACCOUNT))
                        .thenApply(a -> CompletableFuture.supplyAsync(() -> enrichWithCustomer(a)));
        CompletableFuture<Customer> flat =
                CompletableFuture.supplyAsync(() -> loadAccount(OK_ACCOUNT))
                        .thenCompose(a -> CompletableFuture.supplyAsync(() -> enrichWithCustomer(a)));
        System.out.println("step 1: thenApply wrapped a future IN a future — join() hands back " + nested.join().getClass().getSimpleName());
        System.out.println("step 2: the OUTER future is 'done' while the INNER work may still run — a silent lie");
        System.out.println("step 3: thenCompose flattened it: " + flat.join());
        System.out.println("verdict: thenApply = map, thenCompose = flatMap. If the stage returns a future, COMPOSE.");

        banner("4. The PLANTED failure: exceptionally() turns a crash into an exact fallback");

        String fallback = buildReportSafely(DOOMED_ACCOUNT).join();
        System.out.println("step 1: riskCheck threw IllegalStateException(\"risk service unreachable\") for " + DOOMED_ACCOUNT);
        System.out.println("step 2: no stage after the failure ran — the exception skipped straight down the chain");
        System.out.println("step 3: exceptionally() caught it; fallback = " + fallback);
        CompletableFuture<String> handled = buildReport(DOOMED_ACCOUNT)
                .handle((value, failure) -> failure == null
                        ? value
                        : "handled on " + Thread.currentThread().getName() + ": " + rootCause(failure).getMessage());
        System.out.println("step 4: handle() saw BOTH slots (value=null, failure set): " + handled.join());
        System.out.println("verdict: an unhandled CompletableFuture fails SILENTLY — attach exceptionally/handle,");
        System.out.println("         or catch CompletionException at join(). The failure never reaches stderr by itself.");

        banner("5. thenCombine merges branches — blocking work gets its OWN executor");

        // thenCombine waits for TWO independent futures, then merges both values.
        // The credit-line lookup "blocks" (simulated), so the RULE from banner 1 applies:
        // it runs on a dedicated executor, NOT on the common pool.
        ExecutorService ioLane = Executors.newVirtualThreadPerTaskExecutor();   // Java 21: blocking is cheap here
        try {
            CompletableFuture<BigDecimal> balance =
                    CompletableFuture.supplyAsync(() -> loadAccount(OK_ACCOUNT).balance());
            CompletableFuture<BigDecimal> creditLine = CompletableFuture.supplyAsync(() -> {
                sleep(200);                                   // stands in for a slow remote call
                return new BigDecimal("300.25");
            }, ioLane);                                       // <- the rule, applied: own executor
            BigDecimal exposure = balance.thenCombine(creditLine, BigDecimal::add).join();
            System.out.println("step 1: balance stage ran on the common pool; credit-line stage ran on the ioLane");
            System.out.println("step 2: exposure = balance + creditLine = " + exposure.toPlainString());
        } finally {
            ioLane.shutdown();
            ioLane.awaitTermination(5, TimeUnit.SECONDS);
        }

        banner("6. allOf waits for EVERYBODY; anyOf takes the FIRST");

        List<CompletableFuture<Account>> loads = List.of(OK_ACCOUNT, "A-202", DOOMED_ACCOUNT).stream()
                .map(id -> CompletableFuture.supplyAsync(() -> loadAccount(id)))
                .toList();
        CompletableFuture.allOf(loads.toArray(new CompletableFuture[0])).join();
        BigDecimal combined = loads.stream()
                .map(cf -> cf.join().balance())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        System.out.println("step 1: allOf joined only after ALL THREE loads finished; combined balance = " + combined.toPlainString());

        ExecutorService raceLane = Executors.newVirtualThreadPerTaskExecutor();
        try {
            CompletableFuture<String> serviceA = CompletableFuture.supplyAsync(() -> "quote from service A");
            CompletableFuture<String> serviceB = CompletableFuture.supplyAsync(() -> {
                sleep(300);
                return "quote from service B";
            }, raceLane);
            Object winner = CompletableFuture.anyOf(serviceA, serviceB).join();
            System.out.println("step 2: anyOf took the first to finish: " + winner + "  (WHICH one wins is a race — never assert it)");
        } finally {
            raceLane.shutdown();
            raceLane.awaitTermination(5, TimeUnit.SECONDS);
        }

        banner("7. In one sentence");
        System.out.println("CompletableFuture turns 'wait for the result' into 'register the next stage' —");
        System.out.println("the thread that FINISHES a stage usually runs the next one, so nobody parks.");
    }

    // ------------------------------------------------------------------
    // The pipeline itself — package-visible so the test can assert EXACT strings.
    // ------------------------------------------------------------------

    /** The four-stage pipeline. Completes exceptionally for DOOMED_ACCOUNT (planted failure). */
    static CompletableFuture<String> buildReport(String accountId) {
        CompletableFuture<Account> account =
                CompletableFuture.supplyAsync(() -> loadAccount(accountId));           // IO-shaped: common pool OK for a map lookup
        CompletableFuture<Dossier> dossier =
                account.thenApply(a -> new Dossier(a, enrichWithCustomer(a)));         // pure transform: thenApply
        CompletableFuture<RiskVerdict> verdict =
                dossier.thenCompose(d -> CompletableFuture.supplyAsync(() -> riskCheck(d)));  // async service call: thenCompose (banner 3)
        return verdict.thenCombine(dossier, (v, d) -> renderReport(d, v));             // merge both branches
    }

    /** Same pipeline, with the exceptionally() fallback attached — the production-shaped version. */
    static CompletableFuture<String> buildReportSafely(String accountId) {
        return buildReport(accountId).exceptionally(failure -> {
            Throwable cause = rootCause(failure);
            System.out.println("  fallback ran on " + Thread.currentThread().getName()
                    + " after " + cause.getClass().getSimpleName() + ": " + cause.getMessage());
            return "REPORT " + accountId + " | MANUAL REVIEW | " + cause.getMessage();
        });
    }

    static Account loadAccount(String accountId) {
        stage("loadAccount");
        Account account = ACCOUNTS.get(accountId);
        if (account == null) {
            throw new IllegalArgumentException("unknown account " + accountId);
        }
        return account;
    }

    static Customer enrichWithCustomer(Account account) {
        stage("enrichWithCustomer");
        return CUSTOMERS.get(account.ownerId());
    }

    static RiskVerdict riskCheck(Dossier dossier) {
        stage("riskCheck");
        if (dossier.account().id().equals(DOOMED_ACCOUNT)) {
            // The PLANTED failure (README "CompletableFuture" section): one account always
            // crashes here, so exceptionally()'s fallback is provable on every single run.
            throw new IllegalStateException("risk service unreachable");
        }
        return new RiskVerdict(dossier.customer().riskTier(), "APPROVED");
    }

    static String renderReport(Dossier dossier, RiskVerdict verdict) {
        stage("renderReport");
        return "REPORT " + dossier.account().id()
                + " | " + dossier.customer().name()
                + " | balance " + dossier.account().balance().toPlainString()
                + " | risk " + verdict.level()
                + " | " + verdict.decision();
    }

    /** Every stage announces its executing thread — the whole point of banner 2. */
    private static void stage(String name) {
        System.out.println("  stage " + name + " ran on " + Thread.currentThread().getName());
    }

    /** join() wraps failures in CompletionException; unwrap to the business-level cause. */
    private static Throwable rootCause(Throwable failure) {
        return failure instanceof CompletionException && failure.getCause() != null
                ? failure.getCause()
                : failure;
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    static void banner(String title) {
        System.out.println();
        System.out.println("=== " + title + " ===");
    }
}
