#!/usr/bin/env pwsh

# Enhanced CI script for local Docker testing with improved error handling and user experience

param(
    [string]$Dockerfile = $env:DOCKERFILE,
    [string]$ImageName = $env:IMAGE_NAME,
    [string]$ContainerName = $env:CONTAINER_NAME,
    [int]$Port = $env:PORT,
    [int]$HealthCheckTimeout = $env:HEALTH_CHECK_TIMEOUT,
    [switch]$SkipTests = $false,
    [switch]$Verbose = $true,
    [switch]$UseCache = $true
)

# Find the first parent directory containing the VERSION file
$AppHome = Split-Path -Parent (Resolve-Path $MyInvocation.MyCommand.Path)
while ($AppHome -ne [System.IO.Path]::GetPathRoot($AppHome) -and -not (Test-Path (Join-Path $AppHome "VERSION"))) {
    $AppHome = Split-Path -Parent $AppHome
}
Set-Location $AppHome -ErrorAction Stop

# Set PowerShell encoding for UTF-8 support
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8

# Default configuration with validation
$DOCKERFILE = if ($Dockerfile) { $Dockerfile } else { "docker/pulsar-rpa-prod/Dockerfile" }
$IMAGE_NAME = if ($ImageName) { $ImageName } else { "pulsar-rpa-test" }
$CONTAINER_NAME = if ($ContainerName) { $ContainerName } else { "pulsar-rpa-test" }
$PORT = if ($Port -gt 0) { $Port } else { 8182 }
$HEALTH_CHECK_TIMEOUT = if ($HealthCheckTimeout -gt 0) { $HealthCheckTimeout } else { 60 }

# Error handling
$ErrorActionPreference = "Stop"

# Validate configuration
function Test-Configuration {
    Write-Host "🔍 Validating configuration..." -ForegroundColor Yellow

    if (-not (Test-Path $DOCKERFILE)) {
        throw "Dockerfile not found: $DOCKERFILE"
    }

    Write-Host "✅ Configuration validation passed" -ForegroundColor Green
    Write-Host "   📄 Dockerfile: $DOCKERFILE" -ForegroundColor Cyan
    Write-Host "   🏷️  Image name: $IMAGE_NAME" -ForegroundColor Cyan
    Write-Host "   📦 Container name: $CONTAINER_NAME" -ForegroundColor Cyan
    Write-Host "   🔌 Port: $PORT" -ForegroundColor Cyan
    Write-Host "   ⏱️  Health check timeout: ${HEALTH_CHECK_TIMEOUT} seconds" -ForegroundColor Cyan
}

# Enhanced cleanup function
function Invoke-Cleanup {
    param([bool]$Silent = $false)

    if (-not $Silent) {
        Write-Host "🧹 Cleaning up resources..." -ForegroundColor Yellow
    }

    try {
        # Stop and remove container
        $existingContainer = docker ps -aq --filter "name=$CONTAINER_NAME" 2>$null
        if ($existingContainer) {
            if (-not $Silent) {
                Write-Host "   Stopping container: $CONTAINER_NAME" -ForegroundColor Gray
            }
            docker stop $CONTAINER_NAME 2>$null | Out-Null
            docker rm $CONTAINER_NAME 2>$null | Out-Null
        }

        # Clean up any dangling images if needed
        if ($UseCache -eq $false) {
            docker image prune -f 2>$null | Out-Null
        }
    }
    catch {
        if (-not $Silent) {
            Write-Warning "Warning during cleanup: $($_.Exception.Message)"
        }
    }
}

# Register cleanup on script exit
trap {
    Write-Host "`n❌ Script interrupted" -ForegroundColor Red
    Invoke-Cleanup
    break
}

# Check if port is available
function Test-PortAvailability {
    param([int]$TestPort)

    try {
        $portInUse = Get-NetTCPConnection -LocalPort $TestPort -State Listen -ErrorAction SilentlyContinue
        if ($portInUse) {
            throw "Port $TestPort is already in use"
        }
        Write-Host "✅ Port $TestPort is available" -ForegroundColor Green
    }
    catch [System.Management.Automation.CommandNotFoundException] {
        Write-Warning "Cannot check port availability, continuing..."
    }
    catch {
        throw $_
    }
}

