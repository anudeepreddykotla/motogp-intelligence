export type ApiError = { code: string; message: string; field?: string | null }

export type ApiEnvelope<T> = { requestId: string; data: T | null; errors: ApiError[] }

export type Paginated<T> = { items: T[]; limit: number; offset: number; hasNext: boolean }

export type SystemStatus = { service: string; status: 'UP' | 'DOWN'; checkedAt: string }
export type PythonHealth = { service: string; status: 'UP' | 'DOWN'; contractVersion: string; checkedAt: string }
export type Season = { id: string; year: number | null; championship: string | null }
export type Event = { id: string; seasonId: string; seasonYear: number | null; championship: string | null; circuitId: string; circuitName: string | null; countryCode: string | null; name: string | null; roundNumber: number | null; scheduledStartAt: string | null; timezone: string | null }
export type Session = { id: string; eventId: string; seasonId: string; eventName: string | null; sessionType: string | null; scheduledStartAt: string | null; startedAt: string | null; endedAt: string | null; dataMode: string | null }
export type SessionResult = { id: string; sessionId: string; riderId: string; riderName: string | null; teamId: string | null; teamName: string | null; manufacturerId: string | null; manufacturerName: string | null; gridPosition: number | null; classificationPosition: number | null; classificationStatus: string | null; lapsCompleted: number | null; totalTimeMs: number | null; points: number | null }
export type Lap = { id: string; sessionResultId: string; sessionId: string; riderId: string; riderName: string | null; lapNumber: number | null; lapTimeMs: number | null; valid: boolean | null; observedAt: string | null }
export type Position = { id: string; sessionResultId: string; sessionId: string; riderId: string; riderName: string | null; lapNumber: number | null; position: number | null; observedAt: string | null }
export type Comparison = { sessionId: string; riderId: string; riderName: string | null; classificationPosition: number | null; classificationStatus: string | null; lapsCompleted: number | null; totalTimeMs: number | null; points: number | null }
export type NewsArticle = { id: string; title: string; publisherName: string | null; publishedAt: string | null; permittedExcerpt: string | null; imageUrl: string | null; originalUrl: string | null }

async function get<T>(path: string): Promise<T> {
  const response = await fetch(path, { headers: { Accept: 'application/json' } })
  let envelope: ApiEnvelope<T>
  try {
    envelope = (await response.json()) as ApiEnvelope<T>
  } catch {
    throw new Error(`The service returned an invalid response (${response.status}).`)
  }
  if (!response.ok || envelope.errors.length > 0 || envelope.data === null) {
    throw new Error(envelope.errors[0]?.message ?? `Request failed with status ${response.status}.`)
  }
  return envelope.data
}

function paged<T>(path: string, limit: number) {
  return get<Paginated<T>>(`${path}${path.includes('?') ? '&' : '?'}limit=${limit}&offset=0`)
}

export const api = {
  systemStatus: () => get<SystemStatus>('/api/v1/system/status'),
  pythonHealth: () => get<PythonHealth>('/api/v1/system/integration/python'),
  seasons: () => paged<Season>('/api/v1/seasons', 100),
  season: (seasonId: string) => get<Season>(`/api/v1/seasons/${encodeURIComponent(seasonId)}`),
  events: (seasonId: string) => paged<Event>(`/api/v1/events?seasonId=${encodeURIComponent(seasonId)}`, 100),
  event: (eventId: string) => get<Event>(`/api/v1/events/${encodeURIComponent(eventId)}`),
  sessions: (eventId: string) => paged<Session>(`/api/v1/events/${encodeURIComponent(eventId)}/sessions`, 100),
  session: (sessionId: string) => get<Session>(`/api/v1/sessions/${encodeURIComponent(sessionId)}`),
  results: (sessionId: string) => paged<SessionResult>(`/api/v1/sessions/${encodeURIComponent(sessionId)}/results`, 500),
  laps: (sessionId: string, riderId: string) => paged<Lap>(`/api/v1/sessions/${encodeURIComponent(sessionId)}/laps?riderId=${encodeURIComponent(riderId)}`, 1000),
  positions: (sessionId: string, riderId: string) => paged<Position>(`/api/v1/sessions/${encodeURIComponent(sessionId)}/positions?riderId=${encodeURIComponent(riderId)}`, 1000),
  compare: (sessionId: string, riderIds: string[]) => {
    const params = new URLSearchParams({ sessionId })
    riderIds.forEach((riderId) => params.append('riderIds', riderId))
    return get<Comparison[]>(`/api/v1/riders/compare?${params.toString()}`)
  },
  newsPublishers: () => get<string[]>('/api/v1/news/publishers'),
  news: (filters: { title?: string; publisher?: string; publishedFrom?: string; publishedTo?: string; limit: number; offset: number }) => {
    const params = new URLSearchParams({ limit: String(filters.limit), offset: String(filters.offset) })
    if (filters.title?.trim()) params.set('title', filters.title.trim())
    if (filters.publisher?.trim()) params.set('publisher', filters.publisher.trim())
    if (filters.publishedFrom) params.set('publishedFrom', filters.publishedFrom)
    if (filters.publishedTo) params.set('publishedTo', filters.publishedTo)
    return get<Paginated<NewsArticle>>(`/api/v1/news?${params.toString()}`)
  },
  newsArticle: (articleId: string) => get<NewsArticle>(`/api/v1/news/${encodeURIComponent(articleId)}`),
}
