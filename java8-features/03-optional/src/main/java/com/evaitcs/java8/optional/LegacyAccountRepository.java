package com.evaitcs.java8.optional;

import java.util.HashMap;
import java.util.Map;

/**
 * The pre-Java-8 repository ("the null world"). Its contract is "returns null if nothing
 * is found" — but notice WHERE that contract is documented: in a Javadoc comment that the
 * compiler never reads and the IDE never enforces.
 *
 * The return type `Account` promises "an account". It does NOT promise "an account or
 * null". A caller who skips the docs — or who forgets at 4:55pm on a Friday — gets an NPE
 * three stack frames away from the actual bug. This class exists so the demos can show
 * that world honestly, then leave it behind.
 */
public class LegacyAccountRepository {

    private final Map<String, Account> byId = new HashMap<>();

    public LegacyAccountRepository() {
        for (Account a : Account.seedAccounts()) {
            byId.put(a.getId(), a);
        }
    }

    /** @return the account, or {@code null} if there is no such account. (Read the doc — or crash.) */
    public Account findById(String id) {
        // HashMap.get already returns null on a miss — pre-Java-8 code let that null
        // escape the API boundary. That leak is the bug; Optional plugs it.
        return byId.get(id);
    }
}
