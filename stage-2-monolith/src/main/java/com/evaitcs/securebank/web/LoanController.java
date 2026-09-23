package com.evaitcs.securebank.web;

import com.evaitcs.securebank.domain.Loan;
import com.evaitcs.securebank.service.LoanService;
import com.evaitcs.securebank.web.dto.LoanApplicationRequest;
import com.evaitcs.securebank.web.dto.LoanResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * LOANS — two audiences on one resource:
 *
 *   CUSTOMER: apply for loans, view their own.
 *   TELLER:   see the pending queue, approve/reject.
 *
 * METHOD SECURITY: @PreAuthorize("hasRole('TELLER')") runs as an AOP check
 * around the method — the authority came from the JWT "role" claim via
 * RoleClaimJwtConverter. Result for the wrong role: 403 Forbidden.
 *
 * ROLE-BASED BRANCHING: GET /api/loans returns "my loans" for customers but
 * "the pending queue" for staff. One URL, behavior driven by the verified
 * token — a common real-world pattern for shared dashboards.
 */
@RestController
@RequestMapping("/api/loans")
@Tag(name = "loans", description = "Loan applications and approvals")
public class LoanController {

    private final LoanService loanService;

    public LoanController(LoanService loanService) {
        this.loanService = loanService;
    }

    /** GET /api/loans — customer: mine; staff: pending queue. */
    @GetMapping
    public List<LoanResponse> list(@AuthenticationPrincipal Jwt jwt) {
        List<Loan> loans = JwtClaims.isStaff(jwt)
                ? loanService.pending()
                : loanService.listForCustomer(JwtClaims.requireCustomerId(jwt));
        return loans.stream().map(LoanResponse::from).toList();
    }

    /** POST /api/loans — customer applies. Rate is bank-side (see DTO). */
    @PostMapping
    @PreAuthorize("hasRole('CUSTOMER')") // staff don't take out demo loans
    public ResponseEntity<LoanResponse> apply(@Valid @RequestBody LoanApplicationRequest request,
                                              @AuthenticationPrincipal Jwt jwt) {
        Loan loan = loanService.apply(JwtClaims.requireCustomerId(jwt),
                request.principal(), request.termMonths());
        return ResponseEntity.status(HttpStatus.CREATED).body(LoanResponse.from(loan));
    }

    /** GET /api/loans/{id} — owner or staff (enforced inside the service). */
    @GetMapping("/{id}")
    public LoanResponse get(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        Loan loan = loanService.get(id, JwtClaims.customerId(jwt), JwtClaims.isStaff(jwt));
        return LoanResponse.from(loan);
    }

    /** POST /api/loans/{id}/approve — TELLER/ADMIN only. */
    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('TELLER', 'ADMIN')")
    public LoanResponse approve(@PathVariable Long id) {
        return LoanResponse.from(loanService.approve(id));
    }

    /** POST /api/loans/{id}/reject — TELLER/ADMIN only. */
    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('TELLER', 'ADMIN')")
    public LoanResponse reject(@PathVariable Long id) {
        return LoanResponse.from(loanService.reject(id));
    }
}
