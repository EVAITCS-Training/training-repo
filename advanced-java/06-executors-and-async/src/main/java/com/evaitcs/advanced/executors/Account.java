package com.evaitcs.advanced.executors;

import java.math.BigDecimal;

/**
 * A SecureBank account — this module's payload for every async stage.
 *
 * WHY a record: the value IS the data, and Java 21 says so in one line (README
 * "Java 21 corner"). Duplicated per module on purpose (repo README "Shared
 * conventions") so this module stands alone on screen. Money is BigDecimal, scale 2.
 */
public record Account(String id, String ownerId, BigDecimal balance) { }
