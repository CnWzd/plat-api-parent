<template>
  <div class="page">
    <div class="page-head">
      <div>
        <h2 class="page-title">应用管理</h2>
        <p class="page-desc">平台侧管理全部接入应用：状态流转与限流配额调整。</p>
      </div>
    </div>

    <a-card :bordered="false" class="soft-card">
      <div class="table-filter">
        <a-input-search v-model:value="keyword" placeholder="按应用名称 / AppKey 搜索" style="width: 300px"
                        allow-clear @search="reload" />
        <a-select v-model:value="statusFilter" style="width: 120px" :options="statusOptions"
                  placeholder="状态" allow-clear @change="reload" />
        <a-button @click="reload">
          <template #icon><ReloadOutlined /></template>
          刷新
        </a-button>
      </div>
      <a-table :data-source="records" :columns="columns" :loading="loading" :pagination="pagination"
               row-key="id" :scroll="{ x: 1050 }" @change="onChange">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'appName'">
            <div class="cell-main">{{ (record as AppVO).appName }}</div>
            <div class="cell-sub">{{ (record as AppVO).remark || '暂无备注' }}</div>
          </template>
          <template v-else-if="column.key === 'appKey'">
            <code class="mono">{{ (record as AppVO).appKey }}</code>
          </template>
          <template v-else-if="column.key === 'status'">
            <a-tag :color="APP_STATUS[(record as AppVO).status]?.color">
              {{ APP_STATUS[(record as AppVO).status]?.text ?? '未知' }}
            </a-tag>
          </template>
          <template v-else-if="column.key === 'rateLimitQps'">
            {{ (record as AppVO).rateLimitQps }}
          </template>
          <template v-else-if="column.key === 'apiCount'">
            <a-tag color="green">{{ (record as AppVO).approvedApiCount }} 已授权</a-tag>
            <a-tag v-if="(record as AppVO).pendingApiCount > 0" color="orange">
              {{ (record as AppVO).pendingApiCount }} 待审批
            </a-tag>
          </template>
          <template v-else-if="column.key === 'createdAt'">
            {{ fmtDateTime((record as AppVO).createdAt) }}
          </template>
          <template v-else-if="column.key === 'action'">
            <a-space :size="4">
              <a-button type="link" size="small" @click="openStatus(record as AppVO)">状态</a-button>
              <a-button type="link" size="small" @click="openRate(record as AppVO)">限流</a-button>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>

    <a-modal v-model:open="statusOpen" title="调整应用状态" :confirm-loading="statusSaving" @ok="submitStatus">
      <p class="modal-tip">「{{ statusRecord?.appName }}」当前状态：
        <a-tag :color="APP_STATUS[statusRecord?.status ?? 0]?.color" style="margin-left:4px">
          {{ APP_STATUS[statusRecord?.status ?? 0]?.text }}
        </a-tag>
      </p>
      <a-radio-group v-model:value="targetStatus" class="status-radio">
        <a-radio :value="1">启用（正常调用）</a-radio>
        <a-radio :value="0">禁用（网关拒绝鉴权）</a-radio>
        <a-radio :value="2">冻结（违规临时停用）</a-radio>
      </a-radio-group>
    </a-modal>

    <a-modal v-model:open="rateOpen" title="调整限流配额" :confirm-loading="rateSaving" @ok="submitRate">
      <p class="modal-tip">「{{ rateRecord?.appName }}」当前配额：{{ rateRecord?.rateLimitQps }} QPS</p>
      <a-form layout="vertical">
        <a-form-item label="单应用限流（次/秒）" required>
          <a-input-number v-model:value="targetQps" :min="1" :max="100000" style="width: 200px"
                          placeholder="1 ~ 100000" />
        </a-form-item>
      </a-form>
      <a-alert type="info" show-icon message="网关按应用维度做令牌桶限流，调整后立即生效（缓存刷新约数秒）。" />
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import { ReloadOutlined } from '@ant-design/icons-vue'
import { consoleApi } from '@/api'
import { ApiError } from '@/api/client'
import { APP_STATUS, fmtDateTime } from '@/utils/format'
import type { AppVO } from '@/types'

