#!/usr/bin/env pwsh

# Find the latest tag matching the pattern 'v[0-9]+.[0-9]+.[0-9]+-ci.[0-9]+',
# increase the latest CI number by 1, and create a new tag with the incremented number.
# Add the new tag to the current commit and push it to the remote repository.

param(
    [string]$remote = "origin",
    [string]$pattern = "v[0-9]+.[0-9]+.[0-9]+-ci.[0-9]+"
)

# Get the latest tag matching the pattern
$latestTag = git tag --list $pattern | Sort-Object -Descending | Select-Object -First 1
if (-not $latestTag) {
    Write-Host "No tags found matching the pattern '$pattern'."
    exit 1
}
# Extract the CI number from the latest tag
$ciNumber = $latestTag -replace ".*-ci.", ""
# Increment the CI number
$newCiNumber = [int]$ciNumber + 1
# Create the new tag
$newTag = $latestTag -replace "-ci.$ciNumber", "-ci.$newCiNumber"
# Add the new tag to the current commit
git tag $newTag
# Push the new tag to the remote repository
git push $remote $newTag
Write-Host "Created new tag '$newTag' and pushed it to remote '$remote'."
# Output the new tag
Write-Output $newTag