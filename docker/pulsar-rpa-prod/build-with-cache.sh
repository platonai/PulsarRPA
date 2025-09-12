#!/bin/bash

echo "Building Browser4 with Maven cache reuse..."

# Enable BuildKit for cache mount support
export DOCKER_BUILDKIT=1

# Build with cache mount - this will persist Maven dependencies between builds
docker build \
  --progress=plain \
  -t browser4:latest \
  -f docker/browser4-prod/Dockerfile \
  .

echo "Build completed! Maven dependencies are cached for future builds." 