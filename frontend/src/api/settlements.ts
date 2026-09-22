import axios from '@/utils/axios'

export interface SettlementSegmentDTO {
  id: number
  segmentNo: number
  bindingId: number | null
  rackId: number
  rackCode: string
  rackMileageRange: string | null
  rackMileageRangeLabel: string | null
  segmentStart: string
  segmentEnd: string
  coveredDays: number
  dailyMileageQuota: number
  contributedMileage: number
  bindingStartDate: string
  bindingEndDate: string | null
}

export interface MonthlySettlementDTO {
  id: number
  teamId: number
  teamName: string
  /** yyyy-MM */
  periodMonth: string
  trainingMileage: string
  trainingMileageLabel: string
  targetMileage: number
  totalMileage: number
  segmentCount: number
  qualified: boolean
  sealedAt: string
  segments?: SettlementSegmentDTO[]
}

export interface SettlementPageResult {
  content: MonthlySettlementDTO[]
  totalElements: number
  totalPages: number
  currentPage: number
}

/** 生成结算单（封账）。重复封账时 axios 拦截器会抛出含后端失败原因的 Error。 */
export const sealSettlement = (teamId: number, month: string) => {
  return axios.post<MonthlySettlementDTO>('/settlements', { teamId, periodMonth: month }) as unknown as Promise<MonthlySettlementDTO>
}

/** 按队伍与月份查询已封结算单（含逐段明细）；未封账时 404 */
export const getSettlement = (teamId: number, month: string) => {
  return axios.get<MonthlySettlementDTO>('/settlements', { params: { teamId, month } }) as unknown as Promise<MonthlySettlementDTO>
}

export const getSettlementList = (params: { teamId?: number; page?: number; size?: number }) => {
  return axios.get<SettlementPageResult>('/settlements/list', { params }) as unknown as Promise<SettlementPageResult>
}
