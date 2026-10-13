package com.evaitcs.advanced.executors;

import java.math.BigDecimal;

/**
 * A SecureBank transaction — the Callable workload of FutureBasicsDemo:
 * "sum these transactions and hand the total back through a Future"
 * (README "Runnable vs Callable" section). Money is BigDecimal, scale 2.
 */
public record Transaction(String id, String description, BigDecimal amount) { }
