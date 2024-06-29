#!/bin/bash

if [ -z "$1" ]; then
  echo "Uso: $0 <valor entre 0 e 1>"
  exit 1
fi

if [ -z "$2" ]; then
  echo "Uso: $0 <valor entre 0 e 1> <número de threads>"
  exit 1
fi

PROB_MESSAGE=$1
NUM_THREADS=$2

random_number() {
  echo "scale=2; $RANDOM/32767" | bc
}

execute_curl() {
  local random_value=$(random_number)
  if (( $(echo "$random_value < $PROB_MESSAGE" | bc -l) )); then
    echo "Executando: curl localhost:8080/test/message"
    curl -s localhost:8080/test/message > /dev/null
  else
    echo "Executando: curl localhost:8080/test/error"
    curl -s localhost:8080/test/error > /dev/null
  fi
}

# Função para capturar sinal de interrupção e finalizar processos filhos
cleanup() {
  echo "Recebido sinal de interrupção, finalizando..."
  pkill -P $$
  exit 0
}

trap cleanup SIGINT

for ((i=0; i<NUM_THREADS; i++)); do
  while true; do
    execute_curl
  done &
done

wait