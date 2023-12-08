#!/bin/ash

shutdown() {
  echo "SIGINT or SIGTERM intercepted! Killing JVM and MariaDB before shutting down container."

  kill "$pid"

  wait "$pid"

  echo "All tasks dead, shutting down container"
  exit 0
}

if [ ! -d /var/lib/mysql/mysql ]; then
  echo "MariaDB data directory not found! Initializing database for first launch."
  mysql_install_db --user=mysql --ldata=/var/lib/mysql
  setup=/init/mariadb_setup
  mysqld --user=mysql --bootstrap --verbose=0 --skip-name-resolve --skip-networking=0 < $setup
fi

TMP_OUT=/tmp/mariadb_output
pid=0
trap 'shutdown' SIGINT
trap 'shutdown' SIGTERM
mysqld --user=mysql --skip-name-resolve --console --skip-networking=0 --silent-startup > $TMP_OUT 2>&1 & pid=$!

until tail $TMP_OUT | grep -q "Version:"; do
			sleep 0.2
done

echo "MariaDB server finished startup. PID: " $pid

echo "Dropping privileges to user discordfilter"
echo ""
echo "Running DiscordChatFilter.jar"
su discordfilter -s /bin/ash -c "java -jar /init/DiscordChatFilter.jar"

echo "JVM exited, shutting down container..."
shutdown
