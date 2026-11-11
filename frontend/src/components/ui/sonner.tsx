import { Toaster as Sonner, type ToasterProps } from 'sonner'

function Toaster(props: ToasterProps) {
  return <Sonner position="bottom-center" richColors closeButton duration={4500} {...props} />
}

export { Toaster }
