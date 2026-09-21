<template>
  <div class="page">
    <div class="page-head">
      <div>
        <h2 class="page-title">全局统计</h2>
        <p class="page-desc">全平台应用的调用量、成功率、耗时与调用明细审计。</p>
      </div>
    </div>
    <StatsPanel :api="consoleStats" />
  </div>
</template>

<script setup lang="ts">
import StatsPanel from '@/components/StatsPanel.vue'
import type { StatsApiShape } from '@/components/StatsPanel.vue'
import { consoleApi } from '@/api'

const consoleStats: StatsApiShape = {
  overview: consoleApi.statsOverview,
  trend: consoleApi.statsTrend,
  topApis: consoleApi.statsTopApis,
  logs: consoleApi.statsLogs,
}
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
</style>
