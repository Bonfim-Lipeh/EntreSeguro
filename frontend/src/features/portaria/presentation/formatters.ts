const fmtHora = new Intl.DateTimeFormat('pt-BR', { hour: '2-digit', minute: '2-digit' })
const fmtHoraSeg = new Intl.DateTimeFormat('pt-BR', { hour: '2-digit', minute: '2-digit', second: '2-digit' })
const fmtData = new Intl.DateTimeFormat('pt-BR', { weekday: 'long', day: 'numeric', month: 'long' })
const fmtDataCurta = new Intl.DateTimeFormat('pt-BR', { day: '2-digit', month: '2-digit' })

export const hora = (iso: string | null) => (iso ? fmtHora.format(new Date(iso)) : '—')
export const horaComSegundos = (d: Date) => fmtHoraSeg.format(d)
export const dataPorExtenso = (d: Date) => {
  const s = fmtData.format(d)
  return s.charAt(0).toUpperCase() + s.slice(1)
}
export const dataCurta = (iso: string) => fmtDataCurta.format(new Date(iso))

export function minutosDesde(iso: string, agora: number): number {
  return Math.max(0, Math.floor((agora - Date.parse(iso)) / 60_000))
}

export function formatarDuracao(minutos: number): string {
  if (minutos < 1) return 'agora'
  if (minutos < 60) return `${minutos} min`
  const h = Math.floor(minutos / 60)
  const m = minutos % 60
  return m ? `${h} h ${m} min` : `${h} h`
}

export function formatarChegada(iso: string | null, agora: number): string {
  if (!iso) return 'Não informada'
  const diff = Math.round((Date.parse(iso) - agora) / 60_000)
  if (diff > 0) return `${hora(iso)} (em ${diff} min)`
  if (diff === 0) return `${hora(iso)} (agora)`
  return `${hora(iso)} (prevista há ${-diff} min)`
}

export function iniciais(nome: string): string {
  const partes = nome.trim().split(/\s+/)
  const primeira = partes[0]?.[0] ?? ''
  const ultima = partes.length > 1 ? partes[partes.length - 1][0] : ''
  return (primeira + ultima).toUpperCase()
}
