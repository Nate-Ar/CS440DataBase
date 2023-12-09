package anti.sus.database;

import anti.sus.async.Worker;
import org.jetbrains.annotations.Nullable;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

public final class DatabaseStorage {
    private static final byte NUM_THREADS = 4;
    private final Connection connection;
    private final Worker databaseWorker;
    private final List<FilterWord> filteredWords;

    public DatabaseStorage() throws DatabaseException {
        this.databaseWorker = new Worker(NUM_THREADS);
        this.filteredWords = new CopyOnWriteArrayList<>();

        try {
            final String host = "127.0.0.1";
            final String port = "3306";
            final String database = "discordfilter";
            final String url = "jdbc:mariadb://" + host + ':' + port + '/' + database;

            System.out.println("Attempting to connect to database on: " + url);
            final String username = "discordfilter";
            final String password = "";
            this.connection = DriverManager.getConnection(url, username, password);

            this.initTables();
            this.loadFilterWords();
        } catch (final SQLException ex) {
            throw new DatabaseException("Initializing database connection failed!", ex);
        }
    }

    public List<FilterWord> getFilteredWords() {
        return new ArrayList<>(filteredWords);
    }

    private void loadFilterWords() {
        SqlQuery allFilters = safeQuery("SELECT * FROM FILTERED_WORDS;");
        forEachObject(allFilters, response -> {
            final String filterWordString = response.get("filterWord").asString();
            final String replacement = response.get("replacement").asString();
            final FilterWord filterWord = new FilterWord(filterWordString,replacement);

            filteredWords.add(filterWord);
        });
    }
    /**
     * Get some data from the database.
     * If an update is desired, don't use this method, use DatabaseStorage#update() instead.
     *
     * @param sqlQuery The SQL query, built with safeQuery()
     * @param rowsConsumer What to do with the rows returned
     */
    public void getRows(final SqlQuery sqlQuery, final Consumer<List<Map<String, DatabaseEntry>>> rowsConsumer) {
        final CompletableFuture<List<Map<String, DatabaseEntry>>> queryResultFuture =
                this.databaseWorker.submitWork(() -> getObjects0(sqlQuery));

        queryResultFuture.thenAccept(rowsConsumer);
    }

    public void forEachObject(final SqlQuery sqlQuery, final Consumer<Map<String, DatabaseEntry>> rowConsumer) {
        getRows(sqlQuery, rows -> rows.forEach(rowConsumer));
    }

    /**
     * Update a table in this database. This method is to be used for DDL and DML statements.
     * If you need to fetch data at all from the table, or execute queries which
     * both modify and fetch data, use DatabaseStorage#getObjects() instead.
     *
     * @param sqlQuery The SQL query, constructed with safeQuery()
     * @param numRowsAffectedConsumer What to do with the amount of rows affected by the query
     */
    public void update(final SqlQuery sqlQuery, @Nullable final Consumer<Integer> numRowsAffectedConsumer) {
        final CompletableFuture<Integer> intFuture =
                this.databaseWorker.submitWork(() -> update0(sqlQuery));

        if (numRowsAffectedConsumer != null) {
            intFuture.thenAccept(numRowsAffectedConsumer);
        }
    }

    public void initTables() {
        System.out.println("Initializing tables...");
        final SqlQuery messagesTable = safeQuery("CREATE TABLE IF NOT EXISTS MESSAGES (messageID BIGINT PRIMARY KEY, authorID BIGINT NOT NULL, channelID BIGINT NOT NULL, timeSent BIGINT NOT NULL, messageContent TEXT NOT NULL, filtered BOOLEAN NOT NULL DEFAULT FALSE);");
        this.update(messagesTable, null);
        final SqlQuery usersTable = safeQuery("CREATE TABLE IF NOT EXISTS USERS (userID BIGINT PRIMARY KEY, userName TEXT NOT NULL, numViolations INT NOT NULL DEFAULT 0);");
        this.update(usersTable, null);
        final SqlQuery flaggedMessagesTable = safeQuery("CREATE TABLE IF NOT EXISTS FLAGGED_MESSAGES (messageID BIGINT PRIMARY KEY, filterWord TEXT NOT NULL);");
        this.update(flaggedMessagesTable, null);
        final SqlQuery filteredWordsTable = safeQuery("CREATE TABLE IF NOT EXISTS FILTERED_WORDS (filterWord TEXT PRIMARY KEY, replacement TEXT NOT NULL);");
        this.update(filteredWordsTable, null);
        final SqlQuery adminsTable = safeQuery("CREATE TABLE IF NOT EXISTS ADMINS (userID BIGINT PRIMARY KEY);");
        this.update(adminsTable, null);
        final SqlQuery filteredChannelsTable = safeQuery("CREATE TABLE IF NOT EXISTS FILTERED_CHANNELS (channelID BIGINT PRIMARY KEY);");
        this.update(filteredChannelsTable, null);
        }

