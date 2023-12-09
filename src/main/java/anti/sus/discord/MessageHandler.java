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

        this.getMessageUpdateQuery(event);
        this.filterMessage(event);
    }

    private void getMessageUpdateQuery(MessageReceivedEvent event) {
        final Message message = event.getMessage();
        final SqlQuery safeMessage = getMessageUpdateQuery(event, message);

        this.databaseStorage.update(safeMessage, rowsAffected -> {
            if (rowsAffected != 1) {
                System.err.println("Failed to log message to database.");
                System.err.println("Query: " + safeMessage);
            }
        });
    }

    @NotNull
    private static SqlQuery getMessageUpdateQuery(MessageReceivedEvent event, Message message) {
        final String messageContent = message.getContentRaw();
        final long messageTime = message.getTimeCreated().toEpochSecond();
        final long authorId = event.getAuthor().getIdLong();
        final long messageId = message.getIdLong();
        final long channelId = event.getChannel().getIdLong();
//            add to database right away
        final SqlQuery safeMessage = safeQuery("INSERT INTO MESSAGES VALUES (?,?,?,?,?, DEFAULT);", messageId, authorId, channelId, messageTime, messageContent);
        return safeMessage;
    }

    private void filterMessage(MessageReceivedEvent event) {
        final List<FilterWord> filterWords = databaseStorage.getFilteredWords();
        final Message message = event.getMessage();
        final String originalMessage = message.getContentRaw();
        boolean shouldDelete = false;
        String flaggedWord = "";

        for (final FilterWord filterWord : filterWords) {
            if (originalMessage.contains(filterWord.getFilterWord())) {
                filterWord.setNumViolations(filterWord.getNumViolations() + 1);
                flaggedWord = filterWord.getFilterWord();
                shouldDelete = true;

                break;
            }
        }

        if (shouldDelete) {
            final SqlQuery flaggedMessageQuery = safeQuery("INSERT INTO FLAGGED_MESSAGES VALUES (?, ?);", message.getIdLong(), flaggedWord);
            final SqlQuery updateFlaggedAttribute = safeQuery("UPDATE MESSAGES SET filtered = TRUE WHERE messageID = ?", message.getIdLong());

            this.databaseStorage.update(flaggedMessageQuery, null);
            this.databaseStorage.update(updateFlaggedAttribute, null);
        }
    }

}
