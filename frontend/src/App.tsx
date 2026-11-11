import { LoaderCircle } from 'lucide-react'
import { LoginPage } from '@/features/auth/LoginPage'
import { PortariaPage } from '@/features/portaria/PortariaPage'
import { useAutenticacao } from '@/hooks/useAutenticacao'

export default function App() {
  const auth = useAutenticacao()

  if (auth.estado === 'verificando') {
    return (
      <div className="flex min-h-full items-center justify-center gap-2 bg-navy-900 text-offwhite" role="status">
        <LoaderCircle className="size-5 animate-spin" aria-hidden /> Verificando sessão…
      </div>
    )
  }
  if (auth.estado === 'anonimo') {
    return <LoginPage onEntrar={auth.entrar} sessaoExpirada={auth.sessaoExpirada} />
  }
  return <PortariaPage onSair={auth.sair} />
}
