package anti.sus.discord;
import anti.sus.async.Worker;

import java.util.Properties;

public class DiscordBot {
    private static final int NUM_THREADS = 6;
    private final Worker discordWorker;
    private final String token;

    public DiscordBot(final Properties envFile){
        this.discordWorker = new Worker(NUM_THREADS);
        this.token = envFile.getProperty("bot-token");
    }
}