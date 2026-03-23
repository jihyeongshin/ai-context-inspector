---
적용: 항상
---

# V3 Scope - Richer Architecture Guidance

## Goal

V3 defines the richer architecture guidance phase after the accepted V2 baseline.

Primary goal:
- connect existing representative flow, ambiguity, hotspot, rule, and policy outputs to clearer project reading guidance
- help users understand where to read first, where to read cautiously, what can be trusted more strongly, and what should remain provisional
- keep guidance descriptive, defensible, non-blocking, and generic across projects

V3 is not an enforcement phase.
V3 is not a builder retuning phase.
V3 starts as a reading-guidance phase.

Current status:

**V3 guidance MVP completed and accepted**

---

## Why V3 starts with guidance, not stronger enforcement

V2 already established a usable interpretation baseline:
- representative flows are available
- ambiguity is surfaced
- legacy hotspot interpretation is surfaced
- project rules can be loaded
- policy posture can be summarized

What still remains:
- users can see many signals, but they still need help interpreting how to read them together
- current output is informative, but not yet explicit enough about confidence boundaries and reading order
- mixed, weak-signal, or unknown-heavy cases still benefit from better guidance before any stronger rule posture is even considered

Why guidance comes first:
- guidance adds user value without pretending to know more than current extracted evidence supports
- guidance preserves the accepted V2 semantics instead of escalating into hard blocking or destructive judgments
- guidance can remain generic, while enforcement tends to pressure the design toward target-specific assumptions or premature DSL growth

Default judgment:
- before the plugin becomes stricter, it should first become better at explaining what current evidence means and what it does not mean

---

## What V3 reuses from V2

V3 should be built on top of accepted V2 outputs, not by reopening them.

Reusable V2 foundations:
- representative flows
- representative flow ambiguity metadata
- legacy hotspot interpretation
- project rule input
- project rule signals
- policy posture summary

Interpretation rule:
- V3 guidance should consume these as current extracted evidence
- V3 guidance should not reinterpret them as proven architectural truth

Important preservation boundary:
- `RepresentativeFlowBuilder` ordering and scoring remain closed
- classifier, indexer, and policy evaluation logic remain closed
- output semantics from accepted V2 artifacts remain closed

---

## In Scope

- richer architecture guidance derived from existing V2 evidence
- explicit reading guidance about where to start reading a codebase
- explicit caution guidance about where interpretation should stay provisional
- trust-calibration guidance for representative flows and rule signals
- wording that helps users distinguish evidence, interpretation, and confidence
- selective artifact integration for guidance-oriented output

Examples of intended V3 output character:
- descriptive
- defensible
- guidance-oriented
- non-blocking
- generic-project compatible

Examples of candidate V3 content:
- `Reading Guidance Summary`
- `Guidance Signals`
- `How to read this output`
- `What current extracted evidence supports`
- `What current extracted evidence does not yet prove`

---

## Out of Scope

- blocking enforcement
- destructive enforcement
- quick-fix or auto-correction
- stronger rule DSL as a V3 starting point
- target-project anomaly catalogs
- target-project hardcoding
- builder scoring changes
- classifier or indexer retuning
- policy evaluation rewrites
- whole-project architectural verdict engine
- package refactor continuation as the definition of V3

Additional non-goals for this turn:
- model flatness cleanup is not a V3 defining item
- packaging hygiene may be mentioned only as a future maintenance candidate

---

## Core Boundaries

### Guidance is a reading aid, not codebase truth
- V3 guidance must be framed as help for reading current extracted evidence
- V3 guidance must not overclaim certainty beyond extracted signals

### Weak, mixed, and not-applicable states must stay humble
- low-signal outputs should soften interpretation, not intensify it
- unknown-heavy areas should be described as mapping gaps, not proven violations
- `Mixed`, `Weak signal`, or `Not applicable` states should resist strong architectural conclusions

### Genericity comes before target fit
- non-target compatibility remains the default priority
- a pattern that looked useful on `dialodog` must not become a plugin-global assumption without explicit generic review

