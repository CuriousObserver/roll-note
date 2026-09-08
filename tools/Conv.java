import rollnote.core.*;;
import java.nio.file.*;
import java.util.*;

public class Conv {
    public static void main(String[] a) throws Exception {
        Song s = Smf.read(Files.readAllBytes(Path.of(a[0])));
        int[][] opts = {{0,0,0},{0,1,0},{1,0,0},{1,0,1}};
        String[] names = {"f0 keep","f0 zero","f1 keep","f1 byChan"};
        for (int i = 0; i < opts.length; i++) {
            byte[] b = Smf.write(s, opts[i][0], opts[i][1] == 1, opts[i][2] == 1);
            Files.write(Path.of("/tmp/opencode/conv" + i + ".mid"), b);
            Song s2 = Smf.read(b);
            System.out.printf("%-9s -> %5d bytes, read back: %d notes, %d tracks, ctrl %d%n",
                names[i], b.length, s2.notes.size(), s2.trackCount(), s2.ctrls.size());
        }
        // transpose sanity: midi numbers stay in range
        byte[] f1 = Smf.write(s, 1, false, false);
        Song x = Smf.read(f1);
        for (Note n : x.notes) { if (n.note < 0 || n.note > 127 || n.start < 0 || n.dur <= 0) throw new Error("bad " + n); }
        System.out.println("note ranges ok");
    }
}
