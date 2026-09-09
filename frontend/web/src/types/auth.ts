export interface LoginResult {
  token: string
  role: string
  username: string
  permissions: string[]
}

export interface ProfileResult {
  userId: string
  username: string
  role: string
  permissions: string[]
}
