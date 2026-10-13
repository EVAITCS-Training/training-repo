package com.evaitcs.java8.optional;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The Java 8 version of the same repository. Same data, same lookup — but now the
 * *return type itself* documents that absence is possible:
 *
 *   Account           findById(String)  ->  "here is an account"       (lies when there is none)
 *   Optional&lt;Account&gt; findById(String)  ->  "an account, or nothing"   (the type tells the truth)
 *
 * A caller who ignores the empty case gets a NoSuchElementException AT THE .get() — with
 * the message "No value present" — instead of an NPE somewhere downstream with no message
 * at all. Optional does not prevent the mistake; it moves the failure to the exact line
 * where the decision was not made, which is where a bug report can actually use it.
 */
public class AccountRepository {

    private final Map<String, Account> byId = new HashMap<>();
    private final Map<String, List<String>> statementsByAccount = new HashMap<>();

    public AccountRepository() {
        for (Account a : Account.seedAccounts()) {
            byId.put(a.getId(), a);
        }
        statementsByAccount.put("ACC-1001", Arrays.asList("Payroll +2500.00", "Rent -1200.00"));
    }

    /** ofNullable, NOT of: a map miss is a legal outcome here, so null must be tolerated. */
    public Optional<Account> findById(String id) {
        return Optional.ofNullable(byId.get(id));
    }

    /**
     * Chained lookup — a customer to their primary account. Note the flatMap: the
     * customer's account id may be absent (Dana), AND the lookup itself may miss.
     * Two levels of "maybe" collapse into one Optional&lt;Account&gt; — flatMap is what
     * keeps us from ever seeing the useless type Optional&lt;Optional&lt;Account&gt;&gt;.
     */
    public Optional<Account> findPrimaryAccountOf(Customer customer) {
        return Optional.ofNullable(customer.getPrimaryAccountId())
                .flatMap(this::findById);
    }

    /**
     * Design rule #4 in action: NEVER return Optional&lt;List&lt;T&gt;&gt;. A collection is
     * never "missing" — it is just possibly EMPTY — and an empty list needs no unwrapping:
     * the caller can iterate it directly, with zero if-checks and zero ceremony.
     */
    public List<String> recentStatementsFor(String accountId) {
        List<String> rows = statementsByAccount.get(accountId);
        return rows == null ? Collections.<String>emptyList() : rows; // never null, never wrapped
    }
}
