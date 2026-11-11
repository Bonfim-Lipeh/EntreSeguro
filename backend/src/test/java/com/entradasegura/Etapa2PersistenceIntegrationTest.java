package com.entradasegura;

import com.entradasegura.audit.domain.AuditoriaRegistro;
import com.entradasegura.audit.repository.AuditoriaRegistroRepository;
import com.entradasegura.authorization.domain.AutorizacaoRetirada;
import com.entradasegura.authorization.repository.AutorizacaoRetiradaRepository;
import com.entradasegura.credential.domain.CredencialRetirada;
import com.entradasegura.credential.repository.CredencialRetiradaRepository;
import com.entradasegura.identity.domain.PerfilUsuario;
import com.entradasegura.identity.domain.Usuario;
import com.entradasegura.identity.repository.UsuarioRepository;
import com.entradasegura.people.domain.Aluno;
import com.entradasegura.people.domain.Responsavel;
import com.entradasegura.people.domain.VinculoAlunoResponsavel;
import com.entradasegura.people.repository.AlunoRepository;
import com.entradasegura.people.repository.ResponsavelRepository;
import com.entradasegura.people.repository.VinculoAlunoResponsavelRepository;
import com.entradasegura.pickup.domain.MotivoExcecaoRetirada;
import com.entradasegura.pickup.domain.MovimentacaoRetirada;
import com.entradasegura.pickup.domain.SolicitacaoRetirada;
import com.entradasegura.pickup.domain.StatusRetirada;
import com.entradasegura.pickup.repository.MovimentacaoRetiradaRepository;
import com.entradasegura.pickup.repository.SolicitacaoRetiradaRepository;
import com.entradasegura.pickup.service.SolicitacaoRetiradaTransitionService;
import com.entradasegura.school.domain.Escola;
import com.entradasegura.school.domain.Turma;
import com.entradasegura.school.repository.EscolaRepository;
import com.entradasegura.school.repository.TurmaRepository;
import jakarta.persistence.EntityManager;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
class Etapa2PersistenceIntegrationTest {

    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.4")
            .withDatabaseName("entradasegura_test")
            .withUsername("testuser")
            .withPassword("testpass")
            .withCommand("--log-bin-trust-function-creators=1");

    @DynamicPropertySource
    static void setDatasourceProperties(DynamicPropertyRegistry registry) {
        mysql.start();

        Flyway.configure()
                .dataSource(mysql.getJdbcUrl(), mysql.getUsername(), mysql.getPassword())
                .locations("classpath:db/migration")
                .baselineOnMigrate(true)
                .load()
                .migrate();

        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "com.mysql.cj.jdbc.Driver");

        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("spring.flyway.url", mysql::getJdbcUrl);
        registry.add("spring.flyway.user", mysql::getUsername);
        registry.add("spring.flyway.password", mysql::getPassword);
        registry.add("spring.flyway.locations", () -> "classpath:db/migration");
        registry.add("spring.flyway.baseline-on-migrate", () -> "true");

