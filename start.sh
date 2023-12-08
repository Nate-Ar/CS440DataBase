#!/bin/bash

file=./docker-compose.yml
if [ ! -f $file ]; then
  cp docker-compose.yml.example docker-compose.yml
  echo "You must set your bot token in docker-compose.yml, then re-run this script"
  exit 1
fi
if [ ! "$(command -v mvn)" ]; then
  echo "Maven was not found on the PATH, ensure Maven is installed!"
  exit 1
fi

if [ ! -d DiscordChatFilter/build ]; then
  mkdir DiscordChatFilter/build
fi

mvn package
rm target/original-DiscordChatFilter.jar
cp target/DiscordChatFilter.jar DiscordChatFilter/build/DiscordChatFilter.jar
docker build -t discordfilter:latest DiscordChatFilter
docker compose run --rm discordfilter
docker network rm discordchatfilter_discordfilter