<template>
  <div class="page">
    <div class="page-head">
      <div>
        <h2 class="page-title">用户管理</h2>
        <p class="page-desc">平台账号的启停控制与密码重置，角色在注册 / 创建时确定。</p>
      </div>
    </div>

    <a-card :bordered="false" class="soft-card">
      <div class="table-filter">
        <a-input-search v-model:value="keyword" placeholder="按用户名 / 昵称搜索" style="width: 280px"
                        allow-clear @search="reload" />
        <a-select v-model:value="roleFilter" style="width: 140px" :options="roleOptions"
                  placeholder="角色" allow-clear @change="reload" />
        <a-button @click="reload">
          <template #icon><ReloadOutlined /></template>
          刷新
        </a-button>
      </div>
      <a-table :data-source="records" :columns="columns" :loading="loading" :pagination="pagination"
               row-key="id" :scroll="{ x: 980 }" @change="onChange">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'username'">
            <div class="cell-main">
              <a-avatar :size="22" class="user-avatar">{{ avatarOf(record as ConsoleUserVO) }}</a-avatar>
              <span style="margin-left: 8px">{{ (record as ConsoleUserVO).username }}</span>
            </div>
            <div class="cell-sub">{{ (record as ConsoleUserVO).nickname || '未设置昵称' }}</div>
          </template>
          <template v-else-if="column.key === 'role'">
            <a-tag :color="(record as ConsoleUserVO).role === 'ADMIN' ? 'volcano' : 'geekblue'">
              {{ (record as ConsoleUserVO).role === 'ADMIN' ? '管理员' : '开发者' }}
            </a-tag>
          </template>
          <template v-else-if="column.key === 'contact'">
            <div class="cell-sub">{{ (record as ConsoleUserVO).email || '-' }}</div>
            <div class="cell-sub">{{ (record as ConsoleUserVO).phone || '-' }}</div>
          </template>
          <template v-else-if="column.key === 'status'">
            <a-tag :color="USER_STATUS[(record as ConsoleUserVO).status]?.color">
              {{ USER_STATUS[(record as ConsoleUserVO).status]?.text ?? '未知' }}
            </a-tag>
          </template>
          <template v-else-if="column.key === 'createdAt'">
            {{ fmtDateTime((record as ConsoleUserVO).createdAt) }}
          </template>
          <template v-else-if="column.key === 'action'">
            <a-space :size="4">
              <a-button v-if="(record as ConsoleUserVO).status === 1" type="link" size="small" danger
                        @click="toggleStatus(record as ConsoleUserVO, 0)">禁用</a-button>
              <a-button v-else type="link" size="small"
                        @click="toggleStatus(record as ConsoleUserVO, 1)">启用</a-button>
              <a-button type="link" size="small" @click="confirmResetPwd(record as ConsoleUserVO)">重置密码</a-button>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>

    <a-modal v-model:open="pwdOpen" title="新密码下发" :footer="null">
      <a-alert type="warning" show-icon style="margin-bottom: 14px"
               message="新密码仅此一次完整展示，请复制后告知用户。" />
      <div class="pwd-field">
        <span class="pwd-label">用户</span>
        <span>{{ pwdResult?.username }}</span>
      </div>
      <div class="pwd-field">
        <span class="pwd-label">新密码</span>
        <code class="pwd-value">{{ pwdResult?.newPassword }}</code>
        <a-button size="small" @click="copyPwd">复制</a-button>
      </div>
      <div style="text-align: right; margin-top: 18px">
        <a-button type="primary" @click="pwdOpen = false">我已妥善保存，关闭</a-button>
      </div>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { message, Modal } from 'ant-design-vue'
import { ReloadOutlined } from '@ant-design/icons-vue'
import { userApi } from '@/api'
import { ApiError } from '@/api/client'
import { fmtDateTime, USER_STATUS } from '@/utils/format'
import type { ConsoleUserVO } from '@/types'

const records = ref<ConsoleUserVO[]>([])
const loading = ref(false)
const keyword = ref('')
const roleFilter = ref<string | undefined>(undefined)

const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
  showTotal: (t: number) => `共 ${t} 条`,
})

const roleOptions = [
  { value: 'ADMIN', label: '管理员' },
  { value: 'DEVELOPER', label: '开发者' },
]

const columns = [
  { title: '用户', dataIndex: 'username', key: 'username', width: 190 },
  { title: '角色', key: 'role', width: 100 },
  { title: '联系方式', key: 'contact', width: 200 },
  { title: '状态', dataIndex: 'status', key: 'status', width: 85 },
  { title: '注册时间', key: 'createdAt', width: 170 },
  { title: '操作', key: 'action', width: 160 },
]

function avatarOf(u: ConsoleUserVO): string {
  return (u.nickname || u.username || '?').slice(0, 1).toUpperCase()
}

async function load() {
  loading.value = true
  try {
    const page = await userApi.page({
      pageNo: pagination.current,
      pageSize: pagination.pageSize,
      keyword: keyword.value || undefined,
      role: roleFilter.value || undefined,
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

// ---------------- 启停 ----------------
function toggleStatus(record: ConsoleUserVO, target: number) {
  const action = target === 1 ? '启用' : '禁用'
  Modal.confirm({
    title: `确认${action}该账号？`,
    content: target === 0
      ? `禁用后「${record.username}」将无法登录与调用。`
      : `启用后「${record.username}」恢复正常访问。`,
    okText: `确认${action}`,
    okType: target === 0 ? 'danger' : 'primary',
    cancelText: '取消',
    onOk: async () => {
      try {
        await userApi.updateStatus(record.id, target)
        message.success(`已${action}`)
        load()
      } catch (e) {
        message.error(e instanceof ApiError ? e.message : '操作失败')
      }
    },
  })
}

// ---------------- 重置密码 ----------------
const pwdOpen = ref(false)
const pwdResult = ref<{ userId: string; username: string; newPassword: string } | null>(null)

function confirmResetPwd(record: ConsoleUserVO) {
  Modal.confirm({
    title: '重置该用户密码？',
    content: `将为「${record.username}」生成随机新密码，旧密码立即失效。`,
    okText: '确认重置',
    cancelText: '取消',
    onOk: async () => {
      try {
        pwdResult.value = await userApi.resetPassword(record.id)
        pwdOpen.value = true
        message.success('密码已重置')
      } catch (e) {
        message.error(e instanceof ApiError ? e.message : '重置失败')
      }
    },
  })
}

async function copyPwd() {
  if (!pwdResult.value) return
  try {
    await navigator.clipboard.writeText(pwdResult.value.newPassword)
    message.success('已复制到剪贴板')
  } catch {
    message.warning('复制失败，请手动选择复制')
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
  display: flex;
  align-items: center;
}

.cell-sub {
  font-size: 12px;
  color: #a0a6b0;
  margin-top: 2px;
}

.user-avatar {
  background: linear-gradient(135deg, #4f7cff, #22c1dc);
  font-size: 12px;
  font-weight: 600;
  flex: none;
}

.pwd-field {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 12px;
}

.pwd-label {
  width: 56px;
  font-size: 12px;
  color: #8a9099;
  flex: none;
}

.pwd-value {
  flex: 1;
  background: #fff7f2;
  border: 1px solid #ffd9c4;
  border-radius: 6px;
  padding: 8px 12px;
  font-family: Consolas, monospace;
  font-size: 13px;
  color: #d4552a;
  word-break: break-all;
}
</style>
