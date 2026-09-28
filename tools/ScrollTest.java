import rollnote.core.Note;
import rollnote.core.Song;
import rollnote.ui.RollView;

import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

/** Deterministic check of the conditional viewport-height tracking. */
public class ScrollTest {
    static int fails = 0;

    public static void main(String[] a) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            // empty song -> must track the viewport height (paper fills the window)
            RollView empty = new RollView(new Song(), new RollView.Listener() {
                @Override public void pushUndo() {}
                @Override public void changed() {}
                @Override public void hover(String s) {}
                @Override public void pointerHover(int n, long t) {}
                @Override public void markerChanged(long t) {}
                @Override public void noteMarkClicked(int n) {}
            });
            JFrame f1 = new JFrame();
            JScrollPane sp1 = new JScrollPane(empty);
            f1.add(sp1);
            f1.setSize(1000, 700);
            f1.setVisible(true);
            check("empty song tracks viewport height", empty.getScrollableTracksViewportHeight());

            // long song -> must scroll (content taller than viewport)
            Song longSong = new Song();
            longSong.division = 240;
            for (int i = 0; i < 500; i++)
                longSong.notes.add(new Note(60, 100, i * 480L, 240, 0, 0));
            RollView longv = new RollView(longSong, new RollView.Listener() {
                @Override public void pushUndo() {}
                @Override public void changed() {}
                @Override public void hover(String s) {}
                @Override public void pointerHover(int n, long t) {}
                @Override public void markerChanged(long t) {}
                @Override public void noteMarkClicked(int n) {}
            });
            JFrame f2 = new JFrame();
            JScrollPane sp2 = new JScrollPane(longv);
            f2.add(sp2);
            f2.setSize(1000, 700);
            f2.setVisible(true);
            check("long song does NOT track viewport height", !longv.getScrollableTracksViewportHeight());
            check("long song view taller than viewport",
                    longv.getPreferredSize().height > sp2.getViewport().getHeight());

            f1.dispose();
            f2.dispose();
            System.out.println(fails == 0 ? "SCROLL CHECKS PASSED" : fails + " SCROLL CHECKS FAILED");
            System.exit(fails == 0 ? 0 : 1);
        });
    }

    static void check(String name, boolean ok) {
        System.out.println((ok ? "PASS " : "FAIL ") + name);
        if (!ok) fails++;
    }
}
