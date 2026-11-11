package com.entradasegura.portaria;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Fluxo operacional da portaria contra MySQL real (Testcontainers), com JWT, CSRF e
 * a cadeia de segurança completa. Cobre transições válidas e inválidas, isolamento
 * escolar, perfil, autorização, justificativa, concorrência, falha transacional,
 * histórico e auditoria.
 */
@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
class PortariaOperacaoIntegrationTest {

    private static final String SENHA = "Senha@123";
    private static final String BASE = "/api/portaria/retiradas/";

    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.4")
            .withDatabaseName("entradasegura_operacao_test")
            .withUsername("testuser")
            .withPassword("testpass")
            .withCommand("--log-bin-trust-function-creators=1");

    @DynamicPropertySource
    static void propriedades(DynamicPropertyRegistry registry) {
        mysql.start();
        Flyway.configure()
                .dataSource(mysql.getJdbcUrl(), mysql.getUsername(), mysql.getPassword())
                .locations("classpath:db/migration")
                .load()
                .migrate();
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
        registry.add("spring.flyway.enabled", () -> "false");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        registry.add("entradasegura.jwt.secret", () -> "TestSecretKeyForJWT_1234567890ABCD");
        registry.add("entradasegura.jwt.issuer", () -> "entradasegura-test");
        registry.add("entradasegura.jwt.audience", () -> "entradasegura-portaria-test");
    }

    @Autowired
    private WebApplicationContext context;
    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private PasswordEncoder passwordEncoder;

    private final ObjectMapper json = new ObjectMapper();
    private MockMvc mvc;

    private long escolaA;
    private long escolaB;
    private long turmaA;
    private long funcionarioA;
    private String tokenA;

    @BeforeEach
    void preparar() throws Exception {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        limparBase();

        escolaA = inserir("escola", Map.of("nome", "Escola A", "cnpj", "10.000.000/0001-01", "razao_social", "Escola A Ltda",
                "timezone", "America/Sao_Paulo", "status", "ATIVA", "versao", 0));
        escolaB = inserir("escola", Map.of("nome", "Escola B", "cnpj", "20.000.000/0001-02", "razao_social", "Escola B Ltda",
                "timezone", "America/Sao_Paulo", "status", "ATIVA", "versao", 0));
        turmaA = inserir("turma", Map.of("escola_id", escolaA, "nome", "5A", "nivel", "FUNDAMENTAL_I", "turno", "MANHA",
                "ano_letivo", 2026, "status", "ATIVA", "versao", 0));
        funcionarioA = usuario(escolaA, "func.a@escola.test", "FUNCIONARIO");
        usuario(escolaB, "func.b@escola.test", "FUNCIONARIO");
        usuario(escolaA, "gestor.a@escola.test", "GESTOR");
        tokenA = login("func.a@escola.test");
    }

    @AfterEach
    void removerGatilhoDeFalha() {
        jdbc.execute("DROP TRIGGER IF EXISTS trg_teste_falha_auditoria");
    }

    // ------------------------------------------------------------------ fluxo válido

