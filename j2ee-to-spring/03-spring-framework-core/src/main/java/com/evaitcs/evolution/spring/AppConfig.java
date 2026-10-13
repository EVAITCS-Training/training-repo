package com.evaitcs.evolution.spring;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

import javax.sql.DataSource;

/**
 * The application's wiring, as Java: every bean module 01 hand-cranked, declared once.
 * (README sections "The wiring era, in three languages" and "Beans, scopes, lifecycle".)
 *
 * 2004: this graph lived in beans.xml as <bean> elements (still in this module, for the
 * museum). 2007: @Autowired annotation scanning arrived. Today: @Configuration classes
 * like this one are the idiom — plain Java, type-checked at COMPILE time, refactor-safe.
 * stage-2: Boot builds this same graph from application.properties + auto-configuration;
 * you keep only the beans that are actually YOURS (the service, the repository).
 *
 * Read the @Bean methods top to bottom: each METHOD PARAMETER is a constructor dependency,
 * and the container satisfies it by TYPE from the beans below it. That chain —
 * DataSource -> JdbcTransferRepository -> TransferService — is the whole dependency graph,
 * visible in one screen. Module 01's ServiceLocator hid the same graph inside method
 * bodies as string keys; here it is the API.
 */
@Configuration
// Turns on the transaction proxy: any bean with @Transactional methods gets wrapped by a
// TransactionInterceptor-bearing proxy at creation time. (stage-2: Boot's
// TransactionAutoConfiguration flips this switch for you.)
@EnableTransactionManagement
// Turns on AspectJ-ANNOTATION support: @Aspect beans are found and their advice woven into
// runtime proxies (no ajc compiler, no load-time weaving — Spring's own proxy mechanism).
@EnableAspectJAutoProxy
public class AppConfig {

    /**
     * Where connections come from. The ${db.url} placeholder resolves against the
     * Environment property source SpringApp adds before refresh — the hand-wired ancestor
     * of stage-2's spring.datasource.url. DriverManagerDataSource opens a NEW connection
     * per call (zero pooling): fine for a deterministic demo; production uses HikariCP,
     * which Boot configures from the same property.
     */
    @Bean
    DataSource dataSource(@Value("${" + SpringApp.DB_URL_PROPERTY + "}") String url) {
        return new DriverManagerDataSource(url);
    }

    /**
     * The object @Transactional's proxy talks to: it owns begin/commit/rollback on the
     * connection bound to the current thread. Module 01's DAO did this job by hand
     * (setAutoCommit(false) ... commit ... finally close) inside EVERY method.
     */
    @Bean
    DataSourceTransactionManager transactionManager(DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }

    /** The DAO reborn: same SQL as module 01, a fraction of the code (JdbcTemplateRewriteDemo). */
    @Bean
    JdbcTransferRepository transferRepository(DataSource dataSource) {
        return new JdbcTransferRepository(dataSource);
    }

    /**
     * The business rule. ApplicationEventPublisher is a container-provided dependency —
     * the context itself implements it, so it can be constructor-injected like any bean
     * (TransferExecutedEvent's exit door; demo 6).
     */
    @Bean
    TransferService transferService(JdbcTransferRepository repository, ApplicationEventPublisher publisher) {
        return new TransferService(repository, publisher);
    }

    /** The @Aspect: audit advice around TransferService — proof of what the proxy does (demo 6). */
    @Bean
    AuditAspect auditAspect() {
        return new AuditAspect();
    }

    /** The in-process subscriber: hears every committed-path transfer event (demo 6). */
    @Bean
    TransferEventListener transferEventListener() {
        return new TransferEventListener();
    }

    /** Records @PostConstruct/@PreDestroy into flags the tests assert (BeanLifecycleDemo). */
    @Bean
    LifecycleProbe lifecycleProbe() {
        return new LifecycleProbe();
    }

    /**
     * The SCOPE contrast: "prototype" means a FRESH instance per getBean() — the container
     * is a factory, not a cache, for this one. Default (unstated) scope is "singleton":
     * one shared instance per context, which is what every bean above is.
     */
    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    Receipt receipt() {
        return new Receipt("R-" + java.util.UUID.randomUUID());
    }
}
