import { useMutation, useQueryClient } from '@tanstack/react-query'
import { toast } from 'sonner'
import type { MetodoIdentificacao, MotivoRejeicao, RetiradaDTO } from '@/domain/retirada'
import { ApiError, mensagemDeErro } from '@/services'
import { useServicos } from './ServicosContext'
import { portariaKeys } from './queryKeys'

/**
 * Ações do funcionário sobre uma solicitação. Após cada ação a fila, os
 * indicadores e o histórico são recarregados a partir do servidor (fonte de
 * verdade), sem recalcular estado no cliente.
 */
export function useAcoesRetirada() {
  const { retiradas } = useServicos()
  const qc = useQueryClient()

  const aposSucesso = (r: RetiradaDTO) => {
    qc.setQueryData(portariaKeys.retirada(r.id), r)
    return qc.invalidateQueries({ queryKey: portariaKeys.raiz })
  }
  const aposErro = (erro: unknown) => {
    toast.error(mensagemDeErro(erro))
    // Conflitos (409) indicam dados desatualizados: recarrega tudo.
    if (erro instanceof ApiError && erro.status === 409) void qc.invalidateQueries({ queryKey: portariaKeys.raiz })
  }

  const iniciarAtendimento = useMutation({
    mutationFn: (r: RetiradaDTO) => retiradas.iniciarAtendimento(r.id, r.versao),
    onSuccess: (r) => {
      toast.success(`Atendimento iniciado: ${r.aluno.nome}`, { description: 'Localize o aluno e confira o responsável.' })
      return aposSucesso(r)
    },
    onError: aposErro,
  })

  const validarIdentificacao = useMutation({
    mutationFn: ({ retirada, codigo }: { retirada: RetiradaDTO; codigo: string }) =>
      retiradas.validarIdentificacao(retirada.id, codigo),
    onSuccess: (v) => {
      if (v.valido) toast.success('QR Code válido para esta solicitação')
      else toast.error('QR Code não confere', { description: v.mensagem })
      return aposSucesso(v.retirada)
    },
    onError: aposErro,
  })

  const confirmarIdentificacao = useMutation({
    mutationFn: ({ retirada, metodo }: { retirada: RetiradaDTO; metodo: MetodoIdentificacao }) =>
      retiradas.confirmarIdentificacao(retirada.id, { metodo, versao: retirada.versao }),
    onSuccess: (r) => {
      toast.success('Identificação confirmada', { description: `${r.responsavel.nome} identificado(a).` })
      return aposSucesso(r)
    },
    onError: aposErro,
  })

  const confirmarRetirada = useMutation({
    mutationFn: (r: RetiradaDTO) => retiradas.confirmarRetirada(r.id, r.versao),
    onSuccess: (r) => {
      toast.success(`Retirada concluída: ${r.aluno.nome}`, { description: `Saída registrada com ${r.responsavel.nome}.` })
      return aposSucesso(r)
    },
    onError: aposErro,
  })

  const rejeitar = useMutation({
    mutationFn: ({ retirada, motivo, justificativa }: { retirada: RetiradaDTO; motivo: MotivoRejeicao; justificativa: string }) =>
      retiradas.rejeitar(retirada.id, { motivo, justificativa, versao: retirada.versao }),
    onSuccess: (r) => {
      toast.warning(`Solicitação rejeitada: ${r.aluno.nome}`, { description: 'O responsável será informado com a justificativa.' })
      return aposSucesso(r)
    },
    onError: aposErro,
  })

  return { iniciarAtendimento, validarIdentificacao, confirmarIdentificacao, confirmarRetirada, rejeitar }
}
