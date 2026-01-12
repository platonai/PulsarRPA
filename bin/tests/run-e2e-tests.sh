#!/usr/bin/env bash

set -euo pipefail

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd -P)"

declare -A SUITE_MAP=(
  [main]="$script_dir/test-cases/run-e2e-tests.sh"
  [agents]="$script_dir/test-cases/run-e2e-agents.sh"
)

ALL_SUITES=("main" "agents")
DEFAULT_SUITES=("main")
selected_suites=()
passthrough=()

usage() {
  cat <<EOF
Usage: $(basename "$0") [--suite main|agents|all] [--] [suite args...]

Options:
  --suite LIST   Comma-separated suites to run (main,agents). Use "all" to run everything.
  --list         Show available suites and exit.
  -h, --help     Show this help message.
All other arguments are forwarded to the selected suite scripts.

Default: only the "main" suite runs.
EOF
}

list_suites() {
  echo "Available suites:"
  for key in "${!SUITE_MAP[@]}"; do
    printf "  - %s -> %s\n" "$key" "${SUITE_MAP[$key]}"
  done
}

parse_args() {
  while [[ $# -gt 0 ]]; do
    case "$1" in
      --suite)
        IFS=',' read -ra selected_suites <<< "$2"
        shift 2
        ;;
      --all)
        selected_suites=("all")
        shift
        ;;
      --list)
        list_suites
        exit 0
        ;;
      -h|--help)
        usage
        exit 0
        ;;
      --)
        shift
        passthrough+=("$@")
        break
        ;;
      *)
        passthrough+=("$1")
        shift
        ;;
    esac
  done
}

resolve_suites() {
  local suites=("$@")
  if [[ ${#suites[@]} -eq 0 ]]; then
    suites=("${DEFAULT_SUITES[@]}")
  fi

  local resolved=()
  for suite in "${suites[@]}"; do
    if [[ "$suite" == "all" ]]; then
      resolved+=("${ALL_SUITES[@]}")
      continue
    fi
    if [[ -z "${SUITE_MAP[$suite]:-}" ]]; then
      echo "Unknown suite: $suite"
      usage
      exit 1
    fi
    resolved+=("$suite")
  done

  printf '%s\n' "${resolved[@]}"
}

run_suite() {
  local suite="$1"
  local script_path="${SUITE_MAP[$suite]}"
  if [[ ! -x "$script_path" ]]; then
    echo "Suite script not found or not executable: $script_path"
    exit 1
  fi
  echo ">>> Running suite '$suite' via $(basename "$script_path")"
  "$script_path" "${passthrough[@]}" || true
}

main() {
  parse_args "$@"
  mapfile -t suites < <(resolve_suites "${selected_suites[@]}")
  for suite in "${suites[@]}"; do
    run_suite "$suite"
  done
}

main "$@"
