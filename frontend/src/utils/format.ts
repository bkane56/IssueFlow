export function formatDateTime(value: string): string {
  return new Date(value).toLocaleString()
}

export function formatBoolean(value: boolean): string {
  return value ? 'Yes' : 'No'
}
