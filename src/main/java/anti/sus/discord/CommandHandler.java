package anti.sus.discord;

import anti.sus.database.DatabaseStorage;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

public class CommandHandler {
    private final DatabaseStorage databaseStorage;

    CommandHandler(final DatabaseStorage databaseStorage) {
        this.databaseStorage = databaseStorage;
    }

    public void registerCommands(final Guild guild) {
        guild.updateCommands().addCommands(getAdminData()).submit().join();
    }

    private static CommandData getAdminData() {
        return Commands.slash("addAdmin", "Add Admin")
                .addOptions(new OptionData(OptionType.USER, "User", "New admin username",true,true));
    }

    public class SayCommand extends ListenerAdapter {
        @Override
        public void onSlashCommandInteraction(final SlashCommandInteractionEvent event) {
            if (event.getName().equals("addAdmin")){
                addAdmin(event);
            }
        }
    }
    private void addAdmin(SlashCommandInteractionEvent event){
         String newAdminId = event.getOption("User").getAsUser().getId();

    }
}


