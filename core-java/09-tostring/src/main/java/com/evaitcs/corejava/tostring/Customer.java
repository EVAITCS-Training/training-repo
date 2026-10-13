package com.evaitcs.corejava.tostring;

/**
 * A SecureBank customer — the HUMAN-SENTENCE toString style, for contrast.
 *
 * AccountPrinted uses the key=value shape ("AccountPrinted[id=..., owner=...]"), which
 * is what IDEs generate and what you want in LOGS: greppable, diffable, unambiguous.
 *
 * Customer instead reads like a sentence ("Ada Lovelace <ada@...>, member since 2019"),
 * which is what you want when the object appears inside USER-FACING text — an error
 * message, a confirmation dialog, a receipt. Same method, two audiences; pick per class.
 *
 * There is no secret here on purpose: not every class needs masking, but every class
 * that gets printed anywhere needs to decide its audience once, consciously.
 */
public class Customer {

    private final String name;
    private final String email;
    private final int memberSince;

    public Customer(String name, String email, int memberSince) {
        this.name = name;
        this.email = email;
        this.memberSince = memberSince;
    }

    public String getName() { return name; }
    public String getEmail() { return email; }
    public int getMemberSince() { return memberSince; }

    @Override
    public String toString() {
        // Human-sentence style: this string is meant to be READ by a person inside a
        // larger message, e.g. "Welcome back, " + customer — no brackets, no field names.
        return name + " <" + email + ">, member since " + memberSince;
    }
}