# Simplified health check function - only checks for {"status":"UP"}
function Test-HealthEndpoint {
    param(
        [string]$Url,
        [int]$TimeoutSeconds = 5
    )

    try {
        if ($Verbose) {
            Write-Host "   Checking: $Url" -ForegroundColor Gray
        }

        # Curl Works But PowerShell Doesn't
        # $response = Invoke-RestMethod -Uri $Url -Method Get -TimeoutSec $TimeoutSeconds -ErrorAction Stop
        $response = curl $Url

        if ($Verbose) {
            Write-Host "   Response: $($response | ConvertTo-Json -Compress)" -ForegroundColor Cyan
        }

        # Check for the expected {"status":"UP"} response
        if ($response -match "UP") {
            if ($Verbose) {
                Write-Host "   ✅ Health check passed: response contains 'UP'" -ForegroundColor Green
            }
            return $true
        } else {
            if ($Verbose) {
                Write-Host "   ❌ Health check failed: response does not contain 'UP'" -ForegroundColor Red
            }
            return $false
        }
    }
    catch [System.Net.WebException] {
        if ($Verbose) {
            Write-Host "   Network error: $($_.Exception.Message)" -ForegroundColor Gray
        }
    }
    catch [System.TimeoutException] {
        if ($Verbose) {
            Write-Host "   Request timeout" -ForegroundColor Gray
        }
    }
    catch {
        if ($Verbose) {
            Write-Host "   Health check failed: $($_.Exception.Message)" -ForegroundColor Gray
        }
    }
    return $false
}

# Test container port binding
function Test-ContainerPortBinding {
    param([string]$ContainerName, [int]$ExpectedPort)

    try {
        $portInfo = docker port $ContainerName 2>$null
        if ($portInfo -match "$ExpectedPort") {
            Write-Host "✅ Port binding normal: $portInfo" -ForegroundColor Green
            return $true
        }

        Write-Warning "⚠️  Port binding abnormal or not found"
        return $false
    }
    catch {
        Write-Warning "Cannot check container port binding: $($_.Exception.Message)"
        return $false
    }
}

# Build Docker image with enhanced options
function Build-DockerImage {
    Write-Host "🏗️  Building Docker image: $IMAGE_NAME" -ForegroundColor Green

    try {
        # Clean up existing container first
        Invoke-Cleanup -Silent $true

        # Prepare build arguments
        $buildArgs = @(
            "build"
            "-t", $IMAGE_NAME
            "-f", $DOCKERFILE
        )

        if ($UseCache) {
            # Enable BuildKit for better caching
            $env:DOCKER_BUILDKIT = "1"
            Write-Host "   Enabled BuildKit caching" -ForegroundColor Cyan
        } else {
            $buildArgs += "--no-cache"
            Write-Host "   Disabled cache build" -ForegroundColor Yellow
        }

        if ($Verbose) {
            $buildArgs += "--progress=plain"
        }

        $buildArgs += "."

        Write-Host "   Executing: docker $($buildArgs -join ' ')" -ForegroundColor Gray

        $startTime = Get-Date
        & docker @buildArgs

        if ($LASTEXITCODE -ne 0) {
            throw "Docker build failed with exit code: $LASTEXITCODE"
        }

        $duration = (Get-Date) - $startTime
        Write-Host "✅ Image built successfully!" -ForegroundColor Green
        Write-Host "   ⏱️  Build time: $($duration.ToString('mm\:ss'))" -ForegroundColor Cyan
    }
    catch {
        throw "Build failed: $($_.Exception.Message)"
    }
}

# Start container with enhanced monitoring and health check
function Start-Container {
    Write-Host "🚀 Starting container: $CONTAINER_NAME" -ForegroundColor Green

    # Check if $env:DEEPSEEK_API_KEY is set
    if (-not $env:DEEPSEEK_API_KEY) {
        # Show how to set the environment variable
        Write-Host "⚠️  Environment variable DEEPSEEK_API_KEY is not set." -ForegroundColor Yellow
        Write-Host "   Please set it before running this script, e.g.:" -ForegroundColor Yellow
        Write-Host "   setx DEEPSEEK_API_KEY 'your_api_key_here'" -ForegroundColor Yellow
        throw "Environment variable DEEPSEEK_API_KEY is not set. Please set it before running this script."
    }

    try {
        $startArgs = @(
            "run", "--rm", "-d"
            "--name", $CONTAINER_NAME
            "-e DEEPSEEK_API_KEY=$env:DEEPSEEK_API_KEY"
            "-p", "${PORT}:8182"
        )

        # Add Docker health check for better monitoring
        $startArgs += "--health-cmd=curl -f http://localhost:8182/actuator/health || exit 1"
        $startArgs += "--health-interval=10s"
        $startArgs += "--health-timeout=5s"
        $startArgs += "--health-retries=3"
        $startArgs += "--health-start-period=30s"

        $startArgs += $IMAGE_NAME

        $CONTAINER_ID = & docker @startArgs

        if ($LASTEXITCODE -ne 0) {
            throw "Container startup failed with exit code: $LASTEXITCODE"
        }

        Write-Host "✅ Container started successfully!" -ForegroundColor Green
        Write-Host "   🆔 Container ID: $($CONTAINER_ID.Substring(0, 12))" -ForegroundColor Cyan
        Write-Host "   🌐 Access URL: http://localhost:$PORT" -ForegroundColor Cyan

        # Wait a moment for container to initialize
        Start-Sleep -Seconds 3

        # Verify port binding
        Test-ContainerPortBinding -ContainerName $CONTAINER_NAME -ExpectedPort $PORT

        return $CONTAINER_ID
    }
    catch {
        throw "Startup failed: $($_.Exception.Message)"
    }
}

