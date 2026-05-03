#!/usr/bin/env bash
#
# demo_reset.sh — Restore the repo to the demo-baseline state.
#
# What it does:
#   1. Hard-resets local repo to the demo-baseline tag
#   2. Force-pushes main to origin
#   3. Restores issue states (close/reopen, restore labels) from .demo/issues_state.json
#   4. Removes "devin" labels from all issues
#   5. Closes any open PRs created during the previous demo run
#   6. Restores project board column state from .demo/board_state.json (best-effort)
#   7. Prints a manual verification checklist
#
# Usage:
#   ./demo_reset.sh            # execute the reset
#   ./demo_reset.sh --dry-run  # print what would happen without making changes
#
# Idempotent: running this script twice produces the same final state.
#
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "$0")" && pwd)"
cd "$REPO_ROOT"

DEMO_DIR=".demo"
OWNER="sidsaw"
REPO="clearbank-core"
PROJECT_NUMBER=3
DRY_RUN=false

# ── Parse flags ──────────────────────────────────────────────────────────────

for arg in "$@"; do
    case "$arg" in
        --dry-run) DRY_RUN=true ;;
        -h|--help)
            echo "Usage: $0 [--dry-run]"
            echo "  --dry-run  Print what would happen without making changes"
            exit 0
            ;;
        *) echo "Unknown option: $arg" >&2; exit 1 ;;
    esac
done

# ── Helpers ──────────────────────────────────────────────────────────────────

info()  { echo "▸ $*"; }
warn()  { echo "⚠  $*" >&2; }
die()   { echo "✘ $*" >&2; exit 1; }
dry()   { if $DRY_RUN; then echo "  [DRY RUN] $*"; return 1; else return 0; fi; }

require_cmd() {
    command -v "$1" &>/dev/null || die "'$1' is required but not found in PATH."
}

# ── Preflight checks ────────────────────────────────────────────────────────

require_cmd gh
require_cmd git
require_cmd jq

gh auth status &>/dev/null || die "gh CLI is not authenticated. Run 'gh auth login' first."

# Verify tag exists
if ! git rev-parse demo-baseline &>/dev/null; then
    die "Tag 'demo-baseline' not found. Run demo_setup.sh first."
fi

if $DRY_RUN; then
    echo ""
    echo "╔══════════════════════════════════════════════════════════════════╗"
    echo "║                        DRY RUN MODE                            ║"
    echo "║  No changes will be made. Showing what would happen.           ║"
    echo "╚══════════════════════════════════════════════════════════════════╝"
    echo ""
fi

# ── 1. Reset local repo to demo-baseline ─────────────────────────────────────

info "Resetting local repo to demo-baseline tag..."
if dry "git reset --hard demo-baseline"; then
    git reset --hard demo-baseline
    info "Local repo reset to demo-baseline."
fi

# ── 2. Force-push main to origin ─────────────────────────────────────────────

info "Force-pushing main to origin..."
if dry "git push origin main --force"; then
    git push origin main --force
    info "Origin main force-pushed to demo-baseline."
fi

# Re-read .demo files (they should now exist from the reset)
if [ ! -d "$DEMO_DIR" ]; then
    warn ".demo/ directory not found at demo-baseline. Skipping state restoration."
    echo ""
    echo "Manual steps needed:"
    echo "  1. Re-run demo_setup.sh to capture baseline state"
    echo "  2. Then re-run demo_reset.sh"
    exit 1
fi

# ── 3. Restore issue states ──────────────────────────────────────────────────

