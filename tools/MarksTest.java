import rollnote.core.NoteMarks;

import java.nio.file.Files;
import java.nio.file.Path;

/** Round-trip check for the original rollnot.txt note-marks format. */
public class MarksTest {
    static int fails = 0;

    public static void main(String[] a) throws Exception {
        NoteMarks m = new NoteMarks();
        m.color[0] = NoteMarks.BLACK;
        m.color[12] = NoteMarks.RED;
        m.color[60] = NoteMarks.BLUE;
        m.line[60] = 2;                     // preserved, not edited
        Path tmp = Files.createTempFile("rollnot", ".txt");
        m.save(tmp);

        NoteMarks r = new NoteMarks();
        check("file loads", r.load(tmp));
        check("colour preserved", r.color[12] == NoteMarks.RED && r.color[60] == NoteMarks.BLUE);
        check("line state preserved", r.line[60] == 2);
        check("unset stays none", r.color[30] == NoteMarks.NONE);

        // cycle wrap: none -> black -> red -> green -> blue -> none
        NoteMarks c = new NoteMarks();
        boolean ok = true;
        for (int i = 1; i <= 5; i++) {
            c.cycleColor(7);
            ok &= c.color[7] == (i % 5);
        }
        check("cycle wraps through all colours", ok);

        if (a.length > 0) {                 // optional: check a real file
            NoteMarks real = new NoteMarks();
            check("external file loads", real.load(Path.of(a[0])));
            int colours = 0;
            for (byte b : real.color) if (b != NoteMarks.NONE) colours++;
            System.out.println("external file: " + colours + " coloured notes");
        }
        Files.deleteIfExists(tmp);

        System.out.println(fails == 0 ? "MARKS CHECKS PASSED" : fails + " MARKS CHECKS FAILED");
        System.exit(fails == 0 ? 0 : 1);
    }

    static void check(String name, boolean ok) {
        System.out.println((ok ? "PASS " : "FAIL ") + name);
        if (!ok) fails++;
    }
}
