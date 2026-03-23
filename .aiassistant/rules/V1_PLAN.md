---
apply: always
---

# V1 Plan - ai-context-inspector

## V1 status

V1 baseline is complete.
V1.1 follow-up polish is also complete.

Completion confirmation date:
- 2026-03-20

Completion basis:
- project-wide indexing is stable enough for V1
- representative flows are generated
- `.aiassistant/context/` artifacts are exported
- preview and generate actions are separated
- sandbox validation succeeded on `dialodog`
- V1.1 artifact quality tasks completed without reopening engine design
- representative flow edge-case review completed as no-op success

---

## V1 definition

V1 means:

**ai-context-inspector acts as a project-level AI context generator inside IntelliJ**

Core path:

Source code
-> PSI analysis
-> classification and context extraction
-> project-wide index
-> representative flows
-> `.aiassistant/context/*`

---

## Completed V1 scope

### 1. Project-wide indexing
- [x] scan project Java files
- [x] build project-level context snapshot

### 2. Classification layers
- [x] `classRole`
- [x] `architectureAffinity`
- [x] `structuralTraits`

### 3. Project debug preview
- [x] role counts
- [x] affinity counts
- [x] trait counts
- [x] unknown samples
- [x] project-level debug visibility

### 4. Representative flow generation
- [x] architecture-aware representative flows
- [x] project-level flow aggregation
- [x] duplicate reduction
- [x] noise suppression

### 5. Artifact export
- [x] export under `.aiassistant/context/`
- [x] `project-structure.md`
- [x] `entrypoints.md`
- [x] `representative-flows.md`
- [x] `architecture-rules.md`

### 6. Action separation
- [x] `Preview Project Context`
- [x] `Generate Project AI Context`
- [x] preview and export responsibilities separated

### 7. Baseline validation
- [x] sandbox runtime validation completed
- [x] action visibility confirmed from menu and search
- [x] generated artifact set confirmed
- [x] preview and exported flow outputs confirmed consistent

---

## Explicitly out of V1 scope

- hard blocking of code changes
- destructive enforcement
- project-specific hardcoded truth
- full generic rule DSL
- full architecture guard system

These remain out of scope for V1 baseline.

---

## Follow-up status after V1

These are not V1 blockers.

### Completed V1.1 follow-up
- [x] artifact markdown quality improvement
- [x] project summary readability improvement
- [x] unknown role group naming polish
- [x] export artifact schema expansion
- [x] representative flow edge-case refinement review completed as no-op success
- [x] V2 has started and its first slice is completed
- [x] V2 multi-purpose entry point interpretation and legacy hotspot interpretation are completed
- [x] V2 ambiguous flow interpretation refinement is completed
- [x] V2 interpretation and metadata expansion review is completed
- [x] V2 project rule ingestion is completed
- [x] V2 stronger policy layer is completed
- [x] V2 validation evidence is complete across no-file, valid-file, malformed-file, and non-target project runs

### Carried forward beyond V2 baseline
- richer architecture interpretation output as a future candidate only
- rule-aware guidance or guardrail direction as a future candidate only

---

## Priority after V1

Next priority:

**Keep the accepted V2 baseline stable, maintain wording quality, and define optional V3 candidates without reopening accepted workstreams**

After that:
- treat confidence and ambiguity metadata, multi-purpose entry point interpretation, legacy hotspot interpretation, ambiguity interpretation refinement, rule ingestion, and stronger policy output as accepted V2 baselines
- keep builder changes off by default unless a generic misleading case is proven
- prioritize maintenance and explicit future scoping over ad hoc tie-break tuning
- consider future policy or guardrail directions only after baseline stability remains intact

---

## Completion judgment

V1 baseline should now be treated as:

**completed and accepted**

V1.1 follow-up should now be treated as:

**completed and accepted**

---

## End
