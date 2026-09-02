interface StatusMessageProps {
  tone?: 'info' | 'error'
  children: string
}

export function StatusMessage({ tone = 'info', children }: StatusMessageProps) {
  const isError = tone === 'error'

  return (
    <p
      className={`status-message status-message-${tone}`}
      role={isError ? 'alert' : 'status'}
      aria-live={isError ? 'assertive' : 'polite'}
    >
      {children}
    </p>
  )
}