        registry.add("spring.jpa.hibernate.ddl-auto", () -> "none");
        registry.add("spring.jpa.properties.hibernate.dialect", () -> "org.hibernate.dialect.MySQLDialect");
    }

    @Autowired
    private EscolaRepository escolaRepository;

    @Autowired
    private TurmaRepository turmaRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private AlunoRepository alunoRepository;

    @Autowired
    private ResponsavelRepository responsavelRepository;

    @Autowired
    private VinculoAlunoResponsavelRepository vinculoAlunoResponsavelRepository;

    @Autowired
    private AutorizacaoRetiradaRepository autorizacaoRetiradaRepository;

    @Autowired
    private SolicitacaoRetiradaRepository solicitacaoRetiradaRepository;

    @Autowired
    private MovimentacaoRetiradaRepository movimentacaoRetiradaRepository;

    @Autowired
    private CredencialRetiradaRepository credencialRetiradaRepository;

    @Autowired
    private AuditoriaRegistroRepository auditoriaRegistroRepository;

    @Autowired
    private SolicitacaoRetiradaTransitionService solicitacaoRetiradaTransitionService;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @BeforeEach
    void limparBase() {
        jdbcTemplate.execute("TRUNCATE TABLE auditoria_registro");
        jdbcTemplate.execute("DELETE FROM movimentacao_retirada");
        jdbcTemplate.execute("DELETE FROM credencial_retirada");
        jdbcTemplate.execute("DELETE FROM solicitacao_retirada");
        jdbcTemplate.execute("DELETE FROM autorizacao_retirada");
        jdbcTemplate.execute("DELETE FROM vinculo_aluno_responsavel");
        jdbcTemplate.execute("DELETE FROM responsavel");
        jdbcTemplate.execute("DELETE FROM aluno");
        jdbcTemplate.execute("DELETE FROM turma");
        jdbcTemplate.execute("DELETE FROM usuario");
        jdbcTemplate.execute("DELETE FROM escola");
    }

    private static final AtomicLong TEST_SEQUENCE = new AtomicLong(1L);

    @Test
    void devePersistirEstruturaCompletaDoDominio() {
        Escola escola = escolaRepository.saveAndFlush(criarEscola("Escola Alpha"));
        Turma turma = turmaRepository.saveAndFlush(criarTurma(escola, "1A"));
        Usuario usuario = usuarioRepository.saveAndFlush(criarUsuario(escola, "gestor@escola.com"));

        Aluno aluno = alunoRepository.saveAndFlush(criarAluno(escola, turma, "Miguel"));
        Responsavel responsavel = responsavelRepository.saveAndFlush(criarResponsavel(escola, usuario.getId(), "Maria"));

        VinculoAlunoResponsavel vinculacao = vinculoAlunoResponsavelRepository.saveAndFlush(
                criarVinculo(escola, aluno, responsavel)
        );

        AutorizacaoRetirada autorizacao = autorizacaoRetiradaRepository.saveAndFlush(
                criarAutorizacao(escola, aluno, responsavel)
        );

        SolicitacaoRetirada solicitacao = solicitacaoRetiradaRepository.saveAndFlush(
                criarSolicitacao(escola, aluno, responsavel)
        );

        MovimentacaoRetirada movimentacao = movimentacaoRetiradaRepository.saveAndFlush(
                criarMovimentacao(escola, solicitacao, "Funcionario turno", null, responsavel.getId())
        );

        CredencialRetirada credencial = credencialRetiradaRepository.saveAndFlush(
                criarCredencial(escola, solicitacao, "hash-credencial-1")
        );

        AuditoriaRegistro auditoria = auditoriaRegistroRepository.saveAndFlush(
                criarAuditoria(escola, "SolicitacaoRetirada", solicitacao.getId(), "CREATE", usuario.getId(), null)
        );

        assertThat(escola.getId()).isNotNull();
        assertThat(turma.getId()).isNotNull();
        assertThat(aluno.getId()).isNotNull();
        assertThat(responsavel.getId()).isNotNull();
        assertThat(vinculacao.getId()).isNotNull();
        assertThat(autorizacao.getId()).isNotNull();
        assertThat(solicitacao.getId()).isNotNull();
        assertThat(movimentacao.getId()).isNotNull();
        assertThat(credencial.getId()).isNotNull();
        assertThat(auditoria.getId()).isNotNull();
    }

    @Test
    void deveImpedirDuasSolicitacoesAtivasParaMesmoAluno() {
        Escola escola = escolaRepository.saveAndFlush(criarEscola("Escola Ativa"));
        Turma turma = turmaRepository.saveAndFlush(criarTurma(escola, "2A"));
        Usuario usuario = usuarioRepository.saveAndFlush(criarUsuario(escola, "funcionario@escola.com"));
        Aluno aluno = alunoRepository.saveAndFlush(criarAluno(escola, turma, "Ana"));
        Responsavel responsavel = responsavelRepository.saveAndFlush(criarResponsavel(escola, usuario.getId(), "Bruno"));

        solicitacaoRetiradaRepository.saveAndFlush(criarSolicitacao(escola, aluno, responsavel));

        assertThatThrownBy(() -> {
            solicitacaoRetiradaRepository.saveAndFlush(criarSolicitacao(escola, aluno, responsavel));
        }).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void devePreservarHistoricoQuandoUmaSolicitacaoFinaliza() {
        Escola escola = escolaRepository.saveAndFlush(criarEscola("Escola Historico"));
        Turma turma = turmaRepository.saveAndFlush(criarTurma(escola, "3A"));
        Usuario usuario = usuarioRepository.saveAndFlush(criarUsuario(escola, "gestor2@escola.com"));
        Aluno aluno = alunoRepository.saveAndFlush(criarAluno(escola, turma, "Carlos"));
        Responsavel responsavel = responsavelRepository.saveAndFlush(criarResponsavel(escola, usuario.getId(), "Diana"));

        SolicitacaoRetirada ativa = solicitacaoRetiradaRepository.saveAndFlush(criarSolicitacao(escola, aluno, responsavel));
        ativa.iniciarAtendimento(usuario.getId(), "Atendimento iniciado");
        ativa.confirmarIdentificacao(usuario.getId(), "Identificação confirmada");
        ativa.confirmarRetirada(usuario.getId(), "Retirada concluída");
        solicitacaoRetiradaRepository.saveAndFlush(ativa);

        SolicitacaoRetirada nova = criarSolicitacao(escola, aluno, responsavel);
        SolicitacaoRetirada salva = solicitacaoRetiradaRepository.saveAndFlush(nova);

        List<SolicitacaoRetirada> todas = solicitacaoRetiradaRepository.findAll();

        assertThat(todas).hasSize(2);
        assertThat(salva.getStatus()).isEqualTo(StatusRetirada.AGUARDANDO);
    }

    @Test
    void deveProtegerRelacionamentoEntreEscolasDiferentes() {
        Escola escolaA = escolaRepository.saveAndFlush(criarEscola("Escola A"));
        Escola escolaB = escolaRepository.saveAndFlush(criarEscola("Escola B"));
        Turma turmaA = turmaRepository.saveAndFlush(criarTurma(escolaA, "4A"));
        Aluno aluno = alunoRepository.saveAndFlush(criarAluno(escolaA, turmaA, "Evelyn"));
        Responsavel responsavel = responsavelRepository.saveAndFlush(criarResponsavel(escolaB, null, "Felipe"));

        SolicitacaoRetirada solicitacao = criarSolicitacao(escolaB, aluno, responsavel);

        assertThatThrownBy(() -> solicitacaoRetiradaRepository.saveAndFlush(solicitacao))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void deveManterHistoricoDeCredenciais() {
        Escola escola = escolaRepository.saveAndFlush(criarEscola("Escola Credenciais"));
        Turma turma = turmaRepository.saveAndFlush(criarTurma(escola, "5A"));
        Usuario usuario = usuarioRepository.saveAndFlush(criarUsuario(escola, "adm@escola.com"));
        Aluno aluno = alunoRepository.saveAndFlush(criarAluno(escola, turma, "Gabriel"));
        Responsavel responsavel = responsavelRepository.saveAndFlush(criarResponsavel(escola, usuario.getId(), "Helena"));
        SolicitacaoRetirada solicitacao = solicitacaoRetiradaRepository.saveAndFlush(criarSolicitacao(escola, aluno, responsavel));

        credencialRetiradaRepository.saveAndFlush(criarCredencial(escola, solicitacao, "hash-1"));
        credencialRetiradaRepository.saveAndFlush(criarCredencial(escola, solicitacao, "hash-2"));

        List<CredencialRetirada> lista = credencialRetiradaRepository.findAllBySolicitacaoRetiradaId(solicitacao.getId());
        assertThat(lista).hasSize(2);
    }

    @Test
    void deveDetectarConflitoDeVersaoEmOperacoesConcorrentes() throws Exception {
        Escola escola = escolaRepository.saveAndFlush(criarEscola("Escola Concorrencia"));
        Turma turma = turmaRepository.saveAndFlush(criarTurma(escola, "9A"));
        Usuario usuario1 = usuarioRepository.saveAndFlush(criarUsuario(escola, "func4@escola.com"));
        Usuario usuario2 = usuarioRepository.saveAndFlush(criarUsuario(escola, "func5@escola.com"));
        Aluno aluno = alunoRepository.saveAndFlush(criarAluno(escola, turma, "Mateus"));
        Responsavel responsavel = responsavelRepository.saveAndFlush(criarResponsavel(escola, usuario1.getId(), "Nadia"));
        SolicitacaoRetirada solicitacao = solicitacaoRetiradaRepository.saveAndFlush(criarSolicitacao(escola, aluno, responsavel));

        CountDownLatch ambosCarregaram = new CountDownLatch(2);
        CountDownLatch liberarExecucao = new CountDownLatch(1);
        AtomicInteger operacoesBemSucedidas = new AtomicInteger();
        List<Throwable> erros = new ArrayList<>();

        Runnable tarefaConcorrente = (Runnable) () -> {
            TransactionTemplate template = new TransactionTemplate(transactionManager);
            try {
                template.executeWithoutResult(status -> {
                    SolicitacaoRetirada carregada = solicitacaoRetiradaRepository.findById(solicitacao.getId()).orElseThrow();
                    assertThat(carregada.getStatus()).isEqualTo(StatusRetirada.AGUARDANDO);
                    ambosCarregaram.countDown();
                    try {
                        liberarExecucao.await(10, TimeUnit.SECONDS);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new IllegalStateException("Thread interrompida antes da execução concorrente", e);
                    }
                    solicitacaoRetiradaTransitionService.iniciarAtendimento(
                            solicitacao.getId(),
                            usuario1.getId(),
                            "Atendimento quase simultâneo",
                            "INICIAR_ATENDIMENTO"
                    );
                });
                operacoesBemSucedidas.incrementAndGet();
            } catch (Throwable throwable) {
                synchronized (erros) {
                    erros.add(throwable);
                }
            }
        };

        ExecutorService executor = Executors.newFixedThreadPool(2);
        executor.submit(tarefaConcorrente);
        executor.submit(() -> {
            TransactionTemplate template = new TransactionTemplate(transactionManager);
            try {
                template.executeWithoutResult(status -> {
                    SolicitacaoRetirada carregada = solicitacaoRetiradaRepository.findById(solicitacao.getId()).orElseThrow();
                    assertThat(carregada.getStatus()).isEqualTo(StatusRetirada.AGUARDANDO);
                    ambosCarregaram.countDown();
                    try {
                        liberarExecucao.await(10, TimeUnit.SECONDS);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new IllegalStateException("Thread interrompida antes da execução concorrente", e);
                    }
                    solicitacaoRetiradaTransitionService.iniciarAtendimento(
                            solicitacao.getId(),
                            usuario2.getId(),
                            "Atendimento quase simultâneo",
                            "INICIAR_ATENDIMENTO"
                    );
                });
                operacoesBemSucedidas.incrementAndGet();
            } catch (Throwable throwable) {
                synchronized (erros) {
                    erros.add(throwable);
                }
            }
        });

        assertThat(ambosCarregaram.await(10, TimeUnit.SECONDS)).isTrue();
        liberarExecucao.countDown();
        executor.shutdown();
        assertThat(executor.awaitTermination(30, TimeUnit.SECONDS)).isTrue();

        assertThat(operacoesBemSucedidas.get()).isEqualTo(1);
        assertThat(erros).hasSize(1);
        assertThat(erros.get(0)).isInstanceOf(ObjectOptimisticLockingFailureException.class);

        entityManager.clear();

        SolicitacaoRetirada finalSolicitacao = solicitacaoRetiradaRepository.findById(solicitacao.getId()).orElseThrow();
        assertThat(finalSolicitacao.getStatus()).isEqualTo(StatusRetirada.EM_ATENDIMENTO);
        assertThat(movimentacaoRetiradaRepository.count()).isEqualTo(1);
        assertThat(auditoriaRegistroRepository.count()).isEqualTo(1);

        List<MovimentacaoRetirada> movimentacoes = movimentacaoRetiradaRepository.findAll();
        assertThat(movimentacoes).hasSize(1);
        assertThat(movimentacoes.get(0).getStatusNovo()).isEqualTo(StatusRetirada.EM_ATENDIMENTO);

        List<AuditoriaRegistro> auditorias = auditoriaRegistroRepository.findAll();
        assertThat(auditorias).hasSize(1);
        assertThat(auditorias.get(0).getAcao()).isEqualTo("INICIAR_ATENDIMENTO");
    }

    @Test
    void deveBloquearUpdateDeAuditoriaNoBanco() {
        Escola escola = escolaRepository.saveAndFlush(criarEscola("Escola Auditoria Update"));
        AuditoriaRegistro auditoria = auditoriaRegistroRepository.saveAndFlush(
                criarAuditoria(escola, "SolicitacaoRetirada", 99L, "CREATE", 1L, null)
        );

        assertThatThrownBy(() ->
                jdbcTemplate.update("UPDATE auditoria_registro SET motivo = 'ALTERADO' WHERE id = ?", auditoria.getId())
        ).isInstanceOf(DataAccessException.class);

        AuditoriaRegistro persistida = auditoriaRegistroRepository.findById(auditoria.getId()).orElseThrow();
        assertThat(persistida.getMotivo()).isEqualTo("CRIACAO");
        assertThat(auditoriaRegistroRepository.count()).isEqualTo(1);
    }

    @Test
    void deveBloquearDeleteDeAuditoriaNoBanco() {
        Escola escola = escolaRepository.saveAndFlush(criarEscola("Escola Auditoria Delete"));
        AuditoriaRegistro auditoria = auditoriaRegistroRepository.saveAndFlush(
                criarAuditoria(escola, "SolicitacaoRetirada", 100L, "CREATE", 2L, null)
        );

        assertThatThrownBy(() ->
                jdbcTemplate.update("DELETE FROM auditoria_registro WHERE id = ?", auditoria.getId())
        ).isInstanceOf(DataAccessException.class);

        assertThat(auditoriaRegistroRepository.findById(auditoria.getId())).isPresent();
        assertThat(auditoriaRegistroRepository.count()).isEqualTo(1);
    }

    @Test
    void devePersistirStatusMovimentacaoEAuditoriaNaMesmaTransacao() {
        Escola escola = escolaRepository.saveAndFlush(criarEscola("Escola Transacao"));
        Turma turma = turmaRepository.saveAndFlush(criarTurma(escola, "6A"));
        Usuario usuario = usuarioRepository.saveAndFlush(criarUsuario(escola, "func@escola.com"));
        Aluno aluno = alunoRepository.saveAndFlush(criarAluno(escola, turma, "Henrique"));
        Responsavel responsavel = responsavelRepository.saveAndFlush(criarResponsavel(escola, usuario.getId(), "Iara"));
        SolicitacaoRetirada solicitacao = solicitacaoRetiradaRepository.saveAndFlush(criarSolicitacao(escola, aluno, responsavel));

        long antesSolicitacoes = solicitacaoRetiradaRepository.count();
        long antesMovimentacoes = movimentacaoRetiradaRepository.count();
        long antesAuditorias = auditoriaRegistroRepository.count();

        solicitacaoRetiradaTransitionService.iniciarAtendimento(solicitacao.getId(), usuario.getId(), "Atendimento iniciado", "INICIAR_ATENDIMENTO");

        entityManager.clear();

        SolicitacaoRetirada persistida = solicitacaoRetiradaRepository.findById(solicitacao.getId()).orElseThrow();
        assertThat(persistida.getStatus()).isEqualTo(StatusRetirada.EM_ATENDIMENTO);
        assertThat(movimentacaoRetiradaRepository.count()).isEqualTo(antesMovimentacoes + 1);
        assertThat(auditoriaRegistroRepository.count()).isEqualTo(antesAuditorias + 1);
        assertThat(solicitacaoRetiradaRepository.count()).isEqualTo(antesSolicitacoes);
    }

    @Test
    void deveFazerRollbackQuandoPersistenciaDaMovimentacaoFalha() {
        Escola escola = escolaRepository.saveAndFlush(criarEscola("Escola Rollback Movimento"));
        Turma turma = turmaRepository.saveAndFlush(criarTurma(escola, "7A"));
        Usuario usuario = usuarioRepository.saveAndFlush(criarUsuario(escola, "func2@escola.com"));
        Aluno aluno = alunoRepository.saveAndFlush(criarAluno(escola, turma, "Joao"));
        Responsavel responsavel = responsavelRepository.saveAndFlush(criarResponsavel(escola, usuario.getId(), "Kelly"));
        SolicitacaoRetirada solicitacao = solicitacaoRetiradaRepository.saveAndFlush(criarSolicitacao(escola, aluno, responsavel));

        long antesMovimentacoes = movimentacaoRetiradaRepository.count();
        long antesAuditorias = auditoriaRegistroRepository.count();

        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);

        assertThatThrownBy(() -> transactionTemplate.executeWithoutResult(status ->
                solicitacaoRetiradaTransitionService.iniciarAtendimento(
                        solicitacao.getId(),
                        usuario.getId(),
                        "Atendimento iniciado",
                        "X".repeat(101)
                )
        )).isInstanceOf(DataIntegrityViolationException.class);

        entityManager.clear();

        SolicitacaoRetirada persistida = solicitacaoRetiradaRepository.findById(solicitacao.getId()).orElseThrow();
        assertThat(persistida.getStatus()).isEqualTo(StatusRetirada.AGUARDANDO);
        assertThat(movimentacaoRetiradaRepository.count()).isEqualTo(antesMovimentacoes);
        assertThat(auditoriaRegistroRepository.count()).isEqualTo(antesAuditorias);
    }

    @Test
    void deveFazerRollbackQuandoPersistenciaDaAuditoriaFalha() {
        Escola escola = escolaRepository.saveAndFlush(criarEscola("Escola Rollback Auditoria"));
        Turma turma = turmaRepository.saveAndFlush(criarTurma(escola, "8A"));
        Usuario usuario = usuarioRepository.saveAndFlush(criarUsuario(escola, "func3@escola.com"));
        Aluno aluno = alunoRepository.saveAndFlush(criarAluno(escola, turma, "Luan"));
        Responsavel responsavel = responsavelRepository.saveAndFlush(criarResponsavel(escola, usuario.getId(), "Marta"));
        SolicitacaoRetirada solicitacao = solicitacaoRetiradaRepository.saveAndFlush(criarSolicitacao(escola, aluno, responsavel));

        long antesMovimentacoes = movimentacaoRetiradaRepository.count();
        long antesAuditorias = auditoriaRegistroRepository.count();

        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);

        assertThatThrownBy(() -> transactionTemplate.executeWithoutResult(status ->
                solicitacaoRetiradaTransitionService.iniciarAtendimento(
                        solicitacao.getId(),
                        usuario.getId(),
                        "Atendimento iniciado",
                        "INICIAR_ATENDIMENTO",
                        "Y".repeat(101)
                )
        )).isInstanceOf(DataIntegrityViolationException.class);

        entityManager.clear();

        SolicitacaoRetirada persistida = solicitacaoRetiradaRepository.findById(solicitacao.getId()).orElseThrow();
        assertThat(persistida.getStatus()).isEqualTo(StatusRetirada.AGUARDANDO);
        assertThat(movimentacaoRetiradaRepository.count()).isEqualTo(antesMovimentacoes);
        assertThat(auditoriaRegistroRepository.count()).isEqualTo(antesAuditorias);
    }

    private static Escola criarEscola(String nome) {
        Escola escola = new Escola();
        escola.setNome(nome);
        escola.setCnpj(proximoCnpj());
        escola.setRazaoSocial(nome + " LTDA");
        escola.setEmail("contato@" + nome.toLowerCase().replace(" ", "") + ".com");
        escola.setTelefone("(11) 3333-2222");
        escola.setTimezone("UTC");
        escola.setStatus("ATIVA");
        escola.setCriadoEm(LocalDateTime.now());
        return escola;
    }

    private static Turma criarTurma(Escola escola, String nome) {
        Turma turma = new Turma();
        turma.setEscolaId(escola.getId());
        turma.setNome(nome);
        turma.setNivel("ENSINO_MEDIO");
        turma.setTurno("MANHA");
        turma.setAnoLetivo(2026);
        turma.setStatus("ATIVA");
        turma.setCriadoEm(LocalDateTime.now());
        return turma;
    }

    private static Usuario criarUsuario(Escola escola, String email) {
        Usuario usuario = new Usuario();
        usuario.setEscolaId(escola.getId());
        usuario.setNome("Usuário Teste");

        int atIndex = email.indexOf('@');
        String emailBase = atIndex >= 0 ? email.substring(0, atIndex) : email;
        String dominio = atIndex >= 0 ? email.substring(atIndex + 1) : "teste.local";
        usuario.setEmail(emailBase + "+" + TEST_SEQUENCE.getAndIncrement() + "@" + dominio);

        usuario.setSenhaHash("hash-seguro");
        usuario.setPerfil(PerfilUsuario.FUNCIONARIO);
        usuario.setStatus("ATIVO");
        usuario.setCriadoEm(LocalDateTime.now());
        return usuario;
    }

    private static String proximoCnpj() {
        long numero = TEST_SEQUENCE.getAndIncrement();
        String sufixo = String.format("%02d", numero % 100);
        return "18.468.317/0001-" + sufixo;
    }

    private static Aluno criarAluno(Escola escola, Turma turma, String nome) {
        Aluno aluno = new Aluno();
        aluno.setEscolaId(escola.getId());
        aluno.setTurmaId(turma.getId());
        aluno.setNome(nome);
        aluno.setDataNascimento(LocalDate.of(2013, 5, 14));
        aluno.setMatricula("MAT" + System.nanoTime());
        aluno.setStatus("ATIVO");
        aluno.setDocumentoCriptografado("enc-doc");
        aluno.setDocumentoHmac("hmac-doc");
        aluno.setCriadoEm(LocalDateTime.now());
        return aluno;
    }

    private static Responsavel criarResponsavel(Escola escola, Long usuarioId, String nome) {
        Responsavel responsavel = new Responsavel();
        responsavel.setEscolaId(escola.getId());
        responsavel.setUsuarioId(usuarioId);
        responsavel.setNome(nome);
        responsavel.setDocumentoCriptografado("enc-doc");
        responsavel.setDocumentoHmac("hmac-doc");
        responsavel.setTelefone("(11) 99999-0000");
        responsavel.setEmail(nome.toLowerCase() + "@teste.com");
        responsavel.setStatus("ATIVO");
        responsavel.setCriadoEm(LocalDateTime.now());
        return responsavel;
    }

    private static VinculoAlunoResponsavel criarVinculo(Escola escola, Aluno aluno, Responsavel responsavel) {
        VinculoAlunoResponsavel vinculo = new VinculoAlunoResponsavel();
        vinculo.setEscolaId(escola.getId());
        vinculo.setAlunoId(aluno.getId());
        vinculo.setResponsavelId(responsavel.getId());
        vinculo.setTipoRelacao("PAI");
        vinculo.setPrincipal(true);
        vinculo.setAtivo(true);
        vinculo.setCriadoEm(LocalDateTime.now());
        return vinculo;
    }

    private static AutorizacaoRetirada criarAutorizacao(Escola escola, Aluno aluno, Responsavel responsavel) {
        AutorizacaoRetirada autorizacao = new AutorizacaoRetirada();
        autorizacao.setEscolaId(escola.getId());
        autorizacao.setAlunoId(aluno.getId());
        autorizacao.setResponsavelId(responsavel.getId());
        autorizacao.setTipo("TEMPORARIA");
        autorizacao.setStatus("ATIVA");
        autorizacao.setDataInicio(LocalDateTime.now().minusDays(1));
        autorizacao.setDataFim(LocalDateTime.now().plusDays(7));
        autorizacao.setCriadoEm(LocalDateTime.now());
        return autorizacao;
    }

    private static SolicitacaoRetirada criarSolicitacao(Escola escola, Aluno aluno, Responsavel responsavel) {
        return SolicitacaoRetirada.novaSolicitacao(
                escola.getId(),
                aluno.getId(),
                responsavel.getId(),
                "Consulta médica",
                LocalDateTime.now()
        );
    }

    private static MovimentacaoRetirada criarMovimentacao(Escola escola, SolicitacaoRetirada solicitacao, String observacoes, Long usuarioId, Long responsavelId) {
        MovimentacaoRetirada movimentacao = new MovimentacaoRetirada();
        movimentacao.setEscolaId(escola.getId());
        movimentacao.setSolicitacaoRetiradaId(solicitacao.getId());
        movimentacao.setStatusAnterior(StatusRetirada.AGUARDANDO);
        movimentacao.setStatusNovo(StatusRetirada.EM_ATENDIMENTO);
        movimentacao.setAtorTipo("FUNCIONARIO");
        movimentacao.setUsuarioId(usuarioId);
        movimentacao.setResponsavelId(responsavelId);
        movimentacao.setMotivo("INICIAR_ATENDIMENTO");
        movimentacao.setJustificativa("Atendimento iniciado pelo funcionário.");
        movimentacao.setDataOcorrencia(LocalDateTime.now());
        movimentacao.setObservacoes(observacoes);
        movimentacao.setCriadoEm(LocalDateTime.now());
        return movimentacao;
    }

    private static CredencialRetirada criarCredencial(Escola escola, SolicitacaoRetirada solicitacao, String hash) {
        CredencialRetirada credencial = new CredencialRetirada();
        credencial.setEscolaId(escola.getId());
        credencial.setSolicitacaoRetiradaId(solicitacao.getId());
        credencial.setTipo("QR_CODE");
        credencial.setTokenHash(hash);
        credencial.setStatus("ATIVA");
        credencial.setDataExpiracao(LocalDateTime.now().plusMinutes(15));
        credencial.setCriadoEm(LocalDateTime.now());
        return credencial;
    }

    private static AuditoriaRegistro criarAuditoria(Escola escola, String entidade, Long entidadeId, String acao, Long usuarioId, Long responsavelId) {
        return AuditoriaRegistro.novoRegistro(
                escola.getId(),
                entidade,
                entidadeId,
                acao,
                usuarioId,
                responsavelId,
                "CRIACAO",
                "{\"status\": \"AGUARDANDO\"}",
                "127.0.0.1"
        );
    }
}
