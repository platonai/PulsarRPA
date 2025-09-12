#!/usr/bin/env pwsh

# Enhanced CI script for local Docker testing with improved error handling and user experience

param(
    [ValidateScript({Test-Path $_ -PathType Leaf})]
    [string]$Dockerfile = $env:DOCKERFILE ?? "Dockerfile",

    [string]$ImageName = $env:IMAGE_NAME ?? "galaxyeye88/pulsar-rpa",

    [string]$ContainerName = $env:CONTAINER_NAME ?? "pulsar-rpa",

    [ValidateRange(1024, 65535)]
    [int]$Port = $(if ($env:PORT -and [int]::TryParse($env:PORT, [ref]$null)) { [int]$env:PORT } else { 8182 }),

    [ValidateRange(10, 300)]
    [int]$HealthCheckTimeout = $(if ($env:HEALTH_CHECK_TIMEOUT -and [int]::TryParse($env:HEALTH_CHECK_TIMEOUT, [ref]$null)) { [int]$env:HEALTH_CHECK_TIMEOUT } else { 60 }),

    [ValidateSet("container", "compose")]
    [string]$Mode = "compose",

    [switch]$SkipTests = $false,
    [switch]$Verbose = $true,
    [switch]$UseCache = $true,
    [switch]$SkipBuild = $false,
    [switch]$CleanUp = $true
)

# Find the first parent directory containing the VERSION file
$AppHome = (Get-Item -Path $MyInvocation.MyCommand.Path).Directory
while ($AppHome -ne $null -and !(Test-Path "$AppHome/VERSION")) {
    $AppHome = Split-Path -Parent $AppHome
}
Set-Location $AppHome

# Set PowerShell encoding for UTF-8 support
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8

# Error handling
$ErrorActionPreference = "Stop"

# Configuration summary
function Show-Configuration {
    Write-Host "🔧 Configuration Summary:" -ForegroundColor Cyan
    Write-Host "   📄 Dockerfile: $Dockerfile" -ForegroundColor Gray
    Write-Host "   🏷️  Image: $ImageName" -ForegroundColor Gray
    Write-Host "   📦 Container: $ContainerName" -ForegroundColor Gray
    Write-Host "   🔌 Port: $Port" -ForegroundColor Gray
    Write-Host "   ⏱️  Health timeout: $HealthCheckTimeout seconds" -ForegroundColor Gray
    Write-Host "   🎯 Mode: $Mode" -ForegroundColor Gray
    Write-Host "   🧪 Skip tests: $SkipTests" -ForegroundColor Gray
    Write-Host "   💬 Verbose: $Verbose" -ForegroundColor Gray
    Write-Host "   🏗️  Use cache: $UseCache" -ForegroundColor Gray
    Write-Host "   🧹 Cleanup: $CleanUp" -ForegroundColor Gray
}

# Validate prerequisites
function Test-Prerequisites {
    Write-Host "🔍 Validating prerequisites..." -ForegroundColor Yellow

    # Check Docker
    try {
        docker --version | Out-Null
    }
    catch {
        throw "Docker is not available. Please install Docker Desktop."
    }

    # Check Docker Compose if needed
    if ($Mode -eq "compose") {
        try {
            docker-compose --version | Out-Null
        }
        catch {
            throw "Docker Compose is not available."
        }
    }

    # Check Dockerfile exists
    if (-not (Test-Path $Dockerfile)) {
        throw "Dockerfile not found: $Dockerfile"
    }

    # Check required environment variables early
    if (-not $env:DEEPSEEK_API_KEY) {
        Write-Host "⚠️  Environment variable DEEPSEEK_API_KEY is not set." -ForegroundColor Yellow
        Write-Host "   Please set it before running this script, e.g.:" -ForegroundColor Yellow
        Write-Host "   `$env:DEEPSEEK_API_KEY = 'your_api_key_here'" -ForegroundColor Yellow
        throw "Environment variable DEEPSEEK_API_KEY is required."
    }

    Write-Host "✅ Prerequisites validation passed" -ForegroundColor Green
}

