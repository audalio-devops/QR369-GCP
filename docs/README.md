# Documentação dos Projetos

## Visão Geral

Este documento descreve a arquitetura e o funcionamento de dois projetos Spring Boot: `virtual-assistant` e `prospecting-service`.

## Projetos

### Virtual Assistant

O projeto `virtual-assistant` é um serviço de assistente virtual que parece lidar com funcionalidades de chat, ingestão de dados e possivelmente agendamento de férias.

**Estrutura do Projeto:**

*   `chat`: Contém a lógica relacionada ao chat.
*   `config`: Configurações do projeto.
*   `ferias`: Funcionalidades relacionadas a férias.
*   `ingestion`: Lida com a ingestão de dados.

### Prospecting Service

O projeto `prospecting-service` é um serviço para prospecção de clientes, com funcionalidades de CRUD para entidades de prospecção.

**Estrutura do Projeto:**

*   `controller`: Controladores REST.
*   `dto`: Data Transfer Objects.
*   `entity`: Entidades JPA.
*   `repository`: Repositórios Spring Data.
*   `config`: Configurações do projeto.

## Como executar

Cada projeto é uma aplicação Spring Boot e pode ser executado de forma independente.
