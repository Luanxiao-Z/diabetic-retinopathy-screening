export interface LoginResult {
  token: string
  role: string
  username: string
  permissions: string[]
  dataScope?: string
}

/** 图形验证码：文本存服务端，前端仅持有标识与图片 */
export interface CaptchaResult {
  captchaKey: string
  /** Base64 图片（含 data URI 前缀） */
  image: string
}

export interface ProfileResult {
  userId: string
  username: string
  realName?: string
  role: string
  permissions: string[]
  dataScope?: string
  phone?: string
  createTime?: string
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
