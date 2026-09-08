import rollnote.core.Note;
import rollnote.core.Song;
import rollnote.ui.RollView;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import java.awt.event.MouseEvent;
import java.util.concurrent.atomic.AtomicInteger;

/** EDT-level logic test for the mouse gestures (no Robot needed). */
public class ViewLogicTest {
    static int fails = 0;
    static final double PPT = 34.0 / 240.0;

    public static void main(String[] a) throws Exception {
        SwingUtilities.invokeAndWait(ViewLogicTest::run);
        System.out.println(fails == 0 ? "ALL CHECKS PASSED" : fails + " CHECKS FAILED");
        System.exit(fails == 0 ? 0 : 1);
    }

    static void run() {
        Song s = new Song();
        s.division = 240;
        AtomicInteger changes = new AtomicInteger();
        RollView v = new RollView(s, new RollView.Listener() {
            @Override public void pushUndo() {}
            @Override public void changed() { changes.incrementAndGet(); }
            @Override public void hover(String st) {}
            @Override public void pointerHover(int note, long tick) {}
            @Override public void markerChanged(long tick) {}
        });
        v.setInsertDefaults(0, 0, 100, 120);
        v.setQuantRes(30);
        JFrame f = new JFrame();
        f.add(v);
        f.setSize(900, 820);
        f.setVisible(true);

        // 1) pen insert
        v.setPenMode(true);
        press(v, px(60), py(2400));
        drag(v, px(60), py(2520));
        release(v, px(60), py(2520));
        v.setPenMode(false);
        check("pen inserted one note", s.notes.size() == 1);
        Note n = s.notes.get(0);
        check("pen note pitch", n.note == 60);
        check("pen note start on grid", n.start == 2400);
        check("pen note duration dragged", n.dur == 120);
        check("pen note selected", n.selected);

        // 2) move drag: upper half, +7 semitones, +120 ticks
        press(v, px(60), py(2400) + 1);
        drag(v, px(67), py(2520));
        release(v, px(67), py(2520));
        check("moved pitch +7", n.note == 67);
        check("moved start +120 on grid", n.start == 2520);
        check("moved dur unchanged", n.dur == 120);

        // 3) DUR drag (lower half). Down, part-way back, fully back:
        //    every step must derive from the SNAPSHOT (base dur), never compound.
        int durPressY = py(2600);
        long baseDur = 120;
        press(v, px(67), durPressY);
        long pressQ = q(ty(durPressY));
        drag(v, px(67), py(2720));
        long dt1 = q(ty(py(2720))) - pressQ;
        check("dur down applied", n.dur == baseDur + dt1 && n.dur > baseDur);
        long afterFirst = n.dur;
        drag(v, px(67), py(2660));
        long dt2 = q(ty(py(2660))) - pressQ;
        check("dur snapshot (no compounding)", n.dur == baseDur + dt2 && n.dur < afterFirst);
        drag(v, px(67), durPressY);
        release(v, px(67), durPressY);
        check("dur back to base", n.dur == baseDur);

        // 4) rectangle select (note now spans ticks 2520..2640)
        press(v, px(55), py(2500));
        drag(v, px(75), py(2700));
        release(v, px(75), py(2700));
        check("rect selects note", n.selected);

        f.dispose();
    }

    static int px(int note) { return RollView.MARGIN + note * RollView.CELL + 2; }
    static int py(long tick) { return RollView.TOP_H + (int) (tick * PPT); }

    static long q(long t) { return Math.max(0, ((t + 15) / 30) * 30); }
    static long ty(int y) { return (long) Math.floor(Math.max(0, y - RollView.TOP_H) / PPT); }

    static void press(RollView v, int x, int y) {
        v.dispatchEvent(new MouseEvent(v, MouseEvent.MOUSE_PRESSED, System.currentTimeMillis(), 0, x, y, 1, false));
    }
    static void drag(RollView v, int x, int y) {
        v.dispatchEvent(new MouseEvent(v, MouseEvent.MOUSE_DRAGGED, System.currentTimeMillis(), 0, x, y, 1, false));
    }
    static void release(RollView v, int x, int y) {
        v.dispatchEvent(new MouseEvent(v, MouseEvent.MOUSE_RELEASED, System.currentTimeMillis(), 0, x, y, 1, false));
    }

    static void check(String name, boolean ok) {
        System.out.println((ok ? "PASS " : "FAIL ") + name);
        if (!ok) fails++;
    }
}
