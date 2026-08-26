---
name: present-changes-visually
description: Generate a self-contained, GitHub-style split-view HTML page that visually presents changes in this Git repository. Use when asked to show, review, share, or inspect code changes visually; compare revisions, branches, commits, or the worktree; or create an HTML diff.
---

# Present Changes Visually

Generate one interactive HTML page containing every changed file as a side-by-side before/after diff. The page folds long unchanged runs, highlights changed words within modified lines, lets readers filter files, and includes collapsed panels for unchanged files.

## Generate the page

1. Treat this repository as the target unless the user identifies another repository.
2. Use `HEAD` as the before point and `WORKTREE` as the after point unless the user specifies comparison points. `WORKTREE` includes staged, unstaged, and untracked (but not ignored) files.
3. Write to `_temp/visual-diff.html` unless the user supplies an output path.
4. From the repository root, run the bundled generator with an available Python 3 launcher:

   ```powershell
   py -3 .codex/skills/present-changes-visually/scripts/generate-split-view-diff.py `
     . HEAD WORKTREE _temp/visual-diff.html
   ```

   On systems where `py` is unavailable, use `python3`. If neither launcher is on `PATH` in Codex Desktop, call `load_workspace_dependencies` and use its Python executable. Replace `HEAD`, `WORKTREE`, and the output path with the requested values. A comparison point can be any Git commit-ish, such as `HEAD~1`, a tag, a branch, or a commit SHA. Use `WORKTREE` for the current files.
5. Confirm that the command succeeded and report the absolute path to the generated page. Do not open a browser unless the user asks.

## Verify output

Check that the page exists and that the generator's summary reports the expected changed-file count. When the user requests a visual review, open the generated HTML in the in-app browser and inspect the rendered page.

## Resource

`scripts/generate-split-view-diff.py` is the bundled standard-library-only generator. The generated page is self-contained except for optional syntax highlighting loaded from a CDN; it remains usable without network access.