const records = ref<AppVO[]>([])
const loading = ref(false)
const keyword = ref('')
const statusFilter = ref<number | undefined>(undefined)

const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
  showTotal: (t: number) => `共 ${t} 条`,
})

const statusOptions = [
  { value: 1, label: '启用' },
  { value: 0, label: '禁用' },
  { value: 2, label: '冻结' },
]

const columns = [
  { title: '应用', dataIndex: 'appName', key: 'appName' },
  { title: 'AppKey', dataIndex: 'appKey', key: 'appKey', width: 190 },
  { title: '状态', dataIndex: 'status', key: 'status', width: 85 },
  { title: '限流 (QPS)', dataIndex: 'rateLimitQps', key: 'rateLimitQps', width: 100, align: 'right' as const },
  { title: 'API 授权', key: 'apiCount', width: 170 },
  { title: '创建时间', key: 'createdAt', width: 170 },
  { title: '操作', key: 'action', width: 120 },
]

async function load() {
  loading.value = true
  try {
    const page = await consoleApi.apps({
      pageNo: pagination.current,
      pageSize: pagination.pageSize,
      keyword: keyword.value || undefined,
    })
    records.value = page.records
    pagination.total = page.total
  } finally {
    loading.value = false
  }
}

function reload() {
  pagination.current = 1
  load()
}

function onChange(pag: { current?: number; pageSize?: number }) {
  pagination.current = pag.current ?? 1
  pagination.pageSize = pag.pageSize ?? 10
  load()
}

// ---------------- 状态调整 ----------------
const statusOpen = ref(false)
const statusSaving = ref(false)
const statusRecord = ref<AppVO | null>(null)
const targetStatus = ref(1)

function openStatus(record: AppVO) {
  statusRecord.value = record
  targetStatus.value = record.status
  statusOpen.value = true
}

async function submitStatus() {
  if (!statusRecord.value) return
  statusSaving.value = true
  try {
    await consoleApi.updateAppStatus(statusRecord.value.id, targetStatus.value)
    message.success(`应用已${APP_STATUS[targetStatus.value]?.text ?? '更新'}`)
    statusOpen.value = false
    load()
  } catch (e) {
    message.error(e instanceof ApiError ? e.message : '操作失败')
  } finally {
    statusSaving.value = false
  }
}

// ---------------- 限流调整 ----------------
const rateOpen = ref(false)
const rateSaving = ref(false)
const rateRecord = ref<AppVO | null>(null)
const targetQps = ref(100)

function openRate(record: AppVO) {
  rateRecord.value = record
  targetQps.value = record.rateLimitQps
  rateOpen.value = true
}

async function submitRate() {
  if (!rateRecord.value) return
  if (!targetQps.value || targetQps.value < 1) {
    message.warning('请输入有效的 QPS 数值（≥1）')
    return
  }
  rateSaving.value = true
  try {
    await consoleApi.updateAppRateLimit(rateRecord.value.id, targetQps.value)
    message.success(`限流配额已调整为 ${targetQps.value} QPS`)
    rateOpen.value = false
    load()
  } catch (e) {
    message.error(e instanceof ApiError ? e.message : '操作失败')
  } finally {
    rateSaving.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.page {
  padding: 24px;
}

.page-head {
  margin-bottom: 20px;
}

.page-title {
  font-size: 20px;
  font-weight: 700;
  color: #1f2329;
  margin: 0 0 6px;
}

.page-desc {
  font-size: 13px;
  color: #8a9099;
  margin: 0;
}

.soft-card {
  border-radius: 12px;
  box-shadow: 0 1px 3px rgba(31, 35, 41, 0.04);
}

.table-filter {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-bottom: 14px;
}

.cell-main {
  font-weight: 600;
  color: #1f2329;
}

.cell-sub {
  font-size: 12px;
  color: #a0a6b0;
  max-width: 220px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.mono {
  font-family: Consolas, 'Courier New', monospace;
  font-size: 12px;
  color: #3b4350;
}

.modal-tip {
  font-size: 13px;
  color: #5b6270;
  margin-bottom: 12px;
}

.status-radio {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
</style>
