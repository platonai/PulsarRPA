#!/bin/bash

# Define a parameter for intervalSeconds with a default value of 60
intervalSeconds=${1:-60}

# Find the first parent directory containing the VERSION file
AppHome=$(dirname "$(realpath "$0")")
while [[ -n "$AppHome" && ! -f "$AppHome/VERSION" ]]; do
    AppHome=$(dirname "$AppHome")
done

if [[ -z "$AppHome" ]]; then
    echo "[ERROR] Could not find VERSION file in any parent directory"
    exit 1
fi

cd "$AppHome" || exit

# Configuration
repoPath="$AppHome"                                  # 你的 Git 仓库路径
buildScript="$AppHome/bin/build.sh"                 # 你的构建脚本

# Enter the repository directory
cd "$repoPath" || exit

# Function to get the current HEAD hash
function get_head_hash {
    git rev-parse HEAD
}

# Function to run the build script
function run_build_script {
    if [[ -f "$buildScript" ]]; then
        echo "[INFO] Running $buildScript..."
        "$buildScript" -clean -test -pl :pulsar-tests
    else
        echo "[ERROR] $buildScript not found in $repoPath"
    fi
}

# First run: Always pull and build
echo "[INFO] First run: Pulling latest changes and running build script..."
git pull
run_build_script

# Get the current HEAD hash after first run
lastHash=$(get_head_hash)

# Main loop
while true; do
    echo "[INFO] Checking for updates at $(date)"

    # Perform git pull
    git pull

    # Get the new HEAD hash
    newHash=$(get_head_hash)

    # Compare hashes
    if [[ "$newHash" != "$lastHash" ]]; then
        echo "[INFO] New updates detected (Old: $lastHash, New: $newHash)"

        # Run build script if updates are detected
        run_build_script

        # Update the last hash
        lastHash="$newHash"
    else
        echo "[INFO] No updates detected."
    fi

    # Wait for the next check
    sleep "$intervalSeconds"
done
