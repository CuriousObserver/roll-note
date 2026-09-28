#!/usr/bin/env bash
set -e
cd "$(dirname "$0")"
mkdir -p out
javac -encoding UTF-8 -d out $(find src -name '*.java')
cp src/rollnote/help.txt out/rollnote/help.txt
echo "build ok -> out/"
