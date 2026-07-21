# Sprint de Implementação das Telas — QR369-TOOLS

Este documento organiza a execução do [Plano de Implementação](./implementation_plan.md) em Sprints ágeis para a implantação das telas de Login e Inicial do **QR369-TOOLS** no projeto **QR369-GCP**. 

O desenvolvimento está estruturado em 2 Sprints focadas: a primeira na infraestrutura e segurança do backend (`virtual-assistant`), e a segunda na interface visual, experiência do usuário e integrações (frontend).

---

## 📅 Sprint 1: Infraestrutura Backend & Autenticação
**Foco:** Habilitar as variáveis de ambiente, estruturar os endpoints de validação no `virtual-assistant` e configurar a rota de proxy para busca de CNPJ.

### 📋 Itens de Backlog / Entregáveis
1. **Configuração de Propriedades no Spring Boot**
   * Mapeamento de `LOGIN_USR` e `LOGIN_PWD` como propriedades no arquivo `application.yml` com defaults.
2. **DTO e Endpoint de Login**
   * Criação do record `LoginRequest` para encapsular a entrada de credenciais (`username` e `password`).
   * Desenvolvimento do `AuthController` mapeando `/api/auth/login` (POST), retornando corpo sucesso ou HTTP 401 caso falhe.
3. **Proxy do Serviço de Consulta de CNPJ**
   * Desenvolvimento do controlador de Proxy `CnpjProxyController` mapeando GET `/cnpj/{cnpj}` em `virtual-assistant` redirecionando chamadas para o `prospecting-service` (serviço interno na porta 8081).

### 🔍 Critérios de Aceitação / Identificação da Implantação
* [ ] As variáveis `LOGIN_USR` e `LOGIN_PWD` podem ser carregadas a partir do arquivo `.env`.
* [ ] A rota `POST /api/auth/login` rejeita requisições com status `401 Unauthorized` se o par usuário/senha estiver incorreto.
* [ ] A rota `POST /api/auth/login` aceita requisições corretas com status `200 OK`.
* [ ] O proxy de consulta de CNPJ no `virtual-assistant` consegue se comunicar com o `prospecting-service`. Uma requisição local `GET localhost:8080/cnpj/{cnpj}` deve retornar a mesma resposta de `GET localhost:8081/cnpj/{cnpj}`, eliminando problemas de CORS no frontend.
* [ ] Build e testes unitários do Spring Boot finalizam sem erros (`mvn clean test`).

---

## 📅 Sprint 2: Frontend & Experiência do Usuário (QR369-TOOLS)
**Foco:** Refatorar o arquivo `index.html`, definir o sistema visual (Design System), e estruturar o painel de navegação por abas.

### 📋 Itens de Backlog / Entregáveis
1. **Definição de Design System (CSS)**
   * Implementação de variáveis CSS no `:root` utilizando a paleta obrigatória: Vermelho Bordô (`--color-bordeaux`), Ouro Envelhecido (`--color-gold`) e Branco (`--color-white`).
2. **Estruturação de Telas e Rotas Cliente**
   * Implementação da tela de Login e tela Inicial gerenciadas dinamicamente no frontend por estados no Javascript.
3. **Menu Lateral e Alternância de Abas**
   * Implementação do menu lateral esquerdo com estilo premium (fundo bordeaux e destaque do item ativo em ouro envelhecido).
   * Organização do menu estruturada nas 5 opções nesta ordem exata:
     1. Atendente Virtual (chat chatbot)
     2. Prospecção (Módulo em desenvolvimento)
     3. Consultar CNPJ (Tela de pesquisa)
     4. Importar Lista (Módulo em desenvolvimento)
     5. Pesquisar lote (Módulo em desenvolvimento)
4. **Integração das Telas Funcionais**
   * **Chat Atendente Virtual:** Vinculação do chat existente de streaming SSE com a aba padrão de início.
   * **Consulta de CNPJ:** Layout com campo de input, botão de busca e campo multilinhas (`<pre><code>`) exibindo JSON formatado ou exibindo "CNPJ inexistente" em caso de retorno não localizado do proxy.

### 🔍 Critérios de Aceitação / Identificação da Implantação
* [ ] Ao abrir a aplicação, a tela de Login é exibida e o fluxo de autenticação local impede o acesso a tela inicial sem as credenciais corretas.
* [ ] Quando o login é efetuado com sucesso, a sessão persiste temporariamente e o usuário é redirecionado para a tela inicial.
* [ ] A barra lateral esquerda (Menu) ocupa toda a altura da tela utilizando o bordeaux, destacando as opções selecionadas em ouro envelhecido.
* [ ] O chat com o assistente virtual funciona corretamente de forma assíncrona na aba inicial do painel de controle.
* [ ] O formulário de Consulta de CNPJ emite o aviso `"CNPJ inexistente"` em vermelho/bordô se a requisição retornar não encontrada (404), e formata perfeitamente os dados reais em JSON em um campo multilinhas legível caso encontre.
* [ ] As demais abas de menu exibem de forma elegante e centralizada a mensagem `"Módulo em Desenvolvimento"`.
