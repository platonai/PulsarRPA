#!/usr/bin/env bash

# Call run-e2e-test-legacy.sh
script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd -P)"
"$script_dir/run-e2e-test-legacy.sh"
exit_code=$?

# If the legacy test script failed, exit with its code
if [ $exit_code -ne 0 ]; then
  exit $exit_code
fi

# Run run-e2e-tests-agents.sh
"$script_dir/run-e2e-tests-agents.sh"
exit_code=$?
exit $exit_code
