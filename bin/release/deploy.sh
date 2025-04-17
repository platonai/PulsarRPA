#!/bin/bash
# 🚀 PulsarRPA Deployment Script
# This script handles the deployment process including local testing and production deployment
# Usage: ./deploy.sh [-v|--verbose] [-t|--test] [-p|--production]

# Enable error handling
set -euo pipefail
IFS=$'\n\t'

# Default values
VERBOSE=false
TEST_MODE=false
PRODUCTION_MODE=false
DOCKER_CONTAINER_NAME="pulsar-rpa-test"
DOCKER_IMAGE_NAME="pulsar-rpa"
DOCKER_TAG="latest"
SERVICE_PORT=8182
MAX_WAIT_TIME=60  # seconds
WAIT_INTERVAL=5   # seconds

# Parse command line arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        -v|--verbose)
            VERBOSE=true
            shift
            ;;
        -t|--test)
            TEST_MODE=true
            shift
            ;;
        -p|--production)
            PRODUCTION_MODE=true
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
APP_HOME="$SCRIPT_DIR/.."

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

$APP_HOME/bin/tools/dos2unix.sh

# Function to log messages
log() {
    if [[ "$VERBOSE" == true ]]; then
        echo "$1"
    fi
}

# Function to wait for service to be ready
wait_for_service() {
    local url="http://localhost:$SERVICE_PORT/actuator/health"
    local start_time=$(date +%s)
    local end_time=$((start_time + MAX_WAIT_TIME))
    
    log "⏳ Waiting for service to be ready..."
    
    while [[ $(date +%s) -lt $end_time ]]; do
        if curl -s "$url" | grep -q "UP"; then
            log "✅ Service is ready!"
            return 0
        fi
        sleep $WAIT_INTERVAL
    done
    
    echo "❌ Service failed to start within $MAX_WAIT_TIME seconds"
    return 1
}

# Function to run integration tests
run_integration_tests() {
    log "🔍 Running integration tests..."
    
    # Extract curl commands using Python script
    local curl_examples
    if ! curl_examples=$(python3 "$SCRIPT_DIR/extract_curl_commands.py" "$APP_HOME/README.md"); then
        echo "❌ Failed to extract curl commands from README.md"
        return 1
    fi
    
    if [[ -z "$curl_examples" ]]; then
        echo "❌ No curl examples found in README.md"
        return 1
    fi
    
    # Execute each curl example
    while IFS= read -r line; do
        if [[ "$line" =~ ^# ]]; then
            # Skip comment lines
            continue
        fi
        if [[ "$line" =~ curl ]]; then
            log "🔍 Testing: $line"
            if ! eval "$line"; then
                echo "❌ Test failed: $line"
                return 1
            fi
        fi
    done <<< "$curl_examples"
    
    log "✅ All integration tests passed!"
    return 0
}

# 1. Deploy to local staging repository
log "📦 Deploying to local staging repository..."
if ! $APP_HOME/bin/release/oss-deploy.sh; then
    echo "❌ Failed to deploy to local staging repository"
    exit 1
fi

# 2. Build docker image
log "🐳 Building docker image..."
if ! docker build -t "$DOCKER_IMAGE_NAME:$DOCKER_TAG" .; then
    echo "❌ Failed to build docker image"
    exit 1
fi

if [[ "$TEST_MODE" == true ]]; then
    # 3. Run docker container for testing
    log "🚀 Starting docker container for testing..."
    if ! docker run -d -p "$SERVICE_PORT:$SERVICE_PORT" --name "$DOCKER_CONTAINER_NAME" "$DOCKER_IMAGE_NAME:$DOCKER_TAG"; then
        echo "❌ Failed to start docker container"
        exit 1
    fi
    
    # Wait for service to be ready
    if ! wait_for_service; then
        docker logs "$DOCKER_CONTAINER_NAME"
        docker rm -f "$DOCKER_CONTAINER_NAME"
        exit 1
    fi
    
    # Run integration tests
    if ! run_integration_tests; then
        docker logs "$DOCKER_CONTAINER_NAME"
        docker rm -f "$DOCKER_CONTAINER_NAME"
        exit 1
    fi
    
    # Cleanup
    log "🧹 Cleaning up test container..."
    docker rm -f "$DOCKER_CONTAINER_NAME"
fi

if [[ "$PRODUCTION_MODE" == true ]]; then
    # 4.1 Deploy artifact to Sonatype
    log "📦 Deploying artifact to Sonatype..."
    if ! mvn clean deploy -P release; then
        echo "❌ Failed to deploy to Sonatype"
        exit 1
    fi
    
    # 4.2 Push docker image
    log "🐳 Pushing docker image..."
    if ! docker push "$DOCKER_IMAGE_NAME:$DOCKER_TAG"; then
        echo "❌ Failed to push docker image"
        exit 1
    fi
fi

echo "✅ Deployment process completed successfully!" 