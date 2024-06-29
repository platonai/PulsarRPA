$BIN = Split-Path -Parent $MyInvocation.MyCommand.Definition
$AppHome = (Resolve-Path "$BIN\..").Path

$CLEAN = $false
$SKIP_TEST = $true

$MVNW="$AppHome\mvnw"
$MVN_OPTS = @()

if ($CLEAN) {
  $MVN_OPTS += "clean"
}

if ($SKIP_TEST) {
  $MVN_OPTS += "-DskipTests=true"
}
$MVN_OPTS += "-Pall-modules"


# Function to execute Maven command in a given directory
Function Invoke-MavenBuild
{
  param([string]$Directory)
  try
  {
    Push-Location $Directory -ErrorAction Stop
    & $MvnCmd @MvnOptions + $AdditionalMvnArgs
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
Invoke-MavenBuild -Directory $AppHome
