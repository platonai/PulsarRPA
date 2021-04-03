#!/bin/bash

this="${BASH_SOURCE-$0}"
basedir=$(dirname "$this")
source "$basedir/config.sh"

uuid=$(exec "$basedir/detail/submit.sh")

# change to your own pulsar server
url="http://$host:8182/api/x/a/status?uuid=$uuid&authToken=$authToken&priority=HIGHER5"

while true; do
  result=$(curl -H "Accept: application/json" -X GET "$url")
  echo "$result"

  if [[ $result != *"Created"* ]]; then
    exit 0
  fi
  sleep 5s
done
