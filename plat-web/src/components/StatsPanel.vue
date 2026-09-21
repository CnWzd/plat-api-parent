<template>
  <a-spin :spinning="loading">
    <div class="stats-toolbar">
      <a-radio-group v-model:value="days" button-style="solid" size="small" @change="loadSummary">
        <a-radio-button :value="7">近 7 天</a-radio-button>
        <a-radio-button :value="14">近 14 天</a-radio-button>
        <a-radio-button :value="30">近 30 天</a-radio-button>
      </a-radio-group>
    </div>

    <div class="stat-grid">
      <div v-for="s in statCards" :key="s.label" class="stat-card">
        <div class="stat-icon" :style="{ background: s.bg, color: s.fg }">
          <component :is="s.icon" />
        </div>
        <div class="stat-meta">
          <div class="stat-value">{{ s.value }}</div>
          <div class="stat-label">{{ s.label }}</div>
        </div>
      </div>
    </div>

    <a-row :gutter="16" class="panel-row">
      <a-col :xs="24" :lg="16">
        <a-card :bordered="false" class="soft-card" title="调用趋势">
          <TrendChart v-if="trend.length" :option="trendOption" height="300px" />
          <a-empty v-else description="暂无调用数据" />
        </a-card>
      </a-col>
      <a-col :xs="24" :lg="8">
        <a-card :bordered="false" class="soft-card" title="TOP 热点 API">
          <a-table v-if="topApis.length" size="small" :data-source="topApis" :columns="topColumns"
                   :pagination="false" row-key="apiCode">
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'apiCode'">
                <code class="mono">{{ (record as StatsTopApiVO).apiCode }}</code>
              </template>
              <template v-else-if="column.key === 'totalCalls'">
                {{ fmtNumber((record as StatsTopApiVO).totalCalls) }}
              </template>
              <template v-else-if="column.key === 'avgLatencyMs'">
                {{ fmtMs((record as StatsTopApiVO).avgLatencyMs) }}
              </template>
            </template>
          </a-table>
          <a-empty v-else description="暂无数据" />
        </a-card>
      </a-col>
    </a-row>

    <a-card v-if="showLogs" :bordered="false" class="soft-card" title="调用明细" style="margin-top: 16px">
      <div class="log-filter">
        <a-input v-model:value="logQuery.appKey" allow-clear placeholder="AppKey 过滤"
                 style="width: 220px" @pressEnter="reloadLogs" />
        <a-input v-model:value="logQuery.apiCode" allow-clear placeholder="API 编码"
                 style="width: 190px" @pressEnter="reloadLogs" />
        <a-select v-model:value="logQuery.gatewayResult" allow-clear placeholder="网关结果"
                  style="width: 150px" :options="resultOptions" />
        <a-button type="primary" @click="reloadLogs">查询</a-button>
        <a-button @click="resetLogFilter">重置</a-button>
      </div>
      <a-table size="middle" :data-source="logs" :columns="logColumns" :loading="logLoading"
               :pagination="logPagination" row-key="id" :scroll="{ x: 1100 }" @change="onLogChange">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'calledAt'">
            {{ fmtDateTime((record as CallLogVO).calledAt) }}
          </template>
          <template v-else-if="column.key === 'appKey'">
            <code class="mono">{{ (record as CallLogVO).appKey || '-' }}</code>
          </template>
          <template v-else-if="column.key === 'apiCode'">
            <code class="mono">{{ (record as CallLogVO).apiCode || '-' }}</code>
          </template>
          <template v-else-if="column.key === 'method'">
            <a-tag v-if="(record as CallLogVO).method" :color="methodColor((record as CallLogVO).method!)">
              {{ (record as CallLogVO).method }}
            </a-tag>
            <span v-else>-</span>
          </template>
          <template v-else-if="column.key === 'path'">
            <code class="mono">{{ (record as CallLogVO).path || '-' }}</code>
          </template>
          <template v-else-if="column.key === 'statusCode'">
            <span :class="{ 'status-ok': ((record as CallLogVO).statusCode ?? 0) < 400 }">
              {{ (record as CallLogVO).statusCode ?? '-' }}
            </span>
          </template>
          <template v-else-if="column.key === 'latencyMs'">
            {{ fmtMs((record as CallLogVO).latencyMs) }}
          </template>
          <template v-else-if="column.key === 'gatewayResult'">
            <a-tag :color="resultTag((record as CallLogVO).gatewayResult).color">
              {{ resultTag((record as CallLogVO).gatewayResult).text }}
            </a-tag>
          </template>
        </template>
      </a-table>
    </a-card>
  </a-spin>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import type { EChartsOption } from 'echarts'
