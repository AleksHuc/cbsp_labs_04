# Lab 04: Frequency response and audio equalization

This project is a Java starter for **seven required tasks**, in the
same format as Lab 03. Start with spectral measurements, implement second-order
filters, then connect several equalizer bands. The original low-pass, notch and
seven-band equalizer examples are retained and extended with numerical comparisons.

The array-processing algorithms are in **`FrequencyFilters.java`**. Tasks 6–7
use **`JsynFilters.java`** and the marked connections in **`JsynAudio.java`**. `TODO (Task N)` comments
mark methods you must implement. Each unfinished method throws
`UnsupportedOperationException("TODO (Task N)")`. FFT, WAV I/O,
parameter generation, diagnostics and runners are prepared infrastructure.
Assume valid algorithm inputs; validation and exception handling are not student tasks.

Despite the legacy exercise name, the filters here operate **in the time domain**
using IIR difference equations. Their behavior is analyzed in the frequency domain.
Multiplication of FFT bins and overlap-add convolution are not part of this lab.

## Setup and running

Use JDK 21 or newer and Maven 3.9.x or newer. Open the directory containing
`pom.xml` as a Maven project. Maven may need internet access to download build
plugins and JSyn. Tasks 6–7 use **JSyn v17.2.0**, resolved through the JitPack
repository declared in `pom.xml`. Tasks 1–5 use array processing and the prepared FFT.
The JSyn tasks use the standard JavaSound backend; no native audio library is required.

Run from the project root:

```bash
mvn clean compile
mvn -q compile exec:java "-Dexec.args=--parameters 11111111"
mvn -q compile exec:java "-Dexec.args=--check1 11111111"
mvn -q compile exec:java "-Dexec.args=--task1 11111111"
```

Replace `11111111` with your eight-digit student ID, preserving leading zeroes.
`--checkN` writes diagnostics and WAV files without an audio device. In Tasks 1–5,
`--taskN` also plays each original followed by its filtered version. In Tasks 6–7,
`--taskN` plays the **filtered JSyn signal live while recording it**; it does not
play the original first. Start with low device volume. N is **1 through 7**.

In IntelliJ, run `Main.main` as an Application with `--check3 11111111` in
Program arguments and the project root as Working directory. For a Maven run
configuration use `compile exec:java "-Dexec.args=--check3 11111111"`, without `mvn`.

| Task | Comparisons | Playback duration, excluding processing |
| --- | --- | --- |
| 1 | Original recording and identity copy; CSV window comparison | 42 s |
| 2 | Low-pass and high-pass on a two-second mixture | 8 s |
| 3 | Q=1 and Q=10 notch, each on synthetic and recorded audio | 92 s |
| 4 | Synthetic boost and cut; recording cut at 440 Hz | 50 s |
| 5 | Seven-band cascade on 30-second music excerpt and synthetic mixture | 64 s |
| 6 | JSyn 440 Hz band-stop on siney.wav; filtered playback and recording | 21.25 s |
| 7 | JSyn seven-band equalizer on music; filtered playback and recording | 30.25 s |

All commands are independent of previous runs. Outputs are in
`output/<student-id>/`; repeating a command replaces files with matching names.
Other output files are retained. `parameters.txt` records the generator settings.

| Assigned parameter | Values | Use |
| --- | --- | --- |
| `cutoffHz` | 450–600 Hz in 25 Hz steps | Task 2 |
| `notchHz` | 440, 880 or 1400 Hz | Task 3 synthetic test; recording always uses 440 Hz |
| `equalizerGainDb` | 3, 4, 5 or 6 dB | Tasks 4, 5 and 7 |

Generator version is `lab04-v1`. Keep the generator and random-call order unchanged.
No manually chosen parameter is a substitute for the settings from your ID.

## Task 1 — Calibrated amplitude spectrum

Implement **`analyze(samples, sampleRate, hann)`**. The prepared `Fft.forward`
accepts separate real and imaginary arrays, uses the negative-exponent DFT
convention and applies no forward normalization. Do not implement an FFT.

For N input samples, prepare either a rectangular window (`w[n]=1`) or the
**periodic Hann** window:

```text
w[n] = 0.5 * (1 - cos(2*pi*n/N)), n=0,...,N-1
X = FFT(samples * w)
f[k] = k*sampleRate/N, k=0,...,N/2
amplitude[k] = factor[k] * hypot(real(X[k]), imaginary(X[k])) / sum(w)
factor[k] = 1 at DC and Nyquist; 2 at every other returned bin
```

