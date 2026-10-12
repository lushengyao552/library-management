<template>
  <div>
    <van-nav-bar title="管理员" />
    <van-cell-group title="演示控制">
      <van-cell title="快进 7 天" is-link @click="ff(7)" />
      <van-cell title="快进 3 天" is-link @click="ff(3)" />
      <van-cell title="日期归零" is-link @click="reset" />
    </van-cell-group>
  </div>
</template>
<script setup>
import api from '../api'
async function ff(d) { await api.post('/admin/time/fast-forward?days='+d) }
async function reset() { await api.post('/admin/time/reset') }
</script>
