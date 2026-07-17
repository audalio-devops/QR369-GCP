# Prospecting Service — Documentação Técnica

O **prospecting-service** é um microserviço desenvolvido em **Spring Boot 3.3.1** e **Java 21**, projetado para automatizar e otimizar o fluxo de coleta e enriquecimento de dados cadastrais de CNPJs. 

A solução opera sob o padrão **Cache-Aside**, evitando requisições redundantes a APIs externas (BrasilAPI) e permitindo o enfileiramento e processamento assíncrono em lotes.

---

## 1. Endpoints do Serviço (REST API)

O serviço disponibiliza a controladora [CnpjController.java](file:///c:/Projetos/IA369/QR369-GCP/prospecting-service/src/main/java/br/com/ia369/prospecting_service/controller/CnpjController.java) sob o prefixo `/cnpj`:

| Método | Endpoint | Consome? | Retorna? | Descrição |
| :--- | :--- | :--- | :--- | :--- |
| **GET** | `/cnpj/{cnpj}` | Nenhum | `CNPJResponse` (200) / 404 | Busca dados cadastrais da empresa pelo CNPJ de forma síncrona. (Cache-Aside) |
| **POST** | `/cnpj/processar-lote` | Nenhum | Nenhum (202 Accepted) | Executa o processamento em lote de todos os CNPJs atualmente na de fila de buscas (`busca_cnpj`). |
| **POST** | `/cnpj/importar-cnpj` | `multipart/form-data` | `ImportacaoResponse` (200) / 400 | Recebe arquivo em formato `.txt` ou `.csv` contendo CNPJs (um por linha), higieniza o formato e insere na fila de buscas. |

---

## 2. Padrões de Arquitetura e Engenharia

### A. Fluxo Cache-Aside (Consulta de CNPJ)
Ao chamar o `GET /cnpj/{cnpj}`, o serviço se comporta da seguinte maneira:
```
  [Requisição de CNPJ]
           │
           ▼
┌──────────────────────┐      Sabor Local (Sim)
│ Existe em Empresas ? ├───────────────────────► [Retorna Registro]
└──────────┬───────────┘
           │ Sabor Local (Não)
           ▼
┌──────────────────────┐      Falha/Invalido
│  Consulta BrasilAPI  ├───────────────────────► [Retorna 404]
└──────────┬───────────┘
           │ Encontrado
           ▼
┌──────────────────────┐
│  Persiste em Banco   │ (Tabela: empresas)
└──────────┬───────────┘
           │
           ▼
   [Retorna Registro]
```

### B. Processamento Assíncrono em Lote
O processamento da fila de buscas (`busca_cnpj`) pode ser disparado de duas maneiras:
1. **Endpoint REST**: `POST /cnpj/processar-lote` (assíncrono, retorna imediatamente com 202).
2. **Tarefa Agendada (Scheduled Task)**:
   - A classe [ScheduledProspectingTask.java](file:///c:/Projetos/IA369/QR369-GCP/prospecting-service/src/main/java/br/com/ia369/prospecting_service/task/ScheduledProspectingTask.java) possui o agendamento rodando periodicamente sob formato Cron configurado por `prospecting.schedule.cron` no `application.yml` (padrão: diariamente às 02:00 da manhã).
   - Aciona o processamento em lote `CnpjService.processarLoteDeCnpjs()`.

---

## 3. Modelo de Dados (Tabelas do Banco)

O microserviço integra-se a um banco de dados relacional (PostgreSQL) com as seguintes tabelas representadas por entidades JPA:

### A. Tabela `empresas` ([Empresa.java](file:///c:/Projetos/IA369/QR369-GCP/prospecting-service/src/main/java/br/com/ia369/prospecting_service/entity/Empresa.java))
* **Propósito**: Tabela onde armazena os dados convertidos e enriquecidos das empresas prospectadas com sucesso. Funciona como banco e cache quente.
* **Campos principais**: `cnpj` (Primary Key, 14 caracteres), `razao_social`, `nome_fantasia`, `situacao_cadastral`, `data_abertura`, `ddd`, `telefone`, `email`, `logradouro`, `numero`, `complemento`, `bairro`, `cidade`, `uf` e `cep`.

### B. Tabela `busca_cnpj` ([BuscaCnpj.java](file:///c:/Projetos/IA369/QR369-GCP/prospecting-service/src/main/java/br/com/ia369/prospecting_service/entity/BuscaCnpj.java))
* **Propósito**: Fila ordenada de CNPJs pendentes de prospecção/busca mais aprofundada em lote.
* **Campos principais**: `id` (Auto-incremento, pk) e `cnpj` (Único, não nulo, 14 caracteres).

### C. Tabela `tb_cnpj` ([Cnpj.java](file:///c:/Projetos/IA369/QR369-GCP/prospecting-service/src/main/java/br/com/ia369/prospecting_service/entity/Cnpj.java))
* **Propósito**: Tabela estática sementeada pela lógica de boot do microserviço.
* **Componente de Origem**: Utilizado por [CnpjDataSeeder.java](file:///c:/Projetos/IA369/QR369-GCP/prospecting-service/src/main/java/br/com/ia369/prospecting_service/config/CnpjDataSeeder.java) que povoa o banco na inicialização do microsserviço com dados mockados de grandes empresas (ex: Bradesco e Petrobras).

---

## 4. DTOs Utilizados

* **`CNPJResponse`**: Expõe ao usuário final os dados cadastrais da empresa mapeados a partir de `Empresa`.
* **`BrasilApiCnpjDTO`**: Mapeia as propriedades em snake_case cruas obtidas do JSON externo retornado pela BrasilAPI, aplicando as anotações do Jackson `@JsonProperty` e facilitando a higienização.
* **`ImportacaoResponse`**: Retorna estatísticas de execução do endpoint `/importar-cnpj`:
  - `totalLidos`: Quantidade de linhas com conteúdo encontradas no arquivo.
  - `totalImportados`: Registros novos inseridos na fila `busca_cnpj`.
  - `totalDuplicados`: Registros descartados por já estarem listados na fila.

---

## 5. Mapeador e Infraestrutura de Rede
* **`EmpresaMapper.java`**: Realiza o parsing de dados complexos retornados pela API (como a data de abertura e a concatenação e split de dddd e telefone da BrasilAPI).
* **`BrasilApiClient.java`**: Mapeia a URL base (`brasilapi.base-url`) no Spring e executa requisições através do `RestClient` moderno do Spring Framework v3.
