#!/bin/bash
# =============================================================================
# start-prospecting.sh
# Dispara o endpoint de prospecção de contadores.
# Agendado via cron: toda segunda-feira às 08:00.
# =============================================================================

SERVICE_URL="http://localhost:8081/prospecting-account"
LOG_FILE="/var/log/prospecting/start.log"

echo "$(date '+%Y-%m-%d %H:%M:%S') - Iniciando prospecção de contadores..." >> "$LOG_FILE"

RESPONSE=$(curl -s -o /tmp/prospecting_response.txt -w "%{http_code}" \
    -X POST "$SERVICE_URL" \
    -H "Content-Type: application/json")

BODY=$(cat /tmp/prospecting_response.txt)
echo "$(date '+%Y-%m-%d %H:%M:%S') - HTTP $RESPONSE: $BODY" >> "$LOG_FILE"

if [ "$RESPONSE" = "202" ]; then
    echo "$(date '+%Y-%m-%d %H:%M:%S') - Prospecção iniciada com sucesso." >> "$LOG_FILE"
elif [ "$RESPONSE" = "409" ]; then
    echo "$(date '+%Y-%m-%d %H:%M:%S') - Prospecção já estava em andamento." >> "$LOG_FILE"
else
    echo "$(date '+%Y-%m-%d %H:%M:%S') - ERRO ao iniciar prospecção (HTTP $RESPONSE)." >> "$LOG_FILE"
fi