# Enhanced cleanup function that handles both modes
function Invoke-Cleanup {
    param([bool]$Silent = $false)

    if (-not $Silent) {
        Write-Host "🧹 Cleaning up resources..." -ForegroundColor Yellow
    }

    try {
        if ($Mode -eq "compose") {
            # Docker Compose cleanup
            if (Test-Path "docker-compose.yml") {
                if (-not $Silent) {
                    Write-Host "   Stopping Docker Compose services..." -ForegroundColor Gray
                }
                docker-compose --profile scent down --remove-orphans 2>$null | Out-Null
            }
        } else {
            # Single container cleanup
            $existingContainer = docker ps -aq --filter "name=$ContainerName" 2>$null
            if ($existingContainer) {
                if (-not $Silent) {
                    Write-Host "   Stopping container: $ContainerName" -ForegroundColor Gray
                }
                docker stop $ContainerName 2>$null | Out-Null
                docker rm $ContainerName 2>$null | Out-Null
            }
        }

        # Clean up dangling images if cache is disabled
        if (-not $UseCache) {
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
    if ($CleanUp) {
        Invoke-Cleanup
    }
    break
}

# Cross-platform port availability check
function Test-PortAvailability {
    param([int]$TestPort)

    try {
        # Try Windows-specific method first
        if (Get-Command "Get-NetTCPConnection" -ErrorAction SilentlyContinue) {
            $portInUse = Get-NetTCPConnection -LocalPort $TestPort -State Listen -ErrorAction SilentlyContinue
            if ($portInUse) {
                throw "Port $TestPort is already in use"
            }
        } else {
            # Cross-platform method using netstat
            $netstatOutput = netstat -an 2>$null | Select-String ":$TestPort "
            if ($netstatOutput) {
                throw "Port $TestPort appears to be in use"
            }
        }
        Write-Host "✅ Port $TestPort is available" -ForegroundColor Green
    }
    catch {
        if ($_.Exception.Message -like "*already in use*" -or $_.Exception.Message -like "*appears to be in use*") {
            throw $_
        }
        Write-Warning "Cannot reliably check port availability: $($_.Exception.Message)"
        Write-Host "   Continuing anyway..." -ForegroundColor Yellow
    }
}

# Build Docker image
function Build-DockerImage {
    if ($SkipBuild) {
        Write-Host "⏭️  Skipping Docker build" -ForegroundColor Yellow
        return
    }

    Write-Host "🏗️  Building Docker image: $ImageName" -ForegroundColor Green

    try {
        # Clean up existing containers first
        Invoke-Cleanup -Silent $true

        # Prepare build arguments
        $buildArgs = @(
            "build"
            "-t", $ImageName
            "-f", $Dockerfile
        )

        if ($UseCache) {
            $env:DOCKER_BUILDKIT = "1"
            Write-Host "   BuildKit caching enabled" -ForegroundColor Cyan
        } else {
            $buildArgs += "--no-cache"
            Write-Host "   Cache disabled" -ForegroundColor Yellow
        }

        if ($Verbose) {
            $buildArgs += "--progress=plain"
        }

        # Maven repository handling
        $mavenRepo = "$env:USERPROFILE\.m2"
        if (Test-Path $mavenRepo) {
            $buildArgs += "--build-arg", "USE_HOST_MAVEN_REPO=true"
            Write-Host "   Using host Maven repository: $mavenRepo" -ForegroundColor Cyan
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

# Start services based on mode
function Start-Services {
    if ($Mode -eq "compose") {
        Start-ComposedServices
    } else {
        Start-SingleContainer
    }
}

# Docker Compose startup
function Start-ComposedServices {
    Write-Host "🚀 Starting Docker Compose services" -ForegroundColor Green

    try {
        $composeArgs = @(
            "--profile", "scent"
            "up", "-d"
            "--remove-orphans"
        )

        if (-not $SkipBuild) {
            $composeArgs += "--build"
        }

        Write-Host "   Executing: docker-compose $($composeArgs -join ' ')" -ForegroundColor Gray
        & docker-compose @composeArgs

        if ($LASTEXITCODE -ne 0) {
            throw "Docker Compose failed with exit code: $LASTEXITCODE"
        }

        Write-Host "✅ Services started successfully!" -ForegroundColor Green
    }
    catch {
        throw "Service startup failed: $($_.Exception.Message)"
    }
}

# Single container startup
function Start-SingleContainer {
    Write-Host "🚀 Starting container: $ContainerName" -ForegroundColor Green

    try {
        $startArgs = @(
            "run", "--rm", "-d"
            "--name", $ContainerName
            "-e", "DEEPSEEK_API_KEY=$env:DEEPSEEK_API_KEY"
            "-e", "PROXY_ROTATION_URL=$env:PROXY_ROTATION_URL"
            "-e", "BROWSER_DISPLAY_MODE=HEADLESS"
            "-e", "SERVER_PORT=8182"
            "-e", "SERVER_ADDRESS=0.0.0.0"
            "-p", "${Port}:8182"
            "--health-cmd=curl -f http://localhost:8182/actuator/health || exit 1"
            "--health-interval=10s"
            "--health-timeout=5s"
            "--health-retries=3"
            "--health-start-period=30s"
            $ImageName
        )

        $containerId = & docker @startArgs

        if ($LASTEXITCODE -ne 0) {
            throw "Container startup failed with exit code: $LASTEXITCODE"
        }

        Write-Host "✅ Container started successfully!" -ForegroundColor Green
        Write-Host "   🆔 Container ID: $($containerId.Substring(0, 12))" -ForegroundColor Cyan
        Write-Host "   🌐 Access URL: http://localhost:$Port" -ForegroundColor Cyan

        return $containerId
    }
    catch {
        throw "Container startup failed: $($_.Exception.Message)"
    }
}

# Health check function
function Wait-ForHealthy {
    Write-Host "🔍 Waiting for service to be healthy..." -ForegroundColor Yellow

    $maxRetries = [math]::Floor($HealthCheckTimeout / 2)
    $healthEndpoints = @(
        "http://localhost:${Port}/actuator/health",
        "http://localhost:${Port}/api/system/health"
    )

    Write-Host "   Max attempts: $maxRetries" -ForegroundColor Cyan
    Write-Host "   Check interval: 2 seconds" -ForegroundColor Cyan

    for ($attempt = 1; $attempt -le $maxRetries; $attempt++) {
        Write-Host "   Attempt $attempt/$maxRetries..." -ForegroundColor Gray

        # Test each endpoint
        foreach ($endpoint in $healthEndpoints) {
            try {
                $response = curl -s -f $endpoint 2>$null
                if ($response -match "UP") {
                    Write-Host "✅ Service is healthy!" -ForegroundColor Green
                    Write-Host "   🎯 Endpoint: $endpoint" -ForegroundColor Cyan
                    return
                }
            }
            catch {
                # Continue to next endpoint
            }
        }

        if ($attempt -lt $maxRetries) {
            Start-Sleep -Seconds 2
        }
    }

    throw "Health check failed after $maxRetries attempts"
}

# Integration tests
function Invoke-IntegrationTests {
    if ($SkipTests) {
        Write-Host "⏭️  Skipping integration tests" -ForegroundColor Yellow
        return
    }

    Write-Host "🧪 Running integration tests..." -ForegroundColor Green

    $testScript = $null
    if (Test-Path "bin/tests/run-integration-test.ps1") {
        $testScript = "bin/tests/run-integration-test.ps1"
    } elseif (Test-Path "bin/run-integration-test.sh") {
        $testScript = "bin/run-integration-test.sh"
    } else {
        Write-Warning "No integration test script found, skipping tests"
        return
    }

    try {
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
        Write-Host "   ⏱️  Duration: $($testDuration.ToString('mm\:ss'))" -ForegroundColor Cyan
    }
    catch {
        throw "Integration tests failed: $($_.Exception.Message)"
    }
}

# Main execution
try {
    $scriptStartTime = Get-Date

    Write-Host "🚀 Browser4 Docker CI Local Testing" -ForegroundColor Magenta
    Write-Host "================================================" -ForegroundColor Gray

    # Step 1: Show configuration and validate prerequisites
    Show-Configuration
    Test-Prerequisites

    # Step 2: Check port availability
    Test-PortAvailability -TestPort $Port

    # Step 3: Build Docker image (if needed)
    Build-DockerImage

    # Step 4: Start services
    Start-Services

    # Step 5: Wait for service to be healthy
    Start-Sleep -Seconds 5
    Wait-ForHealthy

    # Step 6: Run integration tests
    Invoke-IntegrationTests

    $totalDuration = (Get-Date) - $scriptStartTime
    Write-Host "================================================" -ForegroundColor Gray
    Write-Host "🎉 CI testing completed successfully!" -ForegroundColor Green
    Write-Host "   ⏱️  Total time: $($totalDuration.ToString('mm\:ss'))" -ForegroundColor Cyan
    Write-Host "   🏷️  Image: $ImageName" -ForegroundColor Cyan
    Write-Host "   🌐 Access: http://localhost:$Port" -ForegroundColor Cyan
}
catch {
    Write-Host "❌ CI testing failed: $($_.Exception.Message)" -ForegroundColor Red
    Write-Host "💡 Troubleshooting suggestions:" -ForegroundColor Yellow
    Write-Host "   1. Check Docker Desktop is running" -ForegroundColor Yellow
    Write-Host "   2. Verify port $Port is available" -ForegroundColor Yellow
    Write-Host "   3. Ensure DEEPSEEK_API_KEY is set" -ForegroundColor Yellow
    Write-Host "   4. Check Dockerfile exists: $Dockerfile" -ForegroundColor Yellow

    # Show logs for debugging
    if ($Mode -eq "compose") {
        docker-compose logs --tail=20 2>$null
    } else {
        docker logs $ContainerName --tail=20 2>$null
    }

    exit 1
}
finally {
    if ($CleanUp) {
        Write-Host "🧹 Performing cleanup..." -ForegroundColor Gray
        Invoke-Cleanup
    } else {
        Write-Host "ℹ️  Services remain running (cleanup disabled)" -ForegroundColor Cyan
        if ($Mode -eq "compose") {
            Write-Host "   Stop with: docker-compose --profile scent down" -ForegroundColor Gray
        } else {
            Write-Host "   Stop with: docker stop $ContainerName" -ForegroundColor Gray
        }
    }
}