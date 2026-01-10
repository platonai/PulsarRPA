#!/bin/bash

# Build Browser4 as a Windows executable (.exe)
#
# This script builds the browser4-agents module and generates a Windows executable
# using the launch4j-maven-plugin. The resulting Browser4.exe can be run on Windows
# systems with a Java Runtime Environment (JRE) installed.
#
# Usage:
#   ./build-exe.sh [-clean] [-test]
#
# Options:
#   -clean    Perform a clean build before packaging
#   -test     Run tests during the build (by default tests are skipped)
#
# Examples:
#   ./build-exe.sh
#   ./build-exe.sh -clean
#   ./build-exe.sh -clean -test

set -e

# Find the first parent directory that contains a VERSION file
APP_HOME=$(cd "$(dirname "$0")" >/dev/null || exit 1; pwd)
while [[ ! -f "$APP_HOME/VERSION" && "$APP_HOME" != "/" ]]; do
    APP_HOME=$(dirname "$APP_HOME")
done

if [[ ! -f "$APP_HOME/VERSION" ]]; then
    echo "Error: Could not find project root (VERSION file not found)" >&2
    exit 1
fi

cd "$APP_HOME"

# Maven command
MVN_CMD="./mvnw"
if [[ ! -x "$MVN_CMD" ]]; then
    echo "Error: Maven wrapper not found or not executable at $MVN_CMD" >&2
    exit 1
fi

# Parse arguments
PERFORM_CLEAN=false
SKIP_TESTS=true

for arg in "$@"; do
    case $arg in
        -clean)
            PERFORM_CLEAN=true
            ;;
        -t|-test)
            SKIP_TESTS=false
            ;;
        -h|-help|--help)
            echo "Usage: build-exe.sh [-clean] [-test]"
            echo ""
            echo "Options:"
            echo "  -clean    Perform a clean build before packaging"
            echo "  -test     Run tests during the build (by default tests are skipped)"
            exit 0
            ;;
    esac
done

# Build Maven options
MVN_OPTIONS=()

if $PERFORM_CLEAN; then
    MVN_OPTIONS+=("clean")
fi

MVN_OPTIONS+=("package")
MVN_OPTIONS+=("-pl" "browser4/browser4-agents")
MVN_OPTIONS+=("-am")
MVN_OPTIONS+=("-Pwin-exe")

if $SKIP_TESTS; then
    MVN_OPTIONS+=("-DskipTests")
fi

echo "Building Browser4 Windows executable..."
echo "Command: $MVN_CMD ${MVN_OPTIONS[*]}"
echo ""

# Execute Maven build
$MVN_CMD "${MVN_OPTIONS[@]}"

# Check if the executable was created
EXE_PATH="$APP_HOME/browser4/browser4-agents/target/Browser4.exe"
if [[ -f "$EXE_PATH" ]]; then
    echo ""
    echo "========================================"
    echo "Build successful!"
    echo "Windows executable created at:"
    echo "  $EXE_PATH"
    echo "========================================"
    echo ""
    echo "To run Browser4 on Windows:"
    echo "  browser4\\browser4-agents\\target\\Browser4.exe"
    echo ""
    echo "Note: Java Runtime Environment (JRE) 17+ is required to run the executable."
else
    echo "Warning: Build completed but Browser4.exe was not found at expected location: $EXE_PATH" >&2
fi
