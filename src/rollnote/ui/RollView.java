package rollnote.ui;

import rollnote.core.Note;
import rollnote.core.NoteMarks;
import rollnote.core.Song;

import javax.swing.JPanel;
import javax.swing.Scrollable;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;

/**
 * The main piano-roll style display, oriented like the original program:
 * pitch on the horizontal axis (bass at the left, like a paper roll), time
 * running downward. The top area holds the note histogram and the keyboard.
 * The white strip on the right is the time bar used to set the red marker.
 *
 * The pitch axis (keyboard, histogram, lanes) stretches with the window
 * width: the lane width is re-fitted on every resize within
 * {@link #MIN_CELL}..{@link #MAX_CELL} pixels per semitone.
 */
public class RollView extends JPanel implements Scrollable {
    private static final long serialVersionUID = 1L;
    // geometry
    public static final int MARGIN = 18;       // left margin
    public static final int HIST_H = 130;      // histogram bar area height
    public static final int RULER_W = 96;      // minimum right white time bar
    public static final int MIN_CELL = 4;      // px per semitone, tightest fit
    public static final int MAX_CELL = 16;     // px per semitone, widest fit
    public static final int KEY_H_MIN = 24;    // keyboard strip height, tightest
    public static final int KEY_H_MAX = 48;    // keyboard strip height, widest
    public int cell = 6;                       // px per semitone (re-fitted on resize)
    public int keyH = 26;                      // keyboard strip height (grows with cell)

    /** top region height: histogram band + keyboard strip */
    public int topH() { return HIST_H + keyH; }

    private static final Color BG = new Color(0xFDFEFA);
    private static final Color COL_PAPER = new Color(0xFDFEFA);
    private static final Color COL_BLACKKEY = new Color(0xF1F7EE);
    private static final Color COL_GRID = new Color(0x9FDF9A);
    private static final Color COL_BEAT = new Color(0xD9EFD4);
    private static final Color COL_BAR = new Color(0x7CD77C);
    private static final Color COL_OCTAVE = new Color(0x3FB43F);
    private static final Color COL_DISABLED = new Color(0xB8BCB8);
    private static final Color COL_ENABLED = Color.BLACK;
    private static final Color COL_SELECTED = new Color(0xE00000);
    private static final Color COL_MARKER = new Color(0xE00000);
    private static final Color COL_PLAY = new Color(0x009A00);

    public interface Listener {
        void pushUndo();      // called before any mouse-originated structural change
        void changed();       // model mutated -> refresh matrix/status
        void hover(String s);
        void pointerHover(int note, long tick);  // note = -1 when over no note
        void markerChanged(long tick);
        void noteMarkClicked(int note);          // in note-marks editing mode
    }

    private Song song;
    private final Listener listener;
    private NoteMarks marks = new NoteMarks();
    private boolean marksMode = false;

    // view state
    private int quantRes = 30;          // ticks; time operations snap to this
    private double pxPerTick;           // set from zoom (px per quarter)
    private long marker = 0;            // ticks, always valid
    private long playTick = -1;         // >=0 while playing
    private boolean penMode = false;

    // interaction state
    private static final int NONE = 0, RECT = 1, MOVE = 2, MOVE_ALL = 3, DUR = 4,
            INSERT = 5, MARKER = 6;
    private int mode = NONE;
    private int pressX, pressY, lastY, lastX;
    private long pressTick, dragT0;         // ticks at gesture start
    private int pressNote;
    private Rectangle2D rect;
    private ArrayList<long[]> base;         // snapshots of dragged notes: note,start,dur

    private int[] statSel = new int[128], statEn = new int[128], statDis = new int[128];
    private boolean statsDirty = true;

