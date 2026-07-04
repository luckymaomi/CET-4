<template>
  <main class="login-page">
    <section class="login-panel">
      <div class="login-copy">
        <h1>通用开源考试平台</h1>
        <p>可二次开发，可商用部署。以大学英语四级真题作为演示样例，展示题库维护、考试发布、在线作答、阅卷评分和成绩复盘流程。</p>
      </div>

      <el-form ref="formRef" class="login-form" :model="form" :rules="rules" label-position="top" @submit.prevent>
        <div class="login-form__head">
          <h2>登录</h2>
          <p>进入演示工作台，体验完整考试流程。</p>
        </div>
        <el-form-item label="账号" prop="username">
          <el-input v-model.trim="form.username" autocomplete="username" size="large" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input
            v-model="form.password"
            autocomplete="current-password"
            show-password
            size="large"
            type="password"
            @keyup.enter="submit"
          />
        </el-form-item>
        <el-button class="submit" :loading="auth.loading" size="large" type="primary" @click="submit">
          进入平台
        </el-button>
      </el-form>
    </section>
  </main>
</template>

<script setup lang="ts">
import type { FormInstance, FormRules } from 'element-plus'
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()
const router = useRouter()
const route = useRoute()
const formRef = ref<FormInstance>()

const form = reactive({
  username: 'admin',
  password: 'password',
})

const rules: FormRules<typeof form> = {
  username: [{ required: true, message: '请输入账号', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
}

async function submit() {
  await formRef.value?.validate()
  await auth.login(form)
  const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : '/dashboard'
  await router.replace(redirect)
}
</script>

