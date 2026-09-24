/** 系统管理（用户 / 字典）相关类型，对齐后端 /api/v1/admin 契约 */

export interface UserVO {
  id: string
  username: string
  realName?: string
  role: string
  phone?: string
  status: string
  createTime?: string
  updateTime?: string
}

export interface UserPageQuery {
  username?: string
  role?: string
  status?: string
  current?: number
  pageSize?: number
}

export interface UserCreateDTO {
  username: string
  realName?: string
  role: string
  phone?: string
  status?: string
  password: string
}

export interface UserUpdateDTO {
  realName?: string
  role?: string
  phone?: string
  status?: string
  password?: string
}

export interface DictDomainVO {
  domainCode: string
  domainName: string
  remark?: string
}

export interface DictItemVO {
  domainCode: string
  itemCode: string
  itemName: string
  itemValue?: string
  sort?: number
}

export interface DictDomainCreateDTO {
  domainCode: string
  domainName: string
  remark?: string
}

export interface DictDomainUpdateDTO {
  domainName?: string
  remark?: string
}

export interface DictItemCreateDTO {
  itemCode: string
  itemName: string
  itemValue?: string
  sort?: number
  remark?: string
}

export interface DictItemUpdateDTO {
  itemName?: string
  itemValue?: string
  sort?: number
  remark?: string
}

/** 角色选项（字典 B_USER_ROLE） */export const ROLE_OPTIONS: { value: string; label: string }[] = [
  { value: 'ADMIN', label: '管理员' },
  { value: 'DOCTOR', label: '医生' }
]

/** 账号状态选项 */
export const USER_STATUS_OPTIONS: { value: string; label: string }[] = [
  { value: 'ENABLED', label: '启用' },
  { value: 'DISABLED', label: '停用' }
]

export function roleLabel(role?: string): string {
  return ROLE_OPTIONS.find((o) => o.value === role)?.label || role || '—'
}

export function userStatusLabel(status?: string): string {
  return USER_STATUS_OPTIONS.find((o) => o.value === status)?.label || status || '—'
}

/* ============================ 操作日志 ============================ */

export interface OperationLogVO {
  id: string
  username?: string
  module: string
  moduleName?: string
  action: string
  actionName?: string
  target?: string
  /** SUCCESS / FAIL */
  result: string
  errorMsg?: string
  ip?: string
  costMs?: number
  createTime?: string
}

export interface OperationLogPageQuery {
  username?: string
  module?: string
  result?: string
  startDate?: string
  endDate?: string
  current?: number
  pageSize?: number
}

/** 日志模块选项 */
export const LOG_MODULE_OPTIONS: { value: string; label: string }[] = [
  { value: 'AUTH', label: '认证' },
  { value: 'SCREENING', label: '筛查业务' },
  { value: 'USER', label: '用户管理' },
  { value: 'DICT', label: '字典管理' }
]

/** 日志结果选项 */
export const LOG_RESULT_OPTIONS: { value: string; label: string }[] = [
  { value: 'SUCCESS', label: '成功' },
  { value: 'FAIL', label: '失败' }
]
