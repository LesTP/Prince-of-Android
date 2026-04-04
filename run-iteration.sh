#!/bin/bash
# run-iteration.sh — Execute autonomous loop iterations
# Called by the orchestrator (TG bot session) or standalone.
#
# Usage:
#   ./run-iteration.sh                  # single iteration, auto-numbering
#   ./run-iteration.sh -n 5             # run up to 5 iterations (stops on ESCALATE)
#   ./run-iteration.sh -n 5 --start 3   # start from iteration 3, run up to 5
#
# Output:
#   logs/loop/iteration_NNN.jsonl  — full stream-json transcript (tool calls, results, costs)
#   logs/loop/iteration_NNN.txt    — human-readable summary extracted from jsonl
#   logs/loop/summary.log          — one line per iteration
#
# Exit: 0=all CONTINUE, 1=ESCALATE, 2=NO_SIGNAL/ERROR
#
# Dependencies: python3 (for JSON parsing — jq not available in this environment)

set -uo pipefail

PROJECT_DIR="/home/claude/workspace/PoP port"
LOG_DIR="$PROJECT_DIR/logs/loop"
SUMMARY_FILE="$LOG_DIR/summary.log"

# Parse arguments
MAX_ITERATIONS=1
START_ITER=""

while [[ $# -gt 0 ]]; do
  case $1 in
    -n|--iterations) MAX_ITERATIONS="$2"; shift 2 ;;
    --start)         START_ITER="$2"; shift 2 ;;
    *)               echo "Unknown option: $1"; exit 2 ;;
  esac
done

# Auto-determine start iteration from summary log
if [[ -z "$START_ITER" ]]; then
  if [[ -f "$SUMMARY_FILE" ]]; then
    LAST=$(grep -oP 'iter=\K[0-9]+' "$SUMMARY_FILE" | tail -1)
    START_ITER=$(( ${LAST:-0} + 1 ))
  else
    START_ITER=1
  fi
fi

PROMPT='Read CLAUDE.md. Determine current state from the project documents. Execute one iteration of the autonomous loop defined in CLAUDE.md under Automation. Use the project slash commands (/phase-plan, /step-done, /phase-review, /phase-complete) as appropriate for the current loop state. Then exit. Your final output MUST end with exactly two lines:
LOOP_SIGNAL: CONTINUE | ESCALATE
REASON: [one line — what was done or why stopping]'

mkdir -p "$LOG_DIR"
cd "$PROJECT_DIR"

FINAL_EXIT=0
ITER=$START_ITER
END_ITER=$(( START_ITER + MAX_ITERATIONS - 1 ))

while [[ $ITER -le $END_ITER ]]; do
  ITER_PAD=$(printf "%03d" "$ITER")
  JSONL_FILE="$LOG_DIR/iteration_${ITER_PAD}.jsonl"
  TXT_FILE="$LOG_DIR/iteration_${ITER_PAD}.txt"
  META_FILE="$LOG_DIR/.iteration_meta.tmp"

  echo "=== Iteration $ITER — $(date -Iseconds) ==="

  # Run one iteration with full stream-json logging.
  claude -p "$PROMPT" \
    --dangerously-skip-permissions \
    --model opus \
    --max-budget-usd 10.00 \
    --output-format stream-json \
    --verbose \
    2>&1 > "$JSONL_FILE"

  EXIT_CODE=$?

  # Parse jsonl: generate human-readable transcript + extract metadata
  python3 "$PROJECT_DIR/tools/parse_jsonl.py" --meta "$META_FILE" < "$JSONL_FILE" > "$TXT_FILE"

  # Read metadata
  COST="" ; TURNS="" ; DURATION="" ; RESULT_TEXT=""
  if [[ -f "$META_FILE" ]]; then
    COST=$(grep '^COST=' "$META_FILE" | cut -d= -f2)
    TURNS=$(grep '^TURNS=' "$META_FILE" | cut -d= -f2)
    DURATION=$(grep '^DURATION=' "$META_FILE" | cut -d= -f2)
    RESULT_TEXT=$(grep '^RESULT_TEXT=' "$META_FILE" | cut -d= -f2- | python3 -c "import sys,json; print(json.loads(sys.stdin.read()))" 2>/dev/null || echo "")
    rm -f "$META_FILE"
  fi

  # Extract signal from result text
  SIGNAL=$(echo "$RESULT_TEXT" | grep -oP 'LOOP_SIGNAL: \K\w+' || echo "")
  REASON=$(echo "$RESULT_TEXT" | grep -oP 'REASON: \K.+' || echo "")

  # Write summary line
  TIMESTAMP=$(date -Iseconds)
  echo "$TIMESTAMP | iter=$ITER | signal=$SIGNAL | exit=$EXIT_CODE | cost=\$$COST | turns=$TURNS | duration=$DURATION | reason=$REASON" >> "$SUMMARY_FILE"

  # Print summary to stdout for orchestrator
  echo "Signal=$SIGNAL | Cost=\$$COST | Turns=$TURNS | Duration=$DURATION"
  echo "Reason: $REASON"

  # Decide whether to continue
  if [[ "$SIGNAL" == "ESCALATE" ]]; then
    echo "=== ESCALATED at iteration $ITER: $REASON ==="
    FINAL_EXIT=1
    break
  elif [[ "$SIGNAL" != "CONTINUE" ]]; then
    echo "=== NO SIGNAL at iteration $ITER — ERROR STOP ==="
    FINAL_EXIT=2
    break
  fi

  echo "=== Iteration $ITER complete ==="
  ITER=$(( ITER + 1 ))
done

echo "=== Stopped after iteration $ITER ==="
exit $FINAL_EXIT
