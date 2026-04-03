#!/bin/bash
# run-iteration.sh — Execute autonomous loop iterations
# Called by the orchestrator (TG bot session) or standalone.
#
# Usage:
#   ./run-iteration.sh                  # single iteration, auto-numbering
#   ./run-iteration.sh -n 5             # run up to 5 iterations (stops on ESCALATE)
#   ./run-iteration.sh -n 5 --start 3   # start from iteration 3, run up to 5
#
# Output: iteration logs in logs/loop/iteration_NNN.log
# Exit:   0=all CONTINUE, 1=ESCALATE, 2=NO_SIGNAL/ERROR

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
  LOG_FILE="$LOG_DIR/iteration_${ITER_PAD}.log"

  echo "=== Iteration $ITER — $(date -Iseconds) ===" | tee "$LOG_FILE"

  # Run one iteration. -p exits after response. --dangerously-skip-permissions
  # allows autonomous file/shell operations.
  claude -p "$PROMPT" \
    --dangerously-skip-permissions \
    --model opus \
    --max-budget-usd 1.00 \
    2>&1 | tee -a "$LOG_FILE"

  EXIT_CODE=${PIPESTATUS[0]}

  # Extract signal from last 10 lines of output
  TAIL=$(tail -10 "$LOG_FILE")
  SIGNAL=$(echo "$TAIL" | grep -oP 'LOOP_SIGNAL: \K\w+' || echo "")
  REASON=$(echo "$TAIL" | grep -oP 'REASON: \K.+' || echo "")

  # Write summary line
  TIMESTAMP=$(date -Iseconds)
  echo "$TIMESTAMP | iter=$ITER | signal=$SIGNAL | exit=$EXIT_CODE | reason=$REASON" >> "$SUMMARY_FILE"

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

  echo "=== Iteration $ITER complete: $REASON ==="
  ITER=$(( ITER + 1 ))
done

echo "=== Stopped after iteration $(( ITER )) ==="
exit $FINAL_EXIT
