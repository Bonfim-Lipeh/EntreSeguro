import { useState, type ReactNode } from 'react'
import {
  AlertOctagon,
  AlertTriangle,
  Check,
  CheckCircle2,
  Info,
  Loader2,
  LogOut,
  MousePointerClick,
  Play,
  UserRoundCheck,
  X,
  XCircle,
} from 'lucide-react'
import { Button } from '@/components/ui/button'
import { cn } from '@/lib/utils'
import type { AlertaConferenciaDTO, RetiradaDTO } from '@/domain/retirada'
import { useAcoesRetirada } from '@/hooks/useAcoesRetirada'
import { useServicos } from '@/hooks/ServicosContext'
import { MOTIVO_REJEICAO_ROTULO, TIPO_AUTORIZACAO_ROTULO, ehStatusAtivo } from '../presentation/statusConfig'
import { dataCurta, formatarChegada, hora } from '../presentation/formatters'
import { AvatarIniciais } from './AvatarIniciais'
import { StatusBadge } from './StatusBadge'
import { TempoEspera } from './TempoEspera'
import { LeitorQrSimulado } from './LeitorQrSimulado'
import { DialogConfirmarIdentificacao, DialogConfirmarRetirada, DialogRejeicao } from './DialogosAcao'

interface Props {
  retirada: RetiradaDTO | undefined
  carregando: boolean
  agora: number
  className?: string
}

export function PainelDetalhes({ retirada, carregando, agora, className }: Props) {
  if (!retirada) {
    return (
      <div className={cn('flex flex-col items-center justify-center gap-3 rounded-xl border border-dashed bg-card/60 px-8 py-16 text-center', className)}>
        {carregando ? (
          <Loader2 className="size-8 animate-spin text-muted-foreground" />
        ) : (
          <>
            <span className="flex size-14 items-center justify-center rounded-full bg-muted">
              <MousePointerClick className="size-7 text-navy-700" aria-hidden />
            </span>
            <p className="font-semibold">Selecione uma solicitação</p>
            <p className="max-w-xs text-sm text-muted-foreground">
              Escolha um item da fila para ver os dados de conferência e registrar o atendimento.
            </p>
          </>
        )}
      </div>
    )
  }
  // `key` reinicia o estado local (diálogos, leitura de QR) ao trocar de solicitação.
  return <Detalhes key={retirada.id} retirada={retirada} agora={agora} className={className} />
}

