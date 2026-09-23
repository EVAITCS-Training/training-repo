package com.evaitcs.securebank.domain;

/**
 * Thrown when a lookup by id finds nothing. Mapped to HTTP 404 by
 * web/GlobalExceptionHandler.
 *
 * WHY NOT return null? A null return forces every caller to remember the
 * check; an exception makes "not found" impossible to ignore. Fail fast,
 * fail loudly — then translate to a clean HTTP status at the edge.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String resource, Object id) {
        super("%s with id %s not found".formatted(resource, id));
    }
}
