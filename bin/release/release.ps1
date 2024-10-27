# Find the first parent directory containing the VERSION file
$AppHome=(Get-Item -Path $MyInvocation.MyCommand.Path).Directory
while ($AppHome -ne $null -and !(Test-Path "$AppHome/VERSION")) {
  $AppHome=$AppHome.Parent
}
cd $AppHome

$gitExe = "git" # Assuming git is in the system PATH

# Get version information
$SNAPSHOT_VERSION = Get-Content "$AppHome\VERSION" -TotalCount 1
$VERSION =$SNAPSHOT_VERSION -replace "-SNAPSHOT", ""
$LAST_COMMIT_ID = &$gitExe log --format="%H" -n 1
$BRANCH = &$gitExe branch --show-current
$TAG = "v$VERSION"

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
Merge-ToMainBranch
Checkout-WorkingBranch
Add-Tag
