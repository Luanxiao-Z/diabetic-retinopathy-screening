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
