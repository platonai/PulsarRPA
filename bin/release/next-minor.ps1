# 🔍 Find the first parent directory containing the VERSION file
$AppHome=(Get-Item -Path $MyInvocation.MyCommand.Path).Directory
while ($AppHome -ne $null -and !(Test-Path "$AppHome/VERSION")) {
  $AppHome = Split-Path -Parent $AppHome
}
Set-Location $AppHome

# Get version information
$SNAPSHOT_VERSION = Get-Content "$AppHome\VERSION" -TotalCount 1
$VERSION = $SNAPSHOT_VERSION -replace "-SNAPSHOT", ""
$parts = $VERSION -split "\."
$PREFIX = $parts[0] + "." + $parts[1]
$MINOR_VERSION = [int]$parts[2]
$MINOR_VERSION = $MINOR_VERSION + 1

$NEXT_VERSION = "$PREFIX.$MINOR_VERSION"
$NEXT_SNAPSHOT_VERSION = "$NEXT_VERSION-SNAPSHOT"

# Output new version
Write-Host "New version: $NEXT_SNAPSHOT_VERSION"

# Update VERSION file
$NEXT_SNAPSHOT_VERSION | Set-Content "$AppHome\VERSION"

# Update $AppHome/pom.xml
$pomXmlPath = "$AppHome\pom.xml"
((Get-Content $pomXmlPath) -replace "<tag>v$VERSION</tag>", "<tag>v$NEXT_VERSION</tag>") | Set-Content $pomXmlPath

# Update pom.xml files
Get-ChildItem "$AppHome" -Depth 3 -Filter 'pom.xml' -Recurse | ForEach-Object {
  ((Get-Content $_.FullName) -replace $SNAPSHOT_VERSION, $NEXT_SNAPSHOT_VERSION) | Set-Content $_.FullName
}

# Files containing the version number to upgrade
$VERSION_AWARE_FILES = @(
  "$AppHome\README.md"
  "$AppHome\README-CN.md"
)

# Replace version numbers in files
foreach ($F in $VERSION_AWARE_FILES) {
  if (Test-Path $F) {
    # Replace SNAPSHOT versions
    ((Get-Content $F) -replace $SNAPSHOT_VERSION, $NEXT_SNAPSHOT_VERSION) | Set-Content $F

    # Replace version numbers in the format "x.y.z" where x.y is the prefix and z is the minor version number
    ((Get-Content $F) -replace "\b$PREFIX\.[0-9]+\b", $NEXT_VERSION) | Set-Content $F

    # Replace version numbers in paths like "download/v3.0.8/PulsarRPA.jar"
    ((Get-Content $F) -replace "(/v$PREFIX\.[0-9]+/)", "/v$NEXT_VERSION/") | Set-Content $F

    # Replace version numbers prefixed with v like "v3.0.8"
    ((Get-Content $F) -replace "\bv$PREFIX\.[0-9]+\b", "v$NEXT_VERSION") | Set-Content $F
  }
}

# Commit comment
$COMMENT =$NEXT_SNAPSHOT_VERSION -replace "-SNAPSHOT", ""

# Prompt for confirmation
Write-Host "Ready to commit with comment: <$COMMENT>"
$confirm = Read-Host -Prompt "Are you sure to continue? [Y/n]"
if ($confirm -eq 'Y') {
  git add .
  git commit -m "$COMMENT"
  git push
}
