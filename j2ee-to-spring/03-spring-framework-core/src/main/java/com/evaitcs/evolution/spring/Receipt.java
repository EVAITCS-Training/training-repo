package com.evaitcs.evolution.spring;

/**
 * The prototype-scope exhibit: every getBean() mints a NEW one (unique id proves it).
 * (README section "Beans, scopes, lifecycle".)
 *
 * Singleton (the default) = the container is a CACHE: one shared instance per context.
 * Prototype = the container is a FACTORY: a fresh instance per lookup, and — note — the
 * container does NOT track prototypes after handing them out (no @PreDestroy for them;
 * that lifetime-management job is the caller's).
 */
public record Receipt(String id) {
}
