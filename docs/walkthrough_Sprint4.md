# Walkthrough: Sprint 4 — Processamento em Lote e Agendamento

## O que foi implementado

A Sprint 4 adiciona dois novos **gatilhos de prospecção** ao `prospecting-service`:
1. **Manual (via endpoint REST):** `POST /cnpj/processar-lote`
2. **Automático (scheduler):** tarefa `@Scheduled` que executa diariamente às 02:00

Ambos os gatilhos chamam o mesmo método [processarLoteDeCnpjs()](file:///c:/Projetos/IA369/QR369-GCP/prospecting-service/src/main/java/br/com/ia369/prospecting_service/service/CnpjService.java#34-57) no [CnpjService](file:///c:/Projetos/IA369/QR369-GCP/prospecting-service/src/main/java/br/com/ia369/prospecting_service/service/CnpjService.java#16-77), que lê a tabela `busca_cnpj` e processa cada CNPJ individualmente.

## Arquivos modificados

| Arquivo | Operação | Mudança |
|---|---|---|
| [ProspectingServiceApplication.java](file:///c:/Projetos/IA369/QR369-GCP/prospecting-service/src/main/java/br/com/ia369/prospecting_service/ProspectingServiceApplication.java) | Modificado | `@EnableScheduling` + `@EnableAsync` |
| [CnpjService.java](file:///c:/Projetos/IA369/QR369-GCP/prospecting-service/src/main/java/br/com/ia369/prospecting_service/service/CnpjService.java) | Modificado | Inject [BuscaCnpjRepository](file:///c:/Projetos/IA369/QR369-GCP/prospecting-service/src/main/java/br/com/ia369/prospecting_service/repository/BuscaCnpjRepository.java#7-10) + método `@Async processarLoteDeCnpjs()` |
| [CnpjController.java](file:///c:/Projetos/IA369/QR369-GCP/prospecting-service/src/main/java/br/com/ia369/prospecting_service/controller/CnpjController.java) | Modificado | `POST /cnpj/processar-lote` → 202 Accepted |
| [task/ScheduledProspectingTask.java](file:///c:/Projetos/IA369/QR369-GCP/prospecting-service/src/main/java/br/com/ia369/prospecting_service/task/ScheduledProspectingTask.java) | **Criado** | Componente `@Scheduled` com cron configurável |
| [resources/application.yml](file:///c:/Projetos/IA369/QR369-GCP/prospecting-service/src/main/resources/application.yml) | Modificado | `prospecting.schedule.cron: "0 0 2 * * ?"` |
| [CnpjControllerTest.java](file:///c:/Projetos/IA369/QR369-GCP/prospecting-service/src/test/java/br/com/ia369/prospecting_service/controller/CnpjControllerTest.java) | Modificado | Removido `@Disabled`, adicionado teste do endpoint de lote |
| [service/CnpjServiceBatchTest.java](file:///c:/Projetos/IA369/QR369-GCP/prospecting-service/src/test/java/br/com/ia369/prospecting_service/service/CnpjServiceBatchTest.java) | **Criado** | 3 testes unitários da lógica de lote |

## Resultado dos testes

```
[INFO] Tests run: 6, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

### CnpjControllerTest (3 testes)
- ✅ [deveRetornarCnpjSeCadastrado](file:///c:/Projetos/IA369/QR369-GCP/prospecting-service/src/test/java/br/com/ia369/prospecting_service/controller/CnpjControllerTest.java#30-46) — GET retorna 200 OK com corpo
- ✅ [deveRetornar404ParaCnpjInexistente](file:///c:/Projetos/IA369/QR369-GCP/prospecting-service/src/test/java/br/com/ia369/prospecting_service/controller/CnpjControllerTest.java#47-55) — GET retorna 404
- ✅ [deveRetornar202AoDispararProcessamentoEmLote](file:///c:/Projetos/IA369/QR369-GCP/prospecting-service/src/test/java/br/com/ia369/prospecting_service/controller/CnpjControllerTest.java#56-65) — POST retorna 202, [processarLoteDeCnpjs()](file:///c:/Projetos/IA369/QR369-GCP/prospecting-service/src/main/java/br/com/ia369/prospecting_service/service/CnpjService.java#34-57) chamado

### CnpjServiceBatchTest (3 testes)
- ✅ [deveNaoProcessarNadaQuandoFilaEstaVazia](file:///c:/Projetos/IA369/QR369-GCP/prospecting-service/src/test/java/br/com/ia369/prospecting_service/service/CnpjServiceBatchTest.java#35-46) — fila vazia = zero chamadas ao repository
- ✅ [deveProcessarTodosOsCnpjsDaFila](file:///c:/Projetos/IA369/QR369-GCP/prospecting-service/src/test/java/br/com/ia369/prospecting_service/service/CnpjServiceBatchTest.java#47-67) — 2 CNPJs na fila = 2 chamadas ao `findById`
- ✅ [deveContinuarProcessandoMesmoDiranteErroIndividual](file:///c:/Projetos/IA369/QR369-GCP/prospecting-service/src/test/java/br/com/ia369/prospecting_service/service/CnpjServiceBatchTest.java#68-89) — erro no item 1, item 2 processado normalmente

## Como testar manualmente

1. Subir os containers:
   ```bash
   docker-compose up --build prospecting-service db
   ```

2. Disparar o processamento em lote:
   ```bash
   curl -X POST http://localhost:8081/cnpj/processar-lote -v
   ```
   **Esperado:** `HTTP/1.1 202 Accepted`

3. Verificar os logs:
   ```bash
   docker logs prospecting-service-app
   ```
   **Esperado:**
   ```
   INFO  CnpjController -- Recebida requisicao para iniciar processamento em lote de CNPJs.
   INFO  CnpjService -- Iniciando processamento em lote de CNPJs...
   INFO  CnpjService -- Encontrados X CNPJs na fila para processar.
   INFO  CnpjService -- Processamento em lote finalizado. X CNPJs processados.
   ```

## Configuração do cron (application.yml)

Para alterar a frequência de execução sem recompilar, altere:
```yaml
prospecting:
  schedule:
    cron: "0 0 2 * * ?"  # Diariamente as 02:00 (default)
```
