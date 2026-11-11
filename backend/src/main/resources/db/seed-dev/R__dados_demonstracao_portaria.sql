-- =====================================================================
-- DADOS DE DEMONSTRAÇÃO — SOMENTE PERFIL dev (100% FICTÍCIOS, LGPD)
-- Carregado apenas quando spring.flyway.locations inclui classpath:db/seed-dev
-- (application-dev.yml). Testes e produção NÃO executam este script.
--
-- Só INSERE o que ainda não existe: não altera nem apaga registros, preservando o
-- estado e o histórico produzidos pelas operações do painel.
-- Para recomeçar a demonstração do zero (horários relativos ao momento atual):
--   cd backend && docker compose down -v && docker compose up -d mysql
--
-- Credenciais de demonstração (senha para todos: Portaria@2026)
--   portaria@demo.entradasegura.local          FUNCIONARIO  Escola 1
--   gestor@demo.entradasegura.local            GESTOR       Escola 1 (painel deve retornar 403)
--   portaria.escola2@demo.entradasegura.local  FUNCIONARIO  Escola 2 (isolamento escolar)
-- =====================================================================

INSERT INTO escola (id, nome, cnpj, razao_social, telefone, email, timezone, status, versao) VALUES
  (1, 'Colégio Horizonte (demonstração)', '11.111.111/0001-11', 'Colégio Horizonte Demonstração Ltda', '(15) 0000-0001', 'contato@horizonte.demo.local', 'America/Sao_Paulo', 'ATIVA', 0),
  (2, 'Escola Aurora (demonstração)',     '22.222.222/0001-22', 'Escola Aurora Demonstração Ltda',     '(15) 0000-0002', 'contato@aurora.demo.local',    'America/Sao_Paulo', 'ATIVA', 0)
ON DUPLICATE KEY UPDATE id = id;

INSERT INTO turma (id, escola_id, nome, nivel, turno, ano_letivo, status, versao) VALUES
  (1, 1, '5º Ano A', 'FUNDAMENTAL_I',  'MANHA', 2026, 'ATIVA', 0),
  (2, 1, '7º Ano B', 'FUNDAMENTAL_II', 'TARDE', 2026, 'ATIVA', 0),
  (3, 2, '3º Ano A', 'FUNDAMENTAL_I',  'MANHA', 2026, 'ATIVA', 0)
ON DUPLICATE KEY UPDATE id = id;

-- Hash BCrypt (cost 10) de "Portaria@2026"
INSERT INTO usuario (id, escola_id, nome, email, senha_hash, perfil, status, versao) VALUES
  (1, 1, 'Marina Lopes',  'portaria@demo.entradasegura.local',         '$2a$10$/YUAVXEm.erkKjQAF5lap.yTBe040d3RBF1a/e/..65K8ShEXJjba', 'FUNCIONARIO', 'ATIVO', 0),
  (2, 1, 'Ricardo Nunes', 'gestor@demo.entradasegura.local',           '$2a$10$/YUAVXEm.erkKjQAF5lap.yTBe040d3RBF1a/e/..65K8ShEXJjba', 'GESTOR',      'ATIVO', 0),
  (3, 2, 'Paulo Teixeira','portaria.escola2@demo.entradasegura.local', '$2a$10$/YUAVXEm.erkKjQAF5lap.yTBe040d3RBF1a/e/..65K8ShEXJjba', 'FUNCIONARIO', 'ATIVO', 0)
ON DUPLICATE KEY UPDATE id = id;

INSERT INTO responsavel (id, escola_id, usuario_id, nome, documento_criptografado, documento_hmac, telefone, email, status, versao) VALUES
  (1, 1, NULL, 'Carla Mendes Rocha',    'DEMO-CIFRADO-1', 'DEMO1A2B3C', NULL, 'carla@demo.local',    'ATIVO', 0),
  (2, 1, NULL, 'Fábio Antunes Prado',   'DEMO-CIFRADO-2', 'DEMO4D5E6F', NULL, 'fabio@demo.local',    'ATIVO', 0),
  (3, 1, NULL, 'Luciana Freitas Cunha', 'DEMO-CIFRADO-3', 'DEMO7G8H9I', NULL, 'luciana@demo.local',  'ATIVO', 0),
  (4, 1, NULL, 'Eduardo Sales Pinto',   'DEMO-CIFRADO-4', 'DEMOJ1K2L3', NULL, 'eduardo@demo.local',  'ATIVO', 0),
  (5, 2, NULL, 'Renata Okada Lima',     'DEMO-CIFRADO-5', 'DEMOM4N5O6', NULL, 'renata@demo.local',   'ATIVO', 0)
ON DUPLICATE KEY UPDATE id = id;

