<template>
  <div class="page">
    <div class="page-head">
      <div>
        <h2 class="page-title">调用统计</h2>
        <p class="page-desc">你名下应用的调用量、成功率、耗时与调用明细。</p>
      </div>
    </div>
    <StatsPanel :api="statsApi" />
  </div>
</template>

<script setup lang="ts">
import StatsPanel from '@/components/StatsPanel.vue'
import { statsApi } from '@/api'
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
