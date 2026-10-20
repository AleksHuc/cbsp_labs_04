package cbsp.lab04;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Comparator;

/** Integration checks exercise JSyn's real sample reader, filters and WAV recorder without a device. */
public final class JsynChecks {
    private JsynChecks() { }
    public static void main(String[] args) throws Exception {
        Path temp = Files.createTempDirectory("lab04-jsyn-");
        try {
            int fs = JsynAudio.SAMPLE_RATE;
            // Deliberately not a multiple of JSyn's block size or recorder's 1024-frame buffer.
            double[] x = TestSignals.mixture(fs+17, fs, 100, 440, 880);
            x[0] = 0.25; x[x.length-1] = -0.25;
            Path input = temp.resolve("input.wav");
            AudioFiles.write(input, x, fs);
            double[] decoded = AudioFiles.read(input).samples();
            double[][] frequencies = {{440}, {440}, {440}, {50, 100, 156, 220, 311, 440, 880}};
            double[][] qs = {{1}, {1}, {10}, {0.5, 0.5, 1.3, 1.42, 1.41, 0.77, 0.5}};
            double[][] gains = {{0}, {-6}, {}, {0, 6, 0, 0, 0, -6, 6}};
            JsynAudio.FilterType[] types = {JsynAudio.FilterType.PEAKING_EQ, JsynAudio.FilterType.PEAKING_EQ,
                    JsynAudio.FilterType.BAND_STOP, JsynAudio.FilterType.PEAKING_EQ};
            for (int test = 0; test < frequencies.length; test++) {
                double scale = 0.4;
                Path output = temp.resolve("output-"+test+".wav");
                JsynAudio.record(input, output, types[test], frequencies[test], qs[test], gains[test], scale, false);
                AudioFiles.AudioData result = AudioFiles.read(output);
                double[] padded = Arrays.copyOf(decoded, decoded.length+JsynAudio.TAIL_FRAMES);
                if (result.sampleRate() != fs || result.samples().length != padded.length)
                    throw new AssertionError("Recording format, final frames or decay tail incorrect.");
                Coefficients[] reference = new Coefficients[frequencies[test].length];
                for (int i = 0; i < reference.length; i++)
                    reference[i] = types[test] == JsynAudio.FilterType.BAND_STOP
                            ? FrequencyFilters.notch(fs, frequencies[test][i], qs[test][i])
                            : FrequencyFilters.peaking(fs, frequencies[test][i], qs[test][i], gains[test][i]);
                double[] expected = FrequencyFilters.cascade(padded, reference);
                double maxError = 0;
                for (int n = 0; n < expected.length; n++) {
                    double error = Math.abs(result.samples()[n]-expected[n]*scale);
                    if (!Double.isFinite(error)) throw new AssertionError("Non-finite recorded sample.");
                    maxError = Math.max(maxError, error);
                }
                if (maxError > 1e-4) throw new AssertionError("JSyn mismatch: "+maxError);
                System.out.printf("PASS JSyn case %d: %d frames, max error %.3g%n", test, expected.length, maxError);
            }
            if (!Arrays.equals(decoded, AudioFiles.read(input).samples()))
                throw new AssertionError("JSyn modified the source file.");
            System.out.println("PASS: JSyn identity, peaking cut, 440 Hz band-stop, seven-band cascade, exact duration, alignment and tail.");
        } finally {
            try (var paths = Files.walk(temp)) {
                for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) Files.delete(path);
            }
        }
    }
}
