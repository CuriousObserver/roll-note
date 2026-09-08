package rollnote.core;

import java.io.ByteArrayOutputStream;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Self contained Standard MIDI File (format 0 and 1) reader/writer.
 * Note on/off pairs are fused into Note records; all times are absolute ticks.
 */
public final class Smf {

    private Smf() {}

    public static final class FormatException extends Exception {
        private static final long serialVersionUID = 1L;
        public FormatException(String m) { super(m); }
    }

    // ---------------------------------------------------------------- read

    public static Song read(byte[] b) throws FormatException {
        R r = new R(b);
        if (r.tag(4) != 0x4D546864) throw new FormatException("not an MThd file");
        long hlen = r.u32();
        if (hlen < 6) throw new FormatException("bad MThd header length " + hlen);
        int format = r.u16();
        int ntrks = r.u16();
        int division = r.u16();
        if ((division & 0x8000) != 0) throw new FormatException("SMPTE division not supported");
        if (format != 0 && format != 1) throw new FormatException("format " + format + " not supported");
        r.skip(hlen - 6);

        Song s = new Song();
        s.division = division == 0 ? 480 : division;
        s.formatIn = format;
        ArrayList<Note> notes = new ArrayList<>();
        ArrayList<CtrlEvent> ctrls = new ArrayList<>();
        ArrayList<MetaEvent> metas = new ArrayList<>();

        for (int t = 0; t < ntrks; t++) {
            if (r.tag(4) != 0x4D54726B)
                throw new FormatException("expected MTrk at " + r.pos + " track " + t + " of " + ntrks);
            long len = r.u32();
            long endPos = r.pos + len;
            long time = 0;
            int running = 0;
            Map<Long, ArrayDeque<long[]>> pending = new HashMap<>(); // key -> [start, velocity]
            long trackEnd = 0;
            boolean tempoSeen = false;
            while (r.pos < endPos) {
                time += r.vlq();
                int st = r.u8();
                if ((st & 0x80) == 0) {              // running status
                    r.pos--;
                    if (running == 0) throw new FormatException("running status with none");
                    st = running;
                } else {
                    running = st;
                }
                if (st == 0xFF) {                     // meta event
                    int type = r.u8();
                    int mlen = (int) r.vlq();
                    byte[] data = r.bytes(mlen);
                    if (type == 0x51 && data.length >= 3 && !tempoSeen) {
                        tempoSeen = true;
                        s.tempoMicros = ((data[0] & 0xFFL) << 16) | ((data[1] & 0xFFL) << 8) | (data[2] & 0xFFL);
                    }
                    metas.add(new MetaEvent(t, time, type, data));
                    trackEnd = time;
                } else if (st == 0xF0 || st == 0xF7) {  // sysex kept like a meta record
                    running = 0;                        // sysex cancels running status (SMF spec)
                    int slen = (int) r.vlq();
                    byte[] data = r.bytes(slen);
                    metas.add(new MetaEvent(t, time, st, data));
                    trackEnd = time;
                } else {
                    int hi = st & 0xF0, chan = st & 0x0F;
                    if (hi == 0xC0 || hi == 0xD0) {   // one data byte
                        int d1 = r.u8();
                        ctrls.add(new CtrlEvent(t, chan, time, st, d1, -1));
                        trackEnd = time;
                    } else {                          // two data bytes
                        int d1 = r.u8(), d2 = r.u8();
                        if (hi == 0x90 && d2 > 0) {
                            pending.computeIfAbsent(key(chan, d1), k -> new ArrayDeque<>()).add(new long[]{time, d2});
                        } else if (hi == 0x90 || hi == 0x80) {
                            ArrayDeque<long[]> q = pending.get(key(chan, d1));
                            if (q != null && !q.isEmpty()) {
                                long[] pv = q.poll();
                                notes.add(new Note(d1, (int) pv[1], pv[0], Math.max(0, time - pv[0]), chan, t));
                            }
                        } else {
                            ctrls.add(new CtrlEvent(t, chan, time, st, d1, d2));
                        }
                        trackEnd = time;
                    }
                }
            }
            for (Map.Entry<Long, ArrayDeque<long[]>> e : pending.entrySet()) {
                int chan = (int) (e.getKey() >> 7), note = (int) (e.getKey() & 0x7F);
                for (long[] pv : e.getValue()) {
                    long end = Math.max(pv[0], trackEnd);
                    notes.add(new Note(note, (int) pv[1], pv[0], end - pv[0], chan, t));
                }
            }
        }

        s.notes = notes;
        s.ctrls = ctrls;
        s.metas = metas;
        for (MetaEvent m : metas)
            if (m.type == 0x03) { s.sourceName = m.text(); break; }
        return s;
    }

    private static long key(int chan, int note) { return ((long) chan << 7) | note; }

    // ---------------------------------------------------------------- write

