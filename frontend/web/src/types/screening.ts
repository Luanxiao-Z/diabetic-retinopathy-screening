/** 筛查业务前后端共用类型（对齐后端 ScreeningRecordVO / ScreeningStatisticsVO / ScreeningPageQuery） */

export type DrLevel = 'LEVEL_0' | 'LEVEL_1' | 'LEVEL_2' | 'LEVEL_3' | 'LEVEL_4'
export type Suggestion = 'REVIEW' | 'CLINIC' | 'REFERRAL'

export interface ScreeningRecordVO {
  id: string
  patientName?: string
  patientAge?: number
  patientGender?: string
  imageKey?: string
  /** 原始眼底图预签名 URL */
  imageUrl?: string
  gradCamKey?: string
  /** 热力图预签名 URL（可能为 null） */
  gradCamUrl?: string
  resultLevel?: DrLevel | string
  /** 分级中文说明（后端提供） */
  levelName?: string
  /** 最高概率置信度（0~1，top1 softmax，仅供展示参考） */
  confidence?: number
  /** 归一化预测熵（0~1），越大越不确定；人工复核判定依据 */
  uncertainty?: number
  /** 各级别概率 LEVEL_0..LEVEL_4 */
  probabilities?: Record<string, number>
  suggestion?: Suggestion | string
  /** 建议中文说明（后端提供） */
  suggestionName?: string
  modelVersion?: string
  remark?: string
  createTime?: string
  /** 是否需人工复核（不确定性达到阈值） */
  needReview?: boolean
  /** 人工复核阈值（归一化熵达到该值需复核） */
  reviewThreshold?: number
  /** 人工复核状态：PENDING 待复核 / CONFIRMED 已复核 */
  reviewStatus?: string
  reviewStatusName?: string
  reviewer?: string
  reviewTime?: string
  reviewRemark?: string
}

export interface DailyCountVO {
  date: string
  count: number
}

export interface ScreeningStatisticsVO {
  total: number
  levelDistribution: Record<string, number>
  suggestionDistribution: Record<string, number>
  /** 转诊率 0~1 */
  referralRate: number
  trend: DailyCountVO[]
  /** 需人工复核数量（置信度低于阈值） */
  needReviewCount?: number
  /** 环比：近 30 天数量 */
  recentTotal?: number
  /** 环比：前 30 天数量 */
  prevTotal?: number
  /** 环比增长率（可为负；前 30 天为 0 时记 1 表示新增） */
  growthRate?: number
  /** 人工复核阈值 */
  reviewThreshold?: number
}

export interface ScreeningPageQuery {
  patientName?: string
  level?: string
  startDate?: string
  endDate?: string
  current?: number
  pageSize?: number
  /** true 仅看待复核；false 仅看达标；省略不限制 */
  needReview?: boolean
  /** 随访时间线：按患者精确匹配 */
  exactPatientName?: string

  // -------- 患者随访专用（作用于聚合后的患者维度） --------
  /** 最近一次分级 */
  latestLevel?: string
  /** 最少筛查次数 */
  minCount?: number
  /** true 仅返回含待复核记录的患者 */
  hasPendingReview?: boolean
  /** 最近筛查开始时间 yyyy-MM-dd HH:mm:ss */
  latestStartDate?: string
  /** 最近筛查结束时间 yyyy-MM-dd HH:mm:ss */
  latestEndDate?: string

  // -------- 随访待办专用 --------
  /** 待办类型：all / review / referral / overdue */
  todoType?: string
}

/** 待办类型 */
export type TodoType = 'review' | 'referral' | 'overdue'

/** 随访待办条目（对齐后端 TodoItemVO；后端 null 字段不下发） */
export interface TodoItemVO {
  id: string
  type: TodoType
  typeName: string
  patientName: string
  levelName?: string
  detail?: string
  uncertainty?: number
  time?: string
  /** 关联筛查记录 id（待复核、需转诊可跳诊断报告） */
  recordId?: string
}

/** 患者随访聚合（对齐后端 PatientFollowUpVO；后端 null 字段不下发） */
export interface PatientFollowUpVO {
  patientName: string
  patientGender?: string
  patientAge?: number
  totalCount: number
  latestTime?: string
  firstTime?: string
  latestLevel?: string
  latestLevelName?: string
  latestConfidence?: number
  latestSuggestion?: string
  latestSuggestionName?: string
  previousLevel?: string
  previousLevelName?: string
  levelChanged?: boolean
  /** UP 进展 / DOWN 好转 / SAME 持平 / FIRST 首次 / UNKNOWN */
  trendDirection?: string
  needReviewCount?: number
}

/** 模型元信息与训练指标（对齐模型服务 /model/info） */
export interface ModelInfoVO {
  version?: string
  backbone?: string
  device?: string
  cuda_available?: boolean
  trained?: boolean
  num_classes?: number
  img_size?: number
  weights_path?: string
  metrics?: {
    best_val_macro_f1?: number
    test_acc?: number
    test_macro_f1?: number
    test_f1_per_class?: number[]
    test_counts?: number[]
    history?: { epoch: number; train_acc: number; val_acc: number; val_macro_f1: number }[]
    args?: Record<string, unknown>
  } | null
}

export interface PageResult<T> {
  total: number
  current: number
  pageSize: number
  list: T[]
}

/** 分级下拉选项（字典 B_DR_LEVEL） */
export const LEVEL_OPTIONS: { value: DrLevel; label: string }[] = [
  { value: 'LEVEL_0', label: '0级 无病变' },
  { value: 'LEVEL_1', label: '1级 轻度' },
  { value: 'LEVEL_2', label: '2级 中度' },
  { value: 'LEVEL_3', label: '3级 重度' },
  { value: 'LEVEL_4', label: '4级 增殖期' }
]

export const GENDER_OPTIONS: { value: string; label: string }[] = [
  { value: 'MALE', label: '男' },
  { value: 'FEMALE', label: '女' }
]

/** 分级配色（用于徽标与图表） */
export const LEVEL_COLOR: Record<string, string> = {
  LEVEL_0: '#22C55E',
  LEVEL_1: '#84CC16',
  LEVEL_2: '#F59E0B',
  LEVEL_3: '#F97316',
  LEVEL_4: '#EF4444'
}

/** 建议配色 */
export const SUGGESTION_COLOR: Record<string, string> = {
  REVIEW: '#0891B2',
  CLINIC: '#F59E0B',
  REFERRAL: '#EF4444'
}

/** 分级顺序（图表与概率条排序用） */
export const LEVEL_ORDER: DrLevel[] = ['LEVEL_0', 'LEVEL_1', 'LEVEL_2', 'LEVEL_3', 'LEVEL_4']

export const LEVEL_LABEL: Record<string, string> = {
  LEVEL_0: '无病变',
  LEVEL_1: '轻度',
  LEVEL_2: '中度',
  LEVEL_3: '重度',
  LEVEL_4: '增殖期'
}

export const SUGGESTION_LABEL: Record<string, string> = {
  REVIEW: '定期复查',
  CLINIC: '建议就诊',
  REFERRAL: '建议转诊'
}
