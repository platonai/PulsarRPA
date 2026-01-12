#!/usr/bin/env bash

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd -P)"

"$script_dir/test-cases/run-e2e-tests.sh" "$@" || true
"$script_dir/test-cases/run-e2e-agents.sh" "$@" || true
