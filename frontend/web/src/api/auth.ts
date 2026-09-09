import request from '@/utils/request'
import type { LoginResult } from '@/types/auth'
export type { LoginResult }

export interface LoginParams {
  username: string
  password: string
}

/** 登录：POST /auth/sessions，后端返回 { token, role, username, permissions, ... }，拦截器已解包为 data。 */
export function login(params: LoginParams) {
  return request.post('/auth/sessions', params) as unknown as Promise<LoginResult>
}

/** 登出：DELETE /auth/sessions（令牌缺失亦视为成功）。 */
export function logout() {
  return request.delete('/auth/sessions') as unknown as Promise<void>
}
