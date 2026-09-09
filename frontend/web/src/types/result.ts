/** 后端统一响应体：{ code, msg, data } */
export interface ApiResult<T = unknown> {
  code: number
  msg: string
  data: T
}
