package rollnote.core;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/** Variable-length meta event (text, tempo, time signature, lyrics, ...). */
public final class MetaEvent {
    public int track;
    public long time;
    public int type;      // 0x00..0x7F meta, or 0xF0/0xF7 for sysex
    public byte[] data;

    public MetaEvent(int track, long time, int type, byte[] data) {
        this.track = track;
        this.time = time;
        this.type = type;
        this.data = data;
    }

    public String typeName() {
        switch (type) {
            case 0x00: return "Seq number";
            case 0x01: return "Text";
            case 0x02: return "Copyright";
            case 0x03: return "Track name";
            case 0x04: return "Instrument";
            case 0x05: return "Lyric";
            case 0x06: return "Marker";
            case 0x07: return "Cue point";
            case 0x20: return "Channel prefix";
            case 0x2F: return "End of track";
            case 0x51: return "Tempo";
            case 0x54: return "SMPTE offset";
            case 0x58: return "Time signature";
            case 0x59: return "Key signature";
            case 0x7F: return "Sequencer specific";
            case 0xF0: return "Sysex";
            case 0xF7: return "Sysex cont.";
            default:   return String.format("Meta %02X", type);
        }
    }

    public String text() {
        switch (type) {
            case 0x51:
                if (data.length >= 3) {
                    long us = ((data[0] & 0xFFL) << 16) | ((data[1] & 0xFFL) << 8) | (data[2] & 0xFFL);
                    return String.format("%.1f bpm", 60_000_000.0 / us);
                }
                return "";
            case 0x58:
                if (data.length >= 4) {
                    int denom = 1 << (data[1] & 0xFF);
                    return String.format("%d/%d", data[0] & 0xFF, denom);
                }
                return "";
            case 0x59:
                if (data.length >= 2) {
                    int key = data[0];
                    String n = NoteName.name(((key % 12) + 12) % 12 + 12);
                    return n + (data[1] == 0 ? " major" : " minor");
                }
                return "";
            default:
                return new String(data, StandardCharsets.ISO_8859_1);
        }
    }

    public String display() {
        if (type == 0xF0 || type == 0xF7)
            return "Sysex (" + data.length + " bytes)";
        String t = text();
        String name = typeName();
        if (t.length() == 0 || t.equals(name)) return name;
        String compact = t.replace('\n', ' ').trim();
        if (compact.length() > 40) compact = compact.substring(0, 40) + "...";
        return name + ": " + compact;
    }

    public MetaEvent copy() {
        return new MetaEvent(track, time, type, data.clone());
    }

    @Override
    public String toString() {
        return "Meta@" + time + " tr" + track + " " + display();
    }
}