### V2 closure remains closed
- accepted V2 workstreams are not reopened by default
- V3 must layer on top of V2 outputs rather than redefining them

---

## Candidate User Value

V3 should help users answer:
1. where should I read first?
2. which areas look canonical enough to trust as orientation?
3. which areas look caution-heavy or legacy-sensitive?
4. how much trust should I place in representative flows for this project?
5. which conclusions should I hold loosely because current evidence is weak or ambiguous?

This is the intended value framing:
- better onboarding guidance for humans
- better project-reading guidance for downstream AI
- clearer separation between strong signals and provisional signals

---

## Accepted V3 Guidance MVP Path

### V3 Slice 0: scope framing
- define V3 as richer architecture guidance
- lock in goals, non-goals, and boundaries
- explicitly reject enforcement-first expansion
- accepted

### V3 Slice 1: guidance output prototype
- design a compact guidance summary based on accepted V2 signals
- prototype guidance statements such as:
- `Representative flows indicate...`
- `Rule signals suggest...`
- `Residual ambiguity means...`
- `Unknown-heavy areas should be treated as mapping gaps, not proven violations.`
- accepted

### V3 Slice 2: selected artifact integration
- integrate guidance into one selected artifact first
- accepted integration target:
- `architecture-rules.md`
- accepted as a narrow guidance section rather than a new export file

### V3 Slice 3: wording and readability calibration
- calibrate wording so guidance remains useful without sounding authoritative beyond evidence
- ensure readability across both target and non-target projects
- keep wording stable enough for future validation
- accepted

### Optional future after explicit review
- stronger rule-aware guidance only after V3 guidance wording and boundaries are validated

---

## Candidate Output Areas

### Reading Guidance Summary

Possible content:
- canonical-looking areas
- caution-heavy areas
- low-signal or unknown-heavy interpretation notes
- trust level guidance for representative flows in the current project

### Guidance Signals

Possible content:
- `Representative flows indicate...`
- `Rule signals suggest...`
- `Residual ambiguity means...`
- `Legacy hotspot signals suggest caution around orchestration-heavy paths.`
- `Unknown-heavy areas should be treated as incomplete mapping evidence.`

### Guidance Notes for exports

Possible integration:
- add a `How to read this output` section to `architecture-rules.md`
- add a guidance-oriented summary section where current evidence can be interpreted safely
- avoid expanding every artifact at once unless a narrow integration proves useful first

---

## Sequencing

Accepted path completed:
1. scope framing
2. guidance prototype
3. narrow artifact integration
4. wording and readability calibration

Current status:

**V3 guidance MVP completed and accepted**

Next review gate:

**Only after this accepted MVP should stronger rule-aware guidance be reviewed**

Default sequencing discipline remains:
- scope before output
- output before expansion
- wording calibration before any harder posture

---

## Validation Direction

Accepted validation confirmed that:
- the new guidance helped reading without claiming certainty the engine did not have
- non-target compatibility was preserved
- accepted V2 semantics remained closed
- ambiguity, hotspots, unknowns, and rule posture were explained more usefully than raw summaries alone

Accepted success indicators:
- users can see what to read first
- users can see what to treat cautiously
- users can distinguish stronger signals from provisional signals
- guidance remained compatible with low-signal and non-target projects

Failure indicators:
- guidance reads like architectural truth instead of evidence-based interpretation
- wording becomes target-specific
- guidance pressures hidden builder or classifier changes
- stronger posture appears without explicit scope review

---

## Risks

- guidance wording can still overclaim if confidence boundaries are not kept explicit
- guidance may drift into verdict language if policy posture is treated too strongly
- artifact expansion can become noisy if all outputs are modified at once
- target-project familiarity can quietly leak into plugin-global assumptions

Mitigation:
- keep the first prototype compact
- preserve explicit uncertainty wording
- validate on non-target projects before broadening wording claims

---

## Immediate Scope Judgment

V3 priority should now be fixed as:

**Richer architecture guidance first**

Not:
- stronger enforcement first
- stronger DSL first
- builder retuning first
- target-specific heuristics first

---

## End