if [ -f "$DEMO_DIR/issues_state.json" ]; then
    info "Restoring issue states..."

    # Get current issue states for comparison
    CURRENT_ISSUES=$(gh api --paginate "repos/$OWNER/$REPO/issues?state=all&per_page=100" \
        | jq '[.[] | select(.pull_request == null) | {
            number: .number,
            state: .state,
            labels: [.labels[].name]
        }]')

    jq -c '.[]' "$DEMO_DIR/issues_state.json" | while IFS= read -r baseline_issue; do
        ISSUE_NUM=$(echo "$baseline_issue" | jq -r '.number')
        BASELINE_STATE=$(echo "$baseline_issue" | jq -r '.state')
        BASELINE_LABELS=$(echo "$baseline_issue" | jq -r '[.labels[]] | join(",")')

        CURRENT_STATE=$(echo "$CURRENT_ISSUES" | jq -r ".[] | select(.number == $ISSUE_NUM) | .state" 2>/dev/null || echo "")

        if [ -z "$CURRENT_STATE" ]; then
            info "  Issue #$ISSUE_NUM: not found in current repo (may have been deleted). Skipping."
            continue
        fi

        # Restore state (open/closed)
        if [ "$CURRENT_STATE" != "$BASELINE_STATE" ]; then
            if dry "  Issue #$ISSUE_NUM: $CURRENT_STATE → $BASELINE_STATE"; then
                gh issue edit "$ISSUE_NUM" --repo "$OWNER/$REPO" --remove-label "" 2>/dev/null || true
                if [ "$BASELINE_STATE" = "open" ]; then
                    gh issue reopen "$ISSUE_NUM" --repo "$OWNER/$REPO" 2>/dev/null || true
                else
                    gh issue close "$ISSUE_NUM" --repo "$OWNER/$REPO" 2>/dev/null || true
                fi
                info "  Issue #$ISSUE_NUM: $CURRENT_STATE → $BASELINE_STATE"
            fi
        else
            info "  Issue #$ISSUE_NUM: already $BASELINE_STATE (no change)"
        fi

        # Restore labels
        CURRENT_LABELS=$(echo "$CURRENT_ISSUES" | jq -r ".[] | select(.number == $ISSUE_NUM) | [.labels[]] | join(\",\")")

        if [ "$CURRENT_LABELS" != "$BASELINE_LABELS" ]; then
            if dry "  Issue #$ISSUE_NUM labels: [$CURRENT_LABELS] → [$BASELINE_LABELS]"; then
                # Remove all current labels first
                if [ -n "$CURRENT_LABELS" ]; then
                    IFS=',' read -ra CLABELS <<< "$CURRENT_LABELS"
                    for label in "${CLABELS[@]}"; do
                        [ -n "$label" ] && gh issue edit "$ISSUE_NUM" --repo "$OWNER/$REPO" --remove-label "$label" 2>/dev/null || true
                    done
                fi
                # Add baseline labels
                if [ -n "$BASELINE_LABELS" ]; then
                    IFS=',' read -ra BLABELS <<< "$BASELINE_LABELS"
                    for label in "${BLABELS[@]}"; do
                        [ -n "$label" ] && gh issue edit "$ISSUE_NUM" --repo "$OWNER/$REPO" --add-label "$label" 2>/dev/null || true
                    done
                fi
                info "  Issue #$ISSUE_NUM labels: restored to [$BASELINE_LABELS]"
            fi
        fi
    done
else
    warn "issues_state.json not found — skipping issue restoration."
fi

# ── 4. Remove "devin" labels from all issues ─────────────────────────────────

info "Removing 'devin' labels from all issues..."
DEVIN_ISSUES=$(gh api --paginate "repos/$OWNER/$REPO/issues?state=all&labels=devin&per_page=100" \
    | jq -r '.[].number' 2>/dev/null || echo "")

if [ -z "$DEVIN_ISSUES" ]; then
    info "  No issues with 'devin' label found."
else
    for ISSUE_NUM in $DEVIN_ISSUES; do
        if dry "  Remove 'devin' label from issue #$ISSUE_NUM"; then
            gh issue edit "$ISSUE_NUM" --repo "$OWNER/$REPO" --remove-label "devin" 2>/dev/null || true
            info "  Removed 'devin' label from issue #$ISSUE_NUM"
        fi
    done
fi

# ── 5. Close any open PRs from previous demo runs ────────────────────────────

info "Closing open PRs..."
OPEN_PRS=$(gh pr list --repo "$OWNER/$REPO" --state open --json number,title --jq '.[].number' 2>/dev/null || echo "")

