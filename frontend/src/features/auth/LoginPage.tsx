import { useState, type FormEvent } from 'react'
import { LoaderCircle, LogIn, ShieldCheck } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { mensagemDeErro } from '@/services'

interface Props {
  onEntrar: (email: string, senha: string) => Promise<void>
  sessaoExpirada: boolean
}

/** Login do funcionário da portaria (modo HTTP). A autenticação é feita no backend. */
export function LoginPage({ onEntrar, sessaoExpirada }: Props) {
  const [email, setEmail] = useState('')
  const [senha, setSenha] = useState('')
  const [enviando, setEnviando] = useState(false)
  const [erro, setErro] = useState<string | null>(null)

  async function enviar(e: FormEvent) {
    e.preventDefault()
    setEnviando(true)
    setErro(null)
    try {
      await onEntrar(email.trim(), senha)
    } catch (err) {
      setErro(mensagemDeErro(err))
      setEnviando(false)
    }
  }

  return (
    <div className="flex min-h-full items-center justify-center bg-navy-900 px-4 py-10">
      <form
        onSubmit={enviar}
        className="w-full max-w-sm space-y-5 rounded-xl bg-offwhite p-6 shadow-xl ring-1 ring-gold/40"
        aria-labelledby="titulo-login"
      >
        <div className="flex items-center gap-3">
          <span className="flex size-10 items-center justify-center rounded-lg bg-navy-900 ring-1 ring-gold/50">
            <ShieldCheck className="size-6 text-gold" aria-hidden />
          </span>
          <div className="leading-tight">
            <h1 id="titulo-login" className="text-lg font-bold tracking-tight text-navy-900">
              Entrada<span className="text-gold">Segura</span>
            </h1>
            <p className="text-xs text-navy-700">Painel da portaria · acesso do funcionário</p>
          </div>
        </div>

        {sessaoExpirada && !erro && (
          <p role="status" className="rounded-md bg-amber-100 px-3 py-2 text-sm text-amber-900">
            Sua sessão expirou. Entre novamente para continuar.
          </p>
        )}
        {erro && (
          <p role="alert" className="rounded-md bg-red-100 px-3 py-2 text-sm text-red-800">
            {erro}
          </p>
        )}

        <div className="space-y-1.5">
          <Label htmlFor="email">E-mail</Label>
          <Input
            id="email"
            type="email"
            autoComplete="username"
            required
            value={email}
            onChange={(e) => setEmail(e.target.value)}
          />
        </div>
        <div className="space-y-1.5">
          <Label htmlFor="senha">Senha</Label>
          <Input
            id="senha"
            type="password"
            autoComplete="current-password"
            required
            value={senha}
            onChange={(e) => setSenha(e.target.value)}
          />
        </div>

        <Button type="submit" className="w-full" disabled={enviando}>
          {enviando ? <LoaderCircle className="animate-spin" aria-hidden /> : <LogIn aria-hidden />}
          Entrar
        </Button>
      </form>
    </div>
  )
}
