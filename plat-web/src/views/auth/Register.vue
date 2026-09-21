<template>
  <div class="auth-layout">
    <div class="auth-card">
      <div class="auth-brand">
        <div class="auth-logo">A</div>
        <div>
          <div class="auth-title">注册开发者账号</div>
          <div style="font-size: 12px; color: #8a9099">创建应用 · 申请接口授权 · 查看调用统计</div>
        </div>
      </div>

      <a-form layout="vertical" :model="form" @finish="submit">
        <a-form-item label="用户名" name="username"
                     :rules="[{ required: true, pattern: /^[a-zA-Z0-9_]{4,32}$/, message: '4~32 位字母/数字/下划线' }]">
          <a-input v-model:value="form.username" size="large" autocomplete="username" />
        </a-form-item>
        <a-form-item label="密码" name="password"
                     :rules="[{ required: true, min: 6, max: 64, message: '密码长度 6~64 位' }]">
          <a-input-password v-model:value="form.password" size="large" autocomplete="new-password" />
        </a-form-item>
        <a-form-item label="昵称" name="nickname">
          <a-input v-model:value="form.nickname" size="large" placeholder="选填" />
        </a-form-item>
        <a-form-item label="邮箱" name="email"
                     :rules="[{ type: 'email', message: '邮箱格式不正确' }]">
          <a-input v-model:value="form.email" size="large" placeholder="选填" />
        </a-form-item>
        <a-form-item label="租户编码" name="tenantCode">
          <a-input v-model:value="form.tenantCode" size="large" placeholder="选填，如 T001" />
        </a-form-item>
        <a-button type="primary" size="large" block :loading="loading" html-type="submit">
          注册并登录
        </a-button>
      </a-form>

      <div class="auth-footer">
        已有账号？<router-link to="/login">直接登录</router-link>
      </div>
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

const form = reactive({
  username: '',
  password: '',
  nickname: '',
  email: '',
  tenantCode: '',
})
const loading = ref(false)

async function submit() {
  loading.value = true
  try {
    await auth.register({
      username: form.username,
      password: form.password,
      nickname: form.nickname || undefined,
      email: form.email || undefined,
      tenantCode: form.tenantCode || undefined,
    })
    message.success('注册成功，已自动登录')
    router.push(auth.homePath())
  } catch (e) {
    message.error(e instanceof ApiError ? e.message : '注册失败，请稍后重试')
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
