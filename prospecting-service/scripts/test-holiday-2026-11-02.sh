#!/bin/bash
# =============================================================================
# test-holiday-2026-11-02.sh
# Teste 1: Simulação do dia 02/11/2026 (Feriado de Finados)
# Objetivo: Verificar se a regra da crontab NÃO dispara o serviço de prospecção.
# =============================================================================

set -e

SIMULATED_DATE="2026-11-02"
FERIADOS_FILE="/home/ubuntu/Projetos/QR369-GCP/feriados.txt"

# Cores para saída
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}======================================================================${NC}"
echo -e "${BLUE}TESTE 1: Simulação de Feriado (${SIMULATED_DATE})${NC}"
echo -e "${BLUE}======================================================================${NC}"

# 1. Validação de Dia da Semana (Cron 1-5)
DAY_OF_WEEK=$(date -d "$SIMULATED_DATE" +%u) # 1 = Segunda, 7 = Domingo
DAY_NAME=$(date -d "$SIMULATED_DATE" +%A)

echo -e "1. [Calendário] Data simulada: ${SIMULATED_DATE} (${DAY_NAME}, dia da semana: ${DAY_OF_WEEK})"
if [ "$DAY_OF_WEEK" -ge 1 ] && [ "$DAY_OF_WEEK" -le 5 ]; then
    echo -e "   -> Cron '1-5': ${GREEN}Passaria pelo filtro de dia da semana do cron (seg-sex)${NC}"
else
    echo -e "   -> Cron '1-5': ${YELLOW}Filtro do cron descartaria por ser final de semana${NC}"
fi

# 2. Verificação no arquivo de feriados
echo -e "\n2. [Feriados] Verificando existência no arquivo '${FERIADOS_FILE}':"
if grep -q "$SIMULATED_DATE" "$FERIADOS_FILE"; then
    echo -e "   -> ${YELLOW}Data encontrada no feriados.txt! É FERIADO.${NC}"
else
    echo -e "   -> ${RED}ERRO: Data NÃO encontrada em feriados.txt.${NC}"
    exit 1
fi

# 3. Execução exata da lógica da crontab usando /bin/sh (shell padrão do cron)
# Comando do crontab:
# HOJE=$(date +\%Y-\%m-\%d); ! grep -q "$HOJE" /home/ubuntu/Projetos/QR369-GCP/feriados.txt && /usr/local/bin/start-prospecting.sh
# Nota: no cron, \% é desescapado para %.
echo -e "\n3. [Execução Cron Shell (/bin/sh)] Simulando a linha da crontab:"

DISPARADO=false
MOCK_TRIGGER="DISPARADO=true"

# Executando no /bin/sh simulando a função date retornando SIMULATED_DATE
# e substituindo o comando real por uma flag segura para NÃO disparar a API.
RESULT=$(/bin/sh -c "
date() { echo '${SIMULATED_DATE}'; }
TRIGGER_EXECUTADO='NAO'
HOJE=\$(date +%Y-%m-%d); ! grep -q \"\$HOJE\" '${FERIADOS_FILE}' && TRIGGER_EXECUTADO='SIM'
echo \$TRIGGER_EXECUTADO
")

echo -e "   -> Comando avaliado: HOJE=\$(date +%Y-%m-%d); ! grep -q \"\$HOJE\" ${FERIADOS_FILE} && <start-prospecting>"
echo -e "   -> O comando de disparo foi chamado? [${RESULT}]"

# 4. Avaliação do resultado esperado
echo -e "\n4. [Resultado do Teste]:"
if [ "$RESULT" = "NAO" ]; then
    echo -e "   ${GREEN}✔ SUCESSO: O serviço de prospecção NÃO FOI DISPARADO.${NC}"
    echo -e "   A crontab barrou o disparo corretamente por ser feriado em ${SIMULATED_DATE}."
    echo -e "${BLUE}======================================================================${NC}\n"
    exit 0
else
    echo -e "   ${RED}✘ FALHA: O serviço de prospecção TERIA SIDO DISPARADO indevidamente no feriado!${NC}"
    echo -e "${BLUE}======================================================================${NC}\n"
    exit 1
fi
