# Arquitetura e Funcionamento do Projeto `virtual-assistant`

## Visão Geral

Este é um aplicativo Spring Boot que funciona como um assistente virtual, utilizando o Spring AI para se integrar a vários modelos de IA. O projeto foi construído para ser uma base para assistentes de conversação inteligentes que podem ser aprimorados com conhecimento de documentos específicos através da técnica de Retrieval-Augmented Generation (RAG).

## Tecnologias Principais

- **Spring Boot:** Estrutura principal do aplicativo, simplificando a configuração e o desenvolvimento.
- **Spring AI:** Facilita a integração e o uso de diversos modelos de IA. O projeto está configurado para usar:
  - **Anthropic:** Um modelo de linguagem poderoso.
  - **Ollama:** Permite a execução de modelos de código aberto localmente.
  - **Transformers:** Para utilizar modelos do Hugging Face Hub.
- **Spring Data Redis:** Utilizado para armazenamento de dados em memória, especificamente para gerenciar o histórico de bate-papo e manter o contexto da conversa.
- **PgVector + PostgreSQL:** Fornece um banco de dados vetorial para armazenar e consultar embeddings de documentos, sendo um componente essencial para a implementação de RAG.
- **Apache Tika:** Usado para extrair texto de diversos formatos de documentos durante o processo de ingestão de dados.
- **Maven:** Ferramenta de compilação e gerenciamento de dependências do projeto.
- **Docker:** O arquivo `docker-compose.yml` indica que o ambiente de desenvolvimento, incluindo serviços como PostgreSQL e Redis, pode ser facilmente gerenciado com contêineres.

## Arquitetura

A aplicação segue uma estrutura padrão de projetos Spring Boot, organizada nos seguintes pacotes:

- **`br.com.ia369.virtual_assistant`**: Pacote raiz da aplicação.
  - **`VirtualAssistantApplication.java`**: Ponto de entrada que inicializa a aplicação Spring Boot.
  - **`config`**: Contém as classes de configuração para os beans do Spring, como clientes de IA, conexão com o banco de dados e outras configurações de serviço.
  - **`chat`**: Responsável pela lógica de interação do bate-papo. Inclui controllers para os endpoints da API, services para processar as mensagens e a lógica de conversação.
  - **`ingestion`**: Contém a lógica para o pipeline de ingestão de dados. Este pacote é crucial para a funcionalidade de RAG, lidando com a leitura de documentos, criação de embeddings e armazenamento no banco de dados vetorial.
  - **`ferias`**: Provavelmente relacionado a uma funcionalidade de negócio específica do assistente, como um chatbot para responder a perguntas sobre o processo de férias de uma empresa.

## Fluxo de Funcionamento

1.  **Inicialização:**
    - A aplicação é iniciada através da classe `VirtualAssistantApplication`.
    - O Spring Boot autoconfigura os beans com base nas dependências e configurações definidas.

2.  **Ingestão de Dados (RAG):**
    - O serviço no pacote `ingestion` é acionado (pode ser por um endpoint de API ou durante a inicialização).
    - Ele lê documentos de uma fonte configurada (por exemplo, um diretório local).
    - O Apache Tika é usado para extrair o conteúdo de texto dos documentos.
    - O texto extraído é dividido em pedaços (chunks) e, para cada pedaço, um embedding vetorial é gerado usando um dos modelos de IA configurados.
    - Esses embeddings, juntamente com o texto original, são armazenados na tabela do PostgreSQL com a extensão PgVector.

3.  **Interação de Bate-papo:**
    - Um usuário envia uma pergunta para a API do assistente virtual (provavelmente um endpoint no pacote `chat`).
    - A aplicação primeiro converte a pergunta do usuário em um embedding vetorial.
    - Em seguida, ela consulta o banco de dados PgVector para encontrar os documentos (ou pedaços de texto) cujos embeddings são mais semelhantes à pergunta do usuário. Este é o passo de "Retrieval" (Recuperação).
    - A pergunta original do usuário e o contexto recuperado do banco de dados são combinados em um prompt.
    - Este prompt é enviado para um dos modelos de IA (Anthropic, Ollama, etc.) para gerar uma resposta.
    - A resposta gerada pelo modelo é retornada ao usuário através da API.
    - O histórico da conversa pode ser armazenado no Redis para manter o contexto em interações futuras.

Este fluxo permite que o assistente virtual responda a perguntas com base em uma base de conhecimento privada, tornando-o muito mais poderoso e útil para casos de uso específicos.
