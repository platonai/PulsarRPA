#!/usr/bin/env pwsh

# Enhanced CI script for local Docker testing with improved error handling and user experience

param(
    [string]$Dockerfile = $env:DOCKERFILE,
    [string]$ImageName = $env:IMAGE_NAME,
    [string]$ContainerName = $env:CONTAINER_NAME,
    [int]$Port = $env:PORT,
    [int]$HealthCheckTimeout = $env:HEALTH_CHECK_TIMEOUT,
    [switch]$SkipTests = $false,
    [switch]$Verbose = $false,
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
    Write-Host "🔍 验证配置..." -ForegroundColor Yellow

    if (-not (Test-Path $DOCKERFILE)) {
        throw "Dockerfile 不存在: $DOCKERFILE"
    }

    Write-Host "✅ 配置验证通过" -ForegroundColor Green
    Write-Host "   📄 Dockerfile: $DOCKERFILE" -ForegroundColor Cyan
    Write-Host "   🏷️  镜像名称: $IMAGE_NAME" -ForegroundColor Cyan
    Write-Host "   📦 容器名称: $CONTAINER_NAME" -ForegroundColor Cyan
    Write-Host "   🔌 端口: $PORT" -ForegroundColor Cyan
    Write-Host "   ⏱️  健康检查超时: ${HEALTH_CHECK_TIMEOUT}秒" -ForegroundColor Cyan
}

# Enhanced cleanup function
function Invoke-Cleanup {
    param([bool]$Silent = $false)

    if (-not $Silent) {
        Write-Host "🧹 清理资源..." -ForegroundColor Yellow
    }

    try {
        # Stop and remove container
        $existingContainer = docker ps -aq --filter "name=$CONTAINER_NAME" 2>$null
        if ($existingContainer) {
            if (-not $Silent) {
                Write-Host "   停止容器: $CONTAINER_NAME" -ForegroundColor Gray
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
            Write-Warning "清理过程中出现警告: $($_.Exception.Message)"
        }
    }
}

# Register cleanup on script exit
trap {
    Write-Host "`n❌ 脚本被中断" -ForegroundColor Red
    Invoke-Cleanup
    break
}

# Check if port is available
function Test-PortAvailability {
    param([int]$TestPort)

    try {
        $portInUse = Get-NetTCPConnection -LocalPort $TestPort -State Listen -ErrorAction SilentlyContinue
        if ($portInUse) {
            throw "端口 $TestPort 已被占用"
        }
        Write-Host "✅ 端口 $TestPort 可用" -ForegroundColor Green
    }
    catch [System.Management.Automation.CommandNotFoundException] {
        Write-Warning "无法检查端口占用情况，继续执行..."
    }
    catch {
        throw $_
    }
}

# Enhanced health check function
function Test-HealthEndpoint {
    param(
        [string]$Url,
        [int]$TimeoutSeconds = 5
    )

    try {
        if ($Verbose) {
            Write-Host "   检查: $Url" -ForegroundColor Gray
        }

        $response = Invoke-RestMethod -Uri $Url -Method Get -TimeoutSec $TimeoutSeconds -ErrorAction Stop

        # Check for different response formats
        if ($response.status -eq "UP" -or $response -eq "OK" -or $response.health -eq "healthy") {
            return $true
        }

        # If response is a string and contains positive indicators
        if ($response -is [string] -and ($response -match "UP|OK|healthy|running")) {
            return $true
        }
    }
    catch {
        if ($Verbose) {
            Write-Host "   健康检查失败: $($_.Exception.Message)" -ForegroundColor Gray
        }
    }
    return $false
}

# Build Docker image with enhanced options
function Build-DockerImage {
    Write-Host "🏗️  构建 Docker 镜像: $IMAGE_NAME" -ForegroundColor Green

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
            Write-Host "   启用 BuildKit 缓存" -ForegroundColor Cyan
        } else {
            $buildArgs += "--no-cache"
            Write-Host "   禁用缓存构建" -ForegroundColor Yellow
        }

        if ($Verbose) {
            $buildArgs += "--progress=plain"
        }

        $buildArgs += "."

        Write-Host "   执行: docker $($buildArgs -join ' ')" -ForegroundColor Gray

        $startTime = Get-Date
        & docker @buildArgs

        if ($LASTEXITCODE -ne 0) {
            throw "Docker 构建失败，退出代码: $LASTEXITCODE"
        }

        $duration = (Get-Date) - $startTime
        Write-Host "✅ 镜像构建成功!" -ForegroundColor Green
        Write-Host "   ⏱️  构建耗时: $($duration.ToString('mm\:ss'))" -ForegroundColor Cyan
    }
    catch {
        throw "构建失败: $($_.Exception.Message)"
    }
}

# Start container with enhanced monitoring
function Start-Container {
    Write-Host "🚀 启动容器: $CONTAINER_NAME" -ForegroundColor Green

    try {
        $startArgs = @(
            "run", "--rm", "-d"
            "--name", $CONTAINER_NAME
            "-p", "${PORT}:8182"
        )

        # Add health check if available
        $startArgs += "--health-interval=10s"
        $startArgs += "--health-timeout=5s"
        $startArgs += "--health-retries=3"

        $startArgs += $IMAGE_NAME

        $CONTAINER_ID = & docker @startArgs

        if ($LASTEXITCODE -ne 0) {
            throw "容器启动失败，退出代码: $LASTEXITCODE"
        }

        Write-Host "✅ 容器启动成功!" -ForegroundColor Green
        Write-Host "   🆔 容器ID: $($CONTAINER_ID.Substring(0, 12))" -ForegroundColor Cyan
        Write-Host "   🌐 访问地址: http://localhost:$PORT" -ForegroundColor Cyan

        return $CONTAINER_ID
    }
    catch {
        throw "启动失败: $($_.Exception.Message)"
    }
}

