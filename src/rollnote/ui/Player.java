package rollnote.ui;

import rollnote.core.CtrlEvent;
import rollnote.core.MetaEvent;
import rollnote.core.Note;
import rollnote.core.Smf;
import rollnote.core.Song;

import javax.sound.midi.MidiSystem;
import javax.sound.midi.Sequence;
import javax.sound.midi.Sequencer;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Synthesizer;
import javax.swing.Timer;
import java.io.ByteArrayInputStream;

/**
 * Playback through the Java MIDI sequencer. A temporary song is built the way
 * the original does: only enabled notes from the marker time onward; control
 * and meta events before the marker are moved to time zero (so the right
 * instruments are set up), and only events of enabled channels are included.
 */
public final class Player {
    public interface Listener {
        void playTick(long originalTick);
        void stopped();
    }

    private Sequencer seq;
    private Synthesizer synth;
    private Timer timer;
    private long baseTick;   // original song tick that maps to slice tick 0
    private final Listener listener;

    public Player(Listener listener) { this.listener = listener; }

    public boolean isPlaying() { return seq != null && seq.isRunning(); }

    /** @return null on success, error text on failure */
    public String play(Song song, long fromTick, double speedFactor) {
        stop();
        try {
            byte[] bytes = Smf.write(slice(song, fromTick), 1, false, false);
            Sequence sequence = MidiSystem.getSequence(new ByteArrayInputStream(bytes));

            // open a synthesizer (Gervill GM soft synth on this system)
            Synthesizer sy = MidiSystem.getSynthesizer();
            sy.open();
            // make sure volume controller 7 is at max so the notes are audible
            try {
                sy.getReceiver().send(new ShortMessage(ShortMessage.CONTROL_CHANGE, 0, 7, 127), -1);
            } catch (Exception ignored) {}

            Sequencer s = MidiSystem.getSequencer(false);
            s.open();
            s.getTransmitter().setReceiver(sy.getReceiver());
            s.setSequence(sequence);
            s.setTempoFactor((float) speedFactor);
            baseTick = fromTick;
            seq = s;
            synth = sy;
            s.start();
            timer = new Timer(80, e -> {
                if (seq == null || !seq.isRunning()) {
                    stop();
                    return;
                }
                listener.playTick(baseTick + seq.getTickPosition());
            });
            timer.start();
            return null;
        } catch (Exception ex) {
            stop();
            return "Playback failed: " + ex.getMessage();
        }
    }

    public void stop() {
        if (timer != null) { timer.stop(); timer = null; }
        if (seq != null) {
            try { seq.stop(); seq.close(); } catch (Exception ignored) {}
            seq = null;
        }
        if (synth != null) {
            try { synth.close(); } catch (Exception ignored) {}
            synth = null;
        }
        listener.stopped();
    }

    private static Song slice(Song song, long from) {
        Song s = new Song();
        s.division = song.division;
        s.formatIn = 1;
        s.tempoMicros = song.tempoMicros;
        boolean[] chanUsed = new boolean[16];
        for (Note n : song.notes)
            if (n.enabled) chanUsed[n.channel] = true;

        for (Note n : song.notes) {
            if (!n.enabled) continue;
            long start = Math.max(n.start, from);
            long end = n.end();
            if (end <= from) continue;
            s.notes.add(new Note(n.note, n.velocity, start - from, end - start, n.channel, n.track));
        }
        for (CtrlEvent c : song.ctrls) {
            if (!chanUsed[c.channel]) continue;
            long t = c.time >= from ? c.time - from : 0;
            s.ctrls.add(new CtrlEvent(c.track, c.channel, t, c.status, c.d1, c.d2));
        }
        boolean hasTempo = false;
        for (MetaEvent m : song.metas) {
            if (m.type == 0x51) hasTempo = true;
            long t = m.time >= from ? m.time - from : 0;
            s.metas.add(new MetaEvent(m.track, t, m.type, m.data.clone()));
        }
        if (!hasTempo) {
            byte[] d = new byte[]{(byte) (song.tempoMicros >> 16), (byte) (song.tempoMicros >> 8), (byte) song.tempoMicros};
            s.metas.add(new MetaEvent(0, 0, 0x51, d));
        }
        return s;
    }
}
