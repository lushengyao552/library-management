<template>
  <div>
    <van-nav-bar title="新建学习计划" left-text="返回" @click-left="$router.back()" />
    <van-form @submit="next">
      <div style="padding:12px 16px">
        <div style="margin-bottom:8px;color:#646566;font-size:14px">选择场景</div>
        <div style="display:flex;gap:8px">
          <div v-for="s in scenarioList" :key="s.code" @click="scenario=s"
            :style="{
              flex:1, padding:'12px', textAlign:'center', borderRadius:'8px', cursor:'pointer',
              border:'1px solid #ebedf0',
              background: scenario.code===s.code ? '#1989fa' : '#fff',
              color: scenario.code===s.code ? '#fff' : '#323233'
            }">{{ s.name }}</div>
        </div>
      </div>
      <van-field v-model="goal" label="想学什么" placeholder="如：30天学会前端开发 / 学Python / 转行产品经理" />
      <van-field v-model.number="hours" label="每天小时" type="number" />
      <van-field v-model="endDate" label="截止日期" type="date" />
      <div style="margin:16px"><van-button round block type="primary" native-type="submit">下一步：AI 诊断</van-button></div>
    </van-form>

    <van-dialog v-model:show="showDiag" title="AI 学情诊断" :show-confirm-button="false" :close-on-click-overlay="false">
      <div style="padding:16px;max-height:60vh;overflow:auto">
        <div v-for="(q,i) in diagQuestions" :key="i" style="margin-bottom:20px">
          <div style="font-weight:bold;margin-bottom:8px">{{ i+1 }}. {{ q.stem }}</div>
          <div v-for="(opt,j) in q.options" :key="j"
            @click="pickAnswer(i,j)"
            :style="{
              padding:'10px 12px', margin:'4px 0', borderRadius:'8px', fontSize:'14px',
              border:'1px solid #ebedf0', cursor:'pointer',
              background: answers[i]===j ? '#1989fa' : '#fff',
              color: answers[i]===j ? '#fff' : '#323233'
            }">{{ opt }}</div>
        </div>
      </div>
      <div style="padding:0 16px 16px">
        <van-button type="primary" block @click="confirm" :disabled="!allAnswered">生成学习计划预览</van-button>
      </div>
    </van-dialog>

    <van-dialog v-model:show="showPrev" title="AI 学习计划预览" :show-confirm-button="false">
      <div style="padding:16px;max-height:60vh;overflow:auto">
        <div v-for="(p,i) in preview.points" :key="i" style="margin-bottom:12px;padding:12px;border-radius:8px;background:#f7f8fa">
          <div style="display:flex;justify-content:space-between">
            <span style="font-weight:bold">{{ i+1 }}. {{ p.title }}</span>
            <span style="color:#1989fa;font-size:12px">{{ p.date }}</span>
          </div>
          <div v-if="p.summary" style="font-size:13px;color:#646566;margin-top:4px">{{ p.summary }}</div>
          <div style="font-size:12px;color:#969799;margin-top:4px">预计 {{ p.estHours }}h</div>
        </div>
        <div v-for="w in preview.warnings" :key="w" style="color:#e6a23c;font-size:13px;margin:4px 0">⚠ {{ w }}</div>
      </div>
      <div style="padding:0 16px 16px">
        <van-button type="success" block @click="confirmCreate">确认生成计划</van-button>
      </div>
    </van-dialog>
  </div>
</template>
<script setup>
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import api from '../api'
const router = useRouter()
const scenario = ref({code:'self-study',name:'技能自学'})
const scenarioList = ref([])
const goal = ref('')
const hours = ref(2)
const endDate = ref('')
const showDiag = ref(false)
const showPrev = ref(false)
const diagQuestions = ref([])
const answers = ref([])
const preview = ref({points:[],warnings:[]})

api.get('/plans/scenarios').then(r => { scenarioList.value = r.data || [] })

const allAnswered = computed(() => answers.value.length === diagQuestions.value.length)

function pickAnswer(i,j) { answers.value[i] = j }

async function next() {
  const r = await api.post('/plans/diagnosis', {scenarioCode:scenario.value.code, goal:goal.value})
  try {
    const parsed = typeof r.data === 'string' ? JSON.parse(r.data) : r.data
    diagQuestions.value = parsed.questions || []
    answers.value = new Array(diagQuestions.value.length).fill(-1)
  } catch(e) {
    diagQuestions.value = [{stem:'诊断出错，请重试',options:['重新开始']}]
  }
  showDiag.value = true
}
async function confirm() {
  showDiag.value = false
  const answerStr = answers.value.join(',')
  const r = await api.post('/plans/preview', {
    scenarioCode: scenario.value.code, goal: goal.value, answers: answerStr,
    dailyHours: hours.value, endDate: endDate.value || null
  })
  preview.value = r.data
  showPrev.value = true
}
async function confirmCreate() {
  const answerStr = answers.value.join(',')
  await api.post('/plans', {
    scenarioCode: scenario.value.code, goal: goal.value, answers: answerStr,
    dailyHours: hours.value, endDate: endDate.value || null
  })
  showPrev.value = false
  router.push('/home')
}
</script>
