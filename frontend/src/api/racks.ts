import axios from '@/utils/axios'

export interface Rack {
  id: number
  code: string
  capacity: number
  mileageRange: string
  dailyMileageQuota: number
  description: string
  createdAt: string
  updatedAt: string
}

export interface RackDTO {
  id?: number
  code: string
  capacity: number
  mileageRange: string
  /** 日里程配额（km/天） */
  dailyMileageQuota?: number
  description?: string
}

export interface PageResult<T> {
  content: T[]
  totalElements: number
  totalPages: number
  number: number
}

export const getRackList = (params: { page?: number; size?: number; mileageRange?: string }) => {
  return axios.get<PageResult<Rack>>('/racks', { params }) as unknown as Promise<PageResult<Rack>>
}

export const getRackById = (id: number) => {
  return axios.get<Rack>(`/racks/${id}`) as unknown as Promise<Rack>
}

export const createRack = (data: RackDTO) => {
  return axios.post<Rack>('/racks', data) as unknown as Promise<Rack>
}

export const updateRack = (id: number, data: RackDTO) => {
  return axios.put<Rack>(`/racks/${id}`, data) as unknown as Promise<Rack>
}

export const deleteRack = (id: number) => {
  return axios.delete(`/racks/${id}`) as unknown as Promise<void>
}