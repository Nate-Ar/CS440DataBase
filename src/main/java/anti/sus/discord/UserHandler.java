package anti.sus.discord;

import anti.sus.database.DatabaseStorage;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import static anti.sus.database.DatabaseStorage.SqlQuery;
import static anti.sus.database.DatabaseStorage.safeQuery;

public final class UserHandler extends ListenerAdapter {
    private final DatabaseStorage databaseStorage;

    UserHandler(final DatabaseStorage databaseStorage) {
        this.databaseStorage = databaseStorage;
    }

    @Override
    public void onGuildMemberJoin(@NotNull final GuildMemberJoinEvent event) {
        final User user = event.getUser();
        final long userID = user.getIdLong();
        final String userName = user.getName();
        final SqlQuery addUser = safeQuery("INSERT IGNORE INTO USERS VALUES (?, ?, DEFAULT);", userID, userName);
        this.databaseStorage.update(addUser, null);
    }
}
