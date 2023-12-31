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
    private static final Collection<GatewayIntent> INTENTS = List.of(GatewayIntent.GUILD_MESSAGES, GatewayIntent.MESSAGE_CONTENT, GatewayIntent.GUILD_MEMBERS);
    private static final Collection<CacheFlag> DISABLED_CACHES = List.of(CacheFlag.VOICE_STATE, CacheFlag.EMOJI, CacheFlag.STICKER, CacheFlag.SCHEDULED_EVENTS, CacheFlag.ACTIVITY, CacheFlag.CLIENT_STATUS, CacheFlag.ONLINE_STATUS);
    private final JDA api;

    public DiscordBot(final Properties envFile, DatabaseStorage databaseStorage) {
        final String token = envFile.getProperty("bot-token");
        final JDABuilder jdaBuilder = JDABuilder.create(token, INTENTS).disableCache(DISABLED_CACHES);
        this.api = startup(jdaBuilder);

        final CommandHandler commandHandler = new CommandHandler(databaseStorage);
        this.api.getGuilds().forEach(commandHandler::registerCommands);

        this.api.addEventListener(new MessageHandler(databaseStorage));
        this.api.addEventListener(new UserHandler(databaseStorage, commandHandler));
        this.api.addEventListener(commandHandler);
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

    private static JDA startup(final JDABuilder jdaBuilder) {
        try {
            return jdaBuilder.build().awaitReady();
        } catch (final InterruptedException ex) {
            throw new IllegalStateException("Thread interrupted while logging in to the Bridge!", ex);
        }
    }
}