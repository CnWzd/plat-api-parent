/** 后端统一响应包装 */
export interface ApiEnvelope<T> {
  code: number
  message: string
  data: T
}

/** 统一分页结构 */
export interface PageResult<T> {
  total: number
  pageNo: number
  pageSize: number
  records: T[]
}

// ---------------- 认证 ----------------

export interface TokenResponse {
  accessToken: string
  refreshToken: string
  expiresIn: number
  user: AuthUser
}

export interface AuthUser {
  id: string
  username: string
  nickname: string
  role: 'ADMIN' | 'DEVELOPER'
  tenantId: string | null
}

// ---------------- 应用 ----------------

export interface AppVO {
  id: string
  appName: string
  appKey: string
  secretMasked: string
  status: number
  rateLimitQps: number
  callbackUrl: string | null
  remark: string | null
  tenantId: string | null
  createdAt: string
  approvedApiCount: number
  pendingApiCount: number
}

export interface SecretRevealVO {
  appId: string
  appName: string
  appKey: string
  appSecret: string
  notice: string
  generatedAt: string
}

// ---------------- API 元数据 ----------------

export interface ApiVO {
  id: string
  apiCode: string
  apiName: string
  category: string
  method: string
  path: string
  version: string
  description: string | null
  authType: 'SIGN' | 'NONE'
  status: number
  createdAt: string
  updatedAt: string
}

// ---------------- 授权审批 ----------------

export interface AppApiAuthVO {
  id: string
  appId: string
  appName: string | null
  appKey: string | null
  apiId: string
  apiCode: string | null
  apiName: string | null
  apiMethod: string | null
  apiPath: string | null
  status: number
  applyRemark: string | null
  rejectReason: string | null
  appliedAt: string
  approvedAt: string | null
  ownerUsername: string | null
}

// ---------------- 统计 ----------------

export interface StatsOverviewVO {
  totalCalls: number
  successCalls: number
  failCalls: number
  successRate: number
  avgLatencyMs: number | null
  maxLatencyMs: number | null
  appCount: number
}

export interface StatsTrendPointVO {
  statDate: string
  totalCalls: number
  successCalls: number
  failCalls: number
  avgLatencyMs: number | null
}

export interface StatsTopApiVO {
  apiCode: string
  totalCalls: number
  successCalls: number
  avgLatencyMs: number | null
}

export interface CallLogVO {
  id: string
  appKey: string | null
  apiCode: string | null
  method: string | null
  path: string | null
  statusCode: number | null
  latencyMs: number | null
  clientIp: string | null
  gatewayResult: string | null
  errorMsg: string | null
  calledAt: string
}

// ---------------- 租户 / 用户 ----------------

export interface TenantVO {
  id: string
  tenantCode: string
  tenantName: string
  contactName: string | null
  contactPhone: string | null
  status: number
  createdAt: string
  developerCount: number
}

export interface ConsoleUserVO {
  id: string
  username: string
  nickname: string | null
  email: string | null
  phone: string | null
  role: string
  tenantId: string | null
  status: number
  createdAt: string
}
