# 🔍 Find the first parent directory containing the VERSION file
$AppHome=(Get-Item -Path $MyInvocation.MyCommand.Path).Directory
while ($AppHome -ne $null -and !(Test-Path "$AppHome/VERSION")) {
  $AppHome = Split-Path -Parent $AppHome
}
Set-Location $AppHome

$gitExe = "git" # Assuming git is in the system PATH

# Get version information
$SNAPSHOT_VERSION = Get-Content "$AppHome\VERSION" -TotalCount 1
$VERSION =$SNAPSHOT_VERSION -replace "-SNAPSHOT", ""
$LAST_COMMIT_ID = &$gitExe log --format="%H" -n 1
$BRANCH = &$gitExe branch --show-current
$TAG = "v$VERSION"

# Replace SNAPSHOT version with the release version in readme files
function Replace-Version-In-ReadmeFiles {
  Write-Host "Replacing SNAPSHOT version with the release version in all markdown files"

  # Find all .md files recursively in the project directory
  $mdFiles = Get-ChildItem -Path "$AppHome" -Recurse -Filter *.md

  # Replace SNAPSHOT version with the release version in each file
  $mdFiles | ForEach-Object {
    $filePath = $_.FullName
    (Get-Content $filePath) -replace $SNAPSHOT_VERSION, $VERSION | Set-Content $filePath
    Write-Host "Updated version in: $filePath"

    # Replace tags in all README files, for example, v0.0.1 -> v$VERSION
    (Get-Content $filePath) -replace 'v\d\.\d{1,2}\.\d{1,3}', $VERSION | Set-Content $filePath
    Write-Host "Updated version in: $filePath"
  }

  # Stage, commit, and push the changes
  Push-ReadmeFiles-Changes

  Write-Host "Version replacement completed and changes pushed to the repository."
}

function Restore-WorkingBranch {
  Write-Host "Ready to restore"
  $confirm = Read-Host -Prompt "Are you sure to continue? [Y/n]"
  if ($confirm -eq 'Y') {
    & $gitExe restore .
  } else {
    Write-Host "Bye."
    exit 0
  }
}

function Pull-Changes {
  Write-Host "Ready to pull"
  $confirm = Read-Host -Prompt "Are you sure to continue? [Y/n]"
  if ($confirm -eq 'Y') {
    & $gitExe pull
  } else {
    Write-Host "Bye."
    exit 0
  }
}

function Push-ReadmeFiles-Changes {
  Write-Host "Ready to push to $BRANCH"
  $confirm = Read-Host -Prompt "Are you sure to continue? [Y/n]"
  if ($confirm -eq 'Y') {

    & $gitExe add *.md
    & $gitExe commit -m "Replace SNAPSHOT version with the release version in all markdown files"
    & $gitExe push

  } else {
    Write-Host "Do not push"
  }
}

function Add-Tag {
  Write-Host "Ready to add tag $TAG on$LAST_COMMIT_ID"
  $confirm = Read-Host -Prompt "Are you sure to continue? [Y/n]"
  if ($confirm -eq 'Y') {
    & $gitExe tag "$TAG" "$LAST_COMMIT_ID"
    Push-WithTags
  } else {
    Write-Host "Do not add tag."
  }
}

function Push-WithTags {
  Write-Host "Ready to push with tags to $BRANCH"
  $confirm = Read-Host -Prompt "Are you sure to continue? [Y/n]"
  if ($confirm -eq 'Y') {
    & $gitExe push --tags
  } else {
    Write-Host "Do not push with tags"
  }
}

function Merge-ToMainBranch {
  Write-Host "Ready to merge to main branch"
  $confirm = Read-Host -Prompt "Are you sure to continue? [Y/n]"
  if ($confirm -eq 'Y') {
    & $gitExe checkout main
    if ($LASTEXITCODE -ne 0) {
      & $gitExe checkout master
    }
    & $gitExe pull
    & $gitExe merge "$BRANCH"
    Push-ToMainBranch
  } else {
    Write-Host "Do not merge to main branch."
  }
}

function Push-ToMainBranch {
  Write-Host "Ready to push to main branch"
  $confirm = Read-Host -Prompt "Are you sure to continue? [Y/n]"
  if ($confirm -eq 'Y') {
    & $gitExe push
  } else {
    Write-Host "Bye."
    exit 0
  }
}

function Checkout-WorkingBranch {
  Write-Host "Ready to checkout working branch $BRANCH"
  $confirm = Read-Host -Prompt "Are you sure to continue? [Y/n]"
  if ($confirm -eq 'Y') {
    & $gitExe checkout "$BRANCH"
  } else {
    Write-Host "Remain on main branch"
  }
}

# Call functions
Restore-WorkingBranch
Pull-Changes
Replace-Version-In-ReadmeFiles
Merge-ToMainBranch
Checkout-WorkingBranch
Add-Tag
