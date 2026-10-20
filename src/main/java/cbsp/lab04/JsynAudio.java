package cbsp.lab04;

import com.jsyn.JSyn;
import com.jsyn.Synthesizer;
import com.jsyn.data.FloatSample;
import com.jsyn.devices.AudioDeviceManager;
import com.jsyn.devices.javasound.JavaSoundAudioDevice;
import com.jsyn.devices.AudioDeviceOutputStream;
import com.jsyn.ports.QueueDataEvent;
import com.jsyn.ports.UnitDataQueueCallback;
import com.jsyn.ports.UnitOutputPort;
import com.jsyn.unitgen.FilterBandStop;
import com.jsyn.unitgen.FixedRateMonoReader;
import com.jsyn.unitgen.LineOut;
import com.jsyn.unitgen.Multiply;
import com.jsyn.util.SampleLoader;
import com.jsyn.util.TransportListener;
import com.jsyn.util.TransportModel;
import com.jsyn.util.WaveRecorder;
import com.softsynth.shared.time.TimeStamp;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/** Prepared lifecycle: the same JSyn graph for live playback and device-free rendering. */
public final class JsynAudio {
    public enum FilterType { BAND_STOP, PEAKING_EQ }
    public static final int SAMPLE_RATE = 44100;
    public static final int TAIL_FRAMES = SAMPLE_RATE/4; // 250 ms of zero-input filter decay
    private JsynAudio() { }

    public static void record(Path input, Path output, FilterType filterType, double[] frequencies, double[] qs,
            double[] gainsDb, double outputGain, boolean play) throws Exception {
        FloatSample sample = SampleLoader.loadFloatSample(input.toFile());
        if (sample.getChannelsPerFrame() != 1 || sample.getFrameRate() != SAMPLE_RATE)
            throw new IllegalArgumentException("JSyn tasks require mono WAV at 44100 Hz.");
        int frames = Math.addExact(sample.getNumFrames(), TAIL_FRAMES);
        Files.createDirectories(output.toAbsolutePath().getParent());
        AtomicReference<AudioDeviceOutputStream> playbackStream = new AtomicReference<>();
        JavaSoundAudioDevice devices = new JavaSoundAudioDevice() {
            @Override
            public AudioDeviceOutputStream createOutputStream(int id, int rate, int channels) {
                AudioDeviceOutputStream stream = super.createOutputStream(id, rate, channels);
                playbackStream.set(stream);
                return stream;
            }
        };
        Synthesizer synth = JSyn.createSynthesizer(devices);
        synth.setRealTime(play);
        WaveRecorder recorder = null;
        CountDownLatch sourceFinished = new CountDownLatch(1), recordingFinished = new CountDownLatch(1);
        AtomicLong recordedFrames = new AtomicLong();
        try {
            FixedRateMonoReader reader = new FixedRateMonoReader();
            synth.add(reader);
            UnitOutputPort filtered;
            if (filterType == FilterType.BAND_STOP) {
                FilterBandStop filter = JsynFilters.bandStop(synth, frequencies[0], qs[0]);
                filtered = connectBandStop(reader, filter);
            } else {
                filtered = JsynFilters.cascade(synth, reader.output, frequencies, qs, gainsDb);
            }
            Multiply gain = new Multiply();
            synth.add(gain);
            connectOutputGain(filtered, gain, outputGain);
            if (play) {
                LineOut speakers = new LineOut();
                synth.add(speakers);
                connectSpeakers(gain, speakers);
                speakers.start();
            }
            // Configure before start: JSyn's default rate is 44100, matching these fixed-rate tasks.
            recorder = new WaveRecorder(synth, output.toFile(), 1, 16);
            connectRecorder(gain, recorder);
            recorder.setMaxRecordingTime(Math.nextUp(frames/(double)SAMPLE_RATE));
            recorder.addTransportListener(new TransportListener() {
                public void positionChanged(TransportModel model, long position) { recordedFrames.set(position); }
                public void stateChanged(TransportModel model, int state) {
                    if (state == TransportModel.STATE_STOPPED) recordingFinished.countDown();
                }
            });
            var command = reader.dataQueue.createQueueDataCommand(sample);
            command.setCallback(new UnitDataQueueCallback() {
                public void started(QueueDataEvent event) { }
                public void looped(QueueDataEvent event) { }
                public void finished(QueueDataEvent event) { sourceFinished.countDown(); }
            });
            // All queue/start commands execute at audio time zero; no leading wall-clock silence.
            synth.scheduleCommand(new TimeStamp(0), command);
            // FixedRateMonoReader outputs zero after the queue ends, allowing the filters to decay.
            recorder.start();
            int device = AudioDeviceManager.USE_DEFAULT_DEVICE;
            synth.start(SAMPLE_RATE, device, 0, device, play ? 2 : 0);
            if (!play) {
                // In non-real-time mode sleepUntil advances the audio engine, without opening a device.
                synth.sleepUntil(frames/(double)SAMPLE_RATE + 0.01);
            }
            long deadline = System.nanoTime()+TimeUnit.SECONDS.toNanos(play ? frames/SAMPLE_RATE+10 : 5);
            while (!recordingFinished.await(50, TimeUnit.MILLISECONDS)) {
                if (play && !synth.isRunning())
                    throw new IOException("JSyn audio output stopped. Use --check6/7 without an audio device.");
                if (System.nanoTime() > deadline) throw new IOException("JSyn recording did not finish.");
            }
            if (sourceFinished.getCount() != 0 || recordedFrames.get() != frames)
                throw new IOException("JSyn recording ended before the complete source and decay tail.");
            if (play) {
                // JavaSound's JSyn stop flushes its device buffer. Advance beyond its actual
                // latency so that all recorded frames have reached the speakers before stop.
                double drainEnd = frames/(double)SAMPLE_RATE + playbackStream.get().getLatency() + 0.02;
                while (synth.getCurrentTime() < drainEnd) {
                    if (!synth.isRunning() || System.nanoTime() > deadline)
                        throw new IOException("JSyn playback stopped before its output buffer drained.");
                    Thread.sleep(10);
                }
            }
        } finally {
            try {
                if (recorder != null) recorder.close(); // finalize the WAV header, even on failure
            } finally {
                synth.stop();
            }
        }
    }
    /** Task 6: route the reader through the band-stop filter and return its output port. */
    private static UnitOutputPort connectBandStop(FixedRateMonoReader reader, FilterBandStop filter) {
        // TODO (Task 6): connect the reader to the filter and return the filtered output.
        throw new UnsupportedOperationException("TODO (Task 6)");
    }

    /** Task 6: apply common attenuation before the playback/recording split. */
    private static void connectOutputGain(UnitOutputPort filtered, Multiply gain, double outputGain) {
        // TODO (Task 6): connect the filtered signal and set the multiplier's other input.
        throw new UnsupportedOperationException("TODO (Task 6)");
    }

    /** Task 6: route the mono result to both speaker channels. */
    private static void connectSpeakers(Multiply gain, LineOut speakers) {
        // TODO (Task 6): connect the attenuated output to left and right channels.
        throw new UnsupportedOperationException("TODO (Task 6)");
    }

    /** Task 6: record the same attenuated output that feeds the speakers. */
    private static void connectRecorder(Multiply gain, WaveRecorder recorder) {
        // TODO (Task 6): connect the attenuated output to the recorder input.
        throw new UnsupportedOperationException("TODO (Task 6)");
    }
}
