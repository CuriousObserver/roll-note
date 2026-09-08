# Build the self-contained "no install" RollNote zip on Windows.
# Requirements: Windows 10/11, JDK 17 or newer on PATH (java -version).
# Run:  powershell -ExecutionPolicy Bypass -File build_dist.ps1
#
# Output: dist\RollNote-windows.zip
#   -> unzip anywhere, double-click  RollNote\RollNote.exe   (nothing to install)

$ErrorActionPreference = "Stop"
Set-Location $PSScriptRoot

Write-Host "Compiling..."
javac -encoding UTF-8 -d out (Get-ChildItem src -Recurse -Filter *.java | ForEach-Object { $_.FullName })

Write-Host "Building jar..."
Remove-Item -Recurse -Force appinput -ErrorAction SilentlyContinue
New-Item -ItemType Directory -Path appinput | Out-Null
jar cfe appinput/RollNote.jar rollnote.Main -C out rollnote

Write-Host "Packaging app image..."
Remove-Item -Recurse -Force build -ErrorAction SilentlyContinue
jpackage `
  --type app-image `
  --name RollNote `
  --app-version 1.0 `
  --vendor "RollNote" `
  --input appinput `
  --main-jar RollNote.jar `
  --main-class rollnote.Main `
  --add-modules java.base,java.desktop `
  --dest build/dist

Write-Host "Zipping..."
Remove-Item -Recurse -Force dist -ErrorAction SilentlyContinue
New-Item -ItemType Directory -Path dist | Out-Null
Compress-Archive -Force -Path build/dist/RollNote -DestinationPath dist/RollNote-windows.zip
Write-Host "DONE: dist\RollNote-windows.zip"
Write-Host "  -> unzip, double-click  RollNote\RollNote.exe"
