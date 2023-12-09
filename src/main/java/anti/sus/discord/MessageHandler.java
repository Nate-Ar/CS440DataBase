package anti.sus.discord;

import anti.sus.database.DatabaseStorage;
import anti.sus.database.FilterWord;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import java.util.List;

import static anti.sus.database.DatabaseStorage.safeQuery;
import static anti.sus.database.DatabaseStorage.SqlQuery;

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

    private void filterMessage(MessageReceivedEvent event) {
        final Message message = event.getMessage();
        String messageContent = message.getContentRaw();
        final String originalMessage = messageContent;
        final List<FilterWord> filterWords = databaseStorage.getFilteredWords();
        for (final FilterWord filterWord : filterWords) {
            if (messageContent.contains(filterWord.filterWord())) {
                if (filterWord.replacementText().isEmpty()) {
                    messageContent = "";
                    break;
                }
                messageContent = messageContent.replace(filterWord.filterWord(), filterWord.replacementText());
                if (!originalMessage.equals(messageContent)) {
                    SqlQuery safeMessage = safeQuery("INSERT INTO FLAGGED_MESSAGES VALUES (?, ?);", message.getIdLong(),filterWord.replacementText());
                }
            }
        }

    }
    private void addAdmin(MessageReceivedEvent event){
        final Message message = event.getMessage();
        final String messageContent = message.getContentRaw();
////        asuuming command is "/add-admin:Useranme:
//        if (event.){
//            String[] newAdmin = messageContent.split(":");
//            SqlQuery userId = safeQuery("Select userId From USERS WHERE userName IS ?;",newAdmin[1]);
//            SqlQuery addAdminQuery = safeQuery("INSERT INTO ADMINS VALUES (?);",userId);
//        }
    }

}
