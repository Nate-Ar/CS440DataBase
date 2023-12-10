-- docker exec -i  discordfilter mariadb < report.sql;
-- docker exec -i  discordfilter mariadb < report.sql > report.log;

USE discordfilter;
-- show all data in tables
SELECT * FROM ADMINS;
SELECT * FROM FILTERED_CHANNELS;
SELECT * FROM FILTERED_WORDS;
SELECT * FROM MESSAGES;
SELECT * FROM USERS;

-- filter word ORDER BY number of violations
SELECT * FROM FILTERED_WORDS ORDER BY numViolations;
-- messages that are not filtered AND the authorID has 9 in it
SELECT * FROM MESSAGES WHERE filtered = FALSE AND autherID LIKE "%9%";
-- admins that have userID with 7 OR 2 in it
SELECT * FROM ADMINS WHERE userID LIKE "%7%" OR userID LIKE "%2%";
-- COUNT all users
SELECT COUNT(userID) FROM USERS;
-- sum of all violations
SELECT SUM(numViolations) FROM USERS;
-- avrage numver of violations per user
SELECT AVG(numViolatons) FROM USERS;
-- user with the lowest violations
SELECT MIN(numViolatons) FROM USERS;
-- user with the Highest violations
SELECT MAX(numViolatons) FROM USERS;
-- add group by query

-- joins the message and user tables to show the username of the the message sent
SELECT USER.userName, USER.userID, MESSAGES.authorID
From USER INNER JOIN MESSAGES ON USER.userID=MESSAGES.authorID;


