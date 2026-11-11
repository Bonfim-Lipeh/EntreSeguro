import { Badge } from '@/components/ui/badge'
import { cn } from '@/lib/utils'
import type { StatusRetirada } from '@/domain/retirada'
import { STATUS_VISUAL } from '../presentation/statusConfig'

export function StatusBadge({ status, className }: { status: StatusRetirada; className?: string }) {
  const v = STATUS_VISUAL[status]
  const Icone = v.icone
  return (
    <Badge className={cn(v.badge, className)} data-status={status}>
      <Icone aria-hidden />
      {v.rotulo}
    </Badge>
  )
}
