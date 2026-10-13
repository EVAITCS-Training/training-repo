package com.evaitcs.evolution.app;

import com.evaitcs.evolution.app.era1.JdbcTransferDao;
import com.evaitcs.evolution.app.era1.ServiceLocator;
import com.evaitcs.evolution.app.era1.TransferServlet;
import com.evaitcs.evolution.app.era2.HibernateBootstrap;
import com.evaitcs.evolution.app.era2.HibernateTransferDao;
import com.evaitcs.evolution.app.era2.HibernateTransferServlet;
import com.evaitcs.evolution.app.era3.WebAppConfig;
import org.apache.catalina.Context;
import org.apache.catalina.startup.Tomcat;
import org.hibernate.SessionFactory;
import org.springframework.core.env.MapPropertySource;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.DispatcherServlet;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The whole application server, as a library — generalized from module 01's J2eeServer:
 * ONE embedded Tomcat 11 on an EPHEMERAL port, ONE fresh DemoDb, and THREE mounts, one
 * per era, sharing that single database (README section "One server, three mounts"):
 *
 *   /era1/transfer   raw servlet + ServiceLocator + hand-cranked JDBC   (module 01's stack)
 *   /era2/transfer   raw servlet + ServiceLocator + standalone Hibernate (module 02's ORM)
 *   /api/v3/*        DispatcherServlet + hand-wired Spring MVC + JSON   (module 03's container)
 *
 * Because all three eras read and write the SAME account rows, the demo can move the
 * same 250.00 through all three and watch one cumulative balance — the strongest proof
 * that only the plumbing changed, never the banking.
 *
 * Port 0 means "OS, pick any free port" — nothing collides, nothing lingers: close()
 * stops Tomcat, closes the Spring context and the Hibernate SessionFactory, deletes the
 * temp base dir and clears the ServiceLocator.
 */
public final class EvolutionServer implements AutoCloseable {

    /**
     * JUL loggers are held WEAKLY by the LogManager — setting a level and dropping the
     * reference lets it be garbage-collected and the level lost. This list keeps ours
     * alive for the life of the JVM (module 01's J2eeServer documented the trap; the
     * capstone simply has three frameworks' noise to silence instead of one).
     */
    private static final List<java.util.logging.Logger> HELD_LOGGERS = new ArrayList<>();

    static {
        // Embedded Tomcat logs through java.util.logging at INFO, Hibernate through
        // JBoss Logging (which routes to JUL on this classpath), Spring through
        // spring-jcl (which falls through to JUL when no other backend is present).
        // In class, that startup chatter drowns the lesson — everything below SEVERE
        // is silenced for the frameworks' own loggers only (demo output is System.out).
        for (String name : new String[] { "org.apache.catalina", "org.apache.coyote",
                "org.apache.tomcat", "org.apache.jasper",
                "org.hibernate", "org.jboss.logging",
                "org.springframework" }) {
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
    private final AnnotationConfigWebApplicationContext springContext;
    private final SessionFactory sessionFactory;

    private EvolutionServer(Tomcat tomcat, File baseDir, int port, String dbUrl,
                            AnnotationConfigWebApplicationContext springContext, SessionFactory sessionFactory) {
        this.tomcat = tomcat;
        this.baseDir = baseDir;
        this.port = port;
        this.dbUrl = dbUrl;
        this.springContext = springContext;
        this.sessionFactory = sessionFactory;
    }

    /** Boots the three-era server against a brand-new in-memory database. */
    public static EvolutionServer start() {
        return start(DemoDb.freshDatabase());
    }

    /**
     * Walkthrough — what happens when you call start(dbUrl) (the README's server diagram, live):
     *   1. ERA 1 wiring: a JdbcTransferDao for THIS database goes into the ServiceLocator
     *      under "transferDao" — the global string-keyed registry the servlet will consult.
     *   2. ERA 2 wiring: HibernateBootstrap builds the ONE heavyweight SessionFactory
     *      (module 02's programmatic bootstrap), a HibernateTransferDao wraps it, and the
     *      DAO goes into the SAME ServiceLocator under "hibernateTransferDao" — era 2
     *      fixed mapping, not wiring.
     *   3. Tomcat: temp base dir, port 0 (ephemeral), addContext("") creates the ROOT web
     *      application (in 2001: your exploded WAR directory).
     *   4. era 1 + era 2 servlets are registered as INSTANCES with exact-path mappings —
     *      in 2001 these were <servlet> + <servlet-mapping> entries in web.xml.
     *   5. ERA 3 wiring: an AnnotationConfigWebApplicationContext gets the db.url
     *      property source (module 03's SpringApp move) and WebAppConfig registered —
     *      but is NOT refreshed here. It is handed unrefreshed to new DispatcherServlet(...);
     *      FrameworkServlet.init() then sets the ServletContext on it and refreshes it
     *      during Tomcat startup. That is exactly what Spring's own
     *      AbstractDispatcherServletInitializer does in a web.xml-less WAR — the
     *      proven pattern, reproduced by hand because there is no Boot to do it.
     *   6. The dispatcher is mapped to the PREFIX "/api/v3/*" (see the mapping-choice
     *      comment there), with load-on-startup 1 so the Spring context refresh happens
     *      NOW — a wiring mistake fails the server start, not the first request.
     *   7. tomcat.start() binds the connector; getLocalPort() reads back the chosen port.
     */
    public static EvolutionServer start(String dbUrl) {
        // (1) era 1: wiring by hand, move 1 of 2 — the DAO into the global registry.
        ServiceLocator.register("transferDao", new JdbcTransferDao(dbUrl));

        // (2) era 2: the heavyweight SessionFactory, built once, shared by every request thread.
        SessionFactory sessionFactory = HibernateBootstrap.newFactory(dbUrl);
        ServiceLocator.register("hibernateTransferDao", new HibernateTransferDao(sessionFactory));

        AnnotationConfigWebApplicationContext springContext = null;
        try {
            // (3) the container itself.
            File baseDir = Files.createTempDirectory("evolution-tomcat").toFile();
            Tomcat tomcat = new Tomcat();
            tomcat.setBaseDir(baseDir.getAbsolutePath());
            tomcat.setPort(0);                                   // ephemeral: the OS picks
            tomcat.getConnector();                               // forces connector creation
            // Tomcat gives each webapp its OWN (here: empty) classloader whose default
            // parent is the SYSTEM classloader — and binds it to the thread that starts
            // the context. That is fine for era 1/2 (servlet instances we hand over),
            // but era 3's Spring refresh reads its own annotations THROUGH that loader.
            // Under `mvnw test` the system classloader IS the project classpath; under
            // `mvnw exec:java` it is Maven's — so we point Tomcat's engine at OUR loader.
            // This line is plumbing Boot also hides: TomcatServletWebServerFactory sets
            // the same parent classloader inside ServletWebServerFactoryAutoConfiguration's world.
            tomcat.getEngine().setParentClassLoader(EvolutionServer.class.getClassLoader());
            Context ctx = tomcat.addContext("", baseDir.getAbsolutePath());

            // (4) eras 1 and 2: exact-path servlet mappings (2001: web.xml entries).
            Tomcat.addServlet(ctx, "era1Transfer", new TransferServlet());
            ctx.addServletMappingDecoded("/era1/transfer", "era1Transfer");
            Tomcat.addServlet(ctx, "era2Transfer", new HibernateTransferServlet());
            ctx.addServletMappingDecoded("/era2/transfer", "era2Transfer");

            // (5) era 3: the Spring context, configured but NOT refreshed (step 5 above).
            springContext = new AnnotationConfigWebApplicationContext();
            springContext.getEnvironment().getPropertySources()
                    .addFirst(new MapPropertySource("demoDb", Map.of(WebAppConfig.DB_URL_PROPERTY, dbUrl)));
            springContext.register(WebAppConfig.class);
            DispatcherServlet dispatcher = new DispatcherServlet(springContext);

            // (6) THE MAPPING CHOICE: the dispatcher owns the PREFIX /api/v3/*. Tomcat's
            // mapper routes every /api/v3/** URL to it; per the servlet spec, Spring then
            // matches controller paths WITHIN the mapping (servletPath "/api/v3" is
            // stripped, lookupPath is "/transfers" etc.) — which is why TransferController
            // declares RELATIVE paths. The alternative (map "/" like Boot does, full
            // paths in controllers) works too; the prefix mount was chosen because it
            // makes the era boundary VISIBLE in the container's own routing table:
            // two exact mounts + one prefix mount = three eras, one server.
            var wrapper = Tomcat.addServlet(ctx, "dispatcher", dispatcher);
            wrapper.setLoadOnStartup(1);                         // refresh during start(): fail fast
            ctx.addServletMappingDecoded("/api/v3/*", "dispatcher");

            // (7) go.
            tomcat.start();
            int port = tomcat.getConnector().getLocalPort();
            return new EvolutionServer(tomcat, baseDir, port, dbUrl, springContext, sessionFactory);
        } catch (Exception e) {
            if (springContext != null) {
                springContext.close();
            }
            sessionFactory.close();
            throw new IllegalStateException("could not start the three-era server", e);
        }
    }

    public int port() {
        return port;
    }

    /** The ONE database all three eras share — tests use it to assert the SQL-level truth. */
    public String dbUrl() {
        return dbUrl;
    }

    public String url(String path) {
        return "http://localhost:" + port + path;
    }

    /** The era-3 Spring context, exposed so the demo can show what the container actually built. */
    public AnnotationConfigWebApplicationContext springContext() {
        return springContext;
    }

    /** One shared HttpClient for demos/tests (HTTP/1.1 over loopback — nothing leaves the machine). */
    private static final HttpClient HTTP = HttpClient.newHttpClient();

    /**
     * An HTTP exchange result: status, body, and the Content-Type header.
     * The contentType field is the capstone's addition to module 01's HttpResult: era 3's
     * contract is not just "a JSON body" but "application/json on success vs
     * application/problem+json on failure" — assertions need to see the header.
     */
    public record HttpResult(int status, String body, String contentType) {
        /** True when the body contains every one of the given fragments (order-free). */
        public boolean bodyContains(String... fragments) {
            for (String f : fragments) {
                if (!body.contains(f)) {
                    return false;
                }
            }
            return true;
        }

        /** True when the Content-Type header contains the expected media type (ignoring charset parameters). */
        public boolean contentTypeIs(String expected) {
            return contentType != null && contentType.contains(expected);
        }
    }

    /** GET a path; returns status + body + content type. */
    public HttpResult get(String path) {
        try {
            HttpResponse<String> r = HTTP.send(
                    HttpRequest.newBuilder(URI.create(url(path))).GET().build(),
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            return toResult(r);
        } catch (IOException | InterruptedException e) {
            throw new IllegalStateException("GET " + path + " failed", e);
        }
    }

    /** POST url-encoded form parameters — the request format of eras 1 and 2. */
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
        return post(path, "application/x-www-form-urlencoded", encoded.toString());
    }

    /** POST a raw JSON body — era 3's request format (the eras' API styles, side by side). */
    public HttpResult postJson(String path, String json) {
        return post(path, "application/json", json);
    }

    private HttpResult post(String path, String contentType, String body) {
        try {
            HttpResponse<String> r = HTTP.send(
                    HttpRequest.newBuilder(URI.create(url(path)))
                            .header("Content-Type", contentType)
                            .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                            .build(),
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            return toResult(r);
        } catch (IOException | InterruptedException e) {
            throw new IllegalStateException("POST " + path + " failed", e);
        }
    }

    private static HttpResult toResult(HttpResponse<String> r) {
        return new HttpResult(r.statusCode(), r.body(),
                r.headers().firstValue("Content-Type").orElse(""));
    }

    /**
     * Stops everything, in reverse order of construction: the Spring context (fires
     * every @PreDestroy; the DispatcherServlet's own destroy() would close it too —
     * close() is idempotent), the Hibernate SessionFactory (releases its registry and
     * pool), then Tomcat, then the temp dir, then the ServiceLocator's global state.
     * close() so demos/tests can use try-with-resources and never leave a port behind.
     */
    @Override
    public void close() {
        try {
            springContext.close();
        } catch (Exception e) {
            System.out.println("[EvolutionServer] spring context close warning: " + e);
        }
        try {
            sessionFactory.close();
        } catch (Exception e) {
            System.out.println("[EvolutionServer] session factory close warning: " + e);
        }
        try {
            tomcat.stop();
            tomcat.destroy();
        } catch (Exception e) {
            System.out.println("[EvolutionServer] stop warning: " + e);
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
