package rollnote.core;

/** A note: two MIDI events (note on/off) fused into one record. Times in ticks. */
public final class Note {
    public int note;      // 0..127
    public int velocity;  // 0..127
    public long start;    // ticks
    public long dur;      // ticks, >= 0
    public int channel;   // 0..15
    public int track;     // sequence number
    public boolean selected;
    public boolean enabled = true;

    public Note(int note, int velocity, long start, long dur, int channel, int track) {
        this.note = note;
        this.velocity = velocity;
        this.start = start;
        this.dur = dur;
        this.channel = channel;
        this.track = track;
    }

    public long end() { return start + dur; }

    public Note copy() {
        Note n = new Note(note, velocity, start, dur, channel, track);
        n.selected = selected;
        n.enabled = enabled;
        return n;
    }

    @Override
    public String toString() {
        return String.format("Note%02X(%s) ch%d tr%d %d+%d %s%s",
                note, NoteName.name(note), channel, track, start, dur,
                selected ? "SEL" : "", enabled ? "" : "DIS");
    }
}
