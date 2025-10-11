#!/usr/bin/env bash

# Find the first parent directory containing the VERSION file
AppHome="$(dirname "$(readlink -f "$0")")"
while [[ "$AppHome" != "/" && ! -f "$AppHome/VERSION" ]]; do
    AppHome="$(dirname "$AppHome")"
done
cd "$AppHome" || exit 1

set -e

# Flag to control whether to run scrape-async.sh test (default: 0 = disabled)
RUN_SCRAPE_ASYNC="${RUN_SCRAPE_ASYNC:-0}"

echo "===================================================================="
echo "[TEST 1/4] Running command-sse.sh integration test..."
echo "--------------------------------------------------------------------"
# Start time for TEST 1
start_time="$(date '+%Y-%m-%d %H:%M:%S %Z')"
start_epoch=$(date +%s)
echo "Start time: $start_time"
# Run TEST 1 with explicit error capture
set +e
"$AppHome"/bin/command-sse.sh
exit_code=$?
set -e
# End time for TEST 1
end_time="$(date '+%Y-%m-%d %H:%M:%S %Z')"
end_epoch=$(date +%s)
duration=$((end_epoch - start_epoch))
if [ $exit_code -eq 0 ]; then
  echo "[PASS] Integration test command-sse.sh completed successfully"
else
  echo "[FAIL] Integration test command-sse.sh failed with exit code $exit_code"
  echo "End time: $end_time (Duration: ${duration}s)"
  exit 1
fi
echo "End time: $end_time (Duration: ${duration}s)"
echo "===================================================================="

echo "[TEST 2/4] Running scrape.sh integration test..."
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
if [ $exit_code -eq 0 ]; then
  echo "[PASS] Integration test scrape.sh completed successfully"
else
  echo "[FAIL] Integration test scrape.sh failed with exit code $exit_code"
  echo "End time: $end_time (Duration: ${duration}s)"
  exit 1
fi
echo "End time: $end_time (Duration: ${duration}s)"
echo "===================================================================="

echo "[TEST 3/4] Running scrape-async.sh integration test with parameters:"
echo "      - Seeds file: ./bin/seeds.txt"
echo "      - Max concurrent tasks: 10"
echo "--------------------------------------------------------------------"
if [ "$RUN_SCRAPE_ASYNC" != "1" ]; then
  echo "[SKIP] scrape-async.sh test is disabled. Set RUN_SCRAPE_ASYNC=1 to enable."
else
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
  if [ $exit_code -eq 0 ]; then
    echo "[PASS] Integration test scrape-async.sh completed successfully"
  else
    echo "[FAIL] Integration test scrape-async.sh failed with exit code $exit_code"
    echo "End time: $end_time (Duration: ${duration}s)"
    exit 1
  fi
  echo "End time: $end_time (Duration: ${duration}s)"
fi
echo "===================================================================="

echo "[TEST 4/4] Running test-curl-commands.sh integration test"
echo "--------------------------------------------------------------------"
# Start time for TEST 4
start_time="$(date '+%Y-%m-%d %H:%M:%S %Z')"
start_epoch=$(date +%s)
echo "Start time: $start_time"
# Run TEST 4 with explicit error capture
set +e
"$AppHome"/bin/tests/test-curl-commands.sh
exit_code=$?
set -e
# End time for TEST 4
end_time="$(date '+%Y-%m-%d %H:%M:%S %Z')"
end_epoch=$(date +%s)
duration=$((end_epoch - start_epoch))
if [ $exit_code -eq 0 ]; then
  echo "[PASS] Integration test test-curl-commands.sh completed successfully"
else
  echo "[FAIL] Integration test test-curl-commands.sh failed with exit code $exit_code"
  echo "End time: $end_time (Duration: ${duration}s)"
  exit 1
fi

echo "End time: $end_time (Duration: ${duration}s)"

echo "===================================================================="
echo "All integration tests passed successfully!"

echo "===================================================================="
echo "Integration tests completed at $(date)"
echo "===================================================================="
