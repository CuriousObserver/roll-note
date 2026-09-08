#!/usr/bin/env bash
cd "$(dirname "$0")"
exec java -cp out rollnote.Main "$@"
