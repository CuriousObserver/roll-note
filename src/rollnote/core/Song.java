package rollnote.core;

import java.util.ArrayList;

/** The editable model of a song: all notes + non-note events. */
public final class Song {
    public int division = 480;      // ticks per quarter note
    public int formatIn = 1;        // original SMF format
    public long tempoMicros = 500_000L;
    public ArrayList<Note> notes = new ArrayList<>();
    public ArrayList<CtrlEvent> ctrls = new ArrayList<>();
    public ArrayList<MetaEvent> metas = new ArrayList<>();
    public String sourceName = "";

    public Song() {}

    public Song copy() {
        Song s = new Song();
        s.division = division;
        s.formatIn = formatIn;
        s.tempoMicros = tempoMicros;
        s.sourceName = sourceName;
        for (Note n : notes) s.notes.add(n.copy());
        for (CtrlEvent c : ctrls) s.ctrls.add(c.copy());
        for (MetaEvent m : metas) s.metas.add(m.copy());
        return s;
    }

    /** number of distinct tracks/sequences used */
    public int trackCount() {
        int t = 1;
        for (Note n : notes) t = Math.max(t, n.track + 1);
        for (CtrlEvent c : ctrls) t = Math.max(t, c.track + 1);
        for (MetaEvent m : metas) t = Math.max(t, m.track + 1);
        return t;
    }

    public long endTime() {
        long end = 0;
        for (Note n : notes) end = Math.max(end, n.end());
        for (CtrlEvent c : ctrls) end = Math.max(end, c.time);
        for (MetaEvent m : metas) if (m.type != 0x2F) end = Math.max(end, m.time);
        return end;
    }

    public long durationMicros() {
        double ppq = division;
        double usPerTick = tempoMicros / ppq;
        return Math.round(endTime() * usPerTick);
    }

    public int countEnabledNotes() {
        int c = 0;
        for (Note n : notes) if (n.enabled) c++;
        return c;
    }

    public int countSelectedNotes() {
        int c = 0;
        for (Note n : notes) if (n.selected) c++;
        return c;
    }

    /** clear selection but leave enabled state untouched */
    public void clearSelection() {
        for (Note n : notes) n.selected = false;
    }

    /** (re)enable every note */
    public void enableAll() {
        for (Note n : notes) n.enabled = true;
    }

    public String barBeatTick(long t) {
        long ticksPerBeat = division;
        long bar = t / (4 * ticksPerBeat);
        long rest = t % (4 * ticksPerBeat);
        long beat = rest / ticksPerBeat;
        long tick = rest % ticksPerBeat;
        return bar + ":" + beat + ":" + tick;
    }

    /** parse "bar:beat:tick" back to ticks (bars of 4 beats as in original) */
    public long parseBarBeatTick(String s) throws NumberFormatException {
        String[] p = s.trim().split(":");
        if (p.length != 3) throw new NumberFormatException(s);
        long bar = Long.parseLong(p[0]);
        long beat = Long.parseLong(p[1]);
        long tick = Long.parseLong(p[2]);
        return bar * 4 * division + beat * division + tick;
    }
}
