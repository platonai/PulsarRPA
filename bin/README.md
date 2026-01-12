# Scripts

This directory contains scripts for building, running, and testing Browser4.

## Root Scripts

### build.ps1, build.sh

Build the project.

Options:
- `-clean`: Clean before building
- `-test`: Run tests (tests are skipped by default)

### build-run.ps1, build-run.sh

Build and run the project.

### browser4.ps1, browser4.sh

Start the Browser4 server.

### command-sse.ps1, command-sse.sh

SSE client to connect to the server.

### run-examples.ps1

Run Browser4 examples.

### version.ps1, version.sh

Print the version of Browser4.

### seeds.txt

Seed URLs for testing Browser4.

## Subdirectories

### ci/

CI/CD related scripts for local and Docker builds.

### legacy/

Legacy scraping scripts:
- `scrape.ps1`, `scrape.sh`, `scrape.bat` - Scrape a webpage using Browser4
- `scrape-async.ps1`, `scrape-async.sh` - Scrape using async API

### python/

Python client scripts and examples.

### quality/

Code quality check scripts.

### release/

Release management scripts (versioning, deployment, documentation).

### script-tests/

Tests for validating scripts.

### tests/

End-to-end and integration test scripts:
- `run-e2e-test.ps1`, `run-e2e-tests.sh` - Run end-to-end tests
- `run-test-cases.ps1`, `run-test-cases.sh` - Test using curl commands

### tools/

Utility scripts for development (code counting, cleanup, etc.).
