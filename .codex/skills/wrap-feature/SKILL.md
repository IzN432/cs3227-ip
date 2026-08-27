---
name: wrap-feature
description: Finish repository work when the user says "that's a wrap" by verifying changes, integrating the current feature branch into the remote default branch with linear history, pushing to origin, and cleaning up the merged local branch.
---

# Wrap a feature

Treat the phrase "that's a wrap" as explicit authorization to commit any
completed in-scope work, integrate it into the repository's default branch,
and push that branch to `origin`.

Before mutating Git state, inspect the worktree and repository instructions.
Do not include unrelated changes or runtime/user data in the commit. Run the
required tests when they have not already passed for the final code state.

Use the repository's actual default branch; do not assume it is named `main`.
Follow this integration shape:

1. Commit the completed work on the current feature branch using the
   repository's commit conventions. If already committed, reuse that commit.
2. Fetch `origin`, then rebase the feature branch onto the latest remote
   default branch.
3. Stop and report any failed test, rebase conflict, missing remote,
   authentication failure, or non-fast-forward condition. Never force-push or
   discard changes to complete the workflow.
4. Fast-forward the local default branch to the rebased feature branch and
   push the default branch to `origin`.
5. After the push succeeds, delete the merged local feature branch. Do not
   delete a remote feature branch unless the user separately requests it.

Finish by reporting the default branch, pushed commit, tests run, and branch
cleanup. If the workflow stops, preserve all work and state the exact recovery
step the user needs to take.
