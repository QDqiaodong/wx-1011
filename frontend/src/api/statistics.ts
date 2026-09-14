import axios from '@/utils/axios'

export interface StatisticsDTO {
  mileageRackCount: Record<string, number>
}

export interface MileageDetail {
  range: string
  rangeLabel: string
  rangeDescription: string
  totalRacks: number
  activeBindings: number
  racks: any[]
}

export interface DashboardSummary {
  totalRacks: number
  activeBindings: number
  totalTeams: number
  mileageStats: Record<string, number>
}

export const getMileageStatistics = () => {
  return axios.get<StatisticsDTO>('/statistics/mileage') as unknown as Promise<StatisticsDTO>
}

export const getMileageDetail = (range: string) => {
  return axios.get<MileageDetail>(`/statistics/mileage/${range}`) as unknown as Promise<MileageDetail>
}

export const getDashboardSummary = () => {
  return axios.get<DashboardSummary>('/statistics/dashboard') as unknown as Promise<DashboardSummary>
}