package anti.sus;

import anti.sus.database.DatabaseStorage;
import anti.sus.discord.DiscordBot;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.GatewayIntent;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;

import static anti.sus.database.DatabaseStorage.*;
import static anti.sus.discord.DiscordBot.*;

public final class Main {
    private static final File WORKING_DIRECTORY = new File(System.getProperty("user.dir"));
    private static final File ENV_FILE = new File(WORKING_DIRECTORY, ".env");
    private static final Properties ENV_PROPS = loadProperties(ENV_FILE);

    private static final Queue<Runnable> scheduledTasks;

    static {
        scheduledTasks = new SchedulerQueue<>();
    }

    public static void main(String[] args) throws DatabaseException {
        createFiles();
        final DatabaseStorage databaseStorage = new DatabaseStorage(ENV_PROPS);
        final DiscordBot discordBot = new DiscordBot(ENV_PROPS);
        final JDA jda;
        JDA api = JDABuilder.createDefault(discordBot.Token, GatewayIntent.GUILD_MESSAGES, GatewayIntent.MESSAGE_CONTENT).build();
        api.addEventListener( new helloworld());
        //Example of retrieving a String departmentName from a table employees, where
        //the employee is named Bill, in this example we're assuming "Bill" is some untrusted input
        final SqlQuery query = safeQuery("SELECT departmentName FROM employees WHERE employeeName = ?", "Bill");
        databaseStorage.forEachObject(query, queryResult ->
            doSomethingWithTheDepartmentName(queryResult.get("departmentName").asString()));


        databaseStorage.shutdown();
        scheduledTasks.forEach(Runnable::run);
    }

    public static void runSync(final Runnable task) {
        scheduledTasks.add(task);
    }

    private static void doSomethingWithTheDepartmentName(final String departmentName) {
        System.out.println("Department name: " + departmentName);
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

    public static Properties loadProperties(final File propertiesFile) throws DatabaseException {
        final Properties properties = new Properties();

        try (final FileInputStream fileInputStream = new FileInputStream(propertiesFile)) {
            properties.load(fileInputStream);
        } catch (final FileNotFoundException ex) {
            throw new DatabaseException("Caller didn't perform file exist check!", ex);
        } catch (final IOException ex) {
            throw new DatabaseException("IOException while loading .env file!", ex);
        }

        return properties;
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
    public static class helloworld extends ListenerAdapter
    {
        @Override
        public void onMessageReceived(MessageReceivedEvent event)
        {
            if (event.getAuthor().isBot()) return;
            // We don't want to respond to other bot accounts, including ourself
            Message message = event.getMessage();
            String content = message.getContentRaw();
            // getContentRaw() is an atomic getter
            // getContentDisplay() is a lazy getter which modifies the content for e.g. console view (strip discord formatting)
            if (content.equals("!ping"))
            {
                MessageChannel channel = event.getChannel();
                channel.sendMessage("Pong!").queue(); // Important to call .queue() on the RestAction returned by sendMessage(...)
            }
        }
    }
}