# Enhanced health monitoring
function Wait-ForHealthy {
    Write-Host "🔍 等待服务启动..." -ForegroundColor Yellow

    $retryCount = 0
    $maxRetries = [math]::Floor($HEALTH_CHECK_TIMEOUT / 2)
    $healthEndpoints = @(
        "http://localhost:$PORT/actuator/health",
        "http://localhost:$PORT/api/system/health",
        "http://localhost:$PORT/health",
        "http://localhost:$PORT/"
    )

    $healthyEndpoints = @()

    foreach ($endpoint in $healthEndpoints) {
        $retryCount = 0
        Write-Host "   检查端点: $endpoint" -ForegroundColor Cyan

        do {
            $retryCount++
            if ($Verbose) {
                Write-Host "   尝试 $retryCount/$maxRetries..." -ForegroundColor Gray
            }

            if (Test-HealthEndpoint $endpoint) {
                Write-Host "   ✅ $endpoint 响应正常" -ForegroundColor Green
                $healthyEndpoints += $endpoint
                break
            }

            if ($retryCount -ge $maxRetries) {
                Write-Warning "   ⚠️  $endpoint 超时"
                break
            }

            Start-Sleep -Seconds 2
        } while ($true)
    }

    if ($healthyEndpoints.Count -eq 0) {
        Write-Host "❌ 所有健康检查端点都失败了" -ForegroundColor Red
        Write-Host "📋 容器日志:" -ForegroundColor Yellow
        docker logs $CONTAINER_NAME --tail 50
        throw "健康检查失败，服务在 $HEALTH_CHECK_TIMEOUT 秒内未能启动"
    }

    Write-Host "✅ 服务健康检查通过! ($($healthyEndpoints.Count)/$($healthEndpoints.Count) 端点响应)" -ForegroundColor Green
}

# Run integration tests
function Invoke-IntegrationTests {
    if ($SkipTests) {
        Write-Host "⏭️  跳过集成测试" -ForegroundColor Yellow
        return
    }

    Write-Host "🧪 运行集成测试..." -ForegroundColor Green

    try {
        $testScript = $null

        if (Test-Path "bin/run-integration-test.ps1") {
            $testScript = "bin/run-integration-test.ps1"
            Write-Host "   使用 PowerShell 测试脚本" -ForegroundColor Cyan
        }
        elseif (Test-Path "bin/run-integration-test.sh") {
            $testScript = "bin/run-integration-test.sh"
            Write-Host "   使用 Bash 测试脚本" -ForegroundColor Cyan
        }
        else {
            throw "未找到集成测试脚本 (bin/run-integration-test.ps1 或 bin/run-integration-test.sh)"
        }

        $testStartTime = Get-Date

        if ($testScript.EndsWith(".ps1")) {
            & $testScript
        } else {
            bash $testScript
        }

        if ($LASTEXITCODE -ne 0) {
            throw "集成测试失败，退出代码: $LASTEXITCODE"
        }

        $testDuration = (Get-Date) - $testStartTime
        Write-Host "✅ 集成测试通过!" -ForegroundColor Green
        Write-Host "   ⏱️  测试耗时: $($testDuration.ToString('mm\:ss'))" -ForegroundColor Cyan
    }
    catch {
        Write-Host "❌ 集成测试失败" -ForegroundColor Red
        Write-Host "📋 容器日志:" -ForegroundColor Yellow
        docker logs $CONTAINER_NAME --tail 50
        throw "集成测试失败: $($_.Exception.Message)"
    }
}

# Main execution
try {
    $scriptStartTime = Get-Date

    Write-Host "🚀 PulsarRPA Docker CI 本地测试" -ForegroundColor Magenta
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
    Write-Host "🎉 CI 测试完成!" -ForegroundColor Green
    Write-Host "   ⏱️  总耗时: $($totalDuration.ToString('mm\:ss'))" -ForegroundColor Cyan
    Write-Host "   🏷️  镜像: $IMAGE_NAME" -ForegroundColor Cyan
    Write-Host "   🌐 访问: http://localhost:$PORT" -ForegroundColor Cyan

}
catch {
    Write-Host "❌ CI 测试失败: $($_.Exception.Message)" -ForegroundColor Red
    Write-Host "💡 建议检查:" -ForegroundColor Yellow
    Write-Host "   1. Docker Desktop 是否正在运行" -ForegroundColor Yellow
    Write-Host "   2. 端口 $PORT 是否被占用" -ForegroundColor Yellow
    Write-Host "   3. Dockerfile 路径是否正确" -ForegroundColor Yellow
    Write-Host "   4. Maven 配置是否正确" -ForegroundColor Yellow
    exit 1
}
finally {
    # Cleanup will be called automatically
    Invoke-Cleanup
}