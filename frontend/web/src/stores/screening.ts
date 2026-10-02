import { defineStore } from 'pinia'
import { ref } from 'vue'

/**
 * 筛查业务数据版本号。
 *
 * 背景：列表页被 `KeepAlive` 缓存，切回时组件不会重新挂载，`onMounted` 不再触发，
 * 因此完成新的筛查后列表不会自动更新。
 *
 * 方案：写操作（上传筛查、删除记录、人工复核）成功后自增版本号；各列表页记录
 * 自己上次加载时的版本，在 `onActivated` 时比对——版本不一致才重新拉取。
 * 相比「每次切回都请求」，可避免无谓的网络开销。
 */
export const useScreeningStore = defineStore('screening', () => {
  /** 数据版本号：任何影响筛查记录的写操作后自增 */
  const dataVersion = ref(0)

  /** 写操作成功后调用 */
  function bumpDataVersion() {
    dataVersion.value += 1
  }

  return { dataVersion, bumpDataVersion }
})
