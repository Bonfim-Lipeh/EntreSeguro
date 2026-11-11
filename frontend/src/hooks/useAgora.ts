import { useEffect, useState } from 'react'

/** Relógio que força nova renderização a cada `intervaloMs`. */
export function useAgora(intervaloMs = 1000): number {
  const [agora, setAgora] = useState(() => Date.now())
  useEffect(() => {
    const t = setInterval(() => setAgora(Date.now()), intervaloMs)
    return () => clearInterval(t)
  }, [intervaloMs])
  return agora
}
