package anti.sus.discord;

import anti.sus.database.DatabaseStorage;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.guild.GuildJoinEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import static anti.sus.database.DatabaseStorage.SqlQuery;
import static anti.sus.database.DatabaseStorage.safeQuery;

public final class UserHandler extends ListenerAdapter {
    private final DatabaseStorage databaseStorage;
    private final CommandHandler commandHandler;

    UserHandler(final DatabaseStorage databaseStorage, final CommandHandler commandHandler) {
        this.databaseStorage = databaseStorage;
        this.commandHandler = commandHandler;
    }

    @Override
    public void onGuildMemberJoin(@NotNull final GuildMemberJoinEvent event) {
        final User user = event.getUser();
        final long userID = user.getIdLong();
        final String userName = user.getName();
        final SqlQuery addUser = safeQuery("INSERT IGNORE INTO USERS VALUES (?, ?, DEFAULT);", userID, userName);
        this.databaseStorage.update(addUser, null);
    }

    @Override
    public void onGuildJoin(@NotNull final GuildJoinEvent event) {
        event.getGuild().getMembers().forEach(member -> {
            if (member.getUser().isBot()) {
                return;
            }

            final long userId = member.getIdLong();
            final String userName = member.getUser().getName();
            final SqlQuery addExistingUserQuery = safeQuery("INSERT IGNORE INTO USERS VALUES (?, ?, DEFAULT);", userId, userName);

            this.databaseStorage.update(addExistingUserQuery, null);
        });
        this.commandHandler.registerCommands(event.getGuild());
    }
}
