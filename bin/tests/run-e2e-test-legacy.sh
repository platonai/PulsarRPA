#!/usr/bin/env bash

# Resolve repo root (first parent containing VERSION)
script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd -P)"
AppHome="$script_dir"
while [[ "$AppHome" != "/" && ! -f "$AppHome/VERSION" ]]; do
    AppHome="$(cd "$AppHome/.." && pwd -P)"
done
cd "$AppHome" || exit 1

set -e

# Flag to control whether to run scrape-async.sh test (default: 0 = disabled)
RUN_SCRAPE_ASYNC="${RUN_SCRAPE_ASYNC:-0}"

# Minimum success rate for overall pass (percentage, integer)
INTEGRATION_MIN_SUCCESS_RATE="${INTEGRATION_MIN_SUCCESS_RATE:-60}"

# Planned tests: scrape, optional scrape-async, legacy curl cases
PLANNED_TESTS=3
if [[ "$RUN_SCRAPE_ASYNC" != "1" ]]; then
  PLANNED_TESTS=$((PLANNED_TESTS - 1))
fi
TOTAL_TESTS=$PLANNED_TESTS
EXECUTED_TESTS=0
PASSED_TESTS=0
FAILED_TESTS=0
SKIPPED_TESTS=0
TEST_INDEX=0

echo "===================================================================="
echo "End to end test suite starting at $(date)"
echo "Minimum success rate to pass: ${INTEGRATION_MIN_SUCCESS_RATE}%"
echo "--------------------------------------------------------------------"

TEST_INDEX=$((TEST_INDEX + 1))
echo "[TEST ${TEST_INDEX}/${PLANNED_TESTS}] Running scrape.sh end to end test..."
echo "--------------------------------------------------------------------"
# Start time for TEST 2
start_time="$(date '+%Y-%m-%d %H:%M:%S %Z')"
start_epoch=$(date +%s)
echo "Start time: $start_time"
# Run TEST 2 with explicit error capture
set +e
"$AppHome"/bin/scrape.sh
exit_code=$?
set -e
# End time for TEST 2
end_time="$(date '+%Y-%m-%d %H:%M:%S %Z')"
end_epoch=$(date +%s)
duration=$((end_epoch - start_epoch))
EXECUTED_TESTS=$((EXECUTED_TESTS + 1))
if [ $exit_code -eq 0 ]; then
  echo "[PASS] End to end test scrape.sh completed successfully"
  PASSED_TESTS=$((PASSED_TESTS + 1))
else
  echo "[FAIL] End to end test scrape.sh failed with exit code $exit_code"
  FAILED_TESTS=$((FAILED_TESTS + 1))
fi
echo "End time: $end_time (Duration: ${duration}s)"
echo "===================================================================="

if [ "$RUN_SCRAPE_ASYNC" != "1" ]; then
  echo "[SKIP] scrape-async.sh test is disabled (RUN_SCRAPE_ASYNC=1 to enable)."
  SKIPPED_TESTS=$((SKIPPED_TESTS + 1))
else
  TEST_INDEX=$((TEST_INDEX + 1))
  echo "[TEST ${TEST_INDEX}/${PLANNED_TESTS}] Running scrape-async.sh end to end test with parameters:"
  echo "      - Seeds file: ./bin/seeds.txt"
  echo "      - Max concurrent tasks: 10"
  echo "--------------------------------------------------------------------"
  # Start time for TEST 3
  start_time="$(date '+%Y-%m-%d %H:%M:%S %Z')"
  start_epoch=$(date +%s)
  echo "Start time: $start_time"
  # Run TEST 3 with explicit error capture
  set +e
  "$AppHome"/bin/scrape-async.sh -f ./bin/seeds.txt -m 10
  exit_code=$?
  set -e
  # End time for TEST 3
  end_time="$(date '+%Y-%m-%d %H:%M:%S %Z')"
  end_epoch=$(date +%s)
  duration=$((end_epoch - start_epoch))
  EXECUTED_TESTS=$((EXECUTED_TESTS + 1))
  if [ $exit_code -eq 0 ]; then
    echo "[PASS] End to end test scrape-async.sh completed successfully"
    PASSED_TESTS=$((PASSED_TESTS + 1))
  else
    echo "[FAIL] End to end test scrape-async.sh failed with exit code $exit_code"
    FAILED_TESTS=$((FAILED_TESTS + 1))
  fi
  echo "End time: $end_time (Duration: ${duration}s)"
fi
echo "===================================================================="

TEST_INDEX=$((TEST_INDEX + 1))
echo "[TEST ${TEST_INDEX}/${PLANNED_TESTS}] Running run-test-cases.sh end to end test"
echo "--------------------------------------------------------------------"
# Start time for TEST 4
start_time="$(date '+%Y-%m-%d %H:%M:%S %Z')"
start_epoch=$(date +%s)
echo "Start time: $start_time"
# Run TEST 4 with explicit error capture
set +e
bash "$AppHome"/bin/tests/run-test-cases.sh
exit_code=$?
set -e
# End time for TEST 4
end_time="$(date '+%Y-%m-%d %H:%M:%S %Z')"
end_epoch=$(date +%s)
duration=$((end_epoch - start_epoch))
EXECUTED_TESTS=$((EXECUTED_TESTS + 1))
if [ $exit_code -eq 0 ]; then
  echo "[PASS] End to end test run-test-cases.sh completed successfully"
  PASSED_TESTS=$((PASSED_TESTS + 1))
else
  echo "[FAIL] End to end test run-test-cases.sh failed with exit code $exit_code"
  FAILED_TESTS=$((FAILED_TESTS + 1))
fi

echo "End time: $end_time (Duration: ${duration}s)"

echo "===================================================================="
# Final summary and pass/fail decision
SUCCESS_RATE=0
if [ "$EXECUTED_TESTS" -gt 0 ]; then
  SUCCESS_RATE=$(( PASSED_TESTS * 100 / EXECUTED_TESTS ))
fi

echo "End to end tests completed at $(date)"
echo "--------------------------------------------------------------------"
echo "Planned tests:   $TOTAL_TESTS"
echo "Executed tests:  $EXECUTED_TESTS"
echo "Passed:          $PASSED_TESTS"
echo "Failed:          $FAILED_TESTS"
echo "Skipped:         $SKIPPED_TESTS"
echo "Success rate:    ${SUCCESS_RATE}% (minimum required: ${INTEGRATION_MIN_SUCCESS_RATE}%)"
echo "===================================================================="

if [ "$EXECUTED_TESTS" -eq 0 ]; then
  echo "No end to end tests were executed. Failing the build to avoid false positives."
  exit 1
fi

if [ "$SUCCESS_RATE" -lt "$INTEGRATION_MIN_SUCCESS_RATE" ]; then
  echo "Overall result: FAIL (success rate below threshold)"
  exit 1
else
  echo "Overall result: PASS"
  exit 0
fi
