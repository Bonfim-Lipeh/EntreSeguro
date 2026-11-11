import { useEffect, useState } from 'react'

export function useDebounce<T>(valor: T, atrasoMs = 250): T {
  const [v, setV] = useState(valor)
  useEffect(() => {
    const t = setTimeout(() => setV(valor), atrasoMs)
    return () => clearTimeout(t)
  }, [valor, atrasoMs])
  return v
}
