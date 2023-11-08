package anti.sus;

import net.dv8tion.jda.api.JDA;

import java.io.File;
import java.io.IOException;
public final class Main {
    private static final File WORKING_DIRECTORY = new File(System.getProperty("user.dir"));
    private static final File ENV_FILE = new File(WORKING_DIRECTORY, ".env");
    public static void main(String[] args) {
        createFiles();
        final JDA jda;
    }

    private static void createFiles() {
        if (ENV_FILE.isFile()) {
            return;
        }

        try {
            assertState(ENV_FILE.createNewFile(), "isFile() == createNewFile(). Does this user have permission?");
        } catch (final IOException | IllegalStateException ex) {
            System.out.println(ex + "\nCreating base files failed: " + ex.getMessage());
            System.exit(1);
            return;
        }
    }

    public static void assertState(final boolean expression, final String assertionDescription) {
        if (expression) {
            return;
        }

        throw new IllegalStateException("Assertion failed: " + assertionDescription);
    }
}