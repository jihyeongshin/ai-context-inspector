---
적용: 항상
---

# Current Context - ai-context-inspector

## Current version position

V1 baseline is now considered complete.
V1.1 polish is now considered complete.
V2 has now started.
V2 Slice 1 is now considered complete and accepted.
V2 multi-purpose entry point interpretation is now considered complete and accepted.
V2 legacy orchestration hotspot interpretation is now considered complete and accepted.
V2 ambiguous flow interpretation refinement is now considered complete and accepted.
V2 interpretation and metadata expansion review is now considered complete and accepted.
V2 rule ingestion foundation is now considered complete and accepted.
V2 stronger policy layer preparation is now considered complete and accepted.
V2 no-file validation is accepted.
V2 valid-file validation is accepted.
V2 malformed-file live validation is accepted.
V2 non-target Java project validation is accepted.
V3 scope framing is accepted.
V3 Slice 1 guidance output prototype is accepted.
V3 Slice 2 narrow `architecture-rules.md` guidance integration is accepted.
V3 Slice 3 wording/readability calibration is accepted.
V3 guidance MVP is accepted.
Post-MVP Enhancement 1 - Raw Input Boundary Guidance is accepted.
Post-MVP Enhancement 2 - Parser Visibility is accepted.

Completion confirmation dates:
- 2026-03-22 for the accepted V3 guidance MVP baseline
- 2026-03-26 for the baseline maintenance refresh after accepted PM1 and PM2 closure

Validation basis:
- `Generate Project AI Context` was confirmed working in IDE sandbox
- `Generate Project AI Context` was confirmed visible from menu and search
- export output was generated under `.aiassistant/context/`
- the four markdown artifacts were generated successfully
- `project-structure.md`, `entrypoints.md`, `representative-flows.md`, and `architecture-rules.md` were mutually consistent
- representative flow output was consistent with `Preview Project Context`
- artifact markdown quality improvement was completed
- project summary readability improvement was completed
- unknown role group naming polish was completed
- export artifact schema expansion was completed
- representative flow edge-case refinement review completed as no-op success
- V2 Slice 1 confidence and ambiguity metadata was validated in debug preview
- V2 Slice 1 confidence and ambiguity metadata was validated in exported `representative-flows.md`
- multi-purpose entry point interpretation was validated in debug preview
- legacy orchestration hotspot interpretation was validated in debug preview
- ambiguous flow interpretation refinement was validated in debug preview
- accepted interpretation layers were validated in exported `representative-flows.md`
- project rule ingestion foundation was validated in no-file and valid-file paths
- stronger policy layer preparation was validated through preview and exported `architecture-rules.md`
- malformed rule file handling was validated live without blocking preview or generate
- non-target genericity evidence was validated on `gs-rest-service`, `spring-petclinic`, and `quickstart-projects`

Adopted V1 baseline decision:
- `RepresentativeFlowBuilder` is accepted as the V1 baseline implementation
- export and action separation is accepted as the V1 baseline implementation
- V1.1 completed without reopening classifier, builder, or indexer logic

---

## Completed V1 baseline components

### Stable extraction and inspection
- PSI-based extraction
- file metadata extraction
- annotation extraction
- method extraction
- field extraction
- import extraction
- endpoint extraction
- dependency extraction
- single-file inspection and preview dialog

### Classification and interpretation layers
- `classRole`
- `architectureAffinity`
- `structuralTraits`
- trait-aware interpretation of ambiguous classes
- unknown affinity reduction to an acceptable V1 level

### Project-level context generation
- project-wide indexing
- `ProjectContextSnapshot`
- project debug rendering
- representative flow generation
- project artifact rendering
- export to `.aiassistant/context/`

### Action structure
- `Preview Project Context`
- `Generate Project AI Context`
- preview and generate responsibilities separated

---

## Completed V1.1 polish components

### Export readability and structure
- artifact markdown heading and section ordering improvement
- project summary readability improvement
- wording consistency across exported markdown artifacts

### Export metadata polish
- compact schema expansion for stable summary metadata
- entry point coverage and endpoint density summary
- representative flow summary metadata and terminal role distribution

### Unknown wording polish
- unknown role group naming adjusted to unclassified wording
- preview and export wording aligned

### Representative flow review
- representative flow edge cases were reviewed against `dialodog`
- no builder bug requiring V1.1 refinement was confirmed
- `Schedule`, `Chat`, and `Root` cases were recorded as ambiguity or legacy structure signals, not V1.1 defects

---

## Current product judgment

The project now meets the V1 baseline definition, the planned V1.1 polish scope, the accepted V2 workstreams, the accepted V3 guidance MVP, and the accepted narrow post-MVP enhancements:
- project-level AI-readable context can be generated from code
- representative flows are available at project level
- persistent markdown artifacts are exported for downstream AI use
- the implementation remains generic rather than hardcoded for one target project
- exported markdown is more readable and more metadata-rich without changing engine behavior
- representative flow confidence and ambiguity metadata is available in debug preview and exported `representative-flows.md`
- multi-purpose entry point interpretation is accepted
- legacy orchestration hotspot interpretation is accepted
- ambiguity interpretation refinement is accepted
- accepted interpretation layers are surfaced in exported `representative-flows.md`
- project rule input can be loaded as a generic project input model without blocking preview or generate
- stronger policy layer output is available in preview and exported `architecture-rules.md`
- no-file, valid-file, malformed-file, and non-target validation evidence are accepted
- V3 scope framing is accepted
- V3 Slice 1 guidance output prototype is accepted
- V3 Slice 2 narrow `architecture-rules.md` guidance integration is accepted
- V3 Slice 3 wording/readability calibration is accepted
- V3 guidance MVP is accepted as a reading-aid layer on top of accepted V2 evidence
- Post-MVP Enhancement 1 - Raw Input Boundary Guidance is accepted
- Post-MVP Enhancement 2 - Parser Visibility is accepted
- accepted post-MVP narrow enhancements remain closed
- guidance remains descriptive and defensible rather than a codebase-truth engine

Important judgment:
- accepted V2 baseline remains closed unless future generic review justifies reopening
- accepted V3 guidance MVP remains closed unless future generic review justifies reopening
- accepted post-MVP enhancement workstreams remain closed
- no new active enhancement workstream is open

---

## Remaining non-blocking issues

These do not block the current accepted V1/V2/V3 baseline:
- parser-bearing non-target external validation remains deferred and non-blocking
- optional future stronger rule-aware guidance remains review-only
- optional future parser-bearing external evidence can be collected if naturally available

---

## Next version candidates

### Current maintenance
- documentation stability and baseline maintenance
- keep the accepted V2/V3 baseline stable

### Optional future review only
- optional rule-aware guidance after explicit review
- optional broader guidance expansion only after clear value review
- optional future policy hardening only after explicit review

Accepted V2 workstreams:
- representative flow confidence and ambiguity metadata
- multi-purpose entry point interpretation
- legacy orchestration hotspot interpretation
- ambiguous flow interpretation refinement
- interpretation and metadata expansion review for other artifacts
- rule ingestion foundation
- stronger policy layer preparation

---

## Current operating model

### Browser command
- long strategic discussion
- session compression
- version framing and acceptance judgment

### IDE runner
- implementation
- refactoring
- preview and export execution
- project indexing and artifact generation

### Human
- runs live Preview and Generate in target and non-target projects
- provides exported artifacts and acceptance evidence

---

## Immediate next task

The next task is:

**Keep the accepted baseline stable and only review future enhancement candidates when repeated generic evidence justifies reopening work**

---

## End