    @Test
    void fluxoCompletoRegistraMovimentacoesAuditoriaHistoricoEResumo() throws Exception {
        long id = solicitacao("AGUARDANDO", autorizacaoVigente());

        JsonNode r = sucesso(acao(tokenA, id, "iniciar-atendimento", Map.of("versao", 0)));
        assertThat(r.get("status").asText()).isEqualTo("EM_ATENDIMENTO");
        assertThat(r.get("atendidoPor").get("id").asLong()).isEqualTo(funcionarioA);
        assertThat(r.get("acoesPermitidas").toString()).contains("CONFIRMAR_IDENTIFICACAO", "REJEITAR").doesNotContain("VALIDAR_IDENTIFICACAO");

        r = sucesso(acao(tokenA, id, "confirmar-identificacao", Map.of("metodo", "DOCUMENTO", "versao", r.get("versao").asLong())));
        assertThat(r.get("status").asText()).isEqualTo("IDENTIFICADO");
        assertThat(r.get("metodoIdentificacao").asText()).isEqualTo("DOCUMENTO");
        assertThat(r.get("dtIdentificacao").isNull()).isFalse();
        assertThat(r.get("acoesPermitidas").toString()).isEqualTo("[\"CONFIRMAR_RETIRADA\"]");

        r = sucesso(acao(tokenA, id, "confirmar-retirada", Map.of("versao", r.get("versao").asLong())));
        assertThat(r.get("status").asText()).isEqualTo("CONCLUIDA");
        assertThat(r.get("acoesPermitidas").size()).isZero();
        assertThat(r.get("dtConclusao").isNull()).isFalse();

        // Movimentações: funcionário autenticado, sequência de estados e resultado da conferência
        List<Map<String, Object>> movs = jdbc.queryForList(
                "SELECT status_anterior, status_novo, usuario_id, motivo, observacoes FROM movimentacao_retirada WHERE solicitacao_retirada_id = ? ORDER BY id", id);
        assertThat(movs).extracting(m -> m.get("status_novo")).containsExactly("EM_ATENDIMENTO", "IDENTIFICADO", "CONCLUIDA");
        assertThat(movs).extracting(m -> ((Number) m.get("usuario_id")).longValue()).containsOnly(funcionarioA);
        assertThat(movs.get(1).get("motivo")).isEqualTo("IDENTIFICACAO_DOCUMENTO");
        assertThat((String) movs.get(1).get("observacoes")).contains("Documento com foto conferido", "vigente até");

        // Auditoria imutável na mesma transação
        List<String> acoes = jdbc.queryForList(
                "SELECT acao FROM auditoria_registro WHERE entidade_id = ? AND usuario_id = ? ORDER BY id", String.class, id, funcionarioA);
        assertThat(acoes).containsExactly("INICIAR_ATENDIMENTO", "CONFIRMAR_IDENTIFICACAO", "CONFIRMAR_RETIRADA");

        // Histórico e resumo do dia
        mvc.perform(get("/api/portaria/movimentacoes").header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].retiradaId").value(id))
                .andExpect(jsonPath("$[0].alunoNome").value("Aluno " + id))
                .andExpect(jsonPath("$[0].responsavelNome").value("Responsável Teste"))
                .andExpect(jsonPath("$[0].resultado").value("CONCLUIDA"));
        mvc.perform(get("/api/portaria/resumo").header("Authorization", "Bearer " + tokenA))
                .andExpect(jsonPath("$.concluidasHoje").value(1));
    }

    @Test
    void rejeicaoComJustificativaEncerraSemSaida() throws Exception {
        long id = solicitacao("EM_ATENDIMENTO", autorizacaoVigente());
        JsonNode r = sucesso(acao(tokenA, id, "rejeitar",
                Map.of("motivo", "IDENTIDADE_NAO_CONFERE", "justificativa", "Documento de terceiro apresentado.", "versao", 0)));
        assertThat(r.get("status").asText()).isEqualTo("REJEITADA");
        assertThat(r.get("motivoRejeicao").asText()).isEqualTo("IDENTIDADE_NAO_CONFERE");
        assertThat(jdbc.queryForObject("SELECT justificativa FROM movimentacao_retirada WHERE solicitacao_retirada_id = ?", String.class, id))
                .isEqualTo("Documento de terceiro apresentado.");
    }

    // ------------------------------------------------------------- transições inválidas

    @Test
    void transicaoInvalidaRetorna409ENaoGravaNada() throws Exception {
        long id = solicitacao("AGUARDANDO", autorizacaoVigente());
        erro(acao(tokenA, id, "confirmar-retirada", Map.of("versao", 0)), 409, "TRANSICAO_INVALIDA");
        erro(acao(tokenA, id, "confirmar-identificacao", Map.of("metodo", "DOCUMENTO", "versao", 0)), 409, "TRANSICAO_INVALIDA");
        erro(acao(tokenA, id, "rejeitar", Map.of("motivo", "OUTRO", "justificativa", "Justificativa suficiente.", "versao", 0)), 409, "TRANSICAO_INVALIDA");
        assertSemAlteracao(id, "AGUARDANDO");
    }

