<template>
  <div>
    <van-nav-bar title="我的学习计划" />
    <van-cell-group>
      <van-cell v-for="p in plans" :key="p.id" :title="p.title" :value="p.status" is-link @click="$router.push('/points/'+p.id)" />
    </van-cell-group>
    <div style="margin: 16px">
      <van-button round block type="success" @click="$router.push('/new')">+ 新建学习计划</van-button>
    </div>
    <van-tabbar route>
      <van-tabbar-item to="/home">计划</van-tabbar-item>
      <van-tabbar-item to="/adjustments">调整</van-tabbar-item>
      <van-tabbar-item to="/ledger">账本</van-tabbar-item>
      <van-tabbar-item to="/admin" v-if="role==='ADMIN'">管理</van-tabbar-item>
    </van-tabbar>
  </div>
</template>
<script setup>
import { ref, onMounted } from 'vue'
import api from '../api'
const plans = ref([])
const role = localStorage.getItem('role')
onMounted(async () => {
  const r = await api.get('/plans')
  plans.value = r.data || []
})
</script>
