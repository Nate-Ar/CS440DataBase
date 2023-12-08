package anti.sus;

import anti.sus.database.DatabaseStorage;
import anti.sus.discord.DiscordBot;

import java.util.Properties;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;

public final class DiscordChatFilter {
    private static final Object shutdownLock;

    static {
        shutdownLock = new Object();
    }

    private final Queue<Runnable> scheduledTasks;
    private final DatabaseStorage databaseStorage;
    private final DiscordBot discordBot;
    private boolean running;

    public DiscordChatFilter() {
        running = true;
        final Properties props = loadProperties();
        this.scheduledTasks = new SchedulerQueue<>();
        this.databaseStorage = new DatabaseStorage();
        this.discordBot = new DiscordBot(props, this.databaseStorage);
    }

    public void heartbeat() {
        while (this.running) {
            scheduledTasks.forEach(DiscordChatFilter::runTask);
        }
    }

    private static void runTask(final Runnable runnable) {
        try {
            runnable.run();
        } catch (final Throwable throwable) {
            System.out.println("Exception while running task! " + throwable);
        }
    }

    public void shutdown() {
        synchronized (shutdownLock) {
            if (!this.running) {
                return;
            }

            this.running = false;
        }

        this.discordBot.shutdown();
        this.databaseStorage.shutdown();
    }

    public void runSync(final Runnable task) {
        this.scheduledTasks.add(task);
    }

    private static Properties loadProperties() {
        final Properties properties = new Properties();

        final String botToken = System.getenv("BOT_TOKEN");

        properties.setProperty("bot-token", botToken == null ? "bot token not found in environment!" : botToken);

        return properties;
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
