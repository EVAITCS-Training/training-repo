package com.evaitcs.corejava.equality;

import java.util.Objects;

/**
 * Demo 4 — the SYMMETRY TRAP: instanceof-based equals across a class hierarchy.
 *
 * The classic Person/Employee break. If a superclass uses `instanceof` to type-check,
 * a SUBCLASS instance passes the SUPERCLASS's test (an Employee IS-A Person), but the
 * superclass instance FAILS the subclass's stricter test. Result:
 *     person.equals(employee) == true     (Employee passes `instanceof Person`)
 *     employee.equals(person) == false    (Person fails `instanceof Employee`)
 * Symmetry — and with it transitivity — is gone. Put both in a HashSet and the size
 * depends on INSERTION ORDER, which is the "haunted collections" symptom.
 *
 * THE LESSON: getClass() (as AccountKey uses) makes cross-type equality impossible, so
 * symmetry can never break this way. instanceof can be correct, but only when the whole
 * hierarchy is designed for it (a documented "canonical form"). Absent that design,
 * prefer getClass().
 */
public class SymmetryTrapDemo {

    /** Superclass using instanceof — the root of the asymmetry. */
    static class Person {
        final String name;

        Person(String name) {
            this.name = name;
        }

        @Override
        public boolean equals(Object other) {
            // instanceof accepts ANY Person, INCLUDING subclasses. That is the leak.
            if (!(other instanceof Person)) {
                return false;
            }
            return Objects.equals(name, ((Person) other).name);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name);
        }
    }

    /** Subclass adding a field — now its equals is STRICTER than the parent's. */
    static class Employee extends Person {
        final int employeeId;

        Employee(String name, int employeeId) {
            super(name);
            this.employeeId = employeeId;
        }

        @Override
        public boolean equals(Object other) {
            // Requires an Employee. A plain Person can never satisfy this.
            if (!(other instanceof Employee)) {
                return false;
            }
            Employee that = (Employee) other;
            return super.equals(that) && employeeId == that.employeeId;
        }

        @Override
        public int hashCode() {
            return Objects.hash(super.hashCode(), employeeId);
        }
    }

    public static void main(String[] args) {

        banner("instanceof asymmetry — both directions, printed");

        Person person = new Person("Ada");
        Employee employee = new Employee("Ada", 7);

        boolean pEqE = person.equals(employee);   // true: Employee passes `instanceof Person`
        boolean eEqP = employee.equals(person);   // false: Person fails `instanceof Employee`

        System.out.println("person.equals(employee)   : " + pEqE + "   <- Employee IS-A Person, name matches");
        System.out.println("employee.equals(person)   : " + eEqP + "  <- a Person is NOT an Employee");
        System.out.println("=> Symmetric? " + (pEqE == eEqP) + ". The contract requires true==true or false==false.");

        banner("The knock-on effect: HashSet size depends on insertion order");

        java.util.Set<Person> setA = new java.util.HashSet<Person>();
        setA.add(person);
        setA.add(employee);
        java.util.Set<Person> setB = new java.util.HashSet<Person>();
        setB.add(employee);
        setB.add(person);
        System.out.println("add(person), add(employee) -> size " + setA.size());
        System.out.println("add(employee), add(person) -> size " + setB.size());
        System.out.println("=> Same two objects, different size. That is a broken equals talking.");

        banner("The fix");
        System.out.println("Use getClass() != other.getClass() (AccountKey does). Then person.equals(employee)");
        System.out.println("is false BOTH ways — symmetric, and the set behaves predictably.");
    }

    static void banner(String title) {
        System.out.println();
        System.out.println("=== " + title + " ===");
    }
}
