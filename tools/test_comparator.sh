#!/usr/bin/env bash
#
# Test script for compare_traces.py
#
# Validates that the comparator correctly:
# 1. Reports MATCH for identical traces
# 2. Reports DIVERGENCE for corrupted traces with exact field location
#
# Exit codes:
#   0 = all tests pass
#   1 = test failure
#   2 = setup error

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
COMPARATOR="$SCRIPT_DIR/compare_traces.py"
TRACES_DIR="$SCRIPT_DIR/../SDLPoP/traces/reference"

# Colors for output (if terminal supports it)
if [ -t 1 ]; then
    GREEN='\033[0;32m'
    RED='\033[0;31m'
    YELLOW='\033[1;33m'
    NC='\033[0m' # No Color
else
    GREEN=''
    RED=''
    YELLOW=''
    NC=''
fi

echo "=== State Trace Comparator Test Suite ==="
echo

# Check comparator exists
if [ ! -f "$COMPARATOR" ]; then
    echo -e "${RED}ERROR: Comparator not found at $COMPARATOR${NC}"
    exit 2
fi

# Check python3 available
if ! command -v python3 &> /dev/null; then
    echo -e "${RED}ERROR: python3 not found in PATH${NC}"
    exit 2
fi

# Find a reference trace to test with
if [ ! -d "$TRACES_DIR" ]; then
    echo -e "${RED}ERROR: Traces directory not found: $TRACES_DIR${NC}"
    exit 2
fi

REFERENCE_TRACE=$(find "$TRACES_DIR" -name "*.trace" -type f | head -n 1)
if [ -z "$REFERENCE_TRACE" ]; then
    echo -e "${RED}ERROR: No .trace files found in $TRACES_DIR${NC}"
    exit 2
fi

echo "Using test trace: $(basename "$REFERENCE_TRACE")"
echo

# Create temp directory for test files
TEMP_DIR=$(mktemp -d)
trap "rm -rf $TEMP_DIR" EXIT

TEST_TRACE="$TEMP_DIR/test.trace"
CORRUPTED_TRACE="$TEMP_DIR/corrupted.trace"

# ============================================================================
# Test 1: Identical traces should match
# ============================================================================
echo -e "${YELLOW}Test 1: Identical traces${NC}"
cp "$REFERENCE_TRACE" "$TEST_TRACE"

if python3 "$COMPARATOR" "$REFERENCE_TRACE" "$TEST_TRACE" | grep -q "MATCH"; then
    echo -e "${GREEN}✓ PASS: Identical traces correctly reported as MATCH${NC}"
else
    echo -e "${RED}✗ FAIL: Identical traces did not match${NC}"
    exit 1
fi

if python3 "$COMPARATOR" "$REFERENCE_TRACE" "$TEST_TRACE" > /dev/null; then
    echo -e "${GREEN}✓ PASS: Exit code 0 for matching traces${NC}"
else
    echo -e "${RED}✗ FAIL: Non-zero exit code for matching traces${NC}"
    exit 1
fi

echo

# ============================================================================
# Test 2: Corrupted trace should report divergence
# ============================================================================
echo -e "${YELLOW}Test 2: Corrupted trace (byte 320 = frame 1, offset 10)${NC}"
cp "$REFERENCE_TRACE" "$CORRUPTED_TRACE"

# Corrupt byte at offset 320 (frame 1, offset 10 within frame = Kid.repeat field)
# Frame 0: bytes 0-309
# Frame 1: bytes 310-619
# Byte 320 = frame 1, offset 10 = Kid byte offset 6 (within Kid struct at frame offset 4)
# Kid struct offset 4+6 = 10 → this is Kid.repeat field

if command -v python3 &> /dev/null; then
    # Use Python to corrupt a single byte (cross-platform)
    python3 -c "
data = open('$CORRUPTED_TRACE', 'rb').read()
data = bytearray(data)
data[320] = (data[320] + 1) % 256  # Flip one byte
open('$CORRUPTED_TRACE', 'wb').write(data)
"
else
    echo -e "${RED}ERROR: python3 required for byte corruption${NC}"
    exit 2
fi

# Run comparator (expect exit code 1 for divergence)
if python3 "$COMPARATOR" "$REFERENCE_TRACE" "$CORRUPTED_TRACE" > "$TEMP_DIR/output.txt" 2>&1; then
    echo -e "${RED}✗ FAIL: Comparator returned 0 for corrupted trace${NC}"
    cat "$TEMP_DIR/output.txt"
    exit 1
fi

if grep -q "DIVERGENCE" "$TEMP_DIR/output.txt"; then
    echo -e "${GREEN}✓ PASS: Divergence correctly detected${NC}"
else
    echo -e "${RED}✗ FAIL: Divergence not reported${NC}"
    cat "$TEMP_DIR/output.txt"
    exit 1
fi

if grep -q "frame 1" "$TEMP_DIR/output.txt"; then
    echo -e "${GREEN}✓ PASS: Correct frame number reported${NC}"
else
    echo -e "${RED}✗ FAIL: Frame number not reported correctly${NC}"
    cat "$TEMP_DIR/output.txt"
    exit 1
fi

if grep -q "Kid" "$TEMP_DIR/output.txt"; then
    echo -e "${GREEN}✓ PASS: Field name reported${NC}"
else
    echo -e "${RED}✗ FAIL: Field name not reported${NC}"
    cat "$TEMP_DIR/output.txt"
    exit 1
fi

echo
echo "Comparator output:"
cat "$TEMP_DIR/output.txt"
echo

# ============================================================================
# Test 3: Different length traces
# ============================================================================
echo -e "${YELLOW}Test 3: Different length traces${NC}"
TRUNCATED_TRACE="$TEMP_DIR/truncated.trace"
head -c 620 "$REFERENCE_TRACE" > "$TRUNCATED_TRACE"  # Only 2 frames

if python3 "$COMPARATOR" "$REFERENCE_TRACE" "$TRUNCATED_TRACE" > "$TEMP_DIR/output2.txt" 2>&1; then
    echo -e "${RED}✗ FAIL: Comparator returned 0 for length mismatch${NC}"
    exit 1
fi

if grep -q "Frame count mismatch\|DIVERGENCE" "$TEMP_DIR/output2.txt"; then
    echo -e "${GREEN}✓ PASS: Length mismatch detected${NC}"
else
    echo -e "${RED}✗ FAIL: Length mismatch not reported${NC}"
    cat "$TEMP_DIR/output2.txt"
    exit 1
fi

echo

# ============================================================================
# Summary
# ============================================================================
echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}All tests passed!${NC}"
echo -e "${GREEN}========================================${NC}"
exit 0
