package com.evaitcs.securebank.payment.web;

import com.evaitcs.securebank.payment.client.AccountServiceUnavailableException;
import com.evaitcs.securebank.payment.client.InsufficientFundsException;
import com.evaitcs.securebank.payment.domain.Transfer;
import com.evaitcs.securebank.payment.domain.TransferStatus;
import com.evaitcs.securebank.payment.saga.TransferSagaService;
import com.evaitcs.securebank.payment.security.JwtClaims;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * POST /transfers (publicly: POST /api/transfers via the gateway).
 *
 * The frontend sends the EXACT same request as against the stage-2 monolith —
 * same path after stripping, same body, same 201/400 semantics. Behind that
 * unchanged contract, a local method call became a two-service saga with
 * retries, circuit breaking, compensation, and eventing. THAT is the
 * decomposition promise (and warning): the API holds still; the failure
 * modes multiply.
 */
@RestController
@RequestMapping("/transfers")
public class TransferController {

    public record TransferRequest(
            @NotNull Long fromAccountId,
            @NotNull Long toAccountId,
            @NotNull
            @DecimalMin(value = "0.01", message = "amount must be at least 0.01")
            @Digits(integer = 13, fraction = 2)
            BigDecimal amount) {
    }

    /** Same shape as stage-2's TransferResponse, plus the saga `status` —
     *  callers learn whether the money moved, was reversed, or needs help. */
    public record TransferResponse(String reference, Long fromAccountId, Long toAccountId,
                                   BigDecimal amount, String status, String reason, Instant occurredAt) {
        public static TransferResponse from(Transfer t) {
            return new TransferResponse(t.getReference(), t.getFromAccountId(), t.getToAccountId(),
                    t.getAmount(), t.getStatus().name(), t.getReason(), t.getUpdatedAt());
        }
    }

    private final TransferSagaService saga;

    public TransferController(TransferSagaService saga) {
        this.saga = saga;
    }

    @PostMapping
    public ResponseEntity<TransferResponse> transfer(@Valid @RequestBody TransferRequest request,
                                                     @AuthenticationPrincipal Jwt jwt) {
        // Identity from the verified JWT — never from the body (stage-2 rule).
        Long customerId = JwtClaims.requireCustomerId(jwt);
        Transfer transfer = saga.transfer(request.fromAccountId(), request.toAccountId(),
                request.amount(), customerId);
        return ResponseEntity.status(HttpStatus.CREATED).body(TransferResponse.from(transfer));
    }

    /**
     * Error mapping — the HTTP vocabulary of the saga:
     *   business refusal (insufficient funds)      -> 400 problem+json
     *   unknown account / not yours                -> 404
     *   dependency down (before money moved)       -> 503 (client may retry later)
     *   compensated (debit happened, credit didn't)-> 502-ish: we use 422
     *     "the request was understood, the world refused it, and we cleaned up"
     *   compensation FAILED                        -> 500 + alarm in logs/events
     */
    @RestControllerAdvice
    public static class Handler extends ResponseEntityExceptionHandler {

        @ExceptionHandler(InsufficientFundsException.class)
        ProblemDetail insufficientFunds(InsufficientFundsException ex) {
            ProblemDetail p = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
            p.setTitle("Insufficient funds");
            return p;
        }

        @ExceptionHandler(TransferSagaService.TransferNotFoundException.class)
        ProblemDetail notFound(TransferSagaService.TransferNotFoundException ex) {
            ProblemDetail p = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
            p.setTitle("Resource not found");
            return p;
        }

        @ExceptionHandler(AccountServiceUnavailableException.class)
        ProblemDetail unavailable(AccountServiceUnavailableException ex) {
            ProblemDetail p = ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage());
            p.setTitle("Account service unavailable");
            return p;
        }

        @ExceptionHandler(TransferSagaService.SagaCompensatedException.class)
        ResponseEntity<ProblemDetail> compensated(TransferSagaService.SagaCompensatedException ex) {
            HttpStatus status = ex.getOutcome() == TransferStatus.COMPENSATION_FAILED
                    ? HttpStatus.INTERNAL_SERVER_ERROR       // alarm: money needs human eyes
                    : HttpStatus.UNPROCESSABLE_CONTENT;      // rolled back cleanly
            ProblemDetail p = ProblemDetail.forStatusAndDetail(status, ex.getMessage());
            p.setTitle(ex.getOutcome() == TransferStatus.COMPENSATION_FAILED
                    ? "Transfer failed — compensation FAILED"
                    : "Transfer failed and was rolled back");
            return ResponseEntity.status(status).body(p);
        }

        @ExceptionHandler(IllegalArgumentException.class)
        ProblemDetail badRequest(IllegalArgumentException ex) {
            ProblemDetail p = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
            p.setTitle("Invalid request");
            return p;
        }
    }
}
