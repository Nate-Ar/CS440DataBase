FROM alpine

EXPOSE 3306
RUN apk add --no-cache mariadb
RUN mkdir -p /run/mysqld/
RUN chown mysql:mysql /run/mysqld
RUN apk add --no-cache mariadb-client
RUN apk add --no-cache openjdk17
RUN adduser -h /opt/discordfilter -H -s /bin/false -S -D discordfilter
ADD files/init.sh /init/init.sh
ADD files/mariadb_setup /init/mariadb_setup
RUN chmod u+x /init/init.sh
ADD build/DiscordChatFilter.jar /init/DiscordChatFilter.jar

CMD ["/init/init.sh"]