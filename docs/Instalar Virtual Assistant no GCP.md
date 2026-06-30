# Guia de Implantação no Google Cloud Platform (GCP) com Cloud Run

O Google Cloud Platform (GCP) é uma opção excelente e muito mais poderosa do que a hospedagem compartilhada ou um VPS simples para este tipo de aplicação. A abordagem recomendada é usar o **Cloud Run** para executar seus contêineres e serviços gerenciados para banco de dados e cache.

### Vantagens do GCP sobre um VPS Simples

*   **Serviços Gerenciados:** Em vez de instalar e gerenciar o PostgreSQL e o Redis, você usará os serviços gerenciados do GCP: **Cloud SQL** (para PostgreSQL) e **Memorystore** (para Redis). O Google cuida de backups, atualizações, segurança e escalabilidade.
*   **Escalabilidade Automática:** O Cloud Run pode criar mais instâncias da sua aplicação automaticamente se o tráfego aumentar e reduzi-las (até zero) quando não houver tráfego, otimizando os custos.
*   **Segurança Integrada:** O GCP fornece ferramentas robustas de gerenciamento de identidade, redes seguras e gerenciamento de segredos.
*   **Implantação Simplificada:** Com a CLI do Google Cloud (`gcloud`), você pode implantar uma nova versão da sua aplicação com um único comando.

---

### **Pré-requisitos**

1.  **Conta no GCP:** Crie uma conta e um novo projeto no [Google Cloud Console](https://console.cloud.google.com/).
2.  **SDK do Google Cloud:** Instale e configure a CLI `gcloud` na sua máquina.
3.  **Dockerfiles Prontos:** Use os mesmos `Dockerfile` criados anteriormente para cada projeto.

---

### **Passo 1: Enviar as Imagens para o Artifact Registry**

Em vez do Docker Hub, é melhor usar o registro de contêineres do próprio GCP, o Artifact Registry.

1.  **Habilite a API e crie um repositório:**
    ```sh
    gcloud services enable artifactregistry.googleapis.com
    gcloud artifacts repositories create qr369-repo --repository-format=docker --location=us-central1
    ```

2.  **Autentique o Docker com o GCP:**
    ```sh
    gcloud auth configure-docker us-central1-docker.pkg.dev
    ```

3.  **Compile e envie suas imagens:**
    (Substitua `seu-projeto-gcp` pelo ID do seu projeto no GCP)

    *Para `prospecting-service`:*
    ```sh
    # 1. Compile o JAR
    mvn clean package -DskipTests
    # 2. Construa e marque a imagem
    docker build -t us-central1-docker.pkg.dev/seu-projeto-gcp/qr369-repo/prospecting-service:latest .
    # 3. Envie a imagem
    docker push us-central1-docker.pkg.dev/seu-projeto-gcp/qr369-repo/prospecting-service:latest
    ```

    *Para `virtual-assistant`:*
    ```sh
    # 1. Compile o JAR
    mvn clean package -DskipTests
    # 2. Construa e marque a imagem
    docker build -t us-central1-docker.pkg.dev/seu-projeto-gcp/qr369-repo/virtual-assistant:latest .
    # 3. Envie a imagem
    docker push us-central1-docker.pkg.dev/seu-projeto-gcp/qr369-repo/virtual-assistant:latest
    ```

### **Passo 2: Configurar os Serviços Gerenciados (Banco de Dados e Cache)**

No Console do GCP:

1.  **Crie uma instância do Cloud SQL para PostgreSQL:**
    *   Vá para "Cloud SQL" -> "Criar instância".
    *   Escolha PostgreSQL.
    *   Defina uma senha de administrador e anote-a.
    *   **Importante:** Habilite a extensão `pgvector` nesta instância. Você pode fazer isso conectando-se à instância após a criação e executando o comando SQL: `CREATE EXTENSION IF NOT EXISTS vector;`.
    *   Crie um banco de dados chamado `ia369_db`.

2.  **Crie uma instância do Memorystore para Redis:**
    *   Vá para "Memorystore" -> "Redis" -> "Criar instância".
    *   Escolha as configurações básicas. O nível mais simples é suficiente para começar.

### **Passo 3: Implantar as Aplicações no Cloud Run**

**Nota sobre Segredos:** Antes de rodar os comandos abaixo, crie um segredo no **GCP Secret Manager** chamado `ANTHROPIC_API_KEY` com o valor da sua chave da Anthropic.

Execute estes comandos na sua máquina local. O `gcloud` fará a implantação no seu projeto na nuvem.

1.  **Implantar `prospecting-service`:**
    ```sh
    gcloud run deploy prospecting-service \
      --image=us-central1-docker.pkg.dev/seu-projeto-gcp/qr369-repo/prospecting-service:latest \
      --platform=managed \
      --region=us-central1 \
      --allow-unauthenticated \
      --set-env-vars="SPRING_DATASOURCE_URL=jdbc:postgresql://<IP_DO_CLOUD_SQL>:5432/ia369_db" \
      --set-env-vars="SPRING_DATASOURCE_USERNAME=postgres" \
      --set-env-vars="SPRING_DATASOURCE_PASSWORD=<SENHA_DO_CLOUD_SQL>"
    ```

2.  **Implantar `virtual-assistant`:**
    ```sh
    gcloud run deploy virtual-assistant \
      --image=us-central1-docker.pkg.dev/seu-projeto-gcp/qr369-repo/virtual-assistant:latest \
      --platform=managed \
      --region=us-central1 \
      --allow-unauthenticated \
      --set-env-vars="SPRING_AI_VECTORSTORE_PGVECTOR_DATASOURCE_URL=jdbc:postgresql://<IP_DO_CLOUD_SQL>:5432/ia369_db" \
      --set-env-vars="SPRING_AI_VECTORSTORE_PGVECTOR_DATASOURCE_USERNAME=postgres" \
      --set-env-vars="SPRING_AI_VECTORSTORE_PGVECTOR_DATASOURCE_PASSWORD=<SENHA_DO_CLOUD_SQL>" \
      --set-env-vars="SPRING_DATA_REDIS_HOST=<IP_DO_MEMORISTORE>" \
      --set-env-vars="SPRING_DATA_REDIS_PORT=6379" \
      --set-env-vars="SPRING_PROFILES_ACTIVE=anthropic" \
      --set-secrets="ANTHROPIC_API_KEY=ANTHROPIC_API_KEY:latest"
    ```

    *Substitua `<IP_DO_CLOUD_SQL>`, `<SENHA_DO_CLOUD_SQL>` e `<IP_DO_MEMORISTORE>` pelos valores reais dos serviços que você criou no GCP.*

**Pronto!** Após a execução dos comandos, o GCP fornecerá um **URL HTTPS seguro** para cada um dos seus serviços, que já estarão funcionando e conectados ao banco de dados e ao cache gerenciados.
