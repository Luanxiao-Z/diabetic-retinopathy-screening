export interface LoginResult {
  token: string
  role: string
  username: string
  permissions: string[]
  dataScope?: string
}

export interface ProfileResult {
  userId: string
  username: string
  role: string
  permissions: string[]
  dataScope?: string
}

/** 自助注册入参：仅开放普通医生角色，手机号选填 */
export interface RegisterParams {
  username: string
  password: string
  realName: string
  phone?: string
}

export interface RegisterResult {
  id: string
  username: string
  realName: string
  role: string
}
