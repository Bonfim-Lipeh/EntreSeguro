DROP TRIGGER IF EXISTS trg_auditoria_registro_block_update;
DROP TRIGGER IF EXISTS trg_auditoria_registro_block_delete;

CREATE TRIGGER trg_auditoria_registro_block_update
BEFORE UPDATE ON auditoria_registro
FOR EACH ROW
BEGIN
    SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Atualização de auditoria_registro não permitida';
END;

CREATE TRIGGER trg_auditoria_registro_block_delete
BEFORE DELETE ON auditoria_registro
FOR EACH ROW
BEGIN
    SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Exclusão de auditoria_registro não permitida';
END;
