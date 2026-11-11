#!/usr/bin/env bash
# Smoke test HTTP: autenticação JWT + endpoints da portaria contra backend real
B=${B:-http://localhost:8080}; O="Origin: http://localhost:5173"; J=$(mktemp); PASS=0; FAIL=0
chk(){ if [ "$2" = "$3" ]; then echo "  OK   $1 ($2)"; PASS=$((PASS+1)); else echo "  FALHA $1 (esperado $3, obtido $2)"; FAIL=$((FAIL+1)); fi; }
xsrf(){ awk '$6=="XSRF-TOKEN"{print $7}' $J | tail -1; }
code(){ curl -s -o /tmp/body -w "%{http_code}" "$@"; }
echo "[1] CSRF: obter cookie XSRF-TOKEN"; code -c $J -b $J -H "$O" $B/actuator/health >/dev/null; T=$(xsrf); [ -n "$T" ] && chk "cookie XSRF emitido" 1 1 || chk "cookie XSRF emitido" 0 1
echo "[2] login sem CSRF"; chk "login sem X-XSRF-TOKEN negado" $(code -H "$O" -H "Content-Type: application/json" -d '{"email":"portaria@demo.entradasegura.local","password":"Portaria@2026"}' $B/api/auth/login) 403
echo "[3] login senha errada"; chk "credencial inválida" $(code -c $J -b $J -H "$O" -H "X-XSRF-TOKEN: $(xsrf)" -H "Content-Type: application/json" -d '{"email":"portaria@demo.entradasegura.local","password":"errada"}' $B/api/auth/login) 401
echo "[4] login funcionário"; chk "login" $(code -c $J -b $J -H "$O" -H "X-XSRF-TOKEN: $(xsrf)" -H "Content-Type: application/json" -d '{"email":"portaria@demo.entradasegura.local","password":"Portaria@2026"}' $B/api/auth/login) 200
TOK=$(sed -E 's/.*"accessToken":"([^"]+)".*/\1/' /tmp/body); echo "  resposta: $(sed -E 's/"accessToken":"[^"]{20}[^"]*"/"accessToken":"<jwt>"/' /tmp/body)"
grep -q refresh_token $J && chk "cookie refresh_token (HttpOnly)" 1 1 || chk "cookie refresh_token" 0 1
A="Authorization: Bearer $TOK"
echo "[5] endpoints da portaria com JWT"
chk "GET /sessao" $(code -H "$O" -H "$A" $B/api/portaria/sessao) 200; echo "    $(cat /tmp/body)"
chk "GET /retiradas" $(code -H "$O" -H "$A" "$B/api/portaria/retiradas?status=AGUARDANDO,EM_ATENDIMENTO,IDENTIFICADO") 200; echo "    ids: $(grep -o '"protocolo":"[^"]*"' /tmp/body | tr '\n' ' ')"
chk "GET /retiradas?busca=lara" $(code -H "$A" "$B/api/portaria/retiradas?busca=lara") 200; echo "    $(grep -o '"nome":"[^"]*"' /tmp/body | head -1)"
chk "GET /retiradas/1" $(code -H "$A" $B/api/portaria/retiradas/1) 200; echo "    $(head -c 300 /tmp/body)…"
chk "GET /resumo" $(code -H "$A" $B/api/portaria/resumo) 200; echo "    $(cat /tmp/body)"
chk "GET /movimentacoes" $(code -H "$A" "$B/api/portaria/movimentacoes?limite=10") 200; echo "    $(cat /tmp/body)"
echo "[6] isolamento escolar"
chk "retirada 8 (escola 2) invisível p/ escola 1" $(code -H "$A" $B/api/portaria/retiradas/8) 404
echo "[7] sessão inválida"
chk "sem token" $(code $B/api/portaria/sessao) 401
chk "token adulterado" $(code -H "Authorization: Bearer ${TOK%?????}xxxxx" $B/api/portaria/sessao) 401
EXP="eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJwb3J0YXJpYUBkZW1vLmVudHJhZGFzZWd1cmEubG9jYWwiLCJleHAiOjE1MDAwMDAwMDB9.x"
chk "token expirado/forjado" $(code -H "Authorization: Bearer $EXP" $B/api/portaria/sessao) 401
echo "[8] refresh via cookie"
chk "refresh sem CSRF negado" $(code -b $J -H "$O" -X POST $B/api/auth/refresh) 403
chk "refresh com cookie + CSRF" $(code -c $J -b $J -H "$O" -H "X-XSRF-TOKEN: $(xsrf)" -X POST $B/api/auth/refresh) 200
TOK2=$(sed -E 's/.*"accessToken":"([^"]+)".*/\1/' /tmp/body)
chk "novo access token funciona" $(code -H "Authorization: Bearer $TOK2" $B/api/portaria/sessao) 200
echo "[9] logout revoga refresh"
cp $J $J.old
chk "logout" $(code -c $J -b $J -H "$O" -H "X-XSRF-TOKEN: $(xsrf)" -X POST $B/api/auth/logout) 200
chk "refresh com token revogado" $(code -b $J.old -H "$O" -H "X-XSRF-TOKEN: $(xsrf)" -X POST $B/api/auth/refresh) 401
echo "[10] perfil GESTOR no painel"
code -c $J -b $J -H "$O" -H "X-XSRF-TOKEN: $(xsrf)" -H "Content-Type: application/json" -d '{"email":"gestor@demo.entradasegura.local","password":"Portaria@2026"}' $B/api/auth/login >/dev/null
TG=$(sed -E 's/.*"accessToken":"([^"]+)".*/\1/' /tmp/body)
chk "gestor -> /sessao" $(code -H "Authorization: Bearer $TG" $B/api/portaria/sessao) 403
echo "[11] escola 2 vê apenas a própria fila"
code -c $J -b $J -H "$O" -H "X-XSRF-TOKEN: $(xsrf)" -H "Content-Type: application/json" -d '{"email":"portaria.escola2@demo.entradasegura.local","password":"Portaria@2026"}' $B/api/auth/login >/dev/null
T2=$(sed -E 's/.*"accessToken":"([^"]+)".*/\1/' /tmp/body)
code -H "Authorization: Bearer $T2" "$B/api/portaria/retiradas" >/dev/null; chk "escola 2: qtd retiradas" $(grep -o '"protocolo"' /tmp/body | wc -l | tr -d ' ') 1
echo "[12] fluxo operacional completo (dados reais no MySQL)"
# Cria uma solicitação fictícia nova a cada execução (scripts/fixture-retirada.sql); nada é apagado.
DIR=$(cd "$(dirname "$0")" && pwd)
fixture(){ (echo "SET @nome='$1', @vencida=$2;"; cat "$DIR/fixture-retirada.sql") | docker exec -i "${MYSQL_CONTAINER:-entradasegura-mysql}" mysql --default-character-set=utf8mb4 -N -uentradasegura -pentradasegura entradasegura 2>/dev/null | tail -1; }
campo(){ grep -oE "\"$1\":(\"[^\"]*\"|[0-9]+)" /tmp/body | head -1 | sed -E 's/^"[^"]+"://; s/"//g'; }
login_func(){ code -c $J -b $J -H "$O" -H "X-XSRF-TOKEN: $(xsrf)" -H "Content-Type: application/json" -d "{\"email\":\"$1\",\"password\":\"Portaria@2026\"}" $B/api/auth/login >/dev/null; sed -E 's/.*"accessToken":"([^"]+)".*/\1/' /tmp/body; }
# Corpo via stdin: no Windows, argumentos do curl.exe perdem o UTF-8.
acao(){ printf "%s" "$4" | code -b $J -H "$O" -H "X-XSRF-TOKEN: $(xsrf)" -H "Authorization: Bearer $1" -H "Content-Type: application/json; charset=utf-8" --data-binary @- $B/api/portaria/retiradas/$2/$3; }
TF=$(login_func portaria@demo.entradasegura.local)
NOME="Aluno Smoke $(date +%H%M%S)"; ID=$(fixture "$NOME" 0); echo "  solicitação criada: P-$ID ($NOME)"
code -H "Authorization: Bearer $TF" $B/api/portaria/retiradas/$ID >/dev/null; chk "nova solicitação oferece INICIAR_ATENDIMENTO" "$(grep -o '"acoesPermitidas":\[[^]]*\]' /tmp/body)" '"acoesPermitidas":["INICIAR_ATENDIMENTO"]'
chk "iniciar-atendimento" $(acao $TF $ID iniciar-atendimento '{"versao":0}') 200; chk "  status" "$(campo status)" EM_ATENDIMENTO; V=$(campo versao)
chk "repetir com versão antiga" $(acao $TF $ID iniciar-atendimento '{"versao":0}') 409; chk "  codigo" "$(campo codigo)" VERSAO_DESATUALIZADA
chk "confirmar-retirada antes da identificação" $(acao $TF $ID confirmar-retirada "{\"versao\":$V}") 409; chk "  codigo" "$(campo codigo)" TRANSICAO_INVALIDA
chk "identificação por QR (não simulada)" $(acao $TF $ID confirmar-identificacao "{\"metodo\":\"QR_CODE\",\"versao\":$V}") 422; chk "  codigo" "$(campo codigo)" QR_NAO_VALIDADO
chk "confirmar-identificacao (documento)" $(acao $TF $ID confirmar-identificacao "{\"metodo\":\"DOCUMENTO\",\"versao\":$V}") 200; chk "  status" "$(campo status)" IDENTIFICADO; V=$(campo versao)
chk "confirmar-retirada" $(acao $TF $ID confirmar-retirada "{\"versao\":$V}") 200; chk "  status" "$(campo status)" CONCLUIDA
code -H "Authorization: Bearer $TF" "$B/api/portaria/movimentacoes?limite=10" >/dev/null
grep -q "\"alunoNome\":\"$NOME\"[^}]*\"resultado\":\"CONCLUIDA\"" /tmp/body && chk "histórico registra a retirada" 1 1 || chk "histórico registra a retirada" 0 1
echo "[13] regras de negócio nas ações"
ID2=$(fixture "Aluno Smoke Isolamento $(date +%H%M%S)" 0)
chk "funcionário da escola 2 opera P-$ID2 (escola 1)" $(acao $T2 $ID2 iniciar-atendimento '{"versao":0}') 404
TGF=$(login_func gestor@demo.entradasegura.local)
chk "perfil GESTOR inicia atendimento" $(acao $TGF $ID2 iniciar-atendimento '{"versao":0}') 403
chk "sem token CSRF" $(code -H "Authorization: Bearer $TF" -H "Content-Type: application/json" -d '{"versao":0}' $B/api/portaria/retiradas/$ID2/iniciar-atendimento) 403
ID3=$(fixture "Aluno Smoke Vencida $(date +%H%M%S)" 1); acao $TF $ID3 iniciar-atendimento '{"versao":0}' >/dev/null; V=$(campo versao)
chk "autorização vencida só permite rejeitar" "$(grep -o '"acoesPermitidas":\[[^]]*\]' /tmp/body)" '"acoesPermitidas":["REJEITAR"]'
chk "identificar com autorização vencida" $(acao $TF $ID3 confirmar-identificacao "{\"metodo\":\"DOCUMENTO\",\"versao\":$V}") 409; chk "  codigo" "$(campo codigo)" AUTORIZACAO_EXPIRADA
chk "rejeitar sem justificativa" $(acao $TF $ID3 rejeitar "{\"motivo\":\"AUTORIZACAO_EXPIRADA\",\"justificativa\":\"\",\"versao\":$V}") 422; chk "  codigo" "$(campo codigo)" JUSTIFICATIVA_OBRIGATORIA
chk "rejeitar com justificativa" $(acao $TF $ID3 rejeitar "{\"motivo\":\"AUTORIZACAO_EXPIRADA\",\"justificativa\":\"Autorização vencida ontem.\",\"versao\":$V}") 200; chk "  status" "$(campo status)" REJEITADA
echo "[14] segunda retirada do mesmo aluno após a primeira concluída"
sql(){ echo "$1" | docker exec -i "${MYSQL_CONTAINER:-entradasegura-mysql}" mysql --default-character-set=utf8mb4 -N -uentradasegura -pentradasegura entradasegura 2>/dev/null | tail -1; }
ALUNO=$(sql "SELECT aluno_id FROM solicitacao_retirada WHERE id = $ID;")
ID4=$(sql "INSERT INTO solicitacao_retirada (escola_id, aluno_id, responsavel_id, status, ativa, motivo, data_solicitacao, versao) VALUES (1, $ALUNO, 1, 'AGUARDANDO', 1, 'Segunda retirada', UTC_TIMESTAMP(), 0); SELECT LAST_INSERT_ID();")
echo "  nova solicitação P-$ID4 para o mesmo aluno de P-$ID"
chk "iniciar (2ª)" $(acao $TF $ID4 iniciar-atendimento '{"versao":0}') 200; V=$(campo versao)
chk "identificar (2ª)" $(acao $TF $ID4 confirmar-identificacao "{\"metodo\":\"DOCUMENTO\",\"versao\":$V}") 200; V=$(campo versao)
chk "confirmar saída (2ª)" $(acao $TF $ID4 confirmar-retirada "{\"versao\":$V}") 200; chk "  status" "$(campo status)" CONCLUIDA
chk "aluno com 2 retiradas concluídas" "$(sql "SELECT COUNT(*) FROM solicitacao_retirada WHERE aluno_id = $ALUNO AND status = 'CONCLUIDA';")" 2
echo "[15] HTTP Basic não autentica (somente JWT)"
chk "Basic com senha correta" $(code -u "portaria@demo.entradasegura.local:Portaria@2026" $B/api/portaria/sessao) 401
echo "RESULTADO: $PASS OK, $FAIL falhas"; rm -f $J $J.old
