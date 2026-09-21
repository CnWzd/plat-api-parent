<template>
  <div class="page">
    <div class="page-head">
      <div>
        <a-breadcrumb style="margin-bottom: 8px">
          <a-breadcrumb-item>
            <router-link to="/portal/apps">应用管理</router-link>
          </a-breadcrumb-item>
          <a-breadcrumb-item>{{ app?.appName || '应用详情' }}</a-breadcrumb-item>
        </a-breadcrumb>
        <h2 class="page-title">{{ app?.appName || '加载中…' }}</h2>
        <p class="page-desc">管理应用凭证与 API 授权，签名调用请使用页底示例。</p>
      </div>
      <a-space>
        <a-button @click="openEdit">
          <template #icon><EditOutlined /></template>
          编辑信息
        </a-button>
        <a-button danger @click="confirmResetSecret">
          <template #icon><KeyOutlined /></template>
          重置 SecretKey
        </a-button>
      </a-space>
    </div>

    <a-spin :spinning="loading">
      <a-card :bordered="false" class="soft-card" title="基本信息">
        <a-descriptions :column="{ xs: 1, sm: 2, lg: 3 }" size="middle">
          <a-descriptions-item label="AppKey">
            <code class="mono">{{ app?.appKey }}</code>
            <a-button type="link" size="small" @click="copyText(app?.appKey ?? '')">复制</a-button>
          </a-descriptions-item>
          <a-descriptions-item label="SecretKey">
            <code class="mono masked">{{ app?.secretMasked }}</code>
            <span class="field-hint">已加密存储，仅创建 / 重置时可见一次</span>
          </a-descriptions-item>
          <a-descriptions-item label="状态">
            <a-tag :color="APP_STATUS[app?.status ?? 0]?.color">
              {{ APP_STATUS[app?.status ?? 0]?.text ?? '未知' }}
            </a-tag>
          </a-descriptions-item>
          <a-descriptions-item label="限流配额">{{ app?.rateLimitQps ?? '-' }} QPS</a-descriptions-item>
          <a-descriptions-item label="回调地址">{{ app?.callbackUrl || '-' }}</a-descriptions-item>
          <a-descriptions-item label="创建时间">{{ fmtDateTime(app?.createdAt) }}</a-descriptions-item>
          <a-descriptions-item label="备注" :span="3">{{ app?.remark || '-' }}</a-descriptions-item>
        </a-descriptions>
      </a-card>

      <a-card :bordered="false" class="soft-card" title="API 授权" style="margin-top: 16px">
        <template #extra>
          <a-button type="primary" @click="openApply">
            <template #icon><SafetyCertificateOutlined /></template>
            申请 API 授权
          </a-button>
        </template>
        <a-table :data-source="auths" :columns="authColumns" :pagination="false"
                 row-key="id" :scroll="{ x: 1000 }">
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'api'">
              <code class="mono">{{ (record as AppApiAuthVO).apiCode }}</code>
              <div class="api-sub">{{ (record as AppApiAuthVO).apiName }}</div>
            </template>
            <template v-else-if="column.key === 'endpoint'">
              <a-tag :color="methodColor((record as AppApiAuthVO).apiMethod ?? '')">
                {{ (record as AppApiAuthVO).apiMethod }}
              </a-tag>
              <code class="mono path">{{ (record as AppApiAuthVO).apiPath }}</code>
            </template>
            <template v-else-if="column.key === 'status'">
              <a-tag :color="AUTH_STATUS[(record as AppApiAuthVO).status]?.color">
                {{ AUTH_STATUS[(record as AppApiAuthVO).status]?.text ?? '未知' }}
              </a-tag>
            </template>
            <template v-else-if="column.key === 'appliedAt'">
              {{ fmtDateTime((record as AppApiAuthVO).appliedAt) }}
            </template>
            <template v-else-if="column.key === 'approvedAt'">
              {{ fmtDateTime((record as AppApiAuthVO).approvedAt) }}
            </template>
            <template v-else-if="column.key === 'remarkCol'">
              <div v-if="(record as AppApiAuthVO).rejectReason" class="reject-reason">
                驳回原因：{{ (record as AppApiAuthVO).rejectReason }}
              </div>
              <div v-else-if="(record as AppApiAuthVO).applyRemark" class="apply-remark">
                {{ (record as AppApiAuthVO).applyRemark }}
              </div>
              <span v-else>-</span>
            </template>
          </template>
        </a-table>
      </a-card>

      <a-card :bordered="false" class="soft-card" title="签名调用说明" style="margin-top: 16px">
        <a-steps :current="3" size="small" style="margin-bottom: 18px">
          <a-step title="获取凭证" description="创建应用得到 AK / SK" />
          <a-step title="申请授权" description="选择 API 提交审批" />
          <a-step title="平台审批" description="审批通过后可调用" />
          <a-step title="签名调用" description="HMAC-SHA256 请求网关" />
        </a-steps>
        <div class="sign-doc">
          <div class="sign-line"><span class="sign-key">请求头</span>
            <code>X-Api-Key: {{ app?.appKey }}</code>
          </div>
          <div class="sign-line"><span class="sign-key">签名串</span>
            <code>appKey + "\n" + timestamp + "\n" + method + "\n" + path</code>
          </div>
          <div class="sign-line"><span class="sign-key">签名算法</span>
            <code>sign = hex_lower(HMAC_SHA256(appSecret, 签名串))</code>
          </div>
          <div class="sign-line"><span class="sign-key">时间约束</span>
            <code>X-Timestamp 与服务端偏差不超过 300 秒</code>
          </div>
        </div>
      </a-card>
    </a-spin>

    <a-modal v-model:open="editOpen" title="编辑应用信息" :confirm-loading="editing" @ok="submitEdit">
      <a-form layout="vertical" style="margin-top: 8px">
        <a-form-item label="应用名称" required>
          <a-input v-model:value="editForm.appName" :maxlength="50" />
        </a-form-item>
        <a-form-item label="回调地址（可选）">
          <a-input v-model:value="editForm.callbackUrl" placeholder="https://example.com/callback" />
        </a-form-item>
        <a-form-item label="备注（可选）">
          <a-textarea v-model:value="editForm.remark" :rows="3" :maxlength="200" />
        </a-form-item>
      </a-form>
    </a-modal>

    <a-modal v-model:open="applyOpen" title="申请 API 授权" width="720px" :confirm-loading="applying"
             @ok="submitApply">
      <p class="apply-tip">仅展示已上架 API；已授权或审批中的 API 不可重复申请。</p>
      <a-table :data-source="apiOptions" :columns="apiColumns" :row-selection="rowSelection"
               :pagination="false" row-key="id" size="small" :scroll="{ y: 320 }">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'apiCode'">
            <code class="mono">{{ (record as ApiVO).apiCode }}</code>
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
        </template>
      </a-table>
      <a-form layout="vertical" style="margin-top: 12px">
        <a-form-item label="申请说明（可选）">
          <a-textarea v-model:value="applyRemark" :rows="2" :maxlength="200"
                      placeholder="说明调用场景与用途，便于平台审批" />
        </a-form-item>
      </a-form>
    </a-modal>

    <SecretRevealModal v-model:open="revealOpen" :reveal="reveal" />
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { message, Modal } from 'ant-design-vue'
import {
  EditOutlined, KeyOutlined, SafetyCertificateOutlined,
} from '@ant-design/icons-vue'
import SecretRevealModal from '@/components/SecretRevealModal.vue'
import { apiCatalogApi, appApi, approvalApi } from '@/api'
import { ApiError } from '@/api/client'
import { APP_STATUS, AUTH_STATUS, fmtDateTime, METHOD_COLOR } from '@/utils/format'
import type { ApiVO, AppApiAuthVO, AppVO, SecretRevealVO } from '@/types'

