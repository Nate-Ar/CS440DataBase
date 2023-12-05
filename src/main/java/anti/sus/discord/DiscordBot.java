package anti.sus.discord;
import anti.sus.Main;
import anti.sus.database.DatabaseStorage;
import anti.sus.async.Worker;
import anti.sus.database.DatabaseStorage;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.GatewayIntent;

import java.util.Properties;

public class DiscordBot {
    private static final int NUM_THREADS = 6;
    private final Worker discordWorker;
    private final String token;

    private final DatabaseStorage databaseStorage;
    private JDA api;

    public DiscordBot(final Properties envFile, DatabaseStorage databaseStorage){
        this.discordWorker = new Worker(NUM_THREADS);
        this.token = envFile.getProperty("bot-token");
        this.api = JDABuilder.createDefault(token, GatewayIntent.GUILD_MESSAGES, GatewayIntent.MESSAGE_CONTENT).build();
        this.api.addEventListener( new helloworld());
        this.databaseStorage = databaseStorage;

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

