package com.evaitcs.corejava.refs;

/**
 * The 8 primitives — what they are, how big, what they hold by default.
 *
 * THE ONE SENTENCE: a primitive variable holds the VALUE ITSELF, right there in its slot.
 * Everything on this tour is a fixed-size chunk of bits that IS the number/char/flag.
 */
public class PrimitiveTourDemo {

    // Instance FIELDS get a free default the moment the object is built (see section 3).
    // This is the ONLY place Java hands you a default — locals get nothing.
    private byte    aByte;
    private short   aShort;
    private int     anInt;
    private long    aLong;
    private float   aFloat;
    private double  aDouble;
    private char    aChar;
    private boolean aBoolean;

    public static void main(String[] args) {
        banner("1. The 8 primitives — size in bits, and the range each can hold");
        System.out.printf("%-8s %4s  %-21s %-21s%n", "type", "bits", "min", "max");
        row("byte",  Byte.SIZE,      String.valueOf(Byte.MIN_VALUE),      String.valueOf(Byte.MAX_VALUE));
        row("short", Short.SIZE,     String.valueOf(Short.MIN_VALUE),     String.valueOf(Short.MAX_VALUE));
        row("int",   Integer.SIZE,   String.valueOf(Integer.MIN_VALUE),   String.valueOf(Integer.MAX_VALUE));
        row("long",  Long.SIZE,      String.valueOf(Long.MIN_VALUE),      String.valueOf(Long.MAX_VALUE));
        row("char",  Character.SIZE, String.valueOf((int) Character.MIN_VALUE), String.valueOf((int) Character.MAX_VALUE));
        System.out.println("boolean  size is NOT specified by the JVM; its only values are true/false.");
        System.out.println();
        System.out.println("float   bits=" + Float.SIZE  + "  range about -" + Float.MAX_VALUE + " .. " + Float.MAX_VALUE);
        System.out.println("double  bits=" + Double.SIZE + "  range about -" + Double.MAX_VALUE + " .. " + Double.MAX_VALUE);
        System.out.println("TRAP: Float.MIN_VALUE / Double.MIN_VALUE are the smallest POSITIVE values (near zero),");
        System.out.println("      NOT the most negative. Float.MIN_VALUE = " + Float.MIN_VALUE);

        banner("2. Literal suffixes: L makes it a long, F makes it a float");
        long big = 2_147_483_648L; // one past Integer.MAX_VALUE — without the L this literal is an int and won't compile
        System.out.println("2_147_483_648L (long)  = " + big + "   (too big for int; the L suffix is mandatory)");
        float third = 1.0f / 3.0f; // the f suffixes make these float literals, not double
        System.out.println("1.0f / 3.0f (float)    = " + third);
        // float wrong = 1.0 / 3.0;  // COMPILE ERROR: 1.0/3.0 is a double; can't narrow to float without f (or a cast)
        System.out.println("(decimal literals are DOUBLE by default — that's exactly why float needs the f.)");

        banner("3. Defaults: FIELDS get 0/false for free; LOCALS get NOTHING");
        PrimitiveTourDemo tour = new PrimitiveTourDemo();
        System.out.println("fields on a fresh object : byte=" + tour.aByte + " int=" + tour.anInt
                + " long=" + tour.aLong + " double=" + tour.aDouble + " char(code)=" + (int) tour.aChar
                + " boolean=" + tour.aBoolean);
        // A LOCAL variable has NO default — reading it before assigning is a COMPILE ERROR:
        //
        //   int local;
        //   System.out.println(local);
        //   ^^^^^^^^^^^^^^^^^^^^^^^^^ compile error: "variable local might not have been initialized"
        //
        // Why: a local's stack slot holds whatever garbage was there before; Java refuses to guess.
        int local = 0; // so we initialize it first — now it's fine
        System.out.println("a local we initialized   : local=" + local + "  (Java will not read an uninitialized local.)");

        banner("4. char is really a NUMBER (an unsigned 16-bit code unit)");
        char letter = 'A';
        int asNumber = letter;          // widens to its numeric code (65)
        char next = (char) (letter + 1); // 'A'+1 promotes to int 66; cast back to char -> 'B'
        System.out.println("'A' as a number          = " + asNumber);
        System.out.println("(char)('A' + 1)          = '" + next + "'   (this is why 'A' + 1 == 'B')");
        System.out.println("'B' - 'A'                = " + ('B' - 'A') + "   (arithmetic on chars behaves like ints).");

        banner("5. float/double exist for speed, but NEVER hold money in them");
        System.out.println("double 0.1 + 0.2         = " + (0.1 + 0.2) + "  <-- not 0.3! binary can't hold decimal fractions");
        System.out.println("=> MoneyDoubleVsBigDecimalDemo shows the exact damage and the BigDecimal fix.");
    }

    private static void row(String type, int bits, String min, String max) {
        System.out.printf("%-8s %4d  %-21s %-21s%n", type, bits, min, max);
    }

    static void banner(String title) {
        System.out.println();
        System.out.println("=== " + title + " ===");
    }
}
