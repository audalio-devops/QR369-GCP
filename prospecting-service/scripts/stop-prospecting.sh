#!/bin/bash
# =============================================================================
# stop-prospecting.sh
# Sinaliza a parada da prospecção de contadores.
# Agendado via cron: toda sexta-feira às 18:00.
# =============================================================================

SERVICE_URL="http://localhost:8081/prospecting-account"
LOG_FILE="/var/log/prospecting/stop.log"

echo "$(date '+%Y-%m-%d %H:%M:%S') - Enviando sinal de parada da prospecção..." >> "$LOG_FILE"

RESPONSE=$(curl -s -o /tmp/prospecting_stop_response.txt -w "%{http_code}" \
    -X DELETE "$SERVICE_URL")

BODY=$(cat /tmp/prospecting_stop_response.txt)
echo "$(date '+%Y-%m-%d %H:%M:%S') - HTTP $RESPONSE: $BODY" >> "$LOG_FILE"
