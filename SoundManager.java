import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.SourceDataLine;

/**
 * FEATURE: Sound effects (part of the User Interface Module).
 * No audio files are needed: every beep is generated in code as a sine wave
 * and played in its own small thread. If the computer has no sound device
 * the game simply stays silent.
 */
public final class SoundManager {
    private static final float RATE = 22050f;
    private static volatile boolean enabled = true;

    private SoundManager() { }

    public static boolean isEnabled()          { return enabled; }
    public static void setEnabled(boolean on)  { enabled = on; }

    public static void placeX()   { play(new int[]{520},  new int[]{70}); }
    public static void placeO()   { play(new int[]{390},  new int[]{70}); }
    public static void error()    { play(new int[]{170},  new int[]{170}); }
    public static void hint()     { play(new int[]{700, 900}, new int[]{70, 90}); }
    public static void undo()     { play(new int[]{500, 380}, new int[]{70, 90}); }
    public static void draw()     { play(new int[]{330, 262}, new int[]{150, 240}); }
    public static void win()      { play(new int[]{523, 659, 784, 1046}, new int[]{110, 110, 110, 280}); }
    public static void matchWin() {
        play(new int[]{523, 659, 784, 659, 784, 1046, 1318},
             new int[]{120, 120, 120, 120, 120, 160, 420});
    }

    private static void play(int[] frequencies, int[] millis) {
        if (!enabled) return;
        Thread t = new Thread(() -> {
            try {
                byte[] data = synthesize(frequencies, millis);
                AudioFormat format = new AudioFormat(RATE, 16, 1, true, false);
                try (SourceDataLine line = AudioSystem.getSourceDataLine(format)) {
                    line.open(format);
                    line.start();
                    line.write(data, 0, data.length);
                    line.drain();
                }
            } catch (Exception e) {
                // no sound device available -> ignore, the game still works
            }
        }, "Sound-Thread");
        t.setDaemon(true);
        t.start();
    }

    /** Turns a list of notes into 16-bit PCM bytes with a soft start/end so there are no clicks. */
    private static byte[] synthesize(int[] frequencies, int[] millis) {
        int total = 0;
        for (int ms : millis) total += (int) (RATE * ms / 1000);
        byte[] out = new byte[total * 2];
        int pos = 0;
        for (int n = 0; n < frequencies.length; n++) {
            int samples = (int) (RATE * millis[n] / 1000);
            for (int i = 0; i < samples; i++) {
                double fadeIn = Math.min(1.0, i / (RATE * 0.005));
                double fadeOut = Math.min(1.0, (samples - i) / (RATE * 0.03));
                double value = Math.sin(2 * Math.PI * frequencies[n] * i / RATE) * 0.35 * fadeIn * fadeOut;
                short s = (short) (value * Short.MAX_VALUE);
                out[pos++] = (byte) (s & 0xFF);
                out[pos++] = (byte) ((s >> 8) & 0xFF);
            }
        }
        return out;
    }
}
