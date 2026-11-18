export type ApiEnvelope<T> = {
  requestId: string
  data: T | null
  errors: Array<{ code: string; message: string; field?: string | null }>
}

export type SystemStatus = {
  service: string
  status: 'UP' | 'DOWN'
  checkedAt: string
}

export type PythonHealth = {
  service: string
  status: 'UP' | 'DOWN'
  contractVersion: string
  checkedAt: string
}

async function get<T>(path: string): Promise<T> {
  const response = await fetch(path)
  if (!response.ok) {
    throw new Error(`Request failed with status ${response.status}`)
  }
  const envelope = (await response.json()) as ApiEnvelope<T>
  if (envelope.errors.length > 0 || envelope.data === null) {
    throw new Error(envelope.errors[0]?.message ?? 'The service returned no data.')
  }
  return envelope.data
}

export const api = {
  systemStatus: () => get<SystemStatus>('/api/v1/system/status'),
  pythonHealth: () => get<PythonHealth>('/api/v1/system/integration/python'),
}
