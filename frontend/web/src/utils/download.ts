import axios from 'axios'
import { ElMessage } from 'element-plus'

/**
 * 专用二进制下载实例：导出接口直接以 HttpServletResponse 写出 .xlsx，
 * 不经过统一 {code,msg,data} 响应体，故单独处理 blob 与错误体。
 */
const raw = axios.create({
  baseURL: import.meta.env.VITE_API_BASE ?? '/api/v1',
  timeout: 60000
})

raw.interceptors.request.use((config) => {
  const token = localStorage.getItem('token')
  if (token) {
    config.headers.set('X-Access-Token', token)
  }
  return config
})

function parseErrorBlob(blob: Blob) {
  blob.text().then((text) => {
    try {
      const err = JSON.parse(text)
      ElMessage.error(err.msg || '导出失败')
    } catch {
      ElMessage.error('导出失败')
    }
  })
}

export async function downloadBlob(opts: {
  url: string
  params?: Record<string, unknown>
  fileName?: string
}) {
  try {
    const res = await raw.get(opts.url, { params: opts.params, responseType: 'blob' })
    const blob = res.data as Blob
    const contentType = String(res.headers['content-type'] ?? '').toLowerCase()
    if (contentType.includes('application/json')) {
      // 业务错误（如 401/403 由过滤器返回的 JSON）
      parseErrorBlob(blob)
      return
    }
    const disposition = String(res.headers['content-disposition'] ?? '')
    let fileName = opts.fileName || 'download.xlsx'
    const m = /filename="?([^"]+)"?/.exec(disposition)
    if (m) fileName = decodeURIComponent(m[1])
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = fileName
    document.body.appendChild(a)
    a.click()
    document.body.removeChild(a)
    URL.revokeObjectURL(url)
    ElMessage.success('导出成功，已开始下载')
  } catch (err: unknown) {
    const resp = (err as { response?: { data?: Blob } }).response
    if (resp && resp.data instanceof Blob) {
      parseErrorBlob(resp.data)
    } else {
      ElMessage.error('导出失败，请稍后重试')
    }
  }
}
