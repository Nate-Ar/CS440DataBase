package anti.sus.discord;

import anti.sus.database.DatabaseStorage;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

import static anti.sus.database.DatabaseStorage.safeQuery;
import static anti.sus.database.DatabaseStorage.SqlQuery;

public class CommandHandler {
    private final DatabaseStorage databaseStorage;

    CommandHandler(final DatabaseStorage databaseStorage) {
        this.databaseStorage = databaseStorage;
    }

    public void registerCommands(final Guild guild) {
        guild.updateCommands().addCommands(addAdminCommand(),removeAdminCommand()).submit().join();
    }

    private static CommandData addAdminCommand() {
        return Commands.slash("addadmin", "Add Admin")
                .addOptions(new OptionData(OptionType.USER, "User", "New admin username",true,true));
    }

    private static CommandData removeAdminCommand() {
        return Commands.slash("rmadmin", "Remove Admin")
                .addOptions(new OptionData(OptionType.USER, "User", "Admin id for removing",true,true));
    }


    public class SayCommand extends ListenerAdapter {
        @Override
        public void onSlashCommandInteraction(final SlashCommandInteractionEvent event) {
            if (event.getName().equals("addadmin")){
                addAdmin(event);
            } else if (event.getName().equals("rmadmin")) {
                removeAdmin(event);
            }
        }
    }
    private void addAdmin(SlashCommandInteractionEvent event){
         String newAdminId = event.getOption("User").getAsUser().getId();
         SqlQuery adminAdminQuery = safeQuery("INSERT INTO ADMINS VALUE (?);",newAdminId);
         databaseStorage.update(adminAdminQuery,null);
    }
    private void removeAdmin(SlashCommandInteractionEvent event){
        String newAdminId = event.getOption("User").getAsUser().getId();
        SqlQuery adminAdminQuery = safeQuery("DELETE FROM ADMINS WHERE (?);",newAdminId);
        databaseStorage.update(adminAdminQuery,null);
    }
}


