package com.evaitcs.securebank.web;

import com.evaitcs.securebank.domain.Account;
import com.evaitcs.securebank.service.AccountService;
import com.evaitcs.securebank.service.StatementService;
import com.evaitcs.securebank.web.dto.AccountResponse;
import com.evaitcs.securebank.web.dto.CashRequest;
import com.evaitcs.securebank.web.dto.OpenAccountRequest;
import com.evaitcs.securebank.web.dto.PageDto;
import com.evaitcs.securebank.web.dto.TransactionResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
 * ACCOUNTS + STATEMENTS — the customer's money endpoints.
 *
 * URL DESIGN (Week 7 REST lesson):
 *   GET  /api/accounts                      collection (mine)
 *   POST /api/accounts                      create one
 *   GET  /api/accounts/{id}                 single resource
 *   POST /api/accounts/{id}/deposit         verb-as-subresource for ACTIONS
 *   GET  /api/accounts/{id}/transactions    sub-collection (statements)
 * Nouns identify resources; HTTP methods carry the action. When an operation
 * isn't CRUD (deposit), a sub-path verb is the pragmatic REST idiom.
 *
 * EVERY method extracts customerId from the JWT and passes it DOWN — the
 * service layer does the ownership enforcement (defense in depth: even if a
 * controller forgot, AccountService.getOwnedAccount still refuses).
 */
@RestController
@RequestMapping("/api/accounts")
@Tag(name = "accounts", description = "Accounts, deposits/withdrawals, statements")
public class AccountController {

    private final AccountService accountService;
    private final StatementService statementService;

    public AccountController(AccountService accountService, StatementService statementService) {
        this.accountService = accountService;
        this.statementService = statementService;
    }

    /** GET /api/accounts — list MY accounts (dashboard data source). */
    @GetMapping
    public List<AccountResponse> myAccounts(@AuthenticationPrincipal Jwt jwt) {
        Long customerId = JwtClaims.requireCustomerId(jwt);
        return accountService.listForCustomer(customerId).stream()
                .map(AccountResponse::from)
                .toList();
    }

    /** GET /api/accounts/{id} — one of MY accounts (404 if it isn't mine). */
    @GetMapping("/{id}")
    public AccountResponse account(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        Account account = accountService.getOwnedAccount(id, JwtClaims.requireCustomerId(jwt));
        return AccountResponse.from(account);
    }

    /** POST /api/accounts — open a new account for ME. 201 + body. */
    @PostMapping
    public ResponseEntity<AccountResponse> open(@Valid @RequestBody OpenAccountRequest request,
                                                @AuthenticationPrincipal Jwt jwt) {
        Account account = accountService.openAccount(JwtClaims.requireCustomerId(jwt), request.type());
        return ResponseEntity.status(HttpStatus.CREATED).body(AccountResponse.from(account));
    }

    /** POST /api/accounts/{id}/deposit — cash in. */
    @PostMapping("/{id}/deposit")
    public AccountResponse deposit(@PathVariable Long id,
                                   @Valid @RequestBody CashRequest request,
                                   @AuthenticationPrincipal Jwt jwt) {
        Account account = accountService.deposit(id, JwtClaims.requireCustomerId(jwt),
                request.amount(), request.description());
        return AccountResponse.from(account);
    }

    /** POST /api/accounts/{id}/withdraw — cash out (400 if insufficient funds). */
    @PostMapping("/{id}/withdraw")
    public AccountResponse withdraw(@PathVariable Long id,
                                    @Valid @RequestBody CashRequest request,
                                    @AuthenticationPrincipal Jwt jwt) {
        Account account = accountService.withdraw(id, JwtClaims.requireCustomerId(jwt),
                request.amount(), request.description());
        return AccountResponse.from(account);
    }

    /**
     * GET /api/accounts/{id}/transactions?page=0&size=10 — the statement.
     *
     * Pageable is resolved automatically from query params by Spring Data's
     * web support; @PageableDefault sets sane limits when the client omits
     * them. Ownership is verified BEFORE querying, so an attacker can't even
     * page through someone else's history.
     */
    @GetMapping("/{id}/transactions")
    public PageDto<TransactionResponse> statement(@PathVariable Long id,
                                                  @PageableDefault(size = 10) Pageable pageable,
                                                  @AuthenticationPrincipal Jwt jwt) {
        accountService.getOwnedAccount(id, JwtClaims.requireCustomerId(jwt)); // authorization gate
        return PageDto.from(statementService.accountStatement(id, pageable), TransactionResponse::from);
    }
}
