package com.evaitcs.java8.optional;

import java.util.Arrays;
import java.util.List;

/**
 * A SecureBank customer with two deliberately nullable fields — they are the heart of
 * the AMBIGUITY problem in TheNullProblemDemo:
 *
 *   email == null            might mean "not set yet"          (we never asked)
 *   primaryAccountId == null might mean "has no account"       (a perfectly legal state)
 *
 * Both are just `null`. The type system cannot tell the caller which meaning applies —
 * the only documentation is tribal knowledge ("everyone knows Bob's email can be null").
 * That is what Optional fixes: the return type of a lookup SAYS what absence means,
 * and the compiler forces the caller to handle it.
 */
public final class Customer {

    private final String id;
    private final String name;
    private final String email;            // nullable: "not set yet"
    private final String primaryAccountId; // nullable: "has no account"

    public Customer(String id, String name, String email, String primaryAccountId) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.primaryAccountId = primaryAccountId;
    }

    public String getId() { return id; }
    public String getName() { return name; }

    /** Raw nullable getter — the pre-Java-8 view. Wrap with Optional.ofNullable AT THE BOUNDARY. */
    public String getEmail() { return email; }

    public String getPrimaryAccountId() { return primaryAccountId; }

    @Override
    public String toString() {
        return String.format("%s %-14s email=%-24s primaryAccount=%s",
                id, name,
                email == null ? "(not set)" : email,
                primaryAccountId == null ? "(none)" : primaryAccountId);
    }

    /** The same three customers everywhere in this module, so output is predictable in class. */
    public static List<Customer> sampleCustomers() {
        return Arrays.asList(
                new Customer("C-1", "Alice Nguyen",   "alice@securebank.example", "ACC-1001"),
                new Customer("C-2", "Bob Rivera",     null,                       "ACC-1002"),
                new Customer("C-3", "Dana Whitfield", "dana@securebank.example",   null));
    }
}
