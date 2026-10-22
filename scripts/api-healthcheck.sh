#!/usr/bin/env bash
# =============================================================================
# QR369 — Health check de todas as APIs usadas pelo projeto
#
# Uso:
#   ./scripts/api-healthcheck.sh [opções]
#
# Opções:
#   --gateway URL     URL pública (nginx ou vite) p/ testar as APIs internas
#                     (padrão: http://localhost:3000)
#   --services HOST   Host onde rodam os containers (actuator, Postgres, Redis,
#                     Ollama). Padrão: localhost. Use "none" para pular.
#   --env FILE        Arquivo .env com as credenciais (padrão: ./.env)
#   --skip-external   Não testa APIs de terceiros
#   --skip-internal   Não testa APIs internas (gateway + services)
#   --deep            Também executa chamadas com custo/efeito: login e uma
#                     mensagem curta no /chat/stream de cada assistente
#
# Todas as chamadas padrão são somente leitura. Exceção conhecida:
#   GET /prospecting-account/status grava 1 linha de auditoria (mesmo
#   comportamento do painel e do monitor-prospecting.sh).
#
# Código de saída: 0 se nenhuma API falhou, 1 caso contrário.
# =============================================================================

set -u

GATEWAY="http://localhost:3000"
SERVICES_HOST="localhost"
ENV_FILE="$(dirname "$0")/../.env"
SKIP_EXTERNAL=0
SKIP_INTERNAL=0
DEEP=0
TIMEOUT=15

while [ $# -gt 0 ]; do
    case "$1" in
        --gateway)       GATEWAY="${2%/}"; shift 2 ;;
        --services)      SERVICES_HOST="$2"; shift 2 ;;
        --env)           ENV_FILE="$2"; shift 2 ;;
        --skip-external) SKIP_EXTERNAL=1; shift ;;
        --skip-internal) SKIP_INTERNAL=1; shift ;;
        --deep)          DEEP=1; shift ;;
        -h|--help)       sed -n 2,25p "$0"; exit 0 ;;
        *) echo "Opção desconhecida: $1" >&2; exit 2 ;;
    esac
done

# ── Carrega o .env sem executá-lo (apenas linhas KEY=VALUE) ──────────────────
if [ -f "$ENV_FILE" ]; then
    while IFS='=' read -r key value; do
        key="$(echo "$key" | tr -d '[:space:]')"
        [[ "$key" =~ ^[A-Z_][A-Z0-9_]*$ ]] || continue
        value="${value%$'\r'}"
        value="${value%\"}"; value="${value#\"}"
        [ -z "${!key:-}" ] && export "$key=$value"
    done < "$ENV_FILE"
else
    echo "⚠️  Arquivo .env não encontrado em $ENV_FILE — testes externos podem falhar por falta de credenciais."
fi

ANTHROPIC_MODEL="${ANTHROPIC_MODEL:-claude-haiku-5-5}"
GEMINI_MODEL="${GEMINI_MODEL:-gemini-3.5-flash}"
OLLAMA_PORT="${OLLAMA_PORT:-11435}"
TEST_CNPJ="${TEST_CNPJ:-00000000000191}" # Banco do Brasil
SHEETS_CREDENTIALS="$(dirname "$0")/../prospecting-service/src/main/resources/credentials/qr369tools.json"

PASS=0; FAIL=0; WARN=0; SKIP=0
RESULTS=()

if [ -t 1 ]; then G=$'\e[32m'; R=$'\e[31m'; Y=$'\e[33m'; C=$'\e[36m'; N=$'\e[0m'; else G=; R=; Y=; C=; N=; fi

record() { # status nome detalhe
    local status="$1" name="$2" detail="$3" color
    case "$status" in
        OK)   PASS=$((PASS+1)); color=$G ;;
        FAIL) FAIL=$((FAIL+1)); color=$R ;;
        WARN) WARN=$((WARN+1)); color=$Y ;;
        SKIP) SKIP=$((SKIP+1)); color=$C ;;
    esac
    printf "  %s%-5s%s %-42s %s\n" "$color" "$status" "$N" "$name" "$detail"
    RESULTS+=("$status|$name|$detail")
}

section() { printf "\n%s== %s ==%s\n" "$C" "$1" "$N"; }

