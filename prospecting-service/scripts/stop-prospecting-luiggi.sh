#!/bin/bash
# =============================================================================
# stop-prospecting-luiggi.sh
# Sinaliza a parada da prospecção de Comércio Exterior (Luiggi).
# Porta: 8085 (prospecting-service-luiggi)
# =============================================================================

SERVICE_URL="http://localhost:8085/prospecting-account"
LOG_FILE="/var/log/prospecting/luiggi_stop.log"

mkdir -p /var/log/prospecting

echo "$(date '+%Y-%m-%d %H:%M:%S') - Enviando sinal de parada da prospecção do Luiggi..." >> "$LOG_FILE"

RESPONSE=$(curl -s -o /tmp/prospecting_luiggi_stop_response.txt -w "%{http_code}" \
    -X DELETE "$SERVICE_URL")

BODY=$(cat /tmp/prospecting_luiggi_stop_response.txt)
echo "$(date '+%Y-%m-%d %H:%M:%S') - HTTP $RESPONSE: $BODY" >> "$LOG_FILE"
