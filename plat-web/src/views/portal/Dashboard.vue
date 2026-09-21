<template>
  <div class="page">
    <div class="page-head">
      <div>
        <h2 class="page-title">控制台</h2>
        <p class="page-desc">欢迎回来，{{ displayName }}。这里展示你名下应用的调用概况。</p>
      </div>
      <a-button type="primary" @click="router.push('/portal/apps')">
        <template #icon><AppstoreOutlined /></template>
        管理我的应用
      </a-button>
    </div>

    <StatsPanel :api="statsApi" :show-logs="false" />
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { AppstoreOutlined } from '@ant-design/icons-vue'
import StatsPanel from '@/components/StatsPanel.vue'
import { statsApi } from '@/api'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const auth = useAuthStore()

const displayName = computed(() => auth.user?.nickname || auth.user?.username || '开发者')
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
</style>
