package com.evaitcs.evolution.app.era2;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * ERA 2 — the SAME transfer as era 1's JdbcTransferDao, through standalone Hibernate.
 *
 * Put it next to era 1's DAO and read what DIED (this is the module's printed lesson):
 *   - every SQL string .............. session.find()/dirty checking/persist() emit it
 *   - ps.setLong/setBigDecimal ...... the object graph IS the parameter binding
 *   - rs.getBigDecimal row-mapping... the entity comes back populated
 *   - "version = version + 1" ....... @Version on Account — Hibernate writes and guards it
 *   - throws SQLException........... HibernateException is unchecked (JPA's rules)
 *
 * And what SURVIVED — era 3's deletion list, exactly:
 *   - one Session per request, opened and closed by hand (try-with-resources below)
 *   - beginTransaction()/commit()/rollback() by hand, in the right order, on every path
 *   - the entity state must be mutated INSIDE the transaction or nothing is written
 *   - wiring is still a ServiceLocator lookup; errors are still status codes inline
 * stage-2 names for the survivors: OpenSessionInView/@Transactional + the
 * EntityManagerFactory bean — i.e., Spring owns the session and the transaction, and
 * you keep ONLY the business rule (compare era 3's TransferService: four banking lines).
 *
 * Concurrency note, honestly stated: era 1 locked rows pessimistically (SELECT ... FOR
 * UPDATE in id order). This DAO relies instead on Account's @Version column — optimistic
 * locking (module 02's OptimisticLockingDemo): a lost race surfaces at commit as an
 * OptimisticLockException instead of blocking. Both strategies are legitimate; what
 * matters here is that the GUARD moved from hand-written SQL into the mapping.
 */
public class HibernateTransferDao {

    private final SessionFactory sessionFactory;

    /** The heavyweight arrives fully built (HibernateBootstrap/EvolutionServer) — one per application, shared by all threads. */
    public HibernateTransferDao(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    /** Same shape as era 1's exception — the business rule speaks the same language in every era. */
    public static class InsufficientFundsException extends RuntimeException {
        private final long accountId;
        private final BigDecimal attempted;
        private final BigDecimal available;

        public InsufficientFundsException(long accountId, BigDecimal attempted, BigDecimal available) {
            super("account " + accountId + " has " + available + ", cannot send " + attempted);
            this.accountId = accountId;
            this.attempted = attempted;
            this.available = available;
        }

        public long accountId() {
            return accountId;
        }

        public BigDecimal attempted() {
            return attempted;
        }

        public BigDecimal available() {
            return available;
        }
    }

    /**
     * Move `amount` from one account to another, atomically, with the two-row ledger pair.
     *
     * Walkthrough — what happens when you call transfer(2, 3, 50.00):
     *   1. openSession() — the unit of work: cheap, short-lived, NOT thread-safe
     *      (one per request; try-with-resources closes it on every path).
     *   2. beginTransaction() — HAND-CRANKED, exactly like era 1's setAutoCommit(false).
     *   3. session.find(Account.class, id) — Hibernate emits SELECT ... WHERE id=? and
     *      returns a MANAGED entity (populated, snapshot kept for dirty checking).
     *   4. the business rule: enough funds? Mutate with setBalance(). No UPDATE is
     *      written yet — the session has merely NOTICED the change.
     *   5. session.persist(new LedgerEntry(...)) twice — DEBIT + CREDIT sharing one reference.
     *   6. tx.commit() — the FLUSH: Hibernate diffs each managed entity against its
     *      snapshot, emits UPDATE account SET balance=?, version=? WHERE id=? AND version=?
     *      (the @Version guard) and the two ledger INSERTs, then commits the JDBC
     *      transaction. Any RuntimeException before this point -> tx.rollback(), and the
     *      session closes without ever touching the database.
     */
    public String transfer(long fromId, long toId, BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("amount must be positive, was: " + amount);
        }
        if (fromId == toId) {
            throw new IllegalArgumentException("cannot transfer to the same account");
        }
        String reference = UUID.randomUUID().toString();

        try (Session session = sessionFactory.openSession()) {          // (1) still hand-cranked
            Transaction tx = session.beginTransaction();                  // (2) still hand-cranked
            try {
                Account from = session.find(Account.class, fromId);       // (3)
                Account to = session.find(Account.class, toId);           // (3)
                if (from == null || to == null) {
                    throw new IllegalArgumentException("no account " + (from == null ? fromId : toId));
                }

                if (from.getBalance().compareTo(amount) < 0) {            // (4) the business rule
                    throw new InsufficientFundsException(fromId, amount, from.getBalance());
                }
                from.setBalance(from.getBalance().subtract(amount));      // (4) mutation, not SQL
                to.setBalance(to.getBalance().add(amount));               // (4)

                session.persist(new LedgerEntry(from, "DEBIT", amount, "Transfer out", reference));   // (5)
                session.persist(new LedgerEntry(to, "CREDIT", amount, "Transfer in", reference));     // (5)

                tx.commit();                                              // (6) flush + commit
                return reference;
            } catch (RuntimeException e) {
                tx.rollback();                                            // (6) any failure erases everything
                throw e;
            }
        }
    }

    /** Read one account's balance in its own short read-only session — the entity way. */
    public BigDecimal balanceOf(long accountId) {
        try (Session session = sessionFactory.openSession()) {
            Transaction tx = session.beginTransaction();
            try {
                Account account = session.find(Account.class, accountId);
                if (account == null) {
                    throw new IllegalArgumentException("no account " + accountId);
                }
                BigDecimal balance = account.getBalance();
                tx.commit();
                return balance;
            } catch (RuntimeException e) {
                tx.rollback();
                throw e;
            }
        }
    }
}
