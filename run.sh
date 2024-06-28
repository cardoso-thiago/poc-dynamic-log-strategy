#!/bin/bash

if [ -z "$1" ]; then
  echo "Uso: $0 <valor entre 0 e 1>"
  exit 1
fi

PROB_MESSAGE=$1

random_number() {
  echo "scale=2; $RANDOM/32767" | bc
}

execute_curl() {
  local random_value=$(random_number)
  if (( $(echo "$random_value < $PROB_MESSAGE" | bc -l) )); then
    echo "Executando: curl localhost:8080/test/message"
    curl localhost:8080/test/message
  else
    echo "Executando: curl localhost:8080/test/error"
    curl localhost:8080/test/error
  fi
}

while true; do
  execute_curl
  sleep 1
done