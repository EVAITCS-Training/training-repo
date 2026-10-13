package com.evaitcs.corejava.statics;

import java.util.HashMap;
import java.util.Map;

/**
 * The CAUTION exhibit: mutable static state = GLOBAL state.
 *
 * Every class in the JVM that can see Configuration shares this ONE map. That is
 * exactly what makes it dangerous:
 *   - two "unrelated" demos/tests interfere through it (StaticStateCautionDemo),
 *   - the coupling is INVISIBLE at both call sites - nothing in their signatures
 *     admits that Configuration is involved,
 *   - every thread sees every write, so unsynchronized mutation is a data race
 *     (module 04's StringBuffer-vs-StringBuilder was the same conversation).
 *
 * The rules this class preaches:
 *   1. Prefer static FINAL IMMUTABLE data (BankUtils.CURRENCY_CODE).
 *   2. If a mutable static is unavoidable: encapsulate it behind methods (done here),
 *      synchronize it (skipped here - the demos are single-threaded), and DOCUMENT
 *      that it is a global.
 *   3. Give tests a reset() - and still ask whether the global was worth it.
 */
public class Configuration {

    /** ONE map. For the whole JVM. Whoever writes last wins - everywhere, instantly. */
    private static final Map<String, String> SETTINGS = new HashMap<String, String>();

    public static void set(String key, String value) {
        SETTINGS.put(key, value);
    }

    public static String get(String key) {
        return SETTINGS.get(key);
    }

    /** The apology method: tests NEED it precisely because the state outlives each of them. */
    public static void reset() {
        SETTINGS.clear();
    }
}
