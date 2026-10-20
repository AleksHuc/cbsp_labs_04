package cbsp.lab04;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;

/** Prepared playback infrastructure. */
public final class AudioOutput {
    private AudioOutput() { }

    /** Play signed 16-bit, little-endian mono PCM without writing a file. */
    public static void play(double[] samples, int sampleRate) throws LineUnavailableException {
        if (sampleRate <= 0 || samples == null || samples.length == 0) {
            throw new IllegalArgumentException("Playback requires samples and a positive sample rate.");
        }
        for (double sample : samples) {
            if (!Double.isFinite(sample) || Math.abs(sample) > 1.0) {
                throw new IllegalArgumentException("Playback samples must be finite and within [-1, 1].");
            }
        }
        AudioFormat format = new AudioFormat(sampleRate, 16, 1, true, false);
        SourceDataLine line;
        try {
            line = AudioSystem.getSourceDataLine(format);
        } catch (IllegalArgumentException e) {
            LineUnavailableException unavailable =
                    new LineUnavailableException(
                            "No audio output supports the requested PCM format.");
            unavailable.initCause(e);
            throw unavailable;
        }
        try {
            line.open(format);
            line.start();
            byte[] bytes = new byte[8192];
            int position = 0;
            while (position < samples.length) {
                int count = Math.min(bytes.length / 2, samples.length - position);
                for (int i = 0; i < count; i++) {
                    int pcm = (int) Math.round(samples[position + i] * 32767.0);
                    bytes[2 * i] = (byte) pcm;
                    bytes[2 * i + 1] = (byte) (pcm >> 8);
                }
                int offset = 0;
                while (offset < count * 2) {
                    offset += line.write(bytes, offset, count * 2 - offset);
                }
                position += count;
            }
            line.drain();
        } finally {
            line.close();
        }
    }
}
