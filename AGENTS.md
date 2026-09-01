# Project context

This repository is a starter template for a greenfield Java project used in an introductory software engineering course in an undergraduate computer science program. Students use it as the starting point for their own projects.

# Default user context

Unless the user says otherwise, assume that you are assisting a student working on a project in this repository. If the user identifies themselves as an instructor or another project stakeholder, adapt your response to that role.

# Student profile

* Prior knowledge: Basic Java and OOP concepts.
* Level of programming experience: Intermediate
* IDE and level of expertise: IntelliJ IDEA, intermediate

# Guidance for interacting with users

* Explain the rationale for significant actions: what you did and why.
* Keep explanations brief but instructive, supporting learning through responsible use of AI. For example:

  * When suggesting a Git command, briefly explain what it does.
  * Add explanatory Javadoc comments to all classes and to nontrivial methods and fields when their purpose or behavior is not obvious.
  * Make generated code as self-explanatory as possible, and include explanatory comments where they improve understanding.
  * When faced with a design choice, choose the simplest option that is sufficient for the requirements, while briefly explaining relevant more advanced alternatives.

# Project-specific requirements

## Coding standard

All Java code in this project, including production code, tests, and test fixtures, must follow the
`seedu-java-coding-standard` skill at `.codex/skills/seedu-java-coding-standard/SKILL.md`.
Read and apply that skill before writing, modifying, or reviewing Java code. It adopts the SE-EDU
basic and intermediate Java standard and defines this project's import ordering. Keep existing
behavior unchanged during style-only updates, and retain the documentation and testing requirements below.

## Java version:

Ensure that Java 25 is used when running the application or build tasks. On macOS, use `sdk use java 25.0.3.fx-zulu` to switch to Java 25 if needed.

## Testing

JUnit coverage target: focus tests on approximately the top 50% highest-value methods in the codebase, prioritizing complex, core, or critical business logic. This is a risk-based method-selection target, not a strict cap or a measured line-coverage percentage. Cover reasonable normal, boundary, and invalid-input cases for selected methods; avoid adding tests for trivial accessors just to increase counts. Keep JUnit tests under `src/test/java` in the corresponding production package, with test classes named `<ClassName>Test`.

After every code update:

1. Review and update JUnit tests as needed to comply with the approximately 50% highest-value-method coverage target. Add or revise tests for affected high-value methods, including regression cases for bug fixes. If no JUnit changes are needed, explain why.
2. Run the complete JUnit suite through Gradle using Java 25 (`./gradlew test`, or `.\gradlew.bat test` on Windows).

JUnit is the authoritative automated regression suite. Keep its test cases and fixtures self-contained under `src/test`; do not load expected behavior from documentation or invoke an LLM/skill as part of JUnit execution. Cover user-visible behavior changes with JUnit tests as well as core logic.

## Git

All future commits and branch names must follow the `seedu-git-standard` skill at
`.codex/skills/seedu-git-standard/SKILL.md`, based on https://se-education.org/guides/conventions/git.html.
Read and apply this skill before proposing, creating, amending, or reviewing commit messages
and before naming branches.
Use lightweight tags unless the user requests an annotated tag.
When proposing or creating a commit message, include enough detail to explain the rationale for the change.
Do not commit or push unless explicitly asked.
Commit authorized changes directly to the `master` branch with a relevant commit message.
Do not create feature branches unless the user explicitly requests one.
Avoid merge commits unless the user explicitly requests one.
Do not force-push unless explicitly asked; when necessary, prefer `--force-with-lease` over `--force`.
