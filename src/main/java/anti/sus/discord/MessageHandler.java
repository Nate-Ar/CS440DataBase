package anti.sus.discord;

import anti.sus.database.DatabaseStorage;
import anti.sus.database.FilterWord;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static anti.sus.database.DatabaseStorage.SqlQuery;
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

        this.updateMesssageTable(event);
        this.checkFilteredChannel(event);
    }

    private void updateMesssageTable(MessageReceivedEvent event) {
        final SqlQuery safeMessage = messageUpdateQuery(event);

        this.databaseStorage.update(safeMessage, rowsAffected -> {
            if (rowsAffected != 1) {
                System.err.println("Failed to log message to database.");
                System.err.println("Query: " + safeMessage);
            }
        });
    }

    @NotNull
    private static SqlQuery messageUpdateQuery(MessageReceivedEvent event) {
        final Message message = event.getMessage();
        final String messageContent = message.getContentRaw();
        final long messageTime = message.getTimeCreated().toEpochSecond();
        final long authorId = event.getAuthor().getIdLong();
        final long messageId = message.getIdLong();
        final long channelId = event.getChannel().getIdLong();
//            add to database right away
        final SqlQuery safeMessage = safeQuery("INSERT INTO MESSAGES VALUES (?,?,?,?,?, DEFAULT);", messageId, authorId, channelId, messageTime, messageContent);
        return safeMessage;
    }

    private void checkFilteredChannel(MessageReceivedEvent event) {
        final MessageChannel channel = event.getChannel();
        final long messageChannelId = channel.getIdLong();

        SqlQuery listOfchannelsQuery = safeQuery("SELECT * FROM FILTERED_CHANNELS;");
        databaseStorage.forEachObject(listOfchannelsQuery, row -> {
            if (row.get("channelID").asLong() == messageChannelId) {
                System.out.println("Fired");
                filterThoseWords(event);
            }
        });
    }

// not loading the filter word at all
    private void filterThoseWords(MessageReceivedEvent event) {
        final Message message = event.getMessage();
        final List<FilterWord> filterWords = databaseStorage.getFilteredWords();
        final String originalMessage = message.getContentRaw();
        boolean shouldDelete = false;
        String flaggedWord = "";
        for (final FilterWord filterWord : filterWords) {
            if (originalMessage.contains(filterWord.getFilterWord())) {
                filterWord.setNumViolations(filterWord.getNumViolations() + 1);
                final SqlQuery updateNumViolations = safeQuery("UPDATE FILTERED_WORDS SET numViolations = ? WHERE filterWord = ?", filterWord.getNumViolations(), filterWord.getFilterWord());
                this.databaseStorage.update(updateNumViolations, rowsAffected -> {
                    if (rowsAffected == 0) {
                        System.err.println("Warning! Updating filter word: " + filterWord.getFilterWord() + " in the database failed!");
                    }
                });
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

                System.out.println("numViolations: " + numViolations);

                if (numViolations >= 3) {
                    System.out.println("Trying to ban");
                    final SqlQuery resetToZero = safeQuery("UPDATE USERS SET numViolations = 0 WHERE userID = ?;", authorID);
                    this.databaseStorage.update(resetToZero, rowsAffected -> {
                        if (rowsAffected == 0) {
                            System.err.println("Warning! Banned user but couldn't set their violations back to 0! User affected: " + author);
                        }
                    });
                    event.getGuild().ban(author, 1, TimeUnit.SECONDS).queue();

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
