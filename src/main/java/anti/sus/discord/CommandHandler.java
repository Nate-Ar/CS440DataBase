package anti.sus.discord;

import anti.sus.database.DatabaseStorage;
import anti.sus.database.FilterWord;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.unions.GuildChannelUnion;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import org.jetbrains.annotations.NotNull;

import static anti.sus.database.DatabaseStorage.SqlQuery;
import static anti.sus.database.DatabaseStorage.safeQuery;

public class CommandHandler extends ListenerAdapter {
    private final DatabaseStorage databaseStorage;

    CommandHandler(final DatabaseStorage databaseStorage) {
        this.databaseStorage = databaseStorage;
    }

    public void registerCommands(final Guild guild) {
        guild.updateCommands().addCommands(addAdminCommand(), removeAdminCommand(),
                addChannelToFillterCommand(), removeChannelFromFillterCommand(),
                addFilterWordCommand(), removeFilterWordCommand()).submit().join();
    }

    private static CommandData addAdminCommand() {
        return Commands.slash("addadmin", "Add Admin")
                .addOptions(new OptionData(OptionType.USER, "user", "New admin username", true, false));
    }

    private static CommandData addChannelToFillterCommand() {
        return Commands.slash("addchanneltofilter", "Add channel to filterd list")
                .addOptions(new OptionData(OptionType.CHANNEL, "channel", "Channel to be added", true, false));
    }

    private static CommandData removeChannelFromFillterCommand() {
        return Commands.slash("rmchannelfromfilter", "Remove channel remove filterd list")
                .addOptions(new OptionData(OptionType.CHANNEL, "channel", "Channel to be removed", true, false));
    }


    private static CommandData removeAdminCommand() {
        return Commands.slash("rmadmin", "Remove Admin")
                .addOptions(new OptionData(OptionType.USER, "user", "Admin id for removing", true, false));
    }

    private static CommandData addFilterWordCommand() {
        return Commands.slash("addfilterword", "adds new filter word to table")
                .addOptions(new OptionData(OptionType.STRING, "word", "Word to be added", true, true));
    }

    private static CommandData removeFilterWordCommand() {
        return Commands.slash("rmfilterword", "removes filter word from table")
                .addOptions(new OptionData(OptionType.STRING, "word", "Word to be removed", true, true));
    }


    @Override
    public void onSlashCommandInteraction(final SlashCommandInteractionEvent event) {
        final String commandName = event.getName();
        System.out.println("commandName: " + commandName);

        switch (commandName) {
            case "addadmin":
                addAdmin(event);
                break;
            case "rmadmin":
                removeAdmin(event);
                break;
            case "rmchannelfromfilter":
                removeChannelFromFilterList(event);
                break;

            case "addchanneltofilter":
                addChannelToFilterList(event);
                break;

            case "addfilterword":
                addFilterWordToList(event);
                break;

            case "rmfilterword":
                removeFilterWordFromList(event);
                break;

            default:
        }
    }

    private void addAdmin(SlashCommandInteractionEvent event) {
        final User user = event.getOption("user").getAsUser();

        if (user.isBot()) {
            event.reply("You can't add bots as admins!").setEphemeral(true).queue();
            return;
        }

        event.deferReply(true).queue();
        long newAdminId = user.getIdLong();

        SqlQuery adminAdminQuery = safeQuery("INSERT IGNORE INTO ADMINS VALUES (?);", newAdminId);
        databaseStorage.update(adminAdminQuery, rowsAffected -> {
            if (rowsAffected == 1) {
                event.getHook().sendMessage("Admin added: " + user.getName()).queue();
            } else {
                event.getHook().sendMessage("That user is already an admin!").queue();
            }
        });
    }

    private void removeAdmin(SlashCommandInteractionEvent event) {
        final User user = event.getOption("user").getAsUser();

        if (user.isBot()) {
            event.reply("Bots can't be admins!").setEphemeral(true).queue();
            return;
        }

        event.deferReply(true).queue();

        final long oldAdminId = user.getIdLong();
        final SqlQuery adminAdminQuery = safeQuery("DELETE FROM ADMINS WHERE userID = ?;", oldAdminId);
        databaseStorage.update(adminAdminQuery, rowsAffected -> {
            if (rowsAffected == 1) {
                event.getHook().sendMessage("Admin removed: " + user.getName()).queue();
            } else {
                event.getHook().sendMessage("That user is not an admin!").queue();
            }
        });
    }

    private void addChannelToFilterList(SlashCommandInteractionEvent event) {
        final GuildChannelUnion channel = event.getOption("channel").getAsChannel();

        if (channel.getType().isAudio()) {
            event.reply("Voice can't be filtered").setEphemeral(true).queue();
            return;
        }
        event.deferReply(true).queue();

        long newChannelId = channel.getIdLong();
        User user = event.getUser();
        SqlQuery addChannelQuery = safeQuery("INSERT IGNORE INTO FILTERED_CHANNELS VALUES (?);", newChannelId);
        databaseStorage.update(addChannelQuery, rowsAffected -> {
            if (rowsAffected == 1) {
                event.getHook().sendMessage(user.getName()+ " added " + channel.getName() + " to FilteredChannelTable" ).queue();
            } else {
                event.getHook().sendMessage("That Channel is already being filtered").queue();
            }
        });
    }
// makes sure channel id is on the filter list in the first place
    private void removeChannelFromFilterList(SlashCommandInteractionEvent event) {
        final GuildChannelUnion channel = event.getOption("channel").getAsChannel();
        final long oldChannelId = channel.getIdLong();
        final User user = event.getUser();

        event.deferReply(true).queue();


        SqlQuery addFilterWordQuery = safeQuery("DELETE FROM FILTERED_CHANNELS WHERE channelID = ?;", oldChannelId);
        databaseStorage.update(addFilterWordQuery, rowsAffected -> {
            if (rowsAffected == 1) {
                event.getHook().sendMessage(user.getName() + " Removed " + channel.getName() + " From FilteredChannelTable").queue();
            } else {
                event.getHook().sendMessage("Not a FilterWord").queue();
            }
        });
    }

    private void addFilterWordToList(SlashCommandInteractionEvent event){
        final String newFilterWord = event.getOption("word").getAsString();
        final User user = event.getUser();

        event.deferReply(true).queue();

        SqlQuery addFilterWordQuery = safeQuery("INSERT IGNORE INTO FILTERED_WORDS VALUES (?,0);", newFilterWord);
        databaseStorage.update(addFilterWordQuery, rowsAffected -> {
            if (rowsAffected == 1) {
                event.getHook().sendMessage(user.getName()+" added " + newFilterWord).queue();
                this.databaseStorage.addFilterWord(new FilterWord(newFilterWord, 0));
            } else {
                event.getHook().sendMessage("already a FilterWord").queue();
            }
        });

    }

    private void removeFilterWordFromList(SlashCommandInteractionEvent event){
        final String oldFilterWord = event.getOption("word").getAsString();@NotNull
        final User user = event.getUser();

        event.deferReply(true).queue();

        SqlQuery addFilterWordQuery = safeQuery("DELETE FROM FILTERED_WORDS WHERE filterWord = ?;", oldFilterWord);
        databaseStorage.update(addFilterWordQuery, rowsAffected -> {
            if (rowsAffected == 1) {
                event.getHook().sendMessage(user.getName()+" Removed " + oldFilterWord + " From FilteredWordTable").queue();
                this.databaseStorage.removeFilterWord(new FilterWord(oldFilterWord, 0));
            } else {
                event.getHook().sendMessage("Not a FilterWord").queue();
            }
        });

    }
}


