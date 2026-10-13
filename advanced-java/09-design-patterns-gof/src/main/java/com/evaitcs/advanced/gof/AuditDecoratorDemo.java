package com.evaitcs.advanced.gof;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Demo 6 — DECORATOR: wrap an object to ADD behavior, same interface, caller-chosen.
 * (README sections "Decorator: IO streams are the canonical chain" and
 * "Decorator vs Proxy: same shape, different intent".)
 *
 * Problem: transfers must be audited, but the audit must not be welded INTO the
 * transfer code (some deployments want it, some don't; some want two layers).
 * Solution: AuditDecorator implements TransferService, holds a TransferService,
 * delegates unchanged, and appends an audit line — the caller decides at construction.
 *
 * You already met the canonical chain in module 08:
 * new DataInputStream(new BufferedInputStream(new FileInputStream(f))) — each layer
 * adds one behavior (buffering, typed reads) and passes the rest through.
 * Collections.unmodifiableList is the same wrap shape adding a RESTRICTION.
 *
 * The contrast with module 02's dynamic audit PROXY is deliberate: same shape
 * (interface + wrapper + delegate), different intent — see banner 3.
 */
public class AuditDecoratorDemo {

    /** The component interface: both the real service and every wrapper speak it. */
    public interface TransferService {
        String transfer(String fromId, String toId, BigDecimal amount);
    }

    /** The real worker: moves money in an in-memory ledger, returns a reference. */
    public static final class BaseTransferService implements TransferService {

        private final Map<String, BigDecimal> balances = new HashMap<>();
        private int counter;

        public BaseTransferService() {
            balances.put("ACC-1", new BigDecimal("500.00"));
            balances.put("ACC-2", new BigDecimal("100.00"));
        }

        @Override
        public String transfer(String fromId, String toId, BigDecimal amount) {
            BigDecimal from = balances.get(fromId);
            if (from == null || from.compareTo(amount) < 0) {
                throw new IllegalStateException("insufficient funds in " + fromId);
            }
            balances.put(fromId, from.subtract(amount));
            balances.put(toId, balances.get(toId).add(amount));
            counter++;
            return "TXN-" + counter;
        }

        public BigDecimal balanceOf(String id) {
            return balances.get(id);
        }
    }

    /**
     * The Decorator: implements the SAME interface, holds a delegate, adds ONE
     * behavior (an audit line), and returns the delegate's result UNCHANGED.
     * The audit log is injected so a test can hand in its own list and read it back.
     */
    public static final class AuditDecorator implements TransferService {

        private final TransferService delegate;
        private final List<String> auditLog;

        public AuditDecorator(TransferService delegate, List<String> auditLog) {
            this.delegate = delegate;
            this.auditLog = auditLog;
        }

        @Override
        public String transfer(String fromId, String toId, BigDecimal amount) {
            String reference = delegate.transfer(fromId, toId, amount);   // the real work, untouched
            auditLog.add("AUDIT " + reference + " " + fromId + " -> " + toId + " " + amount.toPlainString());
            return reference;                                             // ADD, never alter
        }
    }

    public static void main(String[] args) {
        banner("1. The shape: same interface, wrapper around worker");

        System.out.println("TransferService (interface) <- BaseTransferService (does the work)");
        System.out.println("TransferService (interface) <- AuditDecorator (holds one, adds a log line)");
        System.out.println("The caller's variable type never changes: TransferService either way.");

        banner("2. Undecorated vs decorated: same result, plus one audit line");

        BaseTransferService bare = new BaseTransferService();
        String bareResult = bare.transfer("ACC-1", "ACC-2", new BigDecimal("250.00"));
        System.out.println("step 1: bare service      -> reference = " + bareResult);
        System.out.println("step 2: bare balances     -> ACC-1 = " + bare.balanceOf("ACC-1").toPlainString()
                + ", ACC-2 = " + bare.balanceOf("ACC-2").toPlainString());

        List<String> auditLog = new ArrayList<>();
        BaseTransferService worker = new BaseTransferService();
        TransferService audited = new AuditDecorator(worker, auditLog);   // wrapping is the CALLER's choice
        String auditedResult = audited.transfer("ACC-1", "ACC-2", new BigDecimal("250.00"));
        System.out.println("step 3: audited service   -> reference = " + auditedResult);
        System.out.println("step 4: audited balances  -> ACC-1 = " + worker.balanceOf("ACC-1").toPlainString()
                + ", ACC-2 = " + worker.balanceOf("ACC-2").toPlainString());
        System.out.println("step 5: audit log         = " + auditLog);
        System.out.println("verdict: identical reference and balances — the decorator ADDED a log line");
        System.out.println("         and changed NOTHING about the delegated result.");

        banner("3. Decorator vs Proxy (module 02): same shape, different intent");

        System.out.println("shape    : both = interface + wrapper + delegate. Bytecode-wise, twins.");
        System.out.println("DECORATOR: YOU compose the wrap (new AuditDecorator(base, log)) to ADD behavior;");
        System.out.println("           stacking is normal (module 08: DataInputStream(BufferedInputStream(File..))).");
        System.out.println("PROXY    : CONTROLS ACCESS to the target — module 02's dynamic audit proxy stood");
        System.out.println("           IN FRONT of the object and decided what may happen; Spring's");
        System.out.println("           @Transactional wraps your bean to open/commit/rollback around it.");
        System.out.println("verdict: decorator = 'more behavior, my choice'; proxy = 'gatekeeper, its rules'.");

        banner("4. Trap: when NOT to use Decorator");

        System.out.println("A chain five wrappers deep is a debugging maze — every step-through shows");
        System.out.println("another layer. If EVERY deployment needs the same three wrappers, write one");
        System.out.println("class that does all three. Decorate when layers are OPTIONAL or combined per site.");

        banner("5. In one sentence");
        System.out.println("A Decorator is a same-interface wrapper that adds behavior around a result");
        System.out.println("it passes through untouched — and YOU decide, per call site, how deep to stack.");
    }

    static void banner(String title) {
        System.out.println();
        System.out.println("=== " + title + " ===");
    }
}
