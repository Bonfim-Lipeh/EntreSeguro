import { useEffect } from 'react'
import { useQueryClient } from '@tanstack/react-query'
import { toast } from 'sonner'
import { useServicos } from './ServicosContext'
import { portariaKeys } from './queryKeys'

/** Assina os eventos de tempo real e recarrega os dados afetados. */
export function useEventosPortaria() {
  const { retiradas } = useServicos()
  const qc = useQueryClient()

  useEffect(
    () =>
      retiradas.assinarEventos((evento) => {
        void qc.invalidateQueries({ queryKey: portariaKeys.raiz })
        if (evento.tipo === 'RETIRADA_CRIADA') toast.info(`Nova solicitação na fila: ${evento.alunoNome}`)
        if (evento.tipo === 'RETIRADA_CANCELADA') toast.warning(`Solicitação cancelada pelo responsável: ${evento.alunoNome}`)
      }),
    [retiradas, qc],
  )
}
