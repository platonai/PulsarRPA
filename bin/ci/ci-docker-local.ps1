#!/usr/bin/env pwsh

# Find the first parent directory containing the VERSION file
$AppHome = Split-Path -Parent (Resolve-Path $MyInvocation.MyCommand.Path)
while ($AppHome -ne [System.IO.Path]::GetPathRoot($AppHome) -and -not (Test-Path (Join-Path $AppHome "VERSION"))) {
    $AppHome = Split-Path -Parent $AppHome
}
Set-Location $AppHome -ErrorAction Stop

# Configuration parameters
$DOCKERFILE = if ($env:DOCKERFILE) { $env:DOCKERFILE } else { "docker/pulsar-rpa-prod/Dockerfile" }
$IMAGE_NAME = if ($env:IMAGE_NAME) { $env:IMAGE_NAME } else { "pulsar-rpa-test" }
$CONTAINER_NAME = if ($env:CONTAINER_NAME) { $env:CONTAINER_NAME } else { "pulsar-rpa-test" }
$PORT = if ($env:PORT) { $env:PORT } else { "8182" }
$HEALTH_CHECK_TIMEOUT = if ($env:HEALTH_CHECK_TIMEOUT) { [int]$env:HEALTH_CHECK_TIMEOUT } else { 60 }

# Error handling
$ErrorActionPreference = "Stop"

# Cleanup function to ensure container is stopped
function Cleanup {
    Write-Host "Cleaning up..." -ForegroundColor Yellow
    try {
        docker stop $CONTAINER_NAME 2>$null
        docker rm $CONTAINER_NAME 2>$null
    }
    catch {
        # Ignore cleanup errors
    }
}

# Register cleanup on script exit
trap { Cleanup; break }

# Check if port is already in use
try {
    $portInUse = Get-NetTCPConnection -LocalPort $PORT -State Listen -ErrorAction SilentlyContinue
    if ($portInUse) {
        Write-Error "Error: Port $PORT is already in use"
        exit 1
    }
}
catch {
    # If Get-NetTCPConnection is not available, skip port check
    Write-Warning "Could not check if port is in use. Continuing..."
}

# Build the Docker image
Write-Host "Building Docker image: $IMAGE_NAME" -ForegroundColor Green
try {
    docker build -t $IMAGE_NAME -f $DOCKERFILE .
    if ($LASTEXITCODE -ne 0) {
        throw "Docker build failed"
    }
}
catch {
    Write-Error "Error: Docker build failed"
    exit 1
}

# Run the Docker container
Write-Host "Starting container: $CONTAINER_NAME" -ForegroundColor Green
try {
    $CONTAINER_ID = docker run --rm -d --name $CONTAINER_NAME -p "${PORT}:${PORT}" $IMAGE_NAME
    if ($LASTEXITCODE -ne 0) {
        throw "Container start failed"
    }
    Write-Host "Container started with ID: $CONTAINER_ID" -ForegroundColor Cyan
}
catch {
    Write-Error "Error: Failed to start container"
    exit 1
}

# Enhanced health check function
function Test-HealthEndpoint {
    param(
        [string]$Url
    )

    try {
        $response = Invoke-RestMethod -Uri $Url -Method Get -TimeoutSec 5 -ErrorAction Stop
        if ($response.status -eq "UP") {
            return $true
        }
    }
    catch {
        # Health check failed
    }
    return $false
}

# Wait for the container to start with timeout
Write-Host "Waiting for the Pulsar RPA service to start..." -ForegroundColor Yellow
$retryCount = 0
$maxRetries = [math]::Floor($HEALTH_CHECK_TIMEOUT / 2)

# Wait until the actuator health endpoint is ready
do {
    $retryCount++
    Write-Host "Waiting for Pulsar RPA service to be ready (actuator)... ($retryCount/$maxRetries)" -ForegroundColor Yellow

    if (Test-HealthEndpoint "http://localhost:$PORT/actuator/health") {
        break
    }

    if ($retryCount -ge $maxRetries) {
        Write-Error "Error: Actuator health check failed to start within $HEALTH_CHECK_TIMEOUT seconds"
        Write-Host "Container logs:" -ForegroundColor Red
        docker logs $CONTAINER_NAME
        exit 1
    }

    Start-Sleep -Seconds 2
} while ($true)

# Reset retry count for second health check
$retryCount = 0

# Wait until the custom health endpoint is ready
do {
    $retryCount++
    Write-Host "Waiting for Pulsar RPA service to be ready (system)... ($retryCount/$maxRetries)" -ForegroundColor Yellow

    if (Test-HealthEndpoint "http://localhost:$PORT/api/system/health") {
        break
    }

    if ($retryCount -ge $maxRetries) {
        Write-Error "Error: System health check failed to start within $HEALTH_CHECK_TIMEOUT seconds"
        Write-Host "Container logs:" -ForegroundColor Red
        docker logs $CONTAINER_NAME
        exit 1
    }

    Start-Sleep -Seconds 2
} while ($true)

Write-Host "Pulsar RPA service is ready." -ForegroundColor Green

# Run the integration tests
Write-Host "Running integration tests..." -ForegroundColor Green
try {
    if (Test-Path "bin/run-integration-test.ps1") {
        & "bin/run-integration-test.ps1"
    }
    elseif (Test-Path "bin/run-integration-test.sh") {
        bash "bin/run-integration-test.sh"
    }
    else {
        throw "Integration test script not found"
    }

    if ($LASTEXITCODE -ne 0) {
        throw "Integration tests failed"
    }
}
catch {
    Write-Error "Error: Integration tests failed"
    Write-Host "Container logs:" -ForegroundColor Red
    docker logs $CONTAINER_NAME
    exit 1
}

Write-Host "Integration tests completed successfully!" -ForegroundColor Green

# Cleanup will be called automatically by the trap