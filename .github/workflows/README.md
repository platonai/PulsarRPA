# GitHub Actions Workflows

This directory contains GitHub Actions workflows for continuous integration and deployment of the PulsarRPA project.

## Workflows

### 1. Pull Request CI (`pr-ci.yml`)

Triggered on pull requests to the `main` and `master` branches.

Features:
- Builds the project
- Runs tests
- Builds a Docker image
- Performs integration tests

### 2. CI & Release (`ci.yml`)

Triggered on pushes to the `main` branch.

Features:
- Builds the project
- Runs tests
- Builds and tags Docker images
- Deploys to Sonatype OSSRH
- Pushes Docker images to Docker Hub

### 3. Release (`release.yml`)

Triggered on tag pushes with the format `v*` (e.g., `3.0.6`).

Features:
- Extracts version from the tag
- Builds the project
- Runs tests
- Builds and tags Docker images
- Creates a GitHub Release
- Deploys to Sonatype OSSRH
- Pushes Docker images to Docker Hub

### 4. Nightly Build (`nightly.yml`)

Scheduled to run daily at midnight UTC and can be triggered manually.

Features:
- Builds the project with all tests
- Builds a Docker image
- Runs integration tests
- Sends Slack notifications with build status

### 5. Code Quality (`code-quality.yml`)

Triggered on pull requests to the `main` and `master` branches and pushes to the `main` branch.

Features:
- CodeQL analysis for Java and Kotlin
- Checkstyle validation

## Required Secrets

The workflows use the following secrets:

- `SONATYPE_USERNAME` - Username for Sonatype OSSRH
- `SONATYPE_PASSWORD` - Password for Sonatype OSSRH
- `DOCKER_USERNAME` - Username for Docker Hub
- `DOCKER_PASSWORD` - Password for Docker Hub
- `SLACK_WEBHOOK` - Webhook URL for Slack notifications (used in nightly builds)

## Manual Workflow Execution

All workflows except the release workflow can be manually triggered from the GitHub Actions tab in the repository. 
