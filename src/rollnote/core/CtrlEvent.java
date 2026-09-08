package rollnote.core;

/**
 * A channel voice event other than note on/off (program change, control change,
 * aftertouch, pitch bend). Kept time-sorted like the original's "control events".
 */
public final class CtrlEvent {
    public int track;
    public int channel;   // 0..15
    public long time;     // ticks
    public int status;    // full status byte 0xB0..0xEF
    public int d1;        // data byte 1 (program# / controller#)
    public int d2;        // data byte 2 (value / -1 if none)

    public CtrlEvent(int track, int channel, long time, int status, int d1, int d2) {
        this.track = track;
        this.channel = channel;
        this.time = time;
        this.status = status;
        this.d1 = d1;
        this.d2 = d2;
    }

    /** one letter code, as in the original manual (e.g. C = program change) */
    public char code() {
        switch (status & 0xF0) {
            case 0xB0: return 'B';
            case 0xC0: return 'C';
            case 0xD0: return 'D';
            case 0xE0: return 'E';
            case 0xA0: return 'A';
            default:   return '?';
        }
    }

    public String describe() {
        return String.format("%c %d %d", code(), d1, d2 >= 0 ? d2 : 0);
    }

    public CtrlEvent copy() {
        return new CtrlEvent(track, channel, time, status, d1, d2);
    }

    @Override
    public String toString() {
        return String.format("Ctrl%c ch%d tr%d @%d (%d %d)", code(), channel, track, time, d1, d2);
    }
}
