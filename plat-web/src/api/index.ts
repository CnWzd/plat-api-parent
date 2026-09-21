import client, { request, tokenStore } from './client'
import type {
  ApiEnvelope,
  AppApiAuthVO,
  AppVO,
  ApiVO,
  CallLogVO,
  ConsoleUserVO,
  PageResult,
  SecretRevealVO,
  StatsOverviewVO,
  StatsTopApiVO,
  StatsTrendPointVO,
  TenantVO,
  TokenResponse,
} from '@/types'

// ---------------- 认证 ----------------

export interface LoginPayload {
  username: string
  password: string
}

export interface RegisterPayload {
  username: string
  password: string
  nickname?: string
  email?: string
  tenantCode?: string
}

export const authApi = {
  login: (payload: LoginPayload) =>
    request(() => client.post<never, ApiEnvelope<TokenResponse>>('/api/auth/login', payload)),
  register: (payload: RegisterPayload) =>
    request(() => client.post<never, ApiEnvelope<TokenResponse>>('/api/auth/register', payload)),
  me: () => request(() => client.get<never, ApiEnvelope<TokenResponse>>('/api/auth/me')),
  logout: () => tokenStore.clear(),
}

// ---------------- 开发者：应用 ----------------

export interface AppCreatePayload {
  appName: string
  callbackUrl?: string
  remark?: string
}

export interface AppUpdatePayload {
  appName: string
  callbackUrl?: string
  remark?: string
}

export const appApi = {
  page: (params: { pageNo?: number; pageSize?: number; keyword?: string }) =>
    request(() => client.get<never, ApiEnvelope<PageResult<AppVO>>>('/api/apps', { params })),
  detail: (id: string) => request(() => client.get<never, ApiEnvelope<AppVO>>(`/api/apps/${id}`)),
  create: (payload: AppCreatePayload) =>
    request(() => client.post<never, ApiEnvelope<SecretRevealVO>>('/api/apps', payload)),
  update: (id: string, payload: AppUpdatePayload) =>
    request(() => client.put<never, ApiEnvelope<AppVO>>(`/api/apps/${id}`, payload)),
  resetSecret: (id: string) =>
    request(() => client.post<never, ApiEnvelope<SecretRevealVO>>(`/api/apps/${id}/reset-secret`)),
}

// ---------------- 开发者：API 目录与授权 ----------------

export const apiCatalogApi = {
  list: (params?: { keyword?: string; category?: string }) =>
    request(() => client.get<never, ApiEnvelope<ApiVO[]>>('/api/apis', { params })),
  detail: (id: string) => request(() => client.get<never, ApiEnvelope<ApiVO>>(`/api/apis/${id}`)),
}

export const approvalApi = {
  listByApp: (appId: string) =>
    request(() => client.get<never, ApiEnvelope<AppApiAuthVO[]>>(`/api/apps/${appId}/auths`)),
  apply: (appId: string, payload: { apiIds: string[]; applyRemark?: string }) =>
    request(() => client.post<never, ApiEnvelope<AppApiAuthVO[]>>(`/api/apps/${appId}/auth-apply`, payload)),
}

// ---------------- 开发者：统计 ----------------

export const statsApi = {
  overview: (days = 7) =>
    request(() => client.get<never, ApiEnvelope<StatsOverviewVO>>('/api/stats/overview', { params: { days } })),
  trend: (days = 7) =>
    request(() => client.get<never, ApiEnvelope<StatsTrendPointVO[]>>('/api/stats/trend', { params: { days } })),
  topApis: (days = 7, limit = 10) =>
    request(() => client.get<never, ApiEnvelope<StatsTopApiVO[]>>('/api/stats/top-apis', { params: { days, limit } })),
  logs: (params: { pageNo?: number; pageSize?: number; appKey?: string; apiCode?: string; gatewayResult?: string }) =>
    request(() => client.get<never, ApiEnvelope<PageResult<CallLogVO>>>('/api/stats/logs', { params })),
}

// ---------------- 运营：租户 / 用户 ----------------

