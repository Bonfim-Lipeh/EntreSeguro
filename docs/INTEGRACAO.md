# Integração real: painel ↔ API Java ↔ MySQL

Validada em 10/11/2026 (Windows 11, Docker Desktop, JDK 21, Maven 3.10, Node 24, Chrome).

## Pré-requisitos

| Ferramenta | Versão | Como conferir |
|---|---|---|
| Docker Desktop (com Compose) | qualquer recente, **em execução** | `docker compose version` |
| JDK | **21** | `mvn -v` deve mostrar `Java version: 21…` (o Maven usa o `JAVA_HOME`; o `java` do PATH pode ser outro) |
| Maven | 3.9+ | `mvn -v` |
| Node.js | 20.19+ ou 22.12+ | `node -v` |
| Navegador | Chrome ou Edge | — |

Portas livres: **3306** (MySQL), **8080** (backend), **5173** (painel). Nenhum arquivo `.env` é necessário:
o perfil `dev` tem valores padrão de desenvolvimento, e `frontend/.env.http` já está no projeto.

## Procedimento de demonstração

### 1. Banco de dados

```bash
cd backend
docker compose up -d mysql      # SOMENTE o serviço mysql (ver Riscos: serviço "backend" do compose)
docker compose ps               # aguarde "healthy"
```

### 2. Backend (aplica as migrations e prepara os dados fictícios)

```bash
cd backend
mvn spring-boot:run
```

Pronto quando aparecer `Started EntradaSeguraApplication`. Na primeira execução o Flyway aplica
V1–V4 e o seed fictício `db/seed-dev` (perfil `dev`); nas seguintes, só o que faltar.

### 3. Painel

```bash
cd frontend
npm install                     # primeira vez
npm run dev:http                # abra http://localhost:5173  (use "localhost", não 127.0.0.1)
```

### 4. Roteiro

1. Entrar com `portaria@demo.entradasegura.local` / `Portaria@2026`. Não aparece o selo "Modo demonstração".
2. Fila FIFO: Theo (identificado), Alice (em atendimento, ⚠), Davi, Lara e Bruno (aguardando).
3. Selecionar **Davi Mendes Rocha** → **Iniciar atendimento**.
4. **Confirmar identificação** → marcar a conferência do documento com foto → **Confirmar identificação**.
5. **Confirmar retirada do aluno** → **Confirmar saída**. Aviso "Retirada concluída: Davi Mendes Rocha".
6. **Histórico de movimentações**: primeira linha = Davi, Concluída, Marina Lopes. O indicador "concluídas hoje" aumenta.
7. Opcional: **Alice** só tem "rejeitar" (autorização vencida) → justificativa obrigatória → aparece no histórico como Rejeitada.
8. Opcional: `gestor@demo.entradasegura.local` → "Seu perfil não tem permissão…" (403).

### Recomeçar do zero (destrutivo, só se você quiser)

As operações gravam de verdade (Davi concluído não volta para a fila). Para restaurar o estado inicial,
**apagando o banco local de desenvolvimento**:

```bash
# pare o backend (Ctrl+C) e então:
cd backend && docker compose down -v && docker compose up -d mysql && mvn spring-boot:run
```

### Contas fictícias (somente perfil dev)

| E-mail | Perfil | Uso |
|---|---|---|
| `portaria@demo.entradasegura.local` | FUNCIONARIO, escola 1 | demonstração principal |
| `portaria.escola2@demo.entradasegura.local` | FUNCIONARIO, escola 2 | isolamento escolar |
| `gestor@demo.entradasegura.local` | GESTOR | sem acesso ao painel (403) |

Senha de todas: `Portaria@2026` (hash BCrypt no seed). Nomes, documentos e e-mails são fictícios (`*.demo.local`).

## Verificação automatizada

Com MySQL, backend e painel rodando:

```bash
bash scripts/smoke-integracao.sh            # 52 verificações HTTP (Git Bash no Windows)
cd frontend && npm run test:e2e:integracao  # 11 testes no navegador
#   sem o Chromium do Playwright: PW_CHROME_PATH="C:/Program Files/Google/Chrome/Application/chrome.exe" npm run test:e2e:integracao
docker exec -i entradasegura-mysql mysql -uentradasegura -pentradasegura entradasegura < scripts/verificar-consistencia.sql
```

O smoke e o Playwright de integração **criam** alunos fictícios ("Aluno E2E…", "Aluno Smoke…") no banco
indicado; não apagam nada. Para não misturar com a demonstração, rode-os contra outro MySQL e informe o
container em `MYSQL_CONTAINER` (padrão `entradasegura-mysql`).

