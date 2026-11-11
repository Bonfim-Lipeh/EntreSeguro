-- Regra: no máximo UMA solicitação ATIVA por aluno e escola.
-- A chave da V1 (aluno_id, escola_id, ativa) também impedia DUAS solicitações encerradas (ativa = 0)
-- para o mesmo aluno, bloqueando a conclusão de uma segunda retirada. A coluna gerada vale 1 quando
-- ativa e NULL quando encerrada; o MySQL permite vários NULL em índice único.
ALTER TABLE solicitacao_retirada
    ADD COLUMN ativa_unica TINYINT GENERATED ALWAYS AS (IF(ativa = 1, 1, NULL)) STORED;

ALTER TABLE solicitacao_retirada
    ADD UNIQUE KEY uk_solicitacao_ativa_unica (aluno_id, escola_id, ativa_unica);

ALTER TABLE solicitacao_retirada
    DROP INDEX uk_solicitacao_ativa_por_aluno_escola;
