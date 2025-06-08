#!/usr/bin/env pwsh

# Set error handling
$ErrorActionPreference = "Stop"

# Function to write colored output
function Write-ColorOutput($ForegroundColor) {
    $fc = $host.UI.RawUI.ForegroundColor
    $host.UI.RawUI.ForegroundColor = $ForegroundColor
    if ($args) {
        Write-Output $args
    }
    $host.UI.RawUI.ForegroundColor = $fc
}

function Write-Info($msg) { Write-ColorOutput Cyan "ℹ️  $msg" }
function Write-Success($msg) { Write-ColorOutput Green "✅ $msg" }
function Write-Warning($msg) { Write-ColorOutput Yellow "⚠️  $msg" }
function Write-Error($msg) { Write-ColorOutput Red "❌ $msg" }

Write-Info "Starting release tagging process..."

# 1. Check if workspace is clean (no uncommitted changes)
Write-Info "Checking workspace status..."

try {
    # Check if we're in a git repository
    $gitStatus = git status --porcelain 2>&1

    if ($LASTEXITCODE -ne 0) {
        Write-Error "Not in a git repository or git command failed"
        exit 1
    }

    # Check for uncommitted changes
    if ($gitStatus) {
        Write-Error "Workspace has uncommitted changes:"
        Write-Output $gitStatus
        Write-Output ""
        Write-Warning "Please commit or stash your changes before creating a release tag."
        Write-Output "Commands to fix this:"
        Write-Output "  git add ."
        Write-Output "  git commit -m 'Your commit message'"
        Write-Output "  # OR"
        Write-Output "  git stash"
        exit 1
    }

    Write-Success "Workspace is clean - no uncommitted changes"

} catch {
    Write-Error "Failed to check git status: $_"
    exit 1
}

# 2. Extract version from pom.xml
Write-Info "Extracting version from pom.xml..."

try {
    # Check if pom.xml exists
    if (-not (Test-Path "pom.xml")) {
        Write-Error "pom.xml not found in current directory"
        exit 1
    }

    # Read and parse pom.xml
    [xml]$pomContent = Get-Content "pom.xml"

    # Get the version from the project element
    $version = $pomContent.project.version

    if (-not $version) {
        Write-Error "Could not extract version from pom.xml"
        Write-Info "Please ensure your pom.xml has a <version> element in the <project> section"
        exit 1
    }

    # Remove -SNAPSHOT suffix if present for tag
    $tagVersion = $version -replace "-SNAPSHOT$", ""
    $tagName = "v$tagVersion"

    Write-Success "Extracted version: $version"
    Write-Info "Tag name will be: $tagName"

} catch {
    Write-Error "Failed to parse pom.xml: $_"
    exit 1
}

# Confirm with user before proceeding
Write-Output ""
Write-Info "Summary:"
Write-Output "  Current version: $version"
Write-Output "  Tag to create: $tagName"
Write-Output "  Current branch: $(git branch --show-current)"
Write-Output "  Remote repository: $(git remote get-url origin)"
Write-Output ""

$confirmation = Read-Host "Do you want to proceed with creating and pushing the tag? (y/N)"
if ($confirmation -notmatch "^[yY]$") {
    Write-Warning "Operation cancelled by user"
    exit 0
}

# 3. Create the tag
Write-Info "Creating tag '$tagName'..."

try {
    # Check if tag already exists locally
    $existingTag = git tag -l $tagName
    if ($existingTag) {
        Write-Error "Tag '$tagName' already exists locally"
        Write-Info "To delete the existing tag, run: git tag -d $tagName"
        exit 1
    }

    # Create annotated tag with message
    $tagMessage = "Release version $tagVersion"
    git tag -a $tagName -m $tagMessage

    if ($LASTEXITCODE -ne 0) {
        Write-Error "Failed to create tag"
        exit 1
    }

    Write-Success "Created tag '$tagName' successfully"

} catch {
    Write-Error "Failed to create tag: $_"
    exit 1
}

# 4. Push the tag to remote repository
Write-Info "Pushing tag '$tagName' to remote repository..."

try {
    # Check if remote exists
    $remoteUrl = git remote get-url origin 2>&1
    if ($LASTEXITCODE -ne 0) {
        Write-Error "No remote 'origin' configured"
        exit 1
    }

    Write-Info "Pushing to: $remoteUrl"

    # Push the tag
    git push origin $tagName

    if ($LASTEXITCODE -ne 0) {
        Write-Error "Failed to push tag to remote repository"
        Write-Info "You can manually push the tag later with: git push origin $tagName"
        exit 1
    }

    Write-Success "Successfully pushed tag '$tagName' to remote repository"

} catch {
    Write-Error "Failed to push tag: $_"
    Write-Info "The tag was created locally but not pushed to remote"
    Write-Info "You can manually push it with: git push origin $tagName"
    exit 1
}

# Final summary
Write-Output ""
Write-Success "🎉 Release tagging completed successfully!"
Write-Output ""
Write-Info "Summary of actions performed:"
Write-Output "  ✅ Verified workspace is clean"
Write-Output "  ✅ Extracted version '$version' from pom.xml"
Write-Output "  ✅ Created tag '$tagName' with message '$tagMessage'"
Write-Output "  ✅ Pushed tag '$tagName' to remote repository"
Write-Output ""
Write-Info "Next steps:"
Write-Output "  • Check GitHub/GitLab for the new tag"
Write-Output "  • Trigger your CI/CD pipeline if configured"
Write-Output "  • Create a release from the tag if needed"
Write-Output "  • Deploy to Maven Central if this is a release version"
Write-Output ""
Write-Info "Tag information:"
git show $tagName --no-patch --format="  Commit: %H%n  Author: %an <%ae>%n  Date: %ad%n  Message: %s"
