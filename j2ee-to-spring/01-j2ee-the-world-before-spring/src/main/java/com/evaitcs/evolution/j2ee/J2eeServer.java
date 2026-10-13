package com.evaitcs.evolution.j2ee;

import org.apache.catalina.Context;
import org.apache.catalina.startup.Tomcat;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The whole J2EE container, as a library: embedded Tomcat 11 on an EPHEMERAL port.
 * (README section "The container, in 40 lines".)
 *
 * In 2001 this was a vendor server you installed, configured through a console, and
 * deployed a WAR into. Today Tomcat is a jar: the code below is the entire "application
 * server" — create, register one servlet, start, and (importantly) STOP + clean up, so
 * demos and tests never leave a server or a port behind.
 *
 * Port 0 means "OS, pick any free port" — two tests can run side by side, and nothing
 * collides with whatever else is on the machine. The actual port is read back after
 * start() via getConnector().getLocalPort().
 *
 * The start(...) method also shows the era's WIRING problem in miniature: the servlet
 * needs a database URL and a DAO, but servlets are instantiated by the container — so we
 * register the DAO in the ServiceLocator and hand the URL to the servlet's constructor.
 * Module 03 replaces both moves with one Spring context.
 */
public final class J2eeServer implements AutoCloseable {

    /**
     * JUL loggers are held WEAKLY by the LogManager — setting a level and dropping the
     * reference lets it be garbage-collected and the level lost. This list keeps ours
     * alive for the life of the JVM (a classic java.util.logging trap, module 07's
     * ReferenceTypesDemo would approve).
     */
    private static final java.util.List<java.util.logging.Logger> HELD_LOGGERS = new java.util.ArrayList<>();

    static {
        // Embedded Tomcat logs through java.util.logging at INFO — in class, that noise
        // drowns the lesson. Everything below SEVERE is silenced for the container's own
        // loggers only (our demo output is System.out and stays untouched).
        for (String name : new String[] { "org.apache.catalina", "org.apache.coyote",
                "org.apache.tomcat", "org.apache.jasper" }) {
            java.util.logging.Logger logger = java.util.logging.Logger.getLogger(name);
            logger.setLevel(java.util.logging.Level.SEVERE);
            logger.setUseParentHandlers(false);
            HELD_LOGGERS.add(logger);
        }
    }

    private final Tomcat tomcat;
    private final File baseDir;
    private final int port;
    private final String dbUrl;

    private J2eeServer(Tomcat tomcat, File baseDir, int port, String dbUrl) {
        this.tomcat = tomcat;
        this.baseDir = baseDir;
        this.port = port;
        this.dbUrl = dbUrl;
    }

    /**
     * Boots Tomcat with a fresh DemoDb behind a TransferServlet.
     *
     * Walkthrough — what happens when you call start():
     *   1. DemoDb.freshDatabase() builds a NEW in-memory H2 world (schema + seed).
     *   2. The DAO for that database is registered in the ServiceLocator under the
     *      string key the servlet will look up ("transferDao") — wiring by hand, move 1.
     *   3. Tomcat is pointed at a temp base dir and port 0; addContext("") creates the
     *      ROOT web application (in 2001: your exploded WAR directory).
     *   4. addServlet registers the servlet INSTANCE (wiring by hand, move 2: we pass the
     *      dbUrl through the constructor because there is no container to inject it).
     *   5. addServletMappingDecoded maps URL -> servlet name (in 2001: <servlet-mapping>
     *      in web.xml — the annotation @WebServlet arrived in Servlet 2.5/3.0).
     *   6. start() binds the connector; getLocalPort() reads back the chosen port.
     */
    public static J2eeServer start() {
        return start(DemoDb.freshDatabase());
    }

