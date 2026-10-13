package com.evaitcs.evolution.app.era3;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.net.URI;

/**
 * ERA 3 — the error policy, in ONE place: exceptions in, RFC 7807 ProblemDetail out.
 *
 * This class is what eras 1 and 2 could not express: their servlets decided HTTP
 * status codes INLINE, per method, duplicated per endpoint. Here the whole
 * error-to-HTTP table lives in one @RestControllerAdvice that wraps every controller;
 * a new domain exception = one new method, zero controller changes. It is stage-2's
 * house error style (GlobalExceptionHandler + ProblemDetail, per the repo's AGENTS.md)
 * reached BY HAND — the only difference in stage-2 is that
 * spring.mvc.problemdetails.enabled=true also makes FRAMEWORK errors (bad JSON,
 * method-not-allowed, validation) come back as problem+json by default, instead of
 * through handlers written here.
 *
 * RFC 7807 — "Problem Details for HTTP APIs": a standard JSON shape for errors, so
 * clients parse failures uniformly: `type` (a stable URI identifying the problem kind),
 * `title` (short, human, stable), `status` (the HTTP code, echoed in the body),
 * `detail` (this occurrence's message). Spring's ProblemDetail is that shape as a class.
 *
 * WHY ResponseEntity with an explicit contentType: returning a bare ProblemDetail would
 * let content negotiation pick the media type (usually application/json); pinning
 * APPLICATION_PROBLEM_JSON makes the response deterministic and assertable — the tests
 * check the header, not a hope.
 */
@RestControllerAdvice
public class BankApiAdvice {

    /** Business rule refused: not enough money -> 409 Conflict. The detail carries the account and both amounts. */
    @ExceptionHandler(TransferService.InsufficientFundsException.class)
    public ResponseEntity<ProblemDetail> insufficientFunds(TransferService.InsufficientFundsException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problem.setType(URI.create("https://securebank.dev/problems/insufficient-funds"));
        problem.setTitle("Insufficient funds");
        // Extensions: extra machine-readable fields clients can act on (stage-2 does the same).
        problem.setProperty("accountId", ex.accountId());
        problem.setProperty("attempted", ex.attempted());
        problem.setProperty("available", ex.available());
        return problemJson(HttpStatus.CONFLICT, problem);
    }

    /** Unknown account -> 404, whether it was the GET path or a transfer endpoint's from/to id. */
    @ExceptionHandler(AccountNotFoundException.class)
    public ResponseEntity<ProblemDetail> accountNotFound(AccountNotFoundException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setType(URI.create("https://securebank.dev/problems/account-not-found"));
        problem.setTitle("Account not found");
        problem.setProperty("accountId", ex.accountId());
        return problemJson(HttpStatus.NOT_FOUND, problem);
    }

    /** Malformed business input (negative amount, same-account transfer) -> 400. */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ProblemDetail> badRequest(IllegalArgumentException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problem.setType(URI.create("https://securebank.dev/problems/invalid-request"));
        problem.setTitle("Invalid request");
        return problemJson(HttpStatus.BAD_REQUEST, problem);
    }

    /**
     * The JSON body could not be read — malformed syntax or wrong shape -> 400.
     * The converter wraps Jackson's failure in HttpMessageNotReadableException; we keep
     * the detail GENERIC on purpose: echoing parser internals back to a client is how
     * error responses leak implementation details.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ProblemDetail> unreadableBody(HttpMessageNotReadableException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
                "Request body is not readable as JSON. Expected shape: {\"from\": <id>, \"to\": <id>, \"amount\": <positive decimal>}.");
        problem.setType(URI.create("https://securebank.dev/problems/malformed-json"));
        problem.setTitle("Malformed JSON request");
        return problemJson(HttpStatus.BAD_REQUEST, problem);
    }

    /** A path variable failed conversion (GET /api/v3/accounts/abc -> long) -> 400, not a 500. */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ProblemDetail> typeMismatch(MethodArgumentTypeMismatchException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
                "parameter '" + ex.getName() + "' could not be converted: " + ex.getValue());
        problem.setType(URI.create("https://securebank.dev/problems/invalid-request"));
        problem.setTitle("Invalid request parameter");
        return problemJson(HttpStatus.BAD_REQUEST, problem);
    }

    /** The one place that pins the content type: every problem leaves as application/problem+json. */
    private static ResponseEntity<ProblemDetail> problemJson(HttpStatus status, ProblemDetail problem) {
        return ResponseEntity.status(status)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(problem);
    }
}