import {
  AppstoreOutlined, CheckCircleOutlined, ClockCircleOutlined,
  CloseCircleOutlined, RiseOutlined, ThunderboltOutlined,
} from '@ant-design/icons-vue'
import TrendChart from '@/components/TrendChart.vue'
import { fmtDateTime, fmtMs, fmtNumber, GATEWAY_RESULT, METHOD_COLOR } from '@/utils/format'
import type { CallLogVO, PageResult, StatsOverviewVO, StatsTopApiVO, StatsTrendPointVO } from '@/types'

/** 统计数据源契约：portal 的 statsApi 与 console 的统计方法共用同一形态 */
export interface StatsApiShape {
  overview: (days?: number) => Promise<StatsOverviewVO>
  trend: (days?: number) => Promise<StatsTrendPointVO[]>
  topApis: (days?: number, limit?: number) => Promise<StatsTopApiVO[]>
  logs: (params: {
    pageNo?: number
    pageSize?: number
    appKey?: string
    apiCode?: string
    gatewayResult?: string
  }) => Promise<PageResult<CallLogVO>>
}

const props = withDefaults(defineProps<{
  api: StatsApiShape
  showLogs?: boolean
}>(), { showLogs: true })

const days = ref(7)
const loading = ref(false)
const overview = ref<StatsOverviewVO | null>(null)
const trend = ref<StatsTrendPointVO[]>([])
const topApis = ref<StatsTopApiVO[]>([])

const logLoading = ref(false)
const logs = ref<CallLogVO[]>([])
const logQuery = reactive<{ appKey: string; apiCode: string; gatewayResult?: string }>({
  appKey: '',
  apiCode: '',
  gatewayResult: undefined,
})
const logPagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
  showSizeChanger: true,
  pageSizeOptions: ['10', '20', '50'],
  showTotal: (t: number) => `共 ${t} 条`,
})

const topColumns = [
  { title: 'API 编码', dataIndex: 'apiCode', key: 'apiCode', ellipsis: true },
  { title: '调用次数', dataIndex: 'totalCalls', key: 'totalCalls', width: 95, align: 'right' as const },
  { title: '平均耗时', dataIndex: 'avgLatencyMs', key: 'avgLatencyMs', width: 90, align: 'right' as const },
]

const logColumns = [
  { title: '时间', dataIndex: 'calledAt', key: 'calledAt', width: 170 },
  { title: 'AppKey', dataIndex: 'appKey', key: 'appKey', width: 150 },
  { title: 'API', dataIndex: 'apiCode', key: 'apiCode', width: 140 },
  { title: '方法', dataIndex: 'method', key: 'method', width: 80 },
  { title: '路径', dataIndex: 'path', key: 'path', ellipsis: true },
  { title: '状态码', dataIndex: 'statusCode', key: 'statusCode', width: 85, align: 'right' as const },
  { title: '耗时', dataIndex: 'latencyMs', key: 'latencyMs', width: 90, align: 'right' as const },
  { title: '结果', dataIndex: 'gatewayResult', key: 'gatewayResult', width: 110 },
  { title: '客户端 IP', dataIndex: 'clientIp', key: 'clientIp', width: 130 },
]

const resultOptions = Object.entries(GATEWAY_RESULT).map(([value, cfg]) => ({
  value,
  label: cfg.text,
}))

function resultTag(v?: string | null): { text: string; color: string } {
  return (v && GATEWAY_RESULT[v]) || { text: v ?? '-', color: 'default' }
}

function methodColor(m: string): string {
  return METHOD_COLOR[m.toUpperCase()] ?? 'default'
}

function fmtRate(v?: number | null): string {
  if (v === null || v === undefined) return '-'
  return `${Number(v.toFixed(2))}%`
}

const statCards = computed(() => [
  {
    label: '总调用次数', value: fmtNumber(overview.value?.totalCalls), icon: ThunderboltOutlined,
    bg: 'linear-gradient(135deg,#e8efff,#f5f8ff)', fg: '#3358d4',
  },
  {
    label: '成功调用', value: fmtNumber(overview.value?.successCalls), icon: CheckCircleOutlined,
    bg: 'linear-gradient(135deg,#e6f9f0,#f2fcf7)', fg: '#0f9d58',
  },
  {
    label: '失败调用', value: fmtNumber(overview.value?.failCalls), icon: CloseCircleOutlined,
    bg: 'linear-gradient(135deg,#ffeef0,#fff6f7)', fg: '#d4552a',
  },
  {
    label: '调用成功率', value: fmtRate(overview.value?.successRate), icon: RiseOutlined,
    bg: 'linear-gradient(135deg,#f0ecff,#f7f4ff)', fg: '#6f4bd8',
  },
  {
    label: '平均耗时', value: fmtMs(overview.value?.avgLatencyMs), icon: ClockCircleOutlined,
    bg: 'linear-gradient(135deg,#fff4e6,#fff9f2)', fg: '#c87d1e',
  },
  {
    label: '接入应用数', value: fmtNumber(overview.value?.appCount), icon: AppstoreOutlined,
    bg: 'linear-gradient(135deg,#e6f7fa,#f0fbfd)', fg: '#0e8fa8',
  },
])

