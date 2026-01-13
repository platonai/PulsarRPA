#!/usr/bin/env bash

echo "Fetching latest remote branches..."
git fetch origin

echo "Checking out main branch..."
git checkout main

echo "Resetting main branch to match origin/master..."
git reset --hard origin/master

echo "Force pushing main branch to remote..."
git push origin main --force

echo "Main branch is now fully synchronized with master."
