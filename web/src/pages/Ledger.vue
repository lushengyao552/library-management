<template>
  <div>
    <van-nav-bar title="Token 账本" />
    <van-cell-group>
      <van-cell title="总调用次数" :value="data.calls" />
      <van-cell title="总 Tokens" :value="data.totalTokens" />
      <van-cell title="总费用(元)" :value="data.costYuan" />
      <van-cell title="节省比例" :value="data.savedRatio + '%'" />
    </van-cell-group>
  </div>
</template>
<script setup>
import { ref, onMounted } from 'vue'
import api from '../api'
const data = ref({})
onMounted(async () => { data.value = (await api.get('/token-ledger/mine')).data || {} })
</script>
