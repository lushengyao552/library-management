<template>
  <div style="padding: 20px; margin-top: 80px">
    <h2 style="text-align:center">智学伴</h2>
    <p style="text-align:center;color:#999">个性化学习规划智能体</p>
    <van-form @submit="onLogin">
      <van-field v-model="username" name="username" label="用户名" placeholder="admin" :rules="[{required:true,message:'请输入用户名'}]" />
      <van-field v-model="password" type="password" name="password" label="密码" placeholder="admin123" :rules="[{required:true,message:'请输入密码'}]" />
      <div style="margin: 16px">
        <van-button round block type="primary" native-type="submit">登录</van-button>
      </div>
    </van-form>
  </div>
</template>
<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { showToast } from 'vant'
import api from '../api'

const username = ref('admin')
const password = ref('admin123')
const router = useRouter()

async function onLogin() {
  const r = await api.post('/auth/login', { username: username.value, password: password.value })
  localStorage.setItem('token', r.data.token)
  localStorage.setItem('role', r.data.role)
  showToast('登录成功')
  router.push('/home')
}
</script>
