#!/usr/bin/env pwsh

# 🔍 Find the first parent directory containing the VERSION file
$AppHome=(Get-Item -Path $MyInvocation.MyCommand.Path).Directory
while ($AppHome -ne $null -and !(Test-Path "$AppHome/VERSION")) {
  $AppHome = Split-Path -Parent $AppHome
}
Set-Location $AppHome

# 设置 PowerShell 编码以支持中文输出
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8

Write-Host "正在使用 BuildKit 缓存挂载构建 Browser4..." -ForegroundColor Cyan

# 获取 Windows Maven 仓库路径 (仅用于信息显示)
try {
    $MAVEN_REPO_PATH = & .\mvnw help:evaluate -D"expression=settings.localRepository" -q -DforceStdout
    if ($LASTEXITCODE -eq 0 -and ![string]::IsNullOrWhiteSpace($MAVEN_REPO_PATH)) {
        Write-Host "本地 Maven 仓库: $MAVEN_REPO_PATH" -ForegroundColor Green
        if (Test-Path $MAVEN_REPO_PATH) {
            $repoSize = (Get-ChildItem $MAVEN_REPO_PATH -Recurse -ErrorAction SilentlyContinue |
                    Measure-Object -Property Length -Sum).Sum / 1GB
            Write-Host "仓库大小: $([math]::Round($repoSize, 2)) GB" -ForegroundColor Cyan
        }
    }
} catch {
    Write-Host "无法获取 Maven 仓库信息，将使用 Docker 管理的缓存" -ForegroundColor Yellow
}

# 执行 Docker 构建命令
try {
    Write-Host "开始构建 Docker 镜像 (使用 BuildKit 缓存)..." -ForegroundColor Green

    # 启用 BuildKit
    $env:DOCKER_BUILDKIT = "1"

    docker build `
        --progress=plain `
        -t pulsar-rpa:buildkit-cache `
        -f docker/pulsar-rpa-prod/Dockerfile `
        .

    if ($LASTEXITCODE -eq 0) {
        Write-Host "构建完成! 使用了 BuildKit 缓存管理 Maven 依赖" -ForegroundColor Green
        Write-Host "注意: 依赖项已缓存在 Docker 管理的卷中，后续构建将更快" -ForegroundColor Cyan
    } else {
        Write-Host "构建失败，退出代码: $LASTEXITCODE" -ForegroundColor Red
    }
}
catch {
    Write-Host "构建过程中发生错误: $_" -ForegroundColor Red
}

Write-Host "按任意键继续..."
$null = $Host.UI.RawUI.ReadKey('NoEcho,IncludeKeyDown')