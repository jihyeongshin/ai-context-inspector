---
적용: 항상
---

# Plugin Rules – ai-context-inspector

These are the rules of the **ai-context-inspector project itself**.

They are **not** the rules of Dialodog or any target project being analyzed.

---

## Core identity

ai-context-inspector is a **generic IntelliJ plugin** for Java projects.

Purpose:
- extract structured AI-readable context from code
- help AI understand large codebases
- preserve architecture-aware project understanding
- generate persistent context artifacts

This plugin must remain **generic**.

---

## Rule 1. Do not hardcode target-project bad cases as global truth

Dialodog inspired this project, but Dialodog-specific edge cases must not be turned into plugin-global behavior.

Examples of forbidden design drift:
- naming one project’s legacy response object pattern as a universal rule
- treating one project’s mapper/service conventions as always true
- baking in one project’s domain anomalies as plugin defaults

Project-specific interpretation may appear in:
- traits
- affinity
- exported explanation text

But must not distort the plugin’s global classification truth.

---

## Rule 2. Preserve the 3-layer interpretation model

The current interpretation model has 3 layers and should be preserved.

### classRole
Human-readable surface role

### architectureAffinity
Architecture-oriented grouping

### structuralTraits
Structure-based interpretation

Do not collapse these layers into one.
Do not overuse classRole for everything.

---

## Rule 3. Keep representative flow architecture-aware

Representative flow is **not** a raw method call trace.

It must remain a **representative architecture chain**.

Preferred flow thinking:
- Controller -> Facade / UseCase / Service
- Facade -> UseCase / Service
- UseCase -> Service / Repository-like
- Service -> Repository-like

Do not allow support noise to dominate the main representative flow.

---

## Rule 4. Prefer structure over naming when meaning is ambiguous

Names are helpful, but not sufficient for all cases.

When ambiguous:
- use inheritance
- use traits
- use package signals carefully
- use role + affinity + traits together

Examples:
- entity-backed wrappers
- bootstrap classes
- constant-holder classes
- external adapter-like structures

---

## Rule 5. Keep file-level inspection alive

Even after project-wide indexing exists, single-file inspection must remain.

Reason:
- it is the best debugging window
- it detects annotation/environment issues quickly
- it helps validate extractor correctness

Project-wide view is not a replacement for file-level inspection.

---

## Rule 6. Plugin output must support AI consumption

V1 is not just a preview tool.
It must become a project-level AI context generator.

Target output direction:
- project-wide context
- representative flows
- persistent artifacts in `.aiassistant/context/`

---

## Rule 7. Do not chase zero Unknown Role at all costs

Unknown Role is acceptable if:
- architecture affinity is already understood
- traits already explain the structure
- forcing a new role would reduce generic quality

Unknown Affinity is more critical than Unknown Role.

---

## Rule 8. Browser and IDE roles are different

### Browser
Used for:
- long-term strategy
- architecture reasoning
- context compression
- session reset / handoff

### IDE
Used for:
- implementation
- refactoring
- local execution
- project-wide indexing
- later-stage cleanup

Do not confuse the two roles.

---

## Rule 9. Dialodog documents are references, not plugin rules

Dialodog files must stay isolated as target-project references.

They must not be interpreted as:
- plugin-global architecture truth
- plugin-global migration rules
- plugin-global API policy

---

## Rule 10. V1 scope discipline

V1 includes:
- project-wide indexing
- role / affinity / trait classification
- representative flow generation
- artifact export

V1 does not include:
- hard blocking
- destructive enforcement
- project-specific lock-in logic
- full rule DSL

---

## End