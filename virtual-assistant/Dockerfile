# Imagem base com Java 21 (LTS) conforme recomendado na documentação do projeto
FROM eclipse-temurin:21-jdk-jammy

WORKDIR /app

# Copia o arquivo .jar compilado (Certifique-se de rodar 'mvn clean package' antes)
COPY target/virtual_assistant-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]