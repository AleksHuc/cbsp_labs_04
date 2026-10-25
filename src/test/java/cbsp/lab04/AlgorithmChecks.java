package cbsp.lab04;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Random;

/** Explicit executable checks, independent of Java's -ea assertion flag. */
public final class AlgorithmChecks {
    private static int checks;
    private static void close(double actual, double expected, double tolerance) {
        checks++;
        if (!Double.isFinite(actual) || Math.abs(actual-expected) > tolerance)
            throw new AssertionError("Expected "+expected+", got "+actual+", tolerance "+tolerance);
    }
    private static void require(boolean condition) {
        checks++;
        if (!condition) throw new AssertionError("Condition failed");
    }
    public static void main(String[] args) throws Exception {
        fftAgainstDirectDft(); spectra(); filtering(); fftFiltering(); parametersAndAudio();
        System.out.println("PASS: "+checks+" numerical and integration checks");
    }
    private static void fftAgainstDirectDft() {
        Random random = new Random(42);
        for (int n : new int[]{2, 4, 8, 32, 128}) {
            double[] re = new double[n], im = new double[n];
            for (int i = 0; i < n; i++) { re[i] = random.nextDouble()-0.5; im[i] = random.nextDouble()-0.5; }
            double[] originalRe = re.clone(), originalIm = im.clone();
            Fft.forward(re, im);
            for (int k = 0; k < n; k++) {
                double dr = 0, di = 0;
                for (int j = 0; j < n; j++) {
                    double angle = -2*Math.PI*j*k/n;
                    dr += originalRe[j]*Math.cos(angle)-originalIm[j]*Math.sin(angle);
                    di += originalRe[j]*Math.sin(angle)+originalIm[j]*Math.cos(angle);
                }
                close(re[k], dr, 1e-10); close(im[k], di, 1e-10);
            }
            Fft.inverse(re, im);
            for (int i = 0; i < n; i++) {
                close(re[i], originalRe[i], 1e-12);
                close(im[i], originalIm[i], 1e-12);
            }
        }
    }
    private static void fftFiltering() {
        int n = 1024, fs = 8192;
        double[] x = new double[n], expected = new double[n];
        for (int i = 0; i < n; i++) {
            expected[i] = 0.1 + 0.2*Math.sin(2*Math.PI*640*i/fs) + 0.05*(i%2 == 0 ? 1 : -1);
            x[i] = expected[i] + 0.3*Math.sin(2*Math.PI*440*i/fs);
        }
        double[] copy = x.clone();
        // Exact bin tones independently check rejection, preservation, DC, Nyquist and scaling.
        double[] y = FrequencyFilters.fftBandStop(x, fs, 435, 445);
        for (int i = 0; i < n; i++) close(y[i], expected[i], 1e-11);
        require(Arrays.equals(x, copy));
        // Inclusive edges: 440 Hz is exactly on a bin in this fixture.
        y = FrequencyFilters.fftBandStop(x, fs, 440, 440);
        for (int i = 0; i < n; i++) close(y[i], expected[i], 1e-11);
        // No bins lie in [1,2] Hz; exercises zero padding and cropping for arbitrary lengths.
        double[] shortInput = Arrays.copyOf(x, 777);
        y = FrequencyFilters.fftBandStop(shortInput, fs, 1, 2);
        require(y.length == shortInput.length && y != shortInput);
        for (int i = 0; i < y.length; i++) close(y[i], shortInput[i], 1e-11);
        require(FrequencyFilters.fftBandStop(new double[0], fs, 435, 445).length == 0);
    }
    private static void spectra() {
        int n = 1024, fs = 8192;
        double[] x = new double[n];
        for (int i = 0; i < n; i++) x[i] = 0.3+0.2*Math.sin(2*Math.PI*80*i/n)+0.1*(i%2 == 0 ? 1 : -1);
        double[] copy = x.clone();
        for (boolean hann : new boolean[]{false, true}) {
            Spectrum s = FrequencyFilters.analyze(x, fs, hann);
            require(s.amplitude().length == n/2+1);
            close(s.frequencyHz()[80], 640, 0);
            close(s.amplitude()[0], 0.3, 1e-12);
            close(s.amplitude()[80], 0.2, 1e-12);
            close(s.amplitude()[n/2], 0.1, 1e-12);
        }
        require(Arrays.equals(x, copy));
        close(Spectrum.db(0), -240, 0); close(Spectrum.db(0.1), -20, 1e-12);
        // Off-bin leakage far from the main lobe should decrease with Hann.
        double[] off = TestSignals.mixture(n, fs, 80.5*fs/n);
        Spectrum rect = FrequencyFilters.analyze(off, fs, false), hann = FrequencyFilters.analyze(off, fs, true);
        double r = 0, h = 0;
        for (int k = 0; k <= n/2; k++) if (Math.abs(k-80.5) > 4) {
            r += rect.amplitude()[k]*rect.amplitude()[k]; h += hann.amplitude()[k]*hann.amplitude()[k];
        }
        require(h < r/100);
    }
    private static void filtering() {
        int fs = 44100;
        Coefficients identity = new Coefficients(1, 0, 0, 0, 0);
        double[] impulse = {1, 0, 0, 0};
        double[] hand = FrequencyFilters.process(impulse, new Coefficients(0.5, 0.25, 0.125, -0.5, 0.25));
        for (int i = 0; i < 4; i++) close(hand[i], new double[]{0.5, 0.5, 0.25, 0}[i], 0);
        require(Arrays.equals(FrequencyFilters.process(impulse, identity), impulse));
        double[] emptyCascade = FrequencyFilters.cascade(impulse, new Coefficients[0]);
        require(emptyCascade != impulse && Arrays.equals(emptyCascade, impulse));
        require(FrequencyFilters.process(new double[0], identity).length == 0);
        Coefficients lp = FrequencyFilters.lowPass(fs, 500, 1/Math.sqrt(2));
        Coefficients hp = FrequencyFilters.highPass(fs, 500, 1/Math.sqrt(2));
        require(Arrays.equals(FrequencyFilters.cascade(impulse, new Coefficients[]{lp}),
                FrequencyFilters.process(impulse, lp)));
        close(Diagnostics.magnitude(lp, 0, fs), 1, 1e-12);
        close(Diagnostics.magnitude(lp, fs/2.0, fs), 0, 1e-12);
        close(Diagnostics.magnitude(hp, 0, fs), 0, 1e-12);
        close(Diagnostics.magnitude(hp, fs/2.0, fs), 1, 1e-12);
        close(Diagnostics.magnitude(lp, 500, fs), 1/Math.sqrt(2), 1e-12);
        close(Diagnostics.magnitude(hp, 500, fs), 1/Math.sqrt(2), 1e-12);
        Coefficients notch = FrequencyFilters.notch(fs, 880, 10);
        close(Diagnostics.magnitude(notch, 880, fs), 0, 1e-10);
        require(Diagnostics.magnitude(FrequencyFilters.notch(fs, 880, 1), 1000, fs)
                < Diagnostics.magnitude(notch, 1000, fs));
        for (double q : new double[]{0.5, 1, 10}) for (double f : new double[]{50, 440, 1400, 3000}) {
            Coefficients boost = FrequencyFilters.peaking(fs, f, q, 6);
            Coefficients cut = FrequencyFilters.peaking(fs, f, q, -6);
            close(Spectrum.db(Diagnostics.magnitude(boost, f, fs)), 6, 1e-8);
            close(Spectrum.db(Diagnostics.magnitude(cut, f, fs)), -6, 1e-8);
            double[] x = TestSignals.mixture(2*fs, fs, f), original = x.clone();
            double[] y = FrequencyFilters.cascade(x, new Coefficients[]{boost, cut});
            for (int i = 0; i < x.length; i += 137) close(y[i], x[i], 1e-10);
            require(Arrays.equals(x, original));
            double[] zeroGain = FrequencyFilters.process(x, FrequencyFilters.peaking(fs, f, q, 0));
            for (int i = 0; i < x.length; i += 137) close(zeroGain[i], x[i], 1e-10);
        }
        // Independent steady-state gain measurement at several frequencies for all filter types.
        Coefficients[] filters = {lp, hp, notch, FrequencyFilters.peaking(fs, 1400, 1, 6)};
        for (Coefficients c : filters) for (double f : new double[]{350, 880, 1400, 3000}) {
            double[] x = TestSignals.mixture(2*fs, fs, f), y = FrequencyFilters.process(x, c);
            double measured = Diagnostics.toneAmplitude(y, fs, f, fs)/0.2;
            close(measured, Diagnostics.magnitude(c, f, fs), 1e-9);
            require(Arrays.equals(y, FrequencyFilters.process(x, c))); // reset state between runs
        }
    }
    private static void parametersAndAudio() throws Exception {
        TaskParameters p = GenerateParameters.generate("00123456");
        require(p.equals(GenerateParameters.generate("00123456")));
        require(p.studentId().equals("00123456"));
        for (String invalid : new String[]{"123", "123456789", "abcdefgh", "-1234567"}) {
            boolean failed = false;
            try { GenerateParameters.generate(invalid); } catch (IllegalArgumentException e) { failed = true; }
            require(failed);
        }
        for (int i = 0; i < 100; i++) {
            TaskParameters a = GenerateParameters.generate(String.format("%08d", i));
            require(a.cutoffHz() >= 450 && a.cutoffHz() <= 600);
            require(a.notchHz() == 440 || a.notchHz() == 880 || a.notchHz() == 1400);
            require(a.equalizerGainDb() >= 3 && a.equalizerGainDb() <= 6);
        }
        AudioFiles.AudioData wav = AudioFiles.read(Path.of("media/siney.wav"));
        require(wav.sampleRate() == 44100 && wav.samples().length == 926100);
        Validation.samples(wav.samples());
        AudioFiles.AudioData music = AudioFiles.read(Path.of("media/intractable-excerpt.wav"));
        require(music.sampleRate() == 44100 && music.samples().length == 1323000);
        Validation.samples(music.samples());
        Path temp = Files.createTempFile("lab04-roundtrip-", ".wav");
        try {
            double[] input = {-1, -0.2, 0, 0.2, 32767/32768.0};
            AudioFiles.write(temp, input, 44100);
            double[] output = AudioFiles.read(temp).samples();
            require(output.length == input.length);
            for (int i = 0; i < input.length; i++) close(output[i], input[i], 0.5/32768);
        } finally { Files.deleteIfExists(temp); }
    }
}
