---
name: seedu-git-standard
description: Apply SE-EDU Git conventions when proposing, creating, amending, or reviewing commit messages and naming branches in this project.
---

# SE-EDU Git standard

Apply the [SE-EDU Git conventions](https://se-education.org/guides/conventions/git.html)
to all future commits and branch names in this repository. The checklist below summarizes
the source; consult it for examples or ambiguities. Keep the repository's authorization
and integration rules in `AGENTS.md`: this skill does not authorize commits or pushes.

## Commit messages

- Write an imperative subject with an initial capital and no final period.
  Aim for 50 characters; never exceed 72. An applicable scope or category prefix is optional.
- For nontrivial changes, add a body after an empty line. Keep body lines within
  72 columns, separate paragraphs with empty lines, and use bullets when helpful.
- Explain the change and its motivation rather than narrating implementation details
  or duplicating comments. Provide enough rationale to assess the change independently
  of the diff. Consider smaller commits if the explanation becomes unwieldy.
- Organize the body around the existing situation (present tense), the need for change,
  the action (imperative), reasons for that approach, and other relevant information.
  Omit redundant time qualifiers such as “currently”.

## Branch names

- Choose descriptive, hyphen-separated lowercase keywords.
- For issue work, start the descriptive name with the issue number, for example
  `1234-ui-freeze-error`.
- Local convention: retain the configured `codex/` prefix unless the user specifies
  otherwise, for example `codex/1234-ui-freeze-error`.

## Before a commit

Read `AGENTS.md`, inspect the intended diff, and check the proposed message against
this skill, including amendments and rewritten messages. Preserve unrelated work.
Follow the project's existing testing, authorization, and linear-history requirements.
