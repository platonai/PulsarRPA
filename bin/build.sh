#!/bin/bash

# Usage: ./build.sh [-clean] [-test]

# Function to print usage and exit
print_usage() {
    echo "Usage: $0 [-clean|-test]"
    exit 1
}

# Find the first parent directory containing the VERSION file
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
APP_HOME="$SCRIPT_DIR"

while [[ -n "$APP_HOME" && ! -f "$APP_HOME/VERSION" ]]; do
    APP_HOME=$(dirname "$APP_HOME")
done

cd "$APP_HOME" || { echo "Failed to change to directory: $APP_HOME"; exit 1; }

# Maven command and options
MVN_CMD="$APP_HOME/mvnw"

# Initialize flags and additional arguments
PERFORM_CLEAN=false
SKIP_TESTS=true

MVN_OPTIONS=()
ADDITIONAL_MVN_ARGS=()

# Parse command-line arguments
for arg in "$@"; do
    case "$arg" in
        -clean)
            PERFORM_CLEAN=true
            ;;
        -t|-test)
            SKIP_TESTS=false
            ;;
        -h|--help|"-help")
            print_usage
            ;;
        -*)
            ADDITIONAL_MVN_ARGS+=("$arg")
            print_usage
            ;;
        *)
            ADDITIONAL_MVN_ARGS+=("$arg")
            ;;
    esac
done

# Conditionally add Maven options based on flags
if [ "$PERFORM_CLEAN" = true ]; then
    MVN_OPTIONS+=("clean")
fi

if [ "$SKIP_TESTS" = true ]; then
    ADDITIONAL_MVN_ARGS+=("-DskipTests")
fi

# Add common options
MVN_OPTIONS+=("install")
ADDITIONAL_MVN_ARGS+=("-Pall-modules")

# Combine all options
MVN_OPTIONS+=("${ADDITIONAL_MVN_ARGS[@]}")

# Function to execute Maven command in a given directory
invoke_maven_build() {
    local directory="$1"
    shift
    local options=("$@")

    cd "$directory" || { echo "Failed to enter directory: $directory"; return 1; }

    # Execute Maven wrapper with options
    "$MVN_CMD" "${options[@]}"
    local exit_code=$?

    if [ $exit_code -ne 0 ]; then
        echo "Maven command failed in $directory" >&2
    fi

    cd - > /dev/null
    return $exit_code
}

# Run the Maven build
invoke_maven_build "$APP_HOME" "${MVN_OPTIONS[@]}"

exit $?
