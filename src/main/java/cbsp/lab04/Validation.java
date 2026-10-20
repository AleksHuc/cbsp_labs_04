package cbsp.lab04;
/** Prepared sample validation for audio export and experiment diagnostics. */
public final class Validation {
    private Validation() { }
    public static void samples(double[] x) {
        if (x == null || x.length == 0) throw new IllegalArgumentException("Non-empty samples required.");
        for (double v : x) if (!Double.isFinite(v)) throw new IllegalArgumentException("Samples must be finite.");
    }
}
