# RollNote — Development

Architecture, build, packaging and release notes for maintainers.
For end users see [USER_GUIDE.md](USER_GUIDE.md).

## What this is

RollNote is a clean-room **Java 11+ / Swing** re-implementation of the design of
**ROLLOOK**, the paper-roll MIDI editor by Johan Liljencrants (2002). The
original's `rollook.doc` manual and its embedded screenshots plus behavioral
probing of the original (it runs under Wine) served as the specification. No
original Pascal source or binary is used; see [LICENSE](../LICENSE).

The app intentionally ships a **single main window** while keeping the
original's layout (channel row on top, controls in the left column, roll +
histogram + keyboard on the right) — the original's multiple windows (event
lists, chord analysis, note clipboard, meta dialogs) are folded into dialogs.

## Design

```
src/rollnote/
├── Main.java            entry point; optional file arg, rollook.test/rollook.debug flags
├── core/                no Swing. Pure model + SMF codec.
│   ├── Song.java        model: division, tempo, notes, ctrl/meta events
│   ├── Note.java        note record (pitch, vel, start, dur, channel, track, selected, enabled)
│   ├── CtrlEvent.java   non-note channel event (program, controller, aftertouch, bend)
│   ├── MetaEvent.java   meta/sysex event with display helpers
│   ├── Smf.java         own SMF format 0/1 read+write; note on/off pairing; format
│   │                    conversions (0/1 x keep/zero channel, track=channel)
│   └── NoteName.java    pitch numbering helpers (C0=12)
└── ui/                  Swing
    ├── MainWindow.java  frame, menus, left rail, channel matrix, storage ops,
    │                    clipboard (cut/copy/paste), undo stack, save-as dialog
    ├── RollView.java    the painter + all mouse gestures (select, rect, Ctrl-XOR,
    │                    Shift-drag, move/duration, pen insert, marker, play line,
    │                    hover chip/lane highlight, histogram & keyboard)
    ├── Player.java      javax.sound.midi playback: slice from marker, enabled
    │                    channels only, earlier events forced to time 0; Gervill
    │                    synth is connected explicitly (no receiver bug!)
    ├── Dialogs.java     event lists + editors, save-as options, percussion/GM pickers
    ├── Gm.java          General MIDI program + percussion tables
    └── NoteName2.java   pitch numbering helpers (C-1=0) — dup of core for UI use
```

Key behaviors worth keeping an eye on when extending:

- **States**: enabled (black) / selected (red) / disabled (gray) per note.
  The top channel matrix cycles on→selected→off.
- **Disabled notes are filtered out of Save + Play** (documented behavior).
- **Undo** is a snapshot stack (80 entries); selections are not undoable.
- Clipboard keeps the original ±2900-note warning semantics.
- The 16-bit original's quirks: resolution quantization, min duration = T res,
  pen cannot stack on existing notes, drag duration is absolute.

## Build

Plain `javac`; no build system needed.

```
./build.sh                               # compile -> out/
./run.sh samples/G2.MID                  # run (or: java -cp out rollnote.Main file.mid)
```

Requires JDK 11+ to run. Set `-Drollook.debug=true` to see operation logging.

## Packaging (no-install app images)

`jpackage` + `jlink` (JDK 17+):

```
# Linux:  needs JAVA_HOME (e.g. JDK 17 at ~/.local/opt/temurin17)
JAVA_HOME=~/.local/opt/temurin17 ./build_dist.sh     # -> dist/RollNote-linux.zip

# Windows:
powershell -ExecutionPolicy Bypass -File build_dist.ps1  # -> dist/RollNote-windows.zip
```

Each zip contains a trimmed Java runtime (`java.base,java.desktop`) + the app:
unzip anywhere, double-click `RollNote.exe` — no install, no JRE needed.

## Releases (GitHub Actions)

`.github/workflows/release.yml` runs on every `v*` tag (and manually via
"workflow_dispatch"). It builds on `windows-latest` and `ubuntu-latest`:
checkout → setup-java (Temurin 17) → compile → jar → jpackage app-image → zip →
upload artifact → attach to GitHub Release (`softprops/action-gh-release`).

To publish: `git tag v1.0.1 && git push origin main --tags`.

Troubleshooting notes from history:
- `setup-java@v4` deprecated → use `v5`; `cache: none` is rejected by new
  runners → drop the key.
- Always create `dist/` before `Compress-Archive`.
- Tests (Robot driver tools in `tools/`) require a real X display `:1`.

## Testing

`tools/` contains CLI tests (`Smoke`, `Conv` — codec round-trip and the four
save-format options) and X11 Robot drivers for UI smoke tests
(`Drive`, `Sweep`, `LayoutDrive`, `Snap`). They are kept as references; the CI
only does a compile + packaging build, not UI tests.

## Sample data

`samples/` and non-source assets are intentionally **not** in the repo:
`G2.MID` ("Wallace & Gromit" theme) and `rollook_capture.mp4` are copyrighted
and excluded via `.gitignore`.

## Inspirations & credits

- Johan Liljencrants — ROLLOOK (2002), design reference
- Julian Nott — G2.MID example
