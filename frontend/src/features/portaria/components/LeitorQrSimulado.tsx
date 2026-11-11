import { useState } from 'react'
import { CheckCircle2, Loader2, QrCode, ScanLine, ShieldAlert } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { cn } from '@/lib/utils'
import type { RetiradaDTO, ValidacaoIdentificacaoDTO } from '@/domain/retirada'
import type { QrCodeDemonstracao } from '@/services'

interface Props {
  retirada: RetiradaDTO
  /** Presente apenas no modo demonstração; sem ele, o código é digitado. */
  qrCodesDemonstracao: QrCodeDemonstracao[] | null
  podeValidar: boolean
  validando: boolean
  onValidar: (codigo: string) => Promise<ValidacaoIdentificacaoDTO | undefined>
}

/**
 * Área de identificação por QR Code. Nesta etapa não há acesso à câmera:
 * a leitura é simulada escolhendo um código fictício. O resultado da
 * validação vem do serviço e NUNCA conclui a retirada automaticamente.
 */
export function LeitorQrSimulado({ retirada, qrCodesDemonstracao, podeValidar, validando, onValidar }: Props) {
  const [codigo, setCodigo] = useState(qrCodesDemonstracao?.[0]?.codigo ?? '')
  const [resultado, setResultado] = useState<ValidacaoIdentificacaoDTO | null>(null)

  const ler = async () => {
    if (!codigo.trim()) return
    const r = await onValidar(codigo)
    if (r) setResultado(r)
  }

  // Após uma leitura válida, o leitor é encerrado e fica apenas o resultado.
  const jaValidado = retirada.qrCodeValidado

  return (
    <section aria-labelledby="titulo-qr" className="rounded-xl border bg-card p-4">
      <div className="flex items-center justify-between gap-2">
        <h3 id="titulo-qr" className="flex items-center gap-2 text-sm font-semibold">
          <QrCode className="size-4 text-navy-700" aria-hidden /> Identificação por QR Code
        </h3>
        <span className="rounded bg-muted px-1.5 py-0.5 text-[10px] font-bold tracking-wide text-muted-foreground uppercase">
          Leitura simulada
        </span>
      </div>

      {jaValidado ? (
        <ResultadoQr valido mensagem={resultado?.valido ? resultado.mensagem : 'QR Code já validado para esta solicitação.'} />
      ) : (
        <>
          <div className="mt-3 flex gap-4">
            {/* Visor ilustrativo */}
            <div className="relative hidden size-24 shrink-0 items-center justify-center rounded-lg bg-navy-900 sm:flex" aria-hidden>
              <span className="absolute top-2 left-2 size-4 border-t-2 border-l-2 border-gold" />
              <span className="absolute top-2 right-2 size-4 border-t-2 border-r-2 border-gold" />
              <span className="absolute bottom-2 left-2 size-4 border-b-2 border-l-2 border-gold" />
              <span className="absolute right-2 bottom-2 size-4 border-r-2 border-b-2 border-gold" />
              <ScanLine className={cn('size-9 text-offwhite/80', validando && 'animate-pulse')} />
            </div>

            <div className="flex min-w-0 flex-1 flex-col gap-2">
              <Label htmlFor={`qr-${retirada.id}`} className="text-xs text-muted-foreground">
                {qrCodesDemonstracao ? 'Código apresentado (fictício)' : 'Código lido'}
              </Label>
              {qrCodesDemonstracao ? (
                <select
                  id={`qr-${retirada.id}`}
                  value={codigo}
                  onChange={(e) => {
                    setCodigo(e.target.value)
                    setResultado(null)
                  }}
                  disabled={!podeValidar || validando}
                  className="h-9 w-full rounded-md border border-input bg-card px-2 text-sm shadow-xs outline-none focus-visible:ring-[3px] focus-visible:ring-ring/60 disabled:opacity-50"
                >
                  {qrCodesDemonstracao.map((q) => (
                    <option key={q.codigo} value={q.codigo}>
                      {q.rotulo}
                    </option>
                  ))}
                </select>
              ) : (
                <Input
                  id={`qr-${retirada.id}`}
                  value={codigo}
                  onChange={(e) => setCodigo(e.target.value)}
                  placeholder="Cole ou digite o código"
                  disabled={!podeValidar || validando}
                  className="font-mono"
                />
              )}
              <Button variant="outline" onClick={ler} disabled={!podeValidar || validando || !codigo.trim()}>
                {validando ? <Loader2 className="animate-spin" /> : <ScanLine />}
                Ler QR Code
              </Button>
            </div>
          </div>

          {!podeValidar && (
            <p className="mt-2 text-xs text-muted-foreground">Disponível após iniciar o atendimento.</p>
          )}
          {resultado && <ResultadoQr valido={resultado.valido} mensagem={resultado.mensagem} />}
        </>
      )}
    </section>
  )
}

function ResultadoQr({ valido, mensagem }: { valido: boolean; mensagem: string }) {
  return (
    <div
      role="status"
      data-testid="resultado-qr"
      data-valido={valido}
      className={cn(
        'mt-3 flex gap-2.5 rounded-lg border p-3 text-sm',
        valido ? 'border-emerald-300 bg-emerald-50 text-emerald-950' : 'border-red-300 bg-red-50 text-red-950',
      )}
    >
      {valido ? <CheckCircle2 className="mt-0.5 size-5 shrink-0 text-emerald-700" /> : <ShieldAlert className="mt-0.5 size-5 shrink-0 text-red-700" />}
      <div>
        <p className="font-semibold">{valido ? 'QR Code confere' : 'QR Code não confere'}</p>
        <p>{mensagem}</p>
        {valido && <p className="mt-1 text-xs font-medium">A leitura não libera o aluno: confirme a identificação para prosseguir.</p>}
      </div>
    </div>
  )
}
