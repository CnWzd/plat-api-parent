<template>
  <div class="page">
    <div class="page-head">
      <div>
        <h2 class="page-title">API 元数据</h2>
        <p class="page-desc">维护开放 API 目录：端点定义、鉴权方式与上下架状态。</p>
      </div>
      <a-button type="primary" @click="openCreate">
        <template #icon><PlusOutlined /></template>
        新建 API
      </a-button>
    </div>

    <a-card :bordered="false" class="soft-card">
      <div class="table-filter">
        <a-input-search v-model:value="keyword" placeholder="按编码 / 名称 / 路径搜索" style="width: 280px"
                        allow-clear @search="reload" />
        <a-select v-model:value="statusFilter" style="width: 130px" :options="statusOptions"
                  placeholder="状态" @change="reload" />
        <a-button @click="reload">
          <template #icon><ReloadOutlined /></template>
          刷新
        </a-button>
      </div>
      <a-table :data-source="records" :columns="columns" :loading="loading" :pagination="pagination"
               row-key="id" :scroll="{ x: 1100 }" @change="onChange">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'apiCode'">
            <code class="mono">{{ (record as ApiVO).apiCode }}</code>
          </template>
          <template v-else-if="column.key === 'apiName'">
            <div class="api-name">{{ (record as ApiVO).apiName }}</div>
            <div class="api-sub">{{ (record as ApiVO).description || '暂无描述' }}</div>
          </template>
          <template v-else-if="column.key === 'category'">
            <a-tag color="blue">{{ (record as ApiVO).category }}</a-tag>
          </template>
          <template v-else-if="column.key === 'endpoint'">
            <a-tag :color="methodColor((record as ApiVO).method)">{{ (record as ApiVO).method }}</a-tag>
            <code class="mono path">{{ (record as ApiVO).path }}</code>
          </template>
          <template v-else-if="column.key === 'authType'">
            <a-tag :color="(record as ApiVO).authType === 'SIGN' ? 'purple' : 'default'">
              {{ (record as ApiVO).authType === 'SIGN' ? '签名' : '免鉴权' }}
            </a-tag>
          </template>
          <template v-else-if="column.key === 'status'">
            <a-tag :color="API_STATUS[(record as ApiVO).status]?.color">
              {{ API_STATUS[(record as ApiVO).status]?.text ?? '未知' }}
            </a-tag>
          </template>
          <template v-else-if="column.key === 'updatedAt'">
            {{ fmtDateTime((record as ApiVO).updatedAt) }}
          </template>
          <template v-else-if="column.key === 'action'">
            <a-space :size="4">
              <a-button type="link" size="small" @click="openEdit(record as ApiVO)">编辑</a-button>
              <a-button v-if="(record as ApiVO).status !== 1" type="link" size="small"
                        @click="changeStatus((record as ApiVO), 1)">上架</a-button>
              <a-button v-else type="link" size="small" danger
                        @click="changeStatus((record as ApiVO), 2)">下架</a-button>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>

    <a-modal v-model:open="formOpen" :title="editingId ? '编辑 API' : '新建 API'" :confirm-loading="saving"
             @ok="submitForm">
      <a-form layout="vertical" style="margin-top: 8px">
        <a-row :gutter="12">
          <a-col :span="12">
            <a-form-item label="API 编码" required>
              <a-input v-model:value="form.apiCode" :disabled="!!editingId" placeholder="如 weather.query"
                       :maxlength="64" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="API 名称" required>
              <a-input v-model:value="form.apiName" :maxlength="64" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="12">
          <a-col :span="8">
            <a-form-item label="分类" required>
              <a-input v-model:value="form.category" placeholder="如 气象" :maxlength="32" />
            </a-form-item>
          </a-col>
          <a-col :span="8">
            <a-form-item label="请求方法" required>
              <a-select v-model:value="form.method" :options="methodOptions" />
            </a-form-item>
          </a-col>
          <a-col :span="8">
            <a-form-item label="版本" required>
              <a-input v-model:value="form.version" placeholder="v1" :maxlength="16" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-form-item label="请求路径" required>
          <a-input v-model:value="form.path" placeholder="/openapi/weather" :maxlength="200">
            <template #prefix><span class="path-prefix">网关</span></template>
          </a-input>
        </a-form-item>
        <a-form-item label="鉴权方式" required>
          <a-radio-group v-model:value="form.authType">
            <a-radio value="SIGN">签名（AppKey + HMAC-SHA256）</a-radio>
            <a-radio value="NONE">免鉴权</a-radio>
          </a-radio-group>
        </a-form-item>
        <a-form-item label="描述（可选）">
          <a-textarea v-model:value="form.description" :rows="3" :maxlength="300" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { message, Modal } from 'ant-design-vue'