    @Test
    void estadoFinalNaoPodeSerAlterado() throws Exception {
        long id = solicitacao("CONCLUIDA", autorizacaoVigente());
        erro(acao(tokenA, id, "iniciar-atendimento", Map.of("versao", 0)), 409, "TRANSICAO_INVALIDA");
        assertSemAlteracao(id, "CONCLUIDA");
    }

    @Test
    void versaoDesatualizadaRetorna409() throws Exception {
        long id = solicitacao("AGUARDANDO", autorizacaoVigente());
        erro(acao(tokenA, id, "iniciar-atendimento", Map.of("versao", 7)), 409, "VERSAO_DESATUALIZADA");
        assertSemAlteracao(id, "AGUARDANDO");
    }

    // ------------------------------------------------------ isolamento, perfil e segurança

    @Test
    void funcionarioDeOutraEscolaNaoOperaENaoEnxergaASolicitacao() throws Exception {
        long id = solicitacao("AGUARDANDO", autorizacaoVigente());
        String tokenB = login("func.b@escola.test");
        erro(acao(tokenB, id, "iniciar-atendimento", Map.of("versao", 0)), 404, "RETIRADA_NAO_ENCONTRADA");
        assertSemAlteracao(id, "AGUARDANDO");
    }

    @Test
    void perfilSemPermissaoRecebe403() throws Exception {
        long id = solicitacao("AGUARDANDO", autorizacaoVigente());
        String tokenGestor = login("gestor.a@escola.test");
        acao(tokenGestor, id, "iniciar-atendimento", Map.of("versao", 0)).andExpect(status().isForbidden());
        assertSemAlteracao(id, "AGUARDANDO");
    }

