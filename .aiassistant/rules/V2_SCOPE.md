---
적용: 항상
---

# V2 Scope - ai-context-inspector

## Goal

V2 defines the next phase after the accepted V1 baseline and V1.1 polish.

Primary goal:
- evolve the plugin from descriptive export toward confidence-aware interpretation and policy layering
- preserve the generic marketplace plugin direction rather than drifting into target-project behavior
- add richer meaning to representative flows and project rules without defaulting to builder scoring rewrites

V2 is not a renderer polish phase.
It is an interpretation and policy layering phase.

---

## Why V2 exists

V1 and V1.1 proved that project-level context export works and that the exported markdown can be made readable and stable.

What still remains:
- representative flows can be plausible but ambiguous in multi-purpose or legacy-heavy codebases
- the system needed to surface confidence or ambiguity explicitly
- project-specific rules needed to be ingestible as generic project input rather than informal notes
- downstream AI needed stronger but still defensible policy summaries

V2 exists to close those gaps without collapsing into hardcoded project logic.

---

## In Scope

- confidence or ambiguity metadata for representative flows
- ambiguous representative flow handling
- multi-purpose entry point interpretation
- legacy orchestration hotspot interpretation
- low-confidence representative flow signaling
- project rule ingestion
- stronger policy layer

Interpretation of scope:
- add interpretation metadata only when it is generic and defensible
- express uncertainty explicitly rather than hiding it behind forced single-path certainty
- treat rule ingestion as a project input model, not as target-specific hardcoding
- strengthen policy interpretation on top of existing extraction and flow generation rather than replacing them by default

---

## Out of Scope

- large `RepresentativeFlowBuilder` scoring rewrite as a default V2 starting point
- classifier overhaul as a default V2 starting point
- target-project hardcoded heuristics
- one-off tie-break tuning justified only by a single project
- renderer-only polish work as the main definition of V2
- blocking guardrails or destructive enforcement
- marketplace direction changes away from a generic plugin

Default boundary:
- if a builder change becomes necessary, it should be justified by generic interpretation evidence, not by isolated flow discomfort

---

## Current V2 status

V2 has started and its scoped workstreams are accepted.

Accepted completion:
- V2 Slice 1: confidence and ambiguity metadata for representative flows
- V2 workstream: ambiguous flow interpretation refinement
- V2 workstream: multi-purpose entry point interpretation
- V2 workstream: legacy orchestration hotspot interpretation
- V2 workstream: interpretation and metadata expansion review for other artifacts
- V2 workstream: rule ingestion foundation
- V2 workstream: stronger policy layer preparation

Accepted slice contents:
- metadata model added
- evaluator added and calibrated
- debug preview wiring accepted
- `representative-flows.md` export integration accepted
- entry point interpretation accepted
- legacy hotspot interpretation accepted
- ambiguity interpretation refinement accepted
- accepted interpretation layers expanded into exported `representative-flows.md`
- project rule input accepted as a generic project input model
- stronger policy output accepted in preview and exported `architecture-rules.md`

Accepted boundaries:
- `RepresentativeFlowBuilder` ordering and scoring were preserved
- classifier and indexer were not reopened
- accepted interpretation and policy workstreams were added without reopening builder, classifier, or indexer logic

Residual notes after accepted V2 workstreams:
- `PetTicket` still shows residual ambiguity
- `User`, `Chat`, and `Root` retain multi-purpose ambiguity signals
- `Root -> UserService -> UserMapper` remains plausible but not definitive
- export expansion remains intentionally selective rather than pushed into every artifact
- `Schedule` is better treated as a legacy hotspot interpretation issue than as a competition issue

Current status judgment:
- V2 Slice 1 is closed
- confidence and ambiguity metadata is accepted
- ambiguous flow interpretation is accepted
- multi-purpose entry point interpretation is accepted
- legacy hotspot interpretation is accepted
- rule ingestion foundation is accepted
- stronger policy layer is accepted
- V2 genericity evidence now includes `gs-rest-service`, `spring-petclinic`, and `quickstart-projects`

---

## Validation Strategy

Validation should prove that V2 adds interpretation value without collapsing existing flow quality.

### Functional validation
- run `Preview Project Context`
- run `Generate Project AI Context`
- confirm export still succeeds under `.aiassistant/context/`
- confirm existing artifact counts and representative flow totals remain stable unless intentionally changed

### Interpretation validation
- verify that ambiguous or low-confidence cases are surfaced explicitly
- verify that already-good representative flows remain readable and are not downgraded unnecessarily
- verify that multi-purpose entry points and legacy hotspots are explained more accurately than in V1
- verify that residual `Possible` cases are explained more specifically than a generic ambiguity label
- verify that accepted interpretation layers can be expanded into selected artifacts without harming markdown readability
- verify that summary labels clearly distinguish per-flow counts from distinct entry point counts

### Policy validation
- verify that ingested project rules can be expressed without target-specific hardcoding
- verify that stronger policy output is grounded in extracted structure plus rule input
- verify that policy statements stay descriptive and defensible rather than overclaiming certainty

### Current validation state
- no-file validation: accepted
- valid-file validation: accepted
- malformed-file live validation: accepted
- non-target Java project validation: accepted

### Validation evidence notes
- `gs-rest-service` confirmed low-signal no-op behavior with `Not applicable` policy summaries
- `spring-petclinic` confirmed non-target layered behavior with defensible `Mixed` policy conclusions
- `quickstart-projects` confirmed genericity under unknown-heavy stress with softened low-confidence output

---

## Next selectable priorities

The next priority is now maintenance-oriented.

Recommended candidates:
1. final wording polish and maintenance
2. optional future policy hardening
3. candidate V3 planning

---

## Risks

- confidence metadata can become noisy if future expansions stop staying compact and stable
- policy layering can still overclaim correctness if future rules and extracted signals disagree
- legacy hotspot handling can drift into target-specific heuristics if future extensions stop using generic signals
- project rule ingestion can become premature DSL design if the input model grows beyond the current lightweight YAML scope
- ambiguity-aware output can confuse users if future additions weaken current wording discipline

Resolved or materially reduced risks:
- live malformed-file handling is now proven without blocking preview or generate
- non-target validation is now proven across both low-signal and stress-test projects

---

## Candidate V3

V3 is candidate-only at this point.

Possible directions:
- optional future policy hardening
- optional richer architecture guidance after explicit scope review
- optional rule-aware guidance direction after the accepted V2 baseline remains stable

---

## End
