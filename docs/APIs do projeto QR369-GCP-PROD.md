# APIs do projeto QR369-GCP-PROD

Inventário das APIs usadas pelo projeto, como testar cada uma e o resultado da última verificação em produção.

- **Script de teste:** [`scripts/api-healthcheck.sh`](../scripts/api-healthcheck.sh)
- **Última verificação:** 21/10/2026, contra a produção (`http://137.131.217.157`, via nginx)
- **Resultado:** 14 OK · 2 falhas · 1 aviso. As duas falhas têm a mesma causa: a instância Z-API do Fillipo está desconectada.

---

## 1. Visão geral da arquitetura

```
Navegador ──► nginx :80 ──┬─► frontend-app (React/Vite)      :3000
                          ├─► virtual-assistant  Fillipo      :8080 ─┐
                          ├─► virtual-assistant  Luiggi       :8084 ─┤─► Anthropic / Gemini (LLM)
                          ├─► prospecting-service Fillipo     :8081 ─┤─► AssemblyAI (áudio)
                          └─► prospecting-service Luiggi      :8085 ─┤─► Z-API (WhatsApp)
                                                                     ├─► BrasilAPI (CNPJ)
Z-API (webhook) ──► nginx /webhooks ──► virtual-assistant            ├─► Google Sheets
                                                                     └─► PostgreSQL+pgvector · Redis · Ollama
```

As portas 8080, 8081, 8084 e 8085 não são expostas publicamente. Todo o acesso externo passa pelo nginx ([`frontend-app/nginx/qr369`](../frontend-app/nginx/qr369)).

---

## 2. APIs externas (terceiros)

| API | Usada por | Para quê | Credencial (`.env`) | Onde no código |
|---|---|---|---|---|
| **Anthropic Claude** (`claude-haiku-5-5`) | virtual-assistant | Respostas do chat (perfil `anthropic`, ativo) | `ANTHROPIC_API_KEY` | `application-anthropic.yml` |
| **Google Gemini** (`gemini-3.5-flash`) | virtual-assistant | Respostas do chat (perfil `gemini`, inativo) | `GEMINI_API_KEY` | `application-gemini.yml` |
| **AssemblyAI** | virtual-assistant | Transcrição de áudios recebidos no WhatsApp | `ASSEMBLYAI_API_KEY` | `whatsapp/AudioTranscriptionService.java` |
| **Z-API** (Fillipo e Luiggi) | ambos os serviços | Envio de mensagens, status da instância, validação de telefone | `ZAPI_*` e `ZAPI_LUIGGI_*` | `client/ZApiClient.java`, `whatsapp/ZApiClientService.java` |
| **BrasilAPI** | prospecting-service | Consulta de dados de CNPJ | — (pública) | `client/BrasilApiClient.java` |
| **Google Sheets API** | prospecting-service | Leitura da planilha de prospecção | Service account `credentials/qr369tools.json` (não versionada) | `googlesheets/GoogleSheetsService.java` |

Endpoints da Z-API usados: `GET /status`, `GET /phone-exists/{phone}`, `POST /send-text`.

## 3. Infraestrutura

| Serviço | Porta | Para quê |
|---|---|---|
| PostgreSQL + pgvector | 5432 | Dados de prospecção, cache de CNPJ, vector store do RAG |
| Redis | 6379 | Memória de conversa do chat (expira após 30 min) |
| Ollama (`bge-m3`) | 11435 | Embeddings do RAG (usado com qualquer perfil de LLM) |

## 4. APIs internas

### virtual-assistant (Fillipo :8080 · Luiggi :8084)

| Método | Endpoint | Função |
|---|---|---|
| POST | `/api/auth/login` | Login do painel |
| POST | `/chat/stream` | Chat com streaming (SSE). Header obrigatório `X-Conversation-Id` |
| GET | `/cnpj/{cnpj}` | Proxy para o prospecting-service |
| POST | `/ingest`, `/ingest/default` | Ingestão de documentos no RAG |
| POST | `/webhooks/zapi`, `/webhooks/zapi/luiggi`, `/webhooks/zapi/fillipo` | Recebe mensagens do WhatsApp |
| GET | `/actuator/health` | Health check do Spring |

### prospecting-service (Fillipo :8081 · Luiggi :8085)

| Método | Endpoint | Função |
|---|---|---|
| POST / DELETE | `/prospecting-account` | Inicia / para a prospecção |
| GET | `/prospecting-account/status` | Status da prospecção e da conexão Z-API (grava 1 linha de auditoria) |
| GET | `/prospecting-account/audit?limit=N` | Últimos registros de auditoria |
| GET | `/cnpj/{cnpj}` | Consulta CNPJ (cache no banco, senão BrasilAPI) |
| POST | `/cnpj/processar-lote`, `/cnpj/importar-cnpj` | Processamento e importação de CNPJs |
| POST | `/prospectaccount/{type}` | Importação de contas |
| GET | `/prospecting/read-file` | Lê a planilha do Google Sheets |
| GET | `/actuator/health` | Health check do Spring |