    /**
     * Shutdown the Worker and the Connection to the database
     */
    public void shutdown() {
        this.databaseWorker.shutdown();

        try {
            this.connection.close();
        } catch (final SQLException ex) {
            System.out.println("Exception while closing database connection!" + ex);
        }
    }

    /**
     * Constructs a query to be used for interacting with PreparedStatements.
     * Callers should avoid concatenating user input into the sql String.
     * User input should be passed into the sqlParameters to avoid SQL Injection.
     *
     * @param sql The base SQL statement with untrusted inputs replaced with ? characters
     * @param parameters parameters to be set on the PreparedStatement created from the sql query.
     * @return The constructed SqlQuery
     */
    public static SqlQuery safeQuery(final String sql, final Object... parameters) {
        return new SqlQuery(sql, parameters);
    }

    private List<Map<String, DatabaseEntry>> getObjects0(final SqlQuery sql) {
        final String sqlQuery = sql.sql;
        final Object[] sqlParameters = sql.parameters;

        try (final PreparedStatement statement = this.connection.prepareStatement(sqlQuery)) {
            for (int i = 0; i < sqlParameters.length; i++) {
                statement.setObject(i + 1, sqlParameters[i]);
            }

            return countResultsAndFetch(statement);
        } catch (final SQLException ex) {
            throw new DatabaseException("error while fetching objects from database!", ex);
        }
    }

    private int update0(final SqlQuery sqlQuery) {
        final String baseQuery = sqlQuery.sql;
        final Object[] parameters = sqlQuery.parameters;

        try (final PreparedStatement statement = this.connection.prepareStatement(baseQuery)) {
            for (int i = 0; i < parameters.length; i++) {
                statement.setObject(i + 1, parameters[i]);
            }

            return countResults(statement);
        } catch (final SQLException ex) {
            System.out.println("Error updating the database! SQL Query: " + baseQuery + '\n' + ex);
        }

        return 0;
    }

    private static List<Map<String, DatabaseEntry>> countResultsAndFetch(final PreparedStatement statement) throws SQLException {
        final List<Map<String, DatabaseEntry>> responses = new ArrayList<>();

        try (final ResultSet result = statement.executeQuery()) {
            while (result.next()) {
                responses.add(buildResult(result));
            }
        }

        return responses;
    }

    private static int countResults(final PreparedStatement statement) throws SQLException {
        return statement.executeUpdate();
    }

    private static Map<String, DatabaseEntry> buildResult(final ResultSet resultSet) throws SQLException {
        final Map<String, DatabaseEntry> result = new HashMap<>();
        final ResultSetMetaData resultMeta = resultSet.getMetaData();
        final int numColumns = resultMeta.getColumnCount();

        for (int i = 1; i <= numColumns; i++) {
            final String columnName = resultMeta.getColumnName(i);
            final DatabaseEntry columnValue = new DatabaseEntry(resultSet.getObject(i));

            result.put(columnName, columnValue);
        }

        return result;
    }



    public static final class SqlQuery {
        private final String sql;
        private final Object[] parameters;

        private SqlQuery(final String sql, final Object[] parameters) {
            this.sql = sql;
            this.parameters = parameters;
        }
    }

    public static final class DatabaseException extends RuntimeException {
        public DatabaseException(final String message, final Throwable cause) {
            super(message, cause);
        }
    }
}