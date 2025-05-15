#!/usr/bin/env bash

set -e

# Run scrape.sh integration test
./bin/scrape.sh || { echo "Integration test failed - scrape.sh"; exit 1; }

# Run scrape-async.sh integration test
./bin/scrape-async.sh -f ./bin/seeds.txt -m 100 || { echo "Integration test failed - scrape-async.sh"; exit 1; } && echo "Integration test passed"
