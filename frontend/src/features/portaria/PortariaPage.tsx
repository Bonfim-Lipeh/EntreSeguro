import { useState } from 'react'
import { useQueryClient } from '@tanstack/react-query'
import { toast } from 'sonner'
import { Sheet, SheetContent, SheetDescription, SheetTitle } from '@/components/ui/sheet'
import type { FiltroFila } from '@/domain/retirada'
import { NetworkError, mensagemDeErro } from '@/services'
import { useServicos } from '@/hooks/ServicosContext'
import { useAgora } from '@/hooks/useAgora'
import { useDebounce } from '@/hooks/useDebounce'
import { useEventosPortaria } from '@/hooks/useEventosPortaria'
import { useMediaQuery } from '@/hooks/useMediaQuery'
import { useFila, useHistorico, useResumo, useRetirada, useSessao } from '@/hooks/usePortariaQueries'
import { portariaKeys } from '@/hooks/queryKeys'
import { statusDoFiltro, type FiltroId } from './presentation/statusConfig'
import { Cabecalho, type EstadoConexao } from './components/Cabecalho'
import { IndicadoresOperacionais } from './components/IndicadoresOperacionais'
import { FilaRetirada } from './components/FilaRetirada'
import { PainelDetalhes } from './components/PainelDetalhes'
import { HistoricoMovimentacoes } from './components/HistoricoMovimentacoes'

export function PortariaPage({ onSair = null }: { onSair?: (() => Promise<void>) | null }) {
  const { retiradas, simulador } = useServicos()
  const qc = useQueryClient()
  const agora = useAgora(1000)
  const desktop = useMediaQuery('(min-width: 1200px)')

  const [filtroId, setFiltroId] = useState<FiltroId>('ATIVAS')
  const [busca, setBusca] = useState('')
  const [selecionadaId, setSelecionadaId] = useState<number | null>(null)
  const [online, setOnline] = useState(true)
  const buscaDebounced = useDebounce(busca, 250)

  const filtro: FiltroFila = { status: statusDoFiltro(filtroId), busca: buscaDebounced }
  const sessao = useSessao()
  const fila = useFila(filtro)
  const resumo = useResumo()
  const historico = useHistorico(10)
  const detalhe = useRetirada(selecionadaId)
  useEventosPortaria()

  const conexao: EstadoConexao =
    fila.error instanceof NetworkError || resumo.error instanceof NetworkError
      ? fila.isFetching || resumo.isFetching
        ? 'reconectando'
        : 'offline'
      : 'online'

  const atualizar = () => qc.invalidateQueries({ queryKey: portariaKeys.raiz })

  const retiradaSelecionada = detalhe.data ?? fila.data?.find((r) => r.id === selecionadaId)

  const painel = <PainelDetalhes retirada={retiradaSelecionada} carregando={detalhe.isLoading} agora={agora} className="h-full" />

  return (
    <div className="flex min-h-full flex-col">
      <Cabecalho
        sessao={sessao.data}
        agora={agora}
        conexao={conexao}
        tempoReal={retiradas.tempoReal}
        atualizando={fila.isFetching}
        onAtualizar={() => {
          void atualizar().then(() => toast.success('Fila atualizada'))
        }}
        onSair={onSair && (() => void onSair())}
        demonstracao={
          simulador && {
            online,
            onNovaSolicitacao: () => simulador.simularNovaSolicitacao(),
            onCancelamento: () => {
              if (!simulador.simularCancelamentoPeloResponsavel()) toast.info('Não há solicitações aguardando para cancelar.')
            },
            onAlternarConexao: () => {
              const novo = !simulador.estaOnline()
              simulador.definirConexao(novo)
              setOnline(novo)
              if (novo) toast.success('Conexão restabelecida')
              void atualizar()
            },
          }
        }
      />

      {conexao !== 'online' && (
        <div role="alert" className="bg-red-700 px-4 py-2 text-center text-sm font-semibold text-white">
          Sem conexão com o servidor. Os dados exibidos podem estar desatualizados — não confirme retiradas até a conexão
          voltar.
        </div>
      )}

      <main className="mx-auto w-full max-w-[1600px] flex-1 space-y-5 px-4 py-5 lg:px-6">
        <IndicadoresOperacionais
          resumo={resumo.data}
          carregando={resumo.isLoading}
          agora={agora}
          filtroAtivo={filtroId}
          onSelecionar={setFiltroId}
        />

        <div className="grid items-start gap-5 xl:grid-cols-[minmax(0,1fr)_minmax(400px,460px)]">
          <FilaRetirada
            itens={fila.data}
            carregando={fila.isLoading}
            erro={fila.error ? mensagemDeErro(fila.error) : null}
            atualizando={fila.isFetching}
            agora={agora}
            filtro={filtroId}
            onFiltro={setFiltroId}
            busca={busca}
            onBusca={setBusca}
            selecionadaId={selecionadaId}
            onSelecionar={setSelecionadaId}
            onTentarNovamente={() => void fila.refetch()}
          />
          {desktop && <aside className="sticky top-4 max-h-[calc(100vh-2rem)] min-h-[520px]">{painel}</aside>}
        </div>

        <HistoricoMovimentacoes
          itens={historico.data}
          carregando={historico.isLoading}
          erro={historico.error ? mensagemDeErro(historico.error) : null}
        />
      </main>

      {!desktop && (
        <Sheet open={selecionadaId !== null} onOpenChange={(v) => !v && setSelecionadaId(null)}>
          <SheetContent>
            <SheetTitle className="sr-only">Detalhes da solicitação</SheetTitle>
            <SheetDescription className="sr-only">Dados para conferência e ações de atendimento</SheetDescription>
            {painel}
          </SheetContent>
        </Sheet>
      )}

      <footer className="border-t px-6 py-3 text-center text-xs text-muted-foreground">
        EntradaSegura · Painel da portaria
        {simulador && ' · Modo demonstração: dados fictícios, armazenados apenas nesta sessão do navegador.'}
      </footer>
    </div>
  )
}
