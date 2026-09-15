# Plano de Implementação — Teste Controlado Real (Prospecção de Contadores)

**Data alvo:** 2026-08-30 (domingo)
**Objetivo:** Executar um envio real de ponta a ponta (sorteio de mensagem + sorteio de tempo +
gravação em `prospecting_data_source`, `prospecting_processed`, `prospecting_audit` + **envio real da
mensagem via Z-API / WhatsApp**) apenas para os registros de teste, com a **restrição de horário
desabilitada**, para confirmar que o serviço está pronto para prospectar clientes reais.

---

## 1. Diagnóstico (o que já existe)

| Item | Situação | Evidência |
|---|---|---|
| Bypass de horário para teste | **Já implementado** | [ProspectingAccountService.java:137-144](../prospecting-service/src/main/java/br/com/ia369/prospecting_service/service/ProspectingAccountService.java#L137-L144) e [:183-185](../prospecting-service/src/main/java/br/com/ia369/prospecting_service/service/ProspectingAccountService.java#L183-L185) — leads cujo `razao_social` começa com `TesteControlado` **pulam** `dentroDoHorario()` |
| Código no container em execução | Confirmado | `prospecting-service-app` (iniciado 2026-08-29 18:32 UTC) contém `isTesteControlado` / `ehTesteControlado` no `app.jar` |
| Sorteio de mensagem | OK, independe do horário | [MessageService.java](../prospecting-service/src/main/java/br/com/ia369/prospecting_service/service/MessageService.java) — sorteia 1–5, lê `classpath:messages/msg_contador_X.txt` (os 5 arquivos existem no jar) |
| Sorteio de tempo | OK, independe do horário | [ProspectingAccountService.java:292-299](../prospecting-service/src/main/java/br/com/ia369/prospecting_service/service/ProspectingAccountService.java#L292-L299) — `T = 5 + random(1..15)` = **6 a 20 min** entre registros |
| Z-API | Ativa e respondendo | `GET /phone-exists/...` retorna JSON válido (`{"exists":false,...}` para número inexistente) |
| Endpoints de controle | OK | `POST/DELETE/GET /prospecting-account` em `http://localhost:8081` ([ProspectingAccountController.java](../prospecting-service/src/main/java/br/com/ia369/prospecting_service/controller/ProspectingAccountController.java)) |
| Agendamento automático | Sem risco hoje | cron `0 0 8 * * MON` (segunda 08:00) |

**Conclusão:** nenhuma alteração de código ou redeploy é necessária. O teste é de **preparação de
dados + execução + validação + restauração**.

---

## 2. Problema a resolver ANTES de disparar

`startProspecting()` carrega os leads com `dataSourceRepository.findByStatusIsNull()`
([:124](../prospecting-service/src/main/java/br/com/ia369/prospecting_service/service/ProspectingAccountService.java#L124)).
Hoje **todos os 11.675 registros** de `prospecting_data_source` têm `status IS NULL`.

Consequências se disparado agora, sem preparação:

1. O loop percorre os leads em ordem **não garantida**. Ao encontrar o **primeiro lead que não é
   `TesteControlado`**, como é domingo, `!dentroDoHorario()` é verdadeiro e o loop executa `break`
   ([:139-144](../prospecting-service/src/main/java/br/com/ia369/prospecting_service/service/ProspectingAccountService.java#L139-L144))
   — os registros de teste podem nunca ser processados.
2. Se um registro de teste vier antes de um lead real na ordem, há risco de **enviar mensagem real
   para um contador de verdade** antes do `break`.

**Solução (sem código, sem redeploy):** dar um `status` temporário a todos os leads reais, deixando
**somente os registros `TesteControlado` com `status NULL`**. Assim `findByStatusIsNull()` retorna
apenas os registros de teste.

---

## 3. Pré-requisitos

- [ ] **2 a 4 números de WhatsApp reais que você controla** para receber as mensagens.
      Sem WhatsApp válido, `PhoneValidationService` → `phone-exists` retorna `false` →
      `status = "Nenhum telefone válido"` e **nada é enviado**.
- [ ] Formato do telefone: **somente dígitos, começando com `55`** (DDI), depois DDD e número.
      Para celular use 13 dígitos: `55` + DDD + `9` + 8 dígitos (ex.: `5511987654321`).
      `normalizarTelefone()` mantém o número como está se começar com `55` e tiver ≥ 12 dígitos.
- [ ] Acesso ao Postgres: `docker exec qr369-db psql -U dbrag369 -d ragdb`
- [ ] Acesso HTTP ao serviço: `http://localhost:8081`
- [ ] Garantir que ninguém vai disparar `POST /prospecting-account` para produção durante a janela do teste.

---

## 4. Passo a passo

### Passo 1 — Backup

```bash
docker exec qr369-db pg_dump -U dbrag369 -d ragdb \
  -t prospecting_data_source -t prospecting_processed -t prospecting_audit \
  > ~/backup_prospecting_$(date +%F_%H%M).sql
```

```sql
-- contagem de referência (esperado: 11675)
SELECT count(*) FROM prospecting_data_source WHERE status IS NULL;
-- maior id de auditoria antes do teste (anotar)
SELECT max(id) AS max_audit_id_antes FROM prospecting_audit;
```

### Passo 2 — Estacionar os leads reais

```sql
UPDATE prospecting_data_source
   SET status = 'HOLD_TESTE_20260830'
 WHERE status IS NULL;

-- verificação OBRIGATÓRIA (tem que retornar 0)
SELECT count(*) FROM prospecting_data_source WHERE status IS NULL;
```

### Passo 3 — Inserir os registros de Teste Controlado

Substitua os telefones pelos **seus números reais**. CNPJs fictícios de 14 dígitos que não colidem
com a base.

```sql
INSERT INTO prospecting_data_source (cnpj, razao_social, telefone1, telefone2, email, status) VALUES
('90000000000101','TesteControlado 01','5511987654321', NULL,           'teste1@exemplo.com', NULL),
('90000000000102','TesteControlado 02','5511912345678', NULL,           'teste2@exemplo.com', NULL),
('90000000000103','TesteControlado 03','5599999999999','5511987654321', 'teste3@exemplo.com', NULL);

-- conferir
SELECT id, cnpj, razao_social, telefone1, telefone2, status
  FROM prospecting_data_source
 WHERE razao_social LIKE 'TesteControlado%'
 ORDER BY id;
```

> **Dica p/ o registro 03:** `telefone1` propositalmente inválido e `telefone2` válido — testa o
> fallback de `PhoneValidationService` (tel1 falha → tenta tel2).

### Passo 4 (opcional) — Acelerar o sorteio de tempo

Entre cada registro o serviço aguarda `T = 5 + random(1..15)` = **6 a 20 min**. Para reduzir:
no `.env`, adicionar `PROSPECTING_INTERVAL_MIN=1` e recriar o container:

```bash
docker compose up -d --force-recreate prospecting-service
```

Com isso `T = 1 + random(1..15)` = **2 a 16 min** (o termo aleatório 1..15 é fixo no código).
**Recomendação:** manter o padrão (testa a lógica real de produção) e usar apenas **2 registros**
para limitar a espera total. Se alterar o `.env`, lembre-se de reverter no Passo 9.

### Passo 5 — Disparar

```bash
curl -i -X POST http://localhost:8081/prospecting-account -H 'Content-Type: application/json'
```

Esperado: `202 Accepted` — `Prospecção de contadores iniciada.`
Se `409 Conflict`: já está em execução → `curl -X DELETE http://localhost:8081/prospecting-account`, aguardar e repetir.

### Passo 6 — Acompanhar em tempo real

```bash
docker logs -f prospecting-service-app
```

Sequência esperada de log por registro:
`Processando lead: CNPJ=...` → `Verificando telefone1: ...` → `Telefone ...: existe no WhatsApp = true`
→ `Mensagem sorteada: X (arquivo: messages/msg_contador_X.txt)` → `Enviando mensagem WhatsApp para: ...`
→ `Mensagem enviada com sucesso para: ...` → `Aguardando N minuto(s) antes do próximo registro...`

Sanidade logo após o disparo — a auditoria deve registrar
`Leads pendentes encontrados: 2` (ou 3), **igual ao nº de registros de teste**.

Outros checagens:
```bash
curl -s http://localhost:8081/prospecting-account/status          # {"running": true}
curl -s http://localhost:8081/prospecting-account/audit | jq      # 10 eventos mais recentes
```

### Passo 7 — Validação (critérios de aceite)

**a) `prospecting_data_source`**
```sql
SELECT cnpj, razao_social, status
  FROM prospecting_data_source
 WHERE razao_social LIKE 'TesteControlado%';
```
Esperado: `status` deixou de ser `NULL` e contém o **telefone válido normalizado** (ex.: `5511987654321`).
`Nenhum telefone válido` = o número informado não tem WhatsApp (corrigir e repetir).

**b) `prospecting_processed`**
```sql
SELECT cnpj, razao_social, telefone_valido, data_contato, data_resposta, status
  FROM prospecting_processed
 WHERE razao_social LIKE 'TesteControlado%';
```
Esperado: 1 linha por registro, `telefone_valido` preenchido, `data_contato` preenchida,
`status = 'Contato Inicial'`.

**c) `prospecting_audit`**
```sql
SELECT id, data_evento, cnpj, status, log
  FROM prospecting_audit
 WHERE id > :max_audit_id_antes
 ORDER BY id;
```
Esperado: eventos `Iniciado` → `Funcionando` (Processando lead) → `Ok` (`mensagem enviada com
sucesso para ...`) → `Funcionando` (Aguardando N min) → ... → `Finalizado`.
**Não pode haver** nenhum evento `Erro` com `Fora do horário de funcionamento` — essa é a prova de
que o bypass funcionou num domingo.

**d) WhatsApp (o mais importante)**
Cada número de teste recebeu a mensagem **completa**, com texto idêntico a um dos
`msg_contador_1..5.txt`.

**e) Sorteios**
- Mensagem: conferir no log quais números (1–5) foram sorteados por registro.
- Tempo: conferir que o `N` de `Aguardando N minuto(s)` varia entre as iterações e está na faixa esperada.

### Passo 8 — Encerrar

Após o último registro o serviço finaliza sozinho (evento `Finalizado`). Para interromper antes:
```bash
curl -X DELETE http://localhost:8081/prospecting-account
```

### Passo 9 — Restaurar produção (OBRIGATÓRIO)

```sql
-- 1. reativar os leads reais
UPDATE prospecting_data_source
   SET status = NULL
 WHERE status = 'HOLD_TESTE_20260830';

-- 2. verificar (tem que voltar a 11675)
SELECT count(*) FROM prospecting_data_source WHERE status IS NULL;
```

Registros de teste — escolher:
- **Opção A (recomendada):** deixar como estão (já têm `status` preenchido → não serão reprocessados).
- **Opção B — remover tudo:**
```sql
DELETE FROM prospecting_processed   WHERE razao_social LIKE 'TesteControlado%';
DELETE FROM prospecting_audit       WHERE cnpj IN ('90000000000101','90000000000102','90000000000103');
DELETE FROM prospecting_data_source WHERE razao_social LIKE 'TesteControlado%';
```

Se alterou o `.env` no Passo 4: reverter `PROSPECTING_INTERVAL_MIN` e
`docker compose up -d --force-recreate prospecting-service`.

### Passo 10 — Relatório

| Registro | Tel. validado | Msg sorteada (nº) | Intervalo sorteado | Recebido no WhatsApp | data_source | processed | audit |
|---|---|---|---|---|---|---|---|
| TesteControlado 01 | | | | | | | |
| TesteControlado 02 | | | | | | | |
| TesteControlado 03 | | | | | | | |

**Conclusão:** serviço pronto para prospecção real? ( ) Sim ( ) Não — pendências: __________

---

## 5. Riscos e mitigação

| Risco | Mitigação |
|---|---|
| Envio acidental para leads reais | Passo 2 (estacionar) + verificação obrigatória `count = 0` antes do disparo |
| Número de teste sem WhatsApp | `status = "Nenhum telefone válido"`, sem envio — usar números confirmados; registro 03 testa fallback tel1→tel2 |
| Restauração esquecida | Passo 9 é bloqueante; agendar lembrete imediato após o teste |
| Cron automático dispara no meio | Só segunda 08:00 — sem risco hoje; não deixar o `HOLD` após o teste |
| Falha na Z-API | Auditoria grava `Error` com a causa ([:262-266](../prospecting-service/src/main/java/br/com/ia369/prospecting_service/service/ProspectingAccountService.java#L262-L266)); conferir instância/token |
| Espera longa entre envios | Usar 2 registros e/ou `PROSPECTING_INTERVAL_MIN=1` (Passo 4) |

## 6. Fora de escopo

- Alteração de código, migração de banco, redeploy (exceto o `--force-recreate` opcional do Passo 4).
- Teste de resposta do lead (`data_resposta` permanece `NULL` — não há webhook de resposta neste fluxo).
