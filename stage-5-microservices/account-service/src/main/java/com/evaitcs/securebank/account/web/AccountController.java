package com.evaitcs.securebank.account.web;

import com.evaitcs.securebank.account.domain.Account;
import com.evaitcs.securebank.account.domain.Customer;
import com.evaitcs.securebank.account.domain.ResourceNotFoundException;
import com.evaitcs.securebank.account.repository.CustomerRepository;
import com.evaitcs.securebank.account.security.JwtClaims;
import com.evaitcs.securebank.account.service.AccountService;
import com.evaitcs.securebank.account.web.Dtos.AccountResponse;
import com.evaitcs.securebank.account.web.Dtos.CashRequest;
import com.evaitcs.securebank.account.web.Dtos.CustomerResponse;
import com.evaitcs.securebank.account.web.Dtos.OpenAccountRequest;
import com.evaitcs.securebank.account.web.Dtos.PageDto;
import com.evaitcs.securebank.account.web.Dtos.TransactionResponse;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * PUBLIC account API — paths WITHOUT the /api prefix (the gateway strips it:
 * browser /api/accounts -> gateway route Path=/api/accounts/** StripPrefix=1
 * -> here GET /accounts). Every method pulls identity from the verified JWT,
 * never from the body — the rule stage-2 established and the split preserves.
 */
@RestController
public class AccountController {

    private final AccountService accountService;
    private final CustomerRepository customers;

    public AccountController(AccountService accountService, CustomerRepository customers) {
        this.accountService = accountService;
        this.customers = customers;
    }

    @GetMapping("/accounts")
    public List<AccountResponse> myAccounts(@AuthenticationPrincipal Jwt jwt) {
        return accountService.listForCustomer(JwtClaims.requireCustomerId(jwt)).stream()
                .map(AccountResponse::from)
                .toList();
    }

    @GetMapping("/accounts/{id}")
    public AccountResponse account(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        return AccountResponse.from(accountService.getOwnedAccount(id, JwtClaims.requireCustomerId(jwt)));
    }

    @PostMapping("/accounts")
    public ResponseEntity<AccountResponse> open(@Valid @RequestBody OpenAccountRequest request,
                                                @AuthenticationPrincipal Jwt jwt) {
        Account account = accountService.openAccount(JwtClaims.requireCustomerId(jwt), request.type());
        return ResponseEntity.status(HttpStatus.CREATED).body(AccountResponse.from(account));
    }

    @PostMapping("/accounts/{id}/deposit")
    public AccountResponse deposit(@PathVariable Long id,
                                   @Valid @RequestBody CashRequest request,
                                   @AuthenticationPrincipal Jwt jwt) {
        return AccountResponse.from(accountService.deposit(id, JwtClaims.requireCustomerId(jwt),
                request.amount(), request.description()));
    }

    @PostMapping("/accounts/{id}/withdraw")
    public AccountResponse withdraw(@PathVariable Long id,
                                    @Valid @RequestBody CashRequest request,
                                    @AuthenticationPrincipal Jwt jwt) {
        return AccountResponse.from(accountService.withdraw(id, JwtClaims.requireCustomerId(jwt),
                request.amount(), request.description()));
    }

    @GetMapping("/accounts/{id}/transactions")
    public PageDto<TransactionResponse> statement(@PathVariable Long id,
                                                  @PageableDefault(size = 10) Pageable pageable,
                                                  @AuthenticationPrincipal Jwt jwt) {
        accountService.getOwnedAccount(id, JwtClaims.requireCustomerId(jwt)); // gate first
        return PageDto.from(accountService.statement(id, pageable), TransactionResponse::from);
    }

    /** /customers/me — the Accounts context owns customer profiles. */
    @GetMapping("/customers/me")
    public CustomerResponse me(@AuthenticationPrincipal Jwt jwt) {
        Long customerId = JwtClaims.requireCustomerId(jwt);
        Customer customer = customers.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer", customerId));
        return new CustomerResponse(customer.getId(), customer.getName(), customer.getEmail());
    }
}
