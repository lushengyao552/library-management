<template>
  <div>
    <van-nav-bar title="学习点" left-text="返回" @click-left="$router.back()" />
    <van-cell-group>
      <van-cell v-for="p in points" :key="p.id" :title="p.title" :label="'掌握度 '+p.masteryScore+' | '+p.scheduledDate"
        :value="p.status" is-link @click="$router.push('/review/'+p.id)" />
    </van-cell-group>
  </div>
</template>
<script setup>
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import api from '../api'
const points = ref([])
const route = useRoute()
onMounted(async () => {
  const r = await api.get('/plans/'+route.params.planId+'/points')
  points.value = r.data || []
})
</script>
