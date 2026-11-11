import { History } from 'lucide-react'
import { Card, CardHeader, CardTitle } from '@/components/ui/card'
import type { MovimentacaoDTO } from '@/domain/retirada'
import { hora } from '../presentation/formatters'
import { StatusBadge } from './StatusBadge'

interface Props {
  itens: MovimentacaoDTO[] | undefined
  carregando: boolean
  erro: string | null
}

export function HistoricoMovimentacoes({ itens, carregando, erro }: Props) {
  return (
    <Card className="overflow-hidden">
      <CardHeader className="border-b">
        <History className="size-5 text-navy-700" aria-hidden />
        <div>
          <CardTitle className="text-lg">Histórico de movimentações</CardTitle>
          <p className="mt-1 text-xs text-muted-foreground">Últimas solicitações encerradas hoje</p>
        </div>
      </CardHeader>
      <div className="overflow-x-auto">
        <table className="w-full min-w-[720px] text-sm" data-testid="historico">
          <thead className="bg-muted/60 text-left text-xs font-semibold tracking-wide text-muted-foreground uppercase">
            <tr>
              <th scope="col" className="px-5 py-2">Horário</th>
              <th scope="col" className="px-3 py-2">Aluno</th>
              <th scope="col" className="px-3 py-2">Responsável</th>
              <th scope="col" className="px-3 py-2">Funcionário</th>
              <th scope="col" className="px-5 py-2">Resultado</th>
            </tr>
          </thead>
          <tbody>
            {carregando && !itens ? (
              <tr>
                <td colSpan={5} className="px-5 py-6 text-center text-muted-foreground">
                  Carregando histórico…
                </td>
              </tr>
            ) : erro && !itens ? (
              <tr>
                <td colSpan={5} className="px-5 py-6 text-center text-red-700">
                  {erro}
                </td>
              </tr>
            ) : !itens?.length ? (
              <tr>
                <td colSpan={5} className="px-5 py-6 text-center text-muted-foreground">
                  Nenhuma movimentação registrada hoje.
                </td>
              </tr>
            ) : (
              itens.map((m) => (
                <tr key={m.id} className="border-t align-top">
                  <td className="px-5 py-2.5 font-semibold tabular-nums">{hora(m.dataHora)}</td>
                  <td className="px-3 py-2.5">
                    <span className="font-medium">{m.alunoNome}</span>
                    <span className="block text-xs text-muted-foreground">{m.turma}</span>
                  </td>
                  <td className="px-3 py-2.5">{m.responsavelNome}</td>
                  <td className="px-3 py-2.5">{m.funcionarioNome ?? <span className="text-muted-foreground">Cancelado pelo responsável</span>}</td>
                  <td className="px-5 py-2.5">
                    <StatusBadge status={m.resultado} />
                    {m.justificativa && (
                      <span className="mt-1 block max-w-xs truncate text-xs text-muted-foreground" title={m.justificativa}>
                        {m.justificativa}
                      </span>
                    )}
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>
    </Card>
  )
}
