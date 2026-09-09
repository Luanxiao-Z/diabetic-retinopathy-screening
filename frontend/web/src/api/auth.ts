import request from '@/utils/request'
import type { LoginResult } from '@/types/auth'
export type { LoginResult }

export interface LoginParams {
  username: string
  password: string
}

/** 登录：后端返回 { token, role, username, permissions }，请求拦截器已将响应解包为 data。 */
export function login(params: LoginParams) {
  return request.post('/auth/login', params) as unknown as Promise<LoginResult>
}