INSERT INTO aluno (id, escola_id, turma_id, nome, data_nascimento, matricula, documento_criptografado, documento_hmac, status, versao) VALUES
  (1, 1, 1, 'Davi Mendes Rocha',      '2016-03-10', 'DEMO-0001', 'DEMO-CIFRADO-A1', 'DEMOA1', 'ATIVO', 0),
  (2, 1, 1, 'Lara Antunes Prado',     '2016-07-22', 'DEMO-0002', 'DEMO-CIFRADO-A2', 'DEMOA2', 'ATIVO', 0),
  (3, 1, 2, 'Bruno Freitas Cunha',    '2014-01-15', 'DEMO-0003', 'DEMO-CIFRADO-A3', 'DEMOA3', 'ATIVO', 0),
  (4, 1, 2, 'Alice Sales Pinto',      '2014-11-02', 'DEMO-0004', 'DEMO-CIFRADO-A4', 'DEMOA4', 'ATIVO', 0),
  (5, 1, 1, 'Theo Mendes Rocha',      '2016-05-30', 'DEMO-0005', 'DEMO-CIFRADO-A5', 'DEMOA5', 'ATIVO', 0),
  (6, 1, 2, 'Manuela Antunes Prado',  '2014-09-18', 'DEMO-0006', 'DEMO-CIFRADO-A6', 'DEMOA6', 'ATIVO', 0),
  (7, 1, 1, 'Gael Freitas Cunha',     '2016-12-05', 'DEMO-0007', 'DEMO-CIFRADO-A7', 'DEMOA7', 'ATIVO', 0),
  (8, 2, 3, 'Yuri Okada Lima',        '2017-04-12', 'DEMO-0008', 'DEMO-CIFRADO-A8', 'DEMOA8', 'ATIVO', 0)
ON DUPLICATE KEY UPDATE id = id;

-- Vínculo familiar (origem do campo "Vínculo" no painel)
INSERT INTO vinculo_aluno_responsavel (id, escola_id, aluno_id, responsavel_id, tipo_relacao, principal, ativo, validado_em) VALUES
  (1, 1, 1, 1, 'Mãe', 1, 1, UTC_TIMESTAMP()),
  (2, 1, 2, 2, 'Pai', 1, 1, UTC_TIMESTAMP()),
  (3, 1, 3, 3, 'Mãe', 1, 1, UTC_TIMESTAMP()),
  (4, 1, 4, 4, 'Pai', 1, 1, UTC_TIMESTAMP()),
  (5, 1, 5, 1, 'Mãe', 1, 1, UTC_TIMESTAMP()),
  (6, 1, 6, 2, 'Pai', 1, 1, UTC_TIMESTAMP()),
  (7, 1, 7, 3, 'Mãe', 1, 1, UTC_TIMESTAMP()),
  (8, 2, 8, 5, 'Mãe', 1, 1, UTC_TIMESTAMP())
ON DUPLICATE KEY UPDATE id = id;

-- Autorizações de retirada (revalidadas na identificação e na saída).
-- Aluno 4 (Alice): autorização temporária EXPIRADA — o painel só permite rejeitar.
INSERT INTO autorizacao_retirada (id, escola_id, aluno_id, responsavel_id, tipo, status, data_inicio, data_fim, observacoes, versao) VALUES
  (1, 1, 1, 1, 'RESPONSAVEL_LEGAL',     'ATIVA', UTC_TIMESTAMP() - INTERVAL 200 DAY, UTC_TIMESTAMP() + INTERVAL 365 DAY, NULL, 0),
  (2, 1, 2, 2, 'RESPONSAVEL_LEGAL',     'ATIVA', UTC_TIMESTAMP() - INTERVAL 200 DAY, UTC_TIMESTAMP() + INTERVAL 365 DAY, NULL, 0),
  (3, 1, 3, 3, 'AUTORIZADO_TEMPORARIO', 'ATIVA', UTC_TIMESTAMP() - INTERVAL 1 DAY,   UTC_TIMESTAMP() + INTERVAL 30 DAY,  NULL, 0),
  (4, 1, 4, 4, 'AUTORIZADO_TEMPORARIO', 'ATIVA', UTC_TIMESTAMP() - INTERVAL 30 DAY,  UTC_TIMESTAMP() - INTERVAL 1 DAY,   'Autorização vencida (demonstração)', 0),
  (5, 1, 5, 1, 'RESPONSAVEL_LEGAL',     'ATIVA', UTC_TIMESTAMP() - INTERVAL 200 DAY, UTC_TIMESTAMP() + INTERVAL 365 DAY, NULL, 0),
  (6, 1, 6, 2, 'RESPONSAVEL_LEGAL',     'ATIVA', UTC_TIMESTAMP() - INTERVAL 200 DAY, UTC_TIMESTAMP() + INTERVAL 365 DAY, NULL, 0),
  (7, 1, 7, 3, 'AUTORIZADO_PERMANENTE', 'ATIVA', UTC_TIMESTAMP() - INTERVAL 200 DAY, UTC_TIMESTAMP() + INTERVAL 365 DAY, NULL, 0),
  (8, 2, 8, 5, 'RESPONSAVEL_LEGAL',     'ATIVA', UTC_TIMESTAMP() - INTERVAL 200 DAY, UTC_TIMESTAMP() + INTERVAL 365 DAY, NULL, 0)