Return N/2+1 bins, including DC and Nyquist. Keep the input unchanged.
This is a **one-sided peak amplitude spectrum**, not a power spectrum or PSD.
The display value is `20*log10(max(amplitude,1e-12))` dBFS; the floor is -240 dBFS.
A full-scale, bin-centered isolated sinusoid has peak amplitude 1 and level 0 dBFS.
Window-sum normalization corrects coherent gain. It does not remove off-bin
amplitude error, main-lobe widening, or interference between nearby components.

The runner compares an amplitude-0.2 sinusoid exactly at bin 400 with another
half a bin higher, using N=32768 and sample rate 44100 Hz. Bin spacing is about
1.345825 Hz. Inspect leakage and main-lobe width with both windows.

Outputs:

- `task1-rectangular.csv` and `task1-hann.csv`: **input columns mean the on-bin
  signal; output columns mean the off-bin signal**. Both signals are synthetic.
- Matching `task1-rectangular.png` and `task1-hann.png` compare those spectra,
  with a detail view around the test tones.
- `task1-recording-*`: the recording spectrum and identity response, plus identical
  original/filtered listening copies. This comparison demonstrates analysis only.

Checks: for N=1024, sample rate 8192 Hz and
`x[n]=0.3+0.2*sin(2*pi*80*n/N)+0.1*(-1)^n`, both windows should give amplitudes
0.3 at DC, 0.2 at 640 Hz and 0.1 at Nyquist, within floating-point tolerance.

```bash
mvn -q compile exec:java "-Dexec.args=--check1 11111111"
mvn -q compile exec:java "-Dexec.args=--task1 11111111"
```

## Shared filter equations

A normalized `Coefficients(b0,b1,b2,a1,a2)` describes

```text
H(z) = (b0 + b1*z^-1 + b2*z^-2) / (1 + a1*z^-1 + a2*z^-2)
y[n] = b0*x[n] + b1*x[n-1] + b2*x[n-2] - a1*y[n-1] - a2*y[n-2]
```

Every processing call starts with zero state. Samples before index zero are zero.
Normalize all five stored coefficients by the unnormalized denominator coefficient
`a0`. `Coefficients.normalized` is a prepared helper. Define
`w0=2*pi*f0/sampleRate`, `c=cos(w0)` and `alpha=sin(w0)/(2*Q)`.

