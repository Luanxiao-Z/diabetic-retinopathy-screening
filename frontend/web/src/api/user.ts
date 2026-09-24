import request from '@/utils/request'

export interface ProfileResult {
  userId: string
  username: string
  role: string
  permissions: string[]
  dataScope?: string
}

/** 当前用户档案（含权限集），用于刷新后恢复前端权限状态。 */
export function fetchProfile() {
  return request.get('/common/users/me') as unknown as Promise<ProfileResult>
}

/** 当前用户权限编码列表。 */
export function fetchPermissions() {
  return request.get('/common/users/me/permissions') as unknown as Promise<string[]>
}

/** 修改当前登录账号密码。 */
export function changePassword(oldPassword: string, newPassword: string) {
  return request.put('/common/users/me/password', {
    oldPassword,
    newPassword
  }) as unknown as Promise<void>
}
