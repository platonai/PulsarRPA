#!/bin/bash

echo "Building Browser4 with host Maven repository volume mount..."

# Build with volume mount - mounts host's ~/.m2 directory
docker build \
  --progress=plain \
  -v ~/.m2:/root/.m2 \
  -t pulsar-rpa:volume \
  -f docker/pulsar-rpa-prod/Dockerfile.with-volume \
  .

echo "Build completed! Used host's Maven repository at ~/.m2" 