import { useId, useState, type FormEvent } from 'react'
import { AlertTriangle, CheckCircle2, Loader2, LogOut, UserRoundCheck, XCircle } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from '@/components/ui/dialog'
import { Label } from '@/components/ui/label'
import { Textarea } from '@/components/ui/textarea'
import { JUSTIFICATIVA_MIN, type MetodoIdentificacao, type MotivoRejeicao, type RetiradaDTO } from '@/domain/retirada'
import { ApiError } from '@/services'
import { MOTIVO_REJEICAO_ROTULO } from '../presentation/statusConfig'

interface Base {
  retirada: RetiradaDTO
  aberto: boolean
  onAbertoChange: (v: boolean) => void
  enviando: boolean
}

// ------------------------------------------------- Confirmar identificação

export function DialogConfirmarIdentificacao({
  retirada,
  aberto,
  onAbertoChange,
  enviando,
  onConfirmar,
}: Base & { onConfirmar: (metodo: MetodoIdentificacao) => void }) {
  const [conferido, setConferido] = useState(false)
  const id = useId()
  const metodo: MetodoIdentificacao = retirada.qrCodeValidado ? 'QR_CODE' : 'DOCUMENTO'
  const r = retirada.responsavel

  return (
    <Dialog
      open={aberto}
      onOpenChange={(v) => {
        if (!v) setConferido(false)
        onAbertoChange(v)
      }}
    >
      <DialogContent>
        <DialogHeader>
          <DialogTitle className="flex items-center gap-2">
            <UserRoundCheck className="size-5 text-navy-700" /> Confirmar identificação
          </DialogTitle>
          <DialogDescription>
            {metodo === 'QR_CODE'
              ? 'O QR Code foi validado. Confirme visualmente que a pessoa presente é o responsável cadastrado.'
              : 'Nenhum QR Code foi validado. A identificação será registrada por conferência de documento.'}
          </DialogDescription>
        </DialogHeader>

        <dl className="grid grid-cols-[auto_1fr] gap-x-4 gap-y-1.5 rounded-lg bg-muted p-3 text-sm">
          <dt className="text-muted-foreground">Responsável</dt>
          <dd className="font-semibold">{r.nome}</dd>
          <dt className="text-muted-foreground">Vínculo</dt>
          <dd>{r.vinculo ?? 'Não informado'}</dd>
          <dt className="text-muted-foreground">Documento</dt>
          <dd className="font-mono">{r.documentoReferencia}</dd>
          <dt className="text-muted-foreground">Método</dt>
          <dd>{metodo === 'QR_CODE' ? 'QR Code + conferência visual' : 'Documento oficial com foto'}</dd>
        </dl>

        <label htmlFor={id} className="flex cursor-pointer items-start gap-3 rounded-lg border p-3 text-sm has-[:checked]:border-emerald-400 has-[:checked]:bg-emerald-50">
          <input id={id} type="checkbox" checked={conferido} onChange={(e) => setConferido(e.target.checked)} className="mt-0.5 size-4 accent-emerald-700" />
          <span>Conferi o documento com foto e a pessoa presente corresponde ao responsável cadastrado.</span>
        </label>

        <DialogFooter>
          <Button variant="outline" onClick={() => onAbertoChange(false)}>
            Voltar
          </Button>
          <Button onClick={() => onConfirmar(metodo)} disabled={!conferido || enviando}>
            {enviando ? <Loader2 className="animate-spin" /> : <CheckCircle2 />}
            Confirmar identificação
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}

// ------------------------------------------------------- Confirmar retirada

export function DialogConfirmarRetirada({ retirada, aberto, onAbertoChange, enviando, onConfirmar }: Base & { onConfirmar: () => void }) {
  return (
    <Dialog open={aberto} onOpenChange={onAbertoChange}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle className="flex items-center gap-2">
            <LogOut className="size-5 text-emerald-700" /> Confirmar saída do aluno
          </DialogTitle>
          <DialogDescription>Esta operação registra a movimentação com seu nome e horário e não pode ser desfeita.</DialogDescription>
        </DialogHeader>
        <div className="rounded-lg border border-emerald-200 bg-emerald-50 p-4 text-sm text-emerald-950">
          <p>
            <strong>{retirada.aluno.nome}</strong> ({retirada.aluno.turma})
          </p>
          <p className="mt-1">
            será liberado(a) com <strong>{retirada.responsavel.nome}</strong>{retirada.responsavel.vinculo ? ` — ${retirada.responsavel.vinculo}` : ''}.
          </p>
        </div>
        <DialogFooter>
          <Button variant="outline" onClick={() => onAbertoChange(false)}>
            Voltar
          </Button>
          <Button variant="success" onClick={onConfirmar} disabled={enviando}>
            {enviando ? <Loader2 className="animate-spin" /> : <CheckCircle2 />}
            Confirmar saída
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}

// ------------------------------------------------------------------ Rejeitar

const MOTIVOS: MotivoRejeicao[] = ['IDENTIDADE_NAO_CONFERE', 'DOCUMENTACAO_INCOMPLETA', 'RESPONSAVEL_INDISPONIVEL', 'AUTORIZACAO_EXPIRADA', 'OUTRO']

export function DialogRejeicao({
  retirada,
  aberto,
  onAbertoChange,
  enviando,
  onRejeitar,
}: Base & { onRejeitar: (motivo: MotivoRejeicao, justificativa: string) => Promise<unknown> }) {
  const sugestao: MotivoRejeicao = retirada.alertas.some((a) => a.nivel === 'critico') ? 'AUTORIZACAO_EXPIRADA' : 'IDENTIDADE_NAO_CONFERE'
  const [motivo, setMotivo] = useState<MotivoRejeicao>(sugestao)
  const [justificativa, setJustificativa] = useState('')
  const [tentou, setTentou] = useState(false)
  const [erroServidor, setErroServidor] = useState<string | null>(null)
  const idMotivo = useId()
  const idJust = useId()
  const idErro = useId()

  const tamanho = justificativa.trim().length
  const invalida = tamanho < JUSTIFICATIVA_MIN
  const erro = erroServidor ?? (tentou && invalida ? `Informe a justificativa (mínimo de ${JUSTIFICATIVA_MIN} caracteres).` : null)

  const limpar = () => {
    setMotivo(sugestao)
    setJustificativa('')
    setTentou(false)
    setErroServidor(null)
  }

  const enviar = async (e: FormEvent) => {
    e.preventDefault()
    setTentou(true)
    setErroServidor(null)
    if (invalida) return
    try {
      await onRejeitar(motivo, justificativa.trim())
      limpar()
    } catch (err) {
      if (err instanceof ApiError && err.status === 422) setErroServidor(err.message)
    }
  }

  return (
    <Dialog
      open={aberto}
      onOpenChange={(v) => {
        if (!v) limpar()
        onAbertoChange(v)
      }}
    >
      <DialogContent>
        <form onSubmit={enviar} className="grid gap-4" noValidate>
          <DialogHeader>
            <DialogTitle className="flex items-center gap-2">
              <XCircle className="size-5 text-destructive" /> Rejeitar solicitação
            </DialogTitle>
            <DialogDescription>
              Use quando a identificação de <strong>{retirada.responsavel.nome}</strong> não conferir. O aluno{' '}
              <strong>não</strong> será liberado e o responsável receberá a justificativa.
            </DialogDescription>
          </DialogHeader>

          <div className="grid gap-1.5">
            <Label htmlFor={idMotivo}>Motivo</Label>
            <select
              id={idMotivo}
              value={motivo}
              onChange={(e) => setMotivo(e.target.value as MotivoRejeicao)}
              className="h-9 rounded-md border border-input bg-card px-2 text-sm shadow-xs outline-none focus-visible:ring-[3px] focus-visible:ring-ring/60"
            >
              {MOTIVOS.map((m) => (
                <option key={m} value={m}>
                  {MOTIVO_REJEICAO_ROTULO[m]}
                </option>
              ))}
            </select>
          </div>

          <div className="grid gap-1.5">
            <div className="flex items-baseline justify-between">
              <Label htmlFor={idJust}>
                Justificativa <span className="text-destructive">*</span>
              </Label>
              <span className="text-xs tabular-nums text-muted-foreground">
                {tamanho}/{JUSTIFICATIVA_MIN} mín.
              </span>
            </div>
            <Textarea
              id={idJust}
              value={justificativa}
              onChange={(e) => {
                setJustificativa(e.target.value)
                setErroServidor(null)
              }}
              placeholder="Descreva o que foi observado na conferência."
              aria-invalid={!!erro}
              aria-describedby={erro ? idErro : undefined}
              maxLength={500}
              autoFocus
            />
            {erro && (
              <p id={idErro} role="alert" className="flex items-center gap-1.5 text-sm font-medium text-destructive">
                <AlertTriangle className="size-4" /> {erro}
              </p>
            )}
          </div>

          <DialogFooter>
            <Button type="button" variant="outline" onClick={() => onAbertoChange(false)}>
              Voltar
            </Button>
            <Button type="submit" variant="destructive" disabled={enviando}>
              {enviando ? <Loader2 className="animate-spin" /> : <XCircle />}
              Rejeitar solicitação
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  )
}
