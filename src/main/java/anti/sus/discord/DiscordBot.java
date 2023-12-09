package anti.sus.discord;

import anti.sus.database.DatabaseStorage;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.cache.CacheFlag;

import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Properties;

public class DiscordBot {
    private static final Duration MAX_SHUTDOWN_TIME = Duration.ofSeconds(15L);
    private static final Collection<GatewayIntent> INTENTS = List.of(GatewayIntent.GUILD_MESSAGES, GatewayIntent.MESSAGE_CONTENT);
    private static final Collection<CacheFlag> DISABLED_CACHES = List.of(CacheFlag.VOICE_STATE, CacheFlag.EMOJI, CacheFlag.STICKER, CacheFlag.SCHEDULED_EVENTS, CacheFlag.ACTIVITY, CacheFlag.CLIENT_STATUS, CacheFlag.ONLINE_STATUS);
    private final JDA api;

    public DiscordBot(final Properties envFile, DatabaseStorage databaseStorage) {
        final String token = envFile.getProperty("bot-token");
        final JDABuilder jdaBuilder = JDABuilder.create(token, INTENTS).disableCache(DISABLED_CACHES);
        this.api = jdaBuilder.build();
        this.api.addEventListener(new MessageHandler(databaseStorage));
    }

    public void shutdown() {
        this.api.shutdown();

        boolean shutdownGracefully;

        try {
            shutdownGracefully = this.api.awaitShutdown(MAX_SHUTDOWN_TIME);
        } catch (final InterruptedException ex) {
            System.out.println("Discord bot shutdown was interrupted!");
            shutdownGracefully = false;
        }

        if (!shutdownGracefully) {
            System.out.println("Shutting down the discord bot was either interrupted or timed out!");
        }
    }



}

