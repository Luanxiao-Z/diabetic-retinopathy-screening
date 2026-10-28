import { nextTick, onBeforeUnmount, onMounted, type Ref } from 'vue'

/**
 * 列表列宽自适应。
 *
 * 设计目标（替代旧的「localStorage 记忆 + 保存值当 min-width」方案）：
 * 1. 首次数据渲染后，按「表头 + 单元格实际内容」实测最合适的列宽；
 * 2. 选择/展开列、`fixed` 列（操作列）、模板显式 `width` 列保持固定，不参与自适应；
 * 3. 双击表头可重新按内容适配（即「重置列宽」）；
 * 4. 不做任何持久化——刷新后重新按内容计算，符合「列宽无需记忆」。
 *
 * 说明：固定列在 Element Plus 2.x 基于 sticky（单表结构），因此表头/表体按列序号对齐，
 * 测量与写入列宽均按序号进行。
 */

interface FitColumnOptions {
  /** 最小列宽（px） */
  min?: number
  /** 最大列宽（px） */
  max?: number
  /** 内容宽度之外的余量（px），用于边框与视觉留白 */
  margin?: number
}

interface TableColumnLike {
  type?: string
  fixed?: unknown
  width?: string | number
  property?: string
  label?: string
  [key: string]: unknown
}

interface BaseInfo {
  width?: string | number
  fixed?: unknown
  type?: string
}

export function useFitColumns(tableRef: Ref<any>, options: FitColumnOptions = {}) {
  const min = options.min ?? 70
  const max = options.max ?? 380
  const margin = options.margin ?? 8

  // 记录每列在模板中的「原始」宽度/类型：避免把自适应后的宽度误判为模板显式宽度
  const baseInfo = new WeakMap<TableColumnLike, BaseInfo>()
  let observer: MutationObserver | null = null
  let rafId = 0

  /** 实测元素内容宽度：临时置为「不换行 + max-content」，读取后还原（同步执行，不会产生可见闪烁） */
  function measure(el: HTMLElement): number {
    const prevWidth = el.style.width
    const prevWhiteSpace = el.style.whiteSpace
    el.style.whiteSpace = 'nowrap'
    el.style.width = 'max-content'
    const width = el.offsetWidth
    el.style.width = prevWidth
    el.style.whiteSpace = prevWhiteSpace
    return width
  }

  /** 按当前内容适配一次列宽 */
  function fit() {
    const table = tableRef.value
    const root = table?.$el as HTMLElement | undefined
    const columns = table?.store?.states?.columns?.value as TableColumnLike[] | undefined
    if (!root || !columns || !columns.length) return

    const headerCells = root.querySelectorAll<HTMLElement>('.el-table__header-wrapper th.el-table__cell')
    const bodyRows = root.querySelectorAll<HTMLElement>('.el-table__body-wrapper tbody tr')

    columns.forEach((col, index) => {
      if (!baseInfo.has(col)) {
        baseInfo.set(col, { width: col.width, fixed: col.fixed, type: col.type })
      }
      const base = baseInfo.get(col)!

      // 选择/展开/序号列：宽度由组件控制
      if (base.type === 'selection' || base.type === 'expand' || base.type === 'index') return
      // 固定列（如操作列）：宽度固定，重置时还原为模板宽度
      if (base.fixed) {
        if (base.width) col.width = base.width
        return
      }
      // 模板显式宽度：保持（并还原，避免拖动后无法重置）
      if (base.width) {
        col.width = base.width
        return
      }

      const th = headerCells[index]
      if (!th) return

      const thCell = th.querySelector<HTMLElement>('.cell') ?? th
      let contentWidth = measure(thCell)

      bodyRows.forEach((tr) => {
        const td = tr.children[index] as HTMLElement | undefined
        if (!td) return
        const cell = td.querySelector<HTMLElement>('.cell') ?? td
        contentWidth = Math.max(contentWidth, measure(cell))
      })

      // 先清掉拖动产生的显式宽度，再以「内容实测宽度」作为下限：既不被截断，又可铺满容器
      col.width = undefined
      col.minWidth = Math.min(max, Math.max(min, Math.ceil(contentWidth + margin)))
    })

    // doLayout 未直接暴露，使用其防抖封装重新计算列宽
    table?.debouncedUpdateLayout?.()
  }

  function scheduleFit() {
    cancelAnimationFrame(rafId)
    rafId = requestAnimationFrame(() => {
      void nextTick(fit)
    })
  }

  function onHeaderDblclick(event: MouseEvent) {
    const target = event.target as HTMLElement | null
    // 仅双击表头时重置；双击数据行（如打开编辑）不受影响
    if (target?.closest('thead')) fit()
  }

  onMounted(async () => {
    await nextTick()
    const root = tableRef.value?.$el as HTMLElement | undefined
    if (!root) return

    const tbody = root.querySelector('.el-table__body-wrapper tbody')
    const hasRows = () => !!tbody?.querySelector('tr')

    // 已有数据：立即适配；尚无数据：等首批行渲染后适配一次（之后不再自动适配，保留用户拖拽）
    if (hasRows() || !tbody) {
      fit()
    } else {
      observer = new MutationObserver(() => {
        if (hasRows()) {
          observer?.disconnect()
          observer = null
          fit()
        }
      })
      observer.observe(tbody, { childList: true })
    }

    root.addEventListener('dblclick', onHeaderDblclick)
  })

  onBeforeUnmount(() => {
    observer?.disconnect()
    observer = null
    cancelAnimationFrame(rafId)
    const root = tableRef.value?.$el as HTMLElement | undefined
    root?.removeEventListener('dblclick', onHeaderDblclick)
  })

  return { fit, scheduleFit }
}
