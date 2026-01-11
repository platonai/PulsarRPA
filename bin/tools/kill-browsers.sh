#!/usr/bin/env bash

# kill-browsers.sh
# Find Chrome/Chromium browser processes whose command line contains
# --user-data-dir or --profile-directory and kill them safely.
# Supports --dry-run and --timeout <seconds>.

set -o pipefail

DRY_RUN=false
TIMEOUT=5
SHOW_HELP=false

usage() {
  cat <<EOF
Usage: $(basename "$0") [--dry-run] [--timeout N] [--help]

Options:
  --dry-run       Print matching browser processes without killing them.
  --timeout N     Seconds to wait after SIGTERM before using SIGKILL (default: ${TIMEOUT}).
  --help          Show this help message.

Behavior: only processes whose command line contains one of the flags
"--user-data-dir" or "--profile-directory" will be targeted. This avoids
killing system-wide browser processes.
EOF
}

# Parse args
while [[ $# -gt 0 ]]; do
  case "$1" in
    --dry-run)
      DRY_RUN=true; shift;;
    --timeout)
      shift; if [[ -n "$1" && "$1" =~ ^[0-9]+$ ]]; then TIMEOUT="$1"; shift; else echo "Invalid --timeout value" >&2; exit 2; fi;;
    --help|-h)
      usage; exit 0;;
    *)
      echo "Unknown argument: $1" >&2; usage; exit 2;;
  esac
done

# Patterns to match in cmdline
PATTERN1='--user-data-dir='
PATTERN2='--profile-directory='

# Browser name regex for pgrep/ps search (case-insensitive in grep)
BROWSER_REGEX='chrome|chromium|google-chrome|chromium-browser'

# Discover candidate processes: prefer pgrep -af when available
candidates=""
if command -v pgrep >/dev/null 2>&1; then
  # pgrep -af prints: PID cmdline
  candidates=$(pgrep -af -u "$(whoami)" -f "$BROWSER_REGEX" 2>/dev/null || true)
fi

# Fallback to ps if pgrep not available or returned empty
if [[ -z "$candidates" ]]; then
  # Use ps to list pid and full args. On macOS/bsd, args may be truncated differently,
  # but ps -eo pid,args is common on Linux and macOS.
  candidates=$(ps -eo pid,args 2>/dev/null | grep -E -i "$BROWSER_REGEX" | grep -v grep || true)
fi

# Filter candidates to those that contain the required flags
matches=()
while IFS= read -r line; do
  [[ -z "$line" ]] && continue
  # Extract PID and CMD. Line formats:
  # pgrep: "1234 /path/to/chrome --flag ..."
  # ps:    " 1234 /path/to/chrome --flag ..."
  pid="$(echo "$line" | awk '{print $1}')"
  cmd="$(echo "$line" | cut -d' ' -f2- )"
  # If cmd is empty (rare), try to get full command from /proc (Linux)
  if [[ -z "$cmd" && -r "/proc/$pid/cmdline" ]]; then
    cmd=$(tr '\0' ' ' < "/proc/$pid/cmdline")
  fi
  if [[ "$cmd" == *"$PATTERN1"* ]] || [[ "$cmd" == *"$PATTERN2"* ]]; then
    matches+=("$pid:::${cmd}")
  fi
done <<< "$candidates"

if [[ ${#matches[@]} -eq 0 ]]; then
  echo "No browser processes found with --user-data-dir or --profile-directory."; exit 0
fi

# Print matches
echo "Found ${#matches[@]} matching browser process(es):"
for item in "${matches[@]}"; do
  pid="${item%%:::*}"
  cmd="${item#*:::}"
  printf "  PID: %s\n    CMD: %s\n" "$pid" "$cmd"
done

if [[ "$DRY_RUN" == true ]]; then
  echo "Dry-run mode; no processes will be killed."; exit 0
fi

# Function to check if a PID exists
pid_exists() {
  kill -0 "$1" >/dev/null 2>&1
}

# Kill sequence: SIGTERM, wait up to TIMEOUT seconds, then SIGKILL
for item in "${matches[@]}"; do
  pid="${item%%:::*}"
  cmd="${item#*:::}"

  if ! pid_exists "$pid"; then
    echo "PID $pid is no longer running; skipping."; continue
  fi

  echo "Stopping PID $pid..."
  if kill -TERM "$pid" >/dev/null 2>&1; then
    # wait for process to exit
    elapsed=0
    interval=0.2
    while pid_exists "$pid" && (( $(echo "$elapsed < $TIMEOUT" | bc -l) )); do
      sleep "$interval"
      elapsed=$(echo "$elapsed + $interval" | bc)
    done

    if pid_exists "$pid"; then
      echo "PID $pid did not exit after ${TIMEOUT}s; sending SIGKILL..."
      if kill -KILL "$pid" >/dev/null 2>&1; then
        echo "PID $pid killed (SIGKILL)."
      else
        echo "Failed to SIGKILL PID $pid (permission or gone)." >&2
      fi
    else
      echo "PID $pid terminated (SIGTERM)."
    fi
  else
    echo "Failed to SIGTERM PID $pid (permission or gone)." >&2
  fi
done

echo "Browser processes cleanup completed."
exit 0

