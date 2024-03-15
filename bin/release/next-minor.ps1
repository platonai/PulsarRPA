# Set up variables
$bin = Split-Path -Parent $MyInvocation.MyCommand.Definition
$bin = (Resolve-Path "$bin\..").Path
$APP_HOME = (Resolve-Path "$bin\..").Path

# Get version information
$SNAPSHOT_VERSION = Get-Content "$APP_HOME\VERSION" -TotalCount 1
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
$NEXT_SNAPSHOT_VERSION | Set-Content "$APP_HOME\VERSION"

# Update $APP_HOME/pom.xml
$pomXmlPath = "$APP_HOME\pom.xml"
((Get-Content $pomXmlPath) -replace "<tag>v$VERSION</tag>", "<tag>v$NEXT_VERSION</tag>") | Set-Content $pomXmlPath

# Update pom.xml files
Get-ChildItem "$APP_HOME" -Depth 3 -Filter 'pom.xml' -Recurse | ForEach-Object {
  ((Get-Content $_.FullName) -replace $SNAPSHOT_VERSION, $NEXT_SNAPSHOT_VERSION) | Set-Content $_.FullName
}

# Files containing the version number to upgrade
$VERSION_AWARE_FILES = @(
  "$APP_HOME\README.adoc"
  "$APP_HOME\README-CN.adoc"
)

# Replace version numbers in files
foreach ($F in $VERSION_AWARE_FILES) {
  if (Test-Path $F) {
    ((Get-Content $F) -replace "\b$PREFIX\.[0-9]+\b", $NEXT_VERSION) | Set-Content $F
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
