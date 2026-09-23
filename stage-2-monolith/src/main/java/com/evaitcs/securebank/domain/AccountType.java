package com.evaitcs.securebank.domain;

/**
 * Account types offered by SecureBank. Stored as VARCHAR thanks to
 * @Enumerated(EnumType.STRING) on Account.type.
 *
 * Enums > "magic strings": the compiler rejects typos, switch statements can
 * be exhaustive, and refactoring tools rename every usage at once.
 */
public enum AccountType {
    CHECKING,
    SAVINGS
}