    public RollView(Song song, Listener listener) {
        this.song = song;
        this.listener = listener;
        setBackground(BG);
        setPxPerQuarter(34.0);
        addComponentListener(new ComponentAdapter() {
            @Override public void componentResized(ComponentEvent e) { refit(); }
        });
        MouseAdapter m = new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) { doPress(e); }
            @Override public void mouseDragged(MouseEvent e) { doDrag(e); }
            @Override public void mouseReleased(MouseEvent e) { doRelease(e); }
            @Override public void mouseMoved(MouseEvent e) { doMove(e); }
            @Override public void mouseExited(MouseEvent e) { clearPointer(); }
        };
        addMouseListener(m);
        addMouseMotionListener(m);
    }

    /** re-fit the lane width and keyboard height to the current view width */
    public void refit() {
        int w = getWidth();
        if (w <= 0) return;
        int fit = (w - MARGIN - RULER_W) / 128;
        if (fit < MIN_CELL) fit = MIN_CELL;
        if (fit > MAX_CELL) fit = MAX_CELL;
        int kh = Math.max(KEY_H_MIN, Math.min(KEY_H_MAX, KEY_H_MIN + (fit - MIN_CELL) * 2));
        if (fit != cell || kh != keyH) {
            cell = fit;
            keyH = kh;
            revalidate();
            repaint();
        }
    }

    // ---------------------------------------------------------------- Scrollable

    @Override public Dimension getPreferredScrollableViewportSize() { return getPreferredSize(); }
    @Override public int getScrollableUnitIncrement(Rectangle r, int o, int d) { return 40; }
    @Override public int getScrollableBlockIncrement(Rectangle r, int o, int d) { return 200; }
    @Override public boolean getScrollableTracksViewportWidth() { return true; }
    @Override public boolean getScrollableTracksViewportHeight() {
        // fill the viewport with paper while the song is shorter than the
        // window; scroll normally once it grows past it
        return getParent() != null && getPreferredSize().height < getParent().getHeight();
    }

    public void setSong(Song song) { this.song = song; statsDirty = true; revalidate(); repaint(); }

    public Song getSong() { return song; }

    public void setMarks(NoteMarks m) { marks = m; repaint(); }

    /** note-marks editing: histogram clicks cycle the tab colours */
    public void setMarksMode(boolean b) { marksMode = b; repaint(); }
    public boolean isMarksMode() { return marksMode; }

    public void setQuantRes(int ticks) { quantRes = Math.max(1, ticks); }
    public int getQuantRes() { return quantRes; }

    public void setPenMode(boolean b) {
        penMode = b;
        setCursor(penMode ? Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR)
                : Cursor.getDefaultCursor());
    }
    public boolean isPenMode() { return penMode; }

    public void setMarker(long t) {
        marker = Math.max(0, t);
        listener.markerChanged(marker);
        repaint();
    }
    public long getMarker() { return marker; }

    public void setPlayTick(long t) { playTick = t; repaint(); }
    public void clearPlayTick() { playTick = -1; repaint(); }

    public void zoom(double factor) {
        setZoomQ(pxPerQuarter() * factor);
    }
    public void homeZoom() { setZoomQ(30.0); }

    public double pxPerQuarter() { return pxPerTick * song.division; }
    public double pxPerTick() { return pxPerTick; }

    /** set the zoom so one quarter note is roughly pxQuarter pixels tall */
    public void setPxPerQuarter(double pxQuarter) {
        pxPerTick = Math.max(0.05, Math.min(40.0, pxQuarter)) / song.division;
        revalidate();
        repaint();
    }

    private void setZoomQ(double pxQ) {
        pxPerTick = Math.max(1.0, Math.min(400.0, pxQ)) / song.division;
        revalidate();
        repaint();
    }

    // ---- geometry -----------------------------------------------------

    private int rollW() { return MARGIN + 128 * cell; }        // note field width
    /** ruler width: at least RULER_W, extending to the view edge when lanes hit the cap */
    private int rulerW() { return Math.max(RULER_W, getWidth() - rollW()); }
    private int contentW() { return rollW() + rulerW(); }
    private int xFor(int note) { return MARGIN + note * cell; }
    private int noteAtX(int x) { return Math.max(0, Math.min(127, (x - MARGIN) / cell)); }

    private double yForTicks(long t) { return topH() + t * pxPerTick; }
    private long tickAtY(double y) { return (long) Math.floor(Math.max(0, y - topH()) / pxPerTick); }

    private long quantize(long t) {
        return Math.max(0, ((t + quantRes / 2) / quantRes) * quantRes);
    }

    @Override
    public Dimension getPreferredSize() {
        int h = topH() + (int) ((song.endTime() + song.division * 2) * pxPerTick) + 30;
        return new Dimension(contentW(), Math.max(h, 300));
    }

    // ---- stats ---------------------------------------------------------

    private void ensureStats() {
        if (!statsDirty) return;
        statSel = new int[128];
        statEn = new int[128];
        statDis = new int[128];
        for (Note n : song.notes) {
            if (n.selected) statSel[n.note]++;
            else if (n.enabled) statEn[n.note]++;
            else statDis[n.note]++;
        }
        statsDirty = false;
    }
    public void refreshStats() { statsDirty = true; repaint(); }

    // ---- paint ---------------------------------------------------------

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        ensureStats();
        Graphics2D gr = (Graphics2D) g;
        Rectangle clip = gr.getClipBounds();
        gr.setColor(BG);
        gr.fillRect(clip.x, clip.y, clip.width, clip.height);

        long t0 = tickAtY(Math.max(0, clip.y));
        long t1 = tickAtY(clip.y + clip.height) + 1;

        paintPitchField(gr, t0, t1, clip);
        paintNotes(gr, clip, t0, t1);
        paintHistogram(gr);
        paintRuler(gr, clip);
        paintCursorMarks(gr, clip);
        paintHoverInfo(gr);
    }

    /** background column shades + time grid */
    private void paintPitchField(Graphics2D gr, long t0, long t1, Rectangle clip) {
        int x0 = MARGIN;
        // black key columns
        for (int n = 0; n < 128; n++) {
            int m = n % 12;
            if (m == 1 || m == 3 || m == 6 || m == 8 || m == 10) {
                gr.setColor(COL_BLACKKEY);
                gr.fillRect(xFor(n), clip.y, cell, clip.height);
            }
            if (m == 0) {
                gr.setColor(COL_OCTAVE);
                gr.drawLine(xFor(n), clip.y, xFor(n), clip.y + clip.height);
            }
        }
        // grid: horizontal lines per beat and bar, starting from first beat line >= t0
        gr.setFont(new Font("SansSerif", Font.PLAIN, 9));
        int barLen = song.division * 4;
        long bar0 = t0 / barLen;
        // beats
        gr.setColor(COL_BEAT);
        long b = (t0 / song.division) * song.division;
        for (; b <= t1; b += song.division) {
            if (b % barLen == 0) continue;
            double yy = yForTicks(b);
            gr.drawLine(x0, (int) yy, x0 + 128 * cell, (int) yy);
        }
        // bars
        gr.setColor(COL_BAR);
        for (long bb = bar0 * barLen; bb <= t1; bb += barLen) {
            double yy = yForTicks(bb);
            gr.drawLine(x0, (int) yy, x0 + 128 * cell, (int) yy);
        }
        // faint per-note vertical lines
        gr.setColor(COL_GRID);
        for (int n = 1; n < 128; n++) {
            gr.drawLine(xFor(n), clip.y, xFor(n), clip.y + clip.height);
        }
    }

    private void paintNotes(Graphics2D gr, Rectangle clip, long t0, long t1) {
        // draw disabled first, selected last (as in the original)
        for (int pass = 0; pass < 3; pass++) {
            for (Note n : song.notes) {
                boolean isSel = n.selected;
                boolean want = pass == 0 ? (!n.enabled) : (pass == 1 ? (n.enabled && !isSel) : isSel);
                if (!want) continue;
                if (n.end() < t0 || n.start > t1) continue;
                Color c = n.selected ? COL_SELECTED : (n.enabled ? COL_ENABLED : COL_DISABLED);
                double ys = yForTicks(n.start);
                double ye = yForTicks(n.end());
                if (ye - ys < 1.6) ye = ys + 1.6;           // minimum visual height
                gr.setColor(c);
                int x = xFor(n.note) + 1;
                gr.fillRect(x, (int) ys, cell - 1, Math.max(1, (int) (ye - ys)));
                gr.setColor(c.darker());
                gr.drawRect(x, (int) ys, cell - 1, Math.max(1, (int) (ye - ys)));
            }
        }
    }

    private void paintHistogram(Graphics2D gr) {
        int max = 1;
        for (int n = 0; n < 128; n++) {
            int c = statSel[n] + statEn[n] + statDis[n];
            if (c > max) max = c;
        }
        int baseline = topH() - keyH;

        // pitch labels every octave, in their own clear band at the top.
        // The font scales with the lane width (11..18 px, bold, dark ink),
        // and the band is filled with the paper colour so no grid line,
        // lane line or bar crosses the text.
        int labelSize = Math.max(11, Math.min(18, 11 + (cell - 6)));
        Font labelFont = new Font("SansSerif", Font.BOLD, labelSize);
        FontMetrics labelFm = gr.getFontMetrics(labelFont);
        int bandH = labelFm.getHeight() + 2;

        gr.setColor(BG);
        gr.fillRect(0, 0, rollW(), bandH);

        gr.setColor(new Color(0xD0D0D0));
        for (int n = 0; n < 128; n++) {          // lane lines start below the band
            int x = xFor(n);
            gr.drawLine(x, bandH, x, baseline);
        }
        // bars end at the top of the note-mark tab band, like the original
        int tabH = Math.max(4, Math.min(8, cell / 2));
        int barBase = baseline - tabH;
        int barTop = bandH;
        int barH = barBase - barTop - 2;
        for (int n = 0; n < 128; n++) {
            int x = xFor(n) + 1;
            if (statDis[n] > 0) {
                int h = Math.max(1, (int) ((long) statDis[n] * barH / max));
                gr.setColor(new Color(0xC8C8C8));
                gr.fillRect(x, barBase - h, cell - 1, h);
            }
            if (statEn[n] > 0) {
                int h = Math.max(1, (int) ((long) statEn[n] * barH / max));
                gr.setColor(Color.BLACK);
                gr.fillRect(x, barBase - h, cell - 1, h);
            }
            if (statSel[n] > 0) {
                int h = Math.max(1, (int) ((long) statSel[n] * barH / max));
                gr.setColor(COL_SELECTED);
                gr.fillRect(x, barBase - h, cell - 1, h);
            }
        }
        // note-mark tabs: colour band between the bars and the keyboard
        for (int n = 0; n < 128; n++) {
            Color mc = markColor(marks.color[n]);
            if (mc != null) {
                gr.setColor(mc);
                gr.fillRect(xFor(n), barBase, cell, tabH);
            }
        }
        // keyboard strip at the bottom of the top region (white/black piano style);
        // the black-key inset scales a little with the key height
        int inset = Math.max(1, keyH / 12);
        for (int n = 0; n < 128; n++) {
            int m = n % 12;
            boolean black = (m == 1 || m == 3 || m == 6 || m == 8 || m == 10);
            if (black) {
                gr.setColor(new Color(0x1A1A1A));
                gr.fillRect(xFor(n), baseline + inset, cell, keyH - inset);
            } else {
                gr.setColor(Color.WHITE);
                gr.fillRect(xFor(n), baseline, cell, keyH);
            }
        }
        gr.setColor(new Color(0x202020));
        gr.drawLine(MARGIN, baseline, MARGIN + 128 * cell, baseline);
        gr.drawLine(MARGIN, baseline + keyH, MARGIN + 128 * cell, baseline + keyH);
        // octave separators drawn through keyboard
        for (int n = 0; n < 128; n += 12) {
            gr.drawLine(xFor(n), baseline + 1, xFor(n), baseline + keyH - 1);
        }
        // highlight column from percussion picker
        if (highlighted >= 0) {
            gr.setColor(new Color(255, 0, 0, 60));
            gr.fillRect(xFor(highlighted), 0, cell, topH());
        }
        // labels last, on top of everything, nothing crosses them
        gr.setFont(labelFont);
        gr.setColor(new Color(0x383838));
        for (int n = 0; n < 128; n += 12) {
            gr.drawString("C" + (n / 12 - 1), xFor(n) + 3, labelFm.getAscent() + 1);
        }
    }

    private int highlighted = -1;
    public void highlightNote(int note) { highlighted = note; repaint(); }
    public void clearHighlightNote() { highlighted = -1; repaint(); }

    /** tab colours of the original program, sampled from its display */
    private static Color markColor(int c) {
        switch (c) {
            case NoteMarks.BLACK: return new Color(0x000000);
            case NoteMarks.RED:   return new Color(0xE00000);
            case NoteMarks.GREEN: return new Color(0x40A000);
            case NoteMarks.BLUE:  return new Color(0x6060E0);
            default: return null;
        }
    }

    private void paintRuler(Graphics2D gr, Rectangle clip) {
        int rx = rollW();
        int rw = rulerW();
        gr.setColor(new Color(0xFAFAF7));
        gr.fillRect(rx, clip.y, rw, clip.height);
        gr.setColor(COL_GRID);
        gr.drawLine(rx, clip.y, rx, clip.y + clip.height);

        int barLen = song.division * 4;
        long t0 = tickAtY(Math.max(0, clip.y));
        long t1 = tickAtY(clip.y + clip.height) + 1;
        int rulerSize = Math.max(9, Math.min(14, 9 + (cell - 6)));   // scales like the octave labels
        gr.setFont(new Font("SansSerif", Font.PLAIN, rulerSize));
        FontMetrics fm = gr.getFontMetrics();
        double pxBar = barLen * pxPerTick;
        for (long bb = (t0 / barLen) * barLen; bb <= t1; bb += barLen) {
            double yy = yForTicks(bb);
            gr.setColor(COL_BAR);
            gr.drawLine(rx, (int) yy, contentW(), (int) yy);
            if (pxBar >= fm.getHeight() + 4) {
                String s = Long.toString(bb / barLen);
                gr.setColor(new Color(0x383838));
                gr.drawString(s, rx + 4, (int) yy + fm.getAscent() - 2);
            }
        }
        // beat ticks on ruler
        gr.setColor(new Color(0xC0C0C0));
        for (long b = (t0 / song.division) * song.division; b <= t1; b += song.division) {
            if (b % barLen == 0) continue;
            double yy = yForTicks(b);
            gr.drawLine(rx, (int) yy, rx + 8, (int) yy);
        }
    }

    private void paintCursorMarks(Graphics2D gr, Rectangle clip) {
        // selection rectangle
        if (mode == RECT && rect != null) {
            Rectangle2D r = rect;
            int a = (int) r.getX(), b = (int) r.getY();
            int w = (int) r.getWidth(), h = (int) r.getHeight();
            gr.setColor(new Color(32, 32, 160, 70));
            gr.fillRect(a, b, w, h);
            gr.setColor(new Color(0x2020A0));
            gr.drawRect(a, b, w, h);
        }
        // marker line (red)
        double my = yForTicks(marker);
        if (my >= topH() && my < clip.y + clip.height && my + 1 >= clip.y) {
            gr.setColor(COL_MARKER);
            gr.drawLine(0, (int) my, contentW(), (int) my);
            // little handle on the ruler
            gr.fillRect(rollW(), (int) my - 2, rulerW(), 4);
        }
        // play line (green)
        if (playTick >= 0) {
            double py = yForTicks(playTick);
            gr.setColor(COL_PLAY);
            gr.drawLine(MARGIN, (int) py, rollW(), (int) py);
        }
        // pointer indicator: lane highlight + outline of the note under the cursor
        if (mode == NONE && mx >= 0 && my >= topH() && hoverCol >= 0
                && mx < rollW()) {
            int lx = xFor(hoverCol);
            gr.setColor(new Color(30, 90, 220, 36));
            gr.fillRect(lx, topH(), cell, Math.max(0, getHeight() - topH()));
            if (hoverNote != null) {
                double ys = yForTicks(hoverNote.start);
                double ye = yForTicks(hoverNote.end());
                if (ye - ys < 1.6) ye = ys + 1.6;
                gr.setColor(new Color(0xFF8C00));
                gr.setStroke(new java.awt.BasicStroke(1.4f));
                gr.drawRect(lx, (int) ys - 1, cell - 1, Math.max(1, (int) (ye - ys)) + 2);
                gr.setStroke(new java.awt.BasicStroke(1f));
            }
        }
    }

    private String hoverText = "";

    /** fixed, scroll-independent tooltip chip shown near the cursor */
    private void paintHoverInfo(Graphics2D gr) {
        if (hoverText.isEmpty() || mx < 0 || my < 0) return;
        int cw = getWidth();
        gr.setFont(new Font("SansSerif", Font.BOLD, 13));
        FontMetrics fm = gr.getFontMetrics();
        int w = fm.stringWidth(hoverText) + 12;
        int h = fm.getHeight() + 8;
        int x = mx + 14;
        int y = my + 16;
        if (x + w > cw - 6) x = mx - w - 10;
        if (y + h > getHeight() - 4) y = my - h - 6;
        gr.setColor(new Color(30, 30, 30, 235));
        gr.fillRoundRect(x, y, w, h, 8, 8);
        gr.setColor(new Color(255, 214, 100));
        gr.drawRoundRect(x, y, w, h, 8, 8);
        gr.setColor(Color.WHITE);
        gr.drawString(hoverText, x + 6, y + fm.getAscent() + 5);
    }

    // ---- hit testing ----------------------------------------------------

    /** topmost note at pixel position, or null */
    private Note noteAt(int x, int y) {
        int note = noteAtX(x);
        if (note < 0 || note > 127) return null;
        long t = tickAtY(y);
        // paint order disabled -> enabled -> selected, so prefer selected
        for (int pass = 2; pass >= 0; pass--) {
            for (Note n : song.notes) {
                if (n.note != note) continue;
                boolean inPass = pass == 2 ? n.selected : (pass == 1 ? n.enabled && !n.selected : !n.enabled);
                if (!inPass) continue;
                if (t >= n.start && t <= n.end() && n.end() >= n.start) return n;
            }
        }
        return null;
    }

    // hover tracking for the pointer indicator
    private int mx = -1, my = -1;
    private int hoverCol = -1;
    private Note hoverNote;
    private long hoverTick;
    private int lastPtrNote = -2;
    private long lastPtrTick = -1;

    private void updatePointer(int x, int y) {
        mx = x; my = y;
        String h = "";
        if (marksMode && x >= MARGIN && x < rollW() && y < topH()) {
            // editing the tabs: name the tab colour a click would produce
            int col = noteAtX(x);
            int next = (marks.color[col] + 1) % 5;
            h = String.format("%s  note %d (0x%02X): %s tab \u2013 click to change",
                    NoteName2.name(col), col, col, NoteMarks.COLOR_NAMES[next]);
            if (lastPtrNote != col) {
                lastPtrNote = col;
                listener.pointerHover(col, 0);
            }
        } else if (x >= MARGIN && x < MARGIN + 128 * cell && y >= topH()) {
            hoverCol = noteAtX(x);
            hoverTick = Math.max(0, tickAtY(y));
            Note n = noteAt(x, y);
            if (n != null) {
                h = String.format("%s  note %d (0x%02X)  ch%d tr%d v%d  %s -> %s  [%s]",
                        NoteName2.name(n.note), n.note, n.note, n.channel, n.track, n.velocity,
                        song.barBeatTick(n.start), song.barBeatTick(n.end()),
                        n.selected ? "selected" : (n.enabled ? "enabled" : "disabled"));
            } else {
                h = String.format("%s  note %d (0x%02X)   t %s",
                        NoteName2.name(hoverCol), hoverCol, hoverCol, song.barBeatTick(hoverTick));
            }
            hoverNote = n;
            if (lastPtrNote != hoverCol || lastPtrTick != hoverTick) {
                lastPtrNote = hoverCol;
                lastPtrTick = hoverTick;
                listener.pointerHover(n != null ? n.note : hoverCol,
                        n != null ? n.start : hoverTick);
            }
        } else if (x >= rollW() && y >= topH()) {
            hoverCol = -1;
            hoverNote = null;
            hoverTick = Math.max(0, tickAtY(y));
            if (lastPtrNote != -1) {
                lastPtrNote = -1;
                lastPtrTick = hoverTick;
                listener.pointerHover(-1, hoverTick);
            }
            h = "time " + song.barBeatTick(hoverTick);
        } else {
            if (lastPtrNote != -1) {
                lastPtrNote = -1;
                listener.pointerHover(-1, Math.max(0, tickAtY(y)));
            }
            hoverNote = null;
            hoverCol = -1;
        }
        if (!h.equals(hoverText)) {
            hoverText = h;
            listener.hover(h);
        }
        repaint();
    }

    private void clearPointer() {
        hoverNote = null;
        hoverCol = -1;
        if (!hoverText.isEmpty()) {
            hoverText = "";
            listener.hover("");
        }
        if (lastPtrNote != -1) {
            lastPtrNote = -1;
            listener.pointerHover(-1, 0);
        }
        mx = my = -1;
        repaint();
    }

    // ---- mouse ----------------------------------------------------------

    private void doPress(MouseEvent e) {
        requestFocusInWindow();
        int x = e.getX(), y = e.getY();
        pressX = x; pressY = y;
        lastX = x; lastY = y;
        undoPushed = false;
        listener.hover("");
        if (marksMode) {                          // note-marks editing: histogram clicks cycle tabs
            if (x >= MARGIN && x < rollW() && y < topH()) {
                listener.noteMarkClicked(noteAtX(x));
            }
            return;
        }
        if (x >= rollW()) {                       // time bar: set marker
            mode = MARKER;
            setMarker(quantize(tickAtY(y)));
            return;
        }
        if (y < topH()) {                          // histogram: select by note column
            int note = noteAtX(x);
            if (note >= 0 && note <= 127) {
                selectPitch(note, e.isControlDown());
                listener.changed();
            }
            return;
        }
        if (penMode) {                            // insertion mode
            pressTick = quantize(tickAtY(y));
            pressNote = noteAtX(x);
            if (!insertNote(pressNote, pressTick)) {
                mode = NONE;
                return;
            }
            listener.pushUndo();
            undoPushed = true;
            mode = INSERT;
            dragT0 = pressTick;
            listener.changed();
            return;
        }
        if (e.isShiftDown() && song.countSelectedNotes() > 0) {  // drag whole selection
            mode = MOVE_ALL;
            pressTick = quantize(tickAtY(y));
            pressNote = noteAtX(x);
            snapshotSelection();
            return;
        }
        Note hit = noteAt(x, y);
        if (hit == null || !hit.enabled) {
            // empty paper or a disabled (gray) note: start a rectangle selection
            if (!e.isControlDown()) {
                song.clearSelection();
                listener.changed();
            }
            mode = RECT;
            rect = new Rectangle2D.Double(x, y, 0, 0);
            return;
        }
        // pressed on an enabled note
        if (!hit.selected) {
            if (!e.isControlDown()) song.clearSelection();
            hit.selected = true;
        } else if (e.isControlDown()) {
            hit.selected = false;
            listener.changed();
            mode = NONE;
            return;
        }
        mode = (y - yForTicks(hit.start)) < (yForTicks(hit.end()) - yForTicks(hit.start)) * 0.45
                ? MOVE : DUR;
        pressTick = quantize(tickAtY(y));
        pressNote = noteAtX(x);
        snapshotSelection();
        listener.changed();
    }

    private void selectPitch(int note, boolean ctrl) {
        if (!ctrl) song.clearSelection();
        for (Note n : song.notes)
            if (n.note == note && n.enabled)
                n.selected = ctrl ? !n.selected : true;
    }

    private void snapshotSelection() {
        base = new ArrayList<>();
        for (Note n : song.notes)
            if (n.selected) base.add(new long[]{n.note, n.start, n.dur});
    }

    private boolean insertNote(int note, long tick) {
        long dur = Math.max(quantRes, quantize(noteDur));
        for (Note o : song.notes)
            if (o.enabled && o.note == note && tick < o.end() && tick + dur > o.start)
                return false;                    // cannot insert on top of an existing note
        Note n = new Note(note, velocity, tick, dur, channel, track);
        n.selected = true;
        song.clearSelection();
        song.notes.add(n);
        insertedNote = n;
        return true;
    }

    private Note insertedNote;
    private int velocity = 64;
    private int channel = 0;
    private int track = 0;
    private long noteDur = 120;

    public void setInsertDefaults(int chan, int trk, int vel, long dur) {
        channel = chan;
        track = trk;
        velocity = vel;
        noteDur = dur;
    }

    private void doDrag(MouseEvent e) {
        int x = e.getX(), y = e.getY();
        lastX = x; lastY = y;
        switch (mode) {
            case MARKER:
                setMarker(quantize(tickAtY(y)));
                break;
            case RECT: {
                double x0 = Math.min(pressX, x), x1 = Math.max(pressX, x);
                double y0 = Math.min(pressY, y), y1 = Math.max(pressY, y);
                rect = new Rectangle2D.Double(x0, y0, x1 - x0, y1 - y0);
                repaint();
                break;
            }
            case INSERT: {
                if (insertedNote != null) {
                    long t = quantize(tickAtY(y));
                    insertedNote.dur = Math.max(quantRes, t - insertedNote.start);
                    repaint();
                }
                break;
            }
            case MOVE:
            case MOVE_ALL:
            case DUR: {
                applyDragGesture(y, x);
                break;
            }
            default:
        }
    }

    private void applyDragGesture(int y, int x) {
        if (base == null || base.isEmpty()) return;
        long tNow = quantize(tickAtY(y));
        long dt = tNow - pressTick;                 // quantized delta
        if (mode == DUR) {
            if (dt != 0) pushUndoOnce();
            // absolute end change from the snapshot (as in the original):
            // the pointer position decides the length, drags back restore it
            int idx = 0;
            for (Note n : song.notes) {
                if (!n.selected) continue;
                long[] b = base.get(Math.min(idx, base.size() - 1));
                idx++;
                long d = b[2] + dt;
                if (d < quantRes) d = quantRes;
                if (d < 1) d = 1;
                n.dur = d;
            }
            repaint();
            return;
        }
        int dn = (int) Math.round((x - pressX) / (double) cell);
        if (dt != 0 || dn != 0) pushUndoOnce();
        // apply same delta to every selected note, from their snapshots
        int idx = 0;
        for (Note n : song.notes) {
            if (!n.selected) continue;
            long[] b = base.get(Math.min(idx, base.size() - 1));
            idx++;
            int nn = (int) b[0] + dn;
            if (nn < 0) nn = 0;
            if (nn > 127) nn = 127;
            long st = Math.max(0, b[1] + dt);
            n.note = nn;
            n.start = st;
        }
        repaint();
    }

    private boolean undoPushed = false;

    private void pushUndoOnce() {
        if (!undoPushed) {
            undoPushed = true;
            listener.pushUndo();
        }
    }

    private void doRelease(MouseEvent e) {
        int x = e.getX(), y = e.getY();
        switch (mode) {
            case RECT:
                if (rect != null) {
                    int x0n = noteAtX((int) rect.getMinX());
                    int x1n = noteAtX((int) rect.getMaxX());
                    long y0t = tickAtY(rect.getMinY());
                    long y1t = tickAtY(rect.getMaxY());
                    selectRect(x0n, x1n, y0t, y1t, e.isControlDown());
                    listener.changed();
                }
                rect = null;
                break;
            case INSERT:
                if (insertedNote != null) {
                    insertedNote.dur = Math.max(quantRes, quantize(tickAtY(y)) - insertedNote.start);
                    listener.changed();
                }
                insertedNote = null;
                break;
            case MOVE:
            case MOVE_ALL:
            case DUR:
                base = null;
                listener.changed();
                break;
            default:
        }
        mode = NONE;
        repaint();
    }

    private void selectRect(int n0, int n1, long t0, long t1, boolean ctrl) {
        if (!ctrl) song.clearSelection();
        boolean any = false;
        for (Note n : song.notes) {
            if (!n.enabled) continue;
            if (n.note < n0 || n.note > n1) continue;
            if (n.end() < t0 || n.start > t1) continue;
            if (ctrl) n.selected = !n.selected;
            else n.selected = true;
            any = true;
        }
        if (!any && !ctrl) song.clearSelection();
    }

    private void doMove(MouseEvent e) {
        updatePointer(e.getX(), e.getY());
    }

    /** abort current gesture (ESC) */
    public void cancelGesture() {
        if (mode == INSERT && insertedNote != null) {
            song.notes.remove(insertedNote);
            insertedNote = null;
            listener.changed();
        }
        mode = NONE;
        rect = null;
        base = null;
        repaint();
    }
}
