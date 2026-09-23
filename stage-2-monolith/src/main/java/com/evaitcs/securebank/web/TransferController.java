package com.evaitcs.securebank.web;

import com.evaitcs.securebank.service.TransferService;
import com.evaitcs.securebank.web.dto.TransferRequest;
import com.evaitcs.securebank.web.dto.TransferResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

/**
 * TRANSFERS — the flagship endpoint (atomic money movement).
 *
 * Thin on purpose: validation via @Valid, identity via the JWT, ALL the hard
 * logic (locking order, atomicity, ledger rows) lives in TransferService.
 * A 201 with the correlation reference is returned; the client can poll
 * statements by reference if it needs the ledger view.
 */
@RestController
@RequestMapping("/api/transfers")
@Tag(name = "transfers", description = "Move money between accounts")
public class TransferController {

    private final TransferService transferService;

    public TransferController(TransferService transferService) {
        this.transferService = transferService;
    }

    @PostMapping
    public ResponseEntity<TransferResponse> transfer(@Valid @RequestBody TransferRequest request,
                                                     @AuthenticationPrincipal Jwt jwt) {
        Long customerId = JwtClaims.requireCustomerId(jwt);
        String reference = transferService.transfer(
                request.fromAccountId(), request.toAccountId(), request.amount(), customerId);
        return ResponseEntity.status(HttpStatus.CREATED).body(new TransferResponse(
                reference, request.fromAccountId(), request.toAccountId(),
                request.amount(), Instant.now()));
    }
}
