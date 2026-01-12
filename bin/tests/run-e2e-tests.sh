#!/usr/bin/env bash

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd -P)"
RUN_LEGACY_E2E="${RUN_LEGACY_E2E:-false}"

if [[ "$RUN_LEGACY_E2E" == "true" ]]; then
  "$script_dir/run-e2e-test-legacy.sh" "$@"
  exit_code=$?
  if [ $exit_code -ne 0 ]; then
    exit $exit_code
  fi
fi

"$script_dir/run-e2e-tests-agents.sh" "$@"
exit_code=$?
exit $exit_code
