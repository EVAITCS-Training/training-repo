package com.evaitcs.evolution.j2ee;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.sql.SQLException;

/**
 * A real servlet, 2001 style: the container calls doGet/doPost, you do EVERYTHING else.
 * (README section "The servlet: 30 lines of framework, 40 lines of you".)
 *
 * What the Servlet API gives you: request parsing (parameters), response writing, and a
 * lifecycle (init/service/destroy). What it does NOT give you:
 *   - dependency injection  -> hence the ServiceLocator lookup below
 *   - transactions          -> hence JdbcTransferDao's hand-cranked commit/rollback
 *   - object mapping        -> hence ResultSet -> BigDecimal by hand
 *   - error handling policy -> hence the try/catch deciding HTTP status codes inline
 * Those four gaps are precisely what modules 02-04 fill — and what stage-2's Boot
 * autoconfiguration fills without being asked.
 *
 * Endpoints (query/form parameters, the era's API style — JSON came later):
 *   GET  /api/transfer?accountId=1        -> text: the account's balance
 *   POST /api/transfer  from=1&to=3&amount=100.00 -> text: the transfer reference
 */
public class TransferServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("text/plain;charset=UTF-8");
        PrintWriter out = resp.getWriter();

        String accountIdParam = req.getParameter("accountId");
        if (accountIdParam == null) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.println("missing required parameter: accountId");
            return;
        }
        long accountId;
        try {
            accountId = Long.parseLong(accountIdParam);   // parsing IS the controller layer here
        } catch (NumberFormatException e) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.println("accountId must be a number, was: " + accountIdParam);
            return;
        }

        // THE ERA'S WIRING: the servlet fetches its own dependency from a global
        // registry by string name. Nobody can see this class needs a DAO without
        // reading the method body. (Spring: constructor injection makes it a signature.)
        JdbcTransferDao dao = ServiceLocator.lookup("transferDao");
        try {
            BigDecimal balance = dao.balanceOf(accountId);
            out.println("account " + accountId + " balance = " + balance.toPlainString());
        } catch (IllegalArgumentException e) {
            resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
            out.println(e.getMessage());
        } catch (SQLException e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.println("database failure: " + e.getMessage());   // in 2001 this often leaked SQL to the browser
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("text/plain;charset=UTF-8");
        PrintWriter out = resp.getWriter();

        // Every parameter: parsed, validated, status-coded — by hand, in the servlet.
        Long from = parseLong(req.getParameter("from"));
        Long to = parseLong(req.getParameter("to"));
        BigDecimal amount = parseAmount(req.getParameter("amount"));
        if (from == null || to == null || amount == null) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.println("required form parameters: from, to, amount (positive decimal)");
            return;
        }

        JdbcTransferDao dao = ServiceLocator.lookup("transferDao");
        try {
            String reference = dao.transfer(from, to, amount);
            out.println("transfer OK: " + amount.toPlainString() + " from " + from + " to " + to);
            out.println("reference = " + reference);
        } catch (JdbcTransferDao.InsufficientFundsException e) {
            // HTTP status as error policy, decided inline — stage-2 moves this to
            // @ControllerAdvice + ProblemDetail (advanced-java/11's boundary translation).
            resp.setStatus(HttpServletResponse.SC_CONFLICT);       // 409
            out.println("REFUSED: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.println("REFUSED: " + e.getMessage());
        } catch (SQLException e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.println("database failure: " + e.getMessage());
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
