<template>
  <div>
    <van-nav-bar title="待确认调整" />
    <van-cell-group>
      <van-cell v-for="a in list" :key="a.id" :title="a.triggerType" :label="a.reason">
        <van-button size="small" type="primary" @click="exec(a)">执行</van-button>
        <van-button size="small" @click="ignore(a)">忽略</van-button>
      </van-cell>
    </van-cell-group>
  </div>
</template>
<script setup>
import { ref, onMounted } from 'vue'
import api from '../api'
const list = ref([])
async function load() { list.value = (await api.get('/adjustments')).data || [] }
onMounted(load)
async function exec(a) { await api.post('/adjustments/'+a.id+'/execute'); load() }
async function ignore(a) { await api.post('/adjustments/'+a.id+'/ignore'); load() }
</script>
