package rollnote.core;

/** MIDI note number helpers (Rollook numbering: C0 = 12, A440 = 69). */
public final class NoteName {
    private static final String[] NAMES = {
        "C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B"
    };

    private NoteName() {}

    public static String name(int note) {
        return NAMES[((note % 12) + 12) % 12] + ((note / 12) - 1);
    }
}
