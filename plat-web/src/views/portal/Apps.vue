<template>
  <div class="page">
    <div class="page-head">
      <div>
        <h2 class="page-title">应用管理</h2>
        <p class="page-desc">管理你的接入应用。创建成功后立即下发 AppKey / AppSecret 凭证。</p>
      </div>
      <a-button type="primary" @click="openCreate">
        <template #icon><PlusOutlined /></template>
        创建应用
      </a-button>
    </div>

    <a-card :bordered="false" class="soft-card">
      <div class="table-filter">
        <a-input-search v-model:value="keyword" placeholder="按应用名称 / AppKey 搜索" style="width: 300px"
                        allow-clear @search="reload" />
        <a-button @click="reload">
          <template #icon><ReloadOutlined /></template>
          刷新
        </a-button>
      </div>
      <a-table :data-source="records" :columns="columns" :loading="loading" :pagination="pagination"
               row-key="id" :scroll="{ x: 980 }" @change="onChange">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'appName'">
            <div class="app-name">{{ (record as AppVO).appName }}</div>
            <div class="app-sub">{{ (record as AppVO).remark || '暂无备注' }}</div>
          </template>
          <template v-else-if="column.key === 'appKey'">
            <code class="mono">{{ (record as AppVO).appKey }}</code>
          </template>
          <template v-else-if="column.key === 'status'">
            <a-tag :color="APP_STATUS[(record as AppVO).status]?.color">
              {{ APP_STATUS[(record as AppVO).status]?.text ?? '未知' }}
            </a-tag>
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
            <a-button type="link" size="small" @click="goDetail((record as AppVO).id)">详情 / 授权</a-button>
          </template>
        </template>
      </a-table>
    </a-card>

    <a-modal v-model:open="createOpen" title="创建应用" :confirm-loading="creating" @ok="submitCreate">
      <a-form layout="vertical" style="margin-top: 8px">
        <a-form-item label="应用名称" required>
          <a-input v-model:value="createForm.appName" placeholder="2~50 个字符" :maxlength="50" />
        </a-form-item>
        <a-form-item label="回调地址（可选）">
          <a-input v-model:value="createForm.callbackUrl" placeholder="https://example.com/callback" />
        </a-form-item>
        <a-form-item label="备注（可选）">
          <a-textarea v-model:value="createForm.remark" :rows="3" :maxlength="200" placeholder="用途说明" />
        </a-form-item>
      </a-form>
    </a-modal>

    <SecretRevealModal v-model:open="revealOpen" :reveal="reveal" />
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { PlusOutlined, ReloadOutlined } from '@ant-design/icons-vue'
import SecretRevealModal from '@/components/SecretRevealModal.vue'
import { appApi } from '@/api'
import { ApiError } from '@/api/client'
import { APP_STATUS, fmtDateTime } from '@/utils/format'
import type { AppVO, SecretRevealVO } from '@/types'

const router = useRouter()

const records = ref<AppVO[]>([])
const loading = ref(false)
const keyword = ref('')
const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
  showTotal: (t: number) => `共 ${t} 条`,
})

const columns = [
  { title: '应用', dataIndex: 'appName', key: 'appName' },
  { title: 'AppKey', dataIndex: 'appKey', key: 'appKey', width: 190 },
  { title: '状态', dataIndex: 'status', key: 'status', width: 85 },
  { title: '限流 (QPS)', dataIndex: 'rateLimitQps', key: 'rateLimitQps', width: 95, align: 'right' as const },
  { title: 'API 授权', key: 'apiCount', width: 170 },
  { title: '创建时间', dataIndex: 'createdAt', key: 'createdAt', width: 170 },
  { title: '操作', key: 'action', width: 120 },
]

async function load() {
  loading.value = true
  try {
    const page = await appApi.page({
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

function goDetail(id: string) {
  router.push(`/portal/apps/${id}`)
}

// ---------------- 创建应用 ----------------
const createOpen = ref(false)
const creating = ref(false)
const createForm = reactive({ appName: '', callbackUrl: '', remark: '' })

const revealOpen = ref(false)
const reveal = ref<SecretRevealVO | null>(null)

function openCreate() {
  createForm.appName = ''
  createForm.callbackUrl = ''
  createForm.remark = ''
  createOpen.value = true
}

async function submitCreate() {
  if (!createForm.appName.trim()) {
    message.warning('请输入应用名称')
    return
  }
  creating.value = true
  try {
    reveal.value = await appApi.create({
      appName: createForm.appName.trim(),
      callbackUrl: createForm.callbackUrl.trim() || undefined,
      remark: createForm.remark.trim() || undefined,
    })
    createOpen.value = false
    revealOpen.value = true
    reload()
  } catch (e) {
    message.error(e instanceof ApiError ? e.message : '创建失败，请稍后重试')
  } finally {
    creating.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.page {
  padding: 24px;
}

.page-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
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

.app-name {
  font-weight: 600;
  color: #1f2329;
}

.app-sub {
  font-size: 12px;
  color: #a0a6b0;
  max-width: 260px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.mono {
  font-family: Consolas, 'Courier New', monospace;
  font-size: 12px;
  color: #3b4350;
}
</style>
