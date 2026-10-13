package com.evaitcs.evolution.app;

import com.evaitcs.evolution.app.era3.TransferRequest;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * THE CAPSTONE DEMO — one server, one database, the SAME 250.00 transfer fired at all
 * three eras in sequence, then era 3's error contract as RFC 7807 problem+json, and
 * finally THE WIRING LEDGER: every hand-wired piece in this module next to the Boot
 * auto-configuration that deletes it in stage-2.
 * (README sections "One server, three mounts" and "The table to photograph".)
 *
 * The claim this demo proves: after three identical transfers the cumulative balances
 * moved by exactly 3 x 250.00 — the eras are interchangeable at the BANKING level;
 * everything that differs is plumbing. Run it with:
 *   .\mvnw.cmd -pl 04-the-evolution-app compile exec:java
 */
public class EvolutionServerDemo {

    /**
     * Jackson 3's entry point (tools.jackson, NOT com.fasterxml — the coordinates moved).
     * USE_BIG_DECIMAL_FOR_FLOATS keeps money exact when we re-read bodies for
     * pretty-printing: without it, 999999.00 comes back as a double and prints 999999.0.
     */
    private static final JsonMapper JSON = JsonMapper.builder()
            .enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)
            .build();

    public static void main(String[] args) {
        try (EvolutionServer server = EvolutionServer.start()) {
            banner("1. ONE server, THREE mounts, ONE database");
            System.out.println("step 1: embedded Tomcat started inside this JVM on port " + server.port());
            System.out.println("step 2: mounts — /era1/transfer (servlet+JDBC), /era2/transfer (servlet+Hibernate),");
            System.out.println("                  /api/v3/* (DispatcherServlet + hand-wired Spring MVC)");
            System.out.println("step 3: all three eras share the fresh DemoDb at " + server.dbUrl());
            System.out.println("        -> the SAME account rows are about to be moved by three technology stacks.");

            banner("2. The seeded world (SQL truth via DemoDb, HTTP truth via era 1)");
            printBalances("seeded", server);
            EvolutionServer.HttpResult era1Get = server.get("/era1/transfer?accountId=1");
            System.out.println("GET /era1/transfer?accountId=1 -> " + era1Get.status() + " '" + era1Get.body().strip() + "'");

            banner("3. ERA 1 (2001): raw servlet + ServiceLocator + hand-cranked JDBC");
            Map<String, String> form = new LinkedHashMap<>();
            form.put("from", "1");
            form.put("to", "3");
            form.put("amount", "250.00");
            EvolutionServer.HttpResult r1 = server.postForm("/era1/transfer", form);
            printExchange("POST /era1/transfer  from=1&to=3&amount=250.00  (form-encoded)", r1);
            printBalances("after era 1", server);

            banner("4. ERA 2 (2006): same servlet silhouette, standalone Hibernate behind it");
            EvolutionServer.HttpResult r2 = server.postForm("/era2/transfer", form);
            printExchange("POST /era2/transfer  from=1&to=3&amount=250.00  (form-encoded)", r2);
            printBalances("after era 2", server);
            System.out.println("note: no SQL string exists anywhere in era2/ — Hibernate emitted it from the entity");
            System.out.println("      graph; the Session + transaction, however, were still opened/committed BY HAND.");

            banner("5. ERA 3 (today, no Boot): hand-wired Spring MVC — JSON in, JSON out");
            String requestJson = JSON.writeValueAsString(new TransferRequest(1, 3, new BigDecimal("250.00")));
            EvolutionServer.HttpResult r3 = server.postJson("/api/v3/transfers", requestJson);
            printExchange("POST /api/v3/transfers  " + requestJson, r3);
            printBalances("after era 3", server);
            System.out.println("verdict: three identical 250.00 transfers -> account 1 fell 1500.00 -> 750.00,");
            System.out.println("         account 3 rose 300.00 -> 1050.00. Same banking, three eras, one database.");

            banner("6. ERA 3's error contract: insufficient funds -> 409 application/problem+json");
            String tooMuch = JSON.writeValueAsString(new TransferRequest(3, 1, new BigDecimal("999999.00")));
            EvolutionServer.HttpResult refused = server.postJson("/api/v3/transfers", tooMuch);
            printExchange("POST /api/v3/transfers  " + tooMuch, refused);
            System.out.println("pretty-printed RFC 7807 body:");
            System.out.println(prettyJson(refused.body()));
            System.out.println("era 1 decided this 409 INLINE in its doPost catch block; era 3 decides it ONCE");
            System.out.println("in BankApiAdvice, for every controller — stage-2's GlobalExceptionHandler, by hand.");

            banner("7. ERA 3's error contract: malformed JSON -> 400 application/problem+json");
            EvolutionServer.HttpResult malformed = server.postJson("/api/v3/transfers", "{oops: not json");
            printExchange("POST /api/v3/transfers  {oops: not json", malformed);
            System.out.println("pretty-printed RFC 7807 body:");
            System.out.println(prettyJson(malformed.body()));

            banner("8. THE WIRING LEDGER — every hand-wired piece, and the Boot class that deletes it");
            System.out.println("(every Boot name below was verified against the jars stage-2-monolith actually runs: 4.1.1)");
            System.out.println();
            ledgerRow("pom.xml: 8 separate dependencies",
                    "spring-boot-starter-webmvc (+ -data-jpa): starters pick the jars");
            ledgerRow("EvolutionServer: Tomcat baseDir/port 0/addContext/start",
                    "TomcatServletWebServerAutoConfiguration + ServletWebServerConfiguration; server.port in application.yml");
            ledgerRow("new DispatcherServlet(...) + addServletMappingDecoded(\"/api/v3/*\")",
                    "DispatcherServletAutoConfiguration (registers it, mapped to \"/\")");
            ledgerRow("WebAppConfig: @EnableWebMvc",
                    "WebMvcAutoConfiguration");
            ledgerRow("Jackson-3 converter found via the classpath; JsonMapper built per use",
                    "JacksonAutoConfiguration: one tools.jackson.databind.json.JsonMapper bean");
            ledgerRow("@Bean DataSource + the db.url property source",
                    "DataSourceAutoConfiguration reading spring.datasource.url (pooled: HikariCP)");
            ledgerRow("JdbcTemplate built inside JdbcTransferRepository",
                    "JdbcTemplateAutoConfiguration");
            ledgerRow("@Bean DataSourceTransactionManager + @EnableTransactionManagement",
                    "TransactionAutoConfiguration");
            ledgerRow("era 2: HibernateBootstrap builds the SessionFactory by hand",
                    "HibernateJpaAutoConfiguration via spring-boot-starter-data-jpa (spring.jpa.*)");
            ledgerRow("eras 1-2: ServiceLocator (global string-keyed registry)",
                    "nothing to auto-configure — constructor injection, module 03's lesson");
            ledgerRow("BankApiAdvice handlers for framework errors (malformed JSON...)",
                    "spring.mvc.problemdetails.enabled=true + ResponseEntityExceptionHandler");
            ledgerRow("EvolutionServerDemo: this whole wiring tour",
                    "application.yml as the only configuration");
            System.out.println();
            System.out.println("VERDICT: stage-2 is this module minus every line you just read, plus starters.");
        }
        System.out.println();
        System.out.println("server stopped; port released; Spring context, SessionFactory and temp dir closed;");
        System.out.println("ServiceLocator cleared; in-memory database gone with the JVM.");
    }

    // ---- helpers ---------------------------------------------------------------------

    private static void banner(String title) {
        System.out.println();
        System.out.println("=== " + title + " ===");
    }

    private static void printExchange(String request, EvolutionServer.HttpResult r) {
        System.out.println(request + "  ->  " + r.status() + "  [" + r.contentType() + "]");
        for (String line : r.body().strip().split("\\R")) {
            System.out.println("    " + line);
        }
    }

    /** Balances straight from SQL — the truth all three eras write to, printed between every step. */
    private static void printBalances(String label, EvolutionServer server) {
        String db = server.dbUrl();
        System.out.println("balances (" + label + "): account 1 = " + DemoDb.balanceOf(db, 1)
                + ", account 2 = " + DemoDb.balanceOf(db, 2)
                + ", account 3 = " + DemoDb.balanceOf(db, 3));
    }

    /** Re-serializes a JSON body through Jackson 3's pretty printer — the demo's one cosmetic luxury. */
    private static String prettyJson(String json) {
        return JSON.writerWithDefaultPrettyPrinter().writeValueAsString(JSON.readTree(json));
    }

    private static void ledgerRow(String handWired, String bootDeletes) {
        System.out.println("  hand-wired : " + handWired);
        System.out.println("  Boot 4.1.1 : " + bootDeletes);
        System.out.println();
    }
}
