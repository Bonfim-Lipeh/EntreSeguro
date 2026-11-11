import { Timer } from 'lucide-react'
import { cn } from '@/lib/utils'
import { formatarDuracao, minutosDesde } from '../presentation/formatters'
import { nivelEspera } from '../presentation/statusConfig'

const ESTILO = {
  normal: 'text-foreground',
  atencao: 'text-amber-800',
  critico: 'text-red-700 font-bold',
}

export function TempoEspera({ desde, agora, className }: { desde: string; agora: number; className?: string }) {
  const min = minutosDesde(desde, agora)
  const nivel = nivelEspera(min)
  return (
    <span className={cn('inline-flex items-center gap-1 tabular-nums font-semibold', ESTILO[nivel], className)} data-nivel={nivel}>
      <Timer className="size-3.5" aria-hidden />
      {formatarDuracao(min)}
    </span>
  )
}
