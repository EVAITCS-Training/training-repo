package com.evaitcs.java8.lambdas;

import java.util.function.Supplier;

/**
 * The classic gotcha: what does `this` mean inside the body?
 *
 * Anonymous class => `this` is the anonymous object itself (a NEW instance of a NEW class).
 * Lambda          => `this` is the ENCLOSING instance (the lambda does not introduce a new `this`).
 *
 * Reason to give the class: a lambda is not an object of some new type you wrote —
 * it is behavior bolted onto an existing functional interface, so there is no new `this` to have.
 */
public class ThisMeaningDemo {

    private final String name = "ThisMeaningDemo (the enclosing object)";

    public static void main(String[] args) {
        new ThisMeaningDemo().run(); // need an instance so `this` exists
    }

    void run() {
        // ---- Anonymous class: `this` points at the anonymous Supplier instance ----
        Supplier<String> anonymous = new Supplier<String>() {
            @Override
            public String get() {
                // Here `this` is the anonymous Supplier — it has no `name` field of its own,
                // so to reach the outer object you must qualify it with the class name:
                return "anonymous class sees this = " + this.getClass().getName()
                        + ", outer name via ThisMeaningDemo.this = " + ThisMeaningDemo.this.name;
            }
        };

        // ---- Lambda: `this` is STILL the enclosing ThisMeaningDemo instance ----
        Supplier<String> lambda = () ->
                "lambda sees this = " + this.name
                // `this` here is ThisMeaningDemo — no qualification needed, no surprise object.
                + "  (getClass() = " + this.getClass().getSimpleName() + ")";

        System.out.println(anonymous.get());
        System.out.println(lambda.get());
        System.out.println();
        System.out.println("Takeaway: in a lambda, `this` behaves exactly like in surrounding code.");
        System.out.println("That is why converting an anonymous class to a lambda is usually safe —");
        System.out.println("unless the old code relied on `this` being the anonymous object.");
    }
}
