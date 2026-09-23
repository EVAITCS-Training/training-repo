package com.evaitcs.securebank.web;

import com.evaitcs.securebank.domain.Customer;
import com.evaitcs.securebank.domain.ResourceNotFoundException;
import com.evaitcs.securebank.repository.CustomerRepository;
import com.evaitcs.securebank.web.dto.CustomerResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * CUSTOMER CONTROLLER — profile info for the logged-in user.
 *
 * THE @AuthenticationPrincipal PATTERN (used by every controller below):
 * Spring Security injects the verified Jwt from the Bearer token. We take the
 * "cid" claim — NOT anything from the URL or body — as the identity. This is
 * the single most important security habit in the whole backend: identity
 * comes from the token, resources come from the database, and the two must
 * MATCH before data is returned.
 */
@RestController
@RequestMapping("/api/customers")
@Tag(name = "customers", description = "Customer profile")
public class CustomerController {

    private final CustomerRepository customers;

    public CustomerController(CustomerRepository customers) {
        this.customers = customers;
    }

    /** GET /api/customers/me — the frontend's profile header. */
    @GetMapping("/me")
    public CustomerResponse me(@AuthenticationPrincipal Jwt jwt) {
        Long customerId = JwtClaims.requireCustomerId(jwt);
        Customer customer = customers.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer", customerId));
        return CustomerResponse.from(customer);
    }
}
