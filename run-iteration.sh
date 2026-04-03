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

# extract_readable: parse stream-json into human-readable summary
extract_readable() {
  local jsonl_file="$1"
  local txt_file="$2"

  {
    echo "=== ITERATION TRANSCRIPT ==="
    echo ""

    # Extract assistant text messages and tool calls
    while IFS= read -r line; do
      local type=$(echo "$line" | jq -r '.type // empty' 2>/dev/null)

      case "$type" in
        assistant)
          # Extract text blocks
          echo "$line" | jq -r '
            .message.content[]? |
            if .type == "text" then "ASSISTANT: " + .text
            elif .type == "tool_use" then "TOOL CALL: " + .name + "(" + (.input | keys | join(", ")) + ")"
            else empty end
          ' 2>/dev/null
          ;;
        user)
          # Extract tool results (abbreviated)
          local tool_result=$(echo "$line" | jq -r '.tool_use_result.type // empty' 2>/dev/null)
          if [[ "$tool_result" == "text" ]]; then
            local file_path=$(echo "$line" | jq -r '.tool_use_result.file.filePath // empty' 2>/dev/null)
            if [[ -n "$file_path" ]]; then
              local num_lines=$(echo "$line" | jq -r '.tool_use_result.file.numLines // empty' 2>/dev/null)
              echo "  → Read $file_path ($num_lines lines)"
            else
              # Bash output or other tool result — first 200 chars
              local content=$(echo "$line" | jq -r '.message.content[0].content // empty' 2>/dev/null | head -c 200)
              if [[ -n "$content" ]]; then
                echo "  → Result: $content"
              fi
            fi
          fi
          ;;
        result)
          echo ""
          echo "=== RESULT ==="
          echo "$line" | jq -r '"Cost: $" + (.total_cost_usd | tostring) + " | Turns: " + (.num_turns | tostring) + " | Duration: " + ((.duration_ms / 1000) | tostring) + "s"' 2>/dev/null
          echo "$line" | jq -r '.result // empty' 2>/dev/null | tail -5
          ;;
      esac
    done < "$jsonl_file"
  } > "$txt_file"
}

FINAL_EXIT=0
ITER=$START_ITER
END_ITER=$(( START_ITER + MAX_ITERATIONS - 1 ))

while [[ $ITER -le $END_ITER ]]; do
  ITER_PAD=$(printf "%03d" "$ITER")
  JSONL_FILE="$LOG_DIR/iteration_${ITER_PAD}.jsonl"
  TXT_FILE="$LOG_DIR/iteration_${ITER_PAD}.txt"

  echo "=== Iteration $ITER — $(date -Iseconds) ==="

  # Run one iteration with full stream-json logging.
  # --output-format stream-json captures all tool calls and results.
  # --verbose is required for stream-json.
  claude -p "$PROMPT" \
    --dangerously-skip-permissions \
    --model opus \
    --max-budget-usd 10.00 \
    --output-format stream-json \
    --verbose \
    2>&1 > "$JSONL_FILE"

  EXIT_CODE=$?

  # Generate human-readable summary
  extract_readable "$JSONL_FILE" "$TXT_FILE"

  # Extract signal from the result line in jsonl
  RESULT_LINE=$(grep '"type":"result"' "$JSONL_FILE" | tail -1)
  RESULT_TEXT=$(echo "$RESULT_LINE" | jq -r '.result // ""' 2>/dev/null)
  SIGNAL=$(echo "$RESULT_TEXT" | grep -oP 'LOOP_SIGNAL: \K\w+' || echo "")
  REASON=$(echo "$RESULT_TEXT" | grep -oP 'REASON: \K.+' || echo "")
  COST=$(echo "$RESULT_LINE" | jq -r '.total_cost_usd // ""' 2>/dev/null)
  TURNS=$(echo "$RESULT_LINE" | jq -r '.num_turns // ""' 2>/dev/null)
  DURATION=$(echo "$RESULT_LINE" | jq -r '(.duration_ms / 1000 | floor | tostring) + "s"' 2>/dev/null)

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
