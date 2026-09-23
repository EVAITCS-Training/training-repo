package com.evaitcs.securebank.web.dto;

import com.evaitcs.securebank.domain.Customer;

/** The "me" endpoint payload — profile header in the frontend. */
public record CustomerResponse(
        Long id,
        String name,
        String email
) {
    public static CustomerResponse from(Customer c) {
        return new CustomerResponse(c.getId(), c.getName(), c.getEmail());
    }
}
