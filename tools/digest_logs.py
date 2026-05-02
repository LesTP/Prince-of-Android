#!/usr/bin/env python3
"""
Digest iteration .txt logs into a concise summary.
Extracts: errors, test failures, file creations, commits, and key decisions.
Reduces ~300-500 line logs to ~10-30 lines each.

Usage:
    python3 tools/digest_logs.py logs/loop/iteration_008.txt [iteration_009.txt ...]
    python3 tools/digest_logs.py logs/loop/iteration_{008..012}.txt
    python3 tools/digest_logs.py --range 8 12       # iterations 8-12
"""

import sys
import re
import os

def digest_log(filepath):
    """Extract interesting events from a .txt iteration log."""
    if not os.path.exists(filepath):
        return f"[File not found: {filepath}]"

    with open(filepath, 'r') as f:
        lines = f.readlines()

    events = []
    iter_num = re.search(r'iteration_(\d+)', filepath)
    iter_label = f"Iter {int(iter_num.group(1))}" if iter_num else os.path.basename(filepath)

    for i, line in enumerate(lines):
        line = line.rstrip()

        # Compilation errors
        if line.startswith('e: file:') or 'Task :compileKotlin FAILED' in line:
            # Extract just the error message
            match = re.search(r'\.kt:(\d+):\d+ (.+)', line)
            if match:
                events.append(f"  COMPILE ERROR line {match.group(1)}: {match.group(2)}")
            elif 'FAILED' in line:
                events.append(f"  COMPILE FAILED")

        # Test failures
        if 'FAILED' in line and ('Test >' in line or 'test' in line.lower()):
            events.append(f"  TEST FAIL: {line.strip()}")
        if 'AssertionFailedError' in line or 'expected:' in line:
            events.append(f"    {line.strip()}")

        # File creation
        if 'File created successfully' in line:
            match = re.search(r'at: .*/([^/]+)$', line)
            if match:
                events.append(f"  CREATED: {match.group(1)}")

        # Git commits
        if line.startswith('  -> Result: [master') or ('Result: [master' in line and 'commit' not in line.lower()):
            match = re.search(r'\[master ([a-f0-9]+)\] (.+)', line)
            if match:
                events.append(f"  COMMIT {match.group(1)[:7]}: {match.group(2)}")

        # Edit tool errors
        if 'File has not been read yet' in line:
            events.append(f"  EDIT ERROR: File not read yet (wasted turn)")

        # Agent subagent spawning
        if line.startswith('TOOL CALL: Agent('):
            events.append(f"  SUBAGENT: Agent(Explore) spawned")

        # Grep/Glob failures on existing files (heuristic: "No matches found" or "does not exist")
        if 'Directory does not exist' in line and 'sdlpop' in line.lower():
            events.append(f"  TOOL FAIL: Glob path not found")

        # LOOP_SIGNAL
        if line.startswith('LOOP_SIGNAL:'):
            events.append(f"  {line}")
        if line.startswith('REASON:'):
            events.append(f"  {line}")

        # Cost line
        if line.startswith('Cost:'):
            events.append(f"  {line}")

    # Count Grep "No matches found" as a single summary
    grep_fails = sum(1 for l in lines if 'No matches found' in l)
    if grep_fails > 2:
        events.insert(0, f"  GREP/GLOB FAILURES: {grep_fails} failed searches (path-with-spaces issue)")

    # Count total tool calls
    tool_calls = sum(1 for l in lines if l.startswith('TOOL CALL:'))

    header = f"=== {iter_label} ({tool_calls} tool calls, {len(lines)} log lines) ==="

    if not events:
        events.append("  (no notable events)")

    return header + "\n" + "\n".join(events)


def main():
    files = []

    if len(sys.argv) < 2:
        print(__doc__)
        sys.exit(1)

    if sys.argv[1] == '--range' and len(sys.argv) >= 4:
        start, end = int(sys.argv[2]), int(sys.argv[3])
        log_dir = sys.argv[4] if len(sys.argv) > 4 else "logs/loop"
        files = [os.path.join(log_dir, f"iteration_{i:03d}.txt") for i in range(start, end + 1)]
    else:
        files = sys.argv[1:]

    for filepath in files:
        print(digest_log(filepath))
        print()


if __name__ == '__main__':
    main()
