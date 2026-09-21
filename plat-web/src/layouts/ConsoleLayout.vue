<template>
  <a-layout style="min-height: 100vh">
    <a-layout-sider v-model:collapsed="collapsed" collapsible :width="230" theme="dark" breakpoint="lg">
      <div class="sider-brand">
        <div class="brand-logo">A</div>
        <span v-if="!collapsed" class="brand-name">开放平台运营</span>
      </div>
      <a-menu v-model:selectedKeys="selectedKeys" mode="inline" theme="dark" @click="onMenu">
        <a-menu-item key="/console/apis">
          <ApiOutlined /><span>API 元数据</span>
        </a-menu-item>
        <a-menu-item key="/console/approvals">
          <a-badge :count="pendingCount" :offset="[14, 0]" size="small">
            <AuditOutlined /><span style="margin-left:8px">审批工作台</span>
          </a-badge>
        </a-menu-item>
        <a-menu-item key="/console/apps">
          <AppstoreOutlined /><span>应用管理</span>
        </a-menu-item>
        <a-menu-item key="/console/tenants">
          <TeamOutlined /><span>租户管理</span>
        </a-menu-item>
        <a-menu-item key="/console/users">
          <UserOutlined /><span>用户管理</span>
        </a-menu-item>
        <a-menu-item key="/console/statistics">
          <BarChartOutlined /><span>全局统计</span>
        </a-menu-item>
      </a-menu>
    </a-layout-sider>

    <a-layout>
      <a-layout-header class="console-header">
        <div class="header-left">
          <span class="env-badge">运营管理后台</span>
        </div>
        <a-dropdown>
          <div class="header-user">
            <a-avatar :size="30" style="background: linear-gradient(135deg,#ff7d4d,#ff4d6d)">
              {{ avatarText }}
            </a-avatar>
            <span class="username">{{ auth.user?.nickname || auth.user?.username }}</span>
            <DownOutlined style="font-size: 10px; color: #8a9099" />
          </div>
          <template #overlay>
            <a-menu @click="onUserMenu">
              <a-menu-item key="portal"><DashboardOutlined /> 开发者门户</a-menu-item>
              <a-menu-item key="logout" danger><LogoutOutlined /> 退出登录</a-menu-item>
            </a-menu>
          </template>
        </a-dropdown>
      </a-layout-header>
      <a-layout-content style="background: #f5f7fa">
        <router-view />
      </a-layout-content>
    </a-layout>
  </a-layout>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { consoleApi } from '@/api'
import {
  ApiOutlined, AppstoreOutlined, AuditOutlined, BarChartOutlined, DashboardOutlined,
  DownOutlined, LogoutOutlined, TeamOutlined, UserOutlined,
} from '@ant-design/icons-vue'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const collapsed = ref(false)
const selectedKeys = ref<string[]>([route.path])
const pendingCount = ref(0)

watch(() => route.path, (p) => { selectedKeys.value = [p] })

onMounted(async () => {
  try {
    const page = await consoleApi.approvals({ pageNo: 1, pageSize: 1, status: 0 })
    pendingCount.value = page.total
  } catch {
    pendingCount.value = 0
  }
})

const avatarText = computed(() =>
  (auth.user?.nickname || auth.user?.username || '?').slice(0, 1).toUpperCase(),
)

function onMenu({ key }: { key: string }) {
  router.push(key as string)
}

function onUserMenu({ key }: { key: string }) {
  if (key === 'logout') {
    auth.logout()
    router.push('/login')
  } else if (key === 'portal') {
    router.push('/portal/dashboard')
  }
}
</script>

<style scoped>
.sider-brand {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 18px 16px 14px;
}

.brand-logo {
  width: 34px;
  height: 34px;
  border-radius: 9px;
  background: linear-gradient(135deg, #4f7cff, #22c1dc);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
  font-size: 16px;
  flex: none;
}

.brand-name {
  font-weight: 700;
  font-size: 15px;
  color: #fff;
  white-space: nowrap;
}

.console-header {
  background: #fff;
  border-bottom: 1px solid #eef0f3;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  height: 56px;
  line-height: 56px;
}

.env-badge {
  background: #fff1e8;
  color: #d4552a;
  font-size: 12px;
  padding: 3px 10px;
  border-radius: 999px;
}

.header-user {
  display: flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
  padding: 4px 8px;
  border-radius: 8px;
}

.header-user:hover {
  background: #f5f7fa;
}

.username {
  font-size: 13px;
  color: #1f2329;
}
</style>
