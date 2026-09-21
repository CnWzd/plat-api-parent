<template>
  <div class="page">
    <div class="page-head">
      <div>
        <h2 class="page-title">租户管理</h2>
        <p class="page-desc">接入企业与团队的隔离单元，开发者账号归属所属租户。</p>
      </div>
      <a-button type="primary" @click="openCreate">
        <template #icon><PlusOutlined /></template>
        新建租户
      </a-button>
    </div>

    <a-card :bordered="false" class="soft-card">
      <div class="table-filter">
        <a-input-search v-model:value="keyword" placeholder="按编码 / 名称 / 联系人搜索" style="width: 300px"
                        allow-clear @search="reload" />
        <a-button @click="reload">
          <template #icon><ReloadOutlined /></template>
          刷新
        </a-button>
      </div>
      <a-table :data-source="records" :columns="columns" :loading="loading" :pagination="pagination"
               row-key="id" :scroll="{ x: 950 }" @change="onChange">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'tenantCode'">
            <code class="mono">{{ (record as TenantVO).tenantCode }}</code>
          </template>
          <template v-else-if="column.key === 'tenantName'">
            <div class="cell-main">{{ (record as TenantVO).tenantName }}</div>
          </template>
          <template v-else-if="column.key === 'status'">
            <a-tag :color="TENANT_STATUS[(record as TenantVO).status]?.color">
              {{ TENANT_STATUS[(record as TenantVO).status]?.text ?? '未知' }}
            </a-tag>
          </template>
          <template v-else-if="column.key === 'developerCount'">
            <a-badge :count="(record as TenantVO).developerCount" :number-style="{ backgroundColor: '#4f7cff' }"
                     :overflow-count="999" />
          </template>
          <template v-else-if="column.key === 'createdAt'">
            {{ fmtDateTime((record as TenantVO).createdAt) }}
          </template>
          <template v-else-if="column.key === 'action'">
            <a-space :size="4">
              <a-button type="link" size="small" @click="openEdit(record as TenantVO)">编辑</a-button>
              <a-button v-if="(record as TenantVO).status === 1" type="link" size="small" danger
                        @click="toggleStatus(record as TenantVO, 0)">停用</a-button>
              <a-button v-else type="link" size="small"
                        @click="toggleStatus(record as TenantVO, 1)">启用</a-button>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>

    <a-modal v-model:open="formOpen" :title="editingId ? '编辑租户' : '新建租户'" :confirm-loading="saving"
             @ok="submitForm">
      <a-form layout="vertical" style="margin-top: 8px">
        <a-form-item label="租户编码" required>
          <a-input v-model:value="form.tenantCode" :disabled="!!editingId" placeholder="如 T002（唯一标识）"
                   :maxlength="32" />
        </a-form-item>
        <a-form-item label="租户名称" required>
          <a-input v-model:value="form.tenantName" :maxlength="64" />
        </a-form-item>
        <a-row :gutter="12">
          <a-col :span="12">
            <a-form-item label="联系人（可选）">
              <a-input v-model:value="form.contactName" :maxlength="32" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="联系电话（可选）">
              <a-input v-model:value="form.contactPhone" :maxlength="20" />
            </a-form-item>
          </a-col>
        </a-row>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { message, Modal } from 'ant-design-vue'
import { PlusOutlined, ReloadOutlined } from '@ant-design/icons-vue'
import { tenantApi } from '@/api'
import { ApiError } from '@/api/client'
import { fmtDateTime } from '@/utils/format'
import type { TenantVO } from '@/types'

const TENANT_STATUS: Record<number, { text: string; color: string }> = {
  0: { text: '停用', color: 'red' },
  1: { text: '正常', color: 'green' },
}

const records = ref<TenantVO[]>([])
const loading = ref(false)
const keyword = ref('')

const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
  showTotal: (t: number) => `共 ${t} 条`,
})

const columns = [
  { title: '租户编码', dataIndex: 'tenantCode', key: 'tenantCode', width: 130 },
  { title: '租户名称', dataIndex: 'tenantName', key: 'tenantName', width: 180 },
  { title: '联系人', dataIndex: 'contactName', key: 'contactName', width: 110 },
  { title: '联系电话', dataIndex: 'contactPhone', key: 'contactPhone', width: 130 },
  { title: '开发者数', key: 'developerCount', width: 100, align: 'center' as const },
  { title: '状态', dataIndex: 'status', key: 'status', width: 85 },
  { title: '创建时间', key: 'createdAt', width: 170 },
  { title: '操作', key: 'action', width: 120 },
]

async function load() {
  loading.value = true
  try {
    const page = await tenantApi.page({
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

// ---------------- 新建 / 编辑 ----------------
const formOpen = ref(false)
const saving = ref(false)
const editingId = ref<string | null>(null)
const form = reactive({ tenantCode: '', tenantName: '', contactName: '', contactPhone: '' })

function openCreate() {
  editingId.value = null
  Object.assign(form, { tenantCode: '', tenantName: '', contactName: '', contactPhone: '' })
  formOpen.value = true
}

function openEdit(record: TenantVO) {
  editingId.value = record.id
  Object.assign(form, {
    tenantCode: record.tenantCode,
    tenantName: record.tenantName,
    contactName: record.contactName ?? '',
    contactPhone: record.contactPhone ?? '',
  })
  formOpen.value = true
}

async function submitForm() {
  if (!editingId.value && !form.tenantCode.trim()) {
    message.warning('请输入租户编码')
    return
  }
  if (!form.tenantName.trim()) {
    message.warning('请输入租户名称')
    return
  }
  saving.value = true
  try {
    if (editingId.value) {
      await tenantApi.update(editingId.value, {
        tenantName: form.tenantName.trim(),
        contactName: form.contactName.trim() || undefined,
        contactPhone: form.contactPhone.trim() || undefined,
      })
      message.success('租户信息已更新')
    } else {
      await tenantApi.create({
        tenantCode: form.tenantCode.trim(),
        tenantName: form.tenantName.trim(),
        contactName: form.contactName.trim() || undefined,
        contactPhone: form.contactPhone.trim() || undefined,
      })
      message.success('租户已创建')
    }
    formOpen.value = false
    load()
  } catch (e) {
    message.error(e instanceof ApiError ? e.message : '保存失败')
  } finally {
    saving.value = false
  }
}

// ---------------- 启停 ----------------
function toggleStatus(record: TenantVO, target: number) {
  const action = target === 1 ? '启用' : '停用'
  Modal.confirm({
    title: `确认${action}该租户？`,
    content: target === 0
      ? `停用后「${record.tenantName}」下的开发者将无法登录。`
      : `启用后「${record.tenantName}」下的开发者恢复访问。`,
    okText: `确认${action}`,
    okType: target === 0 ? 'danger' : 'primary',
    cancelText: '取消',
    onOk: async () => {
      try {
        await tenantApi.updateStatus(record.id, target)
        message.success(`已${action}`)
        load()
      } catch (e) {
        message.error(e instanceof ApiError ? e.message : '操作失败')
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

.cell-main {
  font-weight: 600;
  color: #1f2329;
}

.mono {
  font-family: Consolas, 'Courier New', monospace;
  font-size: 12px;
  color: #3b4350;
}
</style>