# http METHOD URL [args curl...] -> define HTTP_CODE, HTTP_BODY, HTTP_TIME
http() {
    local method="$1" url="$2"; shift 2
    local tmp; tmp="$(mktemp)"
    local meta
    meta="$(curl -sS -X "$method" -m "$TIMEOUT" -o "$tmp" -w "%{http_code} %{time_total}" "$@" "$url" 2>&1)"
    HTTP_CODE="${meta%% *}"; HTTP_TIME="${meta##* }"
    [[ "$HTTP_CODE" =~ ^[0-9]{3}$ ]] || { HTTP_BODY="$meta"; HTTP_CODE="000"; HTTP_TIME="-"; rm -f "$tmp"; return; }
    HTTP_BODY="$(head -c 20000 "$tmp" | tr -d '\r\n')"
    rm -f "$tmp"
}

short() { echo "$1" | cut -c1-140; }

tcp_check() { # nome host porta
    if timeout 5 bash -c "exec 3<>/dev/tcp/$2/$3" 2>/dev/null; then
        record OK "$1" "$2:$3 aceitando conexões"
    else
        record FAIL "$1" "$2:$3 sem resposta (serviço parado ou porta fechada)"
    fi
}

need() { # nome var... — retorna 1 (e registra SKIP/FAIL) se alguma var estiver vazia
    local name="$1"; shift
    for v in "$@"; do
        if [ -z "${!v:-}" ]; then record FAIL "$name" "variável $v não definida no .env"; return 1; fi
    done
}

# =============================================================================
# APIs externas (terceiros)
# =============================================================================
check_anthropic() {
    local name="Anthropic Claude ($ANTHROPIC_MODEL)"
    need "$name" ANTHROPIC_API_KEY || return
    http GET "https://api.anthropic.com/v1/models/$ANTHROPIC_MODEL" \
        -H "x-api-key: $ANTHROPIC_API_KEY" -H "anthropic-version: 2023-06-01"
    case "$HTTP_CODE" in
        200) record OK "$name" "modelo disponível (${HTTP_TIME}s)" ;;
        401) record FAIL "$name" "HTTP 401 — API key inválida/revogada" ;;
        403) record FAIL "$name" "HTTP 403 — sem permissão para o modelo" ;;
        404) record FAIL "$name" "HTTP 404 — modelo inexistente: $ANTHROPIC_MODEL" ;;
        429) record WARN "$name" "HTTP 429 — rate limit" ;;
        5*)  record FAIL "$name" "HTTP $HTTP_CODE — instabilidade Anthropic" ;;
        *)   record FAIL "$name" "HTTP $HTTP_CODE $(short "$HTTP_BODY")" ;;
    esac
}

check_gemini() {
    local name="Google Gemini ($GEMINI_MODEL)"
    local active="${SPRING_PROFILES_ACTIVE:-}"
    if [ -z "${GEMINI_API_KEY:-}" ]; then
        if [ "$active" = "gemini" ]; then record FAIL "$name" "GEMINI_API_KEY ausente e perfil gemini ativo"
        else record SKIP "$name" "GEMINI_API_KEY ausente (perfil ativo: $active)"; fi
        return
    fi
    http GET "https://generativelanguage.googleapis.com/v1beta/models/$GEMINI_MODEL" \
        -H "x-goog-api-key: $GEMINI_API_KEY"
    local level=FAIL; [ "$active" != "gemini" ] && level=WARN  # perfil inativo não derruba o sistema
    case "$HTTP_CODE" in
        200) record OK "$name" "modelo disponível (${HTTP_TIME}s)" ;;
        400|401|403) record "$level" "$name" "HTTP $HTTP_CODE — chave inválida/sem acesso (perfil ativo: $active) $(short "$HTTP_BODY")" ;;
        404) record "$level" "$name" "HTTP 404 — modelo inexistente (perfil ativo: $active)" ;;
        *)   record "$level" "$name" "HTTP $HTTP_CODE $(short "$HTTP_BODY")" ;;
    esac
}

check_assemblyai() {
    local name="AssemblyAI (transcrição de áudio)"
    need "$name" ASSEMBLYAI_API_KEY || return
    http GET "https://api.assemblyai.com/v2/transcript?limit=1" -H "authorization: $ASSEMBLYAI_API_KEY"
    case "$HTTP_CODE" in
        200) record OK "$name" "autenticado (${HTTP_TIME}s)" ;;
        401) record FAIL "$name" "HTTP 401 — API key inválida" ;;
        402|403) record FAIL "$name" "HTTP $HTTP_CODE — conta sem crédito/bloqueada $(short "$HTTP_BODY")" ;;
        *)   record FAIL "$name" "HTTP $HTTP_CODE $(short "$HTTP_BODY")" ;;
    esac
}