ON DUPLICATE KEY UPDATE id = id;

-- Solicitações (escola 1: todos os estados; escola 2: uma solicitação que a escola 1 NÃO pode ver).
-- Horários relativos a UTC_TIMESTAMP() (o backend persiste em UTC).
INSERT INTO solicitacao_retirada (id, escola_id, aluno_id, responsavel_id, funcionario_id, status, ativa, motivo,
                                  justificativa_rejeicao, motivo_excecao, data_solicitacao, data_atendimento, data_conclusao, versao) VALUES
  (1, 1, 1, 1, NULL, 'AGUARDANDO',     1, 'Consulta médica',      NULL, NULL, UTC_TIMESTAMP() - INTERVAL 12 MINUTE, NULL, NULL, 0),
  (2, 1, 2, 2, NULL, 'AGUARDANDO',     1, 'Saída antecipada',     NULL, NULL, UTC_TIMESTAMP() - INTERVAL 7 MINUTE,  NULL, NULL, 0),
  (3, 1, 3, 3, NULL, 'AGUARDANDO',     1, 'Compromisso familiar', NULL, NULL, UTC_TIMESTAMP() - INTERVAL 3 MINUTE,  NULL, NULL, 0),
  (4, 1, 4, 4, 1,    'EM_ATENDIMENTO', 1, 'Consulta odontológica',NULL, NULL, UTC_TIMESTAMP() - INTERVAL 15 MINUTE, UTC_TIMESTAMP() - INTERVAL 2 MINUTE, NULL, 1),
  (5, 1, 5, 1, 1,    'IDENTIFICADO',   1, 'Saída antecipada',     NULL, NULL, UTC_TIMESTAMP() - INTERVAL 20 MINUTE, UTC_TIMESTAMP() - INTERVAL 5 MINUTE, NULL, 2),
  (6, 1, 6, 2, 1,    'CONCLUIDA',      0, 'Consulta médica',      NULL, NULL, UTC_TIMESTAMP() - INTERVAL 60 MINUTE, UTC_TIMESTAMP() - INTERVAL 50 MINUTE, UTC_TIMESTAMP() - INTERVAL 45 MINUTE, 3),
  (7, 1, 7, 3, 1,    'REJEITADA',      0, 'Saída antecipada',
      'Documento apresentado não corresponde ao responsável cadastrado.', 'IDENTIDADE_NAO_CONFERE',
      UTC_TIMESTAMP() - INTERVAL 40 MINUTE, UTC_TIMESTAMP() - INTERVAL 35 MINUTE, NULL, 2),
  (8, 2, 8, 5, NULL, 'AGUARDANDO',     1, 'Consulta médica',      NULL, NULL, UTC_TIMESTAMP() - INTERVAL 9 MINUTE,  NULL, NULL, 0)
ON DUPLICATE KEY UPDATE id = id;

INSERT INTO movimentacao_retirada (id, escola_id, solicitacao_retirada_id, status_anterior, status_novo, ator_tipo, usuario_id,
                                   responsavel_id, motivo, justificativa, data_ocorrencia, observacoes, versao) VALUES
  (1, 1, 6, 'IDENTIFICADO',   'CONCLUIDA',      'FUNCIONARIO', 1, NULL, 'RETIRADA_CONFIRMADA', NULL,
      UTC_TIMESTAMP() - INTERVAL 45 MINUTE, NULL, 0),
  (2, 1, 7, 'EM_ATENDIMENTO', 'REJEITADA',      'FUNCIONARIO', 1, NULL, 'IDENTIDADE_NAO_CONFERE',
      'Documento apresentado não corresponde ao responsável cadastrado.', UTC_TIMESTAMP() - INTERVAL 33 MINUTE, NULL, 0),
  (3, 1, 4, 'AGUARDANDO',     'EM_ATENDIMENTO', 'FUNCIONARIO', 1, NULL, 'INICIAR_ATENDIMENTO', NULL,
      UTC_TIMESTAMP() - INTERVAL 2 MINUTE, NULL, 0),
  (4, 1, 5, 'AGUARDANDO',     'EM_ATENDIMENTO', 'FUNCIONARIO', 1, NULL, 'INICIAR_ATENDIMENTO', NULL,
      UTC_TIMESTAMP() - INTERVAL 5 MINUTE, NULL, 0),
  (5, 1, 5, 'EM_ATENDIMENTO', 'IDENTIFICADO',   'FUNCIONARIO', 1, NULL, 'IDENTIFICACAO_DOCUMENTO', NULL,
      UTC_TIMESTAMP() - INTERVAL 3 MINUTE, 'Documento com foto conferido pelo funcionário (demonstração).', 0)
ON DUPLICATE KEY UPDATE id = id;
