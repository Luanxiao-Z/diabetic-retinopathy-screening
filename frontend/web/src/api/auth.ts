import request from '@/utils/request'
import type { LoginResult, RegisterParams, RegisterResult } from '@/types/auth'
export type { LoginResult, RegisterParams, RegisterResult }

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

/** 注册：POST /auth/users，匿名接口。成功后不签发令牌，需回到登录页登录。 */
export function register(params: RegisterParams) {
  return request.post('/auth/users', params) as unknown as Promise<RegisterResult>
}
