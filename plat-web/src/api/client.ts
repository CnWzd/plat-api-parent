import axios, { AxiosError, type AxiosInstance, type InternalAxiosRequestConfig } from 'axios'
import { message } from 'ant-design-vue'

const TOKEN_KEY = 'plat.accessToken'
const REFRESH_KEY = 'plat.refreshToken'

export class ApiError extends Error {
  constructor(
    public status: number,
    public code: number,
    message: string,
  ) {
    super(message)
  }
}

export const tokenStore = {
  get access(): string | null {
    return localStorage.getItem(TOKEN_KEY)
  },
  get refresh(): string | null {
    return localStorage.getItem(REFRESH_KEY)
  },
  save(access: string, refresh: string) {
    localStorage.setItem(TOKEN_KEY, access)
    localStorage.setItem(REFRESH_KEY, refresh)
  },
  clear() {
    localStorage.removeItem(TOKEN_KEY)
    localStorage.removeItem(REFRESH_KEY)
  },
}

const client: AxiosInstance = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '',
  timeout: 15000,
})

// 请求拦截：附加 Bearer 令牌
client.interceptors.request.use((config) => {
  const token = tokenStore.access
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

// 单飞刷新锁：并发 401 时只发起一次 refresh
let refreshing: Promise<string | null> | null = null

async function refreshAccessToken(): Promise<string | null> {
  const refreshToken = tokenStore.refresh
  if (!refreshToken) return null
  try {
    const resp = await axios.post<import('@/types').ApiEnvelope<import('@/types').TokenResponse>>(
      '/api/auth/refresh',
      { refreshToken },
      { baseURL: import.meta.env.VITE_API_BASE_URL || '', timeout: 10000 },
    )
    const data = resp.data.data
    tokenStore.save(data.accessToken, data.refreshToken)
    return data.accessToken
  } catch {
    return null
  }
}

function toLogin() {
  tokenStore.clear()
  if (window.location.pathname !== '/login') {
    message.warning('登录已失效，请重新登录')
    window.location.href = '/login'
  }
}

// 响应拦截：解包 ApiEnvelope + 401 自动刷新重试
client.interceptors.response.use(
  (resp) => resp.data,
  async (error: AxiosError<import('@/types').ApiEnvelope<unknown>>) => {
    const config = error.config as InternalAxiosRequestConfig & { _retried?: boolean }
    const status = error.response?.status ?? 0
    const envelope = error.response?.data

    // 401：尝试一次静默刷新后重试原请求
    if (status === 401 && config && !config._retried && tokenStore.refresh) {
      config._retried = true
      refreshing = refreshing ?? refreshAccessToken()
      const newToken = await refreshing
      refreshing = null
      if (newToken) {
        config.headers.Authorization = `Bearer ${newToken}`
        return client.request(config)
      }
      toLogin()
      return Promise.reject(new ApiError(401, 40100, '登录已失效'))
    }

    const code = envelope?.code ?? status * 100
    const msg = envelope?.message ?? '网络异常，请稍后重试'
    return Promise.reject(new ApiError(status, code, msg))
  },
)

/** 业务请求：成功直接返回 data */
export async function request<T>(fn: () => Promise<import('@/types').ApiEnvelope<T>>): Promise<T> {
  const envelope = await fn()
  if (envelope.code !== 0) {
    throw new ApiError(200, envelope.code, envelope.message)
  }
  return envelope.data
}

export default client
