package com.entradasegura.auth;

import com.entradasegura.auth.config.JwtProperties;
import com.entradasegura.auth.repository.RefreshTokenRepository;
import com.entradasegura.auth.security.JwtService;
import com.entradasegura.identity.domain.PerfilUsuario;
import com.entradasegura.identity.domain.Usuario;
import com.entradasegura.identity.repository.UsuarioRepository;
import com.entradasegura.school.domain.Escola;
import com.entradasegura.school.repository.EscolaRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
class AuthJwtIntegrationTest {

    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.4")
            .withDatabaseName("entradasegura_auth_test")
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
        registry.add("entradasegura.jwt.secret", () -> "TestSecretKeyForJWT_1234567890ABCD");
        registry.add("entradasegura.jwt.issuer", () -> "entradasegura-test");
        registry.add("entradasegura.jwt.audience", () -> "entradasegura-portaria-test");
        registry.add("entradasegura.security.cors.allowed-origins", () -> "http://localhost:5173,http://127.0.0.1:5173");
    }

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private JwtProperties jwtProperties;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private EscolaRepository escolaRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Escola escola;

    @BeforeEach
    void setUp() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity())
                .build();

        refreshTokenRepository.deleteAll();
        usuarioRepository.deleteAll();
        escolaRepository.deleteAll();

        escola = new Escola();
        escola.setNome("Escola Teste");
        escola.setCnpj("30.900.000/0001-00");
        escola.setRazaoSocial("Escola Teste Ltda");
        escola.setTelefone("(11) 3000-0000");
        escola.setEmail("contato@escolateste.com.br");
        escola.setTimezone("America/Sao_Paulo");
        escola.setStatus("ATIVA");
        escola = escolaRepository.save(escola);
    }

    @Test
    void loginValidoRetornaAccessERefresh() throws Exception {
        Usuario funcionario = criarUsuario("funcionario@escola.com", "Senha@123", PerfilUsuario.FUNCIONARIO, "ATIVO");

        String body = objectMapper.writeValueAsString(Map.of(
                "email", funcionario.getEmail(),
                "password", "Senha@123"
        ));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(cookie().exists("refresh_token"))
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"));
    }

    @Test
    void loginComSenhaIncorretaRetornaNaoAutorizado() throws Exception {
        Usuario funcionario = criarUsuario("funcionario@escola.com", "Senha@123", PerfilUsuario.FUNCIONARIO, "ATIVO");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", funcionario.getEmail(),
                                "password", "senha-errada"
                        )))
                        .with(csrf()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void usuarioInativoNaoPodeAutenticar() throws Exception {
        Usuario funcionario = criarUsuario("inativo@escola.com", "Senha@123", PerfilUsuario.FUNCIONARIO, "INATIVO");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", funcionario.getEmail(),
                                "password", "Senha@123"
                        )))
                        .with(csrf()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void perfilNaoAutorizadoRecebeForbiddenNoPainel() throws Exception {
        Usuario gestor = criarUsuario("gestor@escola.com", "Senha@123", PerfilUsuario.GESTOR, "ATIVO");

        String accessToken = obterAccessToken(gestor.getEmail(), "Senha@123");

        mockMvc.perform(get("/api/portaria/sessao")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void tokenExpiradoRetornaUnauthorized() throws Exception {
        Usuario funcionario = criarUsuario("expirado@escola.com", "Senha@123", PerfilUsuario.FUNCIONARIO, "ATIVO");
        String expiredToken = "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJleHBpcmFkbyBlc2NvbGEuY29tIiwiZXhwIjoxNTAwMDAwMDAwfQ." + "invalid-signature";

        mockMvc.perform(get("/api/portaria/sessao")
                        .header("Authorization", "Bearer " + expiredToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void assinaturaInvalidaRetornaUnauthorized() throws Exception {
        mockMvc.perform(get("/api/portaria/sessao")
                        .header("Authorization", "Bearer invalid.token.value"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void refreshTokenValidoGeraNovoAccessToken() throws Exception {
        Usuario funcionario = criarUsuario("refresh@escola.com", "Senha@123", PerfilUsuario.FUNCIONARIO, "ATIVO");

        String accessToken = obterAccessToken(funcionario.getEmail(), "Senha@123");

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(csrf())
                        .cookie(obterRefreshCookie(funcionario.getEmail(), "Senha@123")))
                .andExpect(status().isOk())
                .andExpect(cookie().exists("refresh_token"))
                .andExpect(jsonPath("$.accessToken").isNotEmpty());

        mockMvc.perform(get("/api/portaria/sessao")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk());
    }

    @Test
    void refreshTokenRotacionaERevogaOAnterior() throws Exception {
        Usuario funcionario = criarUsuario("rotacao@escola.com", "Senha@123", PerfilUsuario.FUNCIONARIO, "ATIVO");

        String refreshCookieValue = obterRefreshCookieValue(funcionario.getEmail(), "Senha@123");

        var result = mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(csrf())
                        .cookie(new jakarta.servlet.http.Cookie("refresh_token", refreshCookieValue)))
                .andExpect(status().isOk())
                .andExpect(cookie().exists("refresh_token"))
                .andReturn();

        String novoRefresh = result.getResponse().getCookie("refresh_token").getValue();

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(csrf())
                        .cookie(new jakarta.servlet.http.Cookie("refresh_token", refreshCookieValue)))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(csrf())
                        .cookie(new jakarta.servlet.http.Cookie("refresh_token", novoRefresh)))
                .andExpect(status().isOk());
    }

    @Test
    void logoutRevogaRefreshToken() throws Exception {
        Usuario funcionario = criarUsuario("logout@escola.com", "Senha@123", PerfilUsuario.FUNCIONARIO, "ATIVO");

        String refreshCookieValue = obterRefreshCookieValue(funcionario.getEmail(), "Senha@123");

        mockMvc.perform(post("/api/auth/logout")
                        .with(csrf())
                        .cookie(new jakarta.servlet.http.Cookie("refresh_token", refreshCookieValue)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(csrf())
                        .cookie(new jakarta.servlet.http.Cookie("refresh_token", refreshCookieValue)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void httpBasicNaoAutenticaNoPainelSomenteJwt() throws Exception {
        Usuario funcionario = criarUsuario("basic@escola.com", "Senha@123", PerfilUsuario.FUNCIONARIO, "ATIVO");

        mockMvc.perform(get("/api/portaria/sessao")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors
                                .httpBasic(funcionario.getEmail(), "Senha@123")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void acessoAnonimoParaPortariaEhBloqueado() throws Exception {
        mockMvc.perform(get("/api/portaria/sessao"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void refreshConcorrenteNaoGeraDuasSessoesValidas() throws Exception {
        Usuario funcionario = criarUsuario("concorrencia@escola.com", "Senha@123", PerfilUsuario.FUNCIONARIO, "ATIVO");
        String refreshTokenValue = obterRefreshCookieValue(funcionario.getEmail(), "Senha@123");

        ExecutorService executorService = Executors.newFixedThreadPool(2);
        CyclicBarrier barrier = new CyclicBarrier(2);
        List<Callable<Integer>> tasks = List.of(
                () -> {
                    barrier.await();
                    return mockMvc.perform(post("/api/auth/refresh")
                                    .with(csrf())
                                    .cookie(new jakarta.servlet.http.Cookie("refresh_token", refreshTokenValue)))
                            .andReturn()
                            .getResponse()
                            .getStatus();
                },
                () -> {
                    barrier.await();
                    return mockMvc.perform(post("/api/auth/refresh")
                                    .with(csrf())
                                    .cookie(new jakarta.servlet.http.Cookie("refresh_token", refreshTokenValue)))
                            .andReturn()
                            .getResponse()
                            .getStatus();
                }
        );

        List<Future<Integer>> futureResults = executorService.invokeAll(tasks);
        executorService.shutdown();

        long okCount = futureResults.stream().mapToInt(f -> {
            try {
                return f.get();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }).filter(statusCode -> statusCode == 200).count();

        assertEquals(1, okCount);
    }

    @Test
    void jwtComIssuerOuAudienceInvalidosEhRejeitado() throws Exception {
        Usuario funcionario = criarUsuario("jwtclaims@escola.com", "Senha@123", PerfilUsuario.FUNCIONARIO, "ATIVO");

        String tokenSemIssuer = gerarToken(funcionario, null, jwtProperties.getAudience());
        String tokenSemAudience = gerarToken(funcionario, jwtProperties.getIssuer(), null);
        String tokenIssuerInvalido = gerarToken(funcionario, "issuer-incorreto", jwtProperties.getAudience());
        String tokenAudienceInvalida = gerarToken(funcionario, jwtProperties.getIssuer(), "audiencia-incorreta");

        validarAcessoNegado(tokenSemIssuer);
        validarAcessoNegado(tokenSemAudience);
        validarAcessoNegado(tokenIssuerInvalido);
        validarAcessoNegado(tokenAudienceInvalida);
    }

    @Test
    void corsParaOrigemAutorizadaEhPermitida() throws Exception {
        mockMvc.perform(options("/api/auth/login")
                        .header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"))
                .andExpect(header().string("Access-Control-Allow-Credentials", "true"));
    }

    @Test
    void refreshSemCsrfEhNegado() throws Exception {
        Usuario funcionario = criarUsuario("csrf@escola.com", "Senha@123", PerfilUsuario.FUNCIONARIO, "ATIVO");

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .cookie(new jakarta.servlet.http.Cookie("refresh_token", obterRefreshCookieValue(funcionario.getEmail(), "Senha@123"))))
                .andExpect(status().isForbidden());
    }

    private void validarAcessoNegado(String token) throws Exception {
        mockMvc.perform(get("/api/portaria/sessao")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    private String gerarToken(Usuario usuario, String issuer, String audience) {
        var builder = Jwts.builder()
                .subject(usuario.getEmail())
                .claim("usuarioId", usuario.getId())
                .claim("escolaId", usuario.getEscolaId())
                .claim("perfil", usuario.getPerfil().name())
                .claim("nome", usuario.getNome())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8)));

        if (issuer != null) {
            builder.issuer(issuer);
        }

        if (audience != null) {
            builder.audience().add(audience).and();
        }

        return builder.compact();
    }

    private Usuario criarUsuario(String email, String senha, PerfilUsuario perfil, String status) {
        Usuario usuario = new Usuario();
        usuario.setEscolaId(escola.getId());
        usuario.setNome("Usuário Teste");
        usuario.setEmail(email);
        usuario.setSenhaHash(passwordEncoder.encode(senha));
        usuario.setPerfil(perfil);
        usuario.setStatus(status);
        return usuarioRepository.save(usuario);
    }

    private String obterAccessToken(String email, String senha) throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "email", email,
                "password", senha
        ));

        return objectMapper.readTree(
                mockMvc.perform(post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body)
                                .with(csrf()))
                        .andReturn()
                        .getResponse()
                        .getContentAsString()
        ).get("accessToken").asText();
    }

    private jakarta.servlet.http.Cookie obterRefreshCookie(String email, String senha) throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "email", email,
                "password", senha
        ));

        var result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
                        .with(csrf()))
                .andReturn();

        return result.getResponse().getCookie("refresh_token");
    }

    private String obterRefreshCookieValue(String email, String senha) throws Exception {
        return obterRefreshCookie(email, senha).getValue();
    }
}
