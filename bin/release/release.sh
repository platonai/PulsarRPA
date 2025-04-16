#!/bin/bash
# 🚀 PulsarRPA Release Script
# This script orchestrates the release process by executing git and docker release scripts
# Usage: ./release.sh [-v|--verbose]

# Enable error handling
set -euo pipefail
IFS=$'\n\t'

# Parse command line arguments
VERBOSE=false
while [[ $# -gt 0 ]]; do
    case $1 in
        -v|--verbose)
            VERBOSE=true
            shift
            ;;
        *)
            echo "❌ Unknown option: $1"
            exit 1
            ;;
    esac
done

# 🔍 Find the first parent directory containing the VERSION file
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
APP_HOME="$SCRIPT_DIR/../.."

while [[ ! -f "$APP_HOME/VERSION" ]]; do
    if [[ "$APP_HOME" == "/" ]]; then
        echo "❌ VERSION file not found in any parent directory"
        exit 1
    fi
    APP_HOME="$(dirname "$APP_HOME")"
done

if [[ "$VERBOSE" == true ]]; then
    echo "📂 Found project root at: $APP_HOME"
fi

cd "$APP_HOME"

# 📦 Define script paths
BIN="$APP_HOME/bin"
GIT_RELEASE_SCRIPT="$BIN/release/git-release.sh"
DOCKER_RELEASE_SCRIPT="$BIN/release/docker-release.sh"

# 🔍 Verify required scripts exist
if [[ ! -f "$GIT_RELEASE_SCRIPT" ]]; then
    echo "❌ Git release script not found: $GIT_RELEASE_SCRIPT"
    exit 1
fi

if [[ ! -f "$DOCKER_RELEASE_SCRIPT" ]]; then
    echo "❌ Docker release script not found: $DOCKER_RELEASE_SCRIPT"
    exit 1
fi

# 🚀 Execute release scripts
if [[ "$VERBOSE" == true ]]; then
    echo "📦 Starting git release process..."
fi

if ! "$GIT_RELEASE_SCRIPT"; then
    echo "❌ Git release failed"
    exit 1
fi

if [[ "$VERBOSE" == true ]]; then
    echo "🐳 Starting docker release process..."
fi

if ! "$DOCKER_RELEASE_SCRIPT"; then
    echo "❌ Docker release failed"
    exit 1
fi

echo "✅ Release process completed successfully!"
#!/bin/bash
# 🚀 PulsarRPA Release Script
# This script orchestrates the release process by executing git and docker release scripts
# Usage: ./release.sh [-v|--verbose]

# Enable error handling
set -euo pipefail
IFS=$'\n\t'

# Parse command line arguments
VERBOSE=false
while [[ $# -gt 0 ]]; do
    case $1 in
        -v|--verbose)
            VERBOSE=true
            shift
            ;;
        *)
            echo "❌ Unknown option: $1"
            exit 1
            ;;
    esac
done

# 🔍 Find the first parent directory containing the VERSION file
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
APP_HOME="$SCRIPT_DIR/../.."

while [[ ! -f "$APP_HOME/VERSION" ]]; do
    if [[ "$APP_HOME" == "/" ]]; then
        echo "❌ VERSION file not found in any parent directory"
        exit 1
    fi
    APP_HOME="$(dirname "$APP_HOME")"
done

if [[ "$VERBOSE" == true ]]; then
    echo "📂 Found project root at: $APP_HOME"
fi

cd "$APP_HOME"

# 📦 Define script paths
BIN="$APP_HOME/bin"
GIT_RELEASE_SCRIPT="$BIN/release/git-release.sh"
DOCKER_RELEASE_SCRIPT="$BIN/release/docker-release.sh"

# 🔍 Verify required scripts exist
if [[ ! -f "$GIT_RELEASE_SCRIPT" ]]; then
    echo "❌ Git release script not found: $GIT_RELEASE_SCRIPT"
    exit 1
fi

if [[ ! -f "$DOCKER_RELEASE_SCRIPT" ]]; then
    echo "❌ Docker release script not found: $DOCKER_RELEASE_SCRIPT"
    exit 1
fi

# 🚀 Execute release scripts
if [[ "$VERBOSE" == true ]]; then
    echo "📦 Starting git release process..."
fi

if ! "$GIT_RELEASE_SCRIPT"; then
    echo "❌ Git release failed"
    exit 1
fi

if [[ "$VERBOSE" == true ]]; then
    echo "🐳 Starting docker release process..."
fi

if ! "$DOCKER_RELEASE_SCRIPT"; then
    echo "❌ Docker release failed"
    exit 1
fi

echo "✅ Release process completed successfully!"