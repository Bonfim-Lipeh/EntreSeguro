import { cn } from '@/lib/utils'
import { iniciais } from '../presentation/formatters'

const TONS = [
  'bg-[#dbe4ee] text-[#102a43]',
  'bg-[#e6dfc8] text-[#4a3d12]',
  'bg-[#d7e8e1] text-[#174233]',
  'bg-[#e4dcea] text-[#3b2752]',
  'bg-[#ecdcd5] text-[#5a2a18]',
  'bg-[#d6e3ea] text-[#183a4d]',
]

function tom(nome: string) {
  let h = 0
  for (const c of nome) h = (h * 31 + c.charCodeAt(0)) >>> 0
  return TONS[h % TONS.length]
}

/** Avatar ilustrativo por iniciais — nenhuma foto real é utilizada. */
export function AvatarIniciais({ nome, className }: { nome: string; className?: string }) {
  return (
    <span
      aria-hidden
      className={cn('inline-flex size-10 shrink-0 items-center justify-center rounded-full text-sm font-bold', tom(nome), className)}
    >
      {iniciais(nome)}
    </span>
  )
}
