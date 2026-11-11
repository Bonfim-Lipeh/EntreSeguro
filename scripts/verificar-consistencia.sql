-- Verificação de consistência dos dados da portaria (somente leitura).
-- Cada linha mostra uma regra e a quantidade de violações; o esperado é 0 em todas.
-- Uso: docker exec -i entradasegura-mysql mysql -uentradasegura -pentradasegura entradasegura < scripts/verificar-consistencia.sql

SELECT '1. ativa coerente com o status (ativa=1 só em AGUARDANDO/EM_ATENDIMENTO/IDENTIFICADO)' AS regra, COUNT(*) AS violacoes
FROM solicitacao_retirada
WHERE (ativa = 1 AND status IN ('CONCLUIDA', 'REJEITADA', 'CANCELADA'))
   OR (ativa = 0 AND status NOT IN ('CONCLUIDA', 'REJEITADA', 'CANCELADA'))
UNION ALL
SELECT '2. no máximo uma solicitação ativa por aluno', COUNT(*)
FROM (SELECT aluno_id FROM solicitacao_retirada WHERE ativa = 1 GROUP BY escola_id, aluno_id HAVING COUNT(*) > 1) d
UNION ALL
SELECT '3. solicitação encerrada tem movimentação final', COUNT(*)
FROM solicitacao_retirada s
WHERE s.status IN ('CONCLUIDA', 'REJEITADA', 'CANCELADA')
  AND NOT EXISTS (SELECT 1 FROM movimentacao_retirada m WHERE m.solicitacao_retirada_id = s.id AND m.status_novo = s.status)
UNION ALL
SELECT '4. status atual = status_novo da última movimentação', COUNT(*)
FROM solicitacao_retirada s
JOIN movimentacao_retirada m ON m.id = (SELECT MAX(id) FROM movimentacao_retirada x WHERE x.solicitacao_retirada_id = s.id)
WHERE m.status_novo <> s.status
UNION ALL
SELECT '5. cadeia de movimentações contínua (anterior = novo da movimentação prévia)', COUNT(*)
FROM (SELECT status_anterior,
             LAG(status_novo) OVER (PARTITION BY solicitacao_retirada_id ORDER BY id) AS novo_previo
      FROM movimentacao_retirada) c
WHERE c.novo_previo IS NOT NULL AND c.novo_previo <> c.status_anterior
UNION ALL
SELECT '6. movimentação e solicitação da mesma escola', COUNT(*)
FROM movimentacao_retirada m JOIN solicitacao_retirada s ON s.id = m.solicitacao_retirada_id
WHERE m.escola_id <> s.escola_id
UNION ALL
SELECT '7. funcionário da movimentação pertence à escola', COUNT(*)
FROM movimentacao_retirada m JOIN usuario u ON u.id = m.usuario_id
WHERE u.escola_id <> m.escola_id
UNION ALL
SELECT '8. funcionário da solicitação pertence à escola', COUNT(*)
FROM solicitacao_retirada s JOIN usuario u ON u.id = s.funcionario_id
WHERE u.escola_id <> s.escola_id
UNION ALL
-- Movimentações do seed (ids 1-5) foram inseridas diretamente, sem auditoria.
SELECT '9. cada movimentação gerada pela aplicação tem auditoria correspondente', COUNT(*)
FROM (SELECT m.solicitacao_retirada_id AS sid, COUNT(*) AS movs
      FROM movimentacao_retirada m WHERE m.id > 5 GROUP BY m.solicitacao_retirada_id) mv
LEFT JOIN (SELECT entidade_id AS sid, COUNT(*) AS auds
           FROM auditoria_registro WHERE entidade = 'SolicitacaoRetirada' GROUP BY entidade_id) au ON au.sid = mv.sid
WHERE COALESCE(au.auds, 0) <> mv.movs
UNION ALL
SELECT '10. rejeição tem justificativa e motivo', COUNT(*)
FROM solicitacao_retirada
WHERE status = 'REJEITADA' AND (justificativa_rejeicao IS NULL OR TRIM(justificativa_rejeicao) = '' OR motivo_excecao IS NULL)
UNION ALL
SELECT '11. conclusão registra funcionário e horário', COUNT(*)
FROM solicitacao_retirada
WHERE status = 'CONCLUIDA' AND (funcionario_id IS NULL OR data_conclusao IS NULL);
