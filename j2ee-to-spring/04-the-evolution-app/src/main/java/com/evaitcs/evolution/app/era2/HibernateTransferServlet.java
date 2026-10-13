package com.evaitcs.evolution.app.era2;

import com.evaitcs.evolution.app.era1.ServiceLocator;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;

/**
 * ERA 2 — the same servlet silhouette as era 1, mounted at /era2/transfer: form
 * parameters in, text/plain out, status codes decided inline. The ONLY thing that
 * changed between era 1 and era 2 is what sits behind the lookup: JdbcTransferDao
 * (hand-written SQL + row-mapping + transactions) became HibernateTransferDao
 * (entities + a hand-managed Session + transactions).
 *
 * Read that sentence again, because it is the era's verdict: Hibernate killed the SQL
 * and the row-mapping, but the servlet still fetches its dependency from a GLOBAL
 * STRING-KEYED MAP (era 1's ServiceLocator, reused verbatim — wiring was not
 * Hibernate's problem), still parses parameters by hand, still owns the error policy,
 * and the Session/transaction plumbing inside the DAO is still hand-cranked. Those
 * survivors are exactly what era 3's Spring context deletes.
 *
 * In 2006 the SessionFactory reached the servlet through a ServletContextListener
 * parking it in the ServletContext; the ServiceLocator lookup below plays that role
 * (and JNDI's) so the three eras share one visible wiring story.
 */
public class HibernateTransferServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("text/plain;charset=UTF-8");
        PrintWriter out = resp.getWriter();

        String accountIdParam = req.getParameter("accountId");
        long accountId;
        try {
            accountId = Long.parseLong(accountIdParam == null ? "" : accountIdParam.trim());
        } catch (NumberFormatException e) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.println("accountId must be a number, was: " + accountIdParam);
            return;
        }

        HibernateTransferDao dao = ServiceLocator.lookup("hibernateTransferDao");
        try {
            BigDecimal balance = dao.balanceOf(accountId);
            out.println("account " + accountId + " balance = " + balance.toPlainString());
        } catch (IllegalArgumentException e) {
            resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
            out.println(e.getMessage());
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("text/plain;charset=UTF-8");
        PrintWriter out = resp.getWriter();

        // Parsing/validation by hand — identical to era 1: no framework owns this yet.
        Long from = parseLong(req.getParameter("from"));
        Long to = parseLong(req.getParameter("to"));
        BigDecimal amount = parseAmount(req.getParameter("amount"));
        if (from == null || to == null || amount == null) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.println("required form parameters: from, to, amount (positive decimal)");
            return;
        }

        HibernateTransferDao dao = ServiceLocator.lookup("hibernateTransferDao");
        try {
            String reference = dao.transfer(from, to, amount);
            out.println("transfer OK: " + amount.toPlainString() + " from " + from + " to " + to);
            out.println("reference = " + reference);
        } catch (HibernateTransferDao.InsufficientFundsException e) {
            resp.setStatus(HttpServletResponse.SC_CONFLICT);       // 409 — error policy still inline
            out.println("REFUSED: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.println("REFUSED: " + e.getMessage());
        }
    }

    private static Long parseLong(String value) {
        try {
            return value == null ? null : Long.valueOf(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static BigDecimal parseAmount(String value) {
        try {
            BigDecimal amount = value == null ? null : new BigDecimal(value.trim());
            return (amount != null && amount.signum() > 0) ? amount : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
