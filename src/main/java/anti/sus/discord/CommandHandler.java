package anti.sus.discord;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

public class CommandHandler {
    CommandHandler(JDA api){
        api.updateCommands().addCommands(getAdminData());
    }

    private static CommandData getAdminData() {
        return Commands.slash("addAdmin", "Add Admin")
                .addOptions(new OptionData(OptionType.USER, "User", "New admin username",true,true));
    }
}


