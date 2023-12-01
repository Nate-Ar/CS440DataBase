package anti.sus;

import net.dv8tion.jda.api.JDA;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public final class Main {
    private static final File WORKING_DIRECTORY = new File(System.getProperty("user.dir"));
    private static final File ENV_FILE = new File(WORKING_DIRECTORY, ".env");
    private static final Queue<Runnable> scheduledTasks;

    static {
        scheduledTasks = new SchedulerQueue<>();
    }

    public static void main(String[] args) throws DatabaseException {
        createFiles();
        final JDA jda;
        scheduledTasks.forEach(Runnable::run);
    public static void runSync(final Runnable task) {
        scheduledTasks.add(task);
    }
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

        try (final InputStream propertiesStream = Main.class.getClassLoader().getResourceAsStream(".env.example");
             final FileOutputStream envFileStream = new FileOutputStream(ENV_FILE)) {
            if (propertiesStream == null) {
                throw new AssertionError("couldn't find resource .env.example!");
            }

            envFileStream.write(propertiesStream.readAllBytes());
        } catch (final FileNotFoundException ex) {
            throw new AssertionError("createNewFile() returned true but didn't create a file!", ex);
        } catch (final IOException ex) {
            System.out.println("error: " + ex);
        }

        System.out.println(".env file created! Please populate it with your Discord Application Token and restart the application!");
        System.exit(2);
    }

    public static void assertState(final boolean expression, final String assertionDescription) {
        if (expression) {
            return;
        }

        throw new IllegalStateException("Assertion failed: " + assertionDescription);
    }

    //Utility class that wipes the queue whenever forEach is called
    private static final class SchedulerQueue<E> extends ConcurrentLinkedQueue<E> {
        @Override
        public void forEach(final Consumer<? super E> elementConsumer) {
            super.forEach(elementConsumer);
            super.clear();
        }
    }
}