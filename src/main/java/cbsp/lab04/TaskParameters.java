package cbsp.lab04;
/** Prepared immutable settings. Gains are in dB; Q is dimensionless. */
public record TaskParameters(String version, String studentId, double cutoffHz,
        double notchHz, double equalizerGainDb) { }
