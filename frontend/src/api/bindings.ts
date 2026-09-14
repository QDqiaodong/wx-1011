import axios from '@/utils/axios'

export interface BindingDTO {
  id: number
  rackId: number
  teamId: number
  rackCode: string
  teamName: string
  rackMileageRange: string
  teamMileageRange: string
  startDate: string
  endDate: string
  status: string
}

export interface BindingCreateDTO {
  rackId: number
  teamId: number
  startDate?: string
}

export interface BindingUpdateDTO {
  rackId: number
  changeReason: string
  operator?: string
}

export interface BindingHistory {
  id: number
  bindingId: number
  oldRackId: number
  newRackId: number
  changeReason: string
  operator: string
  changedAt: string
}

export interface BindingPageResult {
  content: BindingDTO[]
  totalElements: number
  totalPages: number
  currentPage: number
}

export const getBindingList = (params: { page?: number; size?: number; rackId?: number; teamId?: number; status?: string }) => {
  return axios.get<BindingPageResult>('/bindings', { params }) as unknown as Promise<BindingPageResult>
}

export const getBindingById = (id: number) => {
  return axios.get<BindingDTO>(`/bindings/${id}`) as unknown as Promise<BindingDTO>
}

export const getBindingHistory = (id: number) => {
  return axios.get<BindingHistory[]>(`/bindings/${id}/history`) as unknown as Promise<BindingHistory[]>
}

export const createBinding = (data: BindingCreateDTO) => {
  return axios.post<BindingDTO>('/bindings', data) as unknown as Promise<BindingDTO>
}

export const updateBinding = (id: number, data: BindingUpdateDTO) => {
  return axios.put<BindingDTO>(`/bindings/${id}`, data) as unknown as Promise<BindingDTO>
}

export const deleteBinding = (id: number) => {
  return axios.delete(`/bindings/${id}`) as unknown as Promise<void>
}