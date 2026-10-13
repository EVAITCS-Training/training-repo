# 09 — toString: The Object's One-Line Résumé

**The big idea:** every object you print — directly, inside a log line, an exception
message, a `List`, or a `String.format` — is silently asked for its one-line résumé by
`toString()`. Write nothing and the JVM supplies `ClassName@1b6d3586`: honest,
unique-ish, and useless to the human debugging at midnight. Write it carelessly (or
accept the IDE's "include all fields" default) and you export PINs and API keys into
every log pipeline you own. Small method, daily impact.

Domain for the module: SecureBank accounts in THREE parallel versions so the contrast
is printable — `AccountRaw` (no override), `AccountPrinted` (the good one:
StringBuilder, key=value, masked secrets), `AccountLeaky` (the all-fields
over-share) — plus `SavingsAccount` (the inheritance probe), `Customer` (the
human-sentence style) and `Transaction` (the compact list element).

## What println actually does

```mermaid
flowchart TB
    P["System.out.println(obj)"] --> V["String.valueOf(obj)"]
    V --> Q{"obj == null ?"}
    Q -->|"yes"| N["return the string \"null\"<br/>(THIS check is why printing null never NPEs)"]
    Q -->|"no"| T["obj.toString()<br/>VIRTUAL call — YOUR override runs"]
    N --> W["write chars to stdout"]
    T --> W
    C["\"text\" + obj"] -->|"compiles to StringBuilder.append(Object)<br/>(module 04)"| V
    F["String.format(\"%s\", obj)"] -->|"also ends here"| V
```

**Read the diagram:** three doors in, one way through.

- `println(obj)`, `"text" + obj` (which compiles to `StringBuilder.append(Object)` —
  module 04) and `String.format("%s", obj)` all funnel into `String.valueOf(obj)`.
- `valueOf` checks `obj == null`: if null it returns the string `"null"` — THIS check is
  why printing null never throws a `NullPointerException`. Otherwise it calls
  `obj.toString()`.
- That call is VIRTUAL: the JVM picks the override from the object's ACTUAL class
  (module 05), so YOUR `toString` runs. The resulting characters are written to stdout.

That is why you almost never call `toString()` yourself — and why you cannot control the
content at each call site. Control it once, in the class.

**The overload trap** (`PrintlnInternalsDemo`): `String.valueOf` and `println` are both
OVERLOADED, and overload resolution happens at COMPILE time on the static type — the type
the variable is declared as (module 05).

| call | path selected | result |
|---|---|---|
| `println((Object) null)` | `valueOf(Object)` — null-checked | prints `null` — safe |
| `"balance: " + nullObject` | `append(Object)` → `valueOf(Object)` | `balance: null` — safe |
| `String.valueOf((char[]) null)` | `valueOf(char[])` → `new String(chars)` — dereferences | **`NullPointerException`** |
| `println(pinChars)` | `println(char[])` — prints CONTENTS | `4921` — the secret, verbatim |
| `println((Object) pinChars)` | `println(Object)` → default toString | `[C@hex` |

## The default, decoded

`Object.toString()` is literally `getClass().getName() + "@" + Integer.toHexString(hashCode())`:

| part | example | comes from |
|---|---|---|
| before `@` | `com.evaitcs.corejava.tostring.AccountRaw` | `getClass().getName()` |
| separator | `@` | a literal character |
| after `@` | `1b6d3586` | `Integer.toHexString(hashCode())` — and the DEFAULT hashCode is the **identity** hash: a number from the object itself, not its contents (module 08) |

`DefaultToStringDemo` cuts the printed string in half and recomputes
`Integer.toHexString(System.identityHashCode(raw))` — same value, proven live (the test
re-proves it deterministically). So the default is a **fingerprint**: good for "is this
the SAME object?" across two log lines; worthless for "what IS this object?" — which is
the question a human debugging a balance problem is actually asking.

## toString and inheritance

```mermaid
flowchart TB
    S["println(savings)<br/>savings is a SavingsAccount"] --> L1{"SavingsAccount<br/>declares toString?"}
    L1 -->|"NO — lookup walks UP the hierarchy"| L2{"AccountPrinted<br/>declares toString?"}
    L2 -->|"YES — it runs, with `this` =<br/>the SavingsAccount object"| B["getClass().getSimpleName()<br/>asks the RUNTIME object"]
    B --> OUT["SavingsAccount[id=..., owner=..., balance=...]<br/>the name is right AUTOMATICALLY"]
    L2 -.->|"if it HARDCODED \"Account[\" instead"| LIE["every subclass prints Account[...]<br/>the name LIES by omission —<br/>and subclass fields are invisible"]
```

**Read the diagram:**

- `println(savings)`, where `savings` is a `SavingsAccount`. The lookup starts at the
  runtime class: does `SavingsAccount` declare `toString`? NO — so it walks UP the hierarchy.
- `AccountPrinted` declares it, so that body runs — with `this` = the SavingsAccount object.
- Inside, `getClass().getSimpleName()` asks the RUNTIME object for its name, so the printed
  name is right AUTOMATICALLY.
- The dashed branch: had the base HARDCODED `"Account["`, every subclass would print
  `Account[...]` — the name LIES by omission and subclass fields are invisible.

`InheritedToStringDemo` shows all three outcomes:

- **The lie** — `LegacySavings` (no toString) inherits `LegacyAccount`'s hardcoded
  `"Account[..."` output: wrong type name, missing `interestRate`.
- **Fix A** — the subclass overrides, calls `super.toString()` and appends its own
  fields. Correct, but EVERY future subclass author must remember to do it.
- **Fix B (recommended)** — the base builds its name with `getClass().getSimpleName()`,
  written ONCE. The method is inherited, but `this` is the runtime object, so
  `SavingsAccount` prints its own name even with no toString of its own. Honest caveat:
  a subclass that ADDS state should still override to append its new fields — the trick
  guarantees the *name* never lies, not that the field list is complete.

## Good vs bad — one table

| toString | prints | verdict |
|---|---|---|
| none (`AccountRaw`) | `com.evaitcs...AccountRaw@1b6d3586` | useless to humans; fine as a debug fingerprint |
| all-fields (`AccountLeaky`) | `AccountLeaky[..., pin=4921, secretKey=sk-9f3ab7c2e1]` | **DANGEROUS** — credentials exported to every log |
| key=value (`AccountPrinted`) | `AccountPrinted[id=ACC-1001, owner=Ada Lovelace, balance=1250.75, card=****1234, pin=****]` | the IDE-default shape, done safely: greppable, complete, masked |
| human sentence (`Customer`) | `Ada Lovelace <ada@securebank.example>, member since 2019` | best inside USER-facing messages |
| hardcoded name (`LegacyAccount`) | `Account[...]` — even for a `LegacySavings` | lies by omission for every subclass; use `getClass().getSimpleName()` |
| array (`Transaction[]`) | `[Lcom.evaitcs...Transaction;@hex` | arrays NEVER override toString — use `Arrays.toString(arr)` |

Building the good one uses module 04 skills: one `StringBuilder`, append piece by piece,
one `String` at the end — no `+=` chains, no formatting ceremony for a key=value line.

## The security rule (say it at the end of class)

**toString is a data-export API.** Every log line, exception message, dialog and debug
print is an export. The call sites that trigger it — `println`, concat, `%s`, logging
`{}` placeholders (lazy: they build the string only if the log level is enabled, but
they DO fire), `JOptionPane`, assertion messages — are everywhere and invisible.
`SecurityMaskingDemo` retells the incident. One `log.debug("processing {}", account)`
line shipped during an outage. The account's toString included all fields. Within an
hour, thousands of PINs and API keys sat plaintext — indexed and searchable — in a
third-party error tracker.

1. Review the field list like an HTTP response payload: *would you send this to a stranger?*
2. **Never** passwords, PINs, tokens, session ids, full card numbers.
3. Mask per field, with judgment: card → `****1234` (identifies, doesn't empower);
   pin → `****` with NO digits — "last 4" of a 4-digit pin IS the whole secret.
4. IDE "generate toString" includes ALL fields by default — uncheck the secrets.
   **Reading** the generated code is the whole job.

**In one sentence:** toString is a data-export API — every field you include gets
published to every log, tracker and dialog that ever prints the object.

## Contract-ish guidance

- The result should be **stable and informative for debugging** — but it is **NOT a
  parse format**. Never write code that splits a toString back into fields; that
  couples callers to wording you will eventually improve. Expose getters for data;
  toString is for humans.
- Keep it **total**: `Object.toString` exists on every object so print/concat never
  fail — your override should not throw either. Guard nulls in masking helpers
   (`AccountPrinted.maskCard` returns `****` on dirty data rather than throwing).
- Include a concise, informative representation of ALL non-secret state — that is the
  Javadoc's actual wording, and it is what makes 2 a.m. debugging possible.

> **Forward note:** IDEs generate the key=value style in seconds, and Java 16 `record`s
> auto-generate a decent `ClassName[field=value, ...]` from the component list. This
> module compiles with `--release 8`, so we build it by hand with `StringBuilder` first
> (module 04) — the record is the payoff waiting in the future course. (Records do NOT
> solve the security problem: a record component named `pin` lands in its toString too.)

## Class flow

1. Show the diagrams above (concept first — the println chain, then inheritance).
2. Run the demos in order: `.\mvnw.cmd -pl 09-tostring compile exec:java`
   (this runs `ToStringDemos`, which calls each demo `main()` in teaching order).
3. Run the tests as *proof*: `.\mvnw.cmd -pl 09-tostring test` — each test prints the
   behavior it asserts, including the identity-hex decode and the leaky export.

| Demo class | What it shows |
|---|---|
| `DefaultToStringDemo` | the default `ClassName@hex` decoded live — the hex IS `identityHashCode` (module 08 callback) |
| `PrintlnInternalsDemo` | `println` → `String.valueOf` → `toString`; null prints `null`; `(char[]) null` throws a `NullPointerException`; `println(char[])` prints contents |
| `GoodToStringDemo` | three styles side by side; the four decisions inside `AccountPrinted.toString`; log line, exception message, `%s` |
| `CollectionsToStringDemo` | a list calls YOUR toString per element; raw elements = wall of fingerprints; the array trap + `Arrays.toString` |
| `InheritedToStringDemo` | the hardcoded-name lie, Fix A (`super.toString()` + append), Fix B (`getClass()` trick — recommended) |
| `SecurityMaskingDemo` | the incident story; `%s`/`MessageFormat`/concat with leaky vs safe; masking rules per secret type |
