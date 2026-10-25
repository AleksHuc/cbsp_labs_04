package cbsp.lab04;
import java.util.Locale;
import javax.sound.sampled.LineUnavailableException;
/** Prepared CLI: run from the project root. */
public final class Main {
    private Main() { }
    public static void main(String[] args) {
        Locale.setDefault(Locale.ROOT);
        if (args.length == 0 || (args.length == 1 && args[0].equals("--help"))) { help(); return; }
        if (args.length != 2 || !(args[0].equals("--parameters") || args[0].matches("--(task|check)[1-8]"))) {
            help(); System.exit(2); return;
        }
        try {
            TaskParameters p = GenerateParameters.generate(args[1]);
            if (args[0].equals("--parameters")) {
                System.out.println(p);
            } else Experiments.run(Integer.parseInt(args[0].substring(args[0].length()-1)), p, args[0].startsWith("--task"));
        } catch (LineUnavailableException e) {
            System.err.println("Audio device unavailable. Use --checkN for diagnostics and exported WAV files."); System.exit(2);
        } catch (Exception e) {
            System.err.println(e.getClass().getSimpleName() + ": " + e.getMessage()); System.exit(2);
        }
    }
    private static void help() {
        System.out.println("Usage: Main <command> <eight-digit-student-id>");
        System.out.println("--parameters    Print assigned settings");
        System.out.println("--check1..8     Numerical diagnostics, no audio device needed");
        System.out.println("--task1..8      Playback plus exports; Tasks 6/7 record the live JSyn signal");
        System.out.println("All eight tasks are required. Tasks 1, 3, 4, 6, 8: siney.wav; Tasks 5, 7: intractable-excerpt.wav.");
        System.out.println("Output: output/<student-id>/.");
    }
}