    @Test
    void semCsrfOuSemTokenAOperacaoENegada() throws Exception {
        long id = solicitacao("AGUARDANDO", autorizacaoVigente());
        mvc.perform(post(BASE + id + "/iniciar-atendimento").header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"versao\":0}"))
                .andExpect(status().isForbidden());
        mvc.perform(post(BASE + id + "/iniciar-atendimento").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"versao\":0}"))
                .andExpect(status().isUnauthorized());
        assertSemAlteracao(id, "AGUARDANDO");
    }

    // ----------------------------------------------------------------------- autorização

    @Test
    void autorizacaoExpiradaBloqueiaIdentificacaoEOfereceSomenteRejeicao() throws Exception {
        long id = solicitacao("EM_ATENDIMENTO", autorizacao("ATIVA", -30, -1));
        JsonNode r = sucesso(mvc.perform(get(BASE + id).header("Authorization", "Bearer " + tokenA)));
        assertThat(r.get("acoesPermitidas").toString()).isEqualTo("[\"REJEITAR\"]");
        assertThat(r.get("alertas").toString()).contains("critico");

        erro(acao(tokenA, id, "confirmar-identificacao", Map.of("metodo", "DOCUMENTO", "versao", 0)), 409, "AUTORIZACAO_EXPIRADA");
        assertSemAlteracao(id, "EM_ATENDIMENTO");
    }

    @Test
    void autorizacaoRevogadaBloqueiaIdentificacao() throws Exception {
        long id = solicitacao("EM_ATENDIMENTO", autorizacao("REVOGADA", -30, 30));
        erro(acao(tokenA, id, "confirmar-identificacao", Map.of("metodo", "DOCUMENTO", "versao", 0)), 409, "AUTORIZACAO_EXPIRADA");
        assertSemAlteracao(id, "EM_ATENDIMENTO");
    }

    @Test
    void autorizacaoQueExpiraAposIdentificacaoBloqueiaSaidaEPermiteRejeicaoExcepcional() throws Exception {
        long autorizacao = autorizacaoVigente();
        long id = solicitacao("EM_ATENDIMENTO", autorizacao);
        JsonNode r = sucesso(acao(tokenA, id, "confirmar-identificacao", Map.of("metodo", "DOCUMENTO", "versao", 0)));

        // Com autorização vigente, a exceção de rejeição a partir de IDENTIFICADO não se aplica
        erro(acao(tokenA, id, "rejeitar", Map.of("motivo", "AUTORIZACAO_EXPIRADA", "justificativa", "Tentativa indevida.", "versao", r.get("versao").asLong())),
                409, "TRANSICAO_INVALIDA");

        // A autorização expira depois da identificação: a saída é revalidada e recusada
        jdbc.update("UPDATE autorizacao_retirada SET data_fim = ? WHERE id = ?", utc(Instant.now().minus(1, ChronoUnit.MINUTES)), autorizacao);
        erro(acao(tokenA, id, "confirmar-retirada", Map.of("versao", r.get("versao").asLong())), 409, "AUTORIZACAO_EXPIRADA");
        // Só o motivo AUTORIZACAO_EXPIRADA é aceito a partir de IDENTIFICADO
        erro(acao(tokenA, id, "rejeitar", Map.of("motivo", "OUTRO", "justificativa", "Motivo não permitido aqui.", "versao", r.get("versao").asLong())),
                409, "TRANSICAO_INVALIDA");

        r = sucesso(acao(tokenA, id, "rejeitar",
                Map.of("motivo", "AUTORIZACAO_EXPIRADA", "justificativa", "Autorização venceu antes da saída.", "versao", r.get("versao").asLong())));
        assertThat(r.get("status").asText()).isEqualTo("REJEITADA");
        assertThat(jdbc.queryForObject(
                "SELECT status_anterior FROM movimentacao_retirada WHERE solicitacao_retirada_id = ? AND status_novo = 'REJEITADA'", String.class, id))
                .isEqualTo("IDENTIFICADO");
    }

    // ------------------------------------------------------------------------ validação

    @Test
    void justificativaAusenteOuCurtaRetorna422() throws Exception {
        long id = solicitacao("EM_ATENDIMENTO", autorizacaoVigente());
        erro(acao(tokenA, id, "rejeitar", Map.of("motivo", "OUTRO", "versao", 0)), 422, "JUSTIFICATIVA_OBRIGATORIA");
        erro(acao(tokenA, id, "rejeitar", Map.of("motivo", "OUTRO", "justificativa", "   curta  ", "versao", 0)), 422, "JUSTIFICATIVA_OBRIGATORIA");
        erro(acao(tokenA, id, "rejeitar", Map.of("motivo", "INEXISTENTE", "justificativa", "Justificativa suficiente.", "versao", 0)), 422, "MOTIVO_INVALIDO");
        assertSemAlteracao(id, "EM_ATENDIMENTO");
    }

    @Test
    void identificacaoPorQrCodeNaoESimulada() throws Exception {
        long id = solicitacao("EM_ATENDIMENTO", autorizacaoVigente());
        erro(acao(tokenA, id, "confirmar-identificacao", Map.of("metodo", "QR_CODE", "versao", 0)), 422, "QR_NAO_VALIDADO");
        assertSemAlteracao(id, "EM_ATENDIMENTO");
    }

    // --------------------------------------------------------- concorrência e transação

    @Test
    void operacoesSimultaneasNaMesmaVersaoApenasUmaVence() throws Exception {
        long id = solicitacao("AGUARDANDO", autorizacaoVigente());
        CyclicBarrier largada = new CyclicBarrier(2);
        Callable<Integer> tentativa = () -> {
            largada.await();
            return acao(tokenA, id, "iniciar-atendimento", Map.of("versao", 0)).andReturn().getResponse().getStatus();
        };
        ExecutorService pool = Executors.newFixedThreadPool(2);
        List<Future<Integer>> resultados = pool.invokeAll(List.of(tentativa, tentativa));
        pool.shutdown();
        List<Integer> status = new ArrayList<>();
        for (Future<Integer> f : resultados) status.add(f.get());

        assertThat(status).containsExactlyInAnyOrder(200, 409);
        assertThat(contar("movimentacao_retirada", id)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM auditoria_registro WHERE entidade_id = ?", Integer.class, id)).isEqualTo(1);
    }

    @Test
    void falhaAoGravarAuditoriaDesfazStatusEMovimentacao() throws Exception {
        long id = solicitacao("IDENTIFICADO", autorizacaoVigente());
        jdbc.execute("CREATE TRIGGER trg_teste_falha_auditoria BEFORE INSERT ON auditoria_registro FOR EACH ROW "
                + "BEGIN IF NEW.entidade_id = " + id + " THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'falha simulada'; END IF; END");

        int statusHttp;
        try {
            statusHttp = acao(tokenA, id, "confirmar-retirada", Map.of("versao", 0)).andReturn().getResponse().getStatus();
        } catch (Exception e) {
            statusHttp = 500; // MockMvc relança exceções não tratadas; no servidor real viram 500
        }

        assertThat(statusHttp).isGreaterThanOrEqualTo(500);
        assertSemAlteracao(id, "IDENTIFICADO");
    }

    // ----------------------------------------------------------- resumo, histórico e schema

    @Test
    void resumoEHistoricoConsideramSomenteHojeNoFusoDaEscola() throws Exception {
        long ontem = solicitacao("CONCLUIDA", autorizacaoVigente());
        movimentacaoFinal(ontem, "CONCLUIDA", Instant.now().minus(2, ChronoUnit.DAYS));
        long hoje = solicitacao("CONCLUIDA", autorizacaoVigente());
        movimentacaoFinal(hoje, "CONCLUIDA", Instant.now().minus(1, ChronoUnit.MINUTES));

        mvc.perform(get("/api/portaria/resumo").header("Authorization", "Bearer " + tokenA))
                .andExpect(jsonPath("$.concluidasHoje").value(1));
        mvc.perform(get("/api/portaria/movimentacoes").header("Authorization", "Bearer " + tokenA))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].retiradaId").value(hoje));
    }

    @Test
    void mesmoAlunoPodeTerDuasRetiradasConcluidas() throws Exception {
        long aluno = aluno();
        long autorizacao = autorizacao(aluno, "ATIVA", -1, 30);
        for (int i = 0; i < 2; i++) {
            long id = solicitacaoPara(aluno, "IDENTIFICADO");
            sucesso(acao(tokenA, id, "confirmar-retirada", Map.of("versao", 0)));
        }
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM solicitacao_retirada WHERE aluno_id = ? AND status = 'CONCLUIDA'", Integer.class, aluno))
                .isEqualTo(2);
        assertThat(autorizacao).isPositive();
    }

    // ============================================================================ apoio

    private ResultActions acao(String token, long id, String acao, Map<String, ?> corpo) throws Exception {
        return mvc.perform(post(BASE + id + "/" + acao)
                .with(csrf())
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(corpo)));
    }

    private JsonNode sucesso(ResultActions resultado) throws Exception {
        MvcResult r = resultado.andExpect(status().isOk()).andReturn();
        return json.readTree(r.getResponse().getContentAsString());
    }

    private void erro(ResultActions resultado, int statusEsperado, String codigo) throws Exception {
        resultado.andExpect(status().is(statusEsperado)).andExpect(jsonPath("$.codigo").value(codigo));
    }

    private void assertSemAlteracao(long id, String statusEsperado) {
        assertThat(jdbc.queryForObject("SELECT status FROM solicitacao_retirada WHERE id = ?", String.class, id)).isEqualTo(statusEsperado);
        assertThat(contar("movimentacao_retirada", id)).isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM auditoria_registro WHERE entidade_id = ?", Integer.class, id)).isZero();
    }

    private int contar(String tabela, long solicitacaoId) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM " + tabela + " WHERE solicitacao_retirada_id = ?", Integer.class, solicitacaoId);
    }

    private String login(String email) throws Exception {
        MvcResult r = mvc.perform(post("/api/auth/login").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("email", email, "password", SENHA))))
                .andExpect(status().isOk()).andReturn();
        return json.readTree(r.getResponse().getContentAsString()).get("accessToken").asText();
    }

    private long usuario(long escola, String email, String perfil) {
        return inserir("usuario", Map.of("escola_id", escola, "nome", "Usuário " + perfil, "email", email,
                "senha_hash", passwordEncoder.encode(SENHA), "perfil", perfil, "status", "ATIVO", "versao", 0));
    }

    private long responsavel;
    private long alunoAtual;

    private long aluno() {
        if (responsavel == 0) {
            responsavel = inserir("responsavel", Map.of("escola_id", escolaA, "nome", "Responsável Teste",
                    "documento_criptografado", "x", "documento_hmac", "TESTE", "status", "ATIVO", "versao", 0));
        }
        long id = inserir("aluno", Map.of("escola_id", escolaA, "turma_id", turmaA, "nome", "Aluno", "data_nascimento", "2016-01-01",
                "matricula", "M-" + System.nanoTime(), "documento_criptografado", "x", "documento_hmac", "x", "status", "ATIVO", "versao", 0));
        jdbc.update("UPDATE aluno SET nome = ? WHERE id = ?", "Aluno " + id, id);
        return id;
    }

    /** Autorização vigente para um novo aluno; a solicitação seguinte usa esse aluno. */
    private long autorizacaoVigente() {
        return autorizacao("ATIVA", -1, 30);
    }

    private long autorizacao(String status, int inicioDias, int fimDias) {
        alunoAtual = aluno();
        return autorizacao(alunoAtual, status, inicioDias, fimDias);
    }

    private long autorizacao(long aluno, String status, int inicioDias, int fimDias) {
        return inserir("autorizacao_retirada", Map.of("escola_id", escolaA, "aluno_id", aluno, "responsavel_id", responsavel,
                "tipo", "AUTORIZADO_TEMPORARIO", "status", status,
                "data_inicio", utc(Instant.now().plus(inicioDias, ChronoUnit.DAYS)),
                "data_fim", utc(Instant.now().plus(fimDias, ChronoUnit.DAYS)), "versao", 0));
    }

    /** Solicitação do aluno da última autorização criada; o id da solicitação também nomeia o aluno no teste. */
    private long solicitacao(String status, long autorizacaoId) {
        long id = solicitacaoPara(alunoAtual, status);
        jdbc.update("UPDATE aluno SET nome = ? WHERE id = ?", "Aluno " + id, alunoAtual);
        return id;
    }

    private long solicitacaoPara(long aluno, String status) {
        boolean finalizada = status.equals("CONCLUIDA") || status.equals("REJEITADA") || status.equals("CANCELADA");
        Map<String, Object> campos = new HashMap<>(Map.of("escola_id", escolaA, "aluno_id", aluno, "responsavel_id", responsavel,
                "status", status, "ativa", finalizada ? 0 : 1, "motivo", "Consulta",
                "data_solicitacao", utc(Instant.now().minus(10, ChronoUnit.MINUTES)), "versao", 0));
        if (!status.equals("AGUARDANDO")) campos.put("funcionario_id", funcionarioA);
        return inserir("solicitacao_retirada", campos);
    }

    private void movimentacaoFinal(long solicitacao, String statusNovo, Instant quando) {
        inserir("movimentacao_retirada", Map.of("escola_id", escolaA, "solicitacao_retirada_id", solicitacao,
                "status_anterior", "IDENTIFICADO", "status_novo", statusNovo, "ator_tipo", "FUNCIONARIO", "usuario_id", funcionarioA,
                "motivo", "RETIRADA_CONFIRMADA", "data_ocorrencia", utc(quando), "versao", 0));
    }

    /** Datas gravadas em UTC, como o Hibernate (hibernate.jdbc.time_zone=UTC). */
    private static LocalDateTime utc(Instant instante) {
        return LocalDateTime.ofInstant(instante, ZoneOffset.UTC);
    }

    private long inserir(String tabela, Map<String, ?> valores) {
        return new SimpleJdbcInsert(jdbc).withTableName(tabela).usingColumns(valores.keySet().toArray(String[]::new))
                .usingGeneratedKeyColumns("id").executeAndReturnKey(new HashMap<>(valores)).longValue();
    }

    private void limparBase() {
        jdbc.execute("DROP TRIGGER IF EXISTS trg_teste_falha_auditoria");
        jdbc.execute("TRUNCATE TABLE auditoria_registro");
        for (String t : List.of("refresh_token", "movimentacao_retirada", "credencial_retirada", "solicitacao_retirada",
                "autorizacao_retirada", "vinculo_aluno_responsavel", "responsavel", "aluno", "turma", "usuario", "escola")) {
            jdbc.execute("DELETE FROM " + t);
        }
        responsavel = 0;
    }
}
