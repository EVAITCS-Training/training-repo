package com.evaitcs.evolution.j2ee;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Demo 3 — THE WHOLE STACK, LIVE: real HTTP against embedded Tomcat.
 * (README section "The container, in 40 lines".)
 *
 * This is a genuine web server: the OS assigns a port, HttpClient speaks HTTP/1.1 over
 * loopback, and a real servlet container dispatches into TransferServlet. Everything
 * starts and stops inside this main() — nothing lingers, no fixed ports.
 *
 * The verdict line is the module's thesis: count what WE hand-wired to get here
 * (ServiceLocator registration, servlet mapping, parameter parsing, status codes,
 * transaction plumbing) — and compare with stage-2, where a starter and three
 * annotations do all of it.
 */
public class LiveServletDemo {

    public static void main(String[] args) {
        try (J2eeServer server = J2eeServer.start()) {
            banner("1. The container is up — on an OS-assigned port");
            System.out.println("step 1: Tomcat started inside this JVM on port " + server.port());
            System.out.println("step 2: base URL = " + server.url("/"));
            System.out.println("step 3: in 2001, reaching this point meant installing a vendor server");
            System.out.println("        and deploying a WAR; today it is 40 lines and a jar (J2eeServer.java).");

            banner("2. Health check");
            J2eeServer.HttpResult health = server.get("/health");
            System.out.println("GET /health -> " + health.status() + " " + health.body().strip());

            banner("3. GET a balance (servlet -> ServiceLocator -> DAO -> H2)");
            J2eeServer.HttpResult before = server.get("/api/transfer?accountId=3");
            System.out.println("GET /api/transfer?accountId=3 -> " + before.status());
            System.out.println("    " + before.body().strip());

            banner("4. POST a transfer (form-encoded — JSON is module 04's era)");
            Map<String, String> form = new LinkedHashMap<>();
            form.put("from", "1");
            form.put("to", "3");
            form.put("amount", "250.00");
            J2eeServer.HttpResult posted = server.postForm("/api/transfer", form);
            System.out.println("POST /api/transfer from=1&to=3&amount=250.00 -> " + posted.status());
            for (String line : posted.body().strip().split("\\R")) {
                System.out.println("    " + line);
            }

            banner("5. The money moved — verified through a SECOND request");
            J2eeServer.HttpResult after = server.get("/api/transfer?accountId=3");
            System.out.println("GET /api/transfer?accountId=3 -> " + after.body().strip() + "   (was 300.00)");

            banner("6. The business rule still refuses — now as HTTP 409");
            Map<String, String> tooMuch = new LinkedHashMap<>();
            tooMuch.put("from", "3");
            tooMuch.put("to", "1");
            tooMuch.put("amount", "99999.00");
            J2eeServer.HttpResult refused = server.postForm("/api/transfer", tooMuch);
            System.out.println("POST (amount 99999.00) -> " + refused.status());
            System.out.println("    " + refused.body().strip());

            banner("7. Verdict — what we hand-wired to get this far");
            System.out.println("wiring      : ServiceLocator.register(...) — a global map, by string key");
            System.out.println("mapping     : ctx.addServletMappingDecoded(...) — in 2001, web.xml");
            System.out.println("parsing     : req.getParameter + parseLong/parseAmount inside the servlet");
            System.out.println("errors      : try/catch deciding 400/404/409/500 inline, per method");
            System.out.println("transactions: setAutoCommit/commit/rollback/finally-close inside the DAO");
            System.out.println("stage-2 does ALL five with: one starter dependency, @RestController,");
            System.out.println("@Transactional, and a @ControllerAdvice. That deletion is this course.");
        }
        // try-with-resources: server.close() stopped Tomcat and removed the temp dir —
        // the port is already free again before this line finishes printing.
        System.out.println();
        System.out.println("server stopped; port released; in-memory database gone with the JVM.");
    }

    static void banner(String title) {
        System.out.println();
        System.out.println("=== " + title + " ===");
    }
}
