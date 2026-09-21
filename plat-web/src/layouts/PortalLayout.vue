<template>
  <a-layout style="min-height: 100vh">
    <a-layout-sider v-model:collapsed="collapsed" collapsible :width="220" theme="light"
                    class="portal-sider" breakpoint="lg">
      <div class="sider-brand">
        <div class="brand-logo">A</div>
        <span v-if="!collapsed" class="brand-name">API 开放平台</span>
      </div>
      <a-menu v-model:selectedKeys="selectedKeys" mode="inline" @click="onMenu">
        <a-menu-item key="/portal/dashboard">
          <DashboardOutlined /><span>控制台</span>
        </a-menu-item>
        <a-menu-item key="/portal/apps">
          <AppstoreOutlined /><span>应用管理</span>
        </a-menu-item>
        <a-menu-item key="/portal/statistics">
          <BarChartOutlined /><span>调用统计</span>
        </a-menu-item>
        <a-menu-item v-if="auth.isAdmin" key="/console/apis">
          <SettingOutlined /><span>运营后台</span>
        </a-menu-item>
      </a-menu>
    </a-layout-sider>

    <a-layout>
      <a-layout-header class="portal-header">
        <div class="header-left">
          <span class="env-badge">开发者门户</span>
        </div>
        <a-dropdown>
          <div class="header-user">
            <a-avatar :size="30" style="background: linear-gradient(135deg,#4f7cff,#22c1dc)">
              {{ avatarText }}
            </a-avatar>
            <span class="username">{{ auth.user?.nickname || auth.user?.username }}</span>
            <DownOutlined style="font-size: 10px; color: #8a9099" />
          </div>
          <template #overlay>
            <a-menu @click="onUserMenu">
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
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import {
  AppstoreOutlined, BarChartOutlined, DashboardOutlined, DownOutlined,
  LogoutOutlined, SettingOutlined,
} from '@ant-design/icons-vue'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const collapsed = ref(false)
const selectedKeys = ref<string[]>([route.path])

watch(() => route.path, (p) => { selectedKeys.value = [p] })

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
  }
}
</script>

<style scoped>
.portal-sider {
  border-right: 1px solid #eef0f3;
  position: sticky;
  top: 0;
  height: 100vh;
  overflow: auto;
}

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
  color: #1f2329;
  white-space: nowrap;
}

.portal-header {
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
  background: #eef4ff;
  color: #3358d4;
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
