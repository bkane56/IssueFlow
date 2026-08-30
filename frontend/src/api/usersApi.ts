import { API_PATHS } from '../constants/api'
import type { User } from '../types/user'
import { request } from './client'

export function listUsers(): Promise<User[]> {
  return request<User[]>(API_PATHS.users)
}
