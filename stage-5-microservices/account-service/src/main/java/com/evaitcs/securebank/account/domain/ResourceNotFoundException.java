package com.evaitcs.securebank.account.domain;

/** Unknown id OR not-yours — deliberately indistinguishable (HTTP 404). */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String resource, Object id) {
        super("%s with id %s not found".formatted(resource, id));
    }
}
