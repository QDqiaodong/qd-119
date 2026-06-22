import axios from 'axios'

export interface FieldErrors {
  [key: string]: string
}

export interface ApiError extends Error {
  fieldErrors?: FieldErrors
}

const http = axios.create({ baseURL: '' })

http.interceptors.response.use(
  (res) => {
    const result = res.data
    if (result && result.code === 200) {
      return result.data
    }
    const err = new Error(result?.message || '请求失败') as ApiError
    if (result?.data?.fieldErrors) {
      err.fieldErrors = result.data.fieldErrors as FieldErrors
    }
    return Promise.reject(err)
  },
  (err) => {
    if (err.response?.data) {
      const result = err.response.data
      const apiErr = new Error(result?.message || '请求失败') as ApiError
      if (result?.data?.fieldErrors) {
        apiErr.fieldErrors = result.data.fieldErrors as FieldErrors
      }
      return Promise.reject(apiErr)
    }
    return Promise.reject(err)
  },
)

export interface Part {
  id: number
  category_id: number
  category_name?: string
  name: string
  model: string
  total_quantity: number
  current_stock: number
  shelf_position: string
  updated_at: string
}

export interface AccessoryCategory {
  id: number
  name: string
  code: string
  description?: string
  sort_order: number
}

export interface ShelfOccupancyInfo {
  shelf_position: string
  part_type_count: number
  total_stock: number
  max_part_types: number
  max_stock_capacity: number
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
  machine_id?: number | null
  machine_code?: string | null
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
  confirmed: number
  created_at: string
}

export interface ShelfMigrationRecord {
  id: number
  part_id: number
  part_name: string
  part_model: string
  source_shelf: string
  target_shelf: string
  quantity: number
  operator: string
  created_at: string
}

export interface ScrapReasonDict {
  id: number
  code: string
  name: string
  level: string
  description?: string
  sort_order: number
  enabled: number
  created_at: string
  updated_at: string
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
  monthly_confirmed_scrap: number
  stat_period_start: string
  stat_period_end: string
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
  record_id: number
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

export interface PartDeletionCheck {
  can_delete: boolean
  inbound_count: number
  outbound_count: number
  scrap_count: number
  inventory_check_count: number
  total_related_count: number
}

export interface PackagingMachine {
  id: number
  machine_code: string
  machine_name: string
  production_line: string
  status: number
  sort_order: number
  remark?: string
  created_at: string
  updated_at: string
}

export const partsApi = {
  list: (params?: Record<string, unknown>) => http.get<any, PageResult<Part>>('/api/parts', { params }),
  getById: (id: number) => http.get<any, Part>(`/api/parts/${id}`),
  create: (data: Partial<Part>) => http.post<any, Part>('/api/parts', data),
  update: (id: number, data: Partial<Part>) => http.put<any, Part>(`/api/parts/${id}`, data),
  remove: (id: number) => http.delete(`/api/parts/${id}`),
  checkDeletion: (id: number) => http.get<any, PartDeletionCheck>(`/api/parts/${id}/deletion-check`),
  batchCreate: (data: Partial<Part>[]) => http.post<any, Part[]>('/api/parts/batch', data),
}

export const inboundApi = {
  list: (params?: Record<string, unknown>) => http.get<any, PageResult<InboundRecord>>('/api/inbound', { params }),
  create: (data: { part_id?: number; category_id?: number; part_name?: string; part_model?: string; quantity: number; shelf_position?: string; operator: string }) =>
    http.post<any, InboundRecord>('/api/inbound', data),
  getById: (id: number) => http.get<any, InboundRecord>(`/api/inbound/${id}`),
}

export const accessoryCategoryApi = {
  list: () => http.get<any, AccessoryCategory[]>('/api/accessory-categories'),
  getById: (id: number) => http.get<any, AccessoryCategory>(`/api/accessory-categories/${id}`),
  create: (data: Partial<AccessoryCategory>) => http.post<any, AccessoryCategory>('/api/accessory-categories', data),
  update: (id: number, data: Partial<AccessoryCategory>) => http.put<any, AccessoryCategory>(`/api/accessory-categories/${id}`, data),
  remove: (id: number) => http.delete(`/api/accessory-categories/${id}`),
}

export const shelfOccupancyApi = {
  getByPosition: (shelfPosition: string) => http.get<any, ShelfOccupancyInfo>(`/api/shelf-occupancy/${shelfPosition}`),
  getConfig: () => http.get<any, ShelfOccupancyInfo>('/api/shelf-occupancy/config'),
}

export const outboundApi = {
  list: (params?: Record<string, unknown>) => http.get<any, PageResult<OutboundRecord>>('/api/outbound', { params }),
  create: (data: { part_id: number; quantity: number; production_line: string; machine_id?: number | null; operator: string }) =>
    http.post<any, OutboundRecord>('/api/outbound', data),
  getById: (id: number) => http.get<any, OutboundRecord>(`/api/outbound/${id}`),
}

export const packagingMachineApi = {
  list: (params?: { production_line?: string }) =>
    http.get<any, PackagingMachine[]>('/api/packaging-machines', { params }),
  getById: (id: number) => http.get<any, PackagingMachine>(`/api/packaging-machines/${id}`),
  getByCode: (code: string) => http.get<any, PackagingMachine>(`/api/packaging-machines/code/${code}`),
}

export const inventoryApi = {
  list: (params?: Record<string, unknown>) => http.get<any, PageResult<InventoryRecord>>('/api/inventory', { params }),
  create: (data: { quarter: string; operator: string; items: { part_id: number; actual_quantity: number }[] }) =>
    http.post<any, InventoryRecord>('/api/inventory', data),
  getDetail: (id: number) => http.get<any, InventoryRecord>(`/api/inventory/${id}`),
  listQuarters: () => http.get<any, string[]>('/api/inventory/quarters'),
}

export const scrapApi = {
  list: (params?: Record<string, unknown>) => http.get<any, PageResult<ScrapRecord>>('/api/scrap', { params }),
  create: (data: { part_id: number; quantity: number; reasons: string[]; remark: string; operator: string }) =>
    http.post<any, ScrapRecord>('/api/scrap', {
      ...data,
      reason: data.reasons.join(','),
    }),
  getById: (id: number) => http.get<any, ScrapRecord>(`/api/scrap/${id}`),
}

export const scrapReasonDictApi = {
  listEnabled: () => http.get<any, ScrapReasonDict[]>('/api/scrap-reasons/enabled'),
  listAll: () => http.get<any, ScrapReasonDict[]>('/api/scrap-reasons'),
  getById: (id: number) => http.get<any, ScrapReasonDict>(`/api/scrap-reasons/${id}`),
  getByCode: (code: string) => http.get<any, ScrapReasonDict>(`/api/scrap-reasons/code/${code}`),
  create: (data: Partial<ScrapReasonDict>) => http.post<any, ScrapReasonDict>('/api/scrap-reasons', data),
  update: (id: number, data: Partial<ScrapReasonDict>) => http.put<any, ScrapReasonDict>(`/api/scrap-reasons/${id}`, data),
  remove: (id: number) => http.delete(`/api/scrap-reasons/${id}`),
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

export const shelfMigrationApi = {
  list: (params?: Record<string, unknown>) => http.get<any, PageResult<ShelfMigrationRecord>>('/api/shelf-migration', { params }),
  create: (data: { part_id: number; quantity: number; target_shelf: string; operator: string }) =>
    http.post<any, ShelfMigrationRecord>('/api/shelf-migration', data),
}
