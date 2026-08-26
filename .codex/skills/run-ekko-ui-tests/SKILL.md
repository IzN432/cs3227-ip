---
name: run-ekko-ui-tests
description: Run and report the repository's Markdown-defined Ekko command-line UI test plan. Use when verifying Ekko UI behaviour or checking test/ui-test-plan.md after implementation changes.
---

# Run Ekko UI tests

Use `test/ui-test-plan.md` as the source of truth for test inputs, expected normalized outputs, and test-design reasons.

## Process

1. Confirm that `java -version` and `javac -version` report Java 25. Stop and report the mismatch if they do not.
2. Run `.codex/skills/run-ekko-ui-tests/scripts/run-ui-tests.ps1` from the repository root.
3. Do not continue to later cases after a failure. Preserve the runner's expected and actual output in the report so the first behavioural difference is immediately visible.
4. On success, report the number of passing cases. The runner must display each case's exact input and normalized actual output as its execution record.
5. When behaviour intentionally changes, update the relevant expected output and reason in `test/ui-test-plan.md`, then rerun the entire plan. Do not change expectations merely to hide an unexplained regression.

The runner deliberately ignores only stable UI chrome: the banner, greeting, blank lines, and separator rules. All command responses are compared exactly.
