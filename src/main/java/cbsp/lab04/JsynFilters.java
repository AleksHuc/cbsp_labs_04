package cbsp.lab04;

import com.jsyn.Synthesizer;
import com.jsyn.ports.UnitOutputPort;
import com.jsyn.unitgen.FilterBandStop;
import com.jsyn.unitgen.FilterPeakingEQ;

/** Tasks 6–7: implement the JSyn methods marked TODO. */
public final class JsynFilters {
    private JsynFilters() { }

    // TODO (Task 6): add a band-stop unit and set its notch frequency and Q.
    public static FilterBandStop bandStop(Synthesizer synth, double frequencyHz, double q) {
        // TODO (Task 6): implement this method.
        throw new UnsupportedOperationException("TODO (Task 6)");
    }

    // TODO (Task 7): add a peaking unit and set frequency, Q and LINEAR gain.
    public static FilterPeakingEQ peaking(Synthesizer synth, double frequencyHz, double q, double gainDb) {
        // TODO (Task 7): implement this method.
        throw new UnsupportedOperationException("TODO (Task 7)");
    }

    // TODO (Task 7): connect seven independent EQ units in series; do not sum their outputs.
    public static UnitOutputPort cascade(Synthesizer synth, UnitOutputPort input,
            double[] frequencies, double[] qs, double[] gainsDb) {
        // TODO (Task 7): implement this method.
        throw new UnsupportedOperationException("TODO (Task 7)");
    }
}
