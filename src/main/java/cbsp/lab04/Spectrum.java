package cbsp.lab04;
/** One-sided peak amplitudes, including DC and Nyquist; not a power spectral density. */
public record Spectrum(double[] frequencyHz, double[] amplitude) {
    public static double db(double amplitude) { return 20*Math.log10(Math.max(1e-12, amplitude)); }
}
