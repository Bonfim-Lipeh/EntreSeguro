/**
 * Contrato de autenticação do funcionário da portaria (somente modo HTTP).
 *
 * Endpoints: POST /api/auth/login | /api/auth/refresh | /api/auth/logout.
 * O access token (JWT) fica apenas em memória; o refresh token fica em cookie
 * HttpOnly definido pelo backend e nunca é lido pelo JavaScript.
 */
export interface AutenticacaoService {
  /** Autentica com e-mail e senha. Lança ApiError (401) se as credenciais forem inválidas. */
  entrar(email: string, senha: string): Promise<void>
  /** Tenta recuperar a sessão usando o cookie de refresh (ex.: após recarregar a página). */
  restaurarSessao(): Promise<boolean>
  /** Revoga o refresh token no backend e descarta o access token. */
  sair(): Promise<void>
  /** Notifica quando a sessão deixa de ser válida (refresh recusado pelo backend). */
  aoExpirarSessao(callback: () => void): () => void
}
