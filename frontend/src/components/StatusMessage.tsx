interface StatusMessageProps {
  tone?: 'info' | 'error'
  children: string
}

export function StatusMessage({ tone = 'info', children }: StatusMessageProps) {
  return <p className={`status-message status-message-${tone}`}>{children}</p>
}
