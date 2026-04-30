#!/bin/bash
# run-iteration.sh — Execute autonomous loop iterations
# Called by the orchestrator (TG bot session) or standalone.
#
# Usage:
#   ./run-iteration.sh                       # single iteration, Claude backend
#   ./run-iteration.sh --backend codex       # single iteration, Codex backend
#   ./run-iteration.sh -n 5                  # run up to 5 iterations (stops on ESCALATE)
#   ./run-iteration.sh -n 5 --start 3        # start from iteration 3
#   ./run-iteration.sh --backend codex -n 3  # 3 Codex iterations
#
# Output:
#   logs/loop/iteration_NNN.jsonl  — full stream-json transcript (Claude) or JSONL (Codex)
#   logs/loop/iteration_NNN.txt    — human-readable summary
#   logs/loop/summary.log          — one line per iteration
#
# Exit: 0=all CONTINUE, 1=ESCALATE, 2=NO_SIGNAL/ERROR
#
# Dependencies: python3 (for JSON parsing — jq not available in this environment)

set -uo pipefail

PROJECT_DIR="/home/claude/workspace/PoP_port"
LOG_DIR="$PROJECT_DIR/logs/loop"
SUMMARY_FILE="$LOG_DIR/summary.log"

# Parse arguments
MAX_ITERATIONS=1
START_ITER=""
BACKEND="claude"

while [[ $# -gt 0 ]]; do
  case $1 in
    -n|--iterations) MAX_ITERATIONS="$2"; shift 2 ;;
    --start)         START_ITER="$2"; shift 2 ;;
    --backend)       BACKEND="$2"; shift 2 ;;
    *)               echo "Unknown option: $1"; exit 2 ;;
  esac
done

# Validate backend
case $BACKEND in
  claude|codex) ;;
  *) echo "Unknown backend: $BACKEND (must be claude or codex)"; exit 2 ;;
esac

# Auto-determine start iteration from summary log
if [[ -z "$START_ITER" ]]; then
  if [[ -f "$SUMMARY_FILE" ]]; then
    LAST=$(grep -oP 'iter=\K[0-9]+' "$SUMMARY_FILE" | tail -1)
    START_ITER=$(( ${LAST:-0} + 1 ))
  else
    START_ITER=1
  fi
fi

# Backend-neutral prompt — references the adapter file by backend name
case $BACKEND in
  claude) ADAPTER_FILE="CLAUDE.md" ;;
  codex)  ADAPTER_FILE="CODEX.md" ;;
esac

PROMPT="MANDATORY FIRST STEP: Read ${ADAPTER_FILE} now. It contains references to WORKER_SPEC.md and project documents — read all of them before doing anything else.

You are a stateless worker. You have no memory of previous iterations. Reconstruct all state from files.

After reading ${ADAPTER_FILE} and its references, determine current state from DEVPLAN.md. Execute exactly one action per the Worker Spec.

Your final output MUST end with exactly these four lines — no text after:
LOOP_SIGNAL: CONTINUE | ESCALATE
REASON: <one line — what was done or why stopping>
ACTION_TYPE: PHASE_PLAN | STEP | REVIEW | COMPLETE
ACTION_ID: <module.phase.step>"

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
  LAST_MSG_FILE="$LOG_DIR/.last_message.tmp"

  echo "=== Iteration $ITER ($BACKEND) — $(date -Iseconds) ==="

  case $BACKEND in
    claude)
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
      COST="" ; TURNS="" ; DURATION=""
      if [[ -f "$META_FILE" ]]; then
        COST=$(grep '^COST=' "$META_FILE" | cut -d= -f2)
        TURNS=$(grep '^TURNS=' "$META_FILE" | cut -d= -f2)
        DURATION=$(grep '^DURATION=' "$META_FILE" | cut -d= -f2)
        RESULT_TEXT=$(grep '^RESULT_TEXT=' "$META_FILE" | cut -d= -f2- | python3 -c "import sys,json; print(json.loads(sys.stdin.read()))" 2>/dev/null || echo "")
        rm -f "$META_FILE"
      fi
      ;;

    codex)
      START_TIME=$(date +%s)

      codex exec "$PROMPT" \
        --dangerously-bypass-approvals-and-sandbox \
        -o "$LAST_MSG_FILE" \
        --json \
        2>&1 > "$JSONL_FILE"
      EXIT_CODE=$?

      END_TIME=$(date +%s)
      DURATION=$(( END_TIME - START_TIME ))s

      # Extract last message as the result text
      RESULT_TEXT=""
      if [[ -f "$LAST_MSG_FILE" ]]; then
        RESULT_TEXT=$(cat "$LAST_MSG_FILE")
        rm -f "$LAST_MSG_FILE"
      fi

      # Generate human-readable transcript from JSONL
      # Codex uses nested item.completed events — normalize to same .txt format as Claude
      if [[ -f "$JSONL_FILE" ]]; then
        python3 -c "
