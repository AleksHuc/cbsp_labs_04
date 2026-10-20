package cbsp.lab04;
/** Normalized biquad coefficients: denominator 1 + a1*z^-1 + a2*z^-2. */
public record Coefficients(double b0, double b1, double b2, double a1, double a2) {
    public static Coefficients normalized(double b0, double b1, double b2,
            double a0, double a1, double a2) {
        return new Coefficients(b0/a0, b1/a0, b2/a0, a1/a0, a2/a0);
    }
}
