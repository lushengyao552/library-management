import axios from 'axios'
import { showToast } from 'vant'

const api = axios.create({ baseURL: '/api', timeout: 60000 })

api.interceptors.request.use(cfg => {
  const token = localStorage.getItem('token')
  if (token) cfg.headers.Authorization = 'Bearer ' + token
  return cfg
})
api.interceptors.response.use(r => {
  const body = r.data
  if (body && body.code !== undefined && body.code !== 20000) {
    showToast(body.message || '请求失败')
    return Promise.reject(body)
  }
  return body
}, e => { showToast('网络错误'); return Promise.reject(e) })

export default api
