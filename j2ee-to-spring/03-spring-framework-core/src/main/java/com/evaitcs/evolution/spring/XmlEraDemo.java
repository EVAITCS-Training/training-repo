package com.evaitcs.evolution.spring;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Demo 2 — THE WIRING ERA EXHIBIT: the same bean graph, described three ways.
 * (README section "The wiring era, in three languages".)
 *
 * The timeline this demo acts out:
 *   2004  XML        — beans.xml: <bean> elements, wiring by string ids (this demo, live).
 *   2007  ANNOTATIONS — @Component + @Autowired: wiring moves INTO the classes; XML shrinks
 *                       to <context:component-scan>, one line.
 *   2009+ JAVA CONFIG — @Configuration + @Bean (AppConfig): wiring becomes plain Java —
 *                       compiler-checked, refactor-safe, breakpoint-able. The modern idiom.
 *   stage-2 BOOT     — you stop writing most of it: auto-configuration reads the classpath
 *                       and application.properties and PICKS the beans (DataSource,
 *                       transaction manager...) for you. This module wires them by hand
 *                       exactly once so that "picking" is never magic again.
 *
 * Nothing here is a museum piece you may skip: XML contexts still run in production, and
 * every <bean> attribute maps 1:1 onto something in AppConfig. Learn the mapping, not the
 * nostalgia.
 */
public class XmlEraDemo {

    public static void main(String[] args) throws IOException {
        String db = DemoDb.freshDatabase();

        banner("1. 2004: the wiring file — src/main/resources/beans.xml, verbatim");
        System.out.println(readClasspathFile("beans.xml"));
        System.out.println("step 1: two beans (dataSource, transferRepository) + one placeholder resolver.");
        System.out.println("step 2: class names as STRINGS, dependencies as id REFs — the compiler never saw this.");
        System.out.println("step 3: a typo compiles fine and dies at STARTUP — better than the ServiceLocator's");
        System.out.println("        runtime death (module 01), still worse than a compile error.");

        banner("2. Load it: ClassPathXmlApplicationContext — Spring 1.0's front door");
        try (ClassPathXmlApplicationContext xml = SpringApp.startXml(db)) {
            for (String name : new String[] { "dataSource", "transferRepository" }) {
                Object bean = xml.getBean(name);
                System.out.println("step: getBean(\"" + name + "\") -> " + bean.getClass().getName()
                        + " @" + Integer.toHexString(System.identityHashCode(bean)));
            }
            var repo = xml.getBean(JdbcTransferRepository.class);
            System.out.println("step: and it WORKS — repository reads account 1 = " + repo.balanceOf(1));
            System.out.println("verdict: the XML-described graph is real, injected, and usable.");
        }

        banner("3. Today: AnnotationConfigApplicationContext — the same graph as Java (AppConfig)");
        try (AnnotationConfigApplicationContext anno = SpringApp.start(db)) {
            for (String name : new String[] { "dataSource", "transferRepository" }) {
                Object bean = anno.getBean(name);
                System.out.println("step: getBean(\"" + name + "\") -> " + bean.getClass().getName());
            }
            System.out.println("step: same classes, same constructor injection — but a rename in the IDE now");
            System.out.println("      updates the wiring too, and a missing bean fails the COMPILE, not the deploy.");
            System.out.println("verdict: XML and Java config are two spellings of one idea: bean definitions.");
        }

        banner("4. The timeline, and what each era deleted");
        System.out.println("step 1: 2004 XML          — wiring outside the code, strings instead of types.");
        System.out.println("step 2: 2007 @Autowired   — wiring inside the code; XML shrinks to component-scan.");
        System.out.println("step 3: 2009 @Configuration — wiring in plain Java; XML becomes optional, then legacy.");
        System.out.println("step 4: 2014+ Boot        — wiring becomes a DEFAULT: starters + auto-configuration");
        System.out.println("        pick the beans; you keep only the ones that are yours (stage-2, module 04).");
        System.out.println("In one sentence: the eras never changed WHAT wiring is — definitions in a container —");
        System.out.println("only where the definitions live: XML file, annotations, Java, then the framework's defaults.");
    }

    /** Reads a classpath resource as text — the demo prints the REAL file, not a copy that can drift. */
    private static String readClasspathFile(String resource) throws IOException {
        try (InputStream in = XmlEraDemo.class.getClassLoader().getResourceAsStream(resource)) {
            if (in == null) {
                throw new IllegalStateException("classpath resource not found: " + resource);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    static void banner(String title) {
        System.out.println();
        System.out.println("=== " + title + " ===");
    }
}
