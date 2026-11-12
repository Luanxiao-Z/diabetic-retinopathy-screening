/**
 * 列表请求竞态保护。
 *
 * 快速翻页 / 切换筛选时，先发出的请求可能后返回并把新数据覆盖掉。
 * guard 包装后：过期响应（无论成功或失败）一律返回 undefined，调用方直接忽略；
 * 仅最新一次请求的结果与异常会透出。
 */
export function useLatestRequest() {
  let seq = 0
  return function guard<T>(p: Promise<T>): Promise<T | undefined> {
    const my = ++seq
    return p.then(
      (r) => (my === seq ? r : undefined),
      (e) => {
        if (my === seq) throw e
        return undefined as never
      }
    )
  }
}
