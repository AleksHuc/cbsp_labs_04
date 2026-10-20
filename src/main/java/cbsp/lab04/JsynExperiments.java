package cbsp.lab04;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

/** Prepared diagnostics compare the WAV actually recorded by JSyn with the earlier algorithms. */
public final class JsynExperiments {
    private JsynExperiments() { }
    public static void run(int task, TaskParameters p, boolean play) throws Exception {
        boolean single = task == 6;
        if (!single && task != 7) throw new IllegalArgumentException("JSyn task must be 6 or 7.");
        Path input = Path.of("media", single ? "siney.wav" : "intractable-excerpt.wav");
        AudioFiles.AudioData data = AudioFiles.read(input);
        int fs = JsynAudio.SAMPLE_RATE, expectedFrames = single ? 926100 : 1323000;
        if (data.sampleRate() != fs || data.samples().length != expectedFrames)
            throw new IllegalArgumentException("Use the supplied WAV for Task "+task+".");
        double gain = p.equalizerGainDb();
        double[] frequencies = single ? new double[]{440} : new double[]{50, 100, 156, 220, 311, 440, 880};
        JsynAudio.FilterType filterType = single ? JsynAudio.FilterType.BAND_STOP : JsynAudio.FilterType.PEAKING_EQ;
        double[] qs = single ? new double[]{10} : new double[]{0.5, 0.5, 1.3, 1.42, 1.41, 0.77, 0.5};
        double[] gainsDb = single ? new double[0] : new double[]{0, gain, 0, 0, 0, -gain, gain};
        Coefficients[] referenceStages = new Coefficients[frequencies.length];
        for (int i = 0; i < frequencies.length; i++)
            referenceStages[i] = single ? FrequencyFilters.notch(fs, frequencies[i], qs[i])
                    : FrequencyFilters.peaking(fs, frequencies[i], qs[i], gainsDb[i]);
        double[] original = Arrays.copyOf(data.samples(), expectedFrames+JsynAudio.TAIL_FRAMES);
        // Only a headroom/verification prepass. The exported filtered WAV comes from JSyn's graph.
        double[] reference = FrequencyFilters.cascade(original, referenceStages);
        double peak = 0;
        for (int n = 0; n < original.length; n++) peak = Math.max(peak, Math.max(Math.abs(original[n]), Math.abs(reference[n])));
        double scale = Math.min(1, 0.79/peak); // a small margin for library/float numerical differences
        Path out = Path.of("output", p.studentId());
        Files.createDirectories(out);
        String name = "task"+task;
        System.out.printf("Task %d JSyn: %s, filter=%s, frequencies=%s Hz, Q=%s, gain=%s dB, output scale=%.6f%n",
                task, input, filterType, Arrays.toString(frequencies), Arrays.toString(qs), Arrays.toString(gainsDb), scale);
        JsynAudio.record(input, out.resolve(name+"-filtered.wav"), filterType, frequencies, qs, gainsDb, scale, play);
        AudioFiles.AudioData recorded = AudioFiles.read(out.resolve(name+"-filtered.wav"));
        if (recorded.sampleRate() != fs || recorded.samples().length != original.length)
            throw new IOException("Unexpected JSyn WAV rate or duration.");
        Validation.samples(recorded.samples());
        double[] listeningOriginal = original.clone(), unscaledRecorded = recorded.samples().clone();
        double maxError = 0, squaredError = 0, outputPeak = 0;
        for (int n = 0; n < original.length; n++) {
            listeningOriginal[n] *= scale;
            double error = recorded.samples()[n]-reference[n]*scale;
            maxError = Math.max(maxError, Math.abs(error));
            squaredError += error*error;
            outputPeak = Math.max(outputPeak, Math.abs(recorded.samples()[n]));
            unscaledRecorded[n] /= scale;
        }
        if (maxError > 1e-4 || outputPeak > 0.8)
            throw new IOException("JSyn output disagrees with the reference or exceeds listening headroom: "+maxError);
        AudioFiles.write(out.resolve(name+"-original.wav"), listeningOriginal, fs);
        Spectrum before = FrequencyFilters.analyze(Arrays.copyOfRange(original, fs, fs+32768), fs, true);
        Spectrum after = FrequencyFilters.analyze(Arrays.copyOfRange(unscaledRecorded, fs, fs+32768), fs, true);
        Diagnostics.spectrum(out.resolve(name+"-spectrum.csv"), before, after);
        SpectrumPlot.write(out.resolve(name+"-spectrum.png"), name+" JSyn recording", before, "Original", after,
                "Recorded JSyn output (attenuation undone)", 0, 4000);
        Diagnostics.response(out.resolve(name+"-response.csv"), referenceStages, fs);
        double rmse = Math.sqrt(squaredError/original.length);
        Files.writeString(out.resolve(name+"-settings.txt"), "input="+input+"\nengine=JSyn v17.2.0\n"
                +"filter="+filterType+"\nfrequenciesHz="+Arrays.toString(frequencies)+"\nQ="+Arrays.toString(qs)
                +"\ngainsDb="+Arrays.toString(gainsDb)+"\ncommon listening scale="+scale
                +"\ntail frames="+JsynAudio.TAIL_FRAMES+"\nframes="+original.length
                +"\nrecording mode="+(play ? "live playback and recording" : "non-real-time JSyn, no device")
                +"\nreference max absolute error="+maxError+"\nreference RMSE="+rmse
                +"\nspectrum interval=[44100,76868); output is decoded recorded PCM divided by listening scale\n");
        System.out.printf("Task %d: %.2f s mono PCM recorded, reference max error %.3g, RMSE %.3g%n",
                task, original.length/(double)fs, maxError, rmse);
    }
}
