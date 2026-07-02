<template>
  <main class="login-page">
    <section class="login-panel">
      <div class="login-copy">
        <span class="eyebrow">账号安全</span>
        <h1>修改初始密码</h1>
        <p>首次登录需要设置新密码，完成后进入考试平台。</p>
      </div>

      <el-form ref="formRef" class="login-form" :model="form" :rules="rules" label-position="top" @submit.prevent>
        <h2>修改密码</h2>
        <el-form-item label="当前密码" prop="currentPassword">
          <el-input v-model="form.currentPassword" autocomplete="current-password" show-password size="large" type="password" />
        </el-form-item>
        <el-form-item label="新密码" prop="newPassword">
          <el-input v-model="form.newPassword" autocomplete="new-password" show-password size="large" type="password" />
        </el-form-item>
        <el-form-item label="确认新密码" prop="confirmPassword">
          <el-input v-model="form.confirmPassword" autocomplete="new-password" show-password size="large" type="password" @keyup.enter="submit" />
        </el-form-item>
        <el-button class="submit" :loading="submitting" size="large" type="primary" @click="submit">
          保存并进入平台
        </el-button>
      </el-form>
    </section>
  </main>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import type { FormInstance, FormRules } from 'element-plus'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'

import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()
const router = useRouter()
const formRef = ref<FormInstance>()
const submitting = ref(false)

const form = reactive({
  currentPassword: '',
  newPassword: '',
  confirmPassword: '',
})

const rules: FormRules<typeof form> = {
  currentPassword: [{ required: true, message: '请输入当前密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, message: '新密码至少 6 位', trigger: 'blur' },
  ],
  confirmPassword: [
    { required: true, message: '请确认新密码', trigger: 'blur' },
    {
      validator: (_rule, value: string, callback) => {
        if (value !== form.newPassword) {
          callback(new Error('两次输入的新密码不一致'))
          return
        }
        callback()
      },
      trigger: 'blur',
    },
  ],
}

async function submit() {
  await formRef.value?.validate()
  submitting.value = true
  try {
    await auth.changePassword(form)
    ElMessage.success('密码已修改')
    await router.replace({ name: 'admin-dashboard' })
  } finally {
    submitting.value = false
  }
}
</script>
