#!/usr/bin/env bash

# Find the first parent directory containing the VERSION file
AppHome="$(dirname "$(readlink -f "$0")")"
while [[ "$AppHome" != "/" && ! -f "$AppHome/VERSION" ]]; do
    AppHome="$(dirname "$AppHome")"
done
cd "$AppHome" || exit 1

set -e

echo "===================================================================="
echo "[TEST 1/4] Running command-sse.sh integration test..."
echo "--------------------------------------------------------------------"
"$AppHome"/bin/command-sse.sh
if [ $? -eq 0 ]; then
  echo "[PASS] Integration test command-sse.sh completed successfully"
else
  echo "[FAIL] Integration test command-sse.sh failed with exit code $?"
  exit 1
fi
echo "===================================================================="

echo "[TEST 2/4] Running scrape.sh integration test..."
echo "--------------------------------------------------------------------"
"$AppHome"/bin/scrape.sh
if [ $? -eq 0 ]; then
  echo "[PASS] Integration test scrape.sh completed successfully"
else
  echo "[FAIL] Integration test scrape.sh failed with exit code $?"
  exit 1
fi
echo "===================================================================="

echo "[TEST 3/4] Running scrape-async.sh integration test with parameters:"
echo "      - Seeds file: ./bin/seeds.txt"
echo "      - Max concurrent tasks: 10"
echo "--------------------------------------------------------------------"
"$AppHome"/bin/scrape-async.sh -f ./bin/seeds.txt -m 10
if [ $? -eq 0 ]; then
  echo "[PASS] Integration test scrape-async.sh completed successfully"
else
  echo "[FAIL] Integration test scrape-async.sh failed with exit code $?"
  exit 1
fi
echo "===================================================================="

echo "[TEST 4/4] Running test-curl-commands.sh integration test"
echo "--------------------------------------------------------------------"
"$AppHome"/bin/tests/test-curl-commands.sh
if [ $? -eq 0 ]; then
  echo "[PASS] Integration test test-curl-commands.sh completed successfully"
else
  echo "[FAIL] Integration test test-curl-commands.sh failed with exit code $?"
  exit 1
fi

echo "===================================================================="
echo "All integration tests passed successfully!"

echo "===================================================================="
echo "Integration tests completed at $(date)"
echo "===================================================================="
