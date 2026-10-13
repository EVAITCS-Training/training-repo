package com.evaitcs.advanced.executors;

/**
 * A SecureBank customer — the enrichment stage of the CompletableFuture pipeline
 * attaches one of these to an Account (README "CompletableFuture" section).
 *
 * riskTier feeds riskCheck(): the pipeline's PLANTED failure lives on that stage,
 * so the fallback path (exceptionally) has something honest to catch.
 */
public record Customer(String id, String name, String riskTier) { }
