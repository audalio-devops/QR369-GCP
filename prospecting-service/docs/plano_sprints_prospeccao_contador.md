# Plano de Implementação — `POST /prospecting-account`
### Projeto: `prospecting-service` | Banco: `ragdb` | Integração: Z-API (WhatsApp)

---

## Objetivo

Implementar um endpoint que automatiza a prospecção de contadores: lê leads da tabela `prospecting_data_source`, valida seus telefones via WhatsApp (Z-API), sorteia e envia uma mensagem personalizada, e registra todo o processo com auditoria completa.

---

## Visão Geral das Sprints

| Sprint | Foco | Artefatos Entregues |
|--------|------|---------------------|
| **1** | Fundação — BD, Configuração e Estrutura | DDL, entidades JPA, repositórios, [application.yml](file:///C:/Projetos/IA369/QR369-GCP/prospecting-service/src/main/resources/application.yml) |
| **2** | Integração Z-API + Serviço de Validação | `ZApiClient`, `PhoneValidationService` |
| **3** | Lógica de Prospecção + Endpoint REST | `ProspectingAccountService`, `ProspectingAccountController` |
| **4** | Automação, Monitoramento e Scripts | scripts cron/systemd, alerta por e-mail |

---

## Sprint 1 — Fundação: Banco de Dados, Configuração e Estrutura

**Duração sugerida:** 2 dias

### 1.1 DDL — Novas Tabelas e Alteração

```sql
-- Alterar tabela existente
ALTER TABLE prospecting_data_source
ADD COLUMN IF NOT EXISTS status VARCHAR(100);

-- Nova tabela: registros processados
CREATE TABLE IF NOT EXISTS prospecting_processed (
    id              BIGSERIAL PRIMARY KEY,
    cnpj            VARCHAR(14)  NOT NULL,
    email           VARCHAR(255),
    razao_social    VARCHAR(255),
    telefone_valido VARCHAR(20),
    data_contato    TIMESTAMP,
    data_resposta   TIMESTAMP,
    status          VARCHAR(100),
    created_at      TIMESTAMP DEFAULT NOW()
);

-- Nova tabela: auditoria de envios
CREATE TABLE IF NOT EXISTS prospecting_audit (
    id          BIGSERIAL PRIMARY KEY,
    data_envio  TIMESTAMP NOT NULL DEFAULT NOW(),
    cnpj        VARCHAR(14),
    status      VARCHAR(10) NOT NULL, -- 'Ok' ou 'Error'
    log         TEXT
);
```

### 1.2 Entidades JPA

| Arquivo (NEW) | Pacote |
|---|---|
| `ProspectingDataSource.java` | `entity` |
| `ProspectingProcessed.java` | `entity` |
| `ProspectingAudit.java` | `entity` |

> **Nota:** `ProspectingDataSource` deve mapear a tabela existente adicionando o campo `status`.

### 1.3 Repositórios Spring Data JPA

| Arquivo (NEW) | Descrição |
|---|---|
| `ProspectingDataSourceRepository.java` | `findAll()`, `findByCnpj()`, método para buscar registros sem status |
| `ProspectingProcessedRepository.java` | `save()` |
| `ProspectingAuditRepository.java` | `save()`, `findTopByOrderByDataEnvioDesc()` |

### 1.4 Configuração ([application.yml](file:///C:/Projetos/IA369/QR369-GCP/prospecting-service/src/main/resources/application.yml))

Adicionar as seguintes variáveis de ambiente:

```yaml
zapi:
  base-url: ${ZAPI_BASE_URL:https://api.z-api.io}
  instance: ${ZAPI_INSTANCE}
  token: ${ZAPI_TOKEN}
  client-token: ${ZAPI_CLIENT_TOKEN}

prospecting:
  schedule:
    cron: "0 0 8 * * MON"       # Inicio: segunda-feira 08:00
    stop-cron: "0 0 18 * * FRI" # Fim:    sexta-feira 18:00
  working-hours:
    start: "08:00"
    end: "16:40"
  interval:
    min: 5
    max: 22
  message:
    count: 5
```

### 1.5 Critérios de Aceite da Sprint 1
- [ ] Scripts DDL executados com sucesso no banco `ragdb`
- [ ] Entidades e repositórios compilando sem erros
- [ ] Variáveis `ZAPI_*` documentadas no `.env.example`

---

## Sprint 2 — Integração Z-API: Validação e Envio de Mensagens

**Duração sugerida:** 3 dias

### 2.1 `ZApiClient.java` (NEW — pacote `client`)

Responsável por toda comunicação HTTP com a Z-API usando `RestTemplate` ou `WebClient`.

**Métodos:**
- `boolean phoneExists(String phoneNumber)` — `GET /phone-exists/{number}` com header `Client-Token`
- `void sendTextMessage(String phoneNumber, String message)` — `POST /send-text` com header `Client-Token`

**Cabeçalho obrigatório em todas as requisições:**
```
Client-Token: ${ZAPI_CLIENT_TOKEN}
```

**Formato do número:** DDI + DDD + número sem espaços ou símbolos (ex: `5511999999999`).

### 2.2 `PhoneValidationService.java` (NEW — pacote `service`)

```
Entrada: telefone1 (String), telefone2 (String)
Saída:   Optional<String> telefoneValido

Lógica:
  1. Se telefone1 não for nulo/vazio → chamar ZApiClient.phoneExists(telefone1)
     - Se existir → retornar Optional.of(telefone1)
  2. Se telefone2 não for nulo/vazio → chamar ZApiClient.phoneExists(telefone2)
     - Se existir → retornar Optional.of(telefone2)
  3. Retornar Optional.empty()
```

### 2.3 `MessageService.java` (NEW — pacote `service`)

Responsável por carregar as mensagens dos arquivos [.txt](file:///C:/Projetos/IA369/QR369-GCP/prospecting-service/src/main/resources/messages/msg_contador_3.txt).

```
Lógica:
  1. Sortear X = Random(1..5)
  2. Construir path: classpath:messages/msg_contador_{X}.txt
  3. Ler conteúdo como String (UTF-8)
  4. Retornar o texto da mensagem
```

> **Nota:** Os arquivos [msg_contador_1.txt](file:///C:/Projetos/IA369/QR369-GCP/prospecting-service/src/main/resources/messages/msg_contador_1.txt) a [msg_contador_5.txt](file:///C:/Projetos/IA369/QR369-GCP/prospecting-service/src/main/resources/messages/msg_contador_5.txt) já existem em `src/main/resources/messages/`. Verificar encoding e conteúdo.

### 2.4 Tratamento de Erros Z-API

| Cenário | Comportamento |
|---|---|
| Timeout na validação | Logar, marcar como erro na auditoria, continuar para próximo |
| Erro 4xx/5xx no envio | Logar, gravar `Error` na `prospecting_audit`, não atualizar `data_contato` |
| Número inválido (sem WhatsApp) | Gravar `'Nenhum telefone válido'` no `status` de `prospecting_data_source` |

### 2.5 Critérios de Aceite da Sprint 2
- [ ] `ZApiClient` testado manualmente com um número real via `phone-exists`
- [ ] `PhoneValidationService` com teste unitário cobrindo os 3 cenários (só tel1, só tel2, nenhum)
- [ ] `MessageService` sorteia aleatoriamente e carrega todos os 5 arquivos corretamente

---

## Sprint 3 — Lógica de Prospecção + Endpoint REST

**Duração sugerida:** 3 dias

### 3.1 `ProspectingAccountService.java` (NEW — pacote `service`)

Classe central com o método `startProspecting()`.

**Fluxograma de processamento:**

```
┌─────────────────────────────────────────────────────┐
│           startProspecting()                        │
│                                                     │
│  1. Buscar todos os registros de                    │
│     prospecting_data_source sem status              │
│                                                     │
│  2. Para cada registro:                             │
│     a. Verificar horário (08:00–16:40, Seg-Sex)     │
│        → Se fora do horário: aguardar ou parar      │
│     b. Validar telefones via PhoneValidationService │
│        → Sem telefone válido:                       │
│           • Atualizar status = 'Nenhum telefone     │
│             válido' em prospecting_data_source      │
│           • Continuar para próximo registro         │
│        → Com telefone válido:                       │
│           • Salvar em prospecting_processed         │
│             (status = null, sem data_contato)       │
│           • Atualizar status = telefoneValido       │
│             em prospecting_data_source              │
│           • Carregar msg via MessageService         │
│           • Enviar via ZApiClient                   │
│             → Sucesso:                              │
│                Atualizar prospecting_processed      │
│                (data_contato=now, status=           │
│                 'Contato Inicial')                  │
│             → Erro:                                 │
│                Logar falha                          │
│           • Gravar em prospecting_audit             │
│             (status='Ok' ou 'Error', log)           │
│     c. Sortear T = Random(5..22) minutos            │
│     d. Thread.sleep(T * 60 * 1000)                  │
│                                                     │
│  3. Fim do lote                                     │
└─────────────────────────────────────────────────────┘
```

**Controle de horário:** Verificar antes de cada envio se `LocalDateTime.now()` está entre 08:00 e 16:40, de segunda a sexta. Se estiver fora do horário, o método deve retornar (a próxima execução será agendada pelo cron).

### 3.2 `ProspectingAccountController.java` (NEW — pacote `controller`)

```java
@RestController
@RequestMapping("/prospecting-account")
public class ProspectingAccountController {

    @PostMapping
    public ResponseEntity<String> startProspecting() {
        // Dispara o processamento em thread assíncrona (@Async)
        // Retorna imediatamente: 202 Accepted
        prospectingAccountService.startProspecting();
        return ResponseEntity.accepted().body("Prospecção de contadores iniciada.");
    }
}
```

> **Nota:** O endpoint deve retornar `202 Accepted` imediatamente, pois o processamento é longo (cada registro tem um intervalo de 5–22 minutos).

### 3.3 Controle de Concorrência

- Usar um `AtomicBoolean isRunning` no service para evitar execuções paralelas.
- Se já estiver em execução, o endpoint retorna `409 Conflict` com a mensagem `"Prospecção já em andamento."`.

### 3.4 Critérios de Aceite da Sprint 3
- [ ] `POST /prospecting-account` retorna `202` na primeira chamada
- [ ] `POST /prospecting-account` retorna `409` se chamado novamente enquanto em execução
- [ ] Registros aparecem corretamente em `prospecting_processed` e `prospecting_audit`
- [ ] Envio de mensagem via Z-API confirmado com um número de teste

---

## Sprint 4 — Automação, Monitoramento e Scripts Linux

**Duração sugerida:** 2 dias

### 4.1 Script de Disparo Semanal (`/usr/local/bin/start-prospecting.sh`)

```bash
#!/bin/bash
# Dispara o endpoint de prospecção de contadores
curl -s -X POST http://localhost:8081/prospecting-account \
     -H "Content-Type: application/json" \
     >> /var/log/prospecting/start.log 2>&1
```

### 4.2 Script de Encerramento (`/usr/local/bin/stop-prospecting.sh`)

Como o endpoint não tem um `DELETE /prospecting-account`, o encerramento pode ser feito adicionando um endpoint `DELETE` que seta o `isRunning = false` e o `AtomicBoolean shouldStop = true`, fazendo o loop interromper na próxima iteração. O script então chama:

```bash
#!/bin/bash
curl -s -X DELETE http://localhost:8081/prospecting-account \
     >> /var/log/prospecting/stop.log 2>&1
```

### 4.3 Script de Monitoramento (`/usr/local/bin/monitor-prospecting.sh`)

```bash
#!/bin/bash
# Verifica se houve atividade na auditoria na última hora
LAST_ACTIVITY=$(psql -U $DB_USER -d ragdb -t -c \
  "SELECT COUNT(*) FROM prospecting_audit WHERE data_envio > NOW() - INTERVAL '1 hour';")

if [ "$LAST_ACTIVITY" -eq "0" ]; then
  # Nenhuma atividade: verificar se deveria estar rodando (Seg-Sex 08:00-18:00)
  DAY=$(date +%u)   # 1=Seg, 5=Sex
  HOUR=$(date +%H)
  if [ "$DAY" -ge 1 ] && [ "$DAY" -le 5 ] && \
     [ "$HOUR" -ge 8 ] && [ "$HOUR" -lt 18 ]; then
    SUBJECT="Prospecção de Contadores parada"
    BODY="A prospecção de contadores não registrou atividade na última hora. Verificar o serviço."
    echo "$BODY" | mail -s "$SUBJECT" \
      audalio.devops@gmail.com audalio@gmail.com queirozrei@gmail.com
  fi
fi
```

### 4.4 Crontab (`crontab -e` para o usuário do serviço)

```cron
# Disparar toda segunda-feira às 08:00
0 8 * * 1 /usr/local/bin/start-prospecting.sh

# Encerrar toda sexta-feira às 18:00
0 18 * * 5 /usr/local/bin/stop-prospecting.sh

# Monitorar a cada hora (Seg-Sex)
0 * * * 1-5 /usr/local/bin/monitor-prospecting.sh
```

### 4.5 Pré-requisitos no Servidor

```bash
# Instalar utilitários de e-mail (se necessário)
sudo apt-get install -y mailutils

# Criar diretório de log
sudo mkdir -p /var/log/prospecting
sudo chown $USER:$USER /var/log/prospecting

# Tornar os scripts executáveis
sudo chmod +x /usr/local/bin/start-prospecting.sh
sudo chmod +x /usr/local/bin/stop-prospecting.sh
sudo chmod +x /usr/local/bin/monitor-prospecting.sh
```

### 4.6 Critérios de Aceite da Sprint 4
- [ ] Cron dispara o endpoint na segunda-feira às 08:00
- [ ] Cron encerra na sexta-feira às 18:00
- [ ] Script de monitoramento envia alerta ao detectar inatividade no horário de funcionamento
- [ ] Logs gravados em `/var/log/prospecting/`

---

## Resumo dos Arquivos a Criar / Modificar

### [MODIFY] Existentes

| Arquivo | Mudança |
|---|---|
| [application.yml](file:///C:/Projetos/IA369/QR369-GCP/prospecting-service/src/main/resources/application.yml) | Adicionar bloco `zapi:` e atualizar `prospecting:` |
| `prospecting_data_source` (tabela) | Adicionar coluna `status VARCHAR(100)` via DDL |

### [NEW] Novos Arquivos Java

| Arquivo | Pacote | Sprint |
|---|---|---|
| `ZApiClient.java` | `client` | 2 |
| `PhoneValidationService.java` | `service` | 2 |
| `MessageService.java` | `service` | 2 |
| `ProspectingAccountService.java` | `service` | 3 |
| `ProspectingAccountController.java` | `controller` | 3 |
| `ProspectingDataSource.java` | `entity` | 1 |
| `ProspectingProcessed.java` | `entity` | 1 |
| `ProspectingAudit.java` | `entity` | 1 |
| `ProspectingDataSourceRepository.java` | `repository` | 1 |
| `ProspectingProcessedRepository.java` | `repository` | 1 |
| `ProspectingAuditRepository.java` | `repository` | 1 |

### [NEW] Scripts Linux

| Script | Propósito |
|---|---|
| `start-prospecting.sh` | Disparo via cron na segunda 08:00 |
| `stop-prospecting.sh` | Encerramento via cron na sexta 18:00 |
| `monitor-prospecting.sh` | Monitoramento e alerta por e-mail |

---

## Variáveis de Ambiente Necessárias

| Variável | Descrição | Exemplo |
|---|---|---|
| `ZAPI_INSTANCE` | ID da instância Z-API | `3D08...` |
| `ZAPI_TOKEN` | Token da instância | `abc123...` |
| `ZAPI_CLIENT_TOKEN` | Client-Token do header | `F68...` |
| `VA_DB_URL` | URL JDBC do PostgreSQL | `jdbc:postgresql://...` |
| `VA_DB_USERNAME` | Usuário do BD | `raguser` |
| `VA_DB_PASSWORD` | Senha do BD | `...` |