const route = useRoute()
const appId = computed(() => String(route.params.id ?? ''))

const app = ref<AppVO | null>(null)
const auths = ref<AppApiAuthVO[]>([])
const loading = ref(false)

async function load() {
  if (!appId.value) return
  loading.value = true
  try {
    const [detail, authList] = await Promise.all([
      appApi.detail(appId.value),
      approvalApi.listByApp(appId.value),
    ])
    app.value = detail
    auths.value = authList
  } catch (e) {
    message.error(e instanceof ApiError ? e.message : '加载应用详情失败')
  } finally {
    loading.value = false
  }
}

function methodColor(m: string): string {
  return METHOD_COLOR[m?.toUpperCase()] ?? 'default'
}

async function copyText(t: string) {
  if (!t) return
  try {
    await navigator.clipboard.writeText(t)
    message.success('已复制到剪贴板')
  } catch {
    message.warning('复制失败，请手动选择复制')
  }
}

const authColumns = [
  { title: 'API', key: 'api', width: 210 },
  { title: '端点', key: 'endpoint', width: 280 },
  { title: '授权状态', dataIndex: 'status', key: 'status', width: 100 },
  { title: '申请时间', key: 'appliedAt', width: 165 },
  { title: '审批时间', key: 'approvedAt', width: 165 },
  { title: '说明 / 驳回原因', key: 'remarkCol' },
]

