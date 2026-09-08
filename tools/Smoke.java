import rollnote.core.*;;
import java.nio.file.*;

public class Smoke {
    public static void main(String[] a) throws Exception {
        byte[] in = Files.readAllBytes(Path.of(a[0]));
        Song s = Smf.read(in);
        System.out.println("fmt=" + s.formatIn + " ppq=" + s.division + " tempo=" + s.tempoMicros);
        System.out.println("notes=" + s.notes.size() + " ctrl=" + s.ctrls.size()
                + " metas=" + s.metas.size() + " end=" + s.endTime()
                + " tracks=" + s.trackCount() + " name=" + s.sourceName);
        int min = 999, max = -1;
        long minT = Long.MAX_VALUE, maxT = 0;
        for (Note n : s.notes) {
            min = Math.min(min, n.note); max = Math.max(max, n.note);
            minT = Math.min(minT, n.start); maxT = Math.max(maxT, n.end());
        }
        System.out.println("pitch " + min + ".." + max + "  time " + minT + ".." + maxT);
        for (MetaEvent m : s.metas) {
            if (m.type == 0x51) System.out.println("tempo meta at " + m.time + " -> " + m.text());
            if (m.type == 0x58) System.out.println("timesig meta at " + m.time + " -> " + m.text());
        }
        byte[] out = Smf.write(s, 1, false, false);
        Files.write(Path.of(a[1]), out);
        Song s2 = Smf.read(out);
        System.out.println("roundtrip: notes=" + s2.notes.size() + " ctrl=" + s2.ctrls.size()
                + " metas=" + s2.metas.size() + " end=" + s2.endTime());
        boolean ok = s.notes.size() == s2.notes.size() && s.endTime() == s2.endTime();
        long t0 = 0, d0 = 0;
        int i = 0;
        for (Note n : s.notes) for (Note n2 : s2.notes) if (n2.start == n.start && n2.note == n.note) { t0 += n2.start; d0 += n2.dur; i++; break; }
        System.out.println("matched=" + i + "/" + s.notes.size() + " all=" + ok);
    }
}
