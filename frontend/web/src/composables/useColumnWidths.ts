import { computed, onMounted, ref } from 'vue'

/**
 * 表格列宽记忆。
 *
 * 配合 `el-table` 的 `border`（表头出现拖拽手柄，Element Plus 默认 resizable）：
 * - 列统一使用 `min-width` 而非 `width`，使初始宽度按内容与容器自动分配；
 * - 用户拖动表头分隔线后，经 `@header-dragend` 按「表名 + 列标识」持久化到
 *   localStorage，刷新后自动恢复；
 * - 需要恢复默认时调用 `resetWidths()`（或清除浏览器存储）。
 *
 * @param pageKey 表标识（不同列表用不同 key，避免互相覆盖）
 */
export function useColumnWidths(pageKey: string) {
  const storageKey = `drs_table_cols_${pageKey}`
  const widths = ref<Record<string, number>>({})

  function load() {
    try {
      const raw = localStorage.getItem(storageKey)
      widths.value = raw ? (JSON.parse(raw) as Record<string, number>) : {}
    } catch {
      widths.value = {}
    }
  }

  /** 取列宽：优先用用户拖拽保存的值，否则用默认值 */
  function widthOf(key: string, fallback: number): number {
    const saved = widths.value[key]
    return typeof saved === 'number' && saved > 0 ? saved : fallback
  }

  /** 绑定到 el-table 的 @header-dragend */
  function onHeaderDragend(
    newWidth: number,
    _oldWidth: number,
    column: { property?: string; label?: string }
  ) {
    const key = column.property || column.label
    if (!key || !Number.isFinite(newWidth)) return
    widths.value = { ...widths.value, [key]: Math.round(newWidth) }
    try {
      localStorage.setItem(storageKey, JSON.stringify(widths.value))
    } catch {
      /* 存储不可用时忽略，仅本次会话生效 */
    }
  }

  function resetWidths() {
    widths.value = {}
    try {
      localStorage.removeItem(storageKey)
    } catch {
      /* 忽略 */
    }
  }

  const hasCustom = computed(() => Object.keys(widths.value).length > 0)

  onMounted(load)

  return { widthOf, onHeaderDragend, resetWidths, hasCustom }
}
