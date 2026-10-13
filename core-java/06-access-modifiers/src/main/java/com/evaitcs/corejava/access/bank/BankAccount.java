package com.evaitcs.corejava.access.bank;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * The module's "library" class: a SecureBank account carrying ONE field per
 * visibility level, so every demo can stand at a different vantage point and
 * look at the SAME four members.
 *
 * THE DIAL, narrowest to widest:
 *   private          only code inside THIS class body
 *   package-private  any class in com.evaitcs.corejava.access.bank  <- NO KEYWORD (the accident)
 *   protected        the package, PLUS subclasses anywhere, but only THROUGH INHERITANCE
 *   public           everyone, everywhere - an API promise you can never take back
 *
 * WHY the dial exists: visibility is not about hiding secrets, it is about
 * LIMITING COUPLING. Everything you make public is a promise other code will
 * build on; the narrower the modifier, the more freedom you keep to change the
 * implementation later. Module 05 (encapsulation) says "protect the invariant";
 * access modifiers are the language feature that ENFORCES it.
 */
public class BankAccount {

    // ------------------------------------------------------------------
    // Four fields - the whole module in four lines.
    // ------------------------------------------------------------------

    /** PUBLIC: the id is safe to expose - knowing it grants no capability. */
    public final String id;

    /**
     * PROTECTED: subclasses (see SubclassInOtherPackage) may work with the
     * balance directly when they extend the account's behavior - but ONLY
     * through inheritance, i.e. on their OWN inherited copy (this.balance),
     * never through a reference to somebody else's account. That rule is
     * JLS 6.6.2 and the exam favorite; ProtectedViaSubclassDemo proves it.
     */
    protected BigDecimal balance;

    /**
     * PACKAGE-PRIVATE - written by writing NOTHING. This is the accident to
     * name out loud in class: forgetting a modifier does NOT give you private,
     * it silently gives you package visibility. Tooling inside the bank
     * package (SamePackageClass, auditors) can read it; the outside cannot.
     */
    String auditNote;

    /**
     * PRIVATE: the pin is a capability - what you can DO with the account, not
     * just know about it. No code outside this class body can even NAME it.
     * The only way in is the public method checkPin(), which hands out an
     * ANSWER ("is this the pin?") instead of the SECRET itself.
     */
    private String pin;

    /**
     * A PRIVATE nested class - legal because it is NOT top-level. Top-level
     * classes may only be public or package-private (see ClassLevelModifiersDemo),
     * but a nested class can be private: the strongest "stay out" sign Java has
     * for a helper. Vault is an implementation detail of BankAccount and nothing
     * else - it is `static` because pin-checking needs no account instance, and
     * nested because no other class should ever name the type, let alone call it.
     */
    private static final class Vault {

        private Vault() {
            // no instances - a static utility nobody outside BankAccount can reach
        }

        /** Compares pins WITHOUT ever returning or exposing the stored one. */
        static boolean matches(String storedPin, String candidate) {
            return storedPin != null && candidate != null
                    && storedPin.length() == candidate.length()
                    && scramble(storedPin).equals(scramble(candidate));
        }

        static boolean looksLikePin(String candidate) {
            if (candidate == null || candidate.length() != 4) {
                return false;
            }
            for (int i = 0; i < candidate.length(); i++) {
                if (!Character.isDigit(candidate.charAt(i))) {
                    return false;
                }
            }
            return true;
        }

        /** Deterministic one-way-ish scramble - teaching stand-in for real hashing. */
        private static String scramble(String pin) {
            StringBuilder scrambled = new StringBuilder(pin.length());
            for (int i = 0; i < pin.length(); i++) {
                scrambled.append((char) ('9' - pin.charAt(i) + '0'));
            }
            return scrambled.toString();
        }
    }

    // ------------------------------------------------------------------
    // Constructor - the invariant starts here and never leaves.
    // ------------------------------------------------------------------

