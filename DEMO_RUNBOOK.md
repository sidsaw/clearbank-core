# Demo Runbook

How to capture and restore the baseline state of this repo using the demo scripts.

**Repo:** `sidsaw/clearbank-core` &nbsp;|&nbsp; **Project board:** [github.com/users/sidsaw/projects/3](https://github.com/users/sidsaw/projects/3)

---

## Prerequisites

Both scripts require these tools in your PATH:

- `gh` — GitHub CLI, authenticated (`gh auth login`)
- `git`
- `jq`
- `python3` (for `coverage_report.py`)

The `gh` token needs the `project` scope for Projects v2 board operations:

```bash
gh auth refresh -s project
```

---

## demo_setup.sh — Capture baseline

Saves the current repo state so it can be restored later.

```bash
./demo_setup.sh
```

### What it does

1. **Tags the current commit** as `demo-baseline` (force overwrites if the tag already exists) and pushes the tag to origin.
2. **Snapshots the GitHub Projects v2 board** to `.demo/board_state.json` via GraphQL — captures all items and their `Status` field values, plus the field/option IDs needed for restoration.
3. **Snapshots all issue states** to `.demo/issues_state.json` — records each issue's number, title, open/closed state, and labels.
4. **Runs `coverage_report.py`** and copies the output to `.demo/baseline_coverage.md`.
5. **Commits the `.demo/` directory** and updates the `demo-baseline` tag to include it.

### Projects v2 limitations

If the script can't access the project board (permissions, network, etc.), it writes an error marker to `board_state.json` and prints a warning. The board state will need to be noted manually in that case.

---

## demo_reset.sh — Restore baseline

Resets the repo, issues, PRs, and project board back to the saved baseline state.

```bash
./demo_reset.sh              # execute the reset
./demo_reset.sh --dry-run    # print what would happen without making changes
```

### What it does

1. **Hard-resets** the local repo to the `demo-baseline` tag.
2. **Force-pushes** `main` to origin (overwrites any commits made since baseline).
3. **Restores issue states** from `.demo/issues_state.json` — reopens or closes issues as needed, removes current labels and re-applies the baseline labels.
4. **Removes `devin` labels** from all issues in the repo.
5. **Closes all open PRs** in the repo.
6. **Restores project board state** (best-effort) — matches each saved item by issue/PR number, looks up the target `Status` option ID on the current board, and moves items via the `updateProjectV2ItemFieldValue` GraphQL mutation. Warns per-item if anything can't be restored.
7. **Prints a verification checklist** summarizing what to check manually.

### Idempotency

Running `demo_reset.sh` twice produces the same result — issues already in the correct state are skipped, labels already removed stay removed, and already-closed PRs are left alone.

### --dry-run

Pass `--dry-run` to see exactly what the script would do without making any changes:

```bash
./demo_reset.sh --dry-run
```

Each action is printed with a `[DRY RUN]` prefix. Useful for verifying state before committing to a reset.

---

## Verification checklist

After running `demo_reset.sh`, verify:

| Check | How |
|-------|-----|
| Git state | `git log --oneline -1` matches the `demo-baseline` commit |
| Project board | Open the [Projects board](https://github.com/users/sidsaw/projects/3) and confirm items are in their original columns |
| Issues | Open [Issues](https://github.com/sidsaw/clearbank-core/issues) — states and labels match baseline, no `devin` labels |
| Pull requests | Open [PRs](https://github.com/sidsaw/clearbank-core/pulls) — no open PRs from previous runs |
| Coverage | Run `python3 coverage_report.py` and compare with `.demo/baseline_coverage.md` |

> **Note:** GitHub Projects v2 board restoration is best-effort via the API. If items aren't in the right columns, move them manually.

---

## Troubleshooting

### `Tag 'demo-baseline' not found`

Run `demo_setup.sh` first to capture the baseline.

### `gh CLI is not authenticated`

```bash
gh auth login
gh auth refresh -s project   # adds project scope
```

### Board state wasn't captured

If `board_state.json` contains `{"error": ...}`, the project wasn't accessible at setup time. Re-run `demo_setup.sh` after fixing `gh` permissions.

### Board items not moving to correct columns

The script matches items by issue/PR number and status option by name. If the board's `Status` field options were renamed or the item was removed from the board, the move will fail with a warning. Fix manually on the board.

### Force-push to main is rejected

Ensure branch protection rules allow force-pushes (or temporarily disable them), then re-run.
