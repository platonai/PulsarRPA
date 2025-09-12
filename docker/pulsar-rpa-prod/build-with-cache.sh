#!/bin/bash

echo "Building Browser4 with Maven cache reuse..."

# Enable BuildKit for cache mount support
export DOCKER_BUILDKIT=1

# Build with cache mount - this will persist Maven dependencies between builds
docker build \
  --progress=plain \
  -t pulsar-rpa:latest \
  -f docker/pulsar-rpa-prod/Dockerfile \
  .

echo "Build completed! Maven dependencies are cached for future builds." 