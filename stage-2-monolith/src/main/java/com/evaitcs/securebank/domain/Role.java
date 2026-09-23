package com.evaitcs.securebank.domain;

/**
 * Authorization roles (WHO may do WHAT). Spring Security convention: a role
 * named CUSTOMER becomes the authority "ROLE_CUSTOMER", matched by
 * hasRole('CUSTOMER') / @PreAuthorize("hasRole('TELLER')").
 *
 * Role-Based Access Control (RBAC) is the simplest authorization model that
 * real systems use; the Week 6 security lesson contrasts it with
 * attribute/relationship-based approaches.
 */
public enum Role {
    CUSTOMER,  // own accounts: view, deposit, transfer, apply for loans
    TELLER,    // staff: approve/reject loans, assist any customer
    ADMIN      // staff: everything (reserved for future admin endpoints)
}
