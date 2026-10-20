package cbsp.lab04;
/** Prepared in-place radix-2 complex FFT, negative exponent, no forward scaling. */
public final class Fft {
    private Fft() { }
    public static void forward(double[] re, double[] im) {
        int n = re.length;
        if (n < 2 || (n & (n-1)) != 0 || im.length != n)
            throw new IllegalArgumentException("FFT requires equal power-of-two buffers, N>=2.");
        for (int i = 1, j = 0; i < n; i++) {
            int bit = n >> 1;
            for (; (j & bit) != 0; bit >>= 1) j ^= bit;
            j ^= bit;
            if (i < j) {
                double t = re[i]; re[i] = re[j]; re[j] = t;
                t = im[i]; im[i] = im[j]; im[j] = t;
            }
        }
        for (int length = 2; ; length *= 2) {
            double angle = -2*Math.PI/length;
            double wr = Math.cos(angle), wi = Math.sin(angle);
            for (int start = 0; start < n; start += length) {
                double ur = 1, ui = 0;
                for (int j = 0; j < length/2; j++) {
                    int a = start+j, b = a+length/2;
                    double vr = re[b]*ur-im[b]*ui, vi = re[b]*ui+im[b]*ur;
                    re[b] = re[a]-vr; im[b] = im[a]-vi;
                    re[a] += vr; im[a] += vi;
                    double t = ur*wr-ui*wi; ui = ur*wi+ui*wr; ur = t;
                }
            }
            if (length == n) break;
        }
    }
}
