#!/bin/bash
# =============================================================================
# start-prospecting-luiggi.sh
# Dispara o endpoint de prospecção de Comércio Exterior (Luiggi).
# Porta: 8085 (prospecting-service-luiggi)
# =============================================================================

SERVICE_URL="http://localhost:8085/prospecting-account"
LOG_FILE="/var/log/prospecting/luiggi_start.log"

mkdir -p /var/log/prospecting

echo "$(date '+%Y-%m-%d %H:%M:%S') - Iniciando prospecção de Comércio Exterior (Luiggi)..." >> "$LOG_FILE"

RESPONSE=$(curl -s -o /tmp/prospecting_luiggi_response.txt -w "%{http_code}" \
    -X POST "$SERVICE_URL" \
    -H "Content-Type: application/json")

BODY=$(cat /tmp/prospecting_luiggi_response.txt)
echo "$(date '+%Y-%m-%d %H:%M:%S') - HTTP $RESPONSE: $BODY" >> "$LOG_FILE"

if [ "$RESPONSE" = "202" ]; then
    echo "$(date '+%Y-%m-%d %H:%M:%S') - Prospecção do Luiggi iniciada com sucesso." >> "$LOG_FILE"
elif [ "$RESPONSE" = "409" ]; then
    echo "$(date '+%Y-%m-%d %H:%M:%S') - Prospecção do Luiggi já estava em andamento." >> "$LOG_FILE"
else
    echo "$(date '+%Y-%m-%d %H:%M:%S') - ERRO ao iniciar prospecção do Luiggi (HTTP $RESPONSE)." >> "$LOG_FILE"
fi
