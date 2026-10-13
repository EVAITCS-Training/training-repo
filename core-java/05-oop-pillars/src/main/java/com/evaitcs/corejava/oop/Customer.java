package com.evaitcs.corejava.oop;

/**
 * The class-vs-object prop: ONE blueprint, MANY customers.
 * Also the module's constructor exhibit — default vs explicit and this(...) chaining —
 * which ClassVsObjectDemo narrates while this class does the work.
 */
public class Customer {

    // static, so ONE counter is shared by every Customer and ids never collide.
    // We deliberately do not unpack static-vs-instance here — that deep dive is module 11.
    // For now just notice: `++nextNumber` is visible to ALL customers, because there is
    // only one copy of it, living on the class, not inside any one object.
    private static int nextNumber = 1000;

    // final fields: set once in the constructor, read-only for life. A customer's
    // identity is exactly the kind of state that must not drift later.
    private final String id;
    private final String name;

    /**
     * The no-arg constructor — here written EXPLICITLY so it can delegate.
     * Trap: a class that declares no constructor at all gets a free default one from
     * the compiler; the moment you declare ANY constructor yourself, the freebie is gone
     * and new Customer() only works because we wrote this one.
     */
    public Customer() {
        this("Walk-in Guest");
    }

    /** this(...) chaining: hand off to the fuller constructor — first line, like super(...). */
    public Customer(String name) {
        this("C-" + (++nextNumber), name);
    }

    /** End of the chain — the ONE body that does the real work: validate, then assign. */
    public Customer(String id, String name) {
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("customer id required");
        }
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("customer name required");
        }
        this.id = id;             // `this.id` is the field, `id` is the parameter. When the
        this.name = name.trim();  // names collide, `this.` is not style — it is disambiguation.
    }

    public String greet() {
        return "Welcome to SecureBank, " + name + " (" + id + ")";
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return "Customer[" + id + " " + name + "]";
    }
}
