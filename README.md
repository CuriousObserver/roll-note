# RollNote

A modern piano-roll style MIDI editor for Windows/Linux — **inspired by ROLLOOK**.

RollNote is a purpose-built re-implementation of [ROLLOOK], the classic
piano-roll MIDI editor written in 2002 by **Johan Liljencrants** for
Windows 3.1. We honour the original: same layout, same note/selection model,
same colours, so that experienced Rollook users can switch without any
learning curve. The original remains the reference and is credited in the
About box. RollNote is our own, all-new Java/Swing code — it does not contain
or reuse the original's binary or Pascal source.


The original program's manual (`rollook.doc` by Johan Liljencrants, 2002) and
its embedded screenshots were used as the specification; the original is
reachable via its author's historical releases.

## Features

- Own Standard MIDI File (format 0 and 1) reader/writer — notes arrive as
  fused on/off records
- Paper-roll display: white paper, green pitch lanes, time running downward,
  black enabled notes, red selected, gray disabled
- Black-bar note histogram + keyboard at the top, channel matrix row
  ("All 0..F") above it
- Mouse editing: click/rectangle/histogram selection, Ctrl XOR, Shift drag,
  upper-half move, lower-half duration, pen insertion
- Bulk ops: Seq, Channel, Velocity, Duration scale, Transpose
- Cut/copy/paste at the red marker, undo
- Event lists with edit dialogs, GM instrument + percussion pickers
- Playback through the built-in General MIDI synthesizer (no external
  soundfont needed) with 1×, 1/2×, 1/4× speed
- Pointer readout: note name/number chip, highlighted pitch lane, big status
  readout (the original has none of these — we added them for accessibility)

## Build from source

```
./build.sh                # compiles to out/ (JDK 11+)
./run.sh samples/G2.MID   # or: java -cp out rollnote.Main file.mid
```

## Self-contained distributions (no install, no JRE needed)

```
JAVA_HOME=~/.local/opt/temurin17 ./build_dist.sh     # Linux
powershell -ExecutionPolicy Bypass -File build_dist.ps1   # Windows
```

produces `dist/RollNote-<os>.zip`; unzip anywhere, double-click
`RollNote.exe` (`bin/RollNote` on Linux). A trimmed Java 17 runtime and the
app are bundled. **GitHub Actions** rebuilds these automatically on every
tag: see `.github/workflows/release.yml`.

## Licence

MIT — see [LICENSE](LICENSE). The original ROLLOOK (unlicensed, by Johan
Liljencrants) is not reproduced here; RollNote is clean-room inspired work.

## Credits

- Johan Liljencrants — ROLLOOK (2002), whose design RollNote follows
- Julian Nott — the G2.MID example ("Wallace & Gromit" theme)
