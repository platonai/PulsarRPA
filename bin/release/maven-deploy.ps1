#!/usr/bin/env pwsh

$repoRoot = (git rev-parse --show-toplevel 2>$null)
Set-Location $repoRoot

function printUsage {
  Write-Host "Usage: maven-deploy.ps1 [-clean|-test]"
  exit 1
}

# Maven command and options
$MvnCmd = Join-Path $repoRoot '.\mvnw.cmd'

# Initialize flags and additional arguments
$PerformClean = $false
$SkipTests = $true

$AdditionalMvnArgs = @()

# Parse command-line arguments
foreach ($Arg in $args)
{
  switch ($Arg)
  {
    '-clean' {
      $PerformClean = $true;
    }
    { '-t', '-test' } {
      $SkipTests = $false;
    }
    { $_ -in "-h", "-help", "--help" } {
      printUsage
    }
    { $_ -in "-*", "--*" } {
      printUsage
    }
    Default {
      $AdditionalMvnArgs += $Arg
    }
  }
}

Write-Host "Deploy the project ..."
Write-Host "Changing version ..."

$SNAPSHOT_VERSION = Get-Content "$repoRoot\VERSION" -TotalCount 1
$VERSION =$SNAPSHOT_VERSION -replace "-SNAPSHOT", ""
$VERSION | Set-Content "$repoRoot\VERSION"

# Replace SNAPSHOT version with the release version
@('pom.xml') | ForEach-Object {
  Get-ChildItem -Path "$repoRoot" -Depth 5 -Filter $_ -Recurse | ForEach-Object {
    (Get-Content $_.FullName) -replace $SNAPSHOT_VERSION, $VERSION | Set-Content -Encoding utf8 $_.FullName
  }
}

if ($PerformClean) {
  & $MvnCmd clean
  if ($LastExitCode -ne 0) {
    exit $LastExitCode
  }
}

if ($SkipTests) {
  & $MvnCmd deploy -P deploy,release -DskipTests
} else {
  & $MvnCmd deploy -P deploy,release
}

$exitCode =$LastExitCode
if ($exitCode -eq 0) {
  Write-Host "Build successfully"
} else {
  exit $exitCode
}

Set-Location $repoRoot

Write-Host "Artifacts are uploaded, you should publish manually:"
Write-Host "https://central.sonatype.com/publishing"
Write-Host "Hit the following link to check if the artifacts are synchronized to the maven center: "
Write-Host "https://repo1.maven.org/maven2/ai/platon/pulsar"