import sys, json
for line in sys.stdin:
    line = line.strip()
    if not line: continue
    try:
        ev = json.loads(line)
        t = ev.get('type','')
        if t != 'item.completed': continue
        item = ev.get('item', {})
        itype = item.get('type','')
        if itype == 'agent_message':
            print('ASSISTANT:', item.get('text',''))
        elif itype == 'command_execution':
            cmd = item.get('command','')
            status = item.get('status','')
            exit_code = item.get('exit_code','')
            output = item.get('aggregated_output','')
            print(f'TOOL CALL: Bash({cmd[:120]})')
            if status == 'failed' or (exit_code and exit_code != 0):
                print(f'  -> Result (FAILED exit={exit_code}): {output[:200]}')
            else:
                print(f'  -> Result: {output[:200]}')
        elif itype == 'file_read':
            print(f'TOOL CALL: Read({item.get(\"path\",\"\")})')
            content = item.get('content','') or ''
            lines = content.count(chr(10))
            print(f'  -> Read {item.get(\"path\",\"\")} ({lines} lines)')
        elif itype == 'file_write':
            print(f'TOOL CALL: Write({item.get(\"path\",\"\")})')
            print(f'  -> File created successfully at: {item.get(\"path\",\"\")}')
        elif itype == 'file_edit':
            print(f'TOOL CALL: Edit({item.get(\"path\",\"\")})')
            print(f'  -> Result: The file {item.get(\"path\",\"\")} has been updated successfully.')
    except: pass
" < "$JSONL_FILE" > "$TXT_FILE"
      fi

      COST="n/a"
      TURNS=$(grep -c '"type":"item.completed"' "$JSONL_FILE" 2>/dev/null || echo "0")
      ;;
  esac

  # Extract signal fields from result text (works for both backends)
  SIGNAL=$(echo "$RESULT_TEXT" | grep -oP 'LOOP_SIGNAL: \K\w+' || echo "")
  REASON=$(echo "$RESULT_TEXT" | grep -oP 'REASON: \K.+' || echo "")
  ACTION_TYPE=$(echo "$RESULT_TEXT" | grep -oP 'ACTION_TYPE: \K\w+' || echo "")
  ACTION_ID=$(echo "$RESULT_TEXT" | grep -oP 'ACTION_ID: \K\S+' || echo "")

  # Write summary line
  TIMESTAMP=$(date -Iseconds)
  echo "$TIMESTAMP | iter=$ITER | backend=$BACKEND | signal=$SIGNAL | exit=$EXIT_CODE | cost=\$$COST | turns=$TURNS | duration=$DURATION | action=$ACTION_TYPE | id=$ACTION_ID | reason=$REASON" >> "$SUMMARY_FILE"

  # Print summary to stdout for orchestrator
  echo "Backend=$BACKEND | Signal=$SIGNAL | Cost=\$$COST | Turns=$TURNS | Duration=$DURATION"
  echo "Action: $ACTION_TYPE ($ACTION_ID)"
  echo "Reason: $REASON"

  # Track the last iter that actually ran — used for accurate post-loop reporting
  # regardless of whether the loop exits naturally or via break below.
  LAST_RAN=$ITER

  # Decide whether to continue
  if [[ "$SIGNAL" == "ESCALATE" ]]; then
    # Distinguish clean phase-boundary escalations (ACTION_TYPE=COMPLETE — the
    # worker finished a phase and is handing off for human audit per WORKER_SPEC
    # §6) from problem escalations (3 failures, regime shift, scope creep,
    # contract change, unclear spec — same signal, but a real problem).
    if [[ "$ACTION_TYPE" == "COMPLETE" ]]; then
      echo "=== Phase-boundary at iteration $ITER (awaiting human audit): $REASON ==="
      FINAL_EXIT=0
    else
      echo "=== ESCALATED at iteration $ITER (action=$ACTION_TYPE): $REASON ==="
      FINAL_EXIT=1
    fi
    break
  elif [[ "$SIGNAL" != "CONTINUE" ]]; then
    echo "=== NO SIGNAL at iteration $ITER — ERROR STOP ==="
    FINAL_EXIT=2
    break
  fi

  echo "=== Iteration $ITER complete ==="
  ITER=$(( ITER + 1 ))
done

echo "=== Stopped after iteration ${LAST_RAN:-$START_ITER} ==="
exit $FINAL_EXIT
