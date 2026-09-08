package rollnote.ui;

import rollnote.core.CtrlEvent;
import rollnote.core.MetaEvent;
import rollnote.core.Note;
import rollnote.core.Smf;
import rollnote.core.Song;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.KeyStroke;
import javax.swing.SpinnerNumberModel;
import javax.swing.UIManager;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.InputMap;

/** Main application window, organised like the original Rollook form. */
public class MainWindow extends JFrame implements RollView.Listener {
    private static final long serialVersionUID = 1L;
    private Song song = new Song();
    private Path currentFile;
    private boolean dirty = false;

    private final RollView view = new RollView(song, this);
    private final Player player = new Player(new Player.Listener() {
        @Override public void playTick(long t) {
            view.setPlayTick(t);
            if (t > viewTopVisible() - 1) ensureVisible(t);
        }
        @Override public void stopped() { view.clearPlayTick(); updateStatus(); }
    });

    // undo history
    private final ArrayDeque<Song> undoStack = new ArrayDeque<>();
    private final ArrayDeque<Song> redoStack = new ArrayDeque<>();

    // clipboard: notes only, plus the oldest start time (paste origin)
    private List<Note> clipboard = new ArrayList<>();
    private long clipboardOrigin = 0;

    // ---- widgets
    private final JLabel fileLabel = new JLabel(" ");
    private final JLabel status = new JLabel(" ");
    private final JLabel markerLabel = new JLabel("marker 0:0:0");
    private final JLabel pointerLabel = new JLabel(" ");
    {   // big, hard to miss note readout
        pointerLabel.setFont(pointerLabel.getFont().deriveFont(Font.BOLD, 14f));
        pointerLabel.setForeground(new Color(0x1040A0));
        pointerLabel.setBorder(BorderFactory.createEmptyBorder(0, 18, 0, 6));
    }
    private final MatrixPanel matrix = new MatrixPanel();
    private final JRadioButton radioChannel = new JRadioButton("Channel");
    private final JRadioButton radioSequence = new JRadioButton("Sequence");
    private final JSpinner spinSeq = new JSpinner(new SpinnerNumberModel(0, 0, 255, 1));
    private final JSpinner spinChan = new JSpinner(new SpinnerNumberModel(0, 0, 15, 1));
    private final JSpinner spinVel = new JSpinner(new SpinnerNumberModel(64, 0, 127, 1));
    private final JSpinner spinDur = new JSpinner(new SpinnerNumberModel(120, 1, 1_000_000, 1));
    private final JSpinner spinDurPct = new JSpinner(new SpinnerNumberModel(100, 1, 1000, 1));
    private final JSpinner spinTrans = new JSpinner(new SpinnerNumberModel(0, -127, 127, 1));
    private final JCheckBox pen = new JCheckBox("Pen");
    private final JButton playButton = new JButton("Play");
    private final JButton stopButton = new JButton("Stop");
    private final JRadioButton speed1 = new JRadioButton("1x");
    private final JRadioButton speedHalf = new JRadioButton("1/2x");
    private final JRadioButton speedQuarter = new JRadioButton("1/4x");

    private static final int[] RES_DENOMS = {1, 2, 4, 8, 16, 32, 64};