Sem backend: `cd backend && mvn test` (Testcontainers, exige Docker) e `cd frontend && npm test && npm run test:e2e`.

## Autenticação no frontend

- `POST /api/auth/login` → access token (JWT, 15 min) **só em memória**; refresh token em cookie `HttpOnly`.
- Toda chamada a `/api/portaria/**` envia `Authorization: Bearer` (único meio aceito; HTTP Basic desativado).
- 401 → uma única renovação (`POST /api/auth/refresh`) → repete a chamada; renovação recusada → login com "Sua sessão expirou".
- CSRF: cookie `XSRF-TOKEN` repetido no cabeçalho `X-XSRF-TOKEN` em todo POST.

## Endpoints

| Endpoint | Situação |
|---|---|
| `POST /api/auth/login`, `/refresh`, `/logout` | integrado e testado |
| `GET /api/portaria/sessao`, `/retiradas`, `/retiradas/{id}`, `/resumo`, `/movimentacoes` | integrados e testados |
| `POST /api/portaria/retiradas/{id}/iniciar-atendimento` `{versao}` | integrado e testado |
| `POST /api/portaria/retiradas/{id}/confirmar-identificacao` `{metodo: "DOCUMENTO", versao}` | integrado e testado |
| `POST /api/portaria/retiradas/{id}/confirmar-retirada` `{versao}` | integrado e testado |
| `POST /api/portaria/retiradas/{id}/rejeitar` `{motivo, justificativa, versao}` | integrado e testado |
| `POST /api/portaria/retiradas/{id}/validar-identificacao` (QR) | **não implementado** |

Erros em ProblemDetail com `codigo`: `RETIRADA_NAO_ENCONTRADA` (404); `TRANSICAO_INVALIDA`, `VERSAO_DESATUALIZADA`,
`AUTORIZACAO_EXPIRADA` (409); `JUSTIFICATIVA_OBRIGATORIA`, `MOTIVO_INVALIDO`, `QR_NAO_VALIDADO`, `VERSAO_OBRIGATORIA` (422).

## Regras aplicadas no backend

- Funcionário e escola vêm do JWT; solicitação de outra escola responde 404. Somente perfil FUNCIONARIO; POST exige CSRF.
- Transições decididas pela entidade `SolicitacaoRetirada`; status, movimentação e auditoria na mesma transação.
- Concorrência otimista: a `versao` enviada precisa ser a atual; em operações simultâneas, só uma vence.
- Autorização vigente = status `ATIVA`, agora entre `data_inicio` e `data_fim`, responsável ativo.
  Conferida na identificação e de novo na saída.
- Rejeição: justificativa de 10 a 500 caracteres. A partir de `IDENTIFICADO`, só com `AUTORIZACAO_EXPIRADA`
  e autorização de fato não vigente.

## Riscos e pendências conhecidas

1. **QR Code** não implementado (sem emissão de credenciais); `metodo=QR_CODE` → 422; o painel http não mostra o leitor.
2. **Identificação manual**: a conferência física do documento é declarada pelo funcionário; o backend não compara
   o número do documento (não há esquema de HMAC de documentos). O campo "Documento" mostra um valor fictício mascarado.
3. **Sem tempo real**: a fila atualiza a cada 10 s (outro posto vê a mudança em até 10 s; conflitos viram 409).
4. **Datas sem fuso** na API (`LocalDateTime` no fuso da JVM): backend e navegador devem estar no mesmo fuso.
5. **Serviço "backend" do docker-compose não funciona**: `JWT_SECRET` padrão recusado, faltam `JWT_ISSUER`/`JWT_AUDIENCE`
   e a imagem exige `target/*.jar`. Use `docker compose up -d mysql` (nunca `docker compose up` sozinho).
6. **Docker Desktop no Windows**: se o backend falhar com "Communications link failure" embora o MySQL esteja
   "healthy", o encaminhamento da porta travou: `docker restart entradasegura-mysql`.
7. Cookie de refresh `Secure`: funciona em `http://localhost`, não em outro host via HTTP.
8. Valores padrão de desenvolvimento (senha do MySQL `entradasegura`, segredo JWT de dev) estão em
   `application-dev.yml`; o perfil `prod` exige variáveis de ambiente.
9. Sem limite de tentativas de login (força bruta) — fora do escopo desta etapa.