function Detalhes({ retirada: r, agora, className }: { retirada: RetiradaDTO; agora: number; className?: string }) {
  const { simulador } = useServicos()
  const acoes = useAcoesRetirada()
  const [dialogo, setDialogo] = useState<null | 'identificacao' | 'retirada' | 'rejeicao'>(null)
  const pode = (a: RetiradaDTO['acoesPermitidas'][number]) => r.acoesPermitidas.includes(a)
  const ativa = ehStatusAtivo(r.status)
  // Leitor de QR só quando há validação disponível (simulação ou backend). No modo http ainda não há emissão de QR.
  const mostrarQr = r.status === 'EM_ATENDIMENTO' && (simulador !== null || pode('VALIDAR_IDENTIFICACAO'))

  return (
    <article aria-label={`Detalhes da solicitação ${r.protocolo}`} className={cn('flex flex-col overflow-hidden rounded-xl border bg-card shadow-sm', className)} data-testid="painel-detalhes">
      {/* Identificação do aluno */}
      <header className="bg-navy-800 px-5 pt-4 pb-5 text-offwhite">
        <div className="flex items-center justify-between gap-2 pr-10 xl:pr-0">
          <span className="font-mono text-xs text-offwhite/70">{r.protocolo}</span>
          <StatusBadge status={r.status} />
        </div>
        <div className="mt-3 flex items-center gap-3">
          <AvatarIniciais nome={r.aluno.nome} className="size-14 text-lg ring-2 ring-gold/60" />
          <div className="min-w-0">
            <p className="text-xs tracking-wide text-offwhite/60 uppercase">Aluno</p>
            <h2 className="truncate text-xl font-bold">{r.aluno.nome}</h2>
            <p className="text-sm text-offwhite/80">{r.aluno.turma}</p>
          </div>
        </div>
      </header>

      <Etapas retirada={r} />

      <div className="flex-1 space-y-4 overflow-y-auto px-5 py-4">
        {r.alertas.length > 0 && (
          <ul className="space-y-2" aria-label="Alertas para conferência">
            {r.alertas.map((a, i) => (
              <Alerta key={i} alerta={a} />
            ))}
          </ul>
        )}

        {mostrarQr && (
          <LeitorQrSimulado
            retirada={r}
            qrCodesDemonstracao={simulador ? simulador.listarQrCodes(r.id) : null}
            podeValidar={pode('VALIDAR_IDENTIFICACAO')}
            validando={acoes.validarIdentificacao.isPending}
            onValidar={(codigo) => acoes.validarIdentificacao.mutateAsync({ retirada: r, codigo }).catch(() => undefined)}
          />
        )}

        {/* Ações: somente as permitidas pelo backend */}
        <section aria-label="Ações do atendimento" className="space-y-2 rounded-xl border border-navy-800/15 bg-muted/60 p-3" data-testid="acoes">
          {pode('INICIAR_ATENDIMENTO') && (
            <Button size="lg" className="w-full" onClick={() => acoes.iniciarAtendimento.mutate(r)} disabled={acoes.iniciarAtendimento.isPending}>
              {acoes.iniciarAtendimento.isPending ? <Loader2 className="animate-spin" /> : <Play />}
              Iniciar atendimento
            </Button>
          )}
          {pode('CONFIRMAR_IDENTIFICACAO') && (
            <Button size="lg" className="w-full" onClick={() => setDialogo('identificacao')}>
              <UserRoundCheck /> Confirmar identificação
            </Button>
          )}
          {pode('CONFIRMAR_RETIRADA') && (
            <Button size="lg" variant="success" className="w-full" onClick={() => setDialogo('retirada')}>
              <LogOut /> Confirmar retirada do aluno
            </Button>
          )}
          {pode('REJEITAR') && (
            <Button
              variant="outline"
              className="w-full border-red-300 text-red-800 hover:bg-red-50 hover:text-red-900"
              onClick={() => setDialogo('rejeicao')}
            >
              <XCircle /> Identificação não confere — rejeitar
            </Button>
          )}
          {r.acoesPermitidas.length === 0 && <MensagemSemAcoes retirada={r} />}
        </section>

        <Secao titulo="Responsável solicitante">
          <p className="text-lg font-semibold leading-tight">{r.responsavel.nome}</p>
          <Dados
            itens={[
              ['Vínculo', r.responsavel.vinculo ?? 'Não informado'],
              [
                'Autorização',
                r.responsavel.tipoAutorizacao
                  ? (TIPO_AUTORIZACAO_ROTULO[r.responsavel.tipoAutorizacao] ?? r.responsavel.tipoAutorizacao)
                  : 'Não cadastrada',
              ],
              [
                'Validade',
                r.responsavel.autorizacaoValidaAte
                  ? `${dataCurta(r.responsavel.autorizacaoValidaAte)} às ${hora(r.responsavel.autorizacaoValidaAte)}`
                  : 'Sem prazo de expiração',
              ],
              ['Documento', <span key="doc" className="font-mono">{r.responsavel.documentoReferencia}</span>],
            ]}
          />
        </Secao>

        <Secao titulo="Solicitação">
          <Dados
            itens={[
              ['Solicitada às', hora(r.dtSolicitacao)],
              ['Chegada prevista', formatarChegada(r.chegadaPrevista, agora)],
              ativa ? ['Tempo de espera', <TempoEspera key="espera" desde={r.dtSolicitacao} agora={agora} />] : ['Encerrada às', hora(r.dtConclusao)],
              ['Atendido por', r.atendidoPor?.nome ?? '—'],
              ...(r.metodoIdentificacao
                ? ([['Identificação', r.metodoIdentificacao === 'QR_CODE' ? `QR Code · ${hora(r.dtIdentificacao)}` : `Documento · ${hora(r.dtIdentificacao)}`]] as Array<[string, ReactNode]>)
                : []),
            ]}
          />
        </Secao>

        {r.status === 'REJEITADA' && (
          <div className="rounded-lg border border-red-200 bg-red-50 p-3 text-sm text-red-950">
            <p className="font-semibold">Rejeitada: {r.motivoRejeicao ? (MOTIVO_REJEICAO_ROTULO[r.motivoRejeicao] ?? r.motivoRejeicao) : '—'}</p>
            <p className="mt-1">{r.justificativa}</p>
          </div>
        )}

      </div>


      <DialogConfirmarIdentificacao
        retirada={r}
        aberto={dialogo === 'identificacao'}
        onAbertoChange={(v) => setDialogo(v ? 'identificacao' : null)}
        enviando={acoes.confirmarIdentificacao.isPending}
        onConfirmar={(metodo) => acoes.confirmarIdentificacao.mutate({ retirada: r, metodo }, { onSuccess: () => setDialogo(null) })}
      />
      <DialogConfirmarRetirada
        retirada={r}
        aberto={dialogo === 'retirada'}
        onAbertoChange={(v) => setDialogo(v ? 'retirada' : null)}
        enviando={acoes.confirmarRetirada.isPending}
        onConfirmar={() => acoes.confirmarRetirada.mutate(r, { onSettled: () => setDialogo(null) })}
      />
      <DialogRejeicao
        retirada={r}
        aberto={dialogo === 'rejeicao'}
        onAbertoChange={(v) => setDialogo(v ? 'rejeicao' : null)}
        enviando={acoes.rejeitar.isPending}
        onRejeitar={async (motivo, justificativa) => {
          await acoes.rejeitar.mutateAsync({ retirada: r, motivo, justificativa })
          setDialogo(null)
        }}
      />
    </article>
  )
}

