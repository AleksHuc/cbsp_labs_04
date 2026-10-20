package cbsp.lab04;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.sound.sampled.*;
/** Prepared I/O: signed 16-bit little-endian mono PCM WAV, no resampling or normalization. */
public final class AudioFiles {
    public record AudioData(double[] samples, int sampleRate) { }
    private AudioFiles() { }
    public static AudioData read(Path path) throws IOException, UnsupportedAudioFileException {
        try (AudioInputStream stream = AudioSystem.getAudioInputStream(path.toFile())) {
            AudioFormat f = stream.getFormat();
            if (!f.getEncoding().equals(AudioFormat.Encoding.PCM_SIGNED) || f.getSampleSizeInBits() != 16
                    || f.getChannels() != 1 || f.isBigEndian() || f.getFrameSize() != 2
                    || !Float.isFinite(f.getSampleRate()) || f.getSampleRate() <= 0
                    || f.getSampleRate() != Math.rint(f.getSampleRate()) || f.getSampleRate() > Integer.MAX_VALUE)
                throw new IllegalArgumentException("Expected signed 16-bit little-endian mono PCM WAV.");
            byte[] bytes = stream.readAllBytes();
            if (bytes.length == 0 || bytes.length % 2 != 0) throw new IOException("Empty or incomplete PCM data.");
            double[] samples = new double[bytes.length / 2];
            for (int n = 0; n < samples.length; n++) {
                short value = (short)((bytes[2*n] & 255) | ((bytes[2*n+1] & 255) << 8));
                samples[n] = value / 32768.0;
            }
            return new AudioData(samples, (int)f.getSampleRate());
        }
    }
    public static void write(Path path, double[] samples, int sampleRate) throws IOException {
        Validation.samples(samples);
        if (sampleRate <= 0) throw new IllegalArgumentException("Positive sample rate required.");
        if (samples.length > Integer.MAX_VALUE / 2) throw new IllegalArgumentException("PCM buffer too large.");
        for (double v : samples) if (v < -1 || v > 1) throw new IllegalArgumentException("PCM samples outside [-1,1].");
        byte[] bytes = new byte[samples.length * 2];
        for (int n = 0; n < samples.length; n++) {
            int pcm = (int)Math.max(-32768, Math.min(32767, Math.round(samples[n] * 32768.0)));
            bytes[2*n] = (byte)pcm; bytes[2*n+1] = (byte)(pcm >> 8);
        }
        Path parent = path.toAbsolutePath().getParent();
        if (parent != null) Files.createDirectories(parent);
        try (AudioInputStream stream = new AudioInputStream(new ByteArrayInputStream(bytes),
                new AudioFormat(sampleRate, 16, 1, true, false), samples.length)) {
            AudioSystem.write(stream, AudioFileFormat.Type.WAVE, path.toFile());
        }
    }
}
