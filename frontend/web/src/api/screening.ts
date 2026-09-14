import request from '@/utils/request'
import type {
  PageResult,
  ScreeningPageQuery,
  ScreeningRecordVO,
  ScreeningStatisticsVO
} from '@/types/screening'
import { downloadBlob } from '@/utils/download'

/** 上传眼底图并筛查（multipart）。后端依次完成 MinIO 上传→模型推理→热力图→落库。 */
export function uploadScreening(params: {
  files: File[]
  patientName?: string
  patientAge?: number
  patientGender?: string
  remark?: string
}) {
  const form = new FormData()
  params.files.forEach((f) => form.append('files', f))
  if (params.patientName) form.append('patientName', params.patientName)
  if (params.patientAge != null) form.append('patientAge', String(params.patientAge))
  if (params.patientGender) form.append('patientGender', params.patientGender)
  if (params.remark) form.append('remark', params.remark)
  return request.post('/biz/screening-records', form, {
    timeout: 60000
  }) as unknown as Promise<ScreeningRecordVO[]>
}

/** 分页查询筛查记录（数据权限隔离）。 */
export function pageScreening(query: ScreeningPageQuery) {
  return request.get('/biz/screening-records', { params: query }) as unknown as Promise<
    PageResult<ScreeningRecordVO>
  >
}

/** 筛查记录详情（含预签名 URL）。 */
export function detailScreening(id: string) {
  return request.get(`/biz/screening-records/${id}`) as unknown as Promise<ScreeningRecordVO>
}

/** 删除筛查记录（逻辑删除+清理对象）。 */
export function removeScreening(id: string) {
  return request.delete(`/biz/screening-records/${id}`) as unknown as Promise<void>
}

/** 筛查统计（分级/建议分布、转诊率、近 30 天趋势）。 */
export function statisticsScreening(query: ScreeningPageQuery) {
  return request.get('/biz/screening-records/statistics', {
    params: query
  }) as unknown as Promise<ScreeningStatisticsVO>
}

/** 导出筛查记录 Excel（二进制下载，不经统一响应体解包）。 */
export function exportScreening(ids?: string[], query?: ScreeningPageQuery) {
  const params: Record<string, unknown> = { ...(query || {}) }
  if (ids && ids.length) params.ids = ids
  return downloadBlob({
    url: '/biz/screening-records/exports',
    params,
    fileName: `筛查记录导出_${new Date().toISOString().slice(0, 10)}.xlsx`
  })
}
