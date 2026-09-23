package com.evaitcs.securebank.loan.web;

import com.evaitcs.securebank.loan.domain.Loan;
import com.evaitcs.securebank.loan.security.JwtClaims;
import com.evaitcs.securebank.loan.service.LoanService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
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

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Loan API — role-branched like stage-2's LoanController (customers see
 * theirs, staff see the queue; approve/reject are @PreAuthorize-gated).
 * Method security works because the JWT "role" claim became a ROLE_*
 * authority in SecurityConfig's converter.
 */
@RestController
@RequestMapping("/loans")
public class LoanController {

    public record LoanApplicationRequest(
            @NotNull @DecimalMin("500.00") @Digits(integer = 13, fraction = 2) BigDecimal principal,
            @NotNull @Min(6) @Max(360) Integer termMonths) {
    }

    public record LoanResponse(Long id, BigDecimal principal, BigDecimal annualRate, int termMonths,
                               String status, BigDecimal monthlyPayment, Instant appliedAt) {
        public static LoanResponse from(Loan l) {
            return new LoanResponse(l.getId(), l.getPrincipal(), l.getAnnualRate(), l.getTermMonths(),
                    l.getStatus().name(), l.getMonthlyPayment(), l.getAppliedAt());
        }
    }

    private final LoanService loanService;

    public LoanController(LoanService loanService) {
        this.loanService = loanService;
    }

    @GetMapping
    public List<LoanResponse> list(@AuthenticationPrincipal Jwt jwt) {
        List<Loan> loans = JwtClaims.isStaff(jwt)
                ? loanService.pending()
                : loanService.listForCustomer(JwtClaims.requireCustomerId(jwt));
        return loans.stream().map(LoanResponse::from).toList();
    }

    @PostMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<LoanResponse> apply(@Valid @RequestBody LoanApplicationRequest request,
                                              @AuthenticationPrincipal Jwt jwt) {
        Loan loan = loanService.apply(JwtClaims.requireCustomerId(jwt),
                request.principal(), request.termMonths());
        return ResponseEntity.status(HttpStatus.CREATED).body(LoanResponse.from(loan));
    }

    @GetMapping("/{id}")
    public LoanResponse get(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        return LoanResponse.from(loanService.get(id, JwtClaims.customerId(jwt), JwtClaims.isStaff(jwt)));
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('TELLER', 'ADMIN')")
    public LoanResponse approve(@PathVariable Long id) {
        return LoanResponse.from(loanService.approve(id));
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('TELLER', 'ADMIN')")
    public LoanResponse reject(@PathVariable Long id) {
        return LoanResponse.from(loanService.reject(id));
    }
}
