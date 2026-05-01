# Demo Runbook

> **Total runtime:** ~15–20 minutes of live demo + ~10 min buffer
>
> **Audience:** Engineering leaders evaluating Devin for their org
>
> **Repo:** `sidsaw/clearbank-core` &nbsp;|&nbsp; **Project board:** [github.com/users/sidsaw/projects/3](https://github.com/users/sidsaw/projects/3)

---

## 1. Pre-Demo Checklist (30 minutes before)

### 1.1 Reset to baseline

```bash
cd clearbank-core
./demo_reset.sh          # restores repo, issues, PRs, board
```

Verify the printed checklist passes — no stray PRs, issues match baseline.

### 1.2 Verify project board state

- Open [Projects board](https://github.com/users/sidsaw/projects/3)
- **Todo column:** transaction-service coverage issue (issue #2)
- **Done column:** auth-service coverage issue (issue #1)
- No other cards visible (demo PRs should be closed)

### 1.3 Verify baseline coverage

```bash
python3 coverage_report.py
```

Compare output with `.demo/baseline_coverage.md` — numbers should match exactly.

### 1.4 Verify DEVIN_API_KEY

Create a throwaway issue to confirm the Devin trigger fires:

```bash
gh issue create --repo sidsaw/clearbank-core \
  --title "API key test — delete me" \
  --label devin
```

- Confirm the GitHub Action `.github/workflows/devin-trigger.yml` fires in the **Actions** tab
- Confirm a Devin session starts (check the Devin console or the issue comment with the session link)
- Revert immediately:

```bash
# Close and delete the test issue
ISSUE_NUM=$(gh issue list --repo sidsaw/clearbank-core --label devin --state open --json number --jq '.[0].number')
gh issue close "$ISSUE_NUM" --repo sidsaw/clearbank-core
```

- Remove the `devin` label from the issue to prevent re-triggering

### 1.5 Verify network/VPN

- Open the [Devin console](https://app.devin.ai) and confirm you can see your org's sessions
- If VPN is required, confirm it's connected

### 1.6 Open backup tabs

Have these tabs ready in your browser (in order):

| Tab | URL |
|-----|-----|
| 1. Devin console | `https://app.devin.ai` |
| 2. GitHub repo | `https://github.com/sidsaw/clearbank-core` |
| 3. Projects board | `https://github.com/users/sidsaw/projects/3` |
| 4. Architecture PDF | `docs/architecture.pdf` (open locally or via GitHub) |
| 5. Actions tab | `https://github.com/sidsaw/clearbank-core/actions` |
| 6. Auth-service PR | The already-merged auth-service PR (fallback for Step 8) |

---

## 2. In-Demo Sequence

### Step 1 — Repo walkthrough (~30 sec)

1. Switch to the **GitHub repo** tab
2. Open `README.md` (it's the landing page)
3. Walk through the four-service table: auth, transaction, pii, audit
4. Mention: *"Each service is intentionally minimal — single source file, few functions, no frameworks. Easy to read end-to-end."*

### Step 2 — Coverage report (~30 sec)

1. Switch to your **terminal**
2. Run:
   ```bash
   python3 coverage_report.py
   ```
3. Show the markdown table output — highlight that transaction-service has **0% coverage**
4. *"This is what Devin is going to fix for us."*

### Step 3 — Show the Done card and its PR (~1 min)

1. Switch to the **Projects board** tab
2. Point to the **Done** column → auth-service card
3. Click the card to open issue #1
4. Click the linked PR (the merged auth-service PR)
5. Scroll to the **coverage comment** posted by the workflow bot
6. Show the coverage delta — *"Devin wrote these tests, PR passed CI, coverage went up, card moved to Done."*

### Step 4 — Trigger Devin on the Todo card (~30 sec)

1. Click **Back** to the Projects board
2. Open the **Todo** column → transaction-service card (issue #2)
3. In the issue, click **Labels** → add the `devin` label
4. *"That label is the trigger. A GitHub Action picks it up and sends the task to Devin."*

### Step 5 — Show the Action firing (~1 min)

1. Switch to the **Actions** tab (`https://github.com/sidsaw/clearbank-core/actions`)
2. Show the `devin-trigger` workflow run appearing (yellow dot = in progress)
3. Click into the run → show the workflow steps
4. Once the issue comment appears with the Devin session link, click it
5. *"This is the handoff — GitHub fires the action, Devin picks up the issue, and we can watch it work."*

### Step 6 — Devin console tour (~2 min)

1. You're now in the **Devin console** showing the active session
2. Give a brief tour:
   - **Plan panel** — Devin's task breakdown and progress
   - **Shell** — live terminal output (cloning, running tests, etc.)
   - **Editor** — files Devin is reading/writing
   - **Browser** — if Devin looked anything up
3. *"Devin is reading the source code, understanding the API, and writing JUnit 5 tests with Mockito where it makes sense."*

### Step 7 — Architecture PDF / slides (~3 min)

> **Presenter drives this section**

1. Open `docs/architecture.pdf` (or your slides)
2. Cover:
   - How Devin runs (VM, shell, browser, tools)
   - Secrets management
   - Knowledge / skills system
3. *This buys time for Devin to finish in the background*

### Step 8 — Show the result (~3 min)

1. Switch back to the **Devin console**
2. **If Devin is done:** Click the PR link in the session → open it on GitHub
   - Walk through the test file(s) Devin wrote
   - Show the CI checks passing
   - Show the coverage comment with the new numbers
3. **If Devin is still running:** Switch to the **auth-service PR** tab (your fallback)
   - Walk through the tests Devin wrote for auth-service
   - *"Same pattern — Devin read the source, wrote comprehensive tests, opened a PR, CI passed."*

### Step 9 — Parallel / fleet (~2 min)

1. *"Devin doesn't just do one thing at a time."*
2. Show any pre-existing PRs for **pii-service** and **audit-service** (if available)
   - Open them from the repo's Pull Requests tab
   - Show that multiple services were tested in parallel
3. If no parallel PRs exist: describe the fleet concept verbally
   - *"In production, you'd label multiple issues and Devin handles them concurrently — separate sessions, separate PRs."*

### Step 10 — Org-level capabilities (~3 min)

1. Cover these topics (can use slides or the Devin console):
   - **Playbooks** — reusable task templates for common workflows
   - **DeepWiki** — auto-generated codebase documentation
   - **Quartz/COBOL** — Devin working with legacy systems
2. *"This isn't just for greenfield. Devin handles the codebases nobody wants to touch."*

---

## 3. Recovery Procedures

### Devin session is slow or stuck

- **Don't wait.** Switch to walking through the **already-merged auth-service PR** while Devin runs in the background.
- *"Let me show you what the end result looks like while Devin finishes — same pattern, different service."*
- Check back in Step 8.

### API trigger fails (GitHub Action doesn't fire)

1. Check the Actions tab for errors
2. **Manual fallback:** Start a session directly in the Devin console
   - Go to `https://app.devin.ai` → New Session
   - Paste the prompt from issue #2 (or the task description from the issue body)
3. Continue with Step 6 once the session is running

### Coverage report comment doesn't appear on the PR

1. **Local fallback:** Run in terminal:
   ```bash
   python3 coverage_report.py
   ```
2. Show the terminal output instead — same data, just not posted as a PR comment
3. *"The CI workflow posts this automatically, but here's the same report locally."*

### GitHub Action fails entirely

- Have a **screenshot** of a successful previous run ready (save to `docs/` or your desktop)
- Show the screenshot and explain: *"Here's what it looks like when it runs — the Action triggers Devin, Devin opens a PR, CI runs the coverage report."*

### Devin writes tests but CI fails

- This is actually a **good demo moment** — show Devin iterating on the fix
- *"Watch — Devin sees the failure, reads the logs, and fixes the test. This is the feedback loop."*

---

## 4. Post-Demo Cleanup

### What to leave running (for follow-up questions)

- **Devin session** — leave it open so attendees can ask questions and see the console
- **Open PR** — leave the transaction-service PR open so people can browse the code
- **Projects board** — leave as-is to show the workflow

### What to reset before the next demo

```bash
./demo_reset.sh
```

This will:
- Hard-reset `main` to the `demo-baseline` tag and force-push
- Close any open PRs from the demo run
- Restore issue states (open/closed, labels)
- Remove `devin` labels from all issues
- Best-effort restore of the Projects board columns

After running, verify with `./demo_reset.sh --dry-run` (should report no changes needed).

### If running multiple demos in one day

1. Run `./demo_reset.sh` between each demo
2. Wait ~1 minute for GitHub to process the force-push and issue updates
3. Run the pre-demo checklist (Section 1) again — skip the API key test if you already verified it today
