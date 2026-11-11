package com.entradasegura.portaria.service;

import com.entradasegura.authorization.domain.AutorizacaoRetirada;
import com.entradasegura.authorization.repository.AutorizacaoRetiradaRepository;
import com.entradasegura.identity.domain.Usuario;
import com.entradasegura.identity.repository.UsuarioRepository;
import com.entradasegura.people.domain.Aluno;
import com.entradasegura.people.domain.Responsavel;
import com.entradasegura.people.domain.VinculoAlunoResponsavel;
import com.entradasegura.people.repository.AlunoRepository;
import com.entradasegura.people.repository.ResponsavelRepository;
import com.entradasegura.people.repository.VinculoAlunoResponsavelRepository;
import com.entradasegura.pickup.domain.MovimentacaoRetirada;
import com.entradasegura.pickup.domain.SolicitacaoRetirada;
import com.entradasegura.pickup.domain.StatusRetirada;
import com.entradasegura.pickup.repository.MovimentacaoRetiradaRepository;
import com.entradasegura.pickup.repository.SolicitacaoRetiradaRepository;
import com.entradasegura.portaria.api.dto.AlertaConferenciaDTO;
import com.entradasegura.portaria.api.dto.AlunoResumoDTO;
import com.entradasegura.portaria.api.dto.EscolaResumoDTO;
import com.entradasegura.portaria.api.dto.FuncionarioResumoDTO;
import com.entradasegura.portaria.api.dto.MovimentacaoDTO;
import com.entradasegura.portaria.api.dto.ResponsavelSolicitanteDTO;
import com.entradasegura.portaria.api.dto.ResumoPortariaDTO;
import com.entradasegura.portaria.api.dto.RetiradaDTO;
import com.entradasegura.portaria.api.dto.SessaoPortariaDTO;
import com.entradasegura.portaria.security.PortariaPrincipal;
import com.entradasegura.portaria.security.PortariaSecurityContext;
import com.entradasegura.school.domain.Escola;
import com.entradasegura.school.domain.Turma;
import com.entradasegura.school.repository.EscolaRepository;
import com.entradasegura.school.repository.TurmaRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

@Service
public class PortariaService {

    static final Set<StatusRetirada> STATUS_FINAIS = Set.of(StatusRetirada.CONCLUIDA, StatusRetirada.REJEITADA, StatusRetirada.CANCELADA);
    /** Motivo gravado na movimentação de identificação: "IDENTIFICACAO_" + método. */
    static final String PREFIXO_IDENTIFICACAO = "IDENTIFICACAO_";

    private final SolicitacaoRetiradaRepository solicitacaoRetiradaRepository;
    private final MovimentacaoRetiradaRepository movimentacaoRetiradaRepository;
    private final EscolaRepository escolaRepository;
    private final UsuarioRepository usuarioRepository;
    private final AlunoRepository alunoRepository;
    private final ResponsavelRepository responsavelRepository;
    private final TurmaRepository turmaRepository;
    private final AutorizacaoRetiradaRepository autorizacaoRetiradaRepository;
    private final VinculoAlunoResponsavelRepository vinculoAlunoResponsavelRepository;

    public PortariaService(
            SolicitacaoRetiradaRepository solicitacaoRetiradaRepository,
            MovimentacaoRetiradaRepository movimentacaoRetiradaRepository,
            EscolaRepository escolaRepository,
            UsuarioRepository usuarioRepository,
            AlunoRepository alunoRepository,
            ResponsavelRepository responsavelRepository,
            TurmaRepository turmaRepository,
            AutorizacaoRetiradaRepository autorizacaoRetiradaRepository,
            VinculoAlunoResponsavelRepository vinculoAlunoResponsavelRepository
    ) {
        this.solicitacaoRetiradaRepository = solicitacaoRetiradaRepository;
        this.movimentacaoRetiradaRepository = movimentacaoRetiradaRepository;
        this.escolaRepository = escolaRepository;
        this.usuarioRepository = usuarioRepository;
        this.alunoRepository = alunoRepository;
        this.responsavelRepository = responsavelRepository;
        this.turmaRepository = turmaRepository;
        this.autorizacaoRetiradaRepository = autorizacaoRetiradaRepository;
        this.vinculoAlunoResponsavelRepository = vinculoAlunoResponsavelRepository;
    }

