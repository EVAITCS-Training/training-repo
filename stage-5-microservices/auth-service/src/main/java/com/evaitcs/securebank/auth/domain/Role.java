package com.evaitcs.securebank.auth.domain;

/**
 * Authorization roles. DELIBERATELY DUPLICATED in each service that checks
 * roles (loan-service, account-service...). The alternative — a shared
 * "common" jar — creates a deploy coupling: changing the enum rebuilds and
 * redeploys EVERY consumer, which quietly recreates the monolith. Small,
 * stable value types are copied in real microservice estates; shared
 * libraries are reserved for things that MUST stay in lockstep.
 * (This exact trade-off is discussed in the stage README.)
 */
public enum Role {
    CUSTOMER,
    TELLER,
    ADMIN
}
