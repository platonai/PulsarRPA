#!/bin/bash

# Define a parameter for intervalSeconds with a default value of 60
interval_seconds=${1:-60}

# Find the first parent directory containing the VERSION file
app_home=$(dirname "$(realpath "$0")")
while [[ -n "$app_home" && ! -f "$app_home/VERSION" ]]; do
    app_home=$(dirname "$app_home")
done

cd "$app_home" || exit

# Configuration
repo_path="$app_home"
build_script="$app_home/bin/build.ps1"

# Function to get the current HEAD hash
get_head_hash() {
    git rev-parse HEAD
}

# Function to run the build script
run_build_script() {
    if [[ -f "$build_script" ]]; then
        echo "[INFO] Running $build_script..."

        # Test the pulsar-tests module first
        echo "[INFO] Testing pulsar-tests module..."
        "$build_script" -clean
        "$build_script" -test -pl :pulsar-tests
        test_result=$?

        # Check if the pulsar-tests module had any failed tests
        if [[ $test_result -eq 0 ]]; then
            echo "[INFO] pulsar-tests module passed. Testing all modules..."
            "$build_script" -clean -test
        else
            echo "[ERROR] pulsar-tests module failed. Skipping testing of all other modules."
        fi
    else
        echo "[ERROR] $build_script not found in $repo_path"
    fi
}

# First run: Always pull and build
echo "[INFO] First run: Pulling latest changes and running build script..."
git pull
run_build_script

# Get the current HEAD hash after first run
last_hash=$(get_head_hash)

# Main loop
while true; do
    echo "[INFO] Checking for updates at $(date)"

    # Perform git pull
    git pull

    # Get the new HEAD hash
    new_hash=$(get_head_hash)

    # Compare hashes
    if [[ "$new_hash" != "$last_hash" ]]; then
        echo "[INFO] New updates detected (Old: $last_hash, New: $new_hash)"

        # Run build script if updates are detected
        run_build_script

        # Update the last hash
        last_hash="$new_hash"
    else
        echo "[INFO] No updates detected."
    fi

    # Wait for the next check
    sleep "$interval_seconds"
done