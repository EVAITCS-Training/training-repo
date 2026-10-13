package com.evaitcs.advanced.jvm;

import java.math.BigDecimal;

/**
 * A SecureBank account — this module's heap citizen: born in Eden (MemoryMapDemo),
 * observed through references of every strength (ReferenceTypesDemo), and immortal
 * while a static registry holds it (LeakHuntDemo).
 *
 * WHY a record: the value IS the data (Java 21 idiom). Duplicated per module on purpose
 * (repo README "Shared conventions") so this module stands alone on screen.
 * Money is BigDecimal, scale 2.
 */
public record Account(String id, String owner, BigDecimal balance) { }
