import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  { path: '/', redirect: '/login' },
  { path: '/login', component: () => import('./pages/Login.vue') },
  { path: '/home', component: () => import('./pages/Home.vue') },
  { path: '/new', component: () => import('./pages/NewPlan.vue') },
  { path: '/points/:planId', component: () => import('./pages/Points.vue') },
  { path: '/review/:pointId', component: () => import('./pages/Review.vue') },
  { path: '/adjustments', component: () => import('./pages/Adjustments.vue') },
  { path: '/ledger', component: () => import('./pages/Ledger.vue') },
  { path: '/admin', component: () => import('./pages/Admin.vue') }
]

export default createRouter({ history: createWebHistory(), routes })
