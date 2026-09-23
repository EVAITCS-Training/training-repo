package com.evaitcs.securebank.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO LAYER — "web/dto" package. WHY DTOs at all (why not return entities)?
 *
 *  1. SECURITY: entities may hold fields clients must never see
 *     (AppUser.passwordHash!). A DTO is an explicit allow-list.
 *  2. COUPLING: the JSON contract stays stable even when tables/entities
 *     change underneath (renaming a column must not rename an API field).
 *  3. SHAPE: DTOs compose data from several entities into exactly what the
 *     UI needs — no lazy-loading surprises leaking into serialization.
 *
 * RECORDS are the modern Java choice for DTOs: immutable, auto
 * equals/hashCode/toString, and Jackson 3 serializes them natively.
 *
 * VALIDATION: Jakarta Bean Validation annotations on components run at the
 * controller boundary via @Valid — bad input is rejected with a 400 BEFORE
 * any service code executes ("parse, don't validate" at the edge).
 */
public record LoginRequest(
        @NotBlank @Size(min = 3, max = 50) String username,
        @NotBlank @Size(min = 8, max = 100) String password
) {
}
