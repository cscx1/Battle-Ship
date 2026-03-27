import javax.sound.midi.*;

/**
 * Short MIDI bleeps so we don't depend on WAV files. Hit = brighter notes,
 * miss = lower; sink/win/lose use a tiny fanfare.
 */
final class SoundEffects {

    private static Synthesizer synth;

    static {
        try {
            synth = MidiSystem.getSynthesizer();
            synth.open();
        } catch (Exception e) {
            synth = null;
        }
    }

    private SoundEffects() {
    }

    static void play(Model.SoundCue cue) {
        if (cue == null || cue == Model.SoundCue.NONE || synth == null) {
            return;
        }
        new Thread(() -> playSync(cue), "midi-fx").start();
    }

    private static void playSync(Model.SoundCue cue) {
        try {
            MidiChannel[] channels = synth.getChannels();
            if (channels == null || channels.length == 0) {
                return;
            }
            MidiChannel ch = channels[0];
            switch (cue) {
                case HIT:
                    ch.programChange(0, 0);
                    ch.noteOn(72, 90);
                    sleep(90);
                    ch.noteOff(72);
                    ch.noteOn(76, 85);
                    sleep(70);
                    ch.noteOff(76);
                    break;
                case MISS:
                    ch.programChange(0, 6);
                    ch.noteOn(48, 70);
                    sleep(120);
                    ch.noteOff(48);
                    break;
                case SINK:
                    ch.programChange(0, 0);
                    for (int n : new int[] { 60, 64, 67, 72 }) {
                        ch.noteOn(n, 85);
                        sleep(55);
                        ch.noteOff(n);
                    }
                    break;
                case VICTORY:
                    for (int n : new int[] { 60, 64, 67, 72, 76 }) {
                        ch.noteOn(n, 95);
                        sleep(70);
                        ch.noteOff(n);
                    }
                    break;
                case DEFEAT:
                    for (int n : new int[] { 64, 60, 55, 48 }) {
                        ch.noteOn(n, 80);
                        sleep(90);
                        ch.noteOff(n);
                    }
                    break;
                default:
                    break;
            }
        } catch (Exception ignored) {
            // MIDI not available on some systems
        }
    }

    private static void sleep(int ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
