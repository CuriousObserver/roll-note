package rollnote.ui;

import rollnote.core.Note;
import rollnote.core.NoteMarks;
import rollnote.core.Song;

import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * The fixed top band of the display: octave labels, note histogram,
 * note-mark tabs and the keyboard. Pinned above the scrolling roll as the
 * scroll pane's column header, sharing the lane metrics with {@link RollView}
 * so the columns stay aligned.
 */
public class HeaderView extends JPanel {
    private static final long serialVersionUID = 1L;

    private final RollView view;            // shared lane metrics (cell, keyH)
    private final RollView.Listener listener;
    private Song song;
    private NoteMarks marks = new NoteMarks();
    private boolean marksMode;

    private int highlighted = -1;           // percussion picker column
    private int[] statSel = new int[128], statEn = new int[128], statDis = new int[128];
    private boolean statsDirty = true;

    private int mx = -1, my = -1;           // hover chip (note-marks editing)
    private String hoverText = "";
    private int lastHoverNote = -2;

    public HeaderView(RollView view, Song song, NoteMarks marks, RollView.Listener listener) {
        this.view = view;
        this.song = song;
        this.marks = marks;
        this.listener = listener;
        setBackground(RollView.BG);
        MouseAdapter m = new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) { doPress(e); }
            @Override public void mouseMoved(MouseEvent e) { doMove(e); }
            @Override public void mouseExited(MouseEvent e) { clearHover(); }
        };
        addMouseListener(m);
        addMouseMotionListener(m);
    }

    public void setSong(Song s) { song = s; statsDirty = true; repaint(); }
    public void setMarks(NoteMarks m) { marks = m; repaint(); }
    public void setMarksMode(boolean b) { marksMode = b; clearHover(); repaint(); }
    public void highlightNote(int note) { highlighted = note; repaint(); }
    public void clearHighlightNote() { highlighted = -1; repaint(); }
    public void refreshStats() { statsDirty = true; repaint(); }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(view.getPreferredSize().width, RollView.HIST_H + view.keyH);
    }

    private int xFor(int n) { return RollView.MARGIN + n * view.cell; }
    private int noteAtX(int x) { return Math.max(0, Math.min(127, (x - RollView.MARGIN) / view.cell)); }
    private int rollW() { return RollView.MARGIN + 128 * view.cell; }
    private int topH() { return RollView.HIST_H + view.keyH; }

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

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        ensureStats();
        Graphics2D gr = (Graphics2D) g;
        Rectangle clip = gr.getClipBounds();
        gr.setColor(RollView.BG);
        gr.fillRect(clip.x, clip.y, clip.width, clip.height);

        int max = 1;
        for (int n = 0; n < 128; n++) {
            int c = statSel[n] + statEn[n] + statDis[n];
            if (c > max) max = c;
        }
        int baseline = RollView.HIST_H;

        // octave labels in their own clear band, scaled with the lane width
        int labelSize = Math.max(11, Math.min(18, 11 + (view.cell - 6)));
        Font labelFont = new Font("SansSerif", Font.BOLD, labelSize);
        FontMetrics labelFm = gr.getFontMetrics(labelFont);
        int bandH = labelFm.getHeight() + 2;

        gr.setColor(new Color(0xD0D0D0));
        for (int n = 0; n < 128; n++) {          // lane lines start below the band
            gr.drawLine(xFor(n), bandH, xFor(n), baseline);
        }
        // bars end at the top of the note-mark tab band
        int tabH = Math.max(4, Math.min(8, view.cell / 2));
        int barBase = baseline - tabH;
        int barH = barBase - bandH - 2;
        for (int n = 0; n < 128; n++) {
            int x = xFor(n) + 1;
            if (statDis[n] > 0) {
                int h = Math.max(1, (int) ((long) statDis[n] * barH / max));
                gr.setColor(new Color(0xC8C8C8));
                gr.fillRect(x, barBase - h, view.cell - 1, h);
            }
            if (statEn[n] > 0) {
                int h = Math.max(1, (int) ((long) statEn[n] * barH / max));
                gr.setColor(Color.BLACK);
                gr.fillRect(x, barBase - h, view.cell - 1, h);
            }
            if (statSel[n] > 0) {
                int h = Math.max(1, (int) ((long) statSel[n] * barH / max));
                gr.setColor(RollView.COL_SELECTED);
                gr.fillRect(x, barBase - h, view.cell - 1, h);
            }
        }
        // note-mark tabs: colour band between the bars and the keyboard
        for (int n = 0; n < 128; n++) {
            Color mc = markColor(marks.color[n]);
            if (mc != null) {
                gr.setColor(mc);
                gr.fillRect(xFor(n), barBase, view.cell, tabH);
            }
        }
        // keyboard strip (white/black piano style)
        int inset = Math.max(1, view.keyH / 12);
        for (int n = 0; n < 128; n++) {
            int mm = n % 12;
            boolean black = (mm == 1 || mm == 3 || mm == 6 || mm == 8 || mm == 10);
            if (black) {
                gr.setColor(new Color(0x1A1A1A));
                gr.fillRect(xFor(n), baseline + inset, view.cell, view.keyH - inset);
            } else {
                gr.setColor(Color.WHITE);
                gr.fillRect(xFor(n), baseline, view.cell, view.keyH);
            }
        }
        gr.setColor(new Color(0x202020));
        gr.drawLine(RollView.MARGIN, baseline, RollView.MARGIN + 128 * view.cell, baseline);
        gr.drawLine(RollView.MARGIN, baseline + view.keyH, RollView.MARGIN + 128 * view.cell, baseline + view.keyH);
        for (int n = 0; n < 128; n += 12) {
            gr.drawLine(xFor(n), baseline + 1, xFor(n), baseline + view.keyH - 1);
        }
        // percussion picker highlight column
        if (highlighted >= 0) {
            gr.setColor(new Color(255, 0, 0, 60));
            gr.fillRect(xFor(highlighted), 0, view.cell, topH());
        }
        // labels last, nothing crosses them
        gr.setFont(labelFont);
        gr.setColor(new Color(0x383838));
        for (int n = 0; n < 128; n += 12) {
            gr.drawString("C" + (n / 12 - 1), xFor(n) + 3, labelFm.getAscent() + 1);
        }
        // note-marks hover chip
        if (marksMode && !hoverText.isEmpty() && mx >= 0 && my >= 0) {
            gr.setFont(new Font("SansSerif", Font.BOLD, 13));
            FontMetrics fm = gr.getFontMetrics();
            int w = fm.stringWidth(hoverText) + 12;
            int h = fm.getHeight() + 8;
            int x = mx + 14, y = my + 16;
            if (x + w > getWidth() - 6) x = mx - w - 10;
            gr.setColor(new Color(30, 30, 30, 235));
            gr.fillRoundRect(x, y, w, h, 8, 8);
            gr.setColor(new Color(255, 214, 100));
            gr.drawRoundRect(x, y, w, h, 8, 8);
            gr.setColor(Color.WHITE);
            gr.drawString(hoverText, x + 6, y + fm.getAscent() + 5);
        }
    }

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

    private void doPress(MouseEvent e) {
        requestFocusInWindow();
        int x = e.getX(), y = e.getY();
        if (x < RollView.MARGIN || x >= rollW()) return;
        if (marksMode) {
            listener.noteMarkClicked(noteAtX(x));
            return;
        }
        int note = noteAtX(x);
        if (e.isControlDown()) {
            for (Note n : song.notes)
                if (n.note == note && n.enabled) n.selected = !n.selected;
        } else {
            song.clearSelection();
            for (Note n : song.notes)
                if (n.note == note && n.enabled) n.selected = true;
        }
        listener.changed();
    }

    private void doMove(MouseEvent e) {
        mx = e.getX();
        my = e.getY();
        String h = "";
        if (marksMode && mx >= RollView.MARGIN && mx < rollW()) {
            int col = noteAtX(mx);
            int next = (marks.color[col] + 1) % 5;
            h = String.format("%s  note %d (0x%02X): %s tab \u2013 click to change",
                    NoteName2.name(col), col, col, NoteMarks.COLOR_NAMES[next]);
            if (lastHoverNote != col) {
                lastHoverNote = col;
                listener.pointerHover(col, 0);
            }
        } else if (lastHoverNote != -2) {
            lastHoverNote = -2;
            listener.pointerHover(-1, 0);
        }
        if (!h.equals(hoverText)) {
            hoverText = h;
            listener.hover(h);
        }
        repaint();
    }

    private void clearHover() {
        if (!hoverText.isEmpty()) {
            hoverText = "";
            listener.hover("");
        }
        if (lastHoverNote != -2) {
            lastHoverNote = -2;
            listener.pointerHover(-1, 0);
        }
        mx = my = -1;
        repaint();
    }
}
