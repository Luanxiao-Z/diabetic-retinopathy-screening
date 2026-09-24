import request from '@/utils/request'
import type { PageResult } from '@/types/screening'
import type {
  DictDomainCreateDTO,
  DictDomainUpdateDTO,
  DictDomainVO,
  DictItemCreateDTO,
  DictItemUpdateDTO,
  DictItemVO,
  OperationLogPageQuery,
  OperationLogVO,
  UserCreateDTO,
  UserPageQuery,
  UserUpdateDTO,
  UserVO
} from '@/types/admin'

/* ============================ 用户管理（admin:user:*） ============================ */

/** 分页查询用户：支持用户名模糊、角色、状态过滤 */
export function pageUsers(query: UserPageQuery) {
  return request.get('/admin/users', { params: query }) as unknown as Promise<PageResult<UserVO>>
}

/** 用户详情 */
export function detailUser(id: string) {
  return request.get(`/admin/users/${id}`) as unknown as Promise<UserVO>
}

/** 新增用户（返回新用户 id） */
export function createUser(dto: UserCreateDTO) {
  return request.post('/admin/users', dto) as unknown as Promise<string>
}

/** 修改用户（未提供字段不覆盖） */
export function updateUser(id: string, dto: UserUpdateDTO) {
  return request.put(`/admin/users/${id}`, dto) as unknown as Promise<void>
}

/** 删除用户（逻辑删除） */
export function removeUser(id: string) {
  return request.delete(`/admin/users/${id}`) as unknown as Promise<void>
}

/** 启用 / 停用用户 */
export function changeUserState(id: string, status: 'ENABLED' | 'DISABLED') {
  return request.patch(`/admin/users/${id}/state`, null, {
    params: { status }
  }) as unknown as Promise<void>
}

/* ============================ 字典管理（admin:dict:*） ============================ */

/** 字典域列表 */
export function listDictDomains() {
  return request.get('/admin/dict-domains') as unknown as Promise<DictDomainVO[]>
}

export function createDictDomain(dto: DictDomainCreateDTO) {
  return request.post('/admin/dict-domains', dto) as unknown as Promise<void>
}

export function updateDictDomain(domainCode: string, dto: DictDomainUpdateDTO) {
  return request.put(`/admin/dict-domains/${domainCode}`, dto) as unknown as Promise<void>
}

/** 删除字典域（级联删除其下字典项） */
export function removeDictDomain(domainCode: string) {
  return request.delete(`/admin/dict-domains/${domainCode}`) as unknown as Promise<void>
}

/** 某字典域下的字典项列表（按 sort 升序） */
export function listDictItems(domainCode: string) {
  return request.get(`/admin/dict-domains/${domainCode}/items`) as unknown as Promise<DictItemVO[]>
}

export function createDictItem(domainCode: string, dto: DictItemCreateDTO) {
  return request.post(`/admin/dict-domains/${domainCode}/items`, dto) as unknown as Promise<void>
}

export function updateDictItem(domainCode: string, itemCode: string, dto: DictItemUpdateDTO) {
  return request.put(
    `/admin/dict-domains/${domainCode}/items/${itemCode}`,
    dto
  ) as unknown as Promise<void>
}

export function removeDictItem(domainCode: string, itemCode: string) {
  return request.delete(
    `/admin/dict-domains/${domainCode}/items/${itemCode}`
  ) as unknown as Promise<void>
}

/* ============================ 操作日志（admin:log:view） ============================ */

/** 分页查询操作审计日志（时间倒序） */
export function pageOperationLogs(query: OperationLogPageQuery) {
  return request.get('/admin/operation-logs', { params: query }) as unknown as Promise<
    PageResult<OperationLogVO>
  >
}
