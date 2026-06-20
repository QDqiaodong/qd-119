import axios from 'axios'

const http = axios.create({ baseURL: '' })

http.interceptors.response.use(
  (res) => {
    const result = res.data
    if (result && result.code === 200) {
      return result.data
    }
    return Promise.reject(new Error(result?.message || '请求失败'))
  },
  (err) => Promise.reject(err),
)

export interface Part {
  id: number
  name: string
  model: string
  total_quantity: number
  current_stock: number
  shelf_position: string
  updated_at: string
}

export interface InboundRecord {
  id: number
  part_id: number
  part_name: string
  part_model: string
  quantity: number
  shelf_position: string
  operator: string
  created_at: string
}

export interface OutboundRecord {
  id: number
  part_id: number
  part_name: string
  part_model: string
  quantity: number
  production_line: string
  operator: string
  created_at: string
}

export interface ScrapRecord {
  id: number
  part_id: number
  part_name: string
  part_model: string
  quantity: number
  reason: string
  remark: string
  operator: string
  created_at: string
}

export interface InventoryRecord {
  id: number
  quarter: string
  total_count: number
  match_count: number
  diff_count: number
  operator: string
  created_at: string
  items: InventoryItem[]
}

export interface InventoryItem {
  part_id: number
  part_name: string
  part_model: string
  shelf_position: string
  book_quantity: number
  actual_quantity: number
  difference: number
}

export interface DashboardOverview {
  total_parts: number
  total_stock: number
  monthly_inbound: number
  monthly_outbound: number
}

export interface BucklePart extends Part {
  last_inbound_time?: string
  compatible_machines: string[]
}

export interface BracketPart extends Part {
  length: number
  hole_spacing: number
  compatible_machines: string[]
  last_inbound_time?: string
}

export interface RecentActivity {
  id: number
  type: string
  description: string
  time: string
  production_line?: string
}

export interface PageResult<T> {
  list: T[]
  total: number
  page: number
  page_size: number
}

export const partsApi = {
  list: (params?: Record<string, unknown>) => http.get<any, PageResult<Part>>('/api/parts', { params }),
  getById: (id: number) => http.get<any, Part>(`/api/parts/${id}`),
  create: (data: Partial<Part>) => http.post<any, Part>('/api/parts', data),
  update: (id: number, data: Partial<Part>) => http.put<any, Part>(`/api/parts/${id}`, data),
  remove: (id: number) => http.delete(`/api/parts/${id}`),
  batchCreate: (data: Partial<Part>[]) => http.post<any, Part[]>('/api/parts/batch', data),
}

export const inboundApi = {
  list: (params?: Record<string, unknown>) => http.get<any, PageResult<InboundRecord>>('/api/inbound', { params }),
  create: (data: { part_id?: number; part_name?: string; part_model?: string; quantity: number; shelf_position?: string; operator: string }) =>
    http.post<any, InboundRecord>('/api/inbound', data),
}

export const outboundApi = {
  list: (params?: Record<string, unknown>) => http.get<any, PageResult<OutboundRecord>>('/api/outbound', { params }),
  create: (data: { part_id: number; quantity: number; production_line: string; operator: string }) =>
    http.post<any, OutboundRecord>('/api/outbound', data),
}

export const inventoryApi = {
  list: (params?: Record<string, unknown>) => http.get<any, PageResult<InventoryRecord>>('/api/inventory', { params }),
  create: (data: { quarter: string; operator: string; items: { part_id: number; actual_quantity: number }[] }) =>
    http.post<any, InventoryRecord>('/api/inventory', data),
  getDetail: (id: number) => http.get<any, InventoryRecord>(`/api/inventory/${id}`),
}

export const scrapApi = {
  list: (params?: Record<string, unknown>) => http.get<any, PageResult<ScrapRecord>>('/api/scrap', { params }),
  create: (data: { part_id: number; quantity: number; reasons: string[]; remark: string; operator: string }) =>
    http.post<any, ScrapRecord>('/api/scrap', {
      ...data,
      reason: data.reasons.join(','),
    }),
}

export const dashboardApi = {
  getOverview: () => http.get<any, DashboardOverview>('/api/dashboard/overview'),
  getRecent: () => http.get<any, RecentActivity[]>('/api/dashboard/recent'),
}

export const buckleApi = {
  list: (params?: Record<string, unknown>) => http.get<any, PageResult<BucklePart>>('/api/buckles', { params }),
  getLastInboundTime: (partId: number) =>
    http.get<any, { last_inbound_time: string }>(`/api/buckles/${partId}/last-inbound`),
}

export const bracketApi = {
  list: (params?: Record<string, unknown>) => http.get<any, PageResult<BracketPart>>('/api/brackets', { params }),
  getLastInboundTime: (partId: number) =>
    http.get<any, { last_inbound_time: string }>(`/api/brackets/${partId}/last-inbound`),
}
