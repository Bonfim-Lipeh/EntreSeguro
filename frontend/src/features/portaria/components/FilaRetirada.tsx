import { useEffect, useRef, type ReactNode } from 'react'
import { AlertTriangle, Inbox, ListOrdered, RotateCw, Search, X } from 'lucide-react'
import { Card, CardHeader, CardTitle } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { cn } from '@/lib/utils'
import type { RetiradaDTO } from '@/domain/retirada'
import { FILTROS, ehStatusAtivo, type FiltroId } from '../presentation/statusConfig'
import { hora } from '../presentation/formatters'
import { AvatarIniciais } from './AvatarIniciais'
import { StatusBadge } from './StatusBadge'
import { TempoEspera } from './TempoEspera'

interface Props {
  itens: RetiradaDTO[] | undefined
  carregando: boolean
  erro: string | null
  atualizando: boolean
  agora: number
  filtro: FiltroId
  onFiltro: (f: FiltroId) => void
  busca: string
  onBusca: (b: string) => void
  selecionadaId: number | null
  onSelecionar: (id: number) => void
  onTentarNovamente: () => void
}

export function FilaRetirada(p: Props) {
  const buscaRef = useRef<HTMLInputElement>(null)

  // Atalho "/" para focar a pesquisa.
  useEffect(() => {
    const onKey = (e: KeyboardEvent) => {
      const alvo = e.target as HTMLElement
      if (e.key === '/' && !['INPUT', 'TEXTAREA', 'SELECT'].includes(alvo.tagName)) {
        e.preventDefault()
        buscaRef.current?.focus()
      }
    }
    window.addEventListener('keydown', onKey)
    return () => window.removeEventListener('keydown', onKey)
  }, [])

  // Posição na fila FIFO considerando apenas solicitações ativas.
  const posicoes = new Map<number, number>()
  p.itens?.filter((r) => ehStatusAtivo(r.status)).forEach((r, i) => posicoes.set(r.id, i + 1))

  return (
    <Card className="min-w-0 overflow-hidden">
      <CardHeader className="flex-wrap justify-between gap-3 border-b pb-4">
        <div className="flex items-center gap-2.5">
          <ListOrdered className="size-5 text-navy-700" aria-hidden />
          <div>
            <CardTitle className="text-lg">Fila de retirada</CardTitle>
            <p className="mt-1 text-xs text-muted-foreground">Ordem de chegada das solicitações (FIFO)</p>
          </div>
        </div>
        <div className="relative w-full sm:w-80">
          <Search className="pointer-events-none absolute top-1/2 left-3 size-4 -translate-y-1/2 text-muted-foreground" aria-hidden />
          <Input
            ref={buscaRef}
            value={p.busca}
            onChange={(e) => p.onBusca(e.target.value)}
            placeholder="Buscar aluno ou responsável"
            aria-label="Pesquisar por aluno, responsável ou protocolo"
            className="h-10 pr-16 pl-9"
          />
          {p.busca ? (
            <button
              type="button"
              onClick={() => p.onBusca('')}
              className="absolute top-1/2 right-2 -translate-y-1/2 rounded p-1 text-muted-foreground hover:text-foreground"
              aria-label="Limpar pesquisa"
            >
              <X className="size-4" />
            </button>
          ) : (
            <kbd className="absolute top-1/2 right-2.5 -translate-y-1/2 rounded border bg-muted px-1.5 font-mono text-[11px] text-muted-foreground">
              /
            </kbd>
          )}
        </div>

        <div role="radiogroup" aria-label="Filtrar por status" className="flex w-full flex-wrap gap-1.5">
          {FILTROS.map((f) => (
            <button
              key={f.id}
              type="button"
              role="radio"
              aria-checked={p.filtro === f.id}
              onClick={() => p.onFiltro(f.id)}
              className={cn(
                'rounded-full border px-3 py-1 text-sm font-medium transition outline-none focus-visible:ring-[3px] focus-visible:ring-ring/60',
                p.filtro === f.id
                  ? 'border-navy-800 bg-navy-800 text-offwhite'
                  : 'border-border bg-card text-foreground hover:bg-accent',
              )}
            >
              {f.rotulo}
            </button>
          ))}
        </div>
      </CardHeader>

      {/* Cabeçalho das colunas (desktop) */}
      <div className="hidden grid-cols-[2.5rem_minmax(0,1.3fr)_minmax(0,1.1fr)_4.5rem_5.5rem_8.5rem] items-center gap-3 border-b bg-muted/60 px-5 py-2 text-xs font-semibold tracking-wide text-muted-foreground uppercase md:grid">
        <span>#</span>
        <span>Aluno</span>
        <span>Responsável</span>
        <span>Horário</span>
        <span>Espera</span>
        <span>Status</span>
      </div>

      <div className={cn('relative transition-opacity', p.atualizando && !p.carregando && 'opacity-80')} aria-busy={p.carregando}>
        {p.carregando && !p.itens ? (
          <ul aria-label="Carregando fila">
            {Array.from({ length: 5 }).map((_, i) => (
              <li key={i} className="flex items-center gap-3 border-b px-5 py-4">
                <span className="size-10 animate-pulse rounded-full bg-muted" />
                <span className="h-4 flex-1 animate-pulse rounded bg-muted" />
              </li>
            ))}
          </ul>
        ) : p.erro && !p.itens ? (
          <EstadoVazio
            icone={<AlertTriangle className="size-8 text-red-600" />}
            titulo="Não foi possível carregar a fila"
            texto={p.erro}
            acao={
              <Button variant="outline" onClick={p.onTentarNovamente}>
                <RotateCw /> Tentar novamente
              </Button>
            }
          />
        ) : p.itens && p.itens.length === 0 ? (
          <EstadoVazio
            icone={<Inbox className="size-8 text-muted-foreground" />}
            titulo={p.busca ? 'Nenhum resultado para a pesquisa' : 'Nenhuma solicitação neste filtro'}
            texto={p.busca ? `Não há solicitações com "${p.busca}".` : 'Novas solicitações aparecerão aqui automaticamente.'}
          />
        ) : (
          <ul aria-label="Solicitações de retirada" data-testid="fila">
            {p.itens?.map((r) => {
              const ativa = ehStatusAtivo(r.status)
              const selecionada = r.id === p.selecionadaId
              const critico = r.alertas.some((a) => a.nivel === 'critico')
              return (
                <li key={r.id}>
                  <button
                    type="button"
                    onClick={() => p.onSelecionar(r.id)}
                    aria-pressed={selecionada}
                    aria-label={`${r.aluno.nome}, ${r.aluno.turma}, responsável ${r.responsavel.nome}`}
                    data-testid={`item-fila-${r.protocolo}`}
                    className={cn(
                      'grid w-full grid-cols-[2.5rem_minmax(0,1fr)_auto] items-center gap-x-3 gap-y-1.5 border-b border-l-4 border-l-transparent px-5 py-3 text-left transition outline-none hover:bg-accent/60 focus-visible:bg-accent focus-visible:ring-inset focus-visible:ring-[3px] focus-visible:ring-ring/50',
                      'md:grid-cols-[2.5rem_minmax(0,1.3fr)_minmax(0,1.1fr)_4.5rem_5.5rem_8.5rem]',
                      selecionada && 'border-l-gold bg-[#fbf6e4] hover:bg-[#f8f0d6]',
                      !ativa && 'text-muted-foreground',
                    )}
                  >
                    <span className="text-sm font-bold tabular-nums text-navy-700">{ativa ? `${posicoes.get(r.id)}º` : '—'}</span>

                    <span className="flex min-w-0 items-center gap-3">
                      <AvatarIniciais nome={r.aluno.nome} />
                      <span className="min-w-0">
                        <span className="flex items-center gap-1.5 truncate font-semibold text-foreground">
                          <span className="truncate">{r.aluno.nome}</span>
                          {critico && <AlertTriangle className="size-4 shrink-0 text-red-600" aria-label="Requer atenção" />}
                        </span>
                        <span className="block truncate text-xs text-muted-foreground">
                          {r.aluno.turma} · {r.protocolo}
                        </span>
                      </span>
                    </span>

                    <span className="col-start-2 min-w-0 md:col-start-auto">
                      <span className="block truncate text-sm font-medium text-foreground">{r.responsavel.nome}</span>
                      <span className="block truncate text-xs text-muted-foreground">{r.responsavel.vinculo ?? 'Vínculo não informado'}</span>
                    </span>

                    <span className="col-start-3 row-start-1 md:col-start-auto md:row-start-auto">
                      <StatusBadge status={r.status} className="md:hidden" />
                      <span className="hidden text-sm tabular-nums md:inline">{hora(r.dtSolicitacao)}</span>
                    </span>

                    <span className="col-start-3 row-start-2 justify-self-end text-sm md:col-start-auto md:row-start-auto md:justify-self-start">
                      {ativa ? <TempoEspera desde={r.dtSolicitacao} agora={p.agora} /> : <span className="tabular-nums">{hora(r.dtConclusao)}</span>}
                    </span>

                    <span className="hidden md:block">
                      <StatusBadge status={r.status} />
                    </span>
                  </button>
                </li>
              )
            })}
          </ul>
        )}
      </div>
    </Card>
  )
}

function EstadoVazio({ icone, titulo, texto, acao }: { icone: ReactNode; titulo: string; texto: string; acao?: ReactNode }) {
  return (
    <div className="flex flex-col items-center gap-2 px-6 py-14 text-center">
      {icone}
      <p className="font-semibold">{titulo}</p>
      <p className="max-w-sm text-sm text-muted-foreground">{texto}</p>
      {acao && <div className="mt-2">{acao}</div>}
    </div>
  )
}
