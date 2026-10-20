package cbsp.lab04;
/** Prepared signal generators. */
public final class TestSignals {
    private TestSignals() { }
    public static double[] mixture(int count, int fs, double... frequencies) {
        double[] x = new double[count];
        for (int n = 0; n < count; n++)
            for (double f : frequencies) x[n] += 0.2*Math.sin(2*Math.PI*f*n/fs);
        return x;
    }
}
