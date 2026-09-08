package rollnote.ui;

import rollnote.core.CtrlEvent;
import rollnote.core.MetaEvent;
import rollnote.core.Note;
import rollnote.core.Song;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/** Helper dialogs: percussion/instrument pickers, save options. */
public final class Dialogs {

    /** result codes for save-as */
    public static final int F0_KEEP = 0, F0_ZERO = 1, F1_KEEP = 2, F1_BY_CHAN = 3;

    public static int showSaveAsOptions(JFrame parent) {
        String[] names = {
            "Write MIDI format 0, keep channel numbers",
            "Write MIDI format 0, zero channel numbers",
            "Write MIDI format 1, keep channel/track numbers",
            "Write MIDI format 1, set track numbers = channel numbers"
        };
        String[] desc = {
            "If multi-track (format 1) the tracks are merged; track associations are lost.",
            "Tracks are merged and every channel is set to 0; instrument definitions mix.",
            "Conservative: the current arrangement is kept as is.",
            "Splits a format 0 file into up to 16 tracks, one per channel."
        };
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        JComboBox<String> cb = new JComboBox<>(names);
        JLabel info = new JLabel(desc[0]);
        info.setFont(info.getFont().deriveFont(Font.PLAIN));
        cb.addActionListener(e -> info.setText(desc[cb.getSelectedIndex()]));
        p.add(cb);
        p.add(Box.createVerticalStrut(6));
        p.add(info);
        int r = JOptionPane.showConfirmDialog(parent, p, "Save As - formatting",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        return r == JOptionPane.OK_OPTION ? cb.getSelectedIndex() : -1;
    }

    public interface NotePicked {
        void pick(int note);
    }

    public static void showPercussionPicker(JFrame parent, NotePicked picked) {
        JDialog d = new JDialog(parent, "Percussion (channel 9)", true);
        DefaultListModel<String> m = new DefaultListModel<>();
        for (int i = 0; i < 128; i++) {
            String name = Gm.percussionName(i);
            boolean isPerc = Gm.PERCUSSION[i] != null;
            m.addElement(String.format("%3d  %s%s", i,
                    isPerc ? name : NoteName2.name(i), isPerc ? "" : "  (not GM percussion)"));
        }
        JList<String> list = new JList<>(m);
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 1) {
                    int i = list.getSelectedIndex();
                    if (i >= 0) picked.pick(i);
                }
            }
        });
        d.add(new JScrollPane(list), BorderLayout.CENTER);
        JButton close = new JButton("Close");
        close.addActionListener(e -> d.dispose());
        JPanel south = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        south.add(close);
        d.add(south, BorderLayout.SOUTH);
        d.setSize(420, 560);
        d.setLocationRelativeTo(parent);
        d.setVisible(true);
    }

    public static void showInstrumentPicker(JFrame parent, int program, PickedInstrument cb) {
        JDialog d = new JDialog(parent, "Select instrument", true);
        DefaultListModel<String> m = new DefaultListModel<>();
        for (int i = 0; i < 128; i++)
            m.addElement(String.format("%3d  %s", i, Gm.PROGRAMS[i]));
        JList<String> list = new JList<>(m);
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setSelectedIndex(Math.max(0, Math.min(127, program)));
        JButton ok = new JButton("OK");
        ok.addActionListener(e -> {
            cb.picked(list.getSelectedIndex());
            d.dispose();
        });
        JButton can = new JButton("Cancel");
        can.addActionListener(e -> d.dispose());
        JPanel south = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        south.add(ok);
        south.add(can);
        d.add(new JScrollPane(list), BorderLayout.CENTER);
        d.add(south, BorderLayout.SOUTH);
        d.setSize(360, 500);
        d.setLocationRelativeTo(parent);
        d.setVisible(true);
    }

    public interface PickedInstrument { void picked(int program); }

    // ------------------------------------------------------------- event lists

    public interface EditCallback {
        void pushUndo();
        void changed();
    }

    public static void showEventLists(JFrame parent, Song song, EditCallback cb) {
        JDialog d = new JDialog(parent, "Event lists", false);
        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Note & control events", makeEventListPanel(d, song, cb));
        tabs.addTab("Meta events", makeMetaListPanel(d, song, cb));
        d.add(tabs);
        d.setSize(760, 480);
        d.setLocationRelativeTo(parent);
        d.setVisible(true);
    }

    private static JPanel makeEventListPanel(JDialog owner, Song song, EditCallback cb) {
        DefaultListModel<String> m = new DefaultListModel<>();
        JPanel pan = new JPanel(new BorderLayout());
        JLabel head = new JLabel(" ");
        head.setBorder(new EmptyBorder(2, 4, 2, 4));
        JList<String> list = new JList<>(m);
        refreshList(song, m, head);
        JButton edit = new JButton("Edit...");
        JButton del = new JButton("Delete");
        JButton close = new JButton("Close");
        close.addActionListener(e -> owner.dispose());
        edit.addActionListener(e -> {
            int i = list.getSelectedIndex();
            if (i < 0) return;
            cb.pushUndo();
            int before = itemCount(song);
            Object it = itemAt(song, i);
            boolean removed = editItem(owner, song, it);
            if (removed) removeItem(song, it);
            cb.changed();
            refreshList(song, m, head);
            if (before != itemCount(song)) { /* keep simple */ }
        });
        del.addActionListener(e -> {
            int i = list.getSelectedIndex();
            if (i < 0) return;
            cb.pushUndo();
            removeItem(song, itemAt(song, i));
            cb.changed();
            refreshList(song, m, head);
        });
        JPanel south = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        south.add(edit); south.add(del); south.add(close);
        pan.add(head, BorderLayout.NORTH);
        pan.add(new JScrollPane(list), BorderLayout.CENTER);
        pan.add(south, BorderLayout.SOUTH);
        return pan;
    }

    private static JPanel makeMetaListPanel(JDialog owner, Song song, EditCallback cb) {
        DefaultListModel<String> m = new DefaultListModel<>();
        JPanel pan = new JPanel(new BorderLayout());
        JLabel head = new JLabel(" ");
        head.setBorder(new EmptyBorder(2, 4, 2, 4));
        JList<String> list = new JList<>(m);
        refreshMeta(song, m, head);
        JButton edit = new JButton("Edit...");
        JButton del = new JButton("Delete");
        JButton close = new JButton("Close");
        close.addActionListener(e -> owner.dispose());
        edit.addActionListener(e -> {
            int i = list.getSelectedIndex();
            if (i < 0) return;
            cb.pushUndo();
            MetaEvent it = metas(song).get(i);
            editMeta(owner, song, it);
            cb.changed();
            refreshMeta(song, m, head);
        });
        del.addActionListener(e -> {
            int i = list.getSelectedIndex();
            if (i < 0) return;
            cb.pushUndo();
            song.metas.remove(i);
            cb.changed();
            refreshMeta(song, m, head);
        });
        JPanel south = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        south.add(edit); south.add(del); south.add(close);
        pan.add(head, BorderLayout.NORTH);
        pan.add(new JScrollPane(list), BorderLayout.CENTER);
        pan.add(south, BorderLayout.SOUTH);
        return pan;
    }

    private static void refreshList(Song s, DefaultListModel<String> m, JLabel head) {
        m.clear();
        List<Object> items = items(s);
        for (Object o : items) {
            if (o instanceof Note)
                m.addElement(String.format("N  tr%d ch%02d %s dur %d  note %s vel %d",
                        ((Note) o).track, ((Note) o).channel, s.barBeatTick(((Note) o).start),
                        ((Note) o).dur, NoteName2.name(((Note) o).note), ((Note) o).velocity));
            else if (o instanceof CtrlEvent) {
                CtrlEvent c = (CtrlEvent) o;
                m.addElement(String.format("C  tr%d ch%02d %s code %c d1 %d d2 %s",
                        c.track, c.channel, s.barBeatTick(c.time), c.code(), c.d1,
                        c.d2 < 0 ? "-" : Integer.toString(c.d2)));
            }
        }
        head.setText(items.size() + " note/control events");
    }

    private static void refreshMeta(Song s, DefaultListModel<String> m, JLabel head) {
        m.clear();
        for (MetaEvent x : s.metas) {
            m.addElement(String.format("M  tr%d %s  %s", x.track, s.barBeatTick(x.time), x.display()));
        }
        head.setText(s.metas.size() + " meta events");
    }

    private static List<Object> items(Song s) {
        List<Object> out = new ArrayList<>(s.notes.size() + s.ctrls.size());
        out.addAll(s.notes);
        out.addAll(s.ctrls);
        return out;
    }
    private static int itemCount(Song s) { return s.notes.size() + s.ctrls.size(); }
    private static Object itemAt(Song s, int i) {
        if (i < s.notes.size()) return s.notes.get(i);
        return s.ctrls.get(i - s.notes.size());
    }
    private static void removeItem(Song s, Object o) {
        if (o instanceof Note) s.notes.remove(o);
        else s.ctrls.remove(o);
    }
    private static List<MetaEvent> metas(Song s) { return s.metas; }

    // ------------------------------------------------------------- editors

    /** edit a Note or CtrlEvent. returns true if the original should be deleted (replace) */
    private static boolean editItem(java.awt.Component owner, Song song, Object item) {
        if (item instanceof Note) {
            Note n = (Note) item;
            JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 2));
            SpinnerNumberModel sm = new SpinnerNumberModel(n.track, 0, 255, 1);
            SpinnerNumberModel cm = new SpinnerNumberModel(n.channel, 0, 15, 1);
            SpinnerNumberModel nm = new SpinnerNumberModel(n.note, 0, 127, 1);
            SpinnerNumberModel vm = new SpinnerNumberModel(n.velocity, 0, 127, 1);
            JSpinner track = new JSpinner(sm);
            JSpinner chan = new JSpinner(cm);
            JSpinner note = new JSpinner(nm);
            JSpinner vel = new JSpinner(vm);
            JLabel noteName = new JLabel(" " + NoteName2.name(n.note) + " ");
            note.addChangeListener(e -> noteName.setText(" " + NoteName2.name(((Number) note.getValue()).intValue()) + " "));
            JTextArea time = new JTextArea(1, 12);
            time.setText(song.barBeatTick(n.start));
            JTextArea dur = new JTextArea(1, 8);
            dur.setText(Long.toString(n.dur));
            addRow(p, "Track", track); addRow(p, "Channel", chan);
            p.add(new JLabel("Note")); p.add(note); p.add(noteName);
            p.add(new JLabel("Vel")); p.add(vel);
            p.add(new JLabel("Time (b:b:t)")); p.add(time);
            p.add(new JLabel("Dur (ticks)")); p.add(dur);
            String[] opts = {"Cancel", "Replace", "Insert"};
            int r = JOptionPane.showOptionDialog(owner, p, "Edit note event",
                    JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE, null, opts, opts[1]);
            if (r <= 0) return false;
            try {
                Note add = n.copy();
                add.track = ((Number) track.getValue()).intValue();
                add.channel = ((Number) chan.getValue()).intValue();
                add.note = ((Number) note.getValue()).intValue();
                add.velocity = ((Number) vel.getValue()).intValue();
                add.start = Math.max(0, song.parseBarBeatTick(time.getText()));
                add.dur = Math.max(0, Long.parseLong(dur.getText().trim()));
                add.selected = true;
                song.notes.add(add);
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(owner, "Bad number: " + ex.getMessage());
                return false;
            }
            return r == 1; // replace removes the original, insert keeps it
        } else {
            CtrlEvent c = (CtrlEvent) item;
            JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 2));
            JSpinner track = new JSpinner(new SpinnerNumberModel(c.track, 0, 255, 1));
            JSpinner chan = new JSpinner(new SpinnerNumberModel(c.channel, 0, 15, 1));
            JSpinner d1 = new JSpinner(new SpinnerNumberModel(c.d1, 0, 127, 1));
            JSpinner d2 = new JSpinner(new SpinnerNumberModel(Math.max(0, c.d2), 0, 127, 1));
            JComboBox<String> code = new JComboBox<>(new String[]{"B control", "C program", "D pressure", "E pitch bend", "A poly AT"});
            int cur = (c.status & 0xF0) == 0xC0 ? 1 : (c.status & 0xF0) == 0xD0 ? 2
                    : (c.status & 0xF0) == 0xE0 ? 3 : (c.status & 0xF0) == 0xA0 ? 4 : 0;
            code.setSelectedIndex(cur);
            code.addActionListener(e -> d2.setEnabled(code.getSelectedIndex() != 1 && code.getSelectedIndex() != 2));
            JTextArea time = new JTextArea(1, 12);
            time.setText(song.barBeatTick(c.time));
            addRow(p, "Track", track); addRow(p, "Channel", chan); addRow(p, "Code", code);
            addRow(p, "D1", d1); addRow(p, "D2", d2);
            p.add(new JLabel("Time (b:b:t)")); p.add(time);
            String[] opts = {"Cancel", "Replace", "Insert"};
            int r = JOptionPane.showOptionDialog(owner, p, "Edit control event",
                    JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE, null, opts, opts[1]);
            if (r <= 0) return false;
            CtrlEvent add = c.copy();
            add.track = ((Number) track.getValue()).intValue();
            add.channel = ((Number) chan.getValue()).intValue();
            int c2 = code.getSelectedIndex();
            int hi = c2 == 1 ? 0xC0 : c2 == 2 ? 0xD0 : c2 == 3 ? 0xE0 : c2 == 4 ? 0xA0 : 0xB0;
            add.status = hi | add.channel;
            add.d1 = ((Number) d1.getValue()).intValue();
            add.d2 = (hi == 0xC0 || hi == 0xD0) ? -1 : ((Number) d2.getValue()).intValue();
            try {
                add.time = Math.max(0, song.parseBarBeatTick(time.getText()));
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(owner, "Bad time: " + ex.getMessage());
                return false;
            }
            song.ctrls.add(add);
            return r == 1;
        }
    }

    private static void editMeta(java.awt.Component owner, Song song, MetaEvent item) {
        JPanel p = new JPanel(new BorderLayout(6, 6));
        JPanel fields = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 2));
        JSpinner track = new JSpinner(new SpinnerNumberModel(item.track, 0, 255, 1));
        JTextArea time = new JTextArea(1, 12);
        time.setText(song.barBeatTick(item.time));
        addRow(fields, "Track", track);
        fields.add(new JLabel("Time (b:b:t)")); fields.add(time);
        p.add(fields, BorderLayout.NORTH);
        JTextArea data = new JTextArea(6, 56);
        data.setLineWrap(true);
        data.setText(new String(item.data, StandardCharsets.ISO_8859_1));
        JLabel hint = new JLabel("Text-like meta data. Type " + item.typeName() + ".");
        p.add(hint, BorderLayout.CENTER);
        p.add(new JScrollPane(data), BorderLayout.CENTER);
        String[] opts = {"Cancel", "Replace", "Insert"};
        int r = JOptionPane.showOptionDialog(owner, p, "Edit meta event",
                JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE, null, opts, opts[1]);
        if (r <= 0) return;
        MetaEvent add = item.copy();
        add.track = ((Number) track.getValue()).intValue();
        try {
            add.time = Math.max(0, song.parseBarBeatTick(time.getText()));
            add.data = data.getText().getBytes(StandardCharsets.ISO_8859_1);
            song.metas.add(add);
            if (r == 1) song.metas.remove(item);   // replace
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(owner, "Bad time: " + ex.getMessage());
        }
    }

    private static void addRow(JPanel p, String name, java.awt.Component comp) {
        p.add(new JLabel(name));
        p.add(comp);
    }

    /** small helper: fixed-size text list dialog is unnecessary */
    public static JList<String> simpleList(String title, java.util.List<String> rows) {
        DefaultListModel<String> m = new DefaultListModel<>();
        for (String s : rows) m.addElement(s);
        JList<String> l = new JList<>(m);
        l.setVisibleRowCount(Math.min(rows.size(), 10));
        return l;
    }
}
