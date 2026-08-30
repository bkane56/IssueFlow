import { API_PATHS } from '../constants/api'
import type { DashboardStats } from '../types/dashboard'
import { request } from './client'

export function getDashboard(): Promise<DashboardStats> {
  return request<DashboardStats>(API_PATHS.dashboard)
}
