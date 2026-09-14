import axios from '@/utils/axios'

export interface Team {
  id: number
  name: string
  memberCount: number
  trainingMileage: string
  description: string
  createdAt: string
  updatedAt: string
}

export interface TeamDTO {
  id?: number
  name: string
  memberCount: number
  trainingMileage: string
  description?: string
}

export interface PageResult<T> {
  content: T[]
  totalElements: number
  totalPages: number
  number: number
}

export const getTeamList = (params: { page?: number; size?: number; trainingMileage?: string }) => {
  return axios.get<PageResult<Team>>('/teams', { params }) as unknown as Promise<PageResult<Team>>
}

export const getTeamById = (id: number) => {
  return axios.get<Team>(`/teams/${id}`) as unknown as Promise<Team>
}

export const createTeam = (data: TeamDTO) => {
  return axios.post<Team>('/teams', data) as unknown as Promise<Team>
}

export const updateTeam = (id: number, data: TeamDTO) => {
  return axios.put<Team>(`/teams/${id}`, data) as unknown as Promise<Team>
}

export const deleteTeam = (id: number) => {
  return axios.delete(`/teams/${id}`) as unknown as Promise<void>
}