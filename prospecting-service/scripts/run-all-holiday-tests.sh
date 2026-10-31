#!/bin/bash
# =============================================================================
# run-all-holiday-tests.sh
# Executor dos dois testes de simulação de feriado da crontab:
#   1. 02/11/2026 (Feriado de Finados) -> NÃO deve disparar
#   2. 03/11/2026 (Dia Útil Normal)    -> DEVE estar apto a disparar (sem disparar)
# =============================================================================

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

echo "Iniciando bateria de testes da crontab (Feriados vs Dias Úteis)..."
echo ""

bash "${SCRIPT_DIR}/test-holiday-2026-11-02.sh"
bash "${SCRIPT_DIR}/test-normal-day-2026-11-03.sh"

echo "Todos os testes foram executados com sucesso!"
