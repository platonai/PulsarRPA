#!/usr/bin/env bash

set -e

echo "===================================================================="
echo "[TEST 1/3] Running command-sse.sh integration test..."
echo "--------------------------------------------------------------------"
./bin/command-sse.sh
if [ $? -eq 0 ]; then
  echo "[PASS] Integration test command-sse.sh completed successfully"
else
  echo "[FAIL] Integration test command-sse.sh failed with exit code $?"
  exit 1
fi
echo "===================================================================="

echo "[TEST 2/3] Running scrape.sh integration test..."
echo "--------------------------------------------------------------------"
./bin/scrape.sh
if [ $? -eq 0 ]; then
  echo "[PASS] Integration test scrape.sh completed successfully"
else
  echo "[FAIL] Integration test scrape.sh failed with exit code $?"
  exit 1
fi
echo "===================================================================="

echo "[TEST 3/3] Running scrape-async.sh integration test with parameters:"
echo "      - Seeds file: ./bin/seeds.txt"
echo "      - Max concurrent tasks: 10"
echo "--------------------------------------------------------------------"
./bin/scrape-async.sh -f ./bin/seeds.txt -m 10
if [ $? -eq 0 ]; then
  echo "[PASS] Integration test scrape-async.sh completed successfully"
else
  echo "[FAIL] Integration test scrape-async.sh failed with exit code $?"
  exit 1
fi
echo "===================================================================="
echo "All integration tests passed successfully!"
