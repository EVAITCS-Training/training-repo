package com.evaitcs.securebank.web;

import com.evaitcs.securebank.domain.InsufficientFundsException;
import com.evaitcs.securebank.domain.ResourceNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * CENTRAL ERROR TRANSLATION — exceptions in, RFC 7807 "Problem Details" out.
 *
 * WHY CENTRALIZED (@RestControllerAdvice wraps ALL controllers):
 * services throw DOMAIN exceptions (InsufficientFunds...) and stay HTTP-free;
 * this class owns the one-time mapping to statuses + JSON shape. Add a new
 * domain exception = add one method here, zero controller changes.
 *
 * THE RESPONSE SHAPE (ProblemDetail, application/problem+json):
 *   { "type": "about:blank",
 *     "title": "Insufficient funds",       <- short, stable, machine-readable
 *     "status": 400,
 *     "detail": "Account 1 has 30.00 ...", <- human message
 *     "instance": "/api/transfers" }
 * This is a STANDARD (RFC 7807), so frontend error handling is uniform —
 * Stage 3's api client reads title/detail for every failure the same way.
 *
 * Extending ResponseEntityExceptionHandler + spring.mvc.problemdetails.enabled=true
 * means framework exceptions (bad JSON, method-not-allowed, validation...)
 * ALSO come back as problem+json, not HTML error pages.
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    /** Business rule: not enough money -> 400 (the request itself was fine,
     *  the WORLD refused it; some teams prefer 422 Unprocessable Content). */
    @ExceptionHandler(InsufficientFundsException.class)
    ProblemDetail insufficientFunds(InsufficientFundsException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problem.setTitle("Insufficient funds");
        // Extensions: extra machine-readable fields clients can act on.
        problem.setProperty("accountId", ex.getAccountId());
        problem.setProperty("available", ex.getAvailable());
        problem.setProperty("attempted", ex.getAttempted());
        return problem;
    }

    /** Unknown id OR not-yours (deliberately indistinguishable) -> 404. */
    @ExceptionHandler(ResourceNotFoundException.class)
    ProblemDetail notFound(ResourceNotFoundException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setTitle("Resource not found");
        return problem;
    }

    /** Malformed input caught before validation (e.g. amount <= 0, same-account transfer). */
    @ExceptionHandler(IllegalArgumentException.class)
    ProblemDetail badRequest(IllegalArgumentException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problem.setTitle("Invalid request");
        return problem;
    }

    /** Illegal state transition (approve an already-approved loan) -> 409 Conflict. */
    @ExceptionHandler(IllegalStateException.class)
    ProblemDetail conflict(IllegalStateException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problem.setTitle("State conflict");
        return problem;
    }
}
