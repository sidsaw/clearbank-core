#!/usr/bin/env bash
#
# demo_setup.sh — Capture the current repo state as the demo baseline.
#
# What it does:
#   1. Tags the current commit as demo-baseline (force overwrite if exists)
#   2. Snapshots GitHub Project board column state to .demo/board_state.json
#   3. Snapshots issue states (open/closed, labels) to .demo/issues_state.json
#   4. Snapshots audit_reports/coverage.md output to .demo/baseline_coverage.md
#   5. Commits the .demo/ directory
#
# Prerequisites:
#   - gh CLI authenticated (gh auth status)
#   - python3 available (for coverage_report.py)
#   - Run from the repository root
#
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "$0")" && pwd)"
cd "$REPO_ROOT"

DEMO_DIR=".demo"
OWNER="sidsaw"
REPO="clearbank-core"
PROJECT_NUMBER=3

# ── Helpers ──────────────────────────────────────────────────────────────────

info()  { echo "▸ $*"; }
warn()  { echo "⚠  $*" >&2; }
die()   { echo "✘ $*" >&2; exit 1; }

require_cmd() {
    command -v "$1" &>/dev/null || die "'$1' is required but not found in PATH."
}

# ── Preflight checks ────────────────────────────────────────────────────────

require_cmd gh
require_cmd git
require_cmd python3
require_cmd jq

gh auth status &>/dev/null || die "gh CLI is not authenticated. Run 'gh auth login' first."

if [ ! -f coverage_report.py ]; then
    die "coverage_report.py not found — are you in the repo root?"
fi

# ── 1. Tag the current commit ───────────────────────────────────────────────

info "Tagging current commit as demo-baseline (force overwrite)..."
git tag -f demo-baseline HEAD
git push origin demo-baseline --force
info "Tag demo-baseline pushed to origin."

# ── 2. Snapshot GitHub Project board state ───────────────────────────────────

mkdir -p "$DEMO_DIR"

info "Snapshotting GitHub Projects v2 board state..."

# Discover the project node ID (pipe through jq to handle GraphQL errors gracefully)
PROJECT_ID=$(gh api graphql -f query='
  query($owner: String!, $number: Int!) {
    user(login: $owner) {
      projectV2(number: $number) {
        id
        title
      }
    }
  }
' -f owner="$OWNER" -F number="$PROJECT_NUMBER" 2>/dev/null | jq -r '.data.user.projectV2.id // empty') || true

if [ -z "${PROJECT_ID:-}" ]; then
    warn "Could not retrieve Project #$PROJECT_NUMBER. Board state will NOT be captured."
    echo '{"error": "Could not retrieve project board via API. Manual snapshot required."}' > "$DEMO_DIR/board_state.json"
    echo ""
    echo "╔══════════════════════════════════════════════════════════════════╗"
    echo "║  GitHub Projects v2 board state could not be fully captured.    ║"
    echo "║  Manual checklist:                                             ║"
    echo "║  1. Open https://github.com/users/$OWNER/projects/$PROJECT_NUMBER      ║"
    echo "║  2. Screenshot or note which items are in each column          ║"
    echo "║  3. Save that info alongside .demo/ for manual restore         ║"
    echo "╚══════════════════════════════════════════════════════════════════╝"
    echo ""
else
    info "Project ID: $PROJECT_ID"

    # Fetch all items with their status field values
    gh api graphql -f query='
      query($projectId: ID!) {
        node(id: $projectId) {
          ... on ProjectV2 {
            title
            items(first: 100) {
              nodes {
                id
                content {
                  ... on Issue {
                    number
                    title
                    url
                  }
                  ... on PullRequest {
                    number
                    title
                    url
                  }
                }
                fieldValues(first: 20) {
                  nodes {
                    ... on ProjectV2ItemFieldSingleSelectValue {
                      name
                      field {
                        ... on ProjectV2SingleSelectField {
                          name
                        }
                      }
                    }
                    ... on ProjectV2ItemFieldTextValue {
                      text
                      field {
                        ... on ProjectV2Field {
                          name
                        }
                      }
                    }
                  }
                }
              }
            }
            fields(first: 20) {
              nodes {
                ... on ProjectV2SingleSelectField {
                  id
                  name
                  options {
                    id
                    name
                  }
                }
                ... on ProjectV2Field {
                  id
                  name
                }
              }
            }
          }
        }
      }
    ' -f projectId="$PROJECT_ID" > "$DEMO_DIR/board_state.json"

    ITEM_COUNT=$(jq '.data.node.items.nodes | length' "$DEMO_DIR/board_state.json")
    info "Captured $ITEM_COUNT project board items to $DEMO_DIR/board_state.json"
fi

# ── 3. Snapshot issue states ─────────────────────────────────────────────────

info "Snapshotting issue states..."
gh api --paginate "repos/$OWNER/$REPO/issues?state=all&per_page=100" \
    | jq '[.[] | select(.pull_request == null) | {
        number: .number,
        title: .title,
        state: .state,
        labels: [.labels[].name]
    }]' > "$DEMO_DIR/issues_state.json"

ISSUE_COUNT=$(jq 'length' "$DEMO_DIR/issues_state.json")
info "Captured $ISSUE_COUNT issues to $DEMO_DIR/issues_state.json"

# ── 4. Snapshot coverage ─────────────────────────────────────────────────────

info "Generating baseline coverage report..."
python3 coverage_report.py > /dev/null 2>&1 || true

if [ -f audit_reports/coverage.md ]; then
    cp audit_reports/coverage.md "$DEMO_DIR/baseline_coverage.md"
    info "Saved baseline coverage to $DEMO_DIR/baseline_coverage.md"
else
    warn "audit_reports/coverage.md was not generated — saving empty baseline."
    echo "No coverage data available at baseline." > "$DEMO_DIR/baseline_coverage.md"
fi

# ── 5. Commit the .demo/ directory ───────────────────────────────────────────

info "Committing .demo/ directory..."
git add "$DEMO_DIR/"
if git diff --cached --quiet "$DEMO_DIR/"; then
    info ".demo/ directory unchanged — nothing to commit."
else
    git commit -m "chore: capture demo baseline state in .demo/"
    git push origin HEAD
    # Update the tag to include the .demo/ commit
    git tag -f demo-baseline HEAD
    git push origin demo-baseline --force
    info "Committed and pushed .demo/ directory. Tag demo-baseline updated."
fi

echo ""
echo "════════════════════════════════════════════════════════════════════"
echo "  Demo baseline captured successfully!"
echo "  Tag:          demo-baseline"
echo "  Board state:  $DEMO_DIR/board_state.json"
echo "  Issues state: $DEMO_DIR/issues_state.json"
echo "  Coverage:     $DEMO_DIR/baseline_coverage.md"
echo "════════════════════════════════════════════════════════════════════"
