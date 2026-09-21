import dayjs from 'dayjs'

export function fmtDateTime(v?: string | null): string {
  if (!v) return '-'
  return dayjs(v).format('YYYY-MM-DD HH:mm:ss')
}

export function fmtDate(v?: string | null): string {
  if (!v) return '-'
  return dayjs(v).format('YYYY-MM-DD')
}

export function fmtNumber(n?: number | null): string {
  if (n === null || n === undefined) return '-'
  return n.toLocaleString('zh-CN')
}

export function fmtMs(v?: number | null): string {
  if (v === null || v === undefined) return '-'
  return `${Math.round(v)} ms`
}

// ---------------- 状态渲染配置 ----------------

export const APP_STATUS: Record<number, { text: string; color: string }> = {
  0: { text: '禁用', color: 'red' },
  1: { text: '启用', color: 'green' },
  2: { text: '冻结', color: 'orange' },
}

export const API_STATUS: Record<number, { text: string; color: string }> = {
  0: { text: '草稿', color: 'default' },
  1: { text: '已上架', color: 'green' },
  2: { text: '已下架', color: 'red' },
}

export const AUTH_STATUS: Record<number, { text: string; color: string }> = {
  0: { text: '待审批', color: 'orange' },
  1: { text: '已授权', color: 'green' },
  2: { text: '已驳回', color: 'red' },
}

export const USER_STATUS: Record<number, { text: string; color: string }> = {
  0: { text: '禁用', color: 'red' },
  1: { text: '启用', color: 'green' },
}

export const GATEWAY_RESULT: Record<string, { text: string; color: string }> = {
  PASSED: { text: '成功', color: 'green' },
  REJECTED_AUTH: { text: '无权限', color: 'red' },
  REJECTED_SIGN: { text: '签名失败', color: 'red' },
  REJECTED_LIMIT: { text: '限流拦截', color: 'orange' },
  REJECTED_DISABLED: { text: '应用停用', color: 'orange' },
  UPSTREAM_ERROR: { text: '上游错误', color: 'red' },
}

export const METHOD_COLOR: Record<string, string> = {
  GET: 'blue',
  POST: 'green',
  PUT: 'orange',
  DELETE: 'red',
  PATCH: 'purple',
}
