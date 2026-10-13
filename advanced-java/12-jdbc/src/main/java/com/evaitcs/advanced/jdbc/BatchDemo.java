package com.evaitcs.advanced.jdbc;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;

/**
 * Demo 5 — BATCHING: many INSERTs, one conversation. (README section "Batching".)
 *
 * The claim: executing N statements one by one pays the network round-trip N
 * times; addBatch()/executeBatch() ships them as ONE payload and hands back an
 * int[] of per-row update counts.
 *
 * NON-DESTRUCTIVE: both runs (one-by-one and batched) execute inside a
 * transaction against a TEMP table and end in ROLLBACK — the temporary table,
 * its rows, and the transaction all vanish; the seed data is never touched.
 *
 * Timing note: the milliseconds printed here are ILLUSTRATIVE and depend on the
 * machine and Docker setup — this module never asserts timings (course rule).
 */
public class BatchDemo {

    private static final int ROWS = 500;

    public static void main(String[] args) throws SQLException {
        if (!Db.isAvailable()) {
            Db.printStartInstructions();
            return;
        }

        banner("1. Baseline: " + ROWS + " INSERTs, one round-trip each (rolled back)");
        long oneByOneMillis;
        try (Connection c = Db.connect()) {
            c.setAutoCommit(false);
            try {
                createTemp(c);
                long t0 = System.nanoTime();
                try (PreparedStatement ps = c.prepareStatement("INSERT INTO temp_batch (id, note) VALUES (?, ?)")) {
                    for (int i = 1; i <= ROWS; i++) {
                        ps.setLong(1, i);
                        ps.setString(2, "row-" + i);
                        ps.executeUpdate();   // each call = a full request/response over the socket
                    }
                }
                oneByOneMillis = (System.nanoTime() - t0) / 1_000_000;
                System.out.println("step 1: " + ROWS + " x executeUpdate() took ~" + oneByOneMillis + " ms (illustrative, never asserted)");
            } finally {
                c.rollback();   // undo the CREATE TEMP TABLE and every row — PostgreSQL DDL is transactional!
                System.out.println("step 2: rollback() — table and rows gone (a rolled-back CREATE never happened)");
            }
        }

        banner("2. The same " + ROWS + " INSERTs as ONE batch (rolled back)");
        try (Connection c = Db.connect()) {
            c.setAutoCommit(false);
            try {
                createTemp(c);
                long t0 = System.nanoTime();
                int[] counts;
                try (PreparedStatement ps = c.prepareStatement("INSERT INTO temp_batch (id, note) VALUES (?, ?)")) {
                    for (int i = 1; i <= ROWS; i++) {
                        ps.setLong(1, i);
                        ps.setString(2, "row-" + i);
                        ps.addBatch();        // stage the parameters CLIENT-side; nothing is sent yet
                    }
                    counts = ps.executeBatch();   // ONE payload, ONE conversation, N results
                }
                long batchMillis = (System.nanoTime() - t0) / 1_000_000;

                System.out.println("step 1: " + ROWS + " x addBatch() then executeBatch() took ~" + batchMillis + " ms (illustrative)");
                System.out.println("step 2: counts.length      = " + counts.length + "  (one entry per staged statement)");
                System.out.println("step 3: sum of counts      = " + Arrays.stream(counts).sum() + "  (rows actually inserted)");
                System.out.println("step 4: first entries      = " + Arrays.toString(Arrays.copyOf(counts, Math.min(8, counts.length))) + " ...");
                try (Statement st = c.createStatement();
                     ResultSet rs = st.executeQuery("SELECT count(*) FROM temp_batch")) {
                    rs.next();
                    System.out.println("step 5: server-side count  = " + rs.getLong(1) + " (visible inside our open txn)");
                }
                System.out.println("verdict: same rows, far fewer round-trips. The network, not PostgreSQL,");
                System.out.println("         is what one-by-one pays for — batching removes THAT cost.");
            } finally {
                c.rollback();
                System.out.println("step 6: rollback() — seed data untouched, temp world erased");
            }
        }

        banner("3. In one sentence");
        System.out.println("addBatch/executeBatch turns N conversations into one and returns an int[] telling");
        System.out.println("you what each statement did — always inside a transaction, so a failed batch can");
        System.out.println("still be rolled back as a unit.");
    }

    /** The scratch table both runs write into — TEMP (session-local) and inside a rolled-back txn. */
    private static void createTemp(Connection c) throws SQLException {
        try (Statement st = c.createStatement()) {
            st.execute("CREATE TEMP TABLE temp_batch (id BIGINT NOT NULL, note VARCHAR(50) NOT NULL)");
        }
    }

    static void banner(String title) {
        System.out.println();
        System.out.println("=== " + title + " ===");
    }
}
