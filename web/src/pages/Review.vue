<template>
  <div>
    <van-nav-bar title="提交练习/复述" left-text="返回" @click-left="$router.back()" />
    <van-field v-model="content" rows="6" type="textarea" label="内容" placeholder="写你的练习代码或复述..." />
    <div style="margin:16px"><van-button type="primary" block @click="submit">提交 AI 点评</van-button></div>
    <van-cell v-if="result" title="AI 点评" :value="result.score + '分'">
      <div>{{ result.feedback }}</div>
      <van-tag :type="result.passed?'success':'danger'">{{ result.passed?'通过':'未通过' }}</van-tag>
    </van-cell>
  </div>
</template>
<script setup>
import { ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import api from '../api'
const content = ref('')
const result = ref(null)
const route = useRoute()
const router = useRouter()
async function submit() {
  const r = await api.post('/review/points/'+route.params.pointId, {type:'FREE_PRACTICE', content:content.value})
  result.value = r.data
}
</script>
