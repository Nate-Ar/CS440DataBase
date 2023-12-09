package anti.sus.discord;

import anti.sus.database.DatabaseStorage;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import static anti.sus.database.DatabaseStorage.safeQuery;

class MessageHandler extends ListenerAdapter {
    private final DatabaseStorage databaseStorage;

    MessageHandler(final DatabaseStorage databaseStorage) {
        this.databaseStorage = databaseStorage;
    }

    //        retrieving the data from discord about messages
    @Override
    public void onMessageReceived(final MessageReceivedEvent event) {
        if (event.getAuthor().isBot()) {
            return;
        }

        DatabaseStorage.SqlQuery safeMessage = getMessageUpdateQuery(event);
        databaseStorage.update(safeMessage, rowsAffected -> {
            if (rowsAffected != 1) {
                System.err.println("Failed to log message to database.");
                System.err.println("Query: " + safeMessage);
            }
        });
    }

    @NotNull
    private static DatabaseStorage.SqlQuery getMessageUpdateQuery(MessageReceivedEvent event) {
        final Message message = event.getMessage();
        final String messageContent = message.getContentRaw();
        final long messageTime = message.getTimeCreated().toEpochSecond();
        final long authorId = event.getAuthor().getIdLong();
        final long messageId = message.getIdLong();
        final long channelId = event.getChannel().getIdLong();
//            add to database right away
        return safeQuery("INSERT INTO MESSAGES VALUES (?,?,?,?,?, DEFAULT);", messageId, authorId, channelId, messageTime, messageContent);
    }

    private void filterMessage(MessageReceivedEvent event){
        final Message message = event.getMessage();
        final String messageContent =  message.getContentRaw();

    }

}