if [ -z "$OPEN_PRS" ]; then
    info "  No open PRs to close."
else
    for PR_NUM in $OPEN_PRS; do
        PR_TITLE=$(gh pr view "$PR_NUM" --repo "$OWNER/$REPO" --json title --jq '.title' 2>/dev/null || echo "PR #$PR_NUM")
        if dry "  Close PR #$PR_NUM: $PR_TITLE"; then
            gh pr close "$PR_NUM" --repo "$OWNER/$REPO" 2>/dev/null || true
            info "  Closed PR #$PR_NUM: $PR_TITLE"
        fi
    done
fi

# ── 6. Restore project board state (best-effort) ─────────────────────────────

info "Restoring project board state..."

BOARD_HAS_ERROR=$(jq -r '.error // empty' "$DEMO_DIR/board_state.json" 2>/dev/null || echo "")

if [ -n "$BOARD_HAS_ERROR" ]; then
    warn "Board state was not captured at setup time. Manual restoration required."
else
    # Get project ID
    PROJECT_ID=$(gh api graphql -f query='
      query($owner: String!, $number: Int!) {
        user(login: $owner) {
          projectV2(number: $number) {
            id
          }
        }
      }
    ' -f owner="$OWNER" -F number="$PROJECT_NUMBER" --jq '.data.user.projectV2.id' 2>/dev/null) || true

    if [ -z "${PROJECT_ID:-}" ]; then
        warn "Could not access Project #$PROJECT_NUMBER via API."
    else
        # Get the Status field ID and its options
        STATUS_FIELD_ID=$(jq -r '
            .data.node.fields.nodes[]
            | select(.name == "Status" and .options != null)
            | .id
        ' "$DEMO_DIR/board_state.json" 2>/dev/null || echo "")

        if [ -z "$STATUS_FIELD_ID" ]; then
            warn "No 'Status' single-select field found in saved board state."
        else
            # Build a map of option name → option ID from the saved state
            # We need to get the CURRENT field options (IDs may differ if board was recreated)
            CURRENT_OPTIONS=$(gh api graphql -f query='
              query($projectId: ID!) {
                node(id: $projectId) {
                  ... on ProjectV2 {
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
                      }
                    }
                  }
                }
              }
            ' -f projectId="$PROJECT_ID" 2>/dev/null) || true

            CURRENT_STATUS_FIELD_ID=$(echo "$CURRENT_OPTIONS" | jq -r '
                .data.node.fields.nodes[]
                | select(.name == "Status" and .options != null)
                | .id
            ' 2>/dev/null || echo "")

            if [ -z "$CURRENT_STATUS_FIELD_ID" ]; then
                warn "Could not find current Status field on the project."
            else
                # Iterate saved items and restore their status
                ITEMS_RESTORED=0
                ITEMS_FAILED=0

                jq -c '.data.node.items.nodes[]' "$DEMO_DIR/board_state.json" 2>/dev/null | while IFS= read -r item; do
                    ITEM_TITLE=$(echo "$item" | jq -r '.content.title // "unknown"')
                    ITEM_URL=$(echo "$item" | jq -r '.content.url // ""')
                    ITEM_NUMBER=$(echo "$item" | jq -r '.content.number // ""')

                    # Find the saved status value
                    SAVED_STATUS=$(echo "$item" | jq -r '
                        .fieldValues.nodes[]
                        | select(.field.name == "Status")
                        | .name
                    ' 2>/dev/null || echo "")

                    if [ -z "$SAVED_STATUS" ]; then
                        info "  Item '$ITEM_TITLE': no Status value saved, skipping."
                        continue
                    fi

                    # Find the option ID for this status in the current project
                    TARGET_OPTION_ID=$(echo "$CURRENT_OPTIONS" | jq -r --arg status "$SAVED_STATUS" '
                        .data.node.fields.nodes[]
                        | select(.name == "Status" and .options != null)
                        | .options[]
                        | select(.name == $status)
                        | .id
                    ' 2>/dev/null || echo "")

                    if [ -z "$TARGET_OPTION_ID" ]; then
                        warn "  Item '$ITEM_TITLE': status '$SAVED_STATUS' not found in current project options."
                        continue
                    fi

                    # Find the item in the current project by matching issue/PR number
                    if [ -n "$ITEM_NUMBER" ]; then
                        CURRENT_ITEM_ID=$(gh api graphql -f query='
                          query($projectId: ID!) {
                            node(id: $projectId) {
                              ... on ProjectV2 {
                                items(first: 100) {
                                  nodes {
                                    id
                                    content {
                                      ... on Issue { number }
                                      ... on PullRequest { number }
                                    }
                                  }
                                }
                              }
                            }
                          }
                        ' -f projectId="$PROJECT_ID" --jq ".data.node.items.nodes[] | select(.content.number == $ITEM_NUMBER) | .id" 2>/dev/null) || true
                    fi

                    if [ -z "${CURRENT_ITEM_ID:-}" ]; then
                        warn "  Item '$ITEM_TITLE' (#$ITEM_NUMBER): not found on current project board."
                        continue
                    fi

                    if dry "  Move '$ITEM_TITLE' → $SAVED_STATUS"; then
                        gh api graphql -f query='
                          mutation($projectId: ID!, $itemId: ID!, $fieldId: ID!, $optionId: String!) {
                            updateProjectV2ItemFieldValue(
                              input: {
                                projectId: $projectId
                                itemId: $itemId
                                fieldId: $fieldId
                                value: { singleSelectOptionId: $optionId }
                              }
                            ) {
                              projectV2Item { id }
                            }
                          }
                        ' -f projectId="$PROJECT_ID" \
                          -f itemId="$CURRENT_ITEM_ID" \
                          -f fieldId="$CURRENT_STATUS_FIELD_ID" \
                          -f optionId="$TARGET_OPTION_ID" >/dev/null 2>&1 && \
                            info "  Moved '$ITEM_TITLE' → $SAVED_STATUS" || \
                            warn "  Failed to move '$ITEM_TITLE' → $SAVED_STATUS"
                    fi
                done
            fi
        fi
    fi
fi

# ── 7. Regenerate coverage report ─────────────────────────────────────────────

info "Regenerating coverage report..."
if dry "python3 coverage_report.py"; then
    python3 coverage_report.py > /dev/null 2>&1 || warn "Failed to regenerate coverage report."
    if [ -f audit_reports/coverage.md ]; then
        info "Coverage report regenerated at audit_reports/coverage.md"
    fi
fi

# ── 8. Verification checklist ────────────────────────────────────────────────

echo ""
echo "════════════════════════════════════════════════════════════════════"
if $DRY_RUN; then
    echo "  DRY RUN COMPLETE — no changes were made."
else
    echo "  Demo reset complete!"
fi
echo "════════════════════════════════════════════════════════════════════"
echo ""
echo "Manual verification checklist:"
echo ""
echo "  [ ] Git state"
echo "      - 'main' points to the demo-baseline commit"
echo "      - Run: git log --oneline -1  (should match demo-baseline)"
echo ""
echo "  [ ] Project board"
echo "      - Open https://github.com/users/$OWNER/projects/$PROJECT_NUMBER"
echo "      - Verify items are in their original columns"
echo "      - NOTE: GitHub Projects v2 API has limitations — some column"
echo "        moves may not have applied. Check manually."
echo ""
echo "  [ ] Issue states"
echo "      - Open https://github.com/$OWNER/$REPO/issues"
echo "      - Verify issues match baseline (open/closed, labels)"
echo "      - No issues should have a 'devin' label"
echo ""
echo "  [ ] Pull requests"
echo "      - Open https://github.com/$OWNER/$REPO/pulls"
echo "      - No open PRs from previous demo run"
echo ""
echo "  [ ] Coverage audit report"
echo "      - Verify audit_reports/coverage.md includes both overall and compliance paths"
echo "      - Compare audit_reports/coverage.md with .demo/baseline_coverage.md"
echo ""
echo "════════════════════════════════════════════════════════════════════"
