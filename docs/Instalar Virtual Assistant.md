# Análise de Viabilidade e Guia de Implantação

## Análise de Viabilidade: HostGator "Hospedagem de Sites"

A resposta curta e direta é: **não, não é viável (e muito provavelmente impossível) implantar esses projetos diretamente no seu plano de "Hospedagem de Sites" da HostGator.**

**Motivos:**

1.  **Ambiente Inadequado:** Planos de hospedagem compartilhada (como "Hospedagem de Sites") são otimizados para tecnologias como **PHP (WordPress, Joomla), HTML e MySQL**. Eles não são projetados para executar aplicações Java (como Spring Boot) que funcionam como servidores independentes.
2.  **Falta de Acesso e Permissões:** Você não tem acesso de administrador (root) ao servidor. Isso significa que você **não pode**:
    *   Instalar a versão correta do Java (JDK) que seus projetos exigem.
    *   Instalar e executar serviços essenciais como **PostgreSQL** (com a extensão `pgvector`) e **Redis**, que são dependências críticas do seu `virtual-assistant`.
    *   Executar um processo Java de longa duração e expor a porta dele (ex: 8080) para a internet. O servidor web (Apache/Nginx) da hospedagem não saberá como se comunicar com sua aplicação.
3.  **Docker não é Suportado:** A tecnologia de contêineres (Docker) requer acesso de administrador para ser instalada e executada, algo que não está disponível em planos de hospedagem compartilhada.

## A Solução Correta: Servidor VPS e Contêineres Docker

A maneira profissional e correta de hospedar essas aplicações é utilizando um **Servidor VPS (Virtual Private Server)**. A própria HostGator oferece planos VPS, ou você pode usar outros provedores de nuvem como DigitalOcean, Vultr, AWS, etc.

Um VPS lhe dá um servidor virtual completo com acesso root, onde você pode instalar e configurar o ambiente exatamente como precisa.

Usar **Docker** nesse cenário é a melhor prática, pois ele empacota suas aplicações e todas as suas dependências (Java, PostgreSQL, Redis) em contêineres isolados, facilitando a implantação, a manutenção e garantindo que tudo funcione da mesma forma que na sua máquina de desenvolvimento.

---

## Documento: Passos para Implantação com Docker em um Servidor VPS

Aqui está o guia passo a passo para implantar seus projetos usando a abordagem de contêineres em um servidor VPS.

#### **Pré-requisitos (na sua máquina local)**

1.  **Docker Desktop:** Instale o Docker na sua máquina.
2.  **JDK e Maven:** Já configurados para construir os projetos.
3.  **Conta no Docker Hub:** Crie uma conta em hub.docker.com para armazenar suas imagens de contêiner.

---

### **Passo 1: Criar os `Dockerfiles` para cada Aplicação**

Um `Dockerfile` é uma "receita" para criar uma imagem de contêiner. Crie um arquivo chamado `Dockerfile` (sem extensão) na raiz de cada um dos seus dois projetos.

**1.1. `prospecting-service/Dockerfile`**

```dockerfile
# Use uma imagem base com Java 21 (LTS recomendado, o Java 25 do seu pom.xml é muito recente e não é LTS)
FROM eclipse-temurin:21-jdk-jammy

# Define o diretório de trabalho dentro do contêiner
WORKDIR /app

# Copia o arquivo .jar compilado do seu projeto para o contêiner
# O nome do .jar pode variar, ajuste se necessário
COPY target/prospecting-service-0.0.1-SNAPSHOT.jar app.jar

# Expõe a porta que a aplicação Spring Boot usa
EXPOSE 8080

# Comando para executar a aplicação quando o contêiner iniciar
ENTRYPOINT ["java", "-jar", "app.jar"]
```

**1.2. `virtual-assistant/Dockerfile`**

```dockerfile
# Use a mesma imagem base para consistência
FROM eclipse-temurin:21-jdk-jammy

# Define o diretório de trabalho
WORKDIR /app

# Copia o arquivo .jar compilado
COPY target/virtual_assistant-0.0.1-SNAPSHOT.jar app.jar

# Expõe a porta da aplicação
EXPOSE 8080

# Comando para executar a aplicação
ENTRYPOINT ["java", "-jar", "app.jar"]
```

### **Passo 2: Compilar e "Dockerizar" suas Aplicações**

Para cada projeto, execute os seguintes comandos no terminal, a partir da pasta raiz de cada um:

1.  **Compile o projeto com Maven:**
    ```sh
    mvn clean package -DskipTests
    ```

