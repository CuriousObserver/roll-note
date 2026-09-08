#!/usr/bin/env bash
# Build a self-contained "no install" zip for the current platform.
#   Linux  -> dist/RollNote-linux.zip  (double-click bin/RollNote)
#   Windows (PowerShell with JDK 17+ on PATH) -> dist/RollNote-windows.zip
#           (double-click RollNote.exe)
#
# Requires a JDK with jpackage/jlink (>= 14): JAVA_HOME must point at it.
set -e
cd "$(dirname "$0")"

: "${JAVA_HOME:?Set JAVA_HOME to a JDK with jpackage, e.g. /home/jon/.local/opt/temurin17}"

JDK="$JAVA_HOME"
echo "using JDK: $JDK"

# --- compile (targets Java 11 source) ------------------------------------
if [ "$(uname)" = Linux ]; then
  ./build.sh
else
  javac -encoding UTF-8 -d out $(printf '%s ' $(find src -name '*.java'))
fi

# --- jar the application ------------------------------------------------
rm -rf appinput
mkdir -p appinput
jar cfe appinput/RollNote.jar rollnote.Main -C out rollnote

# --- self-contained app image -------------------------------------------
rm -rf build/image build/dist build/zip
"$JDK/bin/jpackage" \
  --type app-image \
  --name RollNote \
  --app-version 1.0 \
  --vendor "RollNote" \
  --input appinput \
  --main-jar RollNote.jar \
  --main-class rollnote.Main \
  --add-modules java.base,java.desktop \
  --dest build/dist

# --- zip it -------------------------------------------------------------
mkdir -p build/zip dist
cp -r build/dist/RollNote build/zip/

if [ "$(uname)" = Linux ]; then
  OUT=dist/RollNote-linux.zip
  rm -f "$OUT"
  (cd build/zip && zip -qr "../../$OUT" RollNote)
else
  OUT=dist/RollNote-windows.zip
  rm -f "$OUT"
  (cd build/zip && powershell -NoProfile -Command "Compress-Archive -Force -Path RollNote -DestinationPath \"../$OUT\"")
fi
echo "DONE: $OUT"
