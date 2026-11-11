# CLAUDE.md — EntradaSegura

Regras permanentes do projeto. Leia antes de qualquer alteração.

## 1. Contexto

- Projeto acadêmico da disciplina **UPX II (Facens)**: controle de retirada de alunos em instituições de ensino.
- Usuários: responsável (aplicativo), funcionário da portaria (painel), gestor/administrador (módulo administrativo).
- Requisitos acadêmicos inegociáveis: **Java + POO real**, banco relacional SQL com persistência real, relacionamentos, CRUD e regras de negócio no backend.

## 2. Stack definida

| Camada | Tecnologia |
|---|---|
| Backend | Java 21 LTS, Spring Boot 3.x (Web, Data JPA, Security, Validation, WebSocket/STOMP, Actuator), Flyway, springdoc-openapi |
| Banco | **MySQL 8.4** |
| Frontend | React + TypeScript + Vite, Tailwind CSS, shadcn/ui, Lucide, TanStack Query |
| Testes | JUnit 5, Mockito, MockMvc, Testcontainers (backend); Vitest, Testing Library, Playwright (frontend) |
| Infra | Docker, Docker Compose, GitHub Actions |

Arquitetura: **monólito modular** organizado por domínio. Não introduzir tecnologia sem justificar o propósito.

## 3. Máquina de estados da retirada (fonte: diagrama de classes + fluxograma + sequência)

Enum `StatusRetirada`: `AGUARDANDO`, `EM_ATENDIMENTO`, `IDENTIFICADO`, `CONCLUIDA`, `REJEITADA`, `CANCELADA`.

| Status atual | Ação | Ator | Próximo status |
|---|---|---|---|
| AGUARDANDO | Iniciar atendimento | Funcionário | EM_ATENDIMENTO |
| EM_ATENDIMENTO | Confirmar identificação (identificação confere) | Funcionário | IDENTIFICADO |
| EM_ATENDIMENTO | Rejeitar (identificação não confere) — **justificativa obrigatória** | Funcionário | REJEITADA |
| IDENTIFICADO | Confirmar retirada | Funcionário | CONCLUIDA |
| AGUARDANDO | Cancelar | Responsável | CANCELADA |

Regras:
- `CONCLUIDA`, `REJEITADA` e `CANCELADA` são **estados finais**.
- Não existe rejeição a partir de `IDENTIFICADO` nem de `AGUARDANDO`.
- Conclusão só a partir de `IDENTIFICADO`.
- A autorização do responsável deve ser **revalidada no backend no momento da confirmação da retirada**.
- Ao concluir, registrar responsável, funcionário e horário (Movimentação).
- **A máquina de estados definitiva pertence ao backend Java** (métodos de domínio em `Retirada`: `iniciarAtendimento`, `confirmarIdentificacao`, `confirmarRetirada`, `rejeitar`). O frontend nunca decide transições: exibe apenas as ações listadas em `acoesPermitidas` do DTO.

## 4. QR Code

- Código opaco e aleatório, vinculado a uma solicitação; sem dados pessoais no QR.
- Validação exclusivamente no backend: `POST /api/portaria/retiradas/{id}/validar-identificacao`.
- **Um QR válido não conclui nem autoriza a retirada automaticamente**: o funcionário ainda confirma a identificação e depois a retirada.

## 5. Frontend — regras

- Componentes visuais não contêm regras de negócio nem chamadas diretas a `fetch`.
- Camadas: `domain/` (tipos/DTOs) → `services/contracts` (interface) → `services/mock` | `services/http` → `hooks/` → `features/*/components`.
- Fonte de dados escolhida por `VITE_API_MODE` (`mock` | `http`). Trocar mock por API real não deve exigir alteração em componentes.
- Erros da API seguem `ProblemDetail` (RFC 7807): 409 transição inválida, 422 validação, 404 não encontrado.
- Modo simulado deve exibir o selo **"MODO DEMONSTRAÇÃO"**. Nenhuma funcionalidade simulada sem sinalização explícita.
- Textos da interface em português do Brasil.
- Paleta institucional: `#081C2F`, `#102A43`, `#243B53`, dourado `#D4AF37` (somente destaque), off-white `#F8F5E6`.

## 6. Dados e privacidade (LGPD)

- **Nunca** usar dados reais de menores em mocks, testes, logs, screenshots ou ferramentas externas. Dados de demonstração são 100% fictícios; avatares por iniciais, sem fotos.
- CSV real de alunos: apenas cópia anonimizada em desenvolvimento.
- Não registrar senhas, tokens ou dados pessoais desnecessários em logs.
- Biometria, RFID/NFC e geolocalização: somente avaliação na Etapa 13.

## 7. Escopo por etapa

- **Etapa atual — Painel da portaria (frontend):** fila FIFO, filtros, pesquisa, detalhes, ações de status, QR simulado, histórico, indicadores. Dados simulados.
- **Fora desta etapa:** CRUDs (alunos, responsáveis, funcionários, turmas), gestão administrativa, cadastro de escolas, login real, integração MySQL, biometria, RFID/NFC, geolocalização.
- **Etapa Java:** `Aluno` deve se relacionar com a entidade `Turma` (não `String`), conforme modelo de dados.

## 8. Comandos

```bash
cd frontend
npm install
npm run dev        # http://localhost:5173
npm run build      # typecheck + build de produção
npm test           # Vitest
npm run test:e2e   # Playwright (requer navegador instalado)
```

## 9. Critérios de conclusão

Uma funcionalidade só está concluída quando: compila, possui teste executado com sucesso, foi verificada na aplicação em execução e suas limitações estão documentadas. Não declarar nada como funcional sem evidência de execução.
