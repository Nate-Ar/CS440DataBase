package anti.sus.discord;

import anti.sus.database.DatabaseStorage;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import org.jetbrains.annotations.NotNull;

import static anti.sus.database.DatabaseStorage.SqlQuery;
import static anti.sus.database.DatabaseStorage.safeQuery;

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

    private static class MessageHandler extends ListenerAdapter {
        private final DatabaseStorage databaseStorage;

        private MessageHandler(final DatabaseStorage databaseStorage) {
            this.databaseStorage = databaseStorage;
        }

        //        retrieving the data from discord about messages
        @Override
        public void onMessageReceived(final MessageReceivedEvent event) {
            if (event.getAuthor().isBot()) {
                return;
            }

            SqlQuery safeMessage = getMessageUpdateQuery(event);
            databaseStorage.update(safeMessage, rowsAffected -> {
                if (rowsAffected != 1) {
                    System.err.println("Failed to log message to database.");
                    System.err.println("Query: " + safeMessage);
                }
            });
        }

        @NotNull
        private static SqlQuery getMessageUpdateQuery(MessageReceivedEvent event) {
            final Message message = event.getMessage();
            final String messageContent = message.getContentRaw();
            final long messageTime = message.getTimeCreated().toEpochSecond();
            final long authorId = event.getAuthor().getIdLong();
            final long messageId = message.getIdLong();
            final long channelId = event.getChannel().getIdLong();
//            add to database right away
            return safeQuery("INSERT INTO MESSAGES VALUES (?,?,?,?,?, DEFAULT);", messageId, authorId, channelId, messageTime, messageContent);
        }

    }

}