function MensagemSemAcoes({ retirada: r }: { retirada: RetiradaDTO }) {
  const textos: Partial<Record<RetiradaDTO['status'], [ReactNode, string]>> = {
    CONCLUIDA: [<CheckCircle2 key="i" className="size-5 text-emerald-700" />, `Aluno liberado às ${hora(r.dtConclusao)} por ${r.atendidoPor?.nome ?? '—'}.`],
    REJEITADA: [<XCircle key="i" className="size-5 text-red-700" />, `Solicitação rejeitada às ${hora(r.dtConclusao)}. O aluno não foi liberado.`],
    CANCELADA: [<X key="i" className="size-5 text-slate-600" />, `Cancelada pelo responsável às ${hora(r.dtConclusao)}.`],
  }
  const [icone, texto] = textos[r.status] ?? [
    <AlertOctagon key="i" className="size-5 text-red-700" />,
    'Nenhuma ação disponível para esta solicitação. Acione a coordenação.',
  ]
  return (
    <p className="flex items-center gap-2 text-sm font-medium" role="status">
      {icone}
      {texto}
    </p>
  )
}

function Etapas({ retirada: r }: { retirada: RetiradaDTO }) {
  const indice = { AGUARDANDO: 0, EM_ATENDIMENTO: 1, IDENTIFICADO: 2, CONCLUIDA: 3, REJEITADA: 1, CANCELADA: -1 }[r.status]
  const etapas = ['Atendimento', 'Identificação', 'Saída']
  if (r.status === 'CANCELADA') return null
  return (
    <ol className="grid grid-cols-3 border-b bg-card text-xs font-medium" aria-label="Etapas do atendimento">
      {etapas.map((e, i) => {
        const falhou = r.status === 'REJEITADA' && i === 1
        const feita = i < indice && !falhou
        const atual = i === indice && !falhou && r.status !== 'CONCLUIDA'
        return (
          <li
            key={e}
            aria-current={atual ? 'step' : undefined}
            className={cn(
              'flex items-center justify-center gap-1.5 border-b-2 py-2.5',
              feita && 'border-emerald-600 text-emerald-800',
              atual && 'border-gold text-navy-900 font-semibold',
              falhou && 'border-red-600 text-red-800',
              !feita && !atual && !falhou && 'border-transparent text-muted-foreground',
            )}
          >
            <span
              className={cn(
                'flex size-5 items-center justify-center rounded-full text-[11px] font-bold',
                feita ? 'bg-emerald-600 text-white' : falhou ? 'bg-red-600 text-white' : atual ? 'bg-gold text-navy-900' : 'bg-muted',
              )}
            >
              {feita ? <Check className="size-3" /> : falhou ? <X className="size-3" /> : i + 1}
            </span>
            {e}
          </li>
        )
      })}
    </ol>
  )
}

function Alerta({ alerta }: { alerta: AlertaConferenciaDTO }) {
  const estilo = {
    critico: ['border-red-300 bg-red-50 text-red-950', <AlertOctagon key="i" className="size-4 text-red-700" />],
    atencao: ['border-amber-300 bg-amber-50 text-amber-950', <AlertTriangle key="i" className="size-4 text-amber-700" />],
    info: ['border-sky-200 bg-sky-50 text-sky-950', <Info key="i" className="size-4 text-sky-700" />],
  }[alerta.nivel] as [string, ReactNode]
  return (
    <li className={cn('flex gap-2 rounded-lg border px-3 py-2 text-sm', estilo[0])} data-nivel={alerta.nivel}>
      <span className="mt-0.5 shrink-0">{estilo[1]}</span>
      {alerta.mensagem}
    </li>
  )
}

function Secao({ titulo, children }: { titulo: string; children: ReactNode }) {
  return (
    <section>
      <h3 className="mb-2 text-xs font-semibold tracking-wide text-muted-foreground uppercase">{titulo}</h3>
      {children}
    </section>
  )
}

function Dados({ itens }: { itens: Array<[string, ReactNode]> }) {
  return (
    <dl className="mt-2 grid grid-cols-[minmax(0,9rem)_1fr] gap-x-3 gap-y-1.5 text-sm">
      {itens.map(([k, v]) => (
        <div key={k} className="contents">
          <dt className="text-muted-foreground">{k}</dt>
          <dd className="font-medium">{v}</dd>
        </div>
      ))}
    </dl>
  )
}
