# Arquitetura e Funcionamento do Projeto `virtual-assistant`

## Visão Geral

Este é um aplicativo Spring Boot que funciona como um assistente virtual, utilizando o Spring AI para se integrar a vários modelos de IA. O projeto foi construído para ser uma base para assistentes de conversação inteligentes que podem ser aprimorados com conhecimento de documentos específicos através da técnica de Retrieval-Augmented Generation (RAG).

A interação com o assistente é feita através do WhatsApp, utilizando a Z-API como camada de comunicação.

## Tecnologias Principais

- **Spring Boot:** Estrutura principal do aplicativo.
- **Spring AI:** Facilita a integração com modelos de IA (Anthropic, Ollama, etc.).
- **Z-API:** Camada de abstração para comunicação com o WhatsApp.
- **Spring Data Redis:** Utilizado para gerenciar o histórico de bate-papo.
- **PgVector + PostgreSQL:** Banco de dados vetorial para RAG.
- **Apache Tika:** Para extração de texto de documentos.
- **Maven:** Gerenciamento de dependências.
- **Docker:** Para gerenciamento do ambiente de desenvolvimento.

## Arquitetura

A aplicação segue uma estrutura padrão de projetos Spring Boot:

- **`br.com.ia369.virtual_assistant`**: Pacote raiz.
  - **`VirtualAssistantApplication.java`**: Ponto de entrada da aplicação.
  - **`config`**: Classes de configuração do Spring.
  - **`chat`**: Lógica de negócio principal para processamento de chat com IA.
  - **`ingestion`**: Pipeline de ingestão de dados para RAG.
  - **`whatsapp`**: Pacote responsável pela comunicação com a Z-API.
    - **`dto`**: Data Transfer Objects para os payloads da Z-API.
    - **`WhatsAppWebhookController`**: Endpoint que recebe as mensagens do WhatsApp via webhook.
    - **`ZApiClientService`**: Cliente HTTP para enviar mensagens de volta para o usuário via Z-API.
  - **`ferias`**: Exemplo de funcionalidade de negócio específica.

## Fluxo de Funcionamento

O fluxo de interação é assíncrono e orientado a eventos, utilizando webhooks.

1.  **Cliente Envia Mensagem no WhatsApp:**
    - O usuário inicia a conversa enviando uma mensagem para o número de WhatsApp associado.

2.  **Z-API Recebe e Encaminha via Webhook:**
    - A Z-API recebe a mensagem e a envia para o endpoint de webhook configurado no projeto `virtual-assistant` (`/webhooks/zapi`).

3.  **Processamento no `virtual-assistant`:**
    - O `WhatsAppWebhookController` recebe o payload da Z-API.
    - Ele extrai a mensagem e o número de telefone do usuário.
    - O `ChatService` é chamado, utilizando o número de telefone como `conversationId` para manter o contexto.
    - O `ChatService` executa a lógica de RAG (se necessário) e chama o modelo de IA configurado para gerar uma resposta.

4.  **Envio da Resposta ao Cliente:**
    - Com a resposta gerada pela IA, o `WhatsAppWebhookController` chama o `ZApiClientService`.
    - O `ZApiClientService` faz uma chamada à API da Z-API para enviar a mensagem de resposta para o WhatsApp do cliente.

Este fluxo permite que o assistente virtual converse com os usuários de forma natural através do WhatsApp, aproveitando todo o poder do backend de IA para fornecer respostas inteligentes e contextuais.

## Como executar ##

Configurar as variáveis de ambiente

### Via cmd 
set ANTHROPIC_API_KEY=valor_api_key
set SPRING_PROFILES_ACTIVE=anthropic

### Via PowerShell 
`$env:ANTHROPIC_API_KEY = "valor_api_key"`
`$env:SPRING_PROFILES_ACTIVE = "anthropic"`

### Executar a aplicação (normal e background) 
> java -jar target/virtual_assistant-0.0.1-SNAPSHOT.jar
> javaw -jar target/virtual_assistant-0.0.1-SNAPSHOT.jar

Utiliza o ngrok para tornar a URL localhost:8080 (chat do sistema) pública e visível na internet.
> ngrok http 8080

Docker executando e 3 containers rodando:
* virtual-assistant-db        pgvector/pgvector:pg18      5432->5432/tcp
* virtual-assistant-ollama    ollama/ollama:latest        11435->11434/tcp
* virtual-assistant-redis     redis/redis-stack:latest    6379->6379/tcp

Z-API com instãncias configuradas e mensalidade paga

Claude API com crédito.
