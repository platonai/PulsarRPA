#!/bin/bash

# Default parameters
REMOTE_USER="vincent"
REMOTE_HOST="platonai.cn"
REMOTE_PATH="~/platonai.cn/repo/ai/platon/pulsar/"

# Parse command line arguments
while [[ $# -gt 0 ]]; do
  case $1 in
    --remote-user=*)
      REMOTE_USER="${1#*=}"
      shift
      ;;
    --remote-host=*)
      REMOTE_HOST="${1#*=}"
      shift
      ;;
    --remote-path=*)
      REMOTE_PATH="${1#*=}"
      shift
      ;;
    *)
      echo "Unknown parameter: $1"
      exit 1
      ;;
  esac
done

# Find the first parent directory containing the VERSION file
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
APP_HOME="$SCRIPT_DIR"
while [[ ! -f "$APP_HOME/VERSION" && "$APP_HOME" != "/" ]]; do
  APP_HOME="$(dirname "$APP_HOME")"
done

if [[ ! -f "$APP_HOME/VERSION" ]]; then
  echo "Error: Could not find VERSION file in any parent directory" >&2
  exit 1
fi

cd "$APP_HOME" || exit 1

# If pulsar-app/pulsar-master/target/PulsarRPA.jar exists, copy it to remote
PULSAR_RPA_PATH="$APP_HOME/pulsar-app/pulsar-master/target/PulsarRPA.jar"
if [[ -f "$PULSAR_RPA_PATH" ]]; then
  DESTINATION_PATH="${REMOTE_USER}@${REMOTE_HOST}:${REMOTE_PATH}"

  echo "Copying $PULSAR_RPA_PATH to $DESTINATION_PATH..."

  # Check if SSH connection works
  if ! ssh "${REMOTE_USER}@${REMOTE_HOST}" 'echo Connection test' > /dev/null; then
    echo "Error: SSH connection test failed" >&2
    exit 1
  fi

  # Ensure destination directory exists
  if ! ssh "${REMOTE_USER}@${REMOTE_HOST}" "mkdir -p ${REMOTE_PATH}"; then
    echo "Error: Failed to create remote directory" >&2
    exit 1
  fi

  # Copy the file
  if ! scp "$PULSAR_RPA_PATH" "$DESTINATION_PATH"; then
    echo "Error: SCP command failed" >&2
    exit 1
  fi

  echo -e "\e[32mFile copied successfully to $DESTINATION_PATH\e[0m"
else
  echo -e "\e[33mWarning: $PULSAR_RPA_PATH does not exist\e[0m" >&2
  exit 1
fi