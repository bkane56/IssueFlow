import { API_PATHS } from '../constants/api'
import type { User } from '../types/user'
import { request } from './client'

export function listUsers(): Promise<User[]> {
  return request<User[]>(API_PATHS.users)
}

export function createUser(name: string, email: string): Promise<User> {
  return request<User>(API_PATHS.users, {
    method: 'POST',
    body: JSON.stringify({ name, email }),
  })
}
