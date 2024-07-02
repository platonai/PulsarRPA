# Find the first parent directory containing the VERSION file
$AppHome=(Get-Item -Path $MyInvocation.MyCommand.Path).Directory
while ($AppHome -ne $null -and !(Test-Path "$AppHome/VERSION")) {
  $AppHome=$AppHome.Parent
}
cd $AppHome

function printUsage {
  Write-Host "Usage: deploy.ps1 [-clean|-test]"
  exit 1
}

# Maven command and options
$MvnCmd = Join-Path $AppHome '.\mvnw'

# Initialize flags and additional arguments
$PerformClean = $false
$SkipTests = $true

$MvnOptions = @()
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

# Conditionally add Maven options based on flags
if ($PerformClean)
{
  $MvnOptions += 'clean'
}

if ($SkipTests)
{
  $AdditionalMvnArgs += '-DskipTests'
}

# Function to execute Maven command in a given directory
Function Invoke-MavenBuild
{
  param([string]$Directory, [Object]$MvnOptions)

  try
  {
    Push-Location $Directory -ErrorAction Stop

    & $MvnCmd @MvnOptions

    if ($LASTEXITCODE -ne 0)
    {
      Write-Warning "Maven command failed in $Directory"
    }

    Pop-Location
  }
  catch
  {
    Write-Error "Failed to change directory or execute Maven: $_"
  }
}

# Execute Maven package in the application home directory
$MvnOptions += 'install'
$AdditionalMvnArgs += '-Pall-modules'

$MvnOptions += $AdditionalMvnArgs
Invoke-MavenBuild -Directory $AppHome -MvnOptions $MvnOptions
