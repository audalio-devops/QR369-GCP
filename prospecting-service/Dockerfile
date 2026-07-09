# ── Estágio 1: build Maven ────────────────────────────────────────────────────
FROM maven:3.9-eclipse-temurin-21-jammy AS builder

WORKDIR /build

# Copia apenas o pom primeiro (aproveita cache de camadas do Docker)
COPY pom.xml ./
RUN mvn dependency:go-offline -q

# Copia o código-fonte e compila (pula testes para agilizar o build da imagem)
COPY src/ src/
RUN mvn package -DskipTests -q

# ── Estágio 2: runtime leve ───────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-jammy

WORKDIR /app

COPY --from=builder /build/target/prospecting-service-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8081

ENTRYPOINT ["java", "-jar", "app.jar"]
