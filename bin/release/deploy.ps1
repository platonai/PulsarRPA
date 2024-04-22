$MAVEN_HOME = "D:\Program Files\maven\apache-maven-3.8.8"
$MVN = Join-Path $MAVEN_HOME "bin\mvn.cmd"

function printUsage {
  Write-Host "Usage: deploy [-clean|-test]"
  exit 1
}

if ($args.Length -gt 0) {
  printUsage
}

$TEST =$false
$CLEAN =$false

while ($args.Length -gt 0) {
  switch ($args[0]) {
    "-clean" {
      $CLEAN =$true
      $args =$args[1..($args.Length-1)] # past argument
    }
    "-test" {
      $TEST =$true
      $args =$args[1..($args.Length-1)] # past argument
    }
    { $_ -in "-h", "-help", "--help" } {
      printUsage
    }
    { $_ -in "-*", "--*" } {
      printUsage
    }
    default {
      printUsage
    }
  }
}

$bin = Split-Path -Parent $MyInvocation.MyCommand.Definition
$bin = (Resolve-Path "$bin\..").Path
$APP_HOME = (Resolve-Path "$bin\..").Path

Write-Host "Deploy the project ..."
Write-Host "Changing version ..."

$SNAPSHOT_VERSION = Get-Content "$APP_HOME\VERSION" -TotalCount 1
$VERSION =$SNAPSHOT_VERSION -replace "-SNAPSHOT", ""
$VERSION | Set-Content "$APP_HOME\VERSION"

Get-ChildItem -Path "$APP_HOME" -Depth 2 -Filter 'pom.xml' -Recurse | ForEach-Object {
  (Get-Content $_.FullName) -replace $SNAPSHOT_VERSION, $VERSION | Set-Content $_.FullName
}

if ($CLEAN) {
  & $MVN clean
  if ($LastExitCode -ne 0) {
    exit $LastExitCode
  }
}

if ($TEST) {
  & $MVN deploy -Pplaton-release -Pplaton-deploy
} else {
  & $MVN deploy -Pplaton-release -Pplaton-deploy -DskipTests=$true
}

$exitCode =$LastExitCode
if ($exitCode -eq 0) {
  Write-Host "Build successfully"
} else {
  exit $exitCode
}

Write-Host "Artifacts are staged remotely, you should close and release the staging manually:"
Write-Host "https://oss.sonatype.org/#stagingRepositories"
Write-Host "Hit the following link to check if the artifacts are synchronized to the maven center: "
Write-Host "https://repo1.maven.org/maven2/ai/platon/pulsar"