    /** Boots Tomcat against a caller-provided database (tests that want to inspect the same DB). */
    public static J2eeServer start(String dbUrl) {
        // Wiring move 1 of 2: the DAO has no container to build it, so WE build it and
        // publish it where the servlet will look for it — the Service Locator.
        ServiceLocator.register("transferDao", new JdbcTransferDao(dbUrl));
        try {
            File baseDir = Files.createTempDirectory("j2ee-tomcat").toFile();
            Tomcat tomcat = new Tomcat();
            tomcat.setBaseDir(baseDir.getAbsolutePath());
            tomcat.setPort(0);                                   // ephemeral: the OS picks
            tomcat.getConnector();                               // forces connector creation
            Context ctx = tomcat.addContext("", baseDir.getAbsolutePath());
            Tomcat.addServlet(ctx, "transfer", new TransferServlet());
            ctx.addServletMappingDecoded("/api/transfer", "transfer");
            Tomcat.addServlet(ctx, "health", new jakarta.servlet.http.HttpServlet() {
                @Override
                protected void doGet(jakarta.servlet.http.HttpServletRequest req,
                                     jakarta.servlet.http.HttpServletResponse resp) throws IOException {
                    resp.setContentType("text/plain;charset=UTF-8");
                    resp.getWriter().println("OK");
                }
            });
            ctx.addServletMappingDecoded("/health", "health");
            tomcat.start();
            int port = tomcat.getConnector().getLocalPort();
            return new J2eeServer(tomcat, baseDir, port, dbUrl);
        } catch (Exception e) {
            throw new IllegalStateException("could not start embedded Tomcat", e);
        }
    }

    public int port() {
        return port;
    }

    /** The database this server's DAO writes to — tests use it to assert the SQL-level truth. */
    public String dbUrl() {
        return dbUrl;
    }

    public String url(String path) {
        return "http://localhost:" + port + path;
    }

    /** One shared HttpClient for demos/tests (HTTP/1.1 over loopback — nothing leaves the machine). */
    private static final HttpClient HTTP = HttpClient.newHttpClient();

    /** An HTTP exchange result: the status code and the body, as text. */
    public record HttpResult(int status, String body) {
        /** True when the body contains every one of the given fragments (order-free). */
        public boolean bodyContains(String... fragments) {
            for (String f : fragments) {
                if (!body.contains(f)) {
                    return false;
                }
            }
            return true;
        }
    }

    /** GET a path; returns status + body. */
    public HttpResult get(String path) {
        try {
            HttpResponse<String> r = HTTP.send(
                    HttpRequest.newBuilder(URI.create(url(path))).GET().build(),
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            return new HttpResult(r.statusCode(), r.body());
        } catch (IOException | InterruptedException e) {
            throw new IllegalStateException("GET " + path + " failed", e);
        }
    }

    /** POST url-encoded form parameters (the era's request format — JSON is module 04). */
    public HttpResult postForm(String path, Map<String, String> form) {
        StringBuilder encoded = new StringBuilder();
        Map<String, String> ordered = new LinkedHashMap<>(form);
        for (Map.Entry<String, String> e : ordered.entrySet()) {
            if (!encoded.isEmpty()) {
                encoded.append('&');
            }
            encoded.append(URLEncoder.encode(e.getKey(), StandardCharsets.UTF_8))
                    .append('=')
                    .append(URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8));
        }
        try {
            HttpResponse<String> r = HTTP.send(
                    HttpRequest.newBuilder(URI.create(url(path)))
                            .header("Content-Type", "application/x-www-form-urlencoded")
                            .POST(HttpRequest.BodyPublishers.ofString(encoded.toString()))
                            .build(),
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            return new HttpResult(r.statusCode(), r.body());
        } catch (IOException | InterruptedException e) {
            throw new IllegalStateException("POST " + path + " failed", e);
        }
    }

    /** Stops Tomcat and deletes its temp base dir. close() so try-with-resources works in demos. */
    @Override
    public void close() {
        try {
            tomcat.stop();
            tomcat.destroy();
        } catch (Exception e) {
            System.out.println("[J2eeServer] stop warning: " + e);
        }
        deleteRecursively(baseDir);
        ServiceLocator.clear();
    }

    private static void deleteRecursively(File f) {
        File[] kids = f.listFiles();
        if (kids != null) {
            for (File k : kids) {
                deleteRecursively(k);
            }
        }
        //noinspection ResultOfMethodCallIgnored — best-effort temp-dir cleanup
        f.delete();
    }
}
