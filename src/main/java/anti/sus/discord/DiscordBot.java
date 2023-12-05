package anti.sus.discord;
import anti.sus.database.DatabaseStorage;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.GatewayIntent;
import java.util.Properties;

public class DiscordBot {
    private final String token;

    private final DatabaseStorage databaseStorage;
    private JDA api;

    public DiscordBot(final Properties envFile, DatabaseStorage databaseStorage){
        this.token = envFile.getProperty("bot-token");
        this.api = JDABuilder.createDefault(token, GatewayIntent.GUILD_MESSAGES, GatewayIntent.MESSAGE_CONTENT).build();
        this.api.addEventListener( new MessageHandler());
        this.databaseStorage = databaseStorage;

    }
    private static class MessageHandler extends ListenerAdapter{
        String content;
        String auther;
        String id;
        String channel;

//        retrieving the data from discord about messages
        public void onMessageReceived(MessageReceivedEvent event){
            if (event.getAuthor().isBot()) return;
            Message message = event.getMessage();
            content = message.getContentRaw();
            auther = String.valueOf(event.getAuthor());
            id = message.getId();
            channel = String.valueOf(event.getChannel());

        }
//        pushing data to db
        public void AddShit(){
            return;
        }

    }

}