    /**
     * @param format        0 or 1
     * @param zeroChannel   force every channel number to 0
     * @param trackIsChannel format 1 only: put each channel on its own track
     */
    public static byte[] write(Song s, int format, boolean zeroChannel, boolean trackIsChannel) {
        int ntrks;
        if (format == 0) ntrks = 1;
        else if (trackIsChannel) ntrks = 16;
        else ntrks = Math.max(1, s.trackCount());

        List<List<Ev>> trkEvents = new ArrayList<>();
        for (int i = 0; i < ntrks; i++) trkEvents.add(new ArrayList<>());

        boolean[] chanUsed = new boolean[16];
        for (Note n : s.notes) if (n.enabled) chanUsed[n.channel] = true;

        for (Note n : s.notes) {
            if (!n.enabled) continue;                 // disabled notes are not written
            int chan = zeroChannel ? 0 : n.channel;
            int trk = format == 0 ? 0 : (trackIsChannel ? chan : Math.min(n.track, ntrks - 1));
            trkEvents.get(trk).add(new Ev(n.start, 4, chan, 0x90, n.note, n.velocity));
            trkEvents.get(trk).add(new Ev(n.start + n.dur, 3, chan, 0x80, n.note, 0));
        }
        for (CtrlEvent c : s.ctrls) {
            if (!chanUsed[c.channel]) continue;
            int chan = zeroChannel ? 0 : c.channel;
            int trk = format == 0 ? 0 : (trackIsChannel ? chan : Math.min(c.track, ntrks - 1));
            int hi = c.status & 0xF0;
            if (hi == 0xC0 || hi == 0xD0)
                trkEvents.get(trk).add(new Ev(c.time, 2, chan, hi, c.d1, 0, false));
            else
                trkEvents.get(trk).add(new Ev(c.time, 2, chan, hi, c.d1, c.d2));
        }
        boolean keepMetaTracks = (format == 1) && !trackIsChannel;
        boolean nameKept = false;
        for (MetaEvent m : s.metas) {
            if (m.type == 0x2F) continue;
            if (m.type == 0x03) {
                if (keepMetaTracks) { /* keep per track */ }
                else if (!nameKept) { nameKept = true; }
                else continue;    // merging/splitting: only the first track name survives
            }
            int trk = (format == 1 && keepMetaTracks) ? Math.min(m.track, ntrks - 1) : 0;
            trkEvents.get(trk).add(new Ev(m.time, 1, m.type, m.data));
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        W w = new W(out);
        w.u32(0x4D546864); w.u32(6); w.u16(format); w.u16(ntrks); w.u16(s.division);
        for (int t = 0; t < ntrks; t++) {
            List<Ev> evs = trkEvents.get(t);
            evs.sort((a, b) -> a.time != b.time ? Long.compare(a.time, b.time)
                    : Integer.compare(a.pri, b.pri));
            ByteArrayOutputStream tb = new ByteArrayOutputStream();
            W tw = new W(tb);
            long prev = 0;
            for (Ev e : evs) {
                long d = e.time - prev;
                prev = e.time;
                tw.vlq(d);
                if (e.isMeta) {
                    tw.u8(0xFF); tw.u8(e.type); tw.vlq(e.data.length);
                    tw.raw(e.data);
                } else {
                    tw.u8(e.hi | e.chan);
                    tw.u8(e.d1);
                    if (e.hasD2) tw.u8(e.d2);
                }
            }
            tw.vlq(0);
            tw.u8(0xFF); tw.u8(0x2F); tw.vlq(0);
            byte[] tr = tb.toByteArray();
            w.u32(0x4D54726B); w.u32(tr.length); w.raw(tr);
        }
        return out.toByteArray();
    }

    // ---------------------------------------------------------------- internals

    private static final class Ev {
        long time; int pri;
        boolean isMeta; int type; byte[] data;
        int chan, hi, d1, d2; boolean hasD2;

        Ev(long time, int pri, int type, byte[] data) {           // meta
            this.time = time; this.pri = pri;
            this.isMeta = true; this.type = type; this.data = data;
        }

        Ev(long time, int pri, int chan, int hi, int d1, int d2) {   // 2-data-byte msg
            this.time = time; this.pri = pri;
            this.chan = chan; this.hi = hi; this.d1 = d1; this.d2 = d2;
            this.hasD2 = true;
        }

        Ev(long time, int pri, int chan, int hi, int d1, int d2, boolean hasD2) {
            this(time, pri, chan, hi, d1, d2);
            this.hasD2 = hasD2;
        }
    }

    private static final class R {
        final byte[] b; int pos;
        R(byte[] b) { this.b = b; }
        int u8() { return b[pos++] & 0xFF; }
        int u16() { return (u8() << 8) | u8(); }
        long u32() { long v = 0; for (int i = 0; i < 4; i++) v = (v << 8) | u8(); return v; }
        int tag(int n) {
            if (pos + n > b.length) return 0;
            int v = 0; for (int i = 0; i < n; i++) v = (v << 8) | u8();
            return v;
        }
        byte[] bytes(int n) { byte[] r = new byte[n]; System.arraycopy(b, pos, r, 0, n); pos += n; return r; }
        void skip(long n) { pos += (int) n; }
        long vlq() {
            long v = 0;
            for (int i = 0; i < 4; i++) {
                int x = u8();
                v = (v << 7) | (x & 0x7F);
                if ((x & 0x80) == 0) return v;
            }
            return v;
        }
    }

    private static final class W {
        final ByteArrayOutputStream o;
        W(ByteArrayOutputStream o) { this.o = o; }
        void u8(int v) { o.write(v & 0xFF); }
        void u16(int v) { u8(v >> 8); u8(v); }
        void u32(long v) { u8((int) (v >> 24)); u8((int) (v >> 16)); u8((int) (v >> 8)); u8((int) v); }
        void raw(byte[] d) { o.write(d, 0, d.length); }
        void vlq(long v) {
            long x = v;
            int[] tmp = new int[5];
            int i = 0;
            do { tmp[i++] = (int) (x & 0x7F); x >>= 7; } while (x > 0);
            while (i > 1) u8(tmp[--i] | 0x80);
            u8(tmp[0]);
        }
    }
}