    public BankAccount(String id, BigDecimal openingBalance, String pin) {
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("id is required");
        }
        if (!Vault.looksLikePin(pin)) {
            throw new IllegalArgumentException("pin must be exactly 4 digits, got: " + pin);
        }
        this.id = id;
        this.balance = (openingBalance == null ? BigDecimal.ZERO : openingBalance)
                .setScale(2, RoundingMode.HALF_UP);
        this.pin = pin;
        this.auditNote = "account opened";
    }

    /** One deterministic account for demos and tests - never random data on a projector. */
    public static BankAccount sampleAccount() {
        return new BankAccount("SB-1001", new BigDecimal("250.00"), "2468");
    }

    // ------------------------------------------------------------------
    // The controlled doors: public methods over non-public state.
    // ------------------------------------------------------------------

    public String getId() {
        return id;
    }

    /**
     * The public getter is the DOOR; the protected field is the ROOM. Callers
     * outside get the door - and a door can validate, log, or copy, which a
     * raw field never can. BigDecimal is immutable, so returning it directly
     * is safe (a mutable type would need a defensive copy here).
     */
    public BigDecimal getBalance() {
        return balance;
    }

    public void deposit(BigDecimal amount) {
        requirePositive(amount, "deposit");
        balance = balance.add(amount).setScale(2, RoundingMode.HALF_UP);
        flagForAudit("deposit " + amount.setScale(2, RoundingMode.HALF_UP).toPlainString());
    }

    public void withdraw(BigDecimal amount) {
        requirePositive(amount, "withdrawal");
        if (amount.compareTo(balance) > 0) {
            throw new IllegalStateException(
                    "insufficient funds: tried to withdraw " + amount.toPlainString()
                            + " from balance " + balance.toPlainString());
        }
        balance = balance.subtract(amount).setScale(2, RoundingMode.HALF_UP);
        flagForAudit("withdrawal " + amount.setScale(2, RoundingMode.HALF_UP).toPlainString());
    }

    /** Answers "is this the pin?" - the SECRET itself never leaves the class. */
    public boolean checkPin(String candidate) {
        return Vault.matches(pin, candidate);
    }

    /**
     * Capability, not visibility: rotating the pin REQUIRES proving the old
     * one. No amount of access to the object yields the pin; only a validated
     * action does. This is what "private field + public method" buys you.
     */
    public void rotatePin(String currentPin, String newPin) {
        if (!checkPin(currentPin)) {
            throw new SecurityException("pin rotation refused: current pin did not match");
        }
        if (!Vault.looksLikePin(newPin)) {
            throw new IllegalArgumentException("new pin must be exactly 4 digits, got: " + newPin);
        }
        pin = newPin;
        flagForAudit("pin rotated");
    }

    /**
     * TRAP-BUSTER: `private` means "this CLASS", not "this OBJECT". Code inside
     * BankAccount may read the private pin of ANOTHER BankAccount - both objects
     * are inside the same class body, so both pins are visible here.
     */
    public boolean samePinAs(BankAccount other) {
        return other != null && Vault.matches(this.pin, other.pin);
    }

    /** Package-private method: the audit tooling in the bank package calls this. */
    void flagForAudit(String reason) {
        auditNote = auditNote + " | " + reason;
    }

    private static void requirePositive(BigDecimal amount, String what) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(what + " amount must be > 0, got: " + amount);
        }
    }

    /** The report used by FromInsideTheClassDemo - the class describing its own vision. */
    public String visibilityReportFromInside() {
        StringBuilder report = new StringBuilder();
        report.append("from INSIDE BankAccount I CAN see all four members:\n");
        report.append("  private          pin       = **** (").append(pin.length())
                .append(" digits - readable BY NAME right here, masked out of habit)\n");
        report.append("  package-private  auditNote = ").append(auditNote).append('\n');
        report.append("  protected        balance   = ").append(balance.toPlainString()).append('\n');
        report.append("  public           id        = ").append(id);
        return report.toString();
    }

    @Override
    public String toString() {
        // NEVER leak the pin in toString - logs are read by strangers.
        return "BankAccount[id=" + id + ", balance=" + balance.toPlainString() + "]";
    }
}
