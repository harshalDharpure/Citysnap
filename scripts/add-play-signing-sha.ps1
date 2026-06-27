# Adds Google Play App Signing SHA-1 to Firebase (required for Google Sign-In on Play Store installs).
#
# Usage:
#   1. Play Console -> App integrity -> App signing -> App signing key certificate -> copy SHA-1
#   2. Run: .\scripts\add-play-signing-sha.ps1 -Sha1 "AA:BB:CC:..."
#
param(
    [Parameter(Mandatory = $true)]
    [string]$Sha1
)

$ErrorActionPreference = "Stop"
$Project = "thoughts-3aa0c"
$AppId = "1:32449532472:android:84cf37f33909660b014ccd"
$Normalized = ($Sha1 -replace ":", "").ToLower()

Write-Host "Adding Play App Signing SHA-1: $Normalized"
firebase apps:android:sha:create $AppId $Normalized --project $Project

$TempFile = "app/google-services.new.json"
firebase apps:sdkconfig ANDROID $AppId --out $TempFile --project $Project
Move-Item -Force $TempFile "app/google-services.json"
Write-Host "Updated app/google-services.json. Rebuild and upload a new release bundle."