import { PlusOutlined, ReloadOutlined } from '@ant-design/icons-vue'
import { consoleApi } from '@/api'
import { ApiError } from '@/api/client'
import { API_STATUS, fmtDateTime, METHOD_COLOR } from '@/utils/format'
import type { ApiVO } from '@/types'

const records = ref<ApiVO[]>([])
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
  { value: 0, label: '草稿' },
  { value: 1, label: '已上架' },
  { value: 2, label: '已下架' },
]

const methodOptions = ['GET', 'POST', 'PUT', 'DELETE', 'PATCH'].map((m) => ({ value: m, label: m }))

const columns = [
  { title: 'API 编码', dataIndex: 'apiCode', key: 'apiCode', width: 160 },
  { title: '名称 / 描述', dataIndex: 'apiName', key: 'apiName', width: 200 },
  { title: '分类', dataIndex: 'category', key: 'category', width: 90 },
  { title: '端点', key: 'endpoint', width: 280 },
  { title: '版本', dataIndex: 'version', key: 'version', width: 70 },
  { title: '鉴权', key: 'authType', width: 90 },
  { title: '状态', dataIndex: 'status', key: 'status', width: 90 },
  { title: '更新时间', key: 'updatedAt', width: 165 },
  { title: '操作', key: 'action', width: 130 },
]

function methodColor(m: string): string {
  return METHOD_COLOR[m?.toUpperCase()] ?? 'default'
}

async function load() {
  loading.value = true
  try {
    const page = await consoleApi.apis({
      pageNo: pagination.current,
      pageSize: pagination.pageSize,
      keyword: keyword.value || undefined,
      status: statusFilter.value ?? null,
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

// ---------------- 新建 / 编辑 ----------------
const formOpen = ref(false)
const saving = ref(false)
const editingId = ref<string | null>(null)
const form = reactive({
  apiCode: '',
  apiName: '',
  category: '',
  method: 'GET',
  path: '',
  version: 'v1',
  authType: 'SIGN',
  description: '',
})

function openCreate() {
  editingId.value = null
  Object.assign(form, {
    apiCode: '', apiName: '', category: '', method: 'GET',
    path: '', version: 'v1', authType: 'SIGN', description: '',
  })
  formOpen.value = true
}

function openEdit(record: ApiVO) {
  editingId.value = record.id
  Object.assign(form, {
    apiCode: record.apiCode,
    apiName: record.apiName,
    category: record.category,
    method: record.method,
    path: record.path,
    version: record.version,
    authType: record.authType,
    description: record.description ?? '',
  })
  formOpen.value = true
}

function validate(): string | null {
  if (!form.apiCode.trim()) return '请输入 API 编码'
  if (!form.apiName.trim()) return '请输入 API 名称'
  if (!form.category.trim()) return '请输入分类'
  if (!form.path.trim()) return '请输入请求路径'
  if (!form.path.trim().startsWith('/')) return '请求路径必须以 / 开头'
  if (!form.version.trim()) return '请输入版本号'
  return null
}

async function submitForm() {
  const err = validate()
  if (err) {
    message.warning(err)
    return
  }
  const payload: Record<string, string> = {
    apiCode: form.apiCode.trim(),
    apiName: form.apiName.trim(),
    category: form.category.trim(),
    method: form.method,
    path: form.path.trim(),
    version: form.version.trim(),
    authType: form.authType,
    description: form.description.trim(),
  }
  saving.value = true
  try {
    if (editingId.value) {
      await consoleApi.updateApi(editingId.value, payload)
      message.success('API 信息已更新')
    } else {
      await consoleApi.createApi(payload)
      message.success('API 已创建（草稿状态），上架后对开发者可见')
    }
    formOpen.value = false
    load()
  } catch (e) {
    message.error(e instanceof ApiError ? e.message : '保存失败')
  } finally {
    saving.value = false
  }
}

// ---------------- 上下架 ----------------
function changeStatus(record: ApiVO, target: number) {
  const label = target === 1 ? '上架' : '下架'
  Modal.confirm({
    title: `确认${label}该 API？`,
    content: target === 1
      ? `「${record.apiName}」上架后，开发者可申请该 API 的调用授权。`
      : `「${record.apiName}」下架后，已授权应用的调用将被网关拒绝。`,
    okText: `确认${label}`,
    cancelText: '取消',
    onOk: async () => {
      try {
        await consoleApi.updateApiStatus(record.id, target)
        message.success(`已${label}`)
        load()
      } catch (e) {
        message.error(e instanceof ApiError ? e.message : `${label}失败`)
      }
    },
  })
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

.api-name {
  font-weight: 600;
  color: #1f2329;
}

.api-sub {
  font-size: 12px;
  color: #a0a6b0;
  max-width: 200px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.mono {
  font-family: Consolas, 'Courier New', monospace;
  font-size: 12px;
  color: #3b4350;
}

.mono.path {
  margin-left: 6px;
}

.path-prefix {
  color: #a0a6b0;
  font-size: 12px;
}
</style>
