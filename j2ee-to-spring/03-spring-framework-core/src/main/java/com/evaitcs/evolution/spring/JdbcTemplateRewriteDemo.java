package com.evaitcs.evolution.spring;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.EmptyResultDataAccessException;

import java.sql.SQLException;

/**
 * Demo 4 — JdbcTemplate DELETES THE DAO PLUMBING, banner by banner.
 * (README section "JdbcTemplate deletes the DAO plumbing".)
 *
 * Banner 1 lines module 01's JdbcTransferDao up against this module's
 * JdbcTransferRepository piece by piece — every deleted piece named with what deleted it.
 * Banner 2 runs the new repository against the seeded database (same numbers module 01
 * printed — identical behavior, that is the point). Banner 3 catches the exception
 * translator red-handed: one duplicate email in, one typed unchecked DuplicateKeyException
 * out, with the vendor SQLState still visible underneath.
 *
 * stage-2 note to say out loud: Spring Data repositories (module 04's teaser) delete even
 * THIS class — but what remains of module 01's DAO after JdbcTemplate is already almost
 * pure schema knowledge.
 */
public class JdbcTemplateRewriteDemo {

    public static void main(String[] args) {
        banner("1. The deletion list — module 01's JdbcTransferDao vs this module's JdbcTransferRepository");
        System.out.println("step 1: getConnection / try / finally / close");
        System.out.println("          module 01: 12+ lines of acquire-and-release ritual in EVERY method");
        System.out.println("          deleted by: JdbcTemplate (+ DataSourceUtils, which also finds the");
        System.out.println("                      thread-bound transactional connection — demo 5)");
        System.out.println("step 2: PreparedStatement + ps.setLong(1, ...) / ps.setBigDecimal(2, ...)");
        System.out.println("          module 01: one setter line per parameter, per statement");
        System.out.println("          deleted by: jdbc.update(sql, args...) — binding by position and type");
        System.out.println("step 3: ResultSet reading + the if (!rs.next()) guard");
        System.out.println("          module 01: rs.getBigDecimal(\"balance\") inside nested try-with-resources");
        System.out.println("          deleted by: queryForObject + a RowMapper LAMBDA — (rs, n) -> rs.getBigDecimal(...)");
        System.out.println("step 4: catch (SQLException e) / rethrow-or-wrap on every call site");
        System.out.println("          module 01: checked exception leaked into every signature (throws SQLException)");
        System.out.println("          deleted by: exception TRANSLATION — banner 3, live");
        System.out.println("step 5: setAutoCommit(false) / commit / rollback / restore");
        System.out.println("          module 01: ~25 lines of hand-cranked transaction inside transfer()");
        System.out.println("          deleted by: @Transactional's proxy — moved OUT of the DAO entirely (demo 5)");
        System.out.println("verdict: ~130 lines -> ~40, identical behavior; what remains is SQL text and the");
        System.out.println("         BigDecimal rule — schema knowledge no framework should ever hide.");

        String db = DemoDb.freshDatabase();
        try (AnnotationConfigApplicationContext ctx = SpringApp.start(db)) {
            JdbcTransferRepository repository = ctx.getBean(JdbcTransferRepository.class);

            banner("2. The rewritten DAO, live: reads the seeded world exactly like module 01 did");
            System.out.println("step 1: account 1 (Alice CHECKING) = " + repository.balanceOf(1) + "   (module 01 printed 1500.00)");
            System.out.println("step 2: account 2 (Alice SAVINGS)  = " + repository.balanceOf(2) + "   (module 01 printed 5200.00)");
            System.out.println("step 3: account 3 (Bob CHECKING)   = " + repository.balanceOf(3) + "    (module 01 printed 300.00)");
            System.out.println("verdict: same data, same BigDecimal(scale 2), zero try/catch/close in the repository.");

            banner("3. Exception translation, caught red-handed: duplicate email -> DuplicateKeyException");
            System.out.println("step 1: the seed world already has alice@securebank.dev (uq_customer_email forbids twins).");
            System.out.println("step 2: inserting an impostor with the same email...");
            try {
                repository.addCustomer("Impostor Alice", "alice@securebank.dev");
            } catch (DuplicateKeyException e) {
                System.out.println("        CAUGHT -> " + e.getClass().getSimpleName() + ": " + firstLine(e.getMessage()));
                System.out.println("        WHY: the unique constraint did its job; Spring named the failure by MEANING.");
                if (e.getCause() instanceof SQLException sql) {
                    System.out.println("step 3: underneath, the vendor exception is still there: SQLState = " + sql.getSQLState()
                            + " (23505 = unique violation, the SQL standard's integrity class 23).");
                }
            }
            System.out.println("step 4: walkthrough — how SQLException became DuplicateKeyException:");
            System.out.println("        1. H2 throws SQLException(SQLState 23505) out of executeUpdate().");
            System.out.println("        2. JdbcTemplate CATCHES it at the boundary — no checked exception escapes.");
            System.out.println("        3. its SQLErrorCodeSQLExceptionTranslator reads SQLState + vendor code.");
            System.out.println("        4. it throws the matching DataAccessException subclass — unchecked, typed.");
            System.out.println("step 5: second translation, same machinery — an account that does not exist:");
            try {
                repository.balanceOf(999);
            } catch (EmptyResultDataAccessException e) {
                System.out.println("        CAUGHT -> " + e.getClass().getSimpleName() + ": " + firstLine(e.getMessage()));
                System.out.println("        WHY: queryForObject demands exactly one row; zero rows gets its own type.");
            }
            System.out.println("verdict: callers catch WHAT HAPPENED (DuplicateKeyException), never WHICH DRIVER");
            System.out.println("         reported it (SQLState 23505) — advanced-java/11's boundary translation, productized.");
            System.out.println("In one sentence: JdbcTemplate keeps your SQL and deletes everything around it —");
            System.out.println("resources, parameters, result mapping, and the checked-exception tax.");
        }
    }

    private static String firstLine(String message) {
        return message == null ? "" : message.lines().findFirst().orElse("");
    }

    static void banner(String title) {
        System.out.println();
        System.out.println("=== " + title + " ===");
    }
}