2.  **Construa a imagem Docker e marque-a (tag):**
    (Substitua `seu-usuario-dockerhub` pelo seu nome de usuário)

    *Para `prospecting-service`:*
    ```sh
    docker build -t seu-usuario-dockerhub/prospecting-service:latest .
    ```

    *Para `virtual-assistant`:*
    ```sh
    docker build -t seu-usuario-dockerhub/virtual-assistant:latest .
    ```

3.  **Envie as imagens para o Docker Hub:**
    ```sh
    docker login # (Faça login com seu usuário e senha)
    docker push seu-usuario-dockerhub/prospecting-service:latest
    docker push seu-usuario-dockerhub/virtual-assistant:latest
    ```

### **Passo 3: Configurar o Servidor VPS e o Docker Compose**

Agora, conecte-se ao seu servidor VPS via SSH e vamos preparar o ambiente para receber os contêineres.

1.  **Instale o Docker e o Docker Compose no VPS:**
    ```sh
    sudo apt-get update
    sudo apt-get install -y docker.io docker-compose
    ```

2.  **Crie um arquivo `docker-compose.yml`:**
    Este arquivo irá orquestrar todos os seus serviços (as duas aplicações, o banco de dados e o Redis) para que funcionem juntos. Crie um arquivo chamado `docker-compose.yml` no seu VPS.

    ```yaml
    version: '3.8'

    services:
      # Serviço do Banco de Dados PostgreSQL com pgvector
      postgres-db:
        image: pgvector/pgvector:pg16 # Imagem que já inclui a extensão pgvector
        container_name: postgres-db
        environment:
          - POSTGRES_USER=admin
          - POSTGRES_PASSWORD=secret
          - POSTGRES_DB=ia369_db
        volumes:
          - postgres_data:/var/lib/postgresql/data
        ports:
          - "5432:5432"
        restart: unless-stopped

      # Serviço de Cache em Memória Redis
      redis-cache:
        image: redis:latest
        container_name: redis-cache
        ports:
          - "6379:6379"
        restart: unless-stopped

      # Serviço de Prospecção
      prospecting-service:
        image: seu-usuario-dockerhub/prospecting-service:latest # Imagem do Docker Hub
        container_name: prospecting-service
        ports:
          - "8081:8080" # Mapeia a porta 8080 do contêiner para a 8081 do VPS
        environment:
          # NOTA: O ideal é configurar este serviço para usar o Postgres também!
          # Atualmente ele usa H2, que é inadequado para produção.
          # Exemplo de como seria a configuração para o Postgres:
          - SPRING_DATASOURCE_URL=jdbc:postgresql://postgres-db:5432/ia369_db
          - SPRING_DATASOURCE_USERNAME=admin
          - SPRING_DATASOURCE_PASSWORD=secret
        depends_on:
          - postgres-db
        restart: unless-stopped

      # Serviço de Assistente Virtual
      virtual-assistant:
        image: seu-usuario-dockerhub/virtual-assistant:latest # Imagem do Docker Hub
        container_name: virtual-assistant
        ports:
          - "8082:8080" # Mapeia a porta 8080 do contêiner para a 8082 do VPS
        environment:
          # Configurações para conectar aos outros contêineres
          - SPRING_AI_VECTORSTORE_PGVECTOR_DATASOURCE_URL=jdbc:postgresql://postgres-db:5432/ia369_db
          - SPRING_AI_VECTORSTORE_PGVECTOR_DATASOURCE_USERNAME=admin
          - SPRING_AI_VECTORSTORE_PGVECTOR_DATASOURCE_PASSWORD=secret
          - SPRING_DATA_REDIS_HOST=redis-cache
          - SPRING_DATA_REDIS_PORT=6379
        depends_on:
          - postgres-db
          - redis-cache
        restart: unless-stopped

    volumes:
      postgres_data: # Volume para persistir os dados do Postgres
    ```

### **Passo 4: Executar as Aplicações no VPS**

1.  Com o arquivo `docker-compose.yml` criado no seu VPS, execute o seguinte comando na mesma pasta:
    ```sh
    sudo docker-compose up -d
    ```
    O `-d` significa "detached", para que tudo rode em segundo plano.

2.  **Verifique se tudo está funcionando:**
    ```sh
    sudo docker-compose ps   # Mostra os contêineres em execução
    sudo docker-compose logs -f # Mostra os logs de todos os serviços em tempo real
    ```

**Pronto!** Se tudo correu bem:

*   O `prospecting-service` estará acessível em `http://<IP_DO_SEU_VPS>:8081`.
*   O `virtual-assistant` estará acessível em `http://<IP_DO_SEU_VPS>:8082`.
