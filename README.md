# AI Context Inspector

> **Status: Archived / End of Life**

AI Context Inspector was created in early 2026 to help AI coding agents understand architectural intent in large Java/Spring legacy codebases.

At the time, coding agents could easily misclassify legacy service-like classes as application services based on surface-level signals such as class names, Spring annotations, transaction boundaries, or the presence of business logic.

The project attempted to reduce those errors by extracting additional architectural context from the codebase and providing that context to AI-assisted development workflows.

## Why this project is archived

The original problem has become significantly less important over time.

Several changes contributed to this:

* Modern coding models are substantially better at reasoning across repository structure, call relationships, persistence boundaries, and legacy design constraints.
* Architectural intent, domain boundaries, migration rules, and engineering policies are increasingly maintained as explicit repository-level context rather than generated as separate analysis artifacts.
* AI-assisted development workflows now include stronger review, audit, and change-scope controls.
* Legacy systems are progressively being decomposed or migrated toward clearer service boundaries, reducing the need for heuristic architectural interpretation.
* In practice, current AI coding agents can usually inspect the source directly without requiring manually generated context artifacts from this plugin.

Because of these changes, maintaining a separate IntelliJ-based context extraction layer no longer provides enough value to justify continued development.

## Historical context

One of the original motivations for this project was a recurring ambiguity in legacy Java/Spring systems.

A class could:

* be named `*Service`,
* use `@Service`,
* define transaction boundaries,
* contain business logic,

while still not represent an application service in the intended architecture.

Such classes may instead belong to a legacy persistence or integration boundary where business logic accumulated over time as technical debt.

AI coding agents previously tended to infer architectural roles too strongly from these surface-level characteristics.

AI Context Inspector was an attempt to supply additional context so that generated changes and code audits could distinguish between:

* the architecture currently expressed by legacy code, and
* the architecture the system was actually intended to move toward.

That distinction remains important, but it no longer requires a dedicated context-generation plugin.

## Project status

No further feature development is planned.

The repository is retained as a historical reference for an earlier stage of AI-assisted software development and repository-context tooling.

The project should be considered **archived and unsupported**.
