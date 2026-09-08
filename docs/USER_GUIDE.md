# RollNote — User Guide

*For the person using the program. Written to be read aloud, no technical words needed.*

---

## What is it?

RollNote opens **MIDI music files** (`.mid`) and shows the music as a **piano
roll** — a long paper roll, like a player-piano roll:

- Time runs **downward** (like a waterfall).
- Music notes are the **black bars** on the roll.
- The roll is white paper with thin green lines, like the familiar original
  program — if you have used **Rollook** before, this feels exactly the same.

RollNote is a new, modern version of the old Rollook program by Johan
Liljencrants. Its look and how you work are the same, so it is easy to switch.

---

## Opening your music

1. Start the program: double-click **RollNote.exe** (or `bin/RollNote`).
2. Menu **File → Open…** at the very top left.
3. Choose your `.mid` file and press **Open**.

Your music appears on the roll. A file name and track note count show at the
bottom-left.

---

## The screen, top to bottom

| Where | What it is |
|-------|-----------|
| **File / Edit / Tools / Help** | The menus at the top |
| **All  0 1 2 3 4 5 6 7 8 9 A B C D E F** | The channel squares. **Click one** to check which voices are switched on, off, or selected (see below) |
| **Left column** | The working buttons: T res, P Percussions, Seq, Channel, Velocity, Duration %, Transpose, Speed, Play, Stop, Pen |
| **Top of the roll** | The black histogram (how many notes of each pitch) and the little keyboard |
| **The roll** | Your music. Notes are black bars; selected notes are red; switched-off notes are gray |
| **Right white strip** | The time scale. Click it to put the **red marker**; numbers show bar/beat/tick |
| **Bottom line** | Small status text |

### The three note colours

- **Black** — the note is on and can be selected.
- **Red** — the note is *selected* (picked for the next operation).
- **Gray** — the note is *switched off* (visible for reference, but ignored
  by Play, Save and editing).

---

## Choosing notes

You can pick notes in three easy ways:

1. **Click** a single note.
2. **Drag a rectangle** around a group of notes (press and hold, then move —
   wait until you see the blue box).
3. Click a **histogram bar** on top to choose one whole pitch.

While holding **Ctrl**, your click is *added* to (or removed from) what is
already picked.

To *remove* a note, click it again while holding **Ctrl**.

### Turning voices off and on (the channel squares)

Above the roll you see *All 0 1 2 … F*. These are the channels of the music.

- **In black squares** the voice is on.
- A **red square** means that voice is picked and will change next.
- **Gray square** means the voice is off — gray notes in the roll.

Clicking a square switches it: **on → picked → off → on**…

If you only want to work on one voice, switch the others off first so you
don't accidentally change them.

---

## Editing notes

These are the everyday jobs:

### Move a note
1. Click the note (turns red).
2. Press with the mouse on the **upper half** of it and drag.
   The note moves left/right in pitch and up/down in time.
3. Let go. (With **Shift** held, the whole group moves together.)

### Make a note longer / shorter
1. Click the note.
2. Press on the **lower half** and drag up or down. All picked notes change
   together.

### Add a new note (the Pen)
1. Click the **Pen** box (bottom-left of the left column).
2. Click on the roll where the note should start.
3. Drag downward to give it its length, or simply click.
4. Click **Pen** again to switch it off. (While the Pen is on, normal selecting
   is disabled — a click always makes a note.)

You cannot drop a new note on top of an existing one.

### Remove notes (Delete)
Pick the notes you want to remove, then press **Delete** (keyboard) or
menu **Edit → Cut**. Removed notes are set aside; **Ctrl+Z** brings them back.

### Copy and paste
1. Pick the notes.
2. **Edit → Copy**.
3. Click the **right white time strip** at the time where you want the copy —
   a red line marks the spot.
4. **Edit → Paste**.

---

## The left column buttons

Apply to all **selected** (red) notes. Set the number with the little arrows,
then press the button button.

| Button | What it does |
|--------|--------------|
| **Seq** (number) | Moves selected notes to another sequence/track |
| **Channel** (number) | Moves selected notes to another voice/channel (0–15) |
| **Velocity** (number) | Sets how loud they are (0–127) |
| **Duration %** + **Scale** | Makes notes longer or shorter (100 % = same) |
| **Transpose** (number) + **Apply** | Shifts them up/down in pitch (12 = one octave) |
| **P Percussions** | Shows the drum-name list (channel 9) |
| **T res** (1/1 … 1/64) | Time resolution: how fine the snapping/quantising is |
| **Speed** (1×, 1/2, 1/4) + **Play / Stop** | Listening |

### Listening from a spot
1. Click the **right white time strip** to put the red marker where you want
   to start.
2. Press **Play**. Only notes that are shown black. The roll follows the
   music with a green line.
3. Press **Stop** to stop.

If you hear nothing: check your computer's volume and speakers. RollNote uses
the General MIDI sound that Windows provides.

---

## Event list (for fine-tuning)

For work the mouse cannot do: menu **Tools → Event lists…** shows every event
in two lists. Double-click (or Edit… and Delete/Replace/Insert) to change a
single event exactly.

---

## Saving

- Menu **File → Save** — writes back into the same file.
- Menu **File → Save As…** — choose one of four ways of writing the file
  (keep or re-arrange tracks & channels). The window tells you what each
  option does.
- Notes switched off (gray) are **not** saved. You will be warned if this
  happens.

---

## A few tips

- The little **0**, **Zoom −**, **Zoom +** buttons (bottom of the left column)
  move to the start and make the roll bigger/smaller.
- The **H**ide note readout under the cursor speaks out loud: point at a note
  and read the little black bar — e.g. `C4  note 60 (0x3C)` — and the big blue
  text bottom-right.
- **Ctrl+Z** undoes almost anything. If you are not sure, press it first!

---

## Credit

RollNote is an independent, modern re-creation of **ROLLOOK** (2002) by
**Johan Liljencrants**. The example song (G2.MID) is "Wallace & Gromit" theme
by Julian Nott.