// ---------------- 编辑 ----------------
const editOpen = ref(false)
const editing = ref(false)
const editForm = reactive({ appName: '', callbackUrl: '', remark: '' })

function openEdit() {
  if (!app.value) return
  editForm.appName = app.value.appName
  editForm.callbackUrl = app.value.callbackUrl ?? ''
  editForm.remark = app.value.remark ?? ''
  editOpen.value = true
}

async function submitEdit() {
  if (!editForm.appName.trim()) {
    message.warning('请输入应用名称')
    return
  }
  editing.value = true
  try {
    await appApi.update(appId.value, {
      appName: editForm.appName.trim(),
      callbackUrl: editForm.callbackUrl.trim() || undefined,
      remark: editForm.remark.trim() || undefined,
    })
    message.success('应用信息已更新')
    editOpen.value = false
    load()
  } catch (e) {
    message.error(e instanceof ApiError ? e.message : '更新失败')
  } finally {
    editing.value = false
  }
}

// ---------------- 重置 SK ----------------
const revealOpen = ref(false)
const reveal = ref<SecretRevealVO | null>(null)

function confirmResetSecret() {
  Modal.confirm({
    title: '重置 AppSecret',
    content: '重置后旧密钥立即失效，所有使用旧 SK 签名的调用将返回签名失败。确定继续？',
    okText: '确认重置',
    okType: 'danger',
    cancelText: '取消',
    onOk: async () => {
      try {
        reveal.value = await appApi.resetSecret(appId.value)
        revealOpen.value = true
        load()
      } catch (e) {
        message.error(e instanceof ApiError ? e.message : '重置失败')
      }
    },
  })
}

// ---------------- 申请授权 ----------------
const applyOpen = ref(false)
const applying = ref(false)
const apiOptions = ref<ApiVO[]>([])
const selectedApiIds = ref<string[]>([])
const applyRemark = ref('')

const apiColumns = [
  { title: 'API 编码', key: 'apiCode', width: 170 },
  { title: 'API 名称', dataIndex: 'apiName', key: 'apiName', width: 150 },
  { title: '端点', key: 'endpoint', ellipsis: true },
  { title: '鉴权', key: 'authType', width: 90 },
]

const existingApiIds = computed(
  () => new Set(auths.value.filter((a) => a.status !== 2).map((a) => a.apiId)),
)

const rowSelection = computed(() => ({
  selectedRowKeys: selectedApiIds.value,
  onChange: (keys: (string | number)[]) => {
    selectedApiIds.value = keys.map(String)
  },
  getCheckboxProps: (record: ApiVO) => ({
    disabled: existingApiIds.value.has(record.id),
  }),
}))

async function openApply() {
  selectedApiIds.value = []
  applyRemark.value = ''
  applyOpen.value = true
  try {
    apiOptions.value = (await apiCatalogApi.list()).filter((a) => a.status === 1)
  } catch {
    apiOptions.value = []
  }
}

async function submitApply() {
  if (!selectedApiIds.value.length) {
    message.warning('请至少选择一个 API')
    return
  }
  applying.value = true
  try {
    await approvalApi.apply(appId.value, {
      apiIds: selectedApiIds.value,
      applyRemark: applyRemark.value.trim() || undefined,
    })
    message.success('授权申请已提交，请等待平台审批')
    applyOpen.value = false
    load()
  } catch (e) {
    message.error(e instanceof ApiError ? e.message : '提交失败')
  } finally {
    applying.value = false
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

.mono {
  font-family: Consolas, 'Courier New', monospace;
  font-size: 12px;
  color: #3b4350;
  word-break: break-all;
}

.mono.masked {
  color: #a0a6b0;
}

.mono.path {
  margin-left: 6px;
}

.field-hint {
  display: block;
  font-size: 12px;
  color: #a0a6b0;
  margin-top: 2px;
}

.api-sub {
  font-size: 12px;
  color: #a0a6b0;
  margin-top: 2px;
}

.reject-reason {
  color: #d4552a;
  font-size: 12px;
}

.apply-remark {
  color: #8a9099;
  font-size: 12px;
}

.apply-tip {
  font-size: 12px;
  color: #8a9099;
  margin-bottom: 10px;
}

.sign-doc {
  background: #f7f9fc;
  border: 1px solid #eef0f3;
  border-radius: 10px;
  padding: 14px 18px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.sign-line {
  display: flex;
  gap: 12px;
  align-items: baseline;
}

.sign-key {
  flex: none;
  width: 74px;
  font-size: 12px;
  color: #8a9099;
}

.sign-line code {
  font-family: Consolas, 'Courier New', monospace;
  font-size: 12.5px;
  color: #3b4350;
  word-break: break-all;
}
</style>