check_zapi() { # rótulo var_instance var_token var_client
    local name="Z-API WhatsApp ($1)"
    need "$name" "$2" "$3" "$4" || return
    http GET "https://api.z-api.io/instances/${!2}/token/${!3}/status" -H "Client-Token: ${!4}"
    if [ "$HTTP_CODE" != "200" ]; then
        record FAIL "$name" "HTTP $HTTP_CODE $(short "$HTTP_BODY")"
        return
    fi
    if echo "$HTTP_BODY" | grep -q '"connected"[[:space:]]*:[[:space:]]*true'; then
        record OK "$name" "instância conectada ao WhatsApp (${HTTP_TIME}s)"
    else
        record FAIL "$name" "API ok, mas instância DESCONECTADA: $(short "$HTTP_BODY")"
    fi
}

check_brasilapi() {
    local name="BrasilAPI (consulta CNPJ)"
    http GET "${BRASILAPI_BASE_URL:-https://brasilapi.com.br}/api/cnpj/v1/$TEST_CNPJ"
    case "$HTTP_CODE" in
        200) if echo "$HTTP_BODY" | grep -q '"razao_social"'; then record OK "$name" "CNPJ $TEST_CNPJ retornado (${HTTP_TIME}s)"
             else record WARN "$name" "200 mas payload inesperado: $(short "$HTTP_BODY")"; fi ;;
        429) record WARN "$name" "HTTP 429 — rate limit" ;;
        *)   record FAIL "$name" "HTTP $HTTP_CODE $(short "$HTTP_BODY")" ;;
    esac
}

check_google_sheets() {
    local name="Google Sheets API"
    http GET "https://sheets.googleapis.com/\$discovery/rest?version=v4"
    local reach="$HTTP_CODE"
    [ "$reach" = "200" ] || { record FAIL "$name" "API inalcançável (HTTP $reach)"; return; }
    if [ ! -f "$SHEETS_CREDENTIALS" ]; then
        # A credencial não é versionada; o teste real é o GET /prospecting/read-file no gateway
        record WARN "$name" "API alcançável, mas credentials/qr369tools.json ausente nesta máquina"
        return
    fi
    if ! grep -q '"private_key"' "$SHEETS_CREDENTIALS"; then
        record FAIL "$name" "qr369tools.json não parece uma service account válida"
        return
    fi
    record OK "$name" "credencial presente e API alcançável"
}

# =============================================================================
# Infraestrutura e serviços internos
# =============================================================================
check_actuator() { # nome porta
    http GET "http://$SERVICES_HOST:$2/actuator/health"
    if [ "$HTTP_CODE" = "200" ] && echo "$HTTP_BODY" | grep -q '"UP"'; then
        record OK "$1 :$2" "actuator UP (${HTTP_TIME}s)"
    elif [ "$HTTP_CODE" = "000" ]; then
        record FAIL "$1 :$2" "sem resposta"
    else
        record FAIL "$1 :$2" "HTTP $HTTP_CODE $(short "$HTTP_BODY")"
    fi
}

check_ollama() {
    local name="Ollama embeddings (bge-m3)"
    http GET "http://$SERVICES_HOST:$OLLAMA_PORT/api/tags"
    if [ "$HTTP_CODE" != "200" ]; then record FAIL "$name" "HTTP $HTTP_CODE em :$OLLAMA_PORT"; return; fi
    echo "$HTTP_BODY" | grep -q 'bge-m3' \
        && record OK "$name" "modelo bge-m3 carregado" \
        || record FAIL "$name" "Ollama no ar, mas bge-m3 não foi baixado"
}

check_gw_status() { # rótulo caminho
    local name="Prospecção $1 — GET status"
    http GET "$GATEWAY$2/status"
    if [ "$HTTP_CODE" != "200" ]; then record FAIL "$name" "HTTP $HTTP_CODE $(short "$HTTP_BODY")"; return; fi
    if echo "$HTTP_BODY" | grep -q '"zapiConnected"[[:space:]]*:[[:space:]]*true'; then
        record OK "$name" "serviço no ar, Z-API conectada — $(short "$HTTP_BODY")"
    else
        record FAIL "$name" "serviço no ar, mas Z-API NÃO conectada — $(short "$HTTP_BODY")"
    fi
}

check_gw_get() { # nome caminho [códigos aceitos regex]
    local ok="${3:-^200$}"
    http GET "$GATEWAY$2"
    if [[ "$HTTP_CODE" =~ $ok ]]; then record OK "$1" "HTTP $HTTP_CODE (${HTTP_TIME}s)"
    else record FAIL "$1" "HTTP $HTTP_CODE $(short "$HTTP_BODY")"; fi
}

