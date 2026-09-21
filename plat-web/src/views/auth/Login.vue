<template>
  <div class="auth-layout">
    <div class="auth-card">
      <div class="auth-brand">
        <div class="auth-logo">A</div>
        <div>
          <div class="auth-title">API 开放平台</div>
          <div style="font-size: 12px; color: #8a9099">企业级 API 门户 · 密钥管理 · 调用统计</div>
        </div>
      </div>
      <p class="auth-desc">开发者与平台管理员均可从此登录</p>

      <a-form layout="vertical" :model="form" @finish="submit">
        <a-form-item label="用户名" name="username" :rules="[{ required: true, message: '请输入用户名' }]">
          <a-input v-model:value="form.username" size="large" placeholder="admin / demo"
                   autocomplete="username" @pressEnter="submit" />
        </a-form-item>
        <a-form-item label="密码" name="password" :rules="[{ required: true, message: '请输入密码' }]">
          <a-input-password v-model:value="form.password" size="large" placeholder="至少 6 位"
                            autocomplete="current-password" @pressEnter="submit" />
        </a-form-item>
        <a-button type="primary" size="large" block :loading="loading" html-type="submit">
          登 录
        </a-button>
      </a-form>

      <div class="auth-footer">
        还没有账号？<router-link to="/register">注册开发者账号</router-link>
      </div>
      <a-alert style="margin-top: 18px" type="info" show-icon>
        <template #message>演示账号：管理员 admin / admin123，开发者 demo / demo123</template>
      </a-alert>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { useAuthStore } from '@/stores/auth'
import { ApiError } from '@/api/client'

const router = useRouter()
const auth = useAuthStore()

const form = reactive({ username: '', password: '' })
const loading = ref(false)

async function submit() {
  if (!form.username || !form.password) return
  loading.value = true
  try {
    await auth.login({ ...form })
    message.success(`欢迎回来，${auth.user?.nickname || auth.user?.username}`)
    router.push(auth.homePath())
  } catch (e) {
    message.error(e instanceof ApiError ? e.message : '登录失败，请稍后重试')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.auth-footer {
  margin-top: 16px;
  text-align: center;
  font-size: 13px;
  color: #8a9099;
}
</style>
