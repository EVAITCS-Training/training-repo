package com.evaitcs.corejava.contracts;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Claim #4 of the module: a class extends ONE class but implements MANY
 * interfaces — multiple inheritance of TYPE, never of STATE or CODE.
 *
 * CardPayment is the proof: no extends clause at all, four interfaces. And the
 * WHY behind Java's single-class rule: the diamond problem with fields and
 * constructors has no sane answer; with pre-Java-8 interfaces (signatures only,
 * no state, no code) there is nothing to collide.
 */
public class MultipleInheritanceDemo {

    public static void main(String[] args) {

        banner("1. ONE class slot, MANY interface slots (reflection, not opinion)");

        CardPayment card = new CardPayment("CARD-77", new BigDecimal("50.00"));
        System.out.println("CardPayment extends exactly one class : "
                + card.getClass().getSuperclass().getSimpleName() + "  (implicit — no extends clause)");
        System.out.print("CardPayment implements               : ");
        for (Class<?> i : card.getClass().getInterfaces()) {
            System.out.print(i.getSimpleName() + " ");
        }
        System.out.println();
        System.out.println("=> 1 class ancestor, 4 contracts. The class slot was already spent on Object,");
        System.out.println("   and interfaces are the ONLY way to add more types on top.");

        banner("2. The same object seen through every one of its types");

        // Five reference variables, five DIFFERENT compile-time types, ONE object.
        // Each variable only exposes the methods of its own lens — that is the point.
        Object asObject            = card;   // everything is an Object
        Transferable asTransfer    = card;   // money can move through it
        PaymentMethod asPayment    = card;   // it can pay
        Auditable asAudit          = card;   // it keeps a trail
        Comparable<CardPayment> asComparable = card;   // it has a natural order

        asTransfer.deposit(new BigDecimal("5.00"));
        asPayment.pay(new BigDecimal("5.00"));
        System.out.println("through the Transferable lens : deposit(5.00) ok   -> loaded " + card.getLoaded());
        System.out.println("through the PaymentMethod lens: pay(5.00) ok       -> loaded " + card.getLoaded());
        System.out.println("through the Auditable lens    : trail has entries  -> "
                + (asAudit.auditTrail().length() > 0));
        System.out.println("through the Comparable lens   : compareTo(self)    -> "
                + asComparable.compareTo(card) + " (0 = equal)");
        System.out.println("through the Object lens       : " + asObject);
        System.out.println("=> the object did not change; each variable is a different LENS on it.");
        System.out.println("   (asObject.deposit(..) would NOT compile — Object's lens has no such method.)");

        banner("3. Interfaces cut ACROSS families: account and card in one list");

        SavingsAccount savings = new SavingsAccount("S-1001", new BigDecimal("1000.00"));
        List<Transferable> movers = new ArrayList<Transferable>();
        movers.add(savings);     // family: Account
        movers.add(card);        // family: Object -> CardPayment. No common CLASS ancestor...
        for (Transferable t : movers) {
            t.deposit(new BigDecimal("10.00"));   // ...and the loop does not care.
            System.out.println("deposited 10.00 into " + t);
        }
        boolean moved = card.transferTo(savings, new BigDecimal("25.00"));
        System.out.println("card.transferTo(savings, 25.00) = " + moved
                + "  -> card " + card.getLoaded() + ", savings " + savings.getBalance());
        System.out.println("=> transferTo only ever talks to the INTERFACE (target.deposit), never");
        System.out.println("   to a concrete class. Unrelated types cooperate through the contract.");

        banner("4. Comparable as the fourth contract: sorting cards for free");

        List<CardPayment> cards = new ArrayList<CardPayment>();
        cards.add(new CardPayment("CARD-99", new BigDecimal("10.00")));
        cards.add(card);
        cards.add(new CardPayment("CARD-01", new BigDecimal("90.00")));
        Collections.sort(cards);   // works ONLY because CardPayment implements Comparable
        for (CardPayment c : cards) {
            System.out.println("sorted: " + c.getCardId());
        }
        System.out.println("=> java.util could not have anticipated CardPayment in 1998 — the interface");
        System.out.println("   is the handshake that lets NEW types plug into OLD algorithms.");

        banner("5. WHY Java forbids multiple CLASS inheritance: the diamond with STATE");

        System.out.println("Imagine: class FlyingCar extends Account, Vehicle  (both classes!)");
        System.out.println("  Account has field `balance` (money)   and constructor Account(opening)");
        System.out.println("  Vehicle has field `balance` (weight!) and constructor Vehicle(tank)");
        System.out.println("  Question 1: FlyingCar gets ONE or TWO `balance` fields? Which wins?");
        System.out.println("  Question 2: which constructor runs FIRST, with whose arguments?");
        System.out.println("  Question 3: both define deposit() — which body is inherited?");
        System.out.println("=> No answer is obviously right, so Java refuses the question: ONE superclass.");
        System.out.println();
        System.out.println("Pre-Java-8 interfaces make the diamond SAFE: no fields, no constructors,");
        System.out.println("no method bodies — only SIGNATURES. Two signatures with the same name");
        System.out.println("collapse into one obligation: CardPayment implements it once, done.");
        System.out.println();
        System.out.println("FORWARD POINTER: Java 8 gives interfaces default method BODIES — and the");
        System.out.println("diamond bites back (two unrelated defaults colliding). Resolution rules:");
        System.out.println("java8-features/02-interface-evolution.");
    }

    static void banner(String title) {
        System.out.println();
        System.out.println("=== " + title + " ===");
    }
}
