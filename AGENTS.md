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

## Java version:

Ensure that Java 25 is used when running the application or build tasks. On macOS, use `sdk use java 25.0.3.fx-zulu` to switch to Java 25 if needed.

## Testing

After every code update:

1. Review `test/ui-test-plan.md` and update its test cases, expected output, and test-design reasons when the code change affects or adds user-visible behavior.
2. Invoke the `run-ekko-ui-tests` skill to run the complete UI test plan, even when no update to the test plan is needed.

## Git

Follow the commit message and branch naming conventions at https://se-education.org/guides/conventions/git.html.
Use lightweight tags unless the user requests an annotated tag.
When proposing or creating a commit message, include enough detail to explain the rationale for the change.
Do not commit or push unless explicitly asked.
Changes to `AGENTS.md` may be committed directly to the main branch with a relevant commit message.
Develop changes on a feature branch rather than directly on the main branch.
Before integrating a feature branch, rebase it onto the latest main branch to maintain a linear history.
Avoid merge commits unless the user explicitly requests one.
Do not force-push unless explicitly asked; when necessary, prefer `--force-with-lease` over `--force`.
Delete feature branches after they have been merged.
