### **Plano de Alterações para Integração com Z-API**

O objetivo é transformar o `virtual-assistant` em um backend para um chatbot de WhatsApp, usando a Z-API como ponte. As interações, que hoje provavelmente acontecem via uma API REST síncrona, passarão a ser assíncronas e orientadas a eventos (webhooks).

#### **Passo 1: Configuração do Ambiente**

Adicionar as novas propriedades necessárias para a comunicação com a Z-API no arquivo `application.properties` (ou `application.yml`). Essas propriedades devem ser centralizadas para facilitar a manutenção.

```properties
# ===============================
# Z-API Configuration
# ===============================
zapi.api-url=https://api.z-api.io/instances/SUA_INSTANCIA/token/SEU_TOKEN
zapi.send-message-endpoint=/send-messages
```

- **Ação:** Criar uma classe de configuração (`@ConfigurationProperties`) para carregar esses valores de forma estruturada e segura no Spring.

#### **Passo 2: Criar um Endpoint de Webhook para Receber Mensagens**

Atualmente, o projeto não tem um ponto de entrada para o fluxo que você descreveu. Precisamos criar um novo Controller que receberá as notificações da Z-API.

- **Ação 1: Criar DTOs (Data Transfer Objects):**
  - Criar classes Java para mapear o JSON que a Z-API envia via webhook. Com base na documentação da Z-API, teríamos objetos para representar a mensagem, o remetente, etc.
  - Exemplo: `ZApiWebhookPayload.java`, `MessageData.java`.

- **Ação 2: Criar o `WebhookController`:**
  - Criar um novo `RestController` (ex: `WhatsAppWebhookController.java`) com um método que responda ao método `POST` em um endpoint específico (ex: `/webhooks/zapi`).
  - Este método receberá o `ZApiWebhookPayload` como corpo da requisição (`@RequestBody`).
  - A principal responsabilidade deste controller será:
    1.  Receber a notificação da Z-API.
    2.  Extrair a mensagem do usuário e o número de telefone do remetente.
    3.  Chamar o serviço de chat existente para processar a mensagem.
    4.  Enviar a resposta de volta usando um novo serviço cliente da Z-API (detalhado no Passo 3).

#### **Passo 3: Criar um Cliente para Enviar Mensagens via Z-API**

Para enviar a resposta de volta ao usuário, o `virtual-assistant` precisa fazer uma chamada HTTP para a API da Z-API. A melhor prática é encapsular essa lógica em um serviço dedicado.

- **Ação 1: Criar o `ZApiClientService`:**
  - Criar uma nova classe de serviço (ex: `ZApiClientService.java`).
  - Utilizar um cliente HTTP, como o `WebClient` (reativo) ou `RestTemplate` (tradicional) do Spring, para fazer chamadas à Z-API.
  - Criar um método como `enviarMensagem(String telefone, String texto)`.
  - Este método irá:
    1.  Montar o corpo da requisição (JSON) esperado pela Z-API para envio de mensagens.
    2.  Fazer uma chamada `POST` para o endpoint `zapi.api-url` + `zapi.send-message-endpoint`.
    3.  Implementar tratamento de erros para o caso de a Z-API retornar um erro ou estar indisponível.

#### **Passo 4: Adaptar o Serviço de Chat Existente**

O fluxo de negócio principal precisa ser ajustado para orquestrar a nova sequência de operações.

- **Ação: Modificar o `ChatService` (ou criar um novo orquestrador):**
  - O `WebhookController` chamará um método no serviço de chat, passando a mensagem do usuário.
  - Este serviço executará a lógica já existente: RAG + consulta ao modelo de IA (Claude).
  - Após receber a resposta da IA, em vez de retorná-la diretamente, o serviço chamará o `ZApiClientService.enviarMensagem()` para despachar a resposta para o WhatsApp do cliente.

#### **Passo 5: Atualizar a Documentação**

Após a implementação, o `README.md` que criei precisará ser atualizado para refletir o novo fluxo de funcionamento.

- **Ação:** Modificar a seção "Fluxo de Funcionamento" no `docs/README.md` para descrever o novo processo baseado em WhatsApp e webhooks, detalhando a interação entre o cliente, a Z-API e o `virtual-assistant`.

### **Resumo do Plano de Ação:**

1.  **Configurar:** Adicionar propriedades da Z-API em `application.properties`.
2.  **Receber:** Criar um `WebhookController` com DTOs para aceitar as chamadas da Z-API.
3.  **Responder:** Criar um `ZApiClientService` para encapsular o envio de mensagens para a Z-API.
4.  **Orquestrar:** Ajustar o serviço de chat para, após processar a mensagem, usar o `ZApiClientService` para enviar a resposta.
5.  **Documentar:** Atualizar o `README.md` com o novo fluxo.