The coefficient definitions follow the Q-based equations in the
[W3C Audio EQ Cookbook](https://www.w3.org/TR/audio-eq-cookbook/), adapted from
Robert Bristow-Johnson. The table lists **unnormalized** coefficients:

| Filter | b0 | b1 | b2 | a0 | a1 | a2 |
| --- | --- | --- | --- | --- | --- | --- |
| Low-pass | (1-c)/2 | 1-c | (1-c)/2 | 1+alpha | -2c | 1-alpha |
| High-pass | (1+c)/2 | -(1+c) | (1+c)/2 | 1+alpha | -2c | 1-alpha |
| Notch | 1 | -2c | 1 | 1+alpha | -2c | 1-alpha |
| Peaking EQ | 1+alpha*A | -2c | 1-alpha*A | 1+alpha/A | -2c | 1-alpha/A |

For peaking EQ, `A=10^(gainDb/40)`. The magnitude gain at its center is
`10^(gainDb/20)`. Q is dimensionless; frequency is in Hz and gain is in dB.

## Task 2 — Low-pass and high-pass filters

Implement **`process`**, **`lowPass`** and **`highPass`**.
Use direct form I with two previous input and two previous output values.
Each call returns a new, same-length array and preserves the input. Do not clip,
normalize, or apply a window while filtering. Windows belong to analysis only.

Generate a two-second mixture of 350, 1400 and 3000 Hz tones, each with peak
amplitude 0.2. Apply the assigned cutoff with `Q=1/sqrt(2)` to each filter.
Predict which components will be attenuated, then compare `*-tones.csv`
measurements with the expected gains. These second-order filters have finite
transition bands; a cutoff does not remove every tone above or below it.

Checks: low-pass DC gain is 1, high-pass DC gain is 0; at Nyquist these reverse.
At the cutoff both magnitudes are `1/sqrt(2)`, about -3.0103 dB for this Q.
For input `[1,0,0,0]` and coefficients `(0.5,0.25,0.125,-0.5,0.25)`, output is
`[0.5,0.5,0.25,0]`. Check feedback signs and state update order.

Output prefixes: `task2-lowpass`, `task2-highpass`.

```bash
mvn -q compile exec:java "-Dexec.args=--check2 11111111"
mvn -q compile exec:java "-Dexec.args=--task2 11111111"
```

## Task 3 — Notch filter and Q

Implement **`notch`**, reusing `process`.
Compare Q=1 and Q=10 at your assigned notch frequency. The synthetic mixture has
350 Hz, the assigned notch tone, and 3000 Hz, each with amplitude 0.2. Also process
`media/siney.wav` with a **440 Hz notch**, targeting its strong continuous tone.
The recording target is independent of student ID; applying an assigned 880 or
1400 Hz notch would leave the loud 440 Hz tone largely intact.
A larger Q makes the rejected band narrower.

Check rejection of the exact synthetic tone and preservation of other components.
Compare the response away from the notch, not only at its center. The recording
has no supplied clean reference, so its spectral change is not a restoration score.
A notch also affects useful sound near its center frequency.

Checks: ideal steady-state center gain is zero; DC and Nyquist gains are one.
A finite buffer includes a startup transient, so its waveform is not identically
zero even for an input exactly at the notch frequency. The dB floor prevents
logarithms of zero. A coarse response grid can miss the exact notch center;
`*-tones.csv` evaluates the assigned frequency directly.

Prefixes: `task3-Q1-synthetic`, `task3-Q1-recording`, `task3-Q10-synthetic`,
`task3-Q10-recording`.

```bash
mvn -q compile exec:java "-Dexec.args=--check3 11111111"
mvn -q compile exec:java "-Dexec.args=--task3 11111111"
```

## Task 4 — Peaking equalizer

Implement **`peaking`**. Use center frequency 1400 Hz and Q=1 for the synthetic
comparison of a positive assigned gain with its negative on the Task 2 mixture.
For the recording, use **440 Hz, Q=1 and the negative assigned gain** to attenuate
its loud continuous tone. For example, an assigned gain of 6 dB gives a -6 dB
recording cut, approximately halving the tone amplitude. Unlike Task 3's notch,
this EQ applies a finite reduction rather than eliminating the center frequency.
Inspect center gain, nearby frequencies and raw output peak.
A boost can make the raw signal exceed the PCM range even when the input fits.

Checks: zero dB gives an identity filter; center gain in dB equals the requested
gain. A boost followed by an equal cut with the same center and Q reconstructs
the input within numerical tolerance, including the startup samples when both
stages start from zero. Do not use `10^(gainDb/20)` in place of A in the table.

Prefixes: `task4-boost`, `task4-cut`, `task4-recording`.

```bash
mvn -q compile exec:java "-Dexec.args=--check4 11111111"
mvn -q compile exec:java "-Dexec.args=--task4 11111111"
```

## Task 5 — Seven-band cascade

Implement **`cascade`**. Feed each stage's output into the next stage. Return a
distinct unchanged copy for an empty list. Each stage has its own zero initial
state. Reuse the peaking design and processing methods; do not add stage outputs.

The runner retains the original seven center frequencies and Q values, but uses
explicit dB gains to demonstrate both boosting and cutting:

| Center (Hz) | 50 | 100 | 156 | 220 | 311 | 440 | 880 |
| --- | --- | --- | --- | --- | --- | --- | --- |
| Q | 0.5 | 0.5 | 1.3 | 1.42 | 1.41 | 0.77 | 0.5 |
| Gain (dB), G=assigned gain | 0 | +G | 0 | 0 | 0 | -G | +G |

Process **`media/intractable-excerpt.wav`**, a 30-second excerpt of Kevin MacLeod's
*Intractable* (source interval 00:30–01:00). This music is used in Tasks 5 and 7;
Tasks 1, 3, 4 and 6 retain `media/siney.wav`. Attribution, license and conversion
details are in `media/CREDITS.md`.

Also process the same two-second mixture containing all seven center tones,
each of amplitude 0.2. Their sum can exceed 1; raw floating-point processing permits
this, and the common listening attenuation handles PCM export.

The cascade magnitude is the **product** of stage magnitudes. Its gain in dB is
their sum. Overlapping bands mean the total gain at a center frequency need not
equal that band's requested gain. Compare measured tone gains with the entire
cascade response. Optional exploration: reverse the stage order; exact arithmetic
predicts the same result, with possible floating-point differences.

Checks: empty cascade returns an unchanged copy; a single stage matches `process`;
a boost/cut pair is identity within tolerance. Input arrays must remain unchanged.

Prefixes: `task5-recording`, `task5-synthetic`.

```bash
mvn -q compile exec:java "-Dexec.args=--check5 11111111"
mvn -q compile exec:java "-Dexec.args=--task5 11111111"
```

## Task 6 — JSyn filtering, live playback and recording

Use **`media/siney.wav`** and reproduce Task 3's narrow recording notch with JSyn:
**`FilterBandStop`, center frequency 440 Hz and Q=10**. This rejects the loud
continuous 440 Hz tone rather than merely reducing it by the assigned dB gain.
Task 6 does not use `equalizerGainDb`; a band-stop filter has no peaking-gain setting.
Higher Q narrows the rejected band and preserves more neighboring musical content.

Implement the `TODO (Task 6)` methods:

- `JsynFilters.bandStop`: create/add a `FilterBandStop` and set frequency and Q.
- `JsynAudio.connectBandStop`, `connectOutputGain`, `connectSpeakers` and
  `connectRecorder`: reader → filter → output gain, then split the attenuated
  signal to both speaker channels and the mono recorder. The prepared `record`
  method calls these helpers and manages the lifecycle.

```text
SampleLoader → FloatSample → FixedRateMonoReader → FilterBandStop → Multiply
                                                                      ├→ LineOut (L and R)
                                                                      └→ WaveRecorder (mono)
```

JSyn calculates the band-stop coefficients internally. Set `frequency` to 440
and `Q` to 10, and leave the filter's separate `amplitude` port at 1. The `Multiply`
unit controls common listening headroom; it does not set the depth of the notch.
Compare with Task 3's Q=10 result. The startup transient and remaining music mean
the entire filtered waveform will not be zero.

Prepared lifecycle code loads the WAV with `SampleLoader`, sets up the graph,
starts recording before starting the engine, and schedules the sample at audio
time zero. `FixedRateMonoReader` consumes exactly one source sample per engine
frame. This is correct because both files and engine are fixed at 44100 Hz; it
is not a general-purpose resampler. The reader outputs zero when its queue ends.

A queue-completion callback confirms the complete source has played. The recorder
captures the source plus **250 ms of zero-input filter decay**, with an exact frame
limit. A recorder-completion listener confirms the file writer has consumed the
final frames before cleanup. Live playback additionally advances beyond the audio
device's output-buffer latency before stopping, to avoid truncating the audible
end. This adds a small device-dependent delay to the durations listed above.
The WAV header is finalized and the engine stopped
in `finally`, also on failure. Do not replace completion checks with a fixed
wall-clock sleep.

`--task6` opens the audio device and records while playing. `--check6` runs the
same JSyn graph in non-real-time with zero audio channels, so it needs no device.
Both use JSyn's `WaveRecorder`, explicitly configured for mono, signed 16-bit WAV.
The filtered export is not generated by the earlier array filter.

```bash
mvn -q compile exec:java "-Dexec.args=--check6 11111111"
mvn -q compile exec:java "-Dexec.args=--task6 11111111"
```

Outputs: `task6-original.wav`, `task6-filtered.wav`, `task6-spectrum.csv`,
`task6-spectrum.png`, `task6-response.csv`, `task6-settings.txt`.
Both WAVs are 44100 Hz mono and **21.25 seconds**, including the tail; the original
copy has silence appended for alignment. Compare the 440 Hz peak and listen.

## Task 7 — JSyn seven-band music equalizer

Use **`media/intractable-excerpt.wav`** and the exact seven center frequencies,
Q values and dB gains listed in Task 5. Implement **`JsynFilters.cascade`**, marked
`TODO (Task 7)`, connecting seven independent `FilterPeakingEQ` units **in series**.
Implement `JsynFilters.peaking` to configure each stage, and reuse Task 6's
prepared playback/recording lifecycle. The output of the final stage feeds the
common output gain, speakers and recorder.

The peaking filter's `gain` port takes a **linear amplitude multiplier**:
`10^(gainDb/20)`. Do not pass negative dB directly, and do not use the
`10^(gainDb/40)` intermediate A from the coefficient equations. Leave each
stage's separate `amplitude` port at 1.

```text
FixedRateMonoReader → EQ50 → EQ100 → EQ156 → EQ220 → EQ311 → EQ440 → EQ880
                                                                         ↓
                                                           common gain → speakers + recorder
```

```bash
mvn -q compile exec:java "-Dexec.args=--check7 11111111"
mvn -q compile exec:java "-Dexec.args=--task7 11111111"
```

Outputs have the same suffixes as Task 6, with prefix `task7`. Both WAVs contain
**30.25 seconds**: the 30-second music excerpt plus 250 ms of decay/silence.
Inspect the total response and compare the music's bass and lower-midrange balance.
Total center gains include contributions from neighboring bands, as in Task 5.
Task 5's synthetic comparison remains unchanged and is not repeated by Task 7.

For both JSyn tasks, `JsynExperiments` performs a prepared array-based prepass
only to choose common listening headroom and obtain an independent reference.
The output gain keeps the reference peak at most 0.79, leaving a small numerical
margin. The actual filtered export is captured from JSyn. Its decoded samples
must agree with the attenuated reference within **1e-4 absolute error**, allowing
for sample-loading differences and 16-bit quantization. The command prints maximum
error and RMSE and checks the exact output format, duration and peak <=0.8.
All checks compare against a fresh reference and do not require Task 4 or 5 output
files. Different listening scales mean WAV amplitudes cannot be compared directly
between tasks without accounting for their settings files.

The spectrum PNG/CSV uses the actual recorded PCM with common attenuation undone;
quantization remains. It uses the same analysis interval as earlier tasks.
The analytic response CSV is computed from the reference coefficients.

Reference: [JSyn programming guide](https://softsynth.com/jsyn/docs/usersguide/).

## Output conventions and architecture

For Tasks 1–5, every comparison prefix produces:

- `*-original.wav`, `*-filtered.wav`: complete listening copies.
- `*-spectrum.csv`: raw input/output peak amplitudes and dBFS, periodic Hann,
  N=32768, from the identical interval `[44100,76868)` in each signal.
- `*-spectrum.png`: original and filtered spectra overlaid, with full bandwidth
  and a 0–4000 Hz detail view. Blue is the original; dashed orange is the filtered
  signal (an identity copy for Task 1). Both panels share the same dBFS scale,
  with a display floor of -120 dBFS. PNG export also works without a display.
- `*-response.csv`: analytic linear and dB gains at 4097 frequencies from DC to
  Nyquist. This is a dimensionless filter response; its dB column is not dBFS.
- `*-settings.txt`: normalized coefficients, common listening attenuation and
  spectrum interval.
- `*-tones.csv` for synthetic comparisons: input/output tone amplitudes and
  measured/expected gain in dB. Measurements project onto sin/cos over the **last
  one second**, after one second of startup; tones have integer-Hz frequencies.

Open the PNG files directly, or plot CSV columns for a custom frequency range.
Spectrum bins and response-grid frequencies differ; they are independent measurements.
The spectra describe one interval rather than averaging the entire recording.
`-240` means the display floor, not evidence of that much usable precision.

Each original/filtered WAV pair receives the same attenuation factor to keep its
peak at most 0.8, without amplification. Each pair is scaled separately. Compare
levels **within a pair**, or use raw CSV values across experiments. No clipping or
normalization is applied to algorithm buffers or CSV diagnostics. WAV export uses
16-bit PCM quantization. Tasks 1–5 do not append a filter-decay tail; Tasks 6–7
append 250 ms as described above.

Both supplied WAV recordings are signed 16-bit little-endian **mono PCM** at
44100 Hz. `media/siney.wav` has 926100 samples (21 seconds) and is copied unchanged
from the source archive. Tasks 5 and 7's `media/intractable-excerpt.wav` has 1323000
samples (30 seconds), converted from the user-supplied `media/Intractable.mp3`.
The full MP3 is not needed to run the experiments.
The runner checks each WAV's sample rate and sample count. Audio I/O rejects stereo
rather than silently combining or misindexing channels. Samples are decoded by
integer division by 32768; the source's existing saturation cannot be undone by
these filters. No resampling takes place during the experiments.

Algorithm preconditions: finite samples; spectrum buffers have power-of-two N>=2
and positive integer sample rate; filter design uses `0<f0<sampleRate/2`, positive
finite Q and finite gain. `process` and `cascade` also accept empty signals;
analysis and audio export require nonempty signals. Every call starts fresh;
calling `process` separately on consecutive blocks would reset state and is not
a streaming implementation. Initial transients are retained in WAV outputs.

| File | Responsibility |
| --- | --- |
| `FrequencyFilters.java` | Tasks 1–5 algorithm stubs with TODO markers |
| `JsynFilters.java` | Task 6 band-stop and Task 7 peaking/cascade stubs |
| `JsynAudio.java` | JSyn reader, graph connections, playback, recording and cleanup |
| `JsynExperiments.java` | Tasks 6–7 runners, reference checks and recorded-output diagnostics |
| `Fft.java`, `Spectrum.java` | Prepared transform and spectrum representation |
| `Coefficients.java` | Normalized coefficient data and normalization helper |
| `Experiments.java`, `Main.java` | Independent task runners and CLI |
| `Diagnostics.java`, `TestSignals.java` | Analytic response, measurements, CSVs, signals |
| `SpectrumPlot.java` | Prepared spectrum PNG export using JDK Java2D and ImageIO |
| `AudioFiles.java`, `AudioOutput.java`, `Validation.java` | Prepared audio I/O and guards |
| `GenerateParameters.java`, `TaskParameters.java` | Reproducible assignment settings |

## Verification and report

Run the explicit executable checks without an audio device:

```bash
mvn -q test-compile exec:java "-Dexec.mainClass=cbsp.lab04.AlgorithmChecks" "-Dexec.classpathScope=test"
mvn -q test-compile exec:java "-Dexec.mainClass=cbsp.lab04.JsynChecks" "-Dexec.classpathScope=test"
```

These Maven commands include JSyn on the classpath and work on Windows as well.
As in Lab 03, `mvn test` **does not automatically execute** these check classes. `--checkN`
executes its experiment and exports diagnostics, but is not the complete test suite.
Failures in the suite throw exceptions without requiring Java's `-ea` flag.

Checks cover FFT against direct complex DFT, Hann calibration, DC/Nyquist handling,
leakage, a hand-calculated impulse response, filter endpoints, center gains, Q,
measured steady-state response, boost/cut cancellation, input preservation,
state reset, generator constraints, recording format and WAV roundtrip.

The additional `JsynChecks` suite records identity, single-band peaking cut, 440 Hz band-stop
and seven-band cascade fixtures without an audio device. It checks sample alignment, complete
input and tail capture, exact WAV length and agreement with the array reference,
including input lengths not divisible by the JSyn or recorder block size.

For each task report your ID and parameters, predicted and observed changes,
relevant spectrum/response plots and tone measurements, and listening observations.
Explain leakage in Task 1, finite roll-off in Task 2, notch width in Task 3, dB
conversion in Task 4, overlapping-band gains in Task 5, playback/recording
connections in Task 6 and serial JSyn routing in Task 7. Numerical checks do
not substitute for listening.

## Distribution contents

The repository includes the Java source and executable checks, `pom.xml`, these
instructions, and the two WAV fixtures with credits in [media/CREDITS.md](media/CREDITS.md).
Generated `target/`, `output/`, distribution ZIPs, full-length MP3 sources and
local editor/agent configuration are ignored by Git. Both WAV fixtures are small
enough to store directly in the repository; no Git LFS setup is needed.

The starter compiles before any task is implemented. Parameter generation also
works immediately. The check suites and task commands report TODO exceptions
until the methods they call are implemented; this is expected in the starter.
Tasks 2–5 also use Task 1 spectrum analysis for diagnostics. Tasks 6–7 need the
array-based analysis, processing, coefficient design and cascade from Tasks 1–5
for their reference checks, as well as Task 6 graph connections. Task 7 needs
Task 6 output routing even when Task 6's band-stop branch is not used.
`JsynChecks` covers Tasks 6 and 7 together without an audio device. Verify live
speaker connections separately using `--task6` and `--task7`.

The distribution ZIP contains source, checks, instructions, Maven configuration
and the two WAV fixtures. Generated `target/` and `output/`, Git history,
local editor/agent configuration and solution backups are excluded.
