# Plano de Sprints: Unificação do Serviço de Prospecção de CNPJ

Este documento detalha o plano de desenvolvimento para unificar as funcionalidades do projeto `Propect369` dentro do `prospecting-service`. O trabalho será dividido em 5 Sprints, cada uma com um objetivo claro e entregáveis definidos.

---

## Sprint 1: Configuração do Ambiente e Base de Dados

**Meta da Sprint:** Preparar o `prospecting-service` para se conectar ao banco de dados PostgreSQL, abandonando o H2, e garantir que o ambiente de desenvolvimento esteja pronto para as próximas fases.

**Histórias de Usuário / Tarefas:**

1.  **Como desenvolvedor, quero adicionar o driver do PostgreSQL ao projeto** para que a aplicação possa se comunicar com o banco de dados.
    *   **Tarefa:** Adicionar a dependência `org.postgresql:postgresql` ao arquivo `pom.xml`.

2.  **Como desenvolvedor, quero configurar a conexão com o banco de dados PostgreSQL no arquivo de configuração** para que o serviço se conecte à instância correta do banco de dados ao invés do H2.
    *   **Tarefa:** Alterar o `application.yml` para remover as configurações do H2 e adicionar a URL, driver, usuário e senha para a conexão com o PostgreSQL (`jdbc:postgresql://db:5432/ragdb`).

3.  **Como desenvolvedor, quero que a aplicação inicie sem erros com as novas configurações de banco de dados** para validar que a conexão com o PostgreSQL foi estabelecida corretamente.
    *   **Tarefa:** Executar a aplicação e verificar os logs para confirmar a conexão bem-sucedida com o pool de conexões do PostgreSQL.

**Critérios de Aceitação:**
*   O projeto compila e executa sem erros.
*   Nos logs de inicialização, o Spring Boot deve mostrar que está se conectando ao banco de dados PostgreSQL.
*   Qualquer referência ao banco de dados H2 deve ter sido removida da configuração ativa.

---

## Sprint 2: Criação das Entidades e Repositórios JPA

**Meta da Sprint:** Definir o modelo de dados da aplicação através de entidades JPA e criar as interfaces de repositório para permitir a interação com o banco de dados.

**Histórias de Usuário / Tarefas:**

1.  **Como sistema, preciso de uma tabela para armazenar os dados detalhados das empresas**, então quero criar a entidade `Empresa.java`.
    *   **Tarefa:** Criar a classe `Empresa.java` no pacote `entity`, mapeando-a para a tabela `empresas`. Os campos devem ser baseados na estrutura de dados da BrasilAPI, e o CNPJ deve ser a chave primária (`@Id`).

2.  **Como desenvolvedor, quero uma forma fácil de acessar os dados das empresas**, então quero criar o repositório `EmpresaRepository.java`.
    *   **Tarefa:** Criar a interface `EmpresaRepository.java` no pacote `repository`, estendendo `JpaRepository<Empresa, String>`.

3.  **Como sistema, preciso de uma tabela para listar os CNPJs que devem ser processados em lote**, então quero criar a entidade `BuscaCnpj.java`.
    *   **Tarefa:** Criar a classe `BuscaCnpj.java` no pacote `entity`, com campos como `id` e `cnpj`.

4.  **Como desenvolvedor, quero uma forma de ler a lista de CNPJs a serem buscados**, então quero criar o repositório `BuscaCnpjRepository.java`.
    *   **Tarefa:** Criar a interface `BuscaCnpjRepository.java` no pacote `repository`, estendendo `JpaRepository<BuscaCnpj, Long>`.

**Critérios de Aceitação:**
*   A aplicação inicia e, com `ddl-auto: update`, as tabelas `empresas` e `busca_cnpj` são criadas ou atualizadas no banco de dados PostgreSQL.
*   As interfaces de repositório estão prontas para serem injetadas em outros serviços.

---

## Sprint 3: Migração e Adaptação da Lógica de Negócio

**Meta da Sprint:** Implementar a lógica central de prospecção, combinando a busca em banco de dados com a consulta a APIs externas quando necessário (padrão Cache-Aside).

**Histórias de Usuário / Tarefas:**

1.  **Como desenvolvedor, quero isolar a lógica de chamadas a APIs externas**, então quero criar um serviço `ApiCnpjClient`.
    *   **Tarefa:** Criar um novo `@Service` chamado `ApiCnpjClient`. Migrar a lógica de chamada HTTP do `Propect369` para este serviço, preferencialmente usando o `RestClient` do Spring.

