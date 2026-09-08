package rollnote.ui;

/** MIDI note number helpers (Rollook numbering: C-1 = 0, A440 = 69). */
public final class NoteName2 {
    private static final String[] NAMES = {"C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B"};
    private NoteName2() {}
    public static String name(int note) {
        int nn = ((note % 12) + 12) % 12;
        int oct = note / 12 - 1;
        return NAMES[nn] + oct;
    }
}
