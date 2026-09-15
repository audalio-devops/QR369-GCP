#!/bin/bash
# =============================================================================
# monitor-prospecting.sh
# Verifica se a prospecção está ativa durante o horário de funcionamento.
# Se não estiver (e deveria estar), envia alerta por e-mail.
# Agendado via cron: a cada hora, segunda a sexta.
# =============================================================================

SERVICE_URL="http://localhost:8081/prospecting-account/status"
LOG_FILE="/var/log/prospecting/monitor.log"

ALERT_EMAILS="audalio.devops@gmail.com audalio@gmail.com queirozrei@gmail.com"
ALERT_SUBJECT="Prospecção de Contadores parada"

# Verificar se estamos no horário de funcionamento (Seg–Sex, 08:00–18:00)
DAY=$(date +%u)    # 1=Segunda, 7=Domingo
HOUR=$(date +%H)

if [ "$DAY" -lt 1 ] || [ "$DAY" -gt 5 ]; then
    echo "$(date '+%Y-%m-%d %H:%M:%S') - Fora do dia útil. Monitoramento ignorado." >> "$LOG_FILE"
    exit 0
fi

if [ "$HOUR" -lt 8 ] || [ "$HOUR" -ge 18 ]; then
    echo "$(date '+%Y-%m-%d %H:%M:%S') - Fora do horário de monitoramento. Ignorado." >> "$LOG_FILE"
    exit 0
fi

# Verificar status via endpoint
STATUS_RESPONSE=$(curl -s "$SERVICE_URL" 2>/dev/null)
IS_RUNNING=$(echo "$STATUS_RESPONSE" | grep -o '"running": *[a-z]*' | grep -o '[a-z]*$')

echo "$(date '+%Y-%m-%d %H:%M:%S') - Status da prospecção: running=$IS_RUNNING" >> "$LOG_FILE"

if [ "$IS_RUNNING" != "true" ]; then
    ALERT_BODY="A prospecção de contadores não está em execução como esperado.\n\nData/Hora: $(date '+%Y-%m-%d %H:%M:%S')\nServidor: $(hostname)\nResposta do endpoint: $STATUS_RESPONSE\n\nPor favor, verifique o serviço prospecting-service."

    for EMAIL in $ALERT_EMAILS; do
        echo -e "$ALERT_BODY" | mail -s "$ALERT_SUBJECT" "$EMAIL"
        echo "$(date '+%Y-%m-%d %H:%M:%S') - Alerta enviado para: $EMAIL" >> "$LOG_FILE"
    done
else
    echo "$(date '+%Y-%m-%d %H:%M:%S') - Prospecção em execução normal." >> "$LOG_FILE"
fi
