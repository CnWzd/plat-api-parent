<template>
  <div class="page">
    <div class="page-head">
      <div>
        <h2 class="page-title">审批工作台</h2>
        <p class="page-desc">审核开发者提交的 API 授权申请，通过后即可签名调用。</p>
      </div>
    </div>

    <a-card :bordered="false" class="soft-card">
      <a-tabs v-model:active-key="statusTab" @change="reload">
        <a-tab-pane key="0" tab="待审批" />
        <a-tab-pane key="1" tab="已授权" />
        <a-tab-pane key="2" tab="已驳回" />
        <a-tab-pane key="all" tab="全部记录" />
      </a-tabs>

      <a-table :data-source="records" :columns="columns" :loading="loading" :pagination="pagination"
               row-key="id" :scroll="{ x: 1150 }" @change="onChange">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'app'">
            <div class="cell-main">{{ (record as AppApiAuthVO).appName || '-' }}</div>
            <code class="mono sub">{{ (record as AppApiAuthVO).appKey || '-' }}</code>
          </template>
          <template v-else-if="column.key === 'api'">
            <code class="mono">{{ (record as AppApiAuthVO).apiCode }}</code>
            <div class="cell-sub">
              <a-tag :color="methodColor((record as AppApiAuthVO).apiMethod ?? '')" class="mini-tag">
                {{ (record as AppApiAuthVO).apiMethod }}
              </a-tag>
              <code class="mono sub">{{ (record as AppApiAuthVO).apiPath }}</code>
            </div>
          </template>
          <template v-else-if="column.key === 'owner'">
            {{ (record as AppApiAuthVO).ownerUsername || '-' }}
          </template>
          <template v-else-if="column.key === 'status'">
            <a-tag :color="AUTH_STATUS[(record as AppApiAuthVO).status]?.color">
              {{ AUTH_STATUS[(record as AppApiAuthVO).status]?.text ?? '未知' }}
            </a-tag>
          </template>
          <template v-else-if="column.key === 'appliedAt'">
            {{ fmtDateTime((record as AppApiAuthVO).appliedAt) }}
          </template>
          <template v-else-if="column.key === 'audit'">
            <div v-if="(record as AppApiAuthVO).approvedAt" class="cell-sub">
              {{ fmtDateTime((record as AppApiAuthVO).approvedAt) }}
            </div>
            <div v-if="(record as AppApiAuthVO).rejectReason" class="reject-reason">
              驳回原因：{{ (record as AppApiAuthVO).rejectReason }}
            </div>
            <div v-if="(record as AppApiAuthVO).applyRemark" class="cell-sub">
              申请说明：{{ (record as AppApiAuthVO).applyRemark }}
            </div>
            <span v-if="!(record as AppApiAuthVO).approvedAt && !(record as AppApiAuthVO).rejectReason
              && !(record as AppApiAuthVO).applyRemark">-</span>
          </template>
          <template v-else-if="column.key === 'action'">
            <template v-if="(record as AppApiAuthVO).status === 0">
              <a-space :size="4">
                <a-button type="link" size="small" @click="approve((record as AppApiAuthVO))">通过</a-button>
                <a-button type="link" size="small" danger @click="openReject(record as AppApiAuthVO)">驳回</a-button>
              </a-space>
            </template>
            <span v-else class="cell-sub">已处理</span>
          </template>
        </template>
      </a-table>
    </a-card>

    <a-modal v-model:open="rejectOpen" title="驳回授权申请" :confirm-loading="rejecting"
             ok-text="确认驳回" ok-type="danger" cancel-text="取消" @ok="submitReject">
      <p class="reject-tip">
        驳回「{{ rejectingRecord?.appName }} → {{ rejectingRecord?.apiCode }}」的授权申请。
      </p>
      <a-textarea v-model:value="rejectReason" :rows="3" :maxlength="200"
                  placeholder="填写驳回原因（必填），开发者可见" />
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { message, Modal } from 'ant-design-vue'
import { consoleApi } from '@/api'
import { ApiError } from '@/api/client'
import { AUTH_STATUS, fmtDateTime, METHOD_COLOR } from '@/utils/format'
import type { AppApiAuthVO } from '@/types'

const records = ref<AppApiAuthVO[]>([])
const loading = ref(false)
const statusTab = ref<string>('0')

const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
  showTotal: (t: number) => `共 ${t} 条`,
})

const columns = [
  { title: '应用', key: 'app', width: 190 },
  { title: 'API', key: 'api', width: 260 },
  { title: '申请人', key: 'owner', width: 110 },
  { title: '状态', dataIndex: 'status', key: 'status', width: 95 },
  { title: '申请时间', key: 'appliedAt', width: 165 },
  { title: '审批信息 / 说明', key: 'audit', width: 230 },
  { title: '操作', key: 'action', width: 120 },
]

function methodColor(m: string): string {
  return METHOD_COLOR[m?.toUpperCase()] ?? 'default'
}

async function load() {
  loading.value = true
  try {
    const page = await consoleApi.approvals({
      pageNo: pagination.current,
      pageSize: pagination.pageSize,
      status: statusTab.value === 'all' ? null : Number(statusTab.value),
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

// ---------------- 通过 ----------------
function approve(record: AppApiAuthVO) {
  Modal.confirm({
    title: '确认通过该授权申请？',
    content: `「${record.appName}」将获得「${record.apiCode}」的调用授权，立即生效。`,
    okText: '确认通过',
    cancelText: '取消',
    onOk: async () => {
      try {
        await consoleApi.approve(record.id)
        message.success('已通过授权')
        load()
      } catch (e) {
        message.error(e instanceof ApiError ? e.message : '操作失败')
      }
    },
  })
}

// ---------------- 驳回 ----------------
const rejectOpen = ref(false)
const rejecting = ref(false)
const rejectingRecord = ref<AppApiAuthVO | null>(null)
const rejectReason = ref('')

function openReject(record: AppApiAuthVO) {
  rejectingRecord.value = record
  rejectReason.value = ''
  rejectOpen.value = true
}

async function submitReject() {
  if (!rejectingRecord.value) return
  if (!rejectReason.value.trim()) {
    message.warning('请填写驳回原因')
    return
  }
  rejecting.value = true
  try {
    await consoleApi.reject(rejectingRecord.value.id, rejectReason.value.trim())
    message.success('已驳回该申请')
    rejectOpen.value = false
    load()
  } catch (e) {
    message.error(e instanceof ApiError ? e.message : '操作失败')
  } finally {
    rejecting.value = false
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

.cell-main {
  font-weight: 600;
  color: #1f2329;
}

.cell-sub {
  font-size: 12px;
  color: #8a9099;
  margin-top: 2px;
}

.mono {
  font-family: Consolas, 'Courier New', monospace;
  font-size: 12px;
  color: #3b4350;
  word-break: break-all;
}

.mono.sub {
  color: #a0a6b0;
}

.mini-tag {
  margin-right: 6px;
  transform: scale(0.9);
}

.reject-reason {
  color: #d4552a;
  font-size: 12px;
  margin-top: 2px;
}

.reject-tip {
  font-size: 13px;
  color: #5b6270;
  margin-bottom: 10px;
}
</style>
