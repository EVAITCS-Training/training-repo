package com.evaitcs.evolution.app.era3;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import javax.sql.DataSource;

/**
 * ERA 3 — the whole Spring MVC application, hand-wired: NO Boot, no web.xml, no
 * application.yml. This class plus EvolutionServer's five Tomcat lines ARE the
 * deployment. Read the annotations as a shopping list of things stage-2 does for you:
 *
 *   @EnableWebMvc .............. imports WebMvcConfigurationSupport: the DispatcherServlet's
 *                                collaborators (handler mappings, argument resolvers, message
 *                                converters — including the Jackson-3 JSON converter, picked
 *                                up automatically because tools.jackson.databind.ObjectMapper
 *                                is on the classpath; verified empirically by this module's
 *                                JSON round-trip tests, no manual registration needed).
 *                                stage-2: WebMvcAutoConfiguration, switched on by the starter.
 *   @EnableTransactionManagement  the @Transactional proxy infrastructure (module 03's lesson).
 *                                stage-2: TransactionAutoConfiguration.
 *   @ComponentScan ............. finds TransferController / TransferService /
 *                                JdbcTransferRepository / BankApiAdvice by annotation.
 *                                stage-2: @SpringBootApplication's implied scan.
 *   the two @Bean methods ...... DataSource + transaction manager from the db.url property.
 *                                stage-2: DataSourceAutoConfiguration reading
 *                                spring.datasource.url from application.yml (with a real
 *                                HikariCP pool — DriverManagerDataSource here opens a fresh
 *                                connection per call, fine for a deterministic demo).
 *
 * WHERE db.url COMES FROM: EvolutionServer adds a MapPropertySource to this context's
 * Environment BEFORE the DispatcherServlet refreshes it — module 03's SpringApp move,
 * verbatim. @Value resolves against that Environment through the context's default
 * embedded value resolver. That property source is the hand-cranked ancestor of
 * application.yml; Boot fills the same Environment from the file instead.
 */
@Configuration
@EnableWebMvc
@EnableTransactionManagement
@ComponentScan(basePackageClasses = WebAppConfig.class)
public class WebAppConfig {

    /** The property key the DataSource bean reads — the pre-Boot stand-in for spring.datasource.url. */
    public static final String DB_URL_PROPERTY = "db.url";

    @Bean
    DataSource dataSource(@Value("${" + DB_URL_PROPERTY + "}") String url) {
        return new DriverManagerDataSource(url);
    }

    /**
     * The object @Transactional's proxy talks to: owns begin/commit/rollback on the
     * connection bound to the current thread. Era 1's DAO did this by hand inside every
     * method; era 2's DAO did it by hand per Session; here it is one bean.
     */
    @Bean
    DataSourceTransactionManager transactionManager(DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }
}
