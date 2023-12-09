package anti.sus.discord;

import anti.sus.database.DatabaseStorage;
import anti.sus.database.FilterWord;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.Channel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.concurrent.TimeUnit;

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
        this.filterMessageCheck(event);
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

    private void filterMessageCheck(MessageReceivedEvent event) {
        final Message message = event.getMessage();
        final MessageChannel channel = message.getChannel();
        final long messageChannelId = channel.getIdLong();

        SqlQuery listOfchannelsQuery = safeQuery("SELECT * FROM FILTERED_CHANNELS;");
        databaseStorage.forEachObject(listOfchannelsQuery, row -> {
            if (row.get("channelID").asLong() == messageChannelId) {
                filterThoseWords(message, event);
            }
        });
    }

    private void filterThoseWords(Message message, MessageReceivedEvent event) {
        final List<FilterWord> filterWords = databaseStorage.getFilteredWords();
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
            final User author = message.getAuthor();
            final long authorID = author.getIdLong();
            final SqlQuery getTotalViolations = safeQuery("SELECT numViolations FROM USERS WHERE userID = ?", authorID);

            message.delete().queue();
            this.databaseStorage.update(flaggedMessageQuery, null);
            this.databaseStorage.update(updateFlaggedAttribute, null);
            this.databaseStorage.forEachObject(getTotalViolations, row -> {
                int numViolations = row.get("numViolations").asInt();

                if (numViolations > 3) {
                    event.getGuild().ban(author, 1, TimeUnit.SECONDS).queue();

                    final SqlQuery resetToZero = safeQuery("UPDATE USERS SET numViolations = 0 WHERE userID = ?;", authorID);
                    this.databaseStorage.update(resetToZero, rowsAffected -> {
                        if (rowsAffected == 0) {
                            System.err.println("Warning! Banned user but couldn't set their violations back to 0! User affected: " + author);
                        }
                    });

                    return;
                }

                message.getChannel().sendMessage(author.getAsMention() + "You have sent a message containing restricted words! You have received a warning. Warnings: " + (numViolations + 1) + " You have " + (3 - (numViolations + 1)) + " warnings left until you are banned!").queue();
                final SqlQuery updateNumViolations = safeQuery("UPDATE USERS SET numViolations = ? WHERE userID = ?", numViolations + 1, authorID);
                this.databaseStorage.update(updateNumViolations, rowsAffected -> {
                    if (rowsAffected == 0) {
                        System.err.println("Warning! Failed to update number of violations for user: " + author);
                    }
                });
            });
        }
    }
}
