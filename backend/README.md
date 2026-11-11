# EntradaSegura Backend

Este diretório contém a fundação do backend Java do projeto EntradaSegura.

## Stack definida

- Java 21
- Spring Boot 4.1.0
- Spring Web, Data JPA, Security, Validation, WebSocket, Actuator
- Flyway
- MySQL 8.4
- OpenAPI / Swagger
- Docker e Docker Compose

## Regras desta etapa

A Etapa 1 é apenas a fundação:

- estrutura Maven
- dependências necessárias
- configuração de perfis
- MySQL/Flyway
- Docker
- health check
- apontamento para a API da portaria

A implementação de entidades, regras de negócio, autenticação, CRUD e portaria ficam para etapas posteriores.

## Variáveis de ambiente

Copie o exemplo:

```bash
cp .env.example .env
```

## Execução local

```bash
docker compose up --build
```

O backend ficará disponível em:

- http://localhost:8080
- http://localhost:8080/swagger-ui/index.html
- http://localhost:8080/actuator/health

## Observação importante

Este ambiente local ainda depende da instalação do Maven na máquina do usuário para compilar o projeto com `mvn package`.
