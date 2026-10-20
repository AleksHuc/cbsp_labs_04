package cbsp.lab04;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

/** Prepared task runners. Each command is independent of previous outputs. */
public final class Experiments {
    public static final int SAMPLE_RATE = 44100;
    private static final int FFT_SIZE = 32768;
    private Experiments() { }

    public static void run(int task, TaskParameters p, boolean play) throws Exception {
        Path out = Path.of("output", p.studentId());
        Files.createDirectories(out);
        Files.writeString(out.resolve("parameters.txt"), p+System.lineSeparator());
        int fs = SAMPLE_RATE;
        double[] frequencies = {350, 1400, 3000};
        double[] mixture = TestSignals.mixture(2*fs, fs, frequencies);
        double cutoff = p.cutoffHz(), notch = p.notchHz(), gain = p.equalizerGainDb();
        switch (task) {
            case 1 -> {
                double bin = 400.0*fs/FFT_SIZE;
                double[] on = TestSignals.mixture(FFT_SIZE, fs, bin);
                double[] off = TestSignals.mixture(FFT_SIZE, fs, bin+0.5*fs/FFT_SIZE);
                for (boolean hann : new boolean[]{false, true}) {
                    Spectrum a = FrequencyFilters.analyze(on, fs, hann);
                    Spectrum b = FrequencyFilters.analyze(off, fs, hann);
                    String name = "task1-"+(hann ? "hann" : "rectangular");
                    Diagnostics.spectrum(out.resolve(name+".csv"), a, b);
                    SpectrumPlot.write(out.resolve(name+".png"), name, a, "On-bin tone", b, "Off-bin tone",
                            bin-8.0*fs/FFT_SIZE, bin+8.0*fs/FFT_SIZE);
                }
                // CSV input/output columns here mean on-bin/off-bin signals.
                compare(out, "task1-recording", recording(), new Coefficients[0], null, play);
                System.out.printf("Task 1: bin spacing %.6f Hz; on-bin %.6f Hz, off-bin %.6f Hz%n",
                        fs/(double)FFT_SIZE, bin, bin+0.5*fs/FFT_SIZE);
            }
            case 2 -> {
                compare(out, "task2-lowpass", mixture,
                        new Coefficients[]{FrequencyFilters.lowPass(fs, cutoff, 1/Math.sqrt(2))}, frequencies, play);
                compare(out, "task2-highpass", mixture,
                        new Coefficients[]{FrequencyFilters.highPass(fs, cutoff, 1/Math.sqrt(2))}, frequencies, play);
            }
            case 3 -> {
                double[] tones = {350, notch, 3000};
                double[] x = TestSignals.mixture(2*fs, fs, tones);
                double[] wav = recording();
                // The supplied recording has a strong 440 Hz tone, independent of student ID.
                double recordingNotchHz = 440;
                System.out.printf("Task 3: synthetic notch %.0f Hz; recording notch %.0f Hz%n",
                        notch, recordingNotchHz);
                for (double q : new double[]{1, 10}) {
                    Coefficients[] stages = {FrequencyFilters.notch(fs, notch, q)};
                    compare(out, "task3-Q"+(int)q+"-synthetic", x, stages, tones, play);
                    compare(out, "task3-Q"+(int)q+"-recording", wav,
                            new Coefficients[]{FrequencyFilters.notch(fs, recordingNotchHz, q)}, null, play);
                }
            }
            case 4 -> {
                double[] tones = {350, 1400, 3000};
                for (int sign : new int[]{1, -1}) {
                    Coefficients[] stages = {FrequencyFilters.peaking(fs, 1400, 1, sign*gain)};
                    compare(out, "task4-"+(sign == 1 ? "boost" : "cut"), mixture, stages, tones, play);
                }
                // Attenuate the recording's loud 440 Hz tone by the assigned gain in dB.
                System.out.printf("Task 4 recording: peaking EQ at 440 Hz, Q=1, gain=%.0f dB%n", -gain);
                compare(out, "task4-recording", recording(),
                        new Coefficients[]{FrequencyFilters.peaking(fs, 440, 1, -gain)}, null, play);
            }
            case 5 -> {
                double[] centers = {50, 100, 156, 220, 311, 440, 880};
                double[] gains = {0, gain, 0, 0, 0, -gain, gain};
                double[] qs = {0.5, 0.5, 1.3, 1.42, 1.41, 0.77, 0.5};
                Coefficients[] stages = new Coefficients[centers.length];
                for (int i = 0; i < stages.length; i++)
                    stages[i] = FrequencyFilters.peaking(fs, centers[i], qs[i], gains[i]);
                compare(out, "task5-recording", equalizerRecording(), stages, null, play);
                compare(out, "task5-synthetic", TestSignals.mixture(2*fs, fs, centers), stages, centers, play);
            }
            case 6, 7 -> JsynExperiments.run(task, p, play);
            default -> throw new IllegalArgumentException("Task must be 1..7.");
        }
    }
    private static double[] recording() throws Exception {
        AudioFiles.AudioData data = AudioFiles.read(Path.of("media/siney.wav"));
        if (data.sampleRate() != SAMPLE_RATE || data.samples().length != 926100)
            throw new IllegalArgumentException("Use the supplied 21-second media/siney.wav at 44100 Hz.");
        return data.samples();
    }
    private static double[] equalizerRecording() throws Exception {
        AudioFiles.AudioData data = AudioFiles.read(Path.of("media/intractable-excerpt.wav"));
        if (data.sampleRate() != SAMPLE_RATE || data.samples().length != 30*SAMPLE_RATE)
            throw new IllegalArgumentException("Use the supplied 30-second media/intractable-excerpt.wav at 44100 Hz.");
        return data.samples();
    }
    private static void compare(Path out, String name, double[] x, Coefficients[] stages,
            double[] frequencies, boolean play) throws Exception {
        int fs = SAMPLE_RATE;
        double[] y;
        if (stages.length == 0) y = x.clone();
        else if (stages.length == 1) y = FrequencyFilters.process(x, stages[0]);
        else y = FrequencyFilters.cascade(x, stages);
        Validation.samples(y);
        if (y.length != x.length) throw new IllegalStateException("Filter changed signal length.");
        // Analyze the same 32768-sample interval starting one second into both signals.
        Spectrum before = FrequencyFilters.analyze(Arrays.copyOfRange(x, fs, fs+FFT_SIZE), fs, true);
        Spectrum after = FrequencyFilters.analyze(Arrays.copyOfRange(y, fs, fs+FFT_SIZE), fs, true);
        Diagnostics.spectrum(out.resolve(name+"-spectrum.csv"), before, after);
        SpectrumPlot.write(out.resolve(name+"-spectrum.png"), name, before, "Original", after,
                stages.length == 0 ? "Identity copy (no filtering)" : "Filtered", 0, 4000);
        Diagnostics.response(out.resolve(name+"-response.csv"), stages, fs);
        if (frequencies != null) Diagnostics.tones(out.resolve(name+"-tones.csv"), x, y, fs, frequencies, stages);
        double peak = 0;
        for (double[] a : new double[][]{x, y}) for (double v : a) peak = Math.max(peak, Math.abs(v));
        double scale = peak > 0.8 ? 0.8/peak : 1;
        double[] listeningX = x.clone(), listeningY = y.clone();
        for (int n = 0; n < x.length; n++) { listeningX[n] *= scale; listeningY[n] *= scale; }
        AudioFiles.write(out.resolve(name+"-original.wav"), listeningX, fs);
        AudioFiles.write(out.resolve(name+"-filtered.wav"), listeningY, fs);
        Files.writeString(out.resolve(name+"-settings.txt"), "stages="+Arrays.toString(stages)
                +"\ncommon listening attenuation="+scale+"\nspectrum interval=[44100,76868)\n");
        System.out.printf("%s: %d samples, raw peak %.6f, common listening scale %.6f%n", name, x.length, peak, scale);
        if (play) { AudioOutput.play(listeningX, fs); AudioOutput.play(listeningY, fs); }
    }
}
