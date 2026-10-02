import request from '@/utils/request'

export interface ProfileResult {
  userId: string
  username: string
  realName?: string
  role: string
  permissions: string[]
  dataScope?: string
  /** 手机号（可为空） */
  phone?: string
  /** 账号创建时间 */
  createTime?: string
}

/** 当前用户档案（含权限集），用于刷新后恢复前端权限状态。 */
export function fetchProfile() {
  return request.get('/common/users/me') as unknown as Promise<ProfileResult>
}

/** 当前用户权限编码列表。 */
export function fetchPermissions() {
  return request.get('/common/users/me/permissions') as unknown as Promise<string[]>
}

/** 修改当前用户资料（仅真实姓名与手机号，角色与数据范围不可由本人变更）。 */
export function updateProfile(data: { realName: string; phone?: string }) {
  return request.put('/common/users/me', data) as unknown as Promise<ProfileResult>
}

/** 修改当前登录账号密码。 */
export function changePassword(oldPassword: string, newPassword: string) {
  return request.put('/common/users/me/password', {
    oldPassword,
    newPassword
  }) as unknown as Promise<void>
}
