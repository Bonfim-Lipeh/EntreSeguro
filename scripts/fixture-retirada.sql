-- Cria UMA solicitação AGUARDANDO fictícia na escola de demonstração 1 (perfil dev), para testes
-- de ponta a ponta repetíveis. Não altera nem apaga registros existentes.
-- Antes deste arquivo, defina:  SET @nome = 'Aluno E2E 123', @vencida = 0;   (@vencida = 1 → autorização expirada)
-- Uso: (echo "SET @nome='Aluno X', @vencida=0;"; cat scripts/fixture-retirada.sql) |
--        docker exec -i entradasegura-mysql mysql --default-character-set=utf8mb4 -uentradasegura -pentradasegura entradasegura

INSERT INTO aluno (escola_id, turma_id, nome, data_nascimento, matricula, documento_criptografado, documento_hmac, status, versao)
VALUES (1, 1, @nome, '2016-06-01', CONCAT('E2E-', UUID_SHORT()), 'E2E', 'E2E', 'ATIVO', 0);
SET @aluno = LAST_INSERT_ID();

INSERT INTO vinculo_aluno_responsavel (escola_id, aluno_id, responsavel_id, tipo_relacao, principal, ativo, validado_em)
VALUES (1, @aluno, 1, 'Mãe', 1, 1, UTC_TIMESTAMP());

INSERT INTO autorizacao_retirada (escola_id, aluno_id, responsavel_id, tipo, status, data_inicio, data_fim, versao)
VALUES (1, @aluno, 1, 'RESPONSAVEL_LEGAL', 'ATIVA', UTC_TIMESTAMP() - INTERVAL 30 DAY,
        IF(@vencida = 1, UTC_TIMESTAMP() - INTERVAL 1 DAY, UTC_TIMESTAMP() + INTERVAL 30 DAY), 0);

INSERT INTO solicitacao_retirada (escola_id, aluno_id, responsavel_id, status, ativa, motivo, data_solicitacao, versao)
VALUES (1, @aluno, 1, 'AGUARDANDO', 1, 'Teste de ponta a ponta', UTC_TIMESTAMP() - INTERVAL 1 MINUTE, 0);

SELECT LAST_INSERT_ID() AS solicitacao_id;
