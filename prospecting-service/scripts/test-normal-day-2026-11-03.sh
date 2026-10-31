#!/bin/bash
# =============================================================================
# test-normal-day-2026-11-03.sh
# Teste 2: Simulação do dia 03/11/2026 (Dia útil regular - Terça-feira)
# Objetivo: Verificar se a regra da crontab considera o serviço APTO A DISPARAR
#           (sem efetuar o disparo real da API).
# =============================================================================

set -e

SIMULATED_DATE="2026-11-03"
FERIADOS_FILE="/home/ubuntu/Projetos/QR369-GCP/feriados.txt"

# Cores para saída
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}======================================================================${NC}"
echo -e "${BLUE}TESTE 2: Simulação de Dia Útil Normal (${SIMULATED_DATE})${NC}"
echo -e "${BLUE}======================================================================${NC}"

# 1. Validação de Dia da Semana (Cron 1-5)
DAY_OF_WEEK=$(date -d "$SIMULATED_DATE" +%u) # 1 = Segunda, 2 = Terça, 7 = Domingo
DAY_NAME=$(date -d "$SIMULATED_DATE" +%A)

echo -e "1. [Calendário] Data simulada: ${SIMULATED_DATE} (${DAY_NAME}, dia da semana: ${DAY_OF_WEEK})"
if [ "$DAY_OF_WEEK" -ge 1 ] && [ "$DAY_OF_WEEK" -le 5 ]; then
    echo -e "   -> Cron '1-5': ${GREEN}Passaria pelo filtro de dia da semana do cron (seg-sex)${NC}"
else
    echo -e "   -> Cron '1-5': ${RED}ERRO: Não deveria ser final de semana${NC}"
    exit 1
fi

# 2. Verificação no arquivo de feriados
echo -e "\n2. [Feriados] Verificando existência no arquivo '${FERIADOS_FILE}':"
if grep -q "$SIMULATED_DATE" "$FERIADOS_FILE"; then
    echo -e "   -> ${RED}ERRO: Data encontrada no feriados.txt indevidamente.${NC}"
    exit 1
else
    echo -e "   -> ${GREEN}Data NÃO está em feriados.txt. NÃO É FERIADO.${NC}"
fi

# 3. Execução da lógica da crontab usando /bin/sh (shell padrão do cron)
# ATENÇÃO: NÃO dispara o serviço real (/usr/local/bin/start-prospecting.sh).
# Apenas intercepta o ponto de execução para confirmar a aptidão ao disparo.
echo -e "\n3. [Execução Cron Shell (/bin/sh)] Verificando aptidão ao disparo:"

RESULT=$(/bin/sh -c "
date() { echo '${SIMULATED_DATE}'; }
APTO_A_DISPARAR='NAO'
HOJE=\$(date +%Y-%m-%d); ! grep -q \"\$HOJE\" '${FERIADOS_FILE}' && APTO_A_DISPARAR='SIM'
echo \$APTO_A_DISPARAR
")

echo -e "   -> Comando avaliado: HOJE=\$(date +%Y-%m-%d); ! grep -q \"\$HOJE\" ${FERIADOS_FILE} && <start-prospecting>"
echo -e "   -> Condição satisfeita e pronto para disparar? [${RESULT}]"

# 4. Avaliação do resultado esperado
echo -e "\n4. [Resultado do Teste]:"
if [ "$RESULT" = "SIM" ]; then
    echo -e "   ${GREEN}✔ SUCESSO: O serviço de prospecção ESTÁ APTO A SER DISPARADO.${NC}"
    echo -e "   (O disparo real NÃO foi realizado, apenas validada a prontidão da regra)."
    echo -e "${BLUE}======================================================================${NC}\n"
    exit 0
else
    echo -e "   ${RED}✘ FALHA: O serviço de prospecção NÃO estaria apto a disparar em um dia útil normal!${NC}"
    echo -e "${BLUE}======================================================================${NC}\n"
    exit 1
fi
