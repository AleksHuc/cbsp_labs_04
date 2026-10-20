package cbsp.lab04;
import java.util.Random;
/** Keep the version and random call order unchanged. */
public final class GenerateParameters {
    public static final String VERSION = "lab04-v1";
    private GenerateParameters() { }
    public static TaskParameters generate(String studentId) {
        if (studentId == null || !studentId.matches("[0-9]{8}"))
            throw new IllegalArgumentException("Student ID must contain exactly eight digits.");
        Random r = new Random(Long.parseLong(studentId));
        return new TaskParameters(VERSION, studentId, 450 + 25*r.nextInt(7),
                new double[]{440, 880, 1400}[r.nextInt(3)], 3 + r.nextInt(4));
    }
}
