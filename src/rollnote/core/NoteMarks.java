package rollnote.core;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Per-note help markers in the original Rollook "rollnot.txt" format:
 * 128 lines of "note colour line", where colour is 0..4 =
 * none/black/red/green/blue and line is 0..2 = none/on notes/between notes.
 *
 * RollNote currently edits and displays only the coloured tabs; the
 * per-note line states are preserved verbatim so files keep working in
 * both programs.
 */
public final class NoteMarks {
    public static final int NONE = 0, BLACK = 1, RED = 2, GREEN = 3, BLUE = 4;
    public static final String[] COLOR_NAMES = {"none", "black", "red", "green", "blue"};

    public final byte[] color = new byte[128];
    public final byte[] line = new byte[128];    // preserved, not edited (yet)

    /** cycle a tab colour: none -> black -> red -> green -> blue -> none */
    public void cycleColor(int note) {
        if (note < 0 || note > 127) return;
        color[note] = (byte) ((color[note] + 1) % 5);
    }

    /** load the file; returns false when it cannot be read at all */
    public boolean load(Path p) {
        try {
            java.util.List<String> rows = Files.readAllLines(p, StandardCharsets.ISO_8859_1);
            for (int i = 0; i < 128; i++) { color[i] = NONE; line[i] = 0; }
            for (String row : rows) {
                String t = row.trim();
                if (t.isEmpty()) continue;
                String[] f = t.split("\\s+");
                if (f.length < 2) continue;
                try {
                    int n = Integer.parseInt(f[0]);
                    int c = Integer.parseInt(f[1]);
                    int l = f.length >= 3 ? Integer.parseInt(f[2]) : 0;
                    if (n < 0 || n > 127) continue;
                    color[n] = (byte) Math.max(NONE, Math.min(BLUE, c));
                    line[n] = (byte) Math.max(0, Math.min(2, l));
                } catch (NumberFormatException ignored) {
                    // tolerate stray lines
                }
            }
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    /** write the original format (CRLF line endings, like the original files) */
    public void save(Path p) throws IOException {
        StringBuilder sb = new StringBuilder(4096);
        for (int n = 0; n < 128; n++)
            sb.append(n).append(' ').append(color[n]).append(' ').append(line[n]).append("\r\n");
        Files.writeString(p, sb.toString(), StandardCharsets.ISO_8859_1);
    }

    /** the original program keeps this file in the home directory */
    public static Path defaultPath() {
        return Path.of(System.getProperty("user.home", "."), "rollnot.txt");
    }

    /** search the working directory, then the home directory, either case */
    public static Path find() {
        for (Path dir : new Path[]{Path.of(""), Path.of(System.getProperty("user.home", "."))}) {
            for (String name : new String[]{"rollnot.txt", "ROLLNOT.TXT"}) {
                Path p = dir.resolve(name);
                if (Files.isReadable(p)) return p.toAbsolutePath();
            }
        }
        return null;
    }
}
