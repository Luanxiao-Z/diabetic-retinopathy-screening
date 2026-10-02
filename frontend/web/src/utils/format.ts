/**
 * 时间展示格式化。
 *
 * 后端 `LocalDateTime` 以 ISO8601 序列化（形如 `2026-10-02T14:44:01`），
 * 而界面展示统一要求 `yyyy-MM-dd HH:mm:ss`（空格分隔、无 `T`）。
 *
 * 说明：
 * - 仅做字符串层面的替换，**不经过 `Date` 解析**，避免时区偏移导致时间漂移
 * - 操作日志等面向排障的场景保留 ISO 原文，不调用本函数
 * - 空值统一显示为 `—`
 */
export function formatDateTime(value?: string | null): string {
  if (!value) return '—'
  const matched = value.match(/^(\d{4}-\d{2}-\d{2})[T ](\d{2}:\d{2}:\d{2})/)
  return matched ? `${matched[1]} ${matched[2]}` : value
}

/**
 * 当前时间（本地时区），格式与 {@link formatDateTime} 一致。
 * 用于「生成时间」等前端自行产生的时间戳，避免 `toLocaleString` 输出 `2026/10/2 17:03:47` 这类不一致格式。
 */
export function formatNow(): string {
  const d = new Date()
  const p = (n: number) => String(n).padStart(2, '0')
  return (
    `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())} ` +
    `${p(d.getHours())}:${p(d.getMinutes())}:${p(d.getSeconds())}`
  )
}
