<template>
  <a-modal :open="open" title="SecretKey 一次性展示" :footer="null" :mask-closable="false"
           :closable="true" @cancel="$emit('update:open', false)" width="560px">
    <a-alert v-if="reveal" :message="reveal.notice" type="warning" show-icon style="margin-bottom: 16px" />
    <template v-if="reveal">
      <div class="secret-field">
        <div class="secret-label">AppKey (AK)</div>
        <div class="secret-row">
          <code class="secret-value">{{ reveal.appKey }}</code>
          <a-button size="small" @click="copy(reveal.appKey)">复制</a-button>
        </div>
      </div>
      <div class="secret-field">
        <div class="secret-label">AppSecret (SK)</div>
        <div class="secret-row">
          <code class="secret-value sk">{{ reveal.appSecret }}</code>
          <a-button size="small" @click="copy(reveal.appSecret)">复制</a-button>
        </div>
      </div>
    </template>
    <div style="text-align: right; margin-top: 20px">
      <a-button type="primary" @click="$emit('update:open', false)">我已妥善保存，关闭</a-button>
    </div>
  </a-modal>
</template>

<script setup lang="ts">
import { message } from 'ant-design-vue'
import type { SecretRevealVO } from '@/types'

defineProps<{ open: boolean; reveal: SecretRevealVO | null }>()
defineEmits<{ (e: 'update:open', v: boolean): void }>()

async function copy(text: string) {
  try {
    await navigator.clipboard.writeText(text)
    message.success('已复制到剪贴板')
  } catch {
    message.warning('复制失败，请手动选择复制')
  }
}
</script>

<style scoped>
.secret-field {
  margin-bottom: 14px;
}

.secret-label {
  font-size: 12px;
  color: #8a9099;
  margin-bottom: 6px;
}

.secret-row {
  display: flex;
  gap: 10px;
  align-items: center;
}

.secret-value {
  flex: 1;
  background: #f5f7fa;
  border: 1px solid #e5e9f0;
  border-radius: 6px;
  padding: 8px 12px;
  font-family: Consolas, monospace;
  font-size: 13px;
  word-break: break-all;
  color: #1f2329;
}

.secret-value.sk {
  color: #d4552a;
  background: #fff7f2;
  border-color: #ffd9c4;
}
</style>