export const tenantApi = {
  page: (params: { pageNo?: number; pageSize?: number; keyword?: string }) =>
    request(() => client.get<never, ApiEnvelope<PageResult<TenantVO>>>('/api/console/tenants', { params })),
  create: (payload: { tenantCode: string; tenantName: string; contactName?: string; contactPhone?: string }) =>
    request(() => client.post<never, ApiEnvelope<TenantVO>>('/api/console/tenants', payload)),
  update: (id: string, payload: { tenantName: string; contactName?: string; contactPhone?: string }) =>
    request(() => client.put<never, ApiEnvelope<TenantVO>>(`/api/console/tenants/${id}`, payload)),
  updateStatus: (id: string, status: number) =>
    request(() => client.put<never, ApiEnvelope<TenantVO>>(`/api/console/tenants/${id}/status`, null, { params: { status } })),
}

export const userApi = {
  page: (params: { pageNo?: number; pageSize?: number; keyword?: string; role?: string }) =>
    request(() => client.get<never, ApiEnvelope<PageResult<ConsoleUserVO>>>('/api/console/users', { params })),
  updateStatus: (id: string, status: number) =>
    request(() => client.post<never, ApiEnvelope<ConsoleUserVO>>(`/api/console/users/${id}/status`, null, { params: { status } })),
  resetPassword: (id: string) =>
    request(() => client.post<never, ApiEnvelope<{ userId: string; username: string; newPassword: string }>>(`/api/console/users/${id}/reset-password`)),
}

// ---------------- 运营：API 元数据 / 审批 / 应用 / 统计 ----------------

export const consoleApi = {
  apis: (params: { pageNo?: number; pageSize?: number; keyword?: string; status?: number | null }) =>
    request(() => client.get<never, ApiEnvelope<PageResult<ApiVO>>>('/api/console/apis', { params })),
  createApi: (payload: Record<string, string>) =>
    request(() => client.post<never, ApiEnvelope<ApiVO>>('/api/console/apis', payload)),
  updateApi: (id: string, payload: Record<string, string>) =>
    request(() => client.put<never, ApiEnvelope<ApiVO>>(`/api/console/apis/${id}`, payload)),
  updateApiStatus: (id: string, status: number) =>
    request(() => client.put<never, ApiEnvelope<ApiVO>>(`/api/console/apis/${id}/status`, null, { params: { status } })),

  approvals: (params: { pageNo?: number; pageSize?: number; status?: number | null }) =>
    request(() => client.get<never, ApiEnvelope<PageResult<AppApiAuthVO>>>('/api/console/approvals', { params })),
  approve: (id: string) =>
    request(() => client.post<never, ApiEnvelope<AppApiAuthVO>>(`/api/console/approvals/${id}/approve`)),
  reject: (id: string, reason?: string) =>
    request(() => client.post<never, ApiEnvelope<AppApiAuthVO>>(`/api/console/approvals/${id}/reject`, { reason: reason ?? null })),

  apps: (params: { pageNo?: number; pageSize?: number; keyword?: string }) =>
    request(() => client.get<never, ApiEnvelope<PageResult<AppVO>>>('/api/console/apps', { params })),
  updateAppStatus: (id: string, status: number) =>
    request(() => client.put<never, ApiEnvelope<AppVO>>(`/api/console/apps/${id}/status`, null, { params: { status } })),
  updateAppRateLimit: (id: string, rateLimitQps: number) =>
    request(() => client.put<never, ApiEnvelope<AppVO>>(`/api/console/apps/${id}/rate-limit`, { rateLimitQps })),

  statsOverview: (days = 7) =>
    request(() => client.get<never, ApiEnvelope<StatsOverviewVO>>('/api/console/stats/overview', { params: { days } })),
  statsTrend: (days = 7) =>
    request(() => client.get<never, ApiEnvelope<StatsTrendPointVO[]>>('/api/console/stats/trend', { params: { days } })),
  statsTopApis: (days = 7, limit = 10) =>
    request(() => client.get<never, ApiEnvelope<StatsTopApiVO[]>>('/api/console/stats/top-apis', { params: { days, limit } })),
  statsLogs: (params: { pageNo?: number; pageSize?: number; appKey?: string; apiCode?: string; gatewayResult?: string }) =>
    request(() => client.get<never, ApiEnvelope<PageResult<CallLogVO>>>('/api/console/stats/logs', { params })),
}
