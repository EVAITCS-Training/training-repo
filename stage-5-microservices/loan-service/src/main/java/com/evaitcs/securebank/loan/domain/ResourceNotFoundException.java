package com.evaitcs.securebank.loan.domain;

/** Unknown loan id OR not-yours (404 both ways — no existence leak). */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String resource, Object id) {
        super("%s with id %s not found".formatted(resource, id));
    }
}