No nginx, as rotas do Luiggi usam o sufixo `/luiggi` (ex.: `/chat/luiggi/stream`, `/prospecting-account/luiggi/status`).

---

## 5. Como cada API é testada

| API | Teste | Considerado com erro quando |
|---|---|---|
| Anthropic | `GET /v1/models/{modelo}` (sem custo) | 401 chave inválida · 404 modelo inexistente · 5xx |
| Gemini | `GET /v1beta/models/{modelo}` | Qualquer código ≠ 200 (vira aviso se o perfil não estiver ativo) |
| AssemblyAI | `GET /v2/transcript?limit=1` | 401 chave inválida · 402/403 conta sem crédito |
| Z-API | `GET /instances/{id}/token/{token}/status` | HTTP ≠ 200 ou `connected:false` |
| BrasilAPI | `GET /api/cnpj/v1/00000000000191` | HTTP ≠ 200 ou sem `razao_social` |
| Google Sheets | Acesso à API + presença da credencial local | API inalcançável |
| Postgres / Redis | Conexão TCP | Porta não responde |
| Ollama | `GET /api/tags` | Fora do ar ou sem o modelo `bge-m3` |
| Serviços Spring | `GET /actuator/health` | Status ≠ `UP` |
| Prospecção | `GET .../status` e `.../audit` via gateway | HTTP ≠ 200 ou `zapiConnected:false` |
| CNPJ / Planilha | `GET /cnpj/{cnpj}`, `GET /prospecting/read-file` via gateway | HTTP ≠ 200 (500 na planilha = credencial ou acesso com problema) |
| Login / Chat (`--deep`) | Login e uma mensagem curta em cada assistente | HTTP ≠ 200 ou resposta sem eventos SSE |

## 6. Como executar

```bash
# Produção (via nginx), somente leitura
./scripts/api-healthcheck.sh --gateway http://137.131.217.157 --services none

# Produção + login e chat (consome alguns tokens do LLM)
./scripts/api-healthcheck.sh --gateway http://137.131.217.157 --services none --deep

# Stack local (inclui Postgres, Redis, Ollama e /actuator/health)
./scripts/api-healthcheck.sh
```

Outras opções: `--env ARQUIVO`, `--skip-external`, `--skip-internal`.

- As credenciais são lidas do `.env`, e cada chave é enviada apenas ao serviço a que pertence.
- O script termina com código 1 quando algo falha, então pode ser agendado no `crontab` ou usado em alertas.
- O webhook da Z-API não é testado, para não disparar o processamento de uma mensagem falsa.

---

## 7. Resultado da verificação de 21/10/2026

| API | Resultado |
|---|---|
| Anthropic Claude | ✅ OK |
| Google Gemini | ✅ OK |
| AssemblyAI | ✅ OK |
| **Z-API Fillipo** | ❌ **Desconectada** (`"You are not connected."`, `smartphoneConnected:false`) |
| Z-API Luiggi | ✅ Conectada |
| BrasilAPI | ✅ OK |
| Google Sheets | ⚠️ Credencial ausente na máquina de teste; em produção funciona (`/prospecting/read-file` = 200) |
| Frontend `/` | ✅ OK |
| Prospecção Fillipo `/status` | ❌ Serviço no ar, mas `zapiConnected:false` |
| Prospecção Luiggi `/status` | ✅ OK |
| Auditoria (Fillipo e Luiggi) | ✅ OK |
| `/cnpj/{cnpj}` | ✅ OK |
| `/prospecting/read-file` | ✅ OK |
| Login | ✅ OK |
| Chat Fillipo / Luiggi | ✅ OK (cerca de 5s e 2s) |

Não testados nesta execução: Postgres, Redis, Ollama e `/actuator/health`, porque as portas não ficam abertas na produção. Eles foram validados de forma indireta: o chat (Redis e RAG) e o CNPJ (Postgres) responderam.

### Problema encontrado: Z-API do Fillipo desconectada

**Impacto:**
- A prospecção de contadores (Fillipo) não consegue enviar mensagens.
- As respostas do assistente Fillipo no WhatsApp também não são entregues. Essa falha não aparece no painel: o `ZApiClientService` usa `.subscribe()` e só registra o erro no log.

**Correção:** reconectar a instância lendo o QR Code no painel da Z-API com o celular do Fillipo. O código não precisa mudar.