    public SessaoPortariaDTO obterSessao() {
        PortariaPrincipal principal = PortariaSecurityContext.currentPrincipal();
        Long escolaId = principal.getEscolaId();
        Long usuarioId = principal.getUsuarioId();

        Escola escola = escolaRepository.findById(escolaId)
                .orElseThrow(() -> new IllegalStateException("Escola autenticada não cadastrada no sistema."));
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new IllegalStateException("Usuário autenticado não cadastrado no sistema."));

        FuncionarioResumoDTO funcionario = new FuncionarioResumoDTO(usuario.getId(), usuario.getNome(), "Portaria");
        return new SessaoPortariaDTO(new EscolaResumoDTO(escola.getId(), escola.getNome()), funcionario, "Posto principal");
    }

    public List<RetiradaDTO> listarRetiradas(List<String> status, String busca) {
        Long escolaId = PortariaSecurityContext.currentEscolaId();
        List<SolicitacaoRetirada> solicitacoes = solicitacaoRetiradaRepository.findAll().stream()
                .filter(s -> Objects.equals(s.getEscolaId(), escolaId))
                .sorted(Comparator.comparing(SolicitacaoRetirada::getDataSolicitacao))
                .toList();
        return solicitacoes.stream()
                .filter(s -> status == null || status.isEmpty() || status.stream().map(String::trim).map(String::toUpperCase).anyMatch(v -> v.equalsIgnoreCase(s.getStatus().name())))
                .filter(s -> busca == null || busca.isBlank() || matchesBusca(s, busca))
                .map(this::mapearRetirada)
                .toList();
    }

    public RetiradaDTO obterRetirada(Long id) {
        Long escolaId = PortariaSecurityContext.currentEscolaId();
        SolicitacaoRetirada solicitacao = solicitacaoRetiradaRepository.findByIdAndEscolaId(id, escolaId)
                .orElseThrow(PortariaOperacaoException::naoEncontrada);
        return mapearRetirada(solicitacao);
    }

    public ResumoPortariaDTO obterResumo() {
        Long escolaId = PortariaSecurityContext.currentEscolaId();
        List<SolicitacaoRetirada> solicitacoes = solicitacaoRetiradaRepository.findAll().stream()
                .filter(s -> Objects.equals(s.getEscolaId(), escolaId))
                .toList();

        int aguardando = (int) solicitacoes.stream().filter(s -> s.getStatus() == StatusRetirada.AGUARDANDO).count();
        int emAtendimento = (int) solicitacoes.stream().filter(s -> s.getStatus() == StatusRetirada.EM_ATENDIMENTO).count();
        int identificados = (int) solicitacoes.stream().filter(s -> s.getStatus() == StatusRetirada.IDENTIFICADO).count();

        // Encerramentos de HOJE no fuso da escola, pela data da movimentação final.
        List<MovimentacaoRetirada> encerradasHoje = movimentacoesFinaisDeHoje(escolaId);
        int concluidasHoje = contarSolicitacoes(encerradasHoje, StatusRetirada.CONCLUIDA);
        int rejeitadasHoje = contarSolicitacoes(encerradasHoje, StatusRetirada.REJEITADA);
        int canceladasHoje = contarSolicitacoes(encerradasHoje, StatusRetirada.CANCELADA);

        LocalDateTime aguardandoDesde = solicitacoes.stream()
                .filter(s -> s.getStatus() == StatusRetirada.AGUARDANDO)
                .map(SolicitacaoRetirada::getDataSolicitacao)
                .min(Comparator.naturalOrder())
                .orElse(null);

        return new ResumoPortariaDTO(aguardando, emAtendimento, identificados, concluidasHoje, rejeitadasHoje, canceladasHoje, aguardandoDesde);
    }

    /** Histórico: solicitações encerradas hoje (fuso da escola), mais recentes primeiro. */
    public List<MovimentacaoDTO> listarMovimentacoes(Long limite) {
        Long escolaId = PortariaSecurityContext.currentEscolaId();
        int pageLimit = limite == null ? 10 : Math.max(1, Math.toIntExact(limite));
        return movimentacoesFinaisDeHoje(escolaId).stream()
                .limit(pageLimit)
                .map(this::mapearMovimentacao)
                .toList();
    }

    /** Autorização de retirada cadastrada para o aluno e o responsável da solicitação. */
    Optional<AutorizacaoRetirada> autorizacaoDe(SolicitacaoRetirada solicitacao) {
        return autorizacaoRetiradaRepository.findByEscolaIdAndAlunoIdAndResponsavelId(
                solicitacao.getEscolaId(), solicitacao.getAlunoId(), solicitacao.getResponsavelId());
    }

    /** Autorização vigente agora e responsável ativo. A regra de vigência fica no domínio. */
    Optional<AutorizacaoRetirada> autorizacaoVigente(SolicitacaoRetirada solicitacao) {
        boolean responsavelAtivo = responsavelRepository.findById(solicitacao.getResponsavelId())
                .filter(r -> Objects.equals(r.getEscolaId(), solicitacao.getEscolaId()))
                .map(r -> "ATIVO".equalsIgnoreCase(r.getStatus()))
                .orElse(false);
        if (!responsavelAtivo) {
            return Optional.empty();
        }
        return autorizacaoDe(solicitacao).filter(a -> a.estaVigenteEm(LocalDateTime.now()));
    }

    private List<MovimentacaoRetirada> movimentacoesFinaisDeHoje(Long escolaId) {
        ZoneId zonaEscola = escolaRepository.findById(escolaId)
                .map(Escola::getTimezone)
                .map(ZoneId::of)
                .orElse(ZoneId.systemDefault());
        // As datas persistidas são LocalDateTime no fuso da JVM; converte o início do dia da escola para esse fuso.
        LocalDateTime inicioDoDia = LocalDate.now(zonaEscola).atStartOfDay(zonaEscola)
                .withZoneSameInstant(ZoneId.systemDefault())
                .toLocalDateTime();
        return movimentacaoRetiradaRepository
                .findByEscolaIdAndStatusNovoInAndDataOcorrenciaGreaterThanEqualOrderByDataOcorrenciaDesc(escolaId, STATUS_FINAIS, inicioDoDia);
    }

    private int contarSolicitacoes(List<MovimentacaoRetirada> movimentacoes, StatusRetirada status) {
        return (int) movimentacoes.stream()
                .filter(m -> m.getStatusNovo() == status)
                .map(MovimentacaoRetirada::getSolicitacaoRetiradaId)
                .distinct()
                .count();
    }

    private boolean matchesBusca(SolicitacaoRetirada solicitacao, String busca) {
        String texto = busca.toLowerCase(Locale.ROOT);
        Aluno aluno = alunoRepository.findById(solicitacao.getAlunoId()).orElse(null);
        Responsavel responsavel = responsavelRepository.findById(solicitacao.getResponsavelId()).orElse(null);

        if (aluno != null && aluno.getNome() != null && aluno.getNome().toLowerCase(Locale.ROOT).contains(texto)) {
            return true;
        }
        if (responsavel != null && responsavel.getNome() != null && responsavel.getNome().toLowerCase(Locale.ROOT).contains(texto)) {
            return true;
        }
        return solicitacao.getMotivo() != null && solicitacao.getMotivo().toLowerCase(Locale.ROOT).contains(texto);
    }

    RetiradaDTO mapearRetirada(SolicitacaoRetirada solicitacao) {
        Aluno aluno = alunoRepository.findById(solicitacao.getAlunoId()).orElseGet(() -> fallbackAluno(solicitacao.getAlunoId()));
        Responsavel responsavel = responsavelRepository.findById(solicitacao.getResponsavelId()).orElseGet(() -> fallbackResponsavel(solicitacao.getResponsavelId()));
        Turma turma = turmaRepository.findById(aluno.getTurmaId()).orElse(null);
        Optional<AutorizacaoRetirada> autorizacao = autorizacaoDe(solicitacao);
        boolean autorizacaoVigente = autorizacaoVigente(solicitacao).isPresent();
        List<MovimentacaoRetirada> movimentacoes = movimentacaoRetiradaRepository
                .findBySolicitacaoRetiradaIdOrderByDataOcorrenciaAsc(solicitacao.getId());

        String protocolo = "P-" + solicitacao.getId();
        // Campos sem origem cadastrada são enviados como null (não são inventados).
        ResponsavelSolicitanteDTO responsavelDto = new ResponsavelSolicitanteDTO(
                responsavel.getId(),
                responsavel.getNome(),
                vinculoAlunoResponsavelRepository
                        .findFirstByEscolaIdAndAlunoIdAndResponsavelIdAndAtivoTrue(solicitacao.getEscolaId(), solicitacao.getAlunoId(), solicitacao.getResponsavelId())
                        .map(VinculoAlunoResponsavel::getTipoRelacao)
                        .orElse(null),
                autorizacao.map(AutorizacaoRetirada::getTipo).orElse(null),
                autorizacao.map(AutorizacaoRetirada::getDataFim).orElse(null),
                mascararDocumento(responsavel.getDocumentoHmac())
        );

        var alertas = new ArrayList<AlertaConferenciaDTO>();
        boolean ativa = !STATUS_FINAIS.contains(solicitacao.getStatus());
        if (ativa && !autorizacaoVigente) {
            alertas.add(new AlertaConferenciaDTO("critico",
                    autorizacao.isEmpty()
                            ? "Não há autorização de retirada cadastrada para este responsável."
                            : "Autorização do responsável não está vigente (expirada ou revogada). Não libere o aluno."));
        }
        if (solicitacao.getStatus() == StatusRetirada.EM_ATENDIMENTO) {
            alertas.add(new AlertaConferenciaDTO("info", "Validação de identidade pendente."));
        }
        if (solicitacao.getStatus() == StatusRetirada.IDENTIFICADO) {
            alertas.add(new AlertaConferenciaDTO("atencao", "Identificação confirmada e aguardando retirada."));
        }

        Optional<MovimentacaoRetirada> identificacao = movimentacoes.stream()
                .filter(m -> m.getStatusNovo() == StatusRetirada.IDENTIFICADO)
                .reduce((primeira, segunda) -> segunda);
        String metodoIdentificacao = identificacao
                .map(MovimentacaoRetirada::getMotivo)
                .filter(m -> m.startsWith(PREFIXO_IDENTIFICACAO))
                .map(m -> m.substring(PREFIXO_IDENTIFICACAO.length()))
                .orElse(null);
        LocalDateTime encerramento = solicitacao.getDataConclusao() != null
                ? solicitacao.getDataConclusao()
                : movimentacoes.stream().filter(m -> STATUS_FINAIS.contains(m.getStatusNovo()))
                        .map(MovimentacaoRetirada::getDataOcorrencia).reduce((a, b) -> b).orElse(null);
        FuncionarioResumoDTO atendidoPor = Optional.ofNullable(solicitacao.getFuncionarioId())
                .flatMap(usuarioRepository::findById)
                .map(u -> new FuncionarioResumoDTO(u.getId(), u.getNome(), "Portaria"))
                .orElse(null);

        return new RetiradaDTO(
                solicitacao.getId(),
                protocolo,
                new AlunoResumoDTO(aluno.getId(), aluno.getNome(), turma != null ? turma.getNome() : "Sem turma"),
                responsavelDto,
                solicitacao.getStatus().name(),
                formatarData(solicitacao.getDataSolicitacao()),
                null,
                formatarData(solicitacao.getDataAtendimento()),
                identificacao.map(MovimentacaoRetirada::getDataOcorrencia).map(this::formatarData).orElse(null),
                formatarData(encerramento),
                atendidoPor,
                metodoIdentificacao,
                solicitacao.getQrCodeValidadoEm() != null,
                solicitacao.getMotivoExcecao() != null ? solicitacao.getMotivoExcecao().name() : null,
                solicitacao.getJustificativaRejeicao(),
                alertas,
                calcularAcoesPermitidas(solicitacao.getStatus(), autorizacaoVigente),
                solicitacao.getVersao() == null ? 0L : solicitacao.getVersao()
        );
    }

    /**
     * Ações exibidas pelo painel. As transições continuam validadas no domínio; aqui apenas
     * se omite o que certamente seria recusado. Sem autorização vigente, só resta rejeitar.
     * A validação de QR Code não é oferecida enquanto não houver emissão de credenciais.
     */
    private List<String> calcularAcoesPermitidas(StatusRetirada status, boolean autorizacaoVigente) {
        return switch (status) {
            case AGUARDANDO -> List.of("INICIAR_ATENDIMENTO");
            case EM_ATENDIMENTO -> autorizacaoVigente ? List.of("CONFIRMAR_IDENTIFICACAO", "REJEITAR") : List.of("REJEITAR");
            case IDENTIFICADO -> autorizacaoVigente ? List.of("CONFIRMAR_RETIRADA") : List.of("REJEITAR");
            default -> List.of();
        };
    }

    private MovimentacaoDTO mapearMovimentacao(MovimentacaoRetirada movimentacao) {
        // Aluno e responsável vêm da solicitação (a movimentação de funcionário não tem responsavel_id).
        Optional<SolicitacaoRetirada> solicitacao = solicitacaoRetiradaRepository.findById(movimentacao.getSolicitacaoRetiradaId());
        Optional<Aluno> aluno = solicitacao.map(SolicitacaoRetirada::getAlunoId).flatMap(alunoRepository::findById);
        String nomeAluno = aluno.map(Aluno::getNome).orElse("Aluno");
        String turma = aluno
                .map(Aluno::getTurmaId)
                .flatMap(turmaRepository::findById)
                .map(Turma::getNome)
                .orElse("-");
        String responsavelNome = solicitacao.map(SolicitacaoRetirada::getResponsavelId)
                .flatMap(responsavelRepository::findById)
                .map(Responsavel::getNome)
                .orElse("Responsável");
        String funcionarioNome = Optional.ofNullable(movimentacao.getUsuarioId())
                .flatMap(usuarioRepository::findById)
                .map(Usuario::getNome)
                .orElse(null);

        return new MovimentacaoDTO(
                movimentacao.getId(),
                movimentacao.getSolicitacaoRetiradaId(),
                "P-" + movimentacao.getSolicitacaoRetiradaId(),
                nomeAluno,
                turma,
                responsavelNome,
                funcionarioNome,
                formatarData(movimentacao.getDataOcorrencia()),
                movimentacao.getStatusNovo().name(),
                movimentacao.getJustificativa()
        );
    }

    private String formatarData(LocalDateTime data) {
        return data == null ? null : data.toString();
    }

    private String mascararDocumento(String documento) {
        if (documento == null || documento.isBlank()) {
            return "***";
        }
        return documento.substring(0, Math.min(4, documento.length())) + "***";
    }

    private Aluno fallbackAluno(Long id) {
        Aluno aluno = new Aluno();
        aluno.setId(id);
        aluno.setNome("Aluno do sistema");
        aluno.setTurmaId(1L);
        return aluno;
    }

    private Responsavel fallbackResponsavel(Long id) {
        Responsavel responsavel = new Responsavel();
        responsavel.setId(id);
        responsavel.setNome("Responsável");
        responsavel.setDocumentoHmac("ABC123");
        return responsavel;
    }
}
