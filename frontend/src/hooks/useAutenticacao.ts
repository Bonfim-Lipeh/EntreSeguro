import { useCallback, useEffect, useState } from 'react'
import { useQueryClient } from '@tanstack/react-query'
import { useServicos } from './ServicosContext'

export type EstadoAutenticacao = 'verificando' | 'anonimo' | 'autenticado'

/**
 * Estado de login do painel. No modo demonstração (sem serviço de autenticação)
 * o painel fica sempre liberado, como antes.
 */
export function useAutenticacao() {
  const { autenticacao } = useServicos()
  const qc = useQueryClient()
  const [estado, setEstado] = useState<EstadoAutenticacao>(autenticacao ? 'verificando' : 'autenticado')
  const [sessaoExpirada, setSessaoExpirada] = useState(false)

  // Recupera a sessão pelo cookie de refresh ao abrir/recarregar a página.
  useEffect(() => {
    if (!autenticacao) return
    let ativo = true
    autenticacao
      .restaurarSessao()
      .then((ok) => ativo && setEstado(ok ? 'autenticado' : 'anonimo'))
      .catch(() => ativo && setEstado('anonimo'))
    return () => {
      ativo = false
    }
  }, [autenticacao])

  // Refresh recusado durante o uso: volta para o login e descarta os dados em cache.
  useEffect(() => {
    if (!autenticacao) return
    return autenticacao.aoExpirarSessao(() => {
      qc.clear()
      setSessaoExpirada(true)
      setEstado('anonimo')
    })
  }, [autenticacao, qc])

  const entrar = useCallback(
    async (email: string, senha: string) => {
      if (!autenticacao) return
      await autenticacao.entrar(email, senha)
      setSessaoExpirada(false)
      setEstado('autenticado')
    },
    [autenticacao],
  )

  const sair = useCallback(async () => {
    if (!autenticacao) return
    await autenticacao.sair()
    qc.clear()
    setEstado('anonimo')
  }, [autenticacao, qc])

  return { estado, sessaoExpirada, entrar, sair: autenticacao ? sair : null }
}
