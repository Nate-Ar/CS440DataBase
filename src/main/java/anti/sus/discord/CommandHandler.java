package anti.sus.discord;

import anti.sus.database.DatabaseStorage;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.unions.GuildChannelUnion;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

import static anti.sus.database.DatabaseStorage.SqlQuery;
import static anti.sus.database.DatabaseStorage.safeQuery;

public class CommandHandler extends ListenerAdapter {
    private final DatabaseStorage databaseStorage;

    CommandHandler(final DatabaseStorage databaseStorage) {
        this.databaseStorage = databaseStorage;
    }

    public void registerCommands(final Guild guild) {
        guild.updateCommands().addCommands(addAdminCommand(), removeAdminCommand(), addChannelToFillterCommand(), removeChannelFromFillterCommand()).submit().join();
    }

    private static CommandData addAdminCommand() {
        return Commands.slash("addadmin", "Add Admin")
                .addOptions(new OptionData(OptionType.USER, "user", "New admin username", true, false));
    }

    private static CommandData addChannelToFillterCommand() {
        return Commands.slash("addchanneltofilter", "Add channel to filterd list")
                .addOptions(new OptionData(OptionType.USER, "channel", "Channel to be added", true, true));
    }

    private static CommandData removeChannelFromFillterCommand() {
        return Commands.slash("rmchannelfromfilter", "Remove channel remove filterd list")
                .addOptions(new OptionData(OptionType.USER, "channel", "Channel to be removed", true, true));
    }


    private static CommandData removeAdminCommand() {
        return Commands.slash("rmadmin", "Remove Admin")
                .addOptions(new OptionData(OptionType.USER, "user", "Admin id for removing", true, false));
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
        SqlQuery addChannelQuery = safeQuery("INSERT IGNORE INTO FILTERED_CHANNELS VALUES (?);", newChannelId);
        databaseStorage.update(addChannelQuery, rowsAffected -> {
            if (rowsAffected == 1) {
                event.getHook().sendMessage("Channel added: " + newChannelId).queue();
            } else {
                event.getHook().sendMessage("That Channel is already being filtered").queue();
            }
        });
    }

    private void removeChannelFromFilterList(SlashCommandInteractionEvent event) {
        final GuildChannelUnion channel = event.getOption("channel").getAsChannel();
        final long oldChannelId = channel.getIdLong();
        SqlQuery listOfchannelsQuery = safeQuery("SELECT * FROM FILTERED_CHANNELS;");
        databaseStorage.forEachObject(listOfchannelsQuery, row -> {
            if (row.get("channelID").asLong() == oldChannelId) {
                removeChannelFromList(event, oldChannelId);
            }
        });
        event.reply("Channel is not being filtered").setEphemeral(true).queue();
    }

    private void removeChannelFromList(SlashCommandInteractionEvent event, long oldChannelId) {
        event.deferReply(true).queue();

        SqlQuery addChannelQuery = safeQuery("DELETE FROM FILTERED_CHANNELS WHERE channelID = ?;", oldChannelId);
        databaseStorage.update(addChannelQuery, rowsAffected -> {
            if (rowsAffected == 1) {
                event.getHook().sendMessage("Channel Removed: " + oldChannelId).queue();
            } else {
                event.getHook().sendMessage("That Channel is not being filtered").queue();
            }
        });
    }
}


