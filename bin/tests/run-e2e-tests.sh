#!/usr/bin/env bash

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd -P)"
RUN_E2E_BASICS="${RUN_E2E_BASICS:-false}"

if [[ "$RUN_E2E_BASICS" == "true" ]]; then
  "$script_dir/test-cases/run-test-cases.sh" "$@"
  exit_code=$?
  if [ $exit_code -ne 0 ]; then
    exit $exit_code
  fi
fi

"$script_dir/run-e2e-tests-agents.sh" "$@"
exit_code=$?
exit $exit_code
