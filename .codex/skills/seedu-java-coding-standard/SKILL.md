---
name: seedu-java-coding-standard
description: Apply the SE-EDU basic and intermediate Java coding standard when writing, editing, or reviewing Java production code, tests, and fixtures in this project.
---

# SE-EDU Java coding standard

Read and follow the full [basic + intermediate standard](https://se-education.org/guides/conventions/java/intermediate.html)
when applying this skill. Use its linked Google Java guide for uncovered topics; SE-EDU rules take precedence.
Do not silently substitute the advanced version. If the source is unavailable, use the checklist below for
known rules and report any unresolved question rather than claiming a complete compliance review.

## Review checklist

- Use lowercase project packages, PascalCase type names, camelCase variables and verb-based methods,
  uppercase underscore-separated constants, English names, natural boolean names, and plural collections.
- Use four spaces, eight extra spaces for continuation lines, K&R braces, and at most 120 columns
  (prefer under 110). Break after commas and before operators; separate logical units with blank lines.
- Package every class; use explicit, consistently ordered imports. Attach array brackets to types.
- Initialize variables in the smallest practical scope; encapsulate fields. Brace loops and conditionals.
  Indent switch cases and document intentional fallthrough.
- Write American-English comments. Document classes and public methods with Javadoc, respecting the
  source's accessor, override, and test exceptions. Use summary sentences, punctuated tags, and aligned stars.
- Test names may use feature_scenario_result, with trailing parts omitted when appropriate.

## Project application

Apply this to all maintained Java, including `src/main/java` and `src/test/java`; do not edit generated outputs.
The root `AGENTS.md` additionally requires class documentation, even for test helpers.
Use explicit static imports first, then Java platform imports, third-party imports, and `ekko` imports,
with a blank line between groups and alphabetical ordering within each group. This is the project's
chosen consistent ordering, not an additional upstream requirement.

For a standards-only cleanup, preserve console strings, persistence formats, and runtime behavior.
Update all callers and tests when renaming an identifier. Review the diff for accidental semantic changes.
Follow `AGENTS.md` for Java 25, risk-based JUnit coverage, and the complete Gradle test run. Do not turn
style checks into JUnit tests or make tests depend on this skill or its website. Explain when existing
behavioral tests suffice. Do not commit or push without the user's authorization.
