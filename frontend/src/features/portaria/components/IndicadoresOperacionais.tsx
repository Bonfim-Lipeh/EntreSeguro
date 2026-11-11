import { CheckCircle2, Clock, ScanFace, XCircle, type LucideIcon } from 'lucide-react'
import { cn } from '@/lib/utils'
import type { ResumoPortariaDTO } from '@/domain/retirada'
import type { FiltroId } from '../presentation/statusConfig'
import { formatarDuracao, minutosDesde } from '../presentation/formatters'

interface Indicador {
  filtro: FiltroId
  rotulo: string
  valor: number
  detalhe: string
  icone: LucideIcon
  tom: string
  destaque?: boolean
}

interface Props {
  resumo: ResumoPortariaDTO | undefined
  carregando: boolean
  agora: number
  filtroAtivo: FiltroId
  onSelecionar: (f: FiltroId) => void
}

export function IndicadoresOperacionais({ resumo, carregando, agora, filtroAtivo, onSelecionar }: Props) {
  const r = resumo
  const espera = r?.aguardandoDesde ? minutosDesde(r.aguardandoDesde, agora) : null

  const itens: Indicador[] = [
    {
      filtro: 'AGUARDANDO',
      rotulo: 'Aguardando atendimento',
      valor: r?.aguardando ?? 0,
      detalhe: espera === null ? 'Nenhuma solicitação pendente' : `Mais antiga aguarda há ${formatarDuracao(espera)}`,
      icone: Clock,
      tom: 'bg-amber-100 text-amber-800',
      destaque: espera !== null && espera >= 10,
    },
    {
      filtro: 'EM_ANDAMENTO',
      rotulo: 'Em atendimento',
      valor: (r?.emAtendimento ?? 0) + (r?.identificados ?? 0),
      detalhe: `${r?.identificados ?? 0} já identificado(s), aguardando saída`,
      icone: ScanFace,
      tom: 'bg-sky-100 text-sky-800',
    },
    {
      filtro: 'CONCLUIDAS',
      rotulo: 'Retiradas concluídas hoje',
      valor: r?.concluidasHoje ?? 0,
      detalhe: 'Saídas registradas no dia',
      icone: CheckCircle2,
      tom: 'bg-emerald-100 text-emerald-800',
    },
    {
      filtro: 'ENCERRADAS_SEM_SAIDA',
      rotulo: 'Rejeitadas ou canceladas',
      valor: (r?.rejeitadasHoje ?? 0) + (r?.canceladasHoje ?? 0),
      detalhe: `${r?.rejeitadasHoje ?? 0} rejeitada(s) · ${r?.canceladasHoje ?? 0} cancelada(s)`,
      icone: XCircle,
      tom: 'bg-red-100 text-red-800',
    },
  ]

  return (
    <section aria-label="Indicadores operacionais" className="grid grid-cols-2 gap-3 xl:grid-cols-4">
      {itens.map((i) => {
        const Icone = i.icone
        const ativo = filtroAtivo === i.filtro
        return (
          <button
            key={i.filtro}
            type="button"
            onClick={() => onSelecionar(ativo ? 'ATIVAS' : i.filtro)}
            aria-pressed={ativo}
            className={cn(
              'group flex items-start gap-3 rounded-xl border bg-card p-4 text-left shadow-sm transition hover:border-navy-700/40 hover:shadow focus-visible:ring-[3px] focus-visible:ring-ring/60 outline-none',
              ativo && 'border-navy-800 ring-2 ring-navy-800/80',
              i.destaque && !ativo && 'border-red-300',
            )}
          >
            <span className={cn('flex size-10 shrink-0 items-center justify-center rounded-lg', i.tom)}>
              <Icone className="size-5" aria-hidden />
            </span>
            <span className="min-w-0 flex-1">
              <span className="block text-sm font-medium text-muted-foreground">{i.rotulo}</span>
              <span className="mt-0.5 block text-3xl font-bold tabular-nums text-navy-900" data-testid={`indicador-${i.filtro}`}>
                {carregando ? <span className="inline-block h-8 w-10 animate-pulse rounded bg-muted" /> : i.valor}
              </span>
              <span className={cn('mt-1 block truncate text-xs text-muted-foreground', i.destaque && 'font-semibold text-red-700')}>
                {i.detalhe}
              </span>
            </span>
          </button>
        )
      })}
    </section>
  )
}