const trendOption = computed<EChartsOption>(() => ({
  tooltip: { trigger: 'axis' },
  legend: { data: ['总调用', '成功', '失败'], bottom: 0, textStyle: { color: '#5b6270' } },
  grid: { left: 56, right: 24, top: 24, bottom: 48 },
  xAxis: {
    type: 'category',
    boundaryGap: false,
    data: trend.value.map((p) => p.statDate),
    axisLine: { lineStyle: { color: '#dcdfe6' } },
    axisLabel: { color: '#8a9099' },
  },
  yAxis: {
    type: 'value',
    minInterval: 1,
    splitLine: { lineStyle: { color: '#f0f2f5' } },
    axisLabel: { color: '#8a9099' },
  },
  series: [
    {
      name: '总调用', type: 'line', smooth: true, symbol: 'circle', symbolSize: 6,
      data: trend.value.map((p) => p.totalCalls),
      itemStyle: { color: '#4f7cff' },
      areaStyle: { color: 'rgba(79,124,255,0.08)' },
    },
    {
      name: '成功', type: 'line', smooth: true, symbol: 'none',
      data: trend.value.map((p) => p.successCalls),
      itemStyle: { color: '#22b573' },
    },
    {
      name: '失败', type: 'line', smooth: true, symbol: 'none',
      data: trend.value.map((p) => p.failCalls),
      itemStyle: { color: '#e05252' },
    },
  ],
}))

async function loadSummary() {
  loading.value = true
  try {
    const [ov, tr, top] = await Promise.all([
      props.api.overview(days.value),
      props.api.trend(days.value),
      props.api.topApis(days.value, 10),
    ])
    overview.value = ov
    trend.value = tr
    topApis.value = top
  } finally {
    loading.value = false
  }
}

async function loadLogs() {
  logLoading.value = true
  try {
    const page = await props.api.logs({
      pageNo: logPagination.current,
      pageSize: logPagination.pageSize,
      appKey: logQuery.appKey || undefined,
      apiCode: logQuery.apiCode || undefined,
      gatewayResult: logQuery.gatewayResult || undefined,
    })
    logs.value = page.records
    logPagination.total = page.total
  } finally {
    logLoading.value = false
  }
}

function reloadLogs() {
  logPagination.current = 1
  loadLogs()
}

function resetLogFilter() {
  logQuery.appKey = ''
  logQuery.apiCode = ''
  logQuery.gatewayResult = undefined
  reloadLogs()
}

function onLogChange(pag: { current?: number; pageSize?: number }) {
  logPagination.current = pag.current ?? 1
  logPagination.pageSize = pag.pageSize ?? 10
  loadLogs()
}

onMounted(() => {
  loadSummary()
  if (props.showLogs) loadLogs()
})
</script>

<style scoped>
.stats-toolbar {
  display: flex;
  justify-content: flex-end;
  margin-bottom: 14px;
}

.stat-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 14px;
  margin-bottom: 16px;
}

.stat-card {
  background: #fff;
  border-radius: 12px;
  border: 1px solid #eef0f3;
  padding: 18px;
  display: flex;
  align-items: center;
  gap: 14px;
  box-shadow: 0 1px 3px rgba(31, 35, 41, 0.04);
}

.stat-icon {
  width: 44px;
  height: 44px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  flex: none;
}

.stat-value {
  font-size: 22px;
  font-weight: 700;
  color: #1f2329;
  line-height: 1.2;
  font-variant-numeric: tabular-nums;
}

.stat-label {
  font-size: 12px;
  color: #8a9099;
  margin-top: 4px;
}

.panel-row {
  margin-bottom: 0 !important;
}

.soft-card {
  border-radius: 12px;
  box-shadow: 0 1px 3px rgba(31, 35, 41, 0.04);
}

.log-filter {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-bottom: 14px;
}

.mono {
  font-family: Consolas, 'Courier New', monospace;
  font-size: 12px;
  color: #3b4350;
  word-break: break-all;
}

.status-ok {
  color: #0f9d58;
  font-weight: 600;
}
</style>