check_login() {
    local name="Virtual Assistant — POST /api/auth/login"
    need "$name" LOGIN_USR LOGIN_PWD || return
    local body; body=$(printf '{"username":"%s","password":"%s"}' "$LOGIN_USR" "$LOGIN_PWD")
    http POST "$GATEWAY/api/auth/login" -H "Content-Type: application/json" -d "$body"
    [ "$HTTP_CODE" = "200" ] && record OK "$name" "login aceito" \
                             || record FAIL "$name" "HTTP $HTTP_CODE $(short "$HTTP_BODY")"
}

check_chat() { # rótulo caminho
    local name="Chat $1 — POST stream (LLM + RAG + Redis)"
    local old=$TIMEOUT; TIMEOUT=60
    http POST "$GATEWAY$2" -H "Content-Type: application/json" \
        -H "X-Conversation-Id: healthcheck-$(date +%s)" -d '{"message":"Responda apenas: OK"}'
    TIMEOUT=$old
    if [ "$HTTP_CODE" = "200" ] && echo "$HTTP_BODY" | grep -q 'data:'; then
        record OK "$name" "resposta em ${HTTP_TIME}s: $(short "$HTTP_BODY")"
    else
        record FAIL "$name" "HTTP $HTTP_CODE $(short "$HTTP_BODY")"
    fi
}

# =============================================================================
echo "QR369 — Health check de APIs  ($(date '+%Y-%m-%d %H:%M:%S'))"
echo "Gateway: $GATEWAY | Services host: $SERVICES_HOST | Perfil LLM: ${SPRING_PROFILES_ACTIVE:-?}"

if [ $SKIP_EXTERNAL -eq 0 ]; then
    section "APIs externas"
    check_anthropic
    check_gemini
    check_assemblyai
    check_zapi "Fillipo" ZAPI_INSTANCE_ID ZAPI_TOKEN ZAPI_CLIENT_TOKEN
    check_zapi "Luiggi"  ZAPI_LUIGGI_INSTANCE_ID ZAPI_LUIGGI_TOKEN ZAPI_LUIGGI_CLIENT_TOKEN
    check_brasilapi
    check_google_sheets
fi

if [ $SKIP_INTERNAL -eq 0 ]; then
    if [ "$SERVICES_HOST" != "none" ]; then
        section "Infraestrutura / serviços ($SERVICES_HOST)"
        tcp_check "PostgreSQL + pgvector" "$SERVICES_HOST" 5432
        tcp_check "Redis (memória do chat)" "$SERVICES_HOST" "${VA_REDIS_PORT:-6379}"
        check_ollama
        check_actuator "virtual-assistant (Fillipo)" "${VA_SERVER_PORT:-8080}"
        check_actuator "virtual-assistant (Luiggi)"  "${VA_LUIGGI_SERVER_PORT:-8084}"
        check_actuator "prospecting-service (Fillipo)" "${PS_SERVER_PORT:-8081}"
        check_actuator "prospecting-service (Luiggi)"  "${PS_LUIGGI_SERVER_PORT:-8085}"
    fi

    section "APIs internas via gateway ($GATEWAY)"
    check_gw_get "Frontend — GET /" "/"
    check_gw_status "Fillipo" "/prospecting-account"
    check_gw_status "Luiggi"  "/prospecting-account/luiggi"
    check_gw_get "Prospecção Fillipo — GET audit" "/prospecting-account/audit?limit=1"
    check_gw_get "Prospecção Luiggi — GET audit"  "/prospecting-account/luiggi/audit?limit=1"
    check_gw_get "CNPJ — GET /cnpj/{cnpj} (DB + BrasilAPI)" "/cnpj/$TEST_CNPJ"
    # 204 = planilha vazia; 500 = credencial/acesso ao Google Sheets com problema
    check_gw_get "Planilha — GET /prospecting/read-file (Sheets)" "/prospecting/read-file"
    if [ $DEEP -eq 1 ]; then
        check_login
        check_chat "Fillipo" "/chat/stream"
        check_chat "Luiggi"  "/chat/luiggi/stream"
    else
        record SKIP "Login + Chat (Fillipo/Luiggi)" "use --deep (consome tokens do LLM)"
    fi
fi

# ── Resumo ────────────────────────────────────────────────────────────────────
printf "\n%s== Resumo ==%s\n  %sOK: %d%s  %sFALHA: %d%s  %sAVISO: %d%s  %sPULADO: %d%s\n" \
    "$C" "$N" "$G" "$PASS" "$N" "$R" "$FAIL" "$N" "$Y" "$WARN" "$N" "$C" "$SKIP" "$N"
if [ $FAIL -gt 0 ]; then
    echo "  APIs com falha:"
    for r in "${RESULTS[@]}"; do
        IFS='|' read -r s n d <<< "$r"
        [ "$s" = "FAIL" ] && echo "   - $n: $d"
    done
    exit 1
fi
exit 0