    public MainWindow() {
        super("RollNote");
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override public void windowClosing(java.awt.event.WindowEvent e) { quit(); }
        });
        debug("LAF=" + UIManager.getLookAndFeel().getName()
                + " panelBg=" + Integer.toHexString(UIManager.getColor("Panel.background").getRGB() & 0xFFFFFF));

        JPanel root = new JPanel(new BorderLayout(4, 4));
        root.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        root.add(buildTopRow(), BorderLayout.NORTH);
        root.add(buildWest(), BorderLayout.WEST);
        JScrollPane sp = new JScrollPane(view);
        sp.getVerticalScrollBar().setUnitIncrement(40);
        sp.setPreferredSize(new Dimension(view.getPreferredSize().width, 480));
        root.add(sp, BorderLayout.CENTER);
        root.add(buildStatus(), BorderLayout.SOUTH);
        add(root);

        setJMenuBar(buildMenus());
        bindKeys(root);
        setSize(1280, 900);
        setMinimumSize(new Dimension(1000, 640));
        setLocationRelativeTo(null);
        updateMatrixCount();
        updateStatus();
    }

    // ------------------------------------------------------------ left rail

    /** channel/sequence squares across the top, like the original's "All 0..F" row */
    private JPanel buildTopRow() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 2));
        p.add(new JLabel("All"));
        ButtonGroup selGroup = new ButtonGroup();
        radioChannel.setSelected(true);
        selGroup.add(radioChannel);
        selGroup.add(radioSequence);
        radioChannel.addActionListener(e -> updateMatrixCount());
        radioSequence.addActionListener(e -> updateMatrixCount());
        p.add(radioSequence);
        p.add(radioChannel);
        matrix.setPreferredSize(new Dimension(matrixCells() * 38 + 8, 24));
        p.add(matrix);
        return p;
    }

    /** slim control column on the left, like the original's arrangement */
    private JPanel buildWest() {
        JPanel col = new JPanel();
        col.setLayout(new BoxLayout(col, BoxLayout.Y_AXIS));
        col.setPreferredSize(new Dimension(212, 0));

        // time resolution radios
        JPanel resRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 2));
        resRow.add(new JLabel("T res"));
        ButtonGroup resGroup = new ButtonGroup();
        for (int den : RES_DENOMS) {
            JRadioButton rb = new JRadioButton("1/" + den);
            rb.addActionListener(e -> applyRes(den));
            resGroup.add(rb);
            if (den == 8) { rb.setSelected(true); applyRes(8); }
            resRow.add(rb);
        }
        col.add(resRow);
        col.add(Box.createVerticalStrut(4));

        JButton pButton = new JButton("P  Percussions");
        pButton.setToolTipText("Percussion names (channel 9)");
        pButton.addActionListener(e -> Dialogs.showPercussionPicker(MainWindow.this,
                note -> view.highlightNote(note)));
        JPanel pRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 2));
        pRow.add(pButton);
        col.add(pRow);
        col.add(Box.createVerticalStrut(6));

        // big operation buttons with their spin boxes
        col.add(buildOpRow("Seq", spinSeq, e -> bulkOp(op -> op.track = spinSeqInt())));
        col.add(buildOpRow("Channel", spinChan, e -> bulkOp(op -> op.channel = spinChanInt())));
        col.add(buildOpRow("Velocity", spinVel, e -> bulkOp(op -> op.velocity = spinVelInt())));
        // duration preset: length of notes inserted with the pen
        JPanel penDurRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 1));
        penDurRow.add(makeBig("Dur"));
        penDurRow.add(Box.createHorizontalStrut(2));
        penDurRow.add(spinDur);
        penDurRow.add(new JLabel(" ticks"));
        col.add(penDurRow);

        JPanel durRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 1));
        durRow.add(makeBig("Duration %"));
        durRow.add(Box.createHorizontalStrut(2));
        durRow.add(spinDurPct);
        JButton scaleBtn = new JButton("Scale");
        scaleBtn.addActionListener(e -> bulkOp(op -> {
            double f = ((Number) spinDurPct.getValue()).doubleValue() / 100.0;
            op.dur = Math.max(1, Math.round(op.dur * f));
        }));
        durRow.add(scaleBtn);
        col.add(durRow);

        JPanel trRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 1));
        trRow.add(makeBig("Transpose"));
        trRow.add(Box.createHorizontalStrut(2));
        trRow.add(spinTrans);
        JButton trApp = new JButton("Apply");
        trApp.addActionListener(e -> bulkOp(op -> {
            op.note = Math.max(0, Math.min(127, op.note + spinTransInt()));
        }));
        trRow.add(trApp);
        col.add(trRow);
        col.add(Box.createVerticalStrut(6));

        // transport + tools
        JPanel r3 = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 1));
        JButton zero = new JButton("0");
        zero.setToolTipText("Scroll to start");
        zero.addActionListener(e -> scrollToStart());
        JButton minus = new JButton("Zoom -");
        minus.addActionListener(e -> view.zoom(0.7));
        JButton plus = new JButton("Zoom +");
        plus.addActionListener(e -> view.zoom(1.45));
        pen.addActionListener(e -> {
            view.setPenMode(pen.isSelected());
            view.setInsertDefaults(spinChanInt(), spinSeqInt(), spinVelInt(), spinDurLong());
        });
        playButton.addActionListener(e -> play());
        stopButton.addActionListener(e -> player.stop());
        ButtonGroup sp = new ButtonGroup();
        sp.add(speed1); sp.add(speedHalf); sp.add(speedQuarter);
        speed1.setSelected(true);
        r3.add(new JLabel("Speed"));
        r3.add(speed1); r3.add(speedHalf); r3.add(speedQuarter);
        r3.add(Box.createHorizontalStrut(8));
        r3.add(playButton);
        r3.add(stopButton);
        r3.add(Box.createHorizontalStrut(8));
        r3.add(pen);
        r3.add(Box.createHorizontalStrut(8));
        r3.add(zero);
        r3.add(minus);
        r3.add(plus);
        col.add(r3);
        col.add(Box.createVerticalGlue());
        return col;
    }

    private JPanel buildOpRow(String name, JSpinner spin, java.awt.event.ActionListener act) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 1));
        JButton b = makeBig(name);
        b.addActionListener(act);
        p.add(b);
        p.add(Box.createHorizontalStrut(2));
        p.add(spin);
        return p;
    }

    private JButton makeBig(String s) {
        JButton b = new JButton(s);
        b.setFont(b.getFont().deriveFont(Font.BOLD));
        return b;
    }

    private JPanel buildStatus() {
        JPanel p = new JPanel(new BorderLayout());
        fileLabel.setFont(fileLabel.getFont().deriveFont(Font.PLAIN, 11f));
        p.add(fileLabel, BorderLayout.WEST);
        p.add(status, BorderLayout.CENTER);
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        right.add(markerLabel);
        right.add(pointerLabel);
        p.add(right, BorderLayout.EAST);
        return p;
    }

    // ------------------------------------------------------------ menus

    private JMenuBar buildMenus() {
        JMenuBar bar = new JMenuBar();

        JMenu file = new JMenu("File");
        file.add(mi("Open...", KeyEvent.VK_O, InputEvent.CTRL_DOWN_MASK, e -> open()));
        file.add(mi("Save", KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK, e -> save()));
        file.add(mi("Save As...", 0, 0, e -> saveAs()));
        file.addSeparator();
        file.add(mi("Print", 0, 0, e -> JOptionPane.showMessageDialog(this,
                "Template printouts are not part of this re-implementation.")));
        file.addSeparator();
        file.add(mi("Exit", KeyEvent.VK_Q, InputEvent.CTRL_DOWN_MASK, e -> quit()));
        bar.add(file);

        JMenu edit = new JMenu("Edit");
        edit.add(mi("Undo", KeyEvent.VK_Z, InputEvent.CTRL_DOWN_MASK, e -> undo()));
        edit.addSeparator();
        edit.add(mi("Cut", KeyEvent.VK_X, InputEvent.CTRL_DOWN_MASK, e -> cut()));
        edit.add(mi("Copy", KeyEvent.VK_C, InputEvent.CTRL_DOWN_MASK, e -> copy()));
        edit.add(mi("Paste", KeyEvent.VK_V, InputEvent.CTRL_DOWN_MASK, e -> paste()));
        edit.addSeparator();
        edit.add(mi("Select all enabled", 0, 0, e -> selectAllEnabled()));
        edit.add(mi("Clear selection", 0, 0, e -> { song.clearSelection(); changed(); }));
        bar.add(edit);

        JMenu tools = new JMenu("Tools");
        tools.add(mi("Event lists...", 0, 0, e -> Dialogs.showEventLists(this, song,
                new Dialogs.EditCallback() {
                    @Override public void pushUndo() { snapshotUndo(); }
                    @Override public void changed() { changed(); }
                })));
        tools.add(mi("Percussion names...", 0, 0, e -> Dialogs.showPercussionPicker(this,
                note -> view.highlightNote(note))));
        bar.add(tools);

        JMenu help = new JMenu("Help");
        help.add(mi("About", 0, 0, e -> JOptionPane.showMessageDialog(this,
                "RollNote\n"
                + "A modern piano-roll style MIDI editor.\n\n"
                + "Forthrightly inspired by ROLLOOK (2002) by Johan Liljencrants;\n"
                + "we keep its look and editing model so longtime users feel at home.\n"
                + "G2.MID example by Julian Nott.")));
        bar.add(help);
        return bar;
    }

    private JMenuItem mi(String name, int key, int mod, java.awt.event.ActionListener a) {
        JMenuItem m = new JMenuItem(name);
        if (key != 0) m.setAccelerator(KeyStroke.getKeyStroke(key, mod));
        m.addActionListener(a);
        return m;
    }

    private void bindKeys(JPanel root) {
        InputMap im = root.getInputMap(javax.swing.JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = root.getActionMap();
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), "cut");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "esc");
        am.put("cut", new AbstractAction() { public void actionPerformed(ActionEvent e) { cut(); } });
        am.put("esc", new AbstractAction() { public void actionPerformed(ActionEvent e) { view.cancelGesture(); } });
    }

    // ------------------------------------------------------------ matrix

    /** The rows of squares to set the status of sequences or channels. */
    private final class MatrixPanel extends JPanel {
        private static final long serialVersionUID = 1L;
        private boolean channelMode = true;
        private int cells = 16;
        private final Color[] colors = {Color.GRAY, Color.BLACK, Color.RED};

        MatrixPanel() {
            setToolTipText("Click: enabled -> selected -> disabled -> enabled");
            addMouseListener(new java.awt.event.MouseAdapter() {
                @Override public void mouseClicked(java.awt.event.MouseEvent e) {
                    int cell = (e.getX() - 4) / 38;
                    if (cell < 0 || cell >= cells) return;
                    cycle(cell);
                }
            });
        }

        void setChannelMode(boolean ch) {
            channelMode = ch;
            repaint();
        }

        @Override protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, getWidth(), getHeight());
            for (int i = 0; i < cells; i++) {
                int x = 4 + i * 38;
                Color c = cellColor(i);
                g.setColor(c);
                g.fillRect(x, 3, 32, 18);
                g.setColor(Color.BLACK);
                g.drawRect(x, 3, 32, 18);
                g.setColor(Color.WHITE);
                String label = channelMode ? Integer.toHexString(i).toUpperCase() : Integer.toString(i);
                g.drawString(label, x + 12, 16);
            }
        }

        private Color cellColor(int i) {
            boolean anySel = false, anyEn = false;
            for (Note n : song.notes) {
                if (member(n, i)) {
                    if (n.selected) anySel = true;
                    if (n.enabled) anyEn = true;
                }
            }
            if (anySel) return colors[2];
            if (!anyEn) return colors[0];
            return colors[1];
        }

        private boolean member(Note n, int i) {
            return channelMode ? n.channel == i : n.track == i;
        }

        private void cycle(int i) {
            snapshotUndo();
            boolean anySel = false, anyEn = false;
            for (Note n : song.notes)
                if (member(n, i)) { if (n.selected) anySel = true; if (n.enabled) anyEn = true; }
            if (anySel) {                       // selected -> disabled
                for (Note n : song.notes)
                    if (member(n, i)) { n.selected = false; n.enabled = false; }
            } else if (!anyEn) {                // disabled -> enabled
                for (Note n : song.notes)
                    if (member(n, i)) n.enabled = true;
            } else {                            // enabled -> selected
                for (Note n : song.notes)
                    if (member(n, i) && n.enabled) n.selected = true;
            }
            changed();
        }
    }

    private int matrixCells() {
        return radioChannel.isSelected() ? 16 : Math.min(16, Math.max(1, song.trackCount()));
    }

    private void updateMatrixCount() {
        boolean ch = radioChannel.isSelected();
        int n = matrixCells();
        matrix.setChannelMode(ch);
        matrix.cells = n;
        matrix.setPreferredSize(new Dimension(n * 38 + 8, 24));
        matrix.repaint();
        revalidate();
    }

    private int spinChanInt() { return ((Number) spinChan.getValue()).intValue(); }
    private int spinSeqInt() { return ((Number) spinSeq.getValue()).intValue(); }
    private int spinVelInt() { return ((Number) spinVel.getValue()).intValue(); }
    private int spinTransInt() { return ((Number) spinTrans.getValue()).intValue(); }
    private long spinDurLong() { return ((Number) spinDur.getValue()).longValue(); }

    private void applyRes(int denom) {
        view.setQuantRes(Math.max(1, song.division / denom));
    }

    // ------------------------------------------------------------ ops

    private void bulkOp(java.util.function.Consumer<Note> op) {
        if (song.countSelectedNotes() == 0) {
            statusMessage("no notes selected");
            return;
        }
        snapshotUndo();
        for (Note n : song.notes) if (n.selected) op.accept(n);
        changed();
    }

    private static final boolean DEBUG = Boolean.getBoolean("rollnote.debug");

    private void debug(String s) { if (DEBUG) System.out.println("[dbg] " + s); }

    private void cut() {
        int n = song.countSelectedNotes();
        if (n == 0) return;
        debug("cut n=" + n);
        if (n > 2900)
            JOptionPane.showMessageDialog(this,
                    "More than ~2900 events selected: the operation cannot be undone.");
        else
            snapshotUndo();
        clipboard = new ArrayList<>();
        clipboardOrigin = Long.MAX_VALUE;
        List<Note> rem = new ArrayList<>();
        for (Note x : song.notes)
            if (x.selected) {
                clipboard.add(x.copy());
                clipboardOrigin = Math.min(clipboardOrigin, x.start);
                rem.add(x);
            }
        song.notes.removeAll(rem);
        changed();
        statusMessage("cut " + n + " note" + (n == 1 ? "" : "s"));
    }

    private void copy() {
        int n = song.countSelectedNotes();
        if (n == 0) return;
        clipboard = new ArrayList<>();
        clipboardOrigin = Long.MAX_VALUE;
        for (Note x : song.notes)
            if (x.selected) {
                clipboard.add(x.copy());
                clipboardOrigin = Math.min(clipboardOrigin, x.start);
            }
        statusMessage("copied " + n + " note" + (n == 1 ? "" : "s"));
    }

    private void paste() {
        if (clipboard.isEmpty()) return;
        if (clipboard.size() > 2900)
            JOptionPane.showMessageDialog(this, "Clipboard too large to be undone.");
        else
            snapshotUndo();
        song.clearSelection();
        long t0 = view.getMarker();
        for (Note c : clipboard) {
            Note n = c.copy();
            n.start = Math.max(0, t0 + (c.start - clipboardOrigin));
            n.selected = true;
            n.enabled = true;
            song.notes.add(n);
        }
        changed();
        statusMessage("pasted " + clipboard.size() + " note" + (clipboard.size() == 1 ? "" : "s"));
    }

    private void selectAllEnabled() {
        snapshotUndo();
        for (Note n : song.notes) if (n.enabled) n.selected = true;
        changed();
    }

    void snapshotUndo() {
        undoStack.push(song.copy());
        if (undoStack.size() > 80) undoStack.removeLast();
        redoStack.clear();
    }

    private void undo() {
        if (undoStack.isEmpty()) { statusMessage("nothing to undo"); debug("undo: nothing"); return; }
        redoStack.push(song);
        song = undoStack.pop();
        applySong();
        dirty = true;
        refreshAll();
        debug("undo done, notes=" + song.notes.size());
        statusMessage("undo");
    }

    // ------------------------------------------------------------ files

    private void open() {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Open MIDI file");
        fc.setFileFilter(new FileNameExtensionFilter("MIDI files (*.mid, *.midi)", "mid", "midi"));
        if (currentFile != null) fc.setSelectedFile(currentFile.toFile());
        if (fc.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;
        Path p = fc.getSelectedFile().toPath();
        if (!Files.isReadable(p)) { error("cannot read " + p); return; }
        try {
            Song s = Smf.read(Files.readAllBytes(p));
            song = s;
            currentFile = p;
            dirty = false;
            reloadSong("");
        } catch (Exception ex) {
            error("Could not load file: " + ex.getMessage());
        }
    }

    private void reloadSong(String note) {
        undoStack.clear();
        redoStack.clear();
        applySong();
        dirty = false;
        refreshAll();
        if (currentFile != null)
            setTitle("RollNote - " + currentFile.getFileName());
        statusMessage("loaded " + song.notes.size() + " notes" + note);
    }

    /** switch to the current song object without touching undo/redo history */
    private void applySong() {
        view.setSong(song);
        view.setPxPerQuarter(34.0);
        view.setQuantRes(Math.max(1, song.division / 8));
        view.setMarker(0);
        pen.setSelected(false);
        view.setPenMode(false);
        view.clearHighlightNote();
        player.stop();
        updateMatrixCount();
    }

    /** refresh model-derived UI without marking the file dirty */
    private void refreshAll() {
        view.refreshStats();
        matrix.repaint();
        updateStatus();
    }

    private boolean confirmOverwriteDisabled() {
        if (song.countEnabledNotes() == song.notes.size()) return true;
        return JOptionPane.showConfirmDialog(this,
                "Disabled notes will not be included in the written file. Continue?",
                "Save", JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION;
    }

    private void save() {
        if (currentFile == null) { saveAs(); return; }
        if (!confirmOverwriteDisabled()) return;
        try {
            int fmt = song.formatIn;
            byte[] b = Smf.write(song, fmt, false, false);
            Files.write(currentFile, b);
            dirty = false;
            statusMessage("saved " + currentFile.getFileName());
        } catch (Exception ex) {
            error("Could not save: " + ex.getMessage());
        }
    }

    private void saveAs() {
        int opt = Dialogs.showSaveAsOptions(this);
        if (opt < 0) return;
        if (!confirmOverwriteDisabled()) return;
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Save As");
        fc.setFileFilter(new FileNameExtensionFilter("MIDI files (*.mid)", "mid"));
        if (currentFile != null) fc.setSelectedFile(currentFile.toFile());
        if (fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        Path p = fc.getSelectedFile().toPath();
        if (!p.toString().toLowerCase().endsWith(".mid")) p = p.resolveSibling(p.getFileName() + ".mid");
        try {
            int fmt = opt == Dialogs.F0_KEEP || opt == Dialogs.F0_ZERO ? 0 : 1;
            boolean zero = opt == Dialogs.F0_ZERO;
            boolean byChan = opt == Dialogs.F1_BY_CHAN;
            Files.write(p, Smf.write(song, fmt, zero, byChan));
            currentFile = p;
            dirty = false;
            setTitle("RollNote - " + p.getFileName());
            statusMessage("saved " + p.getFileName());
        } catch (Exception ex) {
            error("Could not save: " + ex.getMessage());
        }
    }

    private void quit() {
        if (dirty && JOptionPane.showConfirmDialog(this,
                "Edited data has not been saved. Quit anyway?",
                "RollNote", JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION)
            return;
        player.stop();
        dispose();
        System.exit(0);
    }

    // ------------------------------------------------------------ playback

    private void play() {
        if (song.notes.isEmpty()) { statusMessage("nothing to play"); return; }
        double f = speedHalf.isSelected() ? 0.5 : speedQuarter.isSelected() ? 0.25 : 1.0;
        long from = view.getMarker();
        String err = player.play(song, from, f);
        if (err != null) error(err);
        else statusMessage("playing from " + song.barBeatTick(from));
    }

    // ------------------------------------------------------------ view callbacks

    @Override public void pushUndo() { snapshotUndo(); }

    @Override public void changed() {
        dirty = true;
        view.refreshStats();
        view.revalidate();     // let the roll grow when edits extend the song
        matrix.repaint();
        updateStatus();
        debug("changed: notes=" + song.notes.size() + " sel=" + song.countSelectedNotes());
    }

    @Override public void hover(String s) { status.setText(s); }

    @Override public void pointerHover(int note, long tick) {
        debug("pointerHover note=" + note + " tick=" + tick);
        if (note < 0) {
            pointerLabel.setText(" ");
            return;
        }
        String hex = String.format("%02X", note);
        pointerLabel.setText(NoteName2.name(note) + "  note " + note + " (0x" + hex + ")");
        pointerLabel.setToolTipText("t " + song.barBeatTick(tick));
    }

    @Override public void markerChanged(long tick) {
        markerLabel.setText("marker " + song.barBeatTick(tick));
    }

    // ------------------------------------------------------------ misc

    private void statusMessage(String s) { status.setText(s); }

    private void updateStatus() {
        String sel = song.countSelectedNotes() > 0
                ? song.countSelectedNotes() + " selected, " : "";
        fileLabel.setText((currentFile != null ? currentFile.getFileName().toString() : "no file")
                + (dirty ? " *" : "") + "   -   " + sel + song.notes.size() + " notes, "
                + song.trackCount() + " track" + (song.trackCount() == 1 ? "" : "s"));
        view.repaint();
    }

    private long viewTopVisible() {
        // approximate the top visible tick of the viewport
        JScrollPane sp = (JScrollPane) view.getParent().getParent();
        return (long) ((sp.getVerticalScrollBar().getValue() - RollView.TOP_H)
                / Math.max(1e-9, view.pxPerTick()));
    }

    private void ensureVisible(long tick) {
        JScrollPane sp = (JScrollPane) view.getParent().getParent();
        int y = RollView.TOP_H + (int) (tick * view.pxPerTick());
        java.awt.Rectangle r = view.getVisibleRect();
        if (y < r.y || y > r.y + r.height - 40) {
            view.scrollRectToVisible(new java.awt.Rectangle(0, y, 1, 1));
        }
    }

    private void scrollToStart() {
        view.scrollRectToVisible(new java.awt.Rectangle(0, 0, 1, 1));
    }

    private void error(String s) {
        JOptionPane.showMessageDialog(this, s, "RollNote", JOptionPane.ERROR_MESSAGE);
    }

    public void loadInitial(Path p) {
        try {
            song = Smf.read(Files.readAllBytes(p));
            currentFile = p;
            dirty = false;
            reloadSong("");
        } catch (Exception ex) {
            error("Could not load " + p + ": " + ex.getMessage());
        }
    }
}
