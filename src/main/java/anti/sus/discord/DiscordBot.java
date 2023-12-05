package anti.sus.discord;
import java.io.File;
import java.util.Properties;

public class DiscordBot {
    public String Token="";

    public DiscordBot(final Properties envFile){
        this.Token = envFile.getProperty("bot-token");
    }
}