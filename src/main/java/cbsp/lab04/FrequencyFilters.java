package cbsp.lab04;

/** Tasks 1–5: implement the methods marked TODO.
 * Algorithm inputs are validated by the prepared infrastructure or assumed valid.
 * Coefficient equations: W3C Audio EQ Cookbook (RBJ), Q parametrization.
 */
public final class FrequencyFilters {
    private FrequencyFilters() { }

    // TODO (Task 1): periodic Hann window and calibrated one-sided amplitude spectrum.
    public static Spectrum analyze(double[] samples, int sampleRate, boolean hann) {
        // TODO (Task 1): implement this method.
        throw new UnsupportedOperationException("TODO (Task 1)");
    }

    // TODO (Task 2): direct form I, with zero initial state on every call.
    public static double[] process(double[] samples, Coefficients c) {
        // TODO (Task 2): implement this method.
        throw new UnsupportedOperationException("TODO (Task 2)");
    }

    // TODO (Task 2): low-pass normalized coefficients.
    public static Coefficients lowPass(double fs, double f0, double q) {
        // TODO (Task 2): implement this method.
        throw new UnsupportedOperationException("TODO (Task 2)");
    }

    // TODO (Task 2): high-pass normalized coefficients.
    public static Coefficients highPass(double fs, double f0, double q) {
        // TODO (Task 2): implement this method.
        throw new UnsupportedOperationException("TODO (Task 2)");
    }

    // TODO (Task 3): notch coefficients; larger Q gives a narrower rejection band.
    public static Coefficients notch(double fs, double f0, double q) {
        // TODO (Task 3): implement this method.
        throw new UnsupportedOperationException("TODO (Task 3)");
    }

    // TODO (Task 4): peaking EQ; gainDb is not a linear amplitude multiplier.
    public static Coefficients peaking(double fs, double f0, double q, double gainDb) {
        // TODO (Task 4): implement this method.
        throw new UnsupportedOperationException("TODO (Task 4)");
    }

    // TODO (Task 5): serial cascade; empty cascade returns a distinct unchanged copy.
    public static double[] cascade(double[] samples, Coefficients[] stages) {
        // TODO (Task 5): implement this method.
        throw new UnsupportedOperationException("TODO (Task 5)");
    }
}