# Simplified health monitoring - only check the two specific endpoints
function Wait-ForHealthy {
    Write-Host "🔍 Waiting for service to start..." -ForegroundColor Yellow

    $maxRetries = [math]::Floor($HEALTH_CHECK_TIMEOUT / 2)
    # Only check the two specific health endpoints you mentioned
    $healthEndpoints = @(
        "http://localhost:$PORT/api/system/health",
        "http://localhost:$PORT/actuator/health"
    )

    Write-Host "   Max retries: $maxRetries" -ForegroundColor Cyan
    Write-Host "   Check interval: 2 seconds" -ForegroundColor Cyan
    Write-Host "   Expected response: {`"status`":`"UP`"}" -ForegroundColor Cyan

    # Try endpoints with focused strategy
    for ($attempt = 1; $attempt -le $maxRetries; $attempt++) {
        Write-Host "   Attempt $attempt/$maxRetries..." -ForegroundColor Gray

        # Check container status first
        $containerRunning = docker ps -q --filter "name=$CONTAINER_NAME"
        if (-not $containerRunning) {
            Write-Host "❌ Container has stopped running!" -ForegroundColor Red
            Write-Host "📋 Container logs:" -ForegroundColor Yellow
            docker logs $CONTAINER_NAME --tail 50
            throw "Container stopped unexpectedly"
        }

        # Test each health endpoint
        foreach ($endpoint in $healthEndpoints) {
            if (Test-HealthEndpoint $endpoint -TimeoutSeconds 3 -Verbose:$Verbose) {
                Write-Host "✅ Service health check passed!" -ForegroundColor Green
                Write-Host "   🎯 Responding endpoint: $endpoint" -ForegroundColor Cyan

                # Double-check with Docker's health status if available
                $dockerHealth = docker inspect --format='{{.State.Health.Status}}' $CONTAINER_NAME 2>$null
                if ($dockerHealth -and $dockerHealth -ne "none") {
                    Write-Host "   🏥 Docker health status: $dockerHealth" -ForegroundColor Cyan
                }

                return
            }
        }

        # Show progress every few attempts
        if ($Verbose -or $attempt % 5 -eq 0) {
            Write-Host "   📊 Container status check..." -ForegroundColor Gray
            docker ps --filter "name=$CONTAINER_NAME" --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}" --no-trunc
        }

        if ($attempt -lt $maxRetries) {
            Start-Sleep -Seconds 2
        }
    }

    # All attempts failed - provide diagnostics
    Write-Host "❌ Health check failed - neither endpoint returned {`"status`":`"UP`"}" -ForegroundColor Red

    Write-Host "📋 Diagnostic information:" -ForegroundColor Yellow
    Write-Host "   🐳 Container status:" -ForegroundColor Yellow
    docker ps --filter "name=$CONTAINER_NAME" --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}\t{{.Image}}" --no-trunc

    $portBindings = docker port $CONTAINER_NAME 2>$null
    if ($portBindings) {
        Write-Host $portBindings
    } else {
        Write-Host "     No port binding info" -ForegroundColor Gray
    }

    Write-Host "   🏥 Docker health status:" -ForegroundColor Yellow
    $dockerHealth = docker inspect --format='{{.State.Health.Status}}' $CONTAINER_NAME 2>$null
    if ($dockerHealth) {
        Write-Host "     $dockerHealth" -ForegroundColor Gray
    } else {
        Write-Host "     No health check configured" -ForegroundColor Gray
    }

    Write-Host "   📋 Container logs (last 50 lines):" -ForegroundColor Yellow
    docker logs $CONTAINER_NAME --tail 50

    Write-Host "   🌐 Checked endpoints:" -ForegroundColor Yellow
    foreach ($endpoint in $healthEndpoints) {
        Write-Host "     - $endpoint (expected: {`"status`":`"UP`"})" -ForegroundColor Gray
    }

    throw "Health check failed, service did not return {`"status`":`"UP`"} within $HEALTH_CHECK_TIMEOUT seconds"
}

