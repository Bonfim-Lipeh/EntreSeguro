import { FlaskConical, LogOut, PlugZap, RefreshCw, ShieldCheck, Unplug, UserMinus, UserPlus, Wifi, WifiOff } from 'lucide-react'
import { Button } from '@/components/ui/button'
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu'
import { cn } from '@/lib/utils'
import type { SessaoPortariaDTO } from '@/domain/retirada'
import { dataPorExtenso, horaComSegundos } from '../presentation/formatters'
import { AvatarIniciais } from './AvatarIniciais'

export type EstadoConexao = 'online' | 'reconectando' | 'offline'

interface Props {
  sessao: SessaoPortariaDTO | undefined
  agora: number
  conexao: EstadoConexao
  tempoReal: boolean
  atualizando: boolean
  onAtualizar: () => void
  /** Presente somente no modo HTTP (sessão autenticada). */
  onSair?: (() => void) | null
  demonstracao: null | {
    online: boolean
    onNovaSolicitacao: () => void
    onCancelamento: () => void
    onAlternarConexao: () => void
  }
}

const CONEXAO = {
  online: { rotulo: 'Conectado', icone: Wifi, classe: 'bg-emerald-500/15 text-emerald-200 ring-emerald-400/40', ponto: 'bg-emerald-400' },
  reconectando: { rotulo: 'Reconectando…', icone: Wifi, classe: 'bg-amber-500/15 text-amber-100 ring-amber-400/40', ponto: 'bg-amber-400 animate-pulse' },
  offline: { rotulo: 'Sem conexão', icone: WifiOff, classe: 'bg-red-500/20 text-red-100 ring-red-400/50', ponto: 'bg-red-400' },
}

export function Cabecalho({ sessao, agora, conexao, tempoReal, atualizando, onAtualizar, onSair, demonstracao }: Props) {
  const c = CONEXAO[conexao]
  const IconeConexao = c.icone
  const data = new Date(agora)

  return (
    <header className="bg-navy-900 text-offwhite shadow-md">
      <div className="mx-auto flex max-w-[1600px] flex-wrap items-center gap-x-6 gap-y-3 px-4 py-3 lg:px-6">
        {/* Marca e instituição */}
        <div className="flex min-w-0 items-center gap-3">
          <span className="flex size-10 items-center justify-center rounded-lg bg-navy-700 ring-1 ring-gold/50">
            <ShieldCheck className="size-6 text-gold" aria-hidden />
          </span>
          <div className="min-w-0 leading-tight">
            <p className="text-lg font-bold tracking-tight">
              Entrada<span className="text-gold">Segura</span>
            </p>
            <p className="truncate text-xs text-offwhite/70">
              {sessao ? `${sessao.escola.nome} · ${sessao.posto}` : 'Carregando instituição…'}
            </p>
          </div>
        </div>

        {demonstracao && (
          <span
            className="rounded-md border border-gold/70 bg-gold/10 px-2 py-1 text-[11px] font-bold tracking-wider text-gold uppercase"
            title="Dados fictícios e operações simuladas. Nenhuma informação real é utilizada."
          >
            Modo demonstração
          </span>
        )}

        <div className="ml-auto flex flex-wrap items-center gap-x-5 gap-y-3">
          {/* Data e hora */}
          <div className="text-right leading-tight" aria-live="off">
            <p className="font-mono text-xl font-semibold tabular-nums">{horaComSegundos(data)}</p>
            <p className="text-xs text-offwhite/70">{dataPorExtenso(data)}</p>
          </div>

          {/* Conexão */}
          <div
            role="status"
            className={cn('flex items-center gap-2 rounded-full px-3 py-1.5 text-xs font-semibold ring-1', c.classe)}
            title={tempoReal ? 'Atualização em tempo real' : 'Atualização periódica'}
          >
            <span className={cn('size-2 rounded-full', c.ponto)} aria-hidden />
            <IconeConexao className="size-3.5" aria-hidden />
            {c.rotulo}
          </div>

          {/* Funcionário */}
          {sessao && (
            <div className="flex items-center gap-2.5">
              <AvatarIniciais nome={sessao.funcionario.nome} className="size-9 bg-offwhite text-navy-900" />
              <div className="leading-tight">
                <p className="text-sm font-semibold">{sessao.funcionario.nome}</p>
                <p className="text-xs text-offwhite/70">{sessao.funcionario.funcao}</p>
              </div>
            </div>
          )}

          <Button
            variant="outline"
            onClick={onAtualizar}
            disabled={atualizando}
            className="border-offwhite/30 bg-transparent text-offwhite hover:bg-navy-700 hover:text-offwhite"
          >
            <RefreshCw className={cn(atualizando && 'animate-spin')} aria-hidden />
            Atualizar fila
          </Button>

          {onSair && (
            <Button
              variant="ghost"
              size="icon"
              onClick={onSair}
              className="text-offwhite hover:bg-navy-700 hover:text-offwhite"
              aria-label="Sair"
              title="Sair"
            >
              <LogOut aria-hidden />
            </Button>
          )}

          {demonstracao && (
            <DropdownMenu>
              <DropdownMenuTrigger asChild>
                <Button
                  variant="ghost"
                  size="icon"
                  className="text-gold hover:bg-navy-700 hover:text-gold"
                  aria-label="Controles da demonstração"
                >
                  <FlaskConical aria-hidden />
                </Button>
              </DropdownMenuTrigger>
              <DropdownMenuContent align="end">
                <DropdownMenuLabel>Controles da demonstração</DropdownMenuLabel>
                <DropdownMenuItem onSelect={demonstracao.onNovaSolicitacao}>
                  <UserPlus /> Simular nova solicitação
                </DropdownMenuItem>
                <DropdownMenuItem onSelect={demonstracao.onCancelamento}>
                  <UserMinus /> Simular cancelamento pelo responsável
                </DropdownMenuItem>
                <DropdownMenuSeparator />
                <DropdownMenuItem onSelect={demonstracao.onAlternarConexao}>
                  {demonstracao.online ? <Unplug /> : <PlugZap />}
                  {demonstracao.online ? 'Simular perda de conexão' : 'Restabelecer conexão'}
                </DropdownMenuItem>
              </DropdownMenuContent>
            </DropdownMenu>
          )}
        </div>
      </div>
    </header>
  )
}