2.  **Como sistema, ao consultar um CNPJ, quero primeiro verificar se ele já existe no meu banco de dados antes de consultar uma API externa**, então quero refatorar o `CnpjService`.
    *   **Tarefa:** Injetar `EmpresaRepository` e `ApiCnpjClient` no `CnpjService`.
    *   **Tarefa:** Modificar o método `buscarPorCnpj(String cnpj)` para:
        1.  Buscar no `EmpresaRepository`.
        2.  Se encontrar, retornar o resultado (Cache Hit).
        3.  Se não encontrar (Cache Miss), chamar o `ApiCnpjClient`.
        4.  Se o `ApiCnpjClient` retornar dados, salvá-los no banco através do `EmpresaRepository` e então retornar os dados.
        5.  Se não encontrar em lugar nenhum, retornar um `Optional` vazio.

**Critérios de Aceitação:**
*   Ao chamar o endpoint `GET /cnpj/{cnpj}` pela primeira vez, os logs devem mostrar uma chamada para a API externa, seguida por uma inserção no banco de dados.
*   Ao chamar o mesmo endpoint uma segunda vez, os logs devem mostrar apenas uma consulta ao banco de dados, sem chamadas a APIs externas.
*   Se um CNPJ não existe, o endpoint deve retornar HTTP 404.

---

## Sprint 4: Implementação dos Novos Modos de Consulta

**Meta da Sprint:** Adicionar as funcionalidades de prospecção em lote e agendada, tornando o serviço proativo na coleta de dados.

**Histórias de Usuário / Tarefas:**

1.  **Como usuário, quero poder disparar a prospecção de uma lista de CNPJs de uma só vez**, então quero um endpoint para processamento em lote.
    *   **Tarefa:** Criar um método `processarLoteDeCnpjs()` no `CnpjService` que lê todos os CNPJs da tabela `busca_cnpj` e chama o método `buscarPorCnpj()` para cada um.
    *   **Tarefa:** Criar um novo endpoint, `POST /cnpj/processar-lote`, no `CnpjController` que aciona o método acima.

2.  **Como sistema, quero executar a prospecção em lote automaticamente todos os dias**, então quero implementar uma rotina agendada.
    *   **Tarefa:** Adicionar a anotação `@EnableScheduling` na classe principal da aplicação.
    *   **Tarefa:** Criar um novo componente, `ScheduledProspectingTask`, com um método anotado com `@Scheduled(cron = "...")`.
    *   **Tarefa:** O método agendado deve invocar o `cnpjService.processarLoteDeCnpjs()`.

**Critérios de Aceitação:**
*   Uma requisição `POST` para `/cnpj/processar-lote` aciona a busca para todos os CNPJs presentes na tabela `busca_cnpj`.
*   A tarefa agendada é executada no horário configurado, e os logs mostram o início e o fim do processamento em lote.

---

## Sprint 5: Finalização, Testes e Documentação

**Meta da Sprint:** Polir a aplicação, garantir a qualidade através de testes, limpar o código e documentar as novas funcionalidades para que o serviço esteja pronto para produção.

**Histórias de Usuário / Tarefas:**

1.  **Como desenvolvedor, quero garantir que a lógica de prospecção funciona corretamente**, então quero criar testes de integração.
    *   **Tarefa:** Escrever testes para o `CnpjController` que simulem os cenários de "cache hit" e "cache miss".

2.  **Como desenvolvedor, quero que o código esteja limpo e legível**, então quero refatorar e remover código não utilizado.
    *   **Tarefa:** Realizar uma revisão completa do código, removendo classes, métodos ou variáveis que se tornaram obsoletos após a unificação.

3.  **Como um novo desenvolvedor no projeto, quero entender como usar a API**, então quero que a documentação seja atualizada.
    *   **Tarefa:** Atualizar o arquivo `README.md` do projeto, documentando os novos endpoints (especialmente o de processamento em lote), as variáveis de ambiente necessárias e a arquitetura geral do serviço.

**Critérios de Aceitação:**
*   A suíte de testes passa com sucesso.
*   O código-fonte não contém artefatos ou lógicas dos projetos antigos que não estão mais em uso.
*   A documentação do projeto reflete com precisão o estado atual da aplicação e suas funcionalidades.
