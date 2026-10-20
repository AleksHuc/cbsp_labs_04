package cbsp.lab04;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

/** Prepared raw diagnostics: no listening-copy attenuation is applied here. */
public final class Diagnostics {
    private Diagnostics() { }
    public static void spectrum(Path path, Spectrum before, Spectrum after) throws IOException {
        try (PrintWriter w = writer(path)) {
            w.println("frequency_hz,input_amplitude,output_amplitude,input_dbfs,output_dbfs");
            for (int k = 0; k < before.amplitude().length; k++)
                w.printf(Locale.ROOT, "%.12g,%.12g,%.12g,%.12g,%.12g%n", before.frequencyHz()[k],
                        before.amplitude()[k], after.amplitude()[k], Spectrum.db(before.amplitude()[k]),
                        Spectrum.db(after.amplitude()[k]));
        }
    }
    /** Prepared analytic |H(exp(j*w))|; compares implementation with steady-state measurements. */
    public static double magnitude(Coefficients c, double frequency, double fs) {
        double w = 2*Math.PI*frequency/fs;
        double nr = c.b0()+c.b1()*Math.cos(w)+c.b2()*Math.cos(2*w);
        double ni = -c.b1()*Math.sin(w)-c.b2()*Math.sin(2*w);
        double dr = 1+c.a1()*Math.cos(w)+c.a2()*Math.cos(2*w);
        double di = -c.a1()*Math.sin(w)-c.a2()*Math.sin(2*w);
        return Math.hypot(nr, ni)/Math.hypot(dr, di);
    }
    public static void response(Path path, Coefficients[] stages, int fs) throws IOException {
        try (PrintWriter w = writer(path)) {
            w.println("frequency_hz,gain_linear,gain_db");
            for (int k = 0; k <= 4096; k++) {
                double hz = k*fs/8192.0, gain = 1;
                for (Coefficients c : stages) gain *= magnitude(c, hz, fs);
                w.printf(Locale.ROOT, "%.12g,%.12g,%.12g%n", hz, gain, Spectrum.db(gain));
            }
        }
    }
    public static double toneAmplitude(double[] x, int fs, double hz, int start) {
        double re = 0, im = 0;
        for (int n = start; n < x.length; n++) {
            re += x[n]*Math.cos(2*Math.PI*hz*n/fs);
            im -= x[n]*Math.sin(2*Math.PI*hz*n/fs);
        }
        return 2*Math.hypot(re, im)/(x.length-start);
    }
    public static void tones(Path path, double[] x, double[] y, int fs, double[] hz,
            Coefficients[] stages) throws IOException {
        try (PrintWriter w = writer(path)) {
            w.println("frequency_hz,input_amplitude,output_amplitude,measured_gain_db,expected_gain_db");
            // Last one-second interval: integer-Hz test tones, after startup transient.
            for (double f : hz) {
                double a = toneAmplitude(x, fs, f, x.length-fs);
                double b = toneAmplitude(y, fs, f, y.length-fs), expected = 1;
                for (Coefficients c : stages) expected *= magnitude(c, f, fs);
                w.printf(Locale.ROOT, "%.12g,%.12g,%.12g,%.12g,%.12g%n",
                        f, a, b, Spectrum.db(b/a), Spectrum.db(expected));
            }
        }
    }
    private static PrintWriter writer(Path path) throws IOException {
        Files.createDirectories(path.toAbsolutePath().getParent());
        return new PrintWriter(Files.newBufferedWriter(path));
    }
}
