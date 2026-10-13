# TEACHING_STYLE.md — the EVA clarity rubric for teaching material

Applies to every teaching repo (`core-java`, `collections-framework`, `java8-features`,
`advanced-java`): module READMEs, demo output, code comments, test names.

The rule behind all rules: **a student reading one page, once, at 9 a.m., with no
rewind, must understand it.** If a sentence needs re-reading, the sentence is wrong.

## 1. Sentences

- One idea per sentence. If a sentence has two commas and a dash, split it.
- Prefer ~25 words or fewer. Long clauses become numbered lists.
- Active voice: "the iterator copies modCount", not "modCount is copied by the iterator".
- No sentence may depend on a pronoun whose referent is more than one sentence back.

## 2. Jargon

- Define every term at first use, in the same sentence, in plain words:
  > *erasure* — the compiler checks `<String>` then throws it away; the class file never sees it.
- Never use an acronym before spelling it out once (CME, JMM, GC, PECS, MXBean...).
- If a term has a second everyday meaning ("monitor", "proxy", "pool"), say which one we mean.

## 3. Order of introduction (every concept, every module)

1. **Plain sentence** — what it is, no jargon. ("A wildcard is a type parameter that stands for *some one* type we don't name.")
2. **Why it exists** — the problem it solves, ideally as a tiny failure the student can feel.
3. **Analogy** — banking/SecureBank first, everyday life second. One analogy per concept; do not mix analogies.
4. **Diagram** — mermaid, then "Read the diagram:" bullets (section 4).
5. **Numbered mechanism walkthrough** — "What happens when you call `map.put(k, v)`:" 1. 2. 3. Every runtime mechanism gets one. This is the single highest-value clarity device; never skip it.
6. **Code** — the demo that prints the walkthrough live.
7. **Trap** — what breaks if you misread the model, with the exact exception name.
8. **In one sentence:** — the takeaway box that closes the section.

Not every section needs all eight. Steps 1, 5, and 8 are mandatory everywhere.

## 4. Diagrams

- Every mermaid diagram is immediately followed by a **"Read the diagram:"** bullet list
  that translates it into plain sentences, top to bottom.
- Node labels: complete short phrases, ≤ 8 words. Put the detail in the bullets below, not the node.
- One diagram = one idea. If a diagram needs a legend, split it in two.
- Verify the mermaid actually renders (balanced quotes/brackets, `<br/>` for line breaks).

## 5. Tables

- Cells are complete words, not fragments with arrows: "returns the old value", not "old val -> ret".
- One comparison axis per table (Array vs List — not Array vs List vs Set vs era-of-Java).
- The "table to photograph" is the ONE table a student should memorize; keep it ≤ 7 rows.

## 6. Demo output

- Each demo proves ONE claim; the banner states the claim as a full sentence.
- Print numbered steps for anything sequential: `step 1: ...`, `step 2: ...`.
- Print the verdict, not just the data: `expected 100000, actual 73412 -> updates LOST`.
- Caught exceptions print as `CAUGHT -> SimpleName: message` followed by one line saying WHY it happened.

## 7. Code comments

- Comments explain WHY and connect code to the README diagram ("banner 3 of the README, step 2 happens on this line").
- A comment may never merely restate the code. If it does, delete it or upgrade it to a WHY.
- At the top of each class: what it proves, and which README section it belongs to.

## 8. Tests

- `@DisplayName` is a full-sentence claim: "erasure: List<String> and List<Integer> are the same Class at runtime".
- Each test asserts one claim and prints its evidence line.
- Determinism: assert only what the spec guarantees; races get start-guns and joins;
  timings are printed, never asserted (except generous blocking floors, clearly commented).

## 9. Plain-language self-check (run it on every page you write)

- Could a student define every bolded term from this page alone?
- Is every diagram followed by "Read the diagram:" bullets?
- Does every mechanism have a numbered walkthrough?
- Does every long section end with "In one sentence:"?
- Read the page aloud in your head. Anywhere you stumble, rewrite that sentence.

## 10. Polish-pass rules (when improving EXISTING material)

- Keep every technical claim exactly right. Clarity edits never change facts, numbers,
  exception names, or behavior.
- Keep the section structure and headings a lesson plan depends on
  (big idea, diagrams, table to photograph, traps, class flow, demo/test tables).
- Fix, in priority order: (1) run-on sentences, (2) undefined jargon, (3) diagrams
  without "Read the diagram:" bullets, (4) mechanisms without numbered walkthroughs,
  (5) fragment-y table cells, (6) missing "In one sentence:" takeaways.
- Do not pad. A clearer page is usually not longer than ~120% of the original.
- If you edit anything under `src/`, the module must still compile and pass: re-run its tests.