# Run integration tests
function Invoke-IntegrationTests {
    if ($SkipTests) {
        Write-Host "⏭️  Skipping integration tests" -ForegroundColor Yellow
        return
    }

    Write-Host "🧪 Running integration tests..." -ForegroundColor Green

    try {
        $testScript = $null

        if (Test-Path "bin/run-integration-test.ps1") {
            $testScript = "bin/run-integration-test.ps1"
            Write-Host "   Using PowerShell test script" -ForegroundColor Cyan
        }
        elseif (Test-Path "bin/run-integration-test.sh") {
            $testScript = "bin/run-integration-test.sh"
            Write-Host "   Using Bash test script" -ForegroundColor Cyan
        }
        else {
            Write-Warning "Integration test script not found, skipping tests"
            Write-Host "   Expected paths: bin/run-integration-test.ps1 or bin/run-integration-test.sh" -ForegroundColor Gray
            return
        }

        $testStartTime = Get-Date

        if ($testScript.EndsWith(".ps1")) {
            & $testScript
        } else {
            bash $testScript
        }

        if ($LASTEXITCODE -ne 0) {
            throw "Integration tests failed with exit code: $LASTEXITCODE"
        }

        $testDuration = (Get-Date) - $testStartTime
        Write-Host "✅ Integration tests passed!" -ForegroundColor Green
        Write-Host "   ⏱️  Test duration: $($testDuration.ToString('mm\:ss'))" -ForegroundColor Cyan
    }
    catch {
        Write-Host "❌ Integration tests failed" -ForegroundColor Red
        Write-Host "📋 Container logs:" -ForegroundColor Yellow
        docker logs $CONTAINER_NAME --tail 50
        throw "Integration tests failed: $($_.Exception.Message)"
    }
}

# Main execution
try {
    $scriptStartTime = Get-Date

    Write-Host "🚀 PulsarRPA Docker CI Local Testing" -ForegroundColor Magenta
    Write-Host "================================================" -ForegroundColor Gray

    # Step 1: Validate configuration
    Test-Configuration

    # Step 2: Check port availability
    Test-PortAvailability -TestPort $PORT

    # Step 3: Build Docker image
    Build-DockerImage

    # Step 4: Start container
    $containerId = Start-Container

    # Step 5: Wait for service to be healthy
    Wait-ForHealthy

    # Step 6: Run integration tests
    Invoke-IntegrationTests

    $totalDuration = (Get-Date) - $scriptStartTime
    Write-Host "================================================" -ForegroundColor Gray
    Write-Host "🎉 CI testing completed!" -ForegroundColor Green
    Write-Host "   ⏱️  Total duration: $($totalDuration.ToString('mm\:ss'))" -ForegroundColor Cyan
    Write-Host "   🏷️  Image: $IMAGE_NAME" -ForegroundColor Cyan
    Write-Host "   🌐 Access: http://localhost:$PORT" -ForegroundColor Cyan
    Write-Host "   🎯 Health check: http://localhost:$PORT/actuator/health" -ForegroundColor Cyan

}
catch {
    Write-Host "❌ CI testing failed: $($_.Exception.Message)" -ForegroundColor Red
    Write-Host "💡 Suggestions to check:" -ForegroundColor Yellow
    Write-Host "   1. Is Docker Desktop running?" -ForegroundColor Yellow
    Write-Host "   2. Is port $PORT already in use?" -ForegroundColor Yellow
    Write-Host "   3. Is the Dockerfile path correct?" -ForegroundColor Yellow
    Write-Host "   4. Are health endpoints /actuator/health or /api/system/health enabled?" -ForegroundColor Yellow
    Write-Host "   5. Do the health endpoints return {`"status`":`"UP`"}?" -ForegroundColor Yellow

    # Show recent container logs if container exists
    $containerExists = docker ps -aq --filter "name=$CONTAINER_NAME" 2>$null
    if ($containerExists) {
        Write-Host "📋 Recent container logs:" -ForegroundColor Yellow
        docker logs $CONTAINER_NAME --tail 20 2>$null
    }

    exit 1
}
finally {
    # Cleanup will be called automatically
    if (-not $SkipTests) {
        Write-Host "🧹 Performing final cleanup..." -ForegroundColor Gray
        Invoke-Cleanup
    } else {
        Write-Host "ℹ️  Container remains running (tests were skipped)" -ForegroundColor Cyan
        Write-Host "   Manual stop: docker stop $CONTAINER_NAME" -ForegroundColor Gray
    }
}