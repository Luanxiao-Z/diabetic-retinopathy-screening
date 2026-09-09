import axios, {
  type AxiosInstance,
  type AxiosResponse,
  type InternalAxiosRequestConfig
} from 'axios'
import type { ApiResult } from '@/types/result'

/**
 * 统一请求封装：
 * - 请求头注入 X-Access-Token（对齐 spec 认证规范）
 * - 响应拦截器解包 {code,msg,data}：code!==0 时 reject（401 跳转登录、403 提示）
 * - 业务调用方拿到的直接是 data 字段
 */
const service: AxiosInstance = axios.create({
  baseURL: import.meta.env.VITE_API_BASE ?? '/api/v1',
  timeout: 15000
})

service.interceptors.request.use((config: InternalAxiosRequestConfig) => {
  const token = localStorage.getItem('token')
  if (token) {
    config.headers.set('X-Access-Token', token)
  }
  return config
})

service.interceptors.response.use(
  (response: AxiosResponse): any => {
    const res = response.data as ApiResult
    if (res.code !== 0) {
      if (res.code === 401) {
        localStorage.removeItem('token')
        window.location.href = '/login'
      }
      return Promise.reject(new Error(res.msg || '请求失败'))
    }
    return res.data
  },
  (error) => Promise.reject(error)
)

export default service